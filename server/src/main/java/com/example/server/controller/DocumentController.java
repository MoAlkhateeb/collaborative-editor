package com.example.server.controller;

import com.example.server.model.Document;
import com.example.server.service.DocumentService;
import com.example.server.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/documents")
@CrossOrigin(origins = "*")
public class DocumentController {
    private final DocumentService documentService;
    private final UserService userService;

    @Autowired
    public DocumentController(DocumentService documentService, UserService userService) {
        this.documentService = documentService;
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<Document> createDocument(@RequestParam String userId) {
        Document document = documentService.createDocument();

        userService.updateUserDocument(userId, document.getId());

        return ResponseEntity.ok(document);
    }

    @GetMapping("/{documentId}")
    public ResponseEntity<Document> getDocument(@PathVariable String documentId) {
        Document document = documentService.getDocument(documentId);
        return ResponseEntity.ok(document);
    }

    @PostMapping("/access")
    public ResponseEntity<Map<String, Object>> accessDocument(
            @RequestParam String userId,
            @RequestParam String accessCode) {
        Document document = documentService.findByAccessCode(accessCode);

        boolean isEditor = document.getEditorCode().equals(accessCode);

        userService.updateUserDocument(userId, document.getId());

        documentService.addConnectedUser(document.getId(), userId, isEditor);

        Map<String, Object> response = new HashMap<>();
        response.put("document", document);
        response.put("isEditor", isEditor);

        return ResponseEntity.ok(response);
    }
}
