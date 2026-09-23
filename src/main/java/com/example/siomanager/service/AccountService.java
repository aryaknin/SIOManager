package com.example.siomanager.service;

import com.example.siomanager.model.UserAccount;
import com.example.siomanager.model.UserRole;
import com.example.siomanager.model.AuditEntry;
import com.example.siomanager.repository.UserRepository;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

public final class AccountService {
    private static final Pattern USERNAME_PATTERN = Pattern.compile("[a-z0-9._-]{3,32}");
    private static final int MINIMUM_PASSWORD_LENGTH = 10;

    private final UserRepository repository;
    private final PasswordHasher passwordHasher;

    public AccountService(UserRepository repository) {
        this(repository, new PasswordHasher());
    }

    AccountService(UserRepository repository, PasswordHasher passwordHasher) {
        this.repository = repository;
        this.passwordHasher = passwordHasher;
    }

    public boolean needsInitialAdministrator() {
        try {
            return repository.count() == 0;
        } catch (SQLException exception) {
            throw databaseFailure(exception);
        }
    }

    public UserAccount createInitialAdministrator(
            String username,
            String displayName,
            char[] password,
            char[] confirmation
    ) {
        try {
            if (repository.count() != 0) {
                throw new IllegalStateException("L’administrateur initial existe déjà.");
            }
            validatePassword(password, confirmation);
            UserAccount account = createAccountInternal(username, displayName, password, UserRole.ADMIN, false);
            repository.audit(account.id(), "INITIAL_ADMIN_CREATED", account.username());
            return account;
        } catch (SQLException exception) {
            throw translateDatabaseException(exception);
        } finally {
            wipe(password, confirmation);
        }
    }

    public Optional<UserAccount> authenticate(String username, char[] password) {
        try {
            Optional<UserRepository.StoredUser> storedUser = repository.findStoredByUsername(normalizeUsername(username));
            if (storedUser.isEmpty()
                    || !storedUser.get().account().active()
                    || !passwordHasher.verify(
                            password,
                            storedUser.get().passwordHash(),
                            storedUser.get().passwordSalt(),
                            storedUser.get().iterations())) {
                repository.audit(null, "LOGIN_FAILED", normalizeUsername(username));
                return Optional.empty();
            }
            UserAccount account = storedUser.get().account();
            repository.audit(account.id(), "LOGIN_SUCCESS", account.username());
            return Optional.of(account);
        } catch (SQLException exception) {
            throw databaseFailure(exception);
        } finally {
            wipe(password);
        }
    }

    public List<UserAccount> listAccounts(UserAccount actor) {
        requireAdmin(actor);
        try {
            return repository.findAll();
        } catch (SQLException exception) {
            throw databaseFailure(exception);
        }
    }

    public UserAccount createAccount(
            UserAccount actor,
            String username,
            String displayName,
            char[] temporaryPassword,
            char[] confirmation,
            UserRole role
    ) {
        requireAdmin(actor);
        try {
            validatePassword(temporaryPassword, confirmation);
            UserAccount account = createAccountInternal(username, displayName, temporaryPassword, role, true);
            repository.audit(actor.id(), "ACCOUNT_CREATED", account.username() + ":" + role.name());
            return account;
        } catch (SQLException exception) {
            throw translateDatabaseException(exception);
        } finally {
            wipe(temporaryPassword, confirmation);
        }
    }

    public void setAccountActive(UserAccount actor, UserAccount target, boolean active) {
        requireAdmin(actor);
        if (actor.id() == target.id() && !active) {
            throw new IllegalArgumentException("Tu ne peux pas désactiver ton propre compte.");
        }
        try {
            if (!active && target.isAdmin() && target.active() && repository.countActiveAdmins() <= 1) {
                throw new IllegalArgumentException("Il doit rester au moins un administrateur actif.");
            }
            repository.setActive(target.id(), active);
            repository.audit(actor.id(), active ? "ACCOUNT_ENABLED" : "ACCOUNT_DISABLED", target.username());
        } catch (SQLException exception) {
            throw databaseFailure(exception);
        }
    }

    public UserAccount updateAccount(
            UserAccount actor,
            UserAccount target,
            String displayName,
            UserRole role
    ) {
        requireAdmin(actor);
        String normalizedDisplayName = displayName == null ? "" : displayName.trim();
        if (normalizedDisplayName.length() < 2 || normalizedDisplayName.length() > 80) {
            throw new IllegalArgumentException("Le nom affiché doit contenir entre 2 et 80 caractères.");
        }
        if (role == null) {
            throw new IllegalArgumentException("Le rôle est obligatoire.");
        }
        if (actor.id() == target.id() && role != UserRole.ADMIN) {
            throw new IllegalArgumentException("Reconnecte-toi avec un autre administrateur pour modifier ton propre rôle.");
        }
        try {
            if (target.isAdmin() && role != UserRole.ADMIN && target.active()
                    && repository.countActiveAdmins() <= 1) {
                throw new IllegalArgumentException("Il doit rester au moins un administrateur actif.");
            }
            repository.updateAccount(target.id(), normalizedDisplayName, role);
            repository.audit(actor.id(), "ACCOUNT_UPDATED", target.username() + ":" + role.name());
            return repository.findStoredByUsername(target.username()).orElseThrow().account();
        } catch (SQLException exception) {
            throw databaseFailure(exception);
        }
    }

    public void deleteAccount(UserAccount actor, UserAccount target) {
        requireAdmin(actor);
        if (actor.id() == target.id()) {
            throw new IllegalArgumentException("Tu ne peux pas supprimer ton propre compte.");
        }
        try {
            if (target.isAdmin() && target.active() && repository.countActiveAdmins() <= 1) {
                throw new IllegalArgumentException("Il doit rester au moins un administrateur actif.");
            }
            repository.audit(actor.id(), "ACCOUNT_DELETED", target.username());
            repository.delete(target.id());
        } catch (SQLException exception) {
            throw databaseFailure(exception);
        }
    }

    public List<AuditEntry> listAuditEntries(UserAccount actor, int limit) {
        requireAdmin(actor);
        try {
            return repository.findAuditEntries(limit);
        } catch (SQLException exception) {
            throw databaseFailure(exception);
        }
    }

    public void resetPassword(
            UserAccount actor,
            UserAccount target,
            char[] newPassword,
            char[] confirmation
    ) {
        requireAdmin(actor);
        updatePassword(target, newPassword, confirmation, true);
        try {
            repository.audit(actor.id(), "PASSWORD_RESET", target.username());
        } catch (SQLException exception) {
            throw databaseFailure(exception);
        }
    }

    public UserAccount changeOwnPassword(UserAccount account, char[] newPassword, char[] confirmation) {
        updatePassword(account, newPassword, confirmation, false);
        try {
            repository.audit(account.id(), "PASSWORD_CHANGED", account.username());
            return repository.findStoredByUsername(account.username()).orElseThrow().account();
        } catch (SQLException exception) {
            throw databaseFailure(exception);
        }
    }

    public void recordAdminAction(UserAccount actor, String action, String details) {
        requireAdmin(actor);
        try {
            repository.audit(actor.id(), action, details);
        } catch (SQLException exception) {
            throw databaseFailure(exception);
        }
    }

    public void recordUserAction(UserAccount actor, String action, String details) {
        if (actor == null || !actor.active()) {
            throw new SecurityException("Aucune session utilisateur active.");
        }
        try {
            repository.audit(actor.id(), action, details);
        } catch (SQLException exception) {
            throw databaseFailure(exception);
        }
    }

    private UserAccount createAccountInternal(
            String username,
            String displayName,
            char[] password,
            UserRole role,
            boolean mustChangePassword
    ) throws SQLException {
        String normalizedUsername = normalizeUsername(username);
        String normalizedDisplayName = displayName == null ? "" : displayName.trim();
        if (!USERNAME_PATTERN.matcher(normalizedUsername).matches()) {
            throw new IllegalArgumentException(
                    "L’identifiant doit contenir 3 à 32 caractères : lettres sans accent, chiffres, point, tiret ou underscore."
            );
        }
        if (normalizedDisplayName.length() < 2 || normalizedDisplayName.length() > 80) {
            throw new IllegalArgumentException("Le nom affiché doit contenir entre 2 et 80 caractères.");
        }
        if (role == null) {
            throw new IllegalArgumentException("Le rôle est obligatoire.");
        }

        PasswordHasher.PasswordData passwordData = passwordHasher.hash(password);
        return repository.create(
                normalizedUsername,
                normalizedDisplayName,
                role,
                passwordData.hash(),
                passwordData.salt(),
                passwordData.iterations(),
                mustChangePassword
        );
    }

    private void updatePassword(UserAccount target, char[] password, char[] confirmation, boolean mustChange) {
        try {
            validatePassword(password, confirmation);
            PasswordHasher.PasswordData passwordData = passwordHasher.hash(password);
            repository.updatePassword(
                    target.id(),
                    passwordData.hash(),
                    passwordData.salt(),
                    passwordData.iterations(),
                    mustChange
            );
        } catch (SQLException exception) {
            throw databaseFailure(exception);
        } finally {
            wipe(password, confirmation);
        }
    }

    private void validatePassword(char[] password, char[] confirmation) {
        if (password == null || password.length < MINIMUM_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("Le mot de passe doit contenir au moins 10 caractères.");
        }
        if (password.length > 128) {
            throw new IllegalArgumentException("Le mot de passe est trop long (128 caractères maximum).");
        }
        if (!Arrays.equals(password, confirmation)) {
            throw new IllegalArgumentException("Les deux mots de passe ne correspondent pas.");
        }
    }

    private void requireAdmin(UserAccount actor) {
        if (actor == null || !actor.active() || !actor.isAdmin()) {
            throw new SecurityException("Cette action est réservée aux administrateurs.");
        }
    }

    private String normalizeUsername(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }

    private IllegalStateException translateDatabaseException(SQLException exception) {
        if (exception.getMessage() != null && exception.getMessage().contains("UNIQUE constraint failed: users.username")) {
            return new IllegalStateException("Cet identifiant est déjà utilisé.", exception);
        }
        return databaseFailure(exception);
    }

    private IllegalStateException databaseFailure(SQLException exception) {
        return new IllegalStateException("La base de données locale est indisponible.", exception);
    }

    private void wipe(char[]... values) {
        for (char[] value : values) {
            if (value != null) {
                Arrays.fill(value, '\0');
            }
        }
    }
}
