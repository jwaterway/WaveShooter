package game;

import java.awt.*;
import java.awt.geom.Point2D;

/**
 * Exotic space background structures for Level 2+.
 * Purely decorative (harmless), drawn behind gameplay elements.
 * Types: SPIRAL_GALAXY, SQUARE_WAVE_GALAXY
 */
public class SpaceStructure {

    enum Type { SPIRAL_GALAXY, SQUARE_WAVE_GALAXY }

    private double x, y;       // center position
    private double vx, vy;     // drift velocity
    private Type type;
    private double rotation;   // current angle (radians)
    private double rotSpeed;   // rotation speed
    private int radius;        // visual radius
    private boolean alive = true;
    private double age = 0;    // frames alive
    private Color[] palette;
    private double[] armOffsets; // per-arm random offsets

    public SpaceStructure(double x, double y, Type type, double vx, double vy) {
        this.x = x;
        this.y = y;
        this.type = type;
        this.vx = vx;
        this.vy = vy;
        this.rotation = Math.random() * Math.PI * 2;
        this.rotSpeed = 0.003 + Math.random() * 0.004;
        this.radius = 100 + (int)(Math.random() * 80);

        if (type == Type.SPIRAL_GALAXY) {
            // Warm-cool galaxy palettes
            int pick = (int)(Math.random() * 3);
            switch (pick) {
                case 0: palette = new Color[] {
                    new Color(180, 120, 255), new Color(100, 160, 255),
                    new Color(255, 180, 220), new Color(220, 200, 255)
                }; break;
                case 1: palette = new Color[] {
                    new Color(255, 160, 80), new Color(255, 220, 120),
                    new Color(255, 100, 60), new Color(200, 180, 255)
                }; break;
                default: palette = new Color[] {
                    new Color(80, 200, 255), new Color(120, 255, 200),
                    new Color(200, 220, 255), new Color(160, 240, 255)
                }; break;
            }
            armOffsets = new double[] {
                Math.random() * Math.PI, Math.random() * Math.PI,
                Math.random() * Math.PI, Math.random() * Math.PI
            };
        } else {
            // Square-wave galaxy — geometric, angular colors
            palette = new Color[] {
                new Color(120, 255, 180), new Color(80, 220, 255),
                new Color(200, 255, 160), new Color(160, 200, 255)
            };
            armOffsets = new double[] { Math.random(), Math.random(), Math.random() };
        }
    }

    public void update(int w, int h) {
        x += vx;
        y += vy;
        rotation += rotSpeed;
        age++;

        // Kill when well off-screen
        int margin = radius * 3;
        if (y > h + margin || y < -margin * 2 || x < -margin || x > w + margin) {
            alive = false;
        }
    }

    public boolean isAlive() { return alive; }
    public double getX() { return x; }
    public double getY() { return y; }

    public void draw(Graphics2D g2) {
        Composite oldComp = g2.getComposite();
        Stroke oldStroke = g2.getStroke();

        // Slight fade-in
        float baseAlpha = (float)Math.min(1.0, age / 60.0) * 0.7f;

        if (type == Type.SPIRAL_GALAXY) {
            drawSpiralGalaxy(g2, baseAlpha);
        } else {
            drawSquareWaveGalaxy(g2, baseAlpha);
        }

        g2.setComposite(oldComp);
        g2.setStroke(oldStroke);
    }

    private void drawSpiralGalaxy(Graphics2D g2, float baseAlpha) {
        int numArms = 3 + (int)(armOffsets[0] * 2); // 3-4 arms
        int particlesPerArm = 80;
        double maxR = radius;

        // Core glow
        float[] dist = {0f, 0.4f, 1f};
        Color[] colors = {
            withAlpha(palette[0], (int)(120 * baseAlpha)),
            withAlpha(palette[1], (int)(60 * baseAlpha)),
            new Color(0, 0, 0, 0)
        };
        RadialGradientPaint coreGlow = new RadialGradientPaint(
            new Point2D.Double(x, y), (float)(maxR * 0.35), dist, colors);
        g2.setPaint(coreGlow);
        g2.fillOval((int)(x - maxR * 0.35), (int)(y - maxR * 0.35),
            (int)(maxR * 0.7), (int)(maxR * 0.7));

        // Arms — logarithmic spiral with particles
        for (int arm = 0; arm < numArms; arm++) {
            double armAngle = (arm / (double)numArms) * Math.PI * 2 + rotation;
            Color armColor = palette[arm % palette.length];

            for (int p = 0; p < particlesPerArm; p++) {
                double t = p / (double)particlesPerArm;
                // Logarithmic spiral: r = a * e^(b*theta)
                double theta = armAngle + t * 3.5 + armOffsets[arm % armOffsets.length];
                double r = 8 + maxR * t * 0.9;

                // Scatter perpendicular to arm
                double scatter = (Math.sin(p * 0.7 + age * 0.02) * 8 + Math.cos(p * 1.3) * 5) * t;
                double px = x + Math.cos(theta) * r + Math.cos(theta + Math.PI / 2) * scatter;
                double py = y + Math.sin(theta) * r + Math.sin(theta + Math.PI / 2) * scatter;

                // Fade particles toward edges
                double alpha = baseAlpha * (1.0 - t * 0.7) * (0.6 + 0.4 * Math.sin(p * 0.5 + age * 0.03));
                int sz = (int)(1.5 + (1.0 - t) * 2.5);

                g2.setColor(withAlpha(armColor, (int)(255 * alpha)));
                g2.fillOval((int)px, (int)py, sz, sz);

                // Occasional brighter star
                if (p % 12 == 0) {
                    g2.setColor(withAlpha(Color.WHITE, (int)(180 * alpha)));
                    g2.fillOval((int)px - 1, (int)py - 1, sz + 2, sz + 2);
                }
            }
        }

        // Bright center core
        int coreR = (int)(maxR * 0.06);
        g2.setColor(withAlpha(Color.WHITE, (int)(200 * baseAlpha)));
        g2.fillOval((int)x - coreR, (int)y - coreR, coreR * 2, coreR * 2);
    }

    private void drawSquareWaveGalaxy(Graphics2D g2, float baseAlpha) {
        int numArms = 4;
        int stepsPerArm = 40;
        double maxR = radius;

        // Core glow — angular/square feel
        int coreSize = (int)(maxR * 0.25);
        g2.setColor(withAlpha(palette[0], (int)(50 * baseAlpha)));
        g2.fillRect((int)(x - coreSize), (int)(y - coreSize), coreSize * 2, coreSize * 2);
        g2.setColor(withAlpha(palette[1], (int)(80 * baseAlpha)));
        int innerCore = coreSize / 2;
        g2.fillRect((int)(x - innerCore), (int)(y - innerCore), innerCore * 2, innerCore * 2);

        g2.setStroke(new BasicStroke(1.5f));

        for (int arm = 0; arm < numArms; arm++) {
            double armAngle = (arm / (double)numArms) * Math.PI * 2 + rotation;
            Color armColor = palette[arm % palette.length];

            double cx = x, cy = y;
            double dir = armAngle;
            double segLen = maxR * 0.06;

            for (int s = 0; s < stepsPerArm; s++) {
                double t = s / (double)stepsPerArm;

                // Square-wave: alternate direction with sharp 90-degree turns
                // Step length grows outward
                double len = segLen * (1.0 + t * 2.0);

                // Direction: step forward, then turn 90° on every other step
                double nx, ny;
                if (s % 2 == 0) {
                    // Move outward along current direction
                    nx = cx + Math.cos(dir) * len;
                    ny = cy + Math.sin(dir) * len;
                } else {
                    // Turn 90° and step — this creates the square-wave pattern
                    double turnDir = ((s / 2) % 2 == 0) ? Math.PI / 2 : -Math.PI / 2;
                    nx = cx + Math.cos(dir + turnDir) * len * 0.6;
                    ny = cy + Math.sin(dir + turnDir) * len * 0.6;
                }

                double alpha = baseAlpha * (1.0 - t * 0.6);
                g2.setColor(withAlpha(armColor, (int)(200 * alpha)));
                g2.drawLine((int)cx, (int)cy, (int)nx, (int)ny);

                // Node dots at corners
                if (s % 2 == 1) {
                    int dotR = (int)(2 + (1.0 - t) * 2);
                    g2.setColor(withAlpha(Color.WHITE, (int)(160 * alpha)));
                    g2.fillOval((int)nx - dotR, (int)ny - dotR, dotR * 2, dotR * 2);
                }

                // Glow particles along the line
                if (s % 4 == 0) {
                    double mid = 0.5;
                    double mx = cx + (nx - cx) * mid;
                    double my = cy + (ny - cy) * mid;
                    g2.setColor(withAlpha(armColor, (int)(80 * alpha)));
                    g2.fillOval((int)mx - 3, (int)my - 3, 6, 6);
                }

                cx = nx;
                cy = ny;
            }
        }

        // Bright center point
        g2.setColor(withAlpha(Color.WHITE, (int)(220 * baseAlpha)));
        g2.fillOval((int)x - 3, (int)y - 3, 6, 6);
    }

    private static Color withAlpha(Color c, int alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, alpha)));
    }
}
