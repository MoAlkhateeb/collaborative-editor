package com.example.ui;

import com.example.client.SceneManager;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class LandingPage extends VBox {
    private static final int SPACING = 20;
    private static final int BUTTON_SPACING = 10;

    private SceneManager sceneManager;

    public LandingPage(SceneManager sceneManager) {
        this.sceneManager = sceneManager;

        setSpacing(SPACING);
        setAlignment(Pos.CENTER);

        setupUI();
    }

    private void setupUI() {
        Label titleLabel = new Label("Document Editor");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        HBox buttonContainer = new HBox(BUTTON_SPACING);
        buttonContainer.setAlignment(Pos.CENTER);

        Button newDocButton = new Button("New Document");
        newDocButton.setOnAction(e -> sceneManager.showNewDocumentPage());

        Button browseDocButton = new Button("Browse Document");
        browseDocButton.setOnAction(e -> sceneManager.showBrowsePage());

        buttonContainer.getChildren().addAll(newDocButton, browseDocButton);

        VBox joinSessionBox = new VBox(10);
        joinSessionBox.setAlignment(Pos.CENTER);

        Label joinLabel = new Label("Join Existing Session");

        TextField sessionCodeField = new TextField();
        sessionCodeField.setPromptText("Enter session code...");
        sessionCodeField.setMaxWidth(200);

        Button joinButton = new Button("Join Session");
        joinButton.setOnAction(e -> sceneManager.showJoinSession(sessionCodeField.getText()));

        joinSessionBox.getChildren().addAll(joinLabel, sessionCodeField, joinButton);

        getChildren().addAll(titleLabel, buttonContainer, joinSessionBox);
    }
}