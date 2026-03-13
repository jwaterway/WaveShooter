package game;

import java.awt.Color;
import java.awt.Graphics2D;

public class SmokeParticle {
    private double x, y;
    private double dy;
    private double life = 1.0; // 1 -> 0

    public SmokeParticle(double x, double y) {
        this.x = x;
        this.y = y;
        this.dy = -0.5 - Math.random() * 0.5; // rise up slowly
    }

    public void update() {
        y += dy;
        life -= 0.02;
        if (life < 0) life = 0;
    }

    public void draw(Graphics2D g2) {
        if (life <= 0) return;
        int alpha = (int)(life * 80);
        int size = (int)(6 + (1 - life) * 12);
        g2.setColor(new Color(120, 120, 120, alpha));
        g2.fillOval((int)(x - size / 2), (int)(y - size / 2), size, size);
    }

    public boolean isAlive() {
        return life > 0;
    }
}
