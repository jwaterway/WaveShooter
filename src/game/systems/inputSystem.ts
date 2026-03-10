import type { GunType, Vec2 } from "../types";

export class InputSystem {
  private keys = new Set<string>();
  private mouseDown = false;
  mouse: Vec2 = { x: 600, y: 400 };
  justPressed: string[] = [];

  attach(canvas: HTMLCanvasElement): void {
    window.addEventListener("keydown", (e) => {
      if (!this.keys.has(e.code)) this.justPressed.push(e.code);
      this.keys.add(e.code);
    });
    window.addEventListener("keyup", (e) => this.keys.delete(e.code));

    canvas.addEventListener("mousemove", (e) => {
      const rect = canvas.getBoundingClientRect();
      const sx = canvas.width / rect.width;
      const sy = canvas.height / rect.height;
      this.mouse.x = (e.clientX - rect.left) * sx;
      this.mouse.y = (e.clientY - rect.top) * sy;
    });

    canvas.addEventListener("mousedown", () => {
      this.mouseDown = true;
    });

    window.addEventListener("mouseup", () => {
      this.mouseDown = false;
    });
  }

  consumeGunSwitch(): GunType | null {
    const idx1 = this.justPressed.indexOf("Digit1");
    if (idx1 >= 0) {
      this.justPressed.splice(idx1, 1);
      return "TRIANGLE";
    }
    const idx2 = this.justPressed.indexOf("Digit2");
    if (idx2 >= 0) {
      this.justPressed.splice(idx2, 1);
      return "SQUARE";
    }
    const idx3 = this.justPressed.indexOf("Digit3");
    if (idx3 >= 0) {
      this.justPressed.splice(idx3, 1);
      return "SINE";
    }
    return null;
  }

  getMoveVector(): Vec2 {
    const x = (this.keys.has("ArrowRight") ? 1 : 0) - (this.keys.has("ArrowLeft") ? 1 : 0);
    const y = (this.keys.has("ArrowDown") ? 1 : 0) - (this.keys.has("ArrowUp") ? 1 : 0);
    const d = Math.hypot(x, y) || 1;
    return { x: x / d, y: y / d };
  }

  firing(): boolean {
    return this.mouseDown || this.keys.has("ControlLeft") || this.keys.has("ControlRight");
  }

  offsetDelta(): number {
    let d = 0;
    if (this.keys.has("Equal") || this.keys.has("NumpadAdd")) d += 1;
    if (this.keys.has("Minus") || this.keys.has("NumpadSubtract")) d -= 1;
    return d;
  }

  clearFrame(): void {
    this.justPressed = [];
  }
}
