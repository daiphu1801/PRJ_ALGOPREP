import { render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { tokenStore } from "@/shared/api";

const replace = vi.fn();
const nav = { pathname: "/problems" };

vi.mock("next/navigation", () => ({
  useRouter: () => ({ replace }),
  usePathname: () => nav.pathname,
}));
vi.mock("@/shared/api", async (importOriginal) => ({
  ...(await importOriginal<Record<string, unknown>>()),
  isMockMode: () => false,
}));

import { RoleGate } from "./role-gate";

function tokenFor(category: string): string {
  const encode = (value: unknown) =>
    btoa(JSON.stringify(value))
      .replace(/\+/g, "-")
      .replace(/\//g, "_")
      .replace(/=+$/, "");
  return `${encode({ alg: "RS256" })}.${encode({ base_category: category })}.sig`;
}

function renderStudentGate() {
  return render(
    <RoleGate area="student">
      <p>area content</p>
    </RoleGate>,
  );
}

// DEC-2026-1126-guest-browse-published-problems: a guest may open the list and one problem only.
describe("RoleGate for a guest in the student area", () => {
  beforeEach(() => {
    replace.mockReset();
    tokenStore.set(null);
  });
  afterEach(() => tokenStore.set(null));

  it("lets a guest into the problem list and a problem overview", async () => {
    for (const pathname of ["/problems", "/problems/p1"]) {
      nav.pathname = pathname;
      const { unmount } = renderStudentGate();
      expect(await screen.findByText("area content")).toBeTruthy();
      expect(replace).not.toHaveBeenCalled();
      unmount();
    }
  });

  it("still sends a guest to login, remembering the page, anywhere else", async () => {
    nav.pathname = "/workspace/p1";
    renderStudentGate();
    await waitFor(() =>
      expect(replace).toHaveBeenCalledWith("/login?redirect=%2Fworkspace%2Fp1"),
    );
    expect(screen.queryByText("area content")).toBeNull();
  });

  it("keeps sending an instructor away from the student list as before", async () => {
    nav.pathname = "/problems";
    tokenStore.set(tokenFor("INSTRUCTOR"));
    renderStudentGate();
    await waitFor(() => expect(replace).toHaveBeenCalled());
    expect(screen.queryByText("area content")).toBeNull();
  });
});
