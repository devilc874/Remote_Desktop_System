package src.common.model;

import org.bson.Document;
import org.bson.types.ObjectId;
import java.util.Date;

public class ActivityLog {
    private ObjectId id;
    private ObjectId sessionId;
    private String actionType; // e.g., "login", "input", "screen_capture"
    private String actionDetails;
    private Date timestamp;
    
    // Constructor
    public ActivityLog() {
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
    
    public String getActionType() {
        return actionType;
    }
    
    public void setActionType(String actionType) {
        this.actionType = actionType;
    }
    
    public String getActionDetails() {
        return actionDetails;
    }
    
    public void setActionDetails(String actionDetails) {
        this.actionDetails = actionDetails;
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
                .append("actionType", actionType)
                .append("actionDetails", actionDetails)
                .append("timestamp", timestamp);
        
        return doc;
    }
    
    // Create from MongoDB Document
    public static ActivityLog fromDocument(Document doc) {
        ActivityLog log = new ActivityLog();
        log.id = doc.getObjectId("_id");
        log.sessionId = doc.getObjectId("sessionId");
        log.actionType = doc.getString("actionType");
        log.actionDetails = doc.getString("actionDetails");
        log.timestamp = doc.getDate("timestamp");
        return log;
    }
}