package com.example.client;

import java.io.File;
import java.util.function.IntFunction;

import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.reactfx.Subscription;

import javafx.scene.Node;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;

public class DocumentPage extends BorderPane {
    private static final int TOOLBAR_SPACING = 10;
    private static final int TOOLBAR_PADDING = 10;

    private SceneManager sceneManager;
    private File currentFile;
    private CodeArea codeArea;
    private Subscription highlightingSubscription;

    private UndoRedoManager undoRedoManager;

    public DocumentPage(SceneManager sceneManager, File file) {
        this.sceneManager = sceneManager;
        this.currentFile = file;
        this.undoRedoManager = new UndoRedoManager();

        setupUI();
        loadContent();
    }

    private void setupUI() {
        HBox toolbar = createToolbar();
        setTop(toolbar);

        codeArea = new CodeArea();

        IntFunction<String> format = (digits -> " %" + digits + "d ");
        IntFunction<Node> lineNumberFactory = LineNumberFactory.get(codeArea, format);
        codeArea.setParagraphGraphicFactory(lineNumberFactory);

        codeArea.getStylesheets().add(getClass().getResource("/styles/editor-styles.css").toExternalForm());

        VirtualizedScrollPane<CodeArea> scrollPane = new VirtualizedScrollPane<>(codeArea);
        setCenter(scrollPane);

        codeArea.textProperty().addListener((obs, oldText, newText) -> {
            if (oldText != null && !oldText.equals(newText)) {
                undoRedoManager.saveState(newText);
            }
        });
    }

    private HBox createToolbar() {
        HBox toolbar = new HBox(TOOLBAR_SPACING);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(TOOLBAR_PADDING));

        Button backButton = new Button("Back to Home");
        backButton.setOnAction(e -> {
            if (highlightingSubscription != null) {
                highlightingSubscription.unsubscribe();
            }
            sceneManager.showLandingPage();
        });

        Button undoButton = new Button("Undo");
        undoButton.setOnAction(e -> codeArea.undo());

        Button redoButton = new Button("Redo");
        redoButton.setOnAction(e -> codeArea.redo());

        Button saveButton = new Button("Save");
        saveButton.setOnAction(e -> saveDocument());

        Label editorCodeLabel = new Label("Editor Code: ");
        TextField editorCode = new TextField("123456");
        editorCode.setDisable(true);

        Label viewerCodeLabel = new Label("Viewer Code: ");
        TextField viewerCode = new TextField("987654");
        viewerCode.setDisable(true);

        toolbar.getChildren().addAll(backButton, undoButton, redoButton, saveButton, editorCodeLabel, editorCode,
                viewerCodeLabel, viewerCode);

        return toolbar;
    }

    private void loadContent() {
        if (currentFile != null) {
            DocumentManager documentManager = new DocumentManager(sceneManager);
            String content = documentManager.loadFileContent(currentFile);
            codeArea.replaceText(content);
            undoRedoManager.saveState(content);
        }
    }

    private void saveDocument() {
        if (currentFile == null) {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save Document");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Text Files", "*.txt"));
            currentFile = fileChooser.showSaveDialog(sceneManager.getStage());
        }

        if (currentFile != null) {
            DocumentManager documentManager = new DocumentManager(sceneManager);
            documentManager.saveFile(currentFile, codeArea.getText());
        }
    }
}