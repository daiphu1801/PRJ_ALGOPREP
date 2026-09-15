import { AdminAuthView } from "@/views/admin-auth";

// Separate from the shared `auth` screen at /login — DEC-2026-0915-admin-separate-login-route.
export default function AdminLoginPage() {
  return <AdminAuthView />;
}
