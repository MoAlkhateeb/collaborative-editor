package com.example.client;

import java.util.function.Consumer;
import java.util.function.IntFunction;

import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.model.PlainTextChange;
import org.fxmisc.richtext.model.TwoDimensional.Position;
import org.reactfx.Subscription;

import com.example.crdt.CRDTManager;

import javafx.beans.value.ChangeListener;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;

/**
 * Component responsible for text editing functionality
 */
public class EditorComponent extends BorderPane {
    private final CodeArea codeArea;
    private int caretOffset;
    private int currentLinePosition = 0;
    private boolean isUndoRedoOperation = false;

    private TriConsumer<Integer, String, String> onTextChange;
    private Consumer<Integer> onLineChange;

    private Subscription textChangeSubscription;
    private ChangeListener<Number> caretPositionListener;

    private final CRDTManager crdtManager;

    public EditorComponent(CRDTManager crdtManager, OperationsManager operationsManager,
            Object networkManager, String documentId, boolean readOnly) {
        this.crdtManager = crdtManager;

        this.codeArea = new CodeArea();
        codeArea.setDisable(readOnly);

        setupEditor();
        setupListeners();
    }

    public void setOnTextChange(TriConsumer<Integer, String, String> onTextChange) {
        this.onTextChange = onTextChange;
    }

    public void setOnLineChange(Consumer<Integer> onLineChange) {
        this.onLineChange = onLineChange;
    }

    public String getText() {
        return codeArea.getText();
    }

    private void setupEditor() {
        IntFunction<Node> lineNumberFactory = LineNumberFactory.get(codeArea,
                digits -> " %" + digits + "d ");

        codeArea.setParagraphGraphicFactory(lineNumberFactory);

        codeArea.getStylesheets().add(getClass().getResource("/styles/editor-styles.css").toExternalForm());

        VirtualizedScrollPane<CodeArea> scrollPane = new VirtualizedScrollPane<>(codeArea);
        setCenter(scrollPane);
    }

    private void setupListeners() {
        setupCaretPositionListener();
        listenToTextChanges();
    }

    private void setupCaretPositionListener() {
        caretPositionListener = (obs, oldPos, newPos) -> {
            Position pos = codeArea.offsetToPosition(newPos.intValue(), null);
            int lineNumber = pos.getMajor();

            crdtManager.updateCurrentNodeTracking(newPos.intValue());

            if (lineNumber != currentLinePosition) {
                System.out.println("Line position change: " + lineNumber);
                currentLinePosition = lineNumber;

                if (onLineChange != null) {
                    onLineChange.accept(lineNumber);
                }
            }
        };

        codeArea.caretPositionProperty().addListener(caretPositionListener);
    }

    private void listenToTextChanges() {
        textChangeSubscription = codeArea.plainTextChanges()
                .filter(change -> !change.getInserted().isEmpty() || !change.getRemoved().isEmpty())
                .filter(change -> !isUndoRedoOperation)
                .subscribe(this::handleTextChange);
    }

    private void handleTextChange(PlainTextChange change) {
        if (isUndoRedoOperation) {
            return;
        }

        int position = change.getPosition();
        String inserted = change.getInserted();
        String removed = change.getRemoved();

        if (onTextChange != null) {
            onTextChange.accept(position, inserted, removed);
        }

        caretOffset = codeArea.getCaretPosition();

        // Update line position if needed
        Position pos = codeArea.offsetToPosition(caretOffset, null);
        int lineNumber = pos.getMajor();

        if (lineNumber != currentLinePosition) {
            currentLinePosition = lineNumber;
            if (onLineChange != null) {
                onLineChange.accept(lineNumber);
            }
        }
    }

    public void updateContent(String newText) {
        if (textChangeSubscription != null) {
            textChangeSubscription.unsubscribe();
        }
        codeArea.caretPositionProperty().removeListener(caretPositionListener);

        try {
            isUndoRedoOperation = true;

            // Calculate new caret position
            int newPosition = crdtManager.getCaretPosition();
            if (newPosition < 0) {
                boolean wasAtEnd = caretOffset == codeArea.getLength();
                newPosition = wasAtEnd ? newText.length() : Math.min(caretOffset, codeArea.getLength());
            }

            // Update content
            codeArea.replaceText(newText);

            // Set caret position
            int safePosition = Math.min(newPosition, codeArea.getLength());
            codeArea.moveTo(safePosition);
            caretOffset = safePosition;

        } finally {
            isUndoRedoOperation = false;
            listenToTextChanges();
            setupCaretPositionListener();
        }
    }

    @FunctionalInterface
    public interface TriConsumer<A, B, C> {
        void accept(A a, B b, C c);
    }
}