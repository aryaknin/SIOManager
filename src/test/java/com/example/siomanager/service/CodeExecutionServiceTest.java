package com.example.siomanager.service;

import com.example.siomanager.model.ResourceNode;
import com.example.siomanager.model.ResourceType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CodeExecutionServiceTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void compilesAndRunsASimpleJavaFile() throws Exception {
        Path source = temporaryDirectory.resolve("Hello.java");
        Files.writeString(
                source,
                "public class Hello { public static void main(String[] args) { System.out.println(\"Bonjour SIO\"); } }",
                StandardCharsets.UTF_8
        );
        ResourceNode resource = new ResourceNode(
                "test-hello",
                "Hello.java",
                ResourceType.SOURCE_CODE,
                source,
                List.of()
        );

        StringBuilder output = new StringBuilder();
        CountDownLatch completed = new CountDownLatch(1);
        AtomicReference<CodeExecutionService.ExecutionResult> result = new AtomicReference<>();

        try (CodeExecutionService service = new CodeExecutionService()) {
            service.execute(resource, line -> output.append(line).append('\n'), executionResult -> {
                result.set(executionResult);
                completed.countDown();
            });

            assertTrue(completed.await(20, TimeUnit.SECONDS));
        }

        assertEquals(0, result.get().exitCode());
        assertFalse(result.get().cancelled());
        assertTrue(output.toString().contains("Bonjour SIO"));
    }

    @Test
    void refusesAFileWithoutConfiguredRuntime() throws Exception {
        Path source = temporaryDirectory.resolve("requete.sql");
        Files.writeString(source, "SELECT 1;", StandardCharsets.UTF_8);
        ResourceNode resource = new ResourceNode(
                "test-sql",
                "requete.sql",
                ResourceType.SOURCE_CODE,
                source,
                List.of()
        );

        try (CodeExecutionService service = new CodeExecutionService()) {
            IllegalArgumentException exception = org.junit.jupiter.api.Assertions.assertThrows(
                    IllegalArgumentException.class,
                    () -> service.createPlan(resource)
            );
            assertTrue(exception.getMessage().contains(".sql"));
        }
    }
}
