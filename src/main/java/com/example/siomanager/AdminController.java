package com.example.siomanager;

import com.example.siomanager.model.AuditEntry;
import com.example.siomanager.model.TrashEntry;
import com.example.siomanager.model.UserAccount;
import com.example.siomanager.model.UserRole;
import com.example.siomanager.service.AccountService;
import com.example.siomanager.service.ResourceCatalogService;
import com.example.siomanager.service.TrashService;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.io.IOException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class AdminController {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(ZoneId.systemDefault());

    private AccountService accountService;
    private TrashService trashService;
    private UserAccount currentUser;
    private Runnable resourcesChanged;
    private List<UserAccount> allAccounts = List.of();

    @FXML private TableView<UserAccount> usersTable;
    @FXML private TableColumn<UserAccount, String> usernameColumn;
    @FXML private TableColumn<UserAccount, String> displayNameColumn;
    @FXML private TableColumn<UserAccount, UserRole> roleColumn;
    @FXML private TableColumn<UserAccount, String> statusColumn;
    @FXML private TableColumn<UserAccount, String> createdColumn;
    @FXML private TextField userSearchField;
    @FXML private TextField usernameField;
    @FXML private TextField displayNameField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmationField;
    @FXML private ComboBox<UserRole> roleCombo;
    @FXML private TextField editDisplayNameField;
    @FXML private ComboBox<UserRole> editRoleCombo;
    @FXML private PasswordField resetPasswordField;
    @FXML private PasswordField resetConfirmationField;
    @FXML private Label feedbackLabel;

    @FXML private TableView<AuditEntry> auditTable;
    @FXML private TableColumn<AuditEntry, String> auditDateColumn;
    @FXML private TableColumn<AuditEntry, String> auditActorColumn;
    @FXML private TableColumn<AuditEntry, String> auditActionColumn;
    @FXML private TableColumn<AuditEntry, String> auditDetailsColumn;

    @FXML private TableView<TrashEntry> trashTable;
    @FXML private TableColumn<TrashEntry, String> trashNameColumn;
    @FXML private TableColumn<TrashEntry, String> trashOwnerColumn;
    @FXML private TableColumn<TrashEntry, String> trashDateColumn;
    @FXML private TableColumn<TrashEntry, String> trashPathColumn;

    @FXML
    private void initialize() {
        usernameColumn.setCellValueFactory(value -> new ReadOnlyStringWrapper(value.getValue().username()));
        displayNameColumn.setCellValueFactory(value -> new ReadOnlyStringWrapper(value.getValue().displayName()));
        roleColumn.setCellValueFactory(value -> new ReadOnlyObjectWrapper<>(value.getValue().role()));
        statusColumn.setCellValueFactory(value -> new ReadOnlyStringWrapper(
                value.getValue().active() ? "Actif" : "Désactivé"));
        createdColumn.setCellValueFactory(value -> new ReadOnlyStringWrapper(
                DATE_FORMAT.format(value.getValue().createdAt())));
        roleCombo.getItems().setAll(UserRole.ELEVE, UserRole.ADMIN);
        editRoleCombo.getItems().setAll(UserRole.ELEVE, UserRole.ADMIN);
        roleCombo.setValue(UserRole.ELEVE);

        auditDateColumn.setCellValueFactory(value -> new ReadOnlyStringWrapper(
                DATE_FORMAT.format(value.getValue().createdAt())));
        auditActorColumn.setCellValueFactory(value -> new ReadOnlyStringWrapper(value.getValue().actorUsername()));
        auditActionColumn.setCellValueFactory(value -> new ReadOnlyStringWrapper(value.getValue().action()));
        auditDetailsColumn.setCellValueFactory(value -> new ReadOnlyStringWrapper(value.getValue().details()));

        trashNameColumn.setCellValueFactory(value -> new ReadOnlyStringWrapper(value.getValue().resourceName()));
        trashOwnerColumn.setCellValueFactory(value -> new ReadOnlyStringWrapper(value.getValue().deletedBy()));
        trashDateColumn.setCellValueFactory(value -> new ReadOnlyStringWrapper(
                DATE_FORMAT.format(value.getValue().deletedAt())));
        trashPathColumn.setCellValueFactory(value -> new ReadOnlyStringWrapper(value.getValue().originalPath()));

        userSearchField.textProperty().addListener((observable, previous, current) -> filterUsers());
        usersTable.getSelectionModel().selectedItemProperty().addListener((observable, previous, selected) -> {
            if (selected != null) {
                editDisplayNameField.setText(selected.displayName());
                editRoleCombo.setValue(selected.role());
            }
        });
    }

    public void configure(
            AccountService accountService,
            ResourceCatalogService ignoredCatalogService,
            TrashService trashService,
            UserAccount currentUser,
            Runnable resourcesChanged
    ) {
        if (!currentUser.isAdmin()) {
            throw new SecurityException("Cette fenêtre est réservée aux administrateurs.");
        }
        this.accountService = accountService;
        this.trashService = trashService;
        this.currentUser = currentUser;
        this.resourcesChanged = resourcesChanged;
        refreshUsers();
        refreshAudit();
        refreshTrash();
    }

    @FXML
    private void createAccount() {
        runAction(() -> {
            UserAccount created = accountService.createAccount(
                    currentUser, usernameField.getText(), displayNameField.getText(),
                    passwordField.getText().toCharArray(), confirmationField.getText().toCharArray(),
                    roleCombo.getValue());
            usernameField.clear();
            displayNameField.clear();
            passwordField.clear();
            confirmationField.clear();
            refreshUsers();
            select(created.id());
            feedbackLabel.setText("Compte créé. Le mot de passe devra être changé à la première connexion.");
        });
    }

    @FXML
    private void updateSelectedAccount() {
        UserAccount selected = requireSelection();
        if (selected == null) return;
        runAction(() -> {
            UserAccount updated = accountService.updateAccount(
                    currentUser, selected, editDisplayNameField.getText(), editRoleCombo.getValue());
            if (updated.id() == currentUser.id()) {
                currentUser = updated;
            }
            refreshUsers();
            select(updated.id());
            feedbackLabel.setText("Compte mis à jour.");
        });
    }

    @FXML
    private void toggleSelectedAccount() {
        UserAccount selected = requireSelection();
        if (selected == null) return;
        runAction(() -> {
            accountService.setAccountActive(currentUser, selected, !selected.active());
            refreshUsers();
            select(selected.id());
        });
    }

    @FXML
    private void deleteSelectedAccount() {
        UserAccount selected = requireSelection();
        if (selected == null) return;
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Le compte sera supprimé. Son espace personnel sera conservé sur le disque.",
                ButtonType.OK, ButtonType.CANCEL);
        alert.setHeaderText("Supprimer le compte « " + selected.username() + " » ?");
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            runAction(() -> {
                accountService.deleteAccount(currentUser, selected);
                refreshUsers();
                feedbackLabel.setText("Compte supprimé ; ses fichiers personnels ont été conservés.");
            });
        }
    }

    @FXML
    private void resetSelectedPassword() {
        UserAccount selected = requireSelection();
        if (selected == null) return;
        runAction(() -> {
            accountService.resetPassword(currentUser, selected,
                    resetPasswordField.getText().toCharArray(), resetConfirmationField.getText().toCharArray());
            resetPasswordField.clear();
            resetConfirmationField.clear();
            feedbackLabel.setText("Mot de passe temporaire enregistré.");
        });
    }

    @FXML
    private void refreshUsers() {
        if (accountService != null) {
            allAccounts = accountService.listAccounts(currentUser);
            filterUsers();
        }
    }

    @FXML
    private void refreshAudit() {
        if (accountService != null) {
            auditTable.getItems().setAll(accountService.listAuditEntries(currentUser, 500));
        }
    }

    private void refreshTrash() {
        if (trashService != null) {
            trashTable.getItems().setAll(trashService.list(currentUser));
        }
    }

    @FXML
    private void restoreSelectedTrash() {
        TrashEntry selected = trashTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        try {
            trashService.restore(currentUser, selected);
            accountService.recordAdminAction(currentUser, "RESOURCE_RESTORED", selected.originalPath());
            refreshTrash();
            refreshAudit();
            resourcesChanged.run();
        } catch (IOException | IllegalArgumentException exception) {
            feedbackLabel.setText(exception.getMessage());
        }
    }

    @FXML
    private void purgeSelectedTrash() {
        TrashEntry selected = trashTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Cette suppression est définitive et ne pourra pas être annulée.",
                ButtonType.OK, ButtonType.CANCEL);
        alert.setHeaderText("Vider définitivement cette entrée ?");
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                trashService.purge(currentUser, selected);
                accountService.recordAdminAction(currentUser, "TRASH_PURGED", selected.originalPath());
                refreshTrash();
                refreshAudit();
            } catch (IOException | IllegalArgumentException exception) {
                feedbackLabel.setText(exception.getMessage());
            }
        }
    }

    private void filterUsers() {
        String query = userSearchField.getText() == null ? "" : userSearchField.getText().trim().toLowerCase();
        List<UserAccount> filtered = new ArrayList<>();
        for (UserAccount account : allAccounts) {
            if (query.isEmpty() || account.username().toLowerCase().contains(query)
                    || account.displayName().toLowerCase().contains(query)
                    || account.role().displayName().toLowerCase().contains(query)) {
                filtered.add(account);
            }
        }
        usersTable.getItems().setAll(filtered);
    }

    private UserAccount requireSelection() {
        UserAccount selected = usersTable.getSelectionModel().getSelectedItem();
        if (selected == null) feedbackLabel.setText("Sélectionne d’abord un compte.");
        return selected;
    }

    private void select(long id) {
        usersTable.getItems().stream().filter(account -> account.id() == id).findFirst()
                .ifPresent(account -> usersTable.getSelectionModel().select(account));
    }

    private void runAction(Runnable action) {
        try {
            feedbackLabel.setText("");
            action.run();
            refreshAudit();
        } catch (IllegalArgumentException | IllegalStateException | SecurityException exception) {
            feedbackLabel.setText(exception.getMessage());
        }
    }
}
