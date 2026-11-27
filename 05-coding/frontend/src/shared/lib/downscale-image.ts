// Center-crop an image file to a square and re-encode it small enough for the avatar upload
// (BD USR0502 Sheet 9 NO 16-18; DD USR0502 mục 1): webp 0.9, jpeg when the browser cannot encode webp.

type CanvasLike = {
  width: number;
  height: number;
  getContext(kind: "2d"): {
    drawImage(
      image: ImageBitmap,
      sx: number,
      sy: number,
      sw: number,
      sh: number,
      dx: number,
      dy: number,
      dw: number,
      dh: number,
    ): void;
  } | null;
  toBlob(cb: (blob: Blob | null) => void, type: string, quality?: number): void;
};

export type DownscaledImage = { blob: Blob; filename: string };

/** `createCanvas` is injectable because jsdom has no canvas. */
export async function downscaleToSquare(
  file: Blob,
  size = 256,
  createCanvas: () => CanvasLike = () => document.createElement("canvas"),
): Promise<DownscaledImage> {
  const bitmap = await createImageBitmap(file);
  try {
    const side = Math.min(bitmap.width, bitmap.height);
    const sx = (bitmap.width - side) / 2;
    const sy = (bitmap.height - side) / 2;
    const canvas = createCanvas();
    canvas.width = size;
    canvas.height = size;
    const ctx = canvas.getContext("2d");
    if (!ctx) throw new Error("Canvas 2D context unavailable");
    ctx.drawImage(bitmap, sx, sy, side, side, 0, 0, size, size);
    const encode = (type: string) =>
      new Promise<Blob | null>((resolve) => canvas.toBlob(resolve, type, 0.9));
    const webp = await encode("image/webp");
    if (webp && webp.type === "image/webp") {
      return { blob: webp, filename: "avatar.webp" };
    }
    const jpeg = await encode("image/jpeg");
    if (!jpeg) throw new Error("Image encoding failed");
    return { blob: jpeg, filename: "avatar.jpg" };
  } finally {
    bitmap.close?.();
  }
}
