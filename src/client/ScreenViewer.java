package src.client;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

public class ScreenViewer extends JPanel {
    private BufferedImage screenImage;
    private double scaleX = 1.0;
    private double scaleY = 1.0;
    private Dimension originalSize;
    private boolean maintainAspectRatio = true;
    
    // Track the actual display area for cursor mapping
    private Rectangle displayArea = new Rectangle(0, 0, 0, 0);
    
    // Zoom factor (100% = normal)
    private double zoomFactor = 1.0;
    
    public ScreenViewer() {
        setBackground(Color.BLACK);
        addComponentAdapter();
    }
    
    private void addComponentAdapter() {
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                updateScaling();
            }
        });
    }
    
    public void updateScreen(byte[] imageData) {
        try {
            BufferedImage newImage = ImageIO.read(new ByteArrayInputStream(imageData));
            
            // If this is the first image, set original size
            if (screenImage == null && newImage != null) {
                originalSize = new Dimension(newImage.getWidth(), newImage.getHeight());
                updateScaling();
            }
            
            screenImage = newImage;
            repaint();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void updateScaling() {
        if (originalSize != null) {
            Dimension currentSize = getSize();
            
            if (maintainAspectRatio) {
                // Calculate scale while maintaining aspect ratio
                double widthRatio = (double) currentSize.width / originalSize.width;
                double heightRatio = (double) currentSize.height / originalSize.height;
                
                // Use the smaller ratio to ensure image fits within panel
                double ratio = Math.min(widthRatio, heightRatio);
                
                // Apply zoom factor
                ratio *= zoomFactor;
                
                scaleX = ratio;
                scaleY = ratio;
                
                // Calculate the actual display area for cursor mapping
                int scaledWidth = (int) (originalSize.width * scaleX);
                int scaledHeight = (int) (originalSize.height * scaleY);
                int x = (getWidth() - scaledWidth) / 2;
                int y = (getHeight() - scaledHeight) / 2;
                
                displayArea.setBounds(x, y, scaledWidth, scaledHeight);
            } else {
                // Scale independently in each dimension
                scaleX = (double) currentSize.width / originalSize.width;
                scaleY = (double) currentSize.height / originalSize.height;
                
                // Full panel area - no aspect ratio maintained
                displayArea.setBounds(0, 0, currentSize.width, currentSize.height);
            }
        }
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        if (screenImage != null) {
            Graphics2D g2d = (Graphics2D) g;
            
            // Enable better quality rendering
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, 
                              RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, 
                              RenderingHints.VALUE_RENDER_QUALITY);
            
            // Draw image based on display area
            g2d.drawImage(screenImage, 
                        displayArea.x, displayArea.y, 
                        displayArea.width, displayArea.height, 
                        null);
            
            // Optionally, add a subtle border to indicate active area
            if (maintainAspectRatio) {
                g2d.setColor(new Color(80, 80, 80, 100));
                g2d.drawRect(displayArea.x, displayArea.y, 
                          displayArea.width - 1, displayArea.height - 1);
            }
        }
    }
    
    public double getScaleX() {
        return scaleX;
    }
    
    public double getScaleY() {
        return scaleY;
    }
    
    public Rectangle getDisplayArea() {
        return displayArea;
    }
    
    public Dimension getOriginalSize() {
        return originalSize;
    }
    
    public void setMaintainAspectRatio(boolean maintainAspectRatio) {
        this.maintainAspectRatio = maintainAspectRatio;
        updateScaling();
        repaint();
    }
    
    public boolean getMaintainAspectRatio() {
        return maintainAspectRatio;
    }
    
    public void setZoomFactor(double zoomFactor) {
        this.zoomFactor = zoomFactor;
        updateScaling();
        repaint();
    }
    
    public double getZoomFactor() {
        return zoomFactor;
    }
    
    // Method to check if point is within the display area
    public boolean isPointInDisplayArea(Point p) {
        return displayArea.contains(p);
    }
    
    // Method to map panel coordinates to display area coordinates
    public Point mapToDisplayArea(Point p) {
        if (!isPointInDisplayArea(p)) {
            // If point is outside display area, clamp to the nearest point on the edge
            int x = Math.max(displayArea.x, Math.min(p.x, displayArea.x + displayArea.width - 1));
            int y = Math.max(displayArea.y, Math.min(p.y, displayArea.y + displayArea.height - 1));
            return new Point(x, y);
        }
        return p;
    }
}