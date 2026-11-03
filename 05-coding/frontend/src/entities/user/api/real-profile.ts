// Real calls for `GET` and `PATCH /identity/me/profile` (03-dd/api/F1_identity.md #9 and #10). The
// response also carries `defaultLanguage`, which no screen reads yet; only the fields of
// `UserProfile` are kept.
import { api } from "@/shared/api";
import type { UpdateProfileInput, UserProfile } from "../model/types";

const PROFILE_PATH = "/identity/me/profile";

export async function realGetMyProfile(): Promise<UserProfile> {
  return toProfile(await api.get<UserProfile>(PROFILE_PATH));
}

/** `PATCH /me/profile` (#10) sends the five editable fields; an empty string clears an optional one. */
export async function realUpdateMyProfile(
  input: UpdateProfileInput,
): Promise<UserProfile> {
  return toProfile(await api.patch<UserProfile>(PROFILE_PATH, input));
}

function toProfile(p: UserProfile): UserProfile {
  return {
    userId: p.userId,
    displayName: p.displayName,
    email: p.email,
    schoolOrCompany: p.schoolOrCompany,
    currentPosition: p.currentPosition,
    targetPosition: p.targetPosition,
    phone: p.phone ?? null,
    hasPassword: p.hasPassword,
    passwordChangedAt: p.passwordChangedAt,
    linkedProviders: p.linkedProviders,
    joinedAt: p.joinedAt,
    roleName: p.roleName,
    mustChangePassword: p.mustChangePassword,
  };
}
