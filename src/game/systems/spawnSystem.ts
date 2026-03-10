import { ALIEN_SPAWN_INTERVAL, AGGRESSIVE_ALIEN_EVERY } from "../constants";
import { AggressiveAlien } from "../entities/AggressiveAlien";
import { Alien } from "../entities/Alien";

export class SpawnSystem {
  private timer = 0;
  private count = 0;

  update(dt: number, aliens: (Alien | AggressiveAlien)[], bossSpawned: boolean): void {
    if (bossSpawned) return;
    this.timer += dt;
    if (this.timer < ALIEN_SPAWN_INTERVAL) return;
    this.timer = 0;

    this.count += 1;
    if (this.count % AGGRESSIVE_ALIEN_EVERY === 0) {
      aliens.push(new AggressiveAlien());
    } else {
      aliens.push(new Alien());
    }
  }
}
