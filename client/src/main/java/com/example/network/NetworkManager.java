package com.example.network;

import com.example.crdt.CRDTOperation;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandler;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

public class NetworkManager {
    private static final String SERVER_URL = "http://localhost:8080";
    private static final String WEBSOCKET_URL = "http://localhost:8080/collaborative-editor";

    private static final String USER_API = SERVER_URL + "/api/users";
    private static final String DOCUMENT_API = SERVER_URL + "/api/documents";

    private WebSocketStompClient stompClient;
    private StompSession stompSession;

    private final RestTemplate restTemplate = new RestTemplate();

    private String userId;
    private String documentId;
    private boolean isEditor;
    private String accessCode;
    private int currentLinePosition = 0;

    private Consumer<CRDTOperation> onOperationReceived;
    private Consumer<Map<String, Object>> onUserStatusChanged;
    private Consumer<String> onConnectionError;

    public String initialize() {
        try {
            Map<?, ?> response = restTemplate.postForObject(USER_API, null, Map.class);
            if (response != null && response.containsKey("userId")) {
                this.userId = (String) response.get("userId");
                return this.userId;
            } else {
                throw new RuntimeException("Failed to get user ID from server");
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize network connection: " + e.getMessage(), e);
        }
    }

    public DocumentInfo createDocument() {
        try {
            String url = DOCUMENT_API + "?userId=" + userId;
            Map<?, ?> response = restTemplate.postForObject(url, null, Map.class);

            if (response != null) {
                DocumentInfo docInfo = new DocumentInfo();
                docInfo.setId((String) response.get("id"));
                docInfo.setEditorCode((String) response.get("editorCode"));
                docInfo.setViewerCode((String) response.get("viewerCode"));

                this.documentId = docInfo.getId();
                this.isEditor = true;
                this.accessCode = docInfo.getEditorCode();

                return docInfo;
            } else {
                throw new RuntimeException("Failed to create document");
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to create document: " + e.getMessage(), e);
        }
    }

    public DocumentInfo joinDocument(String code) {
        try {
            String url = DOCUMENT_API + "/access?userId=" + userId + "&accessCode=" + code;
            Map<?, ?> response = restTemplate.postForObject(url, null, Map.class);

            if (response != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> documentMap = (Map<String, Object>) response.get("document");

                DocumentInfo docInfo = new DocumentInfo();
                docInfo.setId((String) documentMap.get("id"));
                docInfo.setEditorCode((String) documentMap.get("editorCode"));
                docInfo.setViewerCode((String) documentMap.get("viewerCode"));

                this.documentId = docInfo.getId();
                this.isEditor = (Boolean) response.get("isEditor");
                this.accessCode = code;

                return docInfo;
            } else {
                throw new RuntimeException("Failed to join document");
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to join document: " + e.getMessage(), e);
        }
    }

    public void connectWebSocket() {
        try {
            List<Transport> transports = new ArrayList<>();
            transports.add(new WebSocketTransport(new StandardWebSocketClient()));
            SockJsClient sockJsClient = new SockJsClient(transports);

            stompClient = new WebSocketStompClient(sockJsClient);
            stompClient.setMessageConverter(new MappingJackson2MessageConverter());

            DocumentStompSessionHandler sessionHandler = new DocumentStompSessionHandler();

            try {
                stompSession = stompClient.connect(WEBSOCKET_URL, sessionHandler).get();
                System.out.println("Successfully connected to WebSocket at: " + WEBSOCKET_URL);

            } catch (InterruptedException | ExecutionException e) {
                String errorMessage = "Failed to connect to WebSocket: " + e.getMessage();
                System.err.println(errorMessage);

                if (e.getCause() != null) {
                    System.err.println("Root cause: " + e.getCause().getMessage());
                }

                if (onConnectionError != null) {
                    onConnectionError.accept(errorMessage);
                }
                throw new RuntimeException(errorMessage, e);
            }

            stompSession.subscribe("/topic/document/" + documentId, new StompSessionHandler() {
                @Override
                public void handleException(StompSession session, StompCommand command, StompHeaders headers,
                        byte[] payload, Throwable exception) {
                    if (onConnectionError != null) {
                        onConnectionError.accept("WebSocket error: " + exception.getMessage());
                    }
                }

                @Override
                public void handleTransportError(StompSession session, Throwable exception) {
                    if (onConnectionError != null) {
                        onConnectionError.accept("Transport error: " + exception.getMessage());
                    }
                }

                @Override
                public Type getPayloadType(StompHeaders headers) {
                    return CRDTOperation.class;
                }

                @Override
                public void handleFrame(StompHeaders headers, Object payload) {
                    if (payload instanceof CRDTOperation && onOperationReceived != null) {
                        CRDTOperation operation = (CRDTOperation) payload;
                        onOperationReceived.accept(operation);
                    }
                }

                @Override
                public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                }
            });

            stompSession.subscribe("/topic/document/" + documentId + "/users", new StompSessionHandler() {
                @Override
                public void handleException(StompSession session, StompCommand command, StompHeaders headers,
                        byte[] payload, Throwable exception) {
                    if (onConnectionError != null) {
                        onConnectionError.accept("WebSocket error: " + exception.getMessage());
                    }
                }

                @Override
                public void handleTransportError(StompSession session, Throwable exception) {
                    if (onConnectionError != null) {
                        onConnectionError.accept("Transport error: " + exception.getMessage());
                    }
                }

                @Override
                public Type getPayloadType(StompHeaders headers) {
                    return Map.class;
                }

                @SuppressWarnings("unchecked")
                @Override
                public void handleFrame(StompHeaders headers, Object payload) {
                    if (payload instanceof Map && onUserStatusChanged != null) {
                        onUserStatusChanged.accept((Map<String, Object>) payload);
                    }
                }

                @Override
                public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                }
            });

            Map<String, String> joinRequest = new HashMap<>();
            joinRequest.put("documentId", documentId);
            joinRequest.put("userId", userId);
            joinRequest.put("accessCode", accessCode);
            joinRequest.put("linePosition", "0");

            stompSession.send("/app/join", joinRequest);

        } catch (Exception e) {
            String errorMessage = "Failed to connect to WebSocket: " + e.getMessage();
            System.err.println(errorMessage);

            if (onConnectionError != null) {
                onConnectionError.accept(errorMessage);
            }
            throw new RuntimeException(errorMessage, e);
        }
    }

    /**
     * Send an operation to the server to be broadcast to other clients.
     * 
     * @param operation The CRDT operation to send
     */
    public void sendOperation(CRDTOperation operation) {
        if (stompSession == null || !stompSession.isConnected()) {
            if (onConnectionError != null) {
                onConnectionError.accept("Not connected to WebSocket");
            }
            return;
        }

        // Only editors can send operations
        if (!isEditor) {
            if (onConnectionError != null) {
                onConnectionError.accept("You don't have editor permissions");
            }
            return;
        }

        try {
            stompSession.send("/app/operations", operation);
        } catch (Exception e) {
            if (onConnectionError != null) {
                onConnectionError.accept("Failed to send operation: " + e.getMessage());
            }
        }
    }

    public void sendLinePosition(String documentId, int linePosition) {
        if (stompSession == null || !stompSession.isConnected()) {
            if (onConnectionError != null) {
                onConnectionError.accept("Not connected to WebSocket");
            }
            return;
        }

        if (linePosition != currentLinePosition) {
            currentLinePosition = linePosition;
            try {
                Map<String, Object> lineUpdate = new HashMap<>();
                lineUpdate.put("documentId", documentId);
                lineUpdate.put("userId", userId);
                lineUpdate.put("linePosition", linePosition);

                stompSession.send("/app/line-position", lineUpdate);
            } catch (Exception e) {
                if (onConnectionError != null) {
                    onConnectionError.accept("Failed to send line position: " + e.getMessage());
                }
            }
        }
    }

    public void disconnect() {
        if (stompSession != null && stompSession.isConnected()) {
            try {
                Map<String, String> leaveRequest = new HashMap<>();
                leaveRequest.put("documentId", documentId);
                leaveRequest.put("userId", userId);

                stompSession.send("/app/leave", leaveRequest);
                stompSession.disconnect();
                stompSession = null;
                System.out.println("Successfully disconnected from WebSocket");
            } catch (Exception e) {
                System.err.println("Error during disconnect: " + e.getMessage());
            }
        }
    }

    public void setOnOperationReceived(Consumer<CRDTOperation> callback) {
        this.onOperationReceived = callback;
    }

    public void setOnUserStatusChanged(Consumer<Map<String, Object>> callback) {
        this.onUserStatusChanged = callback;
    }

    public void setOnConnectionError(Consumer<String> callback) {
        this.onConnectionError = callback;
    }

    public String getUserId() {
        return userId;
    }

    public boolean isEditor() {
        return isEditor;
    }

    private class DocumentStompSessionHandler implements StompSessionHandler {
        @Override
        public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
            System.out.println("Connected to WebSocket session: " + session.getSessionId());
        }

        @Override
        public void handleException(StompSession session, StompCommand command, StompHeaders headers, byte[] payload,
                Throwable exception) {
            String errorMessage = "WebSocket exception: " + exception;
            System.err.println(errorMessage);
            if (exception.getCause() != null) {
                System.err.println("Root cause: " + exception.getCause().getMessage());
            }

            if (onConnectionError != null) {
                onConnectionError.accept(errorMessage);
            }
        }

        @Override
        public void handleTransportError(StompSession session, Throwable exception) {
            String errorMessage = "WebSocket transport error: " + exception.getMessage();
            System.err.println(errorMessage);
            if (exception.getCause() != null) {
                System.err.println("Root cause: " + exception.getCause().getMessage());
            }

            if (onConnectionError != null) {
                onConnectionError.accept(errorMessage);
            }
        }

        @Override
        public Type getPayloadType(StompHeaders headers) {
            return Object.class;
        }

        @Override
        public void handleFrame(StompHeaders headers, Object payload) {
        }
    }

    public static class DocumentInfo {
        private String id;
        private String editorCode;
        private String viewerCode;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getEditorCode() {
            return editorCode;
        }

        public void setEditorCode(String editorCode) {
            this.editorCode = editorCode;
        }

        public String getViewerCode() {
            return viewerCode;
        }

        public void setViewerCode(String viewerCode) {
            this.viewerCode = viewerCode;
        }
    }
}