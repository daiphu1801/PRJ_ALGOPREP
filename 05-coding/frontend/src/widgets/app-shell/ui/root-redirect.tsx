"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { HOME_PATH_BY_ROLE } from "@/entities/user";
import { isMockMode, tokenStore } from "@/shared/api";
import { readBaseCategory } from "@/shared/lib";

/**
 * Trang gốc `/` (01-rd/screens/shared/SHR0101_auth.md Q3, bổ sung 2026-11-26): người đã đăng nhập
 * về trang theo vai trò, khách vào danh sách bài. `AuthProvider` chỉ dựng cây con sau khi đã thử
 * khôi phục phiên, nên token đọc ở đây là kết quả cuối.
 */
export function RootRedirect() {
  const router = useRouter();
  useEffect(() => {
    const role = isMockMode() ? null : readBaseCategory(tokenStore.get());
    router.replace(role ? HOME_PATH_BY_ROLE[role] : "/problems");
  }, [router]);
  return null;
}
