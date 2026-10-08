import { expect, test } from "@playwright/test";

/**
 * PROTOTYPE screen check for problem_authoring. The scope guard is partial scoring: PROTOTYPE_DEBT
 * 2.6 removed the per-testcase weight column, the weight total and the "Chấm điểm từng phần" block.
 * The surviving partial score (F4-13) is an automatic pass ratio computed by judge-orchestration,
 * so nothing on this screen should let an author declare weights.
 */
test("no per-testcase weights or partial-score block survive", async ({
  page,
}) => {
  await page.goto("/admin/problems/1268/edit");

  await expect(page.getByRole("heading", { level: 1 })).toHaveText(
    "Minimum Window Substring",
  );

  await page.getByRole("button", { name: /^Testcase/ }).click();
  const table = page.getByRole("table", { name: "Testcase" });
  // #, input, expected output, visibility, review state, actions: no weight or score column.
  await expect(table.getByRole("columnheader")).toHaveCount(6);
  await expect(page.getByText(/trọng số/i)).toHaveCount(0);
  await expect(page.getByText(/chấm điểm từng phần/i)).toHaveCount(0);
  await expect(table.getByRole("columnheader", { name: "Điểm" })).toHaveCount(
    0,
  );
});

test("the publish checklist gates the CTA", async ({ page }) => {
  await page.goto("/admin/problems/1268/edit");

  // The seeded draft still has testcases awaiting review and a sample solution that has not been
  // confirmed, so the checklist blocks publishing. The reason is a toast (DEC-2026-1003-toast-feedback-
  // channel), raised when the aria-disabled button is pressed; force because Playwright treats
  // aria-disabled as not actionable.
  const publish = page.getByRole("button", { name: "Lưu và xuất bản" });
  await expect(publish).toHaveAttribute("aria-disabled", "true");
  await expect(page.getByText("Có ít nhất 8 testcase")).toBeVisible();
  await publish.click({ force: true });
  await expect(page.getByText(/Chưa xuất bản được/)).toBeVisible();
});

test("five tabs, and only two lifecycle states in properties", async ({
  page,
}) => {
  await page.goto("/admin/problems/1268/edit");

  const tabs = page.getByRole("group", { name: "Phần đang soạn" });
  // Content, examples, testcases, spec (DEC-2026-1003-single-function-signature), AI guidance.
  await expect(tabs.getByRole("button")).toHaveCount(5);

  const status = page.getByRole("group", { name: "Trạng thái" });
  await expect(status.getByRole("button")).toHaveCount(2);
  await expect(status.getByRole("button", { name: /ẩn/i })).toHaveCount(0);
});

test("AI guidance carries two guards, not the cut tiered-hint one", async ({
  page,
}) => {
  await page.goto("/admin/problems/1268/edit");

  await page.getByRole("button", { name: "Gợi ý AI" }).click();
  await expect(page.getByRole("switch")).toHaveCount(2);
  await expect(page.getByText(/gợi ý ẩn/i)).toHaveCount(0);
});

for (const theme of ["light", "dark"] as const) {
  test(`capture ${theme} theme for mockup comparison`, async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 1020 });
    await page.addInitScript(
      (value) => localStorage.setItem("theme", value),
      theme,
    );
    await page.goto("/admin/problems/1268/edit");
    await expect(page.getByRole("heading", { level: 1 })).toBeVisible();
    await page.screenshot({
      path: `e2e/__screenshots__/problem-authoring-${theme}.png`,
      caret: "initial",
    });
  });
}
