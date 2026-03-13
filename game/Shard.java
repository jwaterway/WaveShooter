package game;

import java.awt.*;

import java.util.Random;

public class Shard {
    private static final Random RNG = new Random();

    private double x, y;
    private double dx, dy;
    private double life = 1.0;   // 1.0 -> 0 (decays slower)
    private final Color baseColor;
    private final int baseSize = 4;  // base size multiplier (reduced for smaller trailing sparks)
    private int level = 1;           // splitting level (1 = small, 2 = medium, 3 = large)
    private double spin = 0;
    private final double angle;
    private boolean splitDone = false;

    public Shard(double x, double y, double angle, double speed, Color color) {
        this.x = x;
        this.y = y;
        this.angle = angle;
        this.dx = Math.cos(angle) * speed;
        this.dy = Math.sin(angle) * speed;
        this.baseColor = color;
        this.level = 2; // default shards are slightly larger so they can split
    }

    // overloaded constructor to explicitly set splitting level
    public Shard(double x, double y, double angle, double speed, Color color, int level) {
        this.x = x;
        this.y = y;
        this.angle = angle;
        this.dx = Math.cos(angle) * speed;
        this.dy = Math.sin(angle) * speed;
        this.baseColor = color;
        this.level = Math.max(1, level);
    }

    public void update() {
        x += dx;
        y += dy;
        life -= 0.03;            // slower fade
        if (life < 0) life = 0;
        spin += 0.15;            // rotate over time
    }

    public void draw(Graphics2D g2) {
        if (life <= 0) return;
        // increase brightness towards the end of life to make shards pop
        int alpha = (int)(255 * Math.pow(life, 0.7));
        // flash between yellow and red as life decreases
        Color flash = (RNG.nextDouble() < (1 - life) * 0.6) ? Color.RED : Color.YELLOW;
        Color c = new Color(flash.getRed(), flash.getGreen(), flash.getBlue(), alpha);
        g2.setColor(c);
        // draw rotating outline triangle
        int x0 = (int)Math.round(x);
        int y0 = (int)Math.round(y);
        double rot = spin;
        Polygon poly = new Polygon();
        int effSize = (int)Math.round(baseSize * level);
        for (int k = 0; k < 3; k++) {
            double angk = angle + rot + k * (Math.PI * 2 / 3);
            double radius = effSize * (k == 0 ? 1.0 : 0.5);
            int px = x0 + (int)Math.round(Math.cos(angk) * radius);
            int py = y0 + (int)Math.round(Math.sin(angk) * radius);
            poly.addPoint(px, py);
        }
        g2.setStroke(new BasicStroke(Math.max(0.5f, 1f * level)));
        g2.setColor(new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), (int)(255 * life)));
        g2.drawPolygon(poly);
    }

    // allow GamePanel to query splitting behaviour
    public boolean shouldSplit() {
        return !splitDone && level > 1 && life < 0.5;
    }

    public void markSplitDone() { splitDone = true; }

    public double getX() { return x; }
    public double getY() { return y; }

    public int getLevel() { return level; }

    public boolean isAlive() {
        return life > 0;
    }
}
