package com.example.siomanager.service;

import com.example.siomanager.model.ResourceNode;
import com.example.siomanager.model.ResourceType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalFileServiceTest {
    private final LocalFileService fileService = new LocalFileService();

    @TempDir
    Path temporaryDirectory;

    @Test
    void readsAndWritesUtf8Content() throws Exception {
        Path file = temporaryDirectory.resolve("cours.md");
        Files.writeString(file, "# Première version", StandardCharsets.UTF_8);
        ResourceNode resource = textResource(file);

        assertEquals("# Première version", fileService.readText(resource));

        fileService.saveText(resource, "# Version enregistrée\n\nÉlève en BTS SIO");

        assertEquals(
                "# Version enregistrée\n\nÉlève en BTS SIO",
                Files.readString(file, StandardCharsets.UTF_8)
        );
    }

    @Test
    void createsMissingParentDirectoriesWhenSaving() throws Exception {
        Path file = temporaryDirectory.resolve("chapitres/chapitre-01/cours.md");

        fileService.saveText(textResource(file), "# Nouveau cours");

        assertTrue(Files.exists(file));
        assertEquals("# Nouveau cours", Files.readString(file, StandardCharsets.UTF_8));
    }

    private ResourceNode textResource(Path path) {
        return new ResourceNode(
                "test-resource",
                "cours.md",
                ResourceType.MARKDOWN,
                path,
                List.of()
        );
    }
}
