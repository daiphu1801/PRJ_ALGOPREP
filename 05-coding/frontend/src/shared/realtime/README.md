# shared/realtime

Client STOMP (WebSocket) và client SSE — hạ tầng truyền tin thuần, không biết nghiệp vụ.
Nguồn: `01-rd/system/frontend_architecture.md` mục 2, 2.B.

Chưa dựng ở bước base: chưa có `03-dd/api/` nào định nghĩa topic/endpoint thật để bọc client
vào. Dựng khi làm `features/submit-solution` (WebSocket) hoặc `features/conduct-mock-interview`
(SSE) — không dựng trước, tránh đoán hình dạng message rồi phải viết lại.
