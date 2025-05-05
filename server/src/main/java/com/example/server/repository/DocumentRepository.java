package com.example.server.repository;

import com.example.server.model.CRDTOperation;
import com.example.server.model.Document;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class DocumentRepository {
    private final Map<String, Document> documents = new ConcurrentHashMap<>();
    private final Map<String, String> editorCodeToDocId = new ConcurrentHashMap<>();
    private final Map<String, String> viewerCodeToDocId = new ConcurrentHashMap<>();

    public void addOperation(CRDTOperation operation, String id) {
        Document doc = documents.get(id);
        if (doc == null) {
            return;
        }

        doc.addOperation(operation);
    }

    public List<CRDTOperation> getOperations(String id) {
        Document doc = documents.get(id);
        if (doc == null) {
            return List.of();
        }
        return doc.getOperations();
    }

    public Document save(Document document) {
        documents.put(document.getId(), document);
        editorCodeToDocId.put(document.getEditorCode(), document.getId());
        viewerCodeToDocId.put(document.getViewerCode(), document.getId());
        return document;
    }

    public Optional<Document> findById(String id) {
        return Optional.ofNullable(documents.get(id));
    }

    public Optional<Document> findByEditorCode(String code) {
        String docId = editorCodeToDocId.get(code);
        return docId != null ? Optional.ofNullable(documents.get(docId)) : Optional.empty();
    }

    public Optional<Document> findByViewerCode(String code) {
        String docId = viewerCodeToDocId.get(code);
        return docId != null ? Optional.ofNullable(documents.get(docId)) : Optional.empty();
    }
}
