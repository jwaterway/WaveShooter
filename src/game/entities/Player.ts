import {
  PLAYER_ACCEL,
  PLAYER_FRICTION,
  PLAYER_MAX_SPEED,
  PLAYER_RADIUS,
  WORLD_HEIGHT,
  WORLD_WIDTH,
} from "../constants";
import type { GunType, Vec2 } from "../types";
import { Projectile } from "./Projectile";

export class Player {
  x = WORLD_WIDTH / 2;
  y = WORLD_HEIGHT / 2;
  vx = 0;
  vy = 0;
  angle = 0;
  radius = PLAYER_RADIUS;
  hp = 100;
  offsetAmt = 1;
  gunType: GunType = "TRIANGLE";

  update(dt: number, move: Vec2): void {
    this.vx += move.x * PLAYER_ACCEL * dt;
    this.vy += move.y * PLAYER_ACCEL * dt;

    const speed = Math.hypot(this.vx, this.vy);
    if (speed > PLAYER_MAX_SPEED) {
      const inv = PLAYER_MAX_SPEED / speed;
      this.vx *= inv;
      this.vy *= inv;
    }

    this.vx *= PLAYER_FRICTION;
    this.vy *= PLAYER_FRICTION;

    this.x += this.vx * dt;
    this.y += this.vy * dt;

    this.x = Math.max(this.radius, Math.min(WORLD_WIDTH - this.radius, this.x));
    this.y = Math.max(this.radius, Math.min(WORLD_HEIGHT - this.radius, this.y));
  }

  aimAt(target: Vec2): void {
    this.angle = Math.atan2(target.y - this.y, target.x - this.x);
  }

  setGun(g: GunType): void {
    this.gunType = g;
  }

  fire(): Projectile {
    return new Projectile(
      {
        x: this.x + Math.cos(this.angle) * (this.radius + 10),
        y: this.y + Math.sin(this.angle) * (this.radius + 10),
      },
      this.angle,
      440,
      this.gunType === "SQUARE" ? 5 : 6,
      this.gunType,
      this.offsetAmt
    );
  }
}
