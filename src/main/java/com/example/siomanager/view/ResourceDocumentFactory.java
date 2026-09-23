package com.example.siomanager.view;

import com.example.siomanager.model.ResourceNode;
import com.example.siomanager.service.LocalFileService;
import com.example.siomanager.service.MarkdownRendererService;
import com.example.siomanager.service.SyntaxHighlighter;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebView;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;

import java.io.IOException;

public final class ResourceDocumentFactory {
    private final LocalFileService fileService;
    private final MarkdownRendererService markdownRenderer = new MarkdownRendererService();
    private final SyntaxHighlighter syntaxHighlighter = new SyntaxHighlighter();

    public ResourceDocumentFactory(LocalFileService fileService) {
        this.fileService = fileService;
    }

    public DocumentSession create(ResourceNode resource) throws IOException {
        return create(resource, true);
    }

    public DocumentSession create(ResourceNode resource, boolean editable) throws IOException {
        return switch (resource.type()) {
            case MARKDOWN -> createMarkdownSession(resource, editable);
            case PDF -> createPdfSession(resource);
            case SOURCE_CODE -> createCodeSession(resource, editable);
            default -> createReadOnlySession(resource, createUnsupportedView(resource));
        };
    }

    private DocumentSession createMarkdownSession(ResourceNode resource, boolean editable) throws IOException {
        String initialContent = fileService.readText(resource);
        CodeArea editor = createEditor(resource, initialContent, "markdown-code-area");

        ToggleButton editButton = new ToggleButton("Édition");
        ToggleButton previewButton = new ToggleButton("Aperçu");
        editButton.getStyleClass().add("view-mode-button");
        previewButton.getStyleClass().add("view-mode-button");

        ToggleGroup modeGroup = new ToggleGroup();
        editButton.setToggleGroup(modeGroup);
        previewButton.setToggleGroup(modeGroup);
        editButton.setSelected(true);

        VirtualizedScrollPane<CodeArea> editorScroll = new VirtualizedScrollPane<>(editor);
        WebView preview = new WebView();
        preview.getEngine().setJavaScriptEnabled(false);
        preview.getStyleClass().add("markdown-web-view");
        preview.setVisible(false);
        preview.setManaged(false);

        modeGroup.selectedToggleProperty().addListener((observable, previous, selected) -> {
            if (selected == null) {
                editButton.setSelected(true);
                return;
            }

            boolean editing = selected == editButton;
            if (!editing) {
                preview.getEngine().loadContent(
                        markdownRenderer.renderPage(editor.getText(), displayTitle(resource.name())),
                        "text/html"
                );
            }
            editorScroll.setVisible(editing);
            editorScroll.setManaged(editing);
            preview.setVisible(!editing);
            preview.setManaged(!editing);
        });

        HBox toolbar = new HBox(6, editButton, previewButton);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.getStyleClass().add("document-toolbar");

        StackPane content = new StackPane(editorScroll, preview);
        BorderPane document = new BorderPane(content);
        document.setTop(toolbar);
        document.getStyleClass().add("document-view");

        DocumentSession session = new DocumentSession(
                resource,
                document,
                editable ? () -> fileService.saveText(resource, editor.getText()) : null
        );
        editor.setEditable(editable);
        if (editable) {
            editor.textProperty().addListener((observable, previous, current) -> {
                session.markModified();
                applyHighlighting(editor, resource.name());
            });
        } else {
            editButton.setVisible(false);
            editButton.setManaged(false);
            previewButton.setSelected(true);
        }
        return session;
    }

    private DocumentSession createCodeSession(ResourceNode resource, boolean editable) throws IOException {
        String initialContent = fileService.readText(resource);

        Label filename = styledLabel(resource.name(), "document-filename");
        Label accessMode = styledLabel(editable ? "Modifiable" : "Lecture seule", "access-badge");
        HBox toolbar = new HBox(10, filename, accessMode);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.getStyleClass().add("document-toolbar");

        CodeArea editor = createEditor(resource, initialContent, "source-code-area");
        VirtualizedScrollPane<CodeArea> editorScroll = new VirtualizedScrollPane<>(editor);

        BorderPane document = new BorderPane(editorScroll);
        document.setTop(toolbar);
        document.getStyleClass().add("document-view");

        DocumentSession session = new DocumentSession(
                resource,
                document,
                editable ? () -> fileService.saveText(resource, editor.getText()) : null
        );
        editor.setEditable(editable);
        if (editable) {
            editor.textProperty().addListener((observable, previous, current) -> {
                session.markModified();
                applyHighlighting(editor, resource.name());
            });
        }
        return session;
    }

    private CodeArea createEditor(ResourceNode resource, String initialContent, String styleClass) {
        CodeArea editor = new CodeArea();
        editor.getStyleClass().addAll("code-area", styleClass);
        editor.setParagraphGraphicFactory(LineNumberFactory.get(editor));
        editor.replaceText(initialContent);
        applyHighlighting(editor, resource.name());
        return editor;
    }

    private void applyHighlighting(CodeArea editor, String filename) {
        editor.setStyleSpans(0, syntaxHighlighter.compute(editor.getText(), filename));
    }

    private DocumentSession createPdfSession(ResourceNode resource) throws IOException {
        PdfDocumentView view = new PdfDocumentView(resource.localPath());
        return new DocumentSession(resource, view, null, view::close);
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
