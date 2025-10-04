package game;

import java.awt.*;
import java.util.ArrayList;

public class Star {
    private final int worldW, worldH;
    double speed, x, y, size;       // parallax factor (0.25 .. 1.0)
    Color core, glow;
    double vx = 0, vy = 0;
    boolean orbiting = false;
    double orbitAngle = 0;
    BlackHole orbitTarget = null;

    public Star(int width, int height, double speed) {
        this.worldW = width;
        this.worldH = height;

        final int margin = 20;
        this.x = (Math.random() * (width  + margin * 2)) - margin;
        this.y = (Math.random() * (height + margin * 2)) - margin;
        this.speed = speed;

        // star size based on speed (closer = bigger/brighter)
        this.size = (1 + speed * 2);

        switch ((int)(Math.random() * 9)) {
            case 0: case 5: core = Color.CYAN;    glow = new Color(0, 255, 255, 120); break;
            case 1: case 6: core = Color.MAGENTA; glow = new Color(255, 0, 255, 120); break;
            case 2:         core = Color.YELLOW;  glow = new Color(255, 255, 100, 120); break;
            case 3: case 7: case 8: /* fallthrough */ core = Color.WHITE; glow = new Color(255, 255, 255, 120); break;
            case 4:         core = Color.PINK;    glow = new Color(255, 255, 255, 120); break;
            default:        core = Color.GREEN;   glow = new Color(100, 255, 100, 120); break;
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
    public void update(int width, int height, double playerVX, double playerVY, int coneDeg) {
        // Parallax: opposite player velocity + "draft" toward aim
        x -= Math.round(playerVX * speed * 0.25);
        y -= Math.round(playerVY * speed * 0.25);

        // draft: opposite of player aim (aim points arrow tip; stars drift "past" you)
        double angleRad = Math.toRadians(coneDeg);
        double dx = Math.cos(angleRad);
        double dy = Math.sin(angleRad);
        double draftStrength = 5;
        x -= dx * speed * draftStrength;
        y -= dy * speed * draftStrength;

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

        // glow (bigger, semi-transparent)
        g2.setColor(glow);
        g2.fillOval(
            (int)Math.round(rx - size),
            (int)Math.round(ry - size),
            (int)Math.round(size * 2),
            (int)Math.round(size * 2)
        );

        // core (smaller, solid)
        g2.setColor(core);
        int coreSize = (int)Math.max(1, Math.round(size * 0.6)); // renamed from 'core' to avoid shadowing
        g2.fillOval(
            (int)Math.round(rx - coreSize / 2.0),
            (int)Math.round(ry - coreSize / 2.0),
            coreSize, coreSize
        );
    }
}
