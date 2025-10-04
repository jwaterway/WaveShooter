package game;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.RadialGradientPaint;

public class BlackHole {
    private double x, y, radius;
    private double vx = .5, vy = .23; // drift speed
    private int slowTimer = 0;
    private double slowFactor = 1.0; // 1.0 = no slow
    // Flash state
    private int flashAlpha = 0;   // soft bloom
    private int flashTimer = 0;   // rim pulse countdown (frames)
    
    private int flashCooldown = 0;

    // Ambient glow control (set to 0 for no color when not flashing)
    private int ambientGlowAlpha = 0;  // try 0..80; 0 = off

    public double getX() { return x; }
    public double getY() { return y; }
    public double getRadius() { return radius; }
    private double baseVx() { return 2.5; }   // your nominal drift (or store originals)
    private double baseVy() { return 13.0; }
    
    public void applyDamage(double amount) {
        radius -= amount;
        if (radius < 6) radius = 6; // minimum size
    }

    public void applyKnockback(double hitDx, double hitDy, double power) {
        double d = Math.hypot(hitDx, hitDy);
        if (d < 1e-6) return;
        // normalize away-from-impact vector
        double nx = hitDx / d, ny = hitDy / d;
        vx += nx * power;
        vy += ny * power;
    }

    public void applySlow(int frames, double factor) {
        slowTimer = Math.max(slowTimer, frames);
        slowFactor = Math.min(slowFactor, factor); // keep strongest slow
    }

    public BlackHole(double x, double y, double radius) {
        this.x = x; this.y = y; this.radius = radius;
    }

    public void setAmbientGlowAlpha(int a) {
        ambientGlowAlpha = Math.max(0, Math.min(255, a));
    }

    public void absorbStar() {
        radius += 1; // slow growth
    }

 

    public void flash() {
        if (flashCooldown > 0) return;
        flashAlpha = 180;
        flashTimer = 10;
        flashCooldown = 20;
    }

    public void update(int width, int height) {
        // decay flash & cooldown
        if (flashAlpha > 0) { flashAlpha -= 12; if (flashAlpha < 0) flashAlpha = 0; }
        if (flashTimer > 0)  { flashTimer--; }
        if (flashCooldown > 0) flashCooldown--;

        // slow
        double scale = (slowTimer > 0) ? slowFactor : 1.0;
        if (slowTimer > 0) { slowTimer--; if (slowTimer == 0) slowFactor = 1.0; }

        x += vx * scale;
        y += vy * scale;

        if (x - radius < 0 || x + radius > width)  vx = -vx;
        if (y - radius < 0 || y + radius > height) vy = -vy;
    }


    public void draw(Graphics2D g2) {
        // save state
        Object oldAA = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        Paint oldPaint = g2.getPaint();
        Stroke oldStroke = g2.getStroke();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        float rCore  = (float) radius;
        Point2D center = new Point2D.Float((float) x, (float) y);

        // --- AMBIENT GLOW (off if ambientGlowAlpha == 0) ---
        if (ambientGlowAlpha > 0) {
            float rGlow  = rCore * 1.6f;
            try {
                RadialGradientPaint glow = new RadialGradientPaint(
                    center, rGlow,
                    new float[] { 0f, 0.7f, 1f },
                    new Color[] {
                        new Color(90,  0, 140, Math.min(160, ambientGlowAlpha)),
                        new Color(30,  0,  60, Math.min(80,  ambientGlowAlpha / 2)),
                        new Color(0,   0,   0, 0)
                    }
                );
                g2.setPaint(glow);
                int gx = Math.round((float)(x - rGlow));
                int gy = Math.round((float)(y - rGlow));
                int gd = Math.round(rGlow * 2);
                g2.fillOval(gx, gy, gd, gd);
            } catch (Throwable t) {
                g2.setColor(new Color(90, 0, 140, Math.min(80, ambientGlowAlpha)));
                g2.setStroke(new BasicStroke(Math.max(2f, rCore * 0.08f)));
                g2.drawOval((int)(x - radius * 1.4), (int)(y - radius * 1.4),
                            (int)(radius * 2.8), (int)(radius * 2.8));
            }
        }

        // --- CORE: solid black disk (default state) ---
        g2.setPaint(oldPaint);
        g2.setColor(Color.BLACK);
        g2.fillOval(
            (int)Math.round(x - rCore),
            (int)Math.round(y - rCore),
            (int)Math.round(rCore * 2),
            (int)Math.round(rCore * 2)
        );

        // --- RIM PULSE during flash (brief outline) ---
        if (flashTimer > 0) {
            int alpha = 60 + (int)(120 * (flashTimer / 10.0)); // fades to 60
            g2.setColor(new Color(255, 220, 150, alpha));
            g2.setStroke(new BasicStroke(3));
            float rimR = rCore * 1.1f;
            g2.drawOval(
                (int)Math.round(x - rimR),
                (int)Math.round(y - rimR),
                (int)Math.round(rimR * 2),
                (int)Math.round(rimR * 2)
            );
        }

        // --- SOFT BLOOM over core while flashing (brief fill) ---
        if (flashAlpha > 0) {
            g2.setColor(new Color(255, 255, 200, flashAlpha));
            g2.fillOval((int)(x - radius), (int)(y - radius),
                        (int)(radius * 2), (int)(radius * 2));
        }

        // restore state
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAA);
        g2.setPaint(oldPaint);
        g2.setStroke(oldStroke);
    }

    public void applyLensingOffset(double sx, double sy, double[] outXY) {
        double dx = sx - x, dy = sy - y;
        double d2 = dx*dx + dy*dy;
        if (d2 <= 1e-6) { outXY[0]=sx; outXY[1]=sy; return; }

        double d = Math.sqrt(d2);
        double influence = Math.max(0, (radius * 2 - d) / (radius * 2)); // 0..1 inside ~2R
        double bend = 0.005 * radius * influence * influence;            // tweak strength

        // tangent vector (perpendicular to radial)
        double tx = -dy / d, ty = dx / d;
        outXY[0] = sx + tx * bend * d;
        outXY[1] = sy + ty * bend * d;
    }
  

  
}
