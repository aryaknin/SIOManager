package com.example.siomanager.data;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class DatabaseManager {
    private static final String DATA_DIRECTORY_PROPERTY = "siomanager.dataDir";

    private final Path databasePath;

    public DatabaseManager(Path databasePath) {
        this.databasePath = databasePath.toAbsolutePath().normalize();
    }

    public static DatabaseManager forCurrentUser() {
        String configuredDirectory = System.getProperty(DATA_DIRECTORY_PROPERTY);
        Path dataDirectory = configuredDirectory == null || configuredDirectory.isBlank()
                ? Path.of(System.getProperty("user.home"), ".siomanager")
                : Path.of(configuredDirectory);
        return new DatabaseManager(dataDirectory.resolve("siomanager.db"));
    }

    public Path databasePath() {
        return databasePath;
    }

    public void initialize() throws IOException, SQLException {
        Files.createDirectories(databasePath.getParent());
        try (Connection connection = openConnection(); Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA journal_mode = WAL");
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS users (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        username TEXT NOT NULL COLLATE NOCASE UNIQUE,
                        display_name TEXT NOT NULL,
                        password_hash TEXT NOT NULL,
                        password_salt TEXT NOT NULL,
                        password_iterations INTEGER NOT NULL,
                        role TEXT NOT NULL CHECK (role IN ('ADMIN', 'ELEVE')),
                        active INTEGER NOT NULL DEFAULT 1 CHECK (active IN (0, 1)),
                        must_change_password INTEGER NOT NULL DEFAULT 0 CHECK (must_change_password IN (0, 1)),
                        created_at TEXT NOT NULL,
                        updated_at TEXT NOT NULL
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS audit_log (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        actor_user_id INTEGER,
                        action TEXT NOT NULL,
                        details TEXT NOT NULL DEFAULT '',
                        created_at TEXT NOT NULL,
                        FOREIGN KEY (actor_user_id) REFERENCES users(id) ON DELETE SET NULL
                    )
                    """);
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_audit_created_at ON audit_log(created_at)");
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS resource_catalog (
                        resource_key TEXT PRIMARY KEY,
                        resource_id TEXT NOT NULL,
                        scope TEXT NOT NULL CHECK (scope IN ('SHARED', 'PERSONAL')),
                        owner_username TEXT,
                        relative_path TEXT NOT NULL,
                        title TEXT NOT NULL,
                        subject TEXT NOT NULL DEFAULT '',
                        chapter TEXT NOT NULL DEFAULT '',
                        resource_type TEXT NOT NULL,
                        description TEXT NOT NULL DEFAULT '',
                        checksum TEXT NOT NULL DEFAULT '',
                        size_bytes INTEGER NOT NULL DEFAULT 0,
                        search_text TEXT NOT NULL DEFAULT '',
                        updated_at TEXT NOT NULL,
                        indexed_at TEXT NOT NULL
                    )
                    """);
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_resource_catalog_title ON resource_catalog(title)");
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_resource_catalog_scope ON resource_catalog(scope, owner_username)");
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS user_resource_state (
                        user_id INTEGER NOT NULL,
                        resource_key TEXT NOT NULL,
                        favorite INTEGER NOT NULL DEFAULT 0 CHECK (favorite IN (0, 1)),
                        progress INTEGER NOT NULL DEFAULT 0 CHECK (progress BETWEEN 0 AND 100),
                        last_opened_at TEXT,
                        PRIMARY KEY (user_id, resource_key),
                        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                        FOREIGN KEY (resource_key) REFERENCES resource_catalog(resource_key) ON DELETE CASCADE
                    )
                    """);
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_resource_state_recent ON user_resource_state(user_id, last_opened_at)");
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS trash_entries (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        resource_name TEXT NOT NULL,
                        original_path TEXT NOT NULL,
                        trash_path TEXT NOT NULL UNIQUE,
                        scope TEXT NOT NULL,
                        owner_username TEXT,
                        deleted_by TEXT NOT NULL,
                        deleted_at TEXT NOT NULL
                    )
                    """);
            statement.executeUpdate("PRAGMA user_version = 2");
        }
    }

    public Connection openConnection() throws SQLException {
        Connection connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            statement.execute("PRAGMA busy_timeout = 5000");
        }
        return connection;
    }

    public void checkpoint() throws SQLException {
        try (Connection connection = openConnection(); Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA wal_checkpoint(FULL)");
        }
    }
}
