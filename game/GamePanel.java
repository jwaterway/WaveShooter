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
	ArrayList<HomingMissile> missiles = new ArrayList<>();
	ArrayList<Asteroid> asteroids = new ArrayList<>();
	ArrayList<Nebula> nebulae = new ArrayList<>();
	BossEnemy boss = null;
	ArrayList<LaserEnemy> laserEnemies = new ArrayList<>();
	ArrayList<SmokeParticle> smokes = new ArrayList<>();
	ArrayList<SpaceStructure> spaceStructures = new ArrayList<>();
	ArrayList<PhaseEnemy> phaseEnemies = new ArrayList<>();
	DecoyField activeDecoy = null;  // deployed decoy that attracts missiles
	ArrayList<DeployedGrenade> deployedGrenades = new ArrayList<>();  // blinking grenades
	// Overworld map
	boolean showingMap = true;       // start on the map screen
	OverworldMap overworldMap = new OverworldMap();
	// Screen shake
	private int shakeTimer = 0;
	private double shakeIntensity = 0;
	// Mega grenade flash
	private int grenadeFlashTimer = 0;
	// Satellite boss (level 2)
	SatelliteBoss satelliteBoss = null;
	// Moon background (level 3)
	MoonBackground moonBackground = null;
	// Camera for scrolling levels
	Camera camera = new Camera(WIDTH, HEIGHT);
	double waveT = 0;
	boolean wave1Active = false;
	int waveNumber = 1;  // current wave number (progresses as enemies are cleared)
	int currentLevel = 1;
	// Level-complete cinematic state machine
	int cinematicPhase = 0;   // 0 = normal play, 1-5 = cinematic stages
	int cinematicTimer = 0;   // frame counter within current phase
	
	// firing rate (hold-to-fire)
	double lastFireNs = 0;
	double fireIntervalMs = 100;             // starting fire rate; improved by Voltage powerups
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
    boolean gpYPrev;       // edge detection for decoy deploy
    boolean gpXPrev;       // edge detection for grenade deploy
    // Gamepad map navigation edge detection
    int gpMapDirXPrev;     // previous D-pad X direction for map
    int gpMapDirYPrev;     // previous D-pad Y direction for map
    // Volume slider state
    private boolean draggingSfxSlider = false;
    private boolean draggingMusicSlider = false;
    // Engine sound
    private EngineSound engineSound = new EngineSound();
    private static final int SL_TRACK_X = WIDTH - 280 - 30 + 55;
    private static final int SL_TRACK_W = 225;
    private static final int SL_SFX_Y = 52 + 22 + 150;   // must match drawing Y
    private static final int SL_MUSIC_Y = 52 + 22 + 150 + 22;
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
        engineSound.start();
        // Don't spawn wave yet — player starts on the map
        for (int i = 0; i < NUMBEROFSTARS; i++) {  // number of stars
            stars.add(new Star(WIDTH, HEIGHT, Math.random()+.2)); 
            }
        for (BlackHole bh : blackHoles) {
            bh.update(WIDTH, HEIGHT);
        }
        
        // Initialize gamepad input (gracefully handles missing JAR)
        gamepadInput = new GamepadInput();
        if (gamepadInput.isAvailable()) {
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

    	// Record FPS sample into rolling buffer (clamp to sane range)
    	double instantFps = (frameMs > 1) ? 1000.0 / frameMs : fpsAvg2s;
    	fpsHistory[fpsHistoryIdx] = instantFps;
    	fpsHistoryIdx = (fpsHistoryIdx + 1) % fpsHistory.length;
    	if (fpsHistoryCount < fpsHistory.length) fpsHistoryCount++;
    	// Recalculate stats every ~200ms to avoid jitter
    	fpsStatsTimer += frameMs;
    	if (fpsStatsTimer >= 200) {
    	    fpsStatsTimer = 0;
    	    // Fixed lookback: ~2 seconds of frames at 60fps
    	    int lookback = Math.min(fpsHistoryCount, 120);
    	    double sum = 0, min = Double.MAX_VALUE;
    	    for (int i = 0; i < lookback; i++) {
    	        int idx = (fpsHistoryIdx - 1 - i + fpsHistory.length) % fpsHistory.length;
    	        double v = fpsHistory[idx];
    	        sum += v;
    	        if (v < min) min = v;
    	    }
    	    fpsAvg2s = sum / lookback;
    	    // Min only decreases — sticks at the lowest seen
    	    if (min < fpsMin2s) fpsMin2s = min;
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
    	    
    	    // Y = deploy decoy (edge detection)
    	    boolean yNow = gamepadInput.isYPressed();
    	    if (yNow && !gpYPrev && !showingMap) {
    	        if (activeDecoy == null && player.useDecoy()) {
    	            activeDecoy = new DecoyField(player.getX(), player.getY());
    	            AudioManager.playSfx("powerup7", 0.8f);
    	        }
    	    }
    	    gpYPrev = yNow;
    	    
    	    // X = deploy grenade (edge detection)
    	    boolean xNow = gamepadInput.isXPressed();
    	    if (xNow && !gpXPrev && !showingMap) {
    	        if (player.useGrenade()) {
    	            deployedGrenades.add(new DeployedGrenade(player.getX(), player.getY()));
    	            AudioManager.playSfx("powerup8", 0.8f);
    	        }
    	    }
    	    gpXPrev = xNow;
    	    
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

    	// Screen shake / grenade flash timers (run even during cinematic)
    	if (shakeTimer > 0) { shakeTimer--; shakeIntensity *= 0.92; }
    	if (grenadeFlashTimer > 0) grenadeFlashTimer--;

    	// Overworld map update
    	if (showingMap) {
    	    // Gamepad D-pad map navigation (edge detection)
    	    if (gpMoveX > 0 && gpMapDirXPrev <= 0) overworldMap.navigate(1);
    	    if (gpMoveX < 0 && gpMapDirXPrev >= 0) overworldMap.navigate(-1);
    	    if (gpMoveY > 0 && gpMapDirYPrev <= 0) overworldMap.navigateVertical(1);
    	    if (gpMoveY < 0 && gpMapDirYPrev >= 0) overworldMap.navigateVertical(-1);
    	    gpMapDirXPrev = (int) gpMoveX;
    	    gpMapDirYPrev = (int) gpMoveY;
    	    // Gamepad A = launch selected level
    	    if (gpFire && !gpSwitchPrev) {
    	        launchSelectedLevel();
    	    }
    	    overworldMap.update();
    	    return;
    	}

    	// Level-complete cinematic update
    	if (cinematicPhase > 0) {
    	    updateCinematic();
    	    return;
    	}
    	
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
    	    // When camera is enabled, stars drift laterally based on player position
    	    if (camera.isEnabled()) {
    	        double screenCenter = WIDTH / 2.0;
    	        double offset = (player.getX() - screenCenter) / screenCenter; // -1..+1
    	        s.setLateralDrift(-offset * 5.0); // stars drift opposite to player
    	    } else {
    	        s.setLateralDrift(0);
    	    }
    	    s.update(WIDTH, HEIGHT);
    	    s.updateWithBlackHoles(blackHoles);
    	}
    	if (moonBackground != null) moonBackground.update();
    	camera.update(player.getX(), player.getY());
    	if (nPressed) player.angle += 1; // speed to taste
    	if (bPressed) player.angle -= 1;
    	player.updateMovement(up, down, left, right);
        player.update(); // for spin
        player.updateForcefield();
        player.fireFlash *= 0.9;  // decay firing flash
        if (player.fireFlash < 0.01) player.fireFlash = 0;
        engineSound.setSpeedRatio(player.getSpeed() / player.maxSpeed);
        engineSound.setVolume(AudioManager.getMasterVolume() * AudioManager.getSfxVolume() * 0.8f);
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

        // Update asteroids
        for (int i = asteroids.size() - 1; i >= 0; i--) {
            asteroids.get(i).update(WIDTH, HEIGHT);
            if (!asteroids.get(i).isAlive()) asteroids.remove(i);
        }

        // Update nebulae
        for (int i = nebulae.size() - 1; i >= 0; i--) {
            nebulae.get(i).update(WIDTH, HEIGHT);
            if (!nebulae.get(i).isAlive()) nebulae.remove(i);
        }

        // Update space structures
        for (int i = spaceStructures.size() - 1; i >= 0; i--) {
            spaceStructures.get(i).update(WIDTH, HEIGHT);
            if (!spaceStructures.get(i).isAlive()) spaceStructures.remove(i);
        }

        // Player slowdown inside nebula
        boolean inNebula = false;
        for (Nebula n : nebulae) {
            if (n.contains(player.getX(), player.getY())) {
                inNebula = true;
                break;
            }
        }
        player.setSpeedMultiplier(inNebula ? 0.6 : 1.0);
     // --- PROJECTILE ↔ ASTEROID COLLISIONS ---
        for (int i = projectiles.size() - 1; i >= 0; i--) {
            Projectile p = projectiles.get(i);
            if (!p.isAlive()) continue;
            for (int j = asteroids.size() - 1; j >= 0; j--) {
                Asteroid a = asteroids.get(j);
                double adx = p.getX() - a.getX();
                double ady = p.getY() - a.getY();
                double aHitR = p.getRadius() + a.getRadius();
                if (adx * adx + ady * ady <= aHitR * aHitR) {
                    a.kill();
                    p.kill();
                    player.score += 25;
                    AudioManager.playSfx("explosion");
                    rings.add(new ParticleRing(a.getX(), a.getY(), a.getRadius()));
                    // Spawn debris shards
                    for (int k = 0; k < 5; k++) {
                        double ang = rng.nextDouble() * Math.PI * 2;
                        double spd = 1.5 + rng.nextDouble() * 2.5;
                        shards.add(new Shard(a.getX(), a.getY(), ang, spd, new Color(140, 120, 90)));
                    }
                    // Split into smaller asteroids
                    asteroids.addAll(a.split());
                    break;
                }
            }
        }

        // --- PLAYER ↔ ASTEROID COLLISIONS ---
        for (int i = asteroids.size() - 1; i >= 0; i--) {
            Asteroid a = asteroids.get(i);
            double adx = player.getX() - a.getX();
            double ady = player.getY() - a.getY();
            double aHitR = player.radius + a.getRadius();
            if (adx * adx + ady * ady <= aHitR * aHitR) {
                if (player.hasForcefield()) {
                    spawnForcefieldAbsorb(a.getX(), a.getY());
                } else {
                    player.takeDamage(15.0);
                    player.applyKnockback(adx, ady, 6.0);
                    AudioManager.playSfx("playerhit");
                }
                rings.add(new ParticleRing(a.getX(), a.getY(), a.getRadius()));
                for (int k = 0; k < 4; k++) {
                    double ang = rng.nextDouble() * Math.PI * 2;
                    shards.add(new Shard(a.getX(), a.getY(), ang, 2.0, new Color(140, 120, 90), 1));
                }
                a.kill();
                asteroids.addAll(a.split());
            }
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
                    e.setPosition(p.x, p.y + e.getRowYOffset());
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
                if (player.hasForcefield()) {
                    // Forcefield absorbs — electric burst
                    spawnForcefieldAbsorb(s.getX(), s.getY());
                } else {
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
                }
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
                if (player.hasForcefield()) {
                    // Forcefield destroys enemy on contact
                    spawnForcefieldAbsorb(e.getX(), e.getY());
                    spawnEnemyExplosion(e.getX(), e.getY(), e.getRadius(), Player.GunType.TRIANGLE);
                    e.takeDamage(e.getHealth());
                    player.applyKnockback(dxE, dyE, 3.0);
                } else {
                    // bounce player away, lose flat 50%, enemy explodes
                    player.applyKnockback(dxE, dyE, 8.0);
                    player.takeDamage(50.0);
                    AudioManager.playSfx("playerhit");
                    spawnEnemyExplosion(e.getX(), e.getY(), e.getRadius(), Player.GunType.TRIANGLE);
                    e.takeDamage(e.getHealth());
                }
            }
        }

        // Update power-ups and check collection
        for (int i = powerUps.size() - 1; i >= 0; i--) {
            PowerUp pu = powerUps.get(i);
            pu.update();
            if (!pu.isAlive()) { powerUps.remove(i); continue; }
            double dxPU = player.getX() - pu.getX();
            double dyPU = player.getY() - pu.getY();
            if (dxPU * dxPU + dyPU * dyPU <= (player.radius + 18) * (player.radius + 18)) {
                AudioManager.playSfx(pu.getSoundKey(), 0.8f);
                // Apply effect
                if (pu.getType() == PowerUp.Type.HEALTH) {
                    player.heal(25.0);
                } else if (pu.getType() == PowerUp.Type.SHIELD) {
                    player.addForcefield();  // +10 sec, stacks
                } else if (pu.getType() == PowerUp.Type.DECOY) {
                    // Add decoy charge to inventory
                    player.addDecoy();
                } else if (pu.getType() == PowerUp.Type.GRENADE) {
                    // Add grenade charge to inventory
                    player.addGrenade();
                } else if (pu.getType() == PowerUp.Type.WEAPON_BOOST) {
                    if (player.getVoltageLevel() < Player.MAX_VOLTAGE) {
                        player.addVoltage();
                        // Recalculate fire interval: 100ms → 20ms over MAX_VOLTAGE steps
                        fireIntervalMs = 100.0 - (80.0 * player.getVoltageLevel() / Player.MAX_VOLTAGE);
                    }
                } else if (pu.getType() == PowerUp.Type.SPEED) {
                    player.increaseSpeed();
                } else if (pu.getType() == PowerUp.Type.WEAPON_SINE) {
                    player.setGun(Player.GunType.SINE);
                } else if (pu.getType() == PowerUp.Type.WEAPON_SQUARE) {
                    player.setGun(Player.GunType.SQUARE);
                } else if (pu.getType() == PowerUp.Type.WEAPON_TRIANGLE) {
                    player.setGun(Player.GunType.TRIANGLE);
                }
                rings.add(new ParticleRing(pu.getX(), pu.getY(), 16));
                powerUps.remove(i);
            }
        }

        // Update boss
        if (boss != null && boss.isAlive()) {
            boss.update(player.getX(), player.getY(), missiles);
        }

        // Update decoy field
        if (activeDecoy != null) {
            activeDecoy.update();
            if (!activeDecoy.isAlive()) {
                // Decoy expired — electric burst
                rings.add(new ParticleRing(activeDecoy.getX(), activeDecoy.getY(), 30));
                for (int sp = 0; sp < 8; sp++) {
                    double ang = (sp / 8.0) * Math.PI * 2;
                    shards.add(new Shard(activeDecoy.getX(), activeDecoy.getY(), ang, 3.0, new Color(100, 220, 255), 1));
                }
                AudioManager.playSfx("explosion", 0.4f);
                activeDecoy = null;
            }
        }

        // Update deployed grenades (blinking fuse → detonate)
        for (int i = deployedGrenades.size() - 1; i >= 0; i--) {
            DeployedGrenade dg = deployedGrenades.get(i);
            if (dg.update()) {
                // Fuse expired — detonate at grenade position
                detonateMegaGrenade(dg.getX(), dg.getY());
            }
            if (!dg.isAlive()) deployedGrenades.remove(i);
        }

        // Update homing missiles and check player collision
        // If decoy is active, missiles home toward the decoy instead
        double missileTargetX = (activeDecoy != null) ? activeDecoy.getX() : player.getX();
        double missileTargetY = (activeDecoy != null) ? activeDecoy.getY() : player.getY();
        for (int i = missiles.size() - 1; i >= 0; i--) {
            HomingMissile m = missiles.get(i);
            m.update(missileTargetX, missileTargetY);
            if (!m.isAlive()) {
                // Spawn explosion if missile expired naturally (fizzed out)
                if (m.hasExpired()) {
                    rings.add(new ParticleRing(m.getX(), m.getY(), 18));
                    rings.add(new ParticleRing(m.getX(), m.getY(), 10));
                    for (int sp = 0; sp < 10; sp++) {
                        double sparkAngle = (sp / 10.0) * 2 * Math.PI + Math.random() * 0.3;
                        double spd = 2 + Math.random() * 4;
                        shards.add(new Shard(m.getX(), m.getY(), sparkAngle, spd, new Color(255, 160, 40), 1));
                    }
                    for (int sp = 0; sp < 3; sp++) {
                        double sx = m.getX() + (Math.random() - 0.5) * 16;
                        double sy = m.getY() + (Math.random() - 0.5) * 16;
                        smokes.add(new SmokeParticle(sx, sy));
                    }
                    AudioManager.playSfx("explosion");
                }
                missiles.remove(i); continue;
            }
            double dxM = player.getX() - m.getX();
            double dyM = player.getY() - m.getY();
            double hitRM = player.radius + HomingMissile.RADIUS;
            if (dxM * dxM + dyM * dyM <= hitRM * hitRM) {
                if (player.hasForcefield()) {
                    spawnForcefieldAbsorb(m.getX(), m.getY());
                } else {
                    player.takeDamage(40.0);
                    player.applyKnockback(dxM, dyM, 8.0);
                    AudioManager.playSfx("playerhit");
                    rings.add(new ParticleRing(m.getX(), m.getY(), 14));
                    for (int sp = 0; sp < 6; sp++) {
                        double sparkAngle = (sp / 6.0) * 2 * Math.PI;
                        shards.add(new Shard(m.getX(), m.getY(), sparkAngle, 5.0, new Color(255, 140, 50), 1));
                    }
                }
                missiles.remove(i);
            }
            // Missile ↔ Decoy collision — decoy absorbs missile with electric burst
            else if (activeDecoy != null) {
                double ddx = activeDecoy.getX() - m.getX();
                double ddy = activeDecoy.getY() - m.getY();
                double hitRD = activeDecoy.getRadius() + HomingMissile.RADIUS;
                if (ddx * ddx + ddy * ddy <= hitRD * hitRD) {
                    rings.add(new ParticleRing(m.getX(), m.getY(), 16));
                    for (int sp = 0; sp < 8; sp++) {
                        double sparkAngle = (sp / 8.0) * 2 * Math.PI;
                        shards.add(new Shard(m.getX(), m.getY(), sparkAngle, 4.0, new Color(100, 220, 255), 1));
                    }
                    AudioManager.playSfx("explosion", 0.5f);
                    missiles.remove(i);
                }
            }
        }

        // Projectile vs boss collision
        if (boss != null && boss.isAlive()) {
            for (int i = projectiles.size() - 1; i >= 0; i--) {
                Projectile p = projectiles.get(i);
                if (!p.isAlive()) continue;
                double dxB = p.getX() - boss.getX();
                double dyB = p.getY() - boss.getY();
                double hitRB = p.getRadius() + boss.getRadius();
                if (dxB * dxB + dyB * dyB <= hitRB * hitRB) {
                    double dmg = getProjectileDamage(p);
                    boss.takeDamage(dmg);
                    rings.add(new ParticleRing(p.getX(), p.getY(), 8));
                    p.kill();
                    if (!boss.isAlive()) {
                        spawnBossExplosion(boss.getX(), boss.getY(), boss.getRadius());
                    }
                }
            }
        }

        // Update laser enemies
        for (int i = laserEnemies.size() - 1; i >= 0; i--) {
            LaserEnemy le = laserEnemies.get(i);
            le.update(player.getX(), player.getY());
            if (!le.isAlive()) {
                // Death explosion
                rings.add(new ParticleRing(le.getX(), le.getY(), le.getRadius() + 8));
                rings.add(new ParticleRing(le.getX(), le.getY(), le.getRadius() + 20));
                for (int sp = 0; sp < 12; sp++) {
                    double ang = (sp / 12.0) * Math.PI * 2;
                    shards.add(new Shard(le.getX(), le.getY(), ang, 3 + rng.nextDouble() * 4, new Color(255, 100, 200), 1));
                }
                AudioManager.playSfx("explosion");
                player.score += 150;
                laserEnemies.remove(i);
                continue;
            }
            // Laser beam ↔ player collision
            if (le.isFiring() && le.isBeamHitting(player.getX(), player.getY(), player.radius)) {
                if (player.hasForcefield()) {
                    // Forcefield absorbs — small sparks each frame
                    if (rng.nextInt(8) == 0) spawnForcefieldAbsorb(player.getX(), player.getY());
                } else {
                    player.takeDamage(0.5); // continuous damage per frame (~30 dps)
                    if (rng.nextInt(10) == 0) {
                        AudioManager.playSfx("playerhit");
                        rings.add(new ParticleRing(player.getX(), player.getY(), 8));
                    }
                }
            }
        }

        // Projectile ↔ Laser Enemy collision
        for (int i = projectiles.size() - 1; i >= 0; i--) {
            Projectile p = projectiles.get(i);
            if (!p.isAlive()) continue;
            for (LaserEnemy le : laserEnemies) {
                if (!le.isAlive()) continue;
                double ldx = p.getX() - le.getX();
                double ldy = p.getY() - le.getY();
                double lHitR = p.getRadius() + le.getRadius();
                if (ldx * ldx + ldy * ldy <= lHitR * lHitR) {
                    double dmg = getProjectileDamage(p);
                    le.takeDamage(dmg);
                    rings.add(new ParticleRing(p.getX(), p.getY(), 8));
                    for (int sp = 0; sp < 3; sp++) {
                        double ang = rng.nextDouble() * Math.PI * 2;
                        shards.add(new Shard(le.getX(), le.getY(), ang, 2.0, new Color(255, 100, 200), 1));
                    }
                    switch (p.getGunType()) {
                        case TRIANGLE: p.kill(); break;
                        case SQUARE: p.kill(); break;
                        case SINE: p.incrementPierce(); if (p.getPierceCount() >= 3) p.kill(); break;
                    }
                    break;
                }
            }
        }

        // Update phase enemies (level 2+)
        for (int i = phaseEnemies.size() - 1; i >= 0; i--) {
            PhaseEnemy pe = phaseEnemies.get(i);
            pe.update(player.getX(), player.getY(), enemyShots);
            if (!pe.isAlive()) {
                rings.add(new ParticleRing(pe.getX(), pe.getY(), pe.getRadius() + 10));
                for (int sp = 0; sp < 10; sp++) {
                    double ang = (sp / 10.0) * Math.PI * 2;
                    shards.add(new Shard(pe.getX(), pe.getY(), ang, 3 + rng.nextDouble() * 4,
                        new Color(180, 80, 255), 1));
                }
                AudioManager.playSfx("explosion");
                player.score += 200;
                if (rng.nextInt(4) == 0) {
                    PowerUp.Type[] types = PowerUp.Type.values();
                    powerUps.add(new PowerUp(pe.getX(), pe.getY(), types[rng.nextInt(types.length)]));
                }
                phaseEnemies.remove(i);
                continue;
            }
        }

        // Projectile ↔ Phase Enemy collision
        for (int i = projectiles.size() - 1; i >= 0; i--) {
            Projectile p = projectiles.get(i);
            if (!p.isAlive()) continue;
            for (PhaseEnemy pe : phaseEnemies) {
                if (!pe.isAlive() || pe.getPhaseAlpha() < 0.4) continue;
                double pdx = p.getX() - pe.getX();
                double pdy = p.getY() - pe.getY();
                double pHitR = p.getRadius() + pe.getRadius();
                if (pdx * pdx + pdy * pdy <= pHitR * pHitR) {
                    double dmg = getProjectileDamage(p);
                    pe.takeDamage(dmg);
                    rings.add(new ParticleRing(p.getX(), p.getY(), 8));
                    switch (p.getGunType()) {
                        case TRIANGLE: p.kill(); break;
                        case SQUARE: p.kill(); break;
                        case SINE: p.incrementPierce(); if (p.getPierceCount() >= 3) p.kill(); break;
                    }
                    break;
                }
            }
        }

        // Update satellite boss
        if (satelliteBoss != null && satelliteBoss.isAlive()) {
            satelliteBoss.update(player.getX(), player.getY(), missiles, enemyShots);
            // Satellite laser ↔ player collision
            if (satelliteBoss.isInLaserBeam(player.getX(), player.getY(), player.radius)) {
                if (player.hasForcefield()) {
                    if (rng.nextInt(8) == 0) spawnForcefieldAbsorb(player.getX(), player.getY());
                } else {
                    player.takeDamage(0.6);
                    if (rng.nextInt(10) == 0) {
                        AudioManager.playSfx("playerhit");
                        rings.add(new ParticleRing(player.getX(), player.getY(), 8));
                    }
                }
            }
        }

        // Projectile ↔ Satellite Boss collision
        if (satelliteBoss != null && satelliteBoss.isAlive()) {
            for (int i = projectiles.size() - 1; i >= 0; i--) {
                Projectile p = projectiles.get(i);
                if (!p.isAlive()) continue;
                if (satelliteBoss.damageAt(p.getX(), p.getY(), getProjectileDamage(p))) {
                    rings.add(new ParticleRing(p.getX(), p.getY(), 8));
                    switch (p.getGunType()) {
                        case TRIANGLE: p.kill(); break;
                        case SQUARE: p.kill(); break;
                        case SINE: p.incrementPierce(); if (p.getPierceCount() >= 3) p.kill(); break;
                    }
                    if (!satelliteBoss.isAlive()) {
                        spawnBossExplosion(satelliteBoss.getX(), satelliteBoss.getY(), 120);
                        player.score += 1000;
                    }
                }
            }
        }

        // Wave progression: if all enemies dead (and boss dead/absent), spawn next wave
        boolean anyAlive = false;
        for (Enemy e : enemies) { if (e.isAlive()) { anyAlive = true; break; } }
        boolean bossAlive = (boss != null && boss.isAlive());
        boolean laserAlive = false;
        for (LaserEnemy le : laserEnemies) { if (le.isAlive()) { laserAlive = true; break; } }
        boolean phaseAlive = false;
        for (PhaseEnemy pe : phaseEnemies) { if (pe.isAlive()) { phaseAlive = true; break; } }
        boolean satBossAlive = (satelliteBoss != null && satelliteBoss.isAlive() && waveNumber >= 10);
        if (!anyAlive && !bossAlive && !laserAlive && !phaseAlive && !satBossAlive && !playerDead && enemies.size() > 0) {
            if (waveNumber >= 10 && cinematicPhase == 0) {
                // Level complete — start cinematic
                cinematicPhase = 1;
                cinematicTimer = 0;
                enemies.clear();
                enemyShots.clear();
                missiles.clear();
                asteroids.clear();
                phaseEnemies.clear();
            } else {
                waveNumber++;
                spawnWave(waveNumber);
            }
        }

        // Player death: spawn a massive explosion once when health reaches 0
        if (player.getHealth() <= 0 && !playerDead) {
            spawnPlayerExplosion(player.getX(), player.getY(), player.radius);
            playerDead = true;
            // clear active threats so explosion feels final
            enemyShots.clear();
            enemies.clear();
            projectiles.clear();
            missiles.clear();
            laserEnemies.clear();
            phaseEnemies.clear();
            activeDecoy = null;
            AudioManager.playSfx("explosion", 1.0f);
            AudioManager.playSfx("glassbreak", 1.0f);
            engineSound.setSpeedRatio(0);
        }
        // Cap particle rings to prevent GPU-melting gradient spam
        while (rings.size() > 15) rings.remove(0);
        for (int i = rings.size() - 1; i >= 0; i--) {
            ParticleRing r = rings.get(i);
            r.update();
            if (!r.isAlive()) rings.remove(i);
        }
        // update shards (capped to prevent runaway growth)
        for (int i = shards.size() - 1; i >= 0; i--) {
            Shard s = shards.get(i);
            s.update();
            // Check if shard should split — only if under cap
            if (s.shouldSplit()) {
                s.markSplitDone();
                if (shards.size() < 200) {
                    for (int j = 0; j < 4; j++) {
                        double angle = Math.random() * 2 * Math.PI;
                        Shard child = new Shard(s.getX(), s.getY(), angle, 6.0, new Color(255, 180, 50), 1);
                        shards.add(child);
                    }
                }
                if (rings.size() < 15) {
                    rings.add(new ParticleRing(s.getX(), s.getY(), 12));
                }
            }
            if (!s.isAlive()) shards.remove(i);
        }
        // update smoke (capped)
        while (smokes.size() > 80) smokes.remove(0);
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
            player.fireFlash = 1.0;
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
        missiles.clear();
        waveT = 0;
        wave1Active = true;

        int radius = 22;
        double health = 3.0 + wave * 1.5;
        int formation = ((wave - 1) % 5) + 1;

        int rows, cols, pathType;
        switch (formation) {
            case 1: rows = 2; cols = 3;  pathType = 1; break;  // small sine (fewer)
            case 2: rows = 2; cols = 4;  pathType = 1; break;  // medium sine
            case 3: rows = 2; cols = 5;  pathType = 2; break;  // figure-8
            case 4: rows = 3; cols = 4;  pathType = 3; break;  // diagonal dive
            case 5: rows = 3; cols = 5;  pathType = 1; break;  // bigger sine
            default: rows = 2; cols = 3;  pathType = 1; break;
        }

        double maxOffset = 0;
        for (int i = 0; i < cols; i++) {
            for (int j = 0; j < rows; j++) {
                double offset = (i * 50);
                if (offset > maxOffset) maxOffset = offset;
                Enemy e = new Enemy(WIDTH + 100, 120, radius, health, offset, pathType);
                e.setRowYOffset(j * 160); // permanent vertical separation per row
                enemies.add(e);
            }
        }

        // Spawn boss behind the formation (from wave 2 onwards)
        if (wave >= 2) {
            double bossHealth = 50.0 + wave * 10.0;
            boss = new BossEnemy(WIDTH / 2.0, -200, bossHealth);
            boss.setTargetPosition(WIDTH / 2.0, 140);
        } else {
            boss = null;
        }

        // Spawn asteroids (increasing with waves)
        int asteroidCount = Math.min(3 + wave * 2, 14);
        for (int i = 0; i < asteroidCount; i++) {
            int side = rng.nextInt(4); // 0=top, 1=bottom, 2=left, 3=right
            double ax, ay, avx, avy;
            int aRadius = 20 + rng.nextInt(25);
            switch (side) {
                case 0:  ax = rng.nextInt(WIDTH); ay = -80; avx = (rng.nextDouble()-0.5)*1.5; avy = 0.3+rng.nextDouble()*0.6; break;
                case 1:  ax = rng.nextInt(WIDTH); ay = HEIGHT+80; avx = (rng.nextDouble()-0.5)*1.5; avy = -(0.3+rng.nextDouble()*0.6); break;
                case 2:  ax = -80; ay = rng.nextInt(HEIGHT); avx = 0.3+rng.nextDouble()*0.6; avy = (rng.nextDouble()-0.5)*1.5; break;
                default: ax = WIDTH+80; ay = rng.nextInt(HEIGHT); avx = -(0.3+rng.nextDouble()*0.6); avy = (rng.nextDouble()-0.5)*1.5; break;
            }
            asteroids.add(new Asteroid(ax, ay, aRadius, avx, avy));
        }

        // Spawn nebula clouds (from wave 2, max 2 per wave)
        if (wave >= 2) {
            int nebulaCount = Math.min(1 + wave / 2, 3);
            for (int i = 0; i < nebulaCount; i++) {
                int nRadius = 120 + rng.nextInt(100);
                int side = rng.nextInt(4);
                double nx, ny, nvx, nvy;
                switch (side) {
                    case 0:  nx = rng.nextInt(WIDTH); ny = -nRadius*2; nvx = (rng.nextDouble()-0.5)*0.4; nvy = 0.15+rng.nextDouble()*0.2; break;
                    case 1:  nx = rng.nextInt(WIDTH); ny = HEIGHT+nRadius*2; nvx = (rng.nextDouble()-0.5)*0.4; nvy = -(0.15+rng.nextDouble()*0.2); break;
                    case 2:  nx = -nRadius*2; ny = rng.nextInt(HEIGHT); nvx = 0.15+rng.nextDouble()*0.2; nvy = (rng.nextDouble()-0.5)*0.4; break;
                    default: nx = WIDTH+nRadius*2; ny = rng.nextInt(HEIGHT); nvx = -(0.15+rng.nextDouble()*0.2); nvy = (rng.nextDouble()-0.5)*0.4; break;
                }
                nebulae.add(new Nebula(nx, ny, nRadius, nvx, nvy));
            }
        }

        // Spawn laser enemies (from wave 3 onwards, 1-2 per wave)
        if (wave >= 3) {
            int laserCount = Math.min(1 + (wave - 3) / 2, 3);
            double laserHealth = 8.0 + wave * 3.0;
            for (int i = 0; i < laserCount; i++) {
                // Spawn off-screen, drift to a position in the upper third
                double lx = 100 + rng.nextInt(WIDTH - 200);
                double ly = -80;
                LaserEnemy le = new LaserEnemy(lx, ly, laserHealth);
                le.setTargetPosition(lx, 80 + rng.nextInt(200));
                laserEnemies.add(le);
            }
        }

        // Level 2: Spawn phase enemies (teleporting enemies)
        if (currentLevel >= 2) {
            int phaseCount = Math.min(1 + wave / 2, 4);
            double phaseHealth = 6.0 + wave * 2.0;
            for (int i = 0; i < phaseCount; i++) {
                double px = 100 + rng.nextInt(WIDTH - 200);
                double py = 50 + rng.nextInt((int)(HEIGHT * 0.35));
                phaseEnemies.add(new PhaseEnemy(px, py, phaseHealth, WIDTH, HEIGHT));
            }
        }

        // Level 2: Spawn exotic space structures (spiral/square-wave galaxies) every few waves
        if (currentLevel >= 2 && wave % 3 == 1) {
            SpaceStructure.Type sType = (rng.nextBoolean()) ?
                SpaceStructure.Type.SPIRAL_GALAXY : SpaceStructure.Type.SQUARE_WAVE_GALAXY;
            double sx = 100 + rng.nextInt(WIDTH - 200);
            double sy = -300;
            double svx = (rng.nextDouble() - 0.5) * 0.2;
            double svy = 0.1 + rng.nextDouble() * 0.15;
            spaceStructures.add(new SpaceStructure(sx, sy, sType, svx, svy));
        }

        // Level 2: Satellite boss — approaches each wave, becomes final boss at wave 10
        if (currentLevel >= 2) {
            if (satelliteBoss == null || !satelliteBoss.isAlive()) {
                double armHp = 20.0 + wave * 5.0;
                double coreHp = 60.0 + wave * 10.0;
                satelliteBoss = new SatelliteBoss(WIDTH / 2.0, -250, armHp, coreHp);
            }
            // Each wave the boss drifts closer; by wave 10 it's in the fight zone
            double approachY = -200 + wave * 40;  // wave 1→-160, wave 10→200
            if (wave >= 10) approachY = 220;  // final boss position
            satelliteBoss.setTargetY(approachY);
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

        // Screen shake
        java.awt.geom.AffineTransform origTransform = g2.getTransform();
        if (shakeTimer > 0) {
            double sx = (rng.nextDouble() - 0.5) * 2 * shakeIntensity;
            double sy = (rng.nextDouble() - 0.5) * 2 * shakeIntensity;
            g2.translate(sx, sy);
        }

        final ArrayList<Star> starsSnap         = new ArrayList<>(stars);
        final ArrayList<Projectile> projsSnap   = new ArrayList<>(projectiles);
        final ArrayList<BlackHole> holesSnap    = new ArrayList<>(blackHoles);
        final ArrayList<ParticleRing> ringsSnap = new ArrayList<>(rings);
        final ArrayList<Enemy> enemiesSnap      = new ArrayList<>(enemies);
        final ArrayList<Shard> shardsSnap       = new ArrayList<>(shards);
        final ArrayList<SmokeParticle> smokesSnap = new ArrayList<>(smokes);
        final ArrayList<EnemyShot> enemyShotsSnap = new ArrayList<>(enemyShots);
        final ArrayList<PowerUp> powerUpsSnap = new ArrayList<>(powerUps);
        final ArrayList<HomingMissile> missilesSnap = new ArrayList<>(missiles);
        final ArrayList<Asteroid> asteroidsSnap = new ArrayList<>(asteroids);
        final ArrayList<Nebula> nebulaeSnap = new ArrayList<>(nebulae);
        final ArrayList<LaserEnemy> laserSnap = new ArrayList<>(laserEnemies);
        final ArrayList<SpaceStructure> structSnap = new ArrayList<>(spaceStructures);
        final BossEnemy bossSnap = boss;

        // Draw nebulae behind everything — parallax at 50% camera speed
        if (camera.isEnabled()) {
            g2.translate(-camera.getX() * 0.5, -camera.getY() * 0.5);
        }
        for (Nebula n : nebulaeSnap) {
            n.draw(g2);
        }
        if (camera.isEnabled()) {
            g2.translate(camera.getX() * 0.5, camera.getY() * 0.5);
        }

        // Draw moon background for level 3 (behind stars)
        if (moonBackground != null) {
            moonBackground.draw(g2);
        }

        // Stars are screen-relative (parallax) — no camera offset
        for (Star s : starsSnap) {
            s.draw(g2, holesSnap);
        }

        // Draw space structures — parallax at 80% camera speed
        if (camera.isEnabled()) {
            g2.translate(-camera.getX() * 0.8, -camera.getY() * 0.8);
        }
        for (SpaceStructure ss : structSnap) {
            ss.draw(g2);
        }
        if (camera.isEnabled()) {
            g2.translate(camera.getX() * 0.8, camera.getY() * 0.8);
        }

        // Draw asteroids — parallax at 100% camera speed (full scroll)
        if (camera.isEnabled()) {
            g2.translate(-camera.getX() * 1.0, -camera.getY() * 1.0);
        }
        for (Asteroid a : asteroidsSnap) {
            a.draw(g2);
        }
        if (camera.isEnabled()) {
            g2.translate(camera.getX() * 1.0, camera.getY() * 1.0);
        }

        // ---- Gameplay layer (screen-space, no camera offset) ----

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

        // Draw power-ups
        for (PowerUp pu : powerUpsSnap) {
            if (pu.isAlive()) pu.draw(g2);
        }

        // Draw satellite boss
        if (satelliteBoss != null && satelliteBoss.isAlive()) {
            satelliteBoss.draw(g2);
        }

        // Draw boss
        if (bossSnap != null && bossSnap.isAlive()) {
            bossSnap.draw(g2);
            // Boss health bar
            long timeSinceHit = System.currentTimeMillis() - bossSnap.getLastHitTime();
            if (timeSinceHit < 3000) {
                double bossRatio = bossSnap.getHealth() / bossSnap.getMaxHealth();
                double fadeAlpha = Math.max(0, Math.min(1.0, 1.0 - (timeSinceHit - 1500.0) / 1500.0));
                drawGradientHealthBar(g2, (int)bossSnap.getX() - 60, (int)bossSnap.getY() - bossSnap.getRadius() - 20, 120, 8, bossRatio, false, fadeAlpha);
            }
        }

        // Draw laser enemies (before missiles so beam renders under HUD layer)
        for (LaserEnemy le : laserSnap) {
            if (le.isAlive()) {
                le.draw(g2);
                // Health bar for recently hit laser enemies
                long timeSinceHit = System.currentTimeMillis() - le.getLastHitTime();
                if (timeSinceHit < 1500) {
                    double healthRatio = le.getHealth() / le.getMaxHealth();
                    double fadeAlpha = Math.max(0, Math.min(1.0, 1.0 - (timeSinceHit - 500.0) / 1000.0));
                    drawGradientHealthBar(g2, (int)le.getX() - 16, (int)le.getY() - 38, 32, 5, healthRatio, false, fadeAlpha);
                }
            }
        }

        // Draw homing missiles
        for (HomingMissile m : missilesSnap) {
            if (m.isAlive()) m.draw(g2);
        }

        // Draw phase enemies
        final ArrayList<PhaseEnemy> phaseSnap = new ArrayList<>(phaseEnemies);
        for (PhaseEnemy pe : phaseSnap) {
            if (pe.isAlive()) {
                pe.draw(g2);
                long timeSinceHit = System.currentTimeMillis() - pe.getLastHitTime();
                if (timeSinceHit < 1500) {
                    double pHealthRatio = pe.getHealth() / pe.getMaxHealth();
                    double pFadeAlpha = Math.max(0, Math.min(1.0, 1.0 - (timeSinceHit - 500.0) / 1000.0));
                    drawGradientHealthBar(g2, (int)pe.getX() - 14, (int)pe.getY() - 38, 28, 4, pHealthRatio, false, pFadeAlpha);
                }
            }
        }

        // Draw decoy field
        if (activeDecoy != null && activeDecoy.isAlive()) {
            activeDecoy.draw(g2);
        }

        // Draw deployed grenades (blinking)
        for (DeployedGrenade dg : deployedGrenades) {
            dg.draw(g2);
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

        // ========== VOLTAGE BAR ==========
        {
            double voltRatio = player.getVoltageLevel() / (double)Player.MAX_VOLTAGE;
            int vBarY = barY + barHeight + 14;
            int vBarH = 16;
            drawVoltageBar(g2, barX, vBarY, barWidth, vBarH, voltRatio);
        }

        // ========== FORCEFIELD INDICATOR ==========
        {
            int ffY = barY + barHeight + 48;
            drawForcefieldIndicator(g2, barX, ffY, barWidth);
        }

        // ========== WAVE BADGE + LEVEL NAME + GUN TYPE ==========
        {
            g2.setFont(new Font("Arial", Font.BOLD, 18));
            g2.setColor(new Color(180, 200, 220));
            String levelName = getLevelName(currentLevel);
            String waveStr = levelName + " - WAVE " + waveNumber;
            g2.drawString(waveStr, barX, barY + barHeight + 82);

            // Gun type indicator
            String gunName = player.getGun().name();
            Color gunColor;
            switch (player.getGun()) {
                case TRIANGLE: gunColor = new Color(255, 80, 40); break;
                case SQUARE:   gunColor = new Color(40, 160, 255); break;
                case SINE:     gunColor = new Color(80, 255, 120); break;
                default:       gunColor = Color.WHITE; break;
            }
            g2.setFont(new Font("Consolas", Font.BOLD, 14));
            g2.setColor(gunColor);
            g2.drawString("GUN: " + gunName, barX + 180, barY + barHeight + 82);

            // Inventory indicators
            int invY = barY + barHeight + 98;
            if (player.getDecoyCount() > 0) {
                g2.setColor(new Color(100, 220, 255));
                g2.drawString("DECOY x" + player.getDecoyCount(), barX, invY);
            }
            if (player.getGrenadeCount() > 0) {
                g2.setColor(new Color(255, 140, 40));
                g2.drawString("BOMB x" + player.getGrenadeCount(), barX + 100, invY);
            }
        }

        // ========== SPEED BAR ==========
        {
            int sbY = barY + barHeight + 114;
            double speedRatio = player.getSpeedLevel() / (double)Player.MAX_SPEED_LEVEL;
            drawMiniBar(g2, "SPD", barX, sbY, 120, 10, speedRatio, new Color(80, 200, 255), new Color(40, 100, 180));
            // Speed level number
            g2.setFont(new Font("Consolas", Font.PLAIN, 11));
            g2.setColor(new Color(120, 200, 255));
            g2.drawString("" + (player.getSpeedLevel() + 1) + "/10", barX + 126, sbY + 9);
        }

        // ========== FREQUENCY BAR ==========
        {
            int fbY = barY + barHeight + 130;
            double freqRatio = player.getVoltageLevel() / (double)Player.MAX_VOLTAGE;
            drawMiniBar(g2, "FRQ", barX, fbY, 120, 10, freqRatio, new Color(255, 160, 40), new Color(180, 80, 20));
            g2.setFont(new Font("Consolas", Font.PLAIN, 11));
            g2.setColor(new Color(255, 180, 80));
            g2.drawString("" + (player.getVoltageLevel() + 1) + "/" + (Player.MAX_VOLTAGE + 1), barX + 126, fbY + 9);
        }

        // ========== VOLUME SLIDERS ==========
        {
            int slTrackX = barX + 55;
            int slTrackW = barWidth - 55;
            int slH = 8;
            int sfxSlY = barY + barHeight + 150;
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
        g2.drawString("Level: " + currentLevel + "  Wave: " + waveNumber, 20, statY);
        
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

        // Grenade flash overlay
        if (grenadeFlashTimer > 0) {
            float flashAlpha = Math.min(1f, grenadeFlashTimer / 15f);
            g2.setColor(new Color(255, 240, 200, (int)(180 * flashAlpha)));
            g2.fillRect(-50, -50, WIDTH + 100, HEIGHT + 100);
        }

        // Reset screen shake transform
        g2.setTransform(origTransform);

        // Draw cinematic overlay
        if (cinematicPhase > 0) {
            drawCinematic(g2);
        }

        // Draw overworld map (full-screen overlay)
        if (showingMap) {
            overworldMap.draw(g2);
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
            case KeyEvent.VK_X:
                // Deploy grenade from inventory
                if (player.useGrenade()) {
                    deployedGrenades.add(new DeployedGrenade(player.getX(), player.getY()));
                    AudioManager.playSfx("powerup8", 0.8f);
                }
                // Activate forcefield
                player.addForcefield();
                break;
            case KeyEvent.VK_F: player.addForcefield(); System.out.println("FORCEFIELD ADDED (F): " + player.getForcefieldTimer()); break; // DEBUG: alt key
            case KeyEvent.VK_W: skipWave(); break; // DEBUG: skip to next wave
            case KeyEvent.VK_D: activeDecoy = new DecoyField(player.getX(), player.getY()); System.out.println("DECOY DEPLOYED"); break; // DEBUG: deploy decoy
            case KeyEvent.VK_L: debugCycleLevel(); break; // DEBUG: cycle through levels
            case KeyEvent.VK_ALT:
                // Deploy decoy from inventory
                if (activeDecoy == null && player.useDecoy()) {
                    activeDecoy = new DecoyField(player.getX(), player.getY());
                    AudioManager.playSfx("powerup7", 0.8f);
                }
                break;
            case KeyEvent.VK_M:
                if (!playerDead && cinematicPhase == 0) {
                    showingMap = !showingMap;
                    if (showingMap) overworldMap.resetIntro();
                }
                break;
            case KeyEvent.VK_SPACE:
            case KeyEvent.VK_ENTER:
                if (showingMap && overworldMap.isSelectedUnlocked()) {
                    launchSelectedLevel();
                }
                break;
            case KeyEvent.VK_ESCAPE:
                if (showingMap && wave1Active) {
                    showingMap = false;  // return to game in progress
                }
                break;
        }

        // Map navigation when map is open
        if (showingMap) {
            switch (e.getKeyCode()) {
                case KeyEvent.VK_RIGHT:
                    overworldMap.navigate(1);
                    break;
                case KeyEvent.VK_LEFT:
                    overworldMap.navigate(-1);
                    break;
                case KeyEvent.VK_UP:
                    overworldMap.navigateVertical(-1);
                    break;
                case KeyEvent.VK_DOWN:
                    overworldMap.navigateVertical(1);
                    break;
            }
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
    // 1/5 chance to drop a power-up
    if (rng.nextInt(5) == 0) {
        PowerUp.Type[] types = PowerUp.Type.values();
        PowerUp.Type type = types[rng.nextInt(types.length)];
        powerUps.add(new PowerUp(x, y, type));
    }
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

/** Electric burst when forcefield absorbs a projectile/collision. */
private void spawnForcefieldAbsorb(double x, double y) {
    AudioManager.playSfx("explosion", 0.5f);
    rings.add(new ParticleRing(x, y, 12));
    for (int i = 0; i < 8; i++) {
        double ang = (i / 8.0) * Math.PI * 2 + Math.random() * 0.3;
        shards.add(new Shard(x, y, ang, 4.0 + Math.random() * 3, new Color(100, 220, 255), 1));
    }
}

private void restartGame() {
        playerDead = false;
        paused = false;
        waveNumber = 1;
        currentLevel = 1;
        displayScore = 0;
        lastScoreChangeTime = 0;
        fpsMin2s = 999;
        fpsHistoryCount = 0;
        fireIntervalMs = 100;  // reset to starting fire rate
        cinematicPhase = 0;
        cinematicTimer = 0;
        player = new Player(WIDTH / 2, HEIGHT / 2, 40);
        projectiles.clear();
        enemies.clear();
        enemyShots.clear();
        rings.clear();
        shards.clear();
        smokes.clear();
        blackHoles.clear();
        powerUps.clear();
        missiles.clear();
        asteroids.clear();
        nebulae.clear();
        laserEnemies.clear();
        spaceStructures.clear();
        phaseEnemies.clear();
        activeDecoy = null;
        boss = null;
        satelliteBoss = null;
        moonBackground = null;
        camera.setEnabled(false);
        camera.reset();
        deployedGrenades.clear();
        wave1Active = false;
        // Return to map instead of spawning immediately
        showingMap = true;
        overworldMap = new OverworldMap();
        overworldMap.resetIntro();
    }

/** Launch the level selected on the overworld map. */
private void launchSelectedLevel() {
    currentLevel = overworldMap.getSelectedLevel();
    waveNumber = 1;
    showingMap = false;
    cinematicPhase = 0;
    cinematicTimer = 0;
    // Clear all game objects for fresh level
    projectiles.clear();
    enemies.clear();
    enemyShots.clear();
    rings.clear();
    shards.clear();
    smokes.clear();
    blackHoles.clear();
    powerUps.clear();
    missiles.clear();
    asteroids.clear();
    nebulae.clear();
    laserEnemies.clear();
    spaceStructures.clear();
    phaseEnemies.clear();
    activeDecoy = null;
    boss = null;
    satelliteBoss = null;
    moonBackground = (currentLevel == 3) ? new MoonBackground(WIDTH, HEIGHT) : null;
    // Enable camera scrolling for levels >= 2 (level 1 stays arena-style)
    camera.reset();
    camera.setEnabled(currentLevel >= 2);
    deployedGrenades.clear();
    player = new Player(WIDTH / 2, HEIGHT / 2, 40);
    spawnWave(waveNumber);
}

/** DEBUG: skip current wave by killing all enemies. */
private void skipWave() {
    if (playerDead || cinematicPhase > 0) return;
    for (Enemy e : enemies) e.takeDamage(9999);
    if (boss != null) boss.takeDamage(9999);
    for (LaserEnemy le : laserEnemies) le.takeDamage(9999);
    for (PhaseEnemy pe : phaseEnemies) pe.takeDamage(9999);
    if (satelliteBoss != null) satelliteBoss.damageAllArms(9999);
    System.out.println("SKIP WAVE -> next wave: " + (waveNumber + 1));
}

/** DEBUG: cycle through levels 1-6 instantly for testing. */
private void debugCycleLevel() {
    if (playerDead) return;
    int nextLevel = (currentLevel % 6) + 1;
    currentLevel = nextLevel;
    waveNumber = 1;
    cinematicPhase = 0;
    cinematicTimer = 0;
    projectiles.clear();
    enemies.clear();
    enemyShots.clear();
    rings.clear();
    shards.clear();
    smokes.clear();
    blackHoles.clear();
    powerUps.clear();
    missiles.clear();
    asteroids.clear();
    nebulae.clear();
    laserEnemies.clear();
    spaceStructures.clear();
    phaseEnemies.clear();
    activeDecoy = null;
    boss = null;
    satelliteBoss = null;
    moonBackground = (currentLevel == 3) ? new MoonBackground(WIDTH, HEIGHT) : null;
    camera.reset();
    camera.setEnabled(currentLevel >= 2);
    deployedGrenades.clear();
    player = new Player(WIDTH / 2, HEIGHT / 2, 40);
    spawnWave(waveNumber);
    System.out.println("DEBUG LEVEL SWITCH -> Level " + currentLevel + " (" + getLevelName(currentLevel) + ") camera=" + camera.isEnabled());
}

/** Detonate mega grenade at a specific position: screen shake + flash + damage all enemies. */
private void detonateMegaGrenade(double gx, double gy) {
    shakeTimer = 45;       // 0.75 sec shake
    shakeIntensity = 20;
    grenadeFlashTimer = 30; // 0.5 sec flash

    AudioManager.playSfx("explosion", 1.0f);
    AudioManager.playSfx("glassbreak", 1.0f);

    // Big rings from detonation point
    for (int i = 0; i < 5; i++) {
        rings.add(new ParticleRing(gx, gy, 40 + i * 30));
    }
    // Shards in all directions
    for (int i = 0; i < 30; i++) {
        double ang = rng.nextDouble() * Math.PI * 2;
        double spd = 3 + rng.nextDouble() * 8;
        shards.add(new Shard(gx, gy, ang, spd, new Color(255, 200, 60)));
    }

    // Damage all enemies (30 damage) and spawn explosions for kills
    double grenadeDmg = 30;
    for (Enemy e : enemies) {
        if (e.isAlive()) {
            e.takeDamage(grenadeDmg);
            if (!e.isAlive()) spawnEnemyExplosion(e.getX(), e.getY(), e.getRadius(), Player.GunType.TRIANGLE);
        }
    }
    if (boss != null && boss.isAlive()) {
        boss.takeDamage(grenadeDmg);
        if (!boss.isAlive()) spawnBossExplosion(boss.getX(), boss.getY(), boss.getRadius());
    }
    for (LaserEnemy le : laserEnemies) {
        if (le.isAlive()) {
            le.takeDamage(grenadeDmg);
            if (!le.isAlive()) {
                rings.add(new ParticleRing(le.getX(), le.getY(), le.getRadius() + 8));
                AudioManager.playSfx("explosion");
                player.score += 150;
            }
        }
    }
    for (PhaseEnemy pe : phaseEnemies) {
        if (pe.isAlive()) {
            pe.takeDamage(grenadeDmg);
            if (!pe.isAlive()) {
                rings.add(new ParticleRing(pe.getX(), pe.getY(), pe.getRadius() + 10));
                AudioManager.playSfx("explosion");
                player.score += 200;
            }
        }
    }
    if (satelliteBoss != null && satelliteBoss.isAlive()) {
        satelliteBoss.damageAllArms(grenadeDmg);
    }
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

private void spawnBossExplosion(double x, double y, int radius) {
    player.score += 500;
    AudioManager.playSfx("explosion", 1.0f);
    AudioManager.playSfx("glassbreak", 1.0f);
    // Massive rings
    for (int i = 0; i < 5; i++) {
        rings.add(new ParticleRing(x, y, radius / 2 + i * 20));
    }
    // Tons of shards
    for (int i = 0; i < 40; i++) {
        double ang = rng.nextDouble() * Math.PI * 2;
        double spd = 2 + rng.nextDouble() * 8;
        shards.add(new Shard(x, y, ang, spd, new Color(180, 120, 255)));
    }
    // Heavy smoke
    for (int i = 0; i < 12; i++) {
        double sx = x + (rng.nextDouble() - 0.5) * radius * 2;
        double sy = y + (rng.nextDouble() - 0.5) * radius * 2;
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
    g2.drawString("ENERGY", x, y - 6);

    // Percentage text inside the bar
    int pct = (int)Math.round(ratio * 100);
    String pctStr = pct + "%";
    g2.setFont(new Font("Arial", Font.BOLD, 13));
    FontMetrics fm = g2.getFontMetrics();
    int textX = x + (width - fm.stringWidth(pctStr)) / 2;
    int textY = y + height - (height - fm.getAscent()) / 2 - 1;
    g2.setColor(new Color(0, 0, 0, 140));
    g2.drawString(pctStr, textX + 1, textY + 1);
    g2.setColor(new Color(220, 240, 255, 220));
    g2.drawString(pctStr, textX, textY);
}

/** Get the display name for a level number. */
private String getLevelName(int level) {
    switch (level) {
        case 1: return "NEXUS GATE";
        case 2: return "PLASMA DRIFT";
        case 3: return "VOID STATION";
        case 4: return "NEON ABYSS";
        case 5: return "SIGNAL PRIME";
        case 6: return "OMEGA CORE";
        default: return "SECTOR " + level;
    }
}

/** Draw a compact labelled bar for speed/frequency. */
private void drawMiniBar(Graphics2D g2, String label, int x, int y, int w, int h,
                          double ratio, Color fillColor, Color bgColor) {
    ratio = Math.max(0, Math.min(1, ratio));
    // Label
    g2.setFont(new Font("Consolas", Font.BOLD, 11));
    g2.setColor(new Color(160, 180, 200));
    g2.drawString(label, x, y + h - 1);
    int barX = x + 30;
    int barW = w - 30;
    // Background
    g2.setColor(bgColor.darker().darker());
    g2.fillRoundRect(barX, y, barW, h, 4, 4);
    // Fill
    g2.setColor(fillColor);
    g2.fillRoundRect(barX, y, (int)(barW * ratio), h, 4, 4);
    // Border
    g2.setColor(new Color(fillColor.getRed(), fillColor.getGreen(), fillColor.getBlue(), 100));
    g2.drawRoundRect(barX, y, barW, h, 4, 4);
    // Tick marks
    g2.setColor(new Color(0, 0, 0, 60));
    for (int i = 1; i < 10; i++) {
        int tx = barX + (barW * i) / 10;
        g2.drawLine(tx, y, tx, y + h);
    }
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

/** Voltage bar — gold/electric theme showing weapon boost level. */
private void drawVoltageBar(Graphics2D g2, int x, int y, int width, int height, double ratio) {
    ratio = Math.max(0, Math.min(1, ratio));
    double pulse = 0.85 + 0.15 * Math.sin(hudTick * 0.10);

    // Gold color ramp
    int r = 255, gr = (int)(180 + 40 * ratio), b = 40;

    // Outer glow
    for (int i = 3; i >= 1; i--) {
        int ga = (int)(25 * pulse * (1.0 - i / 4.0));
        g2.setColor(new Color(r, gr, b, ga));
        g2.fillRoundRect(x - i * 2, y - i * 2, width + i * 4, height + i * 4, 6, 6);
    }

    // Background
    g2.setColor(new Color(15, 15, 10, 220));
    g2.fillRoundRect(x, y, width, height, 4, 4);

    // Segment lines
    int numSegments = Player.MAX_VOLTAGE;
    g2.setStroke(new BasicStroke(1.0f));
    for (int i = 1; i < numSegments; i++) {
        int sx = x + (int)(width * i / (double)numSegments);
        g2.setColor(new Color(80, 70, 30, 100));
        g2.drawLine(sx, y + 1, sx, y + height - 1);
    }

    // Fill
    int fillWidth = (int)(width * ratio);
    if (fillWidth > 0) {
        GradientPaint gp = new GradientPaint(x, y,
            new Color(180, 120, 0), x + fillWidth, y, new Color(255, 220, 60));
        g2.setPaint(gp);
        g2.fillRoundRect(x + 1, y + 1, fillWidth - 1, height - 2, 3, 3);

        // Highlight sheen
        g2.setColor(new Color(255, 255, 200, (int)(50 * pulse)));
        g2.fillRoundRect(x + 2, y + 2, fillWidth - 3, height / 3, 2, 2);

        // Electric crackle sweep
        double sweepT = (hudTick % 90) / 90.0;
        int sweepX = x + (int)(width * sweepT);
        if (sweepX < x + fillWidth) {
            for (int i = 0; i < 20; i++) {
                int sx2 = sweepX + i;
                if (sx2 >= x && sx2 <= x + fillWidth) {
                    double si = Math.sin(i * Math.PI / 20);
                    g2.setColor(new Color(255, 255, 180, (int)(60 * si)));
                    g2.drawLine(sx2, y + 1, sx2, y + height - 2);
                }
            }
        }
    }

    // Border
    g2.setStroke(new BasicStroke(1.2f));
    g2.setColor(new Color(200, 180, 80, (int)(160 * pulse)));
    g2.drawRoundRect(x, y, width, height, 4, 4);

    // Label
    g2.setFont(new Font("Arial", Font.BOLD, 11));
    g2.setColor(new Color(220, 200, 100, 200));
    g2.drawString("VOLTAGE", x, y - 3);

    // Level text inside
    String lvlStr = player.getVoltageLevel() + "/" + Player.MAX_VOLTAGE;
    g2.setFont(new Font("Arial", Font.BOLD, 10));
    FontMetrics fm = g2.getFontMetrics();
    int textX = x + (width - fm.stringWidth(lvlStr)) / 2;
    int textY = y + height - (height - fm.getAscent()) / 2 - 1;
    g2.setColor(new Color(0, 0, 0, 120));
    g2.drawString(lvlStr, textX + 1, textY + 1);
    g2.setColor(new Color(255, 240, 180, 220));
    g2.drawString(lvlStr, textX, textY);
}

/** Forcefield indicator — shows remaining time with animated icon. */
private void drawForcefieldIndicator(Graphics2D g2, int x, int y, int width) {
    boolean active = player.hasForcefield();
    double pulse = 0.6 + 0.4 * Math.sin(hudTick * 0.12);

    // Icon: small hexagon
    int iconR = 8;
    int iconCx = x + iconR + 2;
    int iconCy = y + 6;
    Polygon hex = new Polygon();
    for (int i = 0; i < 6; i++) {
        double a = Math.PI / 6 + i * Math.PI / 3;
        hex.addPoint(iconCx + (int)(iconR * Math.cos(a)), iconCy + (int)(iconR * Math.sin(a)));
    }

    if (active) {
        // Glowing hex icon
        g2.setColor(new Color(80, 200, 255, (int)(60 * pulse)));
        g2.fillPolygon(hex);
        g2.setStroke(new BasicStroke(1.5f));
        g2.setColor(new Color(100, 220, 255, (int)(220 * pulse)));
        g2.drawPolygon(hex);

        // Timer text
        double secondsLeft = player.getForcefieldTimer() / 60.0;
        String timeStr = String.format("%.1fs", secondsLeft);
        g2.setFont(new Font("Arial", Font.BOLD, 12));
        g2.setColor(new Color(100, 220, 255, (int)(200 * pulse)));
        g2.drawString("FORCEFIELD", iconCx + iconR + 6, iconCy + 4);

        FontMetrics fm = g2.getFontMetrics();
        int timeX = x + width - fm.stringWidth(timeStr);
        g2.setColor(new Color(0, 0, 0, 100));
        g2.drawString(timeStr, timeX + 1, iconCy + 5);
        g2.setColor(new Color(140, 240, 255));
        g2.drawString(timeStr, timeX, iconCy + 4);

        // Countdown arc around icon
        int arcAngle = (int)(360.0 * (player.getForcefieldTimer() % 600) / 600.0);
        g2.setStroke(new BasicStroke(2.0f));
        g2.setColor(new Color(80, 200, 255, (int)(150 * pulse)));
        g2.drawArc(iconCx - iconR - 2, iconCy - iconR - 2, (iconR + 2) * 2, (iconR + 2) * 2, 90, arcAngle);
    } else {
        // Dimmed icon when inactive
        g2.setColor(new Color(60, 80, 90, 80));
        g2.fillPolygon(hex);
        g2.setStroke(new BasicStroke(1.0f));
        g2.setColor(new Color(80, 100, 110, 100));
        g2.drawPolygon(hex);

        g2.setFont(new Font("Arial", Font.BOLD, 12));
        g2.setColor(new Color(80, 100, 110, 100));
        g2.drawString("FORCEFIELD", iconCx + iconR + 6, iconCy + 4);
    }
}

// ==================== LEVEL-COMPLETE CINEMATIC ====================

/**
 * Cinematic phases:
 * 1 — Victory explosions around the screen (180 frames / 3s)
 * 2 — Ship opens up, electricity shoots everywhere (240 frames / 4s)
 * 3 — White flash + "LEVEL COMPLETE" text (120 frames / 2s)
 * 4 — Hyperspace tunnel transition (180 frames / 3s)
 * 5 — Level 2 arrival — fade in, spawn level 2 content (90 frames / 1.5s)
 */
private void updateCinematic() {
    cinematicTimer++;
    // Keep stars drifting during cinematic
    for (Star s : new ArrayList<>(stars)) {
        s.update(WIDTH, HEIGHT);
    }
    // Update particles still alive
    for (int i = rings.size() - 1; i >= 0; i--) {
        rings.get(i).update();
        if (!rings.get(i).isAlive()) rings.remove(i);
    }
    for (int i = shards.size() - 1; i >= 0; i--) {
        shards.get(i).update();
        if (!shards.get(i).isAlive()) shards.remove(i);
    }
    for (int i = smokes.size() - 1; i >= 0; i--) {
        smokes.get(i).update();
        if (!smokes.get(i).isAlive()) smokes.remove(i);
    }

    switch (cinematicPhase) {
        case 1: // Victory explosions
            if (cinematicTimer % 12 == 0) {
                double ex = 200 + rng.nextInt(WIDTH - 400);
                double ey = 100 + rng.nextInt(HEIGHT - 200);
                rings.add(new ParticleRing(ex, ey, 20 + rng.nextInt(30)));
                for (int i = 0; i < 6; i++) {
                    double ang = rng.nextDouble() * Math.PI * 2;
                    shards.add(new Shard(ex, ey, ang, 2 + rng.nextDouble() * 5,
                        new Color(100 + rng.nextInt(155), 180 + rng.nextInt(75), 255)));
                }
                AudioManager.playSfx("explosion", 0.6f);
            }
            // Move player to center
            player.x += (WIDTH / 2.0 - player.x) * 0.03;
            player.y += (HEIGHT / 2.0 - player.y) * 0.03;
            if (cinematicTimer >= 180) { cinematicPhase = 2; cinematicTimer = 0; }
            break;

        case 2: // Ship electricity — spawn electric arcs from player
            player.x += (WIDTH / 2.0 - player.x) * 0.05;
            player.y += (HEIGHT / 2.0 - player.y) * 0.05;
            // Spawn electric shards radiating outward
            if (cinematicTimer % 3 == 0) {
                int numArcs = 4 + rng.nextInt(4);
                for (int i = 0; i < numArcs; i++) {
                    double ang = rng.nextDouble() * Math.PI * 2;
                    double dist = 30 + rng.nextDouble() * 80;
                    double sx = player.x + Math.cos(ang) * dist;
                    double sy = player.y + Math.sin(ang) * dist;
                    shards.add(new Shard(sx, sy, ang, 3 + rng.nextDouble() * 4,
                        new Color(80 + rng.nextInt(80), 180 + rng.nextInt(75), 255), 1));
                }
            }
            // Expanding rings
            if (cinematicTimer % 20 == 0) {
                rings.add(new ParticleRing(player.x, player.y, 15 + cinematicTimer / 4));
                AudioManager.playSfx("explosion", 0.4f);
            }
            if (cinematicTimer >= 240) { cinematicPhase = 3; cinematicTimer = 0; }
            break;

        case 3: // White flash + level complete text
            if (cinematicTimer >= 120) { cinematicPhase = 4; cinematicTimer = 0; }
            break;

        case 4: // Hyperspace tunnel
            // Speed up stars dramatically
            for (Star s : stars) {
                s.setDriftMultiplier(3.0 + cinematicTimer * 0.1);
            }
            if (cinematicTimer >= 180) {
                cinematicPhase = 5;
                cinematicTimer = 0;
                // Transition to level 2
                currentLevel = 2;
                waveNumber = 1;
                enemies.clear();
                enemyShots.clear();
                projectiles.clear();
                missiles.clear();
                asteroids.clear();
                nebulae.clear();
                laserEnemies.clear();
                powerUps.clear();
                boss = null;
                // Spawn exotic structures for level 2
                spawnLevel2Structures();
            }
            break;

        case 5: // Fade in — then return to map with next level unlocked
            // Reset star speed
            for (Star s : stars) {
                double t = cinematicTimer / 90.0;
                s.setDriftMultiplier(Math.max(1.0, 3.0 - t * 2.0));
            }
            if (cinematicTimer >= 90) {
                cinematicPhase = 0;
                cinematicTimer = 0;
                for (Star s : stars) s.setDriftMultiplier(1.0);
                // Unlock the next level on the map and show it
                overworldMap.unlockLevel(currentLevel);
                showingMap = true;
                overworldMap.resetIntro();
            }
            break;
    }
}

private void drawCinematic(Graphics2D g2) {
    switch (cinematicPhase) {
        case 1: // Victory explosions — draw "LEVEL CLEAR!" fading in
        {
            double alpha = Math.min(1.0, cinematicTimer / 60.0);
            g2.setFont(new Font("Arial", Font.BOLD, 52));
            FontMetrics fm = g2.getFontMetrics();
            String text = "LEVEL " + currentLevel + " CLEAR!";
            int tx = (WIDTH - fm.stringWidth(text)) / 2;
            int ty = HEIGHT / 2 - 160;
            // Glow
            g2.setColor(new Color(100, 200, 255, (int)(60 * alpha)));
            g2.drawString(text, tx - 2, ty - 2);
            g2.drawString(text, tx + 2, ty + 2);
            // Main text
            g2.setColor(new Color(220, 240, 255, (int)(255 * alpha)));
            g2.drawString(text, tx, ty);
            break;
        }

        case 2: // Ship electricity — draw electric arcs from player center
        {
            Composite oldComp = g2.getComposite();
            double pulse = 0.6 + 0.4 * Math.sin(cinematicTimer * 0.3);
            int numBolts = 8 + cinematicTimer / 15;
            g2.setStroke(new BasicStroke(2.5f));
            for (int b = 0; b < numBolts; b++) {
                double bAng = (b / (double)numBolts) * Math.PI * 2 + cinematicTimer * 0.05;
                double bLen = 100 + 200 * (cinematicTimer / 240.0) + rng.nextDouble() * 80;
                // Draw jagged lightning bolt
                double cx = player.x, cy = player.y;
                int segments = 6 + rng.nextInt(4);
                for (int s = 0; s < segments; s++) {
                    double frac = (s + 1.0) / segments;
                    double nx = player.x + Math.cos(bAng) * bLen * frac + (rng.nextDouble() - 0.5) * 30;
                    double ny = player.y + Math.sin(bAng) * bLen * frac + (rng.nextDouble() - 0.5) * 30;
                    int a = (int)(180 * pulse * (1.0 - frac * 0.5));
                    g2.setColor(new Color(120, 200, 255, a));
                    g2.drawLine((int)cx, (int)cy, (int)nx, (int)ny);
                    // Hot core
                    g2.setColor(new Color(200, 240, 255, a / 2));
                    g2.setStroke(new BasicStroke(1.0f));
                    g2.drawLine((int)cx, (int)cy, (int)nx, (int)ny);
                    g2.setStroke(new BasicStroke(2.5f));
                    cx = nx; cy = ny;
                }
            }
            g2.setComposite(oldComp);
            g2.setStroke(new BasicStroke(1.0f));

            // "INITIATING HYPERSPACE" text
            if (cinematicTimer > 120) {
                double a2 = Math.min(1.0, (cinematicTimer - 120) / 60.0);
                g2.setFont(new Font("Arial", Font.BOLD, 36));
                FontMetrics fm = g2.getFontMetrics();
                String text = "INITIATING HYPERSPACE...";
                int tx = (WIDTH - fm.stringWidth(text)) / 2;
                g2.setColor(new Color(180, 220, 255, (int)(255 * a2)));
                g2.drawString(text, tx, HEIGHT / 2 + 200);
            }
            break;
        }

        case 3: // White flash + LEVEL COMPLETE
        {
            double flashAlpha;
            if (cinematicTimer < 20) {
                flashAlpha = cinematicTimer / 20.0;
            } else if (cinematicTimer < 60) {
                flashAlpha = 1.0;
            } else {
                flashAlpha = 1.0 - (cinematicTimer - 60) / 60.0;
            }
            g2.setColor(new Color(255, 255, 255, (int)(200 * Math.max(0, flashAlpha))));
            g2.fillRect(0, 0, WIDTH, HEIGHT);

            if (cinematicTimer > 30) {
                double tAlpha = Math.min(1.0, (cinematicTimer - 30) / 30.0);
                g2.setFont(new Font("Arial", Font.BOLD, 64));
                FontMetrics fm = g2.getFontMetrics();
                String text = "ENTERING LEVEL 2";
                int tx = (WIDTH - fm.stringWidth(text)) / 2;
                int ty = HEIGHT / 2;
                g2.setColor(new Color(0, 0, 0, (int)(200 * tAlpha)));
                g2.drawString(text, tx, ty);
            }
            break;
        }

        case 4: // Hyperspace tunnel
        {
            Composite oldComp = g2.getComposite();
            // Radial streak lines from center
            double intensity = Math.min(1.0, cinematicTimer / 60.0);
            int numStreaks = 60;
            g2.setStroke(new BasicStroke(2.0f));
            for (int i = 0; i < numStreaks; i++) {
                double ang = (i / (double)numStreaks) * Math.PI * 2;
                double innerR = 20 + cinematicTimer * 1.5;
                double outerR = innerR + 200 + cinematicTimer * 3;
                int x1 = WIDTH / 2 + (int)(Math.cos(ang) * innerR);
                int y1 = HEIGHT / 2 + (int)(Math.sin(ang) * innerR);
                int x2 = WIDTH / 2 + (int)(Math.cos(ang) * outerR);
                int y2 = HEIGHT / 2 + (int)(Math.sin(ang) * outerR);
                int a = (int)(160 * intensity);
                int blue = 200 + (int)(55 * Math.sin(i * 0.5 + cinematicTimer * 0.1));
                g2.setColor(new Color(140, 180, Math.min(255, blue), a));
                g2.drawLine(x1, y1, x2, y2);
            }
            g2.setComposite(oldComp);
            g2.setStroke(new BasicStroke(1.0f));

            // Vignette darken edges
            double vAlpha = 0.5 + 0.3 * (cinematicTimer / 180.0);
            g2.setColor(new Color(0, 0, 0, (int)(255 * Math.min(0.8, vAlpha))));
            g2.fillRect(0, 0, WIDTH, 80);
            g2.fillRect(0, HEIGHT - 80, WIDTH, 80);
            break;
        }

        case 5: // Fade in to level 2
        {
            double fadeAlpha = 1.0 - (cinematicTimer / 90.0);
            if (fadeAlpha > 0) {
                g2.setColor(new Color(0, 0, 0, (int)(255 * Math.max(0, fadeAlpha))));
                g2.fillRect(0, 0, WIDTH, HEIGHT);
            }
            // Level 2 title
            if (cinematicTimer > 30) {
                double tAlpha = Math.min(1.0, (cinematicTimer - 30) / 30.0) * Math.max(0, 1.0 - (cinematicTimer - 60) / 30.0);
                if (tAlpha > 0) {
                    g2.setFont(new Font("Arial", Font.BOLD, 48));
                    FontMetrics fm = g2.getFontMetrics();
                    String text = "LEVEL 2";
                    int tx = (WIDTH - fm.stringWidth(text)) / 2;
                    g2.setColor(new Color(180, 140, 255, (int)(255 * Math.max(0, tAlpha))));
                    g2.drawString(text, tx, HEIGHT / 2);
                }
            }
            break;
        }
    }
}

private void spawnLevel2Structures() {
    spaceStructures.clear();
    // Spawn 2-3 spiral galaxies and 1-2 square-wave galaxies
    int spiralCount = 2 + rng.nextInt(2);
    for (int i = 0; i < spiralCount; i++) {
        double sx = 100 + rng.nextInt(WIDTH - 200);
        double sy = -200 - rng.nextInt(400);
        double svx = (rng.nextDouble() - 0.5) * 0.3;
        double svy = 0.15 + rng.nextDouble() * 0.2;
        spaceStructures.add(new SpaceStructure(sx, sy, SpaceStructure.Type.SPIRAL_GALAXY, svx, svy));
    }
    int squareCount = 1 + rng.nextInt(2);
    for (int i = 0; i < squareCount; i++) {
        double sx = 100 + rng.nextInt(WIDTH - 200);
        double sy = -300 - rng.nextInt(400);
        double svx = (rng.nextDouble() - 0.5) * 0.2;
        double svy = 0.1 + rng.nextDouble() * 0.15;
        spaceStructures.add(new SpaceStructure(sx, sy, SpaceStructure.Type.SQUARE_WAVE_GALAXY, svx, svy));
    }
}
}
