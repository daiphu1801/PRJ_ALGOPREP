// Mock-mode smoke tests of the dedicated admin login screen: admin copy, admin-only quick login and
// the forgot-password branch. The identity API is not involved: NEXT_PUBLIC_MOCK_DATA is on.
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { withTestProviders } from "@/shared/test/render-with-providers";
import viMessages from "../../../../../messages/vi.json";

const nav = vi.hoisted(() => ({ push: vi.fn() }));
vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: nav.push, refresh: vi.fn() }),
  useSearchParams: () => new URLSearchParams(),
}));

import { AdminAuthView } from "./admin-auth-view";

const a = viMessages.auth;
const adminAuth = viMessages.adminAuth;

beforeEach(() => nav.push.mockReset());

describe("AdminAuthView", () => {
  it("shows the admin login copy and only the admin quick login", () => {
    render(withTestProviders(<AdminAuthView />));

    expect(
      screen.getByRole("heading", { level: 1, name: adminAuth.title }),
    ).toBeInTheDocument();
    expect(screen.getByText(adminAuth.eyebrow)).toBeInTheDocument();
    expect(screen.getByText(adminAuth.subtitle)).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: a.quickLoginAdmin }),
    ).toBeInTheDocument();
    expect(
      screen.queryByRole("button", { name: a.quickLoginStudent }),
    ).not.toBeInTheDocument();
    expect(screen.getByRole("link", { name: a.loginTitle })).toHaveAttribute(
      "href",
      "/login",
    );
  });

  it("walks into the forgot-password flow with the matching title and no hint link", () => {
    render(withTestProviders(<AdminAuthView />));

    fireEvent.click(screen.getByRole("button", { name: a.forgotPasswordLink }));

    expect(
      screen.getByRole("heading", { level: 1, name: a.forgotEmailTitle }),
    ).toBeInTheDocument();
    expect(screen.queryByText(adminAuth.eyebrow)).not.toBeInTheDocument();
    expect(
      screen.queryByRole("link", { name: a.loginTitle }),
    ).not.toBeInTheDocument();
  });

  it("admin quick login redirects to the admin home", async () => {
    render(withTestProviders(<AdminAuthView />));

    fireEvent.click(screen.getByRole("button", { name: a.quickLoginAdmin }));

    expect(await screen.findByRole("status")).toBeInTheDocument();
    await waitFor(() => expect(nav.push).toHaveBeenCalledTimes(1), {
      timeout: 6000,
    });
    expect(nav.push.mock.calls[0]![0]).toMatch(/^\/admin/);
  }, 10000);
});
