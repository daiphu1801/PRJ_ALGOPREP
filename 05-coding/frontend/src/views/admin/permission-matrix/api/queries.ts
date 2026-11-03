// Data boundary of the permission-matrix screen: `GetPermissionMatrix` (#30) with the role list (#32)
// in real mode, the prototype page in mock mode (03-dd/api/F1_identity.md section 0.5).
import { useQuery } from "@tanstack/react-query";
import { withMockData } from "@/shared/api";
import { fetchPermissionMatrix } from "./__mock__/permission-matrix-mocks";
import { realFetchPermissionMatrix } from "./real-permission-matrix";

export const PERMISSION_MATRIX_KEY = ["permission-matrix"] as const;

export const usePermissionMatrixPage = () =>
  useQuery({
    queryKey: PERMISSION_MATRIX_KEY,
    queryFn: () =>
      withMockData(fetchPermissionMatrix, realFetchPermissionMatrix),
    // A reopened screen must show what the server holds now, not a cached copy.
    staleTime: 0,
    retry: false,
  });
