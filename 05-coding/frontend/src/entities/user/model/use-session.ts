"use client";

import { useSyncExternalStore } from "react";
import { hasSession, tokenStore } from "@/shared/api";

export type SessionState = "guest" | "signedIn";

/**
 * Phiên hiện tại của trình duyệt: `guest` khi không có token, `signedIn` khi có. Đọc từ `tokenStore`
 * (không gọi mạng), nên đăng nhập, đăng xuất hay refresh thất bại đều làm component dựng lại.
 * `AuthProvider` chỉ dựng cây con sau khi đã thử khôi phục phiên, nên ở đây không có trạng thái
 * "đang tải" riêng.
 */
export function useSession(): SessionState {
  return useSyncExternalStore(
    tokenStore.subscribe,
    () => (hasSession() ? "signedIn" : "guest"),
    () => "guest",
  );
}
