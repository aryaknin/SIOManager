package com.example.siomanager.view;

import com.example.siomanager.model.ResourceNode;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Node;

import java.io.IOException;

public final class DocumentSession {
    @FunctionalInterface
    public interface SaveAction {
        void save() throws IOException;
    }

    private final ResourceNode resource;
    private final Node content;
    private final SaveAction saveAction;
    private final Runnable closeAction;
    private final BooleanProperty modified = new SimpleBooleanProperty(false);
    private boolean closed;

    public DocumentSession(ResourceNode resource, Node content, SaveAction saveAction) {
        this(resource, content, saveAction, () -> { });
    }

    public DocumentSession(ResourceNode resource, Node content, SaveAction saveAction, Runnable closeAction) {
        this.resource = resource;
        this.content = content;
        this.saveAction = saveAction;
        this.closeAction = closeAction;
    }

    public ResourceNode resource() {
        return resource;
    }

    public Node content() {
        return content;
    }

    public boolean canSave() {
        return saveAction != null;
    }

    public boolean isModified() {
        return modified.get();
    }

    public ReadOnlyBooleanProperty modifiedProperty() {
        return modified;
    }

    public void markModified() {
        modified.set(true);
    }

    public void save() throws IOException {
        if (saveAction == null) {
            return;
        }
        saveAction.save();
        modified.set(false);
    }

    public void close() {
        if (!closed) {
            closed = true;
            closeAction.run();
        }
    }
}
