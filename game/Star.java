package game;

import java.awt.*;

public class Star {
    int x, y;
    int size;
    double speed;       // parallax factor (0.25 .. 1.0)
    Color core, glow;

    public Star(int width, int height, double speed) {
        final int margin = 20;
        this.x = (int)(Math.random() * (width  + margin * 2)) - margin;
        this.y = (int)(Math.random() * (height + margin * 2)) - margin;
        this.speed = speed;

        // star size based on speed (closer = bigger/brighter)
        this.size = (int)(1 + speed * 2);
        System.out.println("Speed" + speed);
        

        switch ((int)(Math.random() * 9)) {
            case 0: case 5: core = Color.CYAN;    glow = new Color(0, 255, 255, 120); break;
            case 1: case 6: core = Color.MAGENTA; glow = new Color(255, 0, 255, 120); break;
            case 2: core = Color.YELLOW;  glow = new Color(255, 255, 100, 120); break;
            case 3: case 7: case 8: case 9: core = Color.WHITE;    glow = new Color(255, 255, 255, 120); break;
            case 4: core = Color.PINK;    glow = new Color(255, 255, 255, 120); break;
            default:core = Color.GREEN;   glow = new Color(100, 255, 100, 120); break;
        }
    }

    public void update(int width, int height, double playerVX, double playerVY, int coneDeg) {
        // Always move opposite to player velocity + constant draft
    	
        x -= (int)Math.round(playerVX * speed * .25);
        y -= (int)Math.round(playerVY * speed * .25);
        
        // draft: opposite of player aim
        double angleRad = Math.toRadians(coneDeg);
        double dx = Math.cos(angleRad);
        double dy = Math.sin(angleRad);

        double draftStrength = 5; // tweak
        x -= dx * speed * draftStrength;
        y -= dy * speed * draftStrength;

        // Add a steady drift downward to keep stars flowing
       // x += speed * 5;   // subtle leftward motion
       // y += (int)Math.round(Math.random()) * speed;   // faster downward drift5;

        // Wrap around edges with margin
        final int margin = 20;
        final int spanX  = width  + margin * 2;
        final int spanY  = height + margin * 2;

        if (x < -margin)          x += spanX;
        else if (x > width+margin)  x -= spanX;

        if (y < -margin)          y += spanY;
        else if (y > height+margin) y -= spanY;
    }

    public void draw(Graphics2D g2) {
        g2.setColor(glow);
        g2.fillOval(x - size, y - size, size*2, size*2);
        g2.setColor(core);
        g2.fillOval(x, y, size, size);
    }
}
