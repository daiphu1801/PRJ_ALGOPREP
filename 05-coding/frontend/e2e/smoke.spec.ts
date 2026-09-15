import { expect, test } from "@playwright/test";

/**
 * Minimal smoke test for the base shell. NOT the backbone E2E flow — that flow
 * (login -> open problem -> run -> submit -> realtime result -> Accepted -> AI flow) is specified in
 * 01-rd/system/environment.md section 3.B, and can only be stood up once a real backend exists.
 */
test("root route redirects to the login screen", async ({ page }) => {
  await page.goto("/");
  await expect(page).toHaveURL(/\/login$/);
});

test("student area renders the AppShell", async ({ page }) => {
  await page.goto("/problems");
  await expect(page.getByRole("banner")).toContainText("AlgoPrep");
});

test("auth screen renders and the signup/login mode switch is reachable (PROTOTYPE, views/auth)", async ({ page }) => {
  await page.goto("/login");
  await expect(page.getByRole("heading", { level: 1 })).toHaveText("Đăng nhập");

  // Primary action: switch mode via the aside panel button (02-bd/screens/shared/auth.md section 2).
  await page.getByRole("button", { name: "Chưa có tài khoản? Đăng ký" }).first().click();
  await expect(page.getByRole("heading", { level: 1 })).toHaveText("Đăng ký");
});

test("admin overview renders the Admin shell with sidebar nav (PROTOTYPE, views/admin-overview)", async ({ page }) => {
  await page.goto("/admin/overview");
  await expect(page.getByRole("heading", { level: 1 })).toHaveText("Tổng quan");

  // Primary action: sidebar nav reaches an existing real route, not a 404.
  const nav = page.getByRole("navigation", { name: "Điều hướng khu quản trị" });
  await expect(nav).toBeVisible();
  await nav.getByRole("link", { name: "Quản lý bài tập" }).click();
  await expect(page).toHaveURL(/\/admin\/problems$/);
});

test("default locale is Vietnamese and the cookie can switch it to English", async ({ page, context }) => {
  await page.goto("/problems");
  await expect(page.getByRole("heading", { level: 1 })).toHaveText("Danh sách bài toán");

  // Switch locale via cookie, NOT the URL — verifies the non-prefix i18n mechanism that was locked in.
  await context.addCookies([
    { name: "NEXT_LOCALE", value: "en", url: "http://localhost:3000" },
  ]);
  await page.reload();
  await expect(page.getByRole("heading", { level: 1 })).toHaveText("Problems");
});
