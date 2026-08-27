// Role theo README.md mục 4, F1: STUDENT | INSTRUCTOR | ADMIN — quyết định route group nào
// người dùng được vào (app/(student)|(instructor)|(admin)), NHƯNG phân quyền thật do backend
// kiểm, ẩn nút ở client chỉ là trải nghiệm (frontend_architecture.md mục 4).
export type Role = "STUDENT" | "INSTRUCTOR" | "ADMIN";

export type User = {
  id: string;
  displayName: string;
  role: Role;
};
