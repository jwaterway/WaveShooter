package game;

import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;
import net.java.games.input.Component;

/**
 * Gamepad input handler using JInput library.
 * Supports Xbox and generic gamepads on Windows.
 */
public class GamepadInput {
    private Controller gamepad;
    private boolean available;
    
    public GamepadInput() {
        try {
            // Find first gamepad/joystick controller
            Controller[] controllers = ControllerEnvironment.getDefaultEnvironment().getControllers();
            for (Controller controller : controllers) {
                if (isGamepad(controller)) {
                    this.gamepad = controller;
                    this.available = true;
                    System.out.println("Gamepad found: " + controller.getName());
                    return;
                }
            }
            this.available = false;
            System.out.println("No gamepad detected. Keyboard input only.");
        } catch (Exception e) {
            this.available = false;
            System.out.println("JInput library not available: " + e.getMessage());
        }
    }

    /**
     * Check if controller is a gamepad (not keyboard/mouse)
     */
    private boolean isGamepad(Controller controller) {
        Controller.Type type = controller.getType();
        return type == Controller.Type.STICK || 
               type == Controller.Type.GAMEPAD || 
               type == Controller.Type.WHEEL;
    }

    /**
     * Poll gamepad state (must be called each frame)
     */
    public void poll() {
        if (available && gamepad != null) {
            gamepad.poll();
        }
    }
    
    public boolean isAvailable() {
        return available;
    }
    
    /**
     * Get D-Pad horizontal input (-1.0 left, 0.0 center, 1.0 right)
     */
    public float getDPadX() {
        if (!available || gamepad == null) return 0.0f;
        
        Component pov = gamepad.getComponent(Component.Identifier.Axis.POV);
        if (pov != null) {
            float povValue = pov.getPollData();
            // POV values: 0.0=center/none, 0.25=up, 0.5=right, 0.75=down
            if (povValue >= 0.375f && povValue <= 0.625f) return 1.0f;   // right
            if (povValue >= 0.875f || povValue <= 0.125f) return 0.0f;   // center/up
            return -1.0f; // left
        }
        
        // Fallback to left stick if POV not available
        Component xAxis = gamepad.getComponent(Component.Identifier.Axis.X);
        if (xAxis != null) {
            float value = xAxis.getPollData();
            return Math.abs(value) > 0.5f ? Math.signum(value) : 0.0f;
        }
        return 0.0f;
    }

    /**
     * Get D-Pad vertical input (-1.0 down, 0.0 center, 1.0 up)
     */
    public float getDPadY() {
        if (!available || gamepad == null) return 0.0f;
        
        Component pov = gamepad.getComponent(Component.Identifier.Axis.POV);
        if (pov != null) {
            float povValue = pov.getPollData();
            if (povValue >= 0.125f && povValue <= 0.375f) return 1.0f;   // up
            if (povValue >= 0.625f && povValue <= 0.875f) return -1.0f;  // down
            return 0.0f; // center/horizontal
        }
        
        // Fallback to left stick if POV not available
        Component yAxis = gamepad.getComponent(Component.Identifier.Axis.Y);
        if (yAxis != null) {
            float value = yAxis.getPollData();
            return Math.abs(value) > 0.5f ? -Math.signum(value) : 0.0f; // inverted
        }
        return 0.0f;
    }
    
    /**
     * Get right stick X input for aiming (-1.0 left, 0.0 center, 1.0 right)
     */
    public float getRightStickX() {
        if (!available || gamepad == null) return 0.0f;
        
        Component rx = gamepad.getComponent(Component.Identifier.Axis.RX);
        if (rx != null) {
            float value = rx.getPollData();
            return Math.abs(value) > 0.1f ? value : 0.0f; // deadzone
        }
        return 0.0f;
    }

    /**
     * Get right stick Y input for aiming (-1.0 up, 0.0 center, 1.0 down)
     */
    public float getRightStickY() {
        if (!available || gamepad == null) return 0.0f;
        
        Component ry = gamepad.getComponent(Component.Identifier.Axis.RY);
        if (ry != null) {
            float value = ry.getPollData();
            return Math.abs(value) > 0.1f ? value : 0.0f; // deadzone
        }
        return 0.0f;
    }
    
    /**
     * Check if A button is pressed (fire)
     */
    public boolean isAPressed() {
        if (!available || gamepad == null) return false;
        
        Component a = gamepad.getComponent(Component.Identifier.Button._0);  // A button
        return a != null && a.getPollData() == 1.0f;
    }

    /**
     * Check if B button is pressed (weapon switch)
     */
    public boolean isBPressed() {
        if (!available || gamepad == null) return false;
        
        Component b = gamepad.getComponent(Component.Identifier.Button._1);  // B button
        return b != null && b.getPollData() == 1.0f;
    }

    /**
     * Check if X button is pressed
     */
    public boolean isXPressed() {
        if (!available || gamepad == null) return false;
        
        Component x = gamepad.getComponent(Component.Identifier.Button._2);  // X button
        return x != null && x.getPollData() == 1.0f;
    }

    /**
     * Check if Y button is pressed
     */
    public boolean isYPressed() {
        if (!available || gamepad == null) return false;
        
        Component y = gamepad.getComponent(Component.Identifier.Button._3);  // Y button
        return y != null && y.getPollData() == 1.0f;
    }

    /**
     * Check if left shoulder (LB) is pressed
     */
    public boolean isLBPressed() {
        if (!available || gamepad == null) return false;
        
        Component lb = gamepad.getComponent(Component.Identifier.Button._4);
        return lb != null && lb.getPollData() == 1.0f;
    }

    /**
     * Check if right shoulder (RB) is pressed
     */
    public boolean isRBPressed() {
        if (!available || gamepad == null) return false;
        
        Component rb = gamepad.getComponent(Component.Identifier.Button._5);
        return rb != null && rb.getPollData() == 1.0f;
    }

    /**
     * Get connected gamepad name
     */
    public String getGamepadName() {
        if (gamepad != null) {
            return gamepad.getName();
        }
        return "No Gamepad";
    }

    /**
     * Cleanup when game closes
     */
    public void shutdown() {
        // JInput doesn't require explicit cleanup
    }
}
