package com.example.client;

import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.File;

import com.example.network.NetworkManager;
import com.example.network.NetworkManager.DocumentInfo;
import com.example.ui.LandingPage;

public class SceneManager {

    private Stage stage;
    private DocumentLoader documentLoader;
    private NetworkManager networkManager;

    public SceneManager(Stage stage) {
        this.stage = stage;
        this.networkManager = new NetworkManager();
        this.documentLoader = new DocumentLoader(this);
        networkManager.initialize();
    }

    public void showLandingPage() {
        LandingPage landingPage = new LandingPage(this);
        Scene scene = new Scene(landingPage);
        stage.setScene(scene);
    }

    public void showBrowsePage() {
        File selectedFile = documentLoader.openFile();
        if (selectedFile == null) {
            return;
        }

        DocumentInfo docInfo = networkManager.createDocument();
        if (docInfo == null) {
            System.out.println("Failed to create document.");
            return;
        }
        showDocumentPage(docInfo, selectedFile);
    }

    public void showNewDocumentPage() {
        DocumentInfo docInfo = networkManager.createDocument();
        if (docInfo == null) {
            System.out.println("Failed to create document.");
            return;
        }

        showDocumentPage(docInfo, null);
    }

    public void showDocumentPage(DocumentInfo docInfo, File currentFile) {
        DocumentPage documentPage = new DocumentPage(this, networkManager, docInfo, currentFile);
        Scene scene = new Scene(documentPage);
        stage.setScene(scene);
    }

    public void showJoinSession(String sessionCode) {
        sessionCode = sessionCode.toUpperCase();
        if (sessionCode != null && !sessionCode.trim().isEmpty()) {
            System.out.println("Joining session: " + sessionCode);
            DocumentInfo docInfo = networkManager.joinDocument(sessionCode);

            if (docInfo == null) {
                System.out.println("Could not join session.");
                return;
            }

            showDocumentPage(docInfo, null);
        }
    }

    public Stage getStage() {
        return stage;
    }
}