// ADM0302 (AI token usage) against the REAL backend: 03-dd/api/F5_ai-review.md #18 and #27 to #31.
// Replaces the mock-mode `admin-ai-usage.spec.ts`. Needs the stack in e2e/README.md; skips itself
// otherwise (and in mock mode).
import { expect, test } from "@playwright/test";
import { APP_URL, requireRealStack, vi } from "./support/real-stack";
import { fulfillJson, loginAsAdmin } from "./support/real-session";

requireRealStack();

const m = vi.adminAiUsage;
const FEATURES = [
  m.feature.review,
  m.feature.interview,
  m.feature.testcase,
  m.feature.practice,
];

test("only the in-scope AI features are shown and the tiered-hint feature is gone", async ({
  page,
  request,
}) => {
  await loginAsAdmin(page, request, "aiu-scope");
  await page.goto(`${APP_URL}/admin/ai-usage`);
  await expect(page.getByRole("heading", { level: 1 })).toHaveText(m.title);

  // The budget panel lists every feature, whatever the month's data (server #18 feature shares).
  for (const feature of FEATURES) {
    await expect(page.getByText(feature).first()).toBeVisible({
      timeout: 15_000,
    });
  }
  // DEC-2026-0831-remove-tiered-hints-ai-config: "gợi ý" must not appear anywhere in the content.
  await expect(page.getByRole("main").getByText(/gợi ý/i)).toHaveCount(0);
});

test("the daily chart follows the range switch and asks the server for that many days", async ({
  page,
  request,
}) => {
  await loginAsAdmin(page, request, "aiu-range");
  await page.goto(`${APP_URL}/admin/ai-usage`);
  await expect(
    page.getByText(m.dailyChartSubtitle.replace("{days}", "14")),
  ).toBeVisible({ timeout: 15_000 });

  const asked = page.waitForRequest(
    (r) =>
      r.url().includes("/ai/admin/usage/daily") &&
      r.url().includes("rangeDays=7"),
  );
  await page
    .getByRole("group", { name: m.rangeLabel })
    .getByRole("button", { name: m.range["7d"] })
    .click();
  await asked;
  await expect(
    page.getByText(m.dailyChartSubtitle.replace("{days}", "7")),
  ).toBeVisible();
});

test("the anomaly notice warns for review and never claims the account was locked", async ({
  page,
  request,
}) => {
  await loginAsAdmin(page, request, "aiu-anomaly");
  // The server only raises an anomaly after a real usage spike, which this run cannot force, so the
  // alert list is answered in the browser. The wording under test is the screen's own.
  await fulfillJson(page, "/ai/admin/usage/alerts", {
    items: [
      {
        alertType: "ANOMALY",
        alertId: "00000000-0000-4000-8000-0000000000e2",
        callCount: 120,
        ratio: 6.5,
        displayName: "E2E Hoc vien",
      },
    ],
  });
  await page.goto(`${APP_URL}/admin/ai-usage`);

  await expect(page.getByText(m.alert.anomalousAccount.title)).toBeVisible({
    timeout: 15_000,
  });
  await expect(page.getByText(/hệ thống không tự khoá/)).toBeVisible();
  await expect(page.getByText("E2E Hoc vien gọi 120 lượt")).toBeVisible();
  // Nothing in the notice says the account is already locked, and it offers no lock button.
  await expect(page.getByText(/tài khoản (đã )?bị (khoá|khóa)/i)).toHaveCount(
    0,
  );
  await expect(page.getByRole("button", { name: /khoá|khóa/i })).toHaveCount(0);
  await expect(
    page.getByRole("button", { name: m.markReviewed }),
  ).toBeVisible();
});

test("the export button and the stale footnote stay out; the budget panel links to the limits screen", async ({
  page,
  request,
}) => {
  await loginAsAdmin(page, request, "aiu-links");
  await page.goto(`${APP_URL}/admin/ai-usage`);
  await expect(page.getByRole("heading", { level: 1 })).toHaveText(m.title);

  await expect(page.getByRole("button", { name: "Xuất báo cáo" })).toHaveCount(
    0,
  );
  await expect(
    page.getByText("Prompt sinh testcase tốn hơn dự kiến"),
  ).toHaveCount(0);
  const link = page.getByRole("link", { name: m.editLimits });
  await expect(link).toBeVisible({ timeout: 15_000 });
  await link.click();
  await expect(page).toHaveURL(/\/admin\/ai-limits/);
});
