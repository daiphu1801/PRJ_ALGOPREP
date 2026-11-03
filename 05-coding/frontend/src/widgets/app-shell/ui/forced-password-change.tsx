"use client";

import { useEffect } from "react";
import { setMustChangePassword, useMustChangePassword } from "@/entities/auth";
import { useMyProfile, type AppArea } from "@/entities/user";
import { ChangePasswordDialog } from "@/features/change-my-password";
import { isMockMode } from "@/shared/api";
import { LOGIN_PATH_BY_AREA } from "../model/login-path";
import { useSignOut } from "../model/use-sign-out";

/**
 * Blocks the area behind a password dialog while the account still has an admin-set temporary
 * password. Rendered inside each shell so the dialog gets that area's colours. This is UI only:
 * the server does not refuse other endpoints (BD architecture F1 section 3), and mock mode has no
 * real account to flag.
 *
 * The flag first comes from the login response. The profile (`GET /me/profile`, DD F1 #9) then
 * overrides it, so a tab opened later on an old session still learns the truth from the server.
 */
export function ForcedPasswordChange({
  area,
}: {
  area: Exclude<AppArea, "public">;
}) {
  const mustChange = useMustChangePassword();
  const signOut = useSignOut();
  const serverFlag = useMyProfile(area).data?.mustChangePassword;
  useEffect(() => {
    if (!isMockMode() && serverFlag !== undefined) {
      setMustChangePassword(serverFlag);
    }
  }, [serverFlag]);

  if (!mustChange || isMockMode()) return null;
  return (
    <ChangePasswordDialog
      hasPassword
      onClose={() => setMustChangePassword(false)}
      forced={{ onSignOut: () => void signOut(LOGIN_PATH_BY_AREA[area]) }}
    />
  );
}
