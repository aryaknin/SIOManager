package com.example.siomanager.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkdownRendererServiceTest {
    private final MarkdownRendererService renderer = new MarkdownRendererService();

    @Test
    void rendersMarkdownAsACompleteHtmlPage() {
        String html = renderer.renderPage("# Réseau\n\nTexte en **gras**.", "Cours réseau");

        assertTrue(html.contains("<!doctype html>"));
        assertTrue(html.contains("<h1>Réseau</h1>"));
        assertTrue(html.contains("<strong>gras</strong>"));
        assertTrue(html.contains("<title>Cours réseau</title>"));
    }

    @Test
    void escapesRawHtmlAndSanitizesUnsafeUrls() {
        String html = renderer.renderPage(
                "<script>alert('x')</script>\n\n[piège](javascript:alert('x'))",
                "Sécurité"
        );

        assertFalse(html.contains("<script>alert"));
        assertFalse(html.contains("href=\"javascript:"));
        assertTrue(html.contains("&lt;script&gt;"));
    }
}
