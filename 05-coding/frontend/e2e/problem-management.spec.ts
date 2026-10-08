import { expect, test } from "@playwright/test";
import { filterList, openFilter, pickFilter } from "./filter-menu";

/**
 * PROTOTYPE screen check for problem_management. The scope guard is the lifecycle:
 * DEC-2026-0830-problem-lifecycle-two-states leaves draft and published only, while the mockup
 * still has a third "Đã ẩn" state on one row and inside a bulk action labelled "Xuất bản / ẩn".
 */
test("only two lifecycle states exist anywhere on the screen", async ({
  page,
}) => {
  await page.goto("/admin/problems");

  await expect(page.getByRole("heading", { level: 1 })).toHaveText(
    "Quản lý bài tập",
  );

  const statusFilter = await openFilter(page, "Lọc theo trạng thái");
  // All + published + draft. A third state would make it four.
  await expect(statusFilter.getByRole("option")).toHaveCount(3);
  await page.keyboard.press("Escape");
  await expect(page.getByText("Đã ẩn")).toHaveCount(0);

  // A row on page one under the default sort.
  await page.getByRole("checkbox", { name: "Chọn Word Break" }).check();
  await expect(
    page.getByRole("button", { name: "Xuất bản", exact: true }),
  ).toBeVisible();
  await expect(
    page.getByRole("button", { name: /Xuất bản \/ ẩn/ }),
  ).toHaveCount(0);
});

test("sorting and paging work together over the filtered set", async ({
  page,
}) => {
  await page.goto("/admin/problems");

  const table = page.getByRole("table", { name: "Danh sách bài tập" });
  await expect(table.locator("tbody tr")).toHaveCount(8);

  // Sort by submissions ascending: the two drafts have zero.
  const submissions = table
    .getByRole("columnheader")
    .filter({ hasText: "Lượt nộp" });
  await submissions.getByRole("button").click();
  await expect(submissions).toHaveAttribute("aria-sort", "ascending");
  await expect(table.locator("tbody tr").first()).toContainText(
    "Search Suggestions System",
  );

  await page.getByRole("button", { name: "Trang 2" }).click();
  await expect(
    page.getByText("Trang 2 trong 3 · hiển thị 8 dòng"),
  ).toBeVisible();
});

test("filters narrow the list and deleting asks first", async ({ page }) => {
  await page.goto("/admin/problems");

  await pickFilter(page, "Lọc theo trạng thái", "Bản nháp");
  await expect(page.getByText("2 / 21 bài")).toBeVisible();

  await page
    .getByRole("button", { name: "Xoá bài Course Schedule IV" })
    .click();
  const dialog = page.getByRole("dialog");
  await expect(dialog).toContainText("Xoá bài Course Schedule IV?");
  await dialog.getByRole("button", { name: "Xoá" }).click();

  await expect(page.getByText("1 / 21 bài")).toBeVisible();
});

for (const theme of ["light", "dark"] as const) {
  test(`capture ${theme} theme for mockup comparison`, async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 940 });
    await page.addInitScript(
      (value) => localStorage.setItem("theme", value),
      theme,
    );
    await page.goto("/admin/problems");
    await expect(page.getByRole("table")).toBeVisible();
    await page.screenshot({
      path: `e2e/__screenshots__/problem-management-${theme}.png`,
      caret: "initial",
    });
  });
}

test("admin adds a difficulty level, moves problems onto it with the bulk picker, then it is in use", async ({
  page,
}) => {
  await page.goto("/admin/problems");

  const levels = filterList(page, "Lọc theo độ khó");
  await openFilter(page, "Lọc theo độ khó");
  await expect(levels.getByRole("option")).toHaveCount(4); // Tất cả + 3 seeded
  await page.keyboard.press("Escape");

  await page.getByRole("button", { name: "Quản lý độ khó" }).click();
  const dialog = page.getByRole("dialog");
  // All three seeded levels have problems, so none can be deleted.
  await expect(
    dialog.getByRole("button", { name: /chưa xoá được/ }),
  ).toHaveCount(3);
  await dialog.getByLabel("Độ khó mới").fill("Cực khó");
  await dialog.getByRole("button", { name: "Thêm" }).click();
  await expect(dialog.getByLabel("Tên độ khó").last()).toHaveValue("Cực khó");
  await page.screenshot({
    path: "e2e/__screenshots__/problem-level-manager.png",
    caret: "initial",
  });
  await dialog.getByRole("button", { name: "Đóng" }).click();
  await openFilter(page, "Lọc theo độ khó");
  await expect(levels.getByRole("option", { name: "Cực khó" })).toBeVisible();
  await page.keyboard.press("Escape");

  // Bulk change: select the rows on the page, pick the new level, and the badge column follows.
  await page
    .getByRole("checkbox", { name: "Chọn tất cả bài đang hiển thị" })
    .check();
  await page.getByRole("button", { name: "Đổi độ khó" }).click();
  const picker = page.getByRole("dialog");
  await picker.getByLabel("Độ khó mới").selectOption({ label: "Cực khó" });
  await picker.getByRole("button", { name: "Áp dụng" }).click();
  await pickFilter(page, "Lọc theo độ khó", "Cực khó");
  await expect(
    page.getByRole("table", { name: "Danh sách bài tập" }).locator("tbody tr"),
  ).toHaveCount(8);
});
