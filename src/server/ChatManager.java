package src.server;

import src.common.Message;

import java.util.ArrayList;
import java.util.List;
import src.common.database.ChatMessageDAO;
import src.common.model.ChatMessage;
import org.bson.types.ObjectId;
import java.util.Date;

public class ChatManager {
    private List<Message> messages;
    private ChatMessageDAO chatMessageDAO;
    private ObjectId serverSessionId;
    
    public ChatManager() {
        messages = new ArrayList<>();
        chatMessageDAO = new ChatMessageDAO();
    }
    
    // Initialize with session ID
    public void setServerSessionId(ObjectId sessionId) {
        this.serverSessionId = sessionId;
    }
    
    // Update addMessage to store in MongoDB
    public void addMessage(Message message) {
        messages.add(message);
        
        // If server session is set, also store in MongoDB
        if (serverSessionId != null) {
            try {
                ChatMessage chatMessage = new ChatMessage();
                chatMessage.setSessionId(serverSessionId);
                chatMessage.setSenderName(message.getSender());
                chatMessage.setMessageText(message.getContent());
                chatMessage.setTimestamp(new Date());
                
                chatMessageDAO.saveMessage(chatMessage);
            } catch (Exception e) {
                System.err.println("Error storing chat message in database: " + e.getMessage());
            }
        }
    }
    
    // Add method to load chat history from MongoDB
    public List<ChatMessage> loadChatHistory(ObjectId sessionId) {
        try {
            return chatMessageDAO.getSessionMessages(sessionId);
        } catch (Exception e) {
            System.err.println("Error loading chat history from database: " + e.getMessage());
            return new ArrayList<>();
        }
    }
}