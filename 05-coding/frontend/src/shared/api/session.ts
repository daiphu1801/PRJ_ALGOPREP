import { isMockMode } from "./mock-mode";
import { tokenStore } from "./token-store";

/**
 * Có phiên đăng nhập hay không (token còn trong RAM). Chế độ mock coi như luôn có phiên vì mock
 * không có phiên thật. Chỉ để chọn giao diện và việc có gọi API cá nhân hay không: máy chủ vẫn
 * kiểm quyền ở mọi request (DEC-2026-1126-guest-browse-published-problems).
 */
export function hasSession(): boolean {
  return isMockMode() || Boolean(tokenStore.get());
}
