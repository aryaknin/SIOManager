package com.example.siomanager.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DemoContentInitializerTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void createsReadablePdfSamplesAndCanRunTwice() throws Exception {
        DemoContentInitializer initializer = new DemoContentInitializer();
        initializer.ensurePdfSamples(temporaryDirectory);
        initializer.ensurePdfSamples(temporaryDirectory);

        Path sample = temporaryDirectory.resolve("common/cejm/chapitre-01/synthese.pdf");
        assertTrue(Files.isRegularFile(sample));
        assertTrue(Files.size(sample) > 0);

        try (PDDocument document = Loader.loadPDF(sample.toFile())) {
            assertEquals(1, document.getNumberOfPages());
        }
    }
}
