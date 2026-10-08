import { describe, expect, it, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { NextIntlClientProvider } from "@/shared/i18n";
import messages from "../../../../../messages/vi.json";
import { SettingsView } from "./settings-view";

// ThemeLangSwitcher (in AppearanceCard) and the delete-account flow call useRouter() —
// same mock as views/problem-list/ui/problem-list-view.test.tsx.
vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: vi.fn(), refresh: vi.fn() }),
}));

function renderView() {
  const client = new QueryClient();
  return render(
    <QueryClientProvider client={client}>
      <NextIntlClientProvider locale="vi" messages={messages}>
        <SettingsView />
      </NextIntlClientProvider>
    </QueryClientProvider>,
  );
}

describe("SettingsView", () => {
  it("renders the settings groups and the danger zone once loaded", async () => {
    renderView();

    expect(screen.getByText("Giao diện")).toBeInTheDocument();
    expect(await screen.findByText("Workspace")).toBeInTheDocument();
    expect(screen.getByText("Phỏng vấn giả lập")).toBeInTheDocument();
    expect(
      await screen.findByRole("heading", { name: "Vùng nguy hiểm" }),
    ).toBeInTheDocument();
  });
});
