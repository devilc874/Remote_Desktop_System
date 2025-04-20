package src.server;

import src.common.Constants;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicLong;

public class ScreenCapturer {
    private Robot robot;
    private Rectangle screenRect;
    private boolean isCapturing;
    private byte[] latestScreenshot;
    private int currentFps = Constants.DEFAULT_FPS;
    private boolean autoAdjustFps = true; // Add flag to control auto-adjustment
    
    // Performance monitoring
    private AtomicLong lastTransmissionTime = new AtomicLong(0);
    private AtomicLong transmissionDuration = new AtomicLong(0);

    public void setTargetFps(int fps) {
        this.currentFps = Math.max(Constants.MIN_FPS, Math.min(Constants.MAX_FPS, fps));
        this.autoAdjustFps = false; // When manually setting FPS, disable auto-adjustment
    }
    
    // Add method to control auto FPS adjustment
    public void enableAutoFpsAdjustment(boolean enable) {
        this.autoAdjustFps = enable;
    }

    public ScreenCapturer() {
        try {
            robot = new Robot();
            
            // Get screen size
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice[] screens = ge.getScreenDevices();
            
            // Calculate total screen bounds
            Rectangle totalBounds = new Rectangle();
            for (GraphicsDevice screen : screens) {
                Rectangle bounds = screen.getDefaultConfiguration().getBounds();
                totalBounds = totalBounds.union(bounds);
            }
            
            screenRect = totalBounds;
            
        } catch (AWTException e) {
            e.printStackTrace();
        }
    }
    
    public void startCapturing() {
        if (isCapturing) return;
        
        isCapturing = true;
        
        new Thread(() -> {
            long lastCaptureTime = System.currentTimeMillis();
            
            while (isCapturing) {
                try {
                    // Calculate proper frame interval in milliseconds
                    long frameInterval = 1000 / currentFps;
                    
                    // Capture screen
                    BufferedImage screenshot = robot.createScreenCapture(screenRect);
                    
                    // Compress and convert to bytes
                    long startTime = System.currentTimeMillis();
                    latestScreenshot = compressImage(screenshot);
                    
                    // Calculate compression time
                    long compressionTime = System.currentTimeMillis() - startTime;
                    
                    // Get transmission time from the last frame
                    long transmissionTime = transmissionDuration.get();
                    
                    // Adjust frame rate based on performance if auto-adjustment is enabled
                    if (autoAdjustFps) {
                        adjustFrameRate(compressionTime + transmissionTime);
                    }
                    
                    // Reset transmission time for next frame
                    lastTransmissionTime.set(System.currentTimeMillis());
                    
                    // Calculate how long to sleep to maintain desired frame rate
                    long currentTime = System.currentTimeMillis();
                    long elapsedTime = currentTime - lastCaptureTime;
                    long sleepTime = Math.max(0, frameInterval - elapsedTime);
                    
                    // Sleep for the calculated time
                    if (sleepTime > 0) {
                        Thread.sleep(sleepTime);
                    }
                    
                    // Update last capture time for next frame calculation
                    lastCaptureTime = System.currentTimeMillis();
                    
                } catch (InterruptedException e) {
                    // Thread interrupted, exit gracefully
                    break;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
    
    public void stopCapturing() {
        isCapturing = false;
    }
    
    public byte[] getScreenshot() {
        // Record when this screenshot is transmitted
        long transmissionStart = System.currentTimeMillis();
        long lastTransmission = lastTransmissionTime.get();
        
        if (lastTransmission > 0) {
            // Calculate and store how long it took to transmit
            transmissionDuration.set(transmissionStart - lastTransmission);
        }
        
        return latestScreenshot;
    }
    
    private byte[] compressImage(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        // Get JPEG writer
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) {
            throw new IOException("No JPEG writer found");
        }
        
        ImageWriter writer = writers.next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        
        // Set compression quality
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(Constants.JPEG_QUALITY);
        
        // Write image
        MemoryCacheImageOutputStream outputStream = new MemoryCacheImageOutputStream(baos);
        writer.setOutput(outputStream);
        writer.write(null, new IIOImage(image, null, null), param);
        
        // Clean up
        writer.dispose();
        outputStream.close();
        
        return baos.toByteArray();
    }
    
    private void adjustFrameRate(long processingTimeMs) {
        // Only adjust if auto-adjustment is enabled
        if (!autoAdjustFps) return;
        
        // Target 80% of frame time for processing to allow buffer
        long targetFrameTime = 1000 / currentFps;
        long targetProcessingTime = (long)(targetFrameTime * 0.8);
        
        if (processingTimeMs > targetProcessingTime) {
            // Processing is taking too long, reduce frame rate
            currentFps = Math.max(Constants.MIN_FPS, currentFps - 1);
        } else if (processingTimeMs < targetProcessingTime / 2 && currentFps < Constants.MAX_FPS) {
            // Processing is very quick, can increase frame rate
            currentFps = Math.min(Constants.MAX_FPS, currentFps + 1);
        }
    }
    
    public void setCapturingBounds(Rectangle bounds) {
        // Allow overriding the capture area
        this.screenRect = bounds;
    }
    
    public Rectangle getScreenRect() {
        return screenRect;
    }
    
    public int getCurrentFps() {
        return currentFps;
    }
    
    public boolean isAutoAdjustFps() {
        return autoAdjustFps;
    }
}