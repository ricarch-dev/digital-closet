package dev.digitalcloset.server

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals

class HealthRouteTest {
    @Test
    fun healthReportsAnAvailableApi() = testApplication {
        application { module() }

        for (path in listOf("/health", "/api/v1/health")) {
            val response = client.get(path)
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(ContentType.Application.Json, response.contentType()?.withoutParameters())
            assertEquals("""{"status":"ok"}""", response.bodyAsText())
        }
    }
}
