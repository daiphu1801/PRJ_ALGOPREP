import { fireEvent, render, screen } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { tokenStore } from "@/shared/api";
import { withTestProviders } from "@/shared/test/render-with-providers";
import viMessages from "../../../../messages/vi.json";

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: vi.fn(), refresh: vi.fn() }),
  usePathname: () => "/problems",
}));
vi.mock("@/shared/api/mock-mode", () => ({
  isMockMode: () => false,
  withMockData: <T,>(_mock: () => unknown, real: () => Promise<T>) => real(),
}));

import { StudentHeader } from "./student-header";

const nav = viMessages.nav;
const prompt = viMessages.loginPrompt;

// DEC-2026-1126: a guest sees every nav item, and a login button where the account menu would be.
describe("StudentHeader for a guest", () => {
  beforeEach(() => tokenStore.set(null));
  afterEach(() => tokenStore.set(null));

  it("keeps the problem list as a link and turns the other items into prompts", () => {
    render(withTestProviders(<StudentHeader />));
    const inline = screen.getAllByRole("navigation")[0]!;

    expect(inline.querySelector("a[href='/problems']")).not.toBeNull();
    for (const href of [
      "/dashboard",
      "/workspace",
      "/submissions",
      "/classes",
    ]) {
      expect(inline.querySelector(`a[href='${href}']`), href).toBeNull();
    }
    expect(inline.querySelectorAll("button")).toHaveLength(6);
  });

  it("opens the sign-in dialog for a personal item and returns to that item's page", () => {
    render(withTestProviders(<StudentHeader />));
    fireEvent.click(screen.getByRole("button", { name: nav.dashboard }));

    expect(screen.getByText(prompt.title)).toBeInTheDocument();
    const hrefs = screen
      .getAllByRole("link", { name: prompt.login })
      .map((link) => link.getAttribute("href"));
    expect(hrefs).toContain(
      `/login?redirect=${encodeURIComponent("/dashboard")}`,
    );
  });

  it("shows a Login button instead of the account menu", () => {
    render(withTestProviders(<StudentHeader />));

    expect(screen.queryByRole("button", { name: nav.accountMenu })).toBeNull();
    expect(screen.getByRole("link", { name: nav.login })).toHaveAttribute(
      "href",
      `/login?redirect=${encodeURIComponent("/problems")}`,
    );
  });
});
