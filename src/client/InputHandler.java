package src.client;

import src.common.Constants;

import java.awt.*;
import java.awt.event.*;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class InputHandler {
    private Client client;
    private ScreenViewer screenViewer;
    private boolean controlEnabled;
    
    // Mouse listeners
    private MouseAdapter mouseAdapter;
    private MouseMotionAdapter mouseMotionAdapter;
    private MouseWheelListener mouseWheelListener;
    
    // Keyboard listeners
    private KeyAdapter keyAdapter;
    
    public InputHandler(Client client, ScreenViewer screenViewer) {
        this.client = client;
        this.screenViewer = screenViewer;
        // Replace client.hasControl() since that method doesn't exist
        this.controlEnabled = false; // Start with control disabled by default
        
        initializeListeners();
        registerListeners();
    }
    
    private void initializeListeners() {
        // Mouse listener for clicks
        mouseAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (!controlEnabled) return;
                
                try {
                    // Scale coordinates to server's screen size
                    Point scaledPoint = scalePoint(e.getPoint());
                    
                    // Create data with adjusted coordinates and button info
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    DataOutputStream dos = new DataOutputStream(baos);
                    
                    dos.writeInt(scaledPoint.x);
                    dos.writeInt(scaledPoint.y);
                    dos.writeInt(e.getButton());
                    dos.writeBoolean(true); // Pressed
                    
                    // Send to server
                    client.sendMouseEvent("PRESS", baos.toByteArray());
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                if (!controlEnabled) return;
                
                try {
                    // Scale coordinates to server's screen size
                    Point scaledPoint = scalePoint(e.getPoint());
                    
                    // Create data with adjusted coordinates and button info
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    DataOutputStream dos = new DataOutputStream(baos);
                    dos.writeInt(scaledPoint.x);
                    dos.writeInt(scaledPoint.y);
                    dos.writeInt(e.getButton());
                    dos.writeBoolean(false); // Released
                    
                    // Send to server
                    client.sendMouseEvent("RELEASE", baos.toByteArray());
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        };
        
        // Mouse motion listener for movement and dragging
        mouseMotionAdapter = new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                if (!controlEnabled) return;
                
                try {
                    // Scale coordinates to server's screen size
                    Point scaledPoint = scalePoint(e.getPoint());
                    
                    // Create data with adjusted coordinates
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    DataOutputStream dos = new DataOutputStream(baos);
                    
                    dos.writeInt(scaledPoint.x);
                    dos.writeInt(scaledPoint.y);
                    
                    // Send to server
                    client.sendMouseEvent("MOVE", baos.toByteArray());
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
            
            @Override
            public void mouseDragged(MouseEvent e) {
                if (!controlEnabled) return;
                
                try {
                    // Scale coordinates to server's screen size
                    Point scaledPoint = scalePoint(e.getPoint());
                    
                    // Create data with adjusted coordinates
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    DataOutputStream dos = new DataOutputStream(baos);
                    
                    dos.writeInt(scaledPoint.x);
                    dos.writeInt(scaledPoint.y);
                    dos.writeInt(e.getButton());
                    
                    // Send to server
                    client.sendMouseEvent("DRAG", baos.toByteArray());
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        };
        
        // Mouse wheel listener for scrolling
        mouseWheelListener = e -> {
            if (!controlEnabled) return;
            
            try {
                // Create data with wheel rotation
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                DataOutputStream dos = new DataOutputStream(baos);
                
                dos.writeInt(e.getWheelRotation());
                
                // Send to server
                client.sendMouseEvent("WHEEL", baos.toByteArray());
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        };
        
        // Keyboard listener for key presses and releases
        keyAdapter = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (!controlEnabled) return;
                
                try {
                    // Create data with key code
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    DataOutputStream dos = new DataOutputStream(baos);
                    
                    dos.writeInt(e.getKeyCode());
                    dos.writeBoolean(true); // Pressed
                    
                    // Send to server
                    client.sendKeyboardEvent("KEY", baos.toByteArray());
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
            
            @Override
            public void keyReleased(KeyEvent e) {
                if (!controlEnabled) return;
                
                try {
                    // Create data with key code
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    DataOutputStream dos = new DataOutputStream(baos);
                    
                    dos.writeInt(e.getKeyCode());
                    dos.writeBoolean(false); // Released
                    
                    // Send to server
                    client.sendKeyboardEvent("KEY", baos.toByteArray());
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        };
    }
    
    private void registerListeners() {
        screenViewer.addMouseListener(mouseAdapter);
        screenViewer.addMouseMotionListener(mouseMotionAdapter);
        screenViewer.addMouseWheelListener(mouseWheelListener);
        
        // For keyboard events, we need to make sure the panel can get focus
        screenViewer.setFocusable(true);
        screenViewer.addKeyListener(keyAdapter);
        
        // Request focus when clicked
        screenViewer.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                screenViewer.requestFocusInWindow();
            }
        });
    }
    
    private Point scalePoint(Point clientPoint) {
        if (screenViewer.getOriginalSize() == null) {
            return clientPoint;
        }
        
        // First, map point to the actual display area (handles black borders)
        Point mappedPoint = screenViewer.mapToDisplayArea(clientPoint);
        
        // Get display area information
        Rectangle displayArea = screenViewer.getDisplayArea();
        
        // Get original dimensions
        Dimension originalSize = screenViewer.getOriginalSize();
        
        // Adjust the coordinate by the display area offset
        int adjustedX = mappedPoint.x - displayArea.x;
        int adjustedY = mappedPoint.y - displayArea.y;
        
        // Now scale relative to the original screen dimensions
        int serverX = (int)(adjustedX * (originalSize.width / (double)displayArea.width));
        int serverY = (int)(adjustedY * (originalSize.height / (double)displayArea.height));
        
        return new Point(serverX, serverY);
    }
    
    public void setControlEnabled(boolean enabled) {
        this.controlEnabled = enabled;
    }
    
    public void cleanup() {
        screenViewer.removeMouseListener(mouseAdapter);
        screenViewer.removeMouseMotionListener(mouseMotionAdapter);
        screenViewer.removeMouseWheelListener(mouseWheelListener);
        screenViewer.removeKeyListener(keyAdapter);
    }
}