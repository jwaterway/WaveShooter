package game;

import javax.swing.JFrame;


public class Main {
    public static void main(String[] args) {
    // diagnostic: print working directory so we can see where the JVM is starting
    System.out.println("working dir=" + System.getProperty("user.dir"));
    	MidiSynth.init("C:\\Users\\jwate\\ASU-CSE360-SP25\\WaveShooter\\FluidR3_GM.sf2");
    	
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

        //gamePanel.startGameThread();
    }
}
