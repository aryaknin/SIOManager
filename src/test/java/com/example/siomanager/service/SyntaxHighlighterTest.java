package com.example.siomanager.service;

import org.fxmisc.richtext.model.StyleSpans;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SyntaxHighlighterTest {
    private final SyntaxHighlighter highlighter = new SyntaxHighlighter();

    @Test
    void highlightsJavaKeywordsStringsAndComments() {
        String source = "public class Main { String value = \"SIO\"; // cours\n}";
        StyleSpans<Collection<String>> spans = highlighter.compute(source, "Main.java");

        assertEquals(source.length(), spans.length());
        assertTrue(hasStyle(spans, "keyword"));
        assertTrue(hasStyle(spans, "string"));
        assertTrue(hasStyle(spans, "comment"));
    }

    @Test
    void selectsSqlRulesFromTheFilename() {
        String source = "SELECT nom FROM etudiant WHERE id = 42;";
        StyleSpans<Collection<String>> spans = highlighter.compute(source, "requete.sql");

        assertTrue(hasStyle(spans, "keyword"));
        assertTrue(hasStyle(spans, "number"));
    }

    private boolean hasStyle(StyleSpans<Collection<String>> spans, String style) {
        return IntStream.range(0, spans.getSpanCount())
                .mapToObj(spans::getStyleSpan)
                .anyMatch(span -> span.getStyle().contains(style));
    }
}
