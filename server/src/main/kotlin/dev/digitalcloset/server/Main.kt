package dev.digitalcloset.server

import dev.digitalcloset.contracts.HealthResponse
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    embeddedServer(Netty, port = port, host = "127.0.0.1") { module() }.start(wait = true)
}

fun Application.module() {
    install(ContentNegotiation) { json() }
    routing {
        listOf("/health", "/api/v1/health").forEach { path ->
            get(path) {
                call.respond(HealthResponse(status = "ok"))
            }
        }
    }
}
