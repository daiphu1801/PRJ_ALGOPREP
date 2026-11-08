// "Đặc tả" tab on the REAL api path: the declaration lines come from the server (F3 P6), and the
// local sketch is only a fallback. Mock-data behaviour stays in spec-editor.test.tsx.
import { useState } from "react";
import { render, screen, waitFor, within } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "@/shared/api";
import { apiMock, resetApiMock } from "@/shared/test/real-api";
import { withTestProviders } from "@/shared/test/render-with-providers";
import type { ProblemSpec } from "@/entities/problem";
import { SpecEditor } from "./spec-editor";

vi.mock("@/shared/api", async (importOriginal) =>
  (await import("@/shared/test/real-api")).realApiModule(
    await importOriginal<Record<string, unknown>>(),
  ),
);

const base: ProblemSpec = {
  signature: {
    functionName: "min_window",
    returnType: { kind: "STRING" },
    parameters: [{ id: "p1", name: " s ", type: { kind: "STRING" } }],
    nameOverrides: { java: " minimumWindow ", cpp: "" },
  },
  stdinFormat: "in",
  stdoutFormat: "out",
  matchingStrategy: "EXACT",
  epsilon: "",
};

function Harness({ initial = base }: { initial?: ProblemSpec }) {
  const [spec, setSpec] = useState(initial);
  return <SpecEditor spec={spec} onChange={setSpec} />;
}

const SERVER = {
  wrapperSupported: true,
  declarations: {
    JAVA: "SERVER-JAVA public String minimumWindow(String s)",
    CPP: "SERVER-CPP string minWindow(string s)",
    PYTHON: "SERVER-PY def min_window(self, s: str) -> str",
  },
};

function row(name: RegExp) {
  return screen.getByRole("listitem", { name });
}

describe("SpecEditor on the real API", () => {
  beforeEach(() => resetApiMock());

  it("posts the trimmed draft and shows the server's declaration for each language", async () => {
    apiMock.post.mockResolvedValue(SERVER);
    render(withTestProviders(<Harness />));
    await waitFor(() =>
      expect(apiMock.post).toHaveBeenCalledWith(
        "/harness/function-signature/preview",
        {
          functionName: "min_window",
          returnType: { kind: "STRING" },
          parameters: [{ name: "s", type: { kind: "STRING" } }],
          nameOverrides: { JAVA: " minimumWindow " },
        },
      ),
    );
    expect(await screen.findByText(SERVER.declarations.JAVA)).toBeTruthy();
    expect(screen.getByText(SERVER.declarations.CPP)).toBeTruthy();
    expect(screen.getByText(SERVER.declarations.PYTHON)).toBeTruthy();
    expect(apiMock.post).toHaveBeenCalledTimes(1);
  });

  it("keeps the local sketch when the server refuses the draft", async () => {
    apiMock.post.mockRejectedValue(
      new ApiError("HRN-060", 422, "Invalid function signature draft"),
    );
    render(withTestProviders(<Harness />));
    await waitFor(() => expect(apiMock.post).toHaveBeenCalled());
    const java = row(/java/i);
    expect(
      within(java).getByText(/min_window|minWindow|minimumWindow/),
    ).toBeTruthy();
    expect(screen.queryByText(/SERVER-/)).toBeNull();
  });

  it("invents no signature when the server answers null for a language", async () => {
    apiMock.post.mockResolvedValue({
      wrapperSupported: false,
      declarations: { JAVA: null, CPP: null, PYTHON: null },
    });
    render(withTestProviders(<Harness />));
    await waitFor(() => expect(apiMock.post).toHaveBeenCalled());
    await waitFor(() =>
      expect(row(/java/i).querySelector("code")?.textContent).toBe(""),
    );
  });

  it("does not call the server for a signature that has no name yet", async () => {
    render(
      withTestProviders(
        <Harness
          initial={{
            ...base,
            signature: { ...base.signature, functionName: "  " },
          }}
        />,
      ),
    );
    await new Promise((resolve) => setTimeout(resolve, 600));
    expect(apiMock.post).not.toHaveBeenCalled();
  });
});
