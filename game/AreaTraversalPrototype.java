package game;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Hidden prototype level controller for area-based traversal.
 * It is intentionally isolated from the current wave-based level flow.
 */
public class AreaTraversalPrototype {

    private static final double AREA_LENGTH_METERS = 200_000.0;
    private static final double BASE_FORWARD_SPEED_MPS = 1_000.0;
    private static final double BOTTOM_EDGE_SPEED_FACTOR = 0.25;
    private static final double TOP_EDGE_SPEED_FACTOR = 2.0;
    private static final double MAX_LATERAL_SPEED_MPS = 340.0;
    private static final double LATERAL_LIMIT_METERS = 18_000.0;
    private static final double RADAR_FORWARD_RANGE_METERS = AREA_LENGTH_METERS * 0.14;
    private static final double RADAR_SIDE_RANGE_METERS = 12_000.0;
    private static final double BOTTOM_EDGE_THRESHOLD_PX = 180.0;
    private static final double TOP_EDGE_THRESHOLD_PX = 180.0;
    private static final double VIEW_ANCHOR_Y = 0.60;
    private static final double SCREEN_WINDOW_FROM_RADAR = 0.20;
    private static final double VIEW_ANCHOR_X = 0.50;
    private static final double PICKUP_WORLD_RADIUS_METERS = 650.0;

    private static final int RADAR_SIZE = 260;
    private static final int RADAR_MARGIN = 34;

    private final int screenWidth;
    private final int screenHeight;
    private final ArrayList<WorldMarker> markers = new ArrayList<>();

    private double worldForwardMeters;
    private double worldLateralMeters;
    private double forwardSpeedFactor = 1.0;
    private boolean slowedByBottomEdge;
    private boolean leftHeld;
    private boolean rightHeld;
    private boolean areaMapOpen;
    private int tick;

    private enum MarkerKind {
        DECOY,
        ENEMY_ZONE,
        BOSS_ZONE,
        EXIT_VOID
    }

    private static class WorldMarker {
        final MarkerKind kind;
        final double lateralMeters;
        final double forwardMeters;
        final String label;
        final Color color;
        final boolean blink;
        boolean collected;

        WorldMarker(MarkerKind kind, double lateralMeters, double forwardMeters,
                    String label, Color color, boolean blink) {
            this.kind = kind;
            this.lateralMeters = lateralMeters;
            this.forwardMeters = forwardMeters;
            this.label = label;
            this.color = color;
            this.blink = blink;
            this.collected = false;
        }
    }

    public AreaTraversalPrototype(int screenWidth, int screenHeight) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        seedMarkers();
    }

    private void seedMarkers() {
        markers.clear();
        markers.add(new WorldMarker(MarkerKind.DECOY, -10_500, 52_000,
            "Decoy Cache West", new Color(90, 220, 255), true));
        markers.add(new WorldMarker(MarkerKind.DECOY, 11_250, 132_000,
            "Decoy Cache East", new Color(90, 220, 255), true));
        markers.add(new WorldMarker(MarkerKind.ENEMY_ZONE, -3_600, 24_000,
            "Vanguard Cluster", new Color(255, 120, 80), false));
        markers.add(new WorldMarker(MarkerKind.ENEMY_ZONE, 2_800, 68_000,
            "Wave Nest", new Color(255, 120, 80), false));
        markers.add(new WorldMarker(MarkerKind.ENEMY_ZONE, -1_250, 96_000,
            "Crossfire Belt", new Color(255, 120, 80), false));
        markers.add(new WorldMarker(MarkerKind.ENEMY_ZONE, 3_900, 148_000,
            "Signal Hunters", new Color(255, 120, 80), false));
        markers.add(new WorldMarker(MarkerKind.BOSS_ZONE, 0, 176_000,
            "Sentinel Boss", new Color(255, 90, 160), true));
        markers.add(new WorldMarker(MarkerKind.EXIT_VOID, 0, AREA_LENGTH_METERS,
            "Exit Void", new Color(180, 120, 255), true));
    }

    public void reset(Player player) {
        worldForwardMeters = 0;
        worldLateralMeters = 0;
        forwardSpeedFactor = 1.0;
        tick = 0;
        slowedByBottomEdge = false;
        leftHeld = false;
        rightHeld = false;
        areaMapOpen = false;
    }

    public void setInputState(boolean leftHeld, boolean rightHeld) {
        this.leftHeld = leftHeld;
        this.rightHeld = rightHeld;
    }

    public boolean isAreaMapOpen() {
        return areaMapOpen;
    }

    public void toggleAreaMap() {
        areaMapOpen = !areaMapOpen;
    }

    public void update(Player player, double deltaSeconds) {
        tick++;
        if (player == null) {
            return;
        }

        double lateralIntent = getNormalizedLateralIntent(player);
        worldLateralMeters += lateralIntent * MAX_LATERAL_SPEED_MPS * deltaSeconds;
        if (worldLateralMeters > LATERAL_LIMIT_METERS) {
            worldLateralMeters = LATERAL_LIMIT_METERS;
        } else if (worldLateralMeters < -LATERAL_LIMIT_METERS) {
            worldLateralMeters = -LATERAL_LIMIT_METERS;
        }

        forwardSpeedFactor = getForwardSpeedFactor(player);
        slowedByBottomEdge = forwardSpeedFactor < 0.999;
        worldForwardMeters += BASE_FORWARD_SPEED_MPS * deltaSeconds * forwardSpeedFactor;
        if (worldForwardMeters > AREA_LENGTH_METERS) {
            worldForwardMeters = AREA_LENGTH_METERS;
        }

        updateCollectibles(player);
    }

    public String getStatusText() {
        return String.format(
            "AREA %.0fm / %.0fm  LAT %.0fm  ETA %.0fs  SPD %d%%",
            worldForwardMeters,
            AREA_LENGTH_METERS,
            worldLateralMeters,
            Math.max(0.0, (AREA_LENGTH_METERS - worldForwardMeters) / BASE_FORWARD_SPEED_MPS),
            (int) Math.round(forwardSpeedFactor * 100.0)
        );
    }

    public List<String> getNearbyCallouts(Player player) {
        ArrayList<String> callouts = new ArrayList<>();
        for (WorldMarker marker : markers) {
            if (isMarkerVisibleOnRadar(marker, player)) {
                callouts.add(marker.label);
            }
        }
        return callouts;
    }

    public double getStarLateralDrift(Player player) {
        return -getNormalizedLateralIntent(player) * 5.4;
    }

    public double getStarDriftMultiplier(Player player) {
        return 0.9 + forwardSpeedFactor * 0.8;
    }

    public void drawOverlay(Graphics2D g2, Player player) {
        drawTraversalHud(g2, player);
        drawRadar(g2, player);
    }

    public void drawAreaMap(Graphics2D g2, Player player) {
        int panelW = Math.min(1320, screenWidth - 180);
        int panelH = Math.min(820, screenHeight - 140);
        int panelX = (screenWidth - panelW) / 2;
        int panelY = (screenHeight - panelH) / 2;
        int mapX = panelX + 72;
        int mapY = panelY + 110;
        int mapW = panelW - 144;
        int mapH = panelH - 180;
        double playerWorldX = playerWorldX(player);
        double playerWorldY = playerWorldY(player);

        Object oldAA = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(new Color(0, 0, 0, 185));
        g2.fillRect(0, 0, screenWidth, screenHeight);

        g2.setColor(new Color(8, 16, 34, 232));
        g2.fillRoundRect(panelX, panelY, panelW, panelH, 28, 28);
        g2.setColor(new Color(90, 180, 255, 90));
        g2.setStroke(new BasicStroke(2.0f));
        g2.drawRoundRect(panelX, panelY, panelW, panelH, 28, 28);

        g2.setFont(new Font("Consolas", Font.BOLD, 28));
        g2.setColor(new Color(190, 225, 255));
        g2.drawString("AREA MAP", panelX + 38, panelY + 48);
        g2.setFont(new Font("Consolas", Font.PLAIN, 15));
        g2.setColor(new Color(120, 175, 215));
        g2.drawString("Prototype traversal sector view", panelX + 38, panelY + 74);

        g2.setColor(new Color(6, 12, 24, 230));
        g2.fillRoundRect(mapX, mapY, mapW, mapH, 24, 24);
        g2.setColor(new Color(80, 160, 230, 85));
        g2.drawRoundRect(mapX, mapY, mapW, mapH, 24, 24);

        for (int i = 1; i < 8; i++) {
            int gx = mapX + (mapW * i) / 8;
            int gy = mapY + (mapH * i) / 8;
            g2.setColor(new Color(90, 150, 210, 22));
            g2.drawLine(gx, mapY + 18, gx, mapY + mapH - 18);
            g2.drawLine(mapX + 18, gy, mapX + mapW - 18, gy);
        }

        int laneLeftX = worldToMapX(-LATERAL_LIMIT_METERS, mapX, mapW);
        int laneRightX = worldToMapX(LATERAL_LIMIT_METERS, mapX, mapW);
        g2.setColor(new Color(255, 120, 80, 40));
        g2.fillRect(mapX + 18, mapY + 18, 14, mapH - 36);
        g2.fillRect(mapX + mapW - 32, mapY + 18, 14, mapH - 36);
        g2.setColor(new Color(255, 140, 90, 110));
        g2.drawLine(laneLeftX, mapY + 22, laneLeftX, mapY + mapH - 22);
        g2.drawLine(laneRightX, mapY + 22, laneRightX, mapY + mapH - 22);

        for (WorldMarker marker : markers) {
            int mx = worldToMapX(marker.lateralMeters, mapX, mapW);
            int my = worldToMapY(marker.forwardMeters, mapY, mapH);
            switch (marker.kind) {
                case ENEMY_ZONE:
                    g2.setColor(new Color(255, 120, 80, 52));
                    g2.fillOval(mx - 42, my - 28, 84, 56);
                    g2.setColor(new Color(255, 150, 110, 130));
                    g2.drawOval(mx - 42, my - 28, 84, 56);
                    break;
                case DECOY:
                    if (!marker.collected && ((tick / 18) % 2 == 0)) {
                        g2.setColor(new Color(90, 220, 255, 220));
                        g2.fillOval(mx - 7, my - 7, 14, 14);
                        g2.setColor(new Color(120, 235, 255, 90));
                        g2.drawOval(mx - 16, my - 16, 32, 32);
                    }
                    break;
                case BOSS_ZONE:
                    g2.setColor(new Color(255, 90, 160, 200));
                    g2.fillOval(mx - 10, my - 10, 20, 20);
                    g2.setColor(new Color(255, 140, 190, 90));
                    g2.drawOval(mx - 22, my - 22, 44, 44);
                    break;
                case EXIT_VOID:
                    g2.setColor(new Color(180, 120, 255, 210));
                    g2.fillOval(mx - 12, my - 12, 24, 24);
                    g2.setColor(new Color(210, 170, 255, 90));
                    g2.drawOval(mx - 28, my - 28, 56, 56);
                    break;
            }
        }

        int px = worldToMapX(playerWorldX, mapX, mapW);
        int py = worldToMapY(playerWorldY, mapY, mapH);
        g2.setColor(new Color(255, 255, 255, 230));
        g2.fillOval(px - 6, py - 6, 12, 12);
        g2.setColor(new Color(90, 220, 255, 220));
        g2.drawOval(px - 15, py - 15, 30, 30);
        g2.drawLine(px - 10, py, px + 10, py);
        g2.drawLine(px, py - 10, px, py + 10);

        g2.setFont(new Font("Consolas", Font.PLAIN, 15));
        g2.setColor(new Color(160, 205, 235));
        g2.drawString(String.format("Player X %+.0fm", playerWorldX), panelX + 38, panelY + panelH - 70);
        g2.drawString(String.format("Player Y %.0fm / %.0fm", playerWorldY, AREA_LENGTH_METERS), panelX + 38, panelY + panelH - 48);
        g2.drawString("Left / Right edges mark traversal bounds and future hazard lanes", panelX + 38, panelY + panelH - 26);

        int legendX = panelX + panelW - 270;
        int legendY = panelY + 40;
        drawLegendItem(g2, legendX, legendY, new Color(90, 220, 255), "Special item");
        drawLegendItem(g2, legendX, legendY + 28, new Color(255, 120, 80), "Enemy cluster");
        drawLegendItem(g2, legendX, legendY + 56, new Color(255, 90, 160), "Final villain");
        drawLegendItem(g2, legendX, legendY + 84, new Color(180, 120, 255), "Exit void");
        drawLegendItem(g2, legendX, legendY + 112, new Color(255, 255, 255), "Player");

        g2.setFont(new Font("Consolas", Font.PLAIN, 16));
        g2.setColor(new Color(170, 220, 255));
        g2.drawString("Press A to return to flight", panelX + panelW - 280, panelY + panelH - 28);

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAA);
    }

    public void drawWorldObjects(Graphics2D g2, Player player) {
        if (player == null) {
            return;
        }

        Object oldAA = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        for (WorldMarker marker : markers) {
            if (marker.kind != MarkerKind.DECOY || marker.collected) {
                continue;
            }

            ScreenPoint point = getScreenPoint(marker, player);
            if (point == null) {
                continue;
            }

            int alpha = 235;
            g2.setColor(new Color(90, 220, 255, 70));
            g2.fillOval(point.x - 28, point.y - 28, 56, 56);

            g2.setStroke(new BasicStroke(2.0f));
            g2.setColor(new Color(150, 245, 255, alpha));
            g2.drawOval(point.x - 18, point.y - 18, 36, 36);
            g2.drawLine(point.x - 12, point.y, point.x + 12, point.y);
            g2.drawLine(point.x, point.y - 12, point.x, point.y + 12);

            g2.setFont(new Font("Consolas", Font.BOLD, 12));
            g2.setColor(new Color(170, 235, 255));
            g2.drawString("DECOY", point.x - 20, point.y - 28);
        }

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAA);
    }

    private void drawTraversalHud(Graphics2D g2, Player player) {
        int x = 20;
        int y = 120;

        g2.setFont(new Font("Consolas", Font.BOLD, 18));
        g2.setColor(new Color(180, 225, 255));
        g2.drawString("TRAVERSAL PROTOTYPE", x, y);

        g2.setFont(new Font("Consolas", Font.PLAIN, 16));
        g2.setColor(new Color(150, 190, 220));
        g2.drawString(getStatusText(), x, y + 24);

        if (Math.abs(forwardSpeedFactor - 1.0) > 0.01) {
            g2.setColor(new Color(255, 200, 120));
            g2.drawString("FORWARD FLOW " + (int)Math.round(forwardSpeedFactor * 100.0) + "%", x, y + 48);
        }

        List<String> callouts = getNearbyCallouts(player);
        if (!callouts.isEmpty()) {
            g2.setColor(new Color(120, 205, 255));
            g2.drawString("NEARBY:", x, y + 76);
            g2.setFont(new Font("Consolas", Font.PLAIN, 14));
            int lineY = y + 98;
            for (String callout : callouts) {
                g2.drawString("- " + callout, x, lineY);
                lineY += 18;
                if (lineY > y + 152) {
                    break;
                }
            }
        }
    }

    private void drawRadar(Graphics2D g2, Player player) {
        int radarX = screenWidth - RADAR_SIZE - RADAR_MARGIN;
        int radarY = screenHeight - RADAR_SIZE - RADAR_MARGIN;
        int centerX = radarX + RADAR_SIZE / 2;
        int centerY = radarY + RADAR_SIZE / 2;
        double playerWorldX = playerWorldX(player);
        double playerWorldY = playerWorldY(player);

        Object oldAA = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(new Color(4, 12, 24, 210));
        g2.fillOval(radarX, radarY, RADAR_SIZE, RADAR_SIZE);

        g2.setColor(new Color(70, 130, 200, 80));
        g2.setStroke(new BasicStroke(1.2f));
        g2.drawOval(radarX, radarY, RADAR_SIZE, RADAR_SIZE);
        g2.drawOval(radarX + 28, radarY + 28, RADAR_SIZE - 56, RADAR_SIZE - 56);
        g2.drawLine(centerX, radarY + 12, centerX, radarY + RADAR_SIZE - 12);
        g2.drawLine(radarX + 12, centerY, radarX + RADAR_SIZE - 12, centerY);

        drawMovementIntentIndicator(g2, radarX, radarY, centerX, centerY, player);

        g2.setColor(new Color(110, 190, 255, 30));
        for (int i = 0; i < 10; i++) {
            int ring = 12 + i * 10;
            g2.draw(new Ellipse2D.Double(centerX - ring, centerY - ring, ring * 2.0, ring * 2.0));
        }

        for (WorldMarker marker : markers) {
            if (!isRenderableWorldObject(marker)) {
                continue;
            }
            if (!isMarkerVisibleOnRadar(marker, player)) {
                continue;
            }
            if (marker.blink && ((tick / 20) % 2 == 0)) {
                continue;
            }

            double dx = marker.lateralMeters - playerWorldX;
            double dy = marker.forwardMeters - playerWorldY;
            int px = centerX + (int) Math.round((dx / RADAR_SIDE_RANGE_METERS) * (RADAR_SIZE * 0.42));
            int py = centerY - (int) Math.round((dy / RADAR_FORWARD_RANGE_METERS) * (RADAR_SIZE * 0.42));

            g2.setColor(marker.color);
            g2.fillOval(px - 5, py - 5, 10, 10);
            g2.setColor(new Color(marker.color.getRed(), marker.color.getGreen(), marker.color.getBlue(), 60));
            g2.drawOval(px - 10, py - 10, 20, 20);
        }

        g2.setColor(new Color(255, 255, 255, 220));
        g2.fillOval(centerX - 4, centerY - 4, 8, 8);
        g2.setColor(new Color(80, 220, 255, 230));
        g2.setStroke(new BasicStroke(2.0f));
        g2.drawOval(centerX - 10, centerY - 10, 20, 20);
        g2.drawLine(centerX - 7, centerY, centerX + 7, centerY);
        drawHeadingVector(g2, centerX, centerY, player);

        g2.setFont(new Font("Consolas", Font.BOLD, 14));
        g2.setColor(new Color(170, 220, 255));
        g2.drawString("LOCAL RADAR", radarX + 72, radarY - 10);

        g2.setFont(new Font("Consolas", Font.PLAIN, 12));
        g2.setColor(new Color(125, 175, 205));
        g2.drawString("Nearby 10% window", radarX + 76, radarY + RADAR_SIZE + 18);
        g2.drawString(String.format("X %+.0fm", playerWorldX), radarX + 18, radarY + RADAR_SIZE + 18);
        g2.drawString(String.format("Y %.0fm", playerWorldY), radarX + 18, radarY + RADAR_SIZE + 34);
        g2.drawString(String.format("RNG %.0f/%.0f", RADAR_SIDE_RANGE_METERS, RADAR_FORWARD_RANGE_METERS),
            radarX + 148, radarY + RADAR_SIZE + 34);

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAA);
    }

    private boolean isMarkerVisibleOnRadar(WorldMarker marker, Player player) {
        double dx = Math.abs(marker.lateralMeters - playerWorldX(player));
        double dy = Math.abs(marker.forwardMeters - playerWorldY(player));
        return dx <= RADAR_SIDE_RANGE_METERS && dy <= RADAR_FORWARD_RANGE_METERS;
    }

    private boolean isRenderableWorldObject(WorldMarker marker) {
        return marker.kind == MarkerKind.DECOY && !marker.collected;
    }

    private void updateCollectibles(Player player) {
        for (WorldMarker marker : markers) {
            if (marker.kind != MarkerKind.DECOY || marker.collected) {
                continue;
            }

            double dx = marker.lateralMeters - playerWorldX(player);
            double dy = marker.forwardMeters - playerWorldY(player);
            if (dx * dx + dy * dy <= PICKUP_WORLD_RADIUS_METERS * PICKUP_WORLD_RADIUS_METERS) {
                marker.collected = true;
                player.addDecoy();
                AudioManager.playSfx("powerup");
            }
        }
    }

    private ScreenPoint getScreenPoint(WorldMarker marker, Player player) {
        double screenSideRange = RADAR_SIDE_RANGE_METERS * SCREEN_WINDOW_FROM_RADAR;
        double screenForwardRange = RADAR_FORWARD_RANGE_METERS * SCREEN_WINDOW_FROM_RADAR;
        double metersPerPixelLeft = screenSideRange / Math.max(1.0, screenWidth * VIEW_ANCHOR_X);
        double metersPerPixelRight = screenSideRange / Math.max(1.0, screenWidth * (1.0 - VIEW_ANCHOR_X));
        double metersPerPixelUp = screenForwardRange / Math.max(1.0, screenHeight * VIEW_ANCHOR_Y);
        double metersPerPixelDown = screenForwardRange / Math.max(1.0, screenHeight * (1.0 - VIEW_ANCHOR_Y));
        double dx = marker.lateralMeters - worldLateralMeters;
        double dy = marker.forwardMeters - worldForwardMeters;
        double anchorX = screenWidth * VIEW_ANCHOR_X;
        double anchorY = screenHeight * VIEW_ANCHOR_Y;
        double screenX = anchorX + (dx >= 0.0 ? dx / metersPerPixelRight : dx / metersPerPixelLeft);
        double screenY = anchorY - (dy >= 0.0 ? dy / metersPerPixelUp : dy / metersPerPixelDown);

        if (Math.abs(dx) > screenSideRange || Math.abs(dy) > screenForwardRange) {
            return null;
        }
        return new ScreenPoint((int)Math.round(screenX), (int)Math.round(screenY));
    }

    private static class ScreenPoint {
        final int x;
        final int y;

        ScreenPoint(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    private double getNormalizedLateralIntent(Player player) {
        if (player == null) {
            return 0.0;
        }
        double centerX = screenWidth / 2.0;
        double normalized = (player.getX() - centerX) / centerX;
        double clamped = Math.max(-1.0, Math.min(1.0, normalized));
        double abs = Math.abs(clamped);
        double eased = abs * abs * (3.0 - 2.0 * abs);
        double intent = Math.signum(clamped) * eased;

        double minX = player.radius;
        double maxX = screenWidth - player.radius;
        boolean pinnedLeft = player.getX() <= minX + 1.0;
        boolean pinnedRight = player.getX() >= maxX - 1.0;
        if (pinnedLeft && leftHeld && !rightHeld) {
            return -1.0;
        }
        if (pinnedRight && rightHeld && !leftHeld) {
            return 1.0;
        }
        return intent;
    }

    private double getRadarLateralSignal(Player player) {
        if (player == null) {
            return 0.0;
        }
        double centerX = screenWidth / 2.0;
        double normalized = (player.getX() - centerX) / centerX;
        return Math.max(-1.0, Math.min(1.0, normalized));
    }

    private double getForwardSpeedFactor(Player player) {
        if (player == null) {
            return 1.0;
        }
        double minY = player.radius;
        double maxY = screenHeight - player.radius;
        double topBandEnd = Math.min(maxY, minY + TOP_EDGE_THRESHOLD_PX);
        if (player.getY() <= topBandEnd) {
            double t = 1.0 - ((player.getY() - minY) / Math.max(1.0, topBandEnd - minY));
            t = Math.max(0.0, Math.min(1.0, t));
            double eased = t * t * (3.0 - 2.0 * t);
            return 1.0 + eased * (TOP_EDGE_SPEED_FACTOR - 1.0);
        }
        double slowZoneTop = Math.max(minY, maxY - BOTTOM_EDGE_THRESHOLD_PX);
        if (player.getY() <= slowZoneTop) {
            return 1.0;
        }
        double t = (player.getY() - slowZoneTop) / Math.max(1.0, maxY - slowZoneTop);
        t = Math.max(0.0, Math.min(1.0, t));
        double eased = t * t * (3.0 - 2.0 * t);
        return 1.0 - eased * (1.0 - BOTTOM_EDGE_SPEED_FACTOR);
    }

    private void drawMovementIntentIndicator(Graphics2D g2, int radarX, int radarY, int centerX, int centerY, Player player) {
        if (player == null) {
            return;
        }
        double lateral = getRadarLateralSignal(player);
        double magnitude = Math.max(getForwardShaderLevel(), Math.abs(lateral) * 0.75);
        if (magnitude < 0.04) {
            return;
        }

        double headingAngle = Math.atan2(lateral, Math.max(0.10, forwardSpeedFactor));
        double eased = Math.pow(magnitude, 2.1);
        int alpha = 8 + (int)Math.round(eased * 82.0);
        int startDeg = (int)Math.round(90 - Math.toDegrees(headingAngle) - 24);
        g2.setColor(new Color(90, 220, 255, alpha));
        g2.fillArc(radarX + 8, radarY + 8, RADAR_SIZE - 16, RADAR_SIZE - 16, startDeg, 48);
    }

    private void drawHeadingVector(Graphics2D g2, int centerX, int centerY, Player player) {
        double lateral = getRadarLateralSignal(player);
        double forward = Math.max(0.20, forwardSpeedFactor);
        double angle = Math.atan2(lateral, forward);
        int len = 21;
        int endX = centerX + (int)Math.round(Math.sin(angle) * len);
        int endY = centerY - (int)Math.round(Math.cos(angle) * len);
        g2.drawLine(centerX, centerY, endX, endY);
    }

    private int worldToMapX(double worldX, int mapX, int mapW) {
        double t = (worldX + LATERAL_LIMIT_METERS) / (LATERAL_LIMIT_METERS * 2.0);
        t = Math.max(0.0, Math.min(1.0, t));
        return mapX + (int)Math.round(t * mapW);
    }

    private int worldToMapY(double worldY, int mapY, int mapH) {
        double t = worldY / AREA_LENGTH_METERS;
        t = Math.max(0.0, Math.min(1.0, t));
        return mapY + mapH - (int)Math.round(t * mapH);
    }

    private void drawLegendItem(Graphics2D g2, int x, int y, Color color, String label) {
        g2.setColor(color);
        g2.fillOval(x, y - 10, 12, 12);
        g2.setColor(new Color(170, 210, 235));
        g2.drawString(label, x + 22, y);
    }

    private double playerWorldX(Player player) {
        if (player == null) {
            return worldLateralMeters;
        }
        double screenSideRange = RADAR_SIDE_RANGE_METERS * SCREEN_WINDOW_FROM_RADAR;
        double anchorX = screenWidth * VIEW_ANCHOR_X;
        if (player.getX() >= anchorX) {
            double metersPerPixelRight = screenSideRange / Math.max(1.0, screenWidth * (1.0 - VIEW_ANCHOR_X));
            return worldLateralMeters + (player.getX() - anchorX) * metersPerPixelRight;
        }
        double metersPerPixelLeft = screenSideRange / Math.max(1.0, screenWidth * VIEW_ANCHOR_X);
        return worldLateralMeters + (player.getX() - anchorX) * metersPerPixelLeft;
    }

    private double playerWorldY(Player player) {
        if (player == null) {
            return worldForwardMeters;
        }
        double screenForwardRange = RADAR_FORWARD_RANGE_METERS * SCREEN_WINDOW_FROM_RADAR;
        double anchorY = screenHeight * VIEW_ANCHOR_Y;
        if (player.getY() <= anchorY) {
            double metersPerPixelUp = screenForwardRange / Math.max(1.0, screenHeight * VIEW_ANCHOR_Y);
            return worldForwardMeters - (player.getY() - anchorY) * metersPerPixelUp;
        }
        double metersPerPixelDown = screenForwardRange / Math.max(1.0, screenHeight * (1.0 - VIEW_ANCHOR_Y));
        return worldForwardMeters - (player.getY() - anchorY) * metersPerPixelDown;
    }

    private double getForwardShaderLevel() {
        if (forwardSpeedFactor >= 1.0) {
            return 0.45 + (Math.min(TOP_EDGE_SPEED_FACTOR, forwardSpeedFactor) - 1.0)
                / Math.max(0.0001, TOP_EDGE_SPEED_FACTOR - 1.0) * 0.55;
        }
        return 0.12 + (Math.max(BOTTOM_EDGE_SPEED_FACTOR, forwardSpeedFactor) - BOTTOM_EDGE_SPEED_FACTOR)
            / Math.max(0.0001, 1.0 - BOTTOM_EDGE_SPEED_FACTOR) * 0.33;
    }
}
