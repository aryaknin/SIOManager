package com.example.siomanager.model;

public enum UserRole {
    ADMIN("Administrateur"),
    ELEVE("Élève");

    private final String displayName;

    UserRole(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
