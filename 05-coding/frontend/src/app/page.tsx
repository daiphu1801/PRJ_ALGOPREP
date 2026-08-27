import { redirect } from "next/navigation";

/**
 * Route gốc không có màn Landing riêng — `01-rd/screens/` không có màn nào như vậy trong 25 màn.
 *
 * Hành vi đầy đủ theo `01-rd/screens/shared/auth.md:90` (Q3): chưa đăng nhập thì về `/login`;
 * đã đăng nhập thì về đích theo vai trò (`entities/user` → `HOME_PATH_BY_ROLE`), và nếu người
 * dùng tới từ một liên kết cần đăng nhập trước thì ưu tiên quay lại đúng URL đó.
 *
 * Ở khung base chỉ hiện thực nhánh CHƯA ĐĂNG NHẬP, vì chưa có session thật
 * (`app/providers/auth-provider.tsx` còn TODO, cần `03-dd/api/identity.md`). Nhánh theo vai trò
 * và returnUrl thuộc `middleware.ts` — dựng cùng lúc với xác thực thật, không đoán trước.
 */
export default function RootPage() {
  redirect("/login");
}
