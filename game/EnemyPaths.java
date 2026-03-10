package game;

import java.awt.Point;

public class EnemyPaths {

    private static double lerp(double a, double b, double u) {
        return a + (b - a) * u;
    }

    private static double smooth(double u) {
        return u * u * (3 - 2 * u);
    }

    private static double easeIn(double u) {
        return u * u;
    }

    private static double easeOut(double u) {
        return 1 - (1 - u) * (1 - u);
    }

    public static Point getWave1Path(double t, int width) {
        double x, y;

        double rowHeight = 100;
        int cycle = (int)(t / 260.0);
        double localT = t % 520.0;
        double baseY = 120 + cycle * rowHeight;
        double amp = 32;

        if (localT < 40) {
            double u = localT / 40.0;
            u = easeIn(u);
            x = lerp(width + 120, width * 0.80, u);
            y = baseY;

        } else if (localT < 220) {
            double u = (localT - 40) / 180.0;
            double e = smooth(u);
            x = lerp(width * 0.80, width * 0.20, e);
            y = baseY + Math.sin(u * Math.PI * 4) * amp;

        } else if (localT < 260) {
            double u = (localT - 220) / 40.0;
            u = easeOut(u);
            x = lerp(width * 0.20, -120, u);
            y = baseY;

        } else if (localT < 300) {
            double u = (localT - 260) / 40.0;
            u = easeIn(u);
            x = lerp(-120, width * 0.20, u);
            y = baseY;

        } else if (localT < 480) {
            double u = (localT - 300) / 180.0;
            double e = smooth(u);
            x = lerp(width * 0.20, width * 0.80, e);
            y = baseY + Math.sin(u * Math.PI * 4) * amp;

        } else {
            double u = (localT - 480) / 40.0;
            u = easeOut(u);
            x = lerp(width * 0.80, width + 120, u);
            y = baseY;
        }

        return new Point((int)Math.round(x), (int)Math.round(y));
    }
}