package com.example.siomanager.service;

import com.example.siomanager.model.ResourceMetadata;
import com.example.siomanager.model.ResourceNode;
import com.example.siomanager.model.ResourceType;
import com.example.siomanager.model.TrashEntry;
import com.example.siomanager.model.UserAccount;
import com.example.siomanager.repository.ResourceCatalogRepository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

public final class ResourceCatalogService {
    public record DashboardData(
            List<ResourceMetadata> favorites,
            List<ResourceMetadata> recent,
            int completedCount
    ) { }

    private static final long MAX_SEARCHABLE_TEXT_BYTES = 1_000_000;
    private final ResourceCatalogRepository repository;

    public ResourceCatalogService(ResourceCatalogRepository repository) {
        this.repository = repository;
    }

    public void indexShared(ResourceNode root, Path contentRoot) {
        replaceIndex(root, contentRoot, "SHARED", null);
    }

    public void indexPersonal(ResourceNode root, Path workspaceRoot, String username) {
        replaceIndex(root, workspaceRoot, "PERSONAL", username);
    }

    public List<ResourceMetadata> search(UserAccount account, String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        try {
            return repository.search(account.username(), query.trim(), 200);
        } catch (SQLException exception) {
            throw databaseFailure(exception);
        }
    }

    public void recordOpened(UserAccount account, ResourceNode resource) {
        try {
            repository.recordOpened(account.id(), keyFor(account, resource));
        } catch (SQLException exception) {
            throw databaseFailure(exception);
        }
    }

    public boolean toggleFavorite(UserAccount account, ResourceNode resource) {
        try {
            return repository.toggleFavorite(account.id(), keyFor(account, resource));
        } catch (SQLException exception) {
            throw databaseFailure(exception);
        }
    }

    public void markComplete(UserAccount account, ResourceNode resource) {
        try {
            repository.setProgress(account.id(), keyFor(account, resource), 100);
        } catch (SQLException exception) {
            throw databaseFailure(exception);
        }
    }

    public DashboardData dashboard(UserAccount account) {
        try {
            return new DashboardData(
                    repository.findFavorites(account.id(), 8),
                    repository.findRecent(account.id(), 8),
                    repository.countCompleted(account.id())
            );
        } catch (SQLException exception) {
            throw databaseFailure(exception);
        }
    }

    public List<TrashEntry> trashEntries(UserAccount actor) {
        if (!actor.isAdmin()) {
            throw new SecurityException("La corbeille partagée est réservée aux administrateurs.");
        }
        try {
            return repository.findTrashEntries();
        } catch (SQLException exception) {
            throw databaseFailure(exception);
        }
    }

    public TrashEntry registerTrash(
            UserAccount actor,
            ResourceNode resource,
            Path originalPath,
            Path trashPath
    ) {
        try {
            return repository.addTrashEntry(
                    resource.name(), originalPath.toString(), trashPath.toString(),
                    resource.id().startsWith("personal") ? "PERSONAL" : "SHARED",
                    resource.id().startsWith("personal") ? actor.username() : null,
                    actor.username()
            );
        } catch (SQLException exception) {
            throw databaseFailure(exception);
        }
    }

    public void removeTrashEntry(long id) {
        try {
            repository.removeTrashEntry(id);
        } catch (SQLException exception) {
            throw databaseFailure(exception);
        }
    }

    public String keyFor(UserAccount account, ResourceNode resource) {
        return resource.id().startsWith("personal")
                ? "personal:" + account.username() + ":" + resource.id()
                : "shared:" + resource.id();
    }

    private void replaceIndex(ResourceNode root, Path storageRoot, String scope, String ownerUsername) {
        List<ResourceMetadata> resources = new ArrayList<>();
        collect(root, storageRoot, scope, ownerUsername, resources);
        try {
            repository.replaceIndex(scope, ownerUsername, resources);
        } catch (SQLException exception) {
            throw databaseFailure(exception);
        }
    }

    private void collect(
            ResourceNode node,
            Path storageRoot,
            String scope,
            String ownerUsername,
            List<ResourceMetadata> resources
    ) {
        if (!node.isContainer() && node.localPath() != null) {
            try {
                Path path = node.localPath().toAbsolutePath().normalize();
                String relativePath = storageRoot.toAbsolutePath().normalize().relativize(path).toString();
                String searchableText = readSearchableText(node, path);
                resources.add(new ResourceMetadata(
                        scope.equals("PERSONAL")
                                ? "personal:" + ownerUsername + ":" + node.id()
                                : "shared:" + node.id(),
                        node.id(), scope, ownerUsername, relativePath, titleFor(node.name()),
                        subjectFor(node.id(), scope), chapterFor(node.id()), node.type(),
                        "", searchableText, checksum(path), Files.size(path),
                        Files.getLastModifiedTime(path).toInstant()
                ));
            } catch (IOException exception) {
                throw new IllegalStateException("Impossible d’indexer « " + node.name() + " ».", exception);
            }
        }
        node.children().forEach(child -> collect(child, storageRoot, scope, ownerUsername, resources));
    }

    private String readSearchableText(ResourceNode node, Path path) throws IOException {
        if ((node.type() != ResourceType.MARKDOWN && node.type() != ResourceType.SOURCE_CODE)
                || Files.size(path) > MAX_SEARCHABLE_TEXT_BYTES) {
            return "";
        }
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    private String checksum(Path path) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (var input = Files.newInputStream(path)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) >= 0) {
                    digest.update(buffer, 0, read);
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 est indisponible.", exception);
        }
    }

    private String titleFor(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(0, dot) : filename;
    }

    private String subjectFor(String id, String scope) {
        if (scope.equals("PERSONAL")) {
            return "Mon espace";
        }
        String[] parts = id.split("/");
        return parts.length > 1 ? prettify(parts[1]) : prettify(parts[0]);
    }

    private String chapterFor(String id) {
        String[] parts = id.split("/");
        return parts.length > 2 ? prettify(parts[parts.length - 2]) : "";
    }

    private String prettify(String value) {
        String text = value.replace('-', ' ').replace('_', ' ').toLowerCase(Locale.FRENCH);
        return text.isBlank() ? "" : Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    private IllegalStateException databaseFailure(SQLException exception) {
        return new IllegalStateException("Le catalogue local est indisponible.", exception);
    }
}
