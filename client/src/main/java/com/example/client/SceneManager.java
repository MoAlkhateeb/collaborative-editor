package com.example.client;

import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.File;

public class SceneManager {
    private Stage stage;
    private DocumentManager documentManager;

    public SceneManager(Stage stage) {
        this.stage = stage;
        this.documentManager = new DocumentManager(this);
    }

    public void showLandingPage() {
        LandingPage landingPage = new LandingPage(this);
        Scene scene = new Scene(landingPage);
        stage.setScene(scene);
    }

    public void showBrowsePage() {
        File selectedFile = documentManager.openFile();
        if (selectedFile != null) {
            showDocumentPage(selectedFile);
        }
    }

    public void showNewDocumentPage() {
        showDocumentPage(null);
    }

    public void showDocumentPage(File file) {
        DocumentPage documentPage = new DocumentPage(this, file);
        Scene scene = new Scene(documentPage);
        stage.setScene(scene);
    }

    public void showJoinSession(String sessionCode) {
        if (sessionCode != null && !sessionCode.trim().isEmpty()) {
            System.out.println("Joining session: " + sessionCode);
            // TODO: Implementation for joining a session
            showDocumentPage(null);
        }
    }

    public Stage getStage() {
        return stage;
    }
}