// F1 role guards (RoleGate) and the instructor portal against the REAL backend.
// Needs the stack in e2e/README.md; skips itself otherwise.
import { expect, test } from "@playwright/test";
import { APP_URL, createUser, requireRealStack, submitLogin, vi } from "./support/real-stack";

requireRealStack();

test("signed-out visitor is sent to the login page with a redirect", async ({ page }) => {
  await page.goto(`${APP_URL}/admin/overview`);
  await expect(page).toHaveURL(/\/admin\/login\?redirect=%2Fadmin%2Foverview/);
  await page.goto(`${APP_URL}/instructor/overview`);
  await expect(page).toHaveURL(/\/instructor\/login\?redirect=%2Finstructor%2Foverview/);
});

test("student cannot enter the instructor or admin areas", async ({ page, request }) => {
  const user = await createUser(request, { tag: "guard-student" });
  await page.goto(`${APP_URL}/login`);
  await submitLogin(page, user.email, user.password);
  await expect(page).toHaveURL(/\/dashboard/, { timeout: 15_000 });

  await page.goto(`${APP_URL}/instructor/overview`);
  await expect(page).toHaveURL(/\/dashboard/);
  await page.goto(`${APP_URL}/admin/overview`);
  await expect(page).toHaveURL(/\/dashboard/);
});

test("instructor is kept out of the student and admin areas", async ({ page, request }) => {
  const user = await createUser(request, { tag: "guard-instr", role: "INSTRUCTOR" });
  await page.goto(`${APP_URL}/instructor/login`);
  await submitLogin(page, user.email, user.password);
  await expect(page).toHaveURL(/\/instructor\/overview/, { timeout: 15_000 });

  await page.goto(`${APP_URL}/dashboard`);
  await expect(page).toHaveURL(/\/instructor\/overview/);
  await page.goto(`${APP_URL}/admin/overview`);
  await expect(page).toHaveURL(/\/instructor\/overview/);
});

test("admin is kept out of the student and instructor areas", async ({ page, request }) => {
  const user = await createUser(request, { tag: "guard-admin", role: "ADMIN" });
  await page.goto(`${APP_URL}/admin/login`);
  await submitLogin(page, user.email, user.password);
  await expect(page).toHaveURL(/\/admin\/overview/, { timeout: 15_000 });

  await page.goto(`${APP_URL}/dashboard`);
  await expect(page).toHaveURL(/\/admin\/overview/);
  await page.goto(`${APP_URL}/instructor/overview`);
  await expect(page).toHaveURL(/\/admin\/overview/);
});

test("instructor portal accepts an instructor and refuses a student account", async ({
  page,
  request,
}) => {
  const student = await createUser(request, { tag: "portal-student" });
  await page.goto(`${APP_URL}/instructor/login`);
  await submitLogin(page, student.email, student.password);
  await expect(page.getByText(vi.auth.errors.notInstructorAccount).first()).toBeVisible();
  await expect(page).toHaveURL(/\/instructor\/login/);

  const instructor = await createUser(request, { tag: "portal-instr", role: "INSTRUCTOR" });
  await page.goto(`${APP_URL}/instructor/login`);
  await submitLogin(page, instructor.email, instructor.password);
  await expect(page).toHaveURL(/\/instructor\/overview/, { timeout: 15_000 });
});
