import { describe, expect, it } from "vitest";
import { checkImageFile } from "./image-file";

describe("checkImageFile", () => {
  it("accepts png, jpeg and webp up to the limit", () => {
    expect(checkImageFile({ type: "image/png", size: 10 }, 10)).toBeNull();
    expect(checkImageFile({ type: "image/webp", size: 5 }, 10)).toBeNull();
  });
  it("rejects svg and other types, then oversize files", () => {
    expect(checkImageFile({ type: "image/svg+xml", size: 1 }, 10)).toBe("type");
    expect(checkImageFile({ type: "image/png", size: 11 }, 10)).toBe("size");
  });
});
