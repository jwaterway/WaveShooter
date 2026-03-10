# Web Port Plan (Current Project)

## Goal
Port the existing Java Swing prototype in `game/` to a browser-playable TypeScript prototype while preserving core feel:
- player movement and aiming
- waveform-style shooting behavior
- black hole interaction loop
- starfield motion/parallax

Add new gameplay:
- aggressive alien AI
- scientist alien boss with unique attacks
- level completion condition

## Current Java Repository Analysis

### Entry Point
- `game/Main.java`
- Creates Swing window and attaches `GamePanel`

### Main Loop
- `game/GamePanel.java`
- Uses a Swing `Timer` (~16ms) to call:
  1. `update()`
  2. `repaint()`

### Entity Model
- `Player`: movement, gun type (`TRIANGLE`, `SQUARE`, `SINE`), rotation, visual state
- `Projectile`: position/velocity, wave-specific draw behavior, pierce state
- `BlackHole`: drift, knockback, slow effect, damage via radius reduction, flash state
- `Star`: parallax + gravitational interaction with black holes
- `Enemy`: path-followed renderable enemies (currently limited combat integration)
- `ParticleRing`: visual impact/capture effect

### Physics and Collision
- Projectile vs black hole collision is implemented
- Gun-specific hit behavior:
  - Triangle: heavier damage + knockback
  - Square: lower damage + split shrapnel
  - Sine: low damage + slow + limited pierce
- No fully integrated projectile-vs-enemy damage loop yet

### Rendering
- Java2D drawing via `paintComponent(Graphics)` in `GamePanel`
- Procedural visuals (no sprite assets required)

### Input
- Keyboard: arrows, number keys, modifiers (`Ctrl`, `+`, `-`, `C`, `N`, `B`)
- Mouse movement for aiming and click-to-fire

### Audio
- `MidiSynth` and `SoundManager` are used for generated/triggered tones
- Contains hardcoded optional SoundFont path in `Main`

## Web Port Scope

### Must-Have (MVP)
1. Browser launch with `npm run dev`
2. Playable single level on HTML5 Canvas
3. Player movement + aiming + firing
4. Baseline alien spawning and movement
5. Aggressive alien AI active
6. Scientist boss appears and attacks
7. Boss defeat completes level

### Nice-to-Have
- Wave-shaped projectile trails similar to Java visuals
- Particle rings and black hole lensing-style distortions
- Sound using Web Audio API

## Target Stack
- Vite
- TypeScript
- HTML5 Canvas (2D)
- `requestAnimationFrame` game loop

## Proposed Project Structure

```text
src/
  main.ts
  game/
    Game.ts
    constants.ts
    types.ts
    state.ts
    systems/
      inputSystem.ts
      movementSystem.ts
      collisionSystem.ts
      spawnSystem.ts
      aiSystem.ts
      renderSystem.ts
      combatSystem.ts
      levelSystem.ts
    entities/
      Player.ts
      Projectile.ts
      BlackHole.ts
      Star.ts
      Alien.ts
      AggressiveAlien.ts
      ScientistBoss.ts
      BossProjectile.ts
      ParticleRing.ts
```

## Java -> TypeScript Mapping
- `Main` -> `main.ts` + bootstrapping canvas
- `GamePanel` -> `Game.ts` and systems split by concern
- `Player`, `Projectile`, `BlackHole`, `Star`, `ParticleRing` -> direct entity ports
- `Enemy` -> `Alien` base class + behavior variants
- Java timer/thread logic -> deterministic update loop with delta time

## Implementation Phases

### Phase 1: Foundation
- Initialize Vite + TypeScript
- Add canvas root and fixed virtual resolution (1200x800)
- Build basic `Game` loop with update/render split

### Phase 2: Core Port
- Port player movement/aiming/fire
- Port projectile and black hole interactions
- Port starfield and particle rings
- Add basic HUD for debugging/game status

### Phase 3: Alien Layer
- Add `Alien` base with HP and steering
- Add spawn system and wave pacing
- Implement aggressive alien behavior (predictive chase, pressure radius)

### Phase 4: Scientist Boss
- Spawn boss after wave threshold or timer
- Boss abilities:
  - radial pulse shots
  - targeted burst at player
  - temporary shield or summon behavior
- Multi-phase HP thresholds to vary attack patterns

### Phase 5: Win/Lose and Polish
- Win: scientist boss defeated
- Lose: player HP <= 0
- Add restart flow and basic balancing constants

## Risks and Mitigations
- Risk: Java procedural rendering is tightly coupled to classes
  - Mitigation: keep entity state pure and move draw logic to render system
- Risk: behavior drift during port
  - Mitigation: preserve constants first, then tune in one balancing pass
- Risk: scope creep with boss complexity
  - Mitigation: implement two attack patterns first, add extras only if stable

## Definition of Done
- `npm install` and `npm run dev` start a playable browser build
- Movement, shooting, and collisions are functional
- Aggressive aliens and scientist boss are visibly distinct and active
- Level can be completed by defeating the boss
- Plan and architecture doc remain aligned with implemented files
