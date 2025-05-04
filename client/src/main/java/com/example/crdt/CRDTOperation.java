package com.example.crdt;

import java.util.Objects;

public class CRDTOperation {
    public OperationType type;
    public String documentID;
    public String userID;
    public String id;
    public char character;
    public int position;
    public String parentNodeId;

    public CRDTOperation(String userID, String docID, OperationType type, char character, int position, String id,
            String parentNodeId) {
        this.userID = userID;
        this.documentID = docID;
        this.type = type;
        this.character = character;
        this.position = position;
        this.id = id;
        this.parentNodeId = parentNodeId;
    }

    @Override
    public String toString() {
        return "CRDTOperation{" +
                "userID='" + userID + '\'' +
                ", docID='" + documentID + '\'' +
                ", type=" + type +
                ", character=" + character +
                ", position=" + position +
                ", id='" + id + '\'' +
                ", parentNodeId='" + parentNodeId + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        CRDTOperation that = (CRDTOperation) o;
        return character == that.character &&
                position == that.position &&
                userID == that.userID &&
                type == that.type &&
                documentID == that.documentID &&
                Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, id, character, position);
    }
}