import { expect, test } from "@playwright/test";

/**
 * PROTOTYPE screen check for problem_management. The scope guard is the lifecycle:
 * DEC-2026-0830-problem-lifecycle-two-states leaves draft and published only, while the mockup
 * still has a third "Đã ẩn" state on one row and inside a bulk action labelled "Xuất bản / ẩn".
 */
test("only two lifecycle states exist anywhere on the screen", async ({ page }) => {
  await page.goto("/admin/problems");

  await expect(page.getByRole("heading", { level: 1 })).toHaveText("Quản lý bài tập");

  const statusFilter = page.getByRole("group", { name: "Lọc theo trạng thái" });
  // All + published + draft. A third state would make it four.
  await expect(statusFilter.getByRole("button")).toHaveCount(3);
  await expect(page.getByText("Đã ẩn")).toHaveCount(0);

  // A row on page one under the default sort.
  await page.getByRole("checkbox", { name: "Chọn Word Break" }).check();
  await expect(page.getByRole("button", { name: "Xuất bản", exact: true })).toBeVisible();
  await expect(page.getByRole("button", { name: /Xuất bản \/ ẩn/ })).toHaveCount(0);
});

test("sorting and paging work together over the filtered set", async ({ page }) => {
  await page.goto("/admin/problems");

  const table = page.getByRole("table", { name: "Danh sách bài tập" });
  await expect(table.locator("tbody tr")).toHaveCount(8);

  // Sort by submissions ascending: the two drafts have zero.
  const submissions = table.getByRole("columnheader").filter({ hasText: "Lượt nộp" });
  await submissions.getByRole("button").click();
  await expect(submissions).toHaveAttribute("aria-sort", "ascending");
  await expect(table.locator("tbody tr").first()).toContainText("Search Suggestions System");

  await page.getByRole("button", { name: "Trang 2" }).click();
  await expect(page.getByText("Trang 2 trong 3 · hiển thị 8 dòng")).toBeVisible();
});

test("filters narrow the list and deleting asks first", async ({ page }) => {
  await page.goto("/admin/problems");

  await page.getByRole("group", { name: "Lọc theo trạng thái" }).getByRole("button", { name: "Bản nháp" }).click();
  await expect(page.getByText("2 / 21 bài")).toBeVisible();

  await page.getByRole("button", { name: "Xoá bài Course Schedule IV" }).click();
  const dialog = page.getByRole("dialog");
  await expect(dialog).toContainText("Xoá bài Course Schedule IV?");
  await dialog.getByRole("button", { name: "Xoá" }).click();

  await expect(page.getByText("1 / 21 bài")).toBeVisible();
});

for (const theme of ["light", "dark"] as const) {
  test(`capture ${theme} theme for mockup comparison`, async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 940 });
    await page.addInitScript((value) => localStorage.setItem("theme", value), theme);
    await page.goto("/admin/problems");
    await expect(page.getByRole("table")).toBeVisible();
    await page.screenshot({ path: `e2e/__screenshots__/problem-management-${theme}.png`, caret: "initial" });
  });
}
