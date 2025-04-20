package src.common.model;

import org.bson.Document;
import org.bson.types.ObjectId;
import java.util.Date;

public class ChatMessage {
    private ObjectId id;
    private ObjectId sessionId;
    private ObjectId senderId; // Optional, if tracking per user
    private String senderName;
    private String messageText;
    private Date timestamp;
    
    // Constructors
    public ChatMessage() {
        this.timestamp = new Date();
    }
    
    // Getters and setters
    public ObjectId getId() {
        return id;
    }
    
    public void setId(ObjectId id) {
        this.id = id;
    }
    
    public ObjectId getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(ObjectId sessionId) {
        this.sessionId = sessionId;
    }
    
    public ObjectId getSenderId() {
        return senderId;
    }
    
    public void setSenderId(ObjectId senderId) {
        this.senderId = senderId;
    }
    
    public String getSenderName() {
        return senderName;
    }
    
    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }
    
    public String getMessageText() {
        return messageText;
    }
    
    public void setMessageText(String messageText) {
        this.messageText = messageText;
    }
    
    public Date getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
    
    // Convert to MongoDB Document
    public Document toDocument() {
        Document doc = new Document("sessionId", sessionId)
                .append("senderName", senderName)
                .append("messageText", messageText)
                .append("timestamp", timestamp);
                
        if (senderId != null) {
            doc.append("senderId", senderId);
        }
        
        return doc;
    }
    
    // Create from MongoDB Document
    public static ChatMessage fromDocument(Document doc) {
        ChatMessage message = new ChatMessage();
        message.id = doc.getObjectId("_id");
        message.sessionId = doc.getObjectId("sessionId");
        message.senderId = doc.getObjectId("senderId");
        message.senderName = doc.getString("senderName");
        message.messageText = doc.getString("messageText");
        message.timestamp = doc.getDate("timestamp");
        return message;
    }
}