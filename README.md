# WaveShooter
WaveShooter is a Java Swing prototype where projectile waveforms (triangle, square, sine) affect black holes differently, with real-time audio feedback tied to player state.

## Tech Stack
- Java (Swing/AWT)
- `javax.sound.midi` for MIDI control
- `javax.sound.sampled` for generated waveform tone playback

## Project Structure
- `game/Main.java`: app entry point and window setup
- `game/GamePanel.java`: game loop, input, rendering, collision/effects logic
- `game/Player.java`: player movement, aiming, and gun state
- `game/Projectile.java`: projectile motion and waveform visuals
- `game/BlackHole.java`: black hole physics, damage/slow/flash responses
- `game/Star.java`: starfield/parallax + black hole attraction/orbit behavior
- `game/Enemy.java`: enemy rendering + scripted wave path support
- `game/MidiSynth.java`, `game/SoundManager.java`, `game/ToneGenerator.java`: audio systems

## Requirements
- JDK 17+ recommended (JDK 11+ should also work)
- Windows/macOS/Linux with Java sound support
- Optional SoundFont (`.sf2`) file if you want custom MIDI instruments

## Build and Run
From the repository root:

```powershell
javac game\*.java
java game.Main
```

## Browser Prototype (Experiment Branch)
This branch also includes a browser prototype port using Vite + TypeScript.

Run:

```powershell
npm install
npm run dev
```

Then open the local Vite URL (typically `http://localhost:5173`).

Web controls:
- `Arrow keys`: move
- `Mouse`: aim
- `Mouse hold` or `Ctrl`: fire
- `1/2/3`: switch gun
- `+/-`: adjust waveform offset
- `R`: restart after win/loss

## SoundFont Setup (Important)
`Main.java` currently initializes MIDI with a hardcoded absolute path:

`C:\Users\jwate\ASU-CSE360-SP25\WaveShooter\FluidR3_GM.sf2`

If that file is missing, the game falls back to the default soundbank. To use your own SoundFont, edit the path in `game/Main.java`.

## Controls
- `Arrow keys`: move player
- `Mouse move`: aim
- `Mouse click`: fire once
- `Ctrl` (hold): auto-fire
- `1`: Triangle gun
- `2`: Square gun
- `3`: Sine gun
- `C` (hold): cycle guns quickly
- `+` / `-`: increase/decrease waveform offset (`offsetAmt`)
- `N` / `B`: rotate aim angle manually

## Current Gameplay Features
- Three waveform guns with distinct projectile rendering
- Black hole interactions by gun type:
  - Triangle: stronger damage + knockback + impact ring
  - Square: lighter damage + shrapnel split on hit
  - Sine: low damage + slow debuff + limited piercing
- Starfield with parallax drift, gravitational pull, and orbit capture
- Particle ring effects around impacts/captures
- Basic enemy wave path movement/rendering

## Current Limitations / WIP Notes
- Enemy damage/collision from player projectiles is not wired in yet
- No level progression or win/loss loop yet
- Some tuning/debug text is still on-screen

## Troubleshooting
- No audio:
  - Verify system audio device is available.
  - Try removing custom SoundFont init to test default MIDI.
- App does not launch:
  - Make sure you run from repo root and package paths are preserved.
  - Confirm Java is installed: `java -version` and `javac -version`.
