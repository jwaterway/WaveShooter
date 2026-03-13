package game;

import net.java.games.input.*;

/**
 * Testing tool to diagnose gamepad detection issues
 */
public class GamepadTest {
    public static void main(String[] args) throws Exception {
        System.out.println("=== JInput Gamepad Test ===");
        System.out.println("Java: " + System.getProperty("java.version"));
        System.out.println("OS: " + System.getProperty("os.name") + " (" + System.getProperty("os.arch") + ")");
        System.out.println("Library path: " + System.getProperty("java.library.path"));
        System.out.println();
        
        try {
            System.out.println("1. Getting controller environment...");
            ControllerEnvironment env = ControllerEnvironment.getDefaultEnvironment();
            System.out.println("   ✓ Environment: " + env.getClass().getName());
            System.out.println();
            
            System.out.println("2. Scanning for controllers...");
            Controller[] controllers = env.getControllers();
            System.out.println("   Found: " + controllers.length + " device(s)");
            System.out.println();
            
            if (controllers.length == 0) {
                System.out.println("   ✗ No controllers detected");
                System.out.println("   Possible solutions:");
                System.out.println("   - Is the gamepad connected and powered on?");
                System.out.println("   - Do you have gamepad drivers installed?");
                System.out.println("   - Try reconnecting the gamepad");
                System.out.println("   - Check Device Manager for unknown or error devices");
                return;
            }
            
            System.out.println("3. Analyzing each device:");
            for (int i = 0; i < controllers.length; i++) {
                Controller c = controllers[i];
                System.out.println();
                System.out.println("   Device " + i + ":");
                System.out.println("     Name: " + c.getName());
                System.out.println("     Type: " + c.getType());
                System.out.println("     Port: " + c.getPortType());
                
                // Poll to update state
                c.poll();
                
                Component[] comps = c.getComponents();
                System.out.println("     Components: " + comps.length);
                for (Component comp : comps) {
                    if (comp.isAnalog()) {
                        System.out.println("       - [AXIS] " + comp.getName() + " (" + comp.getIdentifier() + ")");
                    } else {
                        System.out.println("       - [BTN] " + comp.getName());
                    }
                }
            }
            
        } catch (Exception e) {
            System.out.println("   ✗ Error: " + e.getMessage());
            System.out.println();
            System.out.println("   Stack trace:");
            e.printStackTrace();
        }
    }
}
