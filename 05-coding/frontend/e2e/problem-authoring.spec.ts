import { expect, test } from "@playwright/test";

/**
 * PROTOTYPE screen check for problem_authoring. The scope guard is partial scoring: PROTOTYPE_DEBT
 * 2.6 removed the per-testcase weight column, the weight total and the "Chấm điểm từng phần" block.
 * The surviving partial score (F4-13) is an automatic pass ratio computed by judge-orchestration,
 * so nothing on this screen should let an author declare weights.
 */
test("no per-testcase weights or partial-score block survive", async ({ page }) => {
  await page.goto("/admin/problems/1268");

  await expect(page.getByRole("heading", { level: 1 })).toHaveText("Minimum Window Substring");

  await page.getByRole("button", { name: /^Testcase/ }).click();
  const table = page.getByRole("table", { name: "Testcase" });
  await expect(table.getByRole("columnheader")).toHaveCount(4);
  await expect(page.getByText(/trọng số/i)).toHaveCount(0);
  await expect(page.getByText(/chấm điểm từng phần/i)).toHaveCount(0);
  await expect(table.getByRole("columnheader", { name: "Điểm" })).toHaveCount(0);
});

test("the publish checklist gates the CTA", async ({ page }) => {
  await page.goto("/admin/problems/1268");

  // The seeded draft meets all four requirements.
  await expect(page.getByRole("button", { name: "Lưu và xuất bản" })).toBeEnabled();
  await expect(page.getByText("Chưa xuất bản được")).toHaveCount(0);
  await expect(page.getByText("Có ít nhất 8 testcase")).toBeVisible();
});

test("four tabs, and only two lifecycle states in properties", async ({ page }) => {
  await page.goto("/admin/problems/1268");

  const tabs = page.getByRole("group", { name: "Phần đang soạn" });
  await expect(tabs.getByRole("button")).toHaveCount(4);

  const status = page.getByRole("group", { name: "Trạng thái" });
  await expect(status.getByRole("button")).toHaveCount(2);
  await expect(status.getByRole("button", { name: /ẩn/i })).toHaveCount(0);
});

test("AI guidance carries two guards, not the cut tiered-hint one", async ({ page }) => {
  await page.goto("/admin/problems/1268");

  await page.getByRole("button", { name: "Gợi ý AI" }).click();
  await expect(page.getByRole("switch")).toHaveCount(2);
  await expect(page.getByText(/gợi ý ẩn/i)).toHaveCount(0);
});

for (const theme of ["light", "dark"] as const) {
  test(`capture ${theme} theme for mockup comparison`, async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 1020 });
    await page.addInitScript((value) => localStorage.setItem("theme", value), theme);
    await page.goto("/admin/problems/1268");
    await expect(page.getByRole("heading", { level: 1 })).toBeVisible();
    await page.screenshot({ path: `e2e/__screenshots__/problem-authoring-${theme}.png`, caret: "initial" });
  });
}
