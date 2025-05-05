package com.example.server.service;

import com.example.server.model.CRDTOperation;
import com.example.server.model.Document;
import com.example.server.repository.DocumentRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DocumentService {
    private final DocumentRepository documentRepository;

    private final Map<String, Map<String, Integer>> userLinePositions = new ConcurrentHashMap<>();

    @Autowired
    public DocumentService(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    public void addOperation(CRDTOperation operation, String id) {
        this.documentRepository.addOperation(operation, id);
    }

    public List<CRDTOperation> getOperations(String id) {
        return this.documentRepository.getOperations(id);
    }

    public Document createDocument() {
        Document document = new Document();
        return documentRepository.save(document);
    }

    public Document getDocument(String documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found: " + documentId));
    }

    public Document findByAccessCode(String code) {
        // Try both editor and viewer codes
        return documentRepository.findByEditorCode(code)
                .orElseGet(() -> documentRepository.findByViewerCode(code)
                        .orElseThrow(() -> new RuntimeException("Invalid access code: " + code)));
    }

    /**
     * Update a user's line position in a document
     * 
     * @param documentId   The document ID
     * @param userId       The user ID
     * @param linePosition The current line position (0-based)
     * @throws Exception If document not found
     */
    public void updateUserLinePosition(String documentId, String userId, int linePosition) throws Exception {
        getDocument(documentId);

        // Update line position
        userLinePositions.computeIfAbsent(documentId, k -> new ConcurrentHashMap<>())
                .put(userId, linePosition);
    }

    /**
     * Get all user line positions for a document
     * 
     * @param documentId The document ID
     * @return Map of user IDs to line positions
     * @throws Exception If document not found
     */
    public Map<String, Integer> getUserLinePositions(String documentId) throws Exception {
        getDocument(documentId);

        Map<String, Integer> positions = userLinePositions.get(documentId);
        if (positions == null) {
            return new HashMap<>();
        }
        return new HashMap<>(positions);
    }

    public void addConnectedUser(String documentId, String userId, boolean isEditor) {
        Document document = getDocument(documentId);
        if (isEditor) {
            document.getConnectedEditors().add(userId);
        } else {
            document.getConnectedViewers().add(userId);
        }
        documentRepository.save(document);
    }

    public boolean isUserConnected(String documentId, String userId) throws Exception {
        return getDocument(documentId).getConnectedEditors().contains(userId);
    }

    public void removeConnectedUser(String documentId, String userId) {
        Document document = getDocument(documentId);
        document.getConnectedEditors().remove(userId);
        document.getConnectedViewers().remove(userId);
        userLinePositions.get(documentId).remove(userId);
        documentRepository.save(document);
    }
}