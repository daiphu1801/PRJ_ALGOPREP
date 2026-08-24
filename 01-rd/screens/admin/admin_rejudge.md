# RD — Màn `admin_rejudge` (Chấm lại lượt nộp)

> Slug: `admin_rejudge` — khớp `01-rd/overview/system_survey.md` mục 7.3 [SoT: 01-rd/overview/system_survey.md:530].
> Bounded Context: `judge-orchestration` (F4) [SoT: 01-rd/overview/system_survey.md:530]. Actor: A3.
>
> Đối chiếu prototype: `09-layoutBase/Admin - Chấm lại.dc.html`. File này mô tả **hành vi và UX ở mức yêu
> cầu** — không lặp lại đặc tả chức năng đã có ở `01-rd/req/req.md` (mục F4-09a→e), chỉ trỏ tới và bổ sung
> phần đặc thù của màn. Khớp RD tốt nhất trong 8 màn Admin — F4-09a→e đã đặc tả rất chi tiết trước khi
> prototype được đối chiếu.

## 1. Mục đích màn hình

Vận hành một phiên chấm lại (rejudge) quy mô lớn — chọn phạm vi, xem ước lượng ảnh hưởng, chạy thử trên mẫu
nhỏ, rồi chạy thật có kiểm soát (tạm dừng/huỷ giữa chừng), để áp dụng thay đổi cho hàng nghìn lượt nộp cũ một
cách an toàn [SoT: 01-rd/req/req.md:229-258 — F4-09a→e].

## 2. Nguồn yêu cầu (không lặp lại — chỉ trỏ)

| Hành vi | Mã | Nguồn |
| :--- | :--- | :--- |
| Chọn phạm vi chấm lại (theo bài toán / theo lượt nộp / theo khoảng thời gian) | F4-09a | `01-rd/req/req.md:231-236` |
| Ước lượng ảnh hưởng trước khi chạy thật (dry-run) | F4-09b | `01-rd/req/req.md:236-240` |
| Tuỳ chọn khi chạy (giữ điểm cao hơn, không hạ điểm) | F4-09c | `01-rd/req/req.md:240-243` |
| Điều khiển phiên đang chạy (tạm dừng/tiếp tục/huỷ), bất biến dữ liệu | F4-09d | `01-rd/req/req.md:244-` |
| Audit trail mỗi phiên chấm lại | F4-09e | `01-rd/req/req.md:250-` |
| Given-When-Then liên quan | — | `01-rd/req/user_stories.md:334-346` (`US-A3-05`) |

## 3. Trạng thái và cấu trúc màn (screen states)

Đối chiếu `09-layoutBase/Admin - Chấm lại.dc.html`:

1. **Chọn phạm vi** — 3 tab: Theo bài toán / Theo lượt nộp / Theo khoảng thời gian, mỗi tab đổi hẳn bộ field
   nhập tương ứng (dòng 429-449) — khớp đúng F4-09a (ba kiểu phạm vi, mỗi kiểu tham số riêng).
2. **Tuỳ chọn khi chạy** — 3 toggle: "Giữ kết quả cũ nếu điểm mới thấp hơn" (khớp F4-09c — không hạ điểm đã
   công bố), "Thông báo cho người học" (chưa có mã — xem Câu hỏi mở Q1), "Chạy ở hàng đợi nền, không chèn
   trước job của người học" (khớp nguyên tắc ưu tiên trải nghiệm thời gian thực đã ghi ở `req.md:243`).
3. **Ước lượng** — số lượt bị ảnh hưởng, thời gian ước tính, số lần chạy testcase, cùng nút "Chạy thử 20
   lượt" và "Bắt đầu chấm lại" (dòng 199-207) — khớp F4-09b.
4. **Đang chạy** — thanh tiến độ %, nút Tạm dừng/Huỷ cho từng phiên (dòng 216-231) — khớp F4-09d.
5. **Quy tắc áp dụng** — 4 dòng quy tắc tĩnh (chỉ chấm lại lượt đã có kết quả cuối; kết quả cũ khôi phục
   được trong 30 ngày; bảng xếp hạng cập nhật sau khi phiên kết thúc; mỗi phiên ghi Nhật ký hệ thống) — khớp
   đúng F4-09c/e và con số 30 ngày khớp `req.md:247-248` (đề xuất `[SoT: Suy luận]`, đã có mặt ở prototype
   với cùng con số — không phải bằng chứng độc lập, chỉ là cùng một nguồn suy luận ban đầu).
6. **Lịch sử chấm lại** — bảng: phiên/phạm vi/lý do/số lượt/đổi kết quả/trạng thái/người chạy, kèm "Xuất CSV"
   (dòng 249-280) — khớp F4-09e (audit trail).

## 4. Given-When-Then bổ sung ở mức màn

- **Cho** quản trị viên chọn phạm vi "Theo lượt nộp" và dán một danh sách mã lượt nộp, **Khi** hệ thống
  kiểm tra, **Thì** màn hiện số lượt hợp lệ trong danh sách trước khi cho phép ước lượng/chạy
  [SoT: 09-layoutBase/Admin - Chấm lại.dc.html:437-440].
- **Cho** một phiên chấm lại đang chạy, **Khi** quản trị viên bấm "Huỷ", **Thì** lượt nộp đang chấm dở tại
  thời điểm huỷ không bị ghi kết quả nửa vời — khớp bất biến dữ liệu đã chốt ở F4-09d.

## 5. Câu hỏi mở

| # | Câu hỏi | Vì sao chưa trả lời được | Đề xuất | Chủ sở hữu |
| :-: | :--- | :--- | :--- | :--- |
| Q1 | Toggle "Thông báo cho người học" (gửi thông báo khi kết quả thay đổi do chấm lại) không có mã `Fx-nn`. `req.md` F4-09 không nhắc việc thông báo người học khi điểm/verdict của họ đổi do chấm lại. | Đây là một hành vi thông báo mới, khác nhóm email OTP (F1-17) hay email nhắc luyện tập đã đề xuất ở `settings` (F1-21) — chưa rõ có nằm trong phạm vi đồ án. | Coi là mở rộng nhỏ của F4-09c (tuỳ chọn khi chạy), không cần mã mới — thông báo trong-app (không nhất thiết qua email) khi lượt nộp cũ của người dùng bị đổi kết quả. Chốt hình thức thông báo cụ thể khi viết BD. | Chủ dự án |

## 6. Ngoài phạm vi file này

- Bảng màu, spacing, component cụ thể — thuộc BD (`02-bd/screens/admin/admin_rejudge.md`, chưa viết).
- Hợp đồng API (chọn phạm vi, dry-run, điều khiển phiên, audit trail) — thuộc DD
  (`03-dd/api/judge-orchestration.md`, chưa viết).
- Khái niệm "kỳ thi" xuất hiện ở nhãn lý do "Kỳ thi giữa kỳ CS201" (dòng 495) — cùng phát hiện Q2 của
  `01-rd/screens/admin/admin_queue_monitor.md`, không lặp lại chi tiết ở đây.

## 7. Tham chiếu

- `01-rd/req/req.md:229-258` — F4-09a→e.
- `01-rd/req/user_stories.md:334-346` — `US-A3-05`.
- `01-rd/overview/system_survey.md` mục 7.3 — dòng `admin_rejudge`.
- `09-layoutBase/Admin - Chấm lại.dc.html` — prototype.
- `01-rd/screens/admin/admin_queue_monitor.md` mục 5 Q2 — khái niệm "kỳ thi" liên quan.
