package game;

import java.awt.Point;

public class EnemyPaths {

    private static double lerp(double a, double b, double u) {
        return a + (b - a) * u;
    }

    private static double smooth(double u) {
        return u * u * (3 - 2 * u);
    }

    /**
     * Standard sine-wave sweep path.
     * Enemies enter smoothly from off-screen, sweep across with sine motion,
     * exit, then come back the other way. The sine amplitude ramps up/down
     * at the edges so there's no abrupt straight→wave transition.
     */
    public static Point getWave1Path(double t, int width) {
        double rowHeight = 100;
        int cycle = (int)(t / 260.0);
        double localT = t % 520.0;
        double baseY = 120 + cycle * rowHeight;
        double amp = 32;

        double x, y;
        // Half-cycle: 260 frames total
        // 0–260: right-to-left, 260–520: left-to-right
        boolean leftward = localT < 260;
        double ht = leftward ? localT : localT - 260;  // 0..260

        // Smooth S-curve across the full 260 frames
        double u = ht / 260.0;
        double e = smooth(u);

        if (leftward) {
            x = lerp(width + 80, -80, e);
        } else {
            x = lerp(-80, width + 80, e);
        }

        // Sine wave with amplitude that fades at the edges
        double edgeFade = Math.sin(u * Math.PI); // 0 at edges, 1 in middle
        y = baseY + Math.sin(u * Math.PI * 4) * amp * edgeFade;

        return new Point((int)Math.round(x), (int)Math.round(y));
    }

    /**
     * Tight figure-8 path — enemies weave in a tighter pattern
     * with more vertical movement.
     */
    public static Point getWave2Path(double t, int width) {
        double rowHeight = 170;
        int cycle = (int)(t / 300.0);
        double localT = t % 600.0;
        double baseY = 90 + cycle * rowHeight;
        double amp = 45;

        boolean leftward = localT < 300;
        double ht = leftward ? localT : localT - 300;
        double u = ht / 300.0;
        double e = centerSlow(u);  // slow in center

        double x, y;
        if (leftward) {
            x = lerp(width + 80, -80, e);
        } else {
            x = lerp(-80, width + 80, e);
        }

        double edgeFade = Math.sin(u * Math.PI);
        y = baseY + Math.sin(u * Math.PI * 6) * amp * edgeFade;

        return new Point((int)Math.round(x), (int)Math.round(y));
    }

    /**
     * Diagonal dive path — enemies sweep across at an angle,
     * diving deeper into the screen before pulling back up.
     */
    public static Point getWave3Path(double t, int width) {
        double rowHeight = 180;
        int cycle = (int)(t / 280.0);
        double localT = t % 560.0;
        double baseY = 70 + cycle * rowHeight;
        double amp = 35;

        boolean leftward = localT < 280;
        double ht = leftward ? localT : localT - 280;
        double u = ht / 280.0;
        double e = centerSlow(u);  // slow in center

        double x, y;
        if (leftward) {
            x = lerp(width + 80, -80, e);
        } else {
            x = lerp(-80, width + 80, e);
        }

        double edgeFade = Math.sin(u * Math.PI);
        double dive = Math.sin(u * Math.PI) * 90;
        double wobble = Math.sin(u * Math.PI * 5) * amp * edgeFade;
        y = baseY + dive + wobble;

        return new Point((int)Math.round(x), (int)Math.round(y));
    }

    /**
     * Generic dispatcher — call the right path by wave type.
     */
    public static Point getPath(int waveType, double t, int width) {
        switch (waveType) {
            case 2: return getWave2Path(t, width);
            case 3: return getWave3Path(t, width);
            default: return getWave1Path(t, width);
        }
    }
}