import { render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { tokenStore } from "@/shared/api";

const replace = vi.fn();
const flags = { mock: false };

vi.mock("next/navigation", () => ({
  useRouter: () => ({ replace }),
  usePathname: () => "/admin/users",
}));

vi.mock("@/shared/api", async (importOriginal) => ({
  ...(await importOriginal<Record<string, unknown>>()),
  isMockMode: () => flags.mock,
}));

// Import after the mocks so RoleGate picks them up.
import { RoleGate } from "./role-gate";

function tokenFor(category: string): string {
  const encode = (value: unknown) =>
    btoa(JSON.stringify(value))
      .replace(/\+/g, "-")
      .replace(/\//g, "_")
      .replace(/=+$/, "");
  return `${encode({ alg: "RS256" })}.${encode({ base_category: category })}.sig`;
}

function renderGate(area: "admin" | "instructor" | "student") {
  return render(
    <RoleGate area={area}>
      <p>area content</p>
    </RoleGate>,
  );
}

describe("RoleGate", () => {
  beforeEach(() => {
    replace.mockReset();
    flags.mock = false;
    tokenStore.set(null);
  });
  afterEach(() => tokenStore.set(null));

  it("shows the area to the role it requires", async () => {
    tokenStore.set(tokenFor("ADMIN"));
    renderGate("admin");
    expect(await screen.findByText("area content")).toBeTruthy();
    expect(replace).not.toHaveBeenCalled();
  });

  it("sends a signed-out visitor to that area's login and remembers the page", async () => {
    renderGate("admin");
    await waitFor(() =>
      expect(replace).toHaveBeenCalledWith(
        "/admin/login?redirect=%2Fadmin%2Fusers",
      ),
    );
    expect(screen.queryByText("area content")).toBeNull();
  });

  it("uses the instructor and student login routes for their areas", async () => {
    renderGate("instructor");
    await waitFor(() =>
      expect(replace.mock.calls[0]?.[0]).toMatch(/^\/instructor\/login\?/),
    );
    replace.mockReset();
    renderGate("student");
    await waitFor(() =>
      expect(replace.mock.calls[0]?.[0]).toMatch(/^\/login\?/),
    );
  });

  it("sends a signed-in user of another role to their own home", async () => {
    tokenStore.set(tokenFor("STUDENT"));
    renderGate("admin");
    await waitFor(() => expect(replace).toHaveBeenCalledWith("/dashboard"));
    expect(screen.queryByText("area content")).toBeNull();
  });

  it("lets everyone through in mock mode, which has no real session", async () => {
    flags.mock = true;
    renderGate("admin");
    expect(await screen.findByText("area content")).toBeTruthy();
    expect(replace).not.toHaveBeenCalled();
  });
});
