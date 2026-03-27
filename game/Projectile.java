package game;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.RadialGradientPaint;

public class Projectile {
    double x, y;                 // current position (on wave path)
    double baseX, baseY;         // straight-line base position
    int size = 6;                // size of projectile
    double dx, dy, radius = 4;   // velocity + collision radius
    double speed;                // stored speed
    double traveled = 0;         // distance traveled along path

    Color color;                 // color based on gun
    String type;                 // "TRIANGLE", "SQUARE", "SINE"
    double angle;                // firing angle
    double offsetAmt;            // wave control amount

    int w = GamePanel.WIDTH;
    int h = GamePanel.HEIGHT;

    private boolean alive = true;
    private int pierceCount = 0;
    private Player.GunType gunType;

    public double getX() { return x; }
    public double getY() { return y; }
    public double getDx() { return dx; }
    public double getDy() { return dy; }
    public double getRadius() { return radius; }
    public double getOffsetAmt() { return offsetAmt; }
    public boolean isAlive() { return alive; }
    public void kill() { alive = false; }

    public void incrementPierce() { pierceCount++; }
    public int getPierceCount() { return pierceCount; }
    public Player.GunType getGunType() { return gunType; }

    public Projectile(double x, double y, double angle, Player.GunType gun, double offsetAmt) {
        this.x = x;
        this.y = y;
        this.baseX = x;
        this.baseY = y;
        this.angle = angle;
        this.offsetAmt = offsetAmt;
        this.gunType = gun;

        this.speed = 12.0;
        this.dx = speed * Math.cos(angle);
        this.dy = speed * Math.sin(angle);

        switch (gun) {
            case TRIANGLE:
                color = new Color(255, 80, 40);
                type = "TRIANGLE";
                break;
            case SQUARE:
                color = new Color(40, 160, 255);
                type = "SQUARE";
                break;
            case SINE:
                color = new Color(80, 255, 120);
                type = "SINE";
                break;
            default:
                color = Color.WHITE;
                type = "CIRCLE";
        }
    }

    // Overloaded constructor used for shards / programmatic spawns
    public Projectile(double x, double y, double vx, double vy, double radius, Player.GunType gun) {
        this.x = x;
        this.y = y;
        this.baseX = x;
        this.baseY = y;
        this.dx = vx;
        this.dy = vy;
        this.radius = radius;
        this.gunType = gun;

        this.angle = Math.atan2(vy, vx);
        this.speed = Math.sqrt(vx * vx + vy * vy);
        this.offsetAmt = 0;

        switch (gun) {
            case TRIANGLE:
                color = new Color(255, 80, 40);
                type = "TRIANGLE";
                break;
            case SQUARE:
                color = new Color(40, 160, 255);
                type = "SQUARE";
                break;
            case SINE:
                color = new Color(80, 255, 120);
                type = "SINE";
                break;
            default:
                color = Color.WHITE;
                type = "CIRCLE";
        }
    }

    public static Projectile childShard(double startX, double startY, double vx, double vy, double radius) {
        return new Projectile(startX, startY, vx, vy, radius, Player.GunType.SQUARE);
    }

    public void update() {
        baseX += dx;
        baseY += dy;
        traveled += speed;

        // Ball follows the wave path
        double offset = computeWaveOffset(0);
        x = baseX + offset * Math.cos(angle + Math.PI / 2.0);
        y = baseY + offset * Math.sin(angle + Math.PI / 2.0);
    }

    private double computeWaveOffset(double distBack) {
        switch (type) {
            case "TRIANGLE": {
                double s = traveled * 0.15 - distBack * 0.22;
                double triVal = (2.0 / Math.PI) * Math.asin(Math.sin(s));
                return triVal * offsetAmt * 7.0;
            }
            case "SQUARE": {
                double s = traveled * 0.25 - distBack * 0.22;
                double squareVal = (Math.sin(s) >= 0) ? 1.0 : -1.0;
                return squareVal * offsetAmt * 5.0;
            }
            case "SINE": {
                double freq = Math.max(0.12, offsetAmt * 0.15);
                double s = traveled * 0.25 - distBack * freq;
                return Math.sin(s) * (10.0 + offsetAmt * 4.0);
            }
            default:
                return 0;
        }
    }

    /**
     * Linearly interpolate between two colors (including alpha).
     */
    private Color lerp(Color a, Color b, double t) {
        t = Math.max(0, Math.min(1, t));
        int ar = a.getRed();
        int ag = a.getGreen();
        int ab = a.getBlue();
        int aa = a.getAlpha();
        int br = b.getRed();
        int bg = b.getGreen();
        int bb = b.getBlue();
        int ba = b.getAlpha();
        int r = (int)(ar + (br - ar) * t);
        int g = (int)(ag + (bg - ag) * t);
        int bcol = (int)(ab + (bb - ab) * t);
        int acol = (int)(aa + (ba - aa) * t);
        return new Color(r, g, bcol, acol);
    }

    /**
     * Draw a radial glow at the tip of the projectile using a
     * RadialGradientPaint. The center color is the projectile's
     * base color and it fades to transparent.
     */
    private void drawTipGlow(Graphics2D g2) {
        // pulse radius based on travelled distance for simple animation
        float baseRadius = 10f;
        float pulse = (float)(Math.sin(traveled * 0.3) * 2.0); // ±2 pixels
        float radius = baseRadius + pulse;

        Point2D center = new Point2D.Float((float)x, (float)y);
        // three stops: solid center, mid-fade, outer fade-to-transparent
        float[] dist = {0.0f, 0.6f, 1.0f};
        Color[] colors = {
            new Color(color.getRed(), color.getGreen(), color.getBlue(), 255),
            new Color(color.getRed(), color.getGreen(), color.getBlue(), 120),
            new Color(color.getRed(), color.getGreen(), color.getBlue(), 0)
        };
        RadialGradientPaint rgp = new RadialGradientPaint(center, radius, dist, colors);
        Paint old = g2.getPaint();
        g2.setPaint(rgp);
        g2.fillOval((int)(x - radius), (int)(y - radius), (int)(radius * 2), (int)(radius * 2));
        g2.setPaint(old);
    }

    public boolean isOffscreen(int width, int height) {
        return baseX < -50 || baseX > width + 50 || baseY < -50 || baseY > height + 50;
    }

    public void draw(Graphics2D g2) {
        // Draw a glowing tip first so it appears on top of the wave segments
        drawTipGlow(g2);

        switch (type) {
            case "TRIANGLE":
                drawTriangleWave(g2);
                break;
            case "SQUARE":
                drawSquareWave(g2);
                break;
            case "SINE":
                drawSineWave(g2);
                break;
            default:
                g2.setColor(color);
                g2.fillOval((int)(x - size / 2.0), (int)(y - size / 2.0), size, size);
        }
    }

    private void drawTriangleWave(Graphics2D g2) {
        double prevX = 0;
        double prevY = 0;
        boolean hasPrev = false;

        double spacing = 2.0;
        int count = 22;
        double freq = 0.22;
        double animSpeed = 0.15;
        double amp = offsetAmt * 7.0;

        // prepare gradient endpoints
        Color tipColor = color.brighter();
        Color tailColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), 50);

        for (int i = 0; i < count; i++) {
            double distBack = i * spacing;

            double px = baseX - distBack * Math.cos(angle);
            double py = baseY - distBack * Math.sin(angle);

            double s = traveled * animSpeed - distBack * freq;
            double triVal = (2.0 / Math.PI) * Math.asin(Math.sin(s));

            double offset = triVal * amp;

            double ox = offset * Math.cos(angle + Math.PI / 2.0);
            double oy = offset * Math.sin(angle + Math.PI / 2.0);

            double drawX = px + ox;
            double drawY = py + oy;

            // set gradient color for this segment
            double t = (double)i / (count - 1);
            g2.setColor(lerp(tipColor, tailColor, t));

            if (hasPrev) {
                g2.drawLine((int)Math.round(prevX), (int)Math.round(prevY),
                            (int)Math.round(drawX), (int)Math.round(drawY));
            }

            prevX = drawX;
            prevY = drawY;
            hasPrev = true;
        }
    }

    private void drawSquareWave(Graphics2D g2) {
        double prevX = 0;
        double prevY = 0;
        boolean hasPrev = false;

        double spacing = 2.0;
        int count = 18;
        double freq = 0.22;
        double animSpeed = 0.25;
        double amp = offsetAmt * 5.0;

        Color tipColor = color.brighter();
        Color tailColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), 50);

        for (int i = 0; i < count; i++) {
            double distBack = i * spacing;

            double px = baseX - distBack * Math.cos(angle);
            double py = baseY - distBack * Math.sin(angle);

            double s = traveled * animSpeed - distBack * freq;
            double squareVal = (Math.sin(s) >= 0) ? 1.0 : -1.0;
            double offset = squareVal * amp;

            double ox = offset * Math.cos(angle + Math.PI / 2.0);
            double oy = offset * Math.sin(angle + Math.PI / 2.0);

            double drawX = px + ox;
            double drawY = py + oy;

            // gradient color
            double t = (double)i / (count - 1);
            g2.setColor(lerp(tipColor, tailColor, t));

            if (hasPrev) {
                g2.drawLine((int)Math.round(prevX), (int)Math.round(prevY),
                            (int)Math.round(drawX), (int)Math.round(drawY));
            }

            g2.fillRect((int)Math.round(drawX), (int)Math.round(drawY), 3, 3);

            prevX = drawX;
            prevY = drawY;
            hasPrev = true;
        }
    }
    
    private void drawSineWave(Graphics2D g2) {
        double prevX = 0;
        double prevY = 0;
        boolean hasPrev = false;

        double spacing = 1.6;
        int count = 24;
        double freq = Math.max(0.12, offsetAmt * 0.15);
        double animSpeed = 0.25;
        double amp = 10.0 + offsetAmt * 4.0;

        Color tipColor = color.brighter();
        Color tailColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), 50);

        for (int i = 0; i < count; i++) {
            double distBack = i * spacing;

            double px = baseX - distBack * Math.cos(angle);
            double py = baseY - distBack * Math.sin(angle);

            double s = traveled * animSpeed - distBack * freq;
            double offset = Math.sin(s) * amp;

            double ox = offset * Math.cos(angle + Math.PI / 2.0);
            double oy = offset * Math.sin(angle + Math.PI / 2.0);

            double drawX = px + ox;
            double drawY = py + oy;

            // gradient color
            double t = (double)i / (count - 1);
            g2.setColor(lerp(tipColor, tailColor, t));

            if (hasPrev) {
                g2.drawLine((int)Math.round(prevX), (int)Math.round(prevY),
                            (int)Math.round(drawX), (int)Math.round(drawY));
            }

            g2.fillOval((int)Math.round(drawX), (int)Math.round(drawY), 3, 3);

            prevX = drawX;
            prevY = drawY;
            hasPrev = true;
        }
    }
}