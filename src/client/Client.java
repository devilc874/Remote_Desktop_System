package src.client;

import src.common.Constants;
import src.common.Message;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import src.common.database.UserDAO;
import src.common.database.SessionDAO;
import src.common.database.UserPreferencesDAO;
import src.common.model.User;
import src.common.model.Session;
import src.common.model.UserPreferences;
import org.bson.types.ObjectId;

public class Client {
    private Socket socket;
    private String serverIP;
    private int port;
    private String username;
    private String password;
    private DataInputStream in;
    private DataOutputStream out;
    private boolean isConnected;
    private List<ClientEventListener> listeners;
    private UserDAO userDAO;
    private SessionDAO sessionDAO;
    private UserPreferencesDAO preferencesDAO;
    private ObjectId userId;
    private ObjectId sessionId;
    
    public Client() {
        this.listeners = new ArrayList<>();
        this.isConnected = false;
        
        // Initialize MongoDB DAOs
        this.userDAO = new UserDAO();
        this.sessionDAO = new SessionDAO();
        this.preferencesDAO = new UserPreferencesDAO();
    }
    
    // Update connect method to work with database
    public void connect(String serverIP, int port, String username, String password) {
        this.serverIP = serverIP;
        this.port = port;
        this.username = username;
        this.password = password;
        
        new Thread(() -> {
            try {
                System.out.println("Connecting to server at " + serverIP + ":" + port);
                socket = new Socket(serverIP, port);
                System.out.println("Socket connected to server");
                
                in = new DataInputStream(socket.getInputStream());
                out = new DataOutputStream(socket.getOutputStream());
                
                System.out.println("Sending authentication message");
                System.out.println("Authentication message type: " + Constants.MESSAGE_TYPE_AUTHENTICATION);
                
                // Send authentication
                out.writeInt(Constants.MESSAGE_TYPE_AUTHENTICATION);
                out.flush();
                System.out.println("Authentication message type sent");
                
                out.writeUTF(password);
                out.flush();
                System.out.println("Password sent: " + password);
                
                out.writeUTF(username);
                out.flush();
                System.out.println("Username sent: " + username);
                
                // Wait for authentication result
                boolean success = in.readBoolean();
                String message = in.readUTF();
                
                System.out.println("Authentication result: " + success + ", Message: " + message);
                
                if (success) {
                    isConnected = true;
                    
                    // Update or create user and session in MongoDB
                    try {
                        // Check if user exists
                        User user = userDAO.findByUsername(username);
                        
                        if (user == null) {
                            // Create new user
                            User newUser = new User();
                            newUser.setUsername(username);
                            newUser.setPassword(userDAO.hashPassword(password));
                            newUser.setAdmin(false);
                            userId = userDAO.createUser(newUser);
                        } else {
                            userId = user.getId();
                            // Update last login
                            userDAO.updateLastLogin(username);
                        }
                        
                        // Create session
                        Session session = new Session();
                        session.setUserId(userId);
                        session.setUsername(username);
                        session.setIpAddress(socket.getLocalAddress().getHostAddress());
                        session.setClientInfo("Client connecting to " + serverIP + ":" + port);
                        sessionId = sessionDAO.startSession(session);
                        
                        // Load user preferences
                        loadUserPreferences();
                        
                    } catch (Exception e) {
                        System.err.println("Database error: " + e.getMessage());
                        e.printStackTrace();
                        // Continue even if database operations fail
                    }
                    
                    // Notify listeners
                    for (ClientEventListener listener : listeners) {
                        listener.onConnected();
                    }
                    
                    // Start receiving messages from server
                    startMessageReceiver();
                } else {
                    // Notify listeners of authentication failure
                    for (ClientEventListener listener : listeners) {
                        listener.onConnectionFailed("Authentication failed: " + message);
                    }
                    
                    // Close connection
                    disconnect();
                }
            } catch (IOException e) {
                System.err.println("Connection error: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getName()));
                e.printStackTrace();
                
                // Notify listeners
                for (ClientEventListener listener : listeners) {
                    listener.onConnectionFailed(e.getMessage());
                }
                
                // Close connection
                disconnect();
            }
        }).start();
    }

    //method to load user preferences
    private void loadUserPreferences() {
        if (userId == null) return;
        
        try {
            UserPreferences prefs = preferencesDAO.getUserPreferences(userId);
            
            if (prefs != null) {
                // Notify listeners about loaded preferences
                for (ClientEventListener listener : listeners) {
                    if (listener instanceof ClientPreferencesListener) {
                        ((ClientPreferencesListener) listener).onPreferencesLoaded(prefs);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading preferences: " + e.getMessage());
        }
    }
    
    //method to save user preferences
    public void saveUserPreferences(UserPreferences prefs) {
        if (userId == null) return;
        
        try {
            prefs.setUserId(userId);
            preferencesDAO.saveUserPreferences(prefs);
            System.out.println("User preferences saved to database");
        } catch (Exception e) {
            System.err.println("Error saving preferences: " + e.getMessage());
        }
    }
    
    private void startMessageReceiver() {
        new Thread(() -> {
            try {
                System.out.println("Starting message receiver");
                while (isConnected) {
                    int messageType = in.readInt();
                    System.out.println("Received message type: " + messageType);
                    
                    switch (messageType) {
                        case Constants.MESSAGE_TYPE_CHAT:
                            handleChatMessage();
                            break;
                        case Constants.MESSAGE_TYPE_FILE:
                            handleFileTransfer();
                            break;
                        case Constants.MESSAGE_TYPE_SCREEN:
                            handleScreenUpdate();
                            break;
                        case Constants.MESSAGE_TYPE_CONTROL_GRANT:
                            handleControlGrant();
                            break;
                        case Constants.MESSAGE_TYPE_CONTROL_REVOKE:
                            handleControlRevoke();
                            break;
                        default:
                            System.out.println("Unknown message type: " + messageType);
                            break;
                    }
                }
            } catch (IOException e) {
                System.err.println("Error receiving message: " + e.getMessage());
                if (isConnected) {
                    disconnect();
                    
                    // Notify listeners
                    for (ClientEventListener listener : listeners) {
                        listener.onDisconnected("Connection lost: " + e.getMessage());
                    }
                }
            }
            System.out.println("Message receiver stopped");
        }).start();
    }
    
    private void handleChatMessage() throws IOException {
        String sender = in.readUTF();
        String message = in.readUTF();
        
        System.out.println("Chat message received from " + sender + ": " + message);
        
        // Notify listeners
        for (ClientEventListener listener : listeners) {
            listener.onChatMessageReceived(sender, message);
        }
    }
    
    private void handleFileTransfer() throws IOException {
        String sender = in.readUTF();
        String fileName = in.readUTF();
        int fileSize = in.readInt();
        
        System.out.println("File transfer received from " + sender + ": " + fileName + " (" + fileSize + " bytes)");
        
        byte[] fileData = new byte[fileSize];
        in.readFully(fileData);
        
        // Notify listeners
        for (ClientEventListener listener : listeners) {
            listener.onFileReceived(sender, fileName, fileData);
        }
    }
    
    private void handleScreenUpdate() throws IOException {
        int dataSize = in.readInt();
        byte[] screenData = new byte[dataSize];
        in.readFully(screenData);
        
        System.out.println("Screen update received: " + dataSize + " bytes");
        
        // Notify listeners
        for (ClientEventListener listener : listeners) {
            listener.onScreenUpdate(screenData);
        }
    }
    
    private void handleControlGrant() {
        System.out.println("Control granted");
        
        // Notify listeners
        for (ClientEventListener listener : listeners) {
            listener.onControlGranted();
        }
    }
    
    private void handleControlRevoke() {
        System.out.println("Control revoked");
        
        // Notify listeners
        for (ClientEventListener listener : listeners) {
            listener.onControlRevoked();
        }
    }
    
    public void sendChatMessage(String message) {
        if (!isConnected) {
            System.out.println("Cannot send chat message - not connected");
            return;
        }
        
        try {
            System.out.println("Sending chat message: " + message);
            out.writeInt(Constants.MESSAGE_TYPE_CHAT);
            out.writeUTF(message);
            out.flush();
        } catch (IOException e) {
            System.err.println("Error sending chat message: " + e.getMessage());
            e.printStackTrace();
        }
    }
    public void sendFile(String fileName, byte[] fileData) {
        if (!isConnected) {
            System.out.println("Cannot send file - not connected");
            return;
        }
        
        try {
            System.out.println("Sending file: " + fileName + " (" + fileData.length + " bytes)");
            out.writeInt(Constants.MESSAGE_TYPE_FILE);
            out.writeUTF(fileName);
            out.writeInt(fileData.length);
            out.write(fileData);
            out.flush();
        } catch (IOException e) {
            System.err.println("Error sending file: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void sendMouseEvent(String eventType, byte[] data) {
        if (!isConnected) {
            System.out.println("Cannot send mouse event - not connected");
            return;
        }
        
        try {
            System.out.println("Sending mouse event: " + eventType);
            out.writeInt(Constants.MESSAGE_TYPE_MOUSE);
            out.writeUTF(eventType);
            out.writeInt(data.length);
            out.write(data);
            out.flush();
        } catch (IOException e) {
            System.err.println("Error sending mouse event: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void sendKeyboardEvent(String eventType, byte[] data) {
        if (!isConnected) {
            System.out.println("Cannot send keyboard event - not connected");
            return;
        }
        
        try {
            System.out.println("Sending keyboard event: " + eventType);
            out.writeInt(Constants.MESSAGE_TYPE_KEYBOARD);
            out.writeUTF(eventType);
            out.writeInt(data.length);
            out.write(data);
            out.flush();
        } catch (IOException e) {
            System.err.println("Error sending keyboard event: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // Update disconnect method to end session
    public void disconnect() {
        if (!isConnected) {
            System.out.println("Already disconnected");
            return;
        }
        
        try {
            System.out.println("Disconnecting from server");
            isConnected = false;
            
            // End session in database
            if (sessionId != null) {
                try {
                    sessionDAO.endSession(sessionId);
                    System.out.println("Session ended in database");
                } catch (Exception e) {
                    System.err.println("Error ending session in database: " + e.getMessage());
                }
            }
            
            // Let the server know we're disconnecting
            if (socket != null && !socket.isClosed() && socket.isConnected()) {
                try {
                    out.writeInt(Constants.MESSAGE_TYPE_DISCONNECT);
                    out.flush();
                    System.out.println("Sent disconnect message to server");
                } catch (IOException e) {
                    System.err.println("Error sending disconnect message: " + e.getMessage());
                }
            }
            
            // Close resources
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    System.err.println("Error closing input stream: " + e.getMessage());
                }
            }
            
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    System.err.println("Error closing output stream: " + e.getMessage());
                }
            }
            
            if (socket != null && !socket.isClosed()) {
                try {
                    socket.close();
                    System.out.println("Socket closed");
                } catch (IOException e) {
                    System.err.println("Error closing socket: " + e.getMessage());
                }
            }
            
            // Notify listeners
            for (ClientEventListener listener : listeners) {
                listener.onDisconnected("Disconnected");
            }
        } catch (Exception e) {
            System.err.println("Error during disconnect: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // Add preferences listener interface
    public interface ClientPreferencesListener {
        void onPreferencesLoaded(UserPreferences preferences);
    }

    
    public void addListener(ClientEventListener listener) {
        listeners.add(listener);
    }
    
    public boolean isConnected() {
        return isConnected;
    }
    
    public String getUsername() {
        return username;
    }
    
    // Client event listener interface
    public interface ClientEventListener {
        void onConnected();
        void onConnectionFailed(String reason);
        void onDisconnected(String reason);
        void onChatMessageReceived(String sender, String message);
        void onFileReceived(String sender, String fileName, byte[] fileData);
        void onScreenUpdate(byte[] screenData);
        void onControlGranted();
        void onControlRevoked();
    }
}