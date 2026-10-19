// PROTOTYPE mock — no backend endpoint exists yet.
// Values from 09-layoutBase/Admin - Cấu hình AI.dc.html:418-470.
import type {
  AiConfigPage,
  PromptConfig,
  PromptVersionEntry,
} from "../../model/types";

// Declared by the dev in the backend config (BD ADM0301 Q14); the admin only chooses among them.
const MODEL_OPTIONS = ["claude-sonnet", "claude-haiku", "claude-opus"];

// dc.html:418-422.
const PROMPTS: PromptConfig[] = [
  {
    feature: "review",
    version: "v3.8",
    status: "live",
    modelId: "claude-sonnet",
    systemInstruction:
      "Bạn là trợ lý chấm bài giải thuật toán. Chỉ ra độ phức tạp và điểm cần sửa, trích đoạn mã liên quan. Không đưa lời giải đầy đủ.",
    maxTokens: "1.600 token",
    updatedAt: "11/08",
  },
  {
    feature: "interview",
    version: "v2.5",
    status: "live",
    modelId: "claude-sonnet",
    systemInstruction:
      "Bạn là người phỏng vấn kỹ thuật. Hỏi tiếp theo câu trả lời của ứng viên qua ba giai đoạn: Giải trình, Phản biện, Mở rộng quy mô; cuối phiên chấm theo rubric bốn tiêu chí.",
    maxTokens: "1.200 token",
    updatedAt: "19/08",
  },
  {
    feature: "testcase",
    version: "v1.9",
    status: "draft",
    modelId: "claude-haiku",
    systemInstruction:
      "Bạn viết script sinh dữ liệu testcase từ đề bài. Chỉ sinh input, không tự viết output; output chạy thật từ đáp án mẫu.",
    maxTokens: "2.000 token",
    updatedAt: "20/08",
  },
];

/**
 * The "Nhật ký phiên bản" modal has no data in the static mockup — it is a button with nothing
 * behind it. These rows are [SoT: Suy luận], derived from the prompt versions above so the modal
 * shows something coherent rather than an empty box. Whether the log is per feature or combined is
 * an open question (level C in 07-review/bd_screens_admin_open_questions_260913.md).
 */
const VERSION_HISTORY: PromptVersionEntry[] = [
  {
    id: "v-1",
    feature: "testcase",
    version: "v1.9",
    publishedAt: "20/08",
    systemInstruction:
      "Bạn viết script sinh dữ liệu testcase từ đề bài. Chỉ sinh input, không tự viết output; output chạy thật từ đáp án mẫu.",
    author: "pdai",
  },
  {
    id: "v-2",
    feature: "interview",
    version: "v2.5",
    publishedAt: "19/08",
    systemInstruction:
      "Bạn là người phỏng vấn kỹ thuật. Hỏi tiếp theo câu trả lời của ứng viên qua ba giai đoạn: Giải trình, Phản biện, Mở rộng quy mô; cuối phiên chấm theo rubric bốn tiêu chí.",
    author: "pdai",
  },
  {
    id: "v-3",
    feature: "interview",
    version: "v2.4",
    publishedAt: "12/08",
    systemInstruction:
      "Bạn là người phỏng vấn kỹ thuật. Hỏi tiếp theo câu trả lời của ứng viên, chấm cuối phiên.",
    author: "pdai",
  },
  {
    id: "v-4",
    feature: "review",
    version: "v3.8",
    publishedAt: "11/08",
    systemInstruction:
      "Bạn là trợ lý chấm bài giải thuật toán. Chỉ ra độ phức tạp và điểm cần sửa, trích đoạn mã liên quan. Không đưa lời giải đầy đủ.",
    author: "minhtri",
  },
  {
    id: "v-5",
    feature: "review",
    version: "v3.7",
    publishedAt: "02/08",
    systemInstruction:
      "Bạn là trợ lý chấm bài giải thuật toán. Nêu độ phức tạp và điểm cần sửa.",
    author: "pdai",
  },
];

export function fetchAiConfigPage(): AiConfigPage {
  return {
    modelOptions: MODEL_OPTIONS,
    prompts: PROMPTS.map((prompt) => ({ ...prompt })),
    // Per feature (D5, 2026-10-10); noFullSolution is locked on (D4).
    guards: {
      review: {
        noFullSolution: true,
        citeLines: true,
        answerInVietnamese: true,
      },
      interview: {
        noFullSolution: true,
        citeLines: false,
        answerInVietnamese: true,
      },
    },
    versionHistory: VERSION_HISTORY.map((entry) => ({ ...entry })),
    // dc.html:259.
  };
}
