package game;

import javax.sound.sampled.*;

public class ToneGenerator {
    private static final float SAMPLE_RATE = 44100f;

    public enum WaveType { SINE, SQUARE, TRIANGLE }

    public static void playTone(double freq, int durationMs, WaveType type) {
        new Thread(() -> {
            try {
                AudioFormat format = new AudioFormat(
                        SAMPLE_RATE,     // sample rate
                        16,              // sample size in bits
                        1,               // mono
                        true,            // signed
                        false            // little endian
                );

                DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
                try (SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info)) {
                    line.open(format);
                    line.start();

                    int numSamples = (int)(durationMs * SAMPLE_RATE / 1000);
                    byte[] buf = new byte[2 * numSamples];

                    for (int i = 0; i < numSamples; i++) {
                        double angle = 2.0 * Math.PI * i * freq / SAMPLE_RATE;
                        short sample;

                        switch (type) {
                            case SQUARE:
                                sample = (short)((Math.sin(angle) >= 0 ? 1 : -1) * Short.MAX_VALUE);
                                break;
                            case TRIANGLE:
                                sample = (short)((2.0 / Math.PI) * Math.asin(Math.sin(angle)) * Short.MAX_VALUE);
                                break;
                            default: // SINE
                                sample = (short)(Math.sin(angle) * Short.MAX_VALUE);
                        }

                        buf[2*i]   = (byte)(sample & 0xFF);
                        buf[2*i+1] = (byte)((sample >> 8) & 0xFF);
                    }

                    line.write(buf, 0, buf.length);
                    line.drain();
                    line.stop();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}
