// Real calls behind SHR0202 (problem authoring), SHR0203 (problem info) and the learner preview
// (03-dd/api/F2_problem-bank.md #33 to #47; DD sections 34 to 48). The screens edit one `ProblemDraft`;
// the server keeps it in separate resources, so a save is a short sequence (content, spec, AI context,
// status) and testcases are written one by one the moment the author confirms them.
//
// Left on mock: #48 (AI testcase generation) has no endpoint yet, see `queries.ts`.
import { api, ApiError } from "@/shared/api";
import { realListLevels } from "./real-catalog";
import type {
  MatchingStrategy,
  ProblemDraft,
  ProblemDraftRecord,
  ProblemSpec,
  ProblemStats,
  ServerState,
  SpecParameter,
  SpecType,
  Testcase,
  TestcaseCategory,
  TestcaseVisibility,
  TypeKind,
  WorkedExample,
} from "../model/draft-types";
import { SPEC_LANGUAGES, TYPE_KINDS } from "../model/draft-types";

const BASE = "/problem-bank/authoring/problems";

// ---------------------------------------------------------------------------------------------
// Wire shapes (the record is the wire: backend `ProblemAuthoringView` and friends).
// ---------------------------------------------------------------------------------------------

type TypeRefDto = { kind: string; of?: TypeRefDto; dimensions?: number };

type SpecDto = {
  matchingStrategy: MatchingStrategy;
  epsilonValue: number | string | null;
  stdinFormatMd: string;
  stdoutFormatMd: string;
  functionName: string | null;
  returnType: TypeRefDto | null;
  parameters: { name: string; type: TypeRefDto }[] | null;
  nameOverrides: Record<string, string> | null;
  specVersion?: number;
};

type TestcaseDto = {
  id: string;
  position: number;
  visibility: "SAMPLE" | "HIDDEN";
  origin: "MANUAL" | "AI";
  approved: boolean;
  category: string | null;
  input: string | null;
  inputStorage: "INLINE" | "MINIO";
  expectedOutput: string | null;
  expectedOutputStorage: "INLINE" | "MINIO";
};

type ExampleDto = {
  id: string | null;
  position?: number;
  input: string;
  output: string;
  explanationMd: string | null;
};

type DetailDto = {
  id: string;
  title: string;
  statementMd: string;
  constraintsMd: string;
  levelId: string;
  status: "UNPUBLISHED" | "PUBLISHED";
  limits: {
    timeLimitMs: number;
    memoryLimitMb: number;
    maxOutputSizeKb: number;
    maxSubmissionsPerHour: number | null;
  };
  topicIds: string[];
  tags: { id: string; name: string }[];
  referenceSolution: { language: string; sourceCode: string } | null;
  workedExamples: ExampleDto[];
  testcases: TestcaseDto[];
  testcaseSummary: ServerState["testcaseSummary"];
  spec: SpecDto | null;
  aiContext: {
    briefText: string | null;
    noFullCode: boolean;
    questionOnly: boolean;
  };
  aiGeneration: { used: number; limit: number };
  checklist?: { specDeclared: boolean };
  updatedAt: string;
  duplicatedFromProblemId: string | null;
  readOnly: boolean;
};

type DuplicateDto = {
  title: string;
  statementMd: string;
  constraintsMd: string;
  levelId: string;
  limits: DetailDto["limits"];
  topicIds: string[];
  tags: string[];
  referenceSolution: DetailDto["referenceSolution"];
  workedExamples: ExampleDto[];
  willCopyOnSave: { testcaseCount: number; hasSpec: boolean };
  duplicatedFromProblemId: string;
};

// ---------------------------------------------------------------------------------------------
// Enum mapping
// ---------------------------------------------------------------------------------------------

const CATEGORY_FROM_WIRE: Record<string, TestcaseCategory> = {
  TYPICAL: "typical",
  BOUNDARY_LOW: "boundaryLow",
  BOUNDARY_HIGH: "boundaryHigh",
  DEGENERATE: "degenerate",
  DUPLICATE: "duplicate",
  SORTED: "sorted",
  EXTREME: "extreme",
};

const LANGUAGE_FROM_WIRE: Record<string, string> = {
  JAVA: "Java",
  CPP: "C++",
  PYTHON: "Python",
};
const LANGUAGE_TO_WIRE: Record<string, string> = {
  Java: "JAVA",
  "C++": "CPP",
  Python: "PYTHON",
};

const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

const NO_CHECK = { ran: false, passed: 0, total: 0 };

export function emptySpec(): ProblemSpec {
  return {
    signature: {
      functionName: "",
      returnType: { kind: "INT" },
      parameters: [],
      nameOverrides: {},
    },
    stdinFormat: "",
    stdoutFormat: "",
    matchingStrategy: "EXACT",
    epsilon: "",
  };
}

// ---------------------------------------------------------------------------------------------
// Spec <-> ProblemSpecDto
// ---------------------------------------------------------------------------------------------

function toType(dto: TypeRefDto): SpecType {
  const kind = (TYPE_KINDS as string[]).includes(dto.kind)
    ? (dto.kind as TypeKind)
    : "OTHER"; // A kind the schema does not know: the screen shows it as "OTHER" and warns.
  return {
    kind,
    ...(kind === "OTHER" ? {rawType: {...dto}} : {}),
    ...(dto.of ? { of: toType(dto.of) } : {}),
    ...(dto.dimensions ? { dimensions: dto.dimensions } : {}),
  };
}

function fromType(type: SpecType): TypeRefDto {
  if (type.kind === "OTHER" && type.rawType) return type.rawType;
  return {
    kind: type.kind,
    ...(type.of ? { of: fromType(type.of) } : {}),
    ...(type.dimensions ? { dimensions: type.dimensions } : {}),
  };
}

export function toSpec(dto: SpecDto | null): ProblemSpec {
  if (!dto) return emptySpec();
  const overrides: ProblemSpec["signature"]["nameOverrides"] = {};
  for (const language of SPEC_LANGUAGES) {
    const value = dto.nameOverrides?.[language.toUpperCase()];
    if (value) overrides[language] = value;
  }
  return {
    signature: {
      functionName: dto.functionName ?? "",
      returnType: dto.returnType ? toType(dto.returnType) : { kind: "INT" },
      parameters: (dto.parameters ?? []).map(
        (parameter, index): SpecParameter => ({
          id: `p${index + 1}`,
          name: parameter.name,
          type: toType(parameter.type),
        }),
      ),
      nameOverrides: overrides,
    },
    stdinFormat: dto.stdinFormatMd,
    stdoutFormat: dto.stdoutFormatMd,
    matchingStrategy: dto.matchingStrategy,
    epsilon: dto.epsilonValue === null ? "" : String(dto.epsilonValue),
  };
}

/** The body of #37. A signature is sent whole or not at all: a half-declared one is HRN-V17. */
export function specBody(spec: ProblemSpec, expectedUpdatedAt: string | null) {
  const { signature } = spec;
  const declared = signature.functionName.trim() !== "";
  const epsilon = Number(spec.epsilon);
  const overrides = Object.fromEntries(
    Object.entries(signature.nameOverrides)
      .filter(([, name]) => name && name.trim())
      .map(([language, name]) => [language.toUpperCase(), name]),
  );
  return {
    matchingStrategy: spec.matchingStrategy,
    epsilonValue:
      spec.epsilon.trim() !== "" && Number.isFinite(epsilon) ? epsilon : null,
    stdinFormatMd: spec.stdinFormat,
    stdoutFormatMd: spec.stdoutFormat,
    functionName: declared ? signature.functionName.trim() : null,
    returnType: declared ? fromType(signature.returnType) : null,
    parameters: declared
      ? signature.parameters.map((parameter) => ({
          name: parameter.name.trim(),
          type: fromType(parameter.type),
        }))
      : null,
    nameOverrides: declared && Object.keys(overrides).length ? overrides : null,
    expectedUpdatedAt,
  };
}

export type SignaturePreview = {
  wrapperSupported: boolean;
  /** Keyed JAVA / CPP / PYTHON; null when the draft cannot be shown (DD F3 3.7). */
  declarations: Record<"JAVA" | "CPP" | "PYTHON", string | null>;
};

/** The body of F3 P6; a signature is previewed whole, so callers only send a declared one. */
export function previewBody(signature: ProblemSpec["signature"]) {
  return {
    functionName: signature.functionName.trim(),
    returnType: fromType(signature.returnType),
    parameters: signature.parameters.map((parameter) => ({
      name: parameter.name.trim(),
      type: fromType(parameter.type),
    })),
    nameOverrides: Object.fromEntries(
      Object.entries(signature.nameOverrides)
        .filter(([, name]) => name && name.trim())
        .map(([language, name]) => [language.toUpperCase(), name]),
    ),
  };
}

/** F3 P6 (03-dd/api/F3_harness.md 3.7): the declaration lines exactly as the starter code will have them. */
export function realPreviewSignature(signature: ProblemSpec["signature"]) {
  return api.post<SignaturePreview>(
    "/harness/function-signature/preview",
    previewBody(signature),
  );
}

// ---------------------------------------------------------------------------------------------
// Testcase <-> TestcaseAuthorDto
// ---------------------------------------------------------------------------------------------

export function toTestcase(dto: TestcaseDto): Testcase {
  const large: ("input" | "expected")[] = [];
  if (dto.inputStorage === "MINIO") large.push("input");
  if (dto.expectedOutputStorage === "MINIO") large.push("expected");
  return {
    id: dto.id,
    input: dto.input ?? "",
    expected: dto.expectedOutput ?? "",
    visibility: dto.visibility === "SAMPLE" ? "public" : "hidden",
    origin: dto.origin === "AI" ? "ai" : "manual",
    approved: dto.approved,
    ...(dto.category && CATEGORY_FROM_WIRE[dto.category]
      ? { category: CATEGORY_FROM_WIRE[dto.category] }
      : {}),
    ...(large.length ? { largeSides: large } : {}),
  };
}

const visibilityToWire = (visibility: TestcaseVisibility) =>
  visibility === "public" ? "SAMPLE" : "HIDDEN";

// ---------------------------------------------------------------------------------------------
// Detail -> draft
// ---------------------------------------------------------------------------------------------

function toExamples(items: ExampleDto[], seed: string): WorkedExample[] {
  return items.map((item, index) => ({
    id: item.id ?? `${seed}-${index + 1}`,
    input: item.input,
    output: item.output,
    explanation: item.explanationMd ?? "",
  }));
}

type Levels = {
  idByCode: Record<string, string>;
  codeById: Record<string, string>;
};

async function loadLevels(): Promise<Levels> {
  const list = await realListLevels();
  return {
    idByCode: Object.fromEntries(list.map((item) => [item.key, item.id ?? ""])),
    codeById: Object.fromEntries(list.map((item) => [item.id ?? "", item.key])),
  };
}

function baseDraft(
  source: Pick<
    DetailDto,
    | "title"
    | "statementMd"
    | "constraintsMd"
    | "limits"
    | "topicIds"
    | "referenceSolution"
  > & { tags: string[]; levelCode: string },
): ProblemDraft {
  return {
    title: source.title,
    body: source.statementMd,
    constraints: source.constraintsMd,
    topic: source.topicIds[0] ?? "",
    otherTopicIds: source.topicIds.slice(1),
    difficulty: source.levelCode,
    status: "draft",
    limits: {
      timeLimitMs: source.limits.timeLimitMs,
      memoryLimitMb: source.limits.memoryLimitMb,
      outputLimitKb: source.limits.maxOutputSizeKb,
      maxSubmissionsPerHour: source.limits.maxSubmissionsPerHour,
    },
    solutionLanguage:
      LANGUAGE_FROM_WIRE[source.referenceSolution?.language ?? ""] ?? "Java",
    solution: source.referenceSolution?.sourceCode ?? "",
    tags: source.tags,
    examples: [],
    testcases: [],
    solutionCheck: NO_CHECK,
    spec: emptySpec(),
    aiBrief: "",
    aiGuards: { noFullCode: false, socraticOnly: false },
  };
}

export function detailToDraft(dto: DetailDto, levels: Levels): ProblemDraft {
  return {
    ...baseDraft({
      ...dto,
      tags: dto.tags.map((tag) => tag.name),
      levelCode: levels.codeById[dto.levelId] ?? dto.levelId,
    }),
    status: dto.status === "PUBLISHED" ? "published" : "draft",
    examples: toExamples(dto.workedExamples, "ex-srv"),
    testcases: dto.testcases.map(toTestcase),
    spec: toSpec(dto.spec),
    aiBrief: dto.aiContext.briefText ?? "",
    aiGuards: {
      noFullCode: dto.aiContext.noFullCode,
      socraticOnly: dto.aiContext.questionOnly,
    },
  };
}

function serverState(
  dto: DetailDto | null,
  levels: Levels,
  duplicate?: DuplicateDto,
): ServerState {
  return {
    updatedAt: dto?.updatedAt ?? null,
    readOnly: dto?.readOnly ?? false,
    levelIdByCode: levels.idByCode,
    testcaseSummary: dto?.testcaseSummary ?? {
      total: 0,
      approved: 0,
      sample: 0,
      hidden: 0,
    },
    specDeclared: dto?.checklist?.specDeclared ?? null,
    willCopy: duplicate?.willCopyOnSave ?? null,
    duplicatedFromProblemId:
      duplicate?.duplicatedFromProblemId ??
      dto?.duplicatedFromProblemId ??
      null,
  };
}

/** #40: the side block; a failure leaves the figures unknown, it never fails the page. */
async function loadStats(problemId: string): Promise<ProblemStats | null> {
  try {
    const dto = await api.get<{
      submissionCount: number;
      acRate: number | null;
    }>(`${BASE}/${problemId}/stats`);
    return { submissionCount: dto.submissionCount, acRate: dto.acRate };
  } catch {
    return null;
  }
}

/** The read record without statistics; the save path uses this to adopt what the server now holds. */
async function readRecord(
  problemId: string,
  levels: Levels,
): Promise<ProblemDraftRecord> {
  const dto = await api.get<DetailDto>(`${BASE}/${problemId}`);
  return {
    draft: detailToDraft(dto, levels),
    savedAt: dto.updatedAt,
    aiGeneration: dto.aiGeneration,
    stats: null,
    server: serverState(dto, levels),
  };
}

/**
 * #33 for an existing problem, #34 for a duplicate (`fromId`), a blank form otherwise. Levels (#14) are
 * read for the code-to-id map; stats (#40) are read beside the problem and may come back unknown.
 */
export async function realLoadDraft(
  problemId?: string,
  fromId?: string,
): Promise<ProblemDraftRecord> {
  const [levels, defaults] = await Promise.all([
    loadLevels(),
    problemId ? Promise.resolve(null) : api.get<{
      timeLimitMs: number; memoryLimitMb: number; maxOutputSizeKb: number; maxAiGenerationsPerProblem: number;
    }>("/problem-bank/authoring/defaults"),
  ]);
  if (problemId) {
    const [record, stats] = await Promise.all([
      readRecord(problemId, levels),
      loadStats(problemId),
    ]);
    return { ...record, stats };
  }
  if (!defaults) throw new ApiError("PRB-050", 503);
  if (fromId) {
    const source = await api.get<DuplicateDto>(
      `${BASE}/${fromId}/duplicate-source`,
    );
    const draft = baseDraft({
      ...source,
      levelCode: levels.codeById[source.levelId] ?? source.levelId,
    });
    return {
      draft: {
        ...draft,
        title: `${source.title} (bản sao)`,
        examples: toExamples(source.workedExamples, "ex-copy"),
      },
      savedAt: null,
      aiGeneration: { used: 0, limit: defaults.maxAiGenerationsPerProblem },
      stats: null,
      server: serverState(null, levels, source),
    };
  }
  const first = Object.keys(levels.idByCode)[0] ?? "";
  return {
    draft: {
      ...baseDraft({
        title: "",
        statementMd: "",
        constraintsMd: "",
        limits: {
          timeLimitMs: defaults.timeLimitMs,
          memoryLimitMb: defaults.memoryLimitMb,
          maxOutputSizeKb: defaults.maxOutputSizeKb,
          maxSubmissionsPerHour: null,
        },
        topicIds: [],
        referenceSolution: null,
        tags: [],
        levelCode: first,
      }),
    },
    savedAt: null,
    aiGeneration: { used: 0, limit: defaults.maxAiGenerationsPerProblem },
    stats: null,
    server: serverState(null, levels),
  };
}

// ---------------------------------------------------------------------------------------------
// Save: a short sequence, each step chains the `updatedAt` the server answered with
// ---------------------------------------------------------------------------------------------

export type SaveSection = "content" | "spec" | "ai" | "status";

export type SaveResult = {
  /** Set when this save created the problem. */
  createdId?: string;
  /** Sections the server now holds; the screen treats exactly these as saved. */
  applied: SaveSection[];
  /** The step that failed, if any; earlier steps are already stored. */
  error?: unknown;
  /** Latest lock value (real mode). */
  updatedAt?: string | null;
  /** What the server holds now, read back after the save (real mode); absent when that read failed. */
  record?: ProblemDraftRecord;
};

const contentKey = (draft: ProblemDraft) =>
  JSON.stringify([
    draft.title,
    draft.body,
    draft.constraints,
    draft.topic,
    draft.otherTopicIds ?? [],
    draft.difficulty,
    draft.limits,
    draft.tags,
    draft.solutionLanguage,
    draft.solution,
    draft.examples.map(({ id, input, output, explanation }) => [
      id,
      input,
      output,
      explanation,
    ]),
  ]);
const specKey = (draft: ProblemDraft) => JSON.stringify(draft.spec);
const aiKey = (draft: ProblemDraft) =>
  JSON.stringify([draft.aiBrief, draft.aiGuards]);

/** Body of #35 and #36 (the update also needs `expectedUpdatedAt`). */
export function contentBody(
  draft: ProblemDraft,
  levelId: string,
  extra: Record<string, unknown>,
) {
  const topicIds = [draft.topic, ...(draft.otherTopicIds ?? [])].filter(
    (id) => id !== "",
  );
  const tags = draft.tags.map((tag) => tag.trim()).filter(Boolean);
  return {
    title: draft.title.trim(),
    statementMd: draft.body,
    constraintsMd: draft.constraints,
    levelId,
    limits: {
      timeLimitMs: draft.limits.timeLimitMs,
      memoryLimitMb: draft.limits.memoryLimitMb,
      maxOutputSizeKb: draft.limits.outputLimitKb,
      maxSubmissionsPerHour: draft.limits.maxSubmissionsPerHour,
    },
    topicIds,
    tags: [...new Set(tags)],
    referenceSolution: draft.solution.trim()
      ? {
          language: LANGUAGE_TO_WIRE[draft.solutionLanguage] ?? "JAVA",
          sourceCode: draft.solution,
        }
      : null,
    workedExamples: draft.examples.map((example) => ({
      id: UUID.test(example.id) ? example.id : null,
      input: example.input,
      output: example.output,
      explanationMd: example.explanation.trim() ? example.explanation : null,
    })),
    ...extra,
  };
}

export type SaveInput = {
  /** What the author sees now. */
  draft: ProblemDraft;
  /** What the server held at the last load or save; a section equal to it is not sent. */
  saved: ProblemDraft;
  problemId?: string;
  server: ServerState;
  /** Latest lock value the screen knows (testcase writes move it too). */
  updatedAt: string | null;
};

/**
 * #35 or #36, then #37, #38 and #39 for whatever differs from `saved`. Never throws: a failing step
 * comes back as `error` together with the sections that did go through, so the screen can keep the
 * author's other edits and mark only those as saved.
 */
export async function realSaveDraft(input: SaveInput): Promise<SaveResult> {
  const { draft, saved, server } = input;
  const applied: SaveSection[] = [];
  let updatedAt = input.updatedAt;
  let id = input.problemId;
  let createdId: string | undefined;
  try {
    const levelId = server.levelIdByCode[draft.difficulty];
    if (!levelId) throw new ApiError("PRB-039", 422, "Level no longer exists");
    if (!id) {
      const created = await api.post<DetailDto>(
        BASE,
        contentBody(draft, levelId, {
          duplicatedFromProblemId: server.duplicatedFromProblemId,
        }),
      );
      id = created.id;
      createdId = id;
      updatedAt = created.updatedAt;
      applied.push("content");
      // Rows added before the first save only exist on the screen; a duplicate's own rows were copied by the server.
      for (const row of draft.testcases) {
        const made = await realCreateTestcase(id, row);
        updatedAt = made.updatedAt;
      }
    } else if (contentKey(draft) !== contentKey(saved)) {
      const updated = await api.put<DetailDto>(
        `${BASE}/${id}/content`,
        contentBody(draft, levelId, { expectedUpdatedAt: updatedAt }),
      );
      updatedAt = updated.updatedAt;
      applied.push("content");
    }
    if (specKey(draft) !== specKey(saved)) {
      const result = await api.put<{ problemUpdatedAt: string }>(
        `${BASE}/${id}/spec`,
        specBody(draft.spec, updatedAt),
      );
      updatedAt = result.problemUpdatedAt;
      applied.push("spec");
    }
    if (aiKey(draft) !== aiKey(saved)) {
      const result = await api.put<{ problemUpdatedAt: string }>(
        `${BASE}/${id}/ai-context`,
        {
          briefText: draft.aiBrief.trim() ? draft.aiBrief : null,
          noFullCode: draft.aiGuards.noFullCode,
          questionOnly: draft.aiGuards.socraticOnly,
          expectedUpdatedAt: updatedAt,
        },
      );
      updatedAt = result.problemUpdatedAt;
      applied.push("ai");
    }
    if (draft.status !== saved.status) {
      const result = await api.put<{ problemUpdatedAt: string }>(
        `${BASE}/${id}/status`,
        {
          status: draft.status === "published" ? "PUBLISHED" : "UNPUBLISHED",
          expectedUpdatedAt: updatedAt,
        },
      );
      updatedAt = result.problemUpdatedAt;
      applied.push("status");
    }
  } catch (error) {
    // A failed step after the problem was created still has to hand over the server's testcase ids.
    const record = createdId ? await tryRead(createdId, server) : undefined;
    return { createdId, applied, error, updatedAt, record };
  }
  // Everything is stored: read it back, so ids, copied testcases and the lock value are the server's.
  const record = await tryRead(id!, server);
  return {
    createdId,
    applied,
    updatedAt: record?.server?.updatedAt ?? updatedAt,
    record,
  };
}

async function tryRead(
  problemId: string,
  server: ServerState,
): Promise<ProblemDraftRecord | undefined> {
  const levels: Levels = {
    idByCode: server.levelIdByCode,
    codeById: Object.fromEntries(
      Object.entries(server.levelIdByCode).map(([code, levelId]) => [
        levelId,
        code,
      ]),
    ),
  };
  try {
    return await readRecord(problemId, levels);
  } catch {
    return undefined;
  }
}

// ---------------------------------------------------------------------------------------------
// Testcases (#41 to #44), written one by one
// ---------------------------------------------------------------------------------------------

type TestcaseWrite = { testcase: Testcase; updatedAt: string };

/** #41: a hand-written row is approved on the server (`origin = MANUAL`). */
export async function realCreateTestcase(
  problemId: string,
  values: Pick<Testcase, "input" | "expected" | "visibility" | "category">,
): Promise<TestcaseWrite> {
  const dto = await api.post<{
    testcase: TestcaseDto;
    problemUpdatedAt: string;
  }>(`${BASE}/${problemId}/testcases`, {
    visibility: visibilityToWire(values.visibility),
    input: values.input,
    expectedOutput: values.expected,
  });
  return {
    testcase: toTestcase(dto.testcase),
    updatedAt: dto.problemUpdatedAt,
  };
}

export type TestcasePatch = Partial<
  Pick<Testcase, "input" | "expected" | "visibility"> & { approved: true }
>;

/** #42: only the keys present are sent; a key left out keeps the stored value (a MinIO side is never sent). */
export async function realUpdateTestcase(
  problemId: string,
  testcaseId: string,
  patch: TestcasePatch,
): Promise<TestcaseWrite> {
  const body: Record<string, unknown> = {};
  if (patch.input !== undefined) body.input = patch.input;
  if (patch.expected !== undefined) body.expectedOutput = patch.expected;
  if (patch.visibility !== undefined)
    body.visibility = visibilityToWire(patch.visibility);
  if (patch.approved !== undefined) body.approved = patch.approved;
  const dto = await api.patch<{
    testcase: TestcaseDto;
    problemUpdatedAt: string;
  }>(`${BASE}/${problemId}/testcases/${testcaseId}`, body);
  return {
    testcase: toTestcase(dto.testcase),
    updatedAt: dto.problemUpdatedAt,
  };
}

/** #43: also how an unapproved AI draft is rejected. */
export async function realDeleteTestcase(
  problemId: string,
  testcaseId: string,
): Promise<{ updatedAt: string }> {
  const dto = await api.delete<{ problemUpdatedAt: string }>(
    `${BASE}/${problemId}/testcases/${testcaseId}`,
  );
  return { updatedAt: dto.problemUpdatedAt };
}

/** #44: the full list of ids in the new order; no screen drives it yet (the prototype has no drag and drop). */
export async function realReorderTestcases(
  problemId: string,
  orderedIds: string[],
): Promise<{ updatedAt: string }> {
  const dto = await api.put<{ problemUpdatedAt: string }>(
    `${BASE}/${problemId}/testcases/order`,
    { orderedIds },
  );
  return { updatedAt: dto.problemUpdatedAt };
}

// ---------------------------------------------------------------------------------------------
// Bulk upload (#45, #46): request URLs, PUT the files straight to storage, confirm
// ---------------------------------------------------------------------------------------------

export type BulkPair = {
  clientKey: string;
  input: Blob;
  expected: Blob;
  visibility: TestcaseVisibility;
};

/**
 * The browser pairs the files (DD 46.2 leaves the pairing rule to the screen) and hands the pairs here.
 * The presigned PUT goes out with no application token and no cookies. One failed PUT stops the run
 * before the confirm call, so nothing is added half way ("không thêm testcase một phần").
 */
export async function realBulkUpload(
  problemId: string,
  pairs: BulkPair[],
): Promise<{ added: Testcase[]; updatedAt: string }> {
  const plan = await api.post<{
    uploadId: string;
    items: {
      clientKey: string;
      input: { putUrl: string };
      expectedOutput: { putUrl: string };
    }[];
  }>(`${BASE}/${problemId}/testcases/bulk-upload-url`, {
    items: pairs.map((pair) => ({
      clientKey: pair.clientKey,
      inputSizeBytes: pair.input.size,
      expectedOutputSizeBytes: pair.expected.size,
      visibility: visibilityToWire(pair.visibility),
    })),
  });
  for (const item of plan.items) {
    const pair = pairs.find(
      (candidate) => candidate.clientKey === item.clientKey,
    );
    if (!pair) throw new ApiError("UNKNOWN_ERROR", 500, "Unknown clientKey");
    for (const [url, body] of [
      [item.input.putUrl, pair.input],
      [item.expectedOutput.putUrl, pair.expected],
    ] as const) {
      const response = await fetch(url, {
        method: "PUT",
        body,
        credentials: "omit",
      });
      if (!response.ok) throw new ApiError("PRB-052", 503, "Upload failed");
    }
  }
  const done = await api.post<{
    added: TestcaseDto[];
    problemUpdatedAt: string;
  }>(`${BASE}/${problemId}/testcases/bulk-upload/confirm`, {
    uploadId: plan.uploadId,
  });
  return {
    added: done.added.map(toTestcase),
    updatedAt: done.problemUpdatedAt,
  };
}

// ---------------------------------------------------------------------------------------------
// Run the reference solution (#47)
// ---------------------------------------------------------------------------------------------

export type ReferenceRun = {
  compileError: string | null;
  passed: number;
  total: number;
  allPass: boolean;
  /** The first failing row in testcase order, with the server's reason; null when none failed. */
  firstFailure: { position: number; failure: string } | null;
};

/**
 * Runs the unsaved code of the editor when there is some (the server then does not read its stored
 * copy), against every approved testcase. The answer holds verdicts only, never the program's output.
 */
export async function realRunReferenceSolution(
  problemId: string,
  code: { language: string; sourceCode: string },
  testcaseId?: string,
): Promise<ReferenceRun> {
  const dto = await api.post<{
    compileError: string | null;
    summary: { passed: number; total: number; allPass: boolean };
    results: {
      position: number;
      verdict: "PASS" | "FAIL";
      failure: string | null;
    }[];
  }>(`${BASE}/${problemId}/reference-solution/run`, {
    language: LANGUAGE_TO_WIRE[code.language] ?? "JAVA",
    sourceCode: code.sourceCode,
    ...(testcaseId ? {testcaseId} : {}),
  });
  const failed = [...dto.results]
    .filter((row) => row.verdict === "FAIL")
    .sort((left, right) => left.position - right.position)[0];
  return {
    compileError: dto.compileError,
    ...dto.summary,
    firstFailure: failed
      ? {
          position: failed.position,
          failure: failed.failure ?? "RUNTIME_ERROR",
        }
      : null,
  };
}

/** Author-only hydration; signed URLs are refreshed from the detail endpoint on every open. */
export async function realReadTestcaseContent(problemId: string, testcaseId: string): Promise<Testcase> {
  const detail = await api.get<{testcases: (TestcaseDto & {inputDownloadUrl?: string | null; expectedOutputDownloadUrl?: string | null})[]}>(`${BASE}/${problemId}`);
  const row = detail.testcases.find(item => item.id === testcaseId);
  if (!row) throw new ApiError("PRB-011", 404);
  async function read(inline: string | null, url?: string | null) {
    if (inline !== null) return inline;
    if (!url) throw new ApiError("PRB-052", 503);
    const response = await fetch(url, {credentials: "omit", signal: AbortSignal.timeout(10000)});
    if (!response.ok) throw new ApiError("PRB-052", 503);
    const bytes = await response.arrayBuffer();
    if (bytes.byteLength > 1048576) throw new ApiError("PRB-048", 422);
    return new TextDecoder("utf-8", {fatal: true}).decode(bytes);
  }
  const [input, expected] = await Promise.all([read(row.input, row.inputDownloadUrl), read(row.expectedOutput, row.expectedOutputDownloadUrl)]);
  return {...toTestcase(row), input, expected, largeSides: undefined};
}

/** #48 uses the saved problem, an empty body, and server-owned quota/state. */
export async function realGenerateTestcases(input: import("../model/draft-types").GenerateTestcasesInput): Promise<import("../model/draft-types").GenerateTestcasesResult> {
  if (!input.problemId) throw new ApiError("PRB-041", 422);
  const result = await api.post<{
    testcases: TestcaseDto[]; dropped: {reason: string; count: number}[];
    warnings: {code: string}[]; aiGeneration: {used: number; limit: number}; problemUpdatedAt: string;
  }>(`${BASE}/${input.problemId}/testcases/generate`, {});
  const reasons: Record<string, import("../model/draft-types").DroppedReason> = {
    INVALID_CONSTRAINT: "invalidConstraint", REFERENCE_FAILED: "referenceFailed", TIMEOUT: "timeout", DUPLICATE_INPUT: "duplicateInput",
  };
  return {
    testcases: result.testcases.map(toTestcase),
    dropped: result.dropped.map(row => ({reason: reasons[row.reason] ?? "invalidConstraint", count: row.count})),
    largestCaseWarning: result.warnings.some(row => row.code === "LARGEST_CASE_TIMEOUT"),
    aiGeneration: result.aiGeneration, problemUpdatedAt: result.problemUpdatedAt,
  };
}
