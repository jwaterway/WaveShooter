package game;

import java.awt.*;
import java.awt.geom.Point2D;

/**
 * PhaseEnemy — Level 2 enemy that teleports between positions,
 * phases in/out of visibility, and fires rapid bursts when solid.
 * States: PHASING_IN (60f) → SOLID (120f) → PHASING_OUT (40f) → TELEPORT → repeat
 */
public class PhaseEnemy {
    private double x, y;
    private double targetX, targetY;
    private int radius = 26;
    private double health;
    private final double maxHealth;
    private boolean alive = true;
    private long lastHitTime = 0;

    enum State { PHASING_IN, SOLID, PHASING_OUT, TELEPORT }
    private State state = State.PHASING_IN;
    private int stateTimer = 0;

    // Aiming
    private double aimAngle = Math.PI / 2;
    private int burstCount = 0;
    private int burstCooldown = 0;

    // Phase visuals
    private double phaseAlpha = 0; // 0=invisible, 1=solid
    private double glitchOffset = 0;

    // World bounds
    private final int worldW, worldH;

    public PhaseEnemy(double x, double y, double health, int worldW, int worldH) {
        this.x = x;
        this.y = y;
        this.targetX = x;
        this.targetY = y;
        this.health = health;
        this.maxHealth = health;
        this.worldW = worldW;
        this.worldH = worldH;
    }

    public void update(double playerX, double playerY, java.util.List<EnemyShot> shots) {
        stateTimer++;

        // Aim toward player smoothly
        double desiredAngle = Math.atan2(playerY - y, playerX - x);
        double diff = desiredAngle - aimAngle;
        while (diff > Math.PI) diff -= Math.PI * 2;
        while (diff < -Math.PI) diff += Math.PI * 2;
        aimAngle += diff * 0.06;

        switch (state) {
            case PHASING_IN:
                phaseAlpha = Math.min(1.0, stateTimer / 60.0);
                glitchOffset = (1.0 - phaseAlpha) * 15 * Math.sin(stateTimer * 0.8);
                if (stateTimer >= 60) {
                    state = State.SOLID;
                    stateTimer = 0;
                    phaseAlpha = 1.0;
                    burstCount = 0;
                }
                break;

            case SOLID:
                phaseAlpha = 1.0;
                glitchOffset = 0;
                burstCooldown--;
                // Fire 3-round bursts
                if (burstCooldown <= 0 && burstCount < 3) {
                    double spd = 4.5;
                    shots.add(new EnemyShot(x + Math.cos(aimAngle) * radius,
                        y + Math.sin(aimAngle) * radius,
                        Math.cos(aimAngle) * spd, Math.sin(aimAngle) * spd));
                    burstCount++;
                    burstCooldown = 10;
                }
                if (stateTimer >= 120) {
                    state = State.PHASING_OUT;
                    stateTimer = 0;
                }
                break;

            case PHASING_OUT:
                phaseAlpha = Math.max(0, 1.0 - stateTimer / 40.0);
                glitchOffset = (1.0 - phaseAlpha) * 20 * Math.cos(stateTimer * 1.2);
                if (stateTimer >= 40) {
                    state = State.TELEPORT;
                    stateTimer = 0;
                }
                break;

            case TELEPORT:
                // Teleport to new position
                x = 100 + Math.random() * (worldW - 200);
                y = 60 + Math.random() * (worldH * 0.4);
                state = State.PHASING_IN;
                stateTimer = 0;
                phaseAlpha = 0;
                burstCount = 0;
                break;
        }
    }

    public void takeDamage(double dmg) {
        // Can only be damaged when mostly solid
        if (phaseAlpha < 0.4) return;
        health -= dmg * phaseAlpha; // less damage when partially phased
        lastHitTime = System.currentTimeMillis();
        if (health <= 0) alive = false;
    }

    public boolean isAlive() { return alive; }
    public double getX() { return x; }
    public double getY() { return y; }
    public int getRadius() { return radius; }
    public double getHealth() { return health; }
    public double getMaxHealth() { return maxHealth; }
    public long getLastHitTime() { return lastHitTime; }
    public double getPhaseAlpha() { return phaseAlpha; }

    public void draw(Graphics2D g2) {
        if (phaseAlpha <= 0.01) return;

        Composite oldComp = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float)Math.min(1, phaseAlpha)));

        double gx = x + glitchOffset;
        double gy = y;

        // Ghostly outer aura
        if (phaseAlpha > 0.3) {
            float[] dist = {0f, 0.5f, 1.0f};
            Color[] colors = {
                new Color(160, 80, 255, (int)(80 * phaseAlpha)),
                new Color(100, 40, 200, (int)(30 * phaseAlpha)),
                new Color(0, 0, 0, 0)
            };
            RadialGradientPaint aura = new RadialGradientPaint(
                new Point2D.Double(gx, gy), radius * 2.0f, dist, colors);
            g2.setPaint(aura);
            g2.fillOval((int)(gx - radius * 2), (int)(gy - radius * 2), radius * 4, radius * 4);
        }

        // Main body — diamond/rhombus shape
        int[] xPts = {(int)gx, (int)(gx + radius), (int)gx, (int)(gx - radius)};
        int[] yPts = {(int)(gy - radius * 1.2), (int)gy, (int)(gy + radius * 1.2), (int)gy};
        Polygon body = new Polygon(xPts, yPts, 4);

        // Fill based on state
        Color bodyColor;
        if (state == State.SOLID) {
            bodyColor = new Color(140, 60, 220);
        } else {
            // Glitchy color shift during phasing
            int rShift = (int)(Math.sin(stateTimer * 0.5) * 40);
            bodyColor = new Color(
                Math.max(0, Math.min(255, 140 + rShift)),
                Math.max(0, Math.min(255, 60 - rShift / 2)),
                220);
        }
        g2.setColor(bodyColor);
        g2.fillPolygon(body);

        // Edge glow
        g2.setStroke(new BasicStroke(2.0f));
        g2.setColor(new Color(200, 140, 255, (int)(200 * phaseAlpha)));
        g2.drawPolygon(body);

        // Inner eye / core
        int coreR = radius / 4;
        g2.setColor(new Color(255, 200, 255, (int)(220 * phaseAlpha)));
        g2.fillOval((int)(gx - coreR), (int)(gy - coreR), coreR * 2, coreR * 2);

        // Glitch scan lines when phasing
        if (phaseAlpha < 0.9) {
            g2.setStroke(new BasicStroke(1.0f));
            int numLines = 6;
            for (int i = 0; i < numLines; i++) {
                int ly = (int)(gy - radius + (i / (double)numLines) * radius * 2);
                int lx1 = (int)(gx - radius + Math.random() * 10);
                int lx2 = (int)(gx + radius - Math.random() * 10);
                g2.setColor(new Color(200, 140, 255, (int)(60 * (1.0 - phaseAlpha))));
                g2.drawLine(lx1, ly, lx2, ly);
            }
        }

        // Aim line when solid
        if (state == State.SOLID) {
            g2.setStroke(new BasicStroke(1.5f));
            g2.setColor(new Color(255, 100, 200, 80));
            int lineLen = 50;
            g2.drawLine((int)gx, (int)gy,
                (int)(gx + Math.cos(aimAngle) * lineLen),
                (int)(gy + Math.sin(aimAngle) * lineLen));
        }

        g2.setComposite(oldComp);
        g2.setStroke(new BasicStroke(1.0f));
    }
}
