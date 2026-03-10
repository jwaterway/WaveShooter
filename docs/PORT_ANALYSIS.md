# Browser Port Analysis

## Entry Point
- Java: `game/Main.java`
- Web target: `src/main.ts`

## Loop Mapping
- Java uses Swing timer (~16ms)
- Web uses `requestAnimationFrame` with delta time

## Entity Mapping
- `Player` -> `src/game/entities/Player.ts`
- `Projectile` -> `src/game/entities/Projectile.ts`
- `BlackHole` -> `src/game/entities/BlackHole.ts`
- `Star` -> `src/game/entities/Star.ts`
- `Enemy` -> `Alien`, `AggressiveAlien`, `ScientistBoss`
- `ParticleRing` -> `src/game/entities/ParticleRing.ts`

## Portable vs Platform
Portable:
- entity state
- movement logic
- AI and level rules
- collisions and combat

Platform:
- Java2D rendering -> Canvas2D renderer
- Swing input -> browser input events
- Java MIDI/audio -> omitted in MVP (hooks can be added)

## Game Flow (Web)
1. poll input state
2. update entities with dt
3. run AI, spawn, and combat systems
4. resolve collisions
5. evaluate level state (win/lose)
6. render frame
