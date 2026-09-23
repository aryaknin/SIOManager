package com.example.siomanager;

import com.example.siomanager.model.UserAccount;
import com.example.siomanager.service.BackupService;
import com.example.siomanager.service.AccountService;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.event.ActionEvent;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Optional;

public class SettingsController {
    private BackupService backupService;
    private AccountService accountService;
    private UserAccount currentUser;
    private Runnable restartHandler;

    @FXML private VBox backupSection;
    @FXML private Label backupFeedbackLabel;

    public void configure(
            BackupService backupService,
            AccountService accountService,
            UserAccount currentUser,
            Runnable restartHandler
    ) {
        this.backupService = backupService;
        this.accountService = accountService;
        this.currentUser = currentUser;
        this.restartHandler = restartHandler;
        backupSection.setVisible(currentUser.isAdmin());
        backupSection.setManaged(currentUser.isAdmin());
    }

    @FXML
    private void closeWindow(ActionEvent event) {
        Node source = (Node) event.getSource();
        Stage stage = (Stage) source.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void saveAndClose(ActionEvent event) {
        Node source = (Node) event.getSource();
        Stage stage = (Stage) source.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void createBackup() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Choisir le dossier de sauvegarde");
        java.io.File directory = chooser.showDialog(backupFeedbackLabel.getScene().getWindow());
        if (directory == null) {
            return;
        }
        try {
            Path archive = backupService.createBackup(directory.toPath());
            accountService.recordAdminAction(currentUser, "BACKUP_CREATED", archive.toString());
            backupFeedbackLabel.setText("Sauvegarde créée : " + archive);
        } catch (IOException | SQLException | IllegalStateException exception) {
            backupFeedbackLabel.setText("Échec de la sauvegarde : " + exception.getMessage());
        }
    }

    @FXML
    private void restoreBackup() {
        if (currentUser == null || !currentUser.isAdmin()) {
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Restaurer une sauvegarde SIOManager");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Sauvegarde ZIP", "*.zip"));
        java.io.File archive = chooser.showOpenDialog(backupFeedbackLabel.getScene().getWindow());
        if (archive == null) {
            return;
        }

        Alert confirmation = new Alert(
                Alert.AlertType.CONFIRMATION,
                "La base locale et les fichiers présents dans la sauvegarde remplaceront les données actuelles. "
                        + "SIOManager reviendra ensuite à l’écran de connexion.",
                ButtonType.OK,
                ButtonType.CANCEL
        );
        confirmation.initOwner(backupFeedbackLabel.getScene().getWindow());
        confirmation.setTitle("Restaurer la sauvegarde");
        confirmation.setHeaderText("Confirmer la restauration ?");
        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }

        try {
            backupService.restoreBackup(archive.toPath());
            ((Stage) backupFeedbackLabel.getScene().getWindow()).close();
            restartHandler.run();
        } catch (IOException | IllegalArgumentException exception) {
            backupFeedbackLabel.setText("Échec de la restauration : " + exception.getMessage());
        }
    }
}
