package game;

import java.util.HashMap;
import java.util.Map;

public final class AudioManager {
    private static final Map<String, SoundPool> sfx = new HashMap<>();
    private static final Map<String, Float> baseVolumes = new HashMap<>();
    private static final MusicPlayer music = new MusicPlayer();

    private static float masterVolume = 1.0f;
    private static float sfxVolume = .5f;
    private static float musicVolume = 0.5f;

    private AudioManager() {}

    public static void init() {
        // register your sound effects here (key, path, poolSize, baseVolume 0-1)
        loadSfx("shootTri",     "/audio/shoot2.wav",       10, 0.6f);
        loadSfx("shootSqr",     "/audio/shoot4.wav",       10, 0.6f);
        loadSfx("shootSin",     "/audio/shoot6.wav",       10, 0.6f);
        loadSfx("switchRay",    "/audio/raygun1.wav",      10, 0.5f);
        loadSfx("hitTri",       "/audio/bottleclink2.wav",  8, 0.4f);
        loadSfx("hitSqr",       "/audio/bottleclink3.wav",  8, 0.2f);
        loadSfx("hitSin",       "/audio/bottleclink1.wav",  8, 0.1f);
        loadSfx("blackholehit", "/audio/warp5.wav",        18, 0.4f);
        loadSfx("explosion",    "/audio/explosion1.wav",    6, 0.5f);
        loadSfx("glassbreak",   "/audio/glassbreak6.wav",   6, 0.9f);
        loadSfx("pickup",       "/audio/warp.wav",          4, 0.5f);
        loadSfx("enemyShoot",   "/audio/shoot3rev.wav",       10, 0.5f);
        loadSfx("playerhit",    "/audio/explosion1.wav",    4, 0.6f);

        refreshVolumes();
    }

    public static void loadSfx(String key, String resourcePath, int poolSize) {
        loadSfx(key, resourcePath, poolSize, 1.0f);
    }

    public static void loadSfx(String key, String resourcePath, int poolSize, float baseVol) {
        SoundPool old = sfx.remove(key);
        if (old != null) {
            old.close();
        }

        baseVolumes.put(key, clamp(baseVol));
        SoundPool pool = new SoundPool(resourcePath, poolSize);
        sfx.put(key, pool);
        pool.setVolume(masterVolume * sfxVolume * clamp(baseVol));
    }

    public static void playSfx(String key) {
        SoundPool pool = sfx.get(key);
        if (pool != null) {
            pool.play();
        }
    }

    public static void playSfx(String key, float extraVolume) {
        SoundPool pool = sfx.get(key);
        if (pool != null) {
            pool.play(extraVolume);
        }
    }

    public static void stopSfx(String key) {
        SoundPool pool = sfx.get(key);
        if (pool != null) {
            pool.stopAll();
        }
    }

    public static void playMusicLoop() {
        music.playLoop();
    }

    public static void playMusicOnce() {
        music.playOnce();
    }

    public static void stopMusic() {
        music.stop();
    }

    public static void pauseMusic() {
        music.pause();
    }

    public static void resumeMusic() {
        music.resume();
    }

    public static void setMasterVolume(float v) {
        masterVolume = clamp(v);
        refreshVolumes();
    }

    public static void setSfxVolume(float v) {
        sfxVolume = clamp(v);
        refreshVolumes();
    }

    public static void setMusicVolume(float v) {
        musicVolume = clamp(v);
        refreshVolumes();
    }

    public static float getMasterVolume() {
        return masterVolume;
    }

    public static float getSfxVolume() {
        return sfxVolume;
    }

    public static float getMusicVolume() {
        return musicVolume;
    }

    public static void shutdown() {
        for (SoundPool pool : sfx.values()) {
            pool.close();
        }
        sfx.clear();
        music.close();
    }

    private static void refreshVolumes() {
        float musicFinal = masterVolume * musicVolume;

        for (Map.Entry<String, SoundPool> entry : sfx.entrySet()) {
            Float base = baseVolumes.get(entry.getKey());
            float b = (base != null) ? base : 1.0f;
            entry.getValue().setVolume(masterVolume * sfxVolume * b);
        }

        music.setVolume(musicFinal);
    }

    private static float clamp(float v) {
        return Math.max(0f, Math.min(1f, v));
    }
}