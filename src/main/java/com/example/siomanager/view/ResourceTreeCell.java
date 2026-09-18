package com.example.siomanager.view;

import com.example.siomanager.model.ResourceNode;
import com.example.siomanager.model.ResourceType;
import javafx.scene.control.TreeCell;

import java.util.Arrays;
import java.util.List;

public class ResourceTreeCell extends TreeCell<ResourceNode> {
    private static final List<String> TYPE_CLASSES = Arrays.stream(ResourceType.values())
            .map(ResourceType::cssClass)
            .toList();

    @Override
    protected void updateItem(ResourceNode resource, boolean empty) {
        super.updateItem(resource, empty);
        getStyleClass().removeAll(TYPE_CLASSES);

        if (empty || resource == null) {
            setText(null);
            return;
        }

        String prefix = resource.type().symbol().isBlank()
                ? ""
                : resource.type().symbol() + "  ";

        setText(prefix + resource.name());
        getStyleClass().add(resource.type().cssClass());
    }
}
