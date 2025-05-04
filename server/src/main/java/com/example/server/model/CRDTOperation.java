package com.example.server.model;

import java.util.Objects;

public class CRDTOperation {
    public OperationType type;
    public String documentID;
    public String userID;
    public String id;
    public char character;
    public int position;
    public String parentNodeId;

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