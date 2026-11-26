package dev.digitalcloset.server.db

import org.jetbrains.exposed.v1.jdbc.Database
import java.nio.file.Files
import java.nio.file.Path
import java.sql.DriverManager
import org.sqlite.SQLiteConfig

internal class ClosetDatabase private constructor(
    val exposedDatabase: Database,
) {
    companion object {
        private val migrations = listOf(
            1 to "/db/migration/V1__auth_profile.sql",
            2 to "/db/migration/V2__media_garments.sql",
        )

        fun open(path: String): ClosetDatabase {
            Class.forName("org.sqlite.JDBC")
            val databasePath = Path.of(path).toAbsolutePath()
            databasePath.parent?.let(Files::createDirectories)
            val jdbcUrl = "jdbc:sqlite:$databasePath"
            val connectionProperties = SQLiteConfig().apply { enforceForeignKeys(true) }.toProperties()
            runMigrations(jdbcUrl, connectionProperties)
            return ClosetDatabase(
                Database.connect(getNewConnection = { DriverManager.getConnection(jdbcUrl, connectionProperties) }),
            )
        }

        private fun runMigrations(jdbcUrl: String, connectionProperties: java.util.Properties) {
            DriverManager.getConnection(jdbcUrl, connectionProperties).use { connection ->
                connection.createStatement().use { statement ->
                    statement.execute(
                        """
                        CREATE TABLE IF NOT EXISTS schema_migrations (
                            version INTEGER PRIMARY KEY,
                            applied_at INTEGER NOT NULL
                        )
                        """.trimIndent(),
                    )
                }
                val appliedVersions = connection.createStatement().use { statement ->
                    statement.executeQuery("SELECT version FROM schema_migrations").use { rows ->
                        buildSet { while (rows.next()) add(rows.getInt(1)) }
                    }
                }
                for ((version, resource) in migrations) {
                    if (version in appliedVersions) continue
                    val migration = ClosetDatabase::class.java.getResourceAsStream(resource)
                        ?.bufferedReader()
                        ?.use { it.readText() }
                        ?: error("Database migration resource is missing: $resource")
                    connection.autoCommit = false
                    try {
                        migration.split(';')
                            .map(String::trim)
                            .filter(String::isNotEmpty)
                            .forEach { sql ->
                                connection.createStatement().use { it.execute(sql) }
                            }
                        connection.prepareStatement(
                            "INSERT INTO schema_migrations(version, applied_at) VALUES (?, ?)",
                        ).use { statement ->
                            statement.setInt(1, version)
                            statement.setLong(2, System.currentTimeMillis())
                            statement.executeUpdate()
                        }
                        connection.commit()
                    } catch (failure: Exception) {
                        connection.rollback()
                        throw failure
                    } finally {
                        connection.autoCommit = true
                    }
                }
            }
        }
    }
}
