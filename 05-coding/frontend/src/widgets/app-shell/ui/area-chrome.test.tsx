import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { withTestProviders } from "@/shared/test/render-with-providers";
import viMessages from "../../../../messages/vi.json";

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: vi.fn() }),
  usePathname: () => "/instructor/overview",
}));
vi.mock("@/entities/user", () => ({
  useMyProfile: () => ({ data: { displayName: "Co Lan" } }),
}));
// Server components read their messages through getT; hand them the same vi.json the client uses.
vi.mock("@/shared/i18n/server", async () => {
  const messages = (await import("../../../../messages/vi.json"))
    .default as unknown as Record<string, Record<string, unknown>>;
  return {
    getT: async (ns: string) => (key: string) => {
      const value = key
        .split(".")
        .reduce<unknown>(
          (acc, part) => (acc as Record<string, unknown>)?.[part],
          messages[ns],
        );
      return typeof value === "string" ? value : key;
    },
  };
});

import { AreaFooter } from "./area-footer";
import { InstructorToolbar } from "./instructor-toolbar";
import { JudgeEngineTag } from "./judge-engine-tag";
import { StudentFooter } from "./student-footer";

describe("AreaFooter", () => {
  it("renders the admin brand, status and the four decorative links", async () => {
    render(await AreaFooter({}));

    expect(
      screen.getByText(viMessages.adminNav.footerBrand),
    ).toBeInTheDocument();
    expect(
      screen.getByText(viMessages.adminNav.footerStatusOk),
    ).toBeInTheDocument();
    expect(
      screen.getByText(viMessages.adminNav.footerLinkApiDocs),
    ).toBeInTheDocument();
    expect(
      screen.getByText(viMessages.adminNav.footerLinkSupport),
    ).toBeInTheDocument();
    // Plain text, not dead anchors.
    expect(screen.queryAllByRole("link")).toHaveLength(0);
  });

  it("uses the instructor namespace when asked", async () => {
    render(await AreaFooter({ namespace: "instructorNav" }));

    expect(
      screen.getByText(viMessages.instructorNav.footerBrand),
    ).toBeInTheDocument();
  });
});

describe("StudentFooter", () => {
  it("renders copyright, cluster status and version in a contentinfo landmark", async () => {
    render(await StudentFooter());

    expect(screen.getByRole("contentinfo")).toBeInTheDocument();
    expect(
      screen.getByText(viMessages.studentShell.footer.copyright),
    ).toBeInTheDocument();
    expect(
      screen.getByText(viMessages.studentShell.footer.clusterStatus),
    ).toBeInTheDocument();
    expect(
      screen.getByText(viMessages.studentShell.footer.version),
    ).toBeInTheDocument();
  });
});

describe("JudgeEngineTag", () => {
  it("names the judge engine", () => {
    render(<JudgeEngineTag />);
    expect(screen.getByText("GO-JUDGE")).toBeInTheDocument();
  });
});

describe("InstructorToolbar", () => {
  it("shows the instructor name and role in the user menu trigger", () => {
    render(withTestProviders(<InstructorToolbar />));

    expect(screen.getByText("Co Lan")).toBeInTheDocument();
    expect(
      screen.getByText(viMessages.instructorNav.roleLabel),
    ).toBeInTheDocument();
  });
});
