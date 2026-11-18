import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { Settings, LogOut } from "lucide-react";
import { beforeEach, describe, expect, it, vi } from "vitest";

const nav = vi.hoisted(() => ({ push: vi.fn(), logout: vi.fn() }));
vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: nav.push }),
}));
vi.mock("@/entities/auth", () => ({ logout: nav.logout }));

import { AreaUserMenu } from "./area-user-menu";

const ITEMS = [
  {
    key: "settings",
    href: "/admin/settings",
    label: "Cai dat",
    icon: Settings,
  },
  { key: "logout", href: "/admin/login", label: "Dang xuat", icon: LogOut },
];

function renderMenu(name = "Admin Demo") {
  return render(
    <AreaUserMenu name={name} roleLabel="Quan tri" items={ITEMS} />,
  );
}

beforeEach(() => {
  nav.push.mockReset();
  nav.logout.mockReset();
  nav.logout.mockResolvedValue(undefined);
});

describe("AreaUserMenu", () => {
  it("shows the name, the role and an initial, with the menu closed", () => {
    renderMenu();

    expect(screen.getByText("Admin Demo")).toBeInTheDocument();
    expect(screen.getByText("Quan tri")).toBeInTheDocument();
    expect(screen.getByText("A")).toBeInTheDocument();
    expect(screen.queryByRole("menu")).not.toBeInTheDocument();
  });

  it("falls back to the letter A when there is no name yet", () => {
    renderMenu("");
    expect(screen.getByText("A")).toBeInTheDocument();
  });

  it("opens and toggles closed from the trigger", () => {
    renderMenu();
    const trigger = screen.getByRole("button", { expanded: false });

    fireEvent.click(trigger);
    expect(screen.getAllByRole("menuitem")).toHaveLength(2);

    fireEvent.click(trigger);
    expect(screen.queryByRole("menu")).not.toBeInTheDocument();
  });

  it("closes on Escape and on an outside press, not on an inside press or other keys", () => {
    renderMenu();
    const trigger = screen.getByRole("button", { expanded: false });

    fireEvent.click(trigger);
    fireEvent.keyDown(document, { key: "Escape" });
    expect(screen.queryByRole("menu")).not.toBeInTheDocument();

    fireEvent.click(trigger);
    fireEvent.keyDown(document, { key: "Tab" });
    expect(screen.getByRole("menu")).toBeInTheDocument();
    fireEvent.pointerDown(screen.getByRole("menu"));
    expect(screen.getByRole("menu")).toBeInTheDocument();
    fireEvent.pointerDown(document.body);
    expect(screen.queryByRole("menu")).not.toBeInTheDocument();
  });

  it("closes after picking a normal link without signing out", () => {
    renderMenu();
    fireEvent.click(screen.getByRole("button", { expanded: false }));

    fireEvent.click(screen.getByRole("menuitem", { name: "Cai dat" }));

    expect(screen.queryByRole("menu")).not.toBeInTheDocument();
    expect(nav.logout).not.toHaveBeenCalled();
  });

  it("signs out and goes to the item's href when the logout entry is picked", async () => {
    renderMenu();
    fireEvent.click(screen.getByRole("button", { expanded: false }));

    fireEvent.click(screen.getByRole("menuitem", { name: "Dang xuat" }));

    await waitFor(() => expect(nav.push).toHaveBeenCalledWith("/admin/login"));
    expect(nav.logout).toHaveBeenCalledTimes(1);
  });
});
