package game;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.RadialGradientPaint;

public class Enemy {
    private double x, y;
    private final int radius;
    private double health;
    private final double maxHealth;  // Track max health for accurate health bar ratio
    private boolean alive = true;

    // how far behind the lead enemy this one is on the shared path
    private final double pathOffset;

    // shooting state
    private int shotsFired = 0;
    private final int maxShots = 6;
    private double gunAngle = Math.PI / 2.0; // pointing downward by default
    private int lastPassNumber = -1;  // Track which pass/row the enemy is on
    private int framesSinceRowStart = 0;  // Track frames elapsed in current row for staggered firing
    private long lastHitTime = 0;  // Track when enemy was last hit for health bar display

    public Enemy(double x, double y, int radius, double health, double pathOffset) {
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.health = health;
        this.maxHealth = health;  // Store max health at creation
        this.pathOffset = pathOffset;
    }

    public void setPosition(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public void takeDamage(double dmg) {
        health -= dmg;
        if (health <= 0) alive = false;
        lastHitTime = System.currentTimeMillis();  // Track hit for health bar display
    }
    
    public long getLastHitTime() { return lastHitTime; }
    public double getHealth() { return health; }
    public double getMaxHealth() { return maxHealth; }

    public boolean isAlive() {
        return alive;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public int getRadius() {
        return radius;
    }

    public double getPathOffset() {
        return pathOffset;
    }

    public int getShotsFired() { return shotsFired; }
    public void incrementShots() { shotsFired++; }
    public void resetShots() { shotsFired = 0; }
    public boolean canShoot() { return shotsFired < maxShots; }
    public int getFramesSinceRowStart() { return framesSinceRowStart; }
    public void incrementFrameCounter() { framesSinceRowStart++; }
    
    // Check if enemy has advanced to a new pass (row) and reset shots
    public void updatePassNumber(double waveT) {
        int currentPass = (int)((waveT - pathOffset) / 260);
        if (currentPass > lastPassNumber) {
            lastPassNumber = currentPass;
            resetShots();  // Reset shot counter for new pass
            framesSinceRowStart = 0;  // Reset frame counter for new row
        }
    }

    public double getGunAngle() { return gunAngle; }
    public void setGunAngle(double a) { gunAngle = a; }

    public void draw(Graphics2D g2) {
        int drawX = (int)Math.round(x);
        int drawY = (int)Math.round(y);
        double t = System.nanoTime() * 0.00000005 + pathOffset * 0.4;

     // 0..1 oscillators with different phase offsets
     double p1 = 0.5 + 0.5 * Math.sin(t);
     double p2 = 0.5 + 0.5 * Math.sin(t + 2.094); // +120 degrees
     double p3 = 0.5 + 0.5 * Math.sin(t + 4.188); // +240 degrees

     int r = (int)(160 + 95 * p1);
     int g = (int)(60 + 195 * p2);
     int b = (int)(120 + 135 * p3);

     Color coreInner = new Color(r, g, b, 245);
     Color coreMid   = new Color(
         Math.min(255, r),
         Math.min(255, g + 20),
         Math.min(255, b + 20),
         210
     );
     Color coreOuter = new Color(
         Math.min(255, r),
         Math.min(255, g),
         Math.min(255, b),
         90
     );

        Object oldAA = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        Paint oldPaint = g2.getPaint();
        Stroke oldStroke = g2.getStroke();

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // --- soft glow around enemy ---
        float glowR = radius * 1.6f;
        Point2D center = new Point2D.Float(drawX, drawY);
        int coreW = (int)Math.round(radius * 0.95);
        int coreH = (int)Math.round(radius * 0.55);

      
        try {
            RadialGradientPaint corePaint = new RadialGradientPaint(
                new Point2D.Float(drawX, drawY),
                Math.max(4f, coreW * 0.7f),
                new float[] {0f, 0.65f, 1f},
                new Color[] {
                    coreInner,
                    coreMid,
                    coreOuter
                }
            );
            g2.setPaint(corePaint);
            g2.fillOval(drawX - coreW / 2, drawY - coreH / 2, coreW, coreH);
        } catch (Throwable t2) {
            g2.setColor(coreInner);
            g2.fillOval(drawX - coreW / 2, drawY - coreH / 2, coreW, coreH);
        }
     // --- outer diamond shell ---
        Polygon diamond = new Polygon();
        diamond.addPoint(drawX, drawY - radius);
        diamond.addPoint(drawX + radius, drawY);
        diamond.addPoint(drawX, drawY + radius);
        diamond.addPoint(drawX - radius, drawY);

        GradientPaint shellPaint = new GradientPaint(
            drawX - radius, drawY - radius, new Color(50, 210, 255),
            drawX + radius, drawY + radius, new Color(0, 50, 140)
        );
        g2.setPaint(shellPaint);
        g2.fillPolygon(diamond);

        g2.setColor(new Color(180, 240, 255));
        g2.setStroke(new BasicStroke(2f));
        g2.drawPolygon(diamond);

        // --- glowing oval core (animated white <-> yellow) ---
        try {
            RadialGradientPaint corePaint = new RadialGradientPaint(
                new Point2D.Float(drawX, drawY),
                Math.max(4f, coreW * 0.7f),
                new float[] {0f, 0.65f, 1f},
                new Color[] {
                    coreInner,
                    coreMid,
                    coreOuter
                }
            );
            g2.setPaint(corePaint);
            g2.fillOval(drawX - coreW / 2, drawY - coreH / 2, coreW, coreH);
        } catch (Throwable t2) {
            g2.setColor(coreInner);
            g2.fillOval(drawX - coreW / 2, drawY - coreH / 2, coreW, coreH);
        }

        g2.setColor(new Color(255, 245, 200));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawOval(drawX - coreW / 2, drawY - coreH / 2, coreW, coreH);

        // draw simple gun extending from centre to just outside the diamond shell
        int extra = 8; // pixels past the diamond edge
        int gunLen = radius + extra;
        int gx1 = drawX;
        int gy1 = drawY;
        int gx2 = (int)Math.round(x + Math.cos(gunAngle) * gunLen);
        int gy2 = (int)Math.round(y + Math.sin(gunAngle) * gunLen);
        g2.setColor(new Color(255, 200, 50));
        g2.setStroke(new BasicStroke(3f));
        g2.drawLine(gx1, gy1, gx2, gy2);

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAA);
        g2.setPaint(oldPaint);
        g2.setStroke(oldStroke);
    }

    /**
     * Returns the tip point of the gun (used for spawning shots).
     */
    public Point getGunTip() {
        int extra = 8;
        double len = radius + extra;
        double tx = x + Math.cos(gunAngle) * len;
        double ty = y + Math.sin(gunAngle) * len;
        return new Point((int)Math.round(tx), (int)Math.round(ty));
    }
}