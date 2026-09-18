package com.example.siomanager.service;

import com.example.siomanager.model.ResourceNode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class LocalFileService {
    public String readText(ResourceNode resource) throws IOException {
        Path path = requireLocalPath(resource);
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    public void saveText(ResourceNode resource, String content) throws IOException {
        Path path = requireLocalPath(resource);
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        Files.writeString(
                path,
                content,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
        );
    }

    private Path requireLocalPath(ResourceNode resource) throws IOException {
        if (resource.localPath() == null) {
            throw new IOException("La ressource ne possède pas de chemin local : " + resource.name());
        }
        return resource.localPath();
    }
}
