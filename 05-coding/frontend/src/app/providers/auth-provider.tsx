"use client";

import type { ReactNode } from "react";

/**
 * TODO(session-bootstrap): khôi phục session (gọi /auth/refresh bằng cookie HttpOnly) khi app
 * khởi động, để access token trong tokenStore không bị mất sau khi reload trang. Chưa dựng vì
 * chưa có 03-dd/api/identity.md — nếu một feature cần session đã khôi phục, việc này phải xong
 * trước (xem nextjs-fsd-expert Layer 6).
 */
export function AuthProvider({ children }: { children: ReactNode }) {
  return <>{children}</>;
}
