import { expect, test } from "@playwright/test";
import { pickFilter } from "./filter-menu";

/**
 * PROTOTYPE screen check for admin_user_management. Covers the selection + bulk-action flow, which
 * is the reason DataTable grew row selection, and the confirmation step that
 * 02-bd/screens/admin/admin_user_management.md section 3 requires for the destructive bulk lock
 * (state `bulk-action-confirming`) — the static mockup never drew that dialog.
 */
test("filters combine and the result count follows them", async ({ page }) => {
  await page.goto("/admin/users");

  await expect(page.getByRole("heading", { level: 1 })).toHaveText(
    "Quản lý người dùng",
  );

  const rows = page
    .getByRole("table", { name: "Danh sách tài khoản" })
    .locator("tbody tr");
  await expect(rows).toHaveCount(9);

  await pickFilter(page, "Lọc theo vai trò", "Giảng viên");
  await expect(rows).toHaveCount(2);

  await page.getByLabel("Tìm người dùng").fill("bklinh");
  await expect(rows).toHaveCount(1);
  await expect(page.getByText("1 / 9 tài khoản")).toBeVisible();
});

test("selecting rows reveals bulk actions and locking asks for confirmation first", async ({
  page,
}) => {
  await page.goto("/admin/users");

  // Bulk actions stay hidden until something is selected.
  await expect(
    page.getByRole("button", { name: "Khóa tài khoản", exact: true }),
  ).toHaveCount(0);

  await page.getByRole("checkbox", { name: "Chọn Nguyễn Văn An" }).check();
  await page.getByRole("checkbox", { name: "Chọn Trần Thị Bích" }).check();
  await expect(page.getByText("Đã chọn 2 tài khoản").last()).toBeVisible();

  await page
    .getByRole("button", { name: "Khóa tài khoản", exact: true })
    .first()
    .click();

  // The destructive action must not fire straight from the toolbar, and it names its targets.
  const dialog = page.getByRole("dialog");
  await expect(dialog).toBeVisible();
  await expect(dialog).toContainText("Khóa 2 tài khoản?");
  await expect(dialog).toContainText("Nguyễn Văn An");
  await expect(dialog).toContainText("Trần Thị Bích");

  // Notification is on by default, so an empty reason blocks the confirm.
  await dialog.getByRole("button", { name: "Khóa tài khoản" }).click();
  await expect(dialog).toBeVisible();
  await expect(
    page.getByText("Nhập lý do để thông báo cho người dùng"),
  ).toBeVisible();

  await dialog
    .getByLabel("Lý do khóa tài khoản")
    .fill("Đăng ký hàng loạt từ cùng một IP");
  await dialog.getByRole("button", { name: "Khóa tài khoản" }).click();
  await expect(page.getByText(/Đã khóa 2 tài khoản/).last()).toBeVisible();
  await expect(page.getByText("Đã chọn 2 tài khoản").last()).toHaveCount(0);
});

test("unlocking asks for confirmation and does not ask for a reason", async ({
  page,
}) => {
  await page.goto("/admin/users");

  await page.getByRole("checkbox", { name: "Chọn Hoàng Minh Trí" }).check();
  // The other two buttons act on rows a selection can only partly contain, so they disable.
  await expect(
    page.getByRole("button", { name: "Khóa tài khoản", exact: true }),
  ).toBeDisabled();
  await expect(
    page.getByRole("button", { name: "Mở khóa tài khoản", exact: true }),
  ).toBeEnabled();

  await page
    .getByRole("button", { name: "Mở khóa tài khoản", exact: true })
    .click();
  const dialog = page.getByRole("dialog");
  await expect(dialog).toContainText("Mở khóa 1 tài khoản?");
  await expect(dialog).toContainText("Hoàng Minh Trí");
  await expect(dialog).not.toContainText("Lý do khóa tài khoản");

  await dialog.getByRole("button", { name: "Mở khóa" }).click();
  await expect(page.getByText("Đã mở khóa 1 tài khoản").last()).toBeVisible();
});

test("changing the filter drops the selection so the count cannot go stale", async ({
  page,
}) => {
  await page.goto("/admin/users");

  await page.getByRole("checkbox", { name: "Chọn Nguyễn Văn An" }).check();
  await expect(page.getByText("Đã chọn 1 tài khoản").last()).toBeVisible();

  await pickFilter(page, "Lọc theo vai trò", "Giảng viên");
  await expect(page.getByText("Đã chọn 1 tài khoản").last()).toHaveCount(0);
});

test("select-all covers only the rows currently visible", async ({ page }) => {
  await page.goto("/admin/users");

  await pickFilter(page, "Lọc theo vai trò", "Quản trị");
  // The label carries the row count, because "select all" over a real page of 143 would not mean
  // "lock everything" — it means "lock the 1 admin on this page".
  await page
    .getByRole("checkbox", { name: "Chọn 1 tài khoản đang hiển thị" })
    .check();

  await expect(page.getByText("Đã chọn 1 tài khoản").last()).toBeVisible();
});

for (const theme of ["light", "dark"] as const) {
  test(`capture ${theme} theme for mockup comparison`, async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 940 });
    await page.addInitScript(
      (value) => localStorage.setItem("theme", value),
      theme,
    );
    await page.goto("/admin/users");
    await expect(page.getByRole("table")).toBeVisible();
    await page.getByRole("checkbox", { name: "Chọn Nguyễn Văn An" }).check();
    await page.screenshot({
      path: `e2e/__screenshots__/user-management-${theme}.png`,
      caret: "initial",
    });
  });
}
