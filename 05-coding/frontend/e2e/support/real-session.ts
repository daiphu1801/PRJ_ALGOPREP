// Small helpers shared by the new real-backend specs (admin and student screens). Everything goes
// through the real login form, the same way `admin-permissions-real.spec.ts` does it.
import {
  expect,
  type APIRequestContext,
  type Page,
  type Route,
} from "@playwright/test";
import {
  APP_URL,
  createUser,
  sql,
  submitLogin,
  type TestUser,
} from "./real-stack";

/** Creates an ADMIN account (STUDENT + SQL role change) and signs it in through /admin/login. */
export async function loginAsAdmin(
  page: Page,
  request: APIRequestContext,
  tag: string,
): Promise<TestUser> {
  const admin = await createUser(request, { tag, role: "ADMIN" });
  await page.goto(`${APP_URL}/admin/login`);
  await submitLogin(page, admin.email, admin.password);
  await expect(page).toHaveURL(/\/admin\/overview/, { timeout: 15_000 });
  return admin;
}

/** Creates a STUDENT account and signs it in through /login (lands on the dashboard). */
export async function loginAsStudent(
  page: Page,
  request: APIRequestContext,
  tag: string,
): Promise<TestUser> {
  const student = await createUser(request, { tag });
  await page.goto(`${APP_URL}/login`);
  await submitLogin(page, student.email, student.password);
  await expect(page).toHaveURL(/\/dashboard/, { timeout: 15_000 });
  return student;
}

export function userId(email: string): string {
  return sql(`select id from identity.users where email='${email}'`);
}

/** Answers one API path in the browser with a fixed JSON body (used only where the server cannot be forced). */
export async function fulfillJson(
  page: Page,
  urlPart: string,
  body: unknown,
): Promise<void> {
  await page.route(
    (url) => url.pathname.endsWith(urlPart),
    (route: Route) => route.fulfill({ json: body }),
  );
}
