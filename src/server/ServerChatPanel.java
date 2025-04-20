package src.server;

import src.common.Constants;
import src.common.Message;
import src.common.database.ActivityLogDAO;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import src.common.database.ChatMessageDAO;
import src.common.model.ChatMessage;
import org.bson.types.ObjectId;
import java.util.List;
import java.util.Date;

public class ServerChatPanel extends JPanel {
    private Server server;
    
    private JTextArea chatArea;
    private JTextField messageField;
    private JButton sendButton;
    private JButton fileButton;
    private ChatMessageDAO chatMessageDAO;
    private ObjectId sessionId;

    private ObjectId currentSessionId;

public void setCurrentSessionId(ObjectId sessionId) {
    this.currentSessionId = sessionId;
}

public ObjectId getCurrentSessionId() {
    return currentSessionId;
}
    
    public ServerChatPanel(Server server) {
        this.server = server;
        this.chatMessageDAO = new ChatMessageDAO();
        
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(), "Chat", 
            TitledBorder.CENTER, TitledBorder.TOP));
        
        initComponents();
    }

    //method to set session ID and load history
    public void setSessionId(ObjectId sessionId) {
        this.sessionId = sessionId;
        loadChatHistory();
    }
    
    // Add method to load chat history
    private void loadChatHistory() {
        if (sessionId == null) return;
        
        try {
            // Get recent messages (limit to 50 to avoid UI overload)
            List<ChatMessage> messages = chatMessageDAO.getRecentMessages(sessionId, 50);
            
            // Clear current messages
            chatArea.setText("");
            
            // Add history messages with timestamp
            for (ChatMessage message : messages) {
                String formattedTimestamp = new java.text.SimpleDateFormat("HH:mm:ss").format(message.getTimestamp());
                chatArea.append("[" + formattedTimestamp + "] " + 
                                message.getSenderName() + ": " + 
                                message.getMessageText() + "\n");
            }
            
            // Add separator if there were messages
            if (!messages.isEmpty()) {
                chatArea.append("--- End of History ---\n");
            }
            
        } catch (Exception e) {
            System.err.println("Error loading chat history: " + e.getMessage());
            e.printStackTrace();
            chatArea.append("Error loading chat history.\n");
        }
    }

    private void initComponents() {
        // Chat area
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        JScrollPane chatScrollPane = new JScrollPane(chatArea);
        chatScrollPane.setPreferredSize(new Dimension(400, 200));
        add(chatScrollPane, BorderLayout.CENTER);
        
        // Message input
        JPanel messagePanel = new JPanel(new BorderLayout(5, 5));
        messageField = new JTextField();
        sendButton = new JButton("Send");
        fileButton = new JButton("Send File");
        
        sendButton.setEnabled(false);
        fileButton.setEnabled(false);
        
        sendButton.addActionListener(this::sendMessage);
        fileButton.addActionListener(this::sendFile);
        
        JPanel buttonBox = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonBox.add(fileButton);
        buttonBox.add(sendButton);
        
        messagePanel.add(messageField, BorderLayout.CENTER);
        messagePanel.add(buttonBox, BorderLayout.EAST);
        add(messagePanel, BorderLayout.SOUTH);
    }
    
     //sendMessage to store in database
     private void sendMessage(ActionEvent e) {
        String messageText = messageField.getText().trim();
        if (!messageText.isEmpty()) {
            Message chatMessage = new Message(
                Constants.MESSAGE_TYPE_CHAT,
                "Server (Host)",
                messageText
            );
            
            server.broadcastMessage(chatMessage);
            
            // Add to local chat area
            addMessage("Server (Host): " + messageText);
            
            // Store in database if session is active
            if (sessionId != null) {
                try {
                    ChatMessage dbMessage = new ChatMessage();
                    dbMessage.setSessionId(sessionId);
                    dbMessage.setSenderName("Server (Host)");
                    dbMessage.setMessageText(messageText);
                    
                    chatMessageDAO.saveMessage(dbMessage);
                } catch (Exception ex) {
                    System.err.println("Error storing message in database: " + ex.getMessage());
                }
            }
            
            // Clear message field
            messageField.setText("");
        }
    }
    
    // Update sendFile to log in database
    private void sendFile(ActionEvent e) {
        JFileChooser fileChooser = new JFileChooser();
        int option = fileChooser.showOpenDialog(this);
        
        if (option == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try {
                byte[] fileData = src.common.FileTransfer.fileToBytes(file);
                
                Message fileMessage = new Message(
                    Constants.MESSAGE_TYPE_FILE,
                    "Server (Host)",
                    file.getName(),
                    fileData
                );
                
                server.broadcastMessage(fileMessage);
                
                // Add to local chat area
                addMessage("Server (Host) sent file: " + file.getName());
                
                // Log file transfer in database
                if (sessionId != null) {
                    try {
                        ActivityLogDAO activityLogDAO = new ActivityLogDAO();
                        activityLogDAO.logActivity(sessionId, "file_send", 
                            "Sent file: " + file.getName() + " (" + fileData.length + " bytes)");
                    } catch (Exception ex) {
                        System.err.println("Error logging file transfer: " + ex.getMessage());
                    }
                }
                
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, 
                    "Error sending file: " + ex.getMessage(), 
                    "File Error", 
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    
    public void addMessage(String message) {
        chatArea.append(message + "\n");
        // Scroll to bottom
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }
    
    public void setEnabled(boolean enabled) {
        sendButton.setEnabled(enabled);
        fileButton.setEnabled(enabled);
        messageField.setEnabled(enabled);
    }
}