package com.example.crdt;

public class CRDTManager {
    private final CRDTDocument crdt;
    private final String userId;

    // Tracks position by node ID instead of absolute position
    private String currentNodeId;

    public CRDTManager(String userId, String documentId) {
        this.userId = userId;
        this.crdt = new CRDTDocument(userId);
    }

    /**
     * Creates an insert operation at the specified position
     */
    public CRDTOperation createInsertOperation(char c, int position, String docId) {
        String parentId = crdt.getInsertParentIdByPosition(position);
        CRDTNode insertedNode = crdt.insert(c, parentId);
        currentNodeId = insertedNode.id;

        return new CRDTOperation(
                userId,
                docId,
                OperationType.INSERT,
                c,
                position,
                insertedNode.id,
                parentId);
    }

    /**
     * Creates a delete operation at the specified position
     */
    public CRDTOperation createDeleteOperation(int position, String docId) {
        CRDTNode node = crdt.getNodeByPosition(position);
        if (node != null) {
            String parentId = node.parent != null ? node.parent.id : null;
            CRDTOperation deleteOp = new CRDTOperation(
                    userId,
                    docId,
                    OperationType.DELETE,
                    node.value,
                    position,
                    node.id,
                    parentId);

            crdt.delete(node.id);
            return deleteOp;
        }
        return null;
    }

    /**
     * Applies an operation to the CRDT document
     */
    public void applyOperation(CRDTOperation op) {
        System.out.println("Applying operation: " + op);

        if (op.type == OperationType.INSERT) {
            String parentId = (op.parentNodeId != null)
                    ? op.parentNodeId
                    : crdt.getInsertParentIdByPosition(op.position);

            crdt.insertWithId(op.id, op.character, parentId);
        } else if (op.type == OperationType.DELETE) {
            crdt.delete(op.id);
        }
    }

    /**
     * Updates the current node tracking based on cursor position
     */
    public void updateCurrentNodeTracking(int position) {
        if (position >= 0) {
            CRDTNode node = crdt.getNodeByPosition(position);
            if (node != null) {
                currentNodeId = node.id;
            } else {
                String parentId = crdt.getInsertParentIdByPosition(position);
                currentNodeId = parentId;
            }
        }
    }

    /**
     * Gets the current caret position
     */
    public int getCaretPosition() {
        return (currentNodeId != null)
                ? crdt.getVisiblePositionByNodeID(currentNodeId)
                : -1;
    }

    /**
     * Builds the text from the CRDT document
     */
    public String buildText() {
        return crdt.buildText();
    }
}