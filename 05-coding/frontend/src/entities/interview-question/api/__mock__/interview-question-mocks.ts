// PROTOTYPE mock — no backend endpoint exists yet.
// Questions from 09-layoutBase/Admin - Câu hỏi phỏng vấn.dc.html:381-409; the "Dùng N lần · điểm TB
// X/5" string there is split into two fields so the screen can format it per locale.
import type {
  InterviewQuestion,
  InterviewQuestionPage,
  QuestionStat,
} from "../../model/types";

const QUESTIONS: InterviewQuestion[] = [
  {
    code: "IQ-014",
    topic: "csTheory",
    level: "medium",
    question: "Hash table xử lý collision bằng cách nào? So sánh chaining và open addressing.",
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
  },
  {
    code: "IQ-021",
    topic: "csTheory",
    level: "easy",
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
  },
  {
    code: "IQ-033",
    topic: "systemDesign",
    level: "hard",
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
  },
  {
    code: "IQ-040",
    topic: "systemDesign",
    level: "medium",
    question: "Cache invalidation: các chiến lược phổ biến và đánh đổi của từng cách.",
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
  },
  {
    code: "IQ-052",
    topic: "database",
    level: "medium",
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
  },
  {
    code: "IQ-058",
    topic: "database",
    level: "hard",
    question: "Giải thích các mức isolation của transaction và hiện tượng đi kèm.",
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
  },
  {
    code: "IQ-071",
    topic: "language",
    level: "easy",
    question: "Truyền tham chiếu trong Python: vì sao sửa list trong hàm lại đổi cả biến ngoài?",
    followUps: ["Default argument dạng list gây lỗi gì?", "Copy nông và copy sâu khác nhau ở đâu?"],
    rubric: [
      { label: "Hiểu cơ chế truyền", weight: 40 },
      { label: "Mutable và immutable", weight: 35 },
      { label: "Cách phòng tránh", weight: 25 },
    ],
    usageCount: 203,
    averageScore: 4.0,
  },
  {
    code: "IQ-084",
    topic: "behavioural",
    level: "medium",
    question: "Kể về một lần bạn đưa quyết định kỹ thuật sai. Bạn phát hiện và xử lý thế nào?",
    followUps: ["Tín hiệu nào giúp bạn phát hiện ra sai?", "Quy trình của nhóm thay đổi gì sau đó?"],
    rubric: [
      { label: "Cấu trúc STAR", weight: 35 },
      { label: "Số liệu cụ thể", weight: 35 },
      { label: "Thay đổi quy trình", weight: 30 },
    ],
    usageCount: 74,
    averageScore: 3.0,
  },
  {
    code: "IQ-092",
    topic: "behavioural",
    level: "easy",
    question: "Bạn xử lý thế nào khi review code của đồng nghiệp và không đồng ý về hướng làm?",
    followUps: ["Bạn dựa vào tiêu chí đo được nào?", "Khi nào thì leo thang và mốc thời gian ra sao?"],
    rubric: [
      { label: "Tách kỹ thuật khỏi cá nhân", weight: 35 },
      { label: "Tiêu chí đo được", weight: 35 },
      { label: "Cách leo thang", weight: 30 },
    ],
    usageCount: 41,
    averageScore: 2.7,
  },
];

// dc.html:428-433.
const STATS: QuestionStat[] = [
  { key: "total", value: "148", delta: "+9", deltaColorVar: "--color-success" },
  { key: "withRubric", value: "131", delta: "89%", deltaColorVar: "--color-success" },
  { key: "averageScore", value: "3.5", delta: "−0.2", deltaColorVar: "--color-admin-warn" },
  { key: "neverUsed", value: "11", delta: "", deltaColorVar: "--color-text-subtle" },
];

export function fetchInterviewQuestionPage(): InterviewQuestionPage {
  return {
    stats: STATS.map((stat) => ({ ...stat })),
    questions: QUESTIONS.map((question) => ({ ...question })),
    totalQuestions: 148,
  };
}
