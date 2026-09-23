package com.example.siomanager.view;

import com.example.siomanager.model.UserAccount;
import com.example.siomanager.service.AccountService;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.util.Optional;

public final class PasswordChangeDialog {
    private PasswordChangeDialog() { }

    public static Optional<UserAccount> show(Window owner, AccountService accountService, UserAccount account) {
        Dialog<UserAccount> dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.setTitle("Nouveau mot de passe — SIOManager");
        dialog.setHeaderText(null);
        ButtonType validateType = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(validateType, ButtonType.CANCEL);

        PasswordField passwordField = new PasswordField();
        PasswordField confirmationField = new PasswordField();
        passwordField.setPromptText("10 caractères minimum");
        confirmationField.setPromptText("Confirmer");
        Label errorLabel = new Label();
        errorLabel.getStyleClass().add("form-error");
        errorLabel.setWrapText(true);

        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(10);
        form.add(new Label("Nouveau mot de passe"), 0, 0);
        form.add(passwordField, 1, 0);
        form.add(new Label("Confirmation"), 0, 1);
        form.add(confirmationField, 1, 1);

        Label explanation = new Label(account.mustChangePassword()
                ? "Le mot de passe temporaire doit être remplacé avant d’accéder à l’application."
                : "Choisis un nouveau mot de passe d’au moins 10 caractères.");
        explanation.setWrapText(true);
        explanation.getStyleClass().add("muted-label");
        VBox content = new VBox(10, explanation, form, errorLabel);
        content.setPadding(new Insets(8));
        content.setPrefWidth(460);
        dialog.getDialogPane().setContent(content);
        AuthenticationDialog.addApplicationStyles(dialog);
        dialog.setResultConverter(buttonType -> null);

        Button validateButton = (Button) dialog.getDialogPane().lookupButton(validateType);
        validateButton.addEventFilter(ActionEvent.ACTION, event -> {
            event.consume();
            try {
                UserAccount updated = accountService.changeOwnPassword(
                        account,
                        passwordField.getText().toCharArray(),
                        confirmationField.getText().toCharArray()
                );
                dialog.setResult(updated);
                dialog.close();
            } catch (IllegalArgumentException | IllegalStateException exception) {
                errorLabel.setText(exception.getMessage());
                passwordField.clear();
                confirmationField.clear();
                passwordField.requestFocus();
            }
        });

        return dialog.showAndWait();
    }
}
