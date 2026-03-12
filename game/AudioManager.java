package game;

import java.util.HashMap;
import java.util.Map;

public final class AudioManager {
    private static final Map<String, SoundPool> sfx = new HashMap<>();
    private static final MusicPlayer music = new MusicPlayer();

    private static float masterVolume = 1.0f;
    private static float sfxVolume = .15f;
    private static float musicVolume = 0.5f;

    private AudioManager() {}

    public static void init() {
        // register your sound effects here
        loadSfx("shootTri", "/audio/shoot2.wav", 10);
        loadSfx("shootSqr", "/audio/shoot4.wav", 10);
        loadSfx("shootSin", "/audio/shoot6.wav", 10);
        loadSfx("switchRay", "/audio/longshot2rev.wav", 10);
        loadSfx("hitTri", "/audio/hit2.wav", 8);
        loadSfx("hitSqr", "/audio/hit3.wav", 8);
        loadSfx("hitSin", "/audio/hit1.wav", 8);
        loadSfx("blackholehit", "/audio/warp5.wav", 18);
        loadSfx("explosion", "/audio/explosion1.wav", 6);
        loadSfx("pickup", "/audio/warp.wav", 4);

        // load music here
        //music.load("/audio/darkalientexture.wav");

        refreshVolumes();
    }

    public static void loadSfx(String key, String resourcePath, int poolSize) {
        SoundPool old = sfx.remove(key);
        if (old != null) {
            old.close();
        }

        SoundPool pool = new SoundPool(resourcePath, poolSize);
        sfx.put(key, pool);
        pool.setVolume(masterVolume * sfxVolume);
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
        float sfxFinal = masterVolume * sfxVolume;
        float musicFinal = masterVolume * musicVolume;

        for (SoundPool pool : sfx.values()) {
            pool.setVolume(sfxFinal);
        }

        music.setVolume(musicFinal);
    }

    private static float clamp(float v) {
        return Math.max(0f, Math.min(1f, v));
    }
}