package com.example.siomanager.model;

import java.time.Instant;

public record TrashEntry(
        long id,
        String resourceName,
        String originalPath,
        String trashPath,
        String scope,
        String ownerUsername,
        String deletedBy,
        Instant deletedAt
) { }
