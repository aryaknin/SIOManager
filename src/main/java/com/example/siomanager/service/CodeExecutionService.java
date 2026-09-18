package com.example.siomanager.service;

import com.example.siomanager.model.ResourceNode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CodeExecutionService implements AutoCloseable {
    private static final Pattern JAVA_PACKAGE = Pattern.compile(
            "(?m)^\\s*package\\s+([A-Za-z_$][\\w$]*(?:\\.[A-Za-z_$][\\w$]*)*)\\s*;"
    );

    private final AtomicBoolean running = new AtomicBoolean();
    private final AtomicBoolean cancellationRequested = new AtomicBoolean();
    private final AtomicReference<Process> activeProcess = new AtomicReference<>();

    public boolean isRunning() {
        return running.get();
    }

    public void execute(
            ResourceNode resource,
            Consumer<String> output,
            Consumer<ExecutionResult> completion
    ) throws IOException {
        ExecutionPlan plan = createPlan(resource);
        if (!running.compareAndSet(false, true)) {
            throw new IllegalStateException("Un programme est déjà en cours d’exécution.");
        }
        cancellationRequested.set(false);

        Thread.ofVirtual().name("code-execution").start(() -> runPlan(plan, output, completion));
    }

    public ExecutionPlan createPlan(ResourceNode resource) throws IOException {
        Path source = resource.localPath();
        if (source == null || !Files.isRegularFile(source)) {
            throw new IOException("Le fichier source est introuvable : " + source);
        }

        String extension = extension(source);
        return switch (extension) {
            case "java" -> javaPlan(resource, source);
            case "sh", "bash" -> shellPlan(source);
            default -> throw new IllegalArgumentException(
                    "L’exécution de ." + extension + " n’est pas encore configurée."
            );
        };
    }

    public void stop() {
        cancellationRequested.set(true);
        Process process = activeProcess.get();
        if (process != null && process.isAlive()) {
            process.destroy();
        }
    }

    private ExecutionPlan javaPlan(ResourceNode resource, Path source) throws IOException {
        String filename = source.getFileName().toString();
        String className = filename.substring(0, filename.lastIndexOf('.'));
        Matcher packageMatcher = JAVA_PACKAGE.matcher(Files.readString(source, StandardCharsets.UTF_8));
        String qualifiedClassName = packageMatcher.find()
                ? packageMatcher.group(1) + "." + className
                : className;

        Path outputDirectory = Path.of(
                System.getProperty("user.dir"),
                "target",
                "siomanager-run",
                resource.id().replaceAll("[^A-Za-z0-9._-]", "_")
        ).toAbsolutePath().normalize();
        Files.createDirectories(outputDirectory);

        List<String> compile = List.of(
                javaTool("javac"),
                "-encoding", "UTF-8",
                "-d", outputDirectory.toString(),
                source.toString()
        );
        List<String> run = List.of(
                javaTool("java"),
                "-cp", outputDirectory.toString(),
                qualifiedClassName
        );
        return new ExecutionPlan(List.of(compile, run), source.getParent(), "Java — " + qualifiedClassName);
    }

    private ExecutionPlan shellPlan(Path source) {
        if (System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win")) {
            throw new IllegalArgumentException(
                    "Les scripts Bash nécessitent Bash ou WSL sous Windows."
            );
        }
        return new ExecutionPlan(
                List.of(List.of("bash", source.toString())),
                source.getParent(),
                "Bash — " + source.getFileName()
        );
    }

    private void runPlan(
            ExecutionPlan plan,
            Consumer<String> output,
            Consumer<ExecutionResult> completion
    ) {
        int exitCode = -1;
        String message = "Exécution interrompue.";
        try {
            output.accept("▶ " + plan.description());
            for (List<String> command : plan.commands()) {
                if (cancellationRequested.get()) {
                    break;
                }

                output.accept("$ " + displayCommand(command));
                Process process = new ProcessBuilder(command)
                        .directory(plan.workingDirectory().toFile())
                        .redirectErrorStream(true)
                        .start();
                activeProcess.set(process);

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                        process.getInputStream(), StandardCharsets.UTF_8
                ))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.accept(line);
                    }
                }

                exitCode = process.waitFor();
                activeProcess.compareAndSet(process, null);
                if (exitCode != 0) {
                    message = "Le processus s’est terminé avec le code " + exitCode + ".";
                    break;
                }
            }

            if (cancellationRequested.get()) {
                message = "Exécution arrêtée par l’utilisateur.";
            } else if (exitCode == 0) {
                message = "Exécution terminée avec succès.";
            }
        } catch (IOException exception) {
            message = "Impossible de démarrer le processus : " + exception.getMessage();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            message = "Le processus a été interrompu.";
        } finally {
            activeProcess.set(null);
            running.set(false);
            completion.accept(new ExecutionResult(exitCode, cancellationRequested.get(), message));
        }
    }

    private String javaTool(String name) {
        String executable = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win")
                ? name + ".exe"
                : name;
        Path bundledTool = Path.of(System.getProperty("java.home"), "bin", executable);
        return Files.isExecutable(bundledTool) ? bundledTool.toString() : executable;
    }

    private String displayCommand(List<String> command) {
        return command.stream()
                .map(part -> part.contains(" ") ? '"' + part + '"' : part)
                .reduce((left, right) -> left + " " + right)
                .orElse("");
    }

    private String extension(Path path) {
        String filename = path.getFileName().toString();
        int dot = filename.lastIndexOf('.');
        return dot < 0 ? "" : filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    @Override
    public void close() {
        stop();
    }

    public record ExecutionPlan(List<List<String>> commands, Path workingDirectory, String description) {
        public ExecutionPlan {
            commands = commands.stream().map(List::copyOf).toList();
            workingDirectory = workingDirectory.toAbsolutePath().normalize();
        }
    }

    public record ExecutionResult(int exitCode, boolean cancelled, String message) {
    }
}
