import { InstructorAuthView } from "@/views/instructor-auth";

// Separate from the shared `auth` screen at /login — DEC-2026-0925-instructor-separate-login-route.
export default function InstructorLoginPage() {
  return <InstructorAuthView />;
}
