package com.example.siomanager;

import com.example.siomanager.model.ResourceNode;
import com.example.siomanager.repository.DemoResourceRepository;
import com.example.siomanager.view.ResourceDocumentFactory;
import com.example.siomanager.view.ResourceTreeCell;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
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

public class MainController {
    private final DemoResourceRepository resourceRepository = new DemoResourceRepository();

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
            if (resource.id().equals(tab.getUserData())) {
                editorTabs.getSelectionModel().select(tab);
                return;
            }
        }

        Tab tab = new Tab(resource.name(), ResourceDocumentFactory.create(resource));
        tab.setUserData(resource.id());
        editorTabs.getTabs().add(tab);
        editorTabs.getSelectionModel().select(tab);

        statusLabel.setText(resource.type().name() + " • " + resource.name());
        writeConsole("Ouverture : " + resource.name());
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
        stage.close();
    }

    private void writeConsole(String message) {
        consoleArea.appendText("> " + message + System.lineSeparator());
    }
}
