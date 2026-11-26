package dev.digitalcloset.server.db

import dev.digitalcloset.contracts.ProfileResponse
import dev.digitalcloset.server.auth.PasswordHash
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import java.util.UUID

internal data class AccountRecord(
    val id: String,
    val normalizedEmail: String,
    val passwordHash: PasswordHash,
)

internal class AuthStore(private val database: Database) {
    fun createAccount(email: String, passwordHash: PasswordHash): String? =
        try {
            transaction(database) {
                if (Users.selectAll().where { Users.normalizedEmail eq email }.any()) {
                    return@transaction null
                }
                val userId = UUID.randomUUID().toString()
                Users.insert {
                    it[id] = userId
                    it[normalizedEmail] = email
                    it[Users.passwordHash] = passwordHash.value
                    it[passwordSalt] = passwordHash.salt
                    it[passwordIterations] = passwordHash.iterations
                    it[status] = "active"
                    it[createdAt] = System.currentTimeMillis()
                }
                Profiles.insert {
                    it[Profiles.userId] = userId
                    it[styles] = encode(emptyList())
                    it[colors] = encode(emptyList())
                    it[occasions] = encode(emptyList())
                }
                userId
            }
        } catch (failure: Exception) {
            if (findAccount(email) != null) null else throw failure
        }

    fun findAccount(email: String): AccountRecord? = transaction(database) {
        Users.selectAll().where { Users.normalizedEmail eq email }.singleOrNull()?.let { row ->
            AccountRecord(
                id = row[Users.id],
                normalizedEmail = row[Users.normalizedEmail],
                passwordHash = PasswordHash(
                    value = row[Users.passwordHash],
                    salt = row[Users.passwordSalt],
                    iterations = row[Users.passwordIterations],
                ),
            )
        }
    }

    fun saveRefreshToken(userId: String, tokenHash: String, now: Long, expiresAt: Long) {
        transaction(database) {
            RefreshTokens.insert {
                it[id] = UUID.randomUUID().toString()
                it[RefreshTokens.userId] = userId
                it[RefreshTokens.tokenHash] = tokenHash
                it[createdAt] = now
                it[RefreshTokens.expiresAt] = expiresAt
                it[revokedAt] = null
            }
        }
    }

    fun rotateRefreshToken(
        oldTokenHash: String,
        newTokenHash: String,
        now: Long,
        newExpiresAt: Long,
    ): String? = transaction(database) {
        val oldToken = RefreshTokens.selectAll()
            .where { RefreshTokens.tokenHash eq oldTokenHash }
            .singleOrNull()
            ?: return@transaction null
        if (oldToken[RefreshTokens.revokedAt] != null ||
            oldToken[RefreshTokens.expiresAt] <= now
        ) {
            return@transaction null
        }
        val revoked = RefreshTokens.update({
            (RefreshTokens.tokenHash eq oldTokenHash) and
                RefreshTokens.revokedAt.isNull() and
                (RefreshTokens.expiresAt greater now)
        }) {
            it[revokedAt] = now
        }
        if (revoked != 1) return@transaction null
        RefreshTokens.insert {
            it[id] = UUID.randomUUID().toString()
            it[RefreshTokens.userId] = oldToken[RefreshTokens.userId]
            it[RefreshTokens.tokenHash] = newTokenHash
            it[createdAt] = now
            it[RefreshTokens.expiresAt] = newExpiresAt
            it[revokedAt] = null
        }
        oldToken[RefreshTokens.userId]
    }

    fun revokeRefreshToken(tokenHash: String, now: Long) {
        transaction(database) {
            RefreshTokens.update({
                (RefreshTokens.tokenHash eq tokenHash) and RefreshTokens.revokedAt.isNull()
            }) {
                it[revokedAt] = now
            }
        }
    }

    fun profile(userId: String): ProfileResponse = transaction(database) {
        val row = Profiles.selectAll()
            .where { Profiles.userId eq userId }
            .singleOrNull()
        ProfileResponse(
            styles = row?.let { decode(it[Profiles.styles]) }.orEmpty(),
            colors = row?.let { decode(it[Profiles.colors]) }.orEmpty(),
            occasions = row?.let { decode(it[Profiles.occasions]) }.orEmpty(),
        )
    }

    fun updateProfile(userId: String, profile: ProfileResponse) {
        transaction(database) {
            val updated = Profiles.update({ Profiles.userId eq userId }) {
                it[styles] = encode(profile.styles)
                it[colors] = encode(profile.colors)
                it[occasions] = encode(profile.occasions)
            }
            if (updated == 0) {
                Profiles.insert {
                    it[Profiles.userId] = userId
                    it[styles] = encode(profile.styles)
                    it[colors] = encode(profile.colors)
                    it[occasions] = encode(profile.occasions)
                }
            }
        }
    }

    private fun encode(values: List<String>): String = Json.encodeToString(values)

    private fun decode(values: String): List<String> = Json.decodeFromString(values)
}

private object Users : Table("users") {
    val id = varchar("id", 36)
    val normalizedEmail = varchar("normalized_email", 320).uniqueIndex()
    val passwordHash = varchar("password_hash", 64)
    val passwordSalt = varchar("password_salt", 64)
    val passwordIterations = integer("password_iterations")
    val status = varchar("status", 20)
    val createdAt = long("created_at")

    override val primaryKey = PrimaryKey(id)
}

private object Profiles : Table("style_profiles") {
    val userId = varchar("user_id", 36)
    val styles = text("styles")
    val colors = text("colors")
    val occasions = text("occasions")

    override val primaryKey = PrimaryKey(userId)
}

private object RefreshTokens : Table("refresh_tokens") {
    val id = varchar("id", 36)
    val userId = varchar("user_id", 36)
    val tokenHash = varchar("token_hash", 64).uniqueIndex()
    val createdAt = long("created_at")
    val expiresAt = long("expires_at")
    val revokedAt = long("revoked_at").nullable()

    override val primaryKey = PrimaryKey(id)
}
