import { StrictMode } from "react";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import viMessages from "../../../../../messages/vi.json";
import { withTestProviders } from "@/shared/test/render-with-providers";

vi.mock("@/shared/api", async (importOriginal) =>
  (await import("@/shared/test/real-api")).realApiModule(
    await importOriginal<Record<string, unknown>>(),
  ),
);
const replace = vi.fn();
vi.mock("next/navigation", () => ({
  useRouter: () => ({ replace }),
  useSearchParams: () => new URLSearchParams("code=abc&state=xyz"),
}));

// Import after the mocks.
import { ApiError, tokenStore } from "@/shared/api";
import { apiMock, resetApiMock } from "@/shared/test/real-api";
import { OAuthCallbackView } from "./oauth-callback-view";

const m = viMessages.authCallback;
const apiErrors = viMessages.apiErrors as Record<string, string>;

function view(provider = "github") {
  return render(
    withTestProviders(
      <StrictMode>
        <OAuthCallbackView provider={provider} />
      </StrictMode>,
    ),
  );
}

describe("OAuthCallbackView", () => {
  beforeEach(() => {
    resetApiMock();
    replace.mockReset();
    tokenStore.set(null);
  });

  it("calls #6 once with the query, clears it from the address bar and goes to the role home", async () => {
    const strip = vi.spyOn(window.history, "replaceState");
    apiMock.get.mockResolvedValue({
      accessToken: "jwt",
      user: { baseCategory: "STUDENT" },
    });

    view("google");

    await waitFor(() => expect(replace).toHaveBeenCalledWith("/dashboard"));
    expect(apiMock.get).toHaveBeenCalledTimes(1); // Strict Mode ran the effect twice
    expect(apiMock.get).toHaveBeenCalledWith(
      "/identity/auth/oauth/google/callback",
      { code: "abc", state: "xyz" },
    );
    expect(strip).toHaveBeenCalled();
    expect(tokenStore.get()).toBe("jwt");
  });

  it.each(["IDT-109", "IDT-110", "IDT-505"])(
    "shows the translated message for %s and a way back",
    async (code) => {
      apiMock.get.mockRejectedValue(new ApiError(code, 400, "raw detail"));

      view();

      expect(await screen.findByText(apiErrors[code]!)).toBeInTheDocument();
      expect(screen.queryByText("raw detail")).not.toBeInTheDocument();
      expect(screen.getByRole("link", { name: m.backToLogin })).toHaveAttribute(
        "href",
        "/login",
      );
      expect(replace).not.toHaveBeenCalled();
    },
  );

  it("offers to restore a self-deleted account and confirms it without signing in (IDT-104)", async () => {
    apiMock.get.mockRejectedValue(
      new ApiError("IDT-104", 403, "x", { restoreTicket: "tk" }),
    );
    apiMock.post.mockResolvedValue({ restored: true });

    view();
    fireEvent.click(await screen.findByRole("button", { name: m.restore }));

    expect(await screen.findByText(m.restored)).toBeInTheDocument();
    expect(apiMock.post).toHaveBeenCalledWith(
      expect.stringContaining("restore"),
      { restoreTicket: "tk" },
    );
    expect(replace).not.toHaveBeenCalled();
    expect(tokenStore.get()).toBeNull();
  });

  it("shows the error of a failed restore (IDT-114) instead of retrying the spent ticket", async () => {
    apiMock.get.mockRejectedValue(
      new ApiError("IDT-104", 403, "x", { restoreTicket: "tk" }),
    );
    apiMock.post.mockRejectedValue(new ApiError("IDT-114", 401, "x"));

    view();
    fireEvent.click(await screen.findByRole("button", { name: m.restore }));

    expect(await screen.findByText(apiErrors["IDT-114"]!)).toBeInTheDocument();
  });

  it("refuses an unknown provider without calling the backend", async () => {
    view("facebook");

    expect(await screen.findByText(m.unknownProvider)).toBeInTheDocument();
    expect(apiMock.get).not.toHaveBeenCalled();
  });
});
