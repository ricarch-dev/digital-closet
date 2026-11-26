package dev.digitalcloset.server.garments

import dev.digitalcloset.server.module
import dev.digitalcloset.server.auth.ServerSettings
import dev.digitalcloset.server.db.ClosetDatabase
import dev.digitalcloset.server.media.PhotoMetadata
import dev.digitalcloset.server.media.PrivatePhotoStore
import dev.digitalcloset.server.media.SignedUpload
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.testing.testApplication
import java.nio.file.Path
import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.io.TempDir

class MediaGarmentRouteTest {
    @TempDir
    lateinit var directory: Path

    @Test
    fun ownerControlsUploadConfirmationReadAndGarmentLifecycle() = testApplication {
        val config = MapApplicationConfig(
            "jwt.secret" to Base64.getUrlEncoder().withoutPadding()
                .encodeToString(ByteArray(32) { it.toByte() }),
            "database.path" to directory.resolve("closet.sqlite").toString(),
        )
        val photos = FakePhotos()
        environment { this.config = config }
        application {
            module(ServerSettings.fromApplicationConfig(config), ClosetDatabase.open(
                directory.resolve("closet.sqlite").toString(),
            ), photos)
        }
        suspend fun register(email: String): String {
            val response = client.post("/api/v1/auth/register") {
                contentType(ContentType.Application.Json)
                setBody("""{"email":"$email","password":"long-password-2026"}""")
            }
            assertEquals(HttpStatusCode.Created, response.status)
            return Json.parseToJsonElement(response.bodyAsText()).jsonObject
                .getValue("accessToken").jsonPrimitive.content
        }
        val owner = register("photo-owner@example.com")
        val other = register("photo-other@example.com")
        val request = """{"contentType":"image/jpeg","byteSize":128}"""
        val upload = client.post("/api/v1/media/upload-url") {
            header(HttpHeaders.Authorization, "Bearer $owner")
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        assertEquals(HttpStatusCode.Created, upload.status)
        val uploadBody = Json.parseToJsonElement(upload.bodyAsText()).jsonObject
        val uploadId = uploadBody.getValue("uploadId").jsonPrimitive.content
        assertTrue(uploadBody.getValue("url").jsonPrimitive.content.startsWith("https://photos.example.invalid/"))
        assertEquals(
            HttpStatusCode.Conflict,
            client.post("/api/v1/media/$uploadId/confirm") {
                header(HttpHeaders.Authorization, "Bearer $owner")
            }.status,
        )
        photos.completeUpload("image/jpeg", 128)
        assertEquals(
            HttpStatusCode.NotFound,
            client.post("/api/v1/media/$uploadId/confirm") {
                header(HttpHeaders.Authorization, "Bearer $other")
            }.status,
        )
        assertEquals(
            HttpStatusCode.OK,
            client.post("/api/v1/media/$uploadId/confirm") {
                header(HttpHeaders.Authorization, "Bearer $owner")
            }.status,
        )
        val createBody =
            """{"uploadId":"$uploadId","category":"top","color":"navy","tags":["minimal"],"season":"all","notes":""}"""
        assertEquals(
            HttpStatusCode.NotFound,
            client.post("/api/v1/garments") {
                header(HttpHeaders.Authorization, "Bearer $other")
                contentType(ContentType.Application.Json)
                setBody(createBody)
            }.status,
        )
        val garment = client.post("/api/v1/garments") {
            header(HttpHeaders.Authorization, "Bearer $owner")
            contentType(ContentType.Application.Json)
            setBody(createBody)
        }
        assertEquals(HttpStatusCode.Created, garment.status)
        val id = Json.parseToJsonElement(garment.bodyAsText()).jsonObject.getValue("id").jsonPrimitive.content
        val otherList = client.get("/api/v1/garments") { header(HttpHeaders.Authorization, "Bearer $other") }
        assertEquals(0, Json.parseToJsonElement(otherList.bodyAsText()).jsonArray.size)
        assertEquals(
            HttpStatusCode.NotFound,
            client.get("/api/v1/garments/$id") { header(HttpHeaders.Authorization, "Bearer $other") }.status,
        )
        val updated = client.patch("/api/v1/garments/$id") {
            header(HttpHeaders.Authorization, "Bearer $owner")
            contentType(ContentType.Application.Json)
            setBody("""{"status":"archived","color":"cream"}""")
        }
        assertEquals(HttpStatusCode.OK, updated.status)
        assertEquals(
            "archived",
            Json.parseToJsonElement(updated.bodyAsText()).jsonObject.getValue("status").jsonPrimitive.content,
        )
        assertEquals(
            HttpStatusCode.NotFound,
            client.get("/api/v1/media/$uploadId/read-url") {
                header(HttpHeaders.Authorization, "Bearer $other")
            }.status,
        )
        assertEquals(
            HttpStatusCode.OK,
            client.get("/api/v1/media/$uploadId/read-url") {
                header(HttpHeaders.Authorization, "Bearer $owner")
            }.status,
        )
        assertEquals(
            HttpStatusCode.NoContent,
            client.delete("/api/v1/garments/$id") {
                header(HttpHeaders.Authorization, "Bearer $owner")
            }.status,
        )
        assertEquals(1, photos.deleted.size)
        assertEquals(
            HttpStatusCode.NotFound,
            client.get("/api/v1/garments/$id") { header(HttpHeaders.Authorization, "Bearer $owner") }.status,
        )
    }

    private class FakePhotos : PrivatePhotoStore {
        private var key: String? = null
        private var metadata: PhotoMetadata? = null
        val deleted = mutableListOf<String>()

        override fun presignUpload(key: String, contentType: String, byteSize: Long): SignedUpload {
            this.key = key
            return SignedUpload("https://photos.example.invalid/upload", mapOf("Content-Type" to contentType))
        }

        fun completeUpload(contentType: String, byteSize: Long) {
            metadata = PhotoMetadata(contentType, byteSize)
        }

        override fun metadata(key: String): PhotoMetadata? = metadata.takeIf { key == this.key }

        override fun presignRead(key: String): String = "https://photos.example.invalid/read"

        override fun delete(key: String) {
            deleted.add(key)
        }
    }
}
