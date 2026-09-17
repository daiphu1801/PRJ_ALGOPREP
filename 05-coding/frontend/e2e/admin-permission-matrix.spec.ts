import { expect, test } from "@playwright/test";

/**
 * PROTOTYPE screen check for admin_permission_matrix. Guards the two rules that make the matrix
 * safe: the function list is fixed seed data matching F1-12 (ten entries, no REJUDGE_MANAGEMENT
 * after DEC-2026-0828-remove-rejudge-scope), and STUDENT's cells are read-only because its basic
 * rights live outside the matrix (F1-05).
 */
test("exactly the ten F1-12 functions, with no rejudge row", async ({ page }) => {
  await page.goto("/admin/permissions");

  await expect(page.getByRole("heading", { level: 1 })).toHaveText("Ma trận phân quyền");

  const rows = page.getByRole("table").locator("tbody tr");
  await expect(rows).toHaveCount(10);
  await expect(page.getByText("REJUDGE_MANAGEMENT")).toHaveCount(0);
  await expect(page.getByText("PERMISSION_MATRIX")).toBeVisible();
});

test("STUDENT cells are read-only, INSTRUCTOR cells are not", async ({ page }) => {
  await page.goto("/admin/permissions");

  const roleSwitcher = page.getByRole("group", { name: "Chọn vai trò" });

  const instructorCell = page.getByRole("checkbox", {
    name: "Sửa trên Quản lý tài khoản người dùng cho vai trò INSTRUCTOR",
  });
  await expect(instructorCell).toBeEnabled();
  await expect(instructorCell).not.toBeChecked();
  await instructorCell.check();
  await expect(instructorCell).toBeChecked();

  await roleSwitcher.getByRole("button", { name: "STUDENT" }).click();
  // `exact` matters: the table's sr-only caption also contains the role name.
  await expect(page.getByText("Vai trò STUDENT", { exact: true })).toBeVisible();
  await expect(page.getByRole("checkbox").first()).toBeDisabled();
});

test("system roles cannot be deleted, a new role can", async ({ page }) => {
  await page.goto("/admin/permissions");

  await expect(page.getByText("Vai trò hệ thống · không thể xoá")).toBeVisible();
  await expect(page.getByRole("button", { name: "Xoá vai trò này" })).toHaveCount(0);

  await page.getByRole("button", { name: "+ Vai trò mới" }).click();
  await page.getByLabel("Tên vai trò mới").fill("Trợ giảng");
  await page.getByRole("button", { name: "Tạo" }).click();

  // A custom role becomes active and IS deletable, behind a confirmation.
  await expect(page.getByRole("group", { name: "Chọn vai trò" }).getByRole("button", { name: "Trợ giảng" })).toBeVisible();
  await page.getByRole("button", { name: "Xoá vai trò này" }).click();
  await expect(page.getByRole("dialog")).toContainText("Xoá vai trò Trợ giảng?");
  await page.getByRole("dialog").getByRole("button", { name: "Xoá vai trò này" }).click();
  await expect(page.getByRole("group", { name: "Chọn vai trò" }).getByRole("button", { name: "Trợ giảng" })).toHaveCount(0);
});

for (const theme of ["light", "dark"] as const) {
  test(`capture ${theme} theme for mockup comparison`, async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 940 });
    await page.addInitScript((value) => localStorage.setItem("theme", value), theme);
    await page.goto("/admin/permissions");
    await expect(page.getByRole("table")).toBeVisible();
    await page.screenshot({ path: `e2e/__screenshots__/permission-matrix-${theme}.png`, caret: "initial" });
  });
}
