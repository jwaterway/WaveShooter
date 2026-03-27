package game;

import java.awt.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.Random;

/**
 * Level 3 moon background — grey cratered surface below, dark starry sky above.
 * The surface scrolls slowly to give a sense of flight over the moon.
 */
public class MoonBackground {

    // Craters on the surface
    private static class Crater {
        double x, y, radius;
        double rimBrightness;
        Crater(double x, double y, double r, double b) {
            this.x = x; this.y = y; this.radius = r; this.rimBrightness = b;
        }
    }

    private final ArrayList<Crater> craters = new ArrayList<>();
    private final Random rng = new Random(42);  // fixed seed for consistency
    private double scrollOffset = 0;
    private final int surfaceY;   // where the surface starts (upper edge of terrain)
    private final int W, H;

    // Terrain height points for the jagged horizon
    private final int[] terrainHeights;
    private static final int TERRAIN_POINTS = 200;

    public MoonBackground(int width, int height) {
        this.W = width;
        this.H = height;
        this.surfaceY = (int)(H * 0.65);  // surface starts at 65% from top

        // Generate terrain profile (jagged moon horizon, wraps)
        terrainHeights = new int[TERRAIN_POINTS + 1];  // +1 for wrap
        for (int i = 0; i <= TERRAIN_POINTS; i++) {
            double base = surfaceY;
            // Multiple octaves of noise for natural looking terrain
            double h1 = Math.sin(i * 0.05) * 30;
            double h2 = Math.sin(i * 0.13 + 1.7) * 15;
            double h3 = Math.sin(i * 0.31 + 3.1) * 8;
            double h4 = (rng.nextDouble() - 0.5) * 6;
            terrainHeights[i] = (int)(base + h1 + h2 + h3 + h4);
        }

        // Generate craters across the surface (double width for scrolling)
        for (int i = 0; i < 60; i++) {
            double cx = rng.nextDouble() * W * 2;
            double cy = surfaceY + 40 + rng.nextDouble() * (H - surfaceY - 60);
            double cr = 8 + rng.nextDouble() * 45;
            double brightness = 0.3 + rng.nextDouble() * 0.5;
            craters.add(new Crater(cx, cy, cr, brightness));
        }
    }

    public void update() {
        scrollOffset += 0.3;  // slow horizontal scroll
        if (scrollOffset >= W) scrollOffset -= W;
    }

    public void draw(Graphics2D g2) {
        // Sky gradient — dark space to slightly lighter near horizon
        GradientPaint sky = new GradientPaint(
            0, 0, new Color(3, 3, 12),
            0, surfaceY, new Color(15, 15, 30)
        );
        g2.setPaint(sky);
        g2.fillRect(0, 0, W, surfaceY);

        // Earth in the sky (small, distant)
        drawEarth(g2, W - 200, 120, 50);

        // Moon surface gradient
        GradientPaint surface = new GradientPaint(
            0, surfaceY, new Color(90, 90, 85),
            0, H, new Color(50, 50, 45)
        );
        g2.setPaint(surface);

        // Draw terrain polygon
        Polygon terrain = new Polygon();
        terrain.addPoint(0, H);
        for (int i = 0; i < TERRAIN_POINTS; i++) {
            int px = (int)(i * (double)W / TERRAIN_POINTS);
            int scrollIdx = (int)((i + scrollOffset * TERRAIN_POINTS / W) % TERRAIN_POINTS);
            terrain.addPoint(px, terrainHeights[scrollIdx]);
        }
        terrain.addPoint(W, terrainHeights[(int)((TERRAIN_POINTS - 1 + scrollOffset * TERRAIN_POINTS / W) % TERRAIN_POINTS)]);
        terrain.addPoint(W, H);
        g2.fillPolygon(terrain);

        // Horizon glow line
        Stroke old = g2.getStroke();
        g2.setStroke(new BasicStroke(1.5f));
        g2.setColor(new Color(140, 140, 130, 80));
        for (int i = 1; i < TERRAIN_POINTS; i++) {
            int px1 = (int)((i - 1) * (double)W / TERRAIN_POINTS);
            int px2 = (int)(i * (double)W / TERRAIN_POINTS);
            int si1 = (int)((i - 1 + scrollOffset * TERRAIN_POINTS / W) % TERRAIN_POINTS);
            int si2 = (int)((i + scrollOffset * TERRAIN_POINTS / W) % TERRAIN_POINTS);
            g2.drawLine(px1, terrainHeights[si1], px2, terrainHeights[si2]);
        }
        g2.setStroke(old);

        // Draw craters
        for (Crater c : craters) {
            double drawX = c.x - scrollOffset;
            // Wrap
            if (drawX < -c.radius * 2) drawX += W * 2;
            if (drawX > W + c.radius * 2) continue;
            if (c.y < surfaceY) continue;

            drawCrater(g2, drawX, c.y, c.radius, c.rimBrightness);
        }

        // Subtle dust haze near the surface
        GradientPaint haze = new GradientPaint(
            0, surfaceY - 20, new Color(80, 80, 75, 0),
            0, surfaceY + 40, new Color(80, 80, 75, 30)
        );
        g2.setPaint(haze);
        g2.fillRect(0, surfaceY - 20, W, 60);
    }

    private void drawCrater(Graphics2D g2, double cx, double cy, double r, double brightness) {
        int gray = (int)(60 * brightness);
        // Shadow interior (darker)
        g2.setColor(new Color(gray / 2, gray / 2, gray / 2 - 2, 120));
        g2.fillOval((int)(cx - r), (int)(cy - r * 0.5), (int)(r * 2), (int)(r));

        // Rim highlight (lighter on top edge)
        Stroke old = g2.getStroke();
        g2.setStroke(new BasicStroke(1.5f));
        int rimGray = (int)(100 + 40 * brightness);
        g2.setColor(new Color(rimGray, rimGray, rimGray - 5, 100));
        g2.drawArc((int)(cx - r), (int)(cy - r * 0.6), (int)(r * 2), (int)(r * 1.2), 20, 140);
        g2.setStroke(old);
    }

    private void drawEarth(Graphics2D g2, int ex, int ey, int er) {
        // Soft glow around earth
        RadialGradientPaint earthGlow = new RadialGradientPaint(
            new Point2D.Double(ex, ey), er + 20,
            new float[]{0f, 0.5f, 1f},
            new Color[]{new Color(60, 120, 200, 40), new Color(40, 80, 150, 15), new Color(0, 0, 0, 0)}
        );
        g2.setPaint(earthGlow);
        g2.fillOval(ex - er - 20, ey - er - 20, (er + 20) * 2, (er + 20) * 2);

        // Earth sphere
        RadialGradientPaint earth = new RadialGradientPaint(
            new Point2D.Double(ex - er * 0.2, ey - er * 0.2), er,
            new float[]{0f, 0.4f, 0.7f, 1f},
            new Color[]{
                new Color(100, 160, 220),
                new Color(40, 120, 180),
                new Color(30, 80, 60),
                new Color(15, 30, 50)
            }
        );
        g2.setPaint(earth);
        g2.fillOval(ex - er, ey - er, er * 2, er * 2);

        // Atmosphere rim
        Stroke old = g2.getStroke();
        g2.setStroke(new BasicStroke(1.5f));
        g2.setColor(new Color(100, 180, 255, 60));
        g2.drawOval(ex - er, ey - er, er * 2, er * 2);
        g2.setStroke(old);
    }
}
