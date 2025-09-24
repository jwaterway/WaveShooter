package game;

import javax.swing.JFrame;

public class Main {
    public static void main(String[] args) {
    	
    	MidiSynth.init("C:\\Users\\jwate\\ASU-CSE360-SP25\\WaveShooter\\FluidR3_GM.sf2");
    	

        // Game window
        JFrame window = new JFrame("Wave Shooter");
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setResizable(false);

        GamePanel gamePanel = new GamePanel();
        window.add(gamePanel);
        window.pack();
        window.setLocationRelativeTo(null);
        window.setVisible(true);

        gamePanel.startGameThread();
    }
}
