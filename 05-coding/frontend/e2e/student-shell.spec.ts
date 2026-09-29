import { expect, test } from "@playwright/test";

/**
 * PROTOTYPE screen check for the Student shell. Every one of the 12 Student prototypes repeats the
 * same header (09-layoutBase/Dashboard AlgoPrep.dc.html:50-92), but the app rendered a
 * brand-plus-area-name placeholder instead, so no Student screen had navigation at all. This spec
 * pins the header down: it shows up on every Student route, marks the current one, and the avatar
 * menu reaches the four account destinations.
 */
test("the header renders on every student route and marks the active one", async ({ page }) => {
  await page.goto("/problems");

  const nav = page.getByRole("navigation", { name: "Khu vực người học" });
  await expect(nav.getByRole("link", { name: "Bài toán" })).toHaveAttribute("aria-current", "page");
  // 5 since DEC-2026-0927-student-dashboard-home added "Tổng quan"; the Workspace and Mock
  // Interview items of the prototype stay out (they need an id — see model/student-nav.ts).
  // 6 per 02-bd/screens/users/_shell.md mục 2.2 (5 items) plus "Tổng quan", which
  // DEC-2026-0927-student-dashboard-home added back once the slug existed.
  await expect(nav.getByRole("link")).toHaveCount(6);

  // A problem detail route keeps its parent nav item marked (prefix match, not equality).
  await nav.getByRole("link", { name: "Bài nộp" }).click();
  await expect(page).toHaveURL(/\/submissions$/);
  await expect(nav.getByRole("link", { name: "Bài nộp" })).toHaveAttribute("aria-current", "page");
});

test("the avatar menu opens and reaches the account destinations", async ({ page }) => {
  await page.goto("/dashboard");

  await page.getByRole("button", { name: "Mở menu tài khoản" }).click();
  const menu = page.getByRole("menu");
  await expect(menu.getByRole("menuitem")).toHaveCount(4);

  await menu.getByRole("menuitem", { name: "Cài đặt" }).click();
  await expect(page).toHaveURL(/\/settings$/);
  await expect(page.getByRole("menu")).toHaveCount(0);
});

test("collapses the nav behind one button on a narrow viewport", async ({ page }) => {
  // 02-bd/screens/users/_shell.md mục 6 Q5: below the breakpoint the nav items go behind a menu
  // button while the brand and the user block stay on the bar.
  await page.setViewportSize({ width: 820, height: 900 });
  await page.goto("/dashboard");

  const nav = page.getByRole("navigation", { name: "Khu vực người học" });
  await expect(nav).toBeHidden();

  await page.getByRole("button", { name: "Mở menu điều hướng" }).click();
  await expect(nav.getByRole("link")).toHaveCount(6);

  // Picking a destination closes the drawer rather than leaving it over the new page.
  await nav.getByRole("link", { name: "Bài toán" }).click();
  await expect(page).toHaveURL(/\/problems$/);
  await expect(page.getByRole("navigation", { name: "Khu vực người học" })).toBeHidden();
});
