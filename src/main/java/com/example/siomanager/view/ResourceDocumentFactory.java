package com.example.siomanager.view;

import com.example.siomanager.model.ResourceNode;
import com.example.siomanager.service.LocalFileService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.IOException;

public final class ResourceDocumentFactory {
    private final LocalFileService fileService;

    public ResourceDocumentFactory(LocalFileService fileService) {
        this.fileService = fileService;
    }

    public DocumentSession create(ResourceNode resource) throws IOException {
        return switch (resource.type()) {
            case MARKDOWN -> createMarkdownSession(resource);
            case PDF -> createReadOnlySession(resource, createPdfView(resource));
            case SOURCE_CODE -> createCodeSession(resource);
            default -> createReadOnlySession(resource, createUnsupportedView(resource));
        };
    }

    private DocumentSession createMarkdownSession(ResourceNode resource) throws IOException {
        String initialContent = fileService.readText(resource);

        ToggleButton editButton = new ToggleButton("Édition");
        ToggleButton previewButton = new ToggleButton("Aperçu");
        editButton.getStyleClass().add("view-mode-button");
        previewButton.getStyleClass().add("view-mode-button");

        ToggleGroup modeGroup = new ToggleGroup();
        editButton.setToggleGroup(modeGroup);
        previewButton.setToggleGroup(modeGroup);
        editButton.setSelected(true);

        TextArea editor = new TextArea(initialContent);
        editor.getStyleClass().add("markdown-editor");

        Label previewText = styledLabel(initialContent, "markdown-preview-content");
        VBox preview = new VBox(14,
                styledLabel(displayTitle(resource.name()), "markdown-preview-title"),
                previewText
        );
        preview.setPadding(new Insets(34));
        preview.getStyleClass().add("markdown-preview");

        ScrollPane previewScroll = new ScrollPane(preview);
        previewScroll.setFitToWidth(true);
        previewScroll.getStyleClass().add("markdown-preview-scroll");
        previewScroll.setVisible(false);
        previewScroll.setManaged(false);

        modeGroup.selectedToggleProperty().addListener((observable, previous, selected) -> {
            if (selected == null) {
                editButton.setSelected(true);
                return;
            }

            boolean editing = selected == editButton;
            if (!editing) {
                previewText.setText(editor.getText());
            }
            editor.setVisible(editing);
            editor.setManaged(editing);
            previewScroll.setVisible(!editing);
            previewScroll.setManaged(!editing);
        });

        HBox toolbar = new HBox(6, editButton, previewButton);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.getStyleClass().add("document-toolbar");

        StackPane content = new StackPane(editor, previewScroll);
        BorderPane document = new BorderPane(content);
        document.setTop(toolbar);
        document.getStyleClass().add("document-view");

        DocumentSession session = new DocumentSession(
                resource,
                document,
                () -> fileService.saveText(resource, editor.getText())
        );
        editor.textProperty().addListener((observable, previous, current) -> session.markModified());
        return session;
    }

    private DocumentSession createCodeSession(ResourceNode resource) throws IOException {
        String initialContent = fileService.readText(resource);

        Label filename = styledLabel(resource.name(), "document-filename");
        HBox toolbar = new HBox(filename);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.getStyleClass().add("document-toolbar");

        TextArea editor = new TextArea(initialContent);
        editor.getStyleClass().add("code-editor-placeholder");

        BorderPane document = new BorderPane(editor);
        document.setTop(toolbar);
        document.getStyleClass().add("document-view");

        DocumentSession session = new DocumentSession(
                resource,
                document,
                () -> fileService.saveText(resource, editor.getText())
        );
        editor.textProperty().addListener((observable, previous, current) -> session.markModified());
        return session;
    }

    private Node createPdfView(ResourceNode resource) {
        VBox content = new VBox(12,
                styledLabel("PDF", "document-type-badge"),
                styledLabel(resource.name(), "document-title"),
                styledLabel("Le lecteur PDF sera connecté à cet emplacement.", "muted-label")
        );
        content.setAlignment(Pos.CENTER);
        content.getStyleClass().add("document-placeholder");
        return content;
    }

    private Node createUnsupportedView(ResourceNode resource) {
        StackPane content = new StackPane(styledLabel(resource.name(), "document-title"));
        content.getStyleClass().add("document-placeholder");
        return content;
    }

    private DocumentSession createReadOnlySession(ResourceNode resource, Node content) {
        return new DocumentSession(resource, content, null);
    }

    private Label styledLabel(String text, String styleClass) {
        Label label = new Label(text);
        label.getStyleClass().add(styleClass);
        label.setWrapText(true);
        return label;
    }

    private String displayTitle(String filename) {
        int extensionPosition = filename.lastIndexOf('.');
        return extensionPosition > 0 ? filename.substring(0, extensionPosition) : filename;
    }
}
