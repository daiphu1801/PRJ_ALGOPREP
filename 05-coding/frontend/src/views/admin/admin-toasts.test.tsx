// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
// Every admin view reports operation results and validation errors through the shared toast, not
// inline text. `useT` is mocked to return the key, so assertions read the i18n key.
import {
  act,
  fireEvent,
  render,
  waitFor,
  renderHook,
  screen,
} from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import type { ReactElement } from "react";
import { aiTokenBudget } from "@/entities/ai-budget";
import { toast, useToasts } from "@/shared/lib/toast-store";

vi.mock("@/shared/i18n", () => ({
  useT: () => (key: string) => key,
  useLocale: () => "vi",
}));

vi.mock("next/navigation", () => ({ useRouter: () => ({ push: vi.fn() }) }));

import { AdminAiConfigView } from "./ai-config";
import { AdminAiUsageView } from "./ai-usage";
import { AdminAiLimitsView } from "./ai-limits";
import { AdminLanguageConfigView } from "./language-config";
import { AdminPermissionMatrixView } from "./permission-matrix";
import { AdminQueueMonitorView } from "./queue-monitor";
import { ExportLogDialog } from "./system-log/ui/export-log-dialog";
import { AdminUserManagementView } from "./user-management";

// The language screen loads through TanStack Query, so it needs a provider and an async first paint.
function withQuery(ui: ReactElement) {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  return <QueryClientProvider client={client}>{ui}</QueryClientProvider>;
}

function watch() {
  const toasts = renderHook(() => useToasts());
  act(() => toast.clear());
  return () => toasts.result.current.map((item) => [item.tone, item.message]);
}

describe("admin toasts", () => {
  beforeEach(() => act(() => toast.clear()));

  it("add-account: invalid form raises one error toast, valid form raises success", async () => {
    const read = watch();
    render(withQuery(<AdminUserManagementView />));
    fireEvent.click(await screen.findByRole("button", { name: "addAccount" }));
    fireEvent.click(screen.getByRole("button", { name: "addDialog.submit" }));
    expect(read()).toEqual([["error", "addDialog.errorName"]]);

    fireEvent.change(screen.getByLabelText("addDialog.nameLabel"), {
      target: { value: "An" },
    });
    fireEvent.change(screen.getByLabelText("addDialog.emailLabel"), {
      target: { value: "an@x.vn" },
    });
    fireEvent.click(screen.getByRole("button", { name: "addDialog.submit" }));
    expect(read().at(-1)).toEqual(["success", "addDialog.createdBody"]);
  });

  it("export dialog: bad range errors, good range succeeds", () => {
    const read = watch();
    render(<ExportLogDialog open onClose={() => {}} />);
    const day = (offset: number) =>
      new Date(Date.now() + offset * 86_400_000).toISOString().slice(0, 10);
    fireEvent.change(screen.getByLabelText("exportDialog.from"), {
      target: { value: day(-1) },
    });
    fireEvent.change(screen.getByLabelText("exportDialog.to"), {
      target: { value: day(-5) },
    });
    fireEvent.click(
      screen.getByRole("button", { name: "exportDialog.submit" }),
    );
    expect(read()).toEqual([["error", "exportDialog.errorRange"]]);

    fireEvent.change(screen.getByLabelText("exportDialog.to"), {
      target: { value: day(0) },
    });
    fireEvent.click(
      screen.getByRole("button", { name: "exportDialog.submit" }),
    );
    expect(read().at(-1)).toEqual(["success", "exportDialog.doneBody"]);
  });

  it("permission matrix: a duplicate role name raises an error toast", async () => {
    const read = watch();
    render(withQuery(<AdminPermissionMatrixView />));
    await screen.findByRole("button", { name: "addRole" });
    fireEvent.click(screen.getByRole("button", { name: "addRole" }));
    fireEvent.change(screen.getByLabelText("newRoleLabel"), {
      target: { value: "quản trị" },
    });
    fireEvent.click(screen.getByRole("button", { name: "createRole" }));
    expect(read()).toEqual([["error", "roleNameError.duplicate"]]);
  });

  it("queue monitor: refresh raises a success toast", async () => {
    const read = watch();
    render(withQuery(<AdminQueueMonitorView />));
    fireEvent.click(await screen.findByRole("button", { name: "refresh" }));
    await waitFor(() => expect(read()).toEqual([["success", "refreshDone"]]));
  });

  it("ai config: the anti-leak guard can be turned off and warns", async () => {
    const read = watch();
    render(withQuery(<AdminAiConfigView />));
    const locked = await screen.findByRole("switch", {
      name: "guard.noFullSolution.label",
    });
    expect(locked).not.toBeDisabled();
    fireEvent.click(locked);
    expect(locked).toHaveAttribute("aria-checked", "false");
    expect(read()).toEqual([["warning", "guardOffWarning"]]);
  });

  it("ai config: publish has nothing to publish until something changes, then raises a success toast", async () => {
    const read = watch();
    render(withQuery(<AdminAiConfigView />));
    const publish = await screen.findByRole("button", { name: "publish" });
    expect(publish).toHaveAttribute("aria-disabled", "true");
    fireEvent.click(publish);
    expect(read().at(-1)).toEqual(["info", "noChanges"]);

    // A guard toggle on the review tab is a change.
    fireEvent.click(screen.getAllByRole("switch")[1]!);
    expect(publish).not.toHaveAttribute("aria-disabled");
    fireEvent.click(publish);
    // The confirmation lists what is about to replace the live prompt; nothing is published yet.
    expect(read().at(-1)).toEqual(["info", "noChanges"]);
    fireEvent.click(
      screen.getByRole("button", { name: "publishDialog.confirm" }),
    );
    expect(read().at(-1)).toEqual(["success", "publishDone"]);
  });

  it("ai config: saving an empty instruction is refused, a real one lands in the draft", async () => {
    const read = watch();
    render(withQuery(<AdminAiConfigView />));
    fireEvent.click(await screen.findByRole("button", { name: "editPrompt" }));
    const box = screen.getByLabelText("editDialog.label");
    fireEvent.change(box, { target: { value: "  " } });
    fireEvent.click(screen.getByRole("button", { name: "editDialog.save" }));
    expect(read().at(-1)).toEqual(["error", "editDialog.errorEmpty"]);

    fireEvent.change(box, { target: { value: "Chi thi moi" } });
    fireEvent.click(screen.getByRole("button", { name: "editDialog.save" }));
    expect(read().at(-1)).toEqual(["success", "editDialog.saved"]);
    expect(screen.getByText("pendingCount")).toBeInTheDocument();
  });

  it("ai limits: saving a bad cap raises an error toast, a good one a success toast", () => {
    const read = watch();
    render(<AdminAiLimitsView />);
    const input = screen.getByLabelText("budget.limitLabel");
    fireEvent.change(input, { target: { value: "0" } });
    fireEvent.click(screen.getByRole("button", { name: "budget.saveLimit" }));
    expect(read().at(-1)).toEqual(["error", "budget.errorLimit"]);

    fireEvent.change(input, { target: { value: "60000000" } });
    fireEvent.click(screen.getByRole("button", { name: "budget.saveLimit" }));
    expect(read().at(-1)).toEqual(["success", "budget.savedLimit"]);
    aiTokenBudget.set({ limitTokens: 52_000_000, locked: false });
  });

  it("language config: save raises a success toast after a change", async () => {
    const read = watch();
    render(withQuery(<AdminLanguageConfigView />));
    const multipliers = await screen.findAllByLabelText("timeMultiplierFor");
    fireEvent.change(multipliers[0]!, { target: { value: "4" } });
    fireEvent.click(screen.getByRole("button", { name: "save" }));
    await vi.waitFor(() => expect(read()).toEqual([["success", "saveDone"]]));
  });
  it("ai usage: marking the anomaly alert reviewed removes it and raises a success toast (BD ADM0302 EVT-5)", async () => {
    const read = watch();
    render(withQuery(<AdminAiUsageView />));
    fireEvent.click(
      await screen.findByRole("button", { name: "markReviewed" }),
    );
    expect(screen.queryByText("noAlerts")).toBeNull();
    expect(screen.queryByText("alert.anomalousAccount.title")).toBeNull();
    expect(screen.getByText("alert.budgetRunningOut.title")).toBeTruthy();
    expect(read()).toEqual([["success", "markReviewedDone"]]);
  });
});
