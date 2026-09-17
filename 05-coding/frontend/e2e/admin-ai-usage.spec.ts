import { expect, test } from "@playwright/test";

/**
 * PROTOTYPE screen check for admin_ai_usage. The load-bearing assertion is scope: the tiered-hint
 * feature was cut entirely by DEC-2026-0831-remove-tiered-hints-ai-config, and the static mockup
 * still charts it as a fourth series and labels a column "lượt gợi ý".
 */
test("only the three in-scope AI features are shown", async ({ page }) => {
  await page.goto("/admin/ai-usage");

  await expect(page.getByRole("heading", { level: 1 })).toHaveText("Tiêu thụ token AI");

  for (const feature of ["Phân tích bài giải", "Phỏng vấn giả lập", "Sinh testcase"]) {
    await expect(page.getByText(feature).first()).toBeVisible();
  }
  // "Gợi ý" must not survive anywhere — legend, column label or subtitle.
  await expect(page.getByText(/gợi ý/i)).toHaveCount(0);
  await expect(page.getByText("Phân tích và phỏng vấn cộng lại")).toBeVisible();
});

test("the anomaly notice warns without claiming the account was locked", async ({ page }) => {
  await page.goto("/admin/ai-usage");

  // DEC-2026-0831-ai-usage-anomaly-alert: soft warning for review, never an automatic lock.
  const notice = page.getByText("Một tài khoản dùng bất thường").locator("..");
  await expect(notice).toContainText("hệ thống không tự khoá");
  await expect(notice.getByRole("button")).toHaveCount(0);
});

test("range switch changes how many days the chart plots", async ({ page }) => {
  await page.goto("/admin/ai-usage");

  await expect(page.getByText("Tách theo tính năng, 14 ngày gần nhất")).toBeVisible();

  await page.getByRole("group", { name: "Khoảng thời gian" }).getByRole("button", { name: "7 ngày" }).click();
  await expect(page.getByText("Tách theo tính năng, 7 ngày gần nhất")).toBeVisible();
});

for (const theme of ["light", "dark"] as const) {
  test(`capture ${theme} theme for mockup comparison`, async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 940 });
    await page.addInitScript((value) => localStorage.setItem("theme", value), theme);
    await page.goto("/admin/ai-usage");
    await expect(page.getByRole("heading", { level: 1 })).toBeVisible();
    await page.screenshot({ path: `e2e/__screenshots__/ai-usage-${theme}.png`, caret: "initial" });
  });
}
