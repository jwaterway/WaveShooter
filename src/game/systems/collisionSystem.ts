import type { Alien } from "../entities/Alien";
import type { AggressiveAlien } from "../entities/AggressiveAlien";
import type { BlackHole } from "../entities/BlackHole";
import type { BossProjectile } from "../entities/BossProjectile";
import type { ParticleRing } from "../entities/ParticleRing";
import type { Player } from "../entities/Player";
import type { Projectile } from "../entities/Projectile";
import type { ScientistBoss } from "../entities/ScientistBoss";
import { ParticleRing as Ring } from "../entities/ParticleRing";
import { WORLD_HEIGHT, WORLD_WIDTH } from "../constants";

function dist2(ax: number, ay: number, bx: number, by: number): number {
  const dx = ax - bx;
  const dy = ay - by;
  return dx * dx + dy * dy;
}

export function runCollisionSystem(params: {
  player: Player;
  blackHoles: BlackHole[];
  projectiles: Projectile[];
  aliens: (Alien | AggressiveAlien)[];
  boss: ScientistBoss | null;
  bossProjectiles: BossProjectile[];
  rings: ParticleRing[];
}): void {
  const { player, blackHoles, projectiles, aliens, boss, bossProjectiles, rings } = params;

  for (const p of projectiles) {
    for (const h of blackHoles) {
      const rr = p.radius + h.radius;
      if (dist2(p.x, p.y, h.x, h.y) <= rr * rr) {
        if (p.gunType === "TRIANGLE") {
          h.applyDamage(1.4);
          h.vx += p.vx * 0.02;
          h.vy += p.vy * 0.02;
          p.alive = false;
        } else if (p.gunType === "SQUARE") {
          h.applyDamage(0.8);
          p.alive = false;
        } else {
          h.applyDamage(0.3);
          h.applySlow(0.5, 0.6);
          p.pierce += 1;
          if (p.pierce >= 3) p.alive = false;
        }
        rings.push(new Ring(h.x, h.y, h.radius + 35));
        break;
      }
    }
  }

  for (const p of projectiles) {
    if (!p.alive) continue;
    for (const a of aliens) {
      if (!a.alive) continue;
      const rr = p.radius + a.radius;
      if (dist2(p.x, p.y, a.x, a.y) <= rr * rr) {
        a.damage(p.gunType === "TRIANGLE" ? 12 : p.gunType === "SQUARE" ? 9 : 6);
        if (p.gunType !== "SINE") p.alive = false;
      }
    }
    if (boss?.alive) {
      const rr = p.radius + boss.radius;
      if (dist2(p.x, p.y, boss.x, boss.y) <= rr * rr) {
        boss.damage(p.gunType === "TRIANGLE" ? 8 : p.gunType === "SQUARE" ? 6 : 4);
        if (p.gunType !== "SINE") p.alive = false;
      }
    }
  }

  for (const a of aliens) {
    if (!a.alive) continue;
    const rr = a.radius + player.radius;
    if (dist2(a.x, a.y, player.x, player.y) <= rr * rr) {
      player.hp -= 18 * (1 / 60);
    }
  }

  for (const bp of bossProjectiles) {
    const rr = bp.radius + player.radius;
    if (dist2(bp.x, bp.y, player.x, player.y) <= rr * rr) {
      player.hp -= 10;
      bp.alive = false;
    }
  }

  for (const p of projectiles) {
    if (p.x < -60 || p.x > WORLD_WIDTH + 60 || p.y < -60 || p.y > WORLD_HEIGHT + 60) p.alive = false;
  }
  for (const b of bossProjectiles) {
    if (b.x < -60 || b.x > WORLD_WIDTH + 60 || b.y < -60 || b.y > WORLD_HEIGHT + 60) b.alive = false;
  }
}
