package dev.digitalcloset.server.db

import dev.digitalcloset.contracts.CreateGarmentRequest
import dev.digitalcloset.contracts.GarmentResponse
import dev.digitalcloset.contracts.UpdateGarmentRequest
import dev.digitalcloset.server.media.PhotoMetadata
import java.util.UUID
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update

internal data class UploadRecord(
    val id: String,
    val objectKey: String,
    val contentType: String,
    val byteSize: Long,
    val status: String,
    val expiresAt: Long,
)

internal data class StoredGarment(val garment: GarmentResponse, val objectKey: String)

internal class GarmentStore(private val database: Database) {
    fun createUpload(ownerId: String, contentType: String, byteSize: Long, expiresAt: Long): UploadRecord {
        val id = UUID.randomUUID().toString()
        val key = "photos/$ownerId/${UUID.randomUUID()}"
        transaction(database) {
            MediaUploads.insert {
                it[MediaUploads.id] = id
                it[MediaUploads.ownerId] = ownerId
                it[objectKey] = key
                it[mimeType] = contentType
                it[MediaUploads.byteSize] = byteSize
                it[status] = "pending"
                it[MediaUploads.expiresAt] = expiresAt
                it[createdAt] = System.currentTimeMillis()
            }
        }
        return UploadRecord(id, key, contentType, byteSize, "pending", expiresAt)
    }

    fun upload(ownerId: String, id: String): UploadRecord? = transaction(database) {
        MediaUploads.selectAll()
            .where { (MediaUploads.ownerId eq ownerId) and (MediaUploads.id eq id) }
            .singleOrNull()
            ?.let(::toUpload)
    }

    fun confirm(ownerId: String, id: String, metadata: PhotoMetadata, now: Long): Boolean =
        transaction(database) {
            val upload = MediaUploads.selectAll()
                .where { (MediaUploads.ownerId eq ownerId) and (MediaUploads.id eq id) }
                .singleOrNull()
                ?.let(::toUpload)
                ?: return@transaction false
            if (upload.status == "confirmed" || upload.status == "attached") return@transaction true
            if (upload.status != "pending" || upload.expiresAt <= now ||
                upload.contentType != metadata.contentType || upload.byteSize != metadata.byteSize
            ) return@transaction false
            MediaUploads.update({
                (MediaUploads.ownerId eq ownerId) and (MediaUploads.id eq id) and
                    (MediaUploads.status eq "pending")
            }) { it[status] = "confirmed" } == 1
        }

    fun createGarment(ownerId: String, request: CreateGarmentRequest): GarmentResponse? =
        transaction(database) {
            val upload = MediaUploads.selectAll().where {
                (MediaUploads.ownerId eq ownerId) and
                    (MediaUploads.id eq request.uploadId) and (MediaUploads.status eq "confirmed")
            }.singleOrNull()?.let(::toUpload) ?: return@transaction null
            val id = UUID.randomUUID().toString()
            val now = System.currentTimeMillis()
            Garments.insert {
                it[Garments.id] = id
                it[Garments.ownerId] = ownerId
                it[photoUploadId] = upload.id
                it[photoKey] = upload.objectKey
                it[category] = request.category
                it[color] = request.color
                it[tags] = Json.encodeToString(request.tags)
                it[season] = request.season
                it[notes] = request.notes
                it[status] = "active"
                it[available] = 1
                it[createdAt] = now
                it[updatedAt] = now
            }
            MediaUploads.update({
                (MediaUploads.ownerId eq ownerId) and (MediaUploads.id eq upload.id)
            }) { it[status] = "attached" }
            GarmentResponse(id, upload.id, request.category, request.color, request.tags,
                request.season, request.notes, "active", true)
        }

    fun garment(ownerId: String, id: String): StoredGarment? = transaction(database) {
        Garments.selectAll().where {
            (Garments.ownerId eq ownerId) and (Garments.id eq id)
        }.singleOrNull()?.let(::toGarment)
    }

    fun garments(ownerId: String, category: String?, color: String?, tag: String?): List<GarmentResponse> =
        transaction(database) {
            Garments.selectAll().where { Garments.ownerId eq ownerId }
                .map(::toGarment)
                .map(StoredGarment::garment)
                .filter { category == null || it.category == category }
                .filter { color == null || it.color == color }
                .filter { tag == null || tag in it.tags }
        }

    fun updateGarment(ownerId: String, id: String, request: UpdateGarmentRequest): GarmentResponse? =
        transaction(database) {
            val current = Garments.selectAll().where {
                (Garments.ownerId eq ownerId) and (Garments.id eq id)
            }.singleOrNull()?.let(::toGarment) ?: return@transaction null
            val next = current.garment.copy(
                category = request.category ?: current.garment.category,
                color = request.color ?: current.garment.color,
                tags = request.tags ?: current.garment.tags,
                season = request.season ?: current.garment.season,
                notes = request.notes ?: current.garment.notes,
                status = request.status ?: current.garment.status,
                available = request.available ?: current.garment.available,
            )
            Garments.update({ (Garments.ownerId eq ownerId) and (Garments.id eq id) }) {
                it[category] = next.category
                it[color] = next.color
                it[tags] = Json.encodeToString(next.tags)
                it[season] = next.season
                it[notes] = next.notes
                it[status] = next.status
                it[available] = if (next.available) 1 else 0
                it[updatedAt] = System.currentTimeMillis()
            }
            next
        }

    fun deleteGarment(ownerId: String, id: String): Boolean = transaction(database) {
        val garment = Garments.selectAll().where {
            (Garments.ownerId eq ownerId) and (Garments.id eq id)
        }.singleOrNull() ?: return@transaction false
        Garments.deleteWhere { (Garments.ownerId eq ownerId) and (Garments.id eq id) }
        MediaUploads.deleteWhere {
            (MediaUploads.ownerId eq ownerId) and (MediaUploads.id eq garment[Garments.photoUploadId])
        }
        true
    }

    private fun toUpload(row: ResultRow) = UploadRecord(
        row[MediaUploads.id],
        row[MediaUploads.objectKey],
        row[MediaUploads.mimeType],
        row[MediaUploads.byteSize],
        row[MediaUploads.status],
        row[MediaUploads.expiresAt],
    )

    private fun toGarment(row: ResultRow) = StoredGarment(
        GarmentResponse(
            id = row[Garments.id],
            photoId = row[Garments.photoUploadId],
            category = row[Garments.category],
            color = row[Garments.color],
            tags = Json.decodeFromString(row[Garments.tags]),
            season = row[Garments.season],
            notes = row[Garments.notes],
            status = row[Garments.status],
            available = row[Garments.available] == 1,
        ),
        row[Garments.photoKey],
    )
}

private object MediaUploads : Table("media_uploads") {
    val id = varchar("id", 36)
    val ownerId = varchar("owner_id", 36)
    val objectKey = text("object_key")
    val mimeType = varchar("mime_type", 80)
    val byteSize = long("byte_size")
    val status = varchar("status", 20)
    val expiresAt = long("expires_at")
    val createdAt = long("created_at")

    override val primaryKey = PrimaryKey(id)
}

private object Garments : Table("garments") {
    val id = varchar("id", 36)
    val ownerId = varchar("owner_id", 36)
    val photoUploadId = varchar("photo_upload_id", 36)
    val photoKey = text("photo_key")
    val category = varchar("category", 60)
    val color = varchar("color", 60)
    val tags = text("tags")
    val season = varchar("season", 60)
    val notes = text("notes")
    val status = varchar("status", 20)
    val available = integer("available")
    val createdAt = long("created_at")
    val updatedAt = long("updated_at")

    override val primaryKey = PrimaryKey(id)
}
