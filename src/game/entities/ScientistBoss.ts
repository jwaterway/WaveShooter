import type { Vec2 } from "../types";
import { BossProjectile } from "./BossProjectile";

export class ScientistBoss {
  x = 600;
  y = 120;
  vx = 70;
  vy = 25;
  radius = 46;
  hp = 320;
  alive = true;
  spawnTime = 0;
  attackCooldown = 0;
  pulseCooldown = 0;

  update(dt: number, target: Vec2): BossProjectile[] {
    const shots: BossProjectile[] = [];
    this.spawnTime += dt;

    this.x += this.vx * dt;
    this.y += this.vy * dt;
    if (this.x - this.radius < 0 || this.x + this.radius > 1200) this.vx *= -1;
    if (this.y - this.radius < 0 || this.y + this.radius > 360) this.vy *= -1;

    this.attackCooldown -= dt;
    this.pulseCooldown -= dt;

    const enraged = this.hp < 160;

    if (this.attackCooldown <= 0) {
      const burst = enraged ? 3 : 2;
      for (let i = 0; i < burst; i++) {
        const offset = (i - (burst - 1) / 2) * 28;
        shots.push(
          new BossProjectile(
            { x: this.x + offset, y: this.y + 8 },
            { x: target.x + offset * 0.3, y: target.y }
          )
        );
      }
      this.attackCooldown = enraged ? 0.85 : 1.25;
    }

    if (this.pulseCooldown <= 0) {
      const n = enraged ? 10 : 6;
      for (let i = 0; i < n; i++) {
        const a = (Math.PI * 2 * i) / n;
        const p = new BossProjectile(
          { x: this.x, y: this.y },
          { x: this.x + Math.cos(a) * 100, y: this.y + Math.sin(a) * 100 },
          enraged ? 220 : 180
        );
        shots.push(p);
      }
      this.pulseCooldown = enraged ? 2.6 : 4;
    }

    return shots;
  }

  damage(amount: number): void {
    this.hp -= amount;
    if (this.hp <= 0) this.alive = false;
  }
}
