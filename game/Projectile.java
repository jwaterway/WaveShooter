package game;

import java.awt.*;

public class Projectile {
    double x, y;           // current position
    int size = 10;      // size of projectile
    double dx, dy, radius = 6;      // velocity
    Color color;        // color based on gun
    String type;        // "TRIANGLE", "SQUARE", "SINE"
    double angle;  		// <--- store firing angle
    double offsetAmt;   // angle offset for waves
    int w = GamePanel.WIDTH;
    int h = GamePanel.HEIGHT;
    private boolean alive = true;
    private int pierceCount = 0;
         // set your default if you have one
    private Player.GunType gunType;     // remember which gun fired this

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
        this.angle = angle;   // <--- save it
        this.offsetAmt = offsetAmt;   // store it
        this.gunType = gun;  
        double speed = 6.0;
        dx = speed * Math.cos(angle);
        dy = speed * Math.sin(angle);

        
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
    
    public void update() {
        x += dx;
        y += dy;
    }
    
    public boolean isOffscreen(int width, int height) {
        return x < -50 || x > width + 50 || y < -50 || y > height + 50;
    }
    
 // Overloaded constructor used for shards / programmatic spawns
    public Projectile(double x, double y, double vx, double vy, double radius, Player.GunType gun) {
        this.x = (int)x;
        this.y = (int)y;
        this.dx = vx;
        this.dy = vy;
        this.radius = radius;
        this.gunType = gun;

        switch (gun) {
            case TRIANGLE: color = new Color(255, 80, 40);  type = "TRIANGLE"; break;
            case SQUARE:   color = new Color(40, 160, 255); type = "SQUARE";   break;
            case SINE:     color = new Color(80, 255, 120); type = "SINE";     break;
            default:       color = Color.WHITE;             type = "CIRCLE";
        }
    }

    
 // Factory for SQUARE shrapnel
    public static Projectile childShard(double startX, double startY, double vx, double vy, double radius) {
        return new Projectile(startX, startY, vx, vy, radius, Player.GunType.SQUARE);
    }

    
    public void draw(Graphics2D g2) {
        g2.setColor(color);
        double prevX = 0, prevY = 0;
        switch (type) {
        case "TRIANGLE":
        	
            for (int i = 0; i < 25; i++) {
            	double px = x - i * 2 * Math.cos(angle);
            	double py = y - i * 2 * Math.sin(angle);
            	prevX = px;
                prevY = py;

                // Triangle wave using sawtooth formula
                double phase = ((x + y + i) * 0.2) % (2 * Math.PI);
                double triVal = 2 * Math.abs((phase / Math.PI) - 1) - 1; // range -1..1

                double offset = triVal * offsetAmt * 8;
                double ox = offset * Math.cos(angle + Math.PI / 2);
                double oy = offset * Math.sin(angle + Math.PI / 2);

                g2.drawLine((int)prevX, (int)prevY, (int)(px + ox), (int)(py + oy));
                prevX = px;
                prevY = py;
            }
            break;

        
        case "SQUARE":
            boolean hasPrev = false;

            for (int i = 0; i < 15; i++) {
                // Travel along projectile direction
                double px = (int)(x - i * 2 * Math.cos(angle));
                double py = (int)(y - i * 2 * Math.sin(angle));

                // Square wave value: +1 or -1
                double phase = (x + y + i) * 0.2;
                double squareVal = (Math.sin(phase) >= 0) ? 1 : -1;
                double offset = (squareVal * (offsetAmt * .5 * 10));

                // Apply perpendicular offset
                double ox = (int)(offset * Math.cos(angle + Math.PI / 2));
                double oy = (int)(offset * Math.sin(angle + Math.PI / 2));

                double drawX = px + ox;
                double drawY = py + oy;

                if (hasPrev) {
                    // Connect to previous point so vertical jumps are drawn
                    g2.drawLine((int)Math.round(prevX), (int)Math.round(prevY), (int)Math.round(drawX), (int)Math.round(drawY));
                }

                g2.fillRect((int)Math.round(drawX), (int)Math.round(drawY), 3, 3);

                prevX = drawX;
                prevY = drawY;
                hasPrev = true;
            }
            break;

          
        case "SINE":
            for (int i = 0; i < 15; i++) {
                // Wave starts at current moving position
                double px = (int)(x - i * 2 * Math.cos(angle));
                double py = (int)(y - i * 2 * Math.sin(angle));

                // Offset perpendicular to direction
                double offset = (int)(Math.sin((x + y + i) * offsetAmt) * 14);

                double ox = (int)(offset * Math.cos(angle + Math.PI/2));
                double oy = (int)(offset * Math.sin(angle + Math.PI/2));

                g2.fillOval((int)Math.round(px + ox), (int)Math.round(py + oy), 3, 3);
            }
            break;


        default:
        	g2.fillOval((int)(x - size / 2.0), (int)(y - size / 2.0), size, size);
    }

    }
}
