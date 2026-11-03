// Actions of the user-management screen (03-dd/api/F1_identity.md #36 to #38, #50). In mock mode they
// resolve at once and the view patches its own rows; in real mode the server answers first and the
// list is refetched afterwards, so the counters and the rows come back from the server.
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { isMockMode, withMockData } from "@/shared/api";
import type { AdminUser, AdminUserRole } from "../model/types";
import { ADMIN_USERS_KEY } from "./queries";
import {
  realChangeUserRole,
  realCreateUserAccount,
  realLockUser,
  realResetUserPassword,
  realUnlockUser,
} from "./real-user-management";

type CreateInput = {
  name: string;
  email: string;
  role: Exclude<AdminUserRole, "admin">;
};

export function useAdminUserActions() {
  const queryClient = useQueryClient();
  // The mock list is static: refetching it would throw away the rows the view just patched.
  const refresh = () =>
    isMockMode()
      ? undefined
      : queryClient.invalidateQueries({ queryKey: ADMIN_USERS_KEY });

  const changeRole = useMutation({
    mutationFn: (input: { id: string; role: AdminUserRole; self: boolean }) =>
      withMockData(
        () => undefined,
        () => realChangeUserRole(input),
      ),
    onSuccess: refresh,
  });
  const lock = useMutation({
    mutationFn: (input: {
      id: string;
      reason: string;
      notify: boolean;
      self: boolean;
    }) =>
      withMockData(
        () => undefined,
        () => realLockUser(input),
      ),
    onSuccess: refresh,
  });
  const unlock = useMutation({
    mutationFn: (input: { id: string }) =>
      withMockData(
        () => undefined,
        () => realUnlockUser(input),
      ),
    onSuccess: refresh,
  });
  const resetPassword = useMutation({
    mutationFn: (input: { id: string }) =>
      withMockData(
        () => undefined,
        () => realResetUserPassword(input),
      ),
  });
  const create = useMutation({
    mutationFn: (input: CreateInput): Promise<AdminUser> =>
      withMockData<AdminUser>(
        () => ({
          id: input.email,
          name: input.name,
          email: input.email,
          role: input.role,
          solvedCount: 0,
          submissionCount: 0,
          lastActiveLabel: "-",
          status: "pending" as const,
        }),
        () => realCreateUserAccount(input),
      ),
    onSuccess: refresh,
  });

  return {
    changeRole: changeRole.mutateAsync,
    lock: lock.mutateAsync,
    unlock: unlock.mutateAsync,
    resetPassword: resetPassword.mutateAsync,
    create: create.mutateAsync,
  };
}
