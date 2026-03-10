import { WORLD_HEIGHT, WORLD_WIDTH } from "../constants";
import type { Vec2 } from "../types";

export class Star {
  x: number;
  y: number;
  size: number;
  speedFactor: number;

  constructor() {
    this.x = Math.random() * WORLD_WIDTH;
    this.y = Math.random() * WORLD_HEIGHT;
    this.speedFactor = 0.2 + Math.random();
    this.size = 1 + this.speedFactor * 2;
  }

  update(dt: number, playerVelocity: Vec2, facing: number): void {
    this.x -= playerVelocity.x * this.speedFactor * dt * 0.25;
    this.y -= playerVelocity.y * this.speedFactor * dt * 0.25;

    this.x -= Math.cos(facing) * this.speedFactor * 52 * dt;
    this.y -= Math.sin(facing) * this.speedFactor * 52 * dt;

    if (this.x < -30) this.x = WORLD_WIDTH + 30;
    if (this.x > WORLD_WIDTH + 30) this.x = -30;
    if (this.y < -30) this.y = WORLD_HEIGHT + 30;
    if (this.y > WORLD_HEIGHT + 30) this.y = -30;
  }
}
