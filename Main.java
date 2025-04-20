import javax.swing.*;
import src.common.database.MongoDBConnection;

public class Main {
    public static void main(String[] args) {
        try {
            // Set system look and feel
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            
            // Initialize MongoDB connection
            initializeMongoDB();
            
            // Add shutdown hook to close MongoDB connection
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                MongoDBConnection.closeConnection();
                System.out.println("MongoDB connection closed");
            }));
            
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Remote Desktop System");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            
            JTabbedPane tabbedPane = new JTabbedPane();
            
            // Add client tab
            tabbedPane.addTab("Client", new src.client.ClientGUI());
            
            // Add server tab
            tabbedPane.addTab("Server", new src.server.ServerGUI());
            
            frame.getContentPane().add(tabbedPane);
            frame.setSize(800, 600);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
    
    private static void initializeMongoDB() {
        try {
            // This will establish the connection
            MongoDBConnection.getDatabase();
            System.out.println("MongoDB connection established");
        } catch (Exception e) {
            System.err.println("Failed to connect to MongoDB: " + e.getMessage());
            // Allow the program to continue even if MongoDB fails to connect
        }
    }
}