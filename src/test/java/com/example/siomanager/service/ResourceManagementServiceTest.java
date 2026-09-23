package com.example.siomanager.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourceManagementServiceTest {
    @TempDir
    Path temporaryDirectory;

    private final ResourceManagementService service = new ResourceManagementService();

    @Test
    void renamesAResourceOnlyInsideTheAllowedRoot() throws Exception {
        Path original = Files.writeString(temporaryDirectory.resolve("cours.md"), "# Cours");

        Path renamed = service.rename(temporaryDirectory, original, "chapitre-01.md");

        assertFalse(Files.exists(original));
        assertTrue(Files.exists(renamed));
        assertThrows(IllegalArgumentException.class, () -> service.rename(
                temporaryDirectory,
                temporaryDirectory,
                "interdit"
        ));
    }

    @Test
    void deletesAFolderRecursivelyWithoutDeletingTheWorkspaceRoot() throws Exception {
        Path project = temporaryDirectory.resolve("projet");
        Files.createDirectories(project.resolve("src"));
        Files.writeString(project.resolve("src/Main.java"), "class Main {}");

        service.delete(temporaryDirectory, project);

        assertFalse(Files.exists(project));
        assertThrows(IllegalArgumentException.class, () -> service.delete(temporaryDirectory, temporaryDirectory));
    }

    @Test
    void importsACompleteDirectoryWithoutOverwritingExistingContent() throws Exception {
        Path destination = temporaryDirectory.resolve("destination");
        Path source = temporaryDirectory.resolve("source/projet-web");
        Files.createDirectories(destination);
        Files.createDirectories(source.resolve("css"));
        Files.writeString(source.resolve("index.html"), "<h1>BTS SIO</h1>");
        Files.writeString(source.resolve("css/style.css"), "body { color: white; }");

        Path imported = service.importDirectory(destination, destination, source);

        assertTrue(Files.exists(imported.resolve("index.html")));
        assertTrue(Files.exists(imported.resolve("css/style.css")));
        assertThrows(IllegalArgumentException.class,
                () -> service.importDirectory(destination, destination, source));
    }
}
