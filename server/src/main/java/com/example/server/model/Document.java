package com.example.server.model;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.*;

@Data
public class Document {
    private String id;
    private LocalDateTime createdAt;
    private String editorCode;
    private String viewerCode;
    private Set<String> connectedEditors;
    private Set<String> connectedViewers;

    private List<CRDTOperation> operations;

    public Document() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = LocalDateTime.now();
        this.editorCode = generateCode("ED");
        this.viewerCode = generateCode("VW");
        this.connectedEditors = new HashSet<>();
        this.connectedViewers = new HashSet<>();
        this.operations = new LinkedList<>();
    }

    public void addOperation(CRDTOperation op) {
        operations.add(op);
    }

    private String generateCode(String prefix) {
        // Generate a 6-character alphanumeric code
        String alphanumeric = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder code = new StringBuilder(prefix);
        Random random = new Random();

        for (int i = 0; i < 6; i++) {
            int index = random.nextInt(alphanumeric.length());
            code.append(alphanumeric.charAt(index));
        }

        return code.toString();
    }
}
