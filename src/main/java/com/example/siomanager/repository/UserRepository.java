package com.example.siomanager.repository;

import com.example.siomanager.data.DatabaseManager;
import com.example.siomanager.model.UserAccount;
import com.example.siomanager.model.UserRole;
import com.example.siomanager.model.AuditEntry;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class UserRepository {
    public record StoredUser(UserAccount account, String passwordHash, String passwordSalt, int iterations) { }

    private final DatabaseManager databaseManager;

    public UserRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public long count() throws SQLException {
        try (Connection connection = databaseManager.openConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM users");
             ResultSet result = statement.executeQuery()) {
            return result.getLong(1);
        }
    }

    public long countActiveAdmins() throws SQLException {
        try (Connection connection = databaseManager.openConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM users WHERE role = 'ADMIN' AND active = 1");
             ResultSet result = statement.executeQuery()) {
            return result.getLong(1);
        }
    }

    public Optional<StoredUser> findStoredByUsername(String username) throws SQLException {
        String sql = """
                SELECT id, username, display_name, role, active, must_change_password, created_at,
                       password_hash, password_salt, password_iterations
                FROM users WHERE username = ? COLLATE NOCASE
                """;
        try (Connection connection = databaseManager.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(toStoredUser(result)) : Optional.empty();
            }
        }
    }

    public List<UserAccount> findAll() throws SQLException {
        String sql = """
                SELECT id, username, display_name, role, active, must_change_password, created_at
                FROM users ORDER BY role, username COLLATE NOCASE
                """;
        List<UserAccount> accounts = new ArrayList<>();
        try (Connection connection = databaseManager.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                accounts.add(toAccount(result));
            }
        }
        return List.copyOf(accounts);
    }

    public UserAccount create(
            String username,
            String displayName,
            UserRole role,
            String passwordHash,
            String passwordSalt,
            int iterations,
            boolean mustChangePassword
    ) throws SQLException {
        Instant now = Instant.now();
        String sql = """
                INSERT INTO users (
                    username, display_name, password_hash, password_salt, password_iterations,
                    role, active, must_change_password, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, 1, ?, ?, ?)
                """;
        try (Connection connection = databaseManager.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, username);
            statement.setString(2, displayName);
            statement.setString(3, passwordHash);
            statement.setString(4, passwordSalt);
            statement.setInt(5, iterations);
            statement.setString(6, role.name());
            statement.setBoolean(7, mustChangePassword);
            statement.setString(8, now.toString());
            statement.setString(9, now.toString());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Aucun identifiant généré pour le compte.");
                }
                return new UserAccount(keys.getLong(1), username, displayName, role, true, mustChangePassword, now);
            }
        }
    }

    public void setActive(long id, boolean active) throws SQLException {
        try (Connection connection = databaseManager.openConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE users SET active = ?, updated_at = ? WHERE id = ?")) {
            statement.setBoolean(1, active);
            statement.setString(2, Instant.now().toString());
            statement.setLong(3, id);
            statement.executeUpdate();
        }
    }

    public void updateAccount(long id, String displayName, UserRole role) throws SQLException {
        try (Connection connection = databaseManager.openConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE users SET display_name = ?, role = ?, updated_at = ? WHERE id = ?")) {
            statement.setString(1, displayName);
            statement.setString(2, role.name());
            statement.setString(3, Instant.now().toString());
            statement.setLong(4, id);
            statement.executeUpdate();
        }
    }

    public void delete(long id) throws SQLException {
        try (Connection connection = databaseManager.openConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM users WHERE id = ?")) {
            statement.setLong(1, id);
            statement.executeUpdate();
        }
    }

    public List<AuditEntry> findAuditEntries(int limit) throws SQLException {
        String sql = """
                SELECT a.id, u.username, a.action, a.details, a.created_at
                FROM audit_log a
                LEFT JOIN users u ON u.id = a.actor_user_id
                ORDER BY a.id DESC LIMIT ?
                """;
        List<AuditEntry> entries = new ArrayList<>();
        try (Connection connection = databaseManager.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, Math.max(1, Math.min(limit, 1_000)));
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    entries.add(new AuditEntry(
                            result.getLong("id"),
                            result.getString("username") == null ? "Système" : result.getString("username"),
                            result.getString("action"),
                            result.getString("details"),
                            Instant.parse(result.getString("created_at"))
                    ));
                }
            }
        }
        return List.copyOf(entries);
    }

    public void updatePassword(long id, String hash, String salt, int iterations, boolean mustChange) throws SQLException {
        String sql = """
                UPDATE users
                SET password_hash = ?, password_salt = ?, password_iterations = ?,
                    must_change_password = ?, updated_at = ?
                WHERE id = ?
                """;
        try (Connection connection = databaseManager.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, hash);
            statement.setString(2, salt);
            statement.setInt(3, iterations);
            statement.setBoolean(4, mustChange);
            statement.setString(5, Instant.now().toString());
            statement.setLong(6, id);
            statement.executeUpdate();
        }
    }

    public void audit(Long actorId, String action, String details) throws SQLException {
        String sql = "INSERT INTO audit_log(actor_user_id, action, details, created_at) VALUES (?, ?, ?, ?)";
        try (Connection connection = databaseManager.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (actorId == null) {
                statement.setNull(1, java.sql.Types.INTEGER);
            } else {
                statement.setLong(1, actorId);
            }
            statement.setString(2, action);
            statement.setString(3, details == null ? "" : details);
            statement.setString(4, Instant.now().toString());
            statement.executeUpdate();
        }
    }

    private StoredUser toStoredUser(ResultSet result) throws SQLException {
        return new StoredUser(
                toAccount(result),
                result.getString("password_hash"),
                result.getString("password_salt"),
                result.getInt("password_iterations")
        );
    }

    private UserAccount toAccount(ResultSet result) throws SQLException {
        return new UserAccount(
                result.getLong("id"),
                result.getString("username"),
                result.getString("display_name"),
                UserRole.valueOf(result.getString("role")),
                result.getBoolean("active"),
                result.getBoolean("must_change_password"),
                Instant.parse(result.getString("created_at"))
        );
    }
}
