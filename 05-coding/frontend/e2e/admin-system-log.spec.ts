import { expect, test } from "@playwright/test";

/**
 * PROTOTYPE screen check for admin_system_log. Guards the hard boundary in
 * 02-bd/screens/admin/admin_system_log.md section 0: human admin actions only, and nothing left
 * over from the rejudge feature that DEC-2026-0828-remove-rejudge-scope removed — the static
 * mockup still carries a "Chấm lại" category, a "Phiên chấm lại đã chạy" stat and an #RJ-0139 row.
 */
test("system log shows only human admin actions, with no rejudge leftovers", async ({ page }) => {
  await page.goto("/admin/system-log");

  await expect(page.getByRole("heading", { level: 1 })).toHaveText("Nhật ký hệ thống");

  // Three stat cards, not the mockup's four.
  await expect(page.getByText("Hành động quản trị 24 giờ")).toBeVisible();
  await expect(page.getByText(/Phiên chấm lại/i)).toHaveCount(0);

  // Four categories in the filter (plus "Tất cả"), no "Chấm lại".
  const filter = page.getByRole("group", { name: "Lọc theo phân loại" });
  await expect(filter.getByRole("button")).toHaveCount(5);
  await expect(filter.getByRole("button", { name: /chấm lại/i })).toHaveCount(0);

  // No rejudge event survived into the sample data.
  await expect(page.getByText(/RJ-0139/)).toHaveCount(0);
});

test("category filter and search combine with AND", async ({ page }) => {
  await page.goto("/admin/system-log");

  // Scoped to the event list: the category chart on the right is also a <ul>.
  const list = page.getByRole("list", { name: "Danh sách sự kiện" }).getByRole("listitem");
  await expect(list).toHaveCount(9);

  await page.getByRole("button", { name: "Xác thực", exact: true }).click();
  await expect(list).toHaveCount(3);

  // Search matches service/actor/event id — not the message, per BD section 2.
  await page.getByLabel("Tìm trong nhật ký").fill("pthuong");
  await expect(list).toHaveCount(0);
  await expect(page.getByText("Không có sự kiện nào khớp bộ lọc hiện tại")).toBeVisible();

  await page.getByRole("button", { name: "Xoá bộ lọc" }).click();
  await expect(list).toHaveCount(9);
});

for (const theme of ["light", "dark"] as const) {
  test(`capture ${theme} theme for mockup comparison`, async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 940 });
    await page.addInitScript((value) => localStorage.setItem("theme", value), theme);
    await page.goto("/admin/system-log");
    await expect(page.getByRole("heading", { level: 1 })).toBeVisible();
    await page.screenshot({ path: `e2e/__screenshots__/system-log-${theme}.png`, caret: "initial" });
  });
}
