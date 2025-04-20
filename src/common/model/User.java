package src.common.model;

import org.bson.Document;
import org.bson.types.ObjectId;
import java.util.Date;

public class User {
    private ObjectId id;
    private String username;
    private String password; // Hashed
    private boolean isAdmin;
    private Date createdAt;
    private Date lastLogin;
    
    // Constructors
    public User() {
        this.createdAt = new Date();
    }
    
    // Getters and setters
    public ObjectId getId() {
        return id;
    }
    
    public void setId(ObjectId id) {
        this.id = id;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public boolean isAdmin() {
        return isAdmin;
    }
    
    public void setAdmin(boolean admin) {
        isAdmin = admin;
    }
    
    public Date getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
    
    public Date getLastLogin() {
        return lastLogin;
    }
    
    public void setLastLogin(Date lastLogin) {
        this.lastLogin = lastLogin;
    }
    
    // Convert to MongoDB Document
    public Document toDocument() {
        Document doc = new Document("username", username)
                .append("password", password)
                .append("isAdmin", isAdmin)
                .append("createdAt", createdAt);
                
        if (lastLogin != null) {
            doc.append("lastLogin", lastLogin);
        }
        
        return doc;
    }
    
    // Create from MongoDB Document
    public static User fromDocument(Document doc) {
        User user = new User();
        user.id = doc.getObjectId("_id");
        user.username = doc.getString("username");
        user.password = doc.getString("password");
        user.isAdmin = doc.getBoolean("isAdmin", false);
        user.createdAt = doc.getDate("createdAt");
        user.lastLogin = doc.getDate("lastLogin");
        return user;
    }
}