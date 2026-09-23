package com.example.siomanager.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.nio.file.StandardCopyOption;

public final class ResourceManagementService {
    public Path rename(Path allowedRoot, Path target, String requestedName) throws IOException {
        Path root = allowedRoot.toAbsolutePath().normalize();
        Path normalizedTarget = validateTarget(root, target);
        String name = validateName(requestedName);
        Path renamed = normalizedTarget.resolveSibling(name).normalize();
        if (!renamed.getParent().equals(normalizedTarget.getParent()) || !renamed.startsWith(root)) {
            throw new IllegalArgumentException("Le nouveau nom n’est pas autorisé.");
        }
        return Files.move(normalizedTarget, renamed);
    }

    public void delete(Path allowedRoot, Path target) throws IOException {
        Path root = allowedRoot.toAbsolutePath().normalize();
        Path normalizedTarget = validateTarget(root, target);
        if (Files.isDirectory(normalizedTarget)) {
            try (var paths = Files.walk(normalizedTarget)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                    Files.delete(path);
                }
            }
        } else {
            Files.delete(normalizedTarget);
        }
    }

    public Path importDirectory(Path allowedRoot, Path destinationDirectory, Path sourceDirectory)
            throws IOException {
        Path root = allowedRoot.toAbsolutePath().normalize();
        Path destination = destinationDirectory.toAbsolutePath().normalize();
        Path source = sourceDirectory.toAbsolutePath().normalize();
        if (!destination.startsWith(root) || !Files.isDirectory(destination) || !Files.isDirectory(source)) {
            throw new IllegalArgumentException("Le dossier source ou la destination n’est pas valide.");
        }
        Path importedRoot = destination.resolve(source.getFileName()).normalize();
        if (!importedRoot.startsWith(root) || Files.exists(importedRoot)) {
            throw new IllegalArgumentException("Un dossier portant ce nom existe déjà dans la destination.");
        }
        try (var paths = Files.walk(source)) {
            for (Path path : paths.toList()) {
                Path target = importedRoot.resolve(source.relativize(path)).normalize();
                if (!target.startsWith(importedRoot)) {
                    throw new IllegalArgumentException("Le dossier contient un chemin non autorisé.");
                }
                if (Files.isDirectory(path)) {
                    Files.createDirectories(target);
                } else if (!Files.isSymbolicLink(path)) {
                    Files.copy(path, target, StandardCopyOption.COPY_ATTRIBUTES);
                }
            }
        } catch (IOException | RuntimeException exception) {
            if (Files.exists(importedRoot)) {
                delete(root, importedRoot);
            }
            throw exception;
        }
        return importedRoot;
    }

    private Path validateTarget(Path root, Path target) {
        Path normalizedTarget = target.toAbsolutePath().normalize();
        if (normalizedTarget.equals(root) || !normalizedTarget.startsWith(root) || !Files.exists(normalizedTarget)) {
            throw new IllegalArgumentException("La ressource ciblée n’est pas modifiable.");
        }
        return normalizedTarget;
    }

    private String validateName(String requestedName) {
        if (requestedName == null || requestedName.isBlank()) {
            throw new IllegalArgumentException("Le nom est obligatoire.");
        }
        String name = requestedName.trim();
        if (name.equals(".") || name.equals("..") || name.endsWith(".") || name.endsWith(" ")
                || name.matches(".*[\\\\/:*?\"<>|].*")) {
            throw new IllegalArgumentException("Le nom contient un caractère interdit sous Linux ou Windows.");
        }
        return name;
    }
}
