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
import type { SolutionReviewDetail } from "../../model/types";

export type SolutionReviewDemoOutcome = "ready" | "errorTransient" | "errorBudgetLocked";

export const AI_BUDGET_LOCKED_CODE = "AI_BUDGET_LOCKED";
export const AI_PROVIDER_TIMEOUT_CODE = "AI_PROVIDER_TIMEOUT";

const SOLUTION_REVIEW_MOCK: SolutionReviewDetail = {
  submissionId: "SUB-2841",
  problemTitle: "1. Two Sum",
  difficulty: "easy",
  approachTitle: "hash map",
  isApproachOptimal: true,
  readabilityScore: 4,
  createdAt: "2026-08-20T21:15:00+07:00",
  summaryText:
    "Hướng giải đã là hướng tối ưu cho bài này: một lượt duyệt với hash map, thời gian O(n). Thứ tự tra cứu trước rồi mới ghi vào dict là đúng, và đây chính là chỗ phần lớn bài nộp sai với đầu vào như [3,3]. Điểm còn lại không nằm ở thuật toán mà ở cách viết: ý nghĩa của biến need chưa nói rõ, hàm chưa có type hint, và câu return rỗng ở cuối làm mờ chi tiết là đề đã bảo đảm luôn có đáp án.",
  language: "Python 3",
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
    spaceExplanation: "Dict lớn theo đầu vào; không tránh được nếu không sắp xếp trước.",
  },
  criteriaScores: [
    { code: "correctness", feedback: "Đúng trên mọi testcase kể cả trùng giá trị như [3,3]." },
    { code: "performance", feedback: "Một lượt duyệt O(n), không có vòng lặp lồng." },
    { code: "cleanCode", feedback: "Dict `seen` chưa có type hint, tên biến `need` dùng một lần." },
    { code: "scalability", feedback: "Không cần sửa gì để chạy tốt với input lớn hơn." },
    { code: "dataStructure", feedback: "Hash map là lựa chọn đúng cho bài toán tra cứu bù trừ." },
  ],
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
  codeDiff: {
    diffNote: "4 dòng thay đổi · độ phức tạp không đổi",
    explanation:
      "Gộp bước tra cứu và tính hiệu số vào một lệnh .get(), thêm type hint cho dict để rõ ý nghĩa map giá trị → chỉ số.",
    diffLines: [
      { kind: "same", text: "def two_sum(nums: list[int], target: int) -> list[int]:" },
      { kind: "del", text: "    seen = {}" },
      { kind: "add", text: "    seen: dict[int, int] = {}" },
      { kind: "same", text: "    for i, x in enumerate(nums):" },
      { kind: "del", text: "        need = target - x" },
      { kind: "del", text: "        if need in seen:" },
      { kind: "del", text: "            return [seen[need], i]" },
      { kind: "add", text: "        j = seen.get(target - x)" },
      { kind: "add", text: "        if j is not None:" },
      { kind: "add", text: "            return [j, i]" },
      { kind: "same", text: "        seen[x] = i" },
      { kind: "same", text: "    return []" },
    ],
  },
};

function delay<T>(value: T, ms = 350): Promise<T> {
  return new Promise((resolve) => setTimeout(() => resolve(value), ms));
}

export async function fetchSolutionReviewMock(
  demo: SolutionReviewDemoOutcome,
): Promise<SolutionReviewDetail> {
  if (demo === "errorBudgetLocked") {
    throw new ApiError(AI_BUDGET_LOCKED_CODE, 429, "AI review budget exhausted for today");
  }
  if (demo === "errorTransient") {
    throw new ApiError(AI_PROVIDER_TIMEOUT_CODE, 503, "AI provider timed out");
  }
  return delay(SOLUTION_REVIEW_MOCK);
}
