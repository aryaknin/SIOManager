package com.example.siomanager.service;

import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SyntaxHighlighter {
    private static final String JAVA_KEYWORDS = String.join("|",
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class",
            "const", "continue", "default", "do", "double", "else", "enum", "exports", "extends",
            "final", "finally", "float", "for", "goto", "if", "implements", "import", "instanceof",
            "int", "interface", "long", "module", "native", "new", "non-sealed", "open", "opens",
            "package", "permits", "private", "protected", "provides", "public", "record", "requires",
            "return", "sealed", "short", "static", "strictfp", "super", "switch", "synchronized",
            "this", "throw", "throws", "to", "transient", "transitive", "try", "uses", "var", "void",
            "volatile", "while", "with", "yield", "true", "false", "null"
    );

    private static final String SQL_KEYWORDS = String.join("|",
            "add", "alter", "and", "as", "asc", "between", "by", "case", "constraint", "create",
            "database", "default", "delete", "desc", "distinct", "drop", "else", "end", "exists",
            "foreign", "from", "full", "group", "having", "in", "index", "inner", "insert", "into",
            "is", "join", "key", "left", "like", "limit", "not", "null", "on", "or", "order",
            "outer", "primary", "references", "right", "select", "set", "table", "then", "union",
            "unique", "update", "values", "view", "when", "where"
    );

    private static final String SHELL_KEYWORDS = String.join("|",
            "case", "do", "done", "elif", "else", "esac", "fi", "for", "function", "if", "in",
            "select", "then", "time", "until", "while"
    );

    public StyleSpans<Collection<String>> compute(String text, String filename) {
        Pattern pattern = patternFor(filename);
        Matcher matcher = pattern.matcher(text);
        StyleSpansBuilder<Collection<String>> spans = new StyleSpansBuilder<>();
        int lastMatchEnd = 0;

        while (matcher.find()) {
            spans.add(Collections.emptyList(), matcher.start() - lastMatchEnd);
            spans.add(Collections.singleton(styleClass(matcher)), matcher.end() - matcher.start());
            lastMatchEnd = matcher.end();
        }
        spans.add(Collections.emptyList(), text.length() - lastMatchEnd);
        return spans.create();
    }

    private Pattern patternFor(String filename) {
        String extension = extension(filename);
        return switch (extension) {
            case "java" -> javaPattern();
            case "sql" -> sqlPattern();
            case "html", "htm", "xml", "fxml" -> htmlPattern();
            case "sh", "bash" -> shellPattern();
            case "md", "markdown" -> markdownPattern();
            default -> genericPattern();
        };
    }

    private Pattern javaPattern() {
        return compile(
                "(?<COMMENT>//[^\\n]*|/\\*(?:.|\\R)*?\\*/)"
                        + "|(?<STRING>\"(?:\\\\.|[^\"\\\\])*\"|'(?:\\\\.|[^'\\\\])*')"
                        + "|(?<ANNOTATION>@[A-Za-z_$][\\w$]*)"
                        + "|(?<KEYWORD>\\b(?:" + JAVA_KEYWORDS + ")\\b)"
                        + "|(?<NUMBER>\\b(?:0[xX][0-9a-fA-F_]+|\\d[\\d_]*(?:\\.\\d[\\d_]*)?)\\b)"
                        + "|(?<PAREN>[()])|(?<BRACE>[{}])|(?<BRACKET>[\\[\\]])"
        );
    }

    private Pattern sqlPattern() {
        return compile(
                "(?<COMMENT>--[^\\n]*|/\\*(?:.|\\R)*?\\*/)"
                        + "|(?<STRING>'(?:''|[^'])*'|\"(?:\"\"|[^\"])*\")"
                        + "|(?<KEYWORD>(?i:\\b(?:" + SQL_KEYWORDS + ")\\b))"
                        + "|(?<NUMBER>\\b\\d+(?:\\.\\d+)?\\b)"
                        + "|(?<PAREN>[()])"
        );
    }

    private Pattern htmlPattern() {
        return compile(
                "(?<COMMENT><!--(?:.|\\R)*?-->)"
                        + "|(?<STRING>\"(?:\\\\.|[^\"\\\\])*\"|'(?:\\\\.|[^'\\\\])*')"
                        + "|(?<TAG></?[A-Za-z][A-Za-z0-9:_-]*|/?>)"
                        + "|(?<ATTRIBUTE>\\b[A-Za-z_:][A-Za-z0-9:_.-]*(?=\\s*=))"
        );
    }

    private Pattern shellPattern() {
        return compile(
                "(?<COMMENT>#[^\\n]*)"
                        + "|(?<STRING>\"(?:\\\\.|[^\"\\\\])*\"|'[^']*')"
                        + "|(?<VARIABLE>\\$\\{?[A-Za-z_][A-Za-z0-9_]*}?)"
                        + "|(?<KEYWORD>\\b(?:" + SHELL_KEYWORDS + ")\\b)"
                        + "|(?<NUMBER>\\b\\d+(?:\\.\\d+)?\\b)"
        );
    }

    private Pattern markdownPattern() {
        return compile(
                "(?<CODE>`{1,3}[^`]*`{1,3})"
                        + "|(?<HEADING>^[ \\t]{0,3}#{1,6}[ \\t].*$)"
                        + "|(?<LINK>!?\\[[^]]*](?:\\([^)]*\\)))"
                        + "|(?<EMPHASIS>(?:\\*\\*|__)[^\\n]+?(?:\\*\\*|__)|(?:\\*|_)[^\\n]+?(?:\\*|_))"
                        + "|(?<QUOTE>^[ \\t]{0,3}>.*$)"
        );
    }

    private Pattern genericPattern() {
        return compile(
                "(?<COMMENT>//[^\\n]*|#[^\\n]*|/\\*(?:.|\\R)*?\\*/)"
                        + "|(?<STRING>\"(?:\\\\.|[^\"\\\\])*\"|'(?:\\\\.|[^'\\\\])*')"
                        + "|(?<NUMBER>\\b\\d+(?:\\.\\d+)?\\b)"
        );
    }

    private Pattern compile(String expression) {
        return Pattern.compile(expression, Pattern.MULTILINE);
    }

    private String styleClass(Matcher matcher) {
        for (String group : new String[]{
                "COMMENT", "STRING", "ANNOTATION", "KEYWORD", "NUMBER", "PAREN", "BRACE", "BRACKET",
                "TAG", "ATTRIBUTE", "VARIABLE", "CODE", "HEADING", "LINK", "EMPHASIS", "QUOTE"
        }) {
            try {
                if (matcher.group(group) != null) {
                    return group.toLowerCase(Locale.ROOT);
                }
            } catch (IllegalArgumentException ignored) {
                // The selected language pattern does not define every possible group.
            }
        }
        return "plain";
    }

    private String extension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot < 0 ? "" : filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
