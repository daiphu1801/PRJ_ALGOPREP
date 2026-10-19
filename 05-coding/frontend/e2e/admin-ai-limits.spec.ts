import { expect, test } from "@playwright/test";

/**
 * PROTOTYPE screen check for admin_ai_limits (BD ADM0303). Every control writes immediately, so the
 * page has no publish button; the cap, the unlock and the other settings are checked here.
 */
test("page has no publish step and links to the usage screen", async ({
  page,
}) => {
  await page.goto("/admin/ai-limits");

  await expect(page.getByRole("heading", { level: 1 })).toHaveText(
    "Hạn mức và chi phí AI",
  );
  await expect(
    page.getByRole("button", { name: "Lưu và phát hành" }),
  ).toHaveCount(0);
  await expect(
    page.getByRole("link", { name: "Xem tiêu thụ token" }),
  ).toHaveAttribute("href", "/admin/ai-usage");
});

test("a cap at or below usage locks AI, and unlocking needs a higher cap", async ({
  page,
}) => {
  await page.goto("/admin/ai-limits");

  await expect(page.getByText("Đang mở")).toBeVisible();
  await expect(page.getByRole("button", { name: "Mở khoá" })).toHaveCount(0);

  const cap = page.getByLabel("Hạn mức token tháng");
  await cap.fill("30000000");
  await page.getByRole("button", { name: "Lưu hạn mức" }).click();
  await expect(page.getByText("Đã khoá tự động")).toBeVisible();

  // Still under usage: the unlock answers with a reason instead of opening.
  await page.getByRole("button", { name: "Mở khoá" }).click({ force: true });
  await expect(page.getByText("Tăng hạn mức trước khi mở khoá.")).toBeVisible();

  await cap.fill("60000000");
  await page.getByRole("button", { name: "Lưu hạn mức" }).click();
  await page.getByRole("button", { name: "Mở khoá" }).click();
  await page
    .getByRole("dialog")
    .getByRole("button", { name: "Mở khoá" })
    .click();
  await expect(page.getByText("Đang mở")).toBeVisible();
});

test("a cap that is not a positive whole number is refused with a toast", async ({
  page,
}) => {
  await page.goto("/admin/ai-limits");

  await page.getByLabel("Hạn mức token tháng").fill("0");
  await page.getByRole("button", { name: "Lưu hạn mức" }).click();
  await expect(
    page.getByText("Hạn mức phải là số nguyên lớn hơn 0."),
  ).toBeVisible();
});

test("rate limits are per hour; they can be typed as well as stepped", async ({
  page,
}) => {
  await page.goto("/admin/ai-limits");

  const review = page.getByRole("spinbutton", { name: "Phân tích mỗi giờ" });
  await expect(review).toHaveValue("10");
  // The unit stays outside the box: only the number is typed.
  await expect(page.getByText("lượt/giờ").first()).toBeVisible();
  await expect(
    page.getByRole("spinbutton", { name: "Phỏng vấn mỗi giờ" }),
  ).toHaveValue("3");

  await page.getByRole("button", { name: "Tăng Phân tích mỗi giờ" }).click();
  await expect(review).toHaveValue("11");

  // Typing: the bare number shows while focused, Enter commits, a bound is enforced (max 999).
  await review.fill("40");
  await review.press("Enter");
  await expect(review).toHaveValue("40");
  await review.fill("5000");
  await review.press("Enter");
  await expect(review).toHaveValue("999");
  // Text that is not a whole number is dropped and the old value stays.
  await review.fill("abc");
  await review.press("Enter");
  await expect(review).toHaveValue("999");
  // Escape abandons what was typed.
  await review.fill("7");
  await review.press("Escape");
  await expect(review).toHaveValue("999");
});

test("the warning threshold and the testcase cap can be typed too", async ({
  page,
}) => {
  await page.goto("/admin/ai-limits");

  const warn = page.getByRole("spinbutton", {
    name: "Ngưỡng cảnh báo ngân sách",
  });
  await warn.fill("85");
  await warn.press("Enter");
  await expect(warn).toHaveValue("85");

  const cap = page.getByRole("spinbutton", {
    name: "Số lần sinh tối đa mỗi bài",
  });
  await cap.fill("4");
  await cap.press("Enter");
  await expect(cap).toHaveValue("4");
});

test("budget warning, price list and testcase cap are all here", async ({
  page,
}) => {
  await page.goto("/admin/ai-limits");

  await expect(page.getByText("Ngân sách và đơn giá")).toBeVisible();
  await expect(page.getByText("Sinh testcase tự động")).toBeVisible();
  await page.getByRole("button", { name: "Quản lý" }).click();
  await expect(page.getByRole("dialog")).toBeVisible();
});

for (const theme of ["light", "dark"] as const) {
  test(`capture ${theme} theme for mockup comparison`, async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 1020 });
    await page.addInitScript(
      (value) => localStorage.setItem("theme", value),
      theme,
    );
    await page.goto("/admin/ai-limits");
    await expect(page.getByRole("heading", { level: 1 })).toBeVisible();
    await page.screenshot({
      path: `e2e/__screenshots__/ai-limits-${theme}.png`,
      caret: "initial",
    });
  });
}
