package src.server;

import src.common.Constants;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import src.common.database.UserDAO;
import src.common.database.SessionDAO;
import src.common.model.User;
import src.common.model.Session;
import org.bson.types.ObjectId;

public class ServerGUI extends JPanel implements Server.ServerEventListener {
    private Server server;
    private UserDAO userDAO;
    private SessionDAO sessionDAO;
    private ObjectId currentSessionId;
    
    // Server controls
    private JTextField ipAddressField;
    private JTextField portField;
    private JPasswordField passwordField;
    private JButton startButton;
    private JButton stopButton;
    
    // Connected clients
    private JList<String> clientList;
    private DefaultListModel<String> clientListModel;
    private JButton grantControlButton;
    private JButton revokeControlButton;
    
    // Chat panel
    private ServerChatPanel chatPanel;
    
    // Screen sharing settings
    private JSlider qualitySlider;
    private JSlider fpsSlider;
    private JCheckBox autoFpsCheckbox;
    
    // Update constructor
    public ServerGUI() {
        server = new Server();
        server.addListener(this);
        
        // Initialize MongoDB DAOs
        userDAO = new UserDAO();
        sessionDAO = new SessionDAO();
        
        setLayout(new BorderLayout(10, 10));
        initComponents();
    }
    
    private void initComponents() {
        // Server configuration panel
        JPanel configPanel = new JPanel();
        configPanel.setLayout(new GridBagLayout());
        configPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(), "Server Configuration", 
            TitledBorder.CENTER, TitledBorder.TOP));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        // IP Address
        gbc.gridx = 0;
        gbc.gridy = 0;
        configPanel.add(new JLabel("IP Address:"), gbc);
        
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        ipAddressField = new JTextField(getLocalIpAddress());
        configPanel.add(ipAddressField, gbc);
        
        // Port
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.0;
        configPanel.add(new JLabel("Port:"), gbc);
        
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        portField = new JTextField(String.valueOf(Constants.DEFAULT_PORT));
        configPanel.add(portField, gbc);
        
        // Password
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0.0;
        configPanel.add(new JLabel("Password:"), gbc);
        
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.weightx = 1.0;
        passwordField = new JPasswordField();
        configPanel.add(passwordField, gbc);
        
        // Start/Stop buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        startButton = new JButton("Start Listening");
        stopButton = new JButton("Stop Listening");
        stopButton.setEnabled(false);
        
        startButton.addActionListener(this::startServer);
        stopButton.addActionListener(this::stopServer);
        
        buttonPanel.add(startButton);
        buttonPanel.add(stopButton);
        
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        configPanel.add(buttonPanel, gbc);
        
        // Screen sharing settings panel
        JPanel settingsPanel = new JPanel();
        settingsPanel.setLayout(new GridBagLayout());
        settingsPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(), "Screen Sharing Settings", 
            TitledBorder.CENTER, TitledBorder.TOP));
        
        gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        // FPS slider with updated tick spacing for 120Hz
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.0;
        settingsPanel.add(new JLabel("FPS:"), gbc);
        
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        fpsSlider = new JSlider(JSlider.HORIZONTAL, Constants.MIN_FPS, Constants.MAX_FPS, Constants.DEFAULT_FPS);
        fpsSlider.setMajorTickSpacing(30); // Changed from 15 to 30 for better spacing with 120 max
        fpsSlider.setMinorTickSpacing(15); // Changed from 5 to 15
        fpsSlider.setPaintTicks(true);
        fpsSlider.setPaintLabels(true);
        settingsPanel.add(fpsSlider, gbc);
        
        // Auto-adjust FPS checkbox
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        autoFpsCheckbox = new JCheckBox("Auto-adjust FPS", true);
        autoFpsCheckbox.addActionListener(e -> {
            boolean autoAdjust = autoFpsCheckbox.isSelected();
            server.enableAutoFpsAdjustment(autoAdjust);
            
            // When auto-adjust is enabled, disable the slider
            fpsSlider.setEnabled(!autoAdjust);
            
            if (autoAdjust) {
                chatPanel.addMessage("Auto FPS adjustment enabled");
            } else {
                chatPanel.addMessage("Manual FPS control enabled");
                // Apply current slider value when switching to manual mode
                server.setTargetFps(fpsSlider.getValue());
            }
        });
        settingsPanel.add(autoFpsCheckbox, gbc);
        
        // Quality slider
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        gbc.weightx = 0.0;
        settingsPanel.add(new JLabel("Quality:"), gbc);
        
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.weightx = 1.0;
        qualitySlider = new JSlider(JSlider.HORIZONTAL, 1, 10, (int)(Constants.JPEG_QUALITY * 10));
        qualitySlider.setMajorTickSpacing(3);
        qualitySlider.setMinorTickSpacing(1);
        qualitySlider.setPaintTicks(true);
        qualitySlider.setPaintLabels(true);
        settingsPanel.add(qualitySlider, gbc);
        
        // Client list panel
        JPanel clientPanel = new JPanel(new BorderLayout(5, 5));
        clientPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(), "Connected Clients", 
            TitledBorder.CENTER, TitledBorder.TOP));
        
        clientListModel = new DefaultListModel<>();
        clientList = new JList<>(clientListModel);
        clientList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        JScrollPane clientScrollPane = new JScrollPane(clientList);
        clientScrollPane.setPreferredSize(new Dimension(200, 150));
        clientPanel.add(clientScrollPane, BorderLayout.CENTER);
        
        // Control buttons
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        grantControlButton = new JButton("Grant Control");
        revokeControlButton = new JButton("Revoke Control");
        
        grantControlButton.setEnabled(false);
        revokeControlButton.setEnabled(false);
        
        grantControlButton.addActionListener(this::grantControl);
        revokeControlButton.addActionListener(this::revokeControl);
        
        controlPanel.add(grantControlButton);
        controlPanel.add(revokeControlButton);
        clientPanel.add(controlPanel, BorderLayout.SOUTH);
        
        // Chat panel
        chatPanel = new ServerChatPanel(server);
        
        // Add panels to main layout
        JPanel topLeftPanel = new JPanel(new BorderLayout());
        topLeftPanel.add(configPanel, BorderLayout.NORTH);
        topLeftPanel.add(settingsPanel, BorderLayout.CENTER);
        
        JPanel topPanel = new JPanel(new GridLayout(1, 2, 10, 10));
        topPanel.add(topLeftPanel);
        topPanel.add(clientPanel);
        
        add(topPanel, BorderLayout.NORTH);
        add(chatPanel, BorderLayout.CENTER);
        
        // Enable client selection to enable/disable control buttons
        clientList.addListSelectionListener(e -> {
            boolean hasSelection = !clientList.isSelectionEmpty();
            grantControlButton.setEnabled(hasSelection);
            
            if (hasSelection) {
                String selectedClient = clientList.getSelectedValue();
                String clientWithControl = server.getClientWithControl();
                revokeControlButton.setEnabled(selectedClient.equals(clientWithControl));
            } else {
                revokeControlButton.setEnabled(false);
            }
        });
        
        // Add listeners for quality and FPS sliders
        qualitySlider.addChangeListener(e -> {
            if (!qualitySlider.getValueIsAdjusting()) {
                float quality = qualitySlider.getValue() / 10.0f;
                server.setJpegQuality(quality);
                chatPanel.addMessage("Quality set to: " + quality);
            }
        });
        
        fpsSlider.addChangeListener(e -> {
            if (!fpsSlider.getValueIsAdjusting() && !autoFpsCheckbox.isSelected()) {
                int fps = fpsSlider.getValue();
                server.setTargetFps(fps);
                chatPanel.addMessage("FPS set to: " + fps);
            }
        });
        
        // Initially disable FPS slider if auto-adjust is on
        fpsSlider.setEnabled(!autoFpsCheckbox.isSelected());
        
        // Set border padding for main panel
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    }
    
    private String getLocalIpAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp()) continue;
                
                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr.getHostAddress().contains(".")) { // IPv4 only
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return "127.0.0.1"; // Default to localhost if no other address is found
    }
    
    // Update startServer method to record session
    private void startServer(ActionEvent e) {
        String ipAddress = ipAddressField.getText().trim();
        String portText = portField.getText().trim();
        String password = new String(passwordField.getPassword());
        
        if (ipAddress.isEmpty() || portText.isEmpty() || password.isEmpty()) {
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
        
        // Create server session in MongoDB
        try {
            // Check if "Server" user exists, create if not
            User serverUser = userDAO.findByUsername("Server");
            ObjectId serverId;
            
            if (serverUser == null) {
                User newServerUser = new User();
                newServerUser.setUsername("Server");
                newServerUser.setPassword(userDAO.hashPassword(password));
                newServerUser.setAdmin(true);
                serverId = userDAO.createUser(newServerUser);
            } else {
                serverId = serverUser.getId();
                // Update password if changed
                if (!userDAO.verifyPassword(password, serverUser.getPassword())) {
                    serverUser.setPassword(userDAO.hashPassword(password));
                    userDAO.updateUser(serverUser);
                }
            }
            
            // Create new session
            Session session = new Session();
            session.setUserId(serverId);
            session.setUsername("Server");
            session.setIpAddress(ipAddress);
            session.setClientInfo("Server Host");
            currentSessionId = sessionDAO.startSession(session);
            
            // Set session ID in server for client handlers
            server.setCurrentSessionId(currentSessionId);
            
            // Start the server
            server.startServer(ipAddress, port, password);
            
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, 
                "Database error: " + ex.getMessage(), 
                "Database Error", 
                JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }
    
    //stopServer method to end session
    private void stopServer(ActionEvent e) {
        // End MongoDB session
        if (currentSessionId != null) {
            sessionDAO.endSession(currentSessionId);
            currentSessionId = null;
        }
        
        server.stopServer();
    }
    
    private void grantControl(ActionEvent e) {
        if (!clientList.isSelectionEmpty()) {
            String clientName = clientList.getSelectedValue();
            server.grantControl(clientName);
        }
    }
    
    private void revokeControl(ActionEvent e) {
        if (!clientList.isSelectionEmpty()) {
            String clientName = clientList.getSelectedValue();
            server.revokeControl(clientName);
        }
    }
    
    // Server event listener implementation
    @Override
    public void onServerStarted() {
        SwingUtilities.invokeLater(() -> {
            startButton.setEnabled(false);
            stopButton.setEnabled(true);
            ipAddressField.setEnabled(false);
            portField.setEnabled(false);
            passwordField.setEnabled(false);
            chatPanel.setEnabled(true);
            
            chatPanel.addMessage("Server started. Waiting for clients...");
        });
    }
    
    @Override
    public void onServerStopped() {
        SwingUtilities.invokeLater(() -> {
            startButton.setEnabled(true);
            stopButton.setEnabled(false);
            ipAddressField.setEnabled(true);
            portField.setEnabled(true);
            passwordField.setEnabled(true);
            grantControlButton.setEnabled(false);
            revokeControlButton.setEnabled(false);
            chatPanel.setEnabled(false);
            
            // Clear client list
            clientListModel.clear();
            
            chatPanel.addMessage("Server stopped.");
        });
    }
    
    @Override
    public void onServerError(String message) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this, 
                "Server error: " + message, 
                "Server Error", 
                JOptionPane.ERROR_MESSAGE);
            
            chatPanel.addMessage("Server error: " + message);
        });
    }
    
    @Override
    public void onClientConnected(String clientName) {
        SwingUtilities.invokeLater(() -> {
            clientListModel.addElement(clientName);
            chatPanel.addMessage(clientName + " connected.");
            
            // Log connection in database through server's client handler
            // The actual DB operation happens in ClientHandler
        });
    }

    
    @Override
    public void onClientDisconnected(String clientName) {
        SwingUtilities.invokeLater(() -> {
            clientListModel.removeElement(clientName);
            
            // Update revoke button if this client had control
            if (clientName.equals(server.getClientWithControl())) {
                revokeControlButton.setEnabled(false);
            }
            
            chatPanel.addMessage(clientName + " disconnected.");
        });
    }
    
    @Override
    public void onControlGranted(String clientName) {
        SwingUtilities.invokeLater(() -> {
            chatPanel.addMessage("Control granted to " + clientName);
            
            // Update buttons if this client is selected
            if (!clientList.isSelectionEmpty() && clientList.getSelectedValue().equals(clientName)) {
                grantControlButton.setEnabled(false);
                revokeControlButton.setEnabled(true);
            }
        });
    }
    
    @Override
    public void onControlRevoked(String clientName) {
        SwingUtilities.invokeLater(() -> {
            chatPanel.addMessage("Control revoked from " + clientName);
            
            // Update buttons if this client is selected
            if (!clientList.isSelectionEmpty() && clientList.getSelectedValue().equals(clientName)) {
                grantControlButton.setEnabled(true);
                revokeControlButton.setEnabled(false);
            }
        });
    }
    
    @Override
    public void onChatMessageReceived(String sender, String message) {
        SwingUtilities.invokeLater(() -> {
            chatPanel.addMessage(sender + ": " + message);
        });
    }
    
    @Override
    public void onFileReceived(String sender, String fileName) {
        SwingUtilities.invokeLater(() -> {
            chatPanel.addMessage(sender + " sent file: " + fileName);
        });
    }
    
    @Override
    public void onMouseEvent(String eventType, byte[] data) {
        // This is handled by InputHandler, not in GUI
    }
    
    @Override
    public void onKeyboardEvent(String eventType, byte[] data) {
        // This is handled by InputHandler, not in GUI
    }
}