import { expect, test } from "@playwright/test";

/**
 * Carried-over debt from the 2026-09-16 session: ThemeLangSwitcher's padding was tuned over three
 * rounds against screenshots, but every measurement was taken in the EXPANDED sidebar (224px) and on
 * the floating /admin/login variant. The collapsed 72px rail was never re-measured after the last
 * round, and that round is exactly what caused the "dính border" bug in the expanded state — the
 * width budget is what breaks, so the narrower rail is the likelier failure.
 *
 * Budget: rail 72px with p-3 (12px) each side leaves 48px of content width.
 */
test("collapsed Admin rail keeps both switcher groups inside the content box", async ({ page }) => {
  await page.goto("/admin/overview");

  const nav = page.getByRole("navigation", { name: "Điều hướng khu quản trị" });
  await expect(nav).toBeVisible();
  await nav.getByRole("button", { name: "Thu gọn / mở rộng" }).click();

  await expect.poll(async () => (await nav.boundingBox())?.width).toBe(72);
  const railBox = await nav.boundingBox();
  expect(railBox).not.toBeNull();
  if (!railBox) return;

  const themeGroup = nav.getByRole("group", { name: "Chọn giao diện sáng/tối" });
  const langGroup = nav.getByRole("group", { name: "Chọn ngôn ngữ giao diện" });

  // Locate them explicitly rather than through a fallback: a locator that silently matches nothing
  // would let this test pass while measuring an element that is not there.
  await expect(themeGroup).toBeVisible();
  await expect(langGroup).toBeVisible();

  const PADDING = 12;
  const contentLeft = railBox.x + PADDING;
  const contentRight = railBox.x + railBox.width - PADDING;

  for (const [name, group] of [
    ["theme", themeGroup],
    ["language", langGroup],
  ] as const) {
    const box = await group.boundingBox();
    expect(box, `${name} group has no box`).not.toBeNull();
    if (!box) continue;

    // overflow-x-hidden on the sidebar means an overflowing child is CLIPPED, not scrollable — it
    // would vanish rather than degrade, so the rail edge is a hard bound.
    expect(box.x, `${name} group overflows the rail's left edge`).toBeGreaterThanOrEqual(railBox.x);
    expect(
      box.x + box.width,
      `${name} group overflows the rail's right edge`,
    ).toBeLessThanOrEqual(railBox.x + railBox.width);

    // Softer bound: staying inside the 12px padding box is the intended look, and the 2026-09-16
    // regression was exactly a group eating into this gutter.
    expect(box.x, `${name} group eats into the left gutter`).toBeGreaterThanOrEqual(contentLeft);
    expect(
      box.x + box.width,
      `${name} group eats into the right gutter`,
    ).toBeLessThanOrEqual(contentRight);
  }

  // Element screenshots rather than a page clip: the switcher sits at the very bottom of a
  // full-height rail, so a viewport-relative clip there gets clamped to a sliver.
  await themeGroup.screenshot({ path: "e2e/__screenshots__/admin-rail-theme-group.png" });
  await langGroup.screenshot({ path: "e2e/__screenshots__/admin-rail-lang-group.png" });
});
