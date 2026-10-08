// PROTOTYPE mock — no backend endpoint exists yet.
// Questions from 09-layoutBase/Admin - Câu hỏi phỏng vấn.dc.html:381-409; the "Dùng N lần · điểm TB
// X/5" string there is split into two fields so the screen can format it per locale.
import type { QuestionDraft } from "../../model/csv-import";
import type {
  InterviewQuestion,
  InterviewQuestionPage,
} from "../../model/types";

// Student-facing fields below are [SoT: Suy luận] where noted — USR0402 has no prototype to
// reference (01-rd/screens/users/USR0402_interview_question_detail.md mục 1), so content is
// authored to satisfy F6-04/F6-05/F6-06, not copied from a mockup.
const QUESTIONS: InterviewQuestion[] = [
  {
    code: "IQ-014",
    topic: "csTheory",
    level: "MEDIUM",
    question:
      "Hash table xử lý collision bằng cách nào? So sánh chaining và open addressing.",
    followUps: [
      "Ngưỡng load factor nào thì cần resize?",
      "Vì sao open addressing thắng khi load factor thấp?",
    ],
    rubric: [
      { label: "Định nghĩa collision", weight: 25 },
      { label: "So sánh hai cơ chế", weight: 40 },
      { label: "Chốt bằng load factor", weight: 35 },
    ],
    usageCount: 184,
    averageScore: 3.8,
    content:
      "Khi hai khoá băm về cùng một chỉ số trong bảng băm, ta gọi đó là collision. Hãy giải thích " +
      "cách chaining và open addressing xử lý tình huống này, và khi nào bạn chọn cách nào.",
    suggestedApproach: [
      "Nêu định nghĩa collision và vì sao không tránh được với hàm băm hữu hạn.",
      "Chaining: mỗi bucket là danh sách liên kết, chi phí trung bình O(1+α).",
      "Open addressing: linear/quadratic probing, cache-friendly nhưng xấu khi load factor cao.",
      "Chốt bằng ngưỡng load factor và thời điểm resize.",
    ],
    coreKeywords: ["Hash map", "Load factor", "Chaining", "Open addressing"],
    sampleAnswerFramework:
      "Trình bày theo ba bước: (1) định nghĩa collision bằng một ví dụ cụ thể, (2) so sánh chaining " +
      "và open addressing trên hai trục chi phí bộ nhớ và locality, (3) chốt bằng ngưỡng load factor " +
      "thực tế (thường 0.7) và chiến lược resize gấp đôi.",
    hasRubric: true,
    defaultRecall: "vague",
    attempts: [],
  },
  {
    code: "IQ-021",
    topic: "csTheory",
    level: "EASY",
    question:
      "Khác biệt giữa process và thread? Khi nào chọn multiprocessing thay vì multithreading?",
    followUps: [
      "Chi phí context switch khác nhau ra sao?",
      "GIL trong Python ảnh hưởng thế nào tới lựa chọn?",
    ],
    rubric: [
      { label: "Phân biệt bộ nhớ", weight: 40 },
      { label: "Chi phí IPC và switch", weight: 30 },
      { label: "Gắn với ngôn ngữ cụ thể", weight: 30 },
    ],
    usageCount: 296,
    averageScore: 4.2,
    content:
      "So sánh process và thread về mặt bộ nhớ, chi phí chuyển ngữ cảnh, và cho một ví dụ cụ thể " +
      "khi bạn chọn multiprocessing thay vì multithreading.",
    suggestedApproach: [
      "Process có không gian địa chỉ riêng, thread chia sẻ heap trong cùng process.",
      "Chi phí context switch và giao tiếp IPC so với shared memory.",
      "Với Python, nhắc GIL: CPU-bound dùng process, I/O-bound dùng thread.",
    ],
    coreKeywords: ["Process", "Thread", "GIL", "Context switch"],
    sampleAnswerFramework:
      "Trả lời theo cặp đối chiếu: bộ nhớ (riêng/chia sẻ) rồi chi phí (context switch/IPC), " +
      "chốt bằng một ví dụ ngôn ngữ cụ thể (ví dụ GIL của Python) để tránh trả lời chung chung.",
    hasRubric: true,
    defaultRecall: "known",
    attempts: [],
  },
  {
    code: "IQ-033",
    topic: "systemDesign",
    level: "HARD",
    question: "Thiết kế hệ thống rút gọn URL cho 100 triệu link mỗi ngày.",
    followUps: [
      "Bạn chốt QPS và tỉ lệ đọc/ghi trước hay sau khi vẽ kiến trúc?",
      "Sinh mã base62 từ counter phân tán hay hash rồi kiểm trùng?",
      "Chiến lược sharding khi một shard nóng lên?",
    ],
    rubric: [
      { label: "Chốt yêu cầu và QPS", weight: 25 },
      { label: "Thiết kế sinh mã", weight: 30 },
      { label: "Lưu trữ và cache", weight: 30 },
      { label: "Ước lượng dung lượng", weight: 15 },
    ],
    usageCount: 62,
    averageScore: 3.1,
    content:
      "Thiết kế một hệ thống rút gọn URL (như bit.ly) phục vụ 100 triệu lượt tạo link mỗi ngày. " +
      "Nêu rõ các quyết định chính và đánh đổi ở từng bước.",
    suggestedApproach: [
      "Chốt yêu cầu: tỉ lệ đọc/ghi, độ dài mã, thời gian sống, thống kê click.",
      "Sinh mã: base62 từ counter phân tán hoặc hash kèm kiểm tra trùng.",
      "Lưu trữ: key-value store, thêm cache cho các link nóng.",
      "Ước lượng dung lượng và chiến lược sharding khi một shard nóng lên.",
    ],
    coreKeywords: ["Scalability", "Sharding", "Cache", "Base62"],
    sampleAnswerFramework:
      "Bốn bước: chốt yêu cầu và QPS trước, rồi mới thiết kế sinh mã, rồi lưu trữ/cache, kết bằng " +
      "ước lượng dung lượng — thứ tự này chính là điểm bị trừ nhiều nhất nếu đảo ngược.",
    hasRubric: true,
    defaultRecall: "vague",
    attempts: [],
  },
  {
    code: "IQ-040",
    topic: "systemDesign",
    level: "MEDIUM",
    question:
      "Cache invalidation: các chiến lược phổ biến và đánh đổi của từng cách.",
    followUps: [
      "Khi cache và DB lệch nhau, đâu là nguồn sự thật?",
      "Chống thundering herd bằng lock hay jitter TTL?",
    ],
    rubric: [
      { label: "Nêu đủ chiến lược", weight: 35 },
      { label: "Phân tích đánh đổi", weight: 35 },
      { label: "Xử lý trường hợp xấu", weight: 30 },
    ],
    usageCount: 117,
    averageScore: 3.4,
    content:
      "Liệt kê các chiến lược cache invalidation phổ biến (TTL, write-through, write-behind, " +
      "cache-aside) và phân tích đánh đổi của từng cách.",
    suggestedApproach: [
      "TTL đơn giản nhưng có cửa sổ dữ liệu cũ.",
      "Write-through, write-behind, cache-aside và ai chịu trách nhiệm ghi.",
      "Nêu vấn đề thundering herd và cách chống bằng lock hoặc jitter TTL.",
    ],
    coreKeywords: ["Cache", "Redis", "TTL", "Thundering herd"],
    sampleAnswerFramework:
      "Nêu đủ ba nhóm chiến lược rồi chọn một tình huống nghiệp vụ cụ thể để nói rõ ai là nguồn sự " +
      "thật khi cache và DB lệch nhau — đây là phần hay bị bỏ qua.",
    hasRubric: false,
    defaultRecall: null,
    attempts: [],
  },
  {
    code: "IQ-052",
    topic: "database",
    level: "MEDIUM",
    question: "Index B-tree hoạt động thế nào và khi nào một index bị bỏ qua?",
    followUps: [
      "Quy tắc tiền tố ngoài cùng bên trái áp dụng ra sao với composite index?",
      "Chi phí ghi và dung lượng của index là bao nhiêu?",
    ],
    rubric: [
      { label: "Hiểu cấu trúc B-tree", weight: 35 },
      { label: "Composite index", weight: 30 },
      { label: "Trường hợp bị bỏ qua", weight: 35 },
    ],
    usageCount: 141,
    averageScore: 3.6,
    content:
      "Giải thích cấu trúc index B-tree và liệt kê các tình huống khiến planner bỏ qua một index sẵn có.",
    suggestedApproach: [
      "Cấu trúc cây nhiều nhánh, độ sâu thấp nên ít lần đọc đĩa.",
      "Composite index và quy tắc tiền tố ngoài cùng bên trái.",
      "Bị bỏ qua khi hàm bọc lên cột, kiểu dữ liệu lệch, hoặc độ chọn lọc quá thấp.",
    ],
    coreKeywords: ["B-tree", "Composite index", "Query planner"],
    sampleAnswerFramework:
      "Trình bày cấu trúc trước, quy tắc tiền tố của composite index sau, rồi liệt kê các nguyên " +
      'nhân planner bỏ qua index — tránh chỉ nói "thêm index thì nhanh hơn".',
    hasRubric: true,
    defaultRecall: null,
    attempts: [],
  },
  {
    code: "IQ-058",
    topic: "database",
    level: "HARD",
    question:
      "Giải thích các mức isolation của transaction và hiện tượng đi kèm.",
    followUps: [
      "MVCC của PostgreSQL khác cơ chế khoá của MySQL thế nào?",
      "Nghiệp vụ nào buộc phải serializable?",
    ],
    rubric: [
      { label: "Nêu đủ bốn mức", weight: 30 },
      { label: "Gắn với hiện tượng", weight: 35 },
      { label: "Ví dụ nghiệp vụ", weight: 35 },
    ],
    usageCount: 88,
    averageScore: 2.9,
    content:
      "Nêu bốn mức isolation của transaction, hiện tượng đi kèm mỗi mức, và ví dụ nghiệp vụ tương ứng.",
    suggestedApproach: [
      "Read uncommitted đến serializable, kèm dirty read, phantom read.",
      "Cơ chế MVCC trong PostgreSQL so với khoá trong MySQL.",
      "Ví dụ thực tế cần serializable, ví dụ chấp nhận repeatable read.",
    ],
    coreKeywords: ["Transaction", "MVCC", "Isolation level"],
    sampleAnswerFramework:
      "Đi từ thấp đến cao, mỗi mức gắn đúng một hiện tượng nó ngăn được, rồi chốt bằng một ví dụ " +
      "nghiệp vụ thật thay vì chỉ đọc thuộc tên bốn mức.",
    hasRubric: true,
    defaultRecall: "vague",
    attempts: [],
  },
  {
    code: "IQ-071",
    topic: "language",
    level: "EASY",
    question:
      "Truyền tham chiếu trong Python: vì sao sửa list trong hàm lại đổi cả biến ngoài?",
    followUps: [
      "Default argument dạng list gây lỗi gì?",
      "Copy nông và copy sâu khác nhau ở đâu?",
    ],
    rubric: [
      { label: "Hiểu cơ chế truyền", weight: 40 },
      { label: "Mutable và immutable", weight: 35 },
      { label: "Cách phòng tránh", weight: 25 },
    ],
    usageCount: 203,
    averageScore: 4.0,
    content:
      "Vì sao sửa một list truyền vào hàm lại đổi luôn biến ngoài trong Python? Giải thích cơ chế " +
      "truyền tham số và hệ quả của nó.",
    suggestedApproach: [
      "Python truyền tham chiếu tới object, không copy giá trị.",
      "Phân biệt mutable và immutable, hệ quả với default argument dạng list.",
      "Cách phòng tránh: copy nông, copy sâu, hoặc trả về object mới.",
    ],
    coreKeywords: ["Python", "Mutable", "Copy nông"],
    sampleAnswerFramework:
      "Giải thích đúng cơ chế truyền tham chiếu trước (không phải truyền giá trị kiểu C++), rồi " +
      "gắn với ví dụ default argument, kết bằng cách phòng tránh cụ thể.",
    hasRubric: true,
    defaultRecall: "known",
    attempts: [],
  },
  {
    code: "IQ-084",
    topic: "behavioural",
    level: "MEDIUM",
    question:
      "Kể về một lần bạn đưa quyết định kỹ thuật sai. Bạn phát hiện và xử lý thế nào?",
    followUps: [
      "Tín hiệu nào giúp bạn phát hiện ra sai?",
      "Quy trình của nhóm thay đổi gì sau đó?",
    ],
    rubric: [
      { label: "Cấu trúc STAR", weight: 35 },
      { label: "Số liệu cụ thể", weight: 35 },
      { label: "Thay đổi quy trình", weight: 30 },
    ],
    usageCount: 74,
    averageScore: 3.0,
    content:
      "Kể về một lần bạn đưa ra một quyết định kỹ thuật sai. Bạn phát hiện ra sai ở đâu và xử lý " +
      "hậu quả thế nào?",
    suggestedApproach: [
      "Dùng khung STAR: bối cảnh, nhiệm vụ, hành động, kết quả.",
      "Nêu tín hiệu giúp phát hiện sai, ưu tiên số liệu cụ thể.",
      "Kết bằng thay đổi quy trình sau đó, không chỉ dừng ở lời xin lỗi.",
    ],
    coreKeywords: ["STAR", "Ra quyết định", "Quy trình"],
    sampleAnswerFramework:
      "**Situation**: bối cảnh và áp lực lúc quyết định.\n" +
      "**Task**: việc bạn phải giải quyết.\n" +
      "**Action**: quyết định bạn đưa ra và vì sao lúc đó có vẻ đúng.\n" +
      "**Result**: hậu quả, cách phát hiện, và thay đổi quy trình sau đó.",
    hasRubric: true,
    defaultRecall: null,
    attempts: [],
  },
  {
    code: "IQ-092",
    topic: "behavioural",
    level: "EASY",
    question:
      "Bạn xử lý thế nào khi review code của đồng nghiệp và không đồng ý về hướng làm?",
    followUps: [
      "Bạn dựa vào tiêu chí đo được nào?",
      "Khi nào thì leo thang và mốc thời gian ra sao?",
    ],
    rubric: [
      { label: "Tách kỹ thuật khỏi cá nhân", weight: 35 },
      { label: "Tiêu chí đo được", weight: 35 },
      { label: "Cách leo thang", weight: 30 },
    ],
    usageCount: 41,
    averageScore: 2.7,
    content:
      "Bạn xử lý thế nào khi review code của đồng nghiệp và không đồng ý với hướng làm của họ?",
    suggestedApproach: [
      "Tách chuyện đúng sai kỹ thuật khỏi ý kiến cá nhân.",
      "Dựa vào tiêu chí đo được: hiệu năng, khả năng test, chi phí bảo trì.",
      "Nêu cách leo thang khi vẫn bế tắc và mốc thời gian quyết định.",
    ],
    coreKeywords: ["Code review", "Teamwork", "Ra quyết định"],
    sampleAnswerFramework:
      "**Situation**: bối cảnh bất đồng trong review.\n" +
      "**Task**: mục tiêu chung cần đạt (chất lượng code, đúng hạn).\n" +
      "**Action**: cách bạn đưa tiêu chí đo được vào cuộc trao đổi.\n" +
      "**Result**: kết quả và cách leo thang nếu vẫn chưa thống nhất.",
    hasRubric: false,
    defaultRecall: "vague",
    attempts: [
      {
        attemptNo: 1,
        createdAt: "2026-09-18T09:12:00.000Z",
        feedbackStatus: "completed",
        answerText:
          "Em sẽ nói chuyện riêng với bạn ấy, chỉ ra tiêu chí hiệu năng và khả năng test, nếu vẫn " +
          "không đồng ý thì nhờ tech lead quyết trong buổi họp gần nhất.",
        strengths: [
          "Tách được chuyện kỹ thuật khỏi cảm xúc cá nhân",
          "Có mốc leo thang rõ ràng",
        ],
        gaps: [
          "Chưa nêu số liệu đo được cụ thể (ví dụ thời gian chạy, coverage)",
        ],
        nextSteps:
          "Thêm một ví dụ số liệu thật để tiêu chí không còn chung chung.",
      },
    ],
  },
];

// Questions added by the CSV import; they live in the module until the page reloads.
let importedCount = 0;

export function fetchInterviewQuestionPage(): InterviewQuestionPage {
  return {
    questions: QUESTIONS.map((question) => ({ ...question })),
    totalQuestions: 148 + importedCount,
  };
}

/** Stand-in for the import endpoint: gives each draft the next IQ-nnn code and adds it to the bank. */
export function appendImportedQuestions(
  drafts: readonly QuestionDraft[],
): InterviewQuestion[] {
  let next =
    Math.max(
      0,
      ...QUESTIONS.map((question) => Number(question.code.replace(/\D/g, ""))),
    ) + 1;
  const created = drafts.map((draft): InterviewQuestion => ({
    ...draft,
    code: `IQ-${String(next++).padStart(3, "0")}`,
    usageCount: 0,
    averageScore: 0,
    content: draft.content ?? draft.question,
    suggestedApproach: draft.suggestedApproach ?? [],
    coreKeywords: draft.coreKeywords ?? [],
    sampleAnswerFramework: draft.sampleAnswerFramework ?? "",
    hasRubric: draft.rubric.length > 0,
    defaultRecall: null,
    attempts: [],
  }));
  QUESTIONS.push(...created);
  importedCount += created.length;
  return created;
}

/**
 * USR0402 (interview_question_detail) resolves `questionId` from the route — the BD's DTO calls
 * it a UUID, but the mock keeps the same human-readable `code` used everywhere else in this
 * prototype rather than inventing a second identifier scheme [SoT: Suy luận].
 */
export function findInterviewQuestionByCode(
  code: string,
): InterviewQuestion | undefined {
  const question = QUESTIONS.find((candidate) => candidate.code === code);
  return question ? { ...question } : undefined;
}
