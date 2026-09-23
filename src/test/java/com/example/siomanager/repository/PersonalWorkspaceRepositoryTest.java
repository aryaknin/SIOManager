package com.example.siomanager.repository;

import com.example.siomanager.model.ResourceNode;
import com.example.siomanager.model.ResourceType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PersonalWorkspaceRepositoryTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void createsAndScansAnIsolatedPersonalWorkspace() throws Exception {
        Path workspace = temporaryDirectory.resolve("workspaces/eleve.1");
        Files.createDirectories(workspace.resolve("projet-java"));
        Files.writeString(workspace.resolve("projet-java/Main.java"), "class Main {}");

        PersonalWorkspaceRepository repository = new PersonalWorkspaceRepository(workspace);
        ResourceNode root = repository.loadTree();

        assertEquals("personal", root.id());
        assertEquals(workspace.toAbsolutePath(), root.localPath());
        assertTrue(Files.isDirectory(repository.workspaceRoot()));
        ResourceNode folder = root.children().getFirst();
        assertEquals(ResourceType.FOLDER, folder.type());
        assertEquals("personal/projet-java", folder.id());
        assertEquals(ResourceType.SOURCE_CODE, folder.children().getFirst().type());
    }
}
