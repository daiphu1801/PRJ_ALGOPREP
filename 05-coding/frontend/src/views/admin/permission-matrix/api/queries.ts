// PROTOTYPE — no DD endpoint wired yet. See 06-plan/PROTOTYPE_DEBT.md
//
// Data boundary of the permission-matrix screen: `GetPermissionMatrix`
// (03-dd/api/F1_identity.md, row 30). Cell toggles are still local state in the view.
import { useMockQuery } from "@/shared/api/mock-query";
import { fetchPermissionMatrix } from "./__mock__/permission-matrix-mocks";

export const usePermissionMatrixPage = () =>
  useMockQuery({
    queryKey: ["permission-matrix"],
    mock: fetchPermissionMatrix,
    spec: "03-dd/api/F1_identity.md",
    staleTime: 0,
  });
