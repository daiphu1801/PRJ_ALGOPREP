import { expect, test } from "@playwright/test";

/**
 * PROTOTYPE screen check for admin_queue_monitor. Two scope guards and the sort behaviour:
 * rejudge is gone (DEC-2026-0828-remove-rejudge-scope) so only two priority levels and two latency
 * queues remain, and the "Kỳ thi" label was confirmed a mislabelled priority tier, never an exam
 * feature (DEC-2026-0831-judge-orchestration-ops-details).
 */
test("only two priority levels survive, with no rejudge queue or exam wording", async ({ page }) => {
  await page.goto("/admin/queue");

  await expect(page.getByRole("heading", { level: 1 })).toHaveText("Hàng đợi chấm");

  await expect(page.getByText("Hàng đợi thường")).toBeVisible();
  await expect(page.getByText("Hàng đợi ưu tiên cao")).toBeVisible();
  await expect(page.getByText(/chấm lại/i)).toHaveCount(0);
  await expect(page.getByText(/kỳ thi/i)).toHaveCount(0);
});

test("infrastructure events live here and point at the system log for admin actions", async ({ page }) => {
  await page.goto("/admin/queue");

  const list = page.getByRole("list", { name: "Sự kiện hạ tầng gần đây" });
  await expect(list.getByRole("listitem")).toHaveCount(6);
  await expect(page.getByText(/xem hành động ở màn Nhật ký hệ thống/)).toBeVisible();
});

test("column sort reorders the job table and announces itself", async ({ page }) => {
  await page.goto("/admin/queue");

  const table = page.getByRole("table", { name: "Job trong hàng đợi" });
  const waitHeader = table.getByRole("columnheader").filter({ hasText: "Chờ" });

  // Default: longest wait first.
  await expect(waitHeader).toHaveAttribute("aria-sort", "descending");
  await expect(table.locator("tbody tr").first()).toContainText("46s");

  await waitHeader.getByRole("button").click();
  await expect(waitHeader).toHaveAttribute("aria-sort", "ascending");
  // -1 stands for "not applicable" on the failed job, so it sorts to the front ascending.
  await expect(table.locator("tbody tr").first()).toContainText("#J-77398");
});

test("cluster controls are real toggles, not decoration", async ({ page }) => {
  await page.goto("/admin/queue");

  const pause = page.getByRole("switch", { name: "Tạm dừng tiêu thụ hàng đợi" });
  await expect(pause).toHaveAttribute("aria-checked", "false");
  await pause.click();
  await expect(pause).toHaveAttribute("aria-checked", "true");
});

for (const theme of ["light", "dark"] as const) {
  test(`capture ${theme} theme for mockup comparison`, async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 940 });
    await page.addInitScript((value) => localStorage.setItem("theme", value), theme);
    await page.goto("/admin/queue");
    await expect(page.getByRole("heading", { level: 1 })).toBeVisible();
    await page.screenshot({ path: `e2e/__screenshots__/queue-monitor-${theme}.png`, caret: "initial" });
  });
}
