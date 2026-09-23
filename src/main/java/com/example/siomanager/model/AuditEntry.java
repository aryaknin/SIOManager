package com.example.siomanager.model;

import java.time.Instant;

public record AuditEntry(
        long id,
        String actorUsername,
        String action,
        String details,
        Instant createdAt
) { }
