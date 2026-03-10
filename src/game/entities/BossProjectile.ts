import type { Vec2 } from "../types";

export class BossProjectile {
  x: number;
  y: number;
  vx: number;
  vy: number;
  radius = 8;
  alive = true;

  constructor(origin: Vec2, target: Vec2, speed = 190) {
    this.x = origin.x;
    this.y = origin.y;
    const dx = target.x - origin.x;
    const dy = target.y - origin.y;
    const d = Math.hypot(dx, dy) || 1;
    this.vx = (dx / d) * speed;
    this.vy = (dy / d) * speed;
  }

  update(dt: number): void {
    this.x += this.vx * dt;
    this.y += this.vy * dt;
  }
}
