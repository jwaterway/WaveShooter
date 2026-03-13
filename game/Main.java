package game;

import javax.swing.JFrame;
import game.AudioManager;


public class Main {
    public static void main(String[] args) {
    // diagnostics: working directory and Java version
    System.out.println("working dir=" + System.getProperty("user.dir"));
    System.out.println("java.version=" + System.getProperty("java.version") + " / vendor=" + System.getProperty("java.vendor"));
    System.out.println("java.library.path=" + System.getProperty("java.library.path"));

    	AudioManager.init();
    	AudioManager.playMusicLoop();
    	

        // Game window
        JFrame window = new JFrame("Wave Shooter");
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setResizable(false);

        GamePanel gamePanel = new GamePanel();
        window.add(gamePanel);
        window.pack();
        window.setLocationRelativeTo(null);
        window.setVisible(true);

        // diagnostics: actual dimensions after showing
        System.out.println("frame size=" + window.getWidth() + "x" + window.getHeight());
        System.out.println("panel size=" + gamePanel.getWidth() + "x" + gamePanel.getHeight());
        // confirm which classes/jar are loaded
        System.out.println("Main location=" + Main.class.getProtectionDomain().getCodeSource().getLocation());
        System.out.flush();
        //gamePanel.startGameThread();
    }
}

