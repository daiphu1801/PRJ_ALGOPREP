import { describe, expect, it } from "vitest";
import { fireEvent, render, screen } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { NextIntlClientProvider } from "@/shared/i18n";
import messages from "../../../../messages/vi.json";
import { MyProgressView } from "./my-progress-view";

function renderView() {
  const client = new QueryClient();
  return render(
    <QueryClientProvider client={client}>
      <NextIntlClientProvider locale="vi" messages={messages}>
        <MyProgressView />
      </NextIntlClientProvider>
    </QueryClientProvider>,
  );
}

describe("MyProgressView", () => {
  it("loads the stats bar and the topic table", async () => {
    renderView();

    expect(await screen.findByText("182 / 640")).toBeInTheDocument();
    expect(screen.getByText("Hash map")).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Nên ưu tiên" })).toBeInTheDocument();
  });

  it("switches the range tab without crashing", async () => {
    renderView();
    await screen.findByText("Hash map");

    fireEvent.click(screen.getByRole("button", { name: "7 ngày" }));

    expect(await screen.findByText("Hash map")).toBeInTheDocument();
  });
});
