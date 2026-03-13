package game;

import net.java.games.input.*;

/**
 * Diagnostic tool to test gamepad detection
 */
public class GamepadDiagnostics {
    public static void main(String[] args) {
        System.out.println("=== JInput Gamepad Diagnostics ===");
        System.out.println("OS: " + System.getProperty("os.name"));
        System.out.println("Arch: " + System.getProperty("os.arch"));
        System.out.println("Java Version: " + System.getProperty("java.version"));
        System.out.println();
        
        try {
            System.out.println("Scanning for controllers...");
            Controller[] controllers = ControllerEnvironment.getDefaultEnvironment().getControllers();
            System.out.println("Found " + controllers.length + " controller(s)\n");
            
            for (int i = 0; i < controllers.length; i++) {
                Controller c = controllers[i];
                System.out.println("Controller " + i + ": " + c.getName());
                System.out.println("  Type: " + c.getType());
                System.out.println("  Port: " + c.getPortType());
                
                // List all components
                Component[] components = c.getComponents();
                System.out.println("  Components (" + components.length + "):");
                for (Component comp : components) {
                    System.out.println("    - " + comp.getName() + " (" + comp.getIdentifier() + ")");
                }
                System.out.println();
            }
        } catch (Exception e) {
            System.out.println("ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
