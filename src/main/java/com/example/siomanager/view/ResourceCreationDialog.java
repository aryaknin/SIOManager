package com.example.siomanager.view;

import com.example.siomanager.service.ResourceCreationService.CreationRequest;
import com.example.siomanager.service.ResourceCreationService.ResourceKind;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.Window;

import java.util.Optional;

public final class ResourceCreationDialog {
    private ResourceCreationDialog() {
    }

    public static Optional<CreationRequest> show(Window owner, String destinationName) {
        Dialog<CreationRequest> dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.setTitle("Nouvelle ressource");
        dialog.setHeaderText("Créer dans « " + destinationName + " »");
        dialog.getDialogPane().getStylesheets().add(
                ResourceCreationDialog.class
                        .getResource("/com/example/siomanager/styles/application.css")
                        .toExternalForm()
        );
        dialog.getDialogPane().getStyleClass().add("resource-dialog");

        ButtonType createButtonType = new ButtonType("Créer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(createButtonType, ButtonType.CANCEL);

        ComboBox<ResourceKind> kindBox = new ComboBox<>();
        kindBox.getItems().setAll(ResourceKind.values());
        kindBox.setValue(ResourceKind.FOLDER);
        kindBox.setMaxWidth(Double.MAX_VALUE);

        TextField nameField = new TextField();
        nameField.setPromptText("Ex. chapitre-03 ou cours");

        Label hint = new Label("L’extension est ajoutée automatiquement pour les fichiers.");
        hint.getStyleClass().add("muted-label");
        hint.setWrapText(true);

        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(10);
        form.setPadding(new Insets(6, 4, 4, 4));
        form.addRow(0, new Label("Type"), kindBox);
        form.addRow(1, new Label("Nom"), nameField);
        form.add(hint, 1, 2);
        GridPane.setHgrow(kindBox, javafx.scene.layout.Priority.ALWAYS);
        GridPane.setHgrow(nameField, javafx.scene.layout.Priority.ALWAYS);
        dialog.getDialogPane().setContent(form);

        Node createButton = dialog.getDialogPane().lookupButton(createButtonType);
        createButton.disableProperty().bind(Bindings.createBooleanBinding(
                () -> nameField.getText() == null || nameField.getText().isBlank(),
                nameField.textProperty()
        ));

        dialog.setResultConverter(button -> button == createButtonType
                ? new CreationRequest(nameField.getText(), kindBox.getValue())
                : null
        );

        dialog.setOnShown(event -> nameField.requestFocus());
        return dialog.showAndWait();
    }
}
