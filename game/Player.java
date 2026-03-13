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
    
    // Ship rotation (constant + speed-based)
    double shipRotation = 0;
    double baseSpinSpeed = 0.03;  // constant spin speed (rad/frame)
    
    // Blinking lights
    double lightBlinkTimer = 0;
    double baseLightBlinkSpeed = 0.08;  // faster blink speed when stationary

    public double offsetAmt = 1.0;

    double vx = 0, vy = 0;
    double ax = 0, ay = 0;

    double maxSpeed = 66;
    double accel = 0.65;
    double friction = 0.95;
    double bounceFactor = 1.0; // 1.0 = same speed back, 0.8 = lose some speed

    enum GunType { TRIANGLE, SQUARE, SINE }
    GunType currentGun = GunType.TRIANGLE;
	public int score = 0;

    // health meter (percentage 0–100)
    private double health = 100.0;

    public double getHealth() { return health; }
    public void takeDamage(double amt) {
        health -= amt;
        if (health < 0) health = 0;
    }
    public void heal(double amt) {
        health += amt;
        if (health > 100) health = 100;
    }

    /**
     * Apply a knockback to the player velocity.  ``dx,dy`` is the vector
     * pointing from the source of the hit toward the player; ``strength``
     * is the amount of velocity to add along that direction.
     */
    public void applyKnockback(double dx, double dy, double strength) {
        double mag = Math.hypot(dx, dy);
        if (mag > 0) {
            vx += (dx / mag) * strength;
            vy += (dy / mag) * strength;
        }
    }

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
        
        // Ship rotation: constant spin + movement-based acceleration
        double movementSpeed = Math.sqrt(vx * vx + vy * vy);
        double speedFactor = 1.0 + (movementSpeed / maxSpeed) * 95.0;  // up to 6x faster when moving
        shipRotation += baseSpinSpeed * speedFactor;
        if (shipRotation >= 2 * Math.PI) {
            shipRotation -= 2 * Math.PI;
        }
        
        // Light blinking: faster when moving
        double blinkSpeed = baseLightBlinkSpeed * speedFactor;
        lightBlinkTimer += blinkSpeed;
        if (lightBlinkTimer >= 2 * Math.PI) {
            lightBlinkTimer -= 2 * Math.PI;
        }
    }

    public void draw(Graphics2D g2) {
        int drawX = (int)Math.round(x);
        int drawY = (int)Math.round(y);

        // Save graphics state for rotation
        g2.translate(drawX, drawY);
        g2.rotate(shipRotation);

        // === DRAW HULL: Rounded triangular/circular shape ===
        double gradRad = spinAngle;
        Color hullColor1, hullColor2, outlineColor;

        switch (currentGun) {
            case TRIANGLE:
                hullColor1 = new Color(255, 140, 100);
                hullColor2 = new Color(120, 20, 0);
                outlineColor = new Color(90, 30, 10);
                break;
            case SQUARE:
                hullColor1 = new Color(120, 220, 255);
                hullColor2 = new Color(0, 40, 120);
                outlineColor = new Color(0, 30, 80);
                break;
            case SINE:
                hullColor1 = new Color(180, 255, 180);
                hullColor2 = new Color(0, 80, 40);
                outlineColor = new Color(0, 40, 20);
                break;
            default:
                hullColor1 = new Color(180, 255, 255);
                hullColor2 = new Color(0, 60, 120);
                outlineColor = new Color(0, 50, 100);
        }

        GradientPaint gradient = new GradientPaint(
            (int)(-radius * Math.cos(gradRad)), (int)(-radius * Math.sin(gradRad)), hullColor1,
            (int)(radius * Math.cos(gradRad)), (int)(radius * Math.sin(gradRad)), hullColor2
        );
        g2.setPaint(gradient);

        // Draw rounded triangle (three curves forming a shield-like shape)
        int numPoints = 30;
        int[] xPoints = new int[numPoints];
        int[] yPoints = new int[numPoints];
        
        for (int i = 0; i < numPoints; i++) {
            double angle = (i / (double)numPoints) * 2 * Math.PI;
            // Rounded triangle: three bumps
            double r = radius * (0.7 + 0.3 * Math.cos(3 * angle));
            xPoints[i] = (int)Math.round(r * Math.cos(angle));
            yPoints[i] = (int)Math.round(r * Math.sin(angle));
        }
        
        Polygon hull = new Polygon(xPoints, yPoints, numPoints);
        g2.fillPolygon(hull);

        g2.setColor(outlineColor);
        g2.setStroke(new BasicStroke(1));
        g2.drawPolygon(hull);

        // === DRAW BLINKING LIGHTS: Three red lights ===
        double blinkIntensity = (Math.sin(lightBlinkTimer) + 1.0) / 2.0;  // 0 to 1
        int lightBrightness = (int)(100 + blinkIntensity * 155);  // 100 to 255
        Color lightColor = new Color(255, Math.max(0, lightBrightness - 150), Math.max(0, lightBrightness - 150));

        // Three light positions (equally spaced around the ship)
        for (int i = 0; i < 3; i++) {
            double lightAngle = (i / 3.0) * 2 * Math.PI;
            int lightX = (int)Math.round((radius * 0.6) * Math.cos(lightAngle));
            int lightY = (int)Math.round((radius * 0.6) * Math.sin(lightAngle));
            int lightRadius = 4 + (int)(2 * blinkIntensity);

            g2.setColor(lightColor);
            g2.fillOval(lightX - lightRadius, lightY - lightRadius, lightRadius * 2, lightRadius * 2);
            g2.setColor(new Color(255, 200, 100));
            g2.setStroke(new BasicStroke(1));
            g2.drawOval(lightX - lightRadius, lightY - lightRadius, lightRadius * 2, lightRadius * 2);
        }

        // === DRAW ELECTRICAL ACCELERATOR (in front of ship) ===
        // The accelerator shoots out in the gun direction (currently pointing right/0°)
        double gunAngle = Math.toRadians(angle - shipRotation * 180 / Math.PI);  // Convert to local space
        
        int innerX = (int)Math.round(radius * Math.cos(gunAngle));
        int innerY = (int)Math.round(radius * Math.sin(gunAngle));

        double arrowLength = (25 * (offsetAmt * 0.66) + 5);
        double arrowWidth = (15 * (offsetAmt * 0.25) + 5);

        int tipX = (int)Math.round(innerX + arrowLength * Math.cos(gunAngle));
        int tipY = (int)Math.round(innerY + arrowLength * Math.sin(gunAngle));

        int baseX1 = (int)Math.round(innerX + arrowWidth * Math.cos(gunAngle + Math.PI / 2));
        int baseY1 = (int)Math.round(innerY + arrowWidth * Math.sin(gunAngle + Math.PI / 2));

        int baseX2 = (int)Math.round(innerX + arrowWidth * Math.cos(gunAngle - Math.PI / 2));
        int baseY2 = (int)Math.round(innerY + arrowWidth * Math.sin(gunAngle - Math.PI / 2));

        // === DRAW ELECTRICAL FLICKER FROM CENTER TO GUN TIP ===
        // Core glow at center - electricity origin point
        int coreRadius = 4;
        float pulse = 0.7f + 0.3f * (float)Math.sin(System.nanoTime() / 80_000_000.0);
        for (int r = coreRadius + 6; r > 0; r--) {
            float t = (float)r / (coreRadius + 6);
            int alpha = (int)(60 * pulse * (1 - t));
            g2.setColor(new Color(140, 220, 255, alpha));
            g2.fillOval(-r, -r, r * 2, r * 2);
        }
        g2.setColor(new Color(220, 240, 255, (int)(255 * pulse)));
        g2.fillOval(-coreRadius, -coreRadius, coreRadius * 2, coreRadius * 2);
        g2.setColor(new Color(255, 255, 255, (int)(200 * pulse)));
        g2.fillOval(-2, -2, 4, 4);

        g2.setColor(new Color(100, 200, 255));
        g2.setStroke(new BasicStroke(1.0f));
        
        // Draw 3-4 random electrical paths from center (0,0) to gun tip
        for (int arc = 0; arc < 3; arc++) {
            // Random offset for jagged path
            double offsetAmount = (Math.random() - 0.5) * 15;
            
            // Draw zigzag path from center to tip
            int prevX = 0, prevY = 0;
            for (int step = 0; step <= 5; step++) {
                double t = step / 5.0;
                int x = (int)Math.round(tipX * t + offsetAmount * Math.sin(t * Math.PI * 3));
                int y = (int)Math.round(tipY * t + offsetAmount * Math.cos(t * Math.PI * 3));
                
                if (step > 0) {
                    // Vary alpha for flicker effect
                    int alpha = (int)(150 + Math.random() * 105);
                    g2.setColor(new Color(100, 200, 255, alpha));
                    g2.drawLine(prevX, prevY, x, y);
                }
                prevX = x;
                prevY = y;
            }
        }

        // Draw electrical arcs
        g2.setColor(new Color(100, 200, 255));
        g2.setStroke(new BasicStroke(1));
        
        // Three arc trails
        for (int arc = 0; arc < 3; arc++) {
            double offset = arc * 0.15;
            for (int j = 0; j < 4; j++) {
                double t = (j / 4.0);
                double jitterX = (Math.random() - 0.5) * 8;
                double jitterY = (Math.random() - 0.5) * 8;
                
                int startX = (int)Math.round(innerX + (arrowLength * 0.4) * t * Math.cos(gunAngle + offset) + jitterX);
                int startY = (int)Math.round(innerY + (arrowLength * 0.4) * t * Math.sin(gunAngle + offset) + jitterY);
                
                int nextT = (int)Math.min((j + 1) / 4.0, 1.0);
                int endX = (int)Math.round(innerX + (arrowLength * 0.4) * nextT * Math.cos(gunAngle + offset) + (Math.random() - 0.5) * 8);
                int endY = (int)Math.round(innerY + (arrowLength * 0.4) * nextT * Math.sin(gunAngle + offset) + (Math.random() - 0.5) * 8);
                
                g2.drawLine(startX, startY, endX, endY);
            }
        }

        // Draw the main accelerator cone - open at tip for natural look
        Polygon electricalCone = new Polygon();
        // Open tip: two points instead of one for a gap at the front
        int tipOffset = 4;
        int tipX1 = (int)Math.round(tipX + tipOffset * Math.cos(gunAngle + Math.PI / 4));
        int tipY1 = (int)Math.round(tipY + tipOffset * Math.sin(gunAngle + Math.PI / 4));
        int tipX2 = (int)Math.round(tipX + tipOffset * Math.cos(gunAngle - Math.PI / 4));
        int tipY2 = (int)Math.round(tipY + tipOffset * Math.sin(gunAngle - Math.PI / 4));
        
        electricalCone.addPoint(tipX1, tipY1);
        electricalCone.addPoint(tipX2, tipY2);
        electricalCone.addPoint(baseX2, baseY2);
        electricalCone.addPoint(baseX1, baseY1);

        g2.setColor(new Color(150, 220, 255, 80));  // More transparent
        g2.fillPolygon(electricalCone);
        
        g2.setColor(new Color(100, 200, 255, 100));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawPolygon(electricalCone);

        // Restore graphics state
        g2.rotate(-shipRotation);
        g2.translate(-drawX, -drawY);

        // Draw offset indicator
        g2.setColor(Color.WHITE);
        g2.drawString("Offset: " + String.format("%.2f", offsetAmt), drawX - 100, drawY - radius - 20);
    }
}