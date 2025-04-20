package src.common.database;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import org.bson.Document;
import org.bson.types.ObjectId;
import src.common.model.UserPreferences;

import java.util.Date;

public class UserPreferencesDAO {
    private final MongoCollection<Document> collection;
    
    public UserPreferencesDAO() {
        this.collection = MongoDBConnection.getDatabase().getCollection("userPreferences");
    }
    
    public void saveUserPreferences(UserPreferences prefs) {
        // Update the last modified date
        prefs.setLastUpdated(new Date());
        
        Document doc = prefs.toDocument();
        
        // Use upsert to insert if not exists, update if exists
        collection.replaceOne(
            Filters.eq("userId", prefs.getUserId()),
            doc,
            new ReplaceOptions().upsert(true)
        );
    }
    
    public UserPreferences getUserPreferences(ObjectId userId) {
        Document doc = collection.find(Filters.eq("userId", userId)).first();
        return doc != null ? UserPreferences.fromDocument(doc) : null;
    }
    
    public void deleteUserPreferences(ObjectId userId) {
        collection.deleteOne(Filters.eq("userId", userId));
    }
}