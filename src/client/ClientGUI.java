package src.client;

import src.common.Constants;
import src.common.FileTransfer;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;

public class ClientGUI extends JPanel implements Client.ClientEventListener {
    private Client client;
    
    // Connection controls
    private JTextField serverIPField;
    private JTextField portField;
    private JPasswordField passwordField;
    private JTextField nameField;
    private JButton connectButton;
    private JButton disconnectButton;
    
    // Chat panel
    private ChatPanel chatPanel;
    
    // Screen viewer window
    private JFrame screenViewerFrame;
    private ScreenViewer screenViewer;
    
    // Input handler for mouse/keyboard control
    private InputHandler inputHandler;
    
    public ClientGUI() {
        client = new Client();
        client.addListener(this);
        
        setLayout(new BorderLayout(10, 10));
        initComponents();
    }
    
    private void initComponents() {
        // Connection panel
        JPanel connectionPanel = new JPanel();
        connectionPanel.setLayout(new GridBagLayout());
        connectionPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(), "Connection", 
            TitledBorder.CENTER, TitledBorder.TOP));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        // Server IP
        gbc.gridx = 0;
        gbc.gridy = 0;
        connectionPanel.add(new JLabel("Server IP:"), gbc);
        
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        serverIPField = new JTextField();
        connectionPanel.add(serverIPField, gbc);
        
        // Port
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.0;
        connectionPanel.add(new JLabel("Port:"), gbc);
        
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        portField = new JTextField(String.valueOf(Constants.DEFAULT_PORT));
        connectionPanel.add(portField, gbc);
        
        // Password
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0.0;
        connectionPanel.add(new JLabel("Password:"), gbc);
        
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.weightx = 1.0;
        passwordField = new JPasswordField();
        connectionPanel.add(passwordField, gbc);
        
        // Name
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weightx = 0.0;
        connectionPanel.add(new JLabel("Your Name:"), gbc);
        
        gbc.gridx = 1;
        gbc.gridy = 3;
        gbc.weightx = 1.0;
        nameField = new JTextField();
        connectionPanel.add(nameField, gbc);
        
        // Connect/Disconnect buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        connectButton = new JButton("Connect");
        disconnectButton = new JButton("Disconnect");
        disconnectButton.setEnabled(false);
        
        connectButton.addActionListener(this::connect);
        disconnectButton.addActionListener(this::disconnect);
        
        buttonPanel.add(connectButton);
        buttonPanel.add(disconnectButton);
        
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        connectionPanel.add(buttonPanel, gbc);
        
        // Chat panel
        chatPanel = new ChatPanel(client);
        chatPanel.setEnabled(false);
        
        // Add panels to main layout
        add(connectionPanel, BorderLayout.NORTH);
        add(chatPanel, BorderLayout.CENTER);
        
        // Set border padding for main panel
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    }
    
    private void connect(ActionEvent e) {
        String serverIP = serverIPField.getText().trim();
        String portText = portField.getText().trim();
        String password = new String(passwordField.getPassword());
        String name = nameField.getText().trim();
        
        if (serverIP.isEmpty() || portText.isEmpty() || password.isEmpty() || name.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "Please fill in all fields", 
                "Input Error", 
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        int port;
        try {
            port = Integer.parseInt(portText);
            if (port < 1024 || port > 65535) {
                throw new NumberFormatException("Port out of range");
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, 
                "Port must be a number between 1024 and 65535", 
                "Input Error", 
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Disable connect button and fields
        connectButton.setEnabled(false);
        serverIPField.setEnabled(false);
        portField.setEnabled(false);
        passwordField.setEnabled(false);
        nameField.setEnabled(false);
        
        // Show connecting message
        chatPanel.addMessage("Connecting to server...");
        
        // Connect to server
        client.connect(serverIP, port, name, password);
    }
    
    private void disconnect(ActionEvent e) {
        client.disconnect();
    }
    
    private void showScreenViewer() {
        screenViewerFrame = new JFrame("Remote Screen - " + serverIPField.getText());
        screenViewerFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        
        // Create screen viewer
        screenViewer = new ScreenViewer();
        
        // Create display options panel
        JPanel optionsPanel = createDisplayOptionsPanel();
        
        // Add components to frame
        screenViewerFrame.getContentPane().setLayout(new BorderLayout());
        screenViewerFrame.getContentPane().add(screenViewer, BorderLayout.CENTER);
        screenViewerFrame.getContentPane().add(optionsPanel, BorderLayout.SOUTH);
        
        // Create input handler
        inputHandler = new InputHandler(client, screenViewer);
        
        // Set size and position
        screenViewerFrame.setSize(1024, 768);
        screenViewerFrame.setLocationRelativeTo(null);
        
        // Handle window closing
        screenViewerFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                client.disconnect();
            }
        });
        
        screenViewerFrame.setVisible(true);
    }
    
    private JPanel createDisplayOptionsPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 5));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, Color.GRAY),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        
        // Display mode toggle
        JToggleButton aspectRatioToggle = new JToggleButton("Maintain Aspect Ratio", true);
        aspectRatioToggle.setToolTipText("Toggle between maintaining aspect ratio or stretching to fit window");
        aspectRatioToggle.addActionListener(e -> {
            boolean maintainRatio = aspectRatioToggle.isSelected();
            screenViewer.setMaintainAspectRatio(maintainRatio);
            
            if (maintainRatio) {
                aspectRatioToggle.setText("Maintain Aspect Ratio");
            } else {
                aspectRatioToggle.setText("Stretch to Fit");
            }
        });
        
        // Zoom slider
        JLabel zoomLabel = new JLabel("Zoom: 100%");
        JSlider zoomSlider = new JSlider(JSlider.HORIZONTAL, 50, 200, 100);
        zoomSlider.setMajorTickSpacing(50);
        zoomSlider.setMinorTickSpacing(10);
        zoomSlider.setPaintTicks(true);
        zoomSlider.setPreferredSize(new Dimension(150, zoomSlider.getPreferredSize().height));
        zoomSlider.setToolTipText("Adjust zoom level");
        
        DecimalFormat df = new DecimalFormat("#%");
        zoomSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                int value = zoomSlider.getValue();
                double zoomFactor = value / 100.0;
                zoomLabel.setText("Zoom: " + value + "%");
                
                if (!zoomSlider.getValueIsAdjusting()) {
                    screenViewer.setZoomFactor(zoomFactor);
                }
            }
        });
        
        // Reset zoom button
        JButton resetZoomButton = new JButton("Reset Zoom");
        resetZoomButton.addActionListener(e -> {
            zoomSlider.setValue(100);
            screenViewer.setZoomFactor(1.0);
        });
        
        // Fullscreen button
        JToggleButton fullscreenToggle = new JToggleButton("Fullscreen");
        fullscreenToggle.addActionListener(e -> {
            boolean isFullscreen = fullscreenToggle.isSelected();
            GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
            
            if (isFullscreen) {
                // Save current bounds to restore later
                screenViewerFrame.dispose();
                screenViewerFrame.setUndecorated(true);
                gd.setFullScreenWindow(screenViewerFrame);
            } else {
                gd.setFullScreenWindow(null);
                screenViewerFrame.dispose();
                screenViewerFrame.setUndecorated(false);
                screenViewerFrame.setSize(1024, 768);
                screenViewerFrame.setLocationRelativeTo(null);
                screenViewerFrame.setVisible(true);
            }
        });
        
        // Add components to panel
        panel.add(aspectRatioToggle);
        panel.add(new JSeparator(JSeparator.VERTICAL) {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(1, 20);
            }
        });
        panel.add(zoomLabel);
        panel.add(zoomSlider);
        panel.add(resetZoomButton);
        panel.add(new JSeparator(JSeparator.VERTICAL) {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(1, 20);
            }
        });
        panel.add(fullscreenToggle);
        
        return panel;
    }
    
    private void closeScreenViewer() {
        if (screenViewerFrame != null) {
            // Make sure to exit fullscreen mode if active
            GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
            if (gd.getFullScreenWindow() == screenViewerFrame) {
                gd.setFullScreenWindow(null);
            }
            
            screenViewerFrame.dispose();
            screenViewerFrame = null;
            screenViewer = null;
            
            if (inputHandler != null) {
                inputHandler.cleanup();
                inputHandler = null;
            }
        }
    }
    
    // Client event listener implementation
    @Override
    public void onConnected() {
        SwingUtilities.invokeLater(() -> {
            connectButton.setEnabled(false);
            disconnectButton.setEnabled(true);
            chatPanel.setEnabled(true);
            
            chatPanel.addMessage("Connected to server.");
            
            // Show screen viewer
            showScreenViewer();
        });
    }
    
    @Override
    public void onDisconnected(String reason) {
        SwingUtilities.invokeLater(() -> {
            connectButton.setEnabled(true);
            disconnectButton.setEnabled(false);
            serverIPField.setEnabled(true);
            portField.setEnabled(true);
            passwordField.setEnabled(true);
            nameField.setEnabled(true);
            chatPanel.setEnabled(false);
            
            chatPanel.addMessage("Disconnected from server: " + reason);
            
            // Close screen viewer
            closeScreenViewer();
        });
    }
    
    @Override
    public void onConnectionFailed(String reason) {
        SwingUtilities.invokeLater(() -> {
            connectButton.setEnabled(true);
            serverIPField.setEnabled(true);
            portField.setEnabled(true);
            passwordField.setEnabled(true);
            nameField.setEnabled(true);
            
            chatPanel.addMessage("Connection failed: " + reason);
            
            JOptionPane.showMessageDialog(this, 
                "Connection failed: " + reason, 
                "Connection Error", 
                JOptionPane.ERROR_MESSAGE);
        });
    }
    
    @Override
    public void onChatMessageReceived(String sender, String message) {
        SwingUtilities.invokeLater(() -> {
            chatPanel.addMessage(sender + ": " + message);
        });
    }
    
    @Override
    public void onFileReceived(String sender, String fileName, byte[] data) {
        SwingUtilities.invokeLater(() -> {
            chatPanel.addMessage(sender + " sent file: " + fileName);
            
            // Ask user where to save the file
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setSelectedFile(new File(fileName));
            int option = fileChooser.showSaveDialog(this);
            
            if (option == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                try {
                    FileTransfer.saveFile(data, file.getName(), file.getParent());
                    
                    chatPanel.addMessage("File saved to: " + file.getAbsolutePath());
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, 
                        "Error saving file: " + ex.getMessage(), 
                        "File Error", 
                        JOptionPane.ERROR_MESSAGE);
                }
            }
        });
    }
    
    @Override
    public void onScreenUpdate(byte[] screenData) {
        if (screenViewer != null) {
            screenViewer.updateScreen(screenData);
        }
    }
    
    @Override
    public void onControlGranted() {
        SwingUtilities.invokeLater(() -> {
            chatPanel.addMessage("Control granted by server. You can now use your mouse and keyboard.");
            
            if (inputHandler != null) {
                inputHandler.setControlEnabled(true);
            }
        });
    }
    
    @Override
    public void onControlRevoked() {
        SwingUtilities.invokeLater(() -> {
            chatPanel.addMessage("Control revoked by server.");
            
            if (inputHandler != null) {
                inputHandler.setControlEnabled(false);
            }
        });
    }
}
