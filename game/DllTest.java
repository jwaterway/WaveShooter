package game;

class DllTest {
    public static void main(String[] args) {
        System.out.println("=== DLL Loading Test ===");
        System.out.println("java.library.path: " + System.getProperty("java.library.path"));
        System.out.println();
        
        String[] dlls = {
            "jinput-dx8_64",
            "jinput-raw_64",
            "jinput-dx8",
            "jinput-raw"
        };
        
        for (String dll : dlls) {
            try {
                System.out.println("Attempting to load: " + dll);
                System.loadLibrary(dll);
                System.out.println("  ✓ Successfully loaded " + dll);
            } catch (UnsatisfiedLinkError e) {
                System.out.println("  ✗ Failed: " + e.getMessage());
            }
        }
        
        System.out.println();
        System.out.println("=== JInput Controller Test ===");
        try {
            Class<?> envClass = Class.forName("net.java.games.input.ControllerEnvironment");
            java.lang.reflect.Method getDefault = envClass.getMethod("getDefaultEnvironment");
            Object env = getDefault.invoke(null);
            
            java.lang.reflect.Method getControllers = envClass.getMethod("getControllers");
            Object[] controllers = (Object[]) getControllers.invoke(env);
            
            System.out.println("Found " + controllers.length + " controller(s)");
            for (int i = 0; i < controllers.length; i++) {
                System.out.println("  Controller " + i + ": " + controllers[i]);
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
