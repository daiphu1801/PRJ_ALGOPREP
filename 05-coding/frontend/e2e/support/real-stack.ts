// Helpers for the specs that run against the REAL backend (F1 auth). See e2e/README.md.
//
// Everything is reached the way the browser reaches it: API calls go through the Next dev server
// proxy (`/api/v1`), the OTP mails are read from Mailpit's HTTP API, and roles / the forced
// password flag are set with SQL inside the Postgres container (`docker exec ... psql`).
import { execFileSync } from "node:child_process";
import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import type ViMessages from "../../messages/vi.json";
import {
  expect,
  test,
  type APIRequestContext,
  type Page,
} from "@playwright/test";

export const APP_URL = process.env.E2E_BASE_URL ?? "http://localhost:3000";
export const MAILPIT_URL =
  process.env.E2E_MAILPIT_URL ?? "http://localhost:8025/api/v1";
const PG_CONTAINER = process.env.E2E_PG_CONTAINER ?? "algoprep-postgres";
const PG_USER = process.env.E2E_PG_USER ?? "algoprep";
const PG_DB = process.env.E2E_PG_DB ?? "algoprep";

/** Vietnamese UI copy, so the specs assert the real strings and not a second copy of them. */
export const vi = JSON.parse(
  readFileSync(
    fileURLToPath(new URL("../../messages/vi.json", import.meta.url)),
    "utf8",
  ),
) as typeof ViMessages;

export const PASSWORD = "Matkhau123";
export type BaseRole = "STUDENT" | "INSTRUCTOR" | "ADMIN";

/** Runs one SQL statement in the Postgres container and returns stdout (-At: bare values). */
export function sql(statement: string): string {
  return execFileSync(
    "docker",
    [
      "exec",
      PG_CONTAINER,
      "psql",
      "-U",
      PG_USER,
      "-d",
      PG_DB,
      "-At",
      "-c",
      statement,
    ],
    { encoding: "utf8" },
  ).trim();
}

/** Returns null when the real stack is usable, otherwise a one-line reason (used as skip text). */
async function probe(page: Page): Promise<string | null> {
  try {
    const res = await page.request.post(
      `${APP_URL}/api/v1/identity/auth/login`,
      {
        data: {},
        timeout: 5_000,
      },
    );
    const body = (await res.json().catch(() => null)) as {
      errorCode?: string;
    } | null;
    // An empty login is a failed attempt for the ("", ip) key, so after 5 probes it answers
    // IDT-103 (rate limited). Both codes prove the real identity backend is behind the proxy.
    if (body?.errorCode !== "IDT-102" && body?.errorCode !== "IDT-103") {
      return "backend not reachable through the frontend proxy (/api/v1/identity/auth/login)";
    }
  } catch {
    return "frontend or backend not reachable on " + APP_URL;
  }
  try {
    const res = await page.request.get(`${MAILPIT_URL}/messages?limit=1`, {
      timeout: 5_000,
    });
    if (!res.ok()) return "Mailpit API answered " + res.status();
  } catch {
    return "Mailpit not reachable on " + MAILPIT_URL;
  }
  try {
    sql("select 1");
  } catch {
    return `Postgres container "${PG_CONTAINER}" not reachable through docker exec`;
  }
  await page.goto(`${APP_URL}/login`);
  const mockBanner = page.getByText("Đăng nhập nhanh (dữ liệu giả)");
  // `isVisible` does not wait (its timeout is ignored), so wait for the panel to render explicitly.
  const inMockMode = await mockBanner
    .waitFor({ state: "visible", timeout: 5_000 })
    .then(() => true)
    .catch(() => false);
  if (inMockMode) {
    return "frontend runs in mock mode (start it with NEXT_PUBLIC_MOCK_DATA=false)";
  }
  return null;
}

/**
 * Call once at the top of a spec file. Skips the whole file, with a reason, when the real stack
 * is not up, so `pnpm e2e` stays green on a machine without it.
 */
export function requireRealStack(): void {
  let probed = false;
  let reason: string | null = null;
  test.beforeEach(async ({ page }) => {
    if (!probed) {
      reason = await probe(page);
      probed = true;
    }
    test.skip(reason !== null, `real stack unavailable: ${reason}`);
  });
}

export type TestUser = { email: string; password: string; displayName: string };

/** Distinctive prefix so the accounts are easy to find (they cannot be deleted: FK to tokens). */
export function uniqueEmail(tag: string): string {
  return `e2e-auth-${tag}-${Date.now()}-${Math.floor(Math.random() * 1000)}@test.vn`;
}

/** Latest 6 digit code mailed to `to`. Clears older mail first so only a fresh code can match. */
export async function clearMail(
  request: APIRequestContext,
  to: string,
): Promise<void> {
  const list = await request.get(
    `${MAILPIT_URL}/search?query=${encodeURIComponent("to:" + to)}`,
  );
  const ids = (
    (await list.json()) as { messages: { ID: string }[] }
  ).messages.map((m) => m.ID);
  if (ids.length > 0)
    await request.delete(`${MAILPIT_URL}/messages`, { data: { IDs: ids } });
}

export async function readOtp(
  request: APIRequestContext,
  to: string,
): Promise<string> {
  let code: string | undefined;
  await expect
    .poll(
      async () => {
        const res = await request.get(
          `${MAILPIT_URL}/search?query=${encodeURIComponent("to:" + to)}&limit=1`,
        );
        const hit = ((await res.json()) as { messages: { ID: string }[] })
          .messages[0];
        if (!hit) return null;
        const msg = (await (
          await request.get(`${MAILPIT_URL}/message/${hit.ID}`)
        ).json()) as {
          Text?: string;
        };
        code = /\b(\d{6})\b/.exec(msg.Text ?? "")?.[1];
        return code ?? null;
      },
      { message: `no 6 digit code mailed to ${to}`, timeout: 15_000 },
    )
    .not.toBeNull();
  return code as string;
}

/**
 * Creates an ACTIVE account through the public API (register, then verify with the mailed OTP),
 * then optionally promotes it with SQL (`identity.users.role_id` -> role by code) and/or sets
 * `must_change_password`. Returns the credentials.
 */
export async function createUser(
  request: APIRequestContext,
  opts: { tag: string; role?: BaseRole; mustChangePassword?: boolean },
): Promise<TestUser> {
  const user: TestUser = {
    email: uniqueEmail(opts.tag),
    password: PASSWORD,
    displayName: `E2E ${opts.tag}`,
  };
  const reg = await request.post(`${APP_URL}/api/v1/identity/auth/register`, {
    data: {
      email: user.email,
      password: user.password,
      displayName: user.displayName,
    },
  });
  expect(reg.status(), "register").toBe(201);
  const otp = await readOtp(request, user.email);
  const ver = await request.post(
    `${APP_URL}/api/v1/identity/auth/email/verify`,
    {
      data: { email: user.email, otp },
    },
  );
  expect(ver.ok(), "verify email").toBe(true);
  if (opts.role && opts.role !== "STUDENT") {
    sql(
      `update identity.users set role_id = (select id from identity.roles where code = '${opts.role}') where email = '${user.email}'`,
    );
  }
  if (opts.mustChangePassword) {
    sql(
      `update identity.users set must_change_password = true where email = '${user.email}'`,
    );
  }
  return user;
}

/** Fills the login form that is currently on screen and submits it. */
export async function submitLogin(
  page: Page,
  email: string,
  password: string,
): Promise<void> {
  await page.getByLabel("Email").fill(email);
  await page.getByLabel("Mật khẩu", { exact: true }).fill(password);
  await page.getByRole("button", { name: "Đăng nhập", exact: true }).click();
}

/** Full UI login on the student/public page; resolves once the role home is shown. */
export async function loginViaUi(
  page: Page,
  user: TestUser,
  path = "/login",
): Promise<void> {
  await page.goto(`${APP_URL}${path}`);
  await submitLogin(page, user.email, user.password);
}
