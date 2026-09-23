package com.example.siomanager.service;

import com.example.siomanager.data.DatabaseManager;
import com.example.siomanager.model.ResourceNode;
import com.example.siomanager.model.ResourceType;
import com.example.siomanager.model.UserAccount;
import com.example.siomanager.repository.ResourceCatalogRepository;
import com.example.siomanager.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrashServiceTest {
    @TempDir Path temporaryDirectory;

    @Test
    void movesAndRestoresAResource() throws Exception {
        DatabaseManager database = new DatabaseManager(temporaryDirectory.resolve("data/siomanager.db"));
        database.initialize();
        AccountService accounts = new AccountService(new UserRepository(database));
        UserAccount admin = accounts.createInitialAdministrator(
                "admin", "Administrateur", "mot-de-passe-solide".toCharArray(),
                "mot-de-passe-solide".toCharArray());
        ResourceCatalogService catalog = new ResourceCatalogService(new ResourceCatalogRepository(database));
        TrashService trash = new TrashService(temporaryDirectory.resolve("data"), catalog);
        Path file = Files.writeString(temporaryDirectory.resolve("cours.md"), "# Cours");
        ResourceNode resource = new ResourceNode(
                "common/cejm/cours.md", "cours.md", ResourceType.MARKDOWN, file, List.of());

        var entry = trash.moveToTrash(admin, resource, file);
        assertFalse(Files.exists(file));
        assertFalse(trash.list(admin).isEmpty());

        trash.restore(admin, entry);
        assertTrue(Files.exists(file));
        assertTrue(trash.list(admin).isEmpty());
    }
}
