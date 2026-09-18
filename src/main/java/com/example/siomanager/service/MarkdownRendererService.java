package com.example.siomanager.service;

import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

public final class MarkdownRendererService {
    private final Parser parser = Parser.builder().build();
    private final HtmlRenderer renderer = HtmlRenderer.builder()
            .escapeHtml(true)
            .sanitizeUrls(true)
            .build();

    public String renderPage(String markdown, String title) {
        String body = renderer.render(parser.parse(markdown));
        return """
                <!doctype html>
                <html lang="fr">
                <head>
                    <meta charset="UTF-8">
                    <meta name="color-scheme" content="dark">
                    <title>%s</title>
                    <style>
                        :root { color-scheme: dark; }
                        body {
                            max-width: 850px; margin: 0 auto; padding: 24px 32px 52px;
                            background: #1b1c20; color: #d7dae0;
                            font: 14px/1.6 Inter, "Segoe UI", sans-serif;
                        }
                        h1, h2, h3, h4 { color: #f2f3f5; line-height: 1.25; margin-top: 1.5em; }
                        h1 { font-size: 2em; border-bottom: 1px solid #393b40; padding-bottom: .35em; }
                        h2 { font-size: 1.5em; border-bottom: 1px solid #33353a; padding-bottom: .25em; }
                        a { color: #6ea8fe; }
                        code, pre { font-family: "JetBrains Mono", "Noto Sans Mono", monospace; }
                        code { background: #292b31; color: #e6b673; padding: .15em .35em; border-radius: 4px; }
                        pre { overflow-x: auto; background: #202126; border: 1px solid #34363c;
                              border-radius: 6px; padding: 12px 14px; }
                        pre code { padding: 0; background: transparent; color: #dfe1e5; }
                        blockquote { margin-left: 0; padding-left: 16px; color: #afb3bb;
                                     border-left: 4px solid #3574f0; }
                        table { border-collapse: collapse; width: 100%%; }
                        th, td { border: 1px solid #45484f; padding: 7px 10px; text-align: left; }
                        th { background: #2b2d30; }
                        hr { border: 0; border-top: 1px solid #45484f; }
                        img { max-width: 100%%; }
                    </style>
                </head>
                <body>%s</body>
                </html>
                """.formatted(escapeHtml(title), body);
    }

    private String escapeHtml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
