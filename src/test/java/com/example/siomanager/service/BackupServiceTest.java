package com.example.siomanager.service;

import com.example.siomanager.data.DatabaseManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BackupServiceTest {
    @TempDir Path temporaryDirectory;

    @Test
    void backsUpAndRestoresDatabaseResourcesAndWorkspaces() throws Exception {
        Path dataDirectory = temporaryDirectory.resolve("data");
        Path sharedResources = temporaryDirectory.resolve("shared");
        DatabaseManager database = new DatabaseManager(dataDirectory.resolve("siomanager.db"));
        database.initialize();
        Files.createDirectories(sharedResources);
        Path lesson = Files.writeString(sharedResources.resolve("cours.md"), "version originale");
        Path workspaceFile = dataDirectory.resolve("workspaces/eleve/Main.java");
        Files.createDirectories(workspaceFile.getParent());
        Files.writeString(workspaceFile, "class Main {}");
        BackupService service = new BackupService(database, sharedResources);

        Path archive = service.createBackup(temporaryDirectory.resolve("backups"));
        assertTrue(Files.isRegularFile(archive));
        Files.writeString(lesson, "version modifiée");

        service.restoreBackup(archive);

        assertEquals("version originale", Files.readString(lesson));
        assertTrue(Files.exists(workspaceFile));
    }
}
