package dev.digitalcloset.server.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.JWTVerifier
import io.ktor.server.config.ApplicationConfig
import java.time.Instant
import java.util.Base64
import java.util.Date

internal class ServerSettings private constructor(
    private val jwtSecret: ByteArray,
    val jwtIssuer: String,
    val jwtAudience: String,
    val databasePath: String,
) {
    private val signingAlgorithm = Algorithm.HMAC256(jwtSecret)

    val jwtVerifier: JWTVerifier by lazy {
        JWT.require(signingAlgorithm)
            .withIssuer(jwtIssuer)
            .withAudience(jwtAudience)
            .build()
    }

    fun createAccessToken(userId: String, issuedAt: Instant): String =
        JWT.create()
            .withIssuer(jwtIssuer)
            .withAudience(jwtAudience)
            .withSubject(userId)
            .withIssuedAt(Date.from(issuedAt))
            .withExpiresAt(Date.from(issuedAt.plus(ACCESS_TOKEN_LIFETIME)))
            .sign(signingAlgorithm)

    companion object {
        private val ACCESS_TOKEN_LIFETIME = java.time.Duration.ofMinutes(15)
        private const val DEFAULT_DATABASE_PATH = "data/digital-closet.sqlite"
        private const val DEFAULT_ISSUER = "digital-closet"
        private const val DEFAULT_AUDIENCE = "digital-closet-client"
        private const val MINIMUM_SECRET_BYTES = 32

        fun fromEnvironment(values: Map<String, String> = System.getenv()): ServerSettings =
            create(
                secret = values["JWT_SECRET"],
                issuer = values["JWT_ISSUER"] ?: DEFAULT_ISSUER,
                audience = values["JWT_AUDIENCE"] ?: DEFAULT_AUDIENCE,
                databasePath = values["DATABASE_PATH"] ?: DEFAULT_DATABASE_PATH,
            )

        fun fromApplicationConfig(config: ApplicationConfig): ServerSettings =
            create(
                secret = config.valueOrEnvironment("jwt.secret", "JWT_SECRET"),
                issuer = config.propertyOrNull("jwt.issuer")?.getString()
                    ?: DEFAULT_ISSUER,
                audience = config.propertyOrNull("jwt.audience")?.getString()
                    ?: DEFAULT_AUDIENCE,
                databasePath = config.propertyOrNull("database.path")?.getString()
                    ?: System.getenv("DATABASE_PATH")
                    ?: DEFAULT_DATABASE_PATH,
            )

        private fun ApplicationConfig.valueOrEnvironment(
            property: String,
            environmentVariable: String,
        ): String? = propertyOrNull(property)?.getString() ?: System.getenv(environmentVariable)

        private fun create(
            secret: String?,
            issuer: String,
            audience: String,
            databasePath: String,
        ): ServerSettings {
            check(!secret.isNullOrBlank()) {
                "JWT_SECRET is required and must be an unpadded base64url secret of at least 32 bytes"
            }
            val decodedSecret = try {
                Base64.getUrlDecoder().decode(secret)
            } catch (_: IllegalArgumentException) {
                throw IllegalStateException(
                    "JWT_SECRET must be an unpadded base64url secret of at least 32 bytes",
                )
            }
            check(
                decodedSecret.size >= MINIMUM_SECRET_BYTES &&
                    Base64.getUrlEncoder().withoutPadding().encodeToString(decodedSecret) == secret,
            ) {
                "JWT_SECRET must be an unpadded base64url secret of at least 32 bytes"
            }
            check(issuer.isNotBlank() && audience.isNotBlank() && databasePath.isNotBlank()) {
                "JWT_ISSUER, JWT_AUDIENCE, and DATABASE_PATH must not be blank"
            }
            return ServerSettings(decodedSecret, issuer, audience, databasePath)
        }
    }
}
