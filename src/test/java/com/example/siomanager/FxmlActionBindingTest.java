package com.example.siomanager;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FxmlActionBindingTest {
    private static final Pattern ACTION_PATTERN = Pattern.compile("onAction=\"#([A-Za-z0-9_]+)\"");

    @Test
    void allFxmlActionsReferenceAControllerMethod() throws Exception {
        verify("main-view.fxml", MainController.class);
        verify("admin-view.fxml", AdminController.class);
        verify("settings-view.fxml", SettingsController.class);
    }

    private void verify(String resourceName, Class<?> controllerType) throws Exception {
        try (InputStream input = MainApplication.class.getResourceAsStream(resourceName)) {
            assertNotNull(input, resourceName);
            String fxml = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            Matcher matcher = ACTION_PATTERN.matcher(fxml);
            while (matcher.find()) {
                String methodName = matcher.group(1);
                assertTrue(
                        Arrays.stream(controllerType.getDeclaredMethods()).map(Method::getName)
                                .anyMatch(methodName::equals),
                        () -> resourceName + " référence une méthode absente : " + methodName
                );
            }
        }
    }
}
