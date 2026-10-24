// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md section 10.2 (USR0301_solution_review).
//
// Data content ported from 09-layoutBase/Phân tích bài giải.dc.html lines 288-320 (the "Two Sum /
// hash map" sample the mockup ships with), kept deterministic — no Math.random, no Date.now — so
// the screen renders identically every load and in tests.
//
// The three-state split (ready / errorTransient / errorBudgetLocked, BD Sheet 5 Khu vực I) has no
// real backend to trigger it from yet. Following the same idea the admin_overview debt row proposes
// ("bật cơ chế demo lỗi/rỗng thật qua query param debug", PROTOTYPE_DEBT.md section 9), the view
// reads a `?demo=` query param and this mock throws ApiError codes for the two failure branches —
// matching entities/admin-dashboard's withMockData + ApiError convention.
import { ApiError } from "@/shared/api";
import {
  AI_BUDGET_LOCKED_CODE,
  AI_PROVIDER_TIMEOUT_CODE,
  REVIEW_NOT_FOUND_CODE,
  type SolutionReviewDemoOutcome,
} from "../../model/review-errors";
import type { SolutionReviewDetail } from "../../model/types";

// Mock id check: any "SB-n" / "SUB-n" id has a review (the list mock hands out SB-*, the interview
// mock SUB-*); anything else is a 404. A real backend resolves ownership and status instead.
const KNOWN_ID = /^(SB|SUB)-\d+$/;

const SOLUTION_REVIEW_MOCK: SolutionReviewDetail = {
  submissionId: "SUB-2841",
  problemId: "1",
  languageKey: "python",
  submissionModel: "function",
  problemTitle: "1. Two Sum",
  difficulty: "EASY",
  schemaVersion: "1",
  approachTitle: "hash map",
  verdict: "OPTIMAL",
  isApproachOptimal: true,
  readabilityScore: 4,
  createdAt: "2026-08-20T21:15:00+07:00",
  sourceCode: [
    "def two_sum(nums, target):",
    "    seen = {}",
    "    for i, x in enumerate(nums):",
    "        need = target - x",
    "        if need in seen:",
    "            return [seen[need], i]",
    "        seen[x] = i",
    "    return []",
  ],
  complexity: {
    actualTime: "O(n)",
    optimalTime: "O(n)",
    timeExplanation: "Một lượt duyệt, tra cứu O(1) trung bình.",
    actualSpace: "O(n)",
    optimalSpace: "O(n)",
    spaceExplanation:
      "Dict lớn theo đầu vào; không tránh được nếu không sắp xếp trước.",
  },
  strengths: [
    "Tra cứu trước khi ghi vào dict, nên [3,3] với target 6 đúng mà không cần xử lý riêng.",
    "Chỉ một lượt duyệt, không có vòng lặp lồng và không quét mảng lần hai.",
    "Trả về chỉ số chứ không trả về giá trị, khớp đúng yêu cầu đầu ra.",
  ],
  improvements: [
    "Dict chưa có type hint nên người đọc không biết nó map giá trị → chỉ số.",
    "Biến need tính ra rồi chỉ dùng một lần; gộp vào một lệnh .get() sẽ bớt một cái tên không mang thêm thông tin.",
    "Câu return rỗng không bao giờ chạy tới theo ràng buộc của đề — nên ghi chú rõ hoặc raise thay vì trả về rỗng.",
  ],
  edgeCases: [
    "Mảng có phần tử trùng nhau (ví dụ [3,3], target 6) — đã xử lý đúng nhờ tra cứu trước khi ghi.",
    "Đề bài đảm bảo luôn có đúng một đáp án, nên nhánh return [] cuối hàm không bao giờ chạy tới.",
  ],
  followUpTopics: ["hash map", "two pointers", "3Sum"],
  codeDiff: {
    explanation:
      "Gộp bước tra cứu và tính hiệu số vào một lệnh .get(), thêm type hint cho dict để rõ ý nghĩa map giá trị → chỉ số.",
    suggestedCode: [
      "def two_sum(nums: list[int], target: int) -> list[int]:",
      "    seen: dict[int, int] = {}",
      "    for i, x in enumerate(nums):",
      "        j = seen.get(target - x)",
      "        if j is not None:",
      "            return [j, i]",
      "        seen[x] = i",
      "    return []",
      "",
    ].join("\n"),
  },
};

function delay<T>(value: T, ms = 350): Promise<T> {
  return new Promise((resolve) => setTimeout(() => resolve(value), ms));
}

export async function fetchSolutionReviewMock(
  demo: SolutionReviewDemoOutcome,
  submissionId: string,
): Promise<SolutionReviewDetail> {
  if (!KNOWN_ID.test(submissionId)) {
    throw new ApiError(REVIEW_NOT_FOUND_CODE, 404, "Submission not found");
  }
  if (demo === "errorBudgetLocked") {
    throw new ApiError(
      AI_BUDGET_LOCKED_CODE,
      429,
      "AI review budget exhausted for today",
    );
  }
  if (demo === "errorTransient") {
    throw new ApiError(AI_PROVIDER_TIMEOUT_CODE, 503, "AI provider timed out");
  }
  return delay({ ...SOLUTION_REVIEW_MOCK, submissionId });
}
