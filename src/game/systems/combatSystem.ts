import type { Projectile } from "../entities/Projectile";

export function runCombatSystem(projectiles: Projectile[], dt: number): void {
  for (const p of projectiles) {
    if (p.alive) p.update(dt);
  }
}
