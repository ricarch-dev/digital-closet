package dev.digitalcloset.server.auth

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

internal data class PasswordHash(
    val value: String,
    val salt: String,
    val iterations: Int,
)

internal class PasswordHasher(
    private val secureRandom: SecureRandom = SecureRandom(),
    private val iterations: Int = DEFAULT_ITERATIONS,
) {
    fun hash(password: String): PasswordHash {
        val salt = ByteArray(SALT_BYTES).also(secureRandom::nextBytes)
        val passwordChars = password.toCharArray()
        return try {
            PasswordHash(
                value = encode(derive(passwordChars, salt, iterations)),
                salt = encode(salt),
                iterations = iterations,
            )
        } finally {
            passwordChars.fill('\u0000')
        }
    }

    fun verify(password: String, stored: PasswordHash): Boolean {
        val expected = runCatching { decode(stored.value) }.getOrNull() ?: return false
        val salt = runCatching { decode(stored.salt) }.getOrNull() ?: return false
        if (salt.size != SALT_BYTES || stored.iterations < MINIMUM_ITERATIONS) return false
        val passwordChars = password.toCharArray()
        return try {
            MessageDigest.isEqual(expected, derive(passwordChars, salt, stored.iterations))
        } finally {
            passwordChars.fill('\u0000')
        }
    }

    private fun derive(password: CharArray, salt: ByteArray, iterationCount: Int): ByteArray {
        val spec = PBEKeySpec(password, salt, iterationCount, KEY_BITS)
        return try {
            SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun encode(value: ByteArray): String = Base64.getEncoder().encodeToString(value)

    private fun decode(value: String): ByteArray = Base64.getDecoder().decode(value)

    private companion object {
        const val ALGORITHM = "PBKDF2WithHmacSHA256"
        const val DEFAULT_ITERATIONS = 600_000
        const val MINIMUM_ITERATIONS = 600_000
        const val KEY_BITS = 256
        const val SALT_BYTES = 16
    }
}
