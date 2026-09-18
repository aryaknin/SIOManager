package com.example.siomanager.model;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ResourceNodeTest {
    @Test
    void copiesTheChildrenList() {
        List<ResourceNode> children = new ArrayList<>();
        ResourceNode folder = new ResourceNode(
                "folder",
                "Chapitre 1",
                ResourceType.FOLDER,
                null,
                children
        );

        children.add(new ResourceNode(
                "file",
                "cours.md",
                ResourceType.MARKDOWN,
                Path.of("cours.md"),
                List.of()
        ));

        assertEquals(0, folder.children().size());
    }

    @Test
    void rejectsChildrenInsideAFile() {
        ResourceNode child = new ResourceNode(
                "child",
                "enfant.md",
                ResourceType.MARKDOWN,
                Path.of("enfant.md"),
                List.of()
        );

        assertThrows(IllegalArgumentException.class, () -> new ResourceNode(
                "invalid",
                "cours.md",
                ResourceType.MARKDOWN,
                Path.of("cours.md"),
                List.of(child)
        ));
    }
}
