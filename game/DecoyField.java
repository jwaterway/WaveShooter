package game;

import java.awt.*;
import java.awt.geom.Point2D;

/**
 * DecoyField — A detached electric forcefield that floats in place and
 * attracts homing missiles for 5 seconds. Deployed by the player via
 * the DECOY powerup. Missiles re-target this instead of the player.
 * Visually: spinning hexagonal electric cage with pulsing attraction rings.
 */
public class DecoyField {
    private double x, y;
    private int timer;             // frames remaining (300 = 5 seconds at 60fps)
    private static final int DURATION = 300;
    private boolean alive = true;
    private int age = 0;
    private final int radius = 40; // attraction visual radius

    public DecoyField(double x, double y) {
        this.x = x;
        this.y = y;
        this.timer = DURATION;
    }

    public void update() {
        timer--;
        age++;
        if (timer <= 0) alive = false;
    }

    public boolean isAlive() { return alive; }
    public double getX() { return x; }
    public double getY() { return y; }
    public int getRadius() { return radius; }
    public int getTimer() { return timer; }

    public void draw(Graphics2D g2) {
        if (!alive) return;

        Composite oldComp = g2.getComposite();
        Stroke oldStroke = g2.getStroke();

        double t = age * 0.05;
        float baseAlpha = 1.0f;
        if (timer < 90) baseAlpha = timer / 90.0f; // fade out last 1.5s

        float pulse = 0.6f + 0.4f * (float)Math.sin(t * 6.0);

        // Attraction rings — expanding concentric pulses pulling inward
        for (int ring = 0; ring < 3; ring++) {
            double ringPhase = (t * 3.0 + ring * 2.1) % 3.0;
            double ringR = radius * 2.5 * (1.0 - ringPhase / 3.0);
            if (ringR > 5) {
                int a = (int)(80 * (ringPhase / 3.0) * baseAlpha);
                g2.setColor(new Color(100, 200, 255, a));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawOval((int)(x - ringR), (int)(y - ringR), (int)(ringR * 2), (int)(ringR * 2));
            }
        }

        // Outer electric glow
        float[] dist = {0f, 0.5f, 1.0f};
        Color[] colors = {
            new Color(80, 200, 255, (int)(60 * pulse * baseAlpha)),
            new Color(60, 150, 220, (int)(25 * pulse * baseAlpha)),
            new Color(0, 0, 0, 0)
        };
        RadialGradientPaint glow = new RadialGradientPaint(
            new Point2D.Double(x, y), radius * 1.5f, dist, colors);
        g2.setPaint(glow);
        g2.fillOval((int)(x - radius * 1.5), (int)(y - radius * 1.5),
            (int)(radius * 3), (int)(radius * 3));

        // Rotating hexagonal cage
        double rotation = t * 2.0;
        int hexPoints = 6;
        Polygon hex = new Polygon();
        double hexR = radius * (0.9 + 0.1 * Math.sin(t * 8.0));
        for (int i = 0; i < hexPoints; i++) {
            double angle = rotation + i * Math.PI / 3;
            double wobbleR = hexR + 3 * Math.sin(t * 12.0 + i * 1.8);
            hex.addPoint((int)(x + wobbleR * Math.cos(angle)),
                         (int)(y + wobbleR * Math.sin(angle)));
        }

        // Hex fill
        g2.setColor(new Color(40, 160, 255, (int)(25 * pulse * baseAlpha)));
        g2.fillPolygon(hex);

        // Hex outline
        g2.setStroke(new BasicStroke(2.5f));
        g2.setColor(new Color(100, 220, 255, (int)(200 * pulse * baseAlpha)));
        g2.drawPolygon(hex);

        // Electric arcs between vertices
        g2.setStroke(new BasicStroke(1.2f));
        int[] xp = hex.xpoints;
        int[] yp = hex.ypoints;
        for (int i = 0; i < hexPoints; i++) {
            int next = (i + 1) % hexPoints;
            // Crawling arc along each edge
            double crawl = (t * 4.0 + i * 0.9) % 1.0;
            double arcX = xp[i] + (xp[next] - xp[i]) * crawl;
            double arcY = yp[i] + (yp[next] - yp[i]) * crawl;
            for (int j = 0; j < 3; j++) {
                double forkAngle = Math.random() * Math.PI * 2;
                double forkLen = 6 + Math.random() * 12;
                int fx = (int)(arcX + forkLen * Math.cos(forkAngle));
                int fy = (int)(arcY + forkLen * Math.sin(forkAngle));
                int a = (int)(180 * baseAlpha * (0.5 + 0.5 * Math.random()));
                g2.setColor(new Color(160, 230, 255, a));
                g2.drawLine((int)arcX, (int)arcY, fx, fy);
            }
        }

        // Vertex sparks
        for (int i = 0; i < hexPoints; i++) {
            double sparkPulse = 0.5 + 0.5 * Math.sin(t * 10 + i * 2.5);
            int sparkR = (int)(4 + 3 * sparkPulse);
            int a = (int)(220 * sparkPulse * baseAlpha);
            g2.setColor(new Color(200, 255, 255, a));
            g2.fillOval(xp[i] - sparkR, yp[i] - sparkR, sparkR * 2, sparkR * 2);
        }

        // Center core — bright pulsing orb
        int coreR = (int)(8 + 4 * pulse);
        g2.setColor(new Color(180, 240, 255, (int)(200 * baseAlpha)));
        g2.fillOval((int)(x - coreR), (int)(y - coreR), coreR * 2, coreR * 2);
        g2.setColor(new Color(255, 255, 255, (int)(180 * pulse * baseAlpha)));
        int innerR = coreR / 2;
        g2.fillOval((int)(x - innerR), (int)(y - innerR), innerR * 2, innerR * 2);

        // "DECOY" label
        g2.setFont(new Font("Arial", Font.BOLD, 10));
        FontMetrics fm = g2.getFontMetrics();
        String label = String.format("%.1fs", timer / 60.0);
        int lx = (int)x - fm.stringWidth(label) / 2;
        g2.setColor(new Color(100, 200, 255, (int)(180 * baseAlpha)));
        g2.drawString(label, lx, (int)(y + radius + 14));

        g2.setComposite(oldComp);
        g2.setStroke(oldStroke);
    }
}
