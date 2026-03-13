package game;

import javax.sound.sampled.*;

/**
 * Continuous synthesized engine hum whose pitch rises with player speed.
 * Runs on a background daemon thread, writing small PCM buffers in real time.
 */
public class EngineSound {
    private static final float SAMPLE_RATE = 44100f;
    private static final int BUFFER_FRAMES = 512; // ~11.6ms per write

    private volatile double baseFreq = 55;   // idle hum (Hz)
    private volatile double maxFreq  = 200;  // full-speed whine (Hz)
    private volatile double speedRatio = 0;  // 0..1 (set from game loop)
    private volatile float  volume = 0.8f;  // 0..1
    private volatile boolean running = true;

    private Thread thread;

    public void start() {
        thread = new Thread(this::loop, "EngineSound");
        thread.setDaemon(true);
        thread.start();
    }

    /** Call every frame: speedRatio in 0..1 (0 = idle, 1 = max speed). */
    public void setSpeedRatio(double r) {
        this.speedRatio = Math.max(0, Math.min(1, r));
    }

    public void setVolume(float v) {
        this.volume = Math.max(0f, Math.min(1f, v));
    }

    public void stop() {
        running = false;
        if (thread != null) {
            thread.interrupt();
        }
    }

    private void loop() {
        try {
            AudioFormat fmt = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, fmt);
            SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(fmt, BUFFER_FRAMES * 4);
            line.start();

            byte[] buf = new byte[BUFFER_FRAMES * 2]; // 16-bit mono
            double phase = 0;

            while (running) {
                double ratio = speedRatio;
                double freq = baseFreq + (maxFreq - baseFreq) * ratio;
                // Volume ramps: quiet at idle, louder when moving
                double vol = volume * (0.3 + 0.7 * ratio);

                for (int i = 0; i < BUFFER_FRAMES; i++) {
                    // Mix a fundamental sine + a quieter harmonic for richness
                    double s = Math.sin(phase * 2 * Math.PI);
                    double h = Math.sin(phase * 4 * Math.PI) * 0.3;
                    // Add slight grit with a soft-clipped sawtooth undertone
                    double saw = (phase % 1.0) * 2 - 1;
                    double mix = (s + h + saw * 0.15) / 1.45; // normalize

                    short sample = (short)(mix * vol * Short.MAX_VALUE);
                    buf[2 * i]     = (byte)(sample & 0xFF);
                    buf[2 * i + 1] = (byte)((sample >> 8) & 0xFF);

                    phase += freq / SAMPLE_RATE;
                    if (phase > 1e6) phase -= 1e6; // prevent overflow
                }

                line.write(buf, 0, buf.length);
            }

            line.drain();
            line.stop();
            line.close();
        } catch (LineUnavailableException e) {
            // Audio line not available — silently degrade
        }
    }
}
