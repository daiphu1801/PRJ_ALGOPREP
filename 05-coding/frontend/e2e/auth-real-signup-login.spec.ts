// F1 auth against the REAL backend: sign-up with e-mail OTP, wrong OTP, login errors, lock after
// 5 failures, session survives reload. Needs the stack in e2e/README.md; skips itself otherwise.
import { expect, test, type Page } from "@playwright/test";
import {
  APP_URL,
  PASSWORD,
  createUser,
  readOtp,
  requireRealStack,
  vi,
  submitLogin,
  uniqueEmail,
} from "./support/real-stack";

requireRealStack();

async function signOut(page: Page) {
  await page.getByRole("button", { name: vi.nav.accountMenu }).click();
  await page.getByRole("link", { name: vi.nav.logout }).click();
  await expect(page).toHaveURL(/\/login/);
}

test("register, read the OTP from Mailpit, verify, login, logout", async ({ page, request }) => {
  const email = uniqueEmail("signup");
  await page.goto(`${APP_URL}/register`);
  await page.getByLabel(vi.auth.displayNameLabel).fill("E2E Signup");
  await page.getByLabel(vi.auth.passwordLabel, { exact: true }).fill(PASSWORD);
  await page.getByLabel(vi.auth.emailLabel).fill(email);
  await page.getByLabel(vi.auth.termsLabel).check();
  await page.getByRole("button", { name: vi.auth.signupSubmit, exact: true }).click();

  // Verify-email step is shown with the target address.
  await expect(page.getByText(`Mã xác thực đã được gửi tới ${email}.`)).toBeVisible();
  await page.getByLabel(vi.auth.otpLabel).fill(await readOtp(request, email));
  await page.getByRole("button", { name: vi.auth.verifyEmailSubmit }).click();
  await expect(page.getByText(vi.auth.notice.emailVerified)).toBeVisible();

  // Login happens on the login mode of the same screen.
  await page.goto(`${APP_URL}/login`);
  await submitLogin(page, email, PASSWORD);
  await expect(page).toHaveURL(/\/dashboard/, { timeout: 15_000 });
  await signOut(page);
});

test("wrong OTP shows the error and the remaining attempts", async ({ page }) => {
  const email = uniqueEmail("badotp");
  await page.goto(`${APP_URL}/register`);
  await page.getByLabel(vi.auth.displayNameLabel).fill("E2E Bad Otp");
  await page.getByLabel(vi.auth.passwordLabel, { exact: true }).fill(PASSWORD);
  await page.getByLabel(vi.auth.emailLabel).fill(email);
  await page.getByLabel(vi.auth.termsLabel).check();
  await page.getByRole("button", { name: vi.auth.signupSubmit, exact: true }).click();

  await page.getByLabel(vi.auth.otpLabel).fill("000000");
  await page.getByRole("button", { name: vi.auth.verifyEmailSubmit }).click();
  await expect(page.getByText(vi.auth.errors.otpInvalid)).toBeVisible();
  await expect(page.getByText("Còn 4 lần thử")).toBeVisible();
});

test("wrong password shows the credentials message, 5 failures lock the login", async ({
  page,
  request,
}) => {
  const user = await createUser(request, { tag: "lock" });
  await page.goto(`${APP_URL}/login`);
  for (let attempt = 1; attempt <= 5; attempt += 1) {
    await submitLogin(page, user.email, "Sai-mat-khau-1");
    await expect(page.getByText(vi.auth.errors.credentialsInvalid).first()).toBeVisible();
  }
  // 6th try is refused even with the right password.
  await submitLogin(page, user.email, user.password);
  await expect(page.getByText(vi.apiErrors["IDT-103"]).first()).toBeVisible();
  await expect(page).toHaveURL(/\/login/);
});

test("session survives a page reload", async ({ page, request }) => {
  const user = await createUser(request, { tag: "reload" });
  await page.goto(`${APP_URL}/login`);
  await submitLogin(page, user.email, user.password);
  await expect(page).toHaveURL(/\/dashboard/, { timeout: 15_000 });

  await page.reload();
  // Refresh cookie restores the session: still on the student area, not bounced to /login.
  await expect(page).toHaveURL(/\/dashboard/);
  await expect(page.getByRole("button", { name: vi.nav.accountMenu })).toBeVisible();
});
