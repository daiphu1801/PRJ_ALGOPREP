// SHR0204 (F2-19) on the REAL api path: only the HTTP layer is faked, by a server that remembers, and the
// template file is the one the app ships.
import { readFileSync } from "node:fs";
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
import viMessages from "../../../../../messages/vi.json";
import { ApiError } from "@/shared/api";
import { toast, useToasts } from "@/shared/lib/toast-store";
import { withTestProviders } from "@/shared/test/render-with-providers";

vi.mock("@/shared/api", async (importOriginal) =>
  (await import("@/shared/test/real-api")).realApiModule(
    await importOriginal<Record<string, unknown>>(),
  ),
);
vi.mock("@/shared/api/mock-mode", () => ({
  isMockMode: () => false,
  withMockData: <T,>(_mock: () => unknown, real: () => Promise<T>) => real(),
}));
vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: vi.fn(), replace: vi.fn() }),
}));

import {
  fakeAuthoringServer,
  type FakeServer,
} from "@/shared/test/fake-authoring-server";
import { resetApiMock } from "@/shared/test/real-api";
import { ProblemImportView } from "./problem-import-view";

const m = viMessages.problemImport;
const apiErrors = viMessages.apiErrors as Record<string, string>;
const bytes = readFileSync("public/templates/mau-nhap-bai-toan.xlsx");

// jsdom's File has no arrayBuffer(); browsers do.
function xlsx(name = "bai.xlsx", content: Uint8Array = bytes) {
  const file = new File([""], name);
  Object.defineProperty(file, "arrayBuffer", {
    value: async () => new Uint8Array(content).buffer,
  });
  return file;
}

const text = (message: string, values: Record<string, unknown>) =>
  Object.entries(values).reduce(
    (out, [key, value]) => out.replaceAll(`{${key}}`, String(value)),
    message,
  );

const HEADER = [
  "Tiêu đề",
  "Độ khó",
  "Chủ đề",
  "Đề bài",
  "Định dạng vào",
  "Định dạng ra",
  "Lời giải",
  "Ngôn ngữ",
  ...Array.from({ length: 8 }, (_, i) => [
    `Input ${i + 1}`,
    `Output ${i + 1}`,
  ]).flat(),
];
const line = (title: string, solution = `print("${title}")`) =>
  [
    title,
    "Dễ",
    "Mảng",
    "Đề",
    "n",
    "m",
    solution,
    "Python",
    ...Array.from({ length: 8 }, (_, i) => [`${i}`, `${i}`]).flat(),
  ].join("\t");

function paste(value: string, target: Element | Document = document) {
  const event = new Event("paste", { bubbles: true, cancelable: true });
  Object.defineProperty(event, "clipboardData", {
    value: { files: [], getData: () => value },
  });
  act(() => {
    target.dispatchEvent(event);
  });
  return event;
}
const pasteRows = (...lines: string[]) =>
  paste([HEADER.join("\t"), ...lines].join("\n"));

function drop(file: File) {
  const event = new Event("drop", { bubbles: true, cancelable: true });
  Object.defineProperty(event, "dataTransfer", {
    value: { files: [file], types: ["Files"] },
  });
  act(() => {
    document.dispatchEvent(event);
  });
  return event;
}

// One reader per test: mounting a new one inside waitFor changes the DOM, which makes waitFor run it again.
let toastReader: { result: { current: readonly { message: string }[] } };
const toasts = () => toastReader.result.current.map((item) => item.message);
/** True when some toast holds all the pieces. */
const toasted = (...pieces: string[]) =>
  toasts().some((message) => pieces.every((piece) => message.includes(piece)));
const button = (name: string | RegExp) => screen.getByRole("button", { name });
const tableRow = (number: number) =>
  within(screen.getByRole("table")).getAllByRole("row")[number]!;

/** Waits until the level and language lists are there, so the run button can be pressed. */
async function pressRun(count: number) {
  const name = text(m.run, { count });
  await waitFor(() => expect(button(name)).toBeEnabled());
  fireEvent.click(button(name));
}
/** The level picker of a table row: a button named "<label>: <chosen level>". */
const levelPicker = (number: number) =>
  screen.getByRole("button", {
    name: new RegExp(`^${text(m.levelOf, { number })}:`),
  });

async function publish(count: number, className?: string) {
  fireEvent.click(
    await screen.findByRole("button", {
      name: text(m.publishCount, { count }),
    }),
  );
  const dialog = await screen.findByRole("dialog");
  if (className) {
    const select = within(dialog).getByRole("combobox");
    await waitFor(() => expect(select).toBeEnabled());
    fireEvent.change(select, { target: { value: "c1" } });
  }
  fireEvent.click(
    within(dialog).getByRole("button", {
      name: text(m.publish.confirm, { count }),
    }),
  );
}

let server: FakeServer;

const show = (canAssign = true) =>
  render(
    withTestProviders(
      <ProblemImportView
        basePath="/instructor/problems"
        canAssign={canAssign}
      />,
    ),
  );

beforeEach(() => {
  resetApiMock();
  server = fakeAuthoringServer();
  act(() => toast.clear());
  toastReader = renderHook(() => useToasts());
});

describe("bringing problems in", () => {
  it("reads the shipped template into five rows", async () => {
    show();
    fireEvent.change(screen.getByTestId("problem-import-file"), {
      target: { files: [xlsx()] },
    });
    await screen.findByRole("table");
    expect(within(screen.getByRole("table")).getAllByRole("row")).toHaveLength(
      6,
    );
    expect(tableRow(1)).toHaveTextContent("8/8");
  });

  it("takes a dropped file", async () => {
    show();
    expect(drop(xlsx()).defaultPrevented).toBe(true);
    expect(await screen.findByRole("table")).toBeInTheDocument();
  });

  it.each([
    ["a.csv", bytes, m.error.file.extension],
    ["a.xlsx", new Uint8Array([1, 2, 3]), m.error.file.notXlsx],
  ])("refuses %s with a reason", async (name, content, message) => {
    show();
    drop(xlsx(name, content));
    await waitFor(() => expect(toasts()).toContain(message));
    expect(screen.queryByRole("table")).not.toBeInTheDocument();
  });

  it("takes pasted cells, but leaves a paste inside a field alone", () => {
    show();
    expect(pasteRows(line("A")).defaultPrevented).toBe(true);
    expect(tableRow(1)).toHaveTextContent("A");
    const field = screen.getByLabelText(m.field.title);
    expect(paste("x\ty", field).defaultPrevented).toBe(false);
    expect(within(screen.getByRole("table")).getAllByRole("row")).toHaveLength(
      2,
    );
  });

  it("stops at 20 problems and says what was left out", () => {
    show();
    pasteRows(...Array.from({ length: 23 }, (_, i) => line(`B${i}`)));
    expect(within(screen.getByRole("table")).getAllByRole("row")).toHaveLength(
      21,
    );
    expect(
      screen.getByText(
        text(m.notice.leftOver, { max: 20, taken: 20, left: 3 }),
      ),
    ).toBeInTheDocument();
    expect(button(m.addRow)).toBeDisabled();
  });

  it("names what is wrong and what is missing; a broken row is not counted in the run", async () => {
    show();
    pasteRows(
      line("A"),
      line("B").replace("\tDễ\t", "\tSiêu khó\t"),
      ["C", "Dễ", "", "Đề"].join("\t"),
    );
    await waitFor(() => expect(tableRow(1)).toHaveTextContent(m.status.ready));
    // The list only shows a status; the reasons are toasts.
    expect(tableRow(2)).toHaveTextContent(m.status.blocked);
    // Cell 4 is the status: only the badge, no reason.
    expect(within(tableRow(2)).getAllByRole("cell")[4]).toHaveTextContent(
      new RegExp(`^${m.status.blocked}$`),
    );
    expect(tableRow(3)).toHaveTextContent(m.status.incomplete);
    expect(toasts()).toContain(text(m.blockedOnIntake, { count: 1 }));
    fireEvent.click(within(tableRow(2)).getByRole("button", { name: "B" }));
    expect(toasted("Bài 2 (B)", 'Không có độ khó "Siêu khó"')).toBe(true);
    fireEvent.click(within(tableRow(3)).getByRole("button", { name: "C" }));
    expect(toasted("Bài 3 (C)", "8 testcase")).toBe(true);
    expect(button(text(m.run, { count: 2 }))).toBeInTheDocument();

    fireEvent.click(levelPicker(2));
    fireEvent.click(screen.getByRole("option", { name: "Dễ" }));
    expect(button(text(m.run, { count: 3 }))).toBeInTheDocument();
  });

  it("warns about a title the bank already has", async () => {
    server.otherTitles = ["tổng SỐ", "Tổng số khác"];
    show();
    pasteRows(line("Tổng số"), line("Khác hẳn"));
    await waitFor(() =>
      expect(toasts()).toContain(text(m.titlesExist, { titles: "Tổng số" })),
    );
  });
});

describe("creating and running", () => {
  it("creates drafts one by one and shows each verdict", async () => {
    server.failsAt["print(0)"] = 3;
    show();
    pasteRows(
      line("A"),
      line("B", "print(0)"),
      ["C", "Dễ", "", "Đề"].join("\t"),
    );
    await pressRun(3);
    await waitFor(() => expect(tableRow(3)).toHaveTextContent(m.status.draft));
    expect(tableRow(1)).toHaveTextContent("Đạt 8/8");
    expect(tableRow(2)).toHaveTextContent("Sai testcase 3");
    expect(tableRow(2)).not.toHaveTextContent("sai kết quả");
    expect(toasted("Bài 2 (B)", "sai kết quả")).toBe(true);
    expect(toasts()).toContain(text(m.runDone, { pass: 1, bad: 1, draft: 1 }));
    expect(server.problems.map((p) => p.content.title)).toEqual([
      "A",
      "B",
      "C",
    ]);
    expect(server.problems.every((p) => p.status === "UNPUBLISHED")).toBe(true);
  });

  it("a fixed row updates its problem instead of creating another", async () => {
    server.failsAt["print(0)"] = 3;
    show();
    pasteRows(line("A", "print(0)"));
    await pressRun(1);
    await waitFor(() =>
      expect(tableRow(1)).toHaveTextContent("Sai testcase 3"),
    );
    fireEvent.click(button(m.editor.solution));
    fireEvent.change(screen.getByLabelText(m.field.solution), {
      target: { value: "print(1)" },
    });
    expect(tableRow(1)).toHaveTextContent(m.status.changed);
    await pressRun(1);
    await waitFor(() => expect(tableRow(1)).toHaveTextContent("Đạt 8/8"));
    expect(server.problems).toHaveLength(1);
    expect(server.problems[0]!.testcases).toHaveLength(8);
    expect(server.problems[0]!.content.referenceSolution).toMatchObject({
      sourceCode: "print(1)",
    });
  });

  it("an unreadable create is not repeated until the author says so", async () => {
    let lost = true;
    server.after = (call) => {
      if (call === "POST " && lost) {
        lost = false;
        throw new ApiError("UNKNOWN_ERROR", 0);
      }
    };
    show();
    pasteRows(line("A"), line("B"));
    await pressRun(2);
    await waitFor(() => expect(tableRow(2)).toHaveTextContent("Đạt 8/8"));
    expect(tableRow(1)).toHaveTextContent(m.status.unknown);
    expect(tableRow(1)).not.toHaveTextContent(m.status.unknownDetail);
    expect(toasted("Bài 1 (A)", m.status.unknownDetail)).toBe(true);
    expect(screen.queryByRole("button", { name: /Tạo nháp/ })).toBeNull();
    // An edit does not lift it either.
    fireEvent.change(screen.getByLabelText(m.field.title), {
      target: { value: "A2" },
    });
    expect(tableRow(1)).toHaveTextContent(m.status.unknown);
    fireEvent.click(button(m.createAgain));
    await pressRun(1);
    await waitFor(() => expect(tableRow(1)).toHaveTextContent("Đạt 8/8"));
  });

  it("a server failure keeps the created problem and the next run finishes it", async () => {
    let broken = true;
    server.before = (call) => {
      if (broken && call === "POST /p1/reference-solution/run")
        throw new ApiError("PRB-050", 503);
    };
    show();
    pasteRows(line("A"));
    await pressRun(1);
    await waitFor(() =>
      expect(tableRow(1)).toHaveTextContent(m.status.errorStored),
    );
    expect(toasted("Bài 1 (A)", apiErrors["PRB-050"]!)).toBe(true);
    broken = false;
    await pressRun(1);
    await waitFor(() => expect(tableRow(1)).toHaveTextContent("Đạt 8/8"));
    expect(server.problems).toHaveLength(1);
  });

  it("removing a created row says the draft stays", async () => {
    show();
    pasteRows(line("A"));
    await pressRun(1);
    await waitFor(() => expect(tableRow(1)).toHaveTextContent("Đạt 8/8"));
    fireEvent.click(button(text(m.removeRow, { number: 1 })));
    expect(toasts()).toContain(m.removedKeepsDraft);
    expect(server.problems).toHaveLength(1);
  });
});

describe("publishing", () => {
  async function ready(...titles: string[]) {
    pasteRows(...titles.map((title) => line(title)));
    await pressRun(titles.length);
    await waitFor(() =>
      expect(tableRow(titles.length)).toHaveTextContent("Đạt 8/8"),
    );
  }

  it("publishes one call per problem and gives them to the class", async () => {
    show();
    await ready("A", "B");
    server.writes.length = 0;
    await publish(2, "Lớp K66");
    await waitFor(() =>
      expect(server.assigned.c1).toEqual(["p1", expect.any(String)]),
    );
    expect(
      server.writes.filter((call) => call.includes("bulk/publish")),
    ).toHaveLength(2);
    expect(server.problems.every((p) => p.status === "PUBLISHED")).toBe(true);
    await waitFor(() =>
      expect(tableRow(1)).toHaveTextContent(m.status.published),
    );
    expect(levelPicker(1)).toBeDisabled();
    expect(screen.getByLabelText(m.field.title)).toBeDisabled();
  });

  it("only gives the class what was published", async () => {
    let calls = 0;
    server.before = (call) => {
      if (call.includes("bulk/publish") && (calls += 1) === 1)
        throw new ApiError("PRB-050", 503);
    };
    show();
    await ready("A", "B");
    await publish(2, "Lớp K66");
    await waitFor(() => expect(server.assigned.c1).toHaveLength(1));
    const [first, second] = server.problems;
    expect(server.assigned.c1).toEqual([second!.id]);
    expect(first!.status).toBe("UNPUBLISHED");
    expect(tableRow(1)).toHaveTextContent(m.status.publishError);
    expect(button(text(m.publishCount, { count: 1 }))).toBeEnabled();
  });

  it("a failed assignment keeps the problems published and can be repeated", async () => {
    let broken = true;
    server.before = (call) => {
      if (broken && call.includes("/assignments"))
        throw new ApiError("PRB-058", 503);
    };
    show();
    await ready("A");
    await publish(1, "Lớp K66");
    await screen.findByText(
      text(m.assignFailed, { count: 1, className: "Lớp K66" }),
    );
    expect(server.problems[0]!.status).toBe("PUBLISHED");
    broken = false;
    await waitFor(() => expect(button(m.assignAgain)).toBeEnabled());
    fireEvent.click(button(m.assignAgain));
    await waitFor(() => expect(server.assigned.c1).toEqual(["p1"]));
    await waitFor(() =>
      expect(screen.queryByRole("button", { name: m.assignAgain })).toBeNull(),
    );
  });

  it("the admin area has no class picker", async () => {
    show(false);
    await ready("A");
    fireEvent.click(button(text(m.publishCount, { count: 1 })));
    expect(
      within(await screen.findByRole("dialog")).queryByRole("combobox"),
    ).toBeNull();
  });
});
