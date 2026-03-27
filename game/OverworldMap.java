package game;

import java.awt.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.Random;

/**
 * Cyberpunk-styled overworld map with neon nodes, glowing connections,
 * animated starfield, and a Star Fox-inspired level selection flow.
 */
public class OverworldMap {

    // ── Node data ──────────────────────────────────────────────
    static class MapNode {
        String name;
        String subtitle;       // short flavour text
        int x, y;              // screen position
        Color baseColor;       // neon accent colour
        boolean unlocked;
        int levelIndex;        // which currentLevel this maps to
        int[] connections;     // indices of connected nodes

        MapNode(String name, String subtitle, int x, int y,
                Color baseColor, int levelIndex, int[] connections) {
            this.name = name;
            this.subtitle = subtitle;
            this.x = x;
            this.y = y;
            this.baseColor = baseColor;
            this.levelIndex = levelIndex;
            this.connections = connections;
            this.unlocked = false;
        }
    }

    // ── Background star particles ──────────────────────────────
    private static class MapStar {
        double x, y, speed, brightness;
        MapStar(Random r, int w, int h) {
            x = r.nextDouble() * w;
            y = r.nextDouble() * h;
            speed = 0.1 + r.nextDouble() * 0.4;
            brightness = 0.3 + r.nextDouble() * 0.7;
        }
    }

    // ── Nebula blob for background ─────────────────────────────
    private static class MapNebula {
        double x, y, radius;
        Color color;
        MapNebula(double x, double y, double radius, Color c) {
            this.x = x; this.y = y; this.radius = radius; this.color = c;
        }
    }

    // ── Fields ────────────────────────────────────────────────
    private final MapNode[] nodes;
    private final ArrayList<MapStar> bgStars = new ArrayList<>();
    private final ArrayList<MapNebula> bgNebulae = new ArrayList<>();
    private int selectedIndex = 0;
    private int highestUnlocked = 0;   // index of furthest unlocked node
    private int tick = 0;              // animation counter
    private final Random rng = new Random();

    // Transition / intro animation
    private int introTimer = 0;
    private static final int INTRO_FRAMES = 60;  // 1 second fade-in

    // Colours
    private static final Color CYAN    = new Color(0, 255, 255);
    private static final Color MAGENTA = new Color(255, 0, 200);
    private static final Color PURPLE  = new Color(160, 60, 255);
    private static final Color PINK    = new Color(255, 80, 200);
    private static final Color BLUE    = new Color(40, 120, 255);
    private static final Color NEON_GREEN = new Color(0, 255, 160);
    private static final Color ORANGE  = new Color(255, 140, 40);
    private static final Color LOCKED  = new Color(50, 55, 70);

    private static final int W = GamePanel.WIDTH;
    private static final int H = GamePanel.HEIGHT;

    // ── Constructor ───────────────────────────────────────────
    public OverworldMap() {
        // Define all map nodes – branching path left→right
        nodes = new MapNode[] {
            //  0  Start
            new MapNode("NEXUS GATE",   "Training Sector",
                    180, H/2, CYAN, 1, new int[]{1, 2}),
            //  1  Upper path
            new MapNode("PLASMA DRIFT", "Ion Storm Zone",
                    460, H/2 - 200, MAGENTA, 2, new int[]{3}),
            //  2  Lower path
            new MapNode("VOID STATION", "Lunar Outpost",
                    460, H/2 + 200, BLUE, 3, new int[]{4}),
            //  3  Upper mid
            new MapNode("NEON ABYSS",   "Deep Nebula Core",
                    760, H/2 - 280, PURPLE, 3, new int[]{5}),
            //  4  Lower mid
            new MapNode("CHROME REEF",  "Asteroid Belt",
                    760, H/2 + 160, PINK, 3, new int[]{5, 6}),
            //  5  Convergence
            new MapNode("SIGNAL PRIME", "Relay Hub",
                    1060, H/2 - 60, NEON_GREEN, 4, new int[]{7}),
            //  6  Alternate lower
            new MapNode("DARK CIRCUIT", "Abandoned Grid",
                    1060, H/2 + 240, ORANGE, 4, new int[]{7}),
            //  7  Pre-final
            new MapNode("ZERO POINT",   "Singularity Edge",
                    1380, H/2 + 40, MAGENTA, 5, new int[]{8}),
            //  8  Final
            new MapNode("OMEGA CORE",   "The Last Frontier",
                    1700, H/2, new Color(255, 60, 60), 6, new int[]{}),
        };

        // Start with first node unlocked
        nodes[0].unlocked = true;
        selectedIndex = 0;

        // Spawn background stars
        for (int i = 0; i < 300; i++) {
            bgStars.add(new MapStar(rng, W, H));
        }

        // Spawn background nebula blobs
        bgNebulae.add(new MapNebula(W * 0.2, H * 0.3, 320, new Color(80, 0, 160, 20)));
        bgNebulae.add(new MapNebula(W * 0.65, H * 0.7, 280, new Color(0, 60, 160, 18)));
        bgNebulae.add(new MapNebula(W * 0.85, H * 0.25, 200, new Color(160, 0, 100, 15)));
        bgNebulae.add(new MapNebula(W * 0.45, H * 0.5, 350, new Color(0, 100, 140, 12)));
    }

    // ── Public API ────────────────────────────────────────────
    public void resetIntro() {
        introTimer = 0;
    }

    /** Unlock nodes reachable from completed level. */
    public void unlockLevel(int levelIndex) {
        for (int i = 0; i < nodes.length; i++) {
            if (nodes[i].unlocked && nodes[i].levelIndex <= levelIndex) {
                for (int ci : nodes[i].connections) {
                    nodes[ci].unlocked = true;
                }
            }
        }
        // Track highest for selection
        for (int i = 0; i < nodes.length; i++) {
            if (nodes[i].unlocked) highestUnlocked = i;
        }
    }

    /** Move selection cursor. Returns true if selection changed. */
    public boolean navigate(int direction) {
        // direction: -1 = left/up, +1 = right/down
        MapNode cur = nodes[selectedIndex];
        if (direction > 0) {
            // Move to first unlocked connection
            for (int ci : cur.connections) {
                if (nodes[ci].unlocked) { selectedIndex = ci; return true; }
            }
        } else {
            // Move backwards – find a node that connects to us
            for (int i = 0; i < nodes.length; i++) {
                if (i == selectedIndex) continue;
                for (int ci : nodes[i].connections) {
                    if (ci == selectedIndex && nodes[i].unlocked) {
                        selectedIndex = i;
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** Navigate vertically between sibling branches (nodes with the same parent). */
    public boolean navigateVertical(int direction) {
        // Find parent node (a node that connects to current)
        int parentIdx = -1;
        for (int i = 0; i < nodes.length; i++) {
            for (int ci : nodes[i].connections) {
                if (ci == selectedIndex) { parentIdx = i; break; }
            }
            if (parentIdx >= 0) break;
        }
        if (parentIdx < 0) return false;

        // Find siblings (other nodes connected from same parent)
        MapNode parent = nodes[parentIdx];
        int currentY = nodes[selectedIndex].y;
        int bestIdx = -1;
        int bestDist = Integer.MAX_VALUE;
        for (int ci : parent.connections) {
            if (ci == selectedIndex) continue;
            if (!nodes[ci].unlocked) continue;
            int dy = nodes[ci].y - currentY;
            // direction > 0 means down (want positive dy), < 0 means up (want negative dy)
            if ((direction > 0 && dy > 0) || (direction < 0 && dy < 0)) {
                if (Math.abs(dy) < bestDist) {
                    bestDist = Math.abs(dy);
                    bestIdx = ci;
                }
            }
        }
        if (bestIdx >= 0) { selectedIndex = bestIdx; return true; }
        return false;
    }

    /** Get the name of the selected node. */
    public String getSelectedName() {
        return nodes[selectedIndex].name;
    }

    /** Get currently selected level index. */
    public int getSelectedLevel() {
        return nodes[selectedIndex].levelIndex;
    }

    /** Is the selected node unlocked? */
    public boolean isSelectedUnlocked() {
        return nodes[selectedIndex].unlocked;
    }

    // ── Update (call every frame while map is shown) ──────────
    public void update() {
        tick++;
        if (introTimer < INTRO_FRAMES) introTimer++;

        // Drift stars slowly
        for (MapStar s : bgStars) {
            s.x -= s.speed;
            if (s.x < 0) { s.x = W; s.y = rng.nextDouble() * H; }
        }
    }

    // ── Draw ──────────────────────────────────────────────────
    public void draw(Graphics2D g2) {
        // Intro fade
        float introAlpha = Math.min(1f, introTimer / (float) INTRO_FRAMES);
        Composite origComposite = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, introAlpha));

        drawBackground(g2);
        drawGrid(g2);
        drawConnections(g2);
        drawNodes(g2);
        drawHUD(g2);

        g2.setComposite(origComposite);
    }

    // ── Background ────────────────────────────────────────────
    private void drawBackground(Graphics2D g2) {
        // Deep space gradient
        GradientPaint bg = new GradientPaint(0, 0, new Color(5, 2, 20),
                                             W, H, new Color(15, 5, 35));
        g2.setPaint(bg);
        g2.fillRect(0, 0, W, H);

        // Nebula blobs
        for (MapNebula n : bgNebulae) {
            float r = (float) n.radius;
            // Subtle breathing
            float breathe = (float)(Math.sin(tick * 0.01 + n.x * 0.01) * 20);
            r += breathe;
            RadialGradientPaint rgp = new RadialGradientPaint(
                new Point2D.Double(n.x, n.y), r,
                new float[]{0f, 0.5f, 1f},
                new Color[]{
                    new Color(n.color.getRed(), n.color.getGreen(), n.color.getBlue(), 30),
                    new Color(n.color.getRed(), n.color.getGreen(), n.color.getBlue(), 12),
                    new Color(0, 0, 0, 0)
                }
            );
            g2.setPaint(rgp);
            g2.fillOval((int)(n.x - r), (int)(n.y - r), (int)(r * 2), (int)(r * 2));
        }

        // Stars
        for (MapStar s : bgStars) {
            float pulse = (float)(0.5 + 0.5 * Math.sin(tick * 0.03 + s.x * 0.1));
            int alpha = (int)(s.brightness * pulse * 200);
            alpha = Math.max(20, Math.min(200, alpha));
            g2.setColor(new Color(180, 200, 255, alpha));
            g2.fillRect((int) s.x, (int) s.y, 1, 1);
            if (s.brightness > 0.8) {
                g2.fillRect((int) s.x + 1, (int) s.y, 1, 1);
            }
        }
    }

    // ── Grid lines (subtle cyber feel) ────────────────────────
    private void drawGrid(Graphics2D g2) {
        Stroke oldStroke = g2.getStroke();
        g2.setStroke(new BasicStroke(0.5f));
        int gridAlpha = 12 + (int)(4 * Math.sin(tick * 0.02));

        // Horizontal scanlines
        for (int y = 0; y < H; y += 60) {
            g2.setColor(new Color(0, 180, 255, gridAlpha));
            g2.drawLine(0, y, W, y);
        }
        // Vertical
        for (int x = 0; x < W; x += 60) {
            g2.setColor(new Color(180, 0, 255, gridAlpha));
            g2.drawLine(x, 0, x, H);
        }
        g2.setStroke(oldStroke);
    }

    // ── Connections between nodes ─────────────────────────────
    private void drawConnections(Graphics2D g2) {
        Stroke oldStroke = g2.getStroke();

        for (int i = 0; i < nodes.length; i++) {
            MapNode a = nodes[i];
            for (int ci : a.connections) {
                MapNode b = nodes[ci];
                boolean pathUnlocked = a.unlocked && b.unlocked;

                if (pathUnlocked) {
                    // Glowing neon line
                    // Outer glow
                    g2.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.setColor(new Color(a.baseColor.getRed(), a.baseColor.getGreen(),
                                          a.baseColor.getBlue(), 30));
                    g2.drawLine(a.x, a.y, b.x, b.y);

                    // Core line
                    g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.setColor(new Color(a.baseColor.getRed(), a.baseColor.getGreen(),
                                          a.baseColor.getBlue(), 160));
                    g2.drawLine(a.x, a.y, b.x, b.y);

                    // Travelling pulse dot
                    double pulseT = ((tick * 0.008 + i * 0.3) % 1.0);
                    double px = a.x + (b.x - a.x) * pulseT;
                    double py = a.y + (b.y - a.y) * pulseT;
                    g2.setColor(new Color(255, 255, 255, 200));
                    g2.fillOval((int)(px - 3), (int)(py - 3), 6, 6);
                } else {
                    // Dim dashed line
                    float[] dash = {8f, 12f};
                    g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_ROUND,
                            BasicStroke.JOIN_ROUND, 0, dash, tick * 0.3f % 20));
                    g2.setColor(new Color(60, 65, 80, 80));
                    g2.drawLine(a.x, a.y, b.x, b.y);
                }
            }
        }
        g2.setStroke(oldStroke);
    }

    // ── Draw each node ────────────────────────────────────────
    private void drawNodes(Graphics2D g2) {
        for (int i = 0; i < nodes.length; i++) {
            MapNode n = nodes[i];
            boolean selected = (i == selectedIndex);
            drawSingleNode(g2, n, selected, i);
        }
    }

    private void drawSingleNode(Graphics2D g2, MapNode node, boolean selected, int idx) {
        int cx = node.x;
        int cy = node.y;
        Color col = node.unlocked ? node.baseColor : LOCKED;

        // ── Outer ring glow (unlocked nodes) ──
        if (node.unlocked) {
            float pulse = (float)(0.6 + 0.4 * Math.sin(tick * 0.05 + idx * 1.2));
            int glowR = 40 + (int)(8 * pulse);
            RadialGradientPaint glow = new RadialGradientPaint(
                new Point2D.Float(cx, cy), glowR,
                new float[]{0f, 0.4f, 1f},
                new Color[]{
                    new Color(col.getRed(), col.getGreen(), col.getBlue(), (int)(80 * pulse)),
                    new Color(col.getRed(), col.getGreen(), col.getBlue(), (int)(30 * pulse)),
                    new Color(0, 0, 0, 0)
                }
            );
            g2.setPaint(glow);
            g2.fillOval(cx - glowR, cy - glowR, glowR * 2, glowR * 2);
        }

        // ── Selection ring ──
        if (selected) {
            Stroke old = g2.getStroke();
            float selPulse = (float)(0.5 + 0.5 * Math.sin(tick * 0.08));
            int selR = 30 + (int)(4 * selPulse);

            // Rotating dashes
            float[] dash = {10f, 6f};
            g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND, 0, dash, tick * 0.5f));
            g2.setColor(new Color(255, 255, 255, (int)(180 * selPulse + 60)));
            g2.drawOval(cx - selR, cy - selR, selR * 2, selR * 2);

            // Inner bright ring
            g2.setStroke(new BasicStroke(1.5f));
            g2.setColor(new Color(col.getRed(), col.getGreen(), col.getBlue(), 220));
            g2.drawOval(cx - selR + 4, cy - selR + 4, (selR - 4) * 2, (selR - 4) * 2);
            g2.setStroke(old);
        }

        // ── Core circle ──
        int coreR = node.unlocked ? 14 : 10;
        // Dark fill
        g2.setColor(new Color(10, 10, 20));
        g2.fillOval(cx - coreR, cy - coreR, coreR * 2, coreR * 2);
        // Neon border
        Stroke old = g2.getStroke();
        g2.setStroke(new BasicStroke(node.unlocked ? 2.5f : 1.5f));
        g2.setColor(col);
        g2.drawOval(cx - coreR, cy - coreR, coreR * 2, coreR * 2);
        g2.setStroke(old);

        // Inner dot
        if (node.unlocked) {
            g2.setColor(new Color(col.getRed(), col.getGreen(), col.getBlue(), 180));
            g2.fillOval(cx - 5, cy - 5, 10, 10);
        }

        // ── Name label ──
        Font nameFont = new Font("Consolas", Font.BOLD, 14);
        g2.setFont(nameFont);
        FontMetrics fm = g2.getFontMetrics();
        int textW = fm.stringWidth(node.name);
        int labelY = cy - coreR - 18;

        if (node.unlocked) {
            // Glow behind text
            g2.setColor(new Color(col.getRed(), col.getGreen(), col.getBlue(), 40));
            g2.fillRoundRect(cx - textW / 2 - 8, labelY - fm.getAscent() - 2,
                    textW + 16, fm.getHeight() + 4, 6, 6);
            // Shadow
            g2.setColor(new Color(0, 0, 0, 160));
            g2.drawString(node.name, cx - textW / 2 + 1, labelY + 1);
            // Text
            g2.setColor(col);
            g2.drawString(node.name, cx - textW / 2, labelY);
        } else {
            g2.setColor(LOCKED);
            g2.drawString(node.name, cx - textW / 2, labelY);
        }

        // ── Subtitle ──
        if (node.unlocked) {
            Font subFont = new Font("Consolas", Font.PLAIN, 11);
            g2.setFont(subFont);
            fm = g2.getFontMetrics();
            int subW = fm.stringWidth(node.subtitle);
            g2.setColor(new Color(col.getRed(), col.getGreen(), col.getBlue(), 100));
            g2.drawString(node.subtitle, cx - subW / 2, cy + coreR + 18);
        }

        // ── Lock icon for locked nodes ──
        if (!node.unlocked) {
            // Simple padlock shape
            g2.setColor(LOCKED);
            g2.fillRect(cx - 5, cy - 2, 10, 8);
            Stroke prev = g2.getStroke();
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawArc(cx - 4, cy - 7, 8, 8, 0, 180);
            g2.setStroke(prev);
        }
    }

    // ── HUD: title, instructions, selected level info ─────────
    private void drawHUD(Graphics2D g2) {
        // ── Title ──
        Font titleFont = new Font("Consolas", Font.BOLD, 42);
        g2.setFont(titleFont);
        FontMetrics fm = g2.getFontMetrics();
        String title = "SECTOR MAP";
        int titleX = (W - fm.stringWidth(title)) / 2;
        int titleY = 60;

        // Title glow
        float titlePulse = (float)(0.7 + 0.3 * Math.sin(tick * 0.03));
        g2.setColor(new Color(0, 200, 255, (int)(40 * titlePulse)));
        g2.fillRoundRect(titleX - 20, titleY - fm.getAscent() - 8,
                fm.stringWidth(title) + 40, fm.getHeight() + 16, 12, 12);

        // Title shadow
        g2.setColor(new Color(0, 0, 0, 180));
        g2.drawString(title, titleX + 2, titleY + 2);
        // Title text
        g2.setColor(new Color(0, (int)(220 * titlePulse + 35), 255));
        g2.drawString(title, titleX, titleY);

        // ── Decorative line under title ──
        Stroke old = g2.getStroke();
        g2.setStroke(new BasicStroke(1f));
        int lineY = titleY + 12;
        int lineW = 300;
        for (int i = 0; i < lineW; i++) {
            float t = i / (float) lineW;
            int alpha = (int)(80 * Math.sin(t * Math.PI));
            g2.setColor(new Color(0, 200, 255, alpha));
            g2.drawLine(W / 2 - lineW / 2 + i, lineY, W / 2 - lineW / 2 + i, lineY);
        }
        g2.setStroke(old);

        // ── Selected level info box ──
        MapNode sel = nodes[selectedIndex];
        int boxW = 340;
        int boxH = 100;
        int boxX = W / 2 - boxW / 2;
        int boxY = H - 150;

        // Box background
        g2.setColor(new Color(5, 5, 15, 200));
        g2.fillRoundRect(boxX, boxY, boxW, boxH, 10, 10);

        // Box border
        Color boxCol = sel.unlocked ? sel.baseColor : LOCKED;
        old = g2.getStroke();
        g2.setStroke(new BasicStroke(1.5f));
        g2.setColor(new Color(boxCol.getRed(), boxCol.getGreen(), boxCol.getBlue(), 120));
        g2.drawRoundRect(boxX, boxY, boxW, boxH, 10, 10);
        g2.setStroke(old);

        // Level name
        g2.setFont(new Font("Consolas", Font.BOLD, 22));
        fm = g2.getFontMetrics();
        g2.setColor(boxCol);
        String selName = sel.name;
        g2.drawString(selName, boxX + boxW / 2 - fm.stringWidth(selName) / 2, boxY + 30);

        // Subtitle
        g2.setFont(new Font("Consolas", Font.PLAIN, 13));
        fm = g2.getFontMetrics();
        g2.setColor(new Color(boxCol.getRed(), boxCol.getGreen(), boxCol.getBlue(), 140));
        g2.drawString(sel.subtitle, boxX + boxW / 2 - fm.stringWidth(sel.subtitle) / 2, boxY + 50);

        // Status
        g2.setFont(new Font("Consolas", Font.BOLD, 14));
        fm = g2.getFontMetrics();
        if (sel.unlocked) {
            // Blinking "PRESS SPACE TO LAUNCH"
            int blinkAlpha = (int)(140 + 115 * Math.sin(tick * 0.06));
            g2.setColor(new Color(255, 255, 255, blinkAlpha));
            String launch = "[ SPACE / ENTER ]  LAUNCH";
            g2.drawString(launch, boxX + boxW / 2 - fm.stringWidth(launch) / 2, boxY + 78);
        } else {
            g2.setColor(new Color(120, 120, 140));
            String locked = "// LOCKED //";
            g2.drawString(locked, boxX + boxW / 2 - fm.stringWidth(locked) / 2, boxY + 78);
        }

        // ── Navigation hints ──
        g2.setFont(new Font("Consolas", Font.PLAIN, 12));
        g2.setColor(new Color(100, 120, 160));
        String navHint = "[ARROWS] Navigate     [ESC] Back to Game";
        fm = g2.getFontMetrics();
        g2.drawString(navHint, W / 2 - fm.stringWidth(navHint) / 2, H - 30);

        // ── Decorative corner brackets ──
        drawCornerBrackets(g2);
    }

    private void drawCornerBrackets(Graphics2D g2) {
        int bLen = 40;
        int pad = 20;
        Stroke old = g2.getStroke();
        g2.setStroke(new BasicStroke(2f));
        int alpha = 40 + (int)(20 * Math.sin(tick * 0.04));
        g2.setColor(new Color(0, 200, 255, alpha));

        // Top-left
        g2.drawLine(pad, pad, pad + bLen, pad);
        g2.drawLine(pad, pad, pad, pad + bLen);
        // Top-right
        g2.drawLine(W - pad, pad, W - pad - bLen, pad);
        g2.drawLine(W - pad, pad, W - pad, pad + bLen);
        // Bottom-left
        g2.drawLine(pad, H - pad, pad + bLen, H - pad);
        g2.drawLine(pad, H - pad, pad, H - pad - bLen);
        // Bottom-right
        g2.drawLine(W - pad, H - pad, W - pad - bLen, H - pad);
        g2.drawLine(W - pad, H - pad, W - pad, H - pad - bLen);

        g2.setStroke(old);
    }
}
