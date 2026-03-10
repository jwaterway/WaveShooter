import { WORLD_HEIGHT, WORLD_WIDTH } from "../constants";

export class BlackHole {
  vx = 32;
  vy = 18;
  slowTimer = 0;
  slowFactor = 1;
  flash = 0;

  constructor(public x: number, public y: number, public radius: number) {}

  update(dt: number): void {
    const s = this.slowTimer > 0 ? this.slowFactor : 1;
    if (this.slowTimer > 0) {
      this.slowTimer -= dt;
      if (this.slowTimer <= 0) {
        this.slowFactor = 1;
      }
    }

    this.x += this.vx * s * dt;
    this.y += this.vy * s * dt;

    if (this.x - this.radius < 0 || this.x + this.radius > WORLD_WIDTH) this.vx *= -1;
    if (this.y - this.radius < 0 || this.y + this.radius > WORLD_HEIGHT) this.vy *= -1;

    this.vx *= 0.998;
    this.vy *= 0.998;
    this.flash = Math.max(0, this.flash - dt * 2.5);
  }

  applyDamage(d: number): void {
    this.radius = Math.max(8, this.radius - d);
    this.flash = 1;
  }

  applySlow(duration: number, factor: number): void {
    this.slowTimer = Math.max(this.slowTimer, duration);
    this.slowFactor = Math.min(this.slowFactor, factor);
  }

  absorbStar(): void {
    this.radius = Math.min(84, this.radius + 0.3);
  }
}
