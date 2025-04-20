package src.common.model;

import org.bson.Document;
import org.bson.types.ObjectId;
import java.util.Date;

public class Session {
    private ObjectId id;
    private ObjectId userId;
    private String username; // For convenience
    private Date startTime;
    private Date endTime;
    private String ipAddress;
    private String clientInfo; // OS, version, etc.
    
    // Constructors
    public Session() {
        this.startTime = new Date();
    }
    
    // Getters and setters
    public ObjectId getId() {
        return id;
    }
    
    public void setId(ObjectId id) {
        this.id = id;
    }
    
    public ObjectId getUserId() {
        return userId;
    }
    
    public void setUserId(ObjectId userId) {
        this.userId = userId;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public Date getStartTime() {
        return startTime;
    }
    
    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }
    
    public Date getEndTime() {
        return endTime;
    }
    
    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }
    
    public String getIpAddress() {
        return ipAddress;
    }
    
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
    
    public String getClientInfo() {
        return clientInfo;
    }
    
    public void setClientInfo(String clientInfo) {
        this.clientInfo = clientInfo;
    }
    
    // Convert to MongoDB Document
    public Document toDocument() {
        Document doc = new Document("userId", userId)
                .append("username", username)
                .append("startTime", startTime)
                .append("ipAddress", ipAddress);
                
        if (clientInfo != null) {
            doc.append("clientInfo", clientInfo);
        }
        
        if (endTime != null) {
            doc.append("endTime", endTime);
        }
        
        return doc;
    }
    
    // Create from MongoDB Document
    public static Session fromDocument(Document doc) {
        Session session = new Session();
        session.id = doc.getObjectId("_id");
        session.userId = doc.getObjectId("userId");
        session.username = doc.getString("username");
        session.startTime = doc.getDate("startTime");
        session.endTime = doc.getDate("endTime");
        session.ipAddress = doc.getString("ipAddress");
        session.clientInfo = doc.getString("clientInfo");
        return session;
    }
}