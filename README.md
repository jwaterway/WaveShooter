WaveShooter is a Java Swing space-action prototype built around waveform weapons, neon arcade visuals, and evolving level structures. The current codebase supports the original wave-based arcade mode, an overworld level map, richer enemy/boss encounters, gamepad support, and an isolated adventure traversal prototype.

The repo is still a prototype, so this README separates currently playable systems from planned design direction.

## Current Features

- Waveform weapons: triangle, square, and sine guns with distinct projectile visuals, sounds, and hit behavior.
- Wave-based arcade combat with level and wave progression.
- Overworld map with neon nodes, branching navigation, level selection, and level unlock flow.
- Player health, forcefield, speed, frequency, and voltage-style weapon boost HUD.
- Power-ups for health, shield/forcefield, waveform weapon pickups, weapon boost, decoy, grenade, and speed.
- Enemy waves with multiple movement paths and health bars.
- Specialist enemies including laser enemies and phase enemies.
- Boss encounters including a missile-firing base boss and a level 2 satellite boss with destructible arms, homing missiles, and laser attacks.
- Black holes, asteroids, shards, smoke, explosions, particle rings, nebula/space structures, moon background, and scrolling camera experiments.
- Audio system using pooled WAV sound effects and looping music playback.
- Optional gamepad support through JInput, including movement, fire, weapon switching, pause, decoy, and grenade controls.
- Hidden area traversal prototype with world-coordinate movement, local radar, paused area map, enemy/item/boss/exit markers, and persistent prototype progress.

## Game Modes

### Arcade Mode

Arcade mode is the protected current game loop. It uses wave-based encounters, projectile combat, power-up drops, enemy waves, boss escalation, and level transitions. The design docs treat this mode as something to preserve while adventure-mode work continues.

### Overworld Map

The game starts on the overworld map instead of immediately spawning a wave. The map supports keyboard/gamepad navigation, locked and unlocked nodes, selected level info, and a hidden traversal prototype entry point.

### Adventure Traversal Prototype

Adventure mode is the long-term campaign direction and is currently represented by an isolated traversal prototype. It tests large-area movement, absolute world coordinates, radar/map readability, optional collectibles, enemy zones, boss regions, and exit areas without replacing arcade mode.

## Long-Term Direction

The design docs currently target:

- 12 main levels.
- Up to 20 unique enemy types.
- At least 6 bosses.
- 3 void levels that act as upgrade/power-up centers.
- Vibratos as wave-specific upgrade currency for triangle, square, and sine progression.
- Replayable areas with hidden items, optional routes, and future gating.

See [docs/README.md](./docs/README.md) for the durable design notes.

## Tech Stack

- Java Swing/AWT.
- `javax.sound.sampled` for WAV sound effects and music playback.
- JInput for optional Windows gamepad support.
- Repository-local audio resources under `audio/`.

## Requirements

- JDK 8+ should compile the project; a modern JDK is recommended.
- Windows is the best-supported environment right now because the included gamepad native libraries are Windows DLLs.
- Keyboard and mouse work without JInput.
- Gamepad support requires the JInput JAR/native files in `lib/` and `lib/natives/`.

## Build And Run

On Windows, use the provided script from the repository root:

```powershell
.\build.bat
```

The script:

- creates `bin/` if needed
- copies `audio/` into `bin/audio/`
- compiles `game/*.java`
- adds JInput to the classpath when available
- sets the native library path for gamepad support
- launches `game.Main`

Manual compile/run without gamepad support:

```powershell
javac -d bin game\*.java
Copy-Item -Recurse -Force audio bin\audio
java -cp bin game.Main
```

Manual compile/run with JInput:

```powershell
javac --release 8 -cp ".;lib\jinput-2.0.7.jar;lib\jutils-1.0.0.jar" -d bin game\*.java
Copy-Item -Recurse -Force audio bin\audio
java --enable-native-access=ALL-UNNAMED -Djava.library.path="lib\natives" -cp "bin;lib\jinput-2.0.7.jar;lib\jutils-1.0.0.jar" game.Main
```

## Controls

### Keyboard And Mouse

- `Arrow keys`: move.
- `Mouse move`: aim.
- `Left mouse`: fire.
- `Right mouse`: cycle weapons.
- `Ctrl` hold: auto-fire.
- `1`, `2`, `3`: switch to triangle, square, or sine gun.
- `C` hold: cycle guns quickly.
- `+` / `-`: adjust waveform offset amount.
- `N` / `B`: rotate aim manually.
- `P`: pause.
- `R`: restart after game over.
- `Q`: quit after game over.
- `M`: open/close the overworld map when available.
- `Alt`: deploy a decoy if one is available.
- `X`: deploy a grenade if one is available.

### Overworld Map

- `Arrow keys`: move between map nodes.
- `Space` / `Enter`: launch selected unlocked level.
- `T`: launch the hidden traversal prototype.
- `M`: close the map.
- `Escape`: close the map when allowed.

### Traversal Prototype

- `A`: toggle the paused area map.

### Gamepad

- D-pad or left stick: move / map navigation.
- `A`: fire / select map node.
- `B`: cycle weapons.
- `X`: deploy grenade.
- `Y`: deploy decoy.
- `Start`: pause.
- `Back`: quit after game over.

Exact mappings depend on how JInput reports the connected controller.

## Project Structure

- `game/Main.java`: application entry point and window setup.
- `game/GamePanel.java`: main loop, input, rendering, collisions, waves, HUD, overworld/traversal integration.
- `game/Player.java`: player state, movement, weapons, health, forcefield, speed, voltage, decoy/grenade inventory.
- `game/Projectile.java`: waveform projectile motion and rendering.
- `game/PowerUp.java`: collectible power-up types and icons.
- `game/OverworldMap.java`, `game/OverworldController.java`: map presentation and navigation.
- `game/AreaTraversalPrototype.java`, `game/TraversalController.java`, `game/AdventureProgress.java`: adventure traversal testbed and persistence.
- `game/Enemy.java`, `game/LaserEnemy.java`, `game/PhaseEnemy.java`, `game/BossEnemy.java`, `game/SatelliteBoss.java`: enemy and boss implementations.
- `game/AudioManager.java`, `game/SoundPool.java`, `game/MusicPlayer.java`: WAV-based audio playback.
- `game/GamepadInput.java`, `game/GamepadDiagnostics.java`, `game/GamepadTest.java`: JInput support and diagnostics.
- `audio/`: sound effect and music resources.
- `lib/`: JInput JARs and native libraries.
- `docs/`: design memory and current direction.

## Docs Map

- [docs/game-vision.md](./docs/game-vision.md): overall game identity and long-term scope.
- [docs/adventure-mode.md](./docs/adventure-mode.md): traversal/adventure mode direction.
- [docs/arcade-mode.md](./docs/arcade-mode.md): protected current arcade/wave mode.
- [docs/upgrades-and-vibratos.md](./docs/upgrades-and-vibratos.md): ship progression and currencies.
- [docs/audio-direction.md](./docs/audio-direction.md): music, SFX, and world-reactive audio plans.
- [docs/enemy-roster.md](./docs/enemy-roster.md): enemy planning and classification.
- [docs/bosses.md](./docs/bosses.md): boss planning and encounter notes.
- [docs/area-traversal-prototype-notes.md](./docs/area-traversal-prototype-notes.md): current traversal prototype rules.

## Debug / Prototype Keys

These are useful while developing but should not be treated as final player controls:

- `F`: add forcefield.
- `W`: skip wave.
- `D`: deploy debug decoy.
- `L`: cycle levels.

## Current WIP Notes

- Adventure mode is a prototype and should stay isolated until the traversal model is proven.
- Arcade mode is intentionally protected and should not be casually broken by adventure work.
- Vibratos and the full upgrade economy are planned but not fully implemented.
- Some debug output and prototype HUD information are still present.
- Gamepad support depends on local native library loading and may vary by controller.

## Troubleshooting

- If audio is missing, make sure the `audio/` folder was copied to `bin/audio/` or run `build.bat`.
- If gamepad input is missing, run `test_gamepad.bat` or `game.GamepadDiagnostics`, confirm the JInput JARs are present, and check `lib/natives/`.
- If the app does not launch, run from the repository root and confirm Java is installed with `java -version` and `javac -version`.
- If compiling manually with JInput, include both the JAR classpath and `-Djava.library.path`.
