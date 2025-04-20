package src.server;

import src.common.Constants;
import src.common.Message;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import org.bson.types.ObjectId;

public class Server {
    private ServerSocket serverSocket;
    private String ipAddress;
    private int port;
    private String password;
    private boolean isListening;
    private ScreenCapturer screenCapturer;
    private ChatManager chatManager;
    private InputHandler inputHandler;
    private org.bson.types.ObjectId currentSessionId;
    
    private ConcurrentHashMap<String, ClientHandler> connectedClients;
    private String clientWithControl;
    
    private List<ServerEventListener> listeners;

    public void setJpegQuality(float quality) {
        Constants.JPEG_QUALITY = quality;
    }

    public void enableAutoFpsAdjustment(boolean enable) {
        if (screenCapturer != null) {
            screenCapturer.enableAutoFpsAdjustment(enable);
        }
    }

    public void setCurrentSessionId(org.bson.types.ObjectId sessionId) {
        this.currentSessionId = sessionId;
    }
    
    public org.bson.types.ObjectId getCurrentSessionId() {
        return this.currentSessionId;
    }
    
    public void setTargetFps(int fps) {
        if (screenCapturer != null) {
            screenCapturer.setTargetFps(fps);
        }
    }
    
    public Server() {
        this.connectedClients = new ConcurrentHashMap<>();
        this.listeners = new ArrayList<>();
        this.isListening = false;
        this.clientWithControl = null;
        
        this.screenCapturer = new ScreenCapturer();
        this.chatManager = new ChatManager();
        this.inputHandler = new InputHandler();
    }
    
    public void startServer(String ipAddress, int port, String password) {
        this.ipAddress = ipAddress;
        this.port = port;
        this.password = password;
        
        if (isListening) {
            return;
        }
        
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(port, 50, InetAddress.getByName(ipAddress));
                isListening = true;
                
                // Notify listeners that server started
                for (ServerEventListener listener : listeners) {
                    listener.onServerStarted();
                }
                
                // Start screen capturing
                screenCapturer.startCapturing();
                
                // Accept client connections
                while (isListening) {
                    Socket clientSocket = serverSocket.accept();
                    
                    // Create client handler
                    new ClientHandler(this, clientSocket);
                }
            } catch (IOException e) {
                // Notify listeners of failure
                for (ServerEventListener listener : listeners) {
                    listener.onServerError(e.getMessage());
                }
            } finally {
                stopServer();
            }
        }).start();
    }
    
    public void stopServer() {
        if (!isListening) {
            return;
        }
        
        isListening = false;
        
        // Stop screen capturing
        screenCapturer.stopCapturing();
        
        // Disconnect all clients
        for (ClientHandler handler : connectedClients.values()) {
            handler.disconnect();
        }
        
        connectedClients.clear();
        
        // Close server socket
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            // Silent close
        }
        
        // Notify listeners
        for (ServerEventListener listener : listeners) {
            listener.onServerStopped();
        }
    }
    
    public void addClient(String clientName, ClientHandler clientHandler) {
        connectedClients.put(clientName, clientHandler);
        
        // Notify listeners
        for (ServerEventListener listener : listeners) {
            listener.onClientConnected(clientName);
        }
    }
    
    public void removeClient(String clientName) {
        connectedClients.remove(clientName);
        
        // If this client had control, revoke it
        if (clientName.equals(clientWithControl)) {
            clientWithControl = null;
        }
        
        // Notify listeners
        for (ServerEventListener listener : listeners) {
            listener.onClientDisconnected(clientName);
        }
    }
    
    public void grantControl(String clientName) {
        // Revoke control from current client if exists
        if (clientWithControl != null) {
            revokeControl(clientWithControl);
        }
        
        clientWithControl = clientName;
        ClientHandler handler = connectedClients.get(clientName);
        
        if (handler != null) {
            // Send control grant message
            Message controlMessage = new Message(
                Constants.MESSAGE_TYPE_CONTROL_GRANT,
                "Server",
                "Control granted"
            );
            handler.sendMessage(controlMessage);
            
            // Notify listeners
            for (ServerEventListener listener : listeners) {
                listener.onControlGranted(clientName);
            }
        }
    }
    
    public void revokeControl(String clientName) {
        ClientHandler handler = connectedClients.get(clientName);
        
        if (handler != null && clientName.equals(clientWithControl)) {
            // Send control revoke message
            Message controlMessage = new Message(
                Constants.MESSAGE_TYPE_CONTROL_REVOKE,
                "Server",
                "Control revoked"
            );
            handler.sendMessage(controlMessage);
            
            clientWithControl = null;
            
            // Notify listeners
            for (ServerEventListener listener : listeners) {
                listener.onControlRevoked(clientName);
            }
        }
    }
    
    public void broadcastMessage(Message message) {
        String sender = message.getSender();
        boolean isServerMessage = sender.equals("Server (Host)"); // Check if the server is the sender
        
        // Send message to all connected clients except the sender
        for (ClientHandler handler : connectedClients.values()) {
            if (!handler.getClientName().equals(sender)) {
                handler.sendMessage(message);
            }
        }
        
        // Add to chat manager
        if (message.getType() == Constants.MESSAGE_TYPE_CHAT || 
            message.getType() == Constants.MESSAGE_TYPE_FILE) {
            chatManager.addMessage(message);
        }
        
        // Only notify listeners if the message is not from the server
        // This prevents server's own messages from being echoed back
        if (!isServerMessage) {
            if (message.getType() == Constants.MESSAGE_TYPE_CHAT) {
                for (ServerEventListener listener : listeners) {
                    listener.onChatMessageReceived(message.getSender(), message.getContent());
                }
            } else if (message.getType() == Constants.MESSAGE_TYPE_FILE) {
                // For files, also save them for server access
                String downloadDir = "server_downloads";
                try {
                    src.common.FileTransfer.saveFile(message.getData(), message.getContent(), downloadDir);
                } catch (Exception e) {
                    // Silent exception handling
                }
                
                for (ServerEventListener listener : listeners) {
                    listener.onFileReceived(message.getSender(), message.getContent());
                }
            }
        }
    }
    
    private void handleFileForServer(String sender, String fileName, byte[] fileData) {
        // Save the file data for server access
        try {
            String downloadDir = "server_downloads";
            src.common.FileTransfer.saveFile(fileData, fileName, downloadDir);
        } catch (Exception e) {
            // Silent exception handling
        }
    }
    
    public void processInputEvent(Message message) {
        String sender = message.getSender();
        
        // Only process input if sender has control
        if (sender.equals(clientWithControl)) {
            // Process mouse/keyboard events
            if (message.getType() == Constants.MESSAGE_TYPE_MOUSE) {
                inputHandler.handleMouseEvent(message.getContent(), message.getData());
            } else if (message.getType() == Constants.MESSAGE_TYPE_KEYBOARD) {
                inputHandler.handleKeyboardEvent(message.getContent(), message.getData());
            }
            
            // Notify listeners (for logging/debugging purposes)
            for (ServerEventListener listener : listeners) {
                if (message.getType() == Constants.MESSAGE_TYPE_MOUSE) {
                    listener.onMouseEvent(message.getContent(), message.getData());
                } else if (message.getType() == Constants.MESSAGE_TYPE_KEYBOARD) {
                    listener.onKeyboardEvent(message.getContent(), message.getData());
                }
            }
        }
    }
    
    public boolean isNameTaken(String name) {
        return connectedClients.containsKey(name);
    }
    
    public void clientConnected(ClientHandler handler) {
        addClient(handler.getClientName(), handler);
    }
    
    public void clientDisconnected(ClientHandler handler) {
        removeClient(handler.getClientName());
    }
    
    public void broadcastChatMessage(String sender, String message) {
        Message chatMessage = new Message(
            Constants.MESSAGE_TYPE_CHAT,
            sender,
            message
        );
        broadcastMessage(chatMessage);
    }
    
    public void broadcastFile(String sender, String fileName, byte[] fileData) {
        Message fileMessage = new Message(
            Constants.MESSAGE_TYPE_FILE,
            sender,
            fileName,
            fileData
        );
        broadcastMessage(fileMessage);
    }
    
    public void handleMouseEvent(String eventType, byte[] data) {
        inputHandler.handleMouseEvent(eventType, data);
    }
    
    public void handleKeyboardEvent(String eventType, byte[] data) {
        inputHandler.handleKeyboardEvent(eventType, data);
    }
    
    public byte[] getScreenshot() {
        return screenCapturer.getScreenshot();
    }
    
    public String getPassword() {
        return password;
    }
    
    public void addListener(ServerEventListener listener) {
        listeners.add(listener);
    }
    
    public ConcurrentHashMap<String, ClientHandler> getConnectedClients() {
        return connectedClients;
    }
    
    public String getClientWithControl() {
        return clientWithControl;
    }
    
    // Server event listener interface
    public interface ServerEventListener {
        void onServerStarted();
        void onServerStopped();
        void onServerError(String message);
        void onClientConnected(String clientName);
        void onClientDisconnected(String clientName);
        void onControlGranted(String clientName);
        void onControlRevoked(String clientName);
        void onChatMessageReceived(String sender, String message);
        void onFileReceived(String sender, String fileName);
        void onMouseEvent(String eventType, byte[] data);
        void onKeyboardEvent(String eventType, byte[] data);
    }
}