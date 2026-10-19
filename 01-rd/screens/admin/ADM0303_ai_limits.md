# RD (Yêu cầu hệ thống mới) — Hạn mức và chi phí AI / `ADM0303`

> Mã màn hình: `ADM0303` [SoT: 02-bd/_rules/bd-template-9sheet.md — mục 8, thêm 2026-10-10].
> Slug chính tắc: `admin_ai_limits` (`DEC-2026-1010-split-ai-admin-screens`).
> Phạm vi/Bounded Context: `ai-review` (F5), riêng một khối thuộc `problem-bank` (F2-14). Actor: A3 (Quản trị viên).
> Nguồn sự thật (SoT): `01-rd/req/ai-review.md` (F5-19, F5-25), `01-rd/screens/admin/ADM0302_ai_usage.md`,
> `DEC-2026-1001-admin-configurable-settings`, `DEC-2026-1003-ai-testcase-generation-quota`.
> Màn **tách từ `ADM0301`** ngày 2026-10-10. Tài liệu này có thể đọc độc lập.

---

## 1. Mục đích và bối cảnh

Một nơi để quản trị viên đặt **giới hạn và chi phí** của AI. Việc này tách khỏi `ADM0301` (chỉnh prompt, rubric, model:
chất lượng AI) vì khác bản chất: giới hạn là tham số ghi ngay, không có vòng đời nháp và phát hành; còn `ADM0302`
chỉ để theo dõi, không ghi.

Hạn mức token tháng và nút mở khoá ngân sách trước đây không có màn nào sở hữu: RD và BD của `ADM0301` đẩy
sang `ADM0302`, còn `ADM0302` đẩy ngược lại [SoT: 02-bd/screens/admin/ADM0302_ai_usage.md:21-22,171]. Chủ dự án chốt
2026-10-10 đặt hai việc đó ở màn này.

---

## 2. Phạm vi

| Hành vi | Mã | Nguồn |
| :--- | :--- | :--- |
| Hạn mức token tháng, tự động khoá khi vượt, ADMIN mở khoá hoặc tăng hạn mức | F5-25 | `01-rd/req/ai-review.md` — F5-25 |
| Giới hạn tần suất gọi AI theo người dùng, **theo giờ** (chủ dự án chốt 2026-10-10) | F5-19 | `01-rd/req/ai-review.md` — F5-19 |
| Ngưỡng cảnh báo ngân sách và đơn giá token theo mô hình, do ADMIN đặt | — | `DEC-2026-1001-admin-configurable-settings` |
| Số lần "Sinh tự động" testcase tối đa mỗi bài (mặc định 2) | F2-14 | `DEC-2026-1003-ai-testcase-generation-quota` |

Yêu cầu về hành vi:

1. Quản trị viên xem hạn mức token tháng, số đã dùng, trạng thái ngân sách (Đang mở / Đã khoá tự động) và đổi hạn mức.
2. Khi ngân sách đang khoá, quản trị viên mở khoá có xác nhận. Nếu số đã dùng chưa nhỏ hơn hạn mức thì phải tăng hạn mức
   trước (nếu không, hệ thống khoá lại ngay) `[SoT: Suy luận]`.
3. Quản trị viên đặt số lượt Phân tích bài giải mỗi giờ, số phiên Phỏng vấn giả lập mỗi giờ và số giây chờ giữa hai
   yêu cầu. Thay đổi áp dụng cho lần gọi kế tiếp.
4. Quản trị viên đặt ngưỡng cảnh báo ngân sách (phần trăm) và danh sách đơn giá token theo mô hình. Màn `ADM0302` đọc lại hai giá trị này.
5. Quản trị viên đặt số lần sinh testcase tối đa mỗi bài. Đổi giá trị không xoá bộ đếm đã dùng.
6. Mọi thay đổi ghi ngay, báo kết quả bằng toast dùng chung.
7. Quyền: Function `AI_TOKEN_BUDGET` cho hạn mức và mở khoá, `AI_CONFIG` cho các khối còn lại [SoT: 02-bd/security/ai-review.md:79-85].

---

## 3. Ngoài phạm vi (Out of Scope)

- Prompt, model, rubric, nguyên tắc trả lời: `ADM0301`.
- Biểu đồ, xếp hạng, cảnh báo bất thường: `ADM0302`.
- Gói đăng ký trả phí tăng hạn mức cho người dùng (chủ dự án nêu như hướng tương lai 2026-10-10): xem Câu hỏi mở Q1.
- Giới hạn sandbox của script sinh dữ liệu: `ADM0501` Khu vực F.
- Bảng màu, bố cục, hợp đồng API: BD và DD (`02-bd/screens/admin/ADM0303_ai_limits.md`, `03-dd/api/ai-review.md` chưa viết).

---

## 4. Tiền đề và ràng buộc

| # | Câu hỏi | Vì sao chưa trả lời được | Đề xuất | Chủ sở hữu |
| :-: | :--- | :--- | :--- | :--- |
| Q1 | Có làm gói đăng ký trả phí (người dùng ngoài đóng tiền tháng để có hạn mức AI cao hơn) trong phạm vi đồ án không? | Cần bounded context thanh toán và gói, giới hạn tần suất và ngân sách theo gói, đổi rule F5-25 | Ghi là hướng mở rộng; BD giữ khoá giới hạn theo `limitCode` để thêm chiều "gói" sau | Chủ dự án |
| Q2 | Điều kiện mở khoá ngân sách (chỉ khi hạn mức lớn hơn số đã dùng, hay cho mở một lần bất chấp) | RD F5-25 chưa nêu ràng buộc | Chỉ khi hạn mức lớn hơn số đã dùng | Chủ dự án |

---

## 5. Danh sách yêu cầu (REQ)

| ID | Yêu cầu | Loại | Nguồn/SoT |
| :--- | :--- | :--- | :--- |
| REQ-01 | Đặt hạn mức token tháng; hệ thống tự khoá khi vượt (F5-25) | Chức năng | `01-rd/req/ai-review.md` — F5-25 |
| REQ-02 | ADMIN mở khoá ngân sách có xác nhận (F5-25) | Chức năng | `01-rd/req/ai-review.md` — F5-25 |
| REQ-03 | Đặt giới hạn tần suất gọi AI theo giờ (F5-19) | Chức năng | `01-rd/req/ai-review.md` — F5-19 |
| REQ-04 | Đặt ngưỡng cảnh báo ngân sách và đơn giá token theo mô hình | Chức năng | `DEC-2026-1001-admin-configurable-settings` |
| REQ-05 | Đặt số lần sinh testcase tối đa mỗi bài (F2-14) | Chức năng | `DEC-2026-1003-ai-testcase-generation-quota` |

---

## 6. Yêu cầu dữ liệu

Chi tiết bảng, cột, kiểu dữ liệu và ràng buộc lưu trữ thuộc BD/DD; tài liệu này không tự suy luận dữ liệu chưa được chốt.
Hợp đồng API thuộc DD.

---

## 7. RACI và danh sách bàn giao

| Bàn giao | Trạng thái |
| :--- | :--- |
| RD màn hình | Có tài liệu này |
| BD màn hình | `02-bd/screens/admin/ADM0303_ai_limits.md` V0.5 |
| Prototype | Đã dựng ở `views/admin/ai-limits`, route `/admin/ai-limits` |
| DD/API liên quan | Chưa xác định — không tự suy luận |

---

## 8. Tham chiếu

| Loại | Tham chiếu |
| :--- | :--- |
| Yêu cầu module | `01-rd/req/ai-review.md` — F5-19, F5-25 |
| Quyết định | `DEC-2026-1010-split-ai-admin-screens`, `DEC-2026-1001-admin-configurable-settings`, `DEC-2026-1003-ai-testcase-generation-quota` |
| Màn liên quan | `01-rd/screens/admin/ADM0301_ai_config.md`, `01-rd/screens/admin/ADM0302_ai_usage.md` |
| Báo cáo | `06-plan/reports/261009-1449-ai-admin-userflow-va-mau-thuan/report.md` — M-01, M-03, M-09 |
| Kế hoạch | `06-plan/261010-1902-ai-admin-tach-man-don-gian-hoa.md` |
