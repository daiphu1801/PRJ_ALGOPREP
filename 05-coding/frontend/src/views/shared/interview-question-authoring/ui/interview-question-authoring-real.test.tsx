// SHR0302 against the REAL api path (F6 #20, #21, #22, #23): only the HTTP layer is faked, so the real
// modules, the error mapping and the real vi texts are what the assertions read. Mock-data behaviour
// stays in interview-question-authoring-view.test.tsx.
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
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import viMessages from "../../../../../messages/vi.json";
import { ApiError } from "@/shared/api";
import { NextIntlClientProvider } from "@/shared/i18n";
import { toast, useToasts } from "@/shared/lib/toast-store";

vi.mock("@/shared/api", async (importOriginal) =>
  (await import("@/shared/test/real-api")).realApiModule(
    await importOriginal<Record<string, unknown>>(),
  ),
);
vi.mock("@/shared/api/mock-mode", () => ({
  isMockMode: () => false,
  withMockData: <T,>(_mock: () => unknown, real: () => Promise<T>) => real(),
}));
const router = vi.hoisted(() => ({ push: vi.fn(), replace: vi.fn() }));
vi.mock("next/navigation", () => ({ useRouter: () => router }));
// The topic and level lists belong to another slice; this screen only reads them.
vi.mock("@/entities/interview-question", async (importOriginal) => ({
  ...(await importOriginal<Record<string, unknown>>()),
  useInterviewTopics: () => [
    { id: "t-1", key: "NETWORK", label: "Mạng", usesStarFramework: false },
    {
      id: "t-2",
      key: "CS_THEORY",
      label: "Lý thuyết CS",
      usesStarFramework: false,
    },
  ],
  useInterviewLevels: () => [
    { id: "l-1", key: "EASY", label: "Dễ", tone: "success" },
    { id: "l-2", key: "MEDIUM", label: "Trung bình", tone: "warn" },
  ],
}));

import { apiMock, resetApiMock } from "@/shared/test/real-api";
import { InterviewQuestionAuthoringView } from "./interview-question-authoring-view";

const m = viMessages.interviewQuestionAuthoring;
const apiErrors = viMessages.apiErrors as Record<string, string>;
const BASE = "/interview-bank/admin/questions";
const LIST = "/admin/interview-questions";
const Q = "11111111-1111-4111-8111-111111111111";
const NEW_ID = "99999999-9999-4999-8999-999999999999";

const detail = (over: Record<string, unknown> = {}) => ({
  id: Q,
  displayNo: 14,
  topicId: "t-2",
  levelId: "l-2",
  title: "Hash table xử lý collision thế nào?",
  contentMarkdown: "Mô tả thêm giữ nguyên",
  followUpQuestions: ["Load factor?"],
  rubric: [
    { criterionCode: "Định nghĩa", description: "mô tả A", weightPercent: 40 },
    { criterionCode: "So sánh", description: null, weightPercent: 60 },
  ],
  status: "ACTIVE",
  practiceAvailable: true,
  usage: { usageCount: 3, averageScorePercent: null },
  createdAt: "2026-11-01T00:00:00Z",
  updatedAt: "2026-11-02T00:00:00Z",
  ...over,
});

function watch() {
  const toasts = renderHook(() => useToasts());
  act(() => toast.clear());
  return () => toasts.result.current.map((item) => [item.tone, item.message]);
}

let client: QueryClient;
function mount(questionId: string) {
  client = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  return render(
    <QueryClientProvider client={client}>
      <NextIntlClientProvider locale="vi" messages={viMessages}>
        <InterviewQuestionAuthoringView
          questionId={questionId}
          listHref={LIST}
        />
      </NextIntlClientProvider>
    </QueryClientProvider>,
  );
}

async function openEdit() {
  mount(Q);
  await screen.findByLabelText(m.questionLabel);
}

const save = () =>
  fireEvent.click(screen.getByRole("button", { name: m.save }));

describe("InterviewQuestionAuthoringView with the real API", () => {
  beforeEach(() => {
    resetApiMock();
    router.push.mockReset();
    router.replace.mockReset();
    apiMock.get.mockResolvedValue(detail());
  });

  describe("edit", () => {
    it("loads by UUID, titles the page with the IQ label and fills the form from the detail", async () => {
      await openEdit();
      expect(apiMock.get).toHaveBeenCalledWith(`${BASE}/${Q}`);
      expect(
        screen.getByText(m.titleEdit.replace("{code}", "IQ-014")),
      ).toBeInTheDocument();
      expect(screen.getByLabelText(m.questionLabel)).toHaveValue(
        "Hash table xử lý collision thế nào?",
      );
      expect(screen.getByLabelText(m.topicLabel)).toHaveValue("t-2");
      expect(screen.getByLabelText(m.levelLabel)).toHaveValue("l-2");
    });

    it("saves with PUT and sends back the content and descriptions the form has no input for", async () => {
      apiMock.put.mockResolvedValue(detail({ title: "Tiêu đề mới" }));
      const toasts = watch();
      await openEdit();
      fireEvent.change(screen.getByLabelText(m.questionLabel), {
        target: { value: "  Tiêu đề mới  " },
      });
      fireEvent.change(screen.getByLabelText(m.topicLabel), {
        target: { value: "t-1" },
      });
      save();
      await waitFor(() =>
        expect(apiMock.put).toHaveBeenCalledWith(`${BASE}/${Q}`, {
          topicId: "t-1",
          levelId: "l-2",
          title: "Tiêu đề mới",
          contentMarkdown: "Mô tả thêm giữ nguyên",
          followUpQuestions: ["Load factor?"],
          rubric: [
            {
              criterionCode: "Định nghĩa",
              weightPercent: 40,
              description: "mô tả A",
            },
            { criterionCode: "So sánh", weightPercent: 60, description: null },
          ],
        }),
      );
      await waitFor(() =>
        expect(toasts().at(-1)).toEqual(["success", m.toast.saved]),
      );
      // A successful save stays on the screen and does not navigate.
      expect(router.replace).not.toHaveBeenCalled();
    });

    it("invalidates the admin reads after a save", async () => {
      apiMock.put.mockResolvedValue(detail());
      await openEdit();
      const spy = vi.spyOn(client, "invalidateQueries");
      save();
      await waitFor(() =>
        expect(spy).toHaveBeenCalledWith({
          queryKey: ["admin-interview-question"],
        }),
      );
    });

    it("drops empty follow-ups before sending", async () => {
      apiMock.put.mockResolvedValue(detail());
      await openEdit();
      fireEvent.click(screen.getByRole("button", { name: m.addFollowUp }));
      save();
      await waitFor(() => expect(apiMock.put).toHaveBeenCalled());
      expect(apiMock.put.mock.calls[0]![1].followUpQuestions).toEqual([
        "Load factor?",
      ]);
    });

    it("shows the not-found state for 404 INB-010 and does not offer the form", async () => {
      apiMock.get.mockRejectedValue(new ApiError("INB-010", 404));
      mount(Q);
      expect(
        await screen.findByText(m.notFoundBody.replace("{code}", Q)),
      ).toBeInTheDocument();
      expect(screen.queryByLabelText(m.questionLabel)).toBeNull();
    });

    it.each([
      ["INB-V08", 400],
      ["INB-V09", 400],
      ["INB-V10", 400],
      ["INB-V11", 400],
      ["INB-V12", 400],
      ["INB-301", 403],
      ["INB-010", 404],
      ["INB-V17", 413],
      ["INB-033", 422],
      ["INB-035", 422],
      ["INB-503", 503],
    ])(
      "toasts the text of %s, keeps the draft and the form",
      async (code, status) => {
        apiMock.put.mockRejectedValue(new ApiError(code, status));
        const toasts = watch();
        await openEdit();
        fireEvent.change(screen.getByLabelText(m.questionLabel), {
          target: { value: "Bản nháp chưa lưu" },
        });
        save();
        await waitFor(() =>
          expect(toasts().at(-1)).toEqual(["error", apiErrors[code]]),
        );
        expect(screen.getByLabelText(m.questionLabel)).toHaveValue(
          "Bản nháp chưa lưu",
        );
        expect(screen.getByRole("button", { name: m.save })).toBeEnabled();
      },
    );

    it("an unknown failure shows the generic text", async () => {
      apiMock.put.mockRejectedValue(new Error("boom"));
      const toasts = watch();
      await openEdit();
      save();
      await waitFor(() =>
        expect(toasts().at(-1)).toEqual(["error", apiErrors.UNKNOWN_ERROR]),
      );
    });
  });

  describe("client checks send nothing", () => {
    it("blocks an empty title", async () => {
      const toasts = watch();
      await openEdit();
      fireEvent.change(screen.getByLabelText(m.questionLabel), {
        target: { value: "   " },
      });
      save();
      expect(toasts().at(-1)).toEqual(["warning", m.saveBlocked.empty]);
      expect(apiMock.put).not.toHaveBeenCalled();
    });

    it("blocks a title over 255 characters and accepts exactly 255", async () => {
      apiMock.put.mockResolvedValue(detail());
      const toasts = watch();
      await openEdit();
      fireEvent.change(screen.getByLabelText(m.questionLabel), {
        target: { value: "a".repeat(256) },
      });
      save();
      expect(toasts().at(-1)).toEqual([
        "warning",
        m.saveBlocked.titleTooLong.replace("{max}", "255"),
      ]);
      expect(apiMock.put).not.toHaveBeenCalled();
      fireEvent.change(screen.getByLabelText(m.questionLabel), {
        target: { value: "a".repeat(255) },
      });
      save();
      await waitFor(() => expect(apiMock.put).toHaveBeenCalledTimes(1));
    });

    it("blocks a rubric that does not total 100", async () => {
      const toasts = watch();
      await openEdit();
      fireEvent.click(
        screen.getByRole("button", {
          name: m.criterionWeightIncrement.replace("{index}", "1"),
        }),
      );
      save();
      expect(toasts().at(-1)![0]).toBe("warning");
      expect(toasts().at(-1)![1]).toContain(m.rubricBlockTitle);
      expect(apiMock.put).not.toHaveBeenCalled();
    });

    it("blocks a blank criterion name and a duplicate one (case-insensitive)", async () => {
      const toasts = watch();
      await openEdit();
      fireEvent.change(
        screen.getByLabelText(m.criterionLabel.replace("{index}", "1")),
        {
          target: { value: "  " },
        },
      );
      save();
      expect(toasts().at(-1)).toEqual([
        "warning",
        m.saveBlocked.criterionBlank,
      ]);
      fireEvent.change(
        screen.getByLabelText(m.criterionLabel.replace("{index}", "1")),
        {
          target: { value: "so sánh" },
        },
      );
      save();
      expect(toasts().at(-1)).toEqual([
        "warning",
        m.saveBlocked.criterionDuplicate,
      ]);
      expect(apiMock.put).not.toHaveBeenCalled();
    });
  });

  describe("create", () => {
    it("posts the first topic and level by default, then opens the edit route of the new UUID", async () => {
      apiMock.post.mockResolvedValue(detail({ id: NEW_ID }));
      const toasts = watch();
      mount("new");
      fireEvent.change(screen.getByLabelText(m.questionLabel), {
        target: { value: "Câu hỏi mới" },
      });
      save();
      await waitFor(() =>
        expect(apiMock.post).toHaveBeenCalledWith(BASE, {
          topicId: "t-1",
          levelId: "l-1",
          title: "Câu hỏi mới",
          contentMarkdown: null,
          followUpQuestions: [],
          rubric: [],
        }),
      );
      await waitFor(() =>
        expect(router.replace).toHaveBeenCalledWith(`${LIST}/${NEW_ID}/edit`),
      );
      expect(toasts().at(-1)).toEqual(["success", m.toast.saved]);
      expect(apiMock.get).not.toHaveBeenCalled();
    });

    it.each([
      ["INB-V08", 400],
      ["INB-V09", 400],
      ["INB-V10", 400],
      ["INB-V11", 400],
      ["INB-V12", 400],
      ["INB-V13", 400],
      ["INB-301", 403],
      ["INB-V17", 413],
      ["INB-033", 422],
      ["INB-035", 422],
      ["INB-503", 503],
    ])(
      "toasts the text of %s and stays on the new form",
      async (code, status) => {
        apiMock.post.mockRejectedValue(new ApiError(code, status));
        const toasts = watch();
        mount("new");
        fireEvent.change(screen.getByLabelText(m.questionLabel), {
          target: { value: "Câu hỏi mới" },
        });
        save();
        await waitFor(() =>
          expect(toasts().at(-1)).toEqual(["error", apiErrors[code]]),
        );
        expect(router.replace).not.toHaveBeenCalled();
        expect(screen.getByLabelText(m.questionLabel)).toHaveValue(
          "Câu hỏi mới",
        );
      },
    );
  });

  describe("soft delete", () => {
    async function confirm() {
      await openEdit();
      fireEvent.click(screen.getByRole("button", { name: m.softDelete }));
      fireEvent.click(
        within(screen.getByRole("dialog")).getByRole("button", {
          name: m.softDelete,
        }),
      );
    }

    it("retires the one UUID, toasts and returns to the list", async () => {
      apiMock.post.mockResolvedValue({ retiredIds: [Q], failedItems: [] });
      const toasts = watch();
      await confirm();
      await waitFor(() =>
        expect(apiMock.post).toHaveBeenCalledWith(`${BASE}/bulk/delete`, {
          ids: [Q],
        }),
      );
      await waitFor(() => expect(router.push).toHaveBeenCalledWith(LIST));
      expect(toasts().at(-1)).toEqual(["success", m.toast.softDeleted]);
    });

    it("a failed item in a 200 answer shows its code and stays on the screen", async () => {
      apiMock.post.mockResolvedValue({
        retiredIds: [],
        failedItems: [{ id: Q, errorCode: "INB-010" }],
      });
      const toasts = watch();
      await confirm();
      await waitFor(() =>
        expect(toasts().at(-1)).toEqual(["error", apiErrors["INB-010"]]),
      );
      expect(router.push).not.toHaveBeenCalled();
    });

    it.each([
      ["INB-301", 403],
      ["INB-V02", 400],
      ["INB-503", 503],
    ])("toasts the text of %s", async (code, status) => {
      apiMock.post.mockRejectedValue(new ApiError(code, status));
      const toasts = watch();
      await confirm();
      await waitFor(() =>
        expect(toasts().at(-1)).toEqual(["error", apiErrors[code]]),
      );
      expect(router.push).not.toHaveBeenCalled();
    });
  });
});
