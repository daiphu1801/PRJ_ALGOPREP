// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { withTestProviders } from "@/shared/test/render-with-providers";
import { InstructorAuthView } from "./instructor-auth-view";

vi.mock("next/navigation", () => ({
  useSearchParams: () => new URLSearchParams(),
  useRouter: () => ({ replace: vi.fn(), push: vi.fn() }),
}));

describe("InstructorAuthView", () => {
  it("is a login-only screen: no sign-up switch, no OAuth buttons", () => {
    render(withTestProviders(<InstructorAuthView />));

    expect(screen.getByRole("heading", { level: 1 })).toBeInTheDocument();
    expect(
      screen.queryByRole("button", { name: /Google|GitHub/i }),
    ).not.toBeInTheDocument();
    expect(screen.queryByText(/Đăng ký/)).not.toBeInTheDocument();
  });

  it("links a non-instructor back to the learner login", () => {
    render(withTestProviders(<InstructorAuthView />));

    expect(
      screen
        .getAllByRole("link")
        .some((a) => a.getAttribute("href") === "/login"),
    ).toBe(true);
  });
});
