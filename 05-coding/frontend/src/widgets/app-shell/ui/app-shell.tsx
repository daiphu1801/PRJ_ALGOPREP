import type { ReactNode } from "react";
import type { AppArea } from "@/entities/user";
import { getT } from "@/shared/i18n/server";

type AppShellProps = {
  /** Khu vực định tuyến, KHÔNG phải role của người đang đăng nhập (xem entities/user/model/area.ts). */
  area: Exclude<AppArea, "public">;
  children: ReactNode;
};

/**
 * Khung ứng dụng dùng chung: header + vùng nội dung. Nội dung nav/sidebar thật theo từng khu vực
 * chưa chốt (02-bd/screens/ còn trống) nên chỉ đặt khung rỗng.
 *
 * Tên người dùng và role thật sẽ hiện ở đây khi có session (app/providers/auth-provider.tsx còn
 * là TODO) — hiện chỉ hiện nhãn khu vực để không giả vờ biết ai đang đăng nhập.
 */
export async function AppShell({ area, children }: AppShellProps) {
  const t = await getT("nav");

  return (
    <div className="flex min-h-screen flex-col bg-[var(--color-background)] text-[var(--color-text)]">
      <header className="glass-surface flex h-14 items-center justify-between border-b border-[var(--color-border)] px-4">
        <span className="font-semibold">AlgoPrep</span>
        <span className="text-sm text-[var(--color-text-muted)]">{t(`area.${area}`)}</span>
      </header>
      <main className="flex-1">{children}</main>
    </div>
  );
}
