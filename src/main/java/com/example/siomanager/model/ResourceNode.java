package com.example.siomanager.model;

import java.util.List;
import java.util.Objects;

public record ResourceNode(
        String id,
        String name,
        ResourceType type,
        List<ResourceNode> children
) {
    public ResourceNode {
        Objects.requireNonNull(id, "L’identifiant est obligatoire");
        Objects.requireNonNull(name, "Le nom est obligatoire");
        Objects.requireNonNull(type, "Le type est obligatoire");
        children = children == null ? List.of() : List.copyOf(children);

        if (!type.isContainer() && !children.isEmpty()) {
            throw new IllegalArgumentException("Un fichier ne peut pas contenir de ressources");
        }
    }

    public boolean isContainer() {
        return type.isContainer();
    }

    @Override
    public String toString() {
        return name;
    }
}
