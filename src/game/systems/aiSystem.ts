import type { AggressiveAlien } from "../entities/AggressiveAlien";
import type { Alien } from "../entities/Alien";
import type { Player } from "../entities/Player";

export function runAISystem(dt: number, aliens: (Alien | AggressiveAlien)[], player: Player): void {
  for (const alien of aliens) {
    if (!alien.alive) continue;
    alien.update(dt, { x: player.x, y: player.y });
  }
}
