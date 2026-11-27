import { beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "@/shared/api";

const get = vi.fn();
const post = vi.fn();
const put = vi.fn();

vi.mock("@/shared/api", async (importOriginal) => ({
  ...(await importOriginal<Record<string, unknown>>()),
  api: {
    get: (...args: unknown[]) => get(...args),
    post: (...args: unknown[]) => post(...args),
    put: (...args: unknown[]) => put(...args),
  },
}));

// Import after the mock so the module talks to the fake `api`.
import {
  realCreateAdminQuestion,
  realGetAdminQuestion,
  realImportAdminQuestions,
  realListAdminQuestions,
  realRetireAdminQuestions,
  realUpdateAdminQuestion,
  type AdminQuestionInput,
} from "./real-admin-questions";

const ID = "11111111-1111-4111-8111-111111111111";
const BASE = "/interview-bank/admin/questions";

const detailDto = {
  id: ID,
  displayNo: 14,
  topicId: "22222222-2222-4222-8222-222222222222",
  levelId: "33333333-3333-4333-8333-333333333333",
  title: "Hash table xử lý collision thế nào?",
  contentMarkdown: null,
  followUpQuestions: ["Load factor?"],
  rubric: [
    { criterionCode: "Định nghĩa", description: null, weightPercent: 40 },
    { criterionCode: "So sánh", description: null, weightPercent: "60.00" },
  ],
  status: "ACTIVE",
  practiceAvailable: true,
  usage: { usageCount: 7, averageScorePercent: "81.5" },
  createdAt: "2026-11-01T00:00:00Z",
  updatedAt: "2026-11-02T00:00:00Z",
  extraField: "never leaks",
};

const detailModel = {
  id: ID,
  displayNo: 14,
  topicId: detailDto.topicId,
  levelId: detailDto.levelId,
  title: detailDto.title,
  contentMarkdown: null,
  followUps: ["Load factor?"],
  rubric: [
    { criterionCode: "Định nghĩa", weightPercent: 40, description: null },
    { criterionCode: "So sánh", weightPercent: 60, description: null },
  ],
  practiceAvailable: true,
  usageCount: 7,
  averageScorePercent: 81.5,
  createdAt: "2026-11-01T00:00:00Z",
  updatedAt: "2026-11-02T00:00:00Z",
};

const input: AdminQuestionInput = {
  topicId: detailDto.topicId,
  levelId: detailDto.levelId,
  title: "  Câu hỏi mới ",
  contentMarkdown: "Mô tả",
  followUps: ["a", "b"],
  rubric: [
    { criterionCode: "Ý một", weightPercent: 50, description: null },
    { criterionCode: "Ý hai", weightPercent: 50, description: null },
  ],
};

const expectedBody = {
  topicId: input.topicId,
  levelId: input.levelId,
  title: "  Câu hỏi mới ",
  contentMarkdown: "Mô tả",
  followUpQuestions: ["a", "b"],
  rubric: [
    { criterionCode: "Ý một", weightPercent: 50, description: null },
    { criterionCode: "Ý hai", weightPercent: 50, description: null },
  ],
};

beforeEach(() => {
  get.mockReset();
  post.mockReset();
  put.mockReset();
});

describe("realListAdminQuestions (#19)", () => {
  const dto = {
    items: [
      {
        id: ID,
        displayNo: 14,
        level: { code: "MEDIUM", displayName: "Trung bình" },
        topic: { code: "CS_THEORY", displayName: "Lý thuyết CS" },
        title: "Hash table?",
        followUpCount: 2,
        rubricCount: 3,
        usageCount: 5,
        averageScorePercent: null,
        hidden: "x",
      },
    ],
    page: { number: 1, size: 8, totalItems: 1, totalPages: 1 },
    meta: { shown: 1, total: 40 },
  };

  it("sends only the parameters that carry a value and maps the page", async () => {
    get.mockResolvedValue(dto);
    const result = await realListAdminQuestions({
      q: "hash",
      topic: "CS_THEORY",
      level: "all",
      sort: "usageCount",
      dir: "desc",
      page: 2,
      pageSize: 20,
    });
    expect(get).toHaveBeenCalledWith(BASE, {
      page: 2,
      pageSize: 20,
      q: "hash",
      topic: "CS_THEORY",
      sort: "usageCount",
      dir: "desc",
    });
    expect(result).toEqual({
      items: [
        {
          id: ID,
          displayNo: 14,
          level: { code: "MEDIUM", displayName: "Trung bình" },
          topic: { code: "CS_THEORY", displayName: "Lý thuyết CS" },
          title: "Hash table?",
          followUpCount: 2,
          rubricCount: 3,
          usageCount: 5,
          averageScorePercent: null,
        },
      ],
      page: { number: 1, size: 8, totalItems: 1, totalPages: 1 },
      total: 40,
    });
  });

  it("sends no sort and no filter by default", async () => {
    get.mockResolvedValue(dto);
    await realListAdminQuestions({ q: "", sort: "", page: 1, pageSize: 8 });
    expect(get).toHaveBeenCalledWith(BASE, { page: 1, pageSize: 8 });
  });

  it("reads a decimal average sent as text", async () => {
    get.mockResolvedValue({
      ...dto,
      items: [{ ...dto.items[0], averageScorePercent: "72.50" }],
    });
    const result = await realListAdminQuestions({ page: 1, pageSize: 8 });
    expect(result.items[0]!.averageScorePercent).toBe(72.5);
  });

  it.each([
    ["INB-V01", 400],
    ["INB-301", 403],
  ])("rethrows %s", async (code, status) => {
    get.mockRejectedValue(new ApiError(code, status));
    await expect(
      realListAdminQuestions({ page: 1, pageSize: 8 }),
    ).rejects.toMatchObject({ code, status });
  });
});

describe("realGetAdminQuestion (#20)", () => {
  it("maps the detail and drops unread fields", async () => {
    get.mockResolvedValue(detailDto);
    const result = await realGetAdminQuestion(ID);
    expect(get).toHaveBeenCalledWith(`${BASE}/${ID}`);
    expect(result).toEqual(detailModel);
  });

  it.each([
    ["INB-010", 404],
    ["INB-301", 403],
  ])("rethrows %s", async (code, status) => {
    get.mockRejectedValue(new ApiError(code, status));
    await expect(realGetAdminQuestion(ID)).rejects.toMatchObject({
      code,
      status,
    });
  });
});

describe("realCreateAdminQuestion (#21)", () => {
  it("posts the exact body and maps the 201 answer", async () => {
    post.mockResolvedValue(detailDto);
    const result = await realCreateAdminQuestion(input);
    expect(post).toHaveBeenCalledWith(BASE, expectedBody);
    expect(result).toEqual(detailModel);
  });

  it("sends a null content as null so the server stores NULL", async () => {
    post.mockResolvedValue(detailDto);
    await realCreateAdminQuestion({ ...input, contentMarkdown: null });
    expect(post.mock.calls[0]![1]).toMatchObject({ contentMarkdown: null });
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
  ])("rethrows %s", async (code, status) => {
    post.mockRejectedValue(new ApiError(code, status));
    await expect(realCreateAdminQuestion(input)).rejects.toMatchObject({
      code,
      status,
    });
  });
});

describe("realUpdateAdminQuestion (#22)", () => {
  it("puts the whole form state and maps the answer", async () => {
    put.mockResolvedValue(detailDto);
    const result = await realUpdateAdminQuestion(ID, input);
    expect(put).toHaveBeenCalledWith(`${BASE}/${ID}`, expectedBody);
    expect(result).toEqual(detailModel);
  });

  it.each([
    ["INB-010", 404],
    ["INB-301", 403],
    ["INB-V08", 400],
    ["INB-V17", 413],
    ["INB-033", 422],
    ["INB-035", 422],
    ["INB-503", 503],
  ])("rethrows %s", async (code, status) => {
    put.mockRejectedValue(new ApiError(code, status));
    await expect(realUpdateAdminQuestion(ID, input)).rejects.toMatchObject({
      code,
      status,
    });
  });
});

describe("realRetireAdminQuestions (#23)", () => {
  it("posts the ids and maps retired and failed items", async () => {
    post.mockResolvedValue({
      retiredIds: [ID],
      failedItems: [{ id: "x", errorCode: "INB-010" }],
    });
    const result = await realRetireAdminQuestions([ID, "x"]);
    expect(post).toHaveBeenCalledWith(`${BASE}/bulk/delete`, {
      ids: [ID, "x"],
    });
    expect(result).toEqual({
      retiredIds: [ID],
      failedItems: [{ id: "x", code: "INB-010" }],
    });
  });

  it.each([
    ["INB-V02", 400],
    ["INB-301", 403],
    ["INB-503", 503],
  ])("rethrows %s", async (code, status) => {
    post.mockRejectedValue(new ApiError(code, status));
    await expect(realRetireAdminQuestions([ID])).rejects.toMatchObject({
      code,
      status,
    });
  });
});

describe("realImportAdminQuestions (#24)", () => {
  const rows = [
    {
      line: 2,
      topic: "CS_THEORY",
      level: "EASY",
      question: "Q?",
      rubric: [{ criterionCode: "A", weightPercent: 100 }],
      followUps: ["f"],
    },
  ];

  it("posts the rows as given and maps the report", async () => {
    post.mockResolvedValue({
      importedCount: 1,
      skippedRows: [{ line: 3, reasonCode: "duplicate", detail: null }],
    });
    const result = await realImportAdminQuestions(rows);
    expect(post).toHaveBeenCalledWith(`${BASE}/import`, { rows });
    expect(result).toEqual({
      importedCount: 1,
      skippedRows: [{ line: 3, reasonCode: "duplicate", detail: null }],
    });
  });

  it.each([
    ["INB-V15", 400],
    ["INB-301", 403],
    ["INB-V17", 413],
    ["INB-503", 503],
  ])("rethrows %s", async (code, status) => {
    post.mockRejectedValue(new ApiError(code, status));
    await expect(realImportAdminQuestions(rows)).rejects.toMatchObject({
      code,
      status,
    });
  });
});
