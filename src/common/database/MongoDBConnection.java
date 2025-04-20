package src.common.database;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.MongoClientSettings;
import com.mongodb.ConnectionString;

public class MongoDBConnection {
    private static final String CONNECTION_STRING = "mongodb://localhost:27017";
    private static final String DATABASE_NAME = "remoteDesktopDB";
    
    private static MongoClient mongoClient;
    private static MongoDatabase database;
    
    public static MongoDatabase getDatabase() {
        if (mongoClient == null) {
            try {
                MongoClientSettings settings = MongoClientSettings.builder()
                    .applyConnectionString(new ConnectionString(CONNECTION_STRING))
                    .applyToConnectionPoolSettings(builder -> 
                        builder.maxSize(20).minSize(5))
                    .build();
                    
                mongoClient = MongoClients.create(settings);
                database = mongoClient.getDatabase(DATABASE_NAME);
                
                // Create indexes for better performance
                createIndexes();
                
            } catch (Exception e) {
                System.err.println("Failed to connect to MongoDB: " + e.getMessage());
                throw e;
            }
        }
        return database;
    }
    
    private static void createIndexes() {
        // Create indexes for better query performance
        database.getCollection("users").createIndex(new org.bson.Document("username", 1).append("unique", true));
        database.getCollection("sessions").createIndex(new org.bson.Document("userId", 1));
        database.getCollection("sessions").createIndex(new org.bson.Document("endTime", 1));
        database.getCollection("chatMessages").createIndex(new org.bson.Document("sessionId", 1));
        database.getCollection("activityLogs").createIndex(new org.bson.Document("sessionId", 1));
        database.getCollection("activityLogs").createIndex(new org.bson.Document("actionType", 1));
    }
    
    public static void closeConnection() {
        if (mongoClient != null) {
            try {
                mongoClient.close();
            } catch (Exception e) {
                System.err.println("Error closing MongoDB connection: " + e.getMessage());
            } finally {
                mongoClient = null;
                database = null;
            }
        }
    }
}