package com.example.server.controller;

import com.example.server.model.CRDTOperation;
import com.example.server.model.Document;
import com.example.server.service.DocumentService;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class WebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final DocumentService documentService;

    @Autowired
    public WebSocketController(SimpMessagingTemplate messagingTemplate, DocumentService documentService) {
        this.messagingTemplate = messagingTemplate;
        this.documentService = documentService;
    }

    @MessageMapping("/operations")
    public void handleOperation(@Payload CRDTOperation operation) {
        // Verify user has access to the document
        String documentId = operation.documentID;
        String userId = operation.userID;

        // Get the document and check if user is connected
        try {
            documentService.getDocument(documentId);

            System.out.println("RECEIVED OPERATION: " + operation);
            documentService.addOperation(operation, documentId);

            // Broadcast the operation to all clients subscribed to the document
            messagingTemplate.convertAndSend("/topic/document/" + documentId, operation);
        } catch (Exception e) {
            System.out.println("Error handling operation " + operation + " from " + userId);
        }
    }

    @MessageMapping("/join")
    public void joinDocument(@Payload Map<String, String> joinRequest) {
        String documentId = joinRequest.get("documentId");
        String userId = joinRequest.get("userId");
        String accessCode = joinRequest.get("accessCode");

        // Get initial line position if provided
        int linePosition = 0;
        if (joinRequest.containsKey("linePosition")) {
            try {
                linePosition = Integer.parseInt(joinRequest.get("linePosition"));
            } catch (NumberFormatException e) {
            }
        }

        try {
            // Validate access
            Document document = documentService.getDocument(documentId);
            boolean isEditor = document.getEditorCode().equals(accessCode);

            // Add user to connected users
            documentService.addConnectedUser(documentId, userId, isEditor);

            // Store the initial line position
            documentService.updateUserLinePosition(documentId, userId, linePosition);

            // Notify other users about the new user
            Map<String, Object> joinNotification = new HashMap<>();
            joinNotification.put("type", "USER_JOINED");
            joinNotification.put("userId", userId);
            joinNotification.put("isEditor", isEditor);
            joinNotification.put("linePosition", linePosition);

            for (CRDTOperation op : documentService.getOperations(documentId)) {
                messagingTemplate.convertAndSend("/topic/document/" + documentId, op);
            }

            messagingTemplate.convertAndSend("/topic/document/" + documentId + "/users", joinNotification);

            broadcastUserPositions(documentId);

        } catch (Exception e) {
            // Handle error
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            messagingTemplate.convertAndSendToUser(userId, "/topic/errors", errorResponse);
        }
    }

    @MessageMapping("/leave")
    public void leaveDocument(@Payload Map<String, String> leaveRequest) {
        String documentId = leaveRequest.get("documentId");
        String userId = leaveRequest.get("userId");

        try {
            // Remove user from connected users
            documentService.removeConnectedUser(documentId, userId);

            // Notify other users
            Map<String, Object> leaveNotification = new HashMap<>();
            leaveNotification.put("type", "USER_LEFT");
            leaveNotification.put("userId", userId);

            messagingTemplate.convertAndSend("/topic/document/" + documentId + "/users", leaveNotification);

            // After user leaves, broadcast updated positions
            broadcastUserPositions(documentId);

        } catch (Exception e) {
            // Handle error
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            messagingTemplate.convertAndSendToUser(userId, "/topic/errors", errorResponse);
        }
    }

    @MessageMapping("/line-position")
    public void handleLinePosition(@Payload Map<String, Object> lineUpdate) {
        String documentId = (String) lineUpdate.get("documentId");
        String userId = (String) lineUpdate.get("userId");

        // Parse line position
        int linePosition = 0;
        Object linePositionObj = lineUpdate.get("linePosition");
        if (linePositionObj instanceof Integer) {
            linePosition = (Integer) linePositionObj;
        } else if (linePositionObj instanceof String) {
            try {
                linePosition = Integer.parseInt((String) linePositionObj);
            } catch (NumberFormatException e) {
                System.out.println("Invalid line position format: " + linePositionObj);
            }
        }

        try {
            // Check if user is connected to this document
            if (documentService.isUserConnected(documentId, userId)) {
                // Update user's line position
                documentService.updateUserLinePosition(documentId, userId, linePosition);

                // Broadcast updated positions to all users
                broadcastUserPositions(documentId);
            }
        } catch (Exception e) {
            System.out.println("Error handling line position update from " + userId + ": " + e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            messagingTemplate.convertAndSendToUser(userId, "/topic/errors", errorResponse);
        }
    }

    /**
     * Broadcasts the current line positions of all connected users to everyone
     * viewing the document
     * 
     * @param documentId The document ID
     */
    private void broadcastUserPositions(String documentId) {
        try {
            // Get all user positions for this document
            Map<String, Integer> userPositions = documentService.getUserLinePositions(documentId);

            System.out.println("BROADCASTING LINE NUMBERS " + userPositions);

            // Create notification with all positions
            Map<String, Object> positionsUpdate = new HashMap<>();
            positionsUpdate.put("type", "USER_POSITIONS");
            positionsUpdate.put("positions", userPositions);

            // Broadcast to all clients
            messagingTemplate.convertAndSend("/topic/document/" + documentId + "/users", positionsUpdate);
        } catch (Exception e) {
            System.out.println("Error broadcasting user positions: " + e.getMessage());
        }
    }
}