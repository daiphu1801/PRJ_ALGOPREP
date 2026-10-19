// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
import { render, screen, within } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { withTestProviders } from "@/shared/test/render-with-providers";
import { ClassDetailView } from "./class-detail-view";

vi.mock("next/navigation", () => ({ useRouter: () => ({ push: vi.fn() }) }));

describe("ClassDetailView", () => {
  it("shows the class, its schedule part by part, its invite codes and its students", async () => {
    render(withTestProviders(<ClassDetailView classId="c1" />));

    expect(
      await screen.findByRole("heading", { name: "Lập trình Java K21" }),
    ).toBeInTheDocument();
    // Schedule, read part by part.
    expect(await screen.findByText("Phòng B203")).toBeInTheDocument();
    expect(screen.getByText("19:00 – 21:00")).toBeInTheDocument();
    expect(screen.getByText("T2, T4, T6")).toBeInTheDocument();
    // Invite codes of the class.
    expect(await screen.findByText("JAVA21-XK9F")).toBeInTheDocument();
    expect(screen.getByText("JAVA21-2T4M")).toBeInTheDocument();
    // Students of this class only, each linking to the student page.
    const table = await screen.findByRole("table", {
      name: "Học viên của lớp",
    });
    expect((await within(table).findAllByRole("link")).length).toBeGreaterThan(
      0,
    );
  });

  it("says so when the class does not exist", async () => {
    render(withTestProviders(<ClassDetailView classId="nope" />));

    expect(
      await screen.findByText("Lớp này không tồn tại hoặc không thuộc về bạn."),
    ).toBeInTheDocument();
  });

  it("creates another invite code from the page", async () => {
    render(withTestProviders(<ClassDetailView classId="c1" />));

    const before = (await screen.findAllByText(/Còn hạn|hết hạn/i)).length;
    (await screen.findByRole("button", { name: "Tạo mã mới" })).click();
    await screen.findByText(/^NEW-/);
    expect(screen.getAllByText(/Còn hạn|hết hạn/i).length).toBeGreaterThan(
      before - 1,
    );
  });
});
