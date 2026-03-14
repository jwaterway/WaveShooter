package game;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Composite;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;

public class EnemyShot {
    private double x, y;
    private double dx, dy;
    private double angle;
    private final double speed = 6.0;
    private final int length = 10;
    private final Color color = new Color(180, 80, 255); // Purple/violet glow
    private boolean alive = true;
    private int frameCounter = 0;  // Track frames for spark spawning

    public EnemyShot(double x, double y, double angle) {
        this.x = x;
        this.y = y;
        this.angle = angle;
        this.dx = speed * Math.cos(angle);
        this.dy = speed * Math.sin(angle);
    }

    public void update() {
        x += dx;
        y += dy;
        frameCounter++;
        if (x < -20 || x > GamePanel.WIDTH + 20 || y < -20 || y > GamePanel.HEIGHT + 20) {
            alive = false;
        }
    }
    
    // Check if it's time to spawn a spark (every 3 frames)
    public boolean shouldSpawnSpark() {
        return alive && frameCounter % 3 == 0;
    }

    public void draw(Graphics2D g2) {
        if (!alive) return;
        int x0 = (int)Math.round(x);
        int y0 = (int)Math.round(y);
        int x2 = (int)Math.round(x + length * Math.cos(angle));
        int y2 = (int)Math.round(y + length * Math.sin(angle));
        
        // Pulsing circular glow at projectile center
        double pulseIntensity = 0.5 + 0.5 * Math.sin(frameCounter * 0.3);
        int glowSize = (int)(4 + 3 * pulseIntensity);
        g2.setColor(new Color(180, 80, 255, (int)(80 * pulseIntensity)));
        g2.fillOval(x0 - glowSize/2, y0 - glowSize/2, glowSize, glowSize);
        
        // thin laser line
        g2.setStroke(new BasicStroke(1.0f));
        g2.setColor(color);
        g2.drawLine(x0, y0, x2, y2);
        // subtle impact glow at origin and tip (concentric alpha ovals)
        Composite orig = g2.getComposite();
        for (int i = 3; i >= 1; i--) {
            int a = 60 + i * 40; // 160..100
            int s = 6 + (4 - i) * 4; // 6,10,14
            g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), a));
            g2.fillOval(x2 - s/2, y2 - s/2, s, s);
            g2.fillOval(x0 - s/3, y0 - s/3, s/2, s/2);
        }
        // bright core spark at tip
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.95f));
        g2.setColor(new Color(255, 200, 255, 255));
        g2.fillOval(x2-2, y2-2, 4, 4);
        g2.setComposite(orig);
    }

    public boolean isAlive() { return alive; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getRadius() { return 3; }
}
