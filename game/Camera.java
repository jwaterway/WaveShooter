package game;

/**
 * Camera tracks a world-space viewport that auto-scrolls forward and drifts
 * left/right based on the player's screen position.
 *
 * When scrolling is enabled, game objects live in world coordinates and the
 * camera offset is subtracted at draw time via g2.translate(-x, -y).
 *
 * The camera's "forward" direction is to the RIGHT (+X world axis) so the
 * level scrolls left-to-right.  The player's vertical position on screen
 * biases camera Y drift so moving up/down lets you explore vertically.
 */
public class Camera {

    /** World position of the top-left corner of the viewport. */
    private double x, y;

    /** Auto-scroll speed in pixels per frame (horizontal / forward). */
    private double scrollSpeed = 8.0;   // ~480 px/sec at 60fps

    /** How much player position biases lateral (Y) camera drift. */
    private double lateralDriftStrength = 0.12;

    /** Viewport size (screen dimensions). */
    private final int viewW, viewH;

    /** Whether scrolling is active. When false the camera stays at (0,0). */
    private boolean enabled = false;

    public Camera(int viewWidth, int viewHeight) {
        this.viewW = viewWidth;
        this.viewH = viewHeight;
        this.x = 0;
        this.y = 0;
    }

    // ── Configuration ───────────────────────────────────────

    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public boolean isEnabled() { return enabled; }

    public void setScrollSpeed(double speed) { this.scrollSpeed = speed; }
    public double getScrollSpeed() { return scrollSpeed; }

    /** Reset camera to origin. */
    public void reset() { x = 0; y = 0; }

    // ── Per-frame update ────────────────────────────────────

    /**
     * Call once per frame.
     * @param playerScreenX  player's X position in screen space (0..viewW)
     * @param playerScreenY  player's Y position in screen space (0..viewH)
     */
    public void update(double playerScreenX, double playerScreenY) {
        if (!enabled) return;

        // Auto-scroll forward (world +X)
        x += scrollSpeed;

        // Lateral drift: player position relative to screen center biases Y
        double centerY = viewH / 2.0;
        double offsetY = (playerScreenY - centerY) / centerY;  // -1..+1
        y += offsetY * lateralDriftStrength * viewH * 0.02;

        // Optional: slight forward boost when player is near right side
        double centerX = viewW / 2.0;
        double offsetX = (playerScreenX - centerX) / centerX;  // -1..+1
        // Subtle: push forward a tiny bit faster or slower
        // (disabled for now — pure constant scroll feels better)
        // x += offsetX * scrollSpeed * 0.15;
    }

    // ── Accessors ───────────────────────────────────────────

    /** World X of the left edge of the viewport. */
    public double getX() { return x; }
    /** World Y of the top edge of the viewport. */
    public double getY() { return y; }

    /** Convert a world-X to screen-X. */
    public double toScreenX(double worldX) { return worldX - x; }
    /** Convert a world-Y to screen-Y. */
    public double toScreenY(double worldY) { return worldY - y; }

    /** Convert screen-X to world-X. */
    public double toWorldX(double screenX) { return screenX + x; }
    /** Convert screen-Y to world-Y. */
    public double toWorldY(double screenY) { return screenY + y; }

    /** Right edge of the viewport in world coordinates. */
    public double getRightEdge() { return x + viewW; }
    /** Bottom edge of the viewport in world coordinates. */
    public double getBottomEdge() { return y + viewH; }

    /** Check if a world-space circle is visible on screen (with margin). */
    public boolean isVisible(double wx, double wy, double radius) {
        double margin = radius + 100;
        return wx + margin > x && wx - margin < x + viewW
            && wy + margin > y && wy - margin < y + viewH;
    }

    /** How far the camera has scrolled forward (total distance). */
    public double getDistance() { return x; }
}
