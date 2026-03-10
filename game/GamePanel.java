package game;

import javax.swing.JPanel;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseAdapter;



public class GamePanel extends JPanel implements KeyListener {
	
	double offsetAmt = 2.0;  // controls wave amplitude (range 0.1 – 2.0)
	ArrayList<Projectile> projectiles = new ArrayList<>();  // waves
	ArrayList<Star> stars = new ArrayList<>();
	ArrayList<Enemy> enemies = new ArrayList<>();
	double waveT = 0;
	boolean wave1Active = false;
	
	// firing rate (hold-to-fire)
	double lastFireNs = 0;
	double fireIntervalMs = 100;              // ~8 shots/sec; change to taste
	int dx = 0, dy = 0; // player movement direction
	public static final int WIDTH = 1920;
	public static final int HEIGHT = 1080;
	public static final int NUMBEROFSTARS = 200;
	private double lastGunSwitch = 0;
    private final long gunSwitchDelay = 50; // ms between switches
    public ArrayList<BlackHole> blackHoles = new ArrayList<>();
    public static ArrayList<ParticleRing> rings = new ArrayList<>();
    
    private double lastFrameTime = System.nanoTime();
    private double startTime = System.nanoTime();
    private double frameMs = 0, maxFrame = 16;


    // Game loop
    int FPS = 60;

    // Player
    Player player;
    

    // Input
    boolean upPressed, downPressed, leftPressed, rightPressed, nPressed, bPressed, cPressed, controlPressed, plusPressed, minusPressed;
  
    public GamePanel() {
    	this.setPreferredSize(new Dimension(WIDTH, HEIGHT));
        this.setBackground(Color.BLACK);
        this.setDoubleBuffered(true);
        this.addKeyListener(this);
        this.setFocusable(true);
        setOpaque(true);
        setBackground(Color.BLACK);
        setDoubleBuffered(true);
        
        
        javax.swing.Timer timer = new javax.swing.Timer(16, e -> {
            update();
            repaint();
        });
        timer.setCoalesce(false);
        timer.start();
     // Example: make one in the middle of the screen
        blackHoles.add(new BlackHole(WIDTH-250, HEIGHT-250, 30));
        // generate stars
  
        player = new Player(WIDTH / 2, HEIGHT / 2, 40);
        spawnWave1();
        for (int i = 0; i < NUMBEROFSTARS; i++) {  // number of stars
            stars.add(new Star(WIDTH, HEIGHT, Math.random()+.2)); 
            }
        for (BlackHole bh : blackHoles) {
            bh.update(WIDTH, HEIGHT);
        }
        

        // Mouse follows movement
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                double dx = e.getX() - player.x;
                double dy = e.getY() - player.y;
                player.angle = Math.toDegrees(Math.atan2(dy, dx));
            }
        });
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
               	tryFire();
                }
        });


    }


    public void update() {
    	double now = System.nanoTime();
    	frameMs = (now - lastFrameTime) / 1_000_000.0;
    	if (now - startTime > 2000000000) { 
    		maxFrame = Math.max(maxFrame, frameMs);
    		System.out.println(startTime + "    Now:  " +  (now - startTime) + "    max:   " + maxFrame + "FrameMS:" + frameMs);
    	}
    	lastFrameTime = now;
    	
    	
    	double vx = 0, vy = 0, dx = 0, dy = 0;
    	if (upPressed)   { vy -= 2; dy = -2; }
    	if (downPressed) { vy += 2; dy = 2; }
    	if (leftPressed) { vx -= 2; dx = -2; }
    	if (rightPressed) { vx += 2; dx = 2; }

    	if (player.spinAngle > Math.PI * 2) {
    	    player.spinAngle -= Math.PI * 2; // keep it bounded
    	}
    	for (Star s : stars) {
    	    s.update(WIDTH, HEIGHT, vx, vy, player.getAngle());
    	    s.updateWithBlackHoles(blackHoles);
    	}
    	if (nPressed) player.angle += 1; // speed to taste
    	if (bPressed) player.angle -= 1;
    	player.updateMovement(upPressed, downPressed, leftPressed, rightPressed);
        player.update(); // for spin
       /* for (int i = 0; i < projectiles.size(); i++) {
            Projectile p = projectiles.get(i);
            p.update();
            if (p.isOffscreen(WIDTH, HEIGHT)) {
                projectiles.remove(i--); // remove and adjust index
            }
        }*/

    	// update "roll angle" if moving
    	if (vx != 0 || vy != 0) {
    	    player.rollAngle = Math.atan2(vy, vx);
    	 // increase roll offset based on movement speed
    	    player.rollOffset += Math.sqrt(vx*vx + vy*vy) * 0.2;
    	}

        // hold-to-fire
        if (controlPressed) tryFire();

        // continuous + / - while held
        if (plusPressed) {
            player.offsetAmt = Math.min(3.0, player.offsetAmt + 0.01);
        }
        if (minusPressed) {
            player.offsetAmt = Math.max(0.1, player.offsetAmt - 0.01);
        }
               
        // inside update()
        if (cPressed) {
            now = System.currentTimeMillis();
            if (now - lastGunSwitch > gunSwitchDelay) {
                Player.GunType[] guns = Player.GunType.values();
                int next = (player.getGun().ordinal() + 1) % guns.length;
                player.setGun(guns[next]);
                lastGunSwitch = now;
            }
        }

        // move projectiles
        for (int i = 0; i < projectiles.size(); i++) {
            projectiles.get(i).update();
            // (optional) remove if off screen:
            if (projectiles.get(i).isOffscreen(WIDTH, HEIGHT)) { projectiles.remove(i--); }
        }
     // in update()
     // update black holes (this decays flashAlpha/flashTimer!)
        for (BlackHole bh : blackHoles) {
            bh.update(WIDTH, HEIGHT);
        }
     // --- PROJECTILE ↔ BLACK HOLE COLLISIONS ---
        for (int i = projectiles.size() - 1; i >= 0; i--) {
            Projectile p = projectiles.get(i);
            if (!p.isAlive()) { projectiles.remove(i); continue; }

            for (BlackHole bh : blackHoles) {
                double hitDx = p.getX() - bh.getX();
                double hitDy = p.getY() - bh.getY();
                double dist2 = hitDx*hitDx + hitDy*hitDy;

                double hitR = bh.getRadius() + p.getRadius();
                if (dist2 <= hitR * hitR) {
                    // Hit! Apply effect based on the current gun
                    Player.GunType gun = player.getGun();
                    if (gun == null) gun = player.getGun(); // fallback, just in case
                    switch (gun) {
                        case TRIANGLE: { // heavy damage + knockback + big rim ring
                            bh.applyDamage(1.2);                         // tune
                            bh.applyKnockback(dx, dy, 0.9);              // push away from impact
                            bh.flash();
                            rings.add(new ParticleRing(bh.getX(), bh.getY(), (int)Math.round(bh.getRadius())));
                            p.kill();                                     // triangle rounds stop on hit
                            break;
                        }
                        case SQUARE: {   // split on hit (shrapnel) + light damage
                            bh.applyDamage(0.45);
                            bh.flash();
                            rings.add(new ParticleRing(bh.getX(), bh.getY(), (int)Math.round(bh.getRadius())));
                            // spawn 4 children at 45° steps (smaller, faster)
                            for (int k = 0; k < 4; k++) {
                                double ang = Math.atan2(p.getDy(), p.getDx()) + Math.toRadians(45 * k);
                                double spd = Math.hypot(p.getDx(), p.getDy()) * 1.15;
                                projectiles.add(Projectile.childShard(
                                    p.getX(), p.getY(), Math.cos(ang)*spd, Math.sin(ang)*spd, p.getRadius()*0.6
                                ));
                            }
                            p.kill();
                            break;
                        }
                        case SINE: {     // pierce + slow debuff + tiny damage
                            bh.applyDamage(0.2);
                            bh.applySlow(28, 0.55);                       // ~28 frames at 55% speed
                            bh.flash();
                            // SINE bullets **pierce**: allow a limited pierce count
                            p.incrementPierce();
                            if (p.getPierceCount() >= 3) p.kill();
                            break;
                        }
                    }
                    // we handled one BH; no double-count this frame
                    break;
                }
            }
        }
        for (int i = projectiles.size() - 1; i >= 0; i--) {
            Projectile p = projectiles.get(i);
            if (!p.isAlive()) {
                projectiles.remove(i);
                continue;
            }

            for (Enemy e : enemies) {
                if (!e.isAlive()) continue;

                double dx2 = p.getX() - e.getX();
                double dy2 = p.getY() - e.getY();
                double hitR = p.getRadius() + e.getRadius();

                if (dx2 * dx2 + dy2 * dy2 <= hitR * hitR) {
                    double dmg = getProjectileDamage(p);
                    e.takeDamage(dmg);

                    // projectile behavior by gun type
                    switch (p.getGunType()) {
                        case TRIANGLE:
                            p.kill();
                            break;

                        case SQUARE:
                            p.kill();
                            break;

                        case SINE:
                            p.incrementPierce();
                            if (p.getPierceCount() >= 3) {
                                p.kill();
                            }
                            break;
                    }

                    // enemy death effect
                    if (!e.isAlive()) {
                        spawnEnemyExplosion(e.getX(), e.getY(), e.getRadius(), p.getGunType());
                    }

                    break;
                }
            }
        }
        if (wave1Active) {
            waveT += 1;

            for (Enemy e : enemies) {
                if (!e.isAlive()) continue;

                double enemyT = waveT - e.getPathOffset();
                if (enemyT >= 0) {
                	Point p = EnemyPaths.getWave1Path(enemyT, WIDTH);
                    e.setPosition(p.x, p.y);
                }
            }
        }
        // update rings and cull dead ones
        for (int i = rings.size() - 1; i >= 0; i--) {
            ParticleRing r = rings.get(i);
            r.update();
            if (!r.isAlive()) rings.remove(i);
        }
        

       
    }
   
    public void playGunSound(int screenWidth) {
        // Map offsetAmt to MIDI velocity (volume)
        int velocity = (int)(player.offsetAmt * 30); 
        
        // Map x position (0 → screenWidth) → pan (0–127)
        int pan = (int) ((double) player.x / WIDTH * 127);
        MidiSynth.setPan(pan);
        
        // pick the scale array based on gun
        int[] notes;
        switch (player.getGun()) {
            case SINE:     notes = MidiSynth.HIJAZ; break;
            case SQUARE:   notes = MidiSynth.PHRYGIAN_DOMINANT; break;
            case TRIANGLE: notes = MidiSynth.ARABIC; break;
            default:       notes = MidiSynth.PHRYGIAN_DOMINANT; break;
        }

        // Map player.y (0..HEIGHT) → index in the scale array (0..notes.length-1)
        int idx = (int)((double) player.y / HEIGHT * notes.length);
        idx = Math.max(0, Math.min(notes.length - 1, idx)); // clamp

        // Get the actual MIDI pitch
        int pitch = notes[idx] + (int)(Math.random() * 1 - 0)-12;

        int brightness = (int)(player.offsetAmt * 64); // scale to 0–127
        
        
        MidiSynth.setBrightness(brightness);
        
        switch (player.getGun()) {
        case SINE:
            SoundManager.playTone(300, 7, SoundManager.WaveType.SINE);
            break;
        case SQUARE:
        	SoundManager.playTone(300, 7, SoundManager.WaveType.SQUARE);
            break;
        case TRIANGLE:
        	SoundManager.playTone(300, 10, SoundManager.WaveType.TRIANGLE);
            break;
    }
    }

    private void tryFire() {
        long nowNs = System.nanoTime();
        if ((nowNs - lastFireNs) / 1_000_000 >= fireIntervalMs) {
        	double rad = Math.toRadians(player.getAngle());
        	double spawnOffset = 36.0;

        	double startX = player.getX() + (player.radius + spawnOffset) * Math.cos(rad);
        	double startY = player.getY() + (player.radius + spawnOffset) * Math.sin(rad);

        	projectiles.add(new Projectile(startX, startY, rad, player.getGun(), player.offsetAmt));
            playGunSound(WIDTH);
            lastFireNs = nowNs;
        }
    }
    private void spawnWave1() {
        enemies.clear();
        waveT = 0;
        wave1Active = true;

        int radius = 22;
        double health = 8.0;
        
        for (int i = 0; i < 8; i++) {
        	for (int j = 0; j < 4; j++) {
        		
        		enemies.add(new Enemy(WIDTH + 100, 120, radius, health, (j * 260) + (i * 20)));
        	}
        	
        }
        
  
    }
    public static Point safeRandomPoint(int width, int height, java.util.List<BlackHole> holes) {
        final int margin = 20;
        final int maxTries = 60;
        final double EXCLUDE_FACTOR = 1.4;   // exclude inside 1.4 * radius
        final double BUFFER = 12.0;          // small pad outside the rim

        for (int i = 0; i < maxTries; i++) {
            double x = (Math.random() * (width  + margin * 2)) - margin;
            double y = (Math.random() * (height + margin * 2)) - margin;

            boolean ok = true;
            for (BlackHole bh : holes) {
                double dx = x - bh.getX();
                double dy = y - bh.getY();
                double d  = Math.hypot(dx, dy);
                double minD = bh.getRadius() * EXCLUDE_FACTOR + BUFFER;
                if (d < minD) { ok = false; break; }
            }
            if (ok) return new Point((int)Math.round(x), (int)Math.round(y));
        }

        // Fallback: spawn along an edge farthest from the largest BH
        if (!holes.isEmpty()) {
            BlackHole biggest = holes.get(0);
            for (BlackHole bh : holes) if (bh.getRadius() > biggest.getRadius()) biggest = bh;

            double bestX = 0, bestY = 0, bestD = -1;
            int[][] edges = { {0, height/2}, {width, height/2}, {width/2, 0}, {width/2, height} };
            for (int[] e : edges) {
                double d = Math.hypot(e[0] - biggest.getX(), e[1] - biggest.getY());
                if (d > bestD) { bestD = d; bestX = e[0]; bestY = e[1]; }
            }
            return new Point((int)bestX, (int)bestY);
        }

        // No holes? Just random.
        int x = (int)(Math.random() * width);
        int y = (int)(Math.random() * height);
        return new Point(x, y);
    }
    
    @Override

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        final ArrayList<Star> starsSnap         = new ArrayList<>(stars);
        final ArrayList<Projectile> projsSnap   = new ArrayList<>(projectiles);
        final ArrayList<BlackHole> holesSnap    = new ArrayList<>(blackHoles);
        final ArrayList<ParticleRing> ringsSnap = new ArrayList<>(rings);
        final ArrayList<Enemy> enemiesSnap      = new ArrayList<>(enemies);

        for (Star s : starsSnap) {
            s.draw(g2, holesSnap);
        }

        player.draw(g2);

        for (Projectile p : projsSnap) {
            p.draw(g2);
        }

        for (BlackHole bh : holesSnap) {
            bh.draw(g2);
        }

        for (ParticleRing ring : ringsSnap) {
            ring.draw(g2);
        }

        for (Enemy e : enemiesSnap) {
            if (e.isAlive()) {
                e.draw(g2);
            }
        }

        g2.setColor(Color.WHITE);
        g2.drawString("Offset: " + String.format("%.2f", player.offsetAmt), 20, 20);
        g2.drawString("Frame ms: " + String.format("%.2f", frameMs), 20, 120);
        g2.drawString("Minimum FPS: " + String.format("%.2f", 1000 / maxFrame), 20, 140);
        g2.drawString("FPS: " + String.format("%.2f", 1000 / frameMs), 20, 100);
        g2.drawString("Gun Angle: " + String.format("%.2f", player.getAngle()) + "°", 20, 60);
        g2.drawString("Projectiles: " + projectiles.size(), 20, 80);
        g2.drawString("Enemies: " + enemies.size(), 20, 40);
    }

    // Input handling
    @Override
    public void keyTyped(KeyEvent e) {}

    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_LEFT:   leftPressed  = true; break;
            case KeyEvent.VK_RIGHT:  rightPressed = true; break;
            case KeyEvent.VK_UP:     upPressed    = true; break;
            case KeyEvent.VK_DOWN:   downPressed  = true; break;
            case KeyEvent.VK_N:      nPressed     = true; break;
            case KeyEvent.VK_B:      bPressed     = true; break;
            case KeyEvent.VK_C:      cPressed     = true; break;
            case KeyEvent.VK_CONTROL:  controlPressed = true; break;
            case KeyEvent.VK_EQUALS: plusPressed  = true; break;   // main keyboard '+'
            case KeyEvent.VK_ADD:    plusPressed  = true; break;   // numpad '+'
            case KeyEvent.VK_MINUS:  minusPressed = true; break;   // main keyboard '-'
            case KeyEvent.VK_SUBTRACT: minusPressed = true; break; // numpad '-'
            case KeyEvent.VK_1: player.setGun(Player.GunType.TRIANGLE); break;
            case KeyEvent.VK_2: player.setGun(Player.GunType.SQUARE);   break;
            case KeyEvent.VK_3: player.setGun(Player.GunType.SINE);     break;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_LEFT:   leftPressed  = false; break;
            case KeyEvent.VK_RIGHT:  rightPressed = false; break;
            case KeyEvent.VK_CONTROL:  controlPressed = false; break;
            case KeyEvent.VK_UP:     upPressed    = false; break;
            case KeyEvent.VK_DOWN:   downPressed  = false; break;
            case KeyEvent.VK_N:      nPressed     = false; break;
            case KeyEvent.VK_B:      bPressed     = false; break;
            case KeyEvent.VK_C:      cPressed     = false; break;
            case KeyEvent.VK_EQUALS:
            case KeyEvent.VK_ADD:    plusPressed  = false; break;
            case KeyEvent.VK_MINUS:
            case KeyEvent.VK_SUBTRACT: minusPressed = false; break;
        }
    }


private double getProjectileDamage(Projectile p) {
    double power = p.getOffsetAmt();   // assuming Projectile has this getter
    Player.GunType gun = p.getGunType();

    switch (gun) {
        case TRIANGLE:
            return 1.6 * power;   // big hit
        case SQUARE:
            return 1.0 * power;   // medium
        case SINE:
            return 0.65 * power;  // lighter, better for multi-hit behavior later
        default:
            return 1.0 * power;
    }
}
private void spawnEnemyExplosion(double x, double y, int radius, Player.GunType gun) {
    rings.add(new ParticleRing(x, y, radius));

    // optional extra rings for stronger feel
    if (gun == Player.GunType.TRIANGLE) {
        rings.add(new ParticleRing(x, y, radius + 8));
        rings.add(new ParticleRing(x, y, radius + 16));
    } else if (gun == Player.GunType.SQUARE) {
        rings.add(new ParticleRing(x, y, radius + 6));
    }
}
}
