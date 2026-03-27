package game;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.ArrayList;

public class Star {
    private final int worldW, worldH;
    double speed, x, y, size;       // parallax factor (0.25 .. 1.0)
    Color core, glow;
    double vx = 0, vy = 0;
    boolean orbiting = false;
    double orbitAngle = 0;
    BlackHole orbitTarget = null;
    private double twinklePhase;
    private double twinkleSpeed;
    private double driftMultiplier = 1.0;
    private double lateralDrift = 0.0;  // horizontal drift (camera-driven)

    public void setDriftMultiplier(double m) { this.driftMultiplier = m; }
    public void setLateralDrift(double d) { this.lateralDrift = d; }

    public Star(int width, int height, double speed) {
        this.worldW = width;
        this.worldH = height;

        final int margin = 20;
        this.x = (Math.random() * (width  + margin * 2)) - margin;
        this.y = (Math.random() * (height + margin * 2)) - margin;
        this.speed = speed;

        // star size based on speed (closer = bigger/brighter)
        this.size = (0.5 + speed * 2.5);

        this.twinklePhase = Math.random() * Math.PI * 2;
        this.twinkleSpeed = 0.03 + Math.random() * 0.08;

        switch ((int)(Math.random() * 12)) {
            case 0: case 5: case 9:
                core = new Color(100, 200, 255);  glow = new Color(80, 180, 255, 100); break;  // cool blue
            case 1: case 6:
                core = new Color(255, 220, 180);  glow = new Color(255, 200, 150, 90); break;  // warm yellow
            case 2: case 10:
                core = Color.CYAN;    glow = new Color(0, 255, 255, 80); break;
            case 3: case 7: case 8: case 11:
                core = Color.WHITE;   glow = new Color(220, 230, 255, 100); break;
            case 4:
                core = new Color(255, 180, 200);  glow = new Color(255, 150, 180, 80); break;  // soft pink
            default:
                core = new Color(200, 220, 255);  glow = new Color(180, 200, 255, 90); break;  // pale blue
        }
    }

    public void reset() {
        final int margin = 20;
        this.x = (Math.random() * (worldW + margin * 2)) - margin;
        this.y = (Math.random() * (worldH + margin * 2)) - margin;

        this.vx = 0;//(Math.random() - 0.5) * speed; // gentle drift
        this.vy = 0;// (Math.random() - 0.5) * speed;

        this.orbiting = false;
        this.orbitTarget = null;
    }
    public void resetSafe(java.util.List<BlackHole> holes) {
        Point p = GamePanel.safeRandomPoint(worldW, worldH, holes);
        this.x = p.x;
        this.y = p.y;
        this.vx = (Math.random() - 0.5) * speed;
        this.vy = (Math.random() - 0.5) * speed;
        this.orbiting = false;
        this.orbitTarget = null;
    }
    public void update(int width, int height) {
        twinklePhase += twinkleSpeed;

        // Fixed drift: stars scroll downward at speed-based rate
        double driftSpeed = 2.5 * driftMultiplier;
        y += speed * driftSpeed;

        // Lateral drift: horizontal scrolling from camera
        x += lateralDrift * speed;

        // Wrap with margin
        final int margin = 20;
        final int spanX = width  + margin * 2;
        final int spanY = height + margin * 2;

        if (x < -margin)             { x += spanX; vx = 0;}
        else if (x > width + margin) { x -= spanX; vx = 0;}

        if (y < -margin)             { y += spanY; vy = 0;}
        else if (y > height + margin){ y -= spanY; vy = 0;}
    }

    // Black hole gravity/orbit
    public void updateWithBlackHoles(ArrayList<BlackHole> blackHoles) {
        if (orbiting && orbitTarget != null) {
            double r = orbitTarget.getRadius();
            double orbitSpeed = 0.0002 + (r * 0.00005);
            orbitAngle += orbitSpeed;

            // lock star to rim radius
            x = orbitTarget.getX() + Math.cos(orbitAngle) * r;
            y = orbitTarget.getY() + Math.sin(orbitAngle) * r;

            // small chance to respawn away from hole
            if (Math.random() < 0.002) resetSafe(blackHoles);
            return;
        }

        for (BlackHole bh : blackHoles) {
            double dxB = bh.getX() - x;
            double dyB = bh.getY() - y;
            double dist = Math.hypot(dxB, dyB);

            // early attraction
            if (dist < bh.getRadius() * 5) {
                double strength = (bh.getRadius() * 15) / (dist * dist + 1);
                vx += (dxB / dist) * strength;
                vy += (dyB / dist) * strength;
            }

            // capture into orbit
            if (dist <= bh.getRadius() * 1.02 && !orbiting) {
                orbiting = true;
                orbitTarget = bh;

                // start orbit at current angle
                orbitAngle = Math.atan2(dyB, dxB);

                // smoothly snap onto rim
                double targetR = bh.getRadius();
                double lerpFactor = 2;
                x = x + (bh.getX() + Math.cos(orbitAngle) * targetR - x) * lerpFactor;
                y = y + (bh.getY() + Math.sin(orbitAngle) * targetR - y) * lerpFactor;

                // spawn a ring that stops at BH radius
                GamePanel.rings.add(new ParticleRing(bh.getX(), bh.getY(), (int)Math.round(bh.getRadius())));

                // grow & flash
                bh.absorbStar();
                //bh.flash();

                return;
            }
        }

        // apply velocity when not orbiting
        x += vx;
        y += vy;
    }

    public void draw(Graphics2D g2, ArrayList<BlackHole> blackHoles) {
        double rx = x, ry = y;

        // gravitational lensing offset accumulation
        double[] tmp = new double[2];
        for (BlackHole bh : blackHoles) {
            bh.applyLensingOffset(rx, ry, tmp);
            rx = tmp[0]; ry = tmp[1];
        }

        double twinkle = 0.6 + 0.4 * Math.sin(twinklePhase);
        double drawSize = size * (0.8 + 0.2 * twinkle);

        // Outer glow
        int glowAlpha = (int)(glow.getAlpha() * twinkle);
        g2.setColor(new Color(glow.getRed(), glow.getGreen(), glow.getBlue(), Math.max(0, Math.min(255, glowAlpha))));
        int glowDiam = (int)Math.round(drawSize * 2.4);
        g2.fillOval(
            (int)Math.round(rx - glowDiam / 2.0),
            (int)Math.round(ry - glowDiam / 2.0),
            glowDiam, glowDiam
        );

        // Bright core
        int coreAlpha = (int)(180 + 75 * twinkle);
        g2.setColor(new Color(core.getRed(), core.getGreen(), core.getBlue(), Math.min(255, coreAlpha)));
        int coreDiam = (int)Math.max(1, Math.round(drawSize * 0.7));
        g2.fillOval(
            (int)Math.round(rx - coreDiam / 2.0),
            (int)Math.round(ry - coreDiam / 2.0),
            coreDiam, coreDiam
        );

        // Tiny white hot center for larger stars
        if (drawSize > 2.0) {
            int hotAlpha = (int)(200 * twinkle);
            g2.setColor(new Color(255, 255, 255, Math.min(255, hotAlpha)));
            g2.fillOval((int)Math.round(rx), (int)Math.round(ry), 1, 1);
        }
    }
}
