import { expect, test } from "@playwright/test";

/**
 * PROTOTYPE screen check for interview_question_authoring — the one Admin screen with NO mockup
 * (02-bd/screens/shared/interview_question_authoring.md:7-13). These assertions cover the rules the
 * BD states rather than any visual, since there is nothing to compare a visual against.
 */
test("saving is a single action with no draft/publish split", async ({ page }) => {
  await page.goto("/admin/interview-questions/new");

  await expect(page.getByRole("heading", { level: 1 })).toHaveText("Soạn câu hỏi phỏng vấn");
  await expect(page.getByRole("button", { name: "Lưu" })).toBeVisible();
  // RD Q5: a question is live once saved. Anything offering to publish separately is wrong.
  await expect(page.getByRole("button", { name: /xuất bản/i })).toHaveCount(0);
  await expect(page.getByText("Chưa lưu")).toBeVisible();
});

test("an empty rubric saves, a rubric that misses 100% does not", async ({ page }) => {
  await page.goto("/admin/interview-questions/new");

  const save = page.getByRole("button", { name: "Lưu" });
  await expect(save).toBeDisabled();

  await page.getByLabel("Nội dung câu hỏi").fill("Hash table xử lý collision bằng cách nào?");
  // Empty rubric is valid — BD section 1.4.
  await expect(page.getByText("Chưa có tiêu chí đánh giá")).toBeVisible();
  await expect(save).toBeEnabled();

  // One criterion at 0% means the total is not 100, which must block saving.
  await page.getByRole("button", { name: "+ Thêm tiêu chí" }).click();
  await expect(page.getByText("Chưa lưu được — tổng trọng số phải bằng 100%")).toBeVisible();
  await expect(save).toBeDisabled();

  // Twenty clicks of +5 gets to 100.
  const increment = page.getByRole("button", { name: "Tăng trọng số tiêu chí 1" });
  for (let index = 0; index < 20; index += 1) await increment.click();

  await expect(page.getByText("Tổng 100%")).toBeVisible();
  await expect(save).toBeEnabled();
});

test("follow-ups are a dynamic list and the topic cannot be a sixth value", async ({ page }) => {
  await page.goto("/admin/interview-questions/new");

  const remove = page.getByRole("button", { name: "Xoá câu đào sâu 1" });
  // The last remaining row cannot be removed, so the list never reaches zero inputs.
  await expect(remove).toBeDisabled();

  await page.getByRole("button", { name: "+ Thêm câu đào sâu" }).click();
  await expect(page.getByRole("button", { name: /^Xoá câu đào sâu/ })).toHaveCount(2);
  await expect(remove).toBeEnabled();

  // Five seeded topics, no free text (BD section 1.2).
  await expect(page.getByLabel("Chủ đề").getByRole("option")).toHaveCount(5);
});

for (const theme of ["light", "dark"] as const) {
  test(`capture ${theme} theme for owner review`, async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 1020 });
    await page.addInitScript((value) => localStorage.setItem("theme", value), theme);
    await page.goto("/admin/interview-questions/new");
    await expect(page.getByRole("heading", { level: 1 })).toBeVisible();
    await page.screenshot({
      path: `e2e/__screenshots__/interview-question-authoring-${theme}.png`,
      caret: "initial",
    });
  });
}
