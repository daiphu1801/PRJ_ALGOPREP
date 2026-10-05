import { expect, test } from "@playwright/test";
import { filterList, openFilter, pickFilter } from "./filter-menu";

/**
 * PROTOTYPE screen check for interview_question_management. The scope guard is the taxonomy:
 * DEC-2026-0830-interview-bank-crud settles FIVE topics, replacing a stale set of four.
 */
test("the topic filter carries the five seeded topics", async ({ page }) => {
  await page.goto("/admin/interview-questions");

  await expect(page.getByRole("heading", { level: 1 })).toHaveText("Ngân hàng câu hỏi phỏng vấn");

  const topics = await openFilter(page, "Lọc theo chủ đề");
  // Five topics plus "Tất cả".
  await expect(topics.getByRole("option")).toHaveCount(6);
  for (const label of ["Lý thuyết CS", "System design", "Database", "Ngôn ngữ", "Hành vi"]) {
    await expect(topics.getByRole("option", { name: label })).toBeVisible();
  }
});

test("the table shows follow-up and rubric counts, not just the question", async ({ page }) => {
  await page.goto("/admin/interview-questions");

  const table = page.getByRole("table", { name: "Danh sách câu hỏi phỏng vấn" });
  for (const header of ["Đào sâu", "Tiêu chí", "Lượt dùng", "Điểm TB"]) {
    await expect(table.getByRole("columnheader", { name: header })).toBeVisible();
  }
  await expect(table.getByRole("row")).toHaveCount(9); // header + 8 rows on page 1
});

test("filters and paging narrow the table", async ({ page }) => {
  await page.goto("/admin/interview-questions");

  await expect(page.getByText("Trang 1 trong 2 · hiển thị 8 dòng")).toBeVisible();

  await pickFilter(page, "Lọc theo chủ đề", "Database");
  await expect(page.getByText("2 / 9 câu hỏi")).toBeVisible();

  await page.getByRole("button", { name: "Xoá câu hỏi IQ-052" }).click();
  await page.getByRole("dialog").getByRole("button", { name: "Xoá" }).click();
  await expect(page.getByText("1 / 9 câu hỏi")).toBeVisible();
});

test("admin adds a difficulty level and it shows up in the filter", async ({ page }) => {
  await page.goto("/admin/interview-questions");

  const levels = filterList(page, "Lọc theo mức độ");
  await openFilter(page, "Lọc theo mức độ");
  await expect(levels.getByRole("option")).toHaveCount(4); // Tất cả + 3 seeded
  await page.keyboard.press("Escape");

  await page.getByRole("button", { name: "Quản lý độ khó" }).click();
  const dialog = page.getByRole("dialog");
  // All three seeded levels have questions, so none can be deleted.
  await expect(dialog.getByRole("button", { name: /chưa xoá được/ })).toHaveCount(3);
  await dialog.getByLabel("Độ khó mới").fill("Cực khó");
  await dialog.getByRole("button", { name: "Thêm" }).click();
  await expect(dialog.getByLabel("Tên độ khó").last()).toHaveValue("Cực khó");

  // Reorder: the new level (last) moves up one place, and the filter follows the new order.
  await dialog.getByRole("button", { name: "Chuyển lên" }).last().click();
  await page.screenshot({ path: "e2e/__screenshots__/interview-level-manager.png", caret: "initial" });
  await dialog.getByRole("button", { name: "Đóng" }).click();

  await openFilter(page, "Lọc theo mức độ");
  await expect(levels.getByRole("option", { name: "Cực khó" })).toBeVisible();
  await expect(levels.getByRole("option").nth(3)).toHaveText("Cực khó");
});

for (const theme of ["light", "dark"] as const) {
  test(`capture ${theme} theme for mockup comparison`, async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 1020 });
    await page.addInitScript((value) => localStorage.setItem("theme", value), theme);
    await page.goto("/admin/interview-questions");
    await expect(page.getByRole("heading", { level: 1 })).toBeVisible();
    await page.screenshot({
      path: `e2e/__screenshots__/interview-question-management-${theme}.png`,
      caret: "initial",
    });
  });
}
