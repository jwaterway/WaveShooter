package game;

import javax.sound.sampled.*;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

public class SoundPool {
    private final Clip[] clips;
    private int nextIndex = 0;
    private float volume = 1.0f;

    /**
     * @param resourcePath classpath resource like "/audio/shoot1.wav"
     * @param poolSize number of overlapping copies allowed
     */
    public SoundPool(String resourcePath, int poolSize) {
        if (poolSize <= 0) {
            throw new IllegalArgumentException("poolSize must be > 0");
        }

        clips = new Clip[poolSize];

        for (int i = 0; i < poolSize; i++) {
            clips[i] = loadClip(resourcePath);
            applyVolume(clips[i], volume);
        }
    }

    public void play() {
        play(1.0f);
    }

    /**
     * extraVolume multiplies this pool's base volume
     */
    public synchronized void play(float extraVolume) {
        Clip clip = clips[nextIndex];
        nextIndex = (nextIndex + 1) % clips.length;

        if (clip.isRunning()) {
            clip.stop();
        }

        clip.setFramePosition(0);
        applyVolume(clip, clamp(volume * extraVolume));
        clip.start();
    }

    public void stopAll() {
        for (Clip clip : clips) {
            if (clip.isRunning()) {
                clip.stop();
            }
            clip.setFramePosition(0);
        }
    }

    public void setVolume(float volume) {
        this.volume = clamp(volume);
        for (Clip clip : clips) {
            applyVolume(clip, this.volume);
        }
    }

    public void close() {
        for (Clip clip : clips) {
            if (clip != null) {
                clip.stop();
                clip.close();
            }
        }
    }

    private static Clip loadClip(String resourcePath) {
        try {
            InputStream raw = SoundPool.class.getResourceAsStream(resourcePath);
            if (raw == null) {
                throw new IllegalArgumentException("Audio resource not found: " + resourcePath);
            }

            try (BufferedInputStream buffered = new BufferedInputStream(raw);
                 AudioInputStream original = AudioSystem.getAudioInputStream(buffered)) {

                AudioFormat baseFormat = original.getFormat();
                AudioFormat decodedFormat = new AudioFormat(
                        AudioFormat.Encoding.PCM_SIGNED,
                        baseFormat.getSampleRate(),
                        16,
                        baseFormat.getChannels(),
                        baseFormat.getChannels() * 2,
                        baseFormat.getSampleRate(),
                        false
                );

                try (AudioInputStream decoded = AudioSystem.getAudioInputStream(decodedFormat, original)) {
                    Clip clip = AudioSystem.getClip();
                    clip.open(decoded);
                    return clip;
                }
            }
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            throw new RuntimeException("Failed to load clip: " + resourcePath, e);
        }
    }

    private static void applyVolume(Clip clip, float volume) {
        if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            VolumeUtil.setClipVolume(gain, volume);
        }
    }

    private static float clamp(float v) {
        return Math.max(0f, Math.min(1f, v));
    }
}