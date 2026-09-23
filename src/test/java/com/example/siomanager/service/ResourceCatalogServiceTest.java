package com.example.siomanager.service;

import com.example.siomanager.data.DatabaseManager;
import com.example.siomanager.model.ResourceNode;
import com.example.siomanager.model.UserAccount;
import com.example.siomanager.repository.LocalResourceRepository;
import com.example.siomanager.repository.ResourceCatalogRepository;
import com.example.siomanager.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourceCatalogServiceTest {
    @TempDir Path temporaryDirectory;

    @Test
    void indexesFullTextAndPersistsDashboardState() throws Exception {
        DatabaseManager database = new DatabaseManager(temporaryDirectory.resolve("data/siomanager.db"));
        database.initialize();
        AccountService accounts = new AccountService(new UserRepository(database));
        UserAccount admin = accounts.createInitialAdministrator(
                "admin", "Administrateur", "mot-de-passe-solide".toCharArray(),
                "mot-de-passe-solide".toCharArray());

        Path content = temporaryDirectory.resolve("content");
        Path lesson = content.resolve("common/cejm/chapitre-01/cours.md");
        Files.createDirectories(lesson.getParent());
        Files.writeString(lesson, "# Le contrat numérique\nUne obligation entre deux parties.");
        LocalResourceRepository localRepository = new LocalResourceRepository(content);
        ResourceNode tree = localRepository.loadTree();
        ResourceCatalogService catalog = new ResourceCatalogService(new ResourceCatalogRepository(database));

        catalog.indexShared(tree, content);
        var results = catalog.search(admin, "obligation entre deux parties");

        assertEquals(1, results.size());
        ResourceNode resource = find(tree, results.getFirst().resourceId());
        catalog.recordOpened(admin, resource);
        assertTrue(catalog.toggleFavorite(admin, resource));
        catalog.markComplete(admin, resource);
        var dashboard = catalog.dashboard(admin);
        assertFalse(dashboard.favorites().isEmpty());
        assertFalse(dashboard.recent().isEmpty());
        assertEquals(1, dashboard.completedCount());
    }

    private ResourceNode find(ResourceNode node, String id) {
        if (node.id().equals(id)) return node;
        for (ResourceNode child : node.children()) {
            ResourceNode found = findOrNull(child, id);
            if (found != null) return found;
        }
        throw new AssertionError("Ressource introuvable : " + id);
    }

    private ResourceNode findOrNull(ResourceNode node, String id) {
        if (node.id().equals(id)) return node;
        for (ResourceNode child : node.children()) {
            ResourceNode found = findOrNull(child, id);
            if (found != null) return found;
        }
        return null;
    }
}
