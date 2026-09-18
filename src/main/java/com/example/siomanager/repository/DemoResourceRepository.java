package com.example.siomanager.repository;

import com.example.siomanager.model.ResourceNode;
import com.example.siomanager.model.ResourceType;

import java.util.Arrays;
import java.util.List;

public class DemoResourceRepository {
    public ResourceNode loadTree() {
        return branch("root", "Ressources", ResourceType.ROOT,
                commonResources(),
                sisrResources(),
                slamResources()
        );
    }

    private ResourceNode commonResources() {
        return branch("common", "Enseignements communs", ResourceType.SECTION,
                branch("common-cge", "Culture générale et expression", ResourceType.SUBJECT,
                        branch("common-cge-chapter-1", "Chapitre 1 — Argumentation", ResourceType.FOLDER,
                                file("common-cge-chapter-1-course", "cours.md", ResourceType.MARKDOWN)
                        )
                ),
                branch("common-maths", "Mathématiques", ResourceType.SUBJECT,
                        branch("common-maths-chapter-1", "Chapitre 1 — Suites", ResourceType.FOLDER,
                                file("common-maths-chapter-1-summary", "fiche-de-révision.pdf", ResourceType.PDF)
                        )
                ),
                branch("common-english", "Anglais", ResourceType.SUBJECT,
                        branch("common-english-vocabulary", "Vocabulaire", ResourceType.FOLDER,
                                file("common-english-network", "network-vocabulary.md", ResourceType.MARKDOWN)
                        )
                ),
                branch("common-cejm", "CEJM", ResourceType.SUBJECT,
                        branch("common-cejm-chapter-1", "Chapitre 1 — Les agents économiques", ResourceType.FOLDER,
                                file("common-cejm-chapter-1-course", "cours.md", ResourceType.MARKDOWN),
                                file("common-cejm-chapter-1-summary", "synthèse.pdf", ResourceType.PDF)
                        ),
                        branch("common-cejm-chapter-2", "Chapitre 2 — Les contrats", ResourceType.FOLDER,
                                file("common-cejm-chapter-2-course", "cours.md", ResourceType.MARKDOWN)
                        )
                )
        );
    }

    private ResourceNode sisrResources() {
        return branch("sisr", "SISR", ResourceType.SECTION,
                branch("sisr-network", "Réseaux", ResourceType.SUBJECT,
                        branch("sisr-network-chapter-1", "Chapitre 1 — Modèle OSI", ResourceType.FOLDER,
                                file("sisr-network-chapter-1-course", "cours.md", ResourceType.MARKDOWN),
                                file("sisr-network-chapter-1-lab", "tp-réseau.pdf", ResourceType.PDF)
                        )
                ),
                branch("sisr-systems", "Systèmes", ResourceType.SUBJECT,
                        branch("sisr-systems-linux", "Linux", ResourceType.FOLDER,
                                file("sisr-systems-linux-commands", "commandes.md", ResourceType.MARKDOWN),
                                file("sisr-systems-linux-script", "script-backup.sh", ResourceType.SOURCE_CODE)
                        )
                ),
                branch("sisr-security", "Cybersécurité", ResourceType.SUBJECT,
                        branch("sisr-security-rights", "Gestion des droits", ResourceType.FOLDER,
                                file("sisr-security-rights-course", "permissions-linux.md", ResourceType.MARKDOWN)
                        )
                )
        );
    }

    private ResourceNode slamResources() {
        return branch("slam", "SLAM", ResourceType.SECTION,
                branch("slam-java", "Java", ResourceType.SUBJECT,
                        branch("slam-java-chapter-1", "Chapitre 1 — Programmation objet", ResourceType.FOLDER,
                                file("slam-java-chapter-1-course", "cours.md", ResourceType.MARKDOWN),
                                file("slam-java-chapter-1-example", "Main.java", ResourceType.SOURCE_CODE)
                        )
                ),
                branch("slam-web", "Web", ResourceType.SUBJECT,
                        branch("slam-web-html", "HTML et CSS", ResourceType.FOLDER,
                                file("slam-web-html-example", "index.html", ResourceType.SOURCE_CODE)
                        )
                ),
                branch("slam-database", "Bases de données", ResourceType.SUBJECT,
                        branch("slam-database-sql", "SQL", ResourceType.FOLDER,
                                file("slam-database-sql-queries", "requetes.sql", ResourceType.SOURCE_CODE)
                        )
                ),
                branch("slam-security", "Cybersécurité", ResourceType.SUBJECT,
                        branch("slam-security-owasp", "Sécurité Web", ResourceType.FOLDER,
                                file("slam-security-owasp-notes", "introduction-owasp.md", ResourceType.MARKDOWN)
                        )
                )
        );
    }

    private ResourceNode branch(String id, String name, ResourceType type, ResourceNode... children) {
        return new ResourceNode(id, name, type, Arrays.asList(children));
    }

    private ResourceNode file(String id, String name, ResourceType type) {
        return new ResourceNode(id, name, type, List.of());
    }
}
