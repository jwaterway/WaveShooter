package game;

import java.awt.*;
import java.awt.geom.Point2D;

/**
 * Homing missile launched by a BossEnemy. Tracks the player with smooth turning.
 * Visually: glowing warhead with a flame trail.
 */
public class HomingMissile {
    private double x, y;
    private double vx, vy;
    private final double speed = 3.5;
    private final double turnRate = 0.04; // radians per frame
    private double angle;
    private boolean alive = true;
    private int age = 0;
    private final int maxAge = 600; // 10 seconds max lifetime
    private final int fizzStart = 510; // start fizzling 90 frames before death
    private boolean expired = false; // true if died from timeout (not killed)
    public static final int RADIUS = 6;

    // Trail particles stored inline (lightweight ring buffer)
    private final double[] trailX = new double[20];
    private final double[] trailY = new double[20];
    private int trailIdx = 0;

    public HomingMissile(double x, double y, double targetX, double targetY) {
        this.x = x;
        this.y = y;
        this.angle = Math.atan2(targetY - y, targetX - x);
        this.vx = Math.cos(angle) * speed;
        this.vy = Math.sin(angle) * speed;
        for (int i = 0; i < trailX.length; i++) {
            trailX[i] = x;
            trailY[i] = y;
        }
    }

    public void update(double targetX, double targetY) {
        age++;
        if (age > maxAge) { alive = false; expired = true; return; }

        // Store trail position
        trailX[trailIdx] = x;
        trailY[trailIdx] = y;
        trailIdx = (trailIdx + 1) % trailX.length;

        boolean fizzling = age >= fizzStart;
        double speedMult = 1.0;
        if (fizzling) {
            // Slow down progressively during fizz phase
            double fizzFrac = (double)(age - fizzStart) / (maxAge - fizzStart);
            speedMult = 1.0 - fizzFrac * 0.85; // slows to 15% speed
            // Random jitter
            x += (Math.random() - 0.5) * 3 * fizzFrac;
            y += (Math.random() - 0.5) * 3 * fizzFrac;
        }

        // Homing: smoothly turn toward target (weaker when fizzling)
        double desired = Math.atan2(targetY - y, targetX - x);
        double diff = desired - angle;
        // Normalize to -PI..PI
        while (diff > Math.PI)  diff -= 2 * Math.PI;
        while (diff < -Math.PI) diff += 2 * Math.PI;

        double effectiveTurnRate = fizzling ? turnRate * 0.3 : turnRate;
        if (diff > effectiveTurnRate) diff = effectiveTurnRate;
        else if (diff < -effectiveTurnRate) diff = -effectiveTurnRate;
        angle += diff;

        vx = Math.cos(angle) * speed * speedMult;
        vy = Math.sin(angle) * speed * speedMult;
        x += vx;
        y += vy;

        // Off-screen kill (generous bounds)
        if (x < -100 || x > 2100 || y < -100 || y > 1200) {
            alive = false;
        }
    }

    public boolean hasExpired() { return expired; }
    public boolean isFizzling() { return age >= fizzStart && alive; }

    public void draw(Graphics2D g2) {
        if (!alive) return;

        Object oldAA = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        Paint oldPaint = g2.getPaint();
        Stroke oldStroke = g2.getStroke();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        boolean fizzling = age >= fizzStart;
        double fizzFrac = fizzling ? (double)(age - fizzStart) / (maxAge - fizzStart) : 0;
        double pulse = 0.7 + 0.3 * Math.sin(age * (fizzling ? 1.5 : 0.3));

        // --- Trail (fading flame dots — flicker when fizzling) ---
        for (int i = 0; i < trailX.length; i++) {
            int idx = (trailIdx - 1 - i + trailX.length) % trailX.length;
            double frac = 1.0 - (double) i / trailX.length;
            int a = (int)(180 * frac * pulse);
            int sz = (int)(4 + 4 * frac);
            int r = 255;
            int gr = (int)(200 * frac);
            int b = (int)(50 * frac * frac);
            if (fizzling) {
                // Trail turns sputtery — white/yellow sparks
                if (Math.random() < fizzFrac * 0.5) {
                    r = 255; gr = 255; b = (int)(200 * Math.random());
                    sz = (int)(2 + 6 * Math.random());
                }
                a = (int)(a * (1.0 - fizzFrac * 0.5));
            }
            g2.setColor(new Color(r, gr, b, Math.max(0, Math.min(255, a))));
            g2.fillOval((int)(trailX[idx] - sz / 2), (int)(trailY[idx] - sz / 2), sz, sz);
        }

        // --- Fizz sparks (random sparks flying off when fizzling) ---
        if (fizzling) {
            int sparkCount = (int)(8 * fizzFrac);
            for (int i = 0; i < sparkCount; i++) {
                double sa = Math.random() * Math.PI * 2;
                double sd = 4 + Math.random() * 12 * fizzFrac;
                int sx = (int)(x + Math.cos(sa) * sd);
                int sy = (int)(y + Math.sin(sa) * sd);
                int sparkAlpha = (int)(200 * (1.0 - fizzFrac * 0.3) * Math.random());
                g2.setColor(new Color(255, (int)(200 * Math.random()), 50, Math.max(0, Math.min(255, sparkAlpha))));
                g2.fillOval(sx - 1, sy - 1, 3, 3);
            }
        }

        // --- Outer glow (flickers when fizzling) ---
        float glowR = RADIUS + 8;
        double glowPulse = fizzling ? pulse * (1.0 - fizzFrac * 0.6) : pulse;
        RadialGradientPaint glowPaint = new RadialGradientPaint(
            new Point2D.Double(x, y), glowR,
            new float[]{0f, 0.5f, 1f},
            new Color[]{
                new Color(255, 100, 40, (int)(120 * glowPulse)),
                new Color(255, 60, 20, (int)(60 * glowPulse)),
                new Color(255, 40, 0, 0)
            }
        );
        g2.setPaint(glowPaint);
        g2.fillOval((int)(x - glowR), (int)(y - glowR), (int)(glowR * 2), (int)(glowR * 2));

        // --- Warhead body (elongated in direction of travel) ---
        Graphics2D g2r = (Graphics2D) g2.create();
        g2r.translate(x, y);
        g2r.rotate(angle);
        // Body — dims when fizzling
        int bodyAlpha = fizzling ? (int)(255 * (1.0 - fizzFrac * 0.5)) : 255;
        g2r.setColor(new Color(200, 80, 40, bodyAlpha));
        g2r.fillOval(-RADIUS, -RADIUS / 2, RADIUS * 2, RADIUS);
        // Nose cone highlight
        g2r.setColor(new Color(255, 200, 100, (int)(200 * pulse * (fizzling ? 1.0 - fizzFrac * 0.4 : 1.0))));
        g2r.fillOval(RADIUS / 2, -RADIUS / 4, RADIUS, RADIUS / 2);
        // Hot core
        g2r.setColor(new Color(255, 255, 200, (int)(240 * pulse)));
        g2r.fillOval(-2, -2, 4, 4);
        g2r.dispose();

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAA);
        g2.setPaint(oldPaint);
        g2.setStroke(oldStroke);
    }

    public boolean isAlive() { return alive; }
    public void kill() { alive = false; }
    public double getX() { return x; }
    public double getY() { return y; }
    public int getRadius() { return RADIUS; }
    public int getAge() { return age; }
}
