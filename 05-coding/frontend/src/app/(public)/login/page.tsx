import { AuthView } from "@/views/auth";

// A single `auth` screen that switches mode in place (01-rd/screens/shared/auth.md section 3,
// point 3); the separate route only exists to support deep-linking into the right mode.
export default function LoginPage() {
  return <AuthView initialMode="login" />;
}
