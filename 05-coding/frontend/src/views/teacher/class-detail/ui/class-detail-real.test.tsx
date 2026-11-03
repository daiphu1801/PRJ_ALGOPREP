// View-level tests of INS0205 against the REAL api path (03-dd/api/F1_identity.md #19, #21 to #24, #26,
// #27): only the HTTP layer is faked. What matters: the three regions load on their own, the roster
// shows the contact details the server has and none of the F4 columns, a new code lands in the list
// again from the server, and a refused removal reads as its error code.
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
const push = vi.fn();
vi.mock("next/navigation", () => ({ useRouter: () => ({ push }) }));

// Import after the mocks.
import { apiMock, resetApiMock } from "@/shared/test/real-api";
import { ClassDetailView } from "./class-detail-view";

const m = viMessages.classDetail;
const mm = viMessages.classManagement;
const apiErrors = viMessages.apiErrors as Record<string, string>;
const BASE = "/identity/classes";

const CLASS = {
  id: "c1",
  name: "Lập trình Java K21",
  description: null,
  schedule: null,
  studentCount: 1,
  createdAt: "2026-11-01T03:00:00Z",
};
const STUDENT = {
  studentId: "s1",
  displayName: "Nguyễn An",
  email: "an@x.vn",
  phone: "0901234567",
  joinedAt: "2026-11-01T03:00:00Z",
  status: null,
};

type Serve = {
  codes?: unknown[] | Error;
  students?: unknown[] | Error;
};

function serve({ codes = [], students = [STUDENT] }: Serve = {}) {
  const answer = (value: unknown[] | Error) =>
    value instanceof Error
      ? Promise.reject(value)
      : Promise.resolve({ items: value });
  apiMock.get.mockImplementation((path: string) => {
    if (path === `${BASE}/my`) return Promise.resolve({ items: [CLASS] });
    if (path === `${BASE}/c1/invite-codes`) return answer(codes);
    if (path === `${BASE}/c1/students`) return answer(students);
    return Promise.reject(new Error(`unexpected GET ${path}`));
  });
}

function watch() {
  const toasts = renderHook(() => useToasts());
  act(() => toast.clear());
  return () => toasts.result.current.map((item) => [item.tone, item.message]);
}

async function open() {
  render(withTestProviders(<ClassDetailView classId="c1" />));
  await screen.findByRole("heading", { name: CLASS.name });
}

const callsOf = (path: string) =>
  apiMock.get.mock.calls.filter(([p]) => p === path).length;

describe("ClassDetailView with the real API", () => {
  beforeEach(() => {
    resetApiMock();
    push.mockReset();
    serve();
  });

  it("shows the roster with email and phone and none of the F4 columns", async () => {
    await open();

    const table = await screen.findByRole("table", {
      name: m.students.title,
    });
    expect(await within(table).findByText("Nguyễn An")).toBeInTheDocument();
    expect(within(table).getByText("an@x.vn")).toBeInTheDocument();
    expect(within(table).getByText("0901234567")).toBeInTheDocument();
    for (const column of ["status", "avgScore", "completion", "lastActive"]) {
      expect(
        within(table).queryByText(
          m.students.col[column as keyof typeof m.students.col],
        ),
      ).toBeNull();
    }
  });

  it("says the class is not found when the list from the server does not have it", async () => {
    apiMock.get.mockImplementation((path: string) =>
      Promise.resolve(path === `${BASE}/my` ? { items: [] } : { items: [] }),
    );
    render(withTestProviders(<ClassDetailView classId="c1" />));
    expect(await screen.findByText(m.notFound)).toBeInTheDocument();
  });

  it("creates a code, then shows the server's list again", async () => {
    apiMock.post.mockResolvedValue({
      id: "i2",
      code: "WXYZ6789",
      expiresAt: "2026-11-09T05:00:00Z",
    });
    const toasts = watch();
    await open();
    const before = callsOf(`${BASE}/c1/invite-codes`);
    serve({
      codes: [
        { id: "i2", code: "WXYZ6789", expiresAt: "2026-11-09T05:00:00Z" },
      ],
    });

    fireEvent.click(screen.getByRole("button", { name: m.invite.create }));

    await waitFor(() =>
      expect(apiMock.post).toHaveBeenCalledWith(`${BASE}/c1/invite-codes`),
    );
    expect(await screen.findByText("WXYZ6789")).toBeInTheDocument();
    expect(callsOf(`${BASE}/c1/invite-codes`)).toBeGreaterThan(before);
    expect(toasts().at(-1)).toEqual(["success", mm.toast.inviteCreated]);
  });

  it("keeps the other regions when the codes fail to load", async () => {
    serve({ codes: new ApiError("IDT-414", 404) });
    await open();

    expect(
      await screen.findByRole("button", { name: "Thử lại" }),
    ).toBeInTheDocument();
    expect(await screen.findByText("Nguyễn An")).toBeInTheDocument();
  });

  it("removes a student by class and student id, and reads a refusal by its code", async () => {
    apiMock.post.mockRejectedValueOnce(new ApiError("IDT-414", 404));
    apiMock.post.mockResolvedValueOnce({
      classId: "c1",
      studentId: "s1",
      leftAt: "2026-11-02T00:00:00Z",
    });
    const toasts = watch();
    await open();
    await screen.findByText("Nguyễn An");
    const confirm = async () => {
      fireEvent.click(
        screen.getByRole("button", { name: `${m.students.remove} Nguyễn An` }),
      );
      fireEvent.click(
        within(await screen.findByRole("dialog")).getByRole("button", {
          name: mm.popup.removeStudentConfirm,
        }),
      );
    };

    await confirm();
    await waitFor(() =>
      expect(toasts().at(-1)).toEqual(["error", apiErrors["IDT-414"]]),
    );

    await confirm();
    await waitFor(() =>
      expect(toasts().at(-1)).toEqual([
        "success",
        m.toast.studentRemoved.replace("{name}", "Nguyễn An"),
      ]),
    );
    expect(apiMock.post).toHaveBeenLastCalledWith(
      `${BASE}/c1/students/s1/leave`,
    );
  });

  it("keeps the edit dialog open with the typed name when the server refuses", async () => {
    apiMock.patch.mockRejectedValue(new ApiError("IDT-V14", 422));
    const toasts = watch();
    await open();

    fireEvent.click(screen.getByRole("button", { name: m.edit }));
    fireEvent.change(
      screen.getByLabelText(new RegExp(mm.popup.classNameLabel)),
      {
        target: { value: "Tên mới" },
      },
    );
    fireEvent.click(screen.getByRole("button", { name: mm.popup.saveClass }));

    await waitFor(() =>
      expect(toasts().at(-1)).toEqual(["error", apiErrors["IDT-V14"]]),
    );
    expect(screen.getByRole("dialog")).toBeInTheDocument();
    expect(
      screen.getByLabelText(new RegExp(mm.popup.classNameLabel)),
    ).toHaveValue("Tên mới");
  });
});
