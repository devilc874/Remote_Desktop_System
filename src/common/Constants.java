package src.common;

public class Constants {
    // Network constants
    public static final int DEFAULT_PORT = 5000;
    public static final int BUFFER_SIZE = 8192;
    
    // Message types
    public static final int MESSAGE_TYPE_AUTHENTICATION = 0;
    public static final int MESSAGE_TYPE_CHAT = 1;
    public static final int MESSAGE_TYPE_FILE = 2;
    public static final int MESSAGE_TYPE_SCREEN = 3;
    public static final int MESSAGE_TYPE_MOUSE = 4;
    public static final int MESSAGE_TYPE_KEYBOARD = 5;
    public static final int MESSAGE_TYPE_CONTROL_GRANT = 6;
    public static final int MESSAGE_TYPE_CONTROL_REVOKE = 7;
    public static final int MESSAGE_TYPE_DISCONNECT = 8;
    
    // Screen capture settings
    public static final int MAX_FPS = 120; 
    public static final int MIN_FPS = 15;
    public static final int DEFAULT_FPS = 30;
    public static float JPEG_QUALITY = 0.7f; // Balance between quality and size
    
    // Display settings
    public static final boolean DEFAULT_MAINTAIN_ASPECT_RATIO = true;
    public static final double DEFAULT_ZOOM_FACTOR = 1.0;
}