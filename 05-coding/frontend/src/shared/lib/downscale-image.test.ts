import { afterEach, describe, expect, it, vi } from "vitest";
import { downscaleToSquare } from "./downscale-image";

function fakeCanvas(encoders: Record<string, Blob | null>) {
  const drawImage = vi.fn();
  const canvas = {
    width: 0,
    height: 0,
    getContext: () => ({ drawImage }),
    toBlob: (cb: (b: Blob | null) => void, type: string) =>
      cb(encoders[type] ?? null),
  };
  return { canvas, drawImage };
}

function stubBitmap(width: number, height: number) {
  const close = vi.fn();
  vi.stubGlobal("createImageBitmap", async () => ({ width, height, close }));
  return close;
}

afterEach(() => vi.unstubAllGlobals());

describe("downscaleToSquare", () => {
  it("center-crops a landscape image to a 256 square and returns webp", async () => {
    const close = stubBitmap(400, 200);
    const webp = new Blob(["w"], { type: "image/webp" });
    const { canvas, drawImage } = fakeCanvas({ "image/webp": webp });
    const out = await downscaleToSquare(new Blob(["x"]), 256, () => canvas);
    expect(out).toEqual({ blob: webp, filename: "avatar.webp" });
    expect(canvas.width).toBe(256);
    expect(canvas.height).toBe(256);
    expect(drawImage).toHaveBeenCalledWith(
      expect.anything(),
      100,
      0,
      200,
      200,
      0,
      0,
      256,
      256,
    );
    expect(close).toHaveBeenCalled();
  });

  it("crops a portrait image on the vertical axis", async () => {
    stubBitmap(200, 400);
    const { canvas, drawImage } = fakeCanvas({
      "image/webp": new Blob(["w"], { type: "image/webp" }),
    });
    await downscaleToSquare(new Blob(["x"]), 256, () => canvas);
    expect(drawImage).toHaveBeenCalledWith(
      expect.anything(),
      0,
      100,
      200,
      200,
      0,
      0,
      256,
      256,
    );
  });

  it("falls back to jpeg when the browser cannot encode webp", async () => {
    stubBitmap(10, 10);
    const jpeg = new Blob(["j"], { type: "image/jpeg" });
    // Safari answers a webp request with a png blob.
    const { canvas } = fakeCanvas({
      "image/webp": new Blob(["p"], { type: "image/png" }),
      "image/jpeg": jpeg,
    });
    const out = await downscaleToSquare(new Blob(["x"]), 256, () => canvas);
    expect(out).toEqual({ blob: jpeg, filename: "avatar.jpg" });
  });

  it("rejects when nothing can be encoded", async () => {
    stubBitmap(10, 10);
    const { canvas } = fakeCanvas({});
    await expect(
      downscaleToSquare(new Blob(["x"]), 256, () => canvas),
    ).rejects.toThrow();
  });
});
