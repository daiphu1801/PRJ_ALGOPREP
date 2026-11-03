// Actions of the permission-matrix screen (03-dd/api/F1_identity.md #31, #33, #34). Mock mode resolves
// at once and the view patches its own state; real mode waits for the server, then refetches so the
// grid, the role list and the user counts come back from it.
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { isMockMode, withMockData } from "@/shared/api";
import { roleKeyFor } from "../model/guards";
import type { BaseCategory, Role } from "../model/types";
import { PERMISSION_MATRIX_KEY } from "./queries";
import {
  realCreateRole,
  realDeleteRole,
  realToggleCell,
} from "./real-permission-matrix";

type ToggleInput = {
  roleId: string;
  functionId: string;
  actionId: string;
  granted: boolean;
};

export function usePermissionActions() {
  const queryClient = useQueryClient();
  // The mock page is static: refetching it would throw away what the view just patched.
  const refresh = () =>
    isMockMode()
      ? undefined
      : queryClient.invalidateQueries({ queryKey: PERMISSION_MATRIX_KEY });

  const toggle = useMutation({
    mutationFn: (input: ToggleInput): Promise<{ granted: boolean }> =>
      withMockData(
        () => ({ granted: input.granted }),
        () => realToggleCell(input),
      ),
    onSuccess: refresh,
  });
  const create = useMutation({
    mutationFn: (input: {
      name: string;
      baseCategory: BaseCategory;
    }): Promise<Role> =>
      withMockData<Role>(
        () => {
          const name = input.name.trim();
          const key = roleKeyFor(name);
          return {
            id: key,
            key,
            label: name,
            system: false,
            baseCategory: input.baseCategory,
            userCount: 0,
          };
        },
        () => realCreateRole(input),
      ),
    onSuccess: refresh,
  });
  const remove = useMutation({
    mutationFn: (input: { roleId: string }): Promise<void> =>
      withMockData(
        () => undefined,
        () => realDeleteRole(input),
      ),
    onSuccess: refresh,
  });

  return {
    toggleCell: toggle.mutateAsync,
    createRole: create.mutateAsync,
    deleteRole: remove.mutateAsync,
  };
}
