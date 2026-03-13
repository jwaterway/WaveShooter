package game;

import java.awt.*;
import java.awt.geom.Point2D;

public class PowerUp {
    public enum Type {
        HEALTH,          // powerup1 — red/pink cross
        SHIELD,          // powerup2 — cyan hexagonal shield
        WEAPON_SINE,     // powerup3 — green sine icon
        WEAPON_SQUARE,   // powerup4 — orange square icon
        WEAPON_TRIANGLE, // powerup5 — blue triangle icon
        WEAPON_BOOST     // powerup6 — gold star burst
    }

    private double x, y;
    private final double radius = 16;
    private double timer = 10.0 * 60; // 10 seconds at 60fps
    private double age = 0;
    private boolean alive = true;
    private final Type type;
    private double vy = 0.3; // gentle drift downward

    public PowerUp(double x, double y, Type type) {
        this.x = x;
        this.y = y;
        this.type = type;
    }

    public void update() {
        timer -= 1;
        age += 1;
        y += vy;
        // Fade kill
        if (timer <= 0 || y > 1100) {
            alive = false;
        }
    }

    public void draw(Graphics2D g2) {
        if (!alive) return;

        Object oldAA = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        Paint oldPaint = g2.getPaint();
        Stroke oldStroke = g2.getStroke();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        double pulse = 0.8 + 0.2 * Math.sin(age * 0.15);
        double glow = 0.5 + 0.5 * Math.sin(age * 0.08);
        float r = (float)(radius * pulse);
        int dx = (int) x, dy = (int) y;

        // Fade out in last 2 seconds
        float fadeAlpha = 1f;
        if (timer < 120) fadeAlpha = (float)(timer / 120.0);

        // Outer neon glow (type-colored halo)
        Color neonColor = getNeonColor();
        for (int i = 3; i >= 1; i--) {
            float gr = r + i * 5;
            int a = (int)(40 * glow * fadeAlpha * (1.0 - i / 4.0));
            g2.setColor(new Color(neonColor.getRed(), neonColor.getGreen(), neonColor.getBlue(), a));
            g2.fillOval((int)(dx - gr), (int)(dy - gr), (int)(gr * 2), (int)(gr * 2));
        }

        // Core radial gradient
        Color coreInner = brighter(neonColor, 80);
        Color coreMid = neonColor;
        Color coreOuter = new Color(neonColor.getRed(), neonColor.getGreen(), neonColor.getBlue(), 0);
        RadialGradientPaint rgp = new RadialGradientPaint(
            new Point2D.Float(dx, dy), r,
            new float[]{0f, 0.5f, 1f},
            new Color[]{withAlpha(coreInner, (int)(220 * fadeAlpha)),
                        withAlpha(coreMid, (int)(140 * fadeAlpha)),
                        coreOuter}
        );
        g2.setPaint(rgp);
        g2.fillOval((int)(dx - r), (int)(dy - r), (int)(r * 2), (int)(r * 2));

        // Pulsing ring
        g2.setColor(withAlpha(coreInner, (int)(160 * pulse * fadeAlpha)));
        g2.setStroke(new BasicStroke(2f));
        float ringR = r + 3 + (float)(2 * glow);
        g2.drawOval((int)(dx - ringR), (int)(dy - ringR), (int)(ringR * 2), (int)(ringR * 2));

        // Draw icon on top
        g2.setColor(withAlpha(Color.WHITE, (int)(230 * fadeAlpha)));
        drawIcon(g2, dx, dy, (int)(r * 0.6));

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAA);
        g2.setPaint(oldPaint);
        g2.setStroke(oldStroke);
    }

    private void drawIcon(Graphics2D g2, int cx, int cy, int s) {
        g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        switch (type) {
            case HEALTH: // Cross
                g2.fillRect(cx - s / 4, cy - s, s / 2, s * 2);
                g2.fillRect(cx - s, cy - s / 4, s * 2, s / 2);
                break;
            case SHIELD: // Hexagon outline
                Polygon hex = new Polygon();
                for (int i = 0; i < 6; i++) {
                    double a = Math.PI / 6 + i * Math.PI / 3;
                    hex.addPoint(cx + (int)(s * Math.cos(a)), cy + (int)(s * Math.sin(a)));
                }
                g2.drawPolygon(hex);
                break;
            case WEAPON_SINE: // Sine wave squiggle
                int prev = cy;
                for (int i = -s; i <= s; i++) {
                    int ny = cy + (int)(s * 0.6 * Math.sin(i * Math.PI * 2.0 / s));
                    if (i > -s) g2.drawLine(cx + i - 1, prev, cx + i, ny);
                    prev = ny;
                }
                break;
            case WEAPON_SQUARE: // Square wave
                g2.drawLine(cx - s, cy + s / 2, cx - s, cy - s / 2);
                g2.drawLine(cx - s, cy - s / 2, cx, cy - s / 2);
                g2.drawLine(cx, cy - s / 2, cx, cy + s / 2);
                g2.drawLine(cx, cy + s / 2, cx + s, cy + s / 2);
                g2.drawLine(cx + s, cy + s / 2, cx + s, cy - s / 2);
                break;
            case WEAPON_TRIANGLE: // Triangle wave
                g2.drawLine(cx - s, cy, cx - s / 2, cy - s / 2);
                g2.drawLine(cx - s / 2, cy - s / 2, cx, cy);
                g2.drawLine(cx, cy, cx + s / 2, cy + s / 2);
                g2.drawLine(cx + s / 2, cy + s / 2, cx + s, cy);
                break;
            case WEAPON_BOOST: // Star burst (4-point star)
                for (int i = 0; i < 4; i++) {
                    double a = i * Math.PI / 2 + age * 0.05;
                    g2.drawLine(cx, cy,
                        cx + (int)(s * Math.cos(a)),
                        cy + (int)(s * Math.sin(a)));
                }
                // Inner diamond
                g2.drawLine(cx, cy - s / 2, cx + s / 2, cy);
                g2.drawLine(cx + s / 2, cy, cx, cy + s / 2);
                g2.drawLine(cx, cy + s / 2, cx - s / 2, cy);
                g2.drawLine(cx - s / 2, cy, cx, cy - s / 2);
                break;
        }
    }

    private Color getNeonColor() {
        switch (type) {
            case HEALTH:          return new Color(255, 60, 120);   // hot pink
            case SHIELD:          return new Color(0, 220, 255);    // cyan
            case WEAPON_SINE:     return new Color(0, 255, 140);    // neon green
            case WEAPON_SQUARE:   return new Color(255, 160, 30);   // neon orange
            case WEAPON_TRIANGLE: return new Color(80, 140, 255);   // electric blue
            case WEAPON_BOOST:    return new Color(255, 220, 50);   // gold
            default:              return Color.WHITE;
        }
    }

    private static Color brighter(Color c, int amt) {
        return new Color(
            Math.min(255, c.getRed() + amt),
            Math.min(255, c.getGreen() + amt),
            Math.min(255, c.getBlue() + amt));
    }

    private static Color withAlpha(Color c, int a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, a)));
    }

    public boolean isAlive() { return alive; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getRadius() { return radius; }
    public Type getType() { return type; }

    /** Returns the sound key for this power-up type (powerup1..powerup6). */
    public String getSoundKey() {
        return "powerup" + (type.ordinal() + 1);
    }
}
