import type { AggressiveAlien } from "../entities/AggressiveAlien";
import type { Alien } from "../entities/Alien";
import type { BlackHole } from "../entities/BlackHole";
import type { BossProjectile } from "../entities/BossProjectile";
import type { ParticleRing } from "../entities/ParticleRing";
import type { Player } from "../entities/Player";
import type { Projectile } from "../entities/Projectile";
import type { ScientistBoss } from "../entities/ScientistBoss";
import { WORLD_HEIGHT, WORLD_WIDTH } from "../constants";
import type { GameStatus } from "../types";

function drawHud(ctx: CanvasRenderingContext2D, player: Player, status: GameStatus, elapsed: number, alienCount: number, boss: ScientistBoss | null): void {
  ctx.fillStyle = "#e0f2ff";
  ctx.font = "16px Consolas, monospace";
  ctx.fillText(`HP: ${Math.max(0, Math.round(player.hp))}`, 20, 30);
  ctx.fillText(`Gun: ${player.gunType}`, 20, 52);
  ctx.fillText(`Offset: ${player.offsetAmt.toFixed(2)}`, 20, 74);
  ctx.fillText(`Aliens: ${alienCount}`, 20, 96);
  ctx.fillText(`Time: ${elapsed.toFixed(1)}s`, 20, 118);
  if (boss) {
    ctx.fillText(`Boss HP: ${Math.max(0, Math.round(boss.hp))}`, 20, 140);
  }

  if (status !== "running") {
    ctx.fillStyle = "rgba(0,0,0,0.4)";
    ctx.fillRect(0, 0, WORLD_WIDTH, WORLD_HEIGHT);
    ctx.fillStyle = "#ffffff";
    ctx.font = "bold 44px Consolas, monospace";
    ctx.textAlign = "center";
    ctx.fillText(status === "won" ? "Level Complete" : "Defeat", WORLD_WIDTH / 2, WORLD_HEIGHT / 2 - 10);
    ctx.font = "22px Consolas, monospace";
    ctx.fillText("Press R to restart", WORLD_WIDTH / 2, WORLD_HEIGHT / 2 + 30);
    ctx.textAlign = "left";
  }
}

export function runRenderSystem(ctx: CanvasRenderingContext2D, state: {
  stars: { x: number; y: number; size: number }[];
  blackHoles: BlackHole[];
  rings: ParticleRing[];
  player: Player;
  projectiles: Projectile[];
  aliens: (Alien | AggressiveAlien)[];
  boss: ScientistBoss | null;
  bossProjectiles: BossProjectile[];
  status: GameStatus;
  elapsed: number;
}): void {
  ctx.clearRect(0, 0, WORLD_WIDTH, WORLD_HEIGHT);

  for (const s of state.stars) {
    ctx.fillStyle = "rgba(170,210,255,0.8)";
    ctx.fillRect(s.x, s.y, s.size, s.size);
  }

  for (const h of state.blackHoles) {
    const glow = ctx.createRadialGradient(h.x, h.y, h.radius * 0.6, h.x, h.y, h.radius * 1.8);
    glow.addColorStop(0, `rgba(255,255,180,${0.35 * h.flash})`);
    glow.addColorStop(1, "rgba(0,0,0,0)");
    ctx.fillStyle = glow;
    ctx.beginPath();
    ctx.arc(h.x, h.y, h.radius * 1.8, 0, Math.PI * 2);
    ctx.fill();

    ctx.fillStyle = "#000";
    ctx.beginPath();
    ctx.arc(h.x, h.y, h.radius, 0, Math.PI * 2);
    ctx.fill();

    ctx.strokeStyle = "rgba(255,215,140,0.6)";
    ctx.lineWidth = 2;
    ctx.beginPath();
    ctx.arc(h.x, h.y, h.radius * 1.1, 0, Math.PI * 2);
    ctx.stroke();
  }

  for (const ring of state.rings) {
    if (!ring.alive) continue;
    ctx.strokeStyle = `rgba(200,130,255,${ring.alpha})`;
    ctx.lineWidth = 4;
    ctx.beginPath();
    ctx.arc(ring.x, ring.y, ring.radius, 0, Math.PI * 2);
    ctx.stroke();
  }

  for (const p of state.projectiles) {
    if (!p.alive) continue;
    ctx.fillStyle = p.gunType === "TRIANGLE" ? "#ff6a3c" : p.gunType === "SQUARE" ? "#58b6ff" : "#6eff8a";
    ctx.beginPath();
    ctx.arc(p.x, p.y, p.radius, 0, Math.PI * 2);
    ctx.fill();
  }

  for (const a of state.aliens) {
    if (!a.alive) continue;
    ctx.fillStyle = "flankSign" in a ? "#ff5079" : "#6fe7ff";
    ctx.beginPath();
    ctx.moveTo(a.x, a.y - a.radius);
    ctx.lineTo(a.x + a.radius, a.y);
    ctx.lineTo(a.x, a.y + a.radius);
    ctx.lineTo(a.x - a.radius, a.y);
    ctx.closePath();
    ctx.fill();
  }

  if (state.boss?.alive) {
    const b = state.boss;
    ctx.fillStyle = "#7a4cff";
    ctx.beginPath();
    ctx.arc(b.x, b.y, b.radius, 0, Math.PI * 2);
    ctx.fill();
    ctx.strokeStyle = "#ffe18c";
    ctx.lineWidth = 3;
    ctx.stroke();
  }

  for (const bp of state.bossProjectiles) {
    if (!bp.alive) continue;
    ctx.fillStyle = "#f6dd4f";
    ctx.beginPath();
    ctx.arc(bp.x, bp.y, bp.radius, 0, Math.PI * 2);
    ctx.fill();
  }

  const p = state.player;
  ctx.fillStyle = "#1a9fff";
  ctx.beginPath();
  ctx.arc(p.x, p.y, p.radius, 0, Math.PI * 2);
  ctx.fill();

  ctx.strokeStyle = "#fff";
  ctx.lineWidth = 2;
  ctx.beginPath();
  ctx.moveTo(p.x, p.y);
  ctx.lineTo(p.x + Math.cos(p.angle) * (p.radius + 18), p.y + Math.sin(p.angle) * (p.radius + 18));
  ctx.stroke();

  drawHud(ctx, p, state.status, state.elapsed, state.aliens.filter((a) => a.alive).length, state.boss);
}
