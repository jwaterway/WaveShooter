import { WORLD_HEIGHT, WORLD_WIDTH } from "../constants";
import type { Vec2 } from "../types";

export class Alien {
  x: number;
  y: number;
  vx = 0;
  vy = 0;
  radius = 16;
  hp = 18;
  maxSpeed = 120;
  alive = true;

  constructor(x?: number, y?: number) {
    this.x = x ?? (Math.random() < 0.5 ? -20 : WORLD_WIDTH + 20);
    this.y = y ?? Math.random() * WORLD_HEIGHT;
  }

  update(dt: number, player: Vec2): void {
    const dx = player.x - this.x;
    const dy = player.y - this.y;
    const d = Math.hypot(dx, dy) || 1;

    this.vx += (dx / d) * 140 * dt;
    this.vy += (dy / d) * 140 * dt;

    const speed = Math.hypot(this.vx, this.vy) || 1;
    if (speed > this.maxSpeed) {
      this.vx *= this.maxSpeed / speed;
      this.vy *= this.maxSpeed / speed;
    }

    this.x += this.vx * dt;
    this.y += this.vy * dt;

    this.vx *= 0.98;
    this.vy *= 0.98;
  }

  damage(amount: number): void {
    this.hp -= amount;
    if (this.hp <= 0) this.alive = false;
  }
}
