/**
 * Điểm sanitize HTML DUY NHẤT của frontend (đề bài Markdown, phản hồi AI) — mọi nơi khác
 * import từ đây, không import dompurify trực tiếp (chặn bằng ESLint no-restricted-imports).
 *
 * CHƯA TRIỂN KHAI, VÀ CHỦ ĐÍCH LÀ NÉM LỖI. Thư viện sanitizer chưa được chọn
 * (01-rd/system/frontend_architecture.md mục 1 — "ba thư viện phải quyết định bằng nhu cầu
 * thật"). Một sanitizer giả (ví dụ regex strip thẻ script) nguy hiểm hơn là không có: nó tạo
 * cảm giác an toàn sai và XSS vẫn đi qua bằng hàng chục vector khác (onerror, javascript:,
 * SVG, thẻ chưa đóng). Ném lỗi ngay để bất kỳ ai vô tình dùng nó đều thấy tại thời điểm dev,
 * thay vì phát hiện khi đã lên production.
 *
 * Khi triển khai thật: sanitize phía server là lớp một (Jsoup, overview.md mục 1.I), hàm này
 * là lớp phòng thủ thứ hai phía client.
 */
export function sanitizeHtml(_rawHtml: string): string {
  throw new Error(
    "sanitizeHtml chưa được triển khai: thư viện sanitizer chưa chọn. Không render HTML do người dùng nhập cho tới khi hàm này có bản thật (xem shared/lib/sanitize-html.ts).",
  );
}
