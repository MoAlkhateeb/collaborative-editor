package com.example.server.model;

import lombok.Data;
import java.util.UUID;

@Data
public class User {
    private String userId;
    private String currentDocumentId;

    public User() {
        this.userId = UUID.randomUUID().toString();
    }
}