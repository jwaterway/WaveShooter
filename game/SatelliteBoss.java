package game;

import java.awt.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.Random;

/**
 * Level 2 satellite boss — a large rotating structure with 4 destructible arms.
 * Each arm has its own health bar and fires independently.
 * The boss drifts closer to the play area each wave, becoming the final boss at wave 10.
 */
public class SatelliteBoss {

    // ── Arm data ──────────────────────────────────────────────
    static class Arm {
        double health, maxHealth;
        boolean alive = true;
        long lastHitTime = 0;

        // Arm fires independently
        int fireTimer;
        int fireInterval;

        Arm(double health, int fireInterval) {
            this.health = health;
            this.maxHealth = health;
            this.fireTimer = (int)(Math.random() * fireInterval);
            this.fireInterval = fireInterval;
        }

        void takeDamage(double amt) {
            if (!alive) return;
            health -= amt;
            lastHitTime = System.currentTimeMillis();
            if (health <= 0) { health = 0; alive = false; }
        }
    }

    // ── Fields ────────────────────────────────────────────────
    private double x, y;           // center position
    private double targetY;        // where it's drifting to
    private final int coreRadius = 50;
    private final int armLength = 140;
    private double rotation = 0;
    private double rotSpeed = 0.008;
    private double age = 0;
    private boolean alive = true;
    private final Random rng = new Random();

    // Core has its own health (exposed after all arms dead)
    private double coreHealth;
    private double coreMaxHealth;
    private long coreLastHitTime = 0;
    private boolean coreVulnerable = false;

    // 4 arms at 90° intervals
    final Arm[] arms = new Arm[4];

    // Missile firing from core
    private int missileTimer = 0;
    private int missileInterval = 240;  // 4 seconds

    // Laser firing
    private int laserTimer = 0;
    private int laserInterval = 360;  // 6 seconds
    private int laserFireDuration = 120;  // 2 seconds
    private double laserAngle = 0;
    private boolean laserFiring = false;
    private int laserChargeTimer = 0;
    private static final int LASER_CHARGE_TIME = 60;

    // Visual animation
    private double pulsePhase = 0;

    // ── Constructor ──────────────────────────────────────────
    public SatelliteBoss(double x, double y, double armHealth, double coreHealth) {
        this.x = x;
        this.y = y;
        this.targetY = y;
        this.coreHealth = coreHealth;
        this.coreMaxHealth = coreHealth;

        for (int i = 0; i < 4; i++) {
            arms[i] = new Arm(armHealth, 150 + (int)(Math.random() * 60));
        }
    }

    // ── Public API ────────────────────────────────────────────
    public boolean isAlive() { return alive; }
    public double getX() { return x; }
    public double getY() { return y; }
    public int getArmLength() { return armLength; }
    public boolean isCoreVulnerable() { return coreVulnerable; }

    /** Set the Y position the boss drifts toward. */
    public void setTargetY(double ty) { this.targetY = ty; }

    /** Damage all living arms (for grenade). */
    public void damageAllArms(double dmg) {
        for (Arm arm : arms) arm.takeDamage(dmg);
        if (coreVulnerable) {
            coreHealth -= dmg;
            coreLastHitTime = System.currentTimeMillis();
            if (coreHealth <= 0) { coreHealth = 0; alive = false; }
        }
        checkCoreVulnerable();
    }

    /** Try to damage at a specific point. Returns true if hit. */
    public boolean damageAt(double hx, double hy, double dmg) {
        // Check core (only if vulnerable)
        if (coreVulnerable) {
            double d = Math.hypot(hx - x, hy - y);
            if (d < coreRadius) {
                coreHealth -= dmg;
                coreLastHitTime = System.currentTimeMillis();
                if (coreHealth <= 0) { coreHealth = 0; alive = false; }
                return true;
            }
        }

        // Check each arm segment
        for (int i = 0; i < 4; i++) {
            if (!arms[i].alive) continue;
            double armAngle = rotation + i * Math.PI / 2;

            // Check along the arm length
            for (double t = 0.2; t <= 1.0; t += 0.15) {
                double ax = x + Math.cos(armAngle) * armLength * t;
                double ay = y + Math.sin(armAngle) * armLength * t;
                double d = Math.hypot(hx - ax, hy - ay);
                if (d < 28) {
                    arms[i].takeDamage(dmg);
                    checkCoreVulnerable();
                    return true;
                }
            }
        }
        return false;
    }

    private void checkCoreVulnerable() {
        boolean anyAlive = false;
        for (Arm a : arms) if (a.alive) anyAlive = true;
        coreVulnerable = !anyAlive;
    }

    /** Check if player is in laser beam path. */
    public boolean isInLaserBeam(double px, double py, double playerRadius) {
        if (!laserFiring || !alive) return false;
        // Line-vs-circle distance check
        double lx = Math.cos(laserAngle);
        double ly = Math.sin(laserAngle);
        double dx = px - x;
        double dy = py - y;
        double proj = dx * lx + dy * ly;
        if (proj < coreRadius || proj > 1400) return false;
        double perpDist = Math.abs(dx * ly - dy * lx);
        return perpDist < playerRadius + 14;
    }

    // ── Update ────────────────────────────────────────────────
    public void update(double playerX, double playerY,
                       ArrayList<HomingMissile> missiles,
                       ArrayList<EnemyShot> enemyShots) {
        if (!alive) return;
        age++;
        pulsePhase += 0.04;
        rotation += rotSpeed;

        // Drift toward target Y
        y += (targetY - y) * 0.005;

        // Gentle horizontal hover
        x += Math.sin(age * 0.01) * 0.3;

        // Missile firing from core
        missileTimer++;
        if (missileTimer >= missileInterval) {
            missileTimer = 0;
            missiles.add(new HomingMissile(x, y, playerX, playerY));
            // Fire from opposite side too if arms dead
            if (coreVulnerable) {
                missiles.add(new HomingMissile(x, y + 30, playerX, playerY));
            }
        }

        // Arm firing (each arm fires independently)
        for (int i = 0; i < 4; i++) {
            if (!arms[i].alive) continue;
            arms[i].fireTimer++;
            if (arms[i].fireTimer >= arms[i].fireInterval) {
                arms[i].fireTimer = 0;
                double armAngle = rotation + i * Math.PI / 2;
                double tipX = x + Math.cos(armAngle) * armLength;
                double tipY = y + Math.sin(armAngle) * armLength;
                double aimAngle = Math.atan2(playerY - tipY, playerX - tipX);
                enemyShots.add(new EnemyShot(tipX, tipY, aimAngle));
            }
        }

        // Laser firing cycle
        laserTimer++;
        if (laserFiring) {
            // Track player slowly during firing
            double targetAngle = Math.atan2(playerY - y, playerX - x);
            double diff = targetAngle - laserAngle;
            while (diff > Math.PI) diff -= 2 * Math.PI;
            while (diff < -Math.PI) diff += 2 * Math.PI;
            laserAngle += diff * 0.015;

            if (laserTimer >= laserFireDuration) {
                laserFiring = false;
                laserTimer = 0;
                laserChargeTimer = 0;
            }
        } else {
            if (laserTimer >= laserInterval) {
                laserChargeTimer++;
                // Aim toward player during charge
                laserAngle = Math.atan2(playerY - y, playerX - x);
                if (laserChargeTimer >= LASER_CHARGE_TIME) {
                    laserFiring = true;
                    laserTimer = 0;
                }
            }
        }
    }

    // ── Draw ──────────────────────────────────────────────────
    public void draw(Graphics2D g2) {
        if (!alive) return;

        Composite oldComp = g2.getComposite();
        Stroke oldStroke = g2.getStroke();

        // Draw arms
        for (int i = 0; i < 4; i++) {
            drawArm(g2, i);
        }

        // Draw laser beam
        if (laserFiring) {
            drawLaserBeam(g2);
        } else if (laserChargeTimer > 0) {
            drawLaserCharge(g2);
        }

        // Draw core
        drawCore(g2);

        // Draw arm health bars
        for (int i = 0; i < 4; i++) {
            if (arms[i].alive) {
                drawArmHealthBar(g2, i);
            }
        }

        // Draw core health bar when vulnerable
        if (coreVulnerable && alive) {
            drawCoreHealthBar(g2);
        }

        g2.setComposite(oldComp);
        g2.setStroke(oldStroke);
    }

    private void drawArm(Graphics2D g2, int idx) {
        Arm arm = arms[idx];
        double armAngle = rotation + idx * Math.PI / 2;
        double tipX = x + Math.cos(armAngle) * armLength;
        double tipY = y + Math.sin(armAngle) * armLength;

        float alpha = arm.alive ? 1f : 0.15f;

        // Arm structure — tapered beam
        Stroke old = g2.getStroke();

        if (arm.alive) {
            // Outer glow
            g2.setStroke(new BasicStroke(16f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(new Color(100, 160, 255, (int)(30 * alpha)));
            g2.drawLine((int)x, (int)y, (int)tipX, (int)tipY);
        }

        // Main arm beam
        g2.setStroke(new BasicStroke(8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Color armColor = arm.alive ? new Color(60, 140, 220, 200) : new Color(40, 50, 60, 60);
        g2.setColor(armColor);
        g2.drawLine((int)x, (int)y, (int)tipX, (int)tipY);

        // Inner bright line
        g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(arm.alive ? new Color(140, 200, 255, 180) : new Color(60, 70, 80, 40));
        g2.drawLine((int)x, (int)y, (int)tipX, (int)tipY);

        // Tip node (like satellite panels)
        if (arm.alive) {
            float pulse = (float)(0.7 + 0.3 * Math.sin(pulsePhase + idx * 1.5));
            int nodeR = 18;
            // Panel shape — rotated rectangle at tip
            Graphics2D g2c = (Graphics2D)g2.create();
            g2c.translate(tipX, tipY);
            g2c.rotate(armAngle + age * 0.02);

            // Solar panel rectangles
            g2c.setColor(new Color(40, 80, 160, (int)(180 * pulse)));
            g2c.fillRect(-nodeR, -6, nodeR * 2, 12);
            g2c.setColor(new Color(80, 160, 255, (int)(120 * pulse)));
            g2c.drawRect(-nodeR, -6, nodeR * 2, 12);

            // Grid lines on panel
            g2c.setStroke(new BasicStroke(0.5f));
            g2c.setColor(new Color(120, 200, 255, (int)(60 * pulse)));
            for (int li = -nodeR + 4; li < nodeR; li += 4) {
                g2c.drawLine(li, -6, li, 6);
            }
            g2c.drawLine(-nodeR, 0, nodeR, 0);

            g2c.dispose();

            // Tip glow
            RadialGradientPaint glow = new RadialGradientPaint(
                new Point2D.Double(tipX, tipY), nodeR + 8,
                new float[]{0f, 0.5f, 1f},
                new Color[]{
                    new Color(100, 180, 255, (int)(60 * pulse)),
                    new Color(60, 120, 255, (int)(20 * pulse)),
                    new Color(0, 0, 0, 0)
                }
            );
            g2.setPaint(glow);
            g2.fillOval((int)(tipX - nodeR - 8), (int)(tipY - nodeR - 8),
                        (nodeR + 8) * 2, (nodeR + 8) * 2);
        } else {
            // Destroyed arm tip — sparking debris
            if (age % 8 < 4) {
                g2.setColor(new Color(255, 160, 40, 60));
                g2.fillOval((int)(tipX - 4), (int)(tipY - 4), 8, 8);
            }
        }

        g2.setStroke(old);
    }

    private void drawCore(Graphics2D g2) {
        float pulse = (float)(0.7 + 0.3 * Math.sin(pulsePhase));

        // Outer energy ring
        Stroke old = g2.getStroke();
        g2.setStroke(new BasicStroke(2f));
        float ringR = coreRadius + 10 + pulse * 5;
        g2.setColor(new Color(coreVulnerable ? 255 : 80,
                              coreVulnerable ? 60 : 200,
                              coreVulnerable ? 60 : 255,
                              (int)(60 * pulse)));
        g2.drawOval((int)(x - ringR), (int)(y - ringR), (int)(ringR * 2), (int)(ringR * 2));
        g2.setStroke(old);

        // Core gradient sphere
        Color coreColor = coreVulnerable
            ? new Color(255, 80, 60)    // angry red when vulnerable
            : new Color(60, 140, 255);  // calm blue normally

        RadialGradientPaint coreGrad = new RadialGradientPaint(
            new Point2D.Double(x - coreRadius * 0.2, y - coreRadius * 0.2),
            coreRadius,
            new float[]{0f, 0.5f, 0.85f, 1f},
            new Color[]{
                new Color(220, 240, 255, 255),
                new Color(coreColor.getRed(), coreColor.getGreen(), coreColor.getBlue(), 200),
                new Color(coreColor.getRed() / 2, coreColor.getGreen() / 2, coreColor.getBlue() / 2, 160),
                new Color(10, 10, 30, 140)
            }
        );
        g2.setPaint(coreGrad);
        g2.fillOval((int)(x - coreRadius), (int)(y - coreRadius),
                    coreRadius * 2, coreRadius * 2);

        // Rotating hex pattern on core
        old = g2.getStroke();
        g2.setStroke(new BasicStroke(1f));
        g2.setColor(new Color(180, 220, 255, (int)(40 * pulse)));
        for (int i = 0; i < 6; i++) {
            double a1 = rotation * 2 + i * Math.PI / 3;
            double a2 = a1 + Math.PI / 3;
            int x1 = (int)(x + Math.cos(a1) * coreRadius * 0.7);
            int y1 = (int)(y + Math.sin(a1) * coreRadius * 0.7);
            int x2 = (int)(x + Math.cos(a2) * coreRadius * 0.7);
            int y2 = (int)(y + Math.sin(a2) * coreRadius * 0.7);
            g2.drawLine(x1, y1, x2, y2);
        }
        g2.setStroke(old);
    }

    private void drawLaserBeam(Graphics2D g2) {
        double beamLen = 1400;
        double ex = x + Math.cos(laserAngle) * beamLen;
        double ey = y + Math.sin(laserAngle) * beamLen;
        Stroke old = g2.getStroke();

        // Outer glow
        g2.setStroke(new BasicStroke(28f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        float flicker = (float)(0.5 + 0.5 * Math.sin(age * 0.5));
        g2.setColor(new Color(255, 40, 40, (int)(30 * flicker)));
        g2.drawLine((int)x, (int)y, (int)ex, (int)ey);

        // Inner beam
        g2.setStroke(new BasicStroke(12f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(255, 100, 60, (int)(120 * flicker)));
        g2.drawLine((int)x, (int)y, (int)ex, (int)ey);

        // Core
        g2.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(255, 220, 200, 200));
        g2.drawLine((int)x, (int)y, (int)ex, (int)ey);

        g2.setStroke(old);
    }

    private void drawLaserCharge(Graphics2D g2) {
        float t = laserChargeTimer / (float)LASER_CHARGE_TIME;
        float chargeR = 15 + t * 20;
        int alpha = (int)(180 * t);

        // Charge orb at core
        RadialGradientPaint chg = new RadialGradientPaint(
            new Point2D.Double(x, y), chargeR,
            new float[]{0f, 0.6f, 1f},
            new Color[]{
                new Color(255, 100, 60, alpha),
                new Color(255, 40, 20, alpha / 2),
                new Color(0, 0, 0, 0)
            }
        );
        g2.setPaint(chg);
        g2.fillOval((int)(x - chargeR), (int)(y - chargeR),
                    (int)(chargeR * 2), (int)(chargeR * 2));

        // Aim line (dotted)
        Stroke old = g2.getStroke();
        float[] dash = {6f, 10f};
        g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND, 0, dash, (float)(age * 0.5)));
        g2.setColor(new Color(255, 80, 40, (int)(100 * t)));
        double aimX = x + Math.cos(laserAngle) * 400;
        double aimY = y + Math.sin(laserAngle) * 400;
        g2.drawLine((int)x, (int)y, (int)aimX, (int)aimY);
        g2.setStroke(old);
    }

    private void drawArmHealthBar(Graphics2D g2, int idx) {
        Arm arm = arms[idx];
        long timeSinceHit = System.currentTimeMillis() - arm.lastHitTime;
        if (timeSinceHit > 2000) return;

        double armAngle = rotation + idx * Math.PI / 2;
        double barX = x + Math.cos(armAngle) * armLength * 0.6;
        double barY = y + Math.sin(armAngle) * armLength * 0.6 - 16;
        double ratio = arm.health / arm.maxHealth;
        double fadeAlpha = Math.max(0, Math.min(1.0, 1.0 - (timeSinceHit - 500.0) / 1500.0));

        int bw = 40, bh = 5;
        // Background
        g2.setColor(new Color(20, 20, 30, (int)(180 * fadeAlpha)));
        g2.fillRect((int)(barX - bw / 2), (int)barY, bw, bh);
        // Fill
        Color hpColor = ratio > 0.5 ? new Color(80, 200, 255) : new Color(255, 100, 60);
        g2.setColor(new Color(hpColor.getRed(), hpColor.getGreen(), hpColor.getBlue(),
                              (int)(200 * fadeAlpha)));
        g2.fillRect((int)(barX - bw / 2), (int)barY, (int)(bw * ratio), bh);
        // Border
        g2.setColor(new Color(120, 180, 255, (int)(100 * fadeAlpha)));
        g2.drawRect((int)(barX - bw / 2), (int)barY, bw, bh);
    }

    private void drawCoreHealthBar(Graphics2D g2) {
        long timeSinceHit = System.currentTimeMillis() - coreLastHitTime;
        double fadeAlpha = 1.0;
        if (timeSinceHit > 500 && timeSinceHit < 3000) {
            fadeAlpha = Math.max(0, 1.0 - (timeSinceHit - 500.0) / 2500.0);
        } else if (timeSinceHit >= 3000) {
            // Always show core health bar when vulnerable (pulsing)
            fadeAlpha = 0.4 + 0.3 * Math.sin(pulsePhase);
        }

        double ratio = coreHealth / coreMaxHealth;
        int bw = 80, bh = 8;
        double barX = x - bw / 2;
        double barY = y - coreRadius - 24;

        // Background
        g2.setColor(new Color(20, 10, 10, (int)(200 * fadeAlpha)));
        g2.fillRect((int)barX, (int)barY, bw, bh);
        // Fill — red pulse
        float pulse = (float)(0.7 + 0.3 * Math.sin(pulsePhase * 2));
        g2.setColor(new Color(255, (int)(60 * pulse), 40, (int)(230 * fadeAlpha)));
        g2.fillRect((int)barX, (int)barY, (int)(bw * ratio), bh);
        // Border
        g2.setColor(new Color(255, 100, 80, (int)(140 * fadeAlpha)));
        g2.drawRect((int)barX, (int)barY, bw, bh);
        // Label
        g2.setFont(new Font("Consolas", Font.BOLD, 10));
        g2.setColor(new Color(255, 120, 80, (int)(200 * fadeAlpha)));
        String label = "CORE";
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(label, (int)(x - fm.stringWidth(label) / 2), (int)barY - 4);
    }
}
