import type { GameStatus } from "../types";

export function runLevelSystem(playerHp: number, bossAlive: boolean, bossSpawned: boolean): GameStatus {
  if (playerHp <= 0) return "lost";
  if (bossSpawned && !bossAlive) return "won";
  return "running";
}
