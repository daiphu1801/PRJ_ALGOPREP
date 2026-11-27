import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { withTestProviders } from "@/shared/test/render-with-providers";
import viMessages from "../../../../messages/vi.json";

const nav = vi.hoisted(() => ({
  pathname: "/problems/two-sum",
  push: vi.fn(),
  logout: vi.fn(),
}));
vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: nav.push, refresh: vi.fn() }),
  usePathname: () => nav.pathname,
}));
vi.mock("@/entities/auth", () => ({ logout: nav.logout }));

import { StudentHeader } from "./student-header";

beforeEach(() => {
  nav.pathname = "/problems/two-sum";
  nav.push.mockReset();
  nav.logout.mockReset();
  nav.logout.mockResolvedValue(undefined);
});

describe("StudentHeader", () => {
  it("lights the nav item whose href is a prefix of the current path", () => {
    render(withTestProviders(<StudentHeader />));

    const inlineNav = screen.getAllByRole("navigation")[0]!;
    const current = Array.from(inlineNav.querySelectorAll("a")).filter(
      (a) => a.getAttribute("aria-current") === "page",
    );
    expect(current).toHaveLength(1);
    expect(current[0]).toHaveAttribute("href", "/problems");
  });

  it("does not mark any item on a path outside the nav", () => {
    nav.pathname = "/somewhere-else";
    render(withTestProviders(<StudentHeader />));

    expect(document.querySelector("[aria-current='page']")).toBeNull();
  });

  it("opens the narrow-viewport drawer, closes it on Escape and returns focus to its button", () => {
    render(withTestProviders(<StudentHeader />));
    const toggle = screen.getByRole("button", {
      name: viMessages.nav.toggleNav,
    });
    expect(screen.getAllByRole("navigation")).toHaveLength(1);

    fireEvent.click(toggle);
    expect(toggle).toHaveAttribute("aria-expanded", "true");
    expect(screen.getAllByRole("navigation")).toHaveLength(2);

    fireEvent.keyDown(document, { key: "Escape" });
    expect(toggle).toHaveAttribute("aria-expanded", "false");
    expect(toggle).toHaveFocus();
    expect(screen.getAllByRole("navigation")).toHaveLength(1);
  });

  it("closes the drawer when a destination is picked", () => {
    nav.pathname = "/dashboard";
    render(withTestProviders(<StudentHeader />));
    const toggle = screen.getByRole("button", {
      name: viMessages.nav.toggleNav,
    });
    fireEvent.click(toggle);
    const drawer = screen.getAllByRole("navigation")[1]!;
    const link = drawer.querySelector("a[href='/dashboard']") as HTMLElement;
    expect(link).toHaveAttribute("aria-current", "page");

    fireEvent.click(link);
    expect(toggle).toHaveAttribute("aria-expanded", "false");
  });

  it("ignores keys other than Escape while a popup is open", () => {
    render(withTestProviders(<StudentHeader />));
    const toggle = screen.getByRole("button", {
      name: viMessages.nav.toggleNav,
    });
    fireEvent.click(toggle);

    fireEvent.keyDown(document, { key: "a" });
    expect(toggle).toHaveAttribute("aria-expanded", "true");
  });

  it("opens the account menu, and Escape closes it with focus back on the avatar button", () => {
    render(withTestProviders(<StudentHeader />));
    const avatar = screen.getByRole("button", {
      name: viMessages.nav.accountMenu,
    });

    fireEvent.click(avatar);
    expect(avatar).toHaveAttribute("aria-expanded", "true");
    expect(
      screen.getByRole("link", { name: viMessages.nav.profile }),
    ).toHaveAttribute("href", "/profile");

    fireEvent.keyDown(document, { key: "Escape" });
    expect(avatar).toHaveAttribute("aria-expanded", "false");
    expect(avatar).toHaveFocus();
  });

  it("closes the account menu through the scrim and when a normal item is picked", () => {
    render(withTestProviders(<StudentHeader />));
    const avatar = screen.getByRole("button", {
      name: viMessages.nav.accountMenu,
    });

    fireEvent.click(avatar);
    fireEvent.click(
      screen.getByRole("button", { name: viMessages.nav.closeMenu }),
    );
    expect(avatar).toHaveAttribute("aria-expanded", "false");

    fireEvent.click(avatar);
    fireEvent.click(screen.getByRole("link", { name: viMessages.nav.saved }));
    expect(avatar).toHaveAttribute("aria-expanded", "false");
    expect(nav.logout).not.toHaveBeenCalled();
  });

  it("signs out through the API and then navigates to the login page", async () => {
    render(withTestProviders(<StudentHeader />));
    fireEvent.click(
      screen.getByRole("button", { name: viMessages.nav.accountMenu }),
    );

    fireEvent.click(screen.getByRole("link", { name: viMessages.nav.logout }));

    await waitFor(() => expect(nav.push).toHaveBeenCalledWith("/login"));
    expect(nav.logout).toHaveBeenCalledTimes(1);
  });

  it("still navigates away when the logout call fails", async () => {
    nav.logout.mockRejectedValue(new Error("network"));
    render(withTestProviders(<StudentHeader />));
    fireEvent.click(
      screen.getByRole("button", { name: viMessages.nav.accountMenu }),
    );

    fireEvent.click(screen.getByRole("link", { name: viMessages.nav.logout }));

    await waitFor(() => expect(nav.push).toHaveBeenCalledWith("/login"));
  });
});
