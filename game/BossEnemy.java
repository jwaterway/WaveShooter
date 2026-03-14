package game;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.ArrayList;

/**
 * Floating base boss — roughly 10x the size of a regular enemy.
 * Hovers behind the enemy formation, periodically launches homing missiles.
 */
public class BossEnemy {
    private double x, y;
    private final int radius = 120; // ~10x regular enemy (radius 22 → ~12 units diameter vs ~240)
    private double health;
    private final double maxHealth;
    private boolean alive = true;
    private double age = 0;
    private long lastHitTime = 0;

    // Movement: gentle hover
    private double hoverPhase;
    private double targetX, targetY;

    // Missile firing
    private int missileTimer = 0;
    private final int missileInterval = 120; // fire every 2 seconds

    public BossEnemy(double x, double y, double health) {
        this.x = x;
        this.y = y;
        this.health = health;
        this.maxHealth = health;
        this.hoverPhase = Math.random() * Math.PI * 2;
        this.targetX = x;
        this.targetY = y;
    }

    public void update(double playerX, double playerY, ArrayList<HomingMissile> missiles) {
        age++;
        hoverPhase += 0.015;

        // Gentle hover motion
        double hoverX = targetX + Math.sin(hoverPhase) * 80;
        double hoverY = targetY + Math.cos(hoverPhase * 0.7) * 30;
        x += (hoverX - x) * 0.02;
        y += (hoverY - y) * 0.02;

        // Fire missiles
        missileTimer++;
        if (missileTimer >= missileInterval && alive) {
            missileTimer = 0;
            missiles.add(new HomingMissile(x, y + radius * 0.5, playerX, playerY));
            AudioManager.playSfx("homing", 0.7f);
        }
    }

    public void takeDamage(double dmg) {
        health -= dmg;
        lastHitTime = System.currentTimeMillis();
        if (health <= 0) {
            alive = false;
        }
    }

    public void draw(Graphics2D g2) {
        if (!alive) return;

        int cx = (int) x, cy = (int) y;
        Object oldAA = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        Paint oldPaint = g2.getPaint();
        Stroke oldStroke = g2.getStroke();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        double pulse = 0.85 + 0.15 * Math.sin(age * 0.04);
        double t = age * 0.02;

        // --- 1. Massive outer shield glow ---
        for (int i = 5; i >= 1; i--) {
            int a = (int)(20 * pulse * (1.0 - i / 6.0));
            g2.setColor(new Color(100, 60, 200, a));
            int s = radius + i * 12;
            g2.fillOval(cx - s, cy - (int)(s * 0.6), s * 2, (int)(s * 1.2));
        }

        // --- 2. Hull plates (large octagon) ---
        Polygon hull = new Polygon();
        int sides = 8;
        for (int i = 0; i < sides; i++) {
            double a = Math.PI / sides + i * 2 * Math.PI / sides + t * 0.1;
            double r = radius * (0.85 + 0.05 * Math.sin(a * 3 + t));
            hull.addPoint(cx + (int)(r * Math.cos(a)), cy + (int)(r * 0.6 * Math.sin(a)));
        }
        GradientPaint hullPaint = new GradientPaint(
            cx, cy - radius, new Color(50, 30, 90),
            cx, cy + radius, new Color(20, 10, 50));
        g2.setPaint(hullPaint);
        g2.fillPolygon(hull);

        // Hull edge glow
        g2.setColor(new Color(150, 100, 255, (int)(120 * pulse)));
        g2.setStroke(new BasicStroke(2.5f));
        g2.drawPolygon(hull);

        // --- 3. Armor panel lines ---
        g2.setStroke(new BasicStroke(1f));
        g2.setColor(new Color(120, 80, 200, (int)(60 * pulse)));
        for (int i = 0; i < sides; i++) {
            double a = Math.PI / sides + i * 2 * Math.PI / sides + t * 0.1;
            double r = radius * 0.85;
            g2.drawLine(cx, cy, cx + (int)(r * Math.cos(a)), cy + (int)(r * 0.6 * Math.sin(a)));
        }

        // --- 4. Central reactor core ---
        int coreR = (int)(radius * 0.35);
        float coreGlow = (float)(0.6 + 0.4 * Math.sin(age * 0.08));
        RadialGradientPaint corePaint = new RadialGradientPaint(
            new Point2D.Float(cx, cy), coreR,
            new float[]{0f, 0.4f, 1f},
            new Color[]{
                new Color(255, 200, 255, (int)(250 * coreGlow)),
                new Color(180, 100, 255, (int)(180 * coreGlow)),
                new Color(100, 50, 200, 0)
            }
        );
        g2.setPaint(corePaint);
        g2.fillOval(cx - coreR, cy - coreR, coreR * 2, coreR * 2);

        // Core ring pulse
        double ringPulse = 0.5 + 0.5 * Math.sin(age * 0.1);
        int ringR = coreR + (int)(8 * ringPulse);
        g2.setColor(new Color(200, 150, 255, (int)(100 * ringPulse)));
        g2.setStroke(new BasicStroke(2f));
        g2.drawOval(cx - ringR, cy - ringR, ringR * 2, ringR * 2);

        // --- 5. Rotating energy arcs around hull ---
        g2.setStroke(new BasicStroke(1.5f));
        for (int i = 0; i < 3; i++) {
            double arcAngle = t * 1.5 + i * 2 * Math.PI / 3;
            int arcA = (int)(120 * pulse);
            g2.setColor(new Color(180, 120, 255, arcA));
            int arcR = radius + 4;
            int startDeg = (int) Math.toDegrees(arcAngle);
            g2.drawArc(cx - arcR, cy - (int)(arcR * 0.6), arcR * 2, (int)(arcR * 1.2), startDeg, 40);
        }

        // --- 6. Engine exhausts (4 spots on bottom) ---
        for (int side = -2; side <= 2; side++) {
            if (side == 0) continue;
            int ex = cx + side * (int)(radius * 0.3);
            int ey = cy + (int)(radius * 0.55);
            double flicker = 0.5 + 0.5 * Math.sin(age * 0.15 + side * 2);
            int eLen = (int)(12 + 8 * flicker);
            for (int j = 0; j < eLen; j++) {
                int ea = (int)(150 * (1.0 - j / (double) eLen) * flicker);
                g2.setColor(new Color(140, 80, 255, ea));
                g2.fillOval(ex - 3, ey + j, 6, 3);
            }
        }

        // --- 7. Missile launcher ports (subtle) ---
        g2.setColor(new Color(200, 100, 50, (int)(140 * pulse)));
        int portR = 5;
        g2.fillOval(cx - radius / 2 - portR, cy + (int)(radius * 0.3) - portR, portR * 2, portR * 2);
        g2.fillOval(cx + radius / 2 - portR, cy + (int)(radius * 0.3) - portR, portR * 2, portR * 2);

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAA);
        g2.setPaint(oldPaint);
        g2.setStroke(oldStroke);
    }

    // Getters
    public boolean isAlive() { return alive; }
    public double getX() { return x; }
    public double getY() { return y; }
    public int getRadius() { return radius; }
    public double getHealth() { return health; }
    public double getMaxHealth() { return maxHealth; }
    public long getLastHitTime() { return lastHitTime; }

    public void setTargetPosition(double tx, double ty) {
        this.targetX = tx;
        this.targetY = ty;
    }
}
