package dev.digitalcloset.server

import dev.digitalcloset.server.auth.AuthService
import dev.digitalcloset.server.auth.ServerSettings
import dev.digitalcloset.server.auth.authRoutes
import dev.digitalcloset.server.auth.profileRoutes
import dev.digitalcloset.server.db.AuthStore
import dev.digitalcloset.server.db.ClosetDatabase
import dev.digitalcloset.contracts.ApiError
import dev.digitalcloset.contracts.HealthResponse
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.http.HttpStatusCode
import io.ktor.server.netty.Netty
import io.ktor.server.response.respond
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json

fun main() {
    val port = serverPort(System.getenv("PORT"))
    val settings = ServerSettings.fromEnvironment()
    val database = ClosetDatabase.open(settings.databasePath)
    embeddedServer(Netty, port = port, host = "127.0.0.1") {
        module(settings, database)
    }.start(wait = true)
}

internal fun serverPort(configured: String?): Int {
    if (configured == null) return 8080
    return configured.toIntOrNull()?.takeIf { it in 1..65535 }
        ?: throw IllegalArgumentException("PORT must be an integer between 1 and 65535")
}

fun Application.module() {
    val settings = ServerSettings.fromApplicationConfig(environment.config)
    module(settings, ClosetDatabase.open(settings.databasePath))
}

internal fun Application.module(settings: ServerSettings, database: ClosetDatabase) {
    val store = AuthStore(database.exposedDatabase)
    val sessions = AuthService(store, settings)

    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = false })
    }
    install(StatusPages) {
        exception<BadRequestException> { call, _ ->
            call.respond(HttpStatusCode.BadRequest, ApiError("invalid_request", "Request body is invalid"))
        }
    }
    install(Authentication) {
        jwt("auth-jwt") {
            realm = "digital-closet"
            verifier(settings.jwtVerifier)
            validate { credential ->
                credential.payload.subject
                    ?.takeIf(String::isNotBlank)
                    ?.let { JWTPrincipal(credential.payload) }
            }
            challenge { _, _ ->
                call.respond(
                    HttpStatusCode.Unauthorized,
                    ApiError("unauthorized", "Authentication is required"),
                )
            }
        }
    }
    routing {
        listOf("/health", "/api/v1/health").forEach { path ->
            get(path) {
                call.respond(HealthResponse(status = "ok"))
            }
        }
        authRoutes(sessions)
        authenticate("auth-jwt") {
            profileRoutes(store)
        }
    }
}
