package com.example.ui;

import com.example.client.SceneManager;
import com.example.network.NetworkManager;
import com.example.network.NetworkManager.DocumentInfo;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

/**
 * Component for document toolbar UI and actions
 */
public class ToolbarComponent extends HBox {
    private static final int TOOLBAR_SPACING = 10;
    private static final int TOOLBAR_PADDING = 10;

    private final SceneManager sceneManager;
    private final NetworkManager networkManager;
    private final DocumentInfo docInfo;

    private final Runnable onSave;
    private final Runnable onUndo;
    private final Runnable onRedo;

    public ToolbarComponent(SceneManager sceneManager, NetworkManager networkManager,
            DocumentInfo docInfo, Runnable onSave, Runnable onUndo, Runnable onRedo) {
        this.sceneManager = sceneManager;
        this.networkManager = networkManager;
        this.docInfo = docInfo;
        this.onSave = onSave;
        this.onUndo = onUndo;
        this.onRedo = onRedo;

        setupToolbar();
    }

    private void setupToolbar() {
        setSpacing(TOOLBAR_SPACING);
        setAlignment(Pos.CENTER_LEFT);
        setPadding(new Insets(TOOLBAR_PADDING));

        // Create toolbar elements
        Button backButton = createBackButton();
        Button undoButton = createUndoButton();
        Button redoButton = createRedoButton();
        Button saveButton = createSaveButton();

        // Create document code fields
        HBox editorCodeContainer = createEditorCodeFields();
        HBox viewerCodeContainer = createViewerCodeFields();

        // Hide editor controls if user is not an editor
        if (!networkManager.isEditor()) {
            undoButton.setVisible(false);
            redoButton.setVisible(false);
            editorCodeContainer.setVisible(false);
        }

        // Add all components to toolbar
        getChildren().addAll(
                backButton,
                undoButton,
                redoButton,
                saveButton,
                editorCodeContainer,
                viewerCodeContainer);
    }

    private Button createBackButton() {
        Button backButton = new Button("Back to Home");
        backButton.setOnAction(e -> {
            networkManager.disconnect();
            sceneManager.showLandingPage();
        });
        return backButton;
    }

    private Button createUndoButton() {
        Button undoButton = new Button("Undo");
        undoButton.setOnAction(e -> {
            if (onUndo != null) {
                onUndo.run();
            }
        });
        return undoButton;
    }

    private Button createRedoButton() {
        Button redoButton = new Button("Redo");
        redoButton.setOnAction(e -> {
            if (onRedo != null) {
                onRedo.run();
            }
        });
        return redoButton;
    }

    private Button createSaveButton() {
        Button saveButton = new Button("Save");
        saveButton.setOnAction(e -> {
            if (onSave != null) {
                onSave.run();
            }
        });
        return saveButton;
    }

    private HBox createEditorCodeFields() {
        HBox container = new HBox(5);

        Label editorCodeLabel = new Label("Editor Code: ");
        TextField editorCode = new TextField(docInfo.getEditorCode());
        editorCode.setDisable(true);

        container.getChildren().addAll(editorCodeLabel, editorCode);
        return container;
    }

    private HBox createViewerCodeFields() {
        HBox container = new HBox(5);

        Label viewerCodeLabel = new Label("Viewer Code: ");
        TextField viewerCode = new TextField(docInfo.getViewerCode());
        viewerCode.setDisable(true);

        container.getChildren().addAll(viewerCodeLabel, viewerCode);
        return container;
    }
}