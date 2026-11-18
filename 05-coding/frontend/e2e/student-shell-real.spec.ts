// Student shell (header, avatar menu, narrow-viewport drawer) against the REAL backend, signed in as a real
// learner. Replaces the mock-mode `student-shell.spec.ts`. That spec expected `role="menu"` with four
// `menuitem` entries; the header deliberately renders plain links (student-header.tsx: "Plain links, not
// role=menu: arrow-key navigation is not implemented"), which is why it counted 0. These tests assert the
// links instead. Needs the stack in e2e/README.md; skips itself otherwise.
import { expect, test } from "@playwright/test";
import { APP_URL, requireRealStack, vi } from "./support/real-stack";
import { loginAsStudent } from "./support/real-session";

requireRealStack();

const nav = vi.nav;
const NAV_LABELS = [
  nav.dashboard,
  nav.problems,
  nav.workspace,
  nav.submissions,
  nav.mockInterview,
  nav.interviewBank,
  nav.myClasses,
];
const MENU_LABELS = [nav.profile, nav.saved, nav.settings, nav.logout];

test("the header lists every learner destination and marks the current one", async ({
  page,
  request,
}) => {
  await loginAsStudent(page, request, "shell-nav");
  await page.goto(`${APP_URL}/problems`);

  const bar = page.getByRole("navigation", { name: nav.area.student });
  await expect(bar.getByRole("link", { name: nav.problems })).toHaveAttribute(
    "aria-current",
    "page",
  );
  for (const label of NAV_LABELS)
    await expect(
      bar.getByRole("link", { name: label, exact: true }),
    ).toBeVisible();
  await expect(bar.getByRole("link")).toHaveCount(NAV_LABELS.length);

  await bar.getByRole("link", { name: nav.submissions, exact: true }).click();
  await expect(page).toHaveURL(/\/submissions$/);
  await expect(
    bar.getByRole("link", { name: nav.submissions }),
  ).toHaveAttribute("aria-current", "page");
});

test("the avatar menu opens with its four links and reaches the settings page", async ({
  page,
  request,
}) => {
  await loginAsStudent(page, request, "shell-menu");
  await page.getByRole("button", { name: nav.accountMenu }).click();

  const header = page.getByRole("banner");
  for (const label of MENU_LABELS)
    await expect(
      header.getByRole("link", { name: label, exact: true }),
    ).toBeVisible();
  // Plain links, not a menu widget: the old mock spec counted `menuitem` and found none.
  await expect(page.getByRole("menuitem")).toHaveCount(0);

  await header.getByRole("link", { name: nav.settings, exact: true }).click();
  await expect(page).toHaveURL(/\/settings$/);
  await expect(
    header.getByRole("link", { name: nav.logout, exact: true }),
  ).toHaveCount(0);
});

test("logging out from the avatar menu returns to the login page", async ({
  page,
  request,
}) => {
  await loginAsStudent(page, request, "shell-logout");
  await page.getByRole("button", { name: nav.accountMenu }).click();
  await page
    .getByRole("banner")
    .getByRole("link", { name: nav.logout, exact: true })
    .click();
  await expect(page).toHaveURL(/\/login/, { timeout: 15_000 });
});

test("a narrow viewport folds the nav behind one button and the drawer closes after a pick", async ({
  page,
  request,
}) => {
  await loginAsStudent(page, request, "shell-narrow");
  await page.setViewportSize({ width: 820, height: 900 });
  await page.goto(`${APP_URL}/dashboard`);

  const bar = page.getByRole("navigation", { name: nav.area.student });
  await expect(bar).toBeHidden();

  await page.getByRole("button", { name: nav.toggleNav }).click();
  await expect(bar.getByRole("link")).toHaveCount(NAV_LABELS.length);

  await bar.getByRole("link", { name: nav.problems, exact: true }).click();
  await expect(page).toHaveURL(/\/problems$/);
  await expect(
    page.getByRole("navigation", { name: nav.area.student }),
  ).toBeHidden();
});
