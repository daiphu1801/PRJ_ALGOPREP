// View-level test of the profile form (BD USR0502 Khu vực B, DD F1 #10 to #12): the phone field,
// the exact PATCH body, every validation code of #10 marking its field, the e-mail change request
// errors, and the hand-over to the code dialog. Only HTTP is faked.
import {
  act,
  fireEvent,
  render,
  renderHook,
  screen,
  waitFor,
} from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { beforeEach, describe, expect, it, vi } from "vitest";
import viMessages from "../../../../messages/vi.json";
import { ApiError } from "@/shared/api";
import { NextIntlClientProvider } from "@/shared/i18n";
import { toast, useToasts } from "@/shared/lib/toast-store";
import type { UserProfile } from "@/entities/user";

vi.mock("@/shared/api", async (importOriginal) =>
  (await import("@/shared/test/real-api")).realApiModule(
    await importOriginal<Record<string, unknown>>(),
  ),
);

// Import after the mocks.
import { apiMock, resetApiMock } from "@/shared/test/real-api";
import { PersonalInfoCard } from "./personal-info-card";

const p = viMessages.profile;
const apiErrors = viMessages.apiErrors as Record<string, string>;
const PROFILE = "/identity/me/profile";

const profile: UserProfile = {
  userId: "u1",
  displayName: "Learner One",
  email: "learner@example.com",
  schoolOrCompany: null,
  currentPosition: null,
  targetPosition: null,
  phone: "0901 234 567",
  hasPassword: true,
  passwordChangedAt: null,
  linkedProviders: [],
  joinedAt: "2026-10-31T00:00:00Z",
  roleName: "Học viên",
};

const onProfileSaved = vi.fn();
let client: QueryClient;
const invalidate = vi.fn();

function watch() {
  const toasts = renderHook(() => useToasts());
  act(() => toast.clear());
  return () => toasts.result.current.map((item) => [item.tone, item.message]);
}

function renderCard() {
  client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  client.invalidateQueries = invalidate;
  render(
    <QueryClientProvider client={client}>
      <NextIntlClientProvider locale="vi" messages={viMessages}>
        <PersonalInfoCard profile={profile} onProfileSaved={onProfileSaved} />
      </NextIntlClientProvider>
    </QueryClientProvider>,
  );
}

const field = (label: string) => screen.getByLabelText(label);
const set = (label: string, value: string) =>
  fireEvent.change(field(label), { target: { value } });
const save = () => fireEvent.click(screen.getByRole("button", { name: p.form.save }));
const saved = (patch: Partial<UserProfile> = {}) => ({ ...profile, ...patch });

describe("PersonalInfoCard", () => {
  beforeEach(() => {
    resetApiMock();
    // The form always PATCHes the profile alongside an e-mail change request.
    apiMock.patch.mockResolvedValue(saved());
    onProfileSaved.mockReset();
    invalidate.mockReset();
    act(() => toast.clear());
  });

  it("shows the stored phone number", () => {
    renderCard();
    expect(field(p.form.phone)).toHaveValue("0901 234 567");
  });

  it("sends the trimmed phone with the other four fields and toasts success", async () => {
    const read = watch();
    apiMock.patch.mockResolvedValue(saved({ phone: "+84 90 123 4567" }));
    renderCard();
    set(p.form.phone, "  +84 90 123 4567 ");

    save();

    await waitFor(() =>
      expect(apiMock.patch).toHaveBeenCalledWith(PROFILE, {
        displayName: "Learner One",
        schoolOrCompany: "",
        currentPosition: "",
        targetPosition: "",
        phone: "+84 90 123 4567",
      }),
    );
    await waitFor(() =>
      expect(read().at(-1)).toEqual(["success", p.form.savedToast]),
    );
    expect(onProfileSaved).toHaveBeenCalledWith(
      expect.objectContaining({ phone: "+84 90 123 4567" }),
    );
  });

  it("sends an empty string when the phone is cleared", async () => {
    apiMock.patch.mockResolvedValue(saved({ phone: null }));
    renderCard();
    set(p.form.phone, "");

    save();

    await waitFor(() =>
      expect(apiMock.patch).toHaveBeenCalledWith(
        PROFILE,
        expect.objectContaining({ phone: "" }),
      ),
    );
  });

  it.each(["abc123", "0901-234-567 ext", "1".repeat(21)])(
    "rejects the phone %j on the client without calling the server",
    async (value) => {
      const read = watch();
      renderCard();
      set(p.form.phone, value);

      save();

      await waitFor(() =>
        expect(read().at(-1)).toEqual(["error", p.errors.phoneInvalid]),
      );
      expect(field(p.form.phone)).toHaveAttribute("aria-invalid", "true");
      expect(apiMock.patch).not.toHaveBeenCalled();
    },
  );

  it.each([
    ["IDT-V13", p.form.phone],
    ["IDT-V19", p.form.displayName],
    ["IDT-V20", p.form.displayName],
    ["IDT-V09", p.form.schoolOrCompany],
    ["IDT-V07", p.form.currentPosition],
    ["IDT-V08", p.form.targetPosition],
  ])("marks the field and toasts apiErrors for %s", async (code, label) => {
    const read = watch();
    apiMock.patch.mockRejectedValue(new ApiError(code, 422));
    renderCard();
    set(p.form.phone, "0901 234 568");

    save();

    await waitFor(() =>
      expect(read().at(-1)).toEqual(["error", apiErrors[code]]),
    );
    expect(field(label)).toHaveAttribute("aria-invalid", "true");
  });

  it("falls back to the generic save failure for a non-validation error", async () => {
    const read = watch();
    apiMock.patch.mockRejectedValue(new ApiError("IDT-503", 503));
    renderCard();
    set(p.form.phone, "0901 234 568");

    save();

    await waitFor(() =>
      expect(read().at(-1)).toEqual(["error", p.form.saveFailed]),
    );
  });

  it("requests an e-mail change, opens the code dialog and refreshes the profile once confirmed", async () => {
    const read = watch();
    apiMock.post.mockImplementation((path: string) =>
      Promise.resolve(
        path.endsWith("/request")
          ? { pendingEmail: "new@example.com", expiresInSeconds: 600 }
          : { email: "new@example.com" },
      ),
    );
    renderCard();
    set(p.form.email, "new@example.com");

    save();

    expect(await screen.findByRole("dialog")).toBeInTheDocument();
    expect(apiMock.post).toHaveBeenCalledWith(
      "/identity/me/email-change/request",
      { newEmail: "new@example.com" },
    );
    expect(apiMock.patch).toHaveBeenCalledTimes(1);

    fireEvent.change(screen.getByLabelText(p.emailOtp.codeLabel), {
      target: { value: "123456" },
    });
    fireEvent.click(screen.getByRole("button", { name: p.emailOtp.confirm }));

    await waitFor(() =>
      expect(invalidate).toHaveBeenCalledWith({
        queryKey: ["user", "my-profile"],
      }),
    );
    expect(read().at(-1)).toEqual(["success", p.emailOtp.done]);
    expect(screen.queryByRole("dialog")).toBeNull();
  });

  it.each(["IDT-415", "IDT-400", "IDT-V05", "IDT-V12", "IDT-503"])(
    "toasts apiErrors.%s when the e-mail change request fails and opens no dialog",
    async (code) => {
      const read = watch();
      apiMock.post.mockRejectedValue(new ApiError(code, 409));
      renderCard();
      set(p.form.email, "taken@example.com");

      save();

      await waitFor(() =>
        expect(read().at(-1)).toEqual(["error", apiErrors[code]]),
      );
      expect(screen.queryByRole("dialog")).toBeNull();
    },
  );
});
