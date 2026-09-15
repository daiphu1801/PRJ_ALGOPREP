// Role per README.md section 4, F1: STUDENT | INSTRUCTOR | ADMIN — determines which route group
// the user can enter (app/(student)|(instructor)|(admin)), BUT real authorization is enforced by
// the backend; hiding a button on the client is only UX (frontend_architecture.md section 4).
export type Role = "STUDENT" | "INSTRUCTOR" | "ADMIN";

export type User = {
  id: string;
  displayName: string;
  role: Role;
};
