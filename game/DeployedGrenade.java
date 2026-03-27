package game;

import java.awt.*;
import java.awt.geom.*;

/**
 * A grenade placed by the player that blinks for 1 second, then detonates.
 * During the blink period it pulses faster and faster.
 */
public class DeployedGrenade {

    private double x, y;
    private int timer = 0;
    private static final int FUSE_FRAMES = 60;  // 1 second at 60fps
    private boolean detonated = false;
    private boolean alive = true;
    private static final int RADIUS = 12;

    public DeployedGrenade(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public boolean isAlive() { return alive; }
    public boolean hasDetonated() { return detonated; }

    /** Returns true when fuse runs out (time to detonate). */
    public boolean update() {
        if (!alive) return false;
        timer++;
        if (timer >= FUSE_FRAMES) {
            detonated = true;
            alive = false;
            return true;  // signal: detonate now
        }
        return false;
    }

    public void draw(Graphics2D g2) {
        if (!alive) return;

        float progress = timer / (float) FUSE_FRAMES;  // 0..1

        // Blink rate increases as fuse burns: starts at 4Hz, ends at 20Hz
        double blinkFreq = 4.0 + progress * 16.0;
        double blinkPhase = Math.sin(timer * blinkFreq * 0.1);
        boolean bright = blinkPhase > 0;

        // Outer warning glow (expanding)
        float glowR = RADIUS + 6 + progress * 12;
        int glowAlpha = (int)(40 + progress * 60);
        Color warnColor = bright ? new Color(255, 100, 30, glowAlpha) : new Color(255, 60, 20, glowAlpha / 2);
        RadialGradientPaint glow = new RadialGradientPaint(
            new Point2D.Double(x, y), glowR,
            new float[]{0f, 0.6f, 1f},
            new Color[]{warnColor, new Color(255, 40, 10, glowAlpha / 3), new Color(0, 0, 0, 0)}
        );
        g2.setPaint(glow);
        g2.fillOval((int)(x - glowR), (int)(y - glowR), (int)(glowR * 2), (int)(glowR * 2));

        // Bomb body
        Color bodyColor = bright ? new Color(255, 120, 40) : new Color(180, 60, 20);
        g2.setColor(bodyColor);
        g2.fillOval((int)(x - RADIUS), (int)(y - RADIUS + 2), RADIUS * 2, RADIUS * 2 - 4);

        // Fuse line
        g2.setStroke(new BasicStroke(2f));
        g2.setColor(new Color(140, 130, 110));
        g2.drawLine((int)x, (int)(y - RADIUS + 2), (int)(x + 6), (int)(y - RADIUS - 6));

        // Fuse spark (flickers)
        if (bright) {
            g2.setColor(new Color(255, 240, 100));
            g2.fillOval((int)(x + 4), (int)(y - RADIUS - 9), 5, 5);
        }

        // Timer ring — fills as fuse burns
        g2.setStroke(new BasicStroke(2.5f));
        g2.setColor(new Color(255, 200, 60, 150));
        int arcDeg = (int)(360 * progress);
        g2.drawArc((int)(x - RADIUS - 3), (int)(y - RADIUS - 1), (RADIUS + 3) * 2, (RADIUS + 3) * 2, 90, -arcDeg);

        g2.setStroke(new BasicStroke(1f));
    }
}
