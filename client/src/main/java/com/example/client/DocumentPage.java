package com.example.client;

import java.io.File;
import java.util.Map;

import com.example.network.NetworkManager;
import com.example.network.NetworkManager.DocumentInfo;
import com.example.ui.ActiveUsersPanel;
import com.example.ui.ToolbarComponent;
import com.example.crdt.CRDTManager;
import com.example.crdt.CRDTOperation;

import javafx.application.Platform;
import javafx.scene.layout.BorderPane;

public class DocumentPage extends BorderPane {
    private final String userId;
    private final NetworkManager networkManager;
    private final DocumentInfo docInfo;
    private File currentFile;

    private final EditorComponent editorComponent;
    private final ToolbarComponent toolbarComponent;
    private final ActiveUsersPanel activeUsersPanel;
    private final CRDTManager crdtManager;
    private final OperationsManager operationsManager;
    private final DocumentLoader documentLoader;

    public DocumentPage(SceneManager sceneManager, NetworkManager networkManager, DocumentInfo documentInfo,
            File currentFile) {
        this.networkManager = networkManager;
        this.docInfo = documentInfo;
        this.currentFile = currentFile;
        this.userId = networkManager.getUserId();

        System.out.printf("\n\nYour userID is: %s\n\n\n", userId);

        // Initialize components
        this.crdtManager = new CRDTManager(userId, documentInfo.getId());
        this.operationsManager = new OperationsManager();
        this.documentLoader = new DocumentLoader(sceneManager);

        this.activeUsersPanel = new ActiveUsersPanel(userId);

        this.editorComponent = new EditorComponent(
                crdtManager,
                operationsManager,
                networkManager,
                documentInfo.getId(),
                !networkManager.isEditor());

        this.toolbarComponent = new ToolbarComponent(
                sceneManager,
                networkManager,
                documentInfo,
                this::saveDocument,
                this::performUndo,
                this::performRedo);

        // Connect to the network
        setupEventHandlers();
        this.networkManager.connectWebSocket();

        setupUI();
        loadContent();
    }

    private void setupUI() {
        setTop(toolbarComponent);

        BorderPane centerLayout = new BorderPane();
        centerLayout.setCenter(editorComponent);
        centerLayout.setRight(activeUsersPanel);

        setCenter(centerLayout);
    }

    private void setupEventHandlers() {
        networkManager.setOnUserStatusChanged(this::handleUserStatusUpdate);
        networkManager.setOnOperationReceived(this::handleRemoteOperation);
        networkManager.setOnConnectionError(this::handleConnectionError);

        editorComponent.setOnTextChange((position, inserted, removed) -> {
            handleTextChanges(position, inserted, removed);
        });

        editorComponent.setOnLineChange((lineNumber) -> {
            networkManager.sendLinePosition(docInfo.getId(), lineNumber);
        });
    }

    private void handleUserStatusUpdate(Map<String, Object> statusUpdate) {
        Platform.runLater(() -> activeUsersPanel.updateActiveUsers(statusUpdate));
    }

    private void handleTextChanges(int position, String inserted, String removed) {
        // Handle deletions
        for (int i = 0; i < removed.length(); i++) {
            CRDTOperation deleteOp = crdtManager.createDeleteOperation(position, docInfo.getId());
            if (deleteOp != null) {
                networkManager.sendOperation(deleteOp);
                operationsManager.recordOperation(deleteOp);
            }
        }

        // Handle insertions
        for (int i = 0; i < inserted.length(); i++) {
            char c = inserted.charAt(i);
            CRDTOperation insertOp = crdtManager.createInsertOperation(c, position + i, docInfo.getId());
            networkManager.sendOperation(insertOp);
            operationsManager.recordOperation(insertOp);
        }
    }

    private void handleRemoteOperation(CRDTOperation op) {
        Platform.runLater(() -> {
            if (!op.userID.equals(userId)) {
                crdtManager.applyOperation(op);
                editorComponent.updateContent(crdtManager.buildText());
            }
        });
    }

    private void loadContent() {
        if (currentFile == null) {
            return;
        }

        String content = documentLoader.loadFileContent(currentFile);
        System.out.println("Loaded content: " + content);

        Platform.runLater(() -> {
            for (int i = 0; i < content.length(); i++) {
                char c = content.charAt(i);
                CRDTOperation op = crdtManager.createInsertOperation(c, i, docInfo.getId());
                networkManager.sendOperation(op);
            }

            editorComponent.updateContent(crdtManager.buildText());
            operationsManager.clearHistory();
        });
    }

    private void saveDocument() {
        if (currentFile == null) {
            currentFile = documentLoader.showSaveDialog();
        }

        if (currentFile != null) {
            documentLoader.saveFile(currentFile, editorComponent.getText());
        }
    }

    private void performUndo() {
        CRDTOperation undoOp = operationsManager.undo();
        if (undoOp != null) {
            crdtManager.applyOperation(undoOp);
            networkManager.sendOperation(undoOp);
            editorComponent.updateContent(crdtManager.buildText());
        }
    }

    private void performRedo() {
        CRDTOperation redoOp = operationsManager.redo();
        if (redoOp != null) {
            crdtManager.applyOperation(redoOp);
            networkManager.sendOperation(redoOp);
            editorComponent.updateContent(crdtManager.buildText());
        }
    }

    private void handleConnectionError(String errorMessage) {
        System.err.println("Network error: " + errorMessage);
    }
}