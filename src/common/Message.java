package src.common;

import java.io.Serializable;
import java.util.Date;

public class Message implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private int type;
    private String sender;
    private String content;
    private Date timestamp;
    private byte[] data;
    
    public Message(int type, String sender, String content) {
        this.type = type;
        this.sender = sender;
        this.content = content;
        this.timestamp = new Date();
    }
    
    public Message(int type, String sender, String content, byte[] data) {
        this(type, sender, content);
        this.data = data;
    }
    
    // Getters and setters
    public int getType() {
        return type;
    }
    
    public String getSender() {
        return sender;
    }
    
    public String getContent() {
        return content;
    }
    
    public Date getTimestamp() {
        return timestamp;
    }
    
    public byte[] getData() {
        return data;
    }
}