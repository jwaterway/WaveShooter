package game;

import java.awt.Point;

public class EnemyPaths {

    private static final int MARGIN = 60; // how close to edge enemies can go

    private static double lerp(double a, double b, double u) {
        return a + (b - a) * u;
    }

    private static double smooth(double u) {
        return u * u * (3 - 2 * u);
    }

    /** Slow in center, fast at edges — lingers where enemies are visible. */
    private static double centerSlow(double u) {
        return 0.5 - 0.5 * Math.cos(u * Math.PI);
    }

    /**
     * Standard sine-wave sweep path.
     * First pass enters from off-screen, then bounces within screen bounds.
     */
    public static Point getWave1Path(double t, int width) {
        double rowHeight = 200;
        double halfCycle = 260.0;
        double fullCycle = halfCycle * 2;
        int cycle = (int)(t / halfCycle);
        double localT = t % fullCycle;
        double baseY = 120 + (t / fullCycle) * rowHeight;
        double amp = 18;

        boolean leftward = localT < halfCycle;
        double ht = leftward ? localT : localT - halfCycle;
        double u = ht / halfCycle;
        double e = smooth(u);

        double x;
        if (cycle == 0) {
            // First pass: enter from off-screen right to left edge
            x = lerp(width + 80, MARGIN, e);
        } else if (leftward) {
            x = lerp(width - MARGIN, MARGIN, e);
        } else {
            x = lerp(MARGIN, width - MARGIN, e);
        }

        double edgeFade = Math.sin(u * Math.PI);
        double y = baseY + Math.sin(u * Math.PI * 4) * amp * edgeFade;

        return new Point((int)Math.round(x), (int)Math.round(y));
    }

    /**
     * Tight figure-8 path — stays on-screen after entry.
     */
    public static Point getWave2Path(double t, int width) {
        double rowHeight = 280;
        double halfCycle = 300.0;
        double fullCycle = halfCycle * 2;
        int cycle = (int)(t / halfCycle);
        double localT = t % fullCycle;
        double baseY = 90 + (t / fullCycle) * rowHeight;
        double amp = 20;

        boolean leftward = localT < halfCycle;
        double ht = leftward ? localT : localT - halfCycle;
        double u = ht / halfCycle;
        double e = centerSlow(u);

        double x;
        if (cycle == 0) {
            x = lerp(width + 80, MARGIN, e);
        } else if (leftward) {
            x = lerp(width - MARGIN, MARGIN, e);
        } else {
            x = lerp(MARGIN, width - MARGIN, e);
        }

        double edgeFade = Math.sin(u * Math.PI);
        double y = baseY + Math.sin(u * Math.PI * 6) * amp * edgeFade;

        return new Point((int)Math.round(x), (int)Math.round(y));
    }

    /**
     * Diagonal dive path — stays on-screen after entry.
     */
    public static Point getWave3Path(double t, int width) {
        double rowHeight = 300;
        double halfCycle = 280.0;
        double fullCycle = halfCycle * 2;
        int cycle = (int)(t / halfCycle);
        double localT = t % fullCycle;
        double baseY = 70 + (t / fullCycle) * rowHeight;
        double amp = 20;

        boolean leftward = localT < halfCycle;
        double ht = leftward ? localT : localT - halfCycle;
        double u = ht / halfCycle;
        double e = centerSlow(u);

        double x;
        if (cycle == 0) {
            x = lerp(width + 80, MARGIN, e);
        } else if (leftward) {
            x = lerp(width - MARGIN, MARGIN, e);
        } else {
            x = lerp(MARGIN, width - MARGIN, e);
        }

        double edgeFade = Math.sin(u * Math.PI);
        double dive = Math.sin(u * Math.PI) * 90;
        double wobble = Math.sin(u * Math.PI * 5) * amp * edgeFade;
        double y = baseY + dive + wobble;

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