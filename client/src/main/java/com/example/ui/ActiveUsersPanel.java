package com.example.ui;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * Panel that displays active users in the document
 */
public class ActiveUsersPanel extends VBox {
    private static final int PANEL_SPACING = 5;
    private static final int PANEL_PADDING = 10;

    private final String currentUserId;

    public ActiveUsersPanel(String currentUserId) {
        this.currentUserId = currentUserId;

        setSpacing(PANEL_SPACING);
        setPadding(new Insets(PANEL_PADDING));
        setStyle("-fx-background-color: #f5f5f5; -fx-min-width: 150px;");

        initializePanel();
    }

    private void initializePanel() {
        Label title = new Label("Active Users");
        title.setStyle("-fx-font-weight: bold;");
        getChildren().add(title);
    }

    /**
     * Updates the panel with active user information
     */
    public void updateActiveUsers(Map<String, Object> userStatusUpdate) {
        String type = (String) userStatusUpdate.get("type");

        if ("USER_POSITIONS".equals(type)) {
            @SuppressWarnings("unchecked")
            Map<String, Integer> userPositions = (Map<String, Integer>) userStatusUpdate.get("positions");
            displayActiveUsers(userPositions);
        } else if ("USER_LEFT".equals(type)) {
            String userId = (String) userStatusUpdate.get("userId");
            System.out.println("User left: " + userId);
        }
    }

    /**
     * Displays active users and their positions in the document
     */
    private void displayActiveUsers(Map<String, Integer> userPositions) {
        Platform.runLater(() -> {
            getChildren().clear();

            Label title = new Label("Active Users");
            title.setStyle("-fx-font-weight: bold;");
            getChildren().add(title);

            AtomicInteger index = new AtomicInteger(0);
            userPositions.forEach((user, line) -> {
                boolean isCurrentUser = user.equals(currentUserId);
                String labelText = formatUserLabel(user, line, isCurrentUser, index.get());
                Label userLabel = createUserLabel(labelText, isCurrentUser);

                getChildren().add(userLabel);
                index.getAndIncrement();
            });
        });
    }

    private String formatUserLabel(String userId, int line, boolean isCurrentUser, int index) {
        String displayName = isCurrentUser ? "You" : Animals.animals[index];
        return displayName + " - Line " + (line + 1);
    }

    private Label createUserLabel(String text, boolean isCurrentUser) {
        Label userLabel = new Label(text);

        if (isCurrentUser) {
            userLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #0066cc;");
        }

        return userLabel;
    }
}