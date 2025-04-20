package src.common.database;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import org.bson.Document;
import org.bson.types.ObjectId;
import src.common.model.ActivityLog;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ActivityLogDAO {
    private final MongoCollection<Document> collection;
    
    public ActivityLogDAO() {
        this.collection = MongoDBConnection.getDatabase().getCollection("activityLogs");
    }
    
    public ObjectId logActivity(ObjectId sessionId, String actionType, String actionDetails) {
        ActivityLog log = new ActivityLog();
        log.setSessionId(sessionId);
        log.setActionType(actionType);
        log.setActionDetails(actionDetails);
        log.setTimestamp(new Date());
        
        Document doc = log.toDocument();
        collection.insertOne(doc);
        return doc.getObjectId("_id");
    }
    
    public List<ActivityLog> getSessionActivities(ObjectId sessionId) {
        List<ActivityLog> logs = new ArrayList<>();
        FindIterable<Document> docs = collection
                .find(Filters.eq("sessionId", sessionId))
                .sort(Sorts.ascending("timestamp"));
                
        for (Document doc : docs) {
            logs.add(ActivityLog.fromDocument(doc));
        }
        
        return logs;
    }
    
    public List<ActivityLog> findByActionType(String actionType, int limit) {
        List<ActivityLog> logs = new ArrayList<>();
        FindIterable<Document> docs = collection
                .find(Filters.eq("actionType", actionType))
                .sort(Sorts.descending("timestamp"))
                .limit(limit);
                
        for (Document doc : docs) {
            logs.add(ActivityLog.fromDocument(doc));
        }
        
        return logs;
    }
    
    // Get activity statistics for a session
    public Document getActivityStats(ObjectId sessionId) {
        List<Document> pipeline = new ArrayList<>();
        
        // Match documents for this session
        pipeline.add(new Document("$match", new Document("sessionId", sessionId)));
        
        // Group by action type and count activities
        pipeline.add(new Document("$group", new Document("_id", "$actionType")
            .append("count", new Document("$sum", 1))
            .append("firstOccurrence", new Document("$min", "$timestamp"))
            .append("lastOccurrence", new Document("$max", "$timestamp"))
        ));
        
        // Sort by count descending
        pipeline.add(new Document("$sort", new Document("count", -1)));
        
        List<Document> results = new ArrayList<>();
        collection.aggregate(pipeline).into(results);
        
        // Count total activities
        long totalActivities = collection.countDocuments(Filters.eq("sessionId", sessionId));
        
        // Create a summary document
        Document summary = new Document("totalActivities", totalActivities)
                            .append("activityTypeStats", results);
        
        return summary;
    }
}