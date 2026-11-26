package dev.digitalcloset.server

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.testing.testApplication
import java.nio.file.Path
import org.junit.jupiter.api.io.TempDir
import kotlin.test.Test
import kotlin.test.assertEquals

class HealthRouteTest {
    @TempDir
    lateinit var temporaryDirectory: Path

    @Test
    fun healthReportsAnAvailableApi() = testApplication {
        environment {
            config = MapApplicationConfig(
                "jwt.secret" to "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY",
                "jwt.issuer" to "digital-closet-tests",
                "jwt.audience" to "digital-closet-test-client",
                "database.path" to temporaryDirectory.resolve("health.sqlite").toString(),
            )
        }
        application { module() }

        for (path in listOf("/health", "/api/v1/health")) {
            val response = client.get(path)
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(ContentType.Application.Json, response.contentType()?.withoutParameters())
            assertEquals("""{"status":"ok"}""", response.bodyAsText())
        }
    }
}
