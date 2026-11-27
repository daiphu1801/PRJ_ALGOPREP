import {
  act,
  fireEvent,
  render,
  screen,
  waitFor,
} from "@testing-library/react";
import { beforeEach, expect, it, vi } from "vitest";
import { withTestProviders } from "@/shared/test/render-with-providers";

vi.mock("@/shared/api", async (original) =>
  (await import("@/shared/test/real-api")).realApiModule(
    await original<Record<string, unknown>>(),
  ),
);
vi.mock("@/shared/api/mock-mode", () => ({
  isMockMode: () => false,
  withMockData: <T,>(_mock: () => unknown, real: () => Promise<T>) => real(),
}));
vi.mock("next/navigation", () => ({ useRouter: () => ({ push: vi.fn() }) }));

import { apiMock, resetApiMock } from "@/shared/test/real-api";
import { SavedProblemsView } from "./saved-problems-view";

beforeEach(() => {
  resetApiMock();
  apiMock.get.mockImplementation(async (path: string) =>
    path === "/problem-bank/levels"
      ? { items: [] }
      : {
          items: [
            {
              problemId: "p1",
              code: "P1",
              title: "Bài thử",
              levelCode: "EASY",
              topicNames: [],
              solveState: null,
              note: "ghi chú cũ",
              savedAt: "2026-11-01T00:00:00Z",
              withdrawn: false,
            },
          ],
          page: { number: 1, size: 20, totalItems: 1, totalPages: 1 },
          meta: { savedTotal: 1, solvedCount: null, filteredTotal: 1 },
        },
  );
});

it("preserves a dirty note when cancelling a filter change and locks it during save", async () => {
  let finish!: () => void;
  apiMock.put.mockImplementation(
    () =>
      new Promise<void>((resolve) => {
        finish = resolve;
      }),
  );
  render(withTestProviders(<SavedProblemsView />));
  fireEvent.click(await screen.findByText("ghi chú cũ"));
  const note = screen
    .getAllByRole("textbox")
    .find((box) => box.tagName === "TEXTAREA")!;
  fireEvent.change(note, { target: { value: "ghi chú mới" } });
  const search = screen.getByRole("textbox", { name: /Tìm/ });
  fireEvent.change(search, { target: { value: "khác" } });
  expect(await screen.findByRole("dialog")).toBeInTheDocument();
  fireEvent.click(screen.getByRole("button", { name: "Tiếp tục sửa" }));
  expect(note).toHaveValue("ghi chú mới");
  expect(search).toHaveValue("");
  fireEvent.click(screen.getByRole("button", { name: "Lưu" }));
  await waitFor(() => expect(note).toBeDisabled());
  expect(screen.getByRole("button", { name: "Hủy" })).toBeDisabled();
  await act(async () => finish());
  await waitFor(() =>
    expect(screen.queryByRole("button", { name: "Lưu" })).toBeNull(),
  );
  expect(apiMock.put).toHaveBeenCalledWith("/problem-bank/me/bookmarks/p1", {
    note: "ghi chú mới",
    createIfMissing: false,
  });
});
