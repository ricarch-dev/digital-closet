package dev.digitalcloset.server.garments

import dev.digitalcloset.contracts.ApiError
import dev.digitalcloset.contracts.CreateGarmentRequest
import dev.digitalcloset.contracts.UpdateGarmentRequest
import dev.digitalcloset.server.auth.authenticatedUserId
import dev.digitalcloset.server.db.GarmentStore
import dev.digitalcloset.server.media.PrivatePhotoStore
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route

private val categories = setOf("top", "bottom", "one_piece", "shoes", "accessory")
private val statuses = setOf("active", "unavailable", "archived")

internal fun Route.garmentRoutes(store: GarmentStore, photos: PrivatePhotoStore) {
    route("/api/v1/garments") {
        post {
            val ownerId = call.authenticatedUserId()
            if (ownerId == null) {
                call.respond(HttpStatusCode.Unauthorized, ApiError("unauthorized", "Authentication is required"))
                return@post
            }
            val request = call.receive<CreateGarmentRequest>()
            if (!valid(request.category, request.color, request.tags, request.season, request.notes)) {
                call.respond(HttpStatusCode.BadRequest, ApiError("invalid_garment", "Garment attributes are invalid"))
                return@post
            }
            val garment = store.createGarment(ownerId, request)
            if (garment == null) {
                call.respond(HttpStatusCode.NotFound, ApiError("not_found", "Confirmed photo was not found"))
                return@post
            }
            call.respond(HttpStatusCode.Created, garment)
        }

        get {
            val ownerId = call.authenticatedUserId()
            if (ownerId == null) {
                call.respond(HttpStatusCode.Unauthorized, ApiError("unauthorized", "Authentication is required"))
                return@get
            }
            call.respond(
                store.garments(
                    ownerId,
                    call.request.queryParameters["category"],
                    call.request.queryParameters["color"],
                    call.request.queryParameters["tag"],
                ),
            )
        }

        get("/{id}") {
            val ownerId = call.authenticatedUserId()
            if (ownerId == null) {
                call.respond(HttpStatusCode.Unauthorized, ApiError("unauthorized", "Authentication is required"))
                return@get
            }
            val garment = store.garment(ownerId, call.parameters["id"].orEmpty())
            if (garment == null) {
                call.respond(HttpStatusCode.NotFound, ApiError("not_found", "Garment was not found"))
                return@get
            }
            call.respond(garment.garment)
        }

        patch("/{id}") {
            val ownerId = call.authenticatedUserId()
            if (ownerId == null) {
                call.respond(HttpStatusCode.Unauthorized, ApiError("unauthorized", "Authentication is required"))
                return@patch
            }
            val request = call.receive<UpdateGarmentRequest>()
            val current = store.garment(ownerId, call.parameters["id"].orEmpty())
            if (current == null) {
                call.respond(HttpStatusCode.NotFound, ApiError("not_found", "Garment was not found"))
                return@patch
            }
            if (!valid(
                    request.category ?: current.garment.category,
                    request.color ?: current.garment.color,
                    request.tags ?: current.garment.tags,
                    request.season ?: current.garment.season,
                    request.notes ?: current.garment.notes,
                ) || (request.status != null && request.status !in statuses)
            ) {
                call.respond(HttpStatusCode.BadRequest, ApiError("invalid_garment", "Garment attributes are invalid"))
                return@patch
            }
            call.respond(store.updateGarment(ownerId, current.garment.id, request)!!)
        }

        delete("/{id}") {
            val ownerId = call.authenticatedUserId()
            if (ownerId == null) {
                call.respond(HttpStatusCode.Unauthorized, ApiError("unauthorized", "Authentication is required"))
                return@delete
            }
            val garment = store.garment(ownerId, call.parameters["id"].orEmpty())
            if (garment == null) {
                call.respond(HttpStatusCode.NotFound, ApiError("not_found", "Garment was not found"))
                return@delete
            }
            photos.delete(garment.objectKey)
            store.deleteGarment(ownerId, garment.garment.id)
            call.respond(HttpStatusCode.NoContent)
        }
    }
}

private fun valid(category: String, color: String, tags: List<String>, season: String, notes: String): Boolean =
    category in categories &&
        color.isNotBlank() && color.length <= 60 &&
        tags.size <= 20 && tags.all { it.isNotBlank() && it.length <= 60 } &&
        season.isNotBlank() && season.length <= 60 && notes.length <= 1000
