import { StrictMode } from "react";
import { act, cleanup, render, screen } from "@testing-library/react";
import { afterEach, expect, it, vi } from "vitest";
import { AuthProvider } from "./auth-provider";
import { tokenStore } from "@/shared/api/token-store";

const mocks = vi.hoisted(() => ({ refresh: vi.fn(), mockMode: false }));
vi.mock("@/shared/api/client", () => ({ refreshAccessToken: mocks.refresh }));
vi.mock("@/shared/api", async () => ({
  isMockMode: () => mocks.mockMode,
  tokenStore: (await import("@/shared/api/token-store")).tokenStore,
}));

afterEach(() => {
  cleanup();
  tokenStore.set(null);
  mocks.mockMode = false;
  mocks.refresh.mockReset();
});

it("waits for session restoration before mounting children, including StrictMode", async () => {
  let resolve!: (token: string | null) => void;
  mocks.refresh.mockReturnValue(
    new Promise<string | null>((done) => {
      resolve = done;
    }),
  );
  render(
    <StrictMode>
      <AuthProvider>
        <div>ready</div>
      </AuthProvider>
    </StrictMode>,
  );
  expect(screen.queryByText("ready")).not.toBeInTheDocument();
  await act(async () => resolve("restored"));
  expect(screen.getByText("ready")).toBeInTheDocument();
  expect(tokenStore.get()).toBe("restored");
});

it("allows anonymous users through after an unsuccessful refresh", async () => {
  mocks.refresh.mockResolvedValue(null);
  render(
    <AuthProvider>
      <div>login</div>
    </AuthProvider>,
  );
  expect(await screen.findByText("login")).toBeInTheDocument();
  expect(tokenStore.get()).toBeNull();
});

it("does not call the backend in mock mode", async () => {
  mocks.mockMode = true;
  render(
    <AuthProvider>
      <div>mock</div>
    </AuthProvider>,
  );
  expect(await screen.findByText("mock")).toBeInTheDocument();
  expect(mocks.refresh).not.toHaveBeenCalled();
});
