package com.example.siomanager.model;

import java.time.Instant;
import java.util.Objects;

public record UserAccount(
        long id,
        String username,
        String displayName,
        UserRole role,
        boolean active,
        boolean mustChangePassword,
        Instant createdAt
) {
    public UserAccount {
        Objects.requireNonNull(username);
        Objects.requireNonNull(displayName);
        Objects.requireNonNull(role);
        Objects.requireNonNull(createdAt);
    }

    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }
}
