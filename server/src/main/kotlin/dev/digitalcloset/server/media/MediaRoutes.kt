package dev.digitalcloset.server.media

import dev.digitalcloset.contracts.ApiError
import dev.digitalcloset.contracts.ConfirmUploadResponse
import dev.digitalcloset.contracts.ReadPhotoUrlResponse
import dev.digitalcloset.contracts.RequestUploadUrl
import dev.digitalcloset.contracts.UploadUrlResponse
import dev.digitalcloset.server.auth.authenticatedUserId
import dev.digitalcloset.server.db.GarmentStore
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import java.time.Duration

private val urlLifetime = Duration.ofMinutes(5)
private val allowedTypes = setOf("image/jpeg", "image/png", "image/webp")
private const val MAX_PHOTO_BYTES = 10L * 1024 * 1024

internal fun Route.mediaRoutes(store: GarmentStore, photos: PrivatePhotoStore) {
    route("/api/v1/media") {
        post("/upload-url") {
            val ownerId = call.authenticatedUserId()
            if (ownerId == null) {
                call.respond(HttpStatusCode.Unauthorized, ApiError("unauthorized", "Authentication is required"))
                return@post
            }
            val request = call.receive<RequestUploadUrl>()
            if (request.contentType !in allowedTypes || request.byteSize !in 1..MAX_PHOTO_BYTES) {
                call.respond(HttpStatusCode.BadRequest, ApiError("invalid_photo", "Unsupported image type or size"))
                return@post
            }
            val expiresAt = System.currentTimeMillis() + urlLifetime.toMillis()
            val upload = store.createUpload(ownerId, request.contentType, request.byteSize, expiresAt)
            val signed = photos.presignUpload(upload.objectKey, request.contentType, request.byteSize)
            call.respond(
                HttpStatusCode.Created,
                UploadUrlResponse(upload.id, signed.url, signed.headers, expiresAt),
            )
        }

        post("/{id}/confirm") {
            val ownerId = call.authenticatedUserId()
            if (ownerId == null) {
                call.respond(HttpStatusCode.Unauthorized, ApiError("unauthorized", "Authentication is required"))
                return@post
            }
            val id = call.parameters["id"].orEmpty()
            val upload = store.upload(ownerId, id)
            if (upload == null) {
                call.respond(HttpStatusCode.NotFound, ApiError("not_found", "Photo was not found"))
                return@post
            }
            val metadata = photos.metadata(upload.objectKey)
            if (metadata == null || !store.confirm(ownerId, id, metadata, System.currentTimeMillis())) {
                call.respond(HttpStatusCode.Conflict, ApiError("upload_not_confirmed", "Photo upload is incomplete"))
                return@post
            }
            call.respond(ConfirmUploadResponse(upload.id))
        }

        get("/{id}/read-url") {
            val ownerId = call.authenticatedUserId()
            if (ownerId == null) {
                call.respond(HttpStatusCode.Unauthorized, ApiError("unauthorized", "Authentication is required"))
                return@get
            }
            val upload = store.upload(ownerId, call.parameters["id"].orEmpty())
            if (upload == null || upload.status !in setOf("confirmed", "attached")) {
                call.respond(HttpStatusCode.NotFound, ApiError("not_found", "Photo was not found"))
                return@get
            }
            val url = photos.presignRead(upload.objectKey)
            call.respond(ReadPhotoUrlResponse(url, System.currentTimeMillis() + urlLifetime.toMillis()))
        }
    }
}
