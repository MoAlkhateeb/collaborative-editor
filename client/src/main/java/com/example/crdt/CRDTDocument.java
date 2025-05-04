package com.example.crdt;

import java.util.*;

public class CRDTDocument {
    private final CRDTNode root = new CRDTNode("system", "root", '\0', null);
    private final Map<String, CRDTNode> nodeMap = new HashMap<>();
    private final Map<String, List<PendingNode>> pendingInserts = new HashMap<>();
    private final String userId;
    private int clock = 0;

    private static final Comparator<CRDTNode> NODE_ORDER = Comparator
            .comparing((CRDTNode n) -> Integer.parseInt(n.clock)).reversed()
            .thenComparing(n -> n.userId);

    public CRDTDocument(String userId) {
        this.userId = userId;
        nodeMap.put(root.id, root);
    }

    public CRDTNode insert(char value, String parentId) {
        return insertWithId(UUID.randomUUID().toString(), value, parentId);
    }

    public CRDTNode insertWithId(String id, char c, String parentId) {
        CRDTNode parent = nodeMap.get(parentId);
        if (parent == null) {
            pendingInserts
                    .computeIfAbsent(parentId, k -> new ArrayList<>())
                    .add(new PendingNode(id, c, parentId));
            return null;
        }

        CRDTNode newNode = new CRDTNode(userId, String.format("%05d", clock++), c, parent, id);
        parent.children.add(newNode);
        nodeMap.put(newNode.id, newNode);

        processPendingInserts(id);

        return newNode;
    }

    private void processPendingInserts(String resolvedParentId) {
        List<PendingNode> pendings = pendingInserts.remove(resolvedParentId);
        if (pendings == null)
            return;

        for (PendingNode pending : pendings) {
            insertWithId(pending.id, pending.value, pending.parentId);
        }
    }

    public void delete(String id) {
        CRDTNode node = nodeMap.get(id);
        if (node != null)
            node.deleted = true;
    }

    public String buildText() {
        StringBuilder sb = new StringBuilder();
        dfsCollect(root, sb);
        return sb.toString();
    }

    private void dfsCollect(CRDTNode node, StringBuilder sb) {
        if (node != root && !node.deleted)
            sb.append(node.value);
        node.children.sort(NODE_ORDER);
        for (CRDTNode child : node.children)
            dfsCollect(child, sb);
    }

    public CRDTNode getNodeByPosition(int pos) {
        List<CRDTNode> flat = flattenVisible();
        return (pos >= 0 && pos < flat.size()) ? flat.get(pos) : null;
    }

    public int getVisiblePositionByNodeID(String id) {
        if (id == null || id.equals(root.id))
            return 0;

        List<CRDTNode> flat = flattenVisible();
        for (int i = 0; i < flat.size(); i++) {
            if (flat.get(i).id.equals(id))
                return i;
        }

        CRDTNode node = nodeMap.get(id);
        if (node != null && node.parent != null) {
            return getVisiblePositionByNodeID(node.parent.id);
        }

        return -1;
    }

    public String getInsertParentIdByPosition(int pos) {
        List<CRDTNode> flat = flattenVisible();
        if (pos <= 0)
            return root.id;
        if (pos >= flat.size())
            return flat.get(flat.size() - 1).id;
        return flat.get(pos - 1).id;
    }

    private List<CRDTNode> flattenVisible() {
        List<CRDTNode> list = new ArrayList<>();
        collectVisible(root, list);
        return list;
    }

    private void collectVisible(CRDTNode node, List<CRDTNode> into) {
        if (node != root && !node.deleted)
            into.add(node);
        node.children.sort(NODE_ORDER);
        for (CRDTNode child : node.children)
            collectVisible(child, into);
    }

    private static class PendingNode {
        final String id;
        final char value;
        final String parentId;

        PendingNode(String id, char value, String parentId) {
            this.id = id;
            this.value = value;
            this.parentId = parentId;
        }
    }
}
