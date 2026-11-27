// Mock-mode smoke tests of the two login screens (shared AuthView and AdminAuthView): the title per
// mode, switching between login and signup, the dev quick-login panel and the loading overlay that
// runs before the redirect. The identity API is not involved: NEXT_PUBLIC_MOCK_DATA is on.
import {
  act,
  fireEvent,
  render,
  screen,
  waitFor,
} from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { withTestProviders } from "@/shared/test/render-with-providers";
import viMessages from "../../../../../messages/vi.json";

const nav = vi.hoisted(() => ({
  push: vi.fn(),
  search: new URLSearchParams(),
}));
vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: nav.push, refresh: vi.fn() }),
  useSearchParams: () => nav.search,
}));

import { authLoading } from "@/features/auth-by-credentials";
import { AuthView } from "./auth-view";

const a = viMessages.auth;

beforeEach(() => {
  // The overlay state is module-level and outlives a test that ends on a successful sign-in.
  act(() => authLoading.clear());
  nav.push.mockReset();
  nav.search = new URLSearchParams();
});

describe("AuthView", () => {
  it("opens in the requested mode and shows the matching title", () => {
    render(withTestProviders(<AuthView initialMode="login" />));

    expect(
      screen.getByRole("heading", { level: 1, name: a.loginTitle }),
    ).toBeInTheDocument();
  });

  it("defaults to the signup mode", () => {
    render(withTestProviders(<AuthView />));

    expect(
      screen.getByRole("heading", { level: 1, name: a.signupTitle }),
    ).toBeInTheDocument();
  });

  it("switches between login and signup from either switch button", () => {
    render(withTestProviders(<AuthView initialMode="login" />));

    const toSignup = screen.getAllByRole("button", { name: a.switchToSignup });
    expect(toSignup).toHaveLength(2);
    fireEvent.click(toSignup[0]!);
    expect(
      screen.getByRole("heading", { level: 1, name: a.signupTitle }),
    ).toBeInTheDocument();

    fireEvent.click(
      screen.getAllByRole("button", { name: a.switchToLogin })[1]!,
    );
    expect(
      screen.getByRole("heading", { level: 1, name: a.loginTitle }),
    ).toBeInTheDocument();
  });

  it("offers a quick login per role in mock mode", () => {
    render(withTestProviders(<AuthView initialMode="login" />));

    expect(
      screen.getByRole("button", { name: a.quickLoginStudent }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: a.quickLoginInstructor }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: a.quickLoginAdmin }),
    ).toBeInTheDocument();
  });

  it("quick login runs the loading overlay and then redirects by role", async () => {
    render(withTestProviders(<AuthView initialMode="login" />));

    fireEvent.click(screen.getByRole("button", { name: a.quickLoginStudent }));

    expect(await screen.findByRole("status")).toHaveTextContent(a.loadingTitle);
    await waitFor(() => expect(nav.push).toHaveBeenCalledTimes(1), {
      timeout: 6000,
    });
    expect(nav.push.mock.calls[0]![0]).toBe("/dashboard");
    // The overlay stays through the navigation, so the form does not flash back; it is dropped
    // once the app has left the sign-in page (AuthLoadingReset).
    expect(screen.getByRole("status")).toHaveTextContent(a.loadingTitle);
    act(() => authLoading.clearIfLeft("/dashboard"));
    expect(screen.queryByRole("status")).not.toBeInTheDocument();
  }, 10000);

  it("honours a safe redirect query over the role home", async () => {
    nav.search = new URLSearchParams({ redirect: "/problems/two-sum" });
    render(withTestProviders(<AuthView initialMode="login" />));

    fireEvent.click(screen.getByRole("button", { name: a.quickLoginStudent }));

    await waitFor(() => expect(nav.push).toHaveBeenCalledTimes(1), {
      timeout: 6000,
    });
    expect(nav.push).toHaveBeenCalledWith("/problems/two-sum");
  }, 10000);
});
