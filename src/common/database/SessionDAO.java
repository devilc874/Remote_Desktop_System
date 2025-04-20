package src.common.database;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.mongodb.client.model.Sorts;
import org.bson.Document;
import org.bson.types.ObjectId;
import src.common.model.Session;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class SessionDAO {
    private final MongoCollection<Document> collection;
    
    public SessionDAO() {
        this.collection = MongoDBConnection.getDatabase().getCollection("sessions");
    }
    
    public ObjectId startSession(Session session) {
        // Ensure start time is set
        if (session.getStartTime() == null) {
            session.setStartTime(new Date());
        }
        
        Document doc = session.toDocument();
        collection.insertOne(doc);
        return doc.getObjectId("_id");
    }
    
    public void endSession(ObjectId sessionId) {
        collection.updateOne(
            Filters.eq("_id", sessionId),
            Updates.set("endTime", new Date())
        );
    }
    
    public Session findById(ObjectId id) {
        Document doc = collection.find(Filters.eq("_id", id)).first();
        return doc != null ? Session.fromDocument(doc) : null;
    }
    
    public List<Session> findByUserId(ObjectId userId, int limit) {
        List<Session> sessions = new ArrayList<>();
        FindIterable<Document> docs = collection
                .find(Filters.eq("userId", userId))
                .sort(Sorts.descending("startTime"))
                .limit(limit);
                
        for (Document doc : docs) {
            sessions.add(Session.fromDocument(doc));
        }
        
        return sessions;
    }
    
    public List<Session> getActiveSessions() {
        List<Session> sessions = new ArrayList<>();
        FindIterable<Document> docs = collection
                .find(Filters.eq("endTime", null))
                .sort(Sorts.descending("startTime"));
                
        for (Document doc : docs) {
            sessions.add(Session.fromDocument(doc));
        }
        
        return sessions;
    }
    
    // Get session statistics for a specific user
    public Document getUserSessionStats(ObjectId userId) {
        List<Document> pipeline = new ArrayList<>();
        
        // Match documents for this user
        pipeline.add(new Document("$match", new Document("userId", userId)));
        
        // Calculate statistics
        pipeline.add(new Document("$group", new Document("_id", "$userId")
            .append("totalSessions", new Document("$sum", 1))
            .append("totalDuration", new Document("$sum", new Document("$subtract", 
                new Object[] {
                    new Document("$ifNull", new Object[] {"$endTime", new Date()}),
                    "$startTime"
                }
            )))
            .append("firstSession", new Document("$min", "$startTime"))
            .append("lastSession", new Document("$max", "$startTime"))
        ));
        
        // Convert duration from milliseconds to minutes
        pipeline.add(new Document("$project", new Document("totalSessions", 1)
            .append("totalDurationMinutes", new Document("$divide", new Object[] {"$totalDuration", 60000}))
            .append("firstSession", 1)
            .append("lastSession", 1)
        ));
        
        Document result = collection.aggregate(pipeline).first();
        return result != null ? result : new Document("totalSessions", 0);
    }
}