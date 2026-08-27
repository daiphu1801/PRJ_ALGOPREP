import type { Role } from "./types";

/**
 * Khu vực định tuyến của ứng dụng, khớp route group trong app/:
 * (public) · (student) · (instructor)/instructor/* · (admin)/admin/*
 */
export type AppArea = "public" | "student" | "instructor" | "admin";

/**
 * Role BẮT BUỘC để vào từng khu vực. Đây là nguồn duy nhất của luật này — middleware.ts (chưa
 * dựng, cần 03-dd/api/identity.md) phải đọc đúng map này thay vì viết lại, nếu không hai chỗ sẽ
 * lệch nhau và một khu vực sẽ hở quyền mà không ai thấy trong diff.
 *
 * LƯU Ý NGỮ NGHĨA: đây là role khu vực YÊU CẦU, KHÔNG phải role của người đang đăng nhập.
 * Phân quyền thật do backend kiểm; chặn ở client chỉ là trải nghiệm
 * (01-rd/system/frontend_architecture.md mục 4).
 */
export const ROLE_BY_AREA = {
  student: "STUDENT",
  instructor: "INSTRUCTOR",
  admin: "ADMIN",
} as const satisfies Record<Exclude<AppArea, "public">, Role>;

/** Đích điều hướng sau khi đăng nhập, theo vai trò — chốt tại 01-rd/screens/shared/auth.md:90 (Q3). */
export const HOME_PATH_BY_ROLE = {
  STUDENT: "/progress",
  INSTRUCTOR: "/instructor/overview",
  // Đã gỡ treo 2026-08-25: `admin_overview` có prototype thật (09-layoutBase/Admin - Tổng
  // quan.dc.html) và đã được bổ sung vào system_survey.md mục 7.3 sau đợt đối chiếu khu Admin.
  ADMIN: "/admin/overview",
} as const satisfies Record<Role, string>;
