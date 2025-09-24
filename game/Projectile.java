package game;

import java.awt.*;

public class Projectile {
    int x, y;           // current position
    int size = 10;      // size of projectile
    double dx, dy;      // velocity
    Color color;        // color based on gun
    String type;        // "TRIANGLE", "SQUARE", "SINE"
    double angle;  		// <--- store firing angle
    double offsetAmt;   // angle offset for waves
    int w = GamePanel.WIDTH;
    int h = GamePanel.HEIGHT;


    public Projectile(int x, int y, double angle, Player.GunType gun, double offsetAmt) {
        this.x = x;
        this.y = y;
        this.angle = angle;   // <--- save it
        this.offsetAmt = offsetAmt;   // store it

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

    
    public void draw(Graphics2D g2) {
        g2.setColor(color);

        switch (type) {
        case "TRIANGLE":
            for (int i = 0; i < 15; i++) {
                int px = (int)(x - i * 2 * Math.cos(angle));
                int py = (int)(y - i * 2 * Math.sin(angle));

                // Triangle wave using sawtooth formula
                double phase = ((x + y + i) * 0.2) % (2 * Math.PI);
                double triVal = 2 * Math.abs((phase / Math.PI) - 1) - 1; // range -1..1

                int offset = (int)(triVal * offsetAmt * 8);

                int ox = (int)(offset * Math.cos(angle + Math.PI / 2));
                int oy = (int)(offset * Math.sin(angle + Math.PI / 2));

                g2.fillRect(px + ox, py + oy, 3, 3);
            }
            break;

        
        case "SQUARE":
            int prevX = 0, prevY = 0;
            boolean hasPrev = false;

            for (int i = 0; i < 15; i++) {
                // Travel along projectile direction
                int px = (int)(x - i * 2 * Math.cos(angle));
                int py = (int)(y - i * 2 * Math.sin(angle));

                // Square wave value: +1 or -1
                double phase = (x + y + i) * 0.2;
                int squareVal = (Math.sin(phase) >= 0) ? 1 : -1;
                int offset = (int)(squareVal * (offsetAmt * .5 * 10));

                // Apply perpendicular offset
                int ox = (int)(offset * Math.cos(angle + Math.PI / 2));
                int oy = (int)(offset * Math.sin(angle + Math.PI / 2));

                int drawX = px + ox;
                int drawY = py + oy;

                if (hasPrev) {
                    // Connect to previous point so vertical jumps are drawn
                    g2.drawLine(prevX, prevY, drawX, drawY);
                }

                g2.fillRect(drawX, drawY, 3, 3);

                prevX = drawX;
                prevY = drawY;
                hasPrev = true;
            }
            break;

          
        case "SINE":
            for (int i = 0; i < 15; i++) {
                // Wave starts at current moving position
                int px = (int)(x - i * 2 * Math.cos(angle));
                int py = (int)(y - i * 2 * Math.sin(angle));

                // Offset perpendicular to direction
                int offset = (int)(Math.sin((x + y + i) * offsetAmt) * 14);

                int ox = (int)(offset * Math.cos(angle + Math.PI/2));
                int oy = (int)(offset * Math.sin(angle + Math.PI/2));

                g2.fillOval(px + ox, py + oy, 3, 3);
            }
            break;


        default:
            g2.fillOval(x - size / 2, y - size / 2, size, size);
    }

    }
}
