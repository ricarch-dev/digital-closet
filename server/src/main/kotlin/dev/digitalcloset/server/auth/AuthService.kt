package dev.digitalcloset.server.auth

import dev.digitalcloset.contracts.SessionResponse
import dev.digitalcloset.server.db.AuthStore
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Duration
import java.time.Instant
import java.util.Base64
import java.util.Locale

internal class AuthService(
    private val store: AuthStore,
    private val settings: ServerSettings,
    private val passwordHasher: PasswordHasher = PasswordHasher(),
    private val secureRandom: SecureRandom = SecureRandom(),
    private val now: () -> Instant = Instant::now,
) {
    fun register(email: String, password: String): RegistrationResult {
        val normalizedEmail = normalizeEmail(email) ?: return RegistrationResult.Invalid
        if (password.length < MINIMUM_PASSWORD_LENGTH) return RegistrationResult.Invalid
        val passwordHash = passwordHasher.hash(password)
        val userId = store.createAccount(normalizedEmail, passwordHash)
            ?: return RegistrationResult.DuplicateEmail
        return RegistrationResult.Created(createSession(userId))
    }

    fun login(email: String, password: String): SessionResponse? {
        val normalizedEmail = normalizeEmail(email) ?: return null
        val account = store.findAccount(normalizedEmail) ?: return null
        if (!passwordHasher.verify(password, account.passwordHash)) return null
        return createSession(account.id)
    }

    fun refresh(refreshToken: String): SessionResponse? {
        if (refreshToken.length !in MINIMUM_REFRESH_TOKEN_LENGTH..MAXIMUM_REFRESH_TOKEN_LENGTH) {
            return null
        }
        val currentTime = now()
        val replacementToken = newRefreshToken()
        val userId = store.rotateRefreshToken(
            oldTokenHash = hashToken(refreshToken),
            newTokenHash = hashToken(replacementToken),
            now = currentTime.toEpochMilli(),
            newExpiresAt = currentTime.plus(REFRESH_TOKEN_LIFETIME).toEpochMilli(),
        ) ?: return null
        return SessionResponse(
            accessToken = settings.createAccessToken(userId, currentTime),
            refreshToken = replacementToken,
        )
    }

    fun logout(refreshToken: String) {
        if (refreshToken.length in MINIMUM_REFRESH_TOKEN_LENGTH..MAXIMUM_REFRESH_TOKEN_LENGTH) {
            store.revokeRefreshToken(hashToken(refreshToken), now().toEpochMilli())
        }
    }

    private fun createSession(userId: String): SessionResponse {
        val currentTime = now()
        val refreshToken = newRefreshToken()
        store.saveRefreshToken(
            userId = userId,
            tokenHash = hashToken(refreshToken),
            now = currentTime.toEpochMilli(),
            expiresAt = currentTime.plus(REFRESH_TOKEN_LIFETIME).toEpochMilli(),
        )
        return SessionResponse(
            accessToken = settings.createAccessToken(userId, currentTime),
            refreshToken = refreshToken,
        )
    }

    private fun newRefreshToken(): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(ByteArray(REFRESH_TOKEN_BYTES).also(secureRandom::nextBytes))

    private fun hashToken(token: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(token.toByteArray(StandardCharsets.UTF_8))
            .joinToString(separator = "") { byte -> "%02x".format(byte) }

    private fun normalizeEmail(email: String): String? {
        val normalized = email.trim().lowercase(Locale.ROOT)
        return normalized.takeIf {
            it.length <= MAXIMUM_EMAIL_LENGTH &&
                EMAIL_PATTERN.matches(it)
        }
    }

    internal sealed interface RegistrationResult {
        data class Created(val session: SessionResponse) : RegistrationResult
        data object DuplicateEmail : RegistrationResult
        data object Invalid : RegistrationResult
    }

    private companion object {
        val REFRESH_TOKEN_LIFETIME: Duration = Duration.ofDays(30)
        val EMAIL_PATTERN = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
        const val MINIMUM_PASSWORD_LENGTH = 12
        const val MAXIMUM_EMAIL_LENGTH = 320
        const val REFRESH_TOKEN_BYTES = 32
        const val MINIMUM_REFRESH_TOKEN_LENGTH = 43
        const val MAXIMUM_REFRESH_TOKEN_LENGTH = 128
    }
}
