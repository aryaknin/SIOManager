package com.example.siomanager.repository;

import com.example.siomanager.data.DatabaseManager;
import com.example.siomanager.model.ResourceMetadata;
import com.example.siomanager.model.ResourceType;
import com.example.siomanager.model.TrashEntry;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class ResourceCatalogRepository {
    private final DatabaseManager databaseManager;

    public ResourceCatalogRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public void replaceIndex(String scope, String ownerUsername, Collection<ResourceMetadata> resources)
            throws SQLException {
        String upsertSql = """
                INSERT INTO resource_catalog(
                    resource_key, resource_id, scope, owner_username, relative_path, title,
                    subject, chapter, resource_type, description, checksum, size_bytes,
                    search_text, updated_at, indexed_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(resource_key) DO UPDATE SET
                    resource_id = excluded.resource_id,
                    relative_path = excluded.relative_path,
                    title = excluded.title,
                    subject = excluded.subject,
                    chapter = excluded.chapter,
                    resource_type = excluded.resource_type,
                    checksum = excluded.checksum,
                    size_bytes = excluded.size_bytes,
                    search_text = excluded.search_text,
                    updated_at = excluded.updated_at,
                    indexed_at = excluded.indexed_at
                """;
        try (Connection connection = databaseManager.openConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement upsert = connection.prepareStatement(upsertSql)) {
                for (ResourceMetadata metadata : resources) {
                    bindMetadata(upsert, metadata);
                    upsert.addBatch();
                }
                upsert.executeBatch();
            }

            StringBuilder deleteSql = new StringBuilder(
                    "DELETE FROM resource_catalog WHERE scope = ? AND ");
            deleteSql.append(ownerUsername == null ? "owner_username IS NULL" : "owner_username = ?");
            if (!resources.isEmpty()) {
                deleteSql.append(" AND resource_key NOT IN (");
                deleteSql.append("?,".repeat(resources.size()));
                deleteSql.setLength(deleteSql.length() - 1);
                deleteSql.append(')');
            }
            try (PreparedStatement delete = connection.prepareStatement(deleteSql.toString())) {
                int parameter = 1;
                delete.setString(parameter++, scope);
                if (ownerUsername != null) {
                    delete.setString(parameter++, ownerUsername);
                }
                for (ResourceMetadata metadata : resources) {
                    delete.setString(parameter++, metadata.resourceKey());
                }
                delete.executeUpdate();
            }
            connection.commit();
        }
    }

    public List<ResourceMetadata> search(String username, String query, int limit) throws SQLException {
        String pattern = "%" + query.toLowerCase() + "%";
        String sql = """
                SELECT * FROM resource_catalog
                WHERE (scope = 'SHARED' OR (scope = 'PERSONAL' AND owner_username = ?))
                  AND (lower(title) LIKE ? OR lower(relative_path) LIKE ? OR lower(subject) LIKE ?
                       OR lower(chapter) LIKE ? OR lower(description) LIKE ? OR lower(search_text) LIKE ?)
                ORDER BY title COLLATE NOCASE LIMIT ?
                """;
        try (Connection connection = databaseManager.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            for (int index = 2; index <= 7; index++) {
                statement.setString(index, pattern);
            }
            statement.setInt(8, limit);
            return readMetadata(statement);
        }
    }

    public void recordOpened(long userId, String resourceKey) throws SQLException {
        String sql = """
                INSERT INTO user_resource_state(user_id, resource_key, favorite, progress, last_opened_at)
                VALUES (?, ?, 0, 10, ?)
                ON CONFLICT(user_id, resource_key) DO UPDATE SET
                    last_opened_at = excluded.last_opened_at,
                    progress = CASE WHEN user_resource_state.progress = 0 THEN 10 ELSE user_resource_state.progress END
                """;
        try (Connection connection = databaseManager.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            statement.setString(2, resourceKey);
            statement.setString(3, Instant.now().toString());
            statement.executeUpdate();
        }
    }

    public boolean toggleFavorite(long userId, String resourceKey) throws SQLException {
        String insert = """
                INSERT INTO user_resource_state(user_id, resource_key, favorite, progress)
                VALUES (?, ?, 1, 0)
                ON CONFLICT(user_id, resource_key) DO UPDATE SET
                    favorite = CASE user_resource_state.favorite WHEN 1 THEN 0 ELSE 1 END
                """;
        try (Connection connection = databaseManager.openConnection();
             PreparedStatement statement = connection.prepareStatement(insert)) {
            statement.setLong(1, userId);
            statement.setString(2, resourceKey);
            statement.executeUpdate();
        }
        try (Connection connection = databaseManager.openConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT favorite FROM user_resource_state WHERE user_id = ? AND resource_key = ?")) {
            statement.setLong(1, userId);
            statement.setString(2, resourceKey);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() && result.getBoolean(1);
            }
        }
    }

    public void setProgress(long userId, String resourceKey, int progress) throws SQLException {
        String sql = """
                INSERT INTO user_resource_state(user_id, resource_key, favorite, progress)
                VALUES (?, ?, 0, ?)
                ON CONFLICT(user_id, resource_key) DO UPDATE SET progress = excluded.progress
                """;
        try (Connection connection = databaseManager.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            statement.setString(2, resourceKey);
            statement.setInt(3, Math.max(0, Math.min(100, progress)));
            statement.executeUpdate();
        }
    }

    public List<ResourceMetadata> findFavorites(long userId, int limit) throws SQLException {
        return findByState(userId, "s.favorite = 1", "r.title COLLATE NOCASE", limit);
    }

    public List<ResourceMetadata> findRecent(long userId, int limit) throws SQLException {
        return findByState(userId, "s.last_opened_at IS NOT NULL", "s.last_opened_at DESC", limit);
    }

    public int countCompleted(long userId) throws SQLException {
        try (Connection connection = databaseManager.openConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM user_resource_state WHERE user_id = ? AND progress = 100")) {
            statement.setLong(1, userId);
            try (ResultSet result = statement.executeQuery()) {
                return result.getInt(1);
            }
        }
    }

    public TrashEntry addTrashEntry(
            String resourceName,
            String originalPath,
            String trashPath,
            String scope,
            String ownerUsername,
            String deletedBy
    ) throws SQLException {
        Instant now = Instant.now();
        String sql = """
                INSERT INTO trash_entries(resource_name, original_path, trash_path, scope, owner_username, deleted_by, deleted_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = databaseManager.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, resourceName);
            statement.setString(2, originalPath);
            statement.setString(3, trashPath);
            statement.setString(4, scope);
            statement.setString(5, ownerUsername);
            statement.setString(6, deletedBy);
            statement.setString(7, now.toString());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                return new TrashEntry(keys.getLong(1), resourceName, originalPath, trashPath,
                        scope, ownerUsername, deletedBy, now);
            }
        }
    }

    public List<TrashEntry> findTrashEntries() throws SQLException {
        List<TrashEntry> entries = new ArrayList<>();
        try (Connection connection = databaseManager.openConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT * FROM trash_entries ORDER BY deleted_at DESC");
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                entries.add(new TrashEntry(
                        result.getLong("id"), result.getString("resource_name"),
                        result.getString("original_path"), result.getString("trash_path"),
                        result.getString("scope"), result.getString("owner_username"),
                        result.getString("deleted_by"), Instant.parse(result.getString("deleted_at"))
                ));
            }
        }
        return List.copyOf(entries);
    }

    public void removeTrashEntry(long id) throws SQLException {
        try (Connection connection = databaseManager.openConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM trash_entries WHERE id = ?")) {
            statement.setLong(1, id);
            statement.executeUpdate();
        }
    }

    private List<ResourceMetadata> findByState(long userId, String condition, String order, int limit)
            throws SQLException {
        String sql = "SELECT r.* FROM resource_catalog r JOIN user_resource_state s ON s.resource_key = r.resource_key "
                + "WHERE s.user_id = ? AND " + condition + " ORDER BY " + order + " LIMIT ?";
        try (Connection connection = databaseManager.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            statement.setInt(2, limit);
            return readMetadata(statement);
        }
    }

    private List<ResourceMetadata> readMetadata(PreparedStatement statement) throws SQLException {
        List<ResourceMetadata> resources = new ArrayList<>();
        try (ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                resources.add(toMetadata(result));
            }
        }
        return List.copyOf(resources);
    }

    private ResourceMetadata toMetadata(ResultSet result) throws SQLException {
        return new ResourceMetadata(
                result.getString("resource_key"), result.getString("resource_id"),
                result.getString("scope"), result.getString("owner_username"),
                result.getString("relative_path"), result.getString("title"),
                result.getString("subject"), result.getString("chapter"),
                ResourceType.valueOf(result.getString("resource_type")),
                result.getString("description"), result.getString("search_text"), result.getString("checksum"),
                result.getLong("size_bytes"), Instant.parse(result.getString("updated_at"))
        );
    }

    private void bindMetadata(PreparedStatement statement, ResourceMetadata metadata) throws SQLException {
        statement.setString(1, metadata.resourceKey());
        statement.setString(2, metadata.resourceId());
        statement.setString(3, metadata.scope());
        statement.setString(4, metadata.ownerUsername());
        statement.setString(5, metadata.relativePath());
        statement.setString(6, metadata.title());
        statement.setString(7, metadata.subject());
        statement.setString(8, metadata.chapter());
        statement.setString(9, metadata.resourceType().name());
        statement.setString(10, metadata.description());
        statement.setString(11, metadata.checksum());
        statement.setLong(12, metadata.sizeBytes());
        statement.setString(13, metadata.searchText());
        statement.setString(14, metadata.updatedAt().toString());
        statement.setString(15, Instant.now().toString());
    }
}
