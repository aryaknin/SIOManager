package com.example.siomanager.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.Set;

public final class ResourceCreationService {
    private static final Set<String> JAVA_KEYWORDS = Set.of(
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class",
            "const", "continue", "default", "do", "double", "else", "enum", "extends", "final",
            "finally", "float", "for", "goto", "if", "implements", "import", "instanceof", "int",
            "interface", "long", "native", "new", "package", "private", "protected", "public", "return",
            "short", "static", "strictfp", "super", "switch", "synchronized", "this", "throw", "throws",
            "transient", "try", "void", "volatile", "while", "true", "false", "null", "record", "sealed",
            "permits", "yield", "var"
    );

    public Path create(Path contentRoot, Path parentDirectory, String requestedName, ResourceKind kind)
            throws IOException {
        Path root = contentRoot.toAbsolutePath().normalize();
        Path parent = parentDirectory.toAbsolutePath().normalize();
        if (!parent.startsWith(root) || !Files.isDirectory(parent)) {
            throw new IllegalArgumentException("Le dossier de destination n’est pas valide.");
        }

        String name = validateName(requestedName);
        if (kind != ResourceKind.FOLDER && !name.toLowerCase(Locale.ROOT).endsWith(kind.extension())) {
            name += kind.extension();
        }

        Path target = parent.resolve(name).normalize();
        if (!target.getParent().equals(parent) || !target.startsWith(root)) {
            throw new IllegalArgumentException("Le nom de ressource n’est pas autorisé.");
        }

        if (kind == ResourceKind.FOLDER) {
            return Files.createDirectory(target);
        }

        String content = initialContent(target, kind);
        try {
            return Files.writeString(
                    target,
                    content,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE_NEW,
                    StandardOpenOption.WRITE
            );
        } catch (FileAlreadyExistsException exception) {
            throw new FileAlreadyExistsException("Une ressource portant ce nom existe déjà : " + target.getFileName());
        }
    }

    private String validateName(String requestedName) {
        if (requestedName == null || requestedName.isBlank()) {
            throw new IllegalArgumentException("Le nom de la ressource est obligatoire.");
        }

        String name = requestedName.trim();
        if (name.equals(".") || name.equals("..") || name.endsWith(".") || name.endsWith(" ")
                || name.matches(".*[\\\\/:*?\"<>|].*")) {
            throw new IllegalArgumentException(
                    "Le nom contient un caractère interdit sous Linux ou Windows."
            );
        }
        return name;
    }

    private String initialContent(Path target, ResourceKind kind) {
        String filename = target.getFileName().toString();
        String baseName = filename.substring(0, filename.length() - kind.extension().length());
        return switch (kind) {
            case MARKDOWN -> "# " + humanize(baseName) + System.lineSeparator() + System.lineSeparator();
            case JAVA -> javaTemplate(baseName);
            case HTML -> """
                    <!doctype html>
                    <html lang="fr">
                    <head>
                        <meta charset="UTF-8">
                        <meta name="viewport" content="width=device-width, initial-scale=1.0">
                        <title>%s</title>
                    </head>
                    <body>
                        <h1>%s</h1>
                    </body>
                    </html>
                    """.formatted(humanize(baseName), humanize(baseName));
            case SQL -> "-- " + humanize(baseName) + System.lineSeparator() + System.lineSeparator();
            case BASH -> """
                    #!/usr/bin/env bash

                    set -euo pipefail

                    echo "Nouveau script SIOManager"
                    """;
            case FOLDER -> throw new IllegalStateException("Un dossier ne possède pas de contenu initial.");
        };
    }

    private String javaTemplate(String className) {
        if (!isJavaIdentifier(className) || JAVA_KEYWORDS.contains(className)) {
            throw new IllegalArgumentException(
                    "Le nom d’un fichier Java doit être un nom de classe valide, par exemple Main."
            );
        }
        return """
                public class %s {
                    public static void main(String[] args) {
                        System.out.println("Bonjour depuis %s !");
                    }
                }
                """.formatted(className, className);
    }

    private boolean isJavaIdentifier(String value) {
        if (value.isEmpty() || !Character.isJavaIdentifierStart(value.charAt(0))) {
            return false;
        }
        for (int index = 1; index < value.length(); index++) {
            if (!Character.isJavaIdentifierPart(value.charAt(index))) {
                return false;
            }
        }
        return true;
    }

    private String humanize(String value) {
        String normalized = value.replace('-', ' ').replace('_', ' ').trim();
        if (normalized.isEmpty()) {
            return "Nouvelle ressource";
        }
        return Character.toUpperCase(normalized.charAt(0)) + normalized.substring(1);
    }

    public enum ResourceKind {
        FOLDER("Dossier", ""),
        MARKDOWN("Cours Markdown", ".md"),
        JAVA("Fichier Java", ".java"),
        HTML("Page HTML", ".html"),
        SQL("Script SQL", ".sql"),
        BASH("Script Bash", ".sh");

        private final String label;
        private final String extension;

        ResourceKind(String label, String extension) {
            this.label = label;
            this.extension = extension;
        }

        public String extension() {
            return extension;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public record CreationRequest(String name, ResourceKind kind) {
    }
}
