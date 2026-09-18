package com.example.siomanager;

import com.example.siomanager.model.ResourceNode;
import com.example.siomanager.repository.DemoResourceRepository;
import com.example.siomanager.service.LocalFileService;
import com.example.siomanager.view.DocumentSession;
import com.example.siomanager.view.ResourceDocumentFactory;
import com.example.siomanager.view.ResourceTreeCell;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.MouseButton;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;

public class MainController {
    private final DemoResourceRepository resourceRepository = new DemoResourceRepository();
    private final ResourceDocumentFactory documentFactory =
            new ResourceDocumentFactory(new LocalFileService());

    @FXML
    private TreeView<ResourceNode> resourceTree;

    @FXML
    private TabPane editorTabs;

    @FXML
    private TextArea consoleArea;

    @FXML
    private Label statusLabel;

    @FXML
    private void initialize() {
        resourceTree.setRoot(toTreeItem(resourceRepository.loadTree()));
        resourceTree.getRoot().setExpanded(true);
        resourceTree.setShowRoot(false);
        resourceTree.setCellFactory(tree -> new ResourceTreeCell());

        resourceTree.getSelectionModel().selectedItemProperty().addListener((observable, previous, selected) -> {
            if (selected != null) {
                ResourceNode resource = selected.getValue();
                statusLabel.setText(resource.type().name() + " • " + resource.name());
            }
        });

        resourceTree.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                TreeItem<ResourceNode> selected = resourceTree.getSelectionModel().getSelectedItem();
                if (selected != null && !selected.getValue().isContainer()) {
                    openSelectedResource();
                }
            }
        });

        writeConsole("SIOManager est prêt.");
    }

    private TreeItem<ResourceNode> toTreeItem(ResourceNode resource) {
        TreeItem<ResourceNode> item = new TreeItem<>(resource);
        resource.children().stream()
                .map(this::toTreeItem)
                .forEach(item.getChildren()::add);
        return item;
    }

    @FXML
    private void openSelectedResource() {
        TreeItem<ResourceNode> selection = resourceTree.getSelectionModel().getSelectedItem();
        if (selection == null) {
            return;
        }

        ResourceNode resource = selection.getValue();
        if (resource.isContainer()) {
            selection.setExpanded(!selection.isExpanded());
            return;
        }

        for (Tab tab : editorTabs.getTabs()) {
            if (tab.getUserData() instanceof DocumentSession session
                    && resource.id().equals(session.resource().id())) {
                editorTabs.getSelectionModel().select(tab);
                return;
            }
        }

        try {
            DocumentSession session = documentFactory.create(resource);
            Tab tab = new Tab(resource.name(), session.content());
            tab.setUserData(session);
            tab.setOnCloseRequest(event -> {
                if (!confirmClose(session)) {
                    event.consume();
                }
            });

            session.modifiedProperty().addListener((observable, previous, modified) ->
                    tab.setText(resource.name() + (modified ? " *" : ""))
            );

            editorTabs.getTabs().add(tab);
            editorTabs.getSelectionModel().select(tab);

            statusLabel.setText(resource.type().name() + " • " + resource.name());
            writeConsole("Ouverture : " + resource.localPath());
        } catch (IOException exception) {
            showError(
                    "Impossible d’ouvrir la ressource",
                    resource.name() + System.lineSeparator() + exception.getMessage()
            );
            writeConsole("Erreur d’ouverture : " + exception.getMessage());
        }
    }

    @FXML
    private void saveSelectedDocument() {
        Tab selectedTab = editorTabs.getSelectionModel().getSelectedItem();
        if (selectedTab == null || !(selectedTab.getUserData() instanceof DocumentSession session)) {
            writeConsole("Aucun document enregistrable n’est sélectionné.");
            return;
        }

        if (!session.canSave()) {
            writeConsole("Cette ressource est en lecture seule : " + session.resource().name());
            return;
        }

        saveSession(session);
    }

    @FXML
    private void clearConsole() {
        consoleArea.clear();
    }

    @FXML
    private void openSettings() {
        try {
            FXMLLoader loader = new FXMLLoader(MainApplication.class.getResource("settings-view.fxml"));
            Parent root = loader.load();

            Stage settingsStage = new Stage();
            Window owner = editorTabs.getScene().getWindow();
            settingsStage.initOwner(owner);
            settingsStage.initModality(Modality.WINDOW_MODAL);
            settingsStage.setTitle("Paramètres — SIOManager");
            settingsStage.setResizable(false);
            settingsStage.setScene(new Scene(root));
            settingsStage.showAndWait();
        } catch (IOException exception) {
            writeConsole("Impossible d’ouvrir les paramètres : " + exception.getMessage());
        }
    }

    @FXML
    private void closeApplication() {
        Stage stage = (Stage) editorTabs.getScene().getWindow();
        if (confirmCloseAll()) {
            stage.hide();
        }
    }

    public boolean confirmCloseAll() {
        for (Tab tab : new ArrayList<>(editorTabs.getTabs())) {
            if (tab.getUserData() instanceof DocumentSession session && !confirmClose(session)) {
                editorTabs.getSelectionModel().select(tab);
                return false;
            }
        }
        return true;
    }

    private boolean confirmClose(DocumentSession session) {
        if (!session.isModified()) {
            return true;
        }

        ButtonType saveButton = new ButtonType("Enregistrer", ButtonBar.ButtonData.YES);
        ButtonType discardButton = new ButtonType("Fermer sans enregistrer", ButtonBar.ButtonData.NO);
        ButtonType cancelButton = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);

        Alert alert = new Alert(
                Alert.AlertType.CONFIRMATION,
                "Le document « " + session.resource().name() + " » contient des modifications non enregistrées.",
                saveButton,
                discardButton,
                cancelButton
        );
        alert.initOwner(editorTabs.getScene().getWindow());
        alert.setTitle("Modifications non enregistrées");
        alert.setHeaderText("Enregistrer avant de fermer ?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isEmpty() || result.get() == cancelButton) {
            return false;
        }
        if (result.get() == saveButton) {
            return saveSession(session);
        }
        return true;
    }

    private boolean saveSession(DocumentSession session) {
        try {
            session.save();
            statusLabel.setText("Enregistré • " + session.resource().name());
            writeConsole("Enregistrement : " + session.resource().localPath());
            return true;
        } catch (IOException exception) {
            showError(
                    "Impossible d’enregistrer le fichier",
                    session.resource().name() + System.lineSeparator() + exception.getMessage()
            );
            writeConsole("Erreur d’enregistrement : " + exception.getMessage());
            return false;
        }
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.initOwner(editorTabs.getScene().getWindow());
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void writeConsole(String message) {
        consoleArea.appendText("> " + message + System.lineSeparator());
    }
}
