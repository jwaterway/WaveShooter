import type { Vec2 } from "../types";
import { Alien } from "./Alien";

export class AggressiveAlien extends Alien {
  flankSign = Math.random() < 0.5 ? -1 : 1;
  pressureRadius = 170;
  override hp = 24;
  override maxSpeed = 170;
  rage = 1;

  override update(dt: number, player: Vec2): void {
    const tx = player.x + this.flankSign * this.pressureRadius;
    const ty = player.y - 50;

    const dx = tx - this.x;
    const dy = ty - this.y;
    const d = Math.hypot(dx, dy) || 1;

    this.vx += (dx / d) * 220 * this.rage * dt;
    this.vy += (dy / d) * 220 * this.rage * dt;

    const speed = Math.hypot(this.vx, this.vy) || 1;
    const cap = this.maxSpeed * this.rage;
    if (speed > cap) {
      this.vx *= cap / speed;
      this.vy *= cap / speed;
    }

    this.x += this.vx * dt;
    this.y += this.vy * dt;

    this.vx *= 0.975;
    this.vy *= 0.975;

    if (Math.random() < 0.01) this.flankSign *= -1;
  }
}
