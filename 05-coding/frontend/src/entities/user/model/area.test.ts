import { describe, expect, it } from "vitest";
import { HOME_PATH_BY_ROLE, ROLE_BY_AREA } from "./area";

describe("ROLE_BY_AREA", () => {
  it("phủ đủ ba khu vực cần quyền", () => {
    expect(Object.keys(ROLE_BY_AREA).sort()).toEqual(["admin", "instructor", "student"]);
  });
});

describe("HOME_PATH_BY_ROLE", () => {
  it("mỗi vai trò có đúng một đích, và đích của INSTRUCTOR/ADMIN nằm trong khu vực riêng", () => {
    expect(HOME_PATH_BY_ROLE.STUDENT).toBe("/progress");
    expect(HOME_PATH_BY_ROLE.INSTRUCTOR.startsWith("/instructor/")).toBe(true);
    // Khoá luật: đích của ADMIN phải nằm dưới /admin — nếu ai đó trỏ nó ra ngoài thì middleware
    // guard theo prefix sẽ không bảo vệ được đường đó nữa.
    expect(HOME_PATH_BY_ROLE.ADMIN.startsWith("/admin/")).toBe(true);
  });
});
