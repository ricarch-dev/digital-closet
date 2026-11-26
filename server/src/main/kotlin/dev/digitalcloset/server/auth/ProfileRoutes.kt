package dev.digitalcloset.server.auth

import dev.digitalcloset.contracts.ApiError
import dev.digitalcloset.contracts.ProfileResponse
import dev.digitalcloset.contracts.UpdateProfileRequest
import dev.digitalcloset.server.db.AuthStore
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import io.ktor.server.routing.route

internal fun Route.profileRoutes(store: AuthStore) {
    route("/api/v1/profile") {
        get {
            val userId = call.authenticatedUserId()
            if (userId == null) {
                call.respond(
                    HttpStatusCode.Unauthorized,
                    ApiError("unauthorized", "Authentication is required"),
                )
                return@get
            }
            call.respond(store.profile(userId))
        }

        put {
            val userId = call.authenticatedUserId()
            if (userId == null) {
                call.respond(
                    HttpStatusCode.Unauthorized,
                    ApiError("unauthorized", "Authentication is required"),
                )
                return@put
            }
            val request = call.receive<UpdateProfileRequest>()
            val profile = ProfileResponse(request.styles, request.colors, request.occasions)
            store.updateProfile(userId, profile)
            call.respond(profile)
        }
    }
}

internal fun ApplicationCall.authenticatedUserId(): String? =
    principal<JWTPrincipal>()?.payload?.subject?.takeIf(String::isNotBlank)
