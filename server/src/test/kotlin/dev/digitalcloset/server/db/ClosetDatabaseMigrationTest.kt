package dev.digitalcloset.server.db

import java.nio.file.Path
import java.sql.DriverManager
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.jupiter.api.io.TempDir

class ClosetDatabaseMigrationTest {
    @TempDir
    lateinit var directory: Path

    @Test
    fun openingExistingAuthDatabaseAddsOwnerScopedPhotoAndGarmentTables() {
        val path = directory.resolve("upgrade.sqlite")
        val url = "jdbc:sqlite:$path"
        val first = ClosetDatabase.open(path.toString())
        DriverManager.getConnection(url).use { connection ->
            connection.prepareStatement(
                """
                INSERT INTO users(id, normalized_email, password_hash, password_salt,
                                  password_iterations, status, created_at)
                VALUES ('existing', 'existing@example.com', 'hash', 'salt', 600000, 'active', 1)
                """.trimIndent(),
            ).use { it.executeUpdate() }
        }
        ClosetDatabase.open(path.toString())
        DriverManager.getConnection(url).use { connection ->
            connection.createStatement().use { statement ->
                statement.executeQuery("SELECT version FROM schema_migrations ORDER BY version").use { rows ->
                    val versions = buildList { while (rows.next()) add(rows.getInt(1)) }
                    assertEquals(listOf(1, 2), versions)
                }
                statement.executeQuery(
                    "SELECT normalized_email FROM users WHERE id = 'existing'",
                ).use { rows ->
                    check(rows.next())
                    assertEquals("existing@example.com", rows.getString(1))
                }
                statement.executeQuery(
                    "SELECT name FROM sqlite_master WHERE type='table' AND name IN ('media_uploads', 'garments') ORDER BY name",
                ).use { rows ->
                    val names = buildList { while (rows.next()) add(rows.getString(1)) }
                    assertEquals(listOf("garments", "media_uploads"), names)
                }
            }
        }
        assertEquals(first.exposedDatabase.url, ClosetDatabase.open(path.toString()).exposedDatabase.url)
    }
}
