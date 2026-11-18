// "Quản lý chủ đề" and "Quản lý độ khó" against the REAL api path (F6 #9 to #18): only the HTTP layer
// is faked, so the real modules, the error-code mapping and the real vi texts are what is asserted.
import {
  act,
  fireEvent,
  render,
  renderHook,
  screen,
  waitFor,
  within,
} from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import viMessages from "../../../../../messages/vi.json";
import { ApiError } from "@/shared/api";
import { toast, useToasts } from "@/shared/lib/toast-store";
import { withTestProviders } from "@/shared/test/render-with-providers";

vi.mock("@/shared/api", async (importOriginal) =>
  (await import("@/shared/test/real-api")).realApiModule(
    await importOriginal<Record<string, unknown>>(),
  ),
);
// `useMockQuery` reads the flag from `mock-mode` directly, not through the barrel mocked above.
vi.mock("@/shared/api/mock-mode", () => ({
  isMockMode: () => false,
  withMockData: <T,>(_mock: () => unknown, real: () => Promise<T>) => real(),
}));

// Import after the mocks.
import { apiMock, resetApiMock } from "@/shared/test/real-api";
import { LevelManagerDialog } from "./level-manager-dialog";
import { TopicManagerDialog } from "./topic-manager-dialog";

const m = viMessages.interviewQuestionManagement;
const errors = viMessages.apiErrors;

const topics = [
  {
    id: "t1",
    code: "CS_THEORY",
    displayName: "Lý thuyết CS",
    sortOrder: 1,
    usesStarFramework: false,
    questionCount: 0,
  },
  {
    id: "t2",
    code: "BEHAVIORAL",
    displayName: "Hành vi",
    sortOrder: 2,
    usesStarFramework: true,
    questionCount: 0,
  },
];
const levels = [
  { id: "l1", code: "EASY", displayName: "Dễ", sortOrder: 1, questionCount: 0 },
  {
    id: "l2",
    code: "HARD",
    displayName: "Khó",
    sortOrder: 2,
    questionCount: 0,
  },
];

function serve() {
  apiMock.get.mockImplementation(async (path: string) => {
    if (path === "/interview-bank/topics") return { items: topics };
    if (path === "/interview-bank/levels") return { items: levels };
    throw new Error(`unexpected GET ${path}`);
  });
  for (const verb of [apiMock.post, apiMock.patch, apiMock.put, apiMock.delete])
    verb.mockResolvedValue({});
}

const lastToast = (toasts: { current: readonly { message: string }[] }) =>
  toasts.current.at(-1)?.message;

function renderTopics() {
  render(
    withTestProviders(
      <TopicManagerDialog open onClose={() => {}} usage={{}} />,
    ),
  );
  return screen.findByDisplayValue("Lý thuyết CS");
}
function renderLevels() {
  render(
    withTestProviders(
      <LevelManagerDialog open onClose={() => {}} usage={{}} />,
    ),
  );
  return screen.findByDisplayValue("Dễ");
}

beforeEach(() => {
  resetApiMock();
  serve();
  act(() => toast.clear());
});

describe("TopicManagerDialog with the real API", () => {
  it("lists the server topics in order and refetches the catalogue after a successful add", async () => {
    await renderTopics();
    const getsBefore = apiMock.get.mock.calls.length;
    fireEvent.change(screen.getByLabelText(m.topicManager.newLabel), {
      target: { value: "Mạng máy tính" },
    });
    fireEvent.click(screen.getByRole("button", { name: m.topicManager.add }));

    await waitFor(() =>
      expect(apiMock.post).toHaveBeenCalledWith("/interview-bank/topics", {
        displayName: "Mạng máy tính",
      }),
    );
    await waitFor(() =>
      expect(apiMock.get.mock.calls.length).toBeGreaterThan(getsBefore),
    );
  });

  it("renames by id with the new display name", async () => {
    await renderTopics();
    const field = screen.getAllByLabelText(m.topicManager.nameLabel)[0]!;
    fireEvent.change(field, { target: { value: "Cấu trúc dữ liệu" } });
    fireEvent.keyDown(field, { key: "Enter" });
    await waitFor(() =>
      expect(apiMock.patch).toHaveBeenCalledWith("/interview-bank/topics/t1", {
        displayName: "Cấu trúc dữ liệu",
      }),
    );
  });

  it("moves a topic down by sending the full list of ids in the new order", async () => {
    await renderTopics();
    fireEvent.click(
      screen.getAllByRole("button", { name: m.topicManager.moveDown })[0]!,
    );
    await waitFor(() =>
      expect(apiMock.put).toHaveBeenCalledWith("/interview-bank/topics/order", {
        orderedIds: ["t2", "t1"],
      }),
    );
  });

  it("deletes by id", async () => {
    await renderTopics();
    fireEvent.click(screen.getAllByRole("button", { name: m.delete })[0]!);
    await waitFor(() =>
      expect(apiMock.delete).toHaveBeenCalledWith("/interview-bank/topics/t1"),
    );
  });

  it("switches the STAR flag with a PATCH of that field only", async () => {
    await renderTopics();
    const toasts = renderHook(() => useToasts());
    fireEvent.click(
      screen.getByRole("switch", { name: "Dùng khung STAR cho Lý thuyết CS" }),
    );
    await waitFor(() =>
      expect(apiMock.patch).toHaveBeenCalledWith("/interview-bank/topics/t1", {
        usesStarFramework: true,
      }),
    );
    await waitFor(() =>
      expect(lastToast(toasts.result)).toBe(
        "Đã bật khung STAR cho Lý thuyết CS",
      ),
    );
  });

  it.each([
    ["INB-020", 409, "add"],
    ["INB-V03", 400, "add"],
    ["INB-301", 403, "add"],
    ["INB-302", 403, "add"],
    ["INB-503", 503, "add"],
    ["INB-011", 404, "rename"],
    ["INB-V13", 400, "rename"],
    ["INB-034", 422, "move"],
    ["INB-V14", 400, "move"],
    ["INB-030", 422, "delete"],
    ["INB-011", 404, "delete"],
    ["INB-302", 403, "star"],
  ])(
    "shows the translated text of %s (%i) from a failed %s and no success toast",
    async (code, status, action) => {
      await renderTopics();
      const toasts = renderHook(() => useToasts());
      const failure = new ApiError(code, status);
      for (const verb of [
        apiMock.post,
        apiMock.patch,
        apiMock.put,
        apiMock.delete,
      ])
        verb.mockRejectedValue(failure);

      if (action === "add") {
        fireEvent.change(screen.getByLabelText(m.topicManager.newLabel), {
          target: { value: "Mới" },
        });
        fireEvent.click(
          screen.getByRole("button", { name: m.topicManager.add }),
        );
      } else if (action === "rename") {
        const field = screen.getAllByLabelText(m.topicManager.nameLabel)[0]!;
        fireEvent.change(field, { target: { value: "Tên khác" } });
        fireEvent.keyDown(field, { key: "Enter" });
      } else if (action === "move") {
        fireEvent.click(
          screen.getAllByRole("button", { name: m.topicManager.moveDown })[0]!,
        );
      } else if (action === "delete") {
        fireEvent.click(screen.getAllByRole("button", { name: m.delete })[0]!);
      } else {
        fireEvent.click(
          screen.getByRole("switch", {
            name: "Dùng khung STAR cho Lý thuyết CS",
          }),
        );
      }

      const expected = errors[code as keyof typeof errors];
      await waitFor(() => expect(lastToast(toasts.result)).toBe(expected));
      expect(toasts.result.current.every((item) => item.tone === "error")).toBe(
        true,
      );
    },
  );

  it("does not call the server for an empty or duplicate name", async () => {
    await renderTopics();
    const toasts = renderHook(() => useToasts());
    fireEvent.click(screen.getByRole("button", { name: m.topicManager.add }));
    expect(lastToast(toasts.result)).toBe(m.topicManager.error.empty);
    fireEvent.change(screen.getByLabelText(m.topicManager.newLabel), {
      target: { value: " hành vi " },
    });
    fireEvent.click(screen.getByRole("button", { name: m.topicManager.add }));
    expect(lastToast(toasts.result)).toBe(m.topicManager.error.duplicate);
    expect(apiMock.post).not.toHaveBeenCalled();
  });
});

describe("LevelManagerDialog with the real API", () => {
  it("adds, renames, reorders and deletes on the level paths", async () => {
    await renderLevels();
    fireEvent.change(screen.getByLabelText(m.levelManager.newLabel), {
      target: { value: "Rất khó" },
    });
    fireEvent.click(screen.getByRole("button", { name: m.levelManager.add }));
    await waitFor(() =>
      expect(apiMock.post).toHaveBeenCalledWith("/interview-bank/levels", {
        displayName: "Rất khó",
      }),
    );

    const field = screen.getAllByLabelText(m.levelManager.nameLabel)[0]!;
    fireEvent.change(field, { target: { value: "Cơ bản" } });
    fireEvent.keyDown(field, { key: "Enter" });
    await waitFor(() =>
      expect(apiMock.patch).toHaveBeenCalledWith("/interview-bank/levels/l1", {
        displayName: "Cơ bản",
      }),
    );

    fireEvent.click(
      screen.getAllByRole("button", { name: m.levelManager.moveDown })[0]!,
    );
    await waitFor(() =>
      expect(apiMock.put).toHaveBeenCalledWith("/interview-bank/levels/order", {
        orderedIds: ["l2", "l1"],
      }),
    );

    fireEvent.click(screen.getAllByRole("button", { name: m.delete })[1]!);
    await waitFor(() =>
      expect(apiMock.delete).toHaveBeenCalledWith("/interview-bank/levels/l2"),
    );
  });

  it.each([
    ["INB-021", 409, "add"],
    ["INB-V03", 400, "add"],
    ["INB-301", 403, "add"],
    ["INB-302", 403, "add"],
    ["INB-503", 503, "add"],
    ["INB-012", 404, "rename"],
    ["INB-034", 422, "move"],
    ["INB-V14", 400, "move"],
    ["INB-031", 422, "delete"],
    ["INB-032", 422, "delete"],
    ["INB-012", 404, "delete"],
  ])(
    "shows the translated text of %s (%i) from a failed %s",
    async (code, status, action) => {
      await renderLevels();
      const toasts = renderHook(() => useToasts());
      const failure = new ApiError(code, status);
      for (const verb of [
        apiMock.post,
        apiMock.patch,
        apiMock.put,
        apiMock.delete,
      ])
        verb.mockRejectedValue(failure);

      if (action === "add") {
        fireEvent.change(screen.getByLabelText(m.levelManager.newLabel), {
          target: { value: "Mới" },
        });
        fireEvent.click(
          screen.getByRole("button", { name: m.levelManager.add }),
        );
      } else if (action === "rename") {
        const field = screen.getAllByLabelText(m.levelManager.nameLabel)[0]!;
        fireEvent.change(field, { target: { value: "Tên khác" } });
        fireEvent.keyDown(field, { key: "Enter" });
      } else if (action === "move") {
        fireEvent.click(
          screen.getAllByRole("button", { name: m.levelManager.moveDown })[0]!,
        );
      } else {
        fireEvent.click(screen.getAllByRole("button", { name: m.delete })[0]!);
      }

      const expected = errors[code as keyof typeof errors];
      await waitFor(() => expect(lastToast(toasts.result)).toBe(expected));
      expect(toasts.result.current.every((item) => item.tone === "error")).toBe(
        true,
      );
    },
  );

  it("keeps the delete control disabled while a single level is left", async () => {
    apiMock.get.mockImplementation(async (path: string) =>
      path === "/interview-bank/levels"
        ? { items: [levels[0]] }
        : { items: topics },
    );
    await renderLevels();
    const [only] = screen.getAllByRole("button", {
      name: m.levelManager.deleteLast,
    });
    expect(only).toHaveAttribute("aria-disabled", "true");
    expect(
      within(document.body).queryAllByRole("button", { name: m.delete }),
    ).toHaveLength(0);
  });
});
