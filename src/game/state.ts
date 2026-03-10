import type { AggressiveAlien } from "./entities/AggressiveAlien";
import type { Alien } from "./entities/Alien";
import type { BlackHole } from "./entities/BlackHole";
import type { BossProjectile } from "./entities/BossProjectile";
import type { ParticleRing } from "./entities/ParticleRing";
import type { Player } from "./entities/Player";
import type { Projectile } from "./entities/Projectile";
import type { ScientistBoss } from "./entities/ScientistBoss";
import type { GameStatus } from "./types";

export interface GameState {
  player: Player;
  projectiles: Projectile[];
  blackHoles: BlackHole[];
  stars: { x: number; y: number; size: number }[];
  rings: ParticleRing[];
  aliens: (Alien | AggressiveAlien)[];
  boss: ScientistBoss | null;
  bossProjectiles: BossProjectile[];
  elapsed: number;
  status: GameStatus;
}
