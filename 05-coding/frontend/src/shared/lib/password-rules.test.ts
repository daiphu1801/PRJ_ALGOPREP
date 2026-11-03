import { describe, expect, it } from "vitest";
import { PASSWORD_MAX_BYTES, fitsBcrypt } from "./password-rules";

describe("fitsBcrypt", () => {
  it("accepts exactly 72 ASCII bytes and rejects 73", () => {
    expect(fitsBcrypt("a".repeat(PASSWORD_MAX_BYTES))).toBe(true);
    expect(fitsBcrypt("a".repeat(PASSWORD_MAX_BYTES + 1))).toBe(false);
  });

  it("counts UTF-8 bytes, not characters", () => {
    // "ệ" is 3 bytes: 24 characters fit (72 bytes), 25 do not even though 25 < 72.
    expect(fitsBcrypt("ệ".repeat(24))).toBe(true);
    expect(fitsBcrypt("ệ".repeat(25))).toBe(false);
  });
});
