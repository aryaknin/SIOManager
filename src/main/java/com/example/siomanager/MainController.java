package com.example.siomanager;

import com.example.siomanager.model.ResourceNode;
import com.example.siomanager.model.ResourceType;
import com.example.siomanager.model.UserAccount;
import com.example.siomanager.model.ResourceMetadata;
import com.example.siomanager.repository.LocalResourceRepository;
import com.example.siomanager.repository.PersonalWorkspaceRepository;
import com.example.siomanager.service.AccountService;
import com.example.siomanager.service.BackupService;
import com.example.siomanager.service.CodeExecutionService;
import com.example.siomanager.service.DemoContentInitializer;
import com.example.siomanager.service.LocalFileService;
import com.example.siomanager.service.ResourceCreationService;
import com.example.siomanager.service.ResourceManagementService;
import com.example.siomanager.service.ResourceCatalogService;
import com.example.siomanager.service.TrashService;
import com.example.siomanager.view.DocumentSession;
import com.example.siomanager.view.ResourceCreationDialog;
import com.example.siomanager.view.ResourceDocumentFactory;
import com.example.siomanager.view.ResourceTreeCell;
import com.example.siomanager.view.PasswordChangeDialog;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuButton;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.TextField;
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
import javafx.stage.FileChooser;
import javafx.stage.DirectoryChooser;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
    private final ResourceManagementService resourceManagementService = new ResourceManagementService();
    private final ResourceDocumentFactory documentFactory =
            new ResourceDocumentFactory(new LocalFileService());
    private final Preferences preferences = Preferences.userNodeForPackage(MainController.class);

    private Stage stage;
    private AccountService accountService;
    private ResourceCatalogService catalogService;
    private TrashService trashService;
    private BackupService backupService;
    private UserAccount currentUser;
    private PersonalWorkspaceRepository personalWorkspaceRepository;
    private MenuItem contextCreateItem;
    private Runnable logoutHandler;
    private ResourceNode completeResourceTree;
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
    private MenuButton accountMenuButton;

    @FXML
    private MenuItem accountRoleMenuItem;

    @FXML
    private Button createResourceButton;

    @FXML
    private Button uploadResourceButton;

    @FXML
    private Button runResourceButton;

    @FXML
    private Button administrationButton;

    @FXML
    private MenuItem createResourceMenuItem;

    @FXML
    private MenuItem uploadResourceMenuItem;

    @FXML
    private MenuItem runResourceMenuItem;

    @FXML
    private Menu administrationMenu;

    @FXML
    private TextField searchField;

    @FXML
    private ListView<ResourceNode> favoriteList;

    @FXML
    private ListView<ResourceNode> recentList;

    @FXML
    private Label welcomeUserLabel;

    @FXML
    private Label dashboardProgressLabel;

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

        searchField.textProperty().addListener((observable, previous, current) -> applyResourceFilter());
        configureDashboardList(favoriteList);
        configureDashboardList(recentList);

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

    public void configureSecurity(
            AccountService accountService,
            ResourceCatalogService catalogService,
            TrashService trashService,
            BackupService backupService,
            UserAccount currentUser,
            Path personalWorkspacePath,
            Runnable logoutHandler
    ) {
        this.accountService = accountService;
        this.catalogService = catalogService;
        this.trashService = trashService;
        this.backupService = backupService;
        this.currentUser = currentUser;
        this.personalWorkspaceRepository = new PersonalWorkspaceRepository(personalWorkspacePath);
        this.logoutHandler = logoutHandler;
        boolean administrator = currentUser.isAdmin();

        accountMenuButton.setText(currentUser.displayName());
        accountRoleMenuItem.setText(currentUser.username() + " • " + currentUser.role().displayName());
        welcomeUserLabel.setText("Bonjour " + currentUser.displayName());
        setManagedVisible(createResourceButton, true);
        setManagedVisible(uploadResourceButton, true);
        setManagedVisible(runResourceButton, true);
        setManagedVisible(administrationButton, administrator);
        createResourceMenuItem.setVisible(true);
        uploadResourceMenuItem.setVisible(true);
        runResourceMenuItem.setVisible(true);
        administrationMenu.setVisible(administrator);
        if (contextCreateItem != null) {
            contextCreateItem.setVisible(true);
        }
        reloadResourceTree(null);
        writeConsole("Session ouverte : " + currentUser.displayName() + " (" + currentUser.role().displayName() + ").");
        if (!administrator) {
            writeConsole("Les ressources communes sont en lecture seule pour les comptes élèves.");
        }
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
            destination = currentUser != null && !currentUser.isAdmin()
                    ? findTreeItem(resourceTree.getRoot(), "personal")
                    : resourceTree.getRoot();
        } else if (!destination.getValue().isContainer()) {
            destination = destination.getParent();
        }

        ResourceNode destinationResource = destination.getValue();
        if (!canModify(destinationResource)) {
            writeConsole("Accès refusé : les ressources communes sont en lecture seule pour les élèves.");
            return;
        }
        Path destinationPath = pathFor(destinationResource);
        Optional<ResourceCreationService.CreationRequest> request = ResourceCreationDialog.show(
                editorTabs.getScene().getWindow(),
                destinationResource.name()
        );
        if (request.isEmpty()) {
            return;
        }

        try {
            Path created = resourceCreationService.create(
                    rootFor(destinationResource),
                    destinationPath,
                    request.get().name(),
                    request.get().kind()
            );
            String createdId = idFor(created, destinationResource);
            reloadResourceTree(createdId);
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
            recordResourceAction("RESOURCE_CREATED", createdId);
        } catch (IOException | IllegalArgumentException exception) {
            showError("Impossible de créer la ressource", exception.getMessage());
            writeConsole("Erreur de création : " + exception.getMessage());
        }
    }

    @FXML
    private void uploadResource() {
        TreeItem<ResourceNode> selected = resourceTree.getSelectionModel().getSelectedItem();
        TreeItem<ResourceNode> destination = selected == null
                ? (currentUser != null && !currentUser.isAdmin()
                    ? findTreeItem(resourceTree.getRoot(), "personal")
                    : resourceTree.getRoot())
                : selected;
        if (!destination.getValue().isContainer()) {
            destination = destination.getParent();
        }
        ResourceNode destinationResource = destination.getValue();
        if (!canModify(destinationResource)) {
            writeConsole("Accès refusé : l’import dans les ressources communes est réservé aux administrateurs.");
            return;
        }
        Path destinationPath = pathFor(destinationResource);

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Importer une ressource dans « " + destination.getValue().name() + " »");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Ressources prises en charge", "*.md", "*.pdf", "*.java", "*.html", "*.css", "*.js", "*.sql", "*.sh", "*.py", "*.txt"),
                new FileChooser.ExtensionFilter("Tous les fichiers", "*.*")
        );
        java.io.File selectedFile = chooser.showOpenDialog(editorTabs.getScene().getWindow());
        if (selectedFile == null) {
            return;
        }

        Path source = selectedFile.toPath().toAbsolutePath().normalize();
        Path target = destinationPath.resolve(source.getFileName()).normalize();
        if (!target.startsWith(rootFor(destinationResource))) {
            showError("Import refusé", "Le fichier sortirait du dossier de ressources autorisé.");
            return;
        }
        if (Files.exists(target)) {
            showError("Import impossible", "Un fichier nommé « " + source.getFileName() + " » existe déjà dans ce dossier.");
            return;
        }

        try {
            Files.copy(source, target);
            String id = idFor(target, destinationResource);
            reloadResourceTree(id);
            recordResourceAction("RESOURCE_UPLOADED", id);
            statusLabel.setText("Importé • " + source.getFileName());
            writeConsole("Import : " + target);
        } catch (IOException | IllegalStateException exception) {
            showError("Impossible d’importer la ressource", exception.getMessage());
            writeConsole("Erreur d’import : " + exception.getMessage());
        }
    }

    @FXML
    private void importDirectory() {
        TreeItem<ResourceNode> selected = resourceTree.getSelectionModel().getSelectedItem();
        TreeItem<ResourceNode> destination = selected == null
                ? (currentUser != null && !currentUser.isAdmin()
                    ? findTreeItem(resourceTree.getRoot(), "personal")
                    : resourceTree.getRoot())
                : selected;
        if (!destination.getValue().isContainer()) {
            destination = destination.getParent();
        }
        ResourceNode destinationResource = destination.getValue();
        if (!canModify(destinationResource)) {
            writeConsole("Accès refusé : ce dossier est en lecture seule.");
            return;
        }
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Importer un dossier dans « " + destinationResource.name() + " »");
        java.io.File selectedDirectory = chooser.showDialog(editorTabs.getScene().getWindow());
        if (selectedDirectory == null) {
            return;
        }
        try {
            Path imported = resourceManagementService.importDirectory(
                    rootFor(destinationResource),
                    pathFor(destinationResource),
                    selectedDirectory.toPath()
            );
            String id = idFor(imported, destinationResource);
            reloadResourceTree(id);
            recordResourceAction("DIRECTORY_IMPORTED", id);
            statusLabel.setText("Dossier importé • " + imported.getFileName());
        } catch (IOException | IllegalArgumentException exception) {
            showError("Import du dossier impossible", exception.getMessage());
        }
    }

    @FXML
    private void downloadResource() {
        TreeItem<ResourceNode> selected = resourceTree.getSelectionModel().getSelectedItem();
        if (selected == null || selected.getValue().isContainer()) {
            writeConsole("Sélectionne un fichier à télécharger.");
            return;
        }
        ResourceNode resource = selected.getValue();
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Télécharger une copie de « " + resource.name() + " »");
        chooser.setInitialFileName(resource.localPath().getFileName().toString());
        java.io.File targetFile = chooser.showSaveDialog(editorTabs.getScene().getWindow());
        if (targetFile == null) {
            return;
        }
        try {
            Files.copy(resource.localPath(), targetFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            recordResourceAction("RESOURCE_DOWNLOADED", resource.id());
            statusLabel.setText("Copie enregistrée • " + targetFile.getName());
            writeConsole("Téléchargement : " + targetFile);
        } catch (IOException | IllegalStateException exception) {
            showError("Téléchargement impossible", exception.getMessage());
        }
    }

    @FXML
    private void renameSelectedResource() {
        TreeItem<ResourceNode> selected = resourceTree.getSelectionModel().getSelectedItem();
        if (selected == null || isProtectedRoot(selected.getValue())) {
            writeConsole("Cette racine ne peut pas être renommée.");
            return;
        }
        ResourceNode resource = selected.getValue();
        if (!canModify(resource)) {
            writeConsole("Accès refusé : cette ressource est en lecture seule.");
            return;
        }
        Path source = pathFor(resource);
        if (isOpen(source)) {
            showError("Renommage impossible", "Ferme d’abord les documents ouverts contenus dans cette ressource.");
            return;
        }

        TextInputDialog dialog = new TextInputDialog(source.getFileName().toString());
        dialog.initOwner(editorTabs.getScene().getWindow());
        dialog.setTitle("Renommer — SIOManager");
        dialog.setHeaderText("Renommer « " + resource.name() + " »");
        dialog.setContentText("Nouveau nom :");
        Optional<String> newName = dialog.showAndWait();
        if (newName.isEmpty()) {
            return;
        }
        try {
            Path renamed = resourceManagementService.rename(rootFor(resource), source, newName.get());
            String id = idFor(renamed, resource);
            reloadResourceTree(id);
            recordResourceAction("RESOURCE_RENAMED", resource.id() + " -> " + id);
            statusLabel.setText("Renommé • " + renamed.getFileName());
        } catch (IOException | IllegalArgumentException | IllegalStateException exception) {
            showError("Renommage impossible", exception.getMessage());
        }
    }

    @FXML
    private void deleteSelectedResource() {
        TreeItem<ResourceNode> selected = resourceTree.getSelectionModel().getSelectedItem();
        if (selected == null || isProtectedRoot(selected.getValue())) {
            writeConsole("Cette racine ne peut pas être supprimée.");
            return;
        }
        ResourceNode resource = selected.getValue();
        if (!canModify(resource)) {
            writeConsole("Accès refusé : cette ressource est en lecture seule.");
            return;
        }
        Path target = pathFor(resource);
        if (isOpen(target)) {
            showError("Suppression impossible", "Ferme d’abord les documents ouverts contenus dans cette ressource.");
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.initOwner(editorTabs.getScene().getWindow());
        confirmation.setTitle("Mettre à la corbeille — SIOManager");
        confirmation.setHeaderText("Déplacer « " + resource.name() + " » dans la corbeille ?");
        confirmation.setContentText(resource.isContainer()
                ? "Le dossier et tout son contenu pourront être restaurés par un administrateur."
                : "Le fichier pourra être restauré par un administrateur.");
        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }

        try {
            trashService.moveToTrash(currentUser, resource, target);
            reloadResourceTree(null);
            recordResourceAction("RESOURCE_TRASHED", resource.id());
            statusLabel.setText("Déplacé dans la corbeille • " + resource.name());
        } catch (IOException | IllegalArgumentException | IllegalStateException exception) {
            showError("Suppression impossible", exception.getMessage());
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
            DocumentSession session = documentFactory.create(resource, canModify(resource));
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
            recordOpened(resource);
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
        if (!canModify(session.resource())) {
            writeConsole("Les élèves peuvent exécuter uniquement les fichiers de leur espace personnel.");
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
            SettingsController controller = loader.getController();
            controller.configure(backupService, accountService, currentUser, this::logoutAfterRestore);

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
    private void changeOwnPassword() {
        PasswordChangeDialog.show(
                editorTabs.getScene().getWindow(),
                accountService,
                currentUser
        ).ifPresent(updated -> {
            currentUser = updated;
            accountMenuButton.setText(updated.displayName());
            statusLabel.setText("Mot de passe modifié");
        });
    }

    @FXML
    private void logout() {
        if (!confirmCloseAll()) {
            return;
        }
        dispose();
        logoutHandler.run();
    }

    private void logoutAfterRestore() {
        dispose();
        logoutHandler.run();
    }

    @FXML
    private void openAdministration() {
        if (!requireAdministrator("ouvrir l’administration")) {
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(MainApplication.class.getResource("admin-view.fxml"));
            Parent root = loader.load();
            root.setStyle("-fx-font-size: " + uiFontSize + "px;");
            AdminController controller = loader.getController();
            controller.configure(accountService, catalogService, trashService, currentUser, this::reloadAfterAdmin);

            Stage adminStage = new Stage();
            adminStage.initOwner(editorTabs.getScene().getWindow());
            adminStage.initModality(Modality.WINDOW_MODAL);
            adminStage.setTitle("Administration — SIOManager");
            adminStage.setMinWidth(820);
            adminStage.setMinHeight(540);
            adminStage.setScene(new Scene(root));
            adminStage.showAndWait();
        } catch (IOException | IllegalStateException | SecurityException exception) {
            showError("Administration indisponible", exception.getMessage());
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
        contextCreateItem = new MenuItem("Nouvelle ressource…");
        contextCreateItem.setOnAction(event -> createResource());
        MenuItem renameItem = new MenuItem("Renommer…");
        renameItem.setOnAction(event -> renameSelectedResource());
        MenuItem deleteItem = new MenuItem("Supprimer…");
        deleteItem.setOnAction(event -> deleteSelectedResource());
        MenuItem downloadItem = new MenuItem("Télécharger une copie…");
        downloadItem.setOnAction(event -> downloadResource());
        MenuItem favoriteItem = new MenuItem("Ajouter/retirer des favoris");
        favoriteItem.setOnAction(event -> toggleSelectedFavorite());
        MenuItem completeItem = new MenuItem("Marquer comme terminé");
        completeItem.setOnAction(event -> markSelectedComplete());
        MenuItem importDirectoryItem = new MenuItem("Importer un dossier…");
        importDirectoryItem.setOnAction(event -> importDirectory());
        MenuItem refreshItem = new MenuItem("Actualiser");
        refreshItem.setOnAction(event -> refreshResources());
        resourceTree.setContextMenu(new ContextMenu(
                contextCreateItem, importDirectoryItem, renameItem, deleteItem,
                downloadItem, favoriteItem, completeItem, refreshItem));
    }

    private void reloadResourceTree(String selectedId) {
        Set<String> expandedIds = new HashSet<>();
        collectExpandedIds(resourceTree.getRoot(), expandedIds);

        try {
            ResourceNode sharedRoot = resourceRepository.loadTree();
            List<ResourceNode> rootChildren = new ArrayList<>(sharedRoot.children());
            ResourceNode personalRoot = null;
            if (personalWorkspaceRepository != null) {
                personalRoot = personalWorkspaceRepository.loadTree();
                rootChildren.add(personalRoot);
            }
            completeResourceTree = new ResourceNode(
                    "root", "Ressources", ResourceType.ROOT, null, rootChildren);

            if (catalogService != null && currentUser != null) {
                catalogService.indexShared(sharedRoot, resourceRepository.contentRoot());
                if (personalRoot != null) {
                    catalogService.indexPersonal(
                            personalRoot,
                            personalWorkspaceRepository.workspaceRoot(),
                            currentUser.username()
                    );
                }
            }
            renderResourceTree(completeResourceTree, selectedId, expandedIds);
            refreshDashboard();
        } catch (IOException | IllegalStateException exception) {
            writeConsole("Impossible d’actualiser les ressources : " + exception.getMessage());
        }
    }

    private void renderResourceTree(ResourceNode model, String selectedId, Set<String> expandedIds) {
            TreeItem<ResourceNode> root = toTreeItem(model);
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
    }

    private void applyResourceFilter() {
        if (completeResourceTree == null || catalogService == null || currentUser == null) {
            return;
        }
        String query = searchField.getText() == null ? "" : searchField.getText().trim();
        if (query.isEmpty()) {
            renderResourceTree(completeResourceTree, null, Set.of("root"));
            return;
        }
        Set<String> matchingIds = new HashSet<>();
        for (ResourceMetadata metadata : catalogService.search(currentUser, query)) {
            matchingIds.add(metadata.resourceId());
        }
        ResourceNode filtered = filterTree(completeResourceTree, matchingIds, query.toLowerCase())
                .orElse(new ResourceNode("root", "Ressources", ResourceType.ROOT, null, List.of()));
        renderResourceTree(filtered, null, Set.of());
        expandAll(resourceTree.getRoot());
        statusLabel.setText(matchingIds.size() + " résultat(s) pour « " + query + " »");
    }

    private Optional<ResourceNode> filterTree(ResourceNode node, Set<String> matchingIds, String query) {
        if (!node.isContainer()) {
            return matchingIds.contains(node.id()) || node.name().toLowerCase().contains(query)
                    ? Optional.of(node)
                    : Optional.empty();
        }
        if (node.name().toLowerCase().contains(query) && node.type() != ResourceType.ROOT) {
            return Optional.of(node);
        }
        List<ResourceNode> children = node.children().stream()
                .map(child -> filterTree(child, matchingIds, query))
                .flatMap(Optional::stream)
                .toList();
        return !children.isEmpty() || node.type() == ResourceType.ROOT
                ? Optional.of(new ResourceNode(node.id(), node.name(), node.type(), node.localPath(), children))
                : Optional.empty();
    }

    private void expandAll(TreeItem<ResourceNode> item) {
        if (item == null) {
            return;
        }
        item.setExpanded(true);
        item.getChildren().forEach(this::expandAll);
    }

    @FXML
    private void toggleSelectedFavorite() {
        ResourceNode resource = selectedFile();
        if (resource == null) {
            return;
        }
        try {
            boolean favorite = catalogService.toggleFavorite(currentUser, resource);
            statusLabel.setText(favorite ? "Ajouté aux favoris" : "Retiré des favoris");
            refreshDashboard();
        } catch (IllegalStateException exception) {
            showError("Favoris indisponibles", exception.getMessage());
        }
    }

    @FXML
    private void markSelectedComplete() {
        ResourceNode resource = selectedFile();
        if (resource == null) {
            return;
        }
        try {
            catalogService.markComplete(currentUser, resource);
            statusLabel.setText("Terminé • " + resource.name());
            refreshDashboard();
        } catch (IllegalStateException exception) {
            showError("Progression indisponible", exception.getMessage());
        }
    }

    private ResourceNode selectedFile() {
        TreeItem<ResourceNode> selected = resourceTree.getSelectionModel().getSelectedItem();
        if (selected == null || selected.getValue().isContainer()) {
            writeConsole("Sélectionne d’abord un fichier.");
            return null;
        }
        return selected.getValue();
    }

    private void recordOpened(ResourceNode resource) {
        try {
            catalogService.recordOpened(currentUser, resource);
            refreshDashboard();
        } catch (IllegalStateException exception) {
            writeConsole("Historique indisponible : " + exception.getMessage());
        }
    }

    private void refreshDashboard() {
        if (catalogService == null || currentUser == null || completeResourceTree == null) {
            return;
        }
        try {
            ResourceCatalogService.DashboardData dashboard = catalogService.dashboard(currentUser);
            favoriteList.getItems().setAll(resolveResources(dashboard.favorites()));
            recentList.getItems().setAll(resolveResources(dashboard.recent()));
            dashboardProgressLabel.setText(dashboard.completedCount() + " ressource(s) terminée(s)");
        } catch (IllegalStateException exception) {
            writeConsole("Tableau de bord indisponible : " + exception.getMessage());
        }
    }

    private List<ResourceNode> resolveResources(List<ResourceMetadata> metadata) {
        List<ResourceNode> resources = new ArrayList<>();
        for (ResourceMetadata item : metadata) {
            ResourceNode resource = findResourceNode(completeResourceTree, item.resourceId());
            if (resource != null) {
                resources.add(resource);
            }
        }
        return resources;
    }

    private ResourceNode findResourceNode(ResourceNode node, String id) {
        if (node.id().equals(id)) {
            return node;
        }
        for (ResourceNode child : node.children()) {
            ResourceNode found = findResourceNode(child, id);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private void configureDashboardList(ListView<ResourceNode> list) {
        list.setCellFactory(view -> new ListCell<>() {
            @Override
            protected void updateItem(ResourceNode item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.type().symbol() + "  " + item.name());
            }
        });
        list.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && list.getSelectionModel().getSelectedItem() != null) {
                ResourceNode resource = list.getSelectionModel().getSelectedItem();
                TreeItem<ResourceNode> item = findTreeItem(resourceTree.getRoot(), resource.id());
                if (item == null) {
                    searchField.clear();
                    item = findTreeItem(resourceTree.getRoot(), resource.id());
                }
                if (item != null) {
                    resourceTree.getSelectionModel().select(item);
                    openSelectedResource();
                }
            }
        });
    }

    private void reloadAfterAdmin() {
        reloadResourceTree(null);
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

    private boolean requireAdministrator(String action) {
        if (currentUser != null && currentUser.isAdmin()) {
            return true;
        }
        writeConsole("Accès refusé : seuls les administrateurs peuvent " + action + ".");
        return false;
    }

    private void setManagedVisible(Node node, boolean visible) {
        node.setVisible(visible);
        node.setManaged(visible);
    }

    private boolean isPersonal(ResourceNode resource) {
        return resource != null && (resource.id().equals("personal") || resource.id().startsWith("personal/"));
    }

    private boolean canModify(ResourceNode resource) {
        return currentUser != null && (currentUser.isAdmin() || isPersonal(resource));
    }

    private Path pathFor(ResourceNode resource) {
        if (isPersonal(resource)) {
            if (resource.localPath() == null) {
                throw new IllegalArgumentException("Le chemin de l’espace personnel est introuvable.");
            }
            return resource.localPath().toAbsolutePath().normalize();
        }
        return resourceRepository.pathFor(resource);
    }

    private Path rootFor(ResourceNode resource) {
        return isPersonal(resource)
                ? personalWorkspaceRepository.workspaceRoot()
                : resourceRepository.contentRoot();
    }

    private String idFor(Path path, ResourceNode destination) {
        return isPersonal(destination)
                ? personalWorkspaceRepository.idFor(path)
                : resourceRepository.idFor(path);
    }

    private boolean isProtectedRoot(ResourceNode resource) {
        if (resource == null || resource.type() == ResourceType.ROOT || resource.id().equals("personal")) {
            return true;
        }
        return !isPersonal(resource) && Set.of("common", "sisr", "slam").contains(resource.id());
    }

    private boolean isOpen(Path target) {
        Path normalized = target.toAbsolutePath().normalize();
        for (Tab tab : editorTabs.getTabs()) {
            if (tab.getUserData() instanceof DocumentSession session
                    && session.resource().localPath() != null
                    && session.resource().localPath().toAbsolutePath().normalize().startsWith(normalized)) {
                return true;
            }
        }
        return false;
    }

    private void recordResourceAction(String action, String details) {
        try {
            accountService.recordUserAction(currentUser, action, details);
        } catch (IllegalStateException exception) {
            writeConsole("Journal d’audit indisponible : " + exception.getMessage());
        }
    }
}
