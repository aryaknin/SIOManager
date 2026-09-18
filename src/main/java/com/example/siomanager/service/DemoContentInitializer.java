package com.example.siomanager.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class DemoContentInitializer {
    public void ensurePdfSamples(Path contentRoot) throws IOException {
        createPdfIfMissing(
                contentRoot.resolve("common/mathematiques/suites/fiche-de-revision.pdf"),
                "Mathématiques — Suites",
                List.of("Définition d'une suite", "Suites arithmétiques", "Suites géométriques", "Exercices de révision")
        );
        createPdfIfMissing(
                contentRoot.resolve("common/cejm/chapitre-01/synthese.pdf"),
                "CEJM — Les agents économiques",
                List.of("Ménages et entreprises", "Administrations publiques", "Institutions financières", "Échanges et flux")
        );
        createPdfIfMissing(
                contentRoot.resolve("sisr/reseaux/modele-osi/tp-reseau.pdf"),
                "SISR — TP modèle OSI",
                List.of("Identifier les sept couches", "Observer les protocoles", "Analyser une trame", "Documenter le diagnostic")
        );
    }

    private void createPdfIfMissing(Path path, String title, List<String> topics) throws IOException {
        if (Files.exists(path)) {
            return;
        }

        Files.createDirectories(path.getParent());
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            PDType1Font titleFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font bodyFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(titleFont, 22);
                content.newLineAtOffset(64, 760);
                content.showText(title);
                content.setFont(bodyFont, 12);
                content.newLineAtOffset(0, -42);
                content.showText("Document de démonstration généré par SIOManager");

                for (String topic : topics) {
                    content.newLineAtOffset(0, -30);
                    content.showText("• " + topic);
                }
                content.endText();
            }
            document.save(path.toFile());
        }
    }
}
