import { BOSS_SPAWN_TIME, MAX_STARS, PLAYER_FIRE_INTERVAL, WORLD_HEIGHT, WORLD_WIDTH } from "./constants";
import { AggressiveAlien } from "./entities/AggressiveAlien";
import { Alien } from "./entities/Alien";
import { BlackHole } from "./entities/BlackHole";
import { BossProjectile } from "./entities/BossProjectile";
import { ParticleRing } from "./entities/ParticleRing";
import { Player } from "./entities/Player";
import { ScientistBoss } from "./entities/ScientistBoss";
import { Star } from "./entities/Star";
import { runAISystem } from "./systems/aiSystem";
import { runCollisionSystem } from "./systems/collisionSystem";
import { runCombatSystem } from "./systems/combatSystem";
import { InputSystem } from "./systems/inputSystem";
import { runLevelSystem } from "./systems/levelSystem";
import { runMovementSystem } from "./systems/movementSystem";
import { runRenderSystem } from "./systems/renderSystem";
import { SpawnSystem } from "./systems/spawnSystem";
import type { GameStatus } from "./types";

export class Game {
  private ctx: CanvasRenderingContext2D;
  private input = new InputSystem();
  private spawn = new SpawnSystem();

  private player = new Player();
  private stars = Array.from({ length: MAX_STARS }, () => new Star());
  private blackHoles = [new BlackHole(950, 560, 42), new BlackHole(280, 200, 30)];
  private rings: ParticleRing[] = [];
  private projectiles = [] as ReturnType<Player["fire"]>[];
  private aliens: (Alien | AggressiveAlien)[] = [];
  private boss: ScientistBoss | null = null;
  private bossProjectiles: BossProjectile[] = [];

  private fireTimer = 0;
  private elapsed = 0;
  private status: GameStatus = "running";

  constructor(private canvas: HTMLCanvasElement) {
    this.ctx = canvas.getContext("2d") as CanvasRenderingContext2D;
    this.input.attach(canvas);
    window.addEventListener("keydown", (e) => {
      if (e.code === "KeyR" && this.status !== "running") {
        this.reset();
      }
    });
  }

  start(): void {
    let last = performance.now();
    const frame = (now: number) => {
      const dt = Math.min(0.033, (now - last) / 1000);
      last = now;
      this.update(dt);
      this.render();
      requestAnimationFrame(frame);
    };
    requestAnimationFrame(frame);
  }

  private reset(): void {
    this.player = new Player();
    this.stars = Array.from({ length: MAX_STARS }, () => new Star());
    this.blackHoles = [new BlackHole(950, 560, 42), new BlackHole(280, 200, 30)];
    this.rings = [];
    this.projectiles = [];
    this.aliens = [];
    this.boss = null;
    this.bossProjectiles = [];
    this.fireTimer = 0;
    this.elapsed = 0;
    this.status = "running";
  }

  private update(dt: number): void {
    if (this.status !== "running") return;
    this.elapsed += dt;

    this.player.aimAt(this.input.mouse);
    runMovementSystem(dt, this.player, this.input.getMoveVector());

    const gun = this.input.consumeGunSwitch();
    if (gun) this.player.setGun(gun);

    this.player.offsetAmt = Math.max(0.1, Math.min(3, this.player.offsetAmt + this.input.offsetDelta() * dt * 1.25));

    this.fireTimer -= dt;
    if (this.input.firing() && this.fireTimer <= 0) {
      this.projectiles.push(this.player.fire());
      this.fireTimer = PLAYER_FIRE_INTERVAL;
    }

    for (const s of this.stars) {
      s.update(dt, { x: this.player.vx, y: this.player.vy }, this.player.angle);
      for (const bh of this.blackHoles) {
        const dx = bh.x - s.x;
        const dy = bh.y - s.y;
        const d = Math.hypot(dx, dy) || 1;
        if (d < bh.radius * 4) {
          s.x += (dx / d) * 45 * dt;
          s.y += (dy / d) * 45 * dt;
        }
        if (d < bh.radius * 1.05) {
          bh.absorbStar();
          s.x = Math.random() * WORLD_WIDTH;
          s.y = Math.random() * WORLD_HEIGHT;
          this.rings.push(new ParticleRing(bh.x, bh.y, bh.radius + 20));
        }
      }
    }

    for (const h of this.blackHoles) h.update(dt);

    runCombatSystem(this.projectiles, dt);
    for (const r of this.rings) r.update(dt);
    this.rings = this.rings.filter((r) => r.alive);

    const bossSpawned = Boolean(this.boss);
    this.spawn.update(dt, this.aliens, bossSpawned);
    runAISystem(dt, this.aliens, this.player);

    if (!this.boss && this.elapsed >= BOSS_SPAWN_TIME) {
      this.boss = new ScientistBoss();
    }
    if (this.boss?.alive) {
      const shots = this.boss.update(dt, { x: this.player.x, y: this.player.y });
      this.bossProjectiles.push(...shots);
    }

    for (const b of this.bossProjectiles) if (b.alive) b.update(dt);

    runCollisionSystem({
      player: this.player,
      blackHoles: this.blackHoles,
      projectiles: this.projectiles,
      aliens: this.aliens,
      boss: this.boss,
      bossProjectiles: this.bossProjectiles,
      rings: this.rings,
    });

    this.projectiles = this.projectiles.filter((p) => p.alive);
    this.aliens = this.aliens.filter((a) => a.alive);
    this.bossProjectiles = this.bossProjectiles.filter((b) => b.alive);

    this.status = runLevelSystem(this.player.hp, this.boss?.alive ?? false, Boolean(this.boss));

    this.input.clearFrame();
  }

  private render(): void {
    runRenderSystem(this.ctx, {
      stars: this.stars,
      blackHoles: this.blackHoles,
      rings: this.rings,
      player: this.player,
      projectiles: this.projectiles,
      aliens: this.aliens,
      boss: this.boss,
      bossProjectiles: this.bossProjectiles,
      status: this.status,
      elapsed: this.elapsed,
    });
  }
}
