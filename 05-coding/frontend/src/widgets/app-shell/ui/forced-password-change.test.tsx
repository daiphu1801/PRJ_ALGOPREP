import { act, render, screen } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { setMustChangePassword } from "@/entities/auth";

const push = vi.fn();
const flags = { mock: false };
const profile: { mustChangePassword?: boolean } = {};

vi.mock("next/navigation", () => ({ useRouter: () => ({ push }) }));

vi.mock("@/entities/user", () => ({
  useMyProfile: () => ({ data: profile }),
}));

vi.mock("@/shared/api", async (importOriginal) => ({
  ...(await importOriginal<Record<string, unknown>>()),
  isMockMode: () => flags.mock,
}));

// The real dialog needs i18n; what matters here is when it shows and how it is wired.
vi.mock("@/features/change-my-password", () => ({
  ChangePasswordDialog: ({
    onClose,
    forced,
  }: {
    onClose: () => void;
    forced?: { onSignOut: () => void };
  }) => (
    <div role="dialog">
      <button onClick={onClose}>changed</button>
      <button onClick={forced?.onSignOut}>sign out</button>
    </div>
  ),
}));

// Import after the mocks so the component picks them up.
import { ForcedPasswordChange } from "./forced-password-change";

describe("ForcedPasswordChange", () => {
  beforeEach(() => {
    push.mockReset();
    flags.mock = false;
    delete profile.mustChangePassword;
    setMustChangePassword(false);
  });
  afterEach(() => setMustChangePassword(false));

  it("shows nothing for an ordinary account", () => {
    render(<ForcedPasswordChange area="student" />);
    expect(screen.queryByRole("dialog")).toBeNull();
  });

  it("blocks the area while the flag is set and lifts once the password changed", () => {
    setMustChangePassword(true);
    render(<ForcedPasswordChange area="student" />);
    expect(screen.getByRole("dialog")).toBeTruthy();

    act(() => screen.getByText("changed").click());
    expect(screen.queryByRole("dialog")).toBeNull();
  });

  it("offers sign-out to the area's own login page", async () => {
    setMustChangePassword(true);
    render(<ForcedPasswordChange area="admin" />);
    await act(async () => screen.getByText("sign out").click());
    expect(push).toHaveBeenCalledWith("/admin/login");
  });

  it("learns the flag from the server profile when this tab never saw the login", () => {
    profile.mustChangePassword = true;
    render(<ForcedPasswordChange area="student" />);
    expect(screen.getByRole("dialog")).toBeTruthy();
  });

  it("drops a stale flag once the server says the password was changed", () => {
    setMustChangePassword(true);
    profile.mustChangePassword = false;
    render(<ForcedPasswordChange area="student" />);
    expect(screen.queryByRole("dialog")).toBeNull();
    expect(sessionStorage.getItem("algoprep.mustChangePassword")).toBeNull();
  });

  it("stays out of the way in mock mode", () => {
    flags.mock = true;
    setMustChangePassword(true);
    render(<ForcedPasswordChange area="student" />);
    expect(screen.queryByRole("dialog")).toBeNull();
  });
});
