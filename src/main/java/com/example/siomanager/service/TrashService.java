package com.example.siomanager.service;

import com.example.siomanager.model.ResourceNode;
import com.example.siomanager.model.TrashEntry;
import com.example.siomanager.model.UserAccount;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public final class TrashService {
    private final Path trashRoot;
    private final ResourceCatalogService catalogService;

    public TrashService(Path dataDirectory, ResourceCatalogService catalogService) {
        this.trashRoot = dataDirectory.resolve("trash").toAbsolutePath().normalize();
        this.catalogService = catalogService;
    }

    public TrashEntry moveToTrash(UserAccount actor, ResourceNode resource, Path originalPath) throws IOException {
        Path source = originalPath.toAbsolutePath().normalize();
        Files.createDirectories(trashRoot);
        Path destinationDirectory = trashRoot.resolve(UUID.randomUUID().toString());
        Files.createDirectories(destinationDirectory);
        Path destination = destinationDirectory.resolve(source.getFileName());
        Files.move(source, destination);
        try {
            return catalogService.registerTrash(actor, resource, source, destination);
        } catch (RuntimeException exception) {
            Files.move(destination, source);
            Files.deleteIfExists(destinationDirectory);
            throw exception;
        }
    }

    public List<TrashEntry> list(UserAccount actor) {
        return catalogService.trashEntries(actor);
    }

    public Path restore(UserAccount actor, TrashEntry entry) throws IOException {
        requireAdmin(actor);
        Path source = Path.of(entry.trashPath()).toAbsolutePath().normalize();
        Path destination = Path.of(entry.originalPath()).toAbsolutePath().normalize();
        if (!source.startsWith(trashRoot) || !Files.exists(source)) {
            throw new IllegalArgumentException("Le contenu de cette entrée de corbeille est introuvable.");
        }
        if (Files.exists(destination)) {
            throw new IllegalArgumentException("Une ressource existe déjà à l’emplacement d’origine.");
        }
        Files.createDirectories(destination.getParent());
        Files.move(source, destination);
        Files.deleteIfExists(source.getParent());
        catalogService.removeTrashEntry(entry.id());
        return destination;
    }

    public void purge(UserAccount actor, TrashEntry entry) throws IOException {
        requireAdmin(actor);
        Path target = Path.of(entry.trashPath()).toAbsolutePath().normalize();
        if (!target.startsWith(trashRoot)) {
            throw new IllegalArgumentException("Chemin de corbeille non autorisé.");
        }
        if (Files.isDirectory(target)) {
            try (var paths = Files.walk(target)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                    Files.deleteIfExists(path);
                }
            }
        } else {
            Files.deleteIfExists(target);
        }
        Files.deleteIfExists(target.getParent());
        catalogService.removeTrashEntry(entry.id());
    }

    private void requireAdmin(UserAccount actor) {
        if (actor == null || !actor.isAdmin()) {
            throw new SecurityException("Cette action est réservée aux administrateurs.");
        }
    }
}
