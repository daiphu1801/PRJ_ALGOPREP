import { InstructorAuthView } from "@/views/teacher/auth";

// Separate from the shared `auth` screen at /login — DEC-2026-0925-instructor-separate-login-route.
export default function InstructorLoginPage() {
  return <InstructorAuthView />;
}
