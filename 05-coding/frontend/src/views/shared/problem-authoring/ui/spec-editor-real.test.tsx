// "Đặc tả" tab on the REAL api path: the declaration lines come from the server (F3 P6), and the
// local sketch is only a fallback. Mock-data behaviour stays in spec-editor.test.tsx.
import { useState } from "react";
import {
  act,
  fireEvent,
  render,
  renderHook,
  screen,
  waitFor,
} from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "@/shared/api";
import { apiMock, resetApiMock } from "@/shared/test/real-api";
import { withTestProviders } from "@/shared/test/render-with-providers";
import type { ProblemSpec } from "@/entities/problem";
import { SpecEditor } from "./spec-editor";
import messages from "../../../../../messages/vi.json";
import { toast, useToasts } from "@/shared/lib/toast-store";

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
  beforeEach(() => {
    resetApiMock();
    act(() => toast.clear());
  });

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
          matchingStrategy: "EXACT",
          epsilonValue: null,
        },
        expect.objectContaining({ signal: expect.any(AbortSignal) }),
      ),
    );
    expect(await screen.findByText(SERVER.declarations.JAVA)).toBeTruthy();
    fireEvent.click(
      screen.getByRole("button", {
        name: messages.problemAuthoring.spec.language.cpp,
      }),
    );
    expect(screen.getByText(SERVER.declarations.CPP)).toBeTruthy();
    fireEvent.click(
      screen.getByRole("button", {
        name: messages.problemAuthoring.spec.language.python,
      }),
    );
    expect(screen.getByText(SERVER.declarations.PYTHON)).toBeTruthy();
    expect(apiMock.post).toHaveBeenCalledTimes(1);
  });

  it("maps server violations to fields and does not present an invalid declaration as valid", async () => {
    apiMock.post.mockRejectedValue(
      new ApiError("HRN-060", 422, "Invalid function signature draft", {
        violations: [
          { code: "HRN-V11", path: "/functionName" },
          { code: "HRN-V10", path: "/parameters/0/type" },
        ],
      }),
    );
    render(withTestProviders(<Harness />));
    await waitFor(() => expect(apiMock.post).toHaveBeenCalled());
    await waitFor(() =>
      expect(
        screen.getByLabelText(messages.problemAuthoring.spec.functionName),
      ).toHaveAttribute("aria-invalid", "true"),
    );
    expect(
      screen.getByLabelText(messages.problemAuthoring.spec.functionName),
    ).toHaveAttribute("aria-invalid", "true");
    expect(
      screen.getByLabelText(
        messages.problemAuthoring.spec.parameterType.replace("{index}", "1"),
      ),
    ).toHaveAttribute("aria-invalid", "true");
    expect(row(/java/i).querySelector("code")?.textContent).toBe("");
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

  it("hides the old declaration during debounce, aborts the old request and ignores its late answer", async () => {
    let late!: (value: typeof SERVER) => void;
    let oldSignal!: AbortSignal;
    apiMock.post.mockImplementationOnce((_path, _body, options) => {
      oldSignal = options.signal;
      return new Promise((resolve) => {
        late = resolve;
      });
    });
    apiMock.post.mockResolvedValueOnce({
      ...SERVER,
      declarations: { ...SERVER.declarations, JAVA: "NEW exact declaration" },
    });
    render(withTestProviders(<Harness />));
    await waitFor(() => expect(apiMock.post).toHaveBeenCalledTimes(1));
    fireEvent.change(
      screen.getByLabelText(messages.problemAuthoring.spec.functionName),
      { target: { value: "new_name" } },
    );
    expect(oldSignal.aborted).toBe(true);
    await act(async () => {
      late(SERVER);
    });
    expect(screen.queryByText(SERVER.declarations.JAVA)).toBeNull();
    expect(apiMock.post).toHaveBeenCalledTimes(1);
    expect(await screen.findByText("NEW exact declaration")).toBeVisible();
  });

  it("does not show a cached server answer for the previous draft in the 400ms debounce window", async () => {
    apiMock.post.mockResolvedValue(SERVER);
    render(withTestProviders(<Harness />));
    await screen.findByText(SERVER.declarations.JAVA);
    fireEvent.change(
      screen.getByLabelText(messages.problemAuthoring.spec.functionName),
      { target: { value: "new_name" } },
    );
    expect(screen.queryByText(SERVER.declarations.JAVA)).toBeNull();
    expect(apiMock.post).toHaveBeenCalledTimes(1);
  });

  it("retries a 429 only after the server delay while the same draft remains active", async () => {
    apiMock.post
      .mockRejectedValueOnce(
        new ApiError("HRN-061", 429, undefined, { retryAfterSeconds: 1 }),
      )
      .mockResolvedValueOnce(SERVER);
    render(withTestProviders(<Harness />));
    await screen.findByText(
      messages.problemAuthoring.spec.preview.rateLimited.replace(
        "{seconds}",
        "1",
      ),
    );
    await new Promise((resolve) => setTimeout(resolve, 300));
    expect(apiMock.post).toHaveBeenCalledTimes(1);
    expect(
      await screen.findByText(SERVER.declarations.JAVA, {}, { timeout: 2000 }),
    ).toBeVisible();
    expect(apiMock.post).toHaveBeenCalledTimes(2);
  });

  it("cancels the queued 429 retry when the author changes the draft", async () => {
    apiMock.post
      .mockRejectedValueOnce(
        new ApiError("HRN-061", 429, undefined, { retryAfterSeconds: 1 }),
      )
      .mockResolvedValueOnce(SERVER);
    render(withTestProviders(<Harness />));
    await screen.findByText(
      messages.problemAuthoring.spec.preview.rateLimited.replace(
        "{seconds}",
        "1",
      ),
    );
    fireEvent.change(
      screen.getByLabelText(messages.problemAuthoring.spec.functionName),
      { target: { value: "changed_draft" } },
    );
    await screen.findByText(SERVER.declarations.JAVA);
    await new Promise((resolve) => setTimeout(resolve, 1100));
    expect(apiMock.post).toHaveBeenCalledTimes(2);
    expect(apiMock.post.mock.calls[1]?.[1]).toMatchObject({
      functionName: "changed_draft",
    });
  });

  it("displays server unsupported reasons even when the local signature rules accept the draft", async () => {
    apiMock.post.mockResolvedValue({
      wrapperSupported: false,
      unsupportedReasons: [{ code: "HRN-V05", path: "/returnType" }],
      declarations: { JAVA: null, CPP: null, PYTHON: null },
    });
    render(withTestProviders(<Harness />));
    expect(
      await screen.findByText(
        messages.problemAuthoring.spec.preview.reasons["HRN-V05"],
      ),
    ).toBeVisible();
    expect(row(/java/i).querySelector("code")?.textContent).toBe("");
  });

  it("shows the translated 413 without leaking the backend detail", async () => {
    const toasts = renderHook(() => useToasts());
    apiMock.post.mockRejectedValue(
      new ApiError("HRN-062", 413, "raw backend detail"),
    );
    render(withTestProviders(<Harness />));
    await waitFor(() =>
      expect(toasts.result.current.at(-1)?.message).toBe(
        messages.apiErrors["HRN-062"],
      ),
    );
    expect(screen.queryByText("raw backend detail")).toBeNull();
  });
});
