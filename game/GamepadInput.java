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
            System.out.println("[GamepadInput] Java: " + System.getProperty("java.version") + " | OS: " + System.getProperty("os.name") + " " + System.getProperty("os.arch"));
            System.out.println("[GamepadInput] Library path: " + System.getProperty("java.library.path"));
            
            Controller[] controllers = ControllerEnvironment.getDefaultEnvironment().getControllers();
            System.out.println("[GamepadInput] Found " + controllers.length + " controller(s)");
            
            for (int i = 0; i < controllers.length; i++) {
                System.out.println("[GamepadInput]   [" + i + "] " + controllers[i].getName() + " (" + controllers[i].getType() + ")");
                if (isGamepad(controllers[i])) {
                    this.gamepad = controllers[i];
                    this.available = true;
                    System.out.println("[GamepadInput] ✓ Using: " + gamepad.getName());
                    dumpComponents();
                    return;
                }
            }
            
            this.available = false;
            System.out.println("[GamepadInput] No suitable gamepad found. Keyboard/mouse only.");
            
        } catch (UnsatisfiedLinkError e) {
            System.out.println("[GamepadInput] ERROR: Native DhookError - DLLs not loaded: " + e.getMessage());
            this.available = false;
        } catch (Exception e) {
            System.out.println("[GamepadInput] Error: " + e.getMessage());
            e.printStackTrace();
            this.available = false;
        }
    }
    
    /**
     * Try to detect gamepads without full JInput support
     */
    private void tryAlternativeDetection() {
        try {
            System.out.println("[GamepadInput] Trying alternative detection...");
            
            // Try to scan with a timeout to avoid hanging
            Thread detectionThread = new Thread(() -> {
                try {
                    Controller[] controllers = ControllerEnvironment.getDefaultEnvironment().getControllers();
                    if (controllers != null && controllers.length > 0) {
                        for (Controller c : controllers) {
                            if (c != null && !isExcludedType(c)) {
                                System.out.println("[GamepadInput] Alternative found: " + c.getName());
                                gamepad = c;
                                available = true;
                                return;
                            }
                        }
                    }
                } catch (Exception e) {
                    // Silent fail - alternative method
                }
            });
            
            detectionThread.setDaemon(true);
            detectionThread.start();
            detectionThread.join(1000);  // Wait max 1 second
            
        } catch (Exception e) {
            // If alternative detection fails, just continue with keyboard-only
        }
    }
    
    /**
     * Check if controller type should be excluded (keyboard, mouse)
     */
    private boolean isExcludedType(Controller controller) {
        try {
            Controller.Type type = controller.getType();
            if (type == Controller.Type.KEYBOARD || 
                type == Controller.Type.MOUSE ||
                type == Controller.Type.TRACKBALL) {
                return true;
            }
        } catch (Exception e) {
            // Can't determine type, don't exclude
        }
        return false;
    }

    /**
     * Check if controller is a usable gamepad (not keyboard/mouse)
     * More lenient: accepts any input device with analog sticks or buttons
     */
    private boolean isGamepad(Controller controller) {
        Controller.Type type = controller.getType();
        
        // Prefer explicit gamepad/stick/wheel types
        if (type == Controller.Type.STICK ||
            type == Controller.Type.GAMEPAD || 
            type == Controller.Type.WHEEL) {
            return true;
        }
        
        // For generic controllers like EasySMX, check if it has useful components
        // Avoid keyboard and mouse
        if (type == Controller.Type.KEYBOARD || 
            type == Controller.Type.MOUSE ||
            type == Controller.Type.TRACKBALL) {
            return false;
        }
        
        // Check if this device has any analog axes (sticks/triggers) or many buttons
        try {
            Component[] components = controller.getComponents();
            int analogCount = 0;
            int buttonCount = 0;
            
            for (Component comp : components) {
                if (comp.isAnalog()) {
                    analogCount++;
                } else if (comp.getName().toLowerCase().contains("button")) {
                    buttonCount++;
                }
            }
            
            // If it has at least 2 analog axes and 1 button, it's probably a gamepad
            if (analogCount >= 2 && buttonCount >= 1) {
                System.out.println("[GamepadInput]   -> Has " + analogCount + " analog, " + buttonCount + " buttons -> treated as gamepad");
                return true;
            }
        } catch (Exception e) {
            // If we can't inspect, skip it
        }
        
        return false;
    }

    private void dumpComponents() {
        if (gamepad == null) return;
        Component[] comps = gamepad.getComponents();
        System.out.println("[GamepadInput] --- Component dump (" + comps.length + " components) ---");
        for (int i = 0; i < comps.length; i++) {
            Component c = comps[i];
            System.out.println("[GamepadInput]   [" + i + "] name=\"" + c.getName() + "\" id=" + c.getIdentifier() + " analog=" + c.isAnalog());
        }
        System.out.println("[GamepadInput] --- End dump ---");
    }

    private int debugCounter = 0;

    /**
     * Poll gamepad state (must be called each frame)
     */
    public void poll() {
        if (available && gamepad != null) {
            gamepad.poll();
            
            // Print active inputs every 60 frames (once per second)
            debugCounter++;
            if (debugCounter % 60 == 0) {
                StringBuilder sb = new StringBuilder();
                for (Component c : gamepad.getComponents()) {
                    float v = c.getPollData();
                    if (Math.abs(v) > 0.01f) {
                        sb.append(c.getName()).append("=").append(String.format("%.2f", v)).append(" ");
                    }
                }
                if (sb.length() > 0) {
                    System.out.println("[GamepadInput] Active: " + sb.toString().trim());
                }
            }
        }
    }
    
    public boolean isAvailable() {
        return available;
    }
    
    /**
     * Get horizontal movement input (-1.0 left, 0.0 center, 1.0 right)
     * Uses D-pad (Hat Switch) OR left stick, whichever is active.
     * POV hat: 0.25=UP, 0.5=RIGHT, 0.75=DOWN, 1.0=LEFT, diagonals in between
     */
    public float getDPadX() {
        if (!available || gamepad == null) return 0.0f;
        
        // Check D-pad (POV hat) first
        Component pov = gamepad.getComponent(Component.Identifier.Axis.POV);
        if (pov != null) {
            float v = pov.getPollData();
            if (v != 0.0f) {
                // Right component: UP_RIGHT(0.3125), RIGHT(0.5), DOWN_RIGHT(0.6875)
                if (v > 0.25f && v < 0.75f) return 1.0f;
                // Left component: DOWN_LEFT(0.8125), LEFT(1.0), UP_LEFT(0.125)
                if (v > 0.75f || (v > 0.0f && v < 0.25f)) return -1.0f;
                return 0.0f; // Pure UP or DOWN
            }
        }
        
        // Also check left stick
        Component xAxis = gamepad.getComponent(Component.Identifier.Axis.X);
        if (xAxis != null) {
            float value = xAxis.getPollData();
            if (Math.abs(value) > 0.2f) return value; // deadzone 0.2
        }
        return 0.0f;
    }

    /**
     * Get vertical movement input (-1.0 up, 0.0 center, 1.0 down)
     * Uses D-pad (Hat Switch) OR left stick, whichever is active.
     * Returns SCREEN coordinates: negative=up, positive=down.
     */
    public float getDPadY() {
        if (!available || gamepad == null) return 0.0f;
        
        // Check D-pad (POV hat) first
        Component pov = gamepad.getComponent(Component.Identifier.Axis.POV);
        if (pov != null) {
            float v = pov.getPollData();
            if (v != 0.0f) {
                // Up component: UP_LEFT(0.125), UP(0.25), UP_RIGHT(0.3125)
                if (v > 0.0f && v < 0.5f) return -1.0f;  // screen up = negative
                // Down component: DOWN_RIGHT(0.6875), DOWN(0.75), DOWN_LEFT(0.8125)
                if (v > 0.5f && v < 1.0f) return 1.0f;    // screen down = positive
                return 0.0f; // Pure LEFT or RIGHT
            }
        }
        
        // Also check left stick (Y axis: negative=up in JInput)
        Component yAxis = gamepad.getComponent(Component.Identifier.Axis.Y);
        if (yAxis != null) {
            float value = yAxis.getPollData();
            if (Math.abs(value) > 0.2f) return value; // deadzone 0.2, already screen-correct
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
     * Get left trigger value (0.0 = released, 1.0 = fully pressed)
     * Xbox 360 via JInput: Z axis negative = LT
     */
    public float getLeftTrigger() {
        if (!available || gamepad == null) return 0.0f;
        Component z = gamepad.getComponent(Component.Identifier.Axis.Z);
        if (z != null) {
            float value = z.getPollData();
            return value < -0.1f ? -value : 0.0f;  // LT is negative Z, return as positive
        }
        return 0.0f;
    }

    /**
     * Get right trigger value (0.0 = released, 1.0 = fully pressed)
     * Xbox 360 via JInput: Z axis positive = RT
     */
    public float getRightTrigger() {
        if (!available || gamepad == null) return 0.0f;
        Component z = gamepad.getComponent(Component.Identifier.Axis.Z);
        if (z != null) {
            float value = z.getPollData();
            return value > 0.1f ? value : 0.0f;  // RT is positive Z
        }
        return 0.0f;
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
     * Check if Start button is pressed (pause)
     */
    public boolean isStartPressed() {
        if (!available || gamepad == null) return false;
        
        Component start = gamepad.getComponent(Component.Identifier.Button._7);
        return start != null && start.getPollData() == 1.0f;
    }

    /**
     * Check if Back/Select button is pressed (quit)
     */
    public boolean isBackPressed() {
        if (!available || gamepad == null) return false;
        
        Component back = gamepad.getComponent(Component.Identifier.Button._6);
        return back != null && back.getPollData() == 1.0f;
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
