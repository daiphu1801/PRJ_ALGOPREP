// T-PF-27 and T-PF-28: no request without a picture, the bytes come through the bearer client with
// the version in the query, a failure falls back to initials without a toast, a new version reloads.
import { render, screen, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { beforeEach, describe, expect, it, vi } from "vitest";

vi.mock("@/shared/api", async (importOriginal) =>
  (await import("@/shared/test/real-api")).realApiModule(
    await importOriginal<Record<string, unknown>>(),
  ),
);

import { apiMock, resetApiMock } from "@/shared/test/real-api";
import { UserAvatar } from "./user-avatar";

beforeEach(() => {
  resetApiMock();
  URL.createObjectURL = vi.fn(() => "blob:me");
  URL.revokeObjectURL = vi.fn();
});

function wrap(version: number | null | undefined, client: QueryClient) {
  return (
    <QueryClientProvider client={client}>
      <UserAvatar userId="u1" name="Lê Minh" avatarVersion={version} />
    </QueryClientProvider>
  );
}

describe("UserAvatar", () => {
  it.each([null, undefined])(
    "draws initials and calls nothing when avatarVersion is %s",
    (version) => {
      render(wrap(version, new QueryClient()));
      expect(screen.getByRole("img", { name: "Lê Minh" })).toHaveTextContent(
        "LM",
      );
      expect(apiMock.getBlob).not.toHaveBeenCalled();
    },
  );

  it("loads #76 with the version and shows the picture", async () => {
    apiMock.getBlob.mockResolvedValue(new Blob(["x"]));
    const { container } = render(wrap(3, new QueryClient()));
    await waitFor(() =>
      expect(container.querySelector("img")).toHaveAttribute("src", "blob:me"),
    );
    expect(apiMock.getBlob).toHaveBeenCalledWith("/identity/users/u1/avatar", {
      v: 3,
    });
  });

  it("falls back to initials when the load fails and reloads on a new version", async () => {
    apiMock.getBlob.mockRejectedValueOnce(new Error("404"));
    const client = new QueryClient();
    const { container, rerender } = render(wrap(1, client));
    await waitFor(() => expect(apiMock.getBlob).toHaveBeenCalledTimes(1));
    expect(container.querySelector("img")).toBeNull();
    expect(screen.getByRole("img", { name: "Lê Minh" })).toHaveTextContent(
      "LM",
    );
    apiMock.getBlob.mockResolvedValue(new Blob(["x"]));
    rerender(wrap(2, client));
    await waitFor(() => expect(apiMock.getBlob).toHaveBeenCalledTimes(2));
  });
});
