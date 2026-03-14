package game;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.Random;

public class Asteroid {
    private double x, y;
    private double vx, vy;
    private double rotation = 0;
    private double rotSpeed;
    private int radius;
    private boolean alive = true;
    private int[] shapeX, shapeY; // irregular polygon vertices
    private int vertexCount;
    private Color baseColor;
    private Color highlightColor;
    private Color shadowColor;
    private static final Random rng = new Random();

    public Asteroid(double x, double y, int radius, double vx, double vy) {
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.vx = vx;
        this.vy = vy;
        this.rotSpeed = (rng.nextDouble() - 0.5) * 0.04;
        generateShape();
        generateColors();
    }

    private void generateShape() {
        vertexCount = 8 + rng.nextInt(5); // 8-12 vertices
        shapeX = new int[vertexCount];
        shapeY = new int[vertexCount];
        for (int i = 0; i < vertexCount; i++) {
            double angle = (i / (double) vertexCount) * Math.PI * 2;
            double r = radius * (0.7 + rng.nextDouble() * 0.5); // jagged radius
            shapeX[i] = (int) (Math.cos(angle) * r);
            shapeY[i] = (int) (Math.sin(angle) * r);
        }
    }

    private void generateColors() {
        int shade = 60 + rng.nextInt(40);
        int warm = rng.nextInt(30);
        baseColor = new Color(shade + warm, shade + warm / 2, shade);
        highlightColor = new Color(
            Math.min(255, shade + warm + 60),
            Math.min(255, shade + warm / 2 + 50),
            Math.min(255, shade + 40),
            160
        );
        shadowColor = new Color(shade / 3, shade / 3, shade / 4, 180);
    }

    public void update(int width, int height) {
        x += vx;
        y += vy;
        rotation += rotSpeed;

        // Remove if drifted way off-screen
        if (x < -200 || x > width + 200 || y < -200 || y > height + 200) {
            alive = false;
        }
    }

    public void draw(Graphics2D g2) {
        if (!alive) return;

        Object oldAA = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        Paint oldPaint = g2.getPaint();
        Stroke oldStroke = g2.getStroke();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        AffineTransform oldXform = g2.getTransform();
        g2.translate(x, y);
        g2.rotate(rotation);

        // Shadow (offset slightly)
        Polygon shadow = new Polygon(shapeX, shapeY, vertexCount);
        g2.translate(3, 3);
        g2.setColor(new Color(0, 0, 0, 60));
        g2.fillPolygon(shadow);
        g2.translate(-3, -3);

        // Main body
        Polygon body = new Polygon(shapeX, shapeY, vertexCount);
        g2.setColor(baseColor);
        g2.fillPolygon(body);

        // Surface cracks / detail lines
        g2.setStroke(new BasicStroke(1.0f));
        g2.setColor(shadowColor);
        for (int i = 0; i < 3; i++) {
            int i1 = rng.nextInt(vertexCount);
            int cx = shapeX[i1] / 3;
            int cy = shapeY[i1] / 3;
            g2.drawLine(shapeX[i1], shapeY[i1], cx, cy);
        }

        // Highlight on upper-left quadrant
        g2.setColor(highlightColor);
        g2.setStroke(new BasicStroke(2.0f));
        for (int i = 0; i < vertexCount; i++) {
            int next = (i + 1) % vertexCount;
            double midX = (shapeX[i] + shapeX[next]) / 2.0;
            double midY = (shapeY[i] + shapeY[next]) / 2.0;
            if (midX < 0 && midY < 0) {
                g2.drawLine(shapeX[i], shapeY[i], shapeX[next], shapeY[next]);
            }
        }

        // Outline
        g2.setStroke(new BasicStroke(1.5f));
        g2.setColor(new Color(40, 35, 30));
        g2.drawPolygon(body);

        g2.setTransform(oldXform);

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAA);
        g2.setPaint(oldPaint);
        g2.setStroke(oldStroke);
    }

    /** Split into smaller asteroids. Returns empty list if too small to split. */
    public ArrayList<Asteroid> split() {
        ArrayList<Asteroid> children = new ArrayList<>();
        if (radius < 15) return children; // too small to split

        int childCount = 2 + (radius > 30 ? 1 : 0);
        int childRadius = (int)(radius * 0.55);
        for (int i = 0; i < childCount; i++) {
            double ang = (i / (double) childCount) * Math.PI * 2 + rng.nextDouble();
            double spd = 0.8 + rng.nextDouble() * 1.2;
            children.add(new Asteroid(x, y, childRadius,
                vx + Math.cos(ang) * spd,
                vy + Math.sin(ang) * spd));
        }
        return children;
    }

    public boolean isAlive() { return alive; }
    public void kill() { alive = false; }
    public double getX() { return x; }
    public double getY() { return y; }
    public int getRadius() { return radius; }
}
