// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// Data boundary of the user-management screen. `fetchReal` is still NOT_IMPLEMENTED until
// 03-dd/api/F1_identity.md `ListUsers` (#35) is wired. The view imports only these hooks.
import { useMockQuery } from "@/shared/api";
import { fetchAdminUserPage } from "./__mock__/admin-user-mocks";

/** `ListUsers`. */
export const useAdminUserPage = () =>
  useMockQuery({
    queryKey: ["admin-user-management"],
    mock: fetchAdminUserPage,
    spec: "03-dd/api/F1_identity.md",
  });
