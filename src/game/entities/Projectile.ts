import type { Vec2 } from "../types";

export class Projectile {
  x: number;
  y: number;
  vx: number;
  vy: number;
  radius: number;
  alive = true;
  pierce = 0;

  constructor(
    start: Vec2,
    angle: number,
    speed: number,
    radius: number,
    public readonly gunType: "TRIANGLE" | "SQUARE" | "SINE",
    public readonly offsetAmt: number
  ) {
    this.x = start.x;
    this.y = start.y;
    this.vx = Math.cos(angle) * speed;
    this.vy = Math.sin(angle) * speed;
    this.radius = radius;
  }

  update(dt: number): void {
    this.x += this.vx * dt;
    this.y += this.vy * dt;
  }
}
