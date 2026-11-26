package dev.digitalcloset.server.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import dev.digitalcloset.server.module
import dev.digitalcloset.server.db.ClosetDatabase
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.testing.testApplication
import java.nio.file.Path
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.sql.DriverManager
import java.time.Instant
import java.util.Date
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.io.TempDir
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class AuthRouteTest {
    @TempDir
    lateinit var temporaryDirectory: Path

    @Test
    fun jwtSecretMustBePresentWellFormedAndAtLeastThirtyTwoBytes() {
        assertFailsWith<IllegalStateException> {
            ServerSettings.fromEnvironment(emptyMap())
        }
        assertFailsWith<IllegalStateException> {
            ServerSettings.fromEnvironment(mapOf("JWT_SECRET" to "not a base64url secret"))
        }
        assertFailsWith<IllegalStateException> {
            ServerSettings.fromEnvironment(
                mapOf("JWT_SECRET" to java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(byteArrayOf(1, 2))),
            )
        }
        ServerSettings.fromEnvironment(mapOf("JWT_SECRET" to TEST_SECRET))
    }

    @Test
    fun databaseConnectionsEnforceOwnerReferences() {
        val database = ClosetDatabase.open(temporaryDirectory.resolve("references.sqlite").toString())
        repeat(2) {
            transaction(database.exposedDatabase) {
                val enabled = exec("PRAGMA foreign_keys") { rows ->
                    check(rows.next())
                    rows.getInt(1)
                }
                assertEquals(1, enabled)
            }
        }
    }

    @Test
    fun malformedAuthAndProfileBodiesHaveAnApiErrorResponse() = testApplication {
        environment { config = serverConfig("malformed.sqlite") }
        application { module() }
        val token = client.register("malformed@example.com", PASSWORD).sessionToken("accessToken")
        for (path in listOf("/api/v1/auth/register", "/api/v1/auth/login", "/api/v1/auth/refresh")) {
            for (body in listOf("{", "{}", """{"email":7}""")) {
                val response = client.post(path) {
                    contentType(ContentType.Application.Json)
                    setBody(body)
                }
                assertEquals(HttpStatusCode.BadRequest, response.status)
                val error = Json.parseToJsonElement(response.bodyAsText()).jsonObject
                assertTrue(error.containsKey("code"))
                assertTrue(error.containsKey("message"))
            }
        }
        val logout = client.delete("/api/v1/auth/session") {
            contentType(ContentType.Application.Json)
            setBody("{}")
        }
        assertEquals(HttpStatusCode.BadRequest, logout.status)
        assertEquals(
            """{"code":"invalid_request","message":"Request body is invalid"}""",
            logout.bodyAsText(),
        )
        val profile = client.putProfile(token, "{}")
        assertEquals(HttpStatusCode.BadRequest, profile.status)
        assertEquals(
            """{"code":"invalid_request","message":"Request body is invalid"}""",
            profile.bodyAsText(),
        )
    }

    @Test
    fun expiredAndIncorrectlySignedAccessTokensCannotReadProfiles() = testApplication {
        environment { config = serverConfig("invalid-tokens.sqlite") }
        application { module() }
        val account = client.register("tokens@example.com", PASSWORD)
        assertEquals(HttpStatusCode.Created, account.status)
        val subject = jwtSubject(account.sessionToken("accessToken"))
        val now = Instant.now()
        val signingKey = Base64.getUrlDecoder().decode(TEST_SECRET)
        val cases = listOf(
            Triple(signingKey.reversedArray(), "digital-closet-tests", "digital-closet-test-client"),
            Triple(signingKey, "wrong-issuer", "digital-closet-test-client"),
            Triple(signingKey, "digital-closet-tests", "wrong-audience"),
        )
        for ((key, issuer, audience) in cases) {
            val token = JWT.create().withSubject(subject).withIssuer(issuer).withAudience(audience)
                .withExpiresAt(Date.from(now.plusSeconds(60)))
                .sign(Algorithm.HMAC256(key))
            assertEquals(HttpStatusCode.Unauthorized, client.getProfile(token).status)
        }
        val expired = JWT.create().withSubject(subject).withIssuer("digital-closet-tests")
            .withAudience("digital-closet-test-client")
            .withExpiresAt(Date.from(now.minusSeconds(60)))
            .sign(Algorithm.HMAC256(signingKey))
        assertEquals(HttpStatusCode.Unauthorized, client.getProfile(expired).status)
    }

    @Test
    fun concurrentRefreshAcceptsTheSameTokenOnce() = testApplication {
        environment { config = serverConfig("concurrent.sqlite") }
        application { module() }
        val registered = client.register("concurrent@example.com", PASSWORD)
        val token = registered.sessionToken("refreshToken")
        val results = coroutineScope {
            (1..2).map { async { client.refresh(token).status } }.map { it.await() }
        }
        assertEquals(listOf(HttpStatusCode.OK, HttpStatusCode.Unauthorized), results.sortedBy { it.value })
    }

    @Test
    fun accountPersistsAcrossRestartAndExpiredRefreshCannotRotate() {
        val databaseName = "restart.sqlite"
        var refreshToken = ""
        testApplication {
            environment { config = serverConfig(databaseName) }
            application { module() }
            val registered = client.register("persist@example.com", PASSWORD)
            assertEquals(HttpStatusCode.Created, registered.status)
            refreshToken = registered.sessionToken("refreshToken")
        }
        testApplication {
            environment { config = serverConfig(databaseName) }
            application { module() }
            assertEquals(HttpStatusCode.OK, client.login("persist@example.com", PASSWORD).status)
            val path = temporaryDirectory.resolve(databaseName)
            DriverManager.getConnection("jdbc:sqlite:$path").use { connection ->
                connection.createStatement().use { statement ->
                    assertTrue(statement.executeUpdate("UPDATE refresh_tokens SET expires_at = 0") > 0)
                }
            }
            val expired = client.refresh(refreshToken)
            assertEquals(HttpStatusCode.Unauthorized, expired.status)
            assertEquals(
                """{"code":"invalid_refresh_token","message":"Refresh token is invalid or expired"}""",
                expired.bodyAsText(),
            )
        }
    }

    @Test
    fun registrationNormalizesEmailAndRejectsDuplicateAndInvalidCredentials() = testApplication {
        environment { config = serverConfig("registration.sqlite") }
        application { module() }

        val registered = client.register(" Alice@Example.COM ", PASSWORD)
        assertEquals(HttpStatusCode.Created, registered.status)
        val registeredSession = Json.parseToJsonElement(registered.bodyAsText()).jsonObject
        val accessToken = registeredSession["accessToken"]?.jsonPrimitive?.content
        val refreshToken = registeredSession["refreshToken"]?.jsonPrimitive?.content
        assertTrue(!accessToken.isNullOrBlank())
        assertTrue(!refreshToken.isNullOrBlank())

        val duplicate = client.register("alice@example.com", PASSWORD)
        assertEquals(HttpStatusCode.Conflict, duplicate.status)
        assertEquals(
            """{"code":"email_already_registered","message":"Email is already registered"}""",
            duplicate.bodyAsText(),
        )

        val invalidLogin = client.login("ALICE@example.com", "incorrect password")
        assertEquals(HttpStatusCode.Unauthorized, invalidLogin.status)
        assertEquals(
            """{"code":"invalid_credentials","message":"Email or password is incorrect"}""",
            invalidLogin.bodyAsText(),
        )

        val validLogin = client.login("alice@example.com", PASSWORD)
        assertEquals(HttpStatusCode.OK, validLogin.status)
        assertTrue(
            Json.parseToJsonElement(validLogin.bodyAsText()).jsonObject["accessToken"]
                ?.jsonPrimitive?.content.orEmpty().isNotBlank(),
        )
    }

    @Test
    fun refreshRotatesAtomicallyAndLogoutRevokesTheCurrentRefreshToken() = testApplication {
        environment { config = serverConfig("rotation.sqlite") }
        application { module() }

        val initial = client.register("rotate@example.com", PASSWORD)
        assertEquals(HttpStatusCode.Created, initial.status)
        val firstRefreshToken = initial.sessionToken("refreshToken")

        val rotated = client.refresh(firstRefreshToken)
        assertEquals(HttpStatusCode.OK, rotated.status)
        val secondRefreshToken = rotated.sessionToken("refreshToken")
        assertNotEquals(firstRefreshToken, secondRefreshToken)

        val replay = client.refresh(firstRefreshToken)
        assertEquals(HttpStatusCode.Unauthorized, replay.status)
        assertEquals(
            """{"code":"invalid_refresh_token","message":"Refresh token is invalid or expired"}""",
            replay.bodyAsText(),
        )

        val logout = client.logout(secondRefreshToken)
        assertEquals(HttpStatusCode.NoContent, logout.status)
        assertEquals("", logout.bodyAsText())

        val revoked = client.refresh(secondRefreshToken)
        assertEquals(HttpStatusCode.Unauthorized, revoked.status)
    }

    @Test
    fun profileReadsAndWritesAreScopedToAuthenticatedAccount() = testApplication {
        environment { config = serverConfig("profiles.sqlite") }
        application { module() }

        val unauthorized = client.get("/api/v1/profile")
        assertEquals(HttpStatusCode.Unauthorized, unauthorized.status)
        assertEquals(
            """{"code":"unauthorized","message":"Authentication is required"}""",
            unauthorized.bodyAsText(),
        )
        val malformedToken = client.get("/api/v1/profile") {
            header(HttpHeaders.Authorization, "Bearer not-a-valid-token")
        }
        assertEquals(HttpStatusCode.Unauthorized, malformedToken.status)
        assertEquals(
            """{"code":"unauthorized","message":"Authentication is required"}""",
            malformedToken.bodyAsText(),
        )

        val accountA = client.register("a@example.com", PASSWORD)
        val accountB = client.register("b@example.com", PASSWORD)
        assertEquals(HttpStatusCode.Created, accountA.status)
        assertEquals(HttpStatusCode.Created, accountB.status)
        val tokenA = accountA.sessionToken("accessToken")
        val tokenB = accountB.sessionToken("accessToken")
        val ownerIdB = jwtSubject(tokenB)

        val profileA = client.putProfile(
            tokenA,
            """{"styles":["minimal"],"colors":["navy"],"occasions":["work"],"ownerId":"$ownerIdB"}""",
        )
        assertEquals(HttpStatusCode.BadRequest, profileA.status)
        assertEquals(
            """{"code":"invalid_request","message":"Request body is invalid"}""",
            profileA.bodyAsText(),
        )
        assertEquals(
            """{"styles":[],"colors":[],"occasions":[]}""",
            client.getProfile(tokenA).bodyAsText(),
        )

        val profileAWithoutOwner = client.putProfile(
            tokenA,
            """{"styles":["minimal"],"colors":["navy"],"occasions":["work"]}""",
        )
        assertEquals(HttpStatusCode.OK, profileAWithoutOwner.status)

        val profileBBeforeUpdate = client.getProfile(tokenB)
        assertEquals(HttpStatusCode.OK, profileBBeforeUpdate.status)
        assertEquals("""{"styles":[],"colors":[],"occasions":[]}""", profileBBeforeUpdate.bodyAsText())

        val profileB = client.putProfile(
            tokenB,
            """{"styles":["classic"],"colors":["cream"],"occasions":["weekend"]}""",
        )
        assertEquals(HttpStatusCode.OK, profileB.status)

        val persistedA = client.getProfile(tokenA)
        assertEquals(HttpStatusCode.OK, persistedA.status)
        assertEquals(
            """{"styles":["minimal"],"colors":["navy"],"occasions":["work"]}""",
            persistedA.bodyAsText(),
        )
        val persistedB = client.getProfile(tokenB)
        assertEquals(
            """{"styles":["classic"],"colors":["cream"],"occasions":["weekend"]}""",
            persistedB.bodyAsText(),
        )
    }

    private fun serverConfig(databaseName: String) =
        MapApplicationConfig(
            "jwt.secret" to TEST_SECRET,
            "jwt.issuer" to "digital-closet-tests",
            "jwt.audience" to "digital-closet-test-client",
            "database.path" to temporaryDirectory.resolve(databaseName).toString(),
        )

    private suspend fun HttpClient.register(email: String, password: String) =
        post("/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"$email","password":"$password"}""")
        }

    private suspend fun HttpClient.login(email: String, password: String) =
        post("/api/v1/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"$email","password":"$password"}""")
        }

    private suspend fun HttpClient.refresh(refreshToken: String) =
        post("/api/v1/auth/refresh") {
            contentType(ContentType.Application.Json)
            setBody("""{"refreshToken":"$refreshToken"}""")
        }

    private suspend fun HttpClient.logout(refreshToken: String) =
        delete("/api/v1/auth/session") {
            contentType(ContentType.Application.Json)
            setBody("""{"refreshToken":"$refreshToken"}""")
        }

    private suspend fun HttpClient.putProfile(accessToken: String, body: String) =
        put("/api/v1/profile") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            contentType(ContentType.Application.Json)
            setBody(body)
        }

    private suspend fun HttpClient.getProfile(accessToken: String) =
        get("/api/v1/profile") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
        }

    private suspend fun io.ktor.client.statement.HttpResponse.sessionToken(name: String): String {
        val value = Json.parseToJsonElement(bodyAsText()).jsonObject[name]?.jsonPrimitive?.content
        assertNotNull(value, "$name must be present in a successful session response")
        assertTrue(value.isNotBlank(), "$name must not be blank")
        return value
    }

    private fun jwtSubject(token: String): String {
        val payload = token.split('.')[1]
        val claims = Json.parseToJsonElement(
            String(Base64.getUrlDecoder().decode(payload), StandardCharsets.UTF_8),
        ).jsonObject
        return claims.getValue("sub").jsonPrimitive.content
    }

    private companion object {
        const val PASSWORD = "a-long-test-password-2026"
        const val TEST_SECRET = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY"
    }
}
