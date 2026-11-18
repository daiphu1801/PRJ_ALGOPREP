import { act, fireEvent, render, screen } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { withTestProviders } from "@/shared/test/render-with-providers";
import viMessages from "../../../../messages/vi.json";

const nav = vi.hoisted(() => ({
  pathname: "/admin/overview",
  from: null as string | null,
}));
vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: vi.fn(), refresh: vi.fn() }),
  usePathname: () => nav.pathname,
  useSearchParams: () => ({ get: () => nav.from }),
}));

import { AdminSidebar } from "./admin-sidebar";
import { InstructorSidebar } from "./instructor-sidebar";

const STORAGE_KEY = "algoprep-admin-collapsed";

function stubMatchMedia(narrow: boolean) {
  const listeners = new Set<() => void>();
  const mql = {
    matches: narrow,
    addEventListener: (_: string, fn: () => void) => listeners.add(fn),
    removeEventListener: (_: string, fn: () => void) => listeners.delete(fn),
  };
  vi.stubGlobal("matchMedia", () => mql);
  return {
    setNarrow(value: boolean) {
      mql.matches = value;
      listeners.forEach((fn) => fn());
    },
  };
}

beforeEach(() => {
  nav.pathname = "/admin/overview";
  nav.from = null;
  window.localStorage.clear();
  stubMatchMedia(false);
});

afterEach(() => {
  vi.unstubAllGlobals();
});

describe("GroupedSidebar via AdminSidebar", () => {
  it("marks the overview link as the current page and opens only the group owning the route", () => {
    render(withTestProviders(<AdminSidebar />));

    expect(
      screen.getByRole("link", { name: viMessages.adminNav.overview }),
    ).toHaveAttribute("aria-current", "page");
    // No group owns /admin/overview, so every group header starts collapsed.
    const headers = screen
      .getAllByRole("button")
      .filter((b) => b.hasAttribute("aria-expanded"));
    expect(headers.length).toBeGreaterThan(0);
    headers.forEach((h) => expect(h).toHaveAttribute("aria-expanded", "false"));
  });

  it("opens the group of the active route and highlights the matching item", () => {
    nav.pathname = "/admin/problems";
    render(withTestProviders(<AdminSidebar />));

    const group = screen.getByRole("button", {
      name: viMessages.adminNav.groupContent,
    });
    expect(group).toHaveAttribute("aria-expanded", "true");
    expect(
      screen.getByRole("link", { name: viMessages.adminNav.problems }),
    ).toHaveAttribute("aria-current", "page");
  });

  it("toggles a group by hand without navigating", () => {
    render(withTestProviders(<AdminSidebar />));
    const group = screen.getByRole("button", {
      name: viMessages.adminNav.groupContent,
    });

    fireEvent.click(group);
    expect(group).toHaveAttribute("aria-expanded", "true");
    expect(
      screen.getByRole("link", { name: viMessages.adminNav.problems }),
    ).toBeInTheDocument();

    fireEvent.click(group);
    expect(group).toHaveAttribute("aria-expanded", "false");
    expect(
      screen.queryByRole("link", { name: viMessages.adminNav.problems }),
    ).not.toBeInTheDocument();
  });

  it("collapses to the icon rail, persists the choice, and expands again", () => {
    render(withTestProviders(<AdminSidebar />));
    const toggle = screen.getByRole("button", {
      name: viMessages.adminNav.toggleSidebar,
    });
    expect(screen.getByText(viMessages.adminNav.collapseSidebar)).toBeVisible();

    fireEvent.click(toggle);
    expect(window.localStorage.getItem(STORAGE_KEY)).toBe("1");
    expect(
      screen.queryByText(viMessages.adminNav.collapseSidebar),
    ).not.toBeInTheDocument();
    // Rail mode hides the label and exposes it as a tooltip instead.
    expect(
      screen.getByRole("link", { name: viMessages.adminNav.overview }),
    ).toHaveAttribute("title", viMessages.adminNav.overview);

    fireEvent.click(toggle);
    expect(window.localStorage.getItem(STORAGE_KEY)).toBe("0");
    expect(screen.getByText(viMessages.adminNav.collapseSidebar)).toBeVisible();
  });

  it("restores the saved collapsed preference after mount", () => {
    window.localStorage.setItem(STORAGE_KEY, "1");
    render(withTestProviders(<AdminSidebar />));

    expect(
      screen.queryByText(viMessages.adminNav.collapseSidebar),
    ).not.toBeInTheDocument();
  });

  it("keeps working when storage is blocked", () => {
    vi.spyOn(Storage.prototype, "setItem").mockImplementation(() => {
      throw new Error("blocked");
    });
    vi.spyOn(Storage.prototype, "getItem").mockImplementation(() => {
      throw new Error("blocked");
    });
    render(withTestProviders(<AdminSidebar />));

    fireEvent.click(
      screen.getByRole("button", { name: viMessages.adminNav.toggleSidebar }),
    );
    expect(
      screen.queryByText(viMessages.adminNav.collapseSidebar),
    ).not.toBeInTheDocument();
    vi.restoreAllMocks();
  });

  it("forces the icon rail on a narrow viewport and follows viewport changes", () => {
    const media = stubMatchMedia(true);
    render(withTestProviders(<AdminSidebar />));
    expect(
      screen.queryByText(viMessages.adminNav.collapseSidebar),
    ).not.toBeInTheDocument();

    act(() => media.setNarrow(false));
    expect(
      screen.getByText(viMessages.adminNav.collapseSidebar),
    ).toBeInTheDocument();
  });
});

describe("GroupedSidebar via InstructorSidebar", () => {
  it("shows the area eyebrow, opens every group and lights the longest matching prefix", () => {
    nav.pathname = "/instructor/classes/c1/students/s1";
    render(withTestProviders(<InstructorSidebar />));

    expect(
      screen.getByText(viMessages.instructorNav.areaEyebrow),
    ).toBeInTheDocument();
    const headers = screen
      .getAllByRole("button")
      .filter((b) => b.hasAttribute("aria-expanded"));
    headers.forEach((h) => expect(h).toHaveAttribute("aria-expanded", "true"));
    expect(
      screen.getByRole("link", { name: viMessages.instructorNav.classes }),
    ).toHaveAttribute("aria-current", "page");
  });

  it("keeps the calling screen lit on a submission result page via ?from=", () => {
    nav.pathname = "/instructor/submissions/SB-1";
    nav.from = "class";
    render(withTestProviders(<InstructorSidebar />));

    expect(
      screen.getByRole("link", { name: viMessages.instructorNav.classes }),
    ).toHaveAttribute("aria-current", "page");
  });
});
