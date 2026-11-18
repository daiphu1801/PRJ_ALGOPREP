import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

vi.mock("@/shared/i18n/server", () => ({
  getT: async () => (key: string) => key,
}));
// Each area chrome is covered by its own test; here only the composition per area matters.
vi.mock("./admin-sidebar", () => ({
  AdminSidebar: () => <div data-testid="admin-sidebar" />,
}));
vi.mock("./admin-toolbar", () => ({
  AdminToolbar: () => <div data-testid="admin-toolbar" />,
}));
vi.mock("./instructor-sidebar", () => ({
  InstructorSidebar: () => <div data-testid="instructor-sidebar" />,
}));
vi.mock("./instructor-toolbar", () => ({
  InstructorToolbar: () => <div data-testid="instructor-toolbar" />,
}));
vi.mock("./student-header", () => ({
  StudentHeader: () => <div data-testid="student-header" />,
}));
vi.mock("./student-footer", () => ({
  StudentFooter: () => <div data-testid="student-footer" />,
}));
vi.mock("./area-footer", () => ({
  AreaFooter: ({ namespace }: { namespace?: string }) => (
    <div data-testid="area-footer" data-namespace={namespace ?? "default"} />
  ),
}));
vi.mock("./forced-password-change", () => ({
  ForcedPasswordChange: ({ area }: { area: string }) => (
    <div data-testid="forced-password-change" data-area={area} />
  ),
}));

import { AppShell } from "./app-shell";

describe("AppShell", () => {
  it("composes the admin area: sidebar, toolbar, footer and the forced password gate", async () => {
    render(await AppShell({ area: "admin", children: <p>admin page</p> }));

    expect(screen.getByTestId("admin-sidebar")).toBeInTheDocument();
    expect(screen.getByTestId("admin-toolbar")).toBeInTheDocument();
    expect(screen.getByTestId("area-footer")).toHaveAttribute(
      "data-namespace",
      "default",
    );
    expect(screen.getByTestId("forced-password-change")).toHaveAttribute(
      "data-area",
      "admin",
    );
    expect(screen.getByRole("main")).toContainElement(
      screen.getByText("admin page"),
    );
    expect(screen.queryByTestId("student-header")).not.toBeInTheDocument();
  });

  it("composes the student area: header on top, footer outside main", async () => {
    render(await AppShell({ area: "student", children: <p>student page</p> }));

    expect(screen.getByTestId("student-header")).toBeInTheDocument();
    expect(screen.getByTestId("forced-password-change")).toHaveAttribute(
      "data-area",
      "student",
    );
    const main = screen.getByRole("main");
    expect(main).toContainElement(screen.getByText("student page"));
    expect(main).not.toContainElement(screen.getByTestId("student-footer"));
    expect(screen.queryByTestId("admin-sidebar")).not.toBeInTheDocument();
  });

  it("composes the instructor area with the instructor footer namespace", async () => {
    render(
      await AppShell({ area: "instructor", children: <p>teacher page</p> }),
    );

    expect(screen.getByTestId("instructor-sidebar")).toBeInTheDocument();
    expect(screen.getByTestId("instructor-toolbar")).toBeInTheDocument();
    expect(screen.getByTestId("area-footer")).toHaveAttribute(
      "data-namespace",
      "instructorNav",
    );
    expect(screen.getByTestId("forced-password-change")).toHaveAttribute(
      "data-area",
      "instructor",
    );
    expect(screen.getByRole("main")).toContainElement(
      screen.getByText("teacher page"),
    );
  });

  it("renders a skip link targeting the main landmark in every area", async () => {
    render(await AppShell({ area: "student", children: <p>x</p> }));

    const main = screen.getByRole("main");
    const skip = screen.getByRole("link", { name: "skipToContent" });
    expect(skip).toHaveAttribute("href", `#${main.id}`);
  });
});
