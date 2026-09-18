package com.example.siomanager;

import com.example.siomanager.model.ResourceNode;
import com.example.siomanager.model.ResourceType;
import com.example.siomanager.repository.LocalResourceRepository;
import com.example.siomanager.service.CodeExecutionService;
import com.example.siomanager.service.DemoContentInitializer;
import com.example.siomanager.service.LocalFileService;
import com.example.siomanager.service.ResourceCreationService;
import com.example.siomanager.view.DocumentSession;
import com.example.siomanager.view.ResourceCreationDialog;
import com.example.siomanager.view.ResourceDocumentFactory;
import com.example.siomanager.view.ResourceTreeCell;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.geometry.Rectangle2D;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.prefs.Preferences;

public class MainController {
    private static final double DEFAULT_UI_FONT_SIZE = 12.0;
    private static final double MIN_UI_FONT_SIZE = 10.0;
    private static final double MAX_UI_FONT_SIZE = 16.0;
    private static final double DEFAULT_EXPLORER_POSITION = 0.205;
    private static final double DEFAULT_BOTTOM_POSITION = 0.76;

    private final LocalResourceRepository resourceRepository = new LocalResourceRepository();
    private final DemoContentInitializer demoContentInitializer = new DemoContentInitializer();
    private final CodeExecutionService executionService = new CodeExecutionService();
    private final ResourceCreationService resourceCreationService = new ResourceCreationService();
    private final ResourceDocumentFactory documentFactory =
            new ResourceDocumentFactory(new LocalFileService());
    private final Preferences preferences = Preferences.userNodeForPackage(MainController.class);

    private Stage stage;
    private double uiFontSize;
    private double explorerDividerPosition;
    private double bottomDividerPosition;

    @FXML
    private BorderPane rootPane;

    @FXML
    private SplitPane mainSplitPane;

    @FXML
    private SplitPane workspaceSplitPane;

    @FXML
    private HBox sidebarContainer;

    @FXML
    private CheckMenuItem explorerVisibilityItem;

    @FXML
    private CheckMenuItem bottomPanelVisibilityItem;

    @FXML
    private TreeView<ResourceNode> resourceTree;

    @FXML
    private TabPane editorTabs;

    @FXML
    private TabPane bottomTabs;

    @FXML
    private Tab outputTab;

    @FXML
    private TextArea consoleArea;

    @FXML
    private TextArea outputArea;

    @FXML
    private Label statusLabel;

    @FXML
    private Label zoomLabel;

    @FXML
    private void initialize() {
        uiFontSize = clamp(
                preferences.getDouble("ui.fontSize", DEFAULT_UI_FONT_SIZE),
                MIN_UI_FONT_SIZE,
                MAX_UI_FONT_SIZE
        );
        explorerDividerPosition = clamp(
                preferences.getDouble("layout.explorer", DEFAULT_EXPLORER_POSITION),
                0.12,
                0.45
        );
        bottomDividerPosition = clamp(
                preferences.getDouble("layout.bottom", DEFAULT_BOTTOM_POSITION),
                0.45,
                0.9
        );
        applyUiFontSize();

        try {
            demoContentInitializer.ensurePdfSamples(resourceRepository.contentRoot());
        } catch (IOException exception) {
            writeConsole("Les PDF de démonstration n’ont pas pu être créés : " + exception.getMessage());
        }

        resourceTree.setShowRoot(false);
        resourceTree.setCellFactory(tree -> new ResourceTreeCell());
        configureResourceContextMenu();
        reloadResourceTree(null);

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

        Platform.runLater(() -> {
            setExplorerVisible(preferences.getBoolean("layout.explorerVisible", true));
            setBottomPanelVisible(preferences.getBoolean("layout.bottomVisible", true));
            applyDividerPositions();
        });
    }

    public void attachStage(Stage stage) {
        this.stage = stage;
        Rectangle2D visualBounds = Screen.getPrimary().getVisualBounds();
        double savedWidth = preferences.getDouble("window.width", 1180);
        double savedHeight = preferences.getDouble("window.height", 720);
        stage.setWidth(clamp(savedWidth, stage.getMinWidth(), visualBounds.getWidth()));
        stage.setHeight(clamp(savedHeight, stage.getMinHeight(), visualBounds.getHeight()));
        stage.setMaximized(preferences.getBoolean("window.maximized", false));
        stage.getScene().addEventFilter(KeyEvent.KEY_PRESSED, this::handleGlobalShortcut);
    }

    @FXML
    private void toggleExplorer() {
        setExplorerVisible(explorerVisibilityItem.isSelected());
    }

    @FXML
    private void toggleExplorerFromButton() {
        boolean visible = !mainSplitPane.getItems().contains(sidebarContainer);
        explorerVisibilityItem.setSelected(visible);
        setExplorerVisible(visible);
    }

    @FXML
    private void toggleBottomPanel() {
        setBottomPanelVisible(bottomPanelVisibilityItem.isSelected());
    }

    @FXML
    private void zoomIn() {
        setUiFontSize(uiFontSize + 1);
    }

    @FXML
    private void zoomOut() {
        setUiFontSize(uiFontSize - 1);
    }

    @FXML
    private void resetZoom() {
        setUiFontSize(DEFAULT_UI_FONT_SIZE);
    }

    @FXML
    private void resetLayout() {
        explorerDividerPosition = DEFAULT_EXPLORER_POSITION;
        bottomDividerPosition = DEFAULT_BOTTOM_POSITION;
        explorerVisibilityItem.setSelected(true);
        bottomPanelVisibilityItem.setSelected(true);
        setExplorerVisible(true);
        setBottomPanelVisible(true);
        resetZoom();
        Platform.runLater(this::applyDividerPositions);
        statusLabel.setText("Disposition réinitialisée");
    }

    private TreeItem<ResourceNode> toTreeItem(ResourceNode resource) {
        TreeItem<ResourceNode> item = new TreeItem<>(resource);
        resource.children().stream()
                .map(this::toTreeItem)
                .forEach(item.getChildren()::add);
        return item;
    }

    @FXML
    private void refreshResources() {
        TreeItem<ResourceNode> selected = resourceTree.getSelectionModel().getSelectedItem();
        reloadResourceTree(selected == null ? null : selected.getValue().id());
        statusLabel.setText("Ressources actualisées");
    }

    @FXML
    private void createResource() {
        TreeItem<ResourceNode> selected = resourceTree.getSelectionModel().getSelectedItem();
        TreeItem<ResourceNode> destination = selected;
        if (destination == null) {
            destination = resourceTree.getRoot();
        } else if (!destination.getValue().isContainer()) {
            destination = destination.getParent();
        }

        ResourceNode destinationResource = destination.getValue();
        Path destinationPath = resourceRepository.pathFor(destinationResource);
        Optional<ResourceCreationService.CreationRequest> request = ResourceCreationDialog.show(
                editorTabs.getScene().getWindow(),
                destinationResource.name()
        );
        if (request.isEmpty()) {
            return;
        }

        try {
            Path created = resourceCreationService.create(
                    resourceRepository.contentRoot(),
                    destinationPath,
                    request.get().name(),
                    request.get().kind()
            );
            reloadResourceTree(resourceRepository.idFor(created));
            TreeItem<ResourceNode> createdItem = resourceTree.getSelectionModel().getSelectedItem();
            if (createdItem != null) {
                if (createdItem.getValue().isContainer()) {
                    createdItem.setExpanded(true);
                } else {
                    openSelectedResource();
                }
            }
            statusLabel.setText("Créé • " + created.getFileName());
            writeConsole("Création : " + created);
        } catch (IOException | IllegalArgumentException exception) {
            showError("Impossible de créer la ressource", exception.getMessage());
            writeConsole("Erreur de création : " + exception.getMessage());
        }
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
            tab.setOnClosed(event -> session.close());

            session.modifiedProperty().addListener((observable, previous, modified) ->
                    tab.setText(resource.name() + (modified ? " *" : ""))
            );

            editorTabs.getTabs().add(tab);
            editorTabs.getSelectionModel().select(tab);
            applyContentZoom(session.content());

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
    private void clearOutput() {
        outputArea.clear();
    }

    @FXML
    private void runSelectedDocument() {
        Tab selectedTab = editorTabs.getSelectionModel().getSelectedItem();
        if (selectedTab == null || !(selectedTab.getUserData() instanceof DocumentSession session)) {
            writeConsole("Sélectionne d’abord un fichier de code.");
            return;
        }
        if (session.resource().type() != ResourceType.SOURCE_CODE) {
            writeConsole("La ressource sélectionnée n’est pas un fichier de code exécutable.");
            return;
        }
        if (session.isModified() && !saveSession(session)) {
            return;
        }

        try {
            if (!workspaceSplitPane.getItems().contains(bottomTabs)) {
                bottomPanelVisibilityItem.setSelected(true);
                setBottomPanelVisible(true);
            }
            bottomTabs.getSelectionModel().select(outputTab);
            outputArea.appendText(System.lineSeparator() + "——— " + session.resource().name() + " ———"
                    + System.lineSeparator());
            executionService.execute(
                    session.resource(),
                    line -> Platform.runLater(() -> outputArea.appendText(line + System.lineSeparator())),
                    result -> Platform.runLater(() -> {
                        outputArea.appendText(result.message() + System.lineSeparator());
                        statusLabel.setText(result.cancelled() ? "Exécution arrêtée" : result.message());
                    })
            );
            statusLabel.setText("Exécution • " + session.resource().name());
            writeConsole("Lancement : " + session.resource().localPath());
        } catch (IOException | IllegalArgumentException | IllegalStateException exception) {
            showError("Impossible d’exécuter ce fichier", exception.getMessage());
            writeConsole("Erreur d’exécution : " + exception.getMessage());
        }
    }

    @FXML
    private void stopExecution() {
        if (!executionService.isRunning()) {
            writeConsole("Aucun programme n’est en cours d’exécution.");
            return;
        }
        executionService.stop();
        writeConsole("Arrêt du programme demandé.");
    }

    @FXML
    private void openSettings() {
        try {
            FXMLLoader loader = new FXMLLoader(MainApplication.class.getResource("settings-view.fxml"));
            Parent root = loader.load();
            root.setStyle("-fx-font-size: " + uiFontSize + "px;");

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

    public boolean confirmCloseAll() {
        for (Tab tab : new ArrayList<>(editorTabs.getTabs())) {
            if (tab.getUserData() instanceof DocumentSession session && !confirmClose(session)) {
                editorTabs.getSelectionModel().select(tab);
                return false;
            }
        }
        return true;
    }

    public void dispose() {
        saveWorkspacePreferences();
        executionService.close();
        for (Tab tab : editorTabs.getTabs()) {
            if (tab.getUserData() instanceof DocumentSession session) {
                session.close();
            }
        }
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

    private void configureResourceContextMenu() {
        MenuItem createItem = new MenuItem("Nouvelle ressource…");
        createItem.setOnAction(event -> createResource());
        MenuItem refreshItem = new MenuItem("Actualiser");
        refreshItem.setOnAction(event -> refreshResources());
        resourceTree.setContextMenu(new ContextMenu(createItem, refreshItem));
    }

    private void reloadResourceTree(String selectedId) {
        Set<String> expandedIds = new HashSet<>();
        collectExpandedIds(resourceTree.getRoot(), expandedIds);

        try {
            TreeItem<ResourceNode> root = toTreeItem(resourceRepository.loadTree());
            root.setExpanded(true);
            if (expandedIds.isEmpty()) {
                root.getChildren().forEach(section -> section.setExpanded(true));
            } else {
                restoreExpandedState(root, expandedIds);
            }
            resourceTree.setRoot(root);

            if (selectedId != null) {
                TreeItem<ResourceNode> selected = findTreeItem(root, selectedId);
                if (selected != null) {
                    expandParents(selected);
                    resourceTree.getSelectionModel().select(selected);
                    resourceTree.scrollTo(resourceTree.getRow(selected));
                }
            }
        } catch (IOException exception) {
            writeConsole("Impossible d’actualiser les ressources : " + exception.getMessage());
        }
    }

    private void collectExpandedIds(TreeItem<ResourceNode> item, Set<String> expandedIds) {
        if (item == null) {
            return;
        }
        if (item.isExpanded()) {
            expandedIds.add(item.getValue().id());
        }
        item.getChildren().forEach(child -> collectExpandedIds(child, expandedIds));
    }

    private void restoreExpandedState(TreeItem<ResourceNode> item, Set<String> expandedIds) {
        item.setExpanded(expandedIds.contains(item.getValue().id()));
        item.getChildren().forEach(child -> restoreExpandedState(child, expandedIds));
    }

    private TreeItem<ResourceNode> findTreeItem(TreeItem<ResourceNode> item, String id) {
        if (item.getValue().id().equals(id)) {
            return item;
        }
        for (TreeItem<ResourceNode> child : item.getChildren()) {
            TreeItem<ResourceNode> found = findTreeItem(child, id);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private void expandParents(TreeItem<ResourceNode> item) {
        TreeItem<ResourceNode> parent = item.getParent();
        while (parent != null) {
            parent.setExpanded(true);
            parent = parent.getParent();
        }
    }

    private void setExplorerVisible(boolean visible) {
        boolean currentlyVisible = mainSplitPane.getItems().contains(sidebarContainer);
        explorerVisibilityItem.setSelected(visible);
        if (visible == currentlyVisible) {
            if (visible) {
                Platform.runLater(() -> mainSplitPane.setDividerPosition(0, explorerDividerPosition));
            }
            return;
        }

        if (visible) {
            mainSplitPane.getItems().add(0, sidebarContainer);
            Platform.runLater(() -> mainSplitPane.setDividerPosition(0, explorerDividerPosition));
        } else {
            rememberExplorerPosition();
            mainSplitPane.getItems().remove(sidebarContainer);
        }
    }

    private void setBottomPanelVisible(boolean visible) {
        boolean currentlyVisible = workspaceSplitPane.getItems().contains(bottomTabs);
        bottomPanelVisibilityItem.setSelected(visible);
        if (visible == currentlyVisible) {
            if (visible) {
                Platform.runLater(() -> workspaceSplitPane.setDividerPosition(0, bottomDividerPosition));
            }
            return;
        }

        if (visible) {
            workspaceSplitPane.getItems().add(bottomTabs);
            Platform.runLater(() -> workspaceSplitPane.setDividerPosition(0, bottomDividerPosition));
        } else {
            rememberBottomPosition();
            workspaceSplitPane.getItems().remove(bottomTabs);
        }
    }

    private void setUiFontSize(double requestedSize) {
        uiFontSize = clamp(requestedSize, MIN_UI_FONT_SIZE, MAX_UI_FONT_SIZE);
        applyUiFontSize();
    }

    private void applyUiFontSize() {
        rootPane.setStyle("-fx-font-size: " + uiFontSize + "px;");
        zoomLabel.setText(Math.round(uiFontSize / DEFAULT_UI_FONT_SIZE * 100) + " %");
        for (Tab tab : editorTabs.getTabs()) {
            if (tab.getContent() != null) {
                applyContentZoom(tab.getContent());
            }
        }
    }

    private void applyContentZoom(Node node) {
        if (node instanceof WebView webView) {
            webView.setZoom(uiFontSize / DEFAULT_UI_FONT_SIZE);
        }
        if (node instanceof Parent parent) {
            parent.getChildrenUnmodifiable().forEach(this::applyContentZoom);
        }
    }

    private void applyDividerPositions() {
        if (mainSplitPane.getItems().size() == 2) {
            mainSplitPane.setDividerPosition(0, explorerDividerPosition);
        }
        if (workspaceSplitPane.getItems().size() == 2) {
            workspaceSplitPane.setDividerPosition(0, bottomDividerPosition);
        }
    }

    private void rememberExplorerPosition() {
        if (!mainSplitPane.getDividers().isEmpty()) {
            explorerDividerPosition = mainSplitPane.getDividers().getFirst().getPosition();
        }
    }

    private void rememberBottomPosition() {
        if (!workspaceSplitPane.getDividers().isEmpty()) {
            bottomDividerPosition = workspaceSplitPane.getDividers().getFirst().getPosition();
        }
    }

    private void handleGlobalShortcut(KeyEvent event) {
        if (!event.isShortcutDown()) {
            return;
        }

        KeyCode code = event.getCode();
        if (code == KeyCode.B) {
            toggleExplorerFromButton();
        } else if (code == KeyCode.J) {
            boolean visible = !workspaceSplitPane.getItems().contains(bottomTabs);
            bottomPanelVisibilityItem.setSelected(visible);
            setBottomPanelVisible(visible);
        } else if (code == KeyCode.MINUS || code == KeyCode.SUBTRACT) {
            zoomOut();
        } else if (code == KeyCode.PLUS || code == KeyCode.ADD || code == KeyCode.EQUALS) {
            zoomIn();
        } else if (code == KeyCode.DIGIT0 || code == KeyCode.NUMPAD0) {
            resetZoom();
        } else {
            return;
        }
        event.consume();
    }

    private void saveWorkspacePreferences() {
        rememberExplorerPosition();
        rememberBottomPosition();
        preferences.putDouble("ui.fontSize", uiFontSize);
        preferences.putDouble("layout.explorer", explorerDividerPosition);
        preferences.putDouble("layout.bottom", bottomDividerPosition);
        preferences.putBoolean("layout.explorerVisible", mainSplitPane.getItems().contains(sidebarContainer));
        preferences.putBoolean("layout.bottomVisible", workspaceSplitPane.getItems().contains(bottomTabs));

        if (stage != null) {
            preferences.putBoolean("window.maximized", stage.isMaximized());
            if (!stage.isMaximized()) {
                preferences.putDouble("window.width", stage.getWidth());
                preferences.putDouble("window.height", stage.getHeight());
            }
        }
    }

    private double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
