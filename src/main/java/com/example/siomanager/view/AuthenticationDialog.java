package com.example.siomanager.view;

import com.example.siomanager.MainApplication;
import com.example.siomanager.model.UserAccount;
import com.example.siomanager.service.AccountService;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.util.Optional;

public final class AuthenticationDialog {
    private AuthenticationDialog() { }

    public static Optional<UserAccount> show(Window owner, AccountService accountService) {
        boolean initialSetup = accountService.needsInitialAdministrator();
        Dialog<UserAccount> dialog = new Dialog<>();
        if (owner != null) {
            dialog.initOwner(owner);
        }
        dialog.setTitle(initialSetup ? "Configuration initiale — SIOManager" : "Connexion — SIOManager");
        dialog.setHeaderText(null);

        ButtonType submitType = new ButtonType(
                initialSetup ? "Créer l’administrateur" : "Se connecter",
                ButtonBar.ButtonData.OK_DONE
        );
        dialog.getDialogPane().getButtonTypes().addAll(submitType, ButtonType.CANCEL);

        TextField usernameField = new TextField(initialSetup ? "admin" : "");
        usernameField.setPromptText("identifiant");
        TextField displayNameField = new TextField();
        displayNameField.setPromptText("Prénom et nom");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("10 caractères minimum");
        PasswordField confirmationField = new PasswordField();
        confirmationField.setPromptText("Confirmer le mot de passe");
        Label errorLabel = new Label();
        errorLabel.getStyleClass().add("form-error");
        errorLabel.setWrapText(true);

        Label title = new Label(initialSetup ? "Créer le premier administrateur" : "Bienvenue");
        title.getStyleClass().add("auth-title");
        Label explanation = new Label(initialSetup
                ? "Aucun compte n’existe encore. Ce compte aura accès à la gestion des ressources et des utilisateurs."
                : "Connecte-toi avec ton compte administrateur ou élève.");
        explanation.getStyleClass().add("muted-label");
        explanation.setWrapText(true);

        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(10);
        int row = 0;
        form.add(new Label("Identifiant"), 0, row);
        form.add(usernameField, 1, row++);
        if (initialSetup) {
            form.add(new Label("Nom affiché"), 0, row);
            form.add(displayNameField, 1, row++);
        }
        form.add(new Label("Mot de passe"), 0, row);
        form.add(passwordField, 1, row++);
        if (initialSetup) {
            form.add(new Label("Confirmation"), 0, row);
            form.add(confirmationField, 1, row);
        }

        VBox content = new VBox(10, title, explanation, form, errorLabel);
        content.setPadding(new Insets(8));
        content.setPrefWidth(450);
        content.setAlignment(Pos.CENTER_LEFT);
        dialog.getDialogPane().setContent(content);
        addApplicationStyles(dialog);
        dialog.setResultConverter(buttonType -> null);

        Button submitButton = (Button) dialog.getDialogPane().lookupButton(submitType);
        submitButton.addEventFilter(ActionEvent.ACTION, event -> {
            event.consume();
            try {
                UserAccount account;
                if (initialSetup) {
                    account = accountService.createInitialAdministrator(
                            usernameField.getText(),
                            displayNameField.getText(),
                            passwordField.getText().toCharArray(),
                            confirmationField.getText().toCharArray()
                    );
                } else {
                    account = accountService.authenticate(
                            usernameField.getText(),
                            passwordField.getText().toCharArray()
                    ).orElseThrow(() -> new IllegalArgumentException("Identifiant ou mot de passe incorrect."));
                    if (account.mustChangePassword()) {
                        Optional<UserAccount> updated = PasswordChangeDialog.show(
                                dialog.getDialogPane().getScene().getWindow(),
                                accountService,
                                account
                        );
                        if (updated.isEmpty()) {
                            errorLabel.setText("Le mot de passe temporaire doit être remplacé avant la connexion.");
                            return;
                        }
                        account = updated.get();
                    }
                }
                dialog.setResult(account);
                dialog.close();
            } catch (IllegalArgumentException | IllegalStateException exception) {
                errorLabel.setText(exception.getMessage());
                passwordField.clear();
                confirmationField.clear();
                passwordField.requestFocus();
            }
        });

        dialog.setOnShown(event -> (initialSetup ? displayNameField : usernameField).requestFocus());
        return dialog.showAndWait();
    }

    static void addApplicationStyles(Dialog<?> dialog) {
        var stylesheet = MainApplication.class.getResource("styles/application.css");
        if (stylesheet != null) {
            dialog.getDialogPane().getStylesheets().add(stylesheet.toExternalForm());
        }
        dialog.getDialogPane().getStyleClass().add("auth-dialog");
    }
}
