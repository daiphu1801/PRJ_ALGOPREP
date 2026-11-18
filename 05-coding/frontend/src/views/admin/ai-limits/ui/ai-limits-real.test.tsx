// ADM0303 against the REAL api path (F5 #18 to #26): only the HTTP layer is faked, so the real modules,
// the error mapping and the real vi texts are what the assertions read. Mock-mode behaviour stays in
// views/admin/admin-toasts.test.tsx and generation-settings.test.tsx.
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
// `useMockQuery` reads the flag from `mock-mode` directly, not through the barrel mocked above.
vi.mock("@/shared/api/mock-mode", () => ({
  isMockMode: () => false,
  withMockData: <T,>(_mock: () => unknown, real: () => Promise<T>) => real(),
}));
vi.mock("next/navigation", () => ({ useRouter: () => ({ push: vi.fn() }) }));

import { apiMock, resetApiMock } from "@/shared/test/real-api";
import { AdminAiLimitsView } from "./admin-ai-limits-view";

const m = viMessages.adminAiLimits;
const apiErrors = viMessages.apiErrors as Record<string, string>;
const B = "/ai/admin";
const P = "/ai/admin/model-prices";
const PRICE = "11111111-1111-4111-8111-111111111111";

const budgetDto = (over: Record<string, unknown> = {}) => ({
  limitTokens: 52_000_000,
  usedTokens: 38_400_000,
  periodStart: "2026-11-01",
  lockedAt: null,
  isLocked: false,
  projectedRunOutDate: null,
  featureBreakdown: [],
  warnPercent: 70,
  ...over,
});

const limitsDto = (over: Record<string, unknown> = {}) => ({
  reviewPerHour: 10,
  interviewPerHour: 3,
  cooldownSeconds: 20,
  warnPercent: 70,
  ...over,
});

let state: {
  budget: Record<string, unknown>;
  limits: Record<string, unknown>;
  prices: { id: string; modelName: string; usdPerMillionTokens: number }[];
  failGet: Record<string, ApiError>;
};

function routes() {
  apiMock.get.mockImplementation(async (path: string) => {
    if (state.failGet[path]) throw state.failGet[path];
    if (path === `${B}/token-budget`) return state.budget;
    if (path === `${B}/limits-config`) return state.limits;
    if (path === P) return { items: state.prices };
    if (path === "/problem-bank/settings/ai-testcase-generation")
      return { maxPerProblem: 2 };
    throw new Error(`unexpected GET ${path}`);
  });
}

function watch() {
  const toasts = renderHook(() => useToasts());
  act(() => toast.clear());
  return () => toasts.result.current.map((item) => [item.tone, item.message]);
}

const gets = (path: string) =>
  apiMock.get.mock.calls.filter(([called]) => called === path).length;

async function open() {
  render(withTestProviders(<AdminAiLimitsView />));
  await screen.findByLabelText(m.budget.limitLabel);
}

beforeEach(() => {
  resetApiMock();
  state = {
    budget: budgetDto(),
    limits: limitsDto(),
    prices: [{ id: PRICE, modelName: "gpt-4o", usdPerMillionTokens: 2.5 }],
    failGet: {},
  };
  routes();
});

describe("budget card (F5 #18 to #20)", () => {
  it("reads the budget from the server and shows its usage and status", async () => {
    await open();
    expect(apiMock.get).toHaveBeenCalledWith(`${B}/token-budget`);
    expect(screen.getByLabelText(m.budget.limitLabel)).toHaveValue(52000000);
    expect(screen.getByText(m.budget.statusOpen)).toBeInTheDocument();
  });

  it("saves a new cap with exactly { limitTokens }, toasts and shows the server's answer", async () => {
    apiMock.put.mockResolvedValue(budgetDto({ limitTokens: 60_000_000 }));
    const read = watch();
    await open();
    fireEvent.change(screen.getByLabelText(m.budget.limitLabel), {
      target: { value: "60000000" },
    });
    fireEvent.click(screen.getByRole("button", { name: m.budget.saveLimit }));
    await waitFor(() =>
      expect(read().at(-1)).toEqual(["success", m.budget.savedLimit]),
    );
    expect(apiMock.put).toHaveBeenCalledWith(`${B}/token-budget/limit`, {
      limitTokens: 60_000_000,
    });
    // The cache holds the answer; nothing was refetched for it.
    expect(gets(`${B}/token-budget`)).toBe(1);
    expect(
      screen.getByRole("button", { name: m.budget.saveLimit }),
    ).toBeDisabled();
  });

  it.each(["0", "-5", "1.5", "", "9000000000001"])(
    "refuses the cap %j on the client without a request",
    async (value) => {
      const read = watch();
      await open();
      fireEvent.change(screen.getByLabelText(m.budget.limitLabel), {
        target: { value },
      });
      fireEvent.click(screen.getByRole("button", { name: m.budget.saveLimit }));
      expect(read().at(-1)).toEqual(["error", m.budget.errorLimit]);
      expect(apiMock.put).not.toHaveBeenCalled();
    },
  );

  it.each(["AIR-V15", "AIR-301", "AIR-503"])(
    "shows the %s text when saving the cap fails and keeps the typed value",
    async (code) => {
      apiMock.put.mockRejectedValue(new ApiError(code, 400));
      const read = watch();
      await open();
      fireEvent.change(screen.getByLabelText(m.budget.limitLabel), {
        target: { value: "1000" },
      });
      fireEvent.click(screen.getByRole("button", { name: m.budget.saveLimit }));
      await waitFor(() =>
        expect(read().at(-1)).toEqual(["error", apiErrors[code]]),
      );
      expect(screen.getByLabelText(m.budget.limitLabel)).toHaveValue(1000);
      expect(screen.getByText(m.budget.statusOpen)).toBeInTheDocument();
    },
  );

  it("unlocks after the confirmation, with no body, and shows the open status", async () => {
    state.budget = budgetDto({
      isLocked: true,
      lockedAt: "2026-11-10T00:00:00Z",
    });
    apiMock.post.mockResolvedValue(budgetDto());
    const read = watch();
    await open();
    expect(screen.getByText(m.budget.statusLocked)).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: m.budget.unlock }));
    expect(apiMock.post).not.toHaveBeenCalled();
    fireEvent.click(
      within(screen.getByRole("dialog")).getByRole("button", {
        name: m.budget.unlockConfirm,
      }),
    );
    await waitFor(() =>
      expect(read().at(-1)).toEqual(["success", m.budget.unlocked]),
    );
    expect(apiMock.post).toHaveBeenCalledWith(`${B}/token-budget/unlock`);
    expect(screen.getByText(m.budget.statusOpen)).toBeInTheDocument();
    expect(screen.queryByRole("dialog")).toBeNull();
  });

  it.each(["AIR-203", "AIR-301", "AIR-503"])(
    "keeps the dialog and the lock when unlock answers %s",
    async (code) => {
      state.budget = budgetDto({ isLocked: true });
      apiMock.post.mockRejectedValue(new ApiError(code, 422));
      const read = watch();
      await open();
      fireEvent.click(screen.getByRole("button", { name: m.budget.unlock }));
      fireEvent.click(
        within(screen.getByRole("dialog")).getByRole("button", {
          name: m.budget.unlockConfirm,
        }),
      );
      await waitFor(() =>
        expect(read().at(-1)).toEqual(["error", apiErrors[code]]),
      );
      expect(screen.getByRole("dialog")).toBeInTheDocument();
      expect(screen.getByText(m.budget.statusLocked)).toBeInTheDocument();
    },
  );

  it("does not send unlock while usage has not dropped below the cap", async () => {
    state.budget = budgetDto({
      isLocked: true,
      limitTokens: 1000,
      usedTokens: 1000,
    });
    const read = watch();
    await open();
    fireEvent.click(screen.getByRole("button", { name: m.budget.unlock }));
    expect(read().at(-1)).toEqual(["warning", m.budget.unlockBlocked]);
    expect(screen.queryByRole("dialog")).toBeNull();
    expect(apiMock.post).not.toHaveBeenCalled();
  });

  it("shows an error block with retry when the budget cannot be read, other cards still work", async () => {
    state.failGet[`${B}/token-budget`] = new ApiError("AIR-301", 403);
    render(withTestProviders(<AdminAiLimitsView />));
    expect(
      await screen.findByRole("button", { name: viMessages.common.retry }),
    ).toBeInTheDocument();
    expect(
      await screen.findByRole("button", {
        name: `${m.limit.increment.replace("{name}", m.limit.reviewPerHour.label)}`,
      }),
    ).toBeInTheDocument();
  });
});

describe("rate limits and warning threshold (F5 #21, #22)", () => {
  const inc = (label: string) =>
    screen.getByRole("button", {
      name: m.limit.increment.replace("{name}", label),
    });

  it("reads the three limits including the cooldown", async () => {
    await open();
    expect(apiMock.get).toHaveBeenCalledWith(`${B}/limits-config`);
    expect(screen.getByText(m.limit.cooldownSeconds.label)).toBeInTheDocument();
  });

  it.each([
    ["reviewPerHour", m.limit.reviewPerHour.label, 11],
    ["interviewPerHour", m.limit.interviewPerHour.label, 4],
    ["cooldownSeconds", m.limit.cooldownSeconds.label, 21],
  ])("sends only %s when its stepper moves", async (key, label, next) => {
    apiMock.patch.mockResolvedValue(limitsDto({ [key]: next }));
    await open();
    await screen.findByLabelText(label);
    fireEvent.click(inc(label));
    await waitFor(() =>
      expect(apiMock.patch).toHaveBeenCalledWith(`${B}/limits-config`, {
        [key]: next,
      }),
    );
    await waitFor(() =>
      expect(screen.getByLabelText(label)).toHaveValue(String(next)),
    );
  });

  it("sends warnPercent and refreshes the budget bar that reads it", async () => {
    apiMock.patch.mockResolvedValue(limitsDto({ warnPercent: 75 }));
    await open();
    await screen.findByLabelText(m.cost.warnLabel);
    fireEvent.click(screen.getByRole("button", { name: m.cost.warnIncrement }));
    await waitFor(() =>
      expect(apiMock.patch).toHaveBeenCalledWith(`${B}/limits-config`, {
        warnPercent: 75,
      }),
    );
    await waitFor(() => expect(gets(`${B}/token-budget`)).toBe(2));
  });

  it.each(["AIR-V16", "AIR-301"])(
    "shows the %s text and keeps the server value when a limit write fails",
    async (code) => {
      apiMock.patch.mockRejectedValue(new ApiError(code, 400));
      const read = watch();
      await open();
      await screen.findByLabelText(m.limit.reviewPerHour.label);
      fireEvent.click(inc(m.limit.reviewPerHour.label));
      await waitFor(() =>
        expect(read().at(-1)).toEqual(["error", apiErrors[code]]),
      );
      expect(screen.getByLabelText(m.limit.reviewPerHour.label)).toHaveValue(
        "10",
      );
    },
  );

  it("shows a retry block, not a mock constant, when limits-config cannot be read", async () => {
    state.failGet[`${B}/limits-config`] = new ApiError("AIR-301", 403);
    render(withTestProviders(<AdminAiLimitsView />));
    await screen.findAllByRole("button", { name: viMessages.common.retry });
    expect(
      screen.queryByLabelText(m.limit.reviewPerHour.label),
    ).not.toBeInTheDocument();
  });
});

describe("model price table (F5 #23 to #26)", () => {
  async function openPrices() {
    await open();
    const manage = await screen.findByRole("button", { name: m.cost.manage });
    await waitFor(() => expect(manage).toBeEnabled());
    fireEvent.click(manage);
    return within(screen.getByRole("dialog"));
  }

  it("lists the server's models with their prices", async () => {
    const dialog = await openPrices();
    expect(apiMock.get).toHaveBeenCalledWith(P);
    expect(dialog.getByDisplayValue("gpt-4o")).toBeInTheDocument();
    expect(
      dialog.getByLabelText(
        m.cost.dialog.priceLabel.replace("{name}", "gpt-4o"),
      ),
    ).toHaveValue(2.5);
  });

  it("adds a model with exactly { modelName } (trimmed) and refetches the list", async () => {
    apiMock.post.mockImplementation(async () => {
      state.prices.push({
        id: "b",
        modelName: "claude",
        usdPerMillionTokens: 0,
      });
      return { id: "b", modelName: "claude", usdPerMillionTokens: 0 };
    });
    const read = watch();
    const dialog = await openPrices();
    fireEvent.change(dialog.getByLabelText(m.cost.dialog.newLabel), {
      target: { value: "  claude " },
    });
    fireEvent.click(dialog.getByRole("button", { name: m.cost.dialog.add }));
    await waitFor(() =>
      expect(read().at(-1)).toEqual(["success", m.cost.dialog.doneAdd]),
    );
    expect(apiMock.post).toHaveBeenCalledWith(P, { modelName: "claude" });
    expect(await dialog.findByDisplayValue("claude")).toBeInTheDocument();
  });

  it("refuses an empty name locally with no request", async () => {
    const read = watch();
    const dialog = await openPrices();
    fireEvent.click(dialog.getByRole("button", { name: m.cost.dialog.add }));
    expect(read().at(-1)).toEqual(["error", m.cost.dialog.errorEmpty]);
    expect(apiMock.post).not.toHaveBeenCalled();
  });

  it.each([
    ["AIR-204", 409],
    ["AIR-V17", 400],
    ["AIR-301", 403],
  ])(
    "shows the %s text when adding fails and adds nothing",
    async (code, status) => {
      apiMock.post.mockRejectedValue(new ApiError(code, status));
      const read = watch();
      const dialog = await openPrices();
      fireEvent.change(dialog.getByLabelText(m.cost.dialog.newLabel), {
        target: { value: "gpt-4o" },
      });
      fireEvent.click(dialog.getByRole("button", { name: m.cost.dialog.add }));
      await waitFor(() =>
        expect(read().at(-1)).toEqual(["error", apiErrors[code]]),
      );
      // The typed name stays in the new-name box; the list was not refetched, so it still has one row.
      expect(gets(P)).toBe(1);
    },
  );

  it("renames with only { modelName }", async () => {
    apiMock.patch.mockResolvedValue({
      id: PRICE,
      modelName: "gpt-4.1",
      usdPerMillionTokens: 2.5,
    });
    const read = watch();
    const dialog = await openPrices();
    const name = dialog.getByDisplayValue("gpt-4o");
    fireEvent.change(name, { target: { value: "gpt-4.1" } });
    fireEvent.keyDown(name, { key: "Enter" });
    await waitFor(() =>
      expect(read().at(-1)).toEqual(["success", m.cost.dialog.doneRename]),
    );
    expect(apiMock.patch).toHaveBeenCalledWith(`${P}/${PRICE}`, {
      modelName: "gpt-4.1",
    });
  });

  it.each([
    ["AIR-V17", 400],
    ["AIR-204", 409],
    ["AIR-301", 403],
  ])("shows the %s text when renaming fails", async (code, status) => {
    apiMock.patch.mockRejectedValue(new ApiError(code, status));
    const read = watch();
    const dialog = await openPrices();
    const name = dialog.getByDisplayValue("gpt-4o");
    fireEvent.change(name, { target: { value: "other" } });
    fireEvent.keyDown(name, { key: "Enter" });
    await waitFor(() =>
      expect(read().at(-1)).toEqual(["error", apiErrors[code]]),
    );
  });

  it("writes the price on blur with only { usdPerMillionTokens }, not on every keystroke", async () => {
    apiMock.patch.mockResolvedValue({
      id: PRICE,
      modelName: "gpt-4o",
      usdPerMillionTokens: 3.25,
    });
    const dialog = await openPrices();
    const price = dialog.getByLabelText(
      m.cost.dialog.priceLabel.replace("{name}", "gpt-4o"),
    );
    fireEvent.change(price, { target: { value: "3" } });
    fireEvent.change(price, { target: { value: "3.25" } });
    expect(apiMock.patch).not.toHaveBeenCalled();
    fireEvent.blur(price);
    await waitFor(() =>
      expect(apiMock.patch).toHaveBeenCalledWith(`${P}/${PRICE}`, {
        usdPerMillionTokens: 3.25,
      }),
    );
    expect(apiMock.patch).toHaveBeenCalledTimes(1);
  });

  it("turns a negative price into 0 before sending (BD Q4)", async () => {
    apiMock.patch.mockResolvedValue({
      id: PRICE,
      modelName: "gpt-4o",
      usdPerMillionTokens: 0,
    });
    const dialog = await openPrices();
    const price = dialog.getByLabelText(
      m.cost.dialog.priceLabel.replace("{name}", "gpt-4o"),
    );
    fireEvent.change(price, { target: { value: "-4" } });
    fireEvent.blur(price);
    await waitFor(() =>
      expect(apiMock.patch).toHaveBeenCalledWith(`${P}/${PRICE}`, {
        usdPerMillionTokens: 0,
      }),
    );
  });

  it.each([
    ["AIR-V18", 400],
    ["AIR-121", 404],
    ["AIR-301", 403],
  ])(
    "shows the %s text when the price write fails and reverts to the server value",
    async (code, status) => {
      apiMock.patch.mockRejectedValue(new ApiError(code, status));
      const read = watch();
      const dialog = await openPrices();
      const label = m.cost.dialog.priceLabel.replace("{name}", "gpt-4o");
      fireEvent.change(dialog.getByLabelText(label), {
        target: { value: "9" },
      });
      fireEvent.blur(dialog.getByLabelText(label));
      await waitFor(() =>
        expect(read().at(-1)).toEqual(["error", apiErrors[code]]),
      );
      await waitFor(() =>
        expect(dialog.getByLabelText(label)).toHaveValue(2.5),
      );
      // A vanished row (AIR-121) triggers a refetch of the list.
      if (code === "AIR-121") await waitFor(() => expect(gets(P)).toBe(2));
    },
  );

  it("deletes a model and refetches; a failed delete shows its text", async () => {
    apiMock.delete.mockImplementation(async () => {
      state.prices = [];
    });
    const read = watch();
    const dialog = await openPrices();
    fireEvent.click(dialog.getByRole("button", { name: m.cost.dialog.delete }));
    await waitFor(() =>
      expect(read().at(-1)).toEqual(["success", m.cost.dialog.doneRemove]),
    );
    expect(apiMock.delete).toHaveBeenCalledWith(`${P}/${PRICE}`);
    await waitFor(() =>
      expect(dialog.queryByDisplayValue("gpt-4o")).toBeNull(),
    );
  });

  it("shows the AIR-301 text when deleting is refused, with no success toast", async () => {
    apiMock.delete.mockRejectedValue(new ApiError("AIR-301", 403));
    const read = watch();
    const dialog = await openPrices();
    fireEvent.click(dialog.getByRole("button", { name: m.cost.dialog.delete }));
    await waitFor(() =>
      expect(read()).toEqual([["error", apiErrors["AIR-301"]]]),
    );
    expect(dialog.getByDisplayValue("gpt-4o")).toBeInTheDocument();
  });

  it("shows a retry block instead of the manage button when the price list cannot be read", async () => {
    state.failGet[P] = new ApiError("AIR-301", 403);
    render(withTestProviders(<AdminAiLimitsView />));
    await screen.findAllByRole("button", { name: viMessages.common.retry });
    expect(
      screen.queryByRole("button", { name: m.cost.manage }),
    ).not.toBeInTheDocument();
  });
});
