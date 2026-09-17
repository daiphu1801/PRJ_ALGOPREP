import { expect, test } from "@playwright/test";

/**
 * PROTOTYPE screen check for admin_user_management. Covers the selection + bulk-action flow, which
 * is the reason DataTable grew row selection, and the confirmation step that
 * 02-bd/screens/admin/admin_user_management.md section 3 requires for the destructive bulk lock
 * (state `bulk-action-confirming`) — the static mockup never drew that dialog.
 */
test("filters combine and the result count follows them", async ({ page }) => {
  await page.goto("/admin/users");

  await expect(page.getByRole("heading", { level: 1 })).toHaveText("Quản lý người dùng");

  const rows = page.getByRole("table", { name: "Danh sách tài khoản" }).locator("tbody tr");
  await expect(rows).toHaveCount(9);

  await page.getByRole("group", { name: "Lọc theo vai trò" }).getByRole("button", { name: "Giảng viên" }).click();
  await expect(rows).toHaveCount(2);

  await page.getByLabel("Tìm người dùng").fill("bklinh");
  await expect(rows).toHaveCount(1);
  await expect(page.getByText("1 / 9 tài khoản")).toBeVisible();
});

test("selecting rows reveals bulk actions and locking asks for confirmation first", async ({ page }) => {
  await page.goto("/admin/users");

  // Bulk actions stay hidden until something is selected.
  await expect(page.getByRole("button", { name: "Khóa tài khoản" })).toHaveCount(0);

  await page.getByRole("checkbox", { name: "Chọn Nguyễn Văn An" }).check();
  await page.getByRole("checkbox", { name: "Chọn Trần Thị Bích" }).check();
  await expect(page.getByText("Đã chọn 2 tài khoản")).toBeVisible();

  await page.getByRole("button", { name: "Khóa tài khoản", exact: true }).first().click();

  // The destructive action must not fire straight from the toolbar.
  const dialog = page.getByRole("dialog");
  await expect(dialog).toBeVisible();
  await expect(dialog).toContainText("Khóa 2 tài khoản?");

  await dialog.getByRole("button", { name: "Huỷ" }).click();
  await expect(page.getByText("Đã chọn 2 tài khoản")).toBeVisible();

  await page.getByRole("button", { name: "Khóa tài khoản", exact: true }).first().click();
  await page.getByRole("dialog").getByRole("button", { name: "Khóa tài khoản" }).click();
  await expect(page.getByText("Đã chọn 2 tài khoản")).toHaveCount(0);
});

test("select-all covers only the rows currently visible", async ({ page }) => {
  await page.goto("/admin/users");

  await page.getByRole("group", { name: "Lọc theo vai trò" }).getByRole("button", { name: "Quản trị" }).click();
  await page.getByRole("checkbox", { name: "Chọn tất cả tài khoản đang hiển thị" }).check();

  await expect(page.getByText("Đã chọn 1 tài khoản")).toBeVisible();
});

for (const theme of ["light", "dark"] as const) {
  test(`capture ${theme} theme for mockup comparison`, async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 940 });
    await page.addInitScript((value) => localStorage.setItem("theme", value), theme);
    await page.goto("/admin/users");
    await expect(page.getByRole("table")).toBeVisible();
    await page.getByRole("checkbox", { name: "Chọn Nguyễn Văn An" }).check();
    await page.screenshot({ path: `e2e/__screenshots__/user-management-${theme}.png`, caret: "initial" });
  });
}
