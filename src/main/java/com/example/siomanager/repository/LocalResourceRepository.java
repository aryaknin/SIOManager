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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public class LocalResourceRepository {
    private static final Map<String, String> DISPLAY_NAMES = displayNames();
    private static final Set<String> SOURCE_EXTENSIONS = Set.of(
            "java", "html", "htm", "css", "js", "ts", "xml", "fxml", "sql", "sh", "bash",
            "py", "php", "c", "cpp", "h", "hpp", "cs", "kt", "kts", "json", "yml", "yaml",
            "properties", "txt"
    );

    private final Path contentRoot;
    private final Collator collator = Collator.getInstance(Locale.FRENCH);

    public LocalResourceRepository() {
        this(Path.of(System.getProperty("user.dir"), "demo-content"));
    }

    public LocalResourceRepository(Path contentRoot) {
        this.contentRoot = contentRoot.toAbsolutePath().normalize();
        collator.setStrength(Collator.PRIMARY);
    }

    public Path contentRoot() {
        return contentRoot;
    }

    public ResourceNode loadTree() throws IOException {
        ensureBaseStructure();
        List<ResourceNode> sections = scanChildren(contentRoot, 0);
        return new ResourceNode("root", "Ressources", ResourceType.ROOT, null, sections);
    }

    public Path pathFor(ResourceNode resource) {
        if (resource.type() == ResourceType.ROOT) {
            return contentRoot;
        }
        Path resolved = contentRoot.resolve(resource.id().replace('/', File.separatorChar)).normalize();
        if (!resolved.startsWith(contentRoot)) {
            throw new IllegalArgumentException("La ressource sort du dossier de contenu autorisé.");
        }
        return resolved;
    }

    public String idFor(Path path) {
        Path normalized = path.toAbsolutePath().normalize();
        if (!normalized.startsWith(contentRoot)) {
            throw new IllegalArgumentException("Le chemin ne se trouve pas dans les ressources.");
        }
        return contentRoot.relativize(normalized).toString().replace(File.separatorChar, '/');
    }

    private void ensureBaseStructure() throws IOException {
        Files.createDirectories(contentRoot);
        Files.createDirectories(contentRoot.resolve("common"));
        Files.createDirectories(contentRoot.resolve("sisr"));
        Files.createDirectories(contentRoot.resolve("slam"));
    }

    private List<ResourceNode> scanChildren(Path directory, int parentDepth) throws IOException {
        List<ResourceNode> nodes = new ArrayList<>();
        try (Stream<Path> paths = Files.list(directory)) {
            for (Path path : paths.toList()) {
                if (shouldIgnore(path)) {
                    continue;
                }
                ResourceNode node = toNode(path, parentDepth);
                if (node != null) {
                    nodes.add(node);
                }
            }
        }
        nodes.sort(nodeComparator());
        return List.copyOf(nodes);
    }

    private ResourceNode toNode(Path path, int parentDepth) throws IOException {
        String id = idFor(path);
        String displayName = DISPLAY_NAMES.getOrDefault(id, prettify(path.getFileName().toString()));

        if (Files.isDirectory(path)) {
            ResourceType type = switch (parentDepth) {
                case 0 -> ResourceType.SECTION;
                case 1 -> ResourceType.SUBJECT;
                default -> ResourceType.FOLDER;
            };
            return new ResourceNode(id, displayName, type, null, scanChildren(path, parentDepth + 1));
        }

        ResourceType type = typeForFile(path);
        return new ResourceNode(id, DISPLAY_NAMES.getOrDefault(id, path.getFileName().toString()), type, path, List.of());
    }

    private ResourceType typeForFile(Path path) {
        String filename = path.getFileName().toString();
        int dot = filename.lastIndexOf('.');
        String extension = dot < 0 ? "" : filename.substring(dot + 1).toLowerCase(Locale.ROOT);
        return switch (extension) {
            case "md", "markdown" -> ResourceType.MARKDOWN;
            case "pdf" -> ResourceType.PDF;
            default -> SOURCE_EXTENSIONS.contains(extension) ? ResourceType.SOURCE_CODE : ResourceType.OTHER;
        };
    }

    private boolean shouldIgnore(Path path) {
        String name = path.getFileName().toString();
        return name.startsWith(".") || Files.isSymbolicLink(path);
    }

    private Comparator<ResourceNode> nodeComparator() {
        return Comparator
                .comparing((ResourceNode node) -> !node.isContainer())
                .thenComparing(ResourceNode::name, collator);
    }

    private String prettify(String rawName) {
        String withoutExtension = rawName;
        int dot = rawName.lastIndexOf('.');
        if (dot > 0) {
            withoutExtension = rawName.substring(0, dot);
        }

        String[] words = withoutExtension.replace('-', ' ').replace('_', ' ').trim().split("\\s+");
        List<String> formatted = new ArrayList<>();
        for (String word : words) {
            if (word.isBlank()) {
                continue;
            }
            String lower = word.toLowerCase(Locale.FRENCH);
            if (Set.of("bts", "sio", "sisr", "slam", "cejm", "cge", "osi", "sql", "html", "css", "owasp")
                    .contains(lower)) {
                formatted.add(lower.toUpperCase(Locale.FRENCH));
            } else if (lower.matches("0\\d+")) {
                formatted.add(String.valueOf(Integer.parseInt(lower)));
            } else {
                formatted.add(Character.toUpperCase(lower.charAt(0)) + lower.substring(1));
            }
        }
        return String.join(" ", formatted);
    }

    private static Map<String, String> displayNames() {
        Map<String, String> names = new HashMap<>();
        names.put("common", "Enseignements communs");
        names.put("common/cge", "Culture générale et expression");
        names.put("common/mathematiques", "Mathématiques");
        names.put("common/anglais", "Anglais");
        names.put("common/cejm", "CEJM");
        names.put("common/cge/argumentation", "Chapitre 1 — Argumentation");
        names.put("common/mathematiques/suites", "Chapitre 1 — Suites");
        names.put("common/cejm/chapitre-01", "Chapitre 1 — Les agents économiques");
        names.put("common/cejm/chapitre-02", "Chapitre 2 — Les contrats");
        names.put("common/cejm/chapitre-01/synthese.pdf", "synthèse.pdf");
        names.put("common/mathematiques/suites/fiche-de-revision.pdf", "fiche-de-révision.pdf");
        names.put("sisr", "SISR");
        names.put("sisr/reseaux", "Réseaux");
        names.put("sisr/systemes", "Systèmes");
        names.put("sisr/cybersecurite", "Cybersécurité");
        names.put("sisr/reseaux/modele-osi", "Chapitre 1 — Modèle OSI");
        names.put("sisr/systemes/linux", "Linux");
        names.put("sisr/cybersecurite/permissions-linux", "Gestion des droits");
        names.put("sisr/reseaux/modele-osi/tp-reseau.pdf", "tp-réseau.pdf");
        names.put("slam", "SLAM");
        names.put("slam/java", "Java");
        names.put("slam/web", "Web");
        names.put("slam/bases-de-donnees", "Bases de données");
        names.put("slam/cybersecurite", "Cybersécurité");
        names.put("slam/java/poo", "Chapitre 1 — Programmation objet");
        names.put("slam/web/html-css", "HTML et CSS");
        names.put("slam/bases-de-donnees/sql", "SQL");
        names.put("slam/cybersecurite/introduction-owasp", "Sécurité Web");
        return Map.copyOf(names);
    }
}
