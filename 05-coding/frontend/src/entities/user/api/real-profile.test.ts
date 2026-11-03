import { beforeEach, describe, expect, it, vi } from "vitest";

const get = vi.fn();
const patch = vi.fn();

vi.mock("@/shared/api", async (importOriginal) => ({
  ...(await importOriginal<Record<string, unknown>>()),
  api: {
    get: (...args: unknown[]) => get(...args),
    patch: (...args: unknown[]) => patch(...args),
  },
}));

// Import after the mock so the module talks to the fake `api`.
import { realGetMyProfile, realUpdateMyProfile } from "./real-profile";

describe("realGetMyProfile", () => {
  beforeEach(() => {
    get.mockReset();
    patch.mockReset();
  });

  it("reads the caller's profile and keeps only the fields the app uses", async () => {
    get.mockResolvedValue({
      userId: "u1",
      displayName: "Learner",
      email: "a@b.vn",
      schoolOrCompany: null,
      currentPosition: null,
      targetPosition: null,
      defaultLanguage: "PYTHON",
      phone: null,
      hasPassword: true,
      passwordChangedAt: null,
      linkedProviders: ["GOOGLE"],
      joinedAt: "2026-10-31T00:00:00Z",
      roleName: "Học viên",
      mustChangePassword: true,
    });

    const profile = await realGetMyProfile();

    expect(get).toHaveBeenCalledWith("/identity/me/profile");
    expect(profile).toMatchObject({
      userId: "u1",
      hasPassword: true,
      linkedProviders: ["GOOGLE"],
      mustChangePassword: true,
    });
    expect(profile.phone).toBeNull();
    expect(profile).not.toHaveProperty("defaultLanguage");
  });

  it("saves the four editable fields and returns the stored profile", async () => {
    patch.mockResolvedValue({
      userId: "u1",
      displayName: "New Name",
      email: "a@b.vn",
      schoolOrCompany: null,
      currentPosition: "Dev",
      targetPosition: null,
      phone: "+84 90 123 4567",
      hasPassword: true,
      passwordChangedAt: null,
      linkedProviders: [],
      joinedAt: "2026-10-31T00:00:00Z",
      roleName: "Học viên",
    });
    const input = {
      displayName: "New Name",
      schoolOrCompany: "",
      currentPosition: "Dev",
      targetPosition: "",
      phone: "+84 90 123 4567",
    };

    const profile = await realUpdateMyProfile(input);

    expect(patch).toHaveBeenCalledWith("/identity/me/profile", input);
    expect(profile).toMatchObject({
      displayName: "New Name",
      schoolOrCompany: null,
      currentPosition: "Dev",
      phone: "+84 90 123 4567",
    });
  });

  it("sends an empty phone string to clear it", async () => {
    patch.mockResolvedValue({ userId: "u1", phone: null });
    const input = {
      displayName: "New Name",
      schoolOrCompany: "",
      currentPosition: "",
      targetPosition: "",
      phone: "",
    };
    await realUpdateMyProfile(input);
    expect(patch).toHaveBeenCalledWith("/identity/me/profile", input);
  });
});
