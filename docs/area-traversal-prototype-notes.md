# Area Traversal Prototype Notes

Last updated: March 27, 2026

## Purpose

This prototype is an isolated testbed for a new way of moving through a level.
It should not replace the normal wave-based level flow until the traversal model
is proven to work.

## Core Traversal Rules

- Forward travel is continuous through an area at a base rate of `1000 mps`.
- The first test area is conceptually `200,000` units long and should take about
  `200` seconds to traverse at normal speed.
- The player ship keeps the same local on-screen movement style as the existing game.
- The world scrolls relative to the player and represents movement through an area.
- Moving left or right changes position within the area map.
- Lateral traversal should begin as soon as the player leaves exact center.

## Edge Speed Rules

- Forward flow now follows the same centered easing idea as lateral scroll.
- The middle of the screen is `100%` forward flow.
- The top half eases upward from `100%` to `200%`.
- The bottom half eases downward from `100%` to `25%`.
- The slowdown and boost should feel smooth, not binary.
- Prototype movement should not bounce off screen edges.
- While pinned to an edge, continuing to push outward may be used to reinforce
  lateral scroll behavior if that feels good in playtesting.

## Radar Rules

- Radar is local, not a full area map.
- Radar center represents the player's absolute position in the area.
- Radar shows a broader local neighborhood than the visible playfield.
- On-screen visibility corresponds roughly to the inner rings of radar range.
- The inner on-screen coverage zone is now highlighted on the radar.
- The heading line and radar shader should reflect current movement direction.
- The heading line is intentionally short and subtle.
- The radar shader should always reflect forward flow, even at neutral center.

## Screen/World Mapping Rules

- Enemies, items, exit, boss, and future hazards should live in absolute world coordinates.
- Radar markers and on-screen positions must derive from the same world model.
- Objects should not track the player's local screen drift.
- Nearby objects should enter from the edge of the screen rather than popping in late.
- Pickups should be evaluated in world space, not by screen overlap.

## Prototype Content

- Two special decoy items exist in the prototype area.
- These are optional collectibles, not required to finish the level.
- Enemy clusters, a boss zone, and an exit void are represented as world locations.
- Prototype enemy clusters now spawn from area-based enemy zones.
- Those prototype enemies are world-anchored, can be hit, and can fire enemy shots.
- Current prototype focus is traversal, radar, area map, and world-coordinate consistency.
- Next likely step is giving prototype enemies relative movement patterns while
  keeping them anchored to area/world coordinates.

## Replay / Progression Direction

- Rare items are meant to support replayability and future gating.
- A player may beat a level without collecting every important item.
- Players may revisit levels later to find items needed elsewhere.
- Vibratos are planned as wave-specific upgrade currency:
  - Triangle vibratos for early upgrades
  - Square vibratos for stronger upgrades
  - Sine vibratos for later progression
- For now, the player can continue to have all guns during prototype work.

## Area Map Direction

- A separate area map should pause gameplay and present a more cinematic view.
- It should show:
  - player position
  - relative enemy cluster regions
  - special items
  - final villain area
  - exit area
  - area edges / traversal bounds
- Edge hazards such as ring-of-fire or asteroid-wall boundaries are planned later.

## Current Prototype Controls

- From the overworld map, press `T` to launch the hidden traversal prototype.
- In the traversal prototype, press `A` on keyboard to toggle the paused area map.

## Current Prototype Status

- The HUD gun label now dynamically avoids inventory overlap.
- The area map now freezes the prototype correctly when opened.
- The area map visual treatment has been pushed toward a stronger neon look.
- The left debug HUD now shows prototype scroll percentages for `X` and `Y`.
- Enemy movement felt promising in testing, but still needs authored movement patterns.

## Notes

- Keep this work isolated from other levels as long as possible.
- Preserve a stable baseline on this feature branch and commit frequently.
- Do not rely on chat history as the only source of design memory.
