package src.common.database;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import org.bson.Document;
import org.bson.types.ObjectId;
import src.common.model.ChatMessage;

import java.util.ArrayList;
import java.util.List;

public class ChatMessageDAO {
    private final MongoCollection<Document> collection;
    
    public ChatMessageDAO() {
        this.collection = MongoDBConnection.getDatabase().getCollection("chatMessages");
    }
    
    public ObjectId saveMessage(ChatMessage message) {
        Document doc = message.toDocument();
        collection.insertOne(doc);
        return doc.getObjectId("_id");
    }
    
    public List<ChatMessage> getSessionMessages(ObjectId sessionId) {
        List<ChatMessage> messages = new ArrayList<>();
        FindIterable<Document> docs = collection
                .find(Filters.eq("sessionId", sessionId))
                .sort(Sorts.ascending("timestamp"));
                
        for (Document doc : docs) {
            messages.add(ChatMessage.fromDocument(doc));
        }
        
        return messages;
    }
    
    public List<ChatMessage> getRecentMessages(ObjectId sessionId, int limit) {
        List<ChatMessage> messages = new ArrayList<>();
        FindIterable<Document> docs = collection
                .find(Filters.eq("sessionId", sessionId))
                .sort(Sorts.descending("timestamp"))
                .limit(limit);
                
        for (Document doc : docs) {
            messages.add(ChatMessage.fromDocument(doc));
        }
        
        // Reverse to get chronological order
        java.util.Collections.reverse(messages);
        return messages;
    }
    
    // Get statistics about messages for a session
    public Document getMessageStats(ObjectId sessionId) {
        List<Document> pipeline = new ArrayList<>();
        
        // Match documents for this session
        pipeline.add(new Document("$match", new Document("sessionId", sessionId)));
        
        // Group by sender and count messages
        pipeline.add(new Document("$group", new Document("_id", "$senderName")
            .append("messageCount", new Document("$sum", 1))
            .append("firstMessage", new Document("$min", "$timestamp"))
            .append("lastMessage", new Document("$max", "$timestamp"))
        ));
        
        // Sort by message count descending
        pipeline.add(new Document("$sort", new Document("messageCount", -1)));
        
        List<Document> results = new ArrayList<>();
        collection.aggregate(pipeline).into(results);
        
        // Count total messages
        long totalMessages = collection.countDocuments(Filters.eq("sessionId", sessionId));
        
        // Create a summary document
        Document summary = new Document("totalMessages", totalMessages)
                            .append("participantStats", results);
        
        return summary;
    }
}