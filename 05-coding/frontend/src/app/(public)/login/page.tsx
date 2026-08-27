import { AuthView } from "@/views/auth";

// Một màn `auth` duy nhất, chuyển chế độ tại chỗ (01-rd/screens/shared/auth.md mục 3 điểm 3);
// route riêng chỉ để hỗ trợ deep-link từ ngoài vào đúng chế độ.
export default function LoginPage() {
  return <AuthView initialMode="login" />;
}
