import { describe, expect, it, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import { withTestProviders } from "@/shared/test/render-with-providers";
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
  it("lists in-progress problems with links into the workspace", async () => {
    render(withTestProviders(<WorkspaceHomeView />));
    expect(
      await screen.findByRole("link", { name: /Word Break/ }),
    ).toHaveAttribute(
      "href",
      "/workspace/139",
    );
  });
});
