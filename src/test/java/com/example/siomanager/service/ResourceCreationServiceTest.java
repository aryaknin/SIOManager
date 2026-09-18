package com.example.siomanager.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.example.siomanager.service.ResourceCreationService.ResourceKind.FOLDER;
import static com.example.siomanager.service.ResourceCreationService.ResourceKind.JAVA;
import static com.example.siomanager.service.ResourceCreationService.ResourceKind.MARKDOWN;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourceCreationServiceTest {
    @TempDir
    Path temporaryDirectory;

    private final ResourceCreationService service = new ResourceCreationService();

    @Test
    void createsFoldersAndFilesWithTemplates() throws Exception {
        Path subject = Files.createDirectories(temporaryDirectory.resolve("slam/java"));
        Path chapter = service.create(temporaryDirectory, subject, "chapitre-02", FOLDER);
        Path markdown = service.create(temporaryDirectory, chapter, "cours", MARKDOWN);
        Path java = service.create(temporaryDirectory, chapter, "Exemple", JAVA);

        assertTrue(Files.isDirectory(chapter));
        assertTrue(markdown.getFileName().toString().equals("cours.md"));
        assertTrue(Files.readString(markdown, StandardCharsets.UTF_8).startsWith("# Cours"));
        assertTrue(Files.readString(java, StandardCharsets.UTF_8).contains("public class Exemple"));
    }

    @Test
    void rejectsTraversalInvalidJavaNamesAndDuplicates() throws Exception {
        Path subject = Files.createDirectories(temporaryDirectory.resolve("slam/java"));
        service.create(temporaryDirectory, subject, "cours", MARKDOWN);

        assertThrows(IllegalArgumentException.class,
                () -> service.create(temporaryDirectory, subject, "../secret", FOLDER));
        assertThrows(IllegalArgumentException.class,
                () -> service.create(temporaryDirectory, subject, "ma-classe", JAVA));
        assertThrows(java.nio.file.FileAlreadyExistsException.class,
                () -> service.create(temporaryDirectory, subject, "cours", MARKDOWN));
    }
}
