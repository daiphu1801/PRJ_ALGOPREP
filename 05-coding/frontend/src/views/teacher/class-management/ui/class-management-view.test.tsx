// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
import {
  act,
  fireEvent,
  render,
  renderHook,
  screen,
  waitFor,
  within,
} from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { toast, useToasts } from "@/shared/lib/toast-store";
import { withTestProviders } from "@/shared/test/render-with-providers";
import { ClassManagementView } from "./class-management-view";

const push = vi.fn();
vi.mock("next/navigation", () => ({ useRouter: () => ({ push }) }));

describe("ClassManagementView", () => {
  it("renders the classes as a list and no student table", async () => {
    render(withTestProviders(<ClassManagementView />));

    expect(await screen.findByText("Lập trình Java K21")).toBeInTheDocument();
    // The student table moved to class_progress on 2026-09-27 — this screen manages classes only,
    // as one list of classes (2026-10-11).
    expect(
      screen.getByRole("table", { name: "Lớp của tôi" }),
    ).toBeInTheDocument();
    expect(
      screen.queryByRole("table", { name: "Danh sách học viên" }),
    ).not.toBeInTheDocument();
  });

  it("raises a success toast after deleting a class", async () => {
    const toasts = renderHook(() => useToasts());
    act(() => toast.clear());
    render(withTestProviders(<ClassManagementView />));

    fireEvent.click(
      (await screen.findAllByRole("button", { name: /^Xoá lớp / }))[0]!,
    );
    fireEvent.click(
      within(await screen.findByRole("dialog")).getByRole("button", {
        name: "Xoá lớp",
      }),
    );

    await waitFor(() =>
      expect(toasts.result.current.map((item) => item.tone)).toEqual([
        "success",
      ]),
    );
    expect(toasts.result.current[0]!.message).not.toMatch(/^toast\./);
  });

  it("opens the edit form with the class's current name, description and schedule", async () => {
    render(withTestProviders(<ClassManagementView />));

    fireEvent.click(
      await screen.findByRole("button", {
        name: "Sửa thông tin lớp Lập trình Java K21",
      }),
    );
    const dialog = await screen.findByRole("dialog");
    expect(
      within(dialog).getByRole("textbox", { name: /Tên lớp/ }),
    ).toHaveValue("Lập trình Java K21");
    expect(within(dialog).getByRole("textbox", { name: "Mô tả" })).toHaveValue(
      "Lập trình hướng đối tượng với Java cho sinh viên năm hai.",
    );
    // The schedule is structured: weekday toggles, a time range, a room and a period.
    expect(within(dialog).getByRole("checkbox", { name: "T2" })).toBeChecked();
    expect(
      within(dialog).getByRole("checkbox", { name: "T3" }),
    ).not.toBeChecked();
    expect(within(dialog).getByLabelText("Giờ bắt đầu")).toHaveValue("19:00");
    expect(within(dialog).getByLabelText("Giờ kết thúc")).toHaveValue("21:00");
    expect(
      within(dialog).getByRole("textbox", { name: "Phòng / địa điểm" }),
    ).toHaveValue("Phòng B203");
    expect(within(dialog).getByLabelText("Ngày bắt đầu")).toHaveValue(
      "2026-08-17",
    );
  });

  it("refuses a name longer than 120 characters with the length toast", async () => {
    const toasts = renderHook(() => useToasts());
    act(() => toast.clear());
    render(withTestProviders(<ClassManagementView />));

    fireEvent.click(
      await screen.findByRole("button", { name: "+ Tạo lớp mới" }),
    );
    const dialog = await screen.findByRole("dialog");
    fireEvent.change(within(dialog).getByRole("textbox", { name: /Tên lớp/ }), {
      target: { value: "a".repeat(121) },
    });
    fireEvent.click(within(dialog).getByRole("button", { name: "Lưu" }));

    await waitFor(() =>
      expect(toasts.result.current.map((item) => item.message)).toContain(
        "Nội dung vượt quá độ dài cho phép.",
      ),
    );
  });

  it("opens the detail page of a new class right after it is created", async () => {
    push.mockClear();
    render(withTestProviders(<ClassManagementView />));

    fireEvent.click(
      await screen.findByRole("button", { name: "+ Tạo lớp mới" }),
    );
    const form = await screen.findByRole("dialog");
    fireEvent.change(within(form).getByRole("textbox", { name: /Tên lớp/ }), {
      target: { value: "Lớp thử nghiệm" },
    });
    fireEvent.click(within(form).getByRole("button", { name: "Lưu" }));

    await waitFor(() =>
      expect(push).toHaveBeenCalledWith(
        expect.stringMatching(/^\/instructor\/classes\/c\d+$/),
      ),
    );
  });

  it("links each class name to its detail page", async () => {
    render(withTestProviders(<ClassManagementView />));

    expect(
      await screen.findByRole("link", { name: "Lập trình Java K21" }),
    ).toHaveAttribute("href", "/instructor/classes/c1");
  });

  it("lists the schedule as one readable line under the class name", async () => {
    render(withTestProviders(<ClassManagementView />));

    expect(
      await screen.findByText(
        "T2 · T4 · T6 — 19:00–21:00 · Phòng B203 · 17/08–20/12",
      ),
    ).toBeInTheDocument();
  });

  it("refuses an end time that is not after the start time", async () => {
    const toasts = renderHook(() => useToasts());
    act(() => toast.clear());
    render(withTestProviders(<ClassManagementView />));

    fireEvent.click(
      await screen.findByRole("button", { name: "+ Tạo lớp mới" }),
    );
    const dialog = await screen.findByRole("dialog");
    fireEvent.change(within(dialog).getByRole("textbox", { name: /Tên lớp/ }), {
      target: { value: "Lớp có lịch sai" },
    });
    fireEvent.click(within(dialog).getByRole("checkbox", { name: "T2" }));
    fireEvent.change(within(dialog).getByLabelText("Giờ bắt đầu"), {
      target: { value: "10:00" },
    });
    fireEvent.change(within(dialog).getByLabelText("Giờ kết thúc"), {
      target: { value: "09:00" },
    });
    fireEvent.click(within(dialog).getByRole("button", { name: "Lưu" }));

    await waitFor(() =>
      expect(toasts.result.current.map((item) => item.message)).toContain(
        "Giờ kết thúc phải sau giờ bắt đầu.",
      ),
    );
  });
});
