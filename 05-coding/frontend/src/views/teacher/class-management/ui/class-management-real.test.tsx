// View-level tests of INS0201 against the REAL api path (03-dd/api/F1_identity.md #19 to #22): only
// the HTTP layer is faked. What matters: the list is the server's and shows no invented F4 figures, a
// refused save keeps the dialog and what was typed, and a refusal reads as its error code.
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
import { ClassManagementView } from "./class-management-view";

const m = viMessages.classManagement;
const apiErrors = viMessages.apiErrors as Record<string, string>;

const CLASS = {
  id: "c1",
  name: "Lập trình Java K21",
  description: "Mô tả cũ",
  schedule: null,
  studentCount: 12,
  createdAt: "2026-11-01T03:00:00Z",
};

function watch() {
  const toasts = renderHook(() => useToasts());
  act(() => toast.clear());
  return () => toasts.result.current.map((item) => [item.tone, item.message]);
}

async function open() {
  render(withTestProviders(<ClassManagementView />));
  await screen.findByText(CLASS.name);
}

function typeName(value: string) {
  fireEvent.change(screen.getByLabelText(new RegExp(m.popup.classNameLabel)), {
    target: { value },
  });
}

describe("ClassManagementView with the real API", () => {
  beforeEach(() => {
    resetApiMock();
    push.mockReset();
    apiMock.get.mockResolvedValue({ items: [CLASS] });
  });

  it("lists the server's classes with the student count and no completion, average or absent column", async () => {
    await open();

    expect(apiMock.get).toHaveBeenCalledWith("/identity/classes/my");
    const table = screen.getByRole("table", { name: m.header.title });
    expect(within(table).getByText("12")).toBeInTheDocument();
    expect(within(table).queryByText(m.classList.col.completionBar)).toBeNull();
    expect(within(table).queryByText(m.classList.col.avgScore)).toBeNull();
    expect(within(table).queryByText(m.classList.col.absentCount)).toBeNull();
  });

  it("creates the class on the server, toasts and opens its page by the returned id", async () => {
    apiMock.post.mockResolvedValue({
      ...CLASS,
      id: undefined,
      classId: "new-id",
      name: "Lớp mới",
      inviteCode: {
        id: "i",
        code: "ABCD2345",
        expiresAt: "2026-11-09T00:00:00Z",
      },
    });
    const toasts = watch();
    await open();

    fireEvent.click(
      screen.getByRole("button", { name: m.header.btnCreateClass }),
    );
    typeName("Lớp mới");
    fireEvent.click(screen.getByRole("button", { name: m.popup.saveClass }));

    await waitFor(() =>
      expect(push).toHaveBeenCalledWith("/instructor/classes/new-id"),
    );
    expect(apiMock.post).toHaveBeenCalledWith("/identity/classes", {
      name: "Lớp mới",
    });
    expect(toasts().at(-1)).toEqual([
      "success",
      m.toast.created.replace("{name}", "Lớp mới"),
    ]);
    await waitFor(() => expect(screen.queryByRole("dialog")).toBeNull());
  });

  it("keeps the dialog and the typed name when the server refuses, and reads the reason from the code", async () => {
    apiMock.post.mockRejectedValue(new ApiError("IDT-V14", 422));
    const toasts = watch();
    await open();

    fireEvent.click(
      screen.getByRole("button", { name: m.header.btnCreateClass }),
    );
    typeName("Tên bị từ chối");
    fireEvent.click(screen.getByRole("button", { name: m.popup.saveClass }));

    await waitFor(() =>
      expect(toasts().at(-1)).toEqual(["error", apiErrors["IDT-V14"]]),
    );
    expect(screen.getByRole("dialog")).toBeInTheDocument();
    expect(
      screen.getByLabelText(new RegExp(m.popup.classNameLabel)),
    ).toHaveValue("Tên bị từ chối");
    expect(push).not.toHaveBeenCalled();
  });

  it("edits with a PATCH that clears what the form left blank", async () => {
    apiMock.patch.mockResolvedValue({
      ...CLASS,
      name: "Đổi tên",
      description: null,
    });
    const toasts = watch();
    await open();

    fireEvent.click(
      screen.getByRole("button", {
        name: m.classList.col.menuEditOf.replace("{name}", CLASS.name),
      }),
    );
    typeName("Đổi tên");
    fireEvent.change(screen.getByLabelText(m.popup.descriptionLabel), {
      target: { value: "" },
    });
    fireEvent.click(screen.getByRole("button", { name: m.popup.saveClass }));

    await waitFor(() =>
      expect(apiMock.patch).toHaveBeenCalledWith("/identity/classes/c1", {
        name: "Đổi tên",
        description: "",
        schedule: {},
      }),
    );
    await waitFor(() =>
      expect(toasts().at(-1)).toEqual([
        "success",
        m.toast.updated.replace("{name}", "Đổi tên"),
      ]),
    );
  });

  it("deletes on the server and reads a refusal by its code", async () => {
    apiMock.delete.mockRejectedValueOnce(new ApiError("IDT-414", 404));
    apiMock.delete.mockResolvedValueOnce(undefined);
    const toasts = watch();
    await open();
    const confirm = async () => {
      fireEvent.click(
        screen.getByRole("button", {
          name: m.classList.col.menuDeleteOf.replace("{name}", CLASS.name),
        }),
      );
      fireEvent.click(
        within(await screen.findByRole("dialog")).getByRole("button", {
          name: m.popup.deleteClassConfirm,
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
        m.toast.deleted.replace("{name}", CLASS.name),
      ]),
    );
    expect(apiMock.delete).toHaveBeenLastCalledWith("/identity/classes/c1");
  });
});
