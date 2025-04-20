package src.common.model;

import org.bson.Document;
import org.bson.types.ObjectId;
import java.util.Date;

public class UserPreferences {
    private ObjectId id;
    private ObjectId userId;
    private String theme;
    private Integer screenQuality;
    private Integer frameRate;
    private Boolean showChat;
    private Date lastUpdated;
    
    // Constructor
    public UserPreferences() {
        this.lastUpdated = new Date();
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
    
    public String getTheme() {
        return theme;
    }
    
    public void setTheme(String theme) {
        this.theme = theme;
    }
    
    public Integer getScreenQuality() {
        return screenQuality;
    }
    
    public void setScreenQuality(Integer screenQuality) {
        this.screenQuality = screenQuality;
    }
    
    public Integer getFrameRate() {
        return frameRate;
    }
    
    public void setFrameRate(Integer frameRate) {
        this.frameRate = frameRate;
    }
    
    public Boolean getShowChat() {
        return showChat;
    }
    
    public void setShowChat(Boolean showChat) {
        this.showChat = showChat;
    }
    
    public Date getLastUpdated() {
        return lastUpdated;
    }
    
    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
    
    // Convert to MongoDB Document
    public Document toDocument() {
        Document doc = new Document("userId", userId)
                .append("lastUpdated", new Date());
        
        if (theme != null) doc.append("theme", theme);
        if (screenQuality != null) doc.append("screenQuality", screenQuality);
        if (frameRate != null) doc.append("frameRate", frameRate);
        if (showChat != null) doc.append("showChat", showChat);
        
        return doc;
    }
    
    // Create from MongoDB Document
    public static UserPreferences fromDocument(Document doc) {
        UserPreferences prefs = new UserPreferences();
        prefs.id = doc.getObjectId("_id");
        prefs.userId = doc.getObjectId("userId");
        prefs.theme = doc.getString("theme");
        prefs.screenQuality = doc.getInteger("screenQuality");
        prefs.frameRate = doc.getInteger("frameRate");
        prefs.showChat = doc.getBoolean("showChat");
        prefs.lastUpdated = doc.getDate("lastUpdated");
        return prefs;
    }
}