package com.example.siomanager.service;

import com.example.siomanager.data.DatabaseManager;
import com.example.siomanager.model.UserAccount;
import com.example.siomanager.model.UserRole;
import com.example.siomanager.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccountServiceTest {
    @TempDir
    Path temporaryDirectory;

    private AccountService accountService;

    @BeforeEach
    void setUp() throws Exception {
        DatabaseManager databaseManager = new DatabaseManager(temporaryDirectory.resolve("test.db"));
        databaseManager.initialize();
        accountService = new AccountService(new UserRepository(databaseManager));
    }

    @Test
    void createsAndAuthenticatesInitialAdministrator() {
        assertTrue(accountService.needsInitialAdministrator());

        UserAccount admin = accountService.createInitialAdministrator(
                "Admin",
                "Administrateur BTS",
                "mot-de-passe-solide".toCharArray(),
                "mot-de-passe-solide".toCharArray()
        );

        assertTrue(admin.isAdmin());
        assertFalse(accountService.needsInitialAdministrator());
        assertEquals(admin.id(), accountService.authenticate(
                "ADMIN",
                "mot-de-passe-solide".toCharArray()
        ).orElseThrow().id());
        assertTrue(accountService.authenticate("admin", "incorrect-123".toCharArray()).isEmpty());
    }

    @Test
    void adminCreatesStudentWhoMustReplaceTemporaryPassword() {
        UserAccount admin = createAdmin();
        UserAccount student = accountService.createAccount(
                admin,
                "martin.l",
                "Léa Martin",
                "temporaire-123".toCharArray(),
                "temporaire-123".toCharArray(),
                UserRole.ELEVE
        );

        assertTrue(student.mustChangePassword());
        UserAccount connectedStudent = accountService.authenticate(
                "martin.l",
                "temporaire-123".toCharArray()
        ).orElseThrow();
        UserAccount updated = accountService.changeOwnPassword(
                connectedStudent,
                "nouveau-secret-456".toCharArray(),
                "nouveau-secret-456".toCharArray()
        );

        assertFalse(updated.mustChangePassword());
        assertTrue(accountService.authenticate("martin.l", "temporaire-123".toCharArray()).isEmpty());
        assertTrue(accountService.authenticate("martin.l", "nouveau-secret-456".toCharArray()).isPresent());
        assertThrows(SecurityException.class, () -> accountService.listAccounts(updated));
    }

    @Test
    void preventsDisablingOwnOrLastAdministratorAccount() {
        UserAccount admin = createAdmin();

        assertThrows(
                IllegalArgumentException.class,
                () -> accountService.setAccountActive(admin, admin, false)
        );
    }

    @Test
    void rejectsDuplicateUsernameAndWeakPassword() {
        UserAccount admin = createAdmin();
        assertThrows(IllegalArgumentException.class, () -> accountService.createAccount(
                admin,
                "eleve.1",
                "Premier Élève",
                "court".toCharArray(),
                "court".toCharArray(),
                UserRole.ELEVE
        ));

        accountService.createAccount(
                admin,
                "eleve.1",
                "Premier Élève",
                "temporaire-123".toCharArray(),
                "temporaire-123".toCharArray(),
                UserRole.ELEVE
        );
        assertThrows(IllegalStateException.class, () -> accountService.createAccount(
                admin,
                "ELEVE.1",
                "Autre Élève",
                "temporaire-456".toCharArray(),
                "temporaire-456".toCharArray(),
                UserRole.ELEVE
        ));
    }

    @Test
    void adminUpdatesDeletesAndAuditsAccounts() {
        UserAccount admin = createAdmin();
        UserAccount student = accountService.createAccount(
                admin, "eleve.2", "Élève Deux",
                "temporaire-123".toCharArray(), "temporaire-123".toCharArray(), UserRole.ELEVE);

        UserAccount updated = accountService.updateAccount(admin, student, "Élève Modifié", UserRole.ADMIN);
        assertEquals("Élève Modifié", updated.displayName());
        assertEquals(UserRole.ADMIN, updated.role());
        assertFalse(accountService.listAuditEntries(admin, 100).isEmpty());

        accountService.deleteAccount(admin, updated);
        assertEquals(1, accountService.listAccounts(admin).size());
    }

    private UserAccount createAdmin() {
        return accountService.createInitialAdministrator(
                "admin",
                "Administrateur BTS",
                "mot-de-passe-solide".toCharArray(),
                "mot-de-passe-solide".toCharArray()
        );
    }
}
