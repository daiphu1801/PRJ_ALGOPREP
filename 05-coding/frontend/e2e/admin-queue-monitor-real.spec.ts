import { expect, test } from "@playwright/test";
import {
  APP_URL,
  createUser,
  requireRealStack,
  submitLogin,
  vi,
} from "./support/real-stack";

requireRealStack();

const m = vi.adminQueueMonitor;

test("reads the grading queue from the backend and opens controls by keyboard", async ({
  page,
  request,
}, testInfo) => {
  const admin = await createUser(request, {
    tag: "queue-admin",
    role: "ADMIN",
  });
  await page.setViewportSize({ width: 1440, height: 940 });
  await page.goto(`${APP_URL}/admin/login`);
  await submitLogin(page, admin.email, admin.password);
  await expect(page).toHaveURL(/\/admin\/overview/, { timeout: 15_000 });

  const reads = ["snapshot", "jobs", "infra-summary", "settings"].map((name) =>
    page.waitForResponse((response) =>
      new URL(response.url()).pathname.endsWith(`/judge/admin/queue/${name}`),
    ),
  );
  await page.goto(`${APP_URL}/admin/queue`);
  for (const response of await Promise.all(reads))
    expect(response.ok()).toBe(true);

  const title = page.getByRole("heading", { level: 1 });
  await expect(title).toHaveText(m.title);
  const titleInset = () =>
    title.evaluate((heading) =>
      Math.round(
        heading.getBoundingClientRect().left -
          heading.closest("header")!.getBoundingClientRect().left,
      ),
    );
  expect(await titleInset()).toBeGreaterThanOrEqual(16);
  const metrics = page.getByLabel(m.metricsLabel);
  await expect(metrics).toBeVisible();
  await expect(
    metrics.getByText(m.grading.throughput, { exact: true }),
  ).toBeVisible();
  const pause = page.getByRole("switch", { name: m.control.paused.label });
  await expect(pause).toBeHidden();
  await page.screenshot({ path: testInfo.outputPath("queue-desktop.png") });

  const controls = page.getByText(m.controlsTitle, { exact: true });
  await controls.focus();
  await controls.press("Enter");
  await expect(pause).toBeVisible();
  await expect(page.getByRole("link", { name: m.historyLink })).toBeVisible();

  await page.setViewportSize({ width: 390, height: 844 });
  expect(await titleInset()).toBeGreaterThanOrEqual(16);
  await expect(metrics).toBeVisible();
  await expect(page.getByRole("heading", { name: m.jobsTitle })).toBeVisible();
  await page.screenshot({ path: testInfo.outputPath("queue-mobile.png") });

  await page.getByRole("button", { name: m.params.open }).click();
  const settings = page.getByRole("dialog", { name: m.params.title });
  for (const label of [
    m.params.autoscaleThreshold,
    m.params.jobsPerPage,
    m.params.errorRetention,
    m.params.latencyCap,
  ]) {
    await expect(
      settings.getByRole("spinbutton", { name: label }),
    ).toBeVisible();
  }
  await expect(settings.getByText(m.params.latencyCapHint)).toBeVisible();
  await expect(
    settings.getByRole("button", { name: m.params.save }),
  ).toBeInViewport();
  await page.screenshot({
    path: testInfo.outputPath("queue-settings-mobile.png"),
  });
});
