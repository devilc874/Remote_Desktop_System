package src.client;

import src.common.Constants;
import src.common.FileTransfer;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class ChatPanel extends JPanel {
    private Client client;
    
    private JTextArea chatArea;
    private JTextField messageField;
    private JButton sendButton;
    private JButton fileButton;
    
    public ChatPanel(Client client) {
        this.client = client;
        
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(), "Chat", 
            TitledBorder.CENTER, TitledBorder.TOP));
        
        initComponents();
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
    
    private void sendMessage(ActionEvent e) {
        String messageText = messageField.getText().trim();
        if (!messageText.isEmpty()) {
            client.sendChatMessage(messageText);
            
            // Add to local chat area
            addMessage(client.getUsername() + ": " + messageText);
            
            // Clear message field
            messageField.setText("");
        }
    }
    
    private void sendFile(ActionEvent e) {
        JFileChooser fileChooser = new JFileChooser();
        int option = fileChooser.showOpenDialog(this);
        
        if (option == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try {
                // Read file data as byte array
                byte[] fileData = Files.readAllBytes(file.toPath());
                client.sendFile(file.getName(), fileData);
                
                // Add to local chat area
                addMessage(client.getUsername() + " sent file: " + file.getName());
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