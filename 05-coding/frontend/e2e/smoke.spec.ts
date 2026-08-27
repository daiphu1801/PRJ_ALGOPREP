import { expect, test } from "@playwright/test";

/**
 * Smoke test tối thiểu của khung base. KHÔNG phải luồng E2E xương sống — luồng đó
 * (đăng nhập → mở bài toán → chạy thử → nộp → kết quả realtime → Accepted → luồng AI) đặc tả ở
 * 01-rd/system/environment.md mục 3.B, chỉ dựng được khi có backend thật.
 */
test("route gốc điều hướng về màn đăng nhập", async ({ page }) => {
  await page.goto("/");
  await expect(page).toHaveURL(/\/login$/);
});

test("khu vực người học render được AppShell", async ({ page }) => {
  await page.goto("/problems");
  await expect(page.getByRole("banner")).toContainText("AlgoPrep");
});

test("locale mặc định là tiếng Việt và cookie đổi được sang tiếng Anh", async ({ page, context }) => {
  await page.goto("/problems");
  await expect(page.getByRole("heading", { level: 1 })).toHaveText("Danh sách bài toán");

  // Đổi ngôn ngữ bằng cookie, KHÔNG đổi URL — kiểm chứng cơ chế i18n non-prefix đã chốt.
  await context.addCookies([
    { name: "NEXT_LOCALE", value: "en", url: "http://localhost:3000" },
  ]);
  await page.reload();
  await expect(page.getByRole("heading", { level: 1 })).toHaveText("Problems");
});
