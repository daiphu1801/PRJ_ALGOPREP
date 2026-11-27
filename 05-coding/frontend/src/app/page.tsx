import { RootRedirect } from "@/widgets/app-shell";

/**
 * Trang gốc không có màn riêng: khách vào danh sách bài `/problems`, người đã đăng nhập về trang
 * theo vai trò (SHR0101 Q3). Việc quyết định cần token trong RAM nên nằm ở component client.
 */
export default function RootPage() {
  return <RootRedirect />;
}
