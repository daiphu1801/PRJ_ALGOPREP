/**
 * Đường dẫn khu Người học mà khách (chưa đăng nhập) được vào: danh sách bài và trang tổng quan một
 * bài (DEC-2026-1126-guest-browse-published-problems). `/problems/{id}/...` và mọi đường dẫn khác
 * vẫn cần phiên. Chỉ là điều hướng giao diện; máy chủ kiểm quyền ở từng request.
 */
export function isGuestReadablePath(pathname: string): boolean {
  return /^\/problems(\/[^/]+)?\/?$/.test(pathname);
}
