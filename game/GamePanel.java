package game;

import javax.swing.JPanel;
import javax.swing.AbstractAction;
import javax.swing.KeyStroke;
import javax.swing.InputMap;
import javax.swing.ActionMap;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Random;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseAdapter;



public class GamePanel extends JPanel implements KeyListener {
	
	double offsetAmt = 2.0;  // controls wave amplitude (range 0.1 – 2.0)
	ArrayList<Projectile> projectiles = new ArrayList<>();  // waves
	ArrayList<Star> stars = new ArrayList<>();
	ArrayList<Enemy> enemies = new ArrayList<>();
	ArrayList<EnemyShot> enemyShots = new ArrayList<>();
	ArrayList<Shard> shards = new ArrayList<>();
	ArrayList<PowerUp> powerUps = new ArrayList<>();
	ArrayList<SmokeParticle> smokes = new ArrayList<>();
	double waveT = 0;
	boolean wave1Active = false;
	int waveNumber = 1;  // current wave number (progresses as enemies are cleared)
	
	// firing rate (hold-to-fire)
	double lastFireNs = 0;
	double fireIntervalMs = 50;              // ~8 shots/sec; change to taste
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
    private final Random rng = new Random();
    private int hudTick = 0;  // increments every frame for HUD animations
    // Rolling 2-sec FPS stats
    private double[] fpsHistory = new double[240];  // ~2 sec at 120fps max
    private int fpsHistoryIdx = 0;
    private int fpsHistoryCount = 0;
    private double fpsAvg2s = 60, fpsMin2s = 60;
    private double fpsStatsTimer = 0;  // accumulates ms, recalculates every 200ms
    private int displayScore = 0;  // smoothly follows player.score
    private long lastScoreChangeTime = 0;  // for score pop animation


    // Game loop
    int FPS = 60;

    // Player
    Player player;
    boolean playerDead = false;
    boolean paused = false;
    boolean mouseDown = false;
    

    // Input
    boolean upPressed, downPressed, leftPressed, rightPressed, nPressed, bPressed, cPressed, controlPressed, plusPressed, minusPressed;
    
    // Gamepad support (graceful fallback if JAR unavailable)
    GamepadInput gamepadInput;
    // Gamepad state - polled fresh each frame (never gets stuck)
    float gpMoveX, gpMoveY;
    boolean gpFire, gpSwitch;
    boolean gpSwitchPrev;  // edge detection for gun cycling
    boolean gpStartPrev;   // edge detection for pause toggle
    boolean gpBackPrev;    // edge detection for quit
    // Volume slider state
    private boolean draggingSfxSlider = false;
    private boolean draggingMusicSlider = false;
    private static final int SL_TRACK_X = WIDTH - 280 - 30 + 55;
    private static final int SL_TRACK_W = 225;
    private static final int SL_SFX_Y = 52 + 22 + 38;
    private static final int SL_MUSIC_Y = 52 + 22 + 38 + 22;
    private static final int SL_H = 8;
    private static final int SL_HIT_PAD = 10;
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
        //blackHoles.add(new BlackHole(WIDTH-250, HEIGHT-250, 30));
        // generate stars
  
        player = new Player(WIDTH / 2, HEIGHT / 2, 40);
        spawnWave(waveNumber);
        for (int i = 0; i < NUMBEROFSTARS; i++) {  // number of stars
            stars.add(new Star(WIDTH, HEIGHT, Math.random()+.2)); 
            }
        for (BlackHole bh : blackHoles) {
            bh.update(WIDTH, HEIGHT);
        }
        
        // Initialize gamepad input (gracefully handles missing JAR)
        gamepadInput = new GamepadInput();
        if (gamepadInput.isAvailable()) {
            System.out.println("Gamepad support initialized");
        }

        // Mouse follows movement
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                double dx = e.getX() - player.x;
                double dy = e.getY() - player.y;
                player.angle = Math.toDegrees(Math.atan2(dy, dx));
            }
            
            @Override
            public void mouseDragged(MouseEvent e) {
                if (draggingSfxSlider || draggingMusicSlider) {
                    float val = (float)(e.getX() - SL_TRACK_X) / SL_TRACK_W;
                    val = Math.max(0f, Math.min(1f, val));
                    if (draggingSfxSlider) AudioManager.setSfxVolume(val);
                    if (draggingMusicSlider) AudioManager.setMusicVolume(val);
                    return;
                }
                // Update angle while dragging (firing) too
                double dx = e.getX() - player.x;
                double dy = e.getY() - player.y;
                player.angle = Math.toDegrees(Math.atan2(dy, dx));
            }
        });
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    int mx = e.getX(), my = e.getY();
                    if (mx >= SL_TRACK_X - 10 && mx <= SL_TRACK_X + SL_TRACK_W + 10) {
                        if (my >= SL_SFX_Y - SL_HIT_PAD && my <= SL_SFX_Y + SL_H + SL_HIT_PAD) {
                            draggingSfxSlider = true;
                            float val = (float)(mx - SL_TRACK_X) / SL_TRACK_W;
                            AudioManager.setSfxVolume(Math.max(0f, Math.min(1f, val)));
                            return;
                        }
                        if (my >= SL_MUSIC_Y - SL_HIT_PAD && my <= SL_MUSIC_Y + SL_H + SL_HIT_PAD) {
                            draggingMusicSlider = true;
                            float val = (float)(mx - SL_TRACK_X) / SL_TRACK_W;
                            AudioManager.setMusicVolume(Math.max(0f, Math.min(1f, val)));
                            return;
                        }
                    }
                    mouseDown = true;  // Left click - hold to fire
                } else if (e.getButton() == MouseEvent.BUTTON3) {
                    // Right click - switch weapon
                    Player.GunType[] guns = Player.GunType.values();
                    int next = (player.getGun().ordinal() + 1) % guns.length;
                    player.setGun(guns[next]);
                    AudioManager.playSfx("switchRay", 0.8f);
                }
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    draggingSfxSlider = false;
                    draggingMusicSlider = false;
                    mouseDown = false;  // Left click released
                }
            }
        });


    }


    public void update() {
    	double now = System.nanoTime();
    	frameMs = (now - lastFrameTime) / 1_000_000.0;
    	if (now - startTime > 2000000000) { 
    		maxFrame = Math.max(maxFrame, frameMs);
    	}
    	lastFrameTime = now;

    	// Record FPS sample into rolling buffer
    	double instantFps = (frameMs > 0) ? 1000.0 / frameMs : 0;
    	fpsHistory[fpsHistoryIdx] = instantFps;
    	fpsHistoryIdx = (fpsHistoryIdx + 1) % fpsHistory.length;
    	if (fpsHistoryCount < fpsHistory.length) fpsHistoryCount++;
    	// Recalculate stats every ~200ms to avoid jitter
    	fpsStatsTimer += frameMs;
    	if (fpsStatsTimer >= 200) {
    	    fpsStatsTimer = 0;
    	    // Only look back 2 seconds worth of frames
    	    int lookback = Math.min(fpsHistoryCount, (int)(2000.0 / Math.max(1, frameMs)));
    	    if (lookback < 2) lookback = fpsHistoryCount;
    	    double sum = 0, min = Double.MAX_VALUE;
    	    for (int i = 0; i < lookback; i++) {
    	        int idx = (fpsHistoryIdx - 1 - i + fpsHistory.length) % fpsHistory.length;
    	        double v = fpsHistory[idx];
    	        sum += v;
    	        if (v < min) min = v;
    	    }
    	    fpsAvg2s = sum / lookback;
    	    fpsMin2s = min;
    	}
    	
    	// Poll gamepad BEFORE pause/dead checks so Start always works
    	gpMoveX = 0; gpMoveY = 0; gpFire = false; gpSwitch = false;
    	if (gamepadInput != null && gamepadInput.isAvailable()) {
    	    gamepadInput.poll();
    	    
    	    // D-pad / left stick movement (per-frame, never sticky)
    	    float dpadX = gamepadInput.getDPadX();
    	    float dpadY = gamepadInput.getDPadY();
    	    if (dpadX < 0) gpMoveX = -1; else if (dpadX > 0) gpMoveX = 1;
    	    if (dpadY < 0) gpMoveY = -1; else if (dpadY > 0) gpMoveY = 1;
    	    
    	    // A = fire (per-frame)
    	    gpFire = gamepadInput.isAPressed();
    	    
    	    // B = cycle weapon (edge detection)
    	    boolean bNow = gamepadInput.isBPressed();
    	    if (bNow && !gpSwitchPrev) {
    	        Player.GunType[] guns = Player.GunType.values();
    	        int next = (player.getGun().ordinal() + 1) % guns.length;
    	        player.setGun(guns[next]);
    	        AudioManager.playSfx("switchRay", 0.8f);
    	    }
    	    gpSwitchPrev = bNow;
    	    
    	    // Start = pause toggle / restart (edge detection)
    	    boolean startNow = gamepadInput.isStartPressed();
    	    if (startNow && !gpStartPrev) {
    	        if (playerDead) { restartGame(); }
    	        else { paused = !paused; }
    	    }
    	    gpStartPrev = startNow;
    	    
    	    // Back = quit at game over (edge detection)
    	    boolean backNow = gamepadInput.isBackPressed();
    	    if (backNow && !gpBackPrev && playerDead) {
    	        System.exit(0);
    	    }
    	    gpBackPrev = backNow;
    	    
    	    // Triggers for gun angle rotation
    	    float lt = gamepadInput.getLeftTrigger();
    	    float rt = gamepadInput.getRightTrigger();
    	    if (lt > 0.1f) player.angle += lt * 5;
    	    if (rt > 0.1f) player.angle -= rt * 5;
    	    
    	    // Right stick for aiming
    	    float rsX = gamepadInput.getRightStickX();
    	    float rsY = gamepadInput.getRightStickY();
    	    if (Math.abs(rsX) > 0.1 || Math.abs(rsY) > 0.1) {
    	        player.angle = Math.toDegrees(Math.atan2(rsY, rsX));
    	    }
    	}
    	
    	if (paused) return;  // Skip update when paused
    	if (playerDead) return;
    	
    	double vx = 0, vy = 0, dx = 0, dy = 0;
    	boolean up = upPressed || gpMoveY < 0;
    	boolean down = downPressed || gpMoveY > 0;
    	boolean left = leftPressed || gpMoveX < 0;
    	boolean right = rightPressed || gpMoveX > 0;
    	if (up)    { vy -= 2; dy = -2; }
    	if (down)  { vy += 2; dy = 2; }
    	if (left)  { vx -= 2; dx = -2; }
    	if (right) { vx += 2; dx = 2; }

    	if (player.spinAngle > Math.PI * 2) {
    	    player.spinAngle -= Math.PI * 2; // keep it bounded
    	}
    	for (Star s : new ArrayList<>(stars)) {
    	    s.update(WIDTH, HEIGHT, vx, vy, player.getAngle());
    	    s.updateWithBlackHoles(blackHoles);
    	}
    	if (nPressed) player.angle += 1; // speed to taste
    	if (bPressed) player.angle -= 1;
    	player.updateMovement(up, down, left, right);
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

        // hold-to-fire with mouse
        if (mouseDown || gpFire) tryFire();
        
        // hold-to-fire with control key
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
                AudioManager.playSfx("switchRay", 0.8f);
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
                	AudioManager.playSfx("blackholehit", 0.8f);
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

                    // Spark effect when enemy is hit (electric-like sparks)
                    for (int spark = 0; spark < 4; spark++) {
                        double sparkAngle = (spark / 4.0) * 2 * Math.PI;
                        Shard sparkParticle = new Shard(e.getX(), e.getY(), sparkAngle, 2.0, new Color(200, 255, 255), 1);
                        shards.add(sparkParticle);
                    }
                    rings.add(new ParticleRing(e.getX(), e.getY(), 8));

                    // projectile behavior by gun type
                    switch (p.getGunType()) {
                        case TRIANGLE:
                            p.kill();
                            AudioManager.playSfx("hitTri", 4f);
                            break;

                        case SQUARE:
                            p.kill();
                            AudioManager.playSfx("hitSqr", 3f);
                            break;

                        case SINE:
                            p.incrementPierce();
                            AudioManager.playSfx("hitSin", 3f);
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
        
        // POSITION ENEMIES FIRST (before shooting phase)
        if (wave1Active) {
            waveT += 1;

            for (Enemy e : enemies) {
                if (!e.isAlive()) continue;

                double enemyT = waveT - e.getPathOffset();
                if (enemyT >= 0) {
                	Point p = EnemyPaths.getPath(e.getWaveType(), enemyT, WIDTH);
                    e.setPosition(p.x, p.y);
                    // Kill enemies that have completed enough cycles (escaped off-screen)
                    if (enemyT > 260 * 8) {  // ~8 passes across the screen
                        e.takeDamage(e.getHealth());
                    }
                }
                // Increment frame counter for staggered firing
                e.incrementFrameCounter();
            }
        }
        
        // ENEMY SHOOTING PHASE — consistent timed firing
        int phase = (int)(waveT / 30) % 3;
        if (wave1Active) {
            for (Enemy e : enemies) {
                // Check if enemy has advanced to new row/pass and reset shots
                e.updatePassNumber(waveT);
                
                if (!e.isAlive() || !e.canShoot()) continue;
                // Fire every 25 frames (steady rhythm per enemy)
                if (e.getFramesSinceRowStart() > 0 && e.getFramesSinceRowStart() % 25 == 0) {
                    // Only fire when on-screen
                    if (e.getX() > 50 && e.getX() < WIDTH - 50) {
                        Point tip = e.getGunTip();
                        enemyShots.add(new EnemyShot(tip.x, tip.y, e.getGunAngle()));
                        e.incrementShots();
                        AudioManager.playSfx("enemyShoot", 0.6f);
                    }
                }
            }
        }
        
        // update enemy shots and check player collision
        for (int i = enemyShots.size() - 1; i >= 0; i--) {
            EnemyShot s = enemyShots.get(i);
            s.update();
            if (!s.isAlive()) { enemyShots.remove(i); continue; }
            
            // Spawn tiny sparks trailing from enemy shot
            if (s.shouldSpawnSpark()) {
                double sparkAngle = Math.random() * Math.PI * 2;
                Shard sparkParticle = new Shard(s.getX(), s.getY(), sparkAngle, 0.5, new Color(200, 150, 255), 1);
                shards.add(sparkParticle);
            }
            
            // CHECK PLAYER HIT
            double dxP = player.getX() - s.getX();
            double dyP = player.getY() - s.getY();
            double hitR = player.radius + s.getRadius();
            if (dxP * dxP + dyP * dyP <= hitR * hitR) {
                player.takeDamage(10.0);
                player.applyKnockback(dxP, dyP, 5.0);
                AudioManager.playSfx("playerhit");
                // Spark effect on hit
                for (int spark = 0; spark < 6; spark++) {
                    double sparkAngle = (spark / 6.0) * 2 * Math.PI;
                    Shard sparkParticle = new Shard(s.getX(), s.getY(), sparkAngle, 7.0, new Color(255, 100, 200), 1);
                    shards.add(sparkParticle);
                }
                rings.add(new ParticleRing(s.getX(), s.getY(), 10));
                enemyShots.remove(i);
            }
        }
        
        // PLAYER ↔ ENEMY COLLISION
        for (Enemy e : enemies) {
            if (!e.isAlive()) continue;
            double dxE = player.getX() - e.getX();
            double dyE = player.getY() - e.getY();
            double hitR = player.radius + e.getRadius();
            if (dxE * dxE + dyE * dyE <= hitR * hitR) {
                // bounce player away, lose flat 50%, enemy explodes
                player.applyKnockback(dxE, dyE, 8.0);
                player.takeDamage(50.0);
                AudioManager.playSfx("playerhit");
                spawnEnemyExplosion(e.getX(), e.getY(), e.getRadius(), Player.GunType.TRIANGLE);
                e.takeDamage(e.getHealth());
            }
        }

        // Wave progression: if all enemies dead, spawn next wave
        boolean anyAlive = false;
        for (Enemy e : enemies) { if (e.isAlive()) { anyAlive = true; break; } }
        if (!anyAlive && !playerDead && enemies.size() > 0) {
            waveNumber++;
            spawnWave(waveNumber);
        }

        // Player death: spawn a massive explosion once when health reaches 0
        if (player.getHealth() <= 0 && !playerDead) {
            spawnPlayerExplosion(player.getX(), player.getY(), player.radius);
            playerDead = true;
            // clear active threats so explosion feels final
            enemyShots.clear();
            enemies.clear();
            projectiles.clear();
            AudioManager.playSfx("explosion", 1.0f);
            AudioManager.playSfx("glassbreak", 1.0f);
        }
        
        // update rings and cull dead ones
        for (int i = rings.size() - 1; i >= 0; i--) {
            ParticleRing r = rings.get(i);
            r.update();
            if (!r.isAlive()) rings.remove(i);
        }
        // update shards
        for (int i = shards.size() - 1; i >= 0; i--) {
            Shard s = shards.get(i);
            s.update();
            // Check if shard should split into smaller fragments
            if (s.shouldSplit()) {
                s.markSplitDone();
                // Spawn 4 smaller shards around the parent in random directions
                for (int j = 0; j < 4; j++) {
                    double angle = Math.random() * 2 * Math.PI;  // Random direction
                    Shard child = new Shard(s.getX(), s.getY(), angle, 6.0, new Color(255, 180, 50), 1);
                    shards.add(child);
                }
                rings.add(new ParticleRing(s.getX(), s.getY(), 12));
            }
            if (!s.isAlive()) shards.remove(i);
        }
        // update smoke
        for (int i = smokes.size() - 1; i >= 0; i--) {
            SmokeParticle sp = smokes.get(i);
            sp.update();
            if (!sp.isAlive()) smokes.remove(i);
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
            //playGunSound(WIDTH);
        	switch (player.getGun() ) {
        		case TRIANGLE:
        			AudioManager.playSfx("shootTri");
        		case SQUARE:
        			AudioManager.playSfx("shootSqr");
        		case SINE:
        			AudioManager.playSfx("shootSin");
        			
        			
        	
        	}
            
            lastFireNs = nowNs;
        }
    }
    private void spawnWave(int wave) {
        enemies.clear();
        waveT = 0;
        wave1Active = true;

        int radius = 22;
        double health = 5.0 + wave * 2.0;
        int formation = ((wave - 1) % 5) + 1;

        int rows, cols, pathType;
        switch (formation) {
            case 1: rows = 2; cols = 5;  pathType = 1; break;  // small sine
            case 2: rows = 3; cols = 5;  pathType = 1; break;  // medium sine
            case 3: rows = 2; cols = 6;  pathType = 2; break;  // figure-8
            case 4: rows = 3; cols = 4;  pathType = 3; break;  // diagonal dive
            case 5: rows = 3; cols = 6;  pathType = 1; break;  // bigger sine
            default: rows = 2; cols = 5;  pathType = 1; break;
        }

        for (int i = 0; i < cols; i++) {
            for (int j = 0; j < rows; j++) {
                double offset = (j * 260) + (i * 20);
                enemies.add(new Enemy(WIDTH + 100, 120, radius, health, offset, pathType));
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
        final ArrayList<Shard> shardsSnap       = new ArrayList<>(shards);
        final ArrayList<SmokeParticle> smokesSnap = new ArrayList<>(smokes);
        final ArrayList<EnemyShot> enemyShotsSnap = new ArrayList<>(enemyShots);

        for (Star s : starsSnap) {
            s.draw(g2, holesSnap);
        }

        // Draw player only if alive
        if (!playerDead) {
            player.draw(g2);
        }

        for (Projectile p : projsSnap) {
            p.draw(g2);
        }

        // Draw enemy shots
        for (EnemyShot es : enemyShotsSnap) {
            es.draw(g2);
        }

        for (BlackHole bh : holesSnap) {
            bh.draw(g2);
        }

        for (ParticleRing ring : ringsSnap) {
            ring.draw(g2);
        }

        for (Shard shard : shardsSnap) {
            shard.draw(g2);
        }

        for (SmokeParticle smoke : smokesSnap) {
            smoke.draw(g2);
        }

        for (Enemy e : enemiesSnap) {
            if (e.isAlive()) {
                e.draw(g2);
                // Draw health bar for recently hit enemies with fade effect
                long timeSinceHit = System.currentTimeMillis() - e.getLastHitTime();
                if (timeSinceHit < 1500) {  // Show for 1.5 seconds with fade
                    double healthRatio = e.getHealth() / e.getMaxHealth();  // Accurate health ratio
                    // Calculate fade alpha: full opacity for first 500ms, then fade to 0 by 1500ms
                    double fadeAlpha = Math.max(0, Math.min(1.0, 1.0 - (timeSinceHit - 500.0) / 1000.0));
                    drawGradientHealthBar(g2, (int)e.getX() - 12, (int)e.getY() - 30, 24, 4, healthRatio, false, fadeAlpha);
                }
            }
        }

        // ========== HUD TICK ==========
        hudTick++;

        // ========== SCORE ANIMATION ==========
        if (displayScore < player.score) {
            int diff = player.score - displayScore;
            displayScore += Math.max(1, diff / 5);
            if (displayScore > player.score) displayScore = player.score;
        }
        if (displayScore != player.score || (player.score > 0 && lastScoreChangeTime == 0)) {
            lastScoreChangeTime = System.currentTimeMillis();
        }

        // ========== PLAYER HEALTH BAR (fancy) ==========
        double healthRatio = player.getHealth() / 100.0;
        int barWidth = 280;
        int barHeight = 22;
        int barX = WIDTH - barWidth - 30;
        int barY = 52;
        drawFancyHealthBar(g2, barX, barY, barWidth, barHeight, healthRatio);

        // ========== SCORE (above health bar) ==========
        {
            long timeSinceScore = System.currentTimeMillis() - lastScoreChangeTime;
            double popScale = 1.0;
            int glowAlpha = 0;
            if (timeSinceScore < 400) {
                double t = timeSinceScore / 400.0;
                popScale = 1.0 + 0.3 * (1.0 - t) * (1.0 - t);
                glowAlpha = (int)(200 * (1.0 - t));
            }
            String scoreStr = String.valueOf(displayScore);
            Font scoreFont = new Font("Arial", Font.BOLD, (int)(28 * popScale));
            g2.setFont(scoreFont);
            FontMetrics sfm = g2.getFontMetrics();
            int scoreX = barX + barWidth - sfm.stringWidth(scoreStr);
            int scoreY = barY - 10;
            if (glowAlpha > 0) {
                g2.setColor(new Color(100, 220, 255, glowAlpha / 3));
                g2.fillRoundRect(scoreX - 8, scoreY - sfm.getAscent() - 4, sfm.stringWidth(scoreStr) + 16, sfm.getHeight() + 8, 10, 10);
                g2.setColor(new Color(150, 240, 255, glowAlpha));
                g2.drawString(scoreStr, scoreX, scoreY);
            }
            g2.setColor(new Color(0, 0, 0, 120));
            g2.drawString(scoreStr, scoreX + 2, scoreY + 2);
            g2.setColor(new Color(220, 240, 255));
            g2.drawString(scoreStr, scoreX, scoreY);
            g2.setFont(new Font("Arial", Font.PLAIN, 13));
            g2.setColor(new Color(140, 180, 200));
            g2.drawString("SCORE", scoreX - g2.getFontMetrics().stringWidth("SCORE") - 8, scoreY);
        }

        // ========== WAVE BADGE (left of health bar) ==========
        {
            g2.setFont(new Font("Arial", Font.BOLD, 18));
            g2.setColor(new Color(180, 200, 220));
            String waveStr = "WAVE " + waveNumber;
            g2.drawString(waveStr, barX, barY + barHeight + 20);
        }

        // ========== VOLUME SLIDERS ==========
        {
            int slTrackX = barX + 55;
            int slTrackW = barWidth - 55;
            int slH = 8;
            int sfxSlY = barY + barHeight + 38;
            int musSlY = sfxSlY + 22;
            drawVolumeSlider(g2, "SFX", barX, sfxSlY, slTrackX, slTrackW, slH, AudioManager.getSfxVolume());
            drawVolumeSlider(g2, "MUSIC", barX, musSlY, slTrackX, slTrackW, slH, AudioManager.getMusicVolume());
        }

        // ========== DEBUG STATS (upper left, larger font) ==========
        g2.setFont(new Font("Consolas", Font.PLAIN, 18));
        g2.setColor(new Color(180, 200, 180, 200));
        int statY = 24;
        int statGap = 22;
        g2.drawString("Enemies: " + enemies.size(), 20, statY); statY += statGap;
        g2.drawString("FPS: " + String.format("%.0f", 1000 / frameMs)
            + "  avg: " + String.format("%.0f", fpsAvg2s)
            + "  low: " + String.format("%.0f", fpsMin2s), 20, statY); statY += statGap;
        g2.drawString("Wave: " + waveNumber, 20, statY);
        
        // Draw pause overlay if paused
        if (paused && !playerDead) {
            g2.setColor(new Color(0, 0, 0, 150));
            g2.fillRect(0, 0, WIDTH, HEIGHT);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Arial", Font.BOLD, 48));
            FontMetrics fm = g2.getFontMetrics();
            String pauseText = "PAUSED";
            int x = (WIDTH - fm.stringWidth(pauseText)) / 2;
            int y = (HEIGHT / 2) - 40;
            g2.drawString(pauseText, x, y);
            g2.setFont(new Font("Arial", Font.PLAIN, 24));
            fm = g2.getFontMetrics();
            String resumeText = "Press P to resume";
            x = (WIDTH - fm.stringWidth(resumeText)) / 2;
            g2.drawString(resumeText, x, y + 60);
        }
        
        // Draw game over screen if player is dead
        if (playerDead) {
            g2.setColor(new Color(0, 0, 0, 200));
            g2.fillRect(0, 0, WIDTH, HEIGHT);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Arial", Font.BOLD, 64));
            FontMetrics fm = g2.getFontMetrics();
            String gameOverText = "GAME OVER";
            int x = (WIDTH - fm.stringWidth(gameOverText)) / 2;
            int y = (HEIGHT / 2) - 80;
            g2.drawString(gameOverText, x, y);
            
            g2.setFont(new Font("Arial", Font.PLAIN, 32));
            fm = g2.getFontMetrics();
            String scoreText = "Final Score: " + player.score;
            x = (WIDTH - fm.stringWidth(scoreText)) / 2;
            g2.drawString(scoreText, x, y + 80);
            
            g2.setFont(new Font("Arial", Font.PLAIN, 24));
            fm = g2.getFontMetrics();
            String optionsText = "R to Restart  |  Q to Quit";
            x = (WIDTH - fm.stringWidth(optionsText)) / 2;
            g2.drawString(optionsText, x, y + 150);
        }
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
            case KeyEvent.VK_P: paused = !paused; break; // Toggle pause
            case KeyEvent.VK_R: if (playerDead) restartGame(); break; // Restart on game over
            case KeyEvent.VK_Q: if (playerDead) System.exit(0); break; // Quit on game over
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
    player.score = player.score + 50;
    AudioManager.playSfx("explosion");
    AudioManager.playSfx("glassbreak");
    // shards flying out (reduced by half)
    for (int i = 0; i < 4; i++) {
        double ang = rng.nextDouble() * Math.PI * 2;
        double spd = 1 + rng.nextDouble() * 3;
        shards.add(new Shard(x, y, ang, spd, new Color(255, 200, 50)));
    }
    // light smoke wisps (reduced by half)
    for (int i = 0; i < 2; i++) {
        double sx = x + (rng.nextDouble() - 0.5) * radius;
        double sy = y + (rng.nextDouble() - 0.5) * radius;
        smokes.add(new SmokeParticle(sx, sy));
    }
    // optional extra rings for stronger feel
    if (gun == Player.GunType.TRIANGLE) {
        rings.add(new ParticleRing(x, y, radius + 8));
        rings.add(new ParticleRing(x, y, radius + 16));
    } else if (gun == Player.GunType.SQUARE) {
        rings.add(new ParticleRing(x, y, radius + 6));
    }
}

private void restartGame() {
        playerDead = false;
        paused = false;
        waveNumber = 1;
        displayScore = 0;
        lastScoreChangeTime = 0;
        player = new Player(WIDTH / 2, HEIGHT / 2, 40);
        projectiles.clear();
        enemies.clear();
        enemyShots.clear();
        rings.clear();
        shards.clear();
        smokes.clear();
        blackHoles.clear();
        spawnWave(waveNumber);
    }

private void spawnPlayerExplosion(double x, double y, int radius) {
    // large rings (reduced by half)
    for (int i = 0; i < 2; i++) {
        rings.add(new ParticleRing(x, y, radius + i * 12));
    }
    // many shards (reduced by half)
    for (int i = 0; i < 30; i++) {
        double ang = rng.nextDouble() * Math.PI * 2;
        double spd = 2 + rng.nextDouble() * 6;
        shards.add(new Shard(x, y, ang, spd, new Color(255, 220, 80)));
    }
    // heavy smoke (reduced by half)
    for (int i = 0; i < 16; i++) {
        double sx = x + (rng.nextDouble() - 0.5) * radius * 3;
        double sy = y + (rng.nextDouble() - 0.5) * radius * 3;
        smokes.add(new SmokeParticle(sx, sy));
    }
}

private void drawGradientHealthBar(Graphics2D g2, int x, int y, int width, int height, double ratio, boolean isPlayer, double alpha) {
    // Clamp ratio between 0 and 1
    ratio = Math.max(0, Math.min(1, ratio));
    alpha = Math.max(0, Math.min(1, alpha));
    
    // Clamp alpha to range 0-255 for color components
    int alphaInt = (int)(alpha * 255);
    
    // Determine base glow color based on ratio
    int r, g, b;
    if (ratio <= 0.5) {
        // Red to Yellow: 0.0 -> 0.5
        float t = (float)(ratio * 2);  // 0.0 to 1.0
        r = 255;
        g = (int)(255 * t);
        b = 0;
    } else {
        // Yellow to Green: 0.5 -> 1.0
        float t = (float)((ratio - 0.5) * 2);  // 0.0 to 1.0
        r = (int)(255 * (1 - t));
        g = 255;
        b = 0;
    }
    
    // Draw glow effect (semi-transparent background layer)
    g2.setColor(new Color(r, g, b, (int)(60 * alpha)));
    g2.fillRect(x - 2, y - 2, width + 4, height + 4);
    
    // Draw dark background
    g2.setColor(new Color(40, 40, 40, (int)(255 * alpha)));
    g2.fillRect(x, y, width, height);
    
    // Calculate fill width
    int fillWidth = (int)(width * ratio);
    
    // Draw filled portion with glow
    if (fillWidth > 0) {
        // Main bright bar
        g2.setColor(new Color(r, g, b, alphaInt));
        g2.fillRect(x, y, fillWidth, height);
        
        // Bright inner glow (edge highlight)
        g2.setColor(new Color(Math.min(255, r + 50), Math.min(255, g + 50), b, (int)(180 * alpha)));
        g2.fillRect(x, y, fillWidth, Math.max(1, height / 3));
    }
    
    // Draw border
    g2.setColor(new Color(200, 200, 200, (int)(200 * alpha)));
    g2.setStroke(new BasicStroke(1.0f));
    g2.drawRect(x, y, width, height);
}

private void drawFancyHealthBar(Graphics2D g2, int x, int y, int width, int height, double ratio) {
    ratio = Math.max(0, Math.min(1, ratio));
    double pulse = 0.85 + 0.15 * Math.sin(hudTick * 0.08);

    int r, gr, b;
    if (ratio > 0.5) {
        double t = (ratio - 0.5) * 2;
        r = (int)(255 * (1 - t) * 0.8);
        gr = (int)(200 + 55 * t);
        b = (int)(180 + 75 * t);
    } else {
        double t = ratio * 2;
        r = 255;
        gr = (int)(200 * t);
        b = (int)(50 * t);
    }

    for (int i = 3; i >= 1; i--) {
        int ga = (int)(30 * pulse * (1.0 - i / 4.0));
        g2.setColor(new Color(r, gr, b, ga));
        g2.fillRoundRect(x - i * 2, y - i * 2, width + i * 4, height + i * 4, 8, 8);
    }

    g2.setColor(new Color(15, 20, 25, 220));
    g2.fillRoundRect(x, y, width, height, 6, 6);

    int numSegments = 20;
    g2.setStroke(new BasicStroke(1.0f));
    for (int i = 1; i < numSegments; i++) {
        int sx = x + (int)(width * i / (double) numSegments);
        g2.setColor(new Color(60, 70, 80, 80));
        g2.drawLine(sx, y + 1, sx, y + height - 1);
    }

    int fillWidth = (int)(width * ratio);
    if (fillWidth > 0) {
        GradientPaint gp = new GradientPaint(x, y,
            new Color((int)(r * 0.6), (int)(gr * 0.6), (int)(b * 0.6)),
            x + fillWidth, y,
            new Color(Math.min(255, r), Math.min(255, gr), Math.min(255, b)));
        g2.setPaint(gp);
        g2.fillRoundRect(x + 1, y + 1, fillWidth - 1, height - 2, 5, 5);

        g2.setColor(new Color(255, 255, 255, (int)(60 * pulse)));
        g2.fillRoundRect(x + 2, y + 2, fillWidth - 3, height / 3, 3, 3);

        if (fillWidth > 4) {
            int tipW = Math.min(8, fillWidth);
            for (int i = 0; i < tipW; i++) {
                int a = (int)(180 * pulse * (1.0 - i / (double) tipW));
                g2.setColor(new Color(Math.min(255, r + 80), Math.min(255, gr + 80), Math.min(255, b + 80), a));
                g2.drawLine(x + fillWidth - i, y + 1, x + fillWidth - i, y + height - 2);
            }
        }

        double sweepT = (hudTick % 120) / 120.0;
        int sweepX = x + (int)(width * sweepT);
        int sweepW = 30;
        if (sweepX < x + fillWidth) {
            for (int i = 0; i < sweepW; i++) {
                int sx2 = sweepX + i;
                if (sx2 >= x && sx2 <= x + fillWidth) {
                    double si = Math.sin(i * Math.PI / sweepW);
                    int a = (int)(50 * si);
                    g2.setColor(new Color(255, 255, 255, a));
                    g2.drawLine(sx2, y + 1, sx2, y + height - 2);
                }
            }
        }

        if (ratio < 0.25) {
            double warn = 0.5 + 0.5 * Math.sin(hudTick * 0.2);
            g2.setColor(new Color(255, 30, 30, (int)(50 * warn)));
            g2.fillRoundRect(x, y, fillWidth, height, 5, 5);
        }
    }

    g2.setStroke(new BasicStroke(1.5f));
    g2.setColor(new Color(130, 160, 180, (int)(180 * pulse)));
    g2.drawRoundRect(x, y, width, height, 6, 6);

    g2.setFont(new Font("Arial", Font.BOLD, 14));
    g2.setColor(new Color(130, 170, 200, 200));
    g2.drawString("HP", x - 28, y + height - 5);
}

private void drawVolumeSlider(Graphics2D g2, String label, int labelX, int y, int trackX, int trackW, int h, float value) {
    g2.setFont(new Font("Consolas", Font.BOLD, 12));
    g2.setColor(new Color(140, 180, 200));
    g2.drawString(label, labelX, y + h / 2 + 4);

    g2.setColor(new Color(30, 40, 50, 180));
    g2.fillRoundRect(trackX, y, trackW, h, h, h);

    int fillW = (int)(trackW * value);
    if (fillW > 0) {
        g2.setPaint(new GradientPaint(trackX, y, new Color(60, 180, 220), trackX + fillW, y, new Color(40, 140, 200)));
        g2.fillRoundRect(trackX, y, fillW, h, h, h);
    }

    g2.setColor(new Color(80, 120, 140, 120));
    g2.drawRoundRect(trackX, y, trackW, h, h, h);

    int handleX = trackX + fillW;
    int handleR = 6;
    g2.setColor(new Color(200, 230, 255));
    g2.fillOval(handleX - handleR, y + h / 2 - handleR, handleR * 2, handleR * 2);
    g2.setColor(new Color(100, 180, 220));
    g2.drawOval(handleX - handleR, y + h / 2 - handleR, handleR * 2, handleR * 2);

    g2.setFont(new Font("Consolas", Font.PLAIN, 11));
    g2.setColor(new Color(160, 200, 220));
    g2.drawString((int)(value * 100) + "%", trackX + trackW + 8, y + h / 2 + 4);
}
}
