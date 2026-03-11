package game;

import javax.sound.sampled.FloatControl;

public final class VolumeUtil {
    private VolumeUtil() {}

    /**
     * volume = 0.0 to 1.0
     */
    public static void setClipVolume(FloatControl gainControl, float volume) {
        volume = Math.max(0.0001f, Math.min(1.0f, volume));

        float min = gainControl.getMinimum();
        float max = gainControl.getMaximum();

        float dB = (float) (20.0 * Math.log10(volume));
        dB = Math.max(min, Math.min(max, dB));

        gainControl.setValue(dB);
    }
}