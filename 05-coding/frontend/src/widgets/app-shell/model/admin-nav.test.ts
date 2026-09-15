// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
import { describe, expect, it } from "vitest";
import { ADMIN_NAV_GROUPS, ADMIN_NAV_MISC, ADMIN_NAV_OVERVIEW } from "./admin-nav";

// Every route the admin sidebar links to must be a stub page that already exists under
// app/(admin)/admin/** — this task's instruction: "the sidebar's links should point at real
// existing routes, not 404s". Kept as a literal list (not a filesystem scan) so this test also
// catches someone renaming a page.tsx without updating the nav config.
const EXISTING_ADMIN_ROUTES = new Set([
  "/admin/overview",
  "/admin/problems",
  "/admin/interview-questions",
  "/admin/queue",
  "/admin/language-config",
  "/admin/ai-config",
  "/admin/ai-usage",
  "/admin/users",
  "/admin/system-log",
  "/admin/permissions",
]);

describe("admin nav config", () => {
  it("every grouped nav item and the overview item point at a route that exists", () => {
    const hrefs = [ADMIN_NAV_OVERVIEW, ...ADMIN_NAV_GROUPS.flatMap((g) => g.items)].map((item) => item.href);
    for (const href of hrefs) {
      expect(EXISTING_ADMIN_ROUTES.has(href)).toBe(true);
    }
  });

  it("does not contain admin_rejudge (removed from scope, DEC-2026-0828-remove-rejudge-scope)", () => {
    const allKeys = [
      ADMIN_NAV_OVERVIEW,
      ...ADMIN_NAV_GROUPS.flatMap((g) => g.items),
      ...ADMIN_NAV_MISC,
    ].map((item) => item.key);
    expect(allKeys).not.toContain("rejudge");
  });
});
