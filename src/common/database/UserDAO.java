package src.common.database;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.InsertOneResult;
import org.bson.Document;
import org.bson.types.ObjectId;
import src.common.model.User;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

public class UserDAO {
    private final MongoCollection<Document> collection;
    
    public UserDAO() {
        this.collection = MongoDBConnection.getDatabase().getCollection("users");
    }
    
    public ObjectId createUser(User user) {
        // Hash password if not already hashed
        if (user.getPassword() != null && !user.getPassword().matches("^[a-f0-9]{64}$")) {
            user.setPassword(hashPassword(user.getPassword()));
        }
        
        // Set creation timestamp
        if (user.getCreatedAt() == null) {
            user.setCreatedAt(new Date());
        }
        
        Document doc = user.toDocument();
        InsertOneResult result = collection.insertOne(doc);
        return result.getInsertedId().asObjectId().getValue();
    }
    
    public User findByUsername(String username) {
        Document doc = collection.find(Filters.eq("username", username)).first();
        return doc != null ? User.fromDocument(doc) : null;
    }
    
    public User findById(ObjectId id) {
        Document doc = collection.find(Filters.eq("_id", id)).first();
        return doc != null ? User.fromDocument(doc) : null;
    }
    
    public void updateLastLogin(String username) {
        collection.updateOne(
            Filters.eq("username", username),
            Updates.set("lastLogin", new Date())
        );
    }
    
    public void updateUser(User user) {
        collection.updateOne(
            Filters.eq("_id", user.getId()),
            Updates.combine(
                Updates.set("username", user.getUsername()),
                Updates.set("password", user.getPassword()),
                Updates.set("isAdmin", user.isAdmin())
            )
        );
    }
    
    public boolean authenticate(String username, String password) {
        User user = findByUsername(username);
        if (user == null) return false;
        
        boolean authenticated = verifyPassword(password, user.getPassword());
        
        if (authenticated) {
            updateLastLogin(username);
        }
        
        return authenticated;
    }
    
    public String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public boolean verifyPassword(String inputPassword, String storedPassword) {
        String hashedInput = hashPassword(inputPassword);
        return hashedInput != null && hashedInput.equals(storedPassword);
    }
}