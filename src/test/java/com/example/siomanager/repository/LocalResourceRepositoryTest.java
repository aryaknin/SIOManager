package com.example.siomanager.repository;

import com.example.siomanager.model.ResourceNode;
import com.example.siomanager.model.ResourceType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalResourceRepositoryTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void discoversFoldersAndSupportedFilesRecursively() throws Exception {
        Path chapter = temporaryDirectory.resolve("common/cejm/chapitre-03");
        Files.createDirectories(chapter);
        Files.writeString(chapter.resolve("cours.md"), "# Contrats", StandardCharsets.UTF_8);
        Files.writeString(chapter.resolve("exemple.java"), "class Exemple {}", StandardCharsets.UTF_8);
        Files.write(chapter.resolve("archive.bin"), new byte[]{1, 2, 3});

        LocalResourceRepository repository = new LocalResourceRepository(temporaryDirectory);
        ResourceNode root = repository.loadTree();

        ResourceNode common = find(root, "common");
        ResourceNode cejm = find(root, "common/cejm");
        ResourceNode chapterNode = find(root, "common/cejm/chapitre-03");

        assertEquals("Enseignements communs", common.name());
        assertEquals(ResourceType.SUBJECT, cejm.type());
        assertEquals("Chapitre 3", chapterNode.name());
        assertEquals(ResourceType.MARKDOWN, find(root, "common/cejm/chapitre-03/cours.md").type());
        assertEquals(ResourceType.SOURCE_CODE, find(root, "common/cejm/chapitre-03/exemple.java").type());
        assertEquals(ResourceType.OTHER, find(root, "common/cejm/chapitre-03/archive.bin").type());
    }

    @Test
    void createsTheThreeBaseSectionsWhenTheyAreMissing() throws Exception {
        LocalResourceRepository repository = new LocalResourceRepository(temporaryDirectory);

        ResourceNode root = repository.loadTree();

        assertNotNull(find(root, "common"));
        assertNotNull(find(root, "sisr"));
        assertNotNull(find(root, "slam"));
        assertTrue(Files.isDirectory(temporaryDirectory.resolve("common")));
    }

    private ResourceNode find(ResourceNode node, String id) {
        if (node.id().equals(id)) {
            return node;
        }
        return node.children().stream()
                .map(child -> findOrNull(child, id))
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Ressource introuvable : " + id));
    }

    private ResourceNode findOrNull(ResourceNode node, String id) {
        if (node.id().equals(id)) {
            return node;
        }
        for (ResourceNode child : node.children()) {
            ResourceNode found = findOrNull(child, id);
            if (found != null) {
                return found;
            }
        }
        return null;
    }
}
