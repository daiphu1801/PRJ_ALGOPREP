import { describe, expect, it } from "vitest";
import en from "../../../messages/en.json";
import vi from "../../../messages/vi.json";
import { defaultLocale, locales } from "./config";

/** Làm phẳng object lồng nhau thành danh sách key dạng "nav.area.student". */
function flattenKeys(obj: unknown, prefix = ""): string[] {
  if (typeof obj !== "object" || obj === null) return [prefix];
  return Object.entries(obj).flatMap(([key, value]) =>
    flattenKeys(value, prefix ? `${prefix}.${key}` : key),
  );
}

describe("messages", () => {
  // Luật "mọi chuỗi hiển thị phải có key ở CẢ vi.json và en.json" trước đây chỉ là quy ước
  // trong review. Test này biến nó thành cổng chặn thật: thiếu bản dịch là đỏ CI.
  it("vi.json và en.json có đúng cùng tập key", () => {
    const viKeys = flattenKeys(vi).sort();
    const enKeys = flattenKeys(en).sort();

    expect(enKeys.filter((k) => !viKeys.includes(k))).toEqual([]);
    expect(viKeys.filter((k) => !enKeys.includes(k))).toEqual([]);
  });

  it("locale khai báo khớp số file messages và vi là mặc định", () => {
    expect(locales).toHaveLength(2);
    expect(defaultLocale).toBe("vi");
  });
});
