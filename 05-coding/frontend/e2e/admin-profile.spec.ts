import { expect, test } from "@playwright/test";

test("the admin profile link stays inside the admin shell", async ({
  page,
}) => {
  await page.goto("/admin/overview");
  // The menu button can be clicked before hydration attaches its handler on a cold dev server, so
  // retry opening the menu until the item shows.
  await expect(async () => {
    await page.locator('button[aria-haspopup="menu"]').click();
    await page
      .getByRole("menuitem", { name: "Trang cá nhân" })
      .click({ timeout: 2_000 });
  }).toPass({ timeout: 15_000 });

  // The first visit compiles the route on the dev server, so give it more than the default 5 s.
  await expect(page).toHaveURL(/\/admin\/profile$/, { timeout: 15_000 });
  await expect(page.locator(".admin-shell")).toHaveCount(1);
  await expect(
    page.locator("main dd", { hasText: "Quản trị viên" }),
  ).toBeVisible({ timeout: 15_000 });
});

test("on a short admin screen the footer sits at the bottom of the viewport", async ({
  page,
}) => {
  await page.setViewportSize({ width: 1440, height: 900 });
  await page.goto("/admin/profile");
  const footer = page.getByText("AlgoPrep Admin · v2.4.1");
  await expect(footer).toBeVisible({ timeout: 15_000 });
  const box = await footer.boundingBox();
  // Within one footer height of the bottom edge, not hugging the content above it.
  expect(box!.y + box!.height).toBeGreaterThan(900 - 80);
});
