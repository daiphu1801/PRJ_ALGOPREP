import { expect, test } from "@playwright/test";

/**
 * PROTOTYPE screen check for the Instructor shell. All 5 Instructor prototypes repeat the same
 * sidebar and the same blue palette (09-layoutBase/Giáo viên - Tổng quan.dc.html:18-46, :63-121),
 * but the app rendered them with no shell at all and on the generic placeholder tokens — reported
 * by the owner 2026-09-27 as "chưa theo layoutbase ở css". This spec pins down what was missing:
 * the sidebar exists on every Instructor route, marks the current one, and the palette scope is
 * actually applied (a screen on the generic tokens has a white background, not the mockup's
 * #F5F8FC).
 */
test("the sidebar renders on every instructor route and marks the active one", async ({ page }) => {
  await page.goto("/instructor/overview");

  const nav = page.getByRole("navigation", { name: "Điều hướng khu giảng viên" });
  await expect(nav.getByRole("link", { name: "Tổng quan" })).toHaveAttribute("aria-current", "page");
  // 5 main destinations + 2 under "Khác".
  await expect(nav.getByRole("link")).toHaveCount(7);

  await nav.getByRole("link", { name: "Lớp của tôi" }).click();
  await expect(page).toHaveURL(/\/instructor\/classes$/);
  await expect(nav.getByRole("link", { name: "Lớp của tôi" })).toHaveAttribute("aria-current", "page");
});

test("instructor screens render in the mockup palette, not the generic tokens", async ({ page }) => {
  await page.goto("/instructor/grading");

  const shell = page.locator(".instructor-shell");
  await expect(shell).toHaveCount(1);
  // dc.html:20 --bg light. The generic :root value is #ffffff, so this is what separates
  // "the scope is applied" from "the scope silently did not match".
  await expect(shell).toHaveCSS("background-color", "rgb(245, 248, 252)");

  await page.screenshot({ path: "e2e/__screenshots__/instructor-grading.png", fullPage: true });
});

test("a nested route lights up exactly one nav item", async ({ page }) => {
  // A student detail page sits under /instructor/classes/<id>/students/<id> — its parent
  // "Lớp của tôi" must be the ONLY item marked.
  await page.goto("/instructor/classes/c1/students/s1");

  const nav = page.getByRole("navigation", { name: "Điều hướng khu giảng viên" });
  await expect(nav.getByRole("link", { name: "Lớp của tôi" })).toHaveAttribute("aria-current", "page");
  await expect(nav.locator('[aria-current="page"]')).toHaveCount(1);
});

test("class management holds no student table; the student list does", async ({ page }) => {
  // The two near-identical student tables were merged 2026-09-27: classes manage classes, the
  // student list manages students.
  await page.goto("/instructor/classes");
  await expect(page.getByRole("table")).toHaveCount(0);

  // "Xem học viên" carries the class through as a preselected filter.
  await page.getByRole("link", { name: "Xem học viên" }).first().click();
  await expect(page).toHaveURL(/\/instructor\/students\?classId=/);
  await expect(page.getByRole("table", { name: "Danh sách học viên" })).toBeVisible();
});

test("the sidebar stays put while a long table scrolls", async ({ page }) => {
  // Before dc.html:63's `position: sticky; top: 0; height: 100vh` was ported, the sidebar was an
  // ordinary flex child: the student table stretched the page and scrolled the whole nav away.
  await page.setViewportSize({ width: 1440, height: 800 });
  await page.goto("/instructor/students");

  const nav = page.getByRole("navigation", { name: "Điều hướng khu giảng viên" });
  await expect(nav).toBeVisible();
  await page.mouse.wheel(0, 2000);

  await expect.poll(async () => (await nav.boundingBox())?.y).toBe(0);
  await expect.poll(async () => (await nav.boundingBox())?.height).toBe(800);
});

test("the student list pages instead of rendering every row", async ({ page }) => {
  await page.goto("/instructor/students");

  const table = page.getByRole("table", { name: "Danh sách học viên" });
  await expect(table).toBeVisible();
  // 10 rows a page, same size as the class_progress table over the same students (12 of them).
  await expect(table.locator("tbody tr")).toHaveCount(10);
  await expect(page.getByText("Trang 1 / 2")).toBeVisible();
});
