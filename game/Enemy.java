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
    private int waveType = 1;  // which path pattern to follow
    private double rowYOffset = 0; // permanent vertical offset per row

    // shooting state
    private int shotsFired = 0;
    private final int maxShots = 4;
    private double gunAngle = Math.PI / 2.0; // pointing downward by default
    private int lastPassNumber = -1;  // Track which pass/row the enemy is on
    private int framesSinceRowStart = 0;  // Track frames elapsed in current row for staggered firing
    private long lastHitTime = 0;  // Track when enemy was last hit for health bar display

    public Enemy(double x, double y, int radius, double health, double pathOffset) {
        this(x, y, radius, health, pathOffset, 1);
    }

    public Enemy(double x, double y, int radius, double health, double pathOffset, int waveType) {
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.health = health;
        this.maxHealth = health;
        this.pathOffset = pathOffset;
        this.waveType = waveType;
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

    public double getRowYOffset() { return rowYOffset; }
    public void setRowYOffset(double offset) { this.rowYOffset = offset; }

    public int getWaveType() { return waveType; }

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

        // Animated color cycling — 3 phase-offset oscillators
        double p1 = 0.5 + 0.5 * Math.sin(t);
        double p2 = 0.5 + 0.5 * Math.sin(t + 2.094);
        double p3 = 0.5 + 0.5 * Math.sin(t + 4.188);

        int cr = (int)(160 + 95 * p1);
        int cg = (int)(60 + 195 * p2);
        int cb = (int)(120 + 135 * p3);

        Object oldAA = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        Paint oldPaint = g2.getPaint();
        Stroke oldStroke = g2.getStroke();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        double pulse = 0.85 + 0.15 * Math.sin(t * 3);

        // --- 1. Outer energy aura (soft radial glow) ---
        for (int i = 4; i >= 1; i--) {
            int a = (int)(25 * pulse * (1.0 - i / 5.0));
            g2.setColor(new Color(cr, cg, cb, a));
            int s = radius + i * 6;
            g2.fillOval(drawX - s, drawY - s / 2, s * 2, s);
        }

        // --- 2. Wing panels (swept-back, angular) ---
        double wingSpread = radius * 1.4;
        double wingBack = radius * 0.6;
        double wingThin = radius * 0.22;

        // Left wing
        Polygon leftWing = new Polygon();
        leftWing.addPoint(drawX - (int)(radius * 0.3), drawY - (int)(wingThin));
        leftWing.addPoint((int)(drawX - wingSpread), drawY + (int)(wingBack * 0.3));
        leftWing.addPoint((int)(drawX - wingSpread * 0.7), drawY + (int)(wingBack));
        leftWing.addPoint(drawX - (int)(radius * 0.15), drawY + (int)(wingThin * 0.5));

        // Right wing
        Polygon rightWing = new Polygon();
        rightWing.addPoint(drawX + (int)(radius * 0.3), drawY - (int)(wingThin));
        rightWing.addPoint((int)(drawX + wingSpread), drawY + (int)(wingBack * 0.3));
        rightWing.addPoint((int)(drawX + wingSpread * 0.7), drawY + (int)(wingBack));
        rightWing.addPoint(drawX + (int)(radius * 0.15), drawY + (int)(wingThin * 0.5));

        // Wing fill — dark metallic gradient
        GradientPaint lwPaint = new GradientPaint(
            drawX, drawY - radius, new Color(30, 80, 140),
            drawX - radius, drawY + radius, new Color(10, 30, 60));
        g2.setPaint(lwPaint);
        g2.fillPolygon(leftWing);
        GradientPaint rwPaint = new GradientPaint(
            drawX, drawY - radius, new Color(30, 80, 140),
            drawX + radius, drawY + radius, new Color(10, 30, 60));
        g2.setPaint(rwPaint);
        g2.fillPolygon(rightWing);

        // Wing edge glow
        g2.setColor(new Color(100, 200, 255, (int)(120 * pulse)));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawPolygon(leftWing);
        g2.drawPolygon(rightWing);

        // --- 3. Central body (elongated hexagon) ---
        int bw = (int)(radius * 0.45);  // half-width of body
        int bh = (int)(radius * 0.9);   // half-height of body
        int nose = (int)(radius * 0.25); // how pointy the nose is

        Polygon body = new Polygon();
        body.addPoint(drawX, drawY - bh - nose);           // nose tip
        body.addPoint(drawX + bw, drawY - bh / 2);         // upper right
        body.addPoint(drawX + bw, drawY + bh / 2);         // lower right
        body.addPoint(drawX, drawY + bh);                   // tail
        body.addPoint(drawX - bw, drawY + bh / 2);         // lower left
        body.addPoint(drawX - bw, drawY - bh / 2);         // upper left

        GradientPaint bodyPaint = new GradientPaint(
            drawX, drawY - bh, new Color(40, 160, 220),
            drawX, drawY + bh, new Color(15, 50, 100));
        g2.setPaint(bodyPaint);
        g2.fillPolygon(body);

        // Body edge
        g2.setColor(new Color(140, 220, 255, (int)(180 * pulse)));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawPolygon(body);

        // --- 4. Glowing core eye (animated color) ---
        int coreW = (int)(radius * 1.3);
        int coreH = (int)(radius * 0.35);

        Color coreInner = new Color(cr, cg, cb, 250);
        Color coreMid = new Color(
            Math.min(255, cr + 30), Math.min(255, cg + 30), Math.min(255, cb + 30), 180);
        Color coreOuter = new Color(cr, cg, cb, 0);

        try {
            RadialGradientPaint corePaint = new RadialGradientPaint(
                new Point2D.Float(drawX, drawY),
                Math.max(4f, coreW * 0.6f),
                new float[] {0f, 0.5f, 1f},
                new Color[] { coreInner, coreMid, coreOuter }
            );
            g2.setPaint(corePaint);
        } catch (Throwable t2) {
            g2.setColor(coreInner);
        }
        g2.fillOval(drawX - coreW / 2, drawY - coreH / 2, coreW, coreH);

        // Core outline — bright ring
        g2.setColor(new Color(255, 255, 255, (int)(100 * pulse)));
        g2.setStroke(new BasicStroke(1.0f));
        g2.drawOval(drawX - coreW / 2, drawY - coreH / 2, coreW, coreH);

        // Hot white dot at center
        g2.setColor(new Color(255, 255, 255, (int)(200 * pulse)));
        g2.fillOval(drawX - 2, drawY - 2, 4, 4);

        // --- 5. Engine exhaust (two small glowing trails at wing roots) ---
        for (int side = -1; side <= 1; side += 2) {
            int ex = drawX + side * (int)(radius * 0.25);
            int ey = drawY + (int)(bh * 0.8);
            double flicker = 0.6 + 0.4 * Math.sin(t * 8 + side * 1.5);
            int eLen = (int)(6 + 4 * flicker);
            for (int j = 0; j < eLen; j++) {
                int ea = (int)(180 * (1.0 - j / (double)eLen) * flicker);
                g2.setColor(new Color(100, 180, 255, ea));
                g2.fillOval(ex - 2, ey + j, 4, 2);
            }
        }

        // --- 6. Gun barrel (sleek, with tip glow) ---
        int gunLen = radius + 10;
        int gx2 = (int)Math.round(x + Math.cos(gunAngle) * gunLen);
        int gy2 = (int)Math.round(y + Math.sin(gunAngle) * gunLen);

        // Barrel shadow
        g2.setColor(new Color(0, 30, 60, 150));
        g2.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine(drawX, drawY, gx2, gy2);

        // Barrel bright
        g2.setColor(new Color(200, 230, 255));
        g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine(drawX, drawY, gx2, gy2);

        // Muzzle glow
        g2.setColor(new Color(cr, cg, cb, (int)(160 * pulse)));
        g2.fillOval(gx2 - 4, gy2 - 4, 8, 8);

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