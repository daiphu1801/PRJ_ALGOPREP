// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md section 10.2 (USR0302_mock_interview).
//
// F5-14 says the AI streams a real reply over SSE, and F5-10..F5-12 say it decides the stage
// transition from conversation context. Neither exists yet, so this prototype scripts a fixed
// question bank per stage instead — deterministic (no Math.random), same script every session, so
// "AI decides" is faked as "AI reads a script index computed from the turn number". Real streaming
// is simulated in the view with setInterval revealing characters, not a network call.
import type {
  AcceptedSubmissionOption,
  BankQuestionOption,
  EntryStats,
  InterviewResult,
  InterviewStage,
  RubricCriterionCode,
} from "../../model/types";
import { RUBRIC_WEIGHTS } from "../../model/types";

export function fetchEntryStats(): EntryStats {
  return {
    sessionsCompleted: 7,
    averageScore: 6.8,
    weakestCriterion: "pushbackHandling",
  };
}

export const ACCEPTED_SUBMISSIONS: AcceptedSubmissionOption[] = [
  { id: "SUB-2841", problemTitle: "1. Two Sum", language: "Python 3" },
  {
    id: "SUB-2799",
    problemTitle: "23. Merge k Sorted Lists",
    language: "Java 21",
  },
  { id: "SUB-2712", problemTitle: "127. Word Ladder", language: "C++ 17" },
];

export const BANK_QUESTIONS: BankQuestionOption[] = [
  { id: "IQ-014", title: "Thiết kế LRU Cache" },
  { id: "IQ-031", title: "Cân bằng tải round-robin có trọng số" },
  { id: "IQ-058", title: "Phát hiện chu trình trong đồ thị có hướng" },
];

const STAGE_ORDER: InterviewStage[] = ["explain", "challenge", "scaleUp"];

const QUESTION_BANK: Record<InterviewStage, string[]> = {
  explain: [
    "Bạn hãy trình bày hướng giải của mình theo từng bước, trước khi đi vào chi tiết cài đặt.",
    "Vì sao bạn chọn cấu trúc dữ liệu này thay vì một lựa chọn đơn giản hơn?",
  ],
  challenge: [
    "Nếu đầu vào có phần tử trùng lặp, giải pháp của bạn còn đúng không? Chứng minh giúp tôi.",
    "Có trường hợp biên nào (mảng rỗng, một phần tử, giá trị âm) khiến mã của bạn sai không?",
  ],
  scaleUp: [
    "Nếu dữ liệu đầu vào lớn tới mức không thể nạp hết vào bộ nhớ, bạn sẽ thay đổi thiết kế thế nào?",
    "Giả sử hệ thống cần xử lý hàng nghìn yêu cầu như thế này mỗi giây, điểm nghẽn ở đâu?",
  ],
};

/** Deterministic scripted question for a given 0-based turn index. */
export function getScriptedQuestion(
  turnIndex: number,
  maxTurns: number,
): { stage: InterviewStage; content: string } {
  const stageIndex = Math.min(
    STAGE_ORDER.length - 1,
    Math.floor((turnIndex / maxTurns) * STAGE_ORDER.length),
  );
  const stage = STAGE_ORDER[stageIndex] ?? "explain";
  const bank = QUESTION_BANK[stage];
  return { stage, content: bank[turnIndex % bank.length] ?? bank[0]! };
}

export const HINT_TEXT =
  "Gợi ý: thử nghĩ về việc đánh đổi giữa thời gian tra cứu và bộ nhớ sử dụng thêm.";

const RESULT_COMMENTS: Record<RubricCriterionCode, string> = {
  clarity:
    "Trình bày các bước rõ ràng, có dẫn dắt trước khi vào chi tiết cài đặt.",
  technicalAccuracy:
    "Nhận diện đúng độ phức tạp và không có lỗi suy luận kỹ thuật lớn.",
  pushbackHandling:
    "Còn lúng túng khi bị hỏi ngược về trường hợp biên, cần luyện thêm.",
  complexityAwareness:
    "Nhận ra điểm nghẽn khi mở rộng quy mô nhưng chưa đề xuất cụ thể.",
};

const RESULT_SCORES: Record<RubricCriterionCode, number> = {
  clarity: 8,
  technicalAccuracy: 7.5,
  pushbackHandling: 5.5,
  complexityAwareness: 6.5,
};

export function computeInterviewResult(): InterviewResult {
  const criteria = (Object.keys(RUBRIC_WEIGHTS) as RubricCriterionCode[]).map(
    (code) => ({
      code,
      score: RESULT_SCORES[code],
      comment: RESULT_COMMENTS[code],
    }),
  );
  const overallScore =
    Math.round(
      criteria.reduce(
        (sum, c) => sum + (c.score * RUBRIC_WEIGHTS[c.code]) / 100,
        0,
      ) * 10,
    ) / 10;

  return {
    overallScore,
    criteria,
    feedbackSummary:
      "Diễn giải thuật toán tốt và nắm chắc độ phức tạp, nhưng cần luyện phản xạ khi bị phản biện về trường hợp biên và đề xuất cụ thể hơn cho bài toán mở rộng quy mô.",
    strengths: [
      "Trình bày mạch lạc theo từng bước",
      "Nắm đúng độ phức tạp thời gian và bộ nhớ",
    ],
    improvements: [
      "Chuẩn bị sẵn phản biện cho các trường hợp biên trước khi trình bày",
      "Luyện tập đề xuất phương án cụ thể khi dữ liệu vượt quá bộ nhớ",
    ],
  };
}
