import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { NextIntlClientProvider } from "@/shared/i18n";
import messages from "../../../../../messages/vi.json";
import { ProfileView } from "./profile-view";

function renderView() {
  const client = new QueryClient();
  return render(
    <QueryClientProvider client={client}>
      <NextIntlClientProvider locale="vi" messages={messages}>
        <ProfileView />
      </NextIntlClientProvider>
    </QueryClientProvider>,
  );
}

describe("ProfileView", () => {
  it("loads the profile and renders the identity + personal-info form", async () => {
    renderView();

    expect(await screen.findByText("Phú Đại")).toBeInTheDocument();
    expect(screen.getByText("phudai@fabbi.io")).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Thông tin cá nhân" })).toBeInTheDocument();
    expect(screen.getByLabelText("Họ và tên")).toHaveValue("Phú Đại");
  });
});
