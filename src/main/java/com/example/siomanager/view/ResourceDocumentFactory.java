package com.example.siomanager.view;

import com.example.siomanager.model.ResourceNode;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public final class ResourceDocumentFactory {
    private ResourceDocumentFactory() {
    }

    public static Node create(ResourceNode resource) {
        return switch (resource.type()) {
            case MARKDOWN -> createMarkdownView(resource);
            case PDF -> createPdfView(resource);
            case SOURCE_CODE -> createCodeView(resource);
            default -> createUnsupportedView(resource);
        };
    }

    private static Node createMarkdownView(ResourceNode resource) {
        ToggleButton editButton = new ToggleButton("Édition");
        ToggleButton previewButton = new ToggleButton("Aperçu");
        editButton.getStyleClass().add("view-mode-button");
        previewButton.getStyleClass().add("view-mode-button");

        ToggleGroup modeGroup = new ToggleGroup();
        editButton.setToggleGroup(modeGroup);
        previewButton.setToggleGroup(modeGroup);
        editButton.setSelected(true);

        TextArea editor = new TextArea(
                "# " + displayTitle(resource.name()) + "\n\n"
                        + "Le contenu Markdown sera chargé depuis un fichier local, puis depuis l’API.\n\n"
                        + "- Mode édition\n"
                        + "- Mode aperçu\n"
                        + "- Sauvegarde à ajouter ensuite\n"
        );
        editor.getStyleClass().add("markdown-editor");

        VBox preview = new VBox(10,
                styledLabel(displayTitle(resource.name()), "markdown-preview-title"),
                styledLabel("Aperçu du document Markdown", "markdown-preview-subtitle"),
                styledLabel("Le moteur de rendu Markdown sera branché lors d’une prochaine étape.", "muted-label")
        );
        preview.setPadding(new Insets(34));
        preview.getStyleClass().add("markdown-preview");
        preview.setVisible(false);
        preview.setManaged(false);

        modeGroup.selectedToggleProperty().addListener((observable, previous, selected) -> {
            if (selected == null) {
                editButton.setSelected(true);
                return;
            }

            boolean editing = selected == editButton;
            editor.setVisible(editing);
            editor.setManaged(editing);
            preview.setVisible(!editing);
            preview.setManaged(!editing);
        });

        HBox toolbar = new HBox(6, editButton, previewButton);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.getStyleClass().add("document-toolbar");

        StackPane content = new StackPane(editor, preview);
        BorderPane document = new BorderPane(content);
        document.setTop(toolbar);
        document.getStyleClass().add("document-view");
        return document;
    }

    private static Node createPdfView(ResourceNode resource) {
        VBox content = new VBox(12,
                styledLabel("PDF", "document-type-badge"),
                styledLabel(resource.name(), "document-title"),
                styledLabel("Le lecteur PDF sera connecté à cet emplacement.", "muted-label")
        );
        content.setAlignment(Pos.CENTER);
        content.getStyleClass().add("document-placeholder");
        return content;
    }

    private static Node createCodeView(ResourceNode resource) {
        Label filename = styledLabel(resource.name(), "document-filename");
        HBox toolbar = new HBox(filename);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.getStyleClass().add("document-toolbar");

        TextArea editor = new TextArea(sourceTemplate(resource.name()));
        editor.getStyleClass().add("code-editor-placeholder");

        BorderPane document = new BorderPane(editor);
        document.setTop(toolbar);
        document.getStyleClass().add("document-view");
        return document;
    }

    private static Node createUnsupportedView(ResourceNode resource) {
        StackPane content = new StackPane(styledLabel(resource.name(), "document-title"));
        content.getStyleClass().add("document-placeholder");
        return content;
    }

    private static Label styledLabel(String text, String styleClass) {
        Label label = new Label(text);
        label.getStyleClass().add(styleClass);
        label.setWrapText(true);
        return label;
    }

    private static String displayTitle(String filename) {
        int extensionPosition = filename.lastIndexOf('.');
        return extensionPosition > 0 ? filename.substring(0, extensionPosition) : filename;
    }

    private static String sourceTemplate(String filename) {
        if (filename.endsWith(".java")) {
            return "public class Main {\n"
                    + "    public static void main(String[] args) {\n"
                    + "        System.out.println(\"Bonjour SIOManager\");\n"
                    + "    }\n"
                    + "}\n";
        }
        if (filename.endsWith(".html")) {
            return "<!doctype html>\n<html lang=\"fr\">\n<head>\n    <meta charset=\"UTF-8\">\n</head>\n<body>\n\n</body>\n</html>\n";
        }
        if (filename.endsWith(".sql")) {
            return "SELECT *\nFROM ressource\nORDER BY date_modification DESC;\n";
        }
        if (filename.endsWith(".sh")) {
            return "#!/usr/bin/env bash\n\necho \"Sauvegarde à configurer\"\n";
        }
        return "// Éditeur de code provisoire\n";
    }
}
