package com.example.siomanager.service;

import com.example.siomanager.data.DatabaseManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public final class BackupService {
    private static final String DATABASE_ENTRY = "database/siomanager.db";
    private final DatabaseManager databaseManager;
    private final Path sharedResourcesRoot;
    private final Path dataDirectory;

    public BackupService(DatabaseManager databaseManager, Path sharedResourcesRoot) {
        this.databaseManager = databaseManager;
        this.sharedResourcesRoot = sharedResourcesRoot.toAbsolutePath().normalize();
        this.dataDirectory = databaseManager.databasePath().getParent();
    }

    public Path createBackup(Path destinationDirectory) throws IOException, SQLException {
        Path normalizedDestination = destinationDirectory.toAbsolutePath().normalize();
        if (normalizedDestination.startsWith(dataDirectory) || normalizedDestination.startsWith(sharedResourcesRoot)) {
            throw new IllegalArgumentException("Choisis un dossier situé en dehors des données SIOManager.");
        }
        Files.createDirectories(normalizedDestination);
        String filename = "siomanager-backup-" + LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".zip";
        Path archive = normalizedDestination.resolve(filename);
        Path temporaryDatabase = Files.createTempFile("siomanager-backup-", ".db");
        try {
            Files.deleteIfExists(temporaryDatabase);
            String escaped = temporaryDatabase.toString().replace("'", "''");
            try (Connection connection = databaseManager.openConnection(); Statement statement = connection.createStatement()) {
                statement.execute("VACUUM INTO '" + escaped + "'");
            }
            try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(archive))) {
                addFile(zip, temporaryDatabase, DATABASE_ENTRY);
                addDirectory(zip, dataDirectory.resolve("workspaces"), "workspaces/");
                addDirectory(zip, sharedResourcesRoot, "shared-resources/");
            }
            return archive;
        } finally {
            Files.deleteIfExists(temporaryDatabase);
        }
    }

    public void restoreBackup(Path archive) throws IOException {
        Path extraction = Files.createTempDirectory("siomanager-restore-");
        try {
            extractSafely(archive, extraction);
            Path restoredDatabase = extraction.resolve(DATABASE_ENTRY);
            if (!Files.isRegularFile(restoredDatabase)) {
                throw new IllegalArgumentException("Cette archive ne contient pas de base SIOManager valide.");
            }
            validateDatabase(restoredDatabase);
            copyDirectory(extraction.resolve("shared-resources"), sharedResourcesRoot);
            copyDirectory(extraction.resolve("workspaces"), dataDirectory.resolve("workspaces"));
            Files.deleteIfExists(Path.of(databaseManager.databasePath() + "-wal"));
            Files.deleteIfExists(Path.of(databaseManager.databasePath() + "-shm"));
            Files.copy(restoredDatabase, databaseManager.databasePath(), StandardCopyOption.REPLACE_EXISTING);
        } finally {
            deleteRecursively(extraction);
        }
    }

    private void validateDatabase(Path database) throws IOException {
        DatabaseManager candidate = new DatabaseManager(database);
        try (Connection connection = candidate.openConnection(); Statement statement = connection.createStatement()) {
            try (var result = statement.executeQuery("PRAGMA integrity_check")) {
                if (!result.next() || !"ok".equalsIgnoreCase(result.getString(1))) {
                    throw new IOException("La base de données de la sauvegarde est endommagée.");
                }
            }
            statement.executeQuery("SELECT id, username, role FROM users LIMIT 1").close();
        } catch (SQLException exception) {
            throw new IOException("La sauvegarde ne contient pas une base SIOManager valide.", exception);
        }
    }

    private void addDirectory(ZipOutputStream zip, Path root, String prefix) throws IOException {
        if (!Files.isDirectory(root)) {
            return;
        }
        try (var paths = Files.walk(root)) {
            for (Path path : paths.filter(Files::isRegularFile).toList()) {
                addFile(zip, path, prefix + root.relativize(path).toString().replace('\\', '/'));
            }
        }
    }

    private void addFile(ZipOutputStream zip, Path source, String entryName) throws IOException {
        zip.putNextEntry(new ZipEntry(entryName));
        Files.copy(source, zip);
        zip.closeEntry();
    }

    private void extractSafely(Path archive, Path destination) throws IOException {
        try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(archive))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                Path target = destination.resolve(entry.getName()).normalize();
                if (!target.startsWith(destination)) {
                    throw new IllegalArgumentException("L’archive contient un chemin non autorisé.");
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(target);
                } else {
                    Files.createDirectories(target.getParent());
                    Files.copy(zip, target, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private void copyDirectory(Path source, Path destination) throws IOException {
        if (!Files.isDirectory(source)) {
            return;
        }
        Files.createDirectories(destination);
        try (var paths = Files.walk(source)) {
            for (Path path : paths.toList()) {
                Path target = destination.resolve(source.relativize(path)).normalize();
                if (Files.isDirectory(path)) {
                    Files.createDirectories(target);
                } else {
                    Files.copy(path, target, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private void deleteRecursively(Path root) throws IOException {
        if (!Files.exists(root)) {
            return;
        }
        try (var paths = Files.walk(root)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }
}
