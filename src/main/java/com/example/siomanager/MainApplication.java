package com.example.siomanager;

import com.example.siomanager.data.DatabaseManager;
import com.example.siomanager.model.UserAccount;
import com.example.siomanager.repository.UserRepository;
import com.example.siomanager.repository.ResourceCatalogRepository;
import com.example.siomanager.repository.LocalResourceRepository;
import com.example.siomanager.service.AccountService;
import com.example.siomanager.service.BackupService;
import com.example.siomanager.service.ResourceCatalogService;
import com.example.siomanager.service.TrashService;
import com.example.siomanager.view.AuthenticationDialog;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.Optional;

public class MainApplication extends Application {
    private DatabaseManager databaseManager;
    private AccountService accountService;
    private ResourceCatalogService catalogService;
    private TrashService trashService;
    private BackupService backupService;

    @Override
    public void start(Stage stage) {
        databaseManager = DatabaseManager.forCurrentUser();
        try {
            databaseManager.initialize();
            Path sharedResourcesRoot = prepareSharedResources(databaseManager.databasePath().getParent());
            System.setProperty(LocalResourceRepository.CONTENT_DIRECTORY_PROPERTY, sharedResourcesRoot.toString());
            accountService = new AccountService(new UserRepository(databaseManager));
            catalogService = new ResourceCatalogService(new ResourceCatalogRepository(databaseManager));
            trashService = new TrashService(databaseManager.databasePath().getParent(), catalogService);
            backupService = new BackupService(
                    databaseManager,
                    sharedResourcesRoot
            );
        } catch (IOException | SQLException exception) {
            showStartupError("Impossible d’initialiser la base de données locale", exception);
            return;
        }

        authenticateAndShow(stage);
    }

    private Path prepareSharedResources(Path dataDirectory) throws IOException {
        String templateProperty = System.getProperty("siomanager.sharedResourcesTemplate");
        if (templateProperty == null || templateProperty.isBlank()) {
            return Path.of(System.getProperty("user.dir"), "demo-content").toAbsolutePath().normalize();
        }

        Path template = Path.of(templateProperty).toAbsolutePath().normalize();
        if (!Files.isDirectory(template)) {
            throw new IOException("Les ressources intégrées sont introuvables : " + template);
        }
        Path destination = dataDirectory.resolve("shared-resources").toAbsolutePath().normalize();
        Files.createDirectories(destination);
        try (var paths = Files.walk(template)) {
            for (Path source : paths.toList()) {
                Path target = destination.resolve(template.relativize(source)).normalize();
                if (Files.isDirectory(source)) {
                    Files.createDirectories(target);
                } else if (!Files.exists(target)) {
                    Files.createDirectories(target.getParent());
                    Files.copy(source, target, StandardCopyOption.COPY_ATTRIBUTES);
                }
            }
        }
        return destination;
    }

    private void authenticateAndShow(Stage stage) {
        Optional<UserAccount> authenticatedUser = AuthenticationDialog.show(null, accountService);
        if (authenticatedUser.isEmpty()) {
            Platform.exit();
            return;
        }

        FXMLLoader loader = new FXMLLoader(MainApplication.class.getResource("main-view.fxml"));
        Scene scene;
        try {
            scene = new Scene(loader.load(), 1180, 720);
        } catch (IOException exception) {
            showStartupError("Impossible de charger l’interface principale", exception);
            return;
        }
        MainController controller = loader.getController();
        controller.configureSecurity(
                accountService,
                catalogService,
                trashService,
                backupService,
                authenticatedUser.get(),
                databaseManager.databasePath().getParent()
                        .resolve("workspaces")
                        .resolve(authenticatedUser.get().username()),
                () -> {
                    stage.hide();
                    Platform.runLater(() -> authenticateAndShow(stage));
                }
        );

        stage.setTitle("SIOManager");
        stage.setMinWidth(820);
        stage.setMinHeight(520);
        stage.setScene(scene);
        controller.attachStage(stage);
        stage.setOnCloseRequest(event -> {
            if (!controller.confirmCloseAll()) {
                event.consume();
            } else {
                controller.dispose();
            }
        });
        stage.show();
    }

    private void showStartupError(String title, Exception exception) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Démarrage impossible — SIOManager");
        alert.setHeaderText(title);
        alert.setContentText(exception.getMessage());
        alert.showAndWait();
        Platform.exit();
    }
}
