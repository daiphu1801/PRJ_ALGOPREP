// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
// Every admin view reports operation results and validation errors through the shared toast, not
// inline text. `useT` is mocked to return the key, so assertions read the i18n key.
import { act, fireEvent, render, renderHook, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { toast, useToasts } from "@/shared/lib/toast-store";

vi.mock("@/shared/i18n", () => ({
  useT: () => (key: string) => key,
  useLocale: () => "vi",
}));

import { AdminAiConfigView } from "./ai-config";
import { AdminAiUsageView } from "./ai-usage";
import { AdminLanguageConfigView } from "./language-config";
import { AdminPermissionMatrixView } from "./permission-matrix";
import { AdminQueueMonitorView } from "./queue-monitor";
import { ExportLogDialog } from "./system-log/ui/export-log-dialog";
import { AdminUserManagementView } from "./user-management";

function watch() {
  const toasts = renderHook(() => useToasts());
  act(() => toast.clear());
  return () => toasts.result.current.map((item) => [item.tone, item.message]);
}

describe("admin toasts", () => {
  beforeEach(() => act(() => toast.clear()));

  it("add-account: invalid form raises one error toast, valid form raises success", () => {
    const read = watch();
    render(<AdminUserManagementView />);
    fireEvent.click(screen.getByRole("button", { name: "addAccount" }));
    fireEvent.click(screen.getByRole("button", { name: "addDialog.submit" }));
    expect(read()).toEqual([["error", "addDialog.errorName"]]);

    fireEvent.change(screen.getByLabelText("addDialog.nameLabel"), { target: { value: "An" } });
    fireEvent.change(screen.getByLabelText("addDialog.emailLabel"), { target: { value: "an@x.vn" } });
    fireEvent.click(screen.getByRole("button", { name: "addDialog.submit" }));
    expect(read().at(-1)).toEqual(["success", "addDialog.createdBody"]);
  });

  it("export dialog: bad range errors, good range succeeds", () => {
    const read = watch();
    render(<ExportLogDialog open onClose={() => {}} />);
    fireEvent.change(screen.getByLabelText("exportDialog.from"), { target: { value: "2026-02-01" } });
    fireEvent.change(screen.getByLabelText("exportDialog.to"), { target: { value: "2026-01-01" } });
    fireEvent.click(screen.getByRole("button", { name: "exportDialog.submit" }));
    expect(read()).toEqual([["error", "exportDialog.errorRange"]]);

    fireEvent.change(screen.getByLabelText("exportDialog.to"), { target: { value: "2026-03-01" } });
    fireEvent.click(screen.getByRole("button", { name: "exportDialog.submit" }));
    expect(read().at(-1)).toEqual(["success", "exportDialog.doneBody"]);
  });

  it("permission matrix: save raises a success toast", () => {
    const read = watch();
    render(<AdminPermissionMatrixView />);
    fireEvent.click(screen.getByRole("button", { name: "save" }));
    expect(read()).toEqual([["success", "saveDone"]]);
  });

  it("queue monitor: refresh raises a success toast", () => {
    const read = watch();
    render(<AdminQueueMonitorView />);
    fireEvent.click(screen.getByRole("button", { name: "refresh" }));
    expect(read()).toEqual([["success", "refreshDone"]]);
  });

  it("ai usage: export raises a success toast", () => {
    const read = watch();
    render(<AdminAiUsageView />);
    fireEvent.click(screen.getByRole("button", { name: "export" }));
    expect(read()).toEqual([["success", "exportDone"]]);
  });

  it("ai config: publish is blocked while the rubric total is not 100%, and explains why", () => {
    const read = watch();
    render(<AdminAiConfigView />);
    // Seeded weights add up to 100; one step up on the first criterion makes the total 101.
    fireEvent.click(screen.getAllByRole("button", { name: /^weightIncrement/ })[0]!);
    const publish = screen.getByRole("button", { name: "publish" });
    expect(publish).toHaveAttribute("aria-disabled", "true");

    fireEvent.click(publish);
    expect(read()).toEqual([["warning", expect.stringContaining("weightWarningTitle")]]);
  });

  it("ai config: publish raises a success toast", () => {
    const read = watch();
    render(<AdminAiConfigView />);
    fireEvent.click(screen.getByRole("button", { name: "publish" }));
    expect(read().at(-1)).toEqual(["success", "publishDone"]);
  });

  it("language config: save raises a success toast after a change", async () => {
    const read = watch();
    render(<AdminLanguageConfigView />);
    fireEvent.click(screen.getAllByRole("switch")[0]!);
    fireEvent.click(screen.getByRole("button", { name: "save" }));
    await vi.waitFor(() => expect(read()).toEqual([["success", "saveDone"]]));
  });
});
