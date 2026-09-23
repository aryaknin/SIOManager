package com.example.siomanager.repository;

import com.example.siomanager.model.ResourceNode;
import com.example.siomanager.model.ResourceType;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

public final class PersonalWorkspaceRepository {
    private static final Set<String> SOURCE_EXTENSIONS = Set.of(
            "java", "html", "htm", "css", "js", "ts", "xml", "fxml", "sql", "sh", "bash",
            "py", "php", "c", "cpp", "h", "hpp", "cs", "kt", "kts", "json", "yml", "yaml",
            "properties", "txt"
    );

    private final Path workspaceRoot;
    private final Collator collator = Collator.getInstance(Locale.FRENCH);

    public PersonalWorkspaceRepository(Path workspaceRoot) {
        this.workspaceRoot = workspaceRoot.toAbsolutePath().normalize();
        collator.setStrength(Collator.PRIMARY);
    }

    public Path workspaceRoot() {
        return workspaceRoot;
    }

    public ResourceNode loadTree() throws IOException {
        Files.createDirectories(workspaceRoot);
        return new ResourceNode(
                "personal",
                "Mon espace",
                ResourceType.SECTION,
                workspaceRoot,
                scanChildren(workspaceRoot)
        );
    }

    public String idFor(Path path) {
        Path normalized = path.toAbsolutePath().normalize();
        if (!normalized.startsWith(workspaceRoot)) {
            throw new IllegalArgumentException("Le chemin ne se trouve pas dans l’espace personnel.");
        }
        Path relative = workspaceRoot.relativize(normalized);
        return relative.getNameCount() == 0
                ? "personal"
                : "personal/" + relative.toString().replace(File.separatorChar, '/');
    }

    private List<ResourceNode> scanChildren(Path directory) throws IOException {
        List<ResourceNode> nodes = new ArrayList<>();
        try (Stream<Path> paths = Files.list(directory)) {
            for (Path path : paths.toList()) {
                if (path.getFileName().toString().startsWith(".") || Files.isSymbolicLink(path)) {
                    continue;
                }
                if (Files.isDirectory(path)) {
                    nodes.add(new ResourceNode(
                            idFor(path),
                            prettify(path.getFileName().toString()),
                            ResourceType.FOLDER,
                            path,
                            scanChildren(path)
                    ));
                } else {
                    nodes.add(new ResourceNode(idFor(path), path.getFileName().toString(), typeFor(path), path, List.of()));
                }
            }
        }
        nodes.sort(Comparator
                .comparing((ResourceNode node) -> !node.isContainer())
                .thenComparing(ResourceNode::name, collator));
        return List.copyOf(nodes);
    }

    private ResourceType typeFor(Path path) {
        String filename = path.getFileName().toString();
        int dot = filename.lastIndexOf('.');
        String extension = dot < 0 ? "" : filename.substring(dot + 1).toLowerCase(Locale.ROOT);
        return switch (extension) {
            case "md", "markdown" -> ResourceType.MARKDOWN;
            case "pdf" -> ResourceType.PDF;
            default -> SOURCE_EXTENSIONS.contains(extension) ? ResourceType.SOURCE_CODE : ResourceType.OTHER;
        };
    }

    private String prettify(String name) {
        String normalized = name.replace('-', ' ').replace('_', ' ').trim();
        return normalized.isEmpty()
                ? name
                : Character.toUpperCase(normalized.charAt(0)) + normalized.substring(1);
    }
}
