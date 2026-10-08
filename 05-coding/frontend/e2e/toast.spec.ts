import { expect, test, type Page } from "@playwright/test";

/**
 * DEC-2026-1003-toast-feedback-channel: one shared toast, top right, auto-dismissing, with an X.
 * The same view is mounted in the Admin and Instructor areas, so both are covered by saving a
 * problem; the Student area is covered through its own "un-save" action.
 */
async function expectBottomRightCard(page: Page, text: RegExp) {
  const region = page.getByRole("region", { name: "Thông báo" });
  const card = region.getByRole("status").filter({ hasText: text });
  await expect(card).toBeVisible();

  const box = await card.boundingBox();
  const viewport = page.viewportSize()!;
  expect(box!.x + box!.width).toBeGreaterThan(viewport.width - 40);
  expect(box!.y).toBeLessThan(viewport.height / 2);
  return card;
}

for (const area of ["admin", "instructor"] as const) {
  test(`saving a problem toasts at the top right in the ${area} area, and X closes it`, async ({
    page,
  }) => {
    await page.goto(`/${area}/problems/1268/edit`);

    await page.getByLabel("Tiêu đề").fill("Minimum Window Substring (đã sửa)");
    await page.getByRole("button", { name: "Lưu", exact: true }).click();

    const card = await expectBottomRightCard(page, /đã lưu/i);
    await page.screenshot({ path: `test-results/toast-${area}.png` });

    await card.getByRole("button", { name: "Đóng thông báo" }).click();
    await expect(card).toHaveCount(0);
  });
}

test("un-saving a problem toasts in the student area, and the toast dismisses itself", async ({
  page,
}) => {
  await page.goto("/saved");

  await page.getByRole("button", { name: "Bỏ lưu" }).first().click();

  const card = await expectBottomRightCard(page, /bỏ lưu/i);
  await page.screenshot({ path: "test-results/toast-student.png" });

  // Success toasts last 4 s; allow for timer slack.
  await expect(card).toHaveCount(0, { timeout: 8000 });
});
