import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { RecentList } from "./recent-list";

describe("RecentList", () => {
  it("renders title, subtitle and trailing slot, and a row with href is a link", () => {
    render(
      <RecentList
        emptyLabel="Trống"
        items={[
          {
            id: "1",
            title: "Nguyễn Văn An",
            subtitle: "Two Sum",
            trailing: <span>AC</span>,
            href: "/instructor/submissions/SB-1",
          },
          { id: "2", title: "Trần Thị Bích" },
        ]}
      />,
    );

    expect(screen.getByText("Two Sum")).toBeInTheDocument();
    expect(screen.getByText("AC")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: /Nguyễn Văn An/ })).toHaveAttribute(
      "href",
      "/instructor/submissions/SB-1",
    );
    // The second row has no href: it is plain text, not a link.
    expect(screen.getAllByRole("link")).toHaveLength(1);
  });

  it("shows the empty label when there are no items", () => {
    render(<RecentList emptyLabel="Trống" items={[]} />);
    expect(screen.getByText("Trống")).toBeInTheDocument();
  });
});
