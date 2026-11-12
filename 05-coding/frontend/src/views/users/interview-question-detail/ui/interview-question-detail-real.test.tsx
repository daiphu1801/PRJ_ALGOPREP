// USR0402 against the REAL api path (F6 #3 to #7): only the HTTP layer is faked, so the real modules,
// the cache patching, the error-code handling and the real vi texts are what is asserted.
import {
  act,
  fireEvent,
  render,
  renderHook,
  screen,
  waitFor,
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
vi.mock("next/navigation", () => ({ useRouter: () => ({ push: vi.fn() }) }));

// Import after the mocks.
import { apiMock, resetApiMock } from "@/shared/test/real-api";
import { InterviewQuestionDetailView } from "./interview-question-detail-view";

const m = viMessages.interviewQuestionDetail;
const errors = viMessages.apiErrors;
const Q = "/interview-bank/questions/q1";
const ATTEMPTS = `${Q}/practice-answers`;

const detail = (over: Record<string, unknown> = {}) => ({
  id: "q1",
  topic: { code: "BEHAVIORAL", displayName: "Hành vi", usesStarFramework: true },
  level: { code: "EASY", displayName: "Dễ" },
  title: "Kể về một lần xung đột trong nhóm",
  contentMarkdown: "Nội dung câu hỏi",
  suggestedApproach: "- Nêu bối cảnh\n- Nêu kết quả",
  sampleAnswerFramework: "S, T, A, R",
  coreKeywords: ["Giao tiếp"],
  followUpQuestions: ["Bạn học được gì?"],
  bookmarked: false,
  practiceAvailable: true,
  recall: null,
  ...over,
});

const attempt = (no: number, over: Record<string, unknown> = {}) => ({
  id: `a${no}`,
  attemptNo: no,
  createdAt: "2026-11-10T03:00:00Z",
  answerText: `Câu trả lời lượt ${no}`,
  feedbackStatus: "COMPLETED",
  feedback: {
    achievedPoints: ["Nêu đúng bối cảnh"],
    missingPoints: ["Thiếu kết quả"],
    improvementSuggestions: ["Thêm số liệu"],
    criteria: [
      { criterionCode: "Bối cảnh", achievementPercent: 80, weightPercent: 50 },
    ],
  },
  feedbackErrorCode: null,
  ...over,
});

type Params = Record<string, unknown> | undefined;
type Handler = (params: Params) => unknown;

function serve(over: Record<string, Handler> = {}) {
  const handlers: Record<string, Handler> = {
    "/interview-bank/topics": () => ({ items: [] }),
    "/interview-bank/levels": () => ({ items: [] }),
    [Q]: () => detail(),
    [ATTEMPTS]: () => ({ items: [], hasMore: false, totalAttempts: 0 }),
    ...over,
  };
  apiMock.get.mockImplementation(async (path: string, params: Params) => {
    const handler = handlers[path];
    if (!handler) throw new Error(`unexpected GET ${path}`);
    const value = handler(params);
    if (value instanceof Error) throw value;
    return value;
  });
}

const toastsOf = () => renderHook(() => useToasts()).result;
const lastToast = (toasts: { current: readonly { message: string }[] }) =>
  toasts.current.at(-1)?.message;
const attemptCalls = () =>
  apiMock.get.mock.calls.filter(([path]) => path === ATTEMPTS);

async function renderLoaded(over: Record<string, Handler> = {}) {
  serve(over);
  render(withTestProviders(<InterviewQuestionDetailView questionId="q1" />));
  await screen.findByText("Kể về một lần xung đột trong nhóm");
}

async function toPractice() {
  fireEvent.click(screen.getByText(m.mode.practice));
  return screen.findByLabelText(m.practice.answerLabel);
}

beforeEach(() => {
  resetApiMock();
  act(() => toast.clear());
});

describe("InterviewQuestionDetailView with the real API", () => {
  describe("study mode", () => {
    it("shows the outline split into items, the framework with the STAR tag, follow-ups and keywords", async () => {
      await renderLoaded();
      expect(apiMock.get).toHaveBeenCalledWith(Q);
      expect(screen.getByText("Nêu bối cảnh")).toBeInTheDocument();
      expect(screen.getByText("Nêu kết quả")).toBeInTheDocument();
      expect(screen.getByText("S, T, A, R")).toBeInTheDocument();
      expect(screen.getByText(m.study.starTag)).toBeInTheDocument();
      expect(screen.getByText("Bạn học được gì?")).toBeInTheDocument();
      expect(screen.getByText("Giao tiếp")).toBeInTheDocument();
      expect(await screen.findByText(m.attempts.emptyState)).toBeInTheDocument();
    });

    it("shows the review date of the saved rating, counted from the server's date", async () => {
      await renderLoaded({
        [Q]: () =>
          detail({
            recall: {
              rating: "VAGUE",
              intervalDays: 3,
              nextReviewAt: new Date(Date.now() + 2 * 86_400_000 + 3_600_000).toISOString(),
              ratedAt: new Date().toISOString(),
            },
          }),
      });
      expect(await screen.findByText("Sẽ nhắc ôn lại sau 3 ngày")).toBeInTheDocument();
    });

    it("says the review is due when the date is past", async () => {
      await renderLoaded({
        [Q]: () =>
          detail({
            recall: {
              rating: "KNOWN",
              intervalDays: 7,
              nextReviewAt: new Date(Date.now() - 86_400_000).toISOString(),
              ratedAt: "2026-11-01T00:00:00Z",
            },
          }),
      });
      expect(await screen.findByText(m.study.nextReviewDue)).toBeInTheDocument();
    });

    it("shows the not-found state when the server answers INB-010", async () => {
      serve({ [Q]: () => new ApiError("INB-010", 404) });
      render(withTestProviders(<InterviewQuestionDetailView questionId="q1" />));
      expect(await screen.findByText(m.notFound)).toBeInTheDocument();
      // No history call for a question that does not exist (it used to hit `/questions/practice-answers`).
      expect(attemptCalls()).toHaveLength(0);
      expect(
        apiMock.get.mock.calls.some(([path]) => String(path).endsWith("/practice-answers")),
      ).toBe(false);
      expect(screen.getByRole("link", { name: m.header.linkBack })).toHaveAttribute(
        "href",
        "/interview-bank",
      );
    });

    it("shows an error with a retry for any other failure, and recovers", async () => {
      let fail = true;
      serve({ [Q]: () => (fail ? new ApiError("UNKNOWN_ERROR", 500) : detail()) });
      render(withTestProviders(<InterviewQuestionDetailView questionId="q1" />));
      const retry = await screen.findByRole("button", { name: viMessages.common.retry });
      fail = false;
      fireEvent.click(retry);
      expect(await screen.findByText("Kể về một lần xung đột trong nhóm")).toBeInTheDocument();
    });

    it("rates with the API rating, then shows the new date and a success toast", async () => {
      await renderLoaded();
      const toasts = toastsOf();
      apiMock.put.mockResolvedValue({
        rating: "FORGOTTEN",
        intervalDays: 1,
        nextReviewAt: new Date(Date.now() + 86_400_000).toISOString(),
        ratedAt: new Date().toISOString(),
      });
      fireEvent.click(screen.getByRole("button", { name: m.study.recall.forgotten }));
      await waitFor(() =>
        expect(apiMock.put).toHaveBeenCalledWith(`${Q}/recall`, { rating: "FORGOTTEN" }),
      );
      expect(await screen.findByText("Sẽ nhắc ôn lại sau 1 ngày")).toBeInTheDocument();
      expect(lastToast(toasts)).toBe(m.toast.rated);
    });

    it.each([
      ["INB-V04", 400],
      ["INB-010", 404],
      ["UNKNOWN_ERROR", 500],
    ])("shows the translated text of %s when rating fails and keeps the old state", async (code, status) => {
      await renderLoaded();
      const toasts = toastsOf();
      apiMock.put.mockRejectedValue(new ApiError(code, status));
      fireEvent.click(screen.getByRole("button", { name: m.study.recall.known }));
      await waitFor(() =>
        expect(lastToast(toasts)).toBe(errors[code as keyof typeof errors]),
      );
      expect(screen.queryByText(/Sẽ nhắc ôn lại/)).not.toBeInTheDocument();
    });

    it("flips the bookmark at once and sends the wanted state", async () => {
      await renderLoaded();
      const toasts = toastsOf();
      apiMock.put.mockResolvedValue({ questionId: "q1", bookmarked: true });
      fireEvent.click(screen.getByRole("button", { name: m.header.bookmark }));
      expect(
        await screen.findByRole("button", { name: m.header.bookmarked }),
      ).toHaveAttribute("aria-pressed", "true");
      expect(apiMock.put).toHaveBeenCalledWith(`${Q}/bookmark`, { bookmarked: true });
      await waitFor(() => expect(lastToast(toasts)).toBe(m.toast.bookmarked));
    });

    it.each([
      ["INB-V05", 400],
      ["INB-010", 404],
    ])("rolls the bookmark back and shows the text of %s", async (code, status) => {
      await renderLoaded();
      const toasts = toastsOf();
      apiMock.put.mockRejectedValue(new ApiError(code, status));
      fireEvent.click(screen.getByRole("button", { name: m.header.bookmark }));
      await waitFor(() =>
        expect(lastToast(toasts)).toBe(errors[code as keyof typeof errors]),
      );
      // INB-010 empties the cached question (not found); otherwise the label is back.
      if (code !== "INB-010")
        expect(screen.getByRole("button", { name: m.header.bookmark })).toHaveAttribute(
          "aria-pressed",
          "false",
        );
    });
  });

  describe("practice mode", () => {
    it("stays locked, with the reason, while the question has no rubric", async () => {
      await renderLoaded({ [Q]: () => detail({ practiceAvailable: false }) });
      const toasts = toastsOf();
      fireEvent.click(screen.getByText(m.mode.practice));
      expect(lastToast(toasts)).toBe(m.mode.practiceLockedReason);
      expect(screen.queryByLabelText(m.practice.answerLabel)).not.toBeInTheDocument();
    });

    it("posts the answer as data, shows the feedback, then refreshes the history", async () => {
      await renderLoaded();
      const toasts = toastsOf();
      apiMock.post.mockResolvedValue(attempt(1));
      const box = await toPractice();
      fireEvent.change(box, { target: { value: "  Trả lời của tôi  " } });
      const callsBefore = attemptCalls().length;
      serve({
        [ATTEMPTS]: () => ({ items: [attempt(1)], hasMore: false, totalAttempts: 1 }),
      });
      fireEvent.click(screen.getByRole("button", { name: m.practice.btnSubmit }));

      await waitFor(() =>
        expect(apiMock.post).toHaveBeenCalledWith(ATTEMPTS, { answerText: "Trả lời của tôi" }),
      );
      expect(await screen.findByText("Nêu đúng bối cảnh")).toBeInTheDocument();
      expect(screen.getByText("Thiếu kết quả")).toBeInTheDocument();
      expect(screen.getByText("Thêm số liệu")).toBeInTheDocument();
      expect(screen.getByText("80% · trọng số 50%")).toBeInTheDocument();
      expect(lastToast(toasts)).toBe(m.toast.answerSubmitted);
      // The composer is read-only on a shown attempt; "retry" empties it again.
      expect(screen.getByLabelText(m.practice.answerLabel)).toBeDisabled();
      // The history was read again and lists the new attempt.
      await waitFor(() => expect(attemptCalls().length).toBeGreaterThan(callsBefore));
      expect(await screen.findByText("Lượt 1")).toBeInTheDocument();
      fireEvent.click(screen.getByRole("button", { name: m.practice.btnRetry }));
      expect(screen.getByLabelText(m.practice.answerLabel)).toHaveValue("");
    });

    it.each(["INB-501", "INB-502"])(
      "shows the AI-unavailable notice for %s, the answer is kept as an attempt, no error toast",
      async (code) => {
        await renderLoaded();
        const toasts = toastsOf();
        apiMock.post.mockResolvedValue(
          attempt(1, { feedbackStatus: "FAILED", feedback: null, feedbackErrorCode: code }),
        );
        fireEvent.change(await toPractice(), { target: { value: "Một câu trả lời" } });
        fireEvent.click(screen.getByRole("button", { name: m.practice.btnSubmit }));

        // The notice sits in the feedback area; the toast carries the same text.
        await waitFor(() => expect(lastToast(toasts)).toBe(m.practice.aiUnavailable));
        expect((await screen.findAllByText(m.practice.aiUnavailable)).length).toBeGreaterThan(0);
        expect(toasts.current.some((item) => item.tone === "error")).toBe(false);
        expect(errors[code as keyof typeof errors]).toBeTruthy();
      },
    );

    it.each([
      ["INB-V06", 400],
      ["INB-V07", 400],
      ["INB-010", 404],
      ["INB-040", 422],
      ["INB-041", 429],
      ["INB-042", 429],
      ["INB-050", 409],
      ["UNKNOWN_ERROR", 500],
    ])("shows the translated text of %s and keeps the draft editable", async (code, status) => {
      await renderLoaded();
      const toasts = toastsOf();
      apiMock.post.mockRejectedValue(new ApiError(code, status));
      fireEvent.change(await toPractice(), { target: { value: "Bản nháp của tôi" } });
      fireEvent.click(screen.getByRole("button", { name: m.practice.btnSubmit }));

      await waitFor(() =>
        expect(lastToast(toasts)).toBe(errors[code as keyof typeof errors]),
      );
      const box = screen.getByLabelText(m.practice.answerLabel);
      expect(box).not.toBeDisabled();
      expect(box).toHaveValue("Bản nháp của tôi");
    });

    it("sends nothing for an empty answer", async () => {
      await renderLoaded();
      await toPractice();
      expect(screen.getByRole("button", { name: m.practice.btnSubmit })).toBeDisabled();
      expect(apiMock.post).not.toHaveBeenCalled();
    });
  });

  describe("attempt history", () => {
    const tenAttempts = Array.from({ length: 10 }, (_, i) => attempt(12 - i));

    it("loads 10 at a time with the `before` cursor and shows the running total", async () => {
      await renderLoaded({
        [ATTEMPTS]: (params) =>
          params?.before === 3
            ? { items: [attempt(2), attempt(1)], hasMore: false, totalAttempts: 12 }
            : { items: tenAttempts, hasMore: true, totalAttempts: 12 },
      });
      expect(await screen.findByText("Lượt 12")).toBeInTheDocument();
      expect(attemptCalls()[0]).toEqual([ATTEMPTS, undefined]);

      fireEvent.click(screen.getByRole("button", { name: "Xem thêm (10 / 12)" }));
      expect(await screen.findByText("Lượt 1")).toBeInTheDocument();
      expect(attemptCalls().at(-1)).toEqual([ATTEMPTS, { before: 3 }]);
      expect(screen.queryByRole("button", { name: /^Xem thêm/ })).not.toBeInTheDocument();
    });

    it("expands an attempt to show the answer text, and labels a failed attempt", async () => {
      await renderLoaded({
        [ATTEMPTS]: () => ({
          items: [attempt(2, { feedbackStatus: "FAILED", feedback: null }), attempt(1)],
          hasMore: false,
          totalAttempts: 2,
        }),
      });
      expect(await screen.findByText(m.attempts.status.failed)).toBeInTheDocument();
      expect(screen.getByText(m.attempts.status.completed)).toBeInTheDocument();
      fireEvent.click(screen.getByRole("button", { name: /Lượt 1/ }));
      expect(screen.getByText("Câu trả lời lượt 1")).toBeInTheDocument();
    });

    it("opens an older COMPLETED attempt with its own feedback and per-criterion levels (EVT-9)", async () => {
      await renderLoaded({
        [ATTEMPTS]: () => ({
          items: [
            attempt(2, {
              feedback: {
                achievedPoints: ["Điểm đạt của lượt hai"],
                missingPoints: ["Điểm thiếu của lượt hai"],
                improvementSuggestions: ["Gợi ý của lượt hai"],
                criteria: [
                  { criterionCode: "Tiêu chí hai", achievementPercent: 40, weightPercent: 60 },
                ],
              },
            }),
            attempt(1),
          ],
          hasMore: false,
          totalAttempts: 2,
        }),
      });
      fireEvent.click(await screen.findByRole("button", { name: /Lượt 1/ }));
      expect(screen.getByText("Câu trả lời lượt 1")).toBeInTheDocument();
      expect(screen.getByText("Nêu đúng bối cảnh")).toBeInTheDocument();
      expect(screen.getByText("Thiếu kết quả")).toBeInTheDocument();
      expect(screen.getByText("Thêm số liệu")).toBeInTheDocument();
      expect(screen.getByText("Bối cảnh")).toBeInTheDocument();
      expect(screen.getByText("80% · trọng số 50%")).toBeInTheDocument();
      // No way back to Study from a history row, and the other attempt's feedback stays closed.
      expect(screen.queryByText("Điểm đạt của lượt hai")).not.toBeInTheDocument();
      expect(screen.queryByRole("button", { name: m.practice.linkBackToStudy })).not.toBeInTheDocument();
      fireEvent.click(screen.getByRole("button", { name: /Lượt 2/ }));
      expect(screen.getByText("Điểm đạt của lượt hai")).toBeInTheDocument();
      expect(screen.getByText("40% · trọng số 60%")).toBeInTheDocument();
    });

    it("opens an older FAILED attempt with the answer and the AI-unavailable notice, no criteria (EVT-9)", async () => {
      await renderLoaded({
        [ATTEMPTS]: () => ({
          items: [attempt(1, { feedbackStatus: "FAILED", feedback: null })],
          hasMore: false,
          totalAttempts: 1,
        }),
      });
      fireEvent.click(await screen.findByRole("button", { name: /Lượt 1/ }));
      expect(screen.getByText("Câu trả lời lượt 1")).toBeInTheDocument();
      expect(screen.getByText(m.practice.aiUnavailable)).toBeInTheDocument();
      expect(screen.queryByText(m.practice.criteriaLabel)).not.toBeInTheDocument();
      expect(screen.queryByText(m.practice.strengthsLabel)).not.toBeInTheDocument();
    });

    it("shows an error with a retry when the history cannot be read, the study content stays", async () => {
      await renderLoaded({ [ATTEMPTS]: () => new ApiError("INB-V08", 400) });
      expect(await screen.findByRole("button", { name: viMessages.common.retry })).toBeInTheDocument();
      expect(screen.getByText("Nêu bối cảnh")).toBeInTheDocument();
      expect(errors["INB-V08"]).toBeTruthy();
    });
  });
});
