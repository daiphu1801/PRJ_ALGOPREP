import { describe, expect, it, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import { NextIntlClientProvider } from "@/shared/i18n";
import messages from "../../../../../messages/vi.json";
import { WorkspaceHomeView } from "./workspace-home-view";

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: vi.fn() }),
}));

describe("workspaceHome toast", () => {
  it("has its own randomEmpty message", () => {
    expect(messages.workspaceHome.toast.randomEmpty).toBe(
      "Bạn đã giải hết mọi bài hiện có.",
    );
  });
});

describe("WorkspaceHomeView", () => {
  it("lists in-progress problems with links into the workspace", () => {
    render(
      <NextIntlClientProvider locale="vi" messages={messages}>
        <WorkspaceHomeView />
      </NextIntlClientProvider>,
    );
    expect(screen.getByRole("link", { name: /Word Break/ })).toHaveAttribute(
      "href",
      "/workspace/139",
    );
  });
});
