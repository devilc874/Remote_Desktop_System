package src.server;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

public class InputHandler {
    private Robot robot;
    
    public InputHandler() {
        try {
            robot = new Robot();
        } catch (AWTException e) {
            e.printStackTrace();
        }
    }
    
    public void handleMouseEvent(String eventType, byte[] data) {
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(data);
            DataInputStream dis = new DataInputStream(bais);
            
            switch (eventType) {
                case "MOVE":
                    int x = dis.readInt();
                    int y = dis.readInt();
                    robot.mouseMove(x, y);
                    break;
                    
                case "PRESS":
                    x = dis.readInt();
                    y = dis.readInt();
                    int button = dis.readInt();
                    boolean pressed = dis.readBoolean();
                    
                    // Move to position first
                    robot.mouseMove(x, y);
                    
                    // Convert button to mask
                    int buttonMask = getButtonMask(button);
                    
                    if (pressed) {
                        robot.mousePress(buttonMask);
                    }
                    break;
                    
                case "RELEASE":
                    x = dis.readInt();
                    y = dis.readInt();
                    button = dis.readInt();
                    pressed = dis.readBoolean();
                    
                    // Move to position first
                    robot.mouseMove(x, y);
                    
                    // Convert button to mask
                    buttonMask = getButtonMask(button);
                    
                    if (!pressed) {
                        robot.mouseRelease(buttonMask);
                    }
                    break;
                    
                case "DRAG":
                    x = dis.readInt();
                    y = dis.readInt();
                    robot.mouseMove(x, y);
                    break;
                    
                case "WHEEL":
                    int rotation = dis.readInt();
                    robot.mouseWheel(rotation);
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void handleKeyboardEvent(String eventType, byte[] data) {
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(data);
            DataInputStream dis = new DataInputStream(bais);
            
            if (eventType.equals("KEY")) {
                int keyCode = dis.readInt();
                boolean pressed = dis.readBoolean();
                
                if (pressed) {
                    robot.keyPress(keyCode);
                } else {
                    robot.keyRelease(keyCode);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private int getButtonMask(int button) {
        switch (button) {
            case 1: // Left button
                return InputEvent.BUTTON1_DOWN_MASK;
            case 2: // Middle button
                return InputEvent.BUTTON2_DOWN_MASK;
            case 3: // Right button
                return InputEvent.BUTTON3_DOWN_MASK;
            default:
                return InputEvent.BUTTON1_DOWN_MASK;
        }
    }
}