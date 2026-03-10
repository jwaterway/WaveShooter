import type { Player } from "../entities/Player";
import type { Vec2 } from "../types";

export function runMovementSystem(dt: number, player: Player, move: Vec2): void {
  player.update(dt, move);
}
