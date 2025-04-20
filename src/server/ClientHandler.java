package src.server;

import src.common.Constants;
import src.common.Message;
import src.common.database.UserDAO;
import src.common.database.SessionDAO;
import src.common.database.ActivityLogDAO;
import src.common.database.ChatMessageDAO;
import src.common.model.User;
import src.common.model.Session;
import src.common.model.ActivityLog;
import src.common.model.ChatMessage;
import org.bson.types.ObjectId;

import java.io.*;
import java.net.Socket;

public class ClientHandler {
    private Server server;
    private Socket socket;
    private String clientName;
    private DataInputStream in;
    private DataOutputStream out;
    private boolean isRunning;
    private boolean hasControl;
    private UserDAO userDAO;
    private SessionDAO sessionDAO;
    private ActivityLogDAO activityLogDAO;
    private ChatMessageDAO chatMessageDAO;
    private ObjectId sessionId;
    private ObjectId userId;
    
    public ClientHandler(Server server, Socket socket) {
        this.server = server;
        this.socket = socket;
        this.clientName = "Unknown";
        this.hasControl = false;
        
        // Initialize MongoDB DAOs
        this.userDAO = new UserDAO();
        this.sessionDAO = new SessionDAO();
        this.activityLogDAO = new ActivityLogDAO();
        this.chatMessageDAO = new ChatMessageDAO();
        
        try {
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            
            isRunning = true;
            
            // Start client handler thread
            new Thread(this::handleClient).start();
        } catch (IOException e) {
            System.err.println("Error setting up client handler: " + e.getMessage());
        }
    }
    
    
    private void handleClient() {
        try {
            // Wait for authentication
            int messageType = in.readInt();
            
            if (messageType == Constants.MESSAGE_TYPE_AUTHENTICATION) {
                handleAuthentication();
            } else {
                close("Invalid initial message type");
                return;
            }
            
            // Main communication loop
            while (isRunning) {
                messageType = in.readInt();
                
                switch (messageType) {
                    case Constants.MESSAGE_TYPE_CHAT:
                        handleChatMessage();
                        break;
                    case Constants.MESSAGE_TYPE_FILE:
                        handleFileTransfer();
                        break;
                    case Constants.MESSAGE_TYPE_MOUSE:
                        handleMouseEvent();
                        break;
                    case Constants.MESSAGE_TYPE_KEYBOARD:
                        handleKeyboardEvent();
                        break;
                    case Constants.MESSAGE_TYPE_DISCONNECT:
                        close("Client disconnected");
                        break;
                    default:
                        break;
                }
            }
        } catch (IOException e) {
            close("Connection error: " + e.getMessage());
        }
    }
    
    private void handleAuthentication() throws IOException {
        String password = in.readUTF();
        String name = in.readUTF();
        
        // Check password against server's password
        if (!password.equals(server.getPassword())) {
            sendAuthenticationResult(false, "Invalid password");
            close("Authentication failed - wrong password");
            return;
        }
        
        // Check if name is already in use
        if (server.isNameTaken(name)) {
            sendAuthenticationResult(false, "Name already in use");
            close("Authentication failed - name already in use");
            return;
        }
        
        // Authentication successful
        clientName = name;
        
        try {
            // Check if user exists in database, create if not
            User user = userDAO.findByUsername(clientName);
            
            if (user == null) {
                // Create new user
                User newUser = new User();
                newUser.setUsername(clientName);
                newUser.setPassword(userDAO.hashPassword(password)); // Store hashed password
                newUser.setAdmin(false);
                userId = userDAO.createUser(newUser);
            } else {
                userId = user.getId();
                // Update last login time
                userDAO.updateLastLogin(clientName);
            }
            
            // Create client session
            Session session = new Session();
            session.setUserId(userId);
            session.setUsername(clientName);
            session.setIpAddress(socket.getInetAddress().getHostAddress());
            session.setClientInfo(socket.getInetAddress().getHostName());
            sessionId = sessionDAO.startSession(session);
            
            // Log connection activity
            activityLogDAO.logActivity(sessionId, "connect", "Client connected from " + socket.getInetAddress().getHostAddress());
            
            // Send success authentication result
            sendAuthenticationResult(true, "");
            
            // Notify server
            server.clientConnected(this);
            
            // Start sending screen updates
            startScreenUpdates();
            
        } catch (Exception e) {
            System.err.println("Database error during authentication: " + e.getMessage());
            e.printStackTrace();
            sendAuthenticationResult(false, "Internal server error");
            close("Database error during authentication");
        }
    }
    
    private void sendAuthenticationResult(boolean success, String message) throws IOException {
        out.writeBoolean(success);
        out.writeUTF(message);
        out.flush();
    }
    
    //handleChatMessage to store in database
    private void handleChatMessage() throws IOException {
        String message = in.readUTF();
        
        // Store message in database
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setSessionId(sessionId);
        chatMessage.setSenderId(userId);
        chatMessage.setSenderName(clientName);
        chatMessage.setMessageText(message);
        chatMessageDAO.saveMessage(chatMessage);
        
        // Log activity
        activityLogDAO.logActivity(sessionId, "chat", "Message sent: " + message.substring(0, Math.min(50, message.length())));
        
        server.broadcastChatMessage(clientName, message);
    }
    
    //handleFileTransfer to log in database
    private void handleFileTransfer() throws IOException {
        String fileName = in.readUTF();
        int fileSize = in.readInt();
        
        byte[] fileData = new byte[fileSize];
        in.readFully(fileData);
        
        // Log file transfer in database
        activityLogDAO.logActivity(sessionId, "file_upload", "Uploaded file: " + fileName + " (" + fileSize + " bytes)");
        
        server.broadcastFile(clientName, fileName, fileData);
    }
    
    //handleMouseEvent to log in database
    private void handleMouseEvent() throws IOException {
        if (!hasControl) {
            return;
        }
        
        String eventType = in.readUTF();
        byte[] data = new byte[in.readInt()];
        in.readFully(data);
        
        // Log control activity periodically (not every event to avoid DB overload)
        if (Math.random() < 0.01) { // Log approximately 1% of events
            activityLogDAO.logActivity(sessionId, "mouse_control", eventType);
        }
        
        server.handleMouseEvent(eventType, data);
    }
    
    // handleKeyboardEvent to log in database
    private void handleKeyboardEvent() throws IOException {
        if (!hasControl) {
            return;
        }
        
        String eventType = in.readUTF();
        byte[] data = new byte[in.readInt()];
        in.readFully(data);
        
        // Log control activity periodically (not every event to avoid DB overload)
        if (Math.random() < 0.05) { // Log approximately 5% of events
            activityLogDAO.logActivity(sessionId, "keyboard_control", eventType);
        }
        
        server.handleKeyboardEvent(eventType, data);
    }
    
    private void startScreenUpdates() {
        new Thread(() -> {
            while (isRunning) {
                try {
                    byte[] screenData = server.getScreenshot();
                    
                    // Send screen data to client
                    out.writeInt(Constants.MESSAGE_TYPE_SCREEN);
                    out.writeInt(screenData.length);
                    out.write(screenData);
                    out.flush();
                    
                    // Control update rate
                    Thread.sleep(1000 / Constants.DEFAULT_FPS);
                } catch (IOException e) {
                    if (isRunning) {
                        close("Error sending screen updates: " + e.getMessage());
                    }
                    break;
                } catch (InterruptedException e) {
                    break;
                }
            }
        }).start();
    }
    
    public void sendChatMessage(String sender, String message) {
        try {
            out.writeInt(Constants.MESSAGE_TYPE_CHAT);
            out.writeUTF(sender);
            out.writeUTF(message);
            out.flush();
        } catch (IOException e) {
            close("Error sending chat message: " + e.getMessage());
        }
    }
    
    public void sendFile(String sender, String fileName, byte[] fileData) {
        try {
            out.writeInt(Constants.MESSAGE_TYPE_FILE);
            out.writeUTF(sender);
            out.writeUTF(fileName);
            out.writeInt(fileData.length);
            out.write(fileData);
            out.flush();
        } catch (IOException e) {
            close("Error sending file: " + e.getMessage());
        }
    }
    
    //grantControl to log in database
    public void grantControl() {
        try {
            hasControl = true;
            out.writeInt(Constants.MESSAGE_TYPE_CONTROL_GRANT);
            out.flush();
            
            // Log control grant in database
            activityLogDAO.logActivity(sessionId, "control_grant", "Control granted to client");
        } catch (IOException e) {
            close("Error granting control: " + e.getMessage());
        }
    }
    
    //revokeControl to log in database
    public void revokeControl() {
        try {
            hasControl = false;
            out.writeInt(Constants.MESSAGE_TYPE_CONTROL_REVOKE);
            out.flush();
            
            // Log control revocation in database
            activityLogDAO.logActivity(sessionId, "control_revoke", "Control revoked from client");
        } catch (IOException e) {
            close("Error revoking control: " + e.getMessage());
        }
    }
    
    public void disconnect() {
        close("Disconnected by server");
    }
    
    public void sendMessage(Message message) {
        try {
            switch (message.getType()) {
                case Constants.MESSAGE_TYPE_CHAT:
                    sendChatMessage(message.getSender(), message.getContent());
                    break;
                case Constants.MESSAGE_TYPE_FILE:
                    sendFile(message.getSender(), message.getContent(), message.getData());
                    break;
                case Constants.MESSAGE_TYPE_CONTROL_GRANT:
                    grantControl();
                    break;
                case Constants.MESSAGE_TYPE_CONTROL_REVOKE:
                    revokeControl();
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            close("Error sending message: " + e.getMessage());
        }
    }
    
    //close method to end session in database
    public void close(String reason) {
        if (!isRunning) return;
        
        isRunning = false;
        
        // End session in database
        if (sessionId != null) {
            try {
                sessionDAO.endSession(sessionId);
                activityLogDAO.logActivity(sessionId, "disconnect", "Disconnected: " + reason);
            } catch (Exception e) {
                System.err.println("Error ending session in database: " + e.getMessage());
            }
        }
        
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            // Silent close
        }
        
        server.clientDisconnected(this);
    }
    
    // Getter for sessionId
    public ObjectId getSessionId() {
        return sessionId;
    }

    
    public String getClientName() {
        return clientName;
    }
    
    public boolean hasControl() {
        return hasControl;
    }

}