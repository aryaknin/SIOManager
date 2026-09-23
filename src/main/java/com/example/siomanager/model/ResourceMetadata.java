package com.example.siomanager.model;

import java.time.Instant;

public record ResourceMetadata(
        String resourceKey,
        String resourceId,
        String scope,
        String ownerUsername,
        String relativePath,
        String title,
        String subject,
        String chapter,
        ResourceType resourceType,
        String description,
        String searchText,
        String checksum,
        long sizeBytes,
        Instant updatedAt
) { }
