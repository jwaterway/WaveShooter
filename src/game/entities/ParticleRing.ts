export class ParticleRing {
  x: number;
  y: number;
  radius = 1;
  alpha = 0.75;
  growth = 170;

  constructor(x: number, y: number, private readonly cap: number = 130) {
    this.x = x;
    this.y = y;
  }

  update(dt: number): void {
    this.radius = Math.min(this.cap, this.radius + this.growth * dt);
    this.alpha -= dt * (this.radius >= this.cap ? 1.4 : 0.9);
  }

  get alive(): boolean {
    return this.alpha > 0;
  }
}
