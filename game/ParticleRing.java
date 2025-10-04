package game;

import java.awt.*;
import java.awt.AlphaComposite;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.util.Random;


public class ParticleRing {
    private final double x, y;
    private double radius;
    private final double expansionRate;
    private int alpha;
    private float stroke = 2f;
    private final Random rng = new Random();
    // If set, the ring will expand until it reaches this radius, then stop and fade.
    private final Integer maxRadius; // nullable

    public ParticleRing(double x, double y) {
        this(x, y, null);
    }

    public ParticleRing(double x, double y, int maxRadius) {
        this(x, y, Integer.valueOf(maxRadius));
    }

    private ParticleRing(double x, double y, Integer maxRadius) {
        this.x = x;
        this.y = y;
        this.radius = 1;         // start tiny
        this.expansionRate = 3;  // tweak feel
        this.alpha = 200;        // pretty visible at spawn
        this.maxRadius = maxRadius;
    }

    public boolean isAlive() {
        return alpha > 0;
    }

    public void update() {
        // Grow unless we've hit the stop radius (if given)
        if (maxRadius != null && radius >= maxRadius) {
            radius = maxRadius;          // lock to rim
            alpha -= 7;                 // fade quickly once locked
            if (alpha < 0) alpha = 0;
            if (stroke > 0.5f) stroke -= 0.2f; // thin the outline as it fades
            return;
        }

        radius += expansionRate;
        alpha -= 50 / maxRadius ; // normal fade while growing
        if (alpha < 0) alpha = 0;
    }

    public void draw(Graphics2D g2) {
        if (alpha <= 0) return;

        // Save state
        Object oldAA = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        Paint oldPaint = g2.getPaint();
        Composite oldComp = g2.getComposite();

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Thickness scales a bit with ring size but stays in a nice range
        float thickness = (float)Math.max(6, Math.min(18, radius * 0.12));
        float baseAlpha = alpha / 255f;

        // --- puffed rim: lots of soft dots around the circle ---
        int puffs = 28;                            // more = smoother / costlier
        double jitter = 2.0 + radius * 0.02;       // slight irregularity along rim
        Color c0 = new Color(200, 100, 255, 180);  // inner of each puff
        Color c1 = new Color(200, 100, 255,  40);  // mid
        Color c2 = new Color(200, 100, 255,   0);  // fully transparent

        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, baseAlpha * 0.9f));

        for (int i = 0; i < puffs; i++) {
            double ang = (i / (double)puffs) * Math.PI * 2.0;
            double jr  = (rng.nextDouble() - 0.5) * jitter;

            float cx = (float)(x + Math.cos(ang) * (radius + jr));
            float cy = (float)(y + Math.sin(ang) * (radius + jr));
            float R  = thickness + (float)(rng.nextDouble() * thickness * 0.6);

            RadialGradientPaint puff = new RadialGradientPaint(
                new Point2D.Float(cx, cy), R,
                new float[] { 0f, 0.6f, 1f },
                new Color[] { c0, c1, c2 }
            );

            g2.setPaint(puff);
            g2.fillOval((int)(cx - R), (int)(cy - R), (int)(2*R), (int)(2*R));
        }

        // --- subtle outer halo to sell the softness ---
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, baseAlpha * 0.32f));
        float haloR = (float)(radius + thickness * 1.5);
        float innerFrac = Math.max(0f, Math.min(1f, (float)(radius / haloR)));
        RadialGradientPaint halo = new RadialGradientPaint(
            new Point2D.Float((float)x, (float)y), haloR,
            new float[] { Math.max(0f, innerFrac - 0.02f), innerFrac, 1f },
            new Color[] {
                new Color(0, 0, 0, 0),
                new Color(200, 100, 255, 60),
                new Color(0, 0, 0, 0)
            }
        );
        g2.setPaint(halo);
        g2.fillOval((int)(x - haloR), (int)(y - haloR), (int)(2*haloR), (int)(2*haloR));

        // Restore state
        g2.setComposite(oldComp);
        g2.setPaint(oldPaint);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAA);
    }
}
