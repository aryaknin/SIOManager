package com.example.siomanager.model;

public enum ResourceType {
    ROOT("", "resource-root", true),
    SECTION("◈", "resource-section", true),
    SUBJECT("◆", "resource-subject", true),
    FOLDER("▰", "resource-folder", true),
    MARKDOWN("M↓", "resource-markdown", false),
    PDF("PDF", "resource-pdf", false),
    SOURCE_CODE("</>", "resource-code", false);

    private final String symbol;
    private final String cssClass;
    private final boolean container;

    ResourceType(String symbol, String cssClass, boolean container) {
        this.symbol = symbol;
        this.cssClass = cssClass;
        this.container = container;
    }

    public String symbol() {
        return symbol;
    }

    public String cssClass() {
        return cssClass;
    }

    public boolean isContainer() {
        return container;
    }
}
