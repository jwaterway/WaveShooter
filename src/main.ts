import "./style.css";
import { WORLD_HEIGHT, WORLD_WIDTH } from "./game/constants";
import { Game } from "./game/Game";

const app = document.querySelector<HTMLDivElement>("#app");
if (!app) {
  throw new Error("#app element missing");
}

const canvas = document.createElement("canvas");
canvas.width = WORLD_WIDTH;
canvas.height = WORLD_HEIGHT;
app.appendChild(canvas);

const game = new Game(canvas);
game.start();
