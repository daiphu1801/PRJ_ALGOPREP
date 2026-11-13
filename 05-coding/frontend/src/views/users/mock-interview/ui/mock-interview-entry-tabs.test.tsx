// USR0302 entry tabs on the REAL api path: "Bai nop" = F4 #23, "Cau hoi ngan hang" = F6 #2.
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import viMessages from "../../../../../messages/vi.json";
import { withTestProviders } from "@/shared/test/render-with-providers";

vi.mock("@/shared/api", async (importOriginal) =>
  (await import("@/shared/test/real-api")).realApiModule(
    await importOriginal<Record<string, unknown>>(),
  ),
);
vi.mock("@/shared/api/mock-mode", () => ({
  isMockMode: () => false,
  withMockData: <T,>(_mock: () => unknown, real: () => Promise<T>) => real(),
}));
vi.mock("@/shared/api/client", () => ({ refreshAccessToken: vi.fn() }));
vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: vi.fn() }),
  usePathname: () => "/mock-interview",
}));

import { apiMock, resetApiMock } from "@/shared/test/real-api";
import { MockInterviewView } from "./mock-interview-view";

const m = viMessages.mockInterview;

function script(accepted: unknown, bank: unknown) {
  apiMock.get.mockImplementation(async (path: string) => {
    if (path === "/judge/me/accepted-submissions") {
      if (accepted instanceof Error) throw accepted;
      return accepted;
    }
    if (path === "/interview-bank/questions") {
      if (bank instanceof Error) throw bank;
      return bank;
    }
    if (path === "/interview-bank/topics") return { items: [] };
    return { items: [] };
  });
}

beforeEach(() => {
  resetApiMock();
  sessionStorage.clear();
});

describe("USR0302 entry tabs (real)", () => {
  it("lists the learner's Accepted submissions and enables Start once one is picked", async () => {
    script(
      {
        items: [
          {
            submissionId: "s-1",
            problem: { title: "Two Sum" },
            language: "PYTHON",
          },
        ],
      },
      { items: [] },
    );
    render(withTestProviders(<MockInterviewView />));
    fireEvent.click(await screen.findByText("Two Sum"));
    expect(screen.getByText("Python")).toBeInTheDocument();
  });

  it("shows the empty copy when there is no Accepted submission", async () => {
    script({ items: [] }, { items: [] });
    render(withTestProviders(<MockInterviewView />));
    expect(await screen.findByText(m.emptySubmissions)).toBeInTheDocument();
  });

  it("lists the first bank page under the bank tab", async () => {
    script(
      { items: [] },
      { items: [{ id: "q-1", title: "Thiết kế LRU Cache" }] },
    );
    render(withTestProviders(<MockInterviewView />));
    fireEvent.click(await screen.findByRole("button", { name: m.tab.bank }));
    expect(await screen.findByText("Thiết kế LRU Cache")).toBeInTheDocument();
  });

  it("shows the generic retry block when the bank list fails", async () => {
    script({ items: [] }, new Error("boom"));
    render(withTestProviders(<MockInterviewView />));
    fireEvent.click(await screen.findByRole("button", { name: m.tab.bank }));
    await waitFor(() =>
      expect(screen.getByText(viMessages.common.retry)).toBeInTheDocument(),
    );
  });
});
