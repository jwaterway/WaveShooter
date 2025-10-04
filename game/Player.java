package game;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.BasicStroke;

public class Player {
    int x, y, radius;
    int angle = 0; // degrees
    int targetX, targetY;
    double rollAngle = 0;   // direction of movement
    double rollOffset = 0;  // shifts gradient to simulate spin
    double spinAngle = 0;  // radians, keeps spinning
    public double offsetAmt = 1.0; // default offset, range 0.1 – 2.0
    double vx = 0, vy = 0;   // velocity
    double ax = 0, ay = 0;   // acceleration
    double maxSpeed = 10;     // cap speed
    double accel = .8;      // acceleration amount
    double friction = 0.9;  // slows down when no keys pressed


    enum GunType { TRIANGLE, SQUARE, SINE }
    GunType currentGun = GunType.TRIANGLE; // default


    public Player(int x, int y, int radius) {
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
    public void rotate(int delta) {
        angle += delta;
        if (angle < 0) angle += 360;
        if (angle >= 360) angle -= 360;
    } 
    public int getAngle() {
        return angle;
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

        // limit speed
        double speed = Math.sqrt(vx*vx + vy*vy);
        if (speed > maxSpeed) {
            vx = (vx / speed) * maxSpeed;
            vy = (vy / speed) * maxSpeed;
        }

        // apply friction
        vx *= friction;
        vy *= friction;
        x += (int)vx;
        y += (int)vy;
     // bounce off walls x2
        if (x - radius < 0) {         // left wall
            x = radius;
            vx = -vx * 20;
        }
        if (x + radius > GamePanel.WIDTH) {   // right wall
            x = GamePanel.WIDTH - radius;
            vx = -vx * 20;
        }
        if (y - radius < 0) {         // top wall
            y = radius;
            vy = -vy * 20;
        }
        if (y + radius > GamePanel.HEIGHT) {  // bottom wall
            y = GamePanel.HEIGHT - radius;
            vy = -vy * 20;
        }

    }
    public void update() {
        // Spin speed is *linearly* proportional to offsetAmt
        double spinSpeed = offsetAmt * .05;  // smaller factor = slower
        spinAngle += spinSpeed;
        if (spinAngle >= 2 * Math.PI) {
            spinAngle -= 2 * Math.PI;
        }
    }

    public void draw(Graphics2D g2) {
        // -------------------------------
        // Gradient spin (uses spinAngle)
        // -------------------------------
        double gradRad = spinAngle;

        int gradX1 = (int)(x - radius * Math.cos(gradRad));
        int gradY1 = (int)(y - radius * Math.sin(gradRad));
        int gradX2 = (int)(x + radius * Math.cos(gradRad));
        int gradY2 = (int)(y + radius * Math.sin(gradRad));
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
        g2.fillOval(x - radius, y - radius, radius * 2, radius * 2);

        g2.setColor(outlineColor);
        g2.setStroke(new BasicStroke(2));
        g2.drawOval(x - radius, y - radius, radius * 2, radius * 2);

        // -------------------------------
        // Gun/arrow (uses angle)
        // -------------------------------
        double coneRad = Math.toRadians(angle);

        double arrowLength = (25 * (offsetAmt * 0.66) + 5);
        double arrowWidth  = (15 * (offsetAmt * 0.25) + 5);

        int innerX = x + (int)(radius * Math.cos(coneRad));
        int innerY = y + (int)(radius * Math.sin(coneRad));

        int tipX = innerX + (int)(arrowLength * Math.cos(coneRad));
        int tipY = innerY + (int)(arrowLength * Math.sin(coneRad));

        int baseX1 = innerX + (int)(arrowWidth * Math.cos(coneRad + Math.PI / 2));
        int baseY1 = innerY + (int)(arrowWidth * Math.sin(coneRad + Math.PI / 2));

        int baseX2 = innerX + (int)(arrowWidth * Math.cos(coneRad - Math.PI / 2));
        int baseY2 = innerY + (int)(arrowWidth * Math.sin(coneRad - Math.PI / 2));

        Polygon arrowHead = new Polygon();
        arrowHead.addPoint(tipX, tipY);
        arrowHead.addPoint(baseX1, baseY1);
        arrowHead.addPoint(baseX2, baseY2);

        // Shading halves for 3D look
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
            case TRIANGLE: light = new Color(255, 200, 160); dark = new Color(120, 40, 0); break;
            case SQUARE:   light = new Color(150, 240, 255); dark = new Color(0, 60, 160); break;
            case SINE:     light = new Color(180, 255, 180); dark = new Color(0, 100, 40); break;
            default:       light = new Color(220, 255, 255); dark = new Color(0, 80, 120); break;
        }

        g2.setColor(light);
        g2.fillPolygon(leftHalf);
        g2.setColor(dark);
        g2.fillPolygon(rightHalf);

        g2.setColor(Color.BLACK);
        g2.setStroke(new BasicStroke(2));
        g2.drawPolygon(arrowHead);

        // HUD for offset
        g2.setColor(Color.WHITE);
        g2.drawString("Offset: " + String.format("%.2f", offsetAmt), 200, 20);
    }
}