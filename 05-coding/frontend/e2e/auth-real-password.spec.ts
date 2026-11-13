// F1 password flows against the REAL backend: forgot-password in three steps (OTP from Mailpit)
// and the non-dismissible forced password dialog (users.must_change_password = true, set by SQL).
// Needs the stack in e2e/README.md; skips itself otherwise.
import { expect, test } from "@playwright/test";
import {
  APP_URL,
  clearMail,
  createUser,
  readOtp,
  requireRealStack,
  vi,
  submitLogin,
} from "./support/real-stack";

requireRealStack();

test("forgot password: e-mail, OTP from Mailpit, new password, login with it", async ({
  page,
  request,
}) => {
  const user = await createUser(request, { tag: "forgot" });
  const newPassword = "MatkhauMoi456";

  await page.goto(`${APP_URL}/login`);
  await clearMail(request, user.email);
  await page.getByRole("button", { name: vi.auth.forgotPasswordLink }).click();
  await page.getByLabel(vi.auth.emailLabel).fill(user.email);
  await page.getByRole("button", { name: vi.auth.forgotEmailSubmit }).click();

  await expect(page.getByText(`Mã xác thực đã được gửi tới ${user.email}.`)).toBeVisible();
  await page.getByLabel(vi.auth.otpLabel).fill(await readOtp(request, user.email));
  await page.getByRole("button", { name: vi.auth.forgotOtpSubmit }).click();

  await page.getByLabel(vi.auth.newPasswordLabel, { exact: true }).fill(newPassword);
  await page.getByLabel(vi.auth.confirmPasswordLabel).fill(newPassword);
  await page.getByRole("button", { name: vi.auth.forgotResetSubmit }).click();
  await expect(page.getByText(vi.auth.notice.passwordReset)).toBeVisible();

  // The old password no longer works, the new one does.
  await submitLogin(page, user.email, user.password);
  await expect(page.getByText(vi.auth.errors.credentialsInvalid).first()).toBeVisible();
  await submitLogin(page, user.email, newPassword);
  await expect(page).toHaveURL(/\/dashboard/, { timeout: 15_000 });
});

test("account with must_change_password sees the forced dialog that cannot be dismissed", async ({
  page,
  request,
}) => {
  const user = await createUser(request, { tag: "forced", mustChangePassword: true });
  await page.goto(`${APP_URL}/login`);
  await submitLogin(page, user.email, user.password);
  await expect(page).toHaveURL(/\/dashboard/, { timeout: 15_000 });

  const dialog = page.getByRole("dialog");
  await expect(dialog.getByRole("heading", { name: vi.profile.passwordDialog.titleForced })).toBeVisible();
  await expect(dialog.getByText(vi.profile.passwordDialog.forcedHint)).toBeVisible();

  // Escape does not close it; the only exits are saving a new password or signing out.
  await page.keyboard.press("Escape");
  await expect(dialog).toBeVisible();
  await expect(dialog.getByRole("button", { name: vi.profile.passwordDialog.signOut })).toBeVisible();

  // Setting a new password lifts the block and the flag is cleared server-side.
  await dialog.getByLabel(vi.profile.passwordDialog.currentPassword).fill(user.password);
  await dialog.getByLabel(vi.profile.passwordDialog.newPassword, { exact: true }).fill("MatkhauMoi789");
  await dialog.getByLabel(vi.profile.passwordDialog.confirmPassword).fill("MatkhauMoi789");
  await dialog.getByRole("button", { name: vi.profile.passwordDialog.confirm }).click();
  await expect(dialog).toBeHidden();
});
