package game;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.RadialGradientPaint;

public class PowerUp {
    private double x, y;
    private final double radius = 14;
    private double timer = 8.0 * 60; // frames (approx 8 seconds at 60fps)
    private double age = 0; // lifetime counter
    private boolean alive = true;

    public PowerUp(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public void update() {
        timer -= 1;
        age += 1;
        if (timer <= 0) {
            alive = false;
        }
    }

    public void draw(Graphics2D g2) {
        if (!alive) return;
        // pulsing radiating orb
        float pulse = (float)(Math.sin(age * 0.1) * 3.0);
        float r = (float)radius + pulse;
        Point2D center = new Point2D.Float((float)x, (float)y);
        float[] dist = {0f, 0.6f, 1f};
        Color[] colors = {
            new Color(255, 255, 100, 220),
            new Color(255, 200, 50, 120),
            new Color(255, 100, 0, 0)
        };
        RadialGradientPaint rgp = new RadialGradientPaint(center, r, dist, colors);
        Paint old = g2.getPaint();
        g2.setPaint(rgp);
        g2.fillOval((int)(x - r), (int)(y - r), (int)(2*r), (int)(2*r));
        // outer ring for additional radiance
        float outer = r + 6;
        g2.setColor(new Color(255, 255, 150, 80));
        g2.setStroke(new BasicStroke(3f));
        g2.drawOval((int)(x - outer), (int)(y - outer), (int)(2*outer), (int)(2*outer));
        g2.setPaint(old);
    }

    public boolean isAlive() { return alive; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getRadius() { return radius; }
}
