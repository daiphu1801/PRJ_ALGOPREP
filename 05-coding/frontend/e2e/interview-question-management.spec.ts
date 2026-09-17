import { expect, test } from "@playwright/test";

/**
 * PROTOTYPE screen check for interview_question_management. The scope guard is the taxonomy:
 * DEC-2026-0830-interview-bank-crud settles FIVE topics, replacing a stale set of four.
 */
test("the topic filter carries exactly the five settled topics", async ({ page }) => {
  await page.goto("/admin/interview-questions");

  await expect(page.getByRole("heading", { level: 1 })).toHaveText("Ngân hàng câu hỏi phỏng vấn");

  const topics = page.getByRole("group", { name: "Lọc theo chủ đề" });
  // Five topics plus "Tất cả".
  await expect(topics.getByRole("button")).toHaveCount(6);
  for (const label of ["Lý thuyết CS", "System design", "Database", "Ngôn ngữ", "Hành vi"]) {
    await expect(topics.getByRole("button", { name: label })).toBeVisible();
  }
});

test("cards show follow-ups and a weighted rubric, not just the question", async ({ page }) => {
  await page.goto("/admin/interview-questions");

  // Card renders a <section>; pick the first one that actually holds a question heading.
  const card = page
    .locator("section")
    .filter({ has: page.getByRole("heading", { level: 3 }) })
    .first();
  await expect(card.getByText("Đào sâu")).toBeVisible();
  await expect(card.getByText("Tiêu chí đánh giá")).toBeVisible();
  await expect(card.getByText(/Dùng \d+ lần · điểm TB/)).toBeVisible();
});

test("filters and paging narrow the card grid", async ({ page }) => {
  await page.goto("/admin/interview-questions");

  await expect(page.getByRole("heading", { level: 3 })).toHaveCount(6);
  await expect(page.getByText("Trang 1 trong 2 · hiển thị 6 thẻ")).toBeVisible();

  await page.getByRole("group", { name: "Lọc theo chủ đề" }).getByRole("button", { name: "Database" }).click();
  await expect(page.getByRole("heading", { level: 3 })).toHaveCount(2);
  await expect(page.getByText("2 / 9 câu hỏi")).toBeVisible();

  await page.getByRole("button", { name: "Xoá câu hỏi IQ-052" }).click();
  await page.getByRole("dialog").getByRole("button", { name: "Xoá" }).click();
  await expect(page.getByText("1 / 9 câu hỏi")).toBeVisible();
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
