// The "Đặc tả" tab body: one shared signature with a stub preview per language, stdin/stdout formats,
// matching strategy.
import { useState } from "react";
import {
  act,
  fireEvent,
  render,
  renderHook,
  screen,
  within,
} from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import type { ProblemSpec } from "@/entities/problem";
import { toast, useToasts } from "@/shared/lib/toast-store";
import { SpecEditor } from "./spec-editor";

vi.mock("@/shared/i18n", () => ({
  useT: () => (key: string, params?: Record<string, unknown>) =>
    params ? `${key} ${JSON.stringify(params)}` : key,
}));

const initial: ProblemSpec = {
  signature: {
    functionName: "min_window",
    returnType: { kind: "STRING" },
    parameters: [
      { id: "p1", name: "s", type: { kind: "STRING" } },
      { id: "p2", name: "t", type: { kind: "STRING" } },
    ],
    nameOverrides: {},
  },
  stdinFormat: "Dòng 1: s",
  stdoutFormat: "Một dòng",
  matchingStrategy: "EXACT",
  epsilon: "",
};

function Harness() {
  const [spec, setSpec] = useState(initial);
  return (
    <>
      <SpecEditor spec={spec} onChange={setSpec} />
      <output data-testid="spec">{JSON.stringify(spec)}</output>
    </>
  );
}

const current = (): ProblemSpec =>
  JSON.parse(screen.getByTestId("spec").textContent!);
const row = (language: string) =>
  screen.getByRole("listitem", { name: `spec.language.${language}` });

beforeEach(() => act(() => toast.clear()));

describe("SpecEditor", () => {
  it("declares the signature once and previews the starter line of each language", () => {
    render(<Harness />);
    expect(screen.getAllByLabelText("spec.functionName")).toHaveLength(1);
    expect(screen.getByLabelText('spec.parameterName {"index":1}')).toHaveValue(
      "s",
    );

    expect(
      within(row("python")).getByText("def min_window(s: str, t: str) -> str:"),
    ).toBeInTheDocument();
    expect(
      within(row("java")).getByText(
        "public String minWindow(String s, String t)",
      ),
    ).toBeInTheDocument();
    expect(
      within(row("cpp")).getByText("string minWindow(string s, string t)"),
    ).toBeInTheDocument();
  });

  it("renaming the shared function updates every language, an override changes only its own", () => {
    render(<Harness />);
    fireEvent.change(screen.getByLabelText("spec.functionName"), {
      target: { value: "smallestWindow" },
    });
    expect(
      within(row("python")).getByText(
        "def smallest_window(s: str, t: str) -> str:",
      ),
    ).toBeInTheDocument();
    expect(
      within(row("java")).getByText(
        "public String smallestWindow(String s, String t)",
      ),
    ).toBeInTheDocument();

    fireEvent.change(
      within(row("java")).getByLabelText(
        'spec.overrideName {"language":"spec.language.java"}',
      ),
      {
        target: { value: "solve" },
      },
    );
    expect(
      within(row("java")).getByText("public String solve(String s, String t)"),
    ).toBeInTheDocument();
    expect(
      within(row("cpp")).getByText("string smallestWindow(string s, string t)"),
    ).toBeInTheDocument();
    expect(current().signature.nameOverrides).toEqual({ java: "solve" });
  });

  it("adds a parameter and removes one", () => {
    render(<Harness />);
    fireEvent.click(screen.getByRole("button", { name: "spec.addParameter" }));
    expect(current().signature.parameters).toHaveLength(3);
    expect(
      within(row("python")).getByText(
        "def min_window(s: str, t: str, ?: int) -> str:",
      ),
    ).toBeInTheDocument();

    fireEvent.click(
      screen.getByRole("button", { name: 'spec.removeParameter {"index":1}' }),
    );
    expect(current().signature.parameters.map((item) => item.name)).toEqual([
      "t",
      "",
    ]);
  });

  it("gives an array its element kind and dimensions, and shows it in each language", () => {
    render(<Harness />);
    fireEvent.change(screen.getByLabelText("spec.returnType"), {
      target: { value: "ARRAY" },
    });
    expect(current().signature.returnType).toEqual({
      kind: "ARRAY",
      of: { kind: "INT" },
      dimensions: 1,
    });

    fireEvent.change(
      screen.getByLabelText('spec.dimensions {"type":"spec.returnType"}'),
      {
        target: { value: "2" },
      },
    );
    expect(current().signature.returnType.dimensions).toBe(2);
    expect(
      within(row("java")).getByText(/^public int\[\]\[\] minWindow/),
    ).toBeInTheDocument();
    expect(
      within(row("cpp")).getByText(/^vector<vector<int>> minWindow/),
    ).toBeInTheDocument();
  });

  it("builds a nested list by stacking element pickers (List of List of string)", () => {
    render(<Harness />);
    fireEvent.change(screen.getByLabelText("spec.returnType"), {
      target: { value: "LIST" },
    });
    // The element picker is named after its parent; pick another LIST, then its element.
    const level2 = screen.getByLabelText(
      'spec.elementKind {"type":"spec.returnType"}',
    );
    fireEvent.change(level2, { target: { value: "LIST" } });
    const level3 = screen.getByLabelText(
      'spec.elementKind {"type":"spec.elementKind {\\"type\\":\\"spec.returnType\\"}"}',
    );
    fireEvent.change(level3, { target: { value: "STRING" } });

    expect(current().signature.returnType).toEqual({
      kind: "LIST",
      of: { kind: "LIST", of: { kind: "STRING" } },
    });
    expect(
      within(row("java")).getByText(/^public List<List<String>> minWindow/),
    ).toBeInTheDocument();
    expect(
      within(row("python")).getByText(
        "def min_window(s: str, t: str) -> List[List[str]]:",
      ),
    ).toBeInTheDocument();
  });

  it("only offers a plain value as the element of an array", () => {
    render(<Harness />);
    fireEvent.change(screen.getByLabelText("spec.returnType"), {
      target: { value: "ARRAY" },
    });
    const element = screen.getByLabelText(
      'spec.elementKind {"type":"spec.returnType"}',
    );
    expect(
      within(element).queryByRole("option", { name: "spec.kind.LIST" }),
    ).not.toBeInTheDocument();
    expect(
      within(element).getByRole("option", { name: "spec.kind.STRING" }),
    ).toBeInTheDocument();
  });

  it("warns that the problem is Standard I/O only when a type falls outside the schema", () => {
    render(<Harness />);
    expect(
      screen.queryByText("spec.wrapperUnsupportedTitle"),
    ).not.toBeInTheDocument();
    fireEvent.change(screen.getByLabelText("spec.returnType"), {
      target: { value: "OTHER" },
    });
    expect(
      screen.getByText("spec.wrapperUnsupportedTitle"),
    ).toBeInTheDocument();
  });

  it("shows the epsilon field only for EPSILON", () => {
    render(<Harness />);
    expect(screen.queryByLabelText("spec.epsilon")).not.toBeInTheDocument();
    fireEvent.change(screen.getByLabelText("spec.matchingStrategy"), {
      target: { value: "EPSILON" },
    });
    fireEvent.change(screen.getByLabelText("spec.epsilon"), {
      target: { value: "0.001" },
    });
    expect(current().epsilon).toBe("0.001");
  });

  it("blocks UNORDERED_SET for a tree return type, in both directions, with a toast", () => {
    const toasts = renderHook(() => useToasts());
    render(<Harness />);

    // Return type first: the strategy option goes disabled.
    fireEvent.change(screen.getByLabelText("spec.returnType"), {
      target: { value: "BINARY_TREE" },
    });
    expect(
      screen.getByRole("option", { name: "spec.strategy.UNORDERED_SET.label" }),
    ).toBeDisabled();

    // Back to a scalar, pick UNORDERED_SET, then try the tree again: refused.
    fireEvent.change(screen.getByLabelText("spec.returnType"), {
      target: { value: "INT" },
    });
    fireEvent.change(screen.getByLabelText("spec.matchingStrategy"), {
      target: { value: "UNORDERED_SET" },
    });
    fireEvent.change(screen.getByLabelText("spec.returnType"), {
      target: { value: "BINARY_TREE" },
    });
    expect(toasts.result.current.at(-1)).toMatchObject({
      tone: "error",
      message: "spec.errorUnorderedSet",
    });
    expect(current().signature.returnType.kind).toBe("INT");
  });
});
