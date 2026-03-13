package game;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.RadialGradientPaint;
import java.awt.geom.AffineTransform;

/**
 * A slow, heavy enemy that parks at a position and fires a continuous
 * devastating laser beam aimed at the player.  The laser charges up,
 * fires for a duration, then cools down before repeating.
 *
 * Visuals: multi-layered beam with electric core, plasma sheath,
 * particle sparks, and a charging orb at the muzzle.
 */
public class LaserEnemy {
    private double x, y;
    private double targetX, targetY;
    private final int radius = 28;
    private double health;
    private final double maxHealth;
    private boolean alive = true;
    private long lastHitTime = 0;

    // Movement: drifts slowly to target position
    private double moveSpeed = 0.4;

    // Laser state machine
    public enum LaserState { ENTERING, CHARGING, FIRING, COOLDOWN }
    private LaserState state = LaserState.ENTERING;
    private int stateTimer = 0;

    // Timing (frames at 60fps)
    private static final int CHARGE_TIME = 120;   // 2 sec charge
    private static final int FIRE_TIME = 180;      // 3 sec beam
    private static final int COOLDOWN_TIME = 150;  // 2.5 sec cooldown

    // Laser aim
    private double laserAngle = Math.PI / 2; // starts pointing down
    private double laserTargetAngle = Math.PI / 2;
    private static final double AIM_SPEED = 0.012; // radians/frame, slow tracking

    // Beam geometry
    public static final double BEAM_RANGE = 1400;
    private static final int BEAM_WIDTH_CORE = 4;
    private static final int BEAM_WIDTH_INNER = 12;
    private static final int BEAM_WIDTH_OUTER = 28;

    // Animation tick
    private double animTick = 0;

    public LaserEnemy(double x, double y, double health) {
        this.x = x;
        this.y = y;
        this.targetX = x;
        this.targetY = y;
        this.health = health;
        this.maxHealth = health;
    }

    public void setTargetPosition(double tx, double ty) {
        this.targetX = tx;
        this.targetY = ty;
    }

    public void update(double playerX, double playerY) {
        if (!alive) return;
        animTick++;

        // Move toward target position
        double ddx = targetX - x;
        double ddy = targetY - y;
        double dist = Math.hypot(ddx, ddy);
        if (dist > 2) {
            x += (ddx / dist) * moveSpeed;
            y += (ddy / dist) * moveSpeed;
        }

        // Aim laser toward player (slowly)
        laserTargetAngle = Math.atan2(playerY - y, playerX - x);
        double angleDiff = laserTargetAngle - laserAngle;
        // Normalize to [-PI, PI]
        while (angleDiff > Math.PI) angleDiff -= 2 * Math.PI;
        while (angleDiff < -Math.PI) angleDiff += 2 * Math.PI;

        // State machine
        stateTimer++;
        switch (state) {
            case ENTERING:
                if (dist <= 2) {
                    state = LaserState.CHARGING;
                    stateTimer = 0;
                }
                // Track quickly while entering
                laserAngle += angleDiff * 0.05;
                break;

            case CHARGING:
                // Track player during charge (medium speed)
                laserAngle += Math.signum(angleDiff) * Math.min(Math.abs(angleDiff), AIM_SPEED * 1.5);
                if (stateTimer >= CHARGE_TIME) {
                    state = LaserState.FIRING;
                    stateTimer = 0;
                }
                break;

            case FIRING:
                // Very slow tracking while firing
                laserAngle += Math.signum(angleDiff) * Math.min(Math.abs(angleDiff), AIM_SPEED * 0.3);
                if (stateTimer >= FIRE_TIME) {
                    state = LaserState.COOLDOWN;
                    stateTimer = 0;
                }
                break;

            case COOLDOWN:
                // Resume normal tracking
                laserAngle += Math.signum(angleDiff) * Math.min(Math.abs(angleDiff), AIM_SPEED);
                if (stateTimer >= COOLDOWN_TIME) {
                    state = LaserState.CHARGING;
                    stateTimer = 0;
                }
                break;
        }
    }

    public void draw(Graphics2D g2) {
        if (!alive) return;

        Object oldAA = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        Paint oldPaint = g2.getPaint();
        Stroke oldStroke = g2.getStroke();
        Composite oldComp = g2.getComposite();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int dx = (int) Math.round(x);
        int dy = (int) Math.round(y);
        double t = animTick * 0.05;

        // ==================== DRAW LASER BEAM ====================
        if (state == LaserState.FIRING) {
            drawLaserBeam(g2, dx, dy, 1.0);
        } else if (state == LaserState.CHARGING) {
            double chargeRatio = stateTimer / (double) CHARGE_TIME;
            drawChargingEffect(g2, dx, dy, chargeRatio);
            // Pre-beam flicker in last 20% of charge
            if (chargeRatio > 0.8) {
                double flicker = (chargeRatio - 0.8) / 0.2;
                if (Math.random() < flicker * 0.6) {
                    drawLaserBeam(g2, dx, dy, flicker * 0.4);
                }
            }
        } else if (state == LaserState.COOLDOWN) {
            // Fading beam in first few frames
            if (stateTimer < 15) {
                double fade = 1.0 - stateTimer / 15.0;
                drawLaserBeam(g2, dx, dy, fade * 0.5);
            }
        }

        // ==================== DRAW BODY ====================
        drawBody(g2, dx, dy, t);

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAA);
        g2.setPaint(oldPaint);
        g2.setStroke(oldStroke);
        g2.setComposite(oldComp);
    }

    private void drawBody(Graphics2D g2, int dx, int dy, double t) {
        double pulse = 0.8 + 0.2 * Math.sin(t * 3);

        // Color based on state
        Color bodyColor, glowColor;
        switch (state) {
            case CHARGING:
                double cr = stateTimer / (double) CHARGE_TIME;
                int rr = (int)(180 + 75 * cr);
                bodyColor = new Color(rr, 40, (int)(180 - 100 * cr));
                glowColor = new Color(255, (int)(80 * cr), (int)(60 * cr));
                break;
            case FIRING:
                bodyColor = new Color(255, 60, 30);
                glowColor = new Color(255, 100, 50);
                break;
            default:
                bodyColor = new Color(160, 50, 180);
                glowColor = new Color(200, 80, 220);
        }

        // Outer aura
        for (int i = 4; i >= 1; i--) {
            int a = (int)(30 * pulse * (1.0 - i / 5.0));
            g2.setColor(new Color(glowColor.getRed(), glowColor.getGreen(), glowColor.getBlue(), a));
            int s = radius + i * 6;
            g2.fillOval(dx - s, dy - s, s * 2, s * 2);
        }

        // Heavy armored body - octagonal
        Polygon body = new Polygon();
        for (int i = 0; i < 8; i++) {
            double angle = i * Math.PI / 4 + Math.PI / 8;
            double r = radius * (0.9 + 0.1 * Math.cos(angle * 3 + t));
            body.addPoint(dx + (int)(r * Math.cos(angle)), dy + (int)(r * Math.sin(angle)));
        }

        GradientPaint bodyPaint = new GradientPaint(
            dx, dy - radius, bodyColor,
            dx, dy + radius, new Color(bodyColor.getRed() / 3, bodyColor.getGreen() / 3, bodyColor.getBlue() / 3));
        g2.setPaint(bodyPaint);
        g2.fillPolygon(body);

        // Body outline
        g2.setStroke(new BasicStroke(2.0f));
        g2.setColor(new Color(glowColor.getRed(), glowColor.getGreen(), glowColor.getBlue(), (int)(180 * pulse)));
        g2.drawPolygon(body);

        // Inner armor plates
        g2.setStroke(new BasicStroke(0.8f));
        for (int i = 0; i < 4; i++) {
            double a1 = i * Math.PI / 2 + Math.PI / 8;
            double a2 = a1 + Math.PI;
            g2.setColor(new Color(glowColor.getRed(), glowColor.getGreen(), glowColor.getBlue(), 50));
            g2.drawLine(dx + (int)(radius * 0.3 * Math.cos(a1)), dy + (int)(radius * 0.3 * Math.sin(a1)),
                        dx + (int)(radius * 0.3 * Math.cos(a2)), dy + (int)(radius * 0.3 * Math.sin(a2)));
        }

        // Central eye/emitter
        int emitterR = (int)(radius * 0.35);

        // Emitter dark disk
        g2.setColor(new Color(10, 5, 20));
        g2.fillOval(dx - emitterR, dy - emitterR, emitterR * 2, emitterR * 2);

        // Emitter inner glow
        Color emitColor = (state == LaserState.FIRING) ?
            new Color(255, 200, 100, (int)(240 * pulse)) :
            new Color(glowColor.getRed(), glowColor.getGreen(), glowColor.getBlue(), (int)(180 * pulse));
        try {
            RadialGradientPaint emitGlow = new RadialGradientPaint(
                new Point2D.Float(dx, dy), emitterR,
                new float[]{0f, 0.5f, 1f},
                new Color[]{emitColor,
                            new Color(emitColor.getRed(), emitColor.getGreen(), emitColor.getBlue(), emitColor.getAlpha() / 2),
                            new Color(emitColor.getRed(), emitColor.getGreen(), emitColor.getBlue(), 0)}
            );
            g2.setPaint(emitGlow);
            g2.fillOval(dx - emitterR, dy - emitterR, emitterR * 2, emitterR * 2);
        } catch (Exception e) {
            g2.setColor(emitColor);
            g2.fillOval(dx - emitterR, dy - emitterR, emitterR * 2, emitterR * 2);
        }

        // Hot center pip
        g2.setColor(new Color(255, 255, 255, (int)(200 * pulse)));
        g2.fillOval(dx - 3, dy - 3, 6, 6);

        // Muzzle direction indicator
        int muzzleLen = radius + 6;
        int mx = dx + (int)(Math.cos(laserAngle) * muzzleLen);
        int my = dy + (int)(Math.sin(laserAngle) * muzzleLen);
        g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(glowColor.getRed(), glowColor.getGreen(), glowColor.getBlue(), 100));
        g2.drawLine(dx, dy, mx, my);
        g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(255, 220, 200));
        g2.drawLine(dx, dy, mx, my);

        // Muzzle glow pip
        g2.setColor(new Color(255, 200, 150, (int)(200 * pulse)));
        g2.fillOval(mx - 4, my - 4, 8, 8);
    }

    private void drawChargingEffect(Graphics2D g2, int dx, int dy, double ratio) {
        // Charging orb at muzzle that grows
        int muzzleLen = radius + 8;
        int mx = dx + (int)(Math.cos(laserAngle) * muzzleLen);
        int my = dy + (int)(Math.sin(laserAngle) * muzzleLen);

        double orbR = 4 + 14 * ratio;
        double flicker = 0.7 + 0.3 * Math.sin(animTick * 0.3 * (1 + ratio * 3));

        // Outer charge glow
        for (int i = 3; i >= 1; i--) {
            int a = (int)(60 * ratio * flicker * (1.0 - i / 4.0));
            g2.setColor(new Color(255, (int)(100 * ratio), 50, a));
            int s = (int)(orbR + i * 6);
            g2.fillOval(mx - s, my - s, s * 2, s * 2);
        }

        // Charge orb core
        try {
            RadialGradientPaint orbPaint = new RadialGradientPaint(
                new Point2D.Float(mx, my), (float) Math.max(2, orbR),
                new float[]{0f, 0.4f, 1f},
                new Color[]{
                    new Color(255, 255, 220, (int)(255 * flicker)),
                    new Color(255, (int)(120 + 80 * ratio), 50, (int)(200 * flicker)),
                    new Color(255, 40, 20, 0)
                }
            );
            g2.setPaint(orbPaint);
        } catch (Exception e) {
            g2.setColor(new Color(255, 180, 80, (int)(200 * flicker)));
        }
        g2.fillOval((int)(mx - orbR), (int)(my - orbR), (int)(orbR * 2), (int)(orbR * 2));

        // Converging energy particles
        int numParticles = (int)(6 * ratio);
        for (int i = 0; i < numParticles; i++) {
            double pAngle = (animTick * 0.08 + i * Math.PI * 2 / numParticles);
            double pDist = 30 * (1 - ratio * 0.5) + 10 * Math.sin(animTick * 0.15 + i);
            int px = mx + (int)(Math.cos(pAngle) * pDist);
            int py = my + (int)(Math.sin(pAngle) * pDist);
            int pa = (int)(180 * ratio * flicker);
            g2.setColor(new Color(255, 200, 100, pa));
            g2.fillOval(px - 2, py - 2, 4, 4);
            // Trail toward muzzle
            g2.setColor(new Color(255, 150, 50, pa / 2));
            g2.setStroke(new BasicStroke(1.0f));
            g2.drawLine(px, py, mx, my);
        }

        // Aim line preview
        double previewLen = 200 * ratio;
        int ex = mx + (int)(Math.cos(laserAngle) * previewLen);
        int ey = my + (int)(Math.sin(laserAngle) * previewLen);
        g2.setStroke(new BasicStroke(1.0f));
        g2.setColor(new Color(255, 80, 40, (int)(60 * ratio * flicker)));
        g2.drawLine(mx, my, ex, ey);
    }

    private void drawLaserBeam(Graphics2D g2, int dx, int dy, double intensity) {
        int muzzleLen = radius + 8;
        int mx = dx + (int)(Math.cos(laserAngle) * muzzleLen);
        int my = dy + (int)(Math.sin(laserAngle) * muzzleLen);
        int ex = mx + (int)(Math.cos(laserAngle) * BEAM_RANGE);
        int ey = my + (int)(Math.sin(laserAngle) * BEAM_RANGE);

        double flicker = 0.85 + 0.15 * Math.sin(animTick * 0.5);
        double wave = Math.sin(animTick * 0.12);
        float alpha = (float)(intensity * flicker);

        AffineTransform oldT = g2.getTransform();

        // Draw beam layers from outside in
        // Layer 1: Outer plasma sheath (wide, translucent, oscillating)
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.min(1f, alpha * 0.3f)));
        int outerW = (int)(BEAM_WIDTH_OUTER + 6 * wave);
        g2.setStroke(new BasicStroke(outerW, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(255, 60, 20));
        g2.drawLine(mx, my, ex, ey);

        // Layer 2: Secondary glow
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.min(1f, alpha * 0.5f)));
        int midW = (int)(BEAM_WIDTH_INNER + 3 * wave);
        g2.setStroke(new BasicStroke(midW, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(255, 140, 40));
        g2.drawLine(mx, my, ex, ey);

        // Layer 3: Inner hot plasma
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.min(1f, alpha * 0.7f)));
        g2.setStroke(new BasicStroke(BEAM_WIDTH_CORE + 2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(255, 220, 120));
        g2.drawLine(mx, my, ex, ey);

        // Layer 4: White-hot core
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.min(1f, alpha * 0.9f)));
        g2.setStroke(new BasicStroke(BEAM_WIDTH_CORE, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(255, 255, 240));
        g2.drawLine(mx, my, ex, ey);

        // Layer 5: Electric crackles along the beam
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.min(1f, alpha * 0.6f)));
        g2.setStroke(new BasicStroke(1.0f));
        double beamLen = Math.hypot(ex - mx, ey - my);
        double perpX = -Math.sin(laserAngle);
        double perpY = Math.cos(laserAngle);

        for (int i = 0; i < 12; i++) {
            double tt = Math.random();
            double bx = mx + (ex - mx) * tt;
            double by = my + (ey - my) * tt;
            double forkLen = (8 + Math.random() * 16) * intensity;
            double side = (Math.random() > 0.5) ? 1 : -1;
            int fex = (int)(bx + perpX * forkLen * side);
            int fey = (int)(by + perpY * forkLen * side);
            int fa = (int)(200 * alpha * (0.5 + 0.5 * Math.random()));
            g2.setColor(new Color(255, 200, 100, Math.min(255, fa)));
            g2.drawLine((int) bx, (int) by, fex, fey);
            // Secondary fork
            if (Math.random() > 0.5) {
                double forkAngle = laserAngle + (Math.random() - 0.5) * Math.PI;
                int f2x = fex + (int)(8 * Math.cos(forkAngle));
                int f2y = fey + (int)(8 * Math.sin(forkAngle));
                g2.setColor(new Color(255, 240, 180, Math.min(255, fa / 2)));
                g2.drawLine(fex, fey, f2x, f2y);
            }
        }

        // Muzzle flash at beam origin
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.min(1f, alpha * 0.8f)));
        int flashR = (int)(16 + 8 * wave);
        try {
            RadialGradientPaint flashPaint = new RadialGradientPaint(
                new Point2D.Float(mx, my), Math.max(2f, flashR),
                new float[]{0f, 0.3f, 0.7f, 1f},
                new Color[]{
                    new Color(255, 255, 240, (int)(255 * alpha)),
                    new Color(255, 200, 80, (int)(200 * alpha)),
                    new Color(255, 80, 20, (int)(100 * alpha)),
                    new Color(255, 40, 10, 0)
                }
            );
            g2.setPaint(flashPaint);
        } catch (Exception e) {
            g2.setColor(new Color(255, 200, 100, (int)(200 * alpha)));
        }
        g2.fillOval(mx - flashR, my - flashR, flashR * 2, flashR * 2);

        // Reset composite
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
        g2.setTransform(oldT);
    }

    // =================== Collision helpers ===================

    /** Check if a point (px, py) is within the laser beam. */
    public boolean isBeamHitting(double px, double py, double targetRadius) {
        if (state != LaserState.FIRING) return false;

        int muzzleLen = radius + 8;
        double mx = x + Math.cos(laserAngle) * muzzleLen;
        double my = y + Math.sin(laserAngle) * muzzleLen;

        // Project point onto beam line segment
        double bx = Math.cos(laserAngle) * BEAM_RANGE;
        double by = Math.sin(laserAngle) * BEAM_RANGE;

        double dpx = px - mx;
        double dpy = py - my;
        double dot = dpx * bx + dpy * by;
        double lenSq = bx * bx + by * by;
        double t = dot / lenSq;

        if (t < 0 || t > 1) return false; // behind or past beam

        double closestX = mx + bx * t;
        double closestY = my + by * t;
        double dist = Math.hypot(px - closestX, py - closestY);

        return dist <= BEAM_WIDTH_INNER / 2.0 + targetRadius;
    }

    // =================== Standard accessors ===================

    public void takeDamage(double dmg) {
        health -= dmg;
        if (health <= 0) alive = false;
        lastHitTime = System.currentTimeMillis();
    }

    public boolean isAlive() { return alive; }
    public double getX() { return x; }
    public double getY() { return y; }
    public int getRadius() { return radius; }
    public double getHealth() { return health; }
    public double getMaxHealth() { return maxHealth; }
    public long getLastHitTime() { return lastHitTime; }
    public LaserState getState() { return state; }
    public double getLaserAngle() { return laserAngle; }
    public boolean isFiring() { return state == LaserState.FIRING; }
}
