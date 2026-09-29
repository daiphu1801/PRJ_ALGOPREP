import { expect, test } from "@playwright/test";

/**
 * PROTOTYPE screen check for USR0601_dashboard — the screen added by
 * DEC-2026-0927-student-dashboard-home, which also moved the STUDENT post-login destination here
 * from /progress. Block-level interaction is unit-tested
 * (src/views/users/dashboard/ui/blocks/dashboard-blocks.test.tsx); this pins the assembled page, which is
 * an async Server Component and therefore only reachable end to end.
 */
test("renders both halves of the merged screen", async ({ page }) => {
  await page.goto("/dashboard");

  await expect(page.getByRole("heading", { name: "Chào bạn, tiếp tục nhé" })).toBeVisible();

  for (const block of [
    "Bài nộp theo ngày",
    "Năng lực theo chủ đề",
    "Hoạt động 12 tháng",
    "Bài toán gợi ý",
    "Mock Interview",
    "Solution Review",
  ]) {
    await expect(page.getByRole("region", { name: block, exact: true })).toBeVisible();
  }

  // The my_progress half, merged in by DEC-2026-0927-student-area-merge-and-shared-shell: the
  // short progress bars were replaced by the full topic table, which says strictly more.
  await expect(page.getByRole("table", { name: "Theo chủ đề" })).toBeVisible();
  await expect(page.getByRole("heading", { name: "Nên ưu tiên" })).toBeVisible();

  // The four stat cards resolve their own queries, so wait on real numbers rather than the frame.
  await expect(page.getByText("182", { exact: true })).toBeVisible();

  // Area chrome: the footer belongs to every Student screen (_shell.md mục 3).
  await expect(page.getByRole("contentinfo")).toBeVisible();
});

test("is the first nav item and the destination the brand links to", async ({ page }) => {
  await page.goto("/problems");

  const nav = page.getByRole("navigation", { name: "Khu vực người học" });
  await nav.getByRole("link", { name: "Tổng quan" }).click();

  await expect(page).toHaveURL(/\/dashboard$/);
  await expect(nav.getByRole("link", { name: "Tổng quan" })).toHaveAttribute("aria-current", "page");
  // "Tiến độ" is gone from the nav entirely: my_progress was merged into this screen.
  await expect(nav.getByRole("link", { name: "Tiến độ" })).toHaveCount(0);
});
