package game;

import java.awt.*;

public class Projectile {
    double x, y;                 // current position
    int size = 10;               // size of projectile
    double dx, dy, radius = 6;   // velocity + collision radius
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
        this.angle = angle;
        this.offsetAmt = offsetAmt;
        this.gunType = gun;

        this.speed = 6.0;
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
        x += dx;
        y += dy;
        traveled += speed;
    }

    public boolean isOffscreen(int width, int height) {
        return x < -50 || x > width + 50 || y < -50 || y > height + 50;
    }

    public void draw(Graphics2D g2) {
        g2.setColor(color);

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

        for (int i = 0; i < count; i++) {
            double distBack = i * spacing;

            double px = x - distBack * Math.cos(angle);
            double py = y - distBack * Math.sin(angle);

            double s = traveled * animSpeed - distBack * freq;
            double triVal = (2.0 / Math.PI) * Math.asin(Math.sin(s));

            double offset = triVal * amp;

            // keep very front centered
            if (i == 0) {
                offset = 0;
            }

            double ox = offset * Math.cos(angle + Math.PI / 2.0);
            double oy = offset * Math.sin(angle + Math.PI / 2.0);

            double drawX = px + ox;
            double drawY = py + oy;

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

        for (int i = 0; i < count; i++) {
            double distBack = i * spacing;

            double px = x - distBack * Math.cos(angle);
            double py = y - distBack * Math.sin(angle);

            double s = traveled * animSpeed - distBack * freq;
            double squareVal = (Math.sin(s) >= 0) ? 1.0 : -1.0;
            double offset = squareVal * amp;

            if (i == 0) {
                offset = 0;
            }

            double ox = offset * Math.cos(angle + Math.PI / 2.0);
            double oy = offset * Math.sin(angle + Math.PI / 2.0);

            double drawX = px + ox;
            double drawY = py + oy;

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
        double freq = Math.max(0.18, offsetAmt * 0.25);
        double animSpeed = 0.25;
        double amp = 10.0 + offsetAmt * 4.0;

        for (int i = 0; i < count; i++) {
            double distBack = i * spacing;

            double px = x - distBack * Math.cos(angle);
            double py = y - distBack * Math.sin(angle);

            double s = traveled * animSpeed - distBack * freq;
            double offset = Math.sin(s) * amp;

            if (i == 0) {
                offset = 0;
            }

            double ox = offset * Math.cos(angle + Math.PI / 2.0);
            double oy = offset * Math.sin(angle + Math.PI / 2.0);

            double drawX = px + ox;
            double drawY = py + oy;

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