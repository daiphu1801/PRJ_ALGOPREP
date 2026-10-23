// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
import {
  fireEvent,
  render,
  screen,
  within,
} from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { withTestProviders } from "@/shared/test/render-with-providers";
import { SubmissionManagementView } from "./submission-management-view";

// The list state lives in the URL (?classId=&result=&q=&page=): the tests drive it through these params.
let params = new URLSearchParams();
const replace = vi.fn();
vi.mock("next/navigation", () => ({
  useSearchParams: () => params,
  useRouter: () => ({ replace }),
  usePathname: () => "/instructor/submissions",
}));

describe("SubmissionManagementView", () => {
  beforeEach(() => {
    params = new URLSearchParams();
    replace.mockClear();
  });

  it("lists every submission with a read-only link that carries the student and the class (INS0206)", async () => {
    render(withTestProviders(<SubmissionManagementView />));

    const table = await screen.findByRole("table", { name: "Bài nộp" });
    const links = (await within(table).findAllByRole("link")).map(
      (a) => a.getAttribute("href") ?? "",
    );
    const open = links.find((href) =>
      href.startsWith("/instructor/submissions/"),
    );
    expect(open).toMatch(/\?studentId=.+&classId=.+&from=submissions$/);
    // No stat-card strip above the list (owner rule 2026-10-13).
    expect(screen.queryByText("Chuỗi ngày")).not.toBeInTheDocument();
  });

  it("writes the search text into the URL and resets the page", async () => {
    render(withTestProviders(<SubmissionManagementView />));
    await screen.findByRole("table", { name: "Bài nộp" });

    fireEvent.change(screen.getByPlaceholderText(/Tìm/), {
      target: { value: " an " },
    });
    expect(replace).toHaveBeenLastCalledWith("/instructor/submissions?q=an", {
      scroll: false,
    });
  });

  it("filters by the learner name carried in ?q=", async () => {
    params = new URLSearchParams("q=Nguyễn Văn An");
    render(withTestProviders(<SubmissionManagementView />));
    const table = await screen.findByRole("table", { name: "Bài nộp" });

    await within(table).findAllByRole("link");
    const names = within(table)
      .getAllByRole("row")
      .slice(1)
      .map((row) => row.textContent ?? "");
    expect(names.every((text) => text.includes("Nguyễn Văn An"))).toBe(true);
  });

  it("shows the empty message when nothing matches", async () => {
    params = new URLSearchParams("q=khong-co-ai-ten-nhu-vay");
    render(withTestProviders(<SubmissionManagementView />));

    expect(
      await screen.findByText(/Không có lượt nộp nào khớp/),
    ).toBeInTheDocument();
  });
});
