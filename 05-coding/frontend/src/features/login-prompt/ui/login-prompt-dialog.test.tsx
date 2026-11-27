import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { withTestProviders } from "@/shared/test/render-with-providers";
import viMessages from "../../../../messages/vi.json";
import { LoginPromptDialog } from "./login-prompt-dialog";

const m = viMessages.loginPrompt;

describe("LoginPromptDialog", () => {
  it("sends Login and Register to auth with the page to come back to", () => {
    render(
      withTestProviders(
        <LoginPromptDialog
          open
          returnTo="/problems/p 1?x=1"
          onClose={vi.fn()}
        />,
      ),
    );

    const redirect = encodeURIComponent("/problems/p 1?x=1");
    expect(screen.getByRole("link", { name: m.login })).toHaveAttribute(
      "href",
      `/login?redirect=${redirect}`,
    );
    expect(screen.getByRole("link", { name: m.register })).toHaveAttribute(
      "href",
      `/register?redirect=${redirect}`,
    );
  });

  it("closes on Not now and renders nothing while closed", () => {
    const onClose = vi.fn();
    const { rerender } = render(
      withTestProviders(
        <LoginPromptDialog open returnTo="/problems" onClose={onClose} />,
      ),
    );
    fireEvent.click(screen.getByRole("button", { name: m.later }));
    expect(onClose).toHaveBeenCalledTimes(1);

    rerender(
      withTestProviders(
        <LoginPromptDialog
          open={false}
          returnTo="/problems"
          onClose={onClose}
        />,
      ),
    );
    expect(screen.queryByRole("link", { name: m.login })).toBeNull();
  });
});
