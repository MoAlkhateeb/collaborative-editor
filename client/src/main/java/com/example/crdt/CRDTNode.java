package com.example.crdt;

import java.util.ArrayList;
import java.util.List;

public class CRDTNode {
    public final String userId;
    public final String clock;
    public final String id;
    public final char value;
    public boolean deleted = false;
    public final CRDTNode parent;
    public final List<CRDTNode> children = new ArrayList<>();

    public CRDTNode(String userId, String clock, char value, CRDTNode parent, String nodeID) {
        this.userId = userId;
        this.clock = clock;
        this.id = nodeID;
        this.value = value;
        this.parent = parent;
    }

    public CRDTNode(String userId, String clock, char value, CRDTNode parent) {
        this.userId = userId;
        this.clock = clock;
        this.id = userId + ":" + clock;
        this.value = value;
        this.parent = parent;
    }
}
