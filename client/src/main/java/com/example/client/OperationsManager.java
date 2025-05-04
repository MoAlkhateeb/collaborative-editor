package com.example.client;

import java.util.Stack;

import com.example.crdt.CRDTOperation;
import com.example.crdt.OperationType;

/**
 * Manager for handling undo/redo operations
 */
public class OperationsManager {
    private final Stack<CRDTOperation> undoStack;
    private final Stack<CRDTOperation> redoStack;

    public OperationsManager() {
        this.undoStack = new Stack<>();
        this.redoStack = new Stack<>();
    }

    /**
     * Records an operation for potential undo
     */
    public void recordOperation(CRDTOperation operation) {
        undoStack.push(operation);
        redoStack.clear();
    }

    /**
     * Performs an undo operation
     * 
     * @return The inverse operation to apply
     */
    public CRDTOperation undo() {
        if (undoStack.isEmpty()) {
            return null;
        }

        CRDTOperation lastOp = undoStack.pop();
        CRDTOperation inverseOp = createInverseOperation(lastOp);

        if (inverseOp != null) {
            redoStack.push(lastOp);
        }

        return inverseOp;
    }

    /**
     * Performs a redo operation
     * 
     * @return The operation to reapply
     */
    public CRDTOperation redo() {
        if (redoStack.isEmpty()) {
            return null;
        }

        CRDTOperation op = redoStack.pop();
        undoStack.push(op);

        return op;
    }

    /**
     * Creates the inverse of an operation for undo/redo
     */
    private CRDTOperation createInverseOperation(CRDTOperation op) {
        if (op.type == OperationType.INSERT) {
            // Invert INSERT to DELETE
            return new CRDTOperation(
                    op.userID,
                    op.documentID,
                    OperationType.DELETE,
                    op.character,
                    op.position,
                    op.id,
                    op.parentNodeId);
        } else if (op.type == OperationType.DELETE) {
            // Invert DELETE to INSERT
            return new CRDTOperation(
                    op.userID,
                    op.documentID,
                    OperationType.INSERT,
                    op.character,
                    op.position,
                    op.id,
                    op.parentNodeId);
        }

        return null;
    }

    /**
     * Clears the undo/redo history
     */
    public void clearHistory() {
        undoStack.clear();
        redoStack.clear();
    }
}