package game;

import javax.sound.sampled.*;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

public class MusicPlayer {
    private Clip clip;
    private float volume = 0.7f;

    public void load(String resourcePath) {
        close();

        try {
            InputStream raw = MusicPlayer.class.getResourceAsStream(resourcePath);
            if (raw == null) {
                throw new IllegalArgumentException("Music resource not found: " + resourcePath);
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
                    clip = AudioSystem.getClip();
                    clip.open(decoded);
                    applyVolume();
                }
            }
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            throw new RuntimeException("Failed to load music: " + resourcePath, e);
        }
    }

    public void playLoop() {
        if (clip == null) return;

        if (clip.isRunning()) {
            clip.stop();
        }

        clip.setFramePosition(0);
        clip.loop(Clip.LOOP_CONTINUOUSLY);
        clip.start();
    }

    public void playOnce() {
        if (clip == null) return;

        if (clip.isRunning()) {
            clip.stop();
        }

        clip.setFramePosition(0);
        clip.start();
    }

    public void stop() {
        if (clip != null && clip.isRunning()) {
            clip.stop();
        }
    }

    public void pause() {
        if (clip != null && clip.isRunning()) {
            clip.stop();
        }
    }

    public void resume() {
        if (clip != null && !clip.isRunning()) {
            clip.start();
        }
    }

    public void setVolume(float volume) {
        this.volume = Math.max(0f, Math.min(1f, volume));
        applyVolume();
    }

    public float getVolume() {
        return volume;
    }

    public void close() {
        if (clip != null) {
            clip.stop();
            clip.close();
            clip = null;
        }
    }

    private void applyVolume() {
        if (clip != null && clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            VolumeUtil.setClipVolume(gain, volume);
        }
    }
}