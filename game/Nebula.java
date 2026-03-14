package game;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.Random;

/**
 * A drifting nebula cloud — beautiful layered translucent gas.
 * Affects gameplay: player moves slower inside, enemies are partially obscured.
 */
public class Nebula {
    private double x, y;
    private double vx, vy;
    private int radius; // overall cloud radius
    private boolean alive = true;
    private int age = 0;

    // Sub-blobs that compose the nebula
    private final int blobCount;
    private final double[] blobOffX, blobOffY, blobR;
    private final Color[] blobColors;
    private final double[] blobPhase; // animation phase offsets

    private static final Random rng = new Random();

    // Color palettes for nebula types
    private static final Color[][] PALETTES = {
        // Purple/magenta
        {new Color(120, 20, 180), new Color(180, 40, 140), new Color(80, 10, 120), new Color(200, 80, 200)},
        // Teal/cyan
        {new Color(20, 100, 140), new Color(40, 180, 180), new Color(10, 80, 100), new Color(60, 200, 160)},
        // Ember/gold
        {new Color(160, 80, 20), new Color(200, 120, 40), new Color(120, 40, 10), new Color(220, 160, 60)},
        // Deep blue
        {new Color(20, 40, 160), new Color(60, 80, 200), new Color(10, 20, 120), new Color(80, 120, 220)},
    };

    public Nebula(double x, double y, int radius, double vx, double vy) {
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.vx = vx;
        this.vy = vy;

        // Generate sub-blobs
        blobCount = 5 + rng.nextInt(4);
        blobOffX = new double[blobCount];
        blobOffY = new double[blobCount];
        blobR = new double[blobCount];
        blobColors = new Color[blobCount];
        blobPhase = new double[blobCount];

        Color[] palette = PALETTES[rng.nextInt(PALETTES.length)];

        for (int i = 0; i < blobCount; i++) {
            double angle = rng.nextDouble() * Math.PI * 2;
            double dist = rng.nextDouble() * radius * 0.6;
            blobOffX[i] = Math.cos(angle) * dist;
            blobOffY[i] = Math.sin(angle) * dist;
            blobR[i] = radius * (0.3 + rng.nextDouble() * 0.5);
            blobColors[i] = palette[rng.nextInt(palette.length)];
            blobPhase[i] = rng.nextDouble() * Math.PI * 2;
        }
    }

    public void update(int width, int height) {
        age++;
        x += vx;
        y += vy;

        // Remove when fully off-screen
        if (x < -radius * 2 || x > width + radius * 2 ||
            y < -radius * 2 || y > height + radius * 2) {
            alive = false;
        }
    }

    public void draw(Graphics2D g2) {
        if (!alive) return;

        Object oldAA = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        Paint oldPaint = g2.getPaint();
        Composite oldComp = g2.getComposite();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        double breathe = 0.95 + 0.05 * Math.sin(age * 0.015);

        for (int i = 0; i < blobCount; i++) {
            double wobble = Math.sin(age * 0.02 + blobPhase[i]) * 4;
            double bx = x + blobOffX[i] + wobble;
            double by = y + blobOffY[i] + Math.cos(age * 0.018 + blobPhase[i]) * 3;
            float br = (float)(blobR[i] * breathe);

            if (br < 2) continue;

            Color c = blobColors[i];
            int alpha = 35 + (int)(15 * Math.sin(age * 0.03 + blobPhase[i]));

            try {
                RadialGradientPaint rgp = new RadialGradientPaint(
                    new Point2D.Double(bx, by), br,
                    new float[]{0f, 0.4f, 0.7f, 1f},
                    new Color[]{
                        new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha),
                        new Color(c.getRed(), c.getGreen(), c.getBlue(), (int)(alpha * 0.7)),
                        new Color(c.getRed(), c.getGreen(), c.getBlue(), (int)(alpha * 0.3)),
                        new Color(c.getRed(), c.getGreen(), c.getBlue(), 0)
                    }
                );
                g2.setPaint(rgp);
                g2.fillOval((int)(bx - br), (int)(by - br), (int)(br * 2), (int)(br * 2));
            } catch (Throwable t) {
                // fallback
                g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha / 2));
                g2.fillOval((int)(bx - br), (int)(by - br), (int)(br * 2), (int)(br * 2));
            }
        }

        // Subtle sparkle points (tiny bright stars within the cloud)
        if (age % 3 == 0) {
            int sparkles = 2 + rng.nextInt(3);
            for (int i = 0; i < sparkles; i++) {
                double sx = x + (rng.nextDouble() - 0.5) * radius * 1.2;
                double sy = y + (rng.nextDouble() - 0.5) * radius * 1.2;
                int sa = 80 + rng.nextInt(100);
                g2.setColor(new Color(255, 255, 255, sa));
                g2.fillOval((int)sx, (int)sy, 2, 2);
            }
        }

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAA);
        g2.setPaint(oldPaint);
        g2.setComposite(oldComp);
    }

    /** Check if a point is inside this nebula's influence radius. */
    public boolean contains(double px, double py) {
        double dx = px - x, dy = py - y;
        return dx * dx + dy * dy <= (double) radius * radius;
    }

    public boolean isAlive() { return alive; }
    public double getX() { return x; }
    public double getY() { return y; }
    public int getRadius() { return radius; }
}
