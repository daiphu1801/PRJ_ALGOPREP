import { expect, test } from "@playwright/test";

/**
 * PROTOTYPE screen check for admin_ai_config. The scope guard here is the "Chạy đối chiếu" button:
 * PROTOTYPE_DEBT 2.8 keeps the UI but defers the backend, so it must be visibly inactive rather
 * than wired to something that does not exist.
 */
test("the comparison run is present but inert, per the deferred-backend decision", async ({ page }) => {
  await page.goto("/admin/ai-config");

  await expect(page.getByRole("heading", { level: 1 })).toHaveText("Cấu hình trợ lý AI");

  const run = page.getByRole("button", { name: "Chạy đối chiếu" });
  await expect(run).toBeVisible();
  await expect(run).toBeDisabled();
});

test("rubric weights add to 100 and warn when they stop doing so", async ({ page }) => {
  await page.goto("/admin/ai-config");

  await expect(page.getByText("Tổng 100%")).toBeVisible();
  await expect(page.getByText("Tổng trọng số chưa bằng 100%")).toHaveCount(0);

  await page.getByRole("button", { name: "Tăng trọng số Tính đúng đắn" }).click();

  await expect(page.getByText("Tổng 105%")).toBeVisible();
  await expect(page.getByText("Tổng trọng số chưa bằng 100%")).toBeVisible();
});

test("version log opens as a dialog with entries", async ({ page }) => {
  await page.goto("/admin/ai-config");

  await page.getByRole("button", { name: "Nhật ký phiên bản" }).click();
  const dialog = page.getByRole("dialog");
  await expect(dialog).toBeVisible();
  await expect(dialog.getByRole("listitem")).toHaveCount(5);

  await dialog.getByRole("button", { name: "Đóng" }).click();
  await expect(page.getByRole("dialog")).toHaveCount(0);
});

test("answer guards are switches, not decoration", async ({ page }) => {
  await page.goto("/admin/ai-config");

  const guard = page.getByRole("switch", { name: "Không đưa lời giải đầy đủ" });
  await expect(guard).toHaveAttribute("aria-checked", "true");
  await guard.click();
  await expect(guard).toHaveAttribute("aria-checked", "false");
});

for (const theme of ["light", "dark"] as const) {
  test(`capture ${theme} theme for mockup comparison`, async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 1020 });
    await page.addInitScript((value) => localStorage.setItem("theme", value), theme);
    await page.goto("/admin/ai-config");
    await expect(page.getByRole("heading", { level: 1 })).toBeVisible();
    await page.screenshot({ path: `e2e/__screenshots__/ai-config-${theme}.png`, caret: "initial" });
  });
}
