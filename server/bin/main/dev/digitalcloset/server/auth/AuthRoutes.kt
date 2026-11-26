package dev.digitalcloset.server.auth

import dev.digitalcloset.contracts.ApiError
import dev.digitalcloset.contracts.LoginRequest
import dev.digitalcloset.contracts.RefreshRequest
import dev.digitalcloset.contracts.RegisterRequest
import dev.digitalcloset.server.auth.AuthService.RegistrationResult
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.post
import io.ktor.server.routing.route

internal fun Route.authRoutes(service: AuthService) {
    route("/api/v1/auth") {
        post("/register") {
            when (val result = call.receive<RegisterRequest>().let {
                service.register(it.email, it.password)
            }) {
                is RegistrationResult.Created ->
                    call.respond(HttpStatusCode.Created, result.session)
                RegistrationResult.DuplicateEmail ->
                    call.respond(
                        HttpStatusCode.Conflict,
                        ApiError("email_already_registered", "Email is already registered"),
                    )
                RegistrationResult.Invalid ->
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ApiError("invalid_registration", "A valid email and password are required"),
                    )
            }
        }

        post("/login") {
            val request = call.receive<LoginRequest>()
            val session = service.login(request.email, request.password)
            if (session == null) {
                call.respond(
                    HttpStatusCode.Unauthorized,
                    ApiError("invalid_credentials", "Email or password is incorrect"),
                )
            } else {
                call.respond(HttpStatusCode.OK, session)
            }
        }

        post("/refresh") {
            val request = call.receive<RefreshRequest>()
            val session = service.refresh(request.refreshToken)
            if (session == null) {
                call.respond(
                    HttpStatusCode.Unauthorized,
                    ApiError("invalid_refresh_token", "Refresh token is invalid or expired"),
                )
            } else {
                call.respond(HttpStatusCode.OK, session)
            }
        }

        delete("/session") {
            val request = call.receive<RefreshRequest>()
            service.logout(request.refreshToken)
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
