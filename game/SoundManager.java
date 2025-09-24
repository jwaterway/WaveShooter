package game;

import javax.sound.sampled.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SoundManager {
    private static final int SAMPLE_RATE = 44100; // CD-quality
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public enum WaveType { SINE, SQUARE, TRIANGLE }
   


    public void playTone(double freq, int durationMs, WaveType type) {
        executor.submit(() -> {
            try {
                byte[] buffer = generateWave(freq, durationMs, type);
                AudioFormat format = new AudioFormat(
                        SAMPLE_RATE,
                        16,     // 16-bit
                        1,      // mono
                        true,   // signed
                        false   // little-endian
                );

                try (SourceDataLine line = AudioSystem.getSourceDataLine(format)) {
                    line.open(format);
                    line.start();
                    line.write(buffer, 0, buffer.length);
                    line.drain();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
    
    private byte[] generateWave(double freq, int durationMs, WaveType type) {
        int length = (int) ((durationMs / 1000.0) * SAMPLE_RATE);
        byte[] buffer = new byte[length * 2]; // 16-bit → 2 bytes per sample

        for (int i = 0; i < length; i++) {
            double t = i / (double) SAMPLE_RATE;
            double value = 0;

            switch (type) {
                case SINE:
                    value = Math.sin(2 * Math.PI * freq * t);
                    break;
                case SQUARE:
                    value = Math.signum(Math.sin(2 * Math.PI * freq * t));
                    break;
                case TRIANGLE:
                    value = 2 * Math.abs(2 * ((t * freq) % 1) - 1) - 1;
                    break;
            }

            // scale to 16-bit signed PCM
            short sample = (short) (value * Short.MAX_VALUE);
            buffer[2 * i]     = (byte) (sample & 0xff);
            buffer[2 * i + 1] = (byte) ((sample >> 8) & 0xff);
        }
        return buffer;
    }
}
