package game;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.BasicStroke;

public class Player {
    double x, y;
    int radius;

    double angle = 0.0;   // degrees
    int targetX, targetY;

    double rollAngle = 0;
    double rollOffset = 0;
    double spinAngle = 0;

    public double offsetAmt = 1.0;

    double vx = 0, vy = 0;
    double ax = 0, ay = 0;

    double maxSpeed = 16;
    double accel = 0.65;
    double friction = 0.9;
    double bounceFactor = 1.0; // 1.0 = same speed back, 0.8 = lose some speed

    enum GunType { TRIANGLE, SQUARE, SINE }
    GunType currentGun = GunType.TRIANGLE;

    public Player(double x, double y, int radius) {
        this.x = x;
        this.y = y;
        this.radius = radius;
    }

    public void setGun(GunType gun) {
        currentGun = gun;
    }

    public GunType getGun() {
        return currentGun;
    }

    public void rotate(double delta) {
        angle += delta;

        while (angle < 0) {
            angle += 360;
        }
        while (angle >= 360) {
            angle -= 360;
        }
    }

    public double getAngle() {
        return angle;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public int getDrawX() {
        return (int)Math.round(x);
    }

    public int getDrawY() {
        return (int)Math.round(y);
    }

    public void updateMovement(boolean up, boolean down, boolean left, boolean right) {
        ax = 0;
        ay = 0;

        if (up)    ay -= accel;
        if (down)  ay += accel;
        if (left)  ax -= accel;
        if (right) ax += accel;

        vx += ax;
        vy += ay;

        double speed = Math.sqrt(vx * vx + vy * vy);
        if (speed > maxSpeed) {
            vx = (vx / speed) * maxSpeed;
            vy = (vy / speed) * maxSpeed;
        }

        vx *= friction;
        vy *= friction;

        x += vx;
        y += vy;

        if (x - radius < 0) {
            x = radius;
            vx = -vx * bounceFactor;
        }
        if (x + radius > GamePanel.WIDTH) {
            x = GamePanel.WIDTH - radius;
            vx = -vx * bounceFactor;
        }
        if (y - radius < 0) {
            y = radius;
            vy = -vy * bounceFactor;
        }
        if (y + radius > GamePanel.HEIGHT) {
            y = GamePanel.HEIGHT - radius;
            vy = -vy * bounceFactor;
        }
    }

    public void update() {
        double spinSpeed = offsetAmt * 0.05;
        spinAngle += spinSpeed;

        if (spinAngle >= 2 * Math.PI) {
            spinAngle -= 2 * Math.PI;
        }
    }

    public void draw(Graphics2D g2) {
        int drawX = (int)Math.round(x);
        int drawY = (int)Math.round(y);

        double gradRad = spinAngle;

        int gradX1 = (int)Math.round(x - radius * Math.cos(gradRad));
        int gradY1 = (int)Math.round(y - radius * Math.sin(gradRad));
        int gradX2 = (int)Math.round(x + radius * Math.cos(gradRad));
        int gradY2 = (int)Math.round(y + radius * Math.sin(gradRad));

        GradientPaint gradient;
        Color outlineColor;

        switch (currentGun) {
            case TRIANGLE:
                gradient = new GradientPaint(
                    gradX1, gradY1, new Color(255, 140, 100),
                    gradX2, gradY2, new Color(120, 20, 0)
                );
                outlineColor = new Color(90, 30, 10);
                break;
            case SQUARE:
                gradient = new GradientPaint(
                    gradX1, gradY1, new Color(120, 220, 255),
                    gradX2, gradY2, new Color(0, 40, 120)
                );
                outlineColor = new Color(0, 30, 80);
                break;
            case SINE:
                gradient = new GradientPaint(
                    gradX1, gradY1, new Color(180, 255, 180),
                    gradX2, gradY2, new Color(0, 80, 40)
                );
                outlineColor = new Color(0, 40, 20);
                break;
            default:
                gradient = new GradientPaint(
                    gradX1, gradY1, new Color(180, 255, 255),
                    gradX2, gradY2, new Color(0, 60, 120)
                );
                outlineColor = new Color(0, 50, 100);
        }

        g2.setPaint(gradient);
        g2.fillOval(drawX - radius, drawY - radius, radius * 2, radius * 2);

        g2.setColor(outlineColor);
        g2.setStroke(new BasicStroke(2));
        g2.drawOval(drawX - radius, drawY - radius, radius * 2, radius * 2);

        double coneRad = Math.toRadians(angle);

        double arrowLength = (25 * (offsetAmt * 0.66) + 5);
        double arrowWidth  = (15 * (offsetAmt * 0.25) + 5);

        int innerX = (int)Math.round(x + radius * Math.cos(coneRad));
        int innerY = (int)Math.round(y + radius * Math.sin(coneRad));

        int tipX = (int)Math.round(innerX + arrowLength * Math.cos(coneRad));
        int tipY = (int)Math.round(innerY + arrowLength * Math.sin(coneRad));

        int baseX1 = (int)Math.round(innerX + arrowWidth * Math.cos(coneRad + Math.PI / 2));
        int baseY1 = (int)Math.round(innerY + arrowWidth * Math.sin(coneRad + Math.PI / 2));

        int baseX2 = (int)Math.round(innerX + arrowWidth * Math.cos(coneRad - Math.PI / 2));
        int baseY2 = (int)Math.round(innerY + arrowWidth * Math.sin(coneRad - Math.PI / 2));

        Polygon arrowHead = new Polygon();
        arrowHead.addPoint(tipX, tipY);
        arrowHead.addPoint(baseX1, baseY1);
        arrowHead.addPoint(baseX2, baseY2);

        Polygon leftHalf = new Polygon();
        leftHalf.addPoint(tipX, tipY);
        leftHalf.addPoint(baseX1, baseY1);
        leftHalf.addPoint(innerX, innerY);

        Polygon rightHalf = new Polygon();
        rightHalf.addPoint(tipX, tipY);
        rightHalf.addPoint(baseX2, baseY2);
        rightHalf.addPoint(innerX, innerY);

        Color light, dark;
        switch (currentGun) {
            case TRIANGLE:
                light = new Color(255, 200, 160);
                dark = new Color(120, 40, 0);
                break;
            case SQUARE:
                light = new Color(150, 240, 255);
                dark = new Color(0, 60, 160);
                break;
            case SINE:
                light = new Color(180, 255, 180);
                dark = new Color(0, 100, 40);
                break;
            default:
                light = new Color(220, 255, 255);
                dark = new Color(0, 80, 120);
                break;
        }

        g2.setColor(light);
        g2.fillPolygon(leftHalf);

        g2.setColor(dark);
        g2.fillPolygon(rightHalf);

        g2.setColor(Color.BLACK);
        g2.setStroke(new BasicStroke(2));
        g2.drawPolygon(arrowHead);

        g2.setColor(Color.WHITE);
        g2.drawString("Offset: " + String.format("%.2f", offsetAmt), 200, 20);
    }
}