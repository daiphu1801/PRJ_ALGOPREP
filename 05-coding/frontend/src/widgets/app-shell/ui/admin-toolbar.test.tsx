import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { withTestProviders } from "@/shared/test/render-with-providers";
import viMessages from "../../../../messages/vi.json";

const profile = vi.hoisted(() => ({ data: undefined as unknown }));
vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: vi.fn() }),
  usePathname: () => "/admin/overview",
}));
vi.mock("@/entities/user", () => ({ useMyProfile: () => profile }));

import { AdminToolbar } from "./admin-toolbar";

describe("AdminToolbar", () => {
  it("shows the signed-in admin's name once the profile is read", () => {
    profile.data = { displayName: "Admin Demo" };
    render(withTestProviders(<AdminToolbar />));
    expect(screen.getByText("Admin Demo")).toBeInTheDocument();
  });

  it("keeps the placeholder while the profile is loading", () => {
    profile.data = undefined;
    render(withTestProviders(<AdminToolbar />));
    expect(
      screen.getByText(viMessages.adminNav.identityNamePlaceholder),
    ).toBeInTheDocument();
  });
});
