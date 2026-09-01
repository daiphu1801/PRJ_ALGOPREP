# RD — Màn `my_submissions` (Bài đã nộp)

> Slug: `my_submissions` — khớp `01-rd/overview/system_survey.md:484` [SoT: 01-rd/overview/system_survey.md:484].
> Bounded Context chính: `judge-orchestration` (F4) [SoT: 01-rd/overview/system_survey.md:484]. Actor: A1.
>
> Đối chiếu prototype: `09-layoutBase/Bài đã nộp.dc.html`. File này mô tả **hành vi và UX ở mức yêu cầu** —
> không lặp lại đặc tả chức năng đã có ở `01-rd/req/identity.md` và `01-rd/req/judge-orchestration.md`, chỉ trỏ tới và bổ sung phần đặc thù của màn.
> **Phát hiện của Phase 2, đã tự chốt 2026-08-25 — xem mục 5**: màn này (danh sách đầy đủ lịch sử nộp bài,
> lọc/tìm kiếm) ban đầu không có mã `Fx-nn` nào phủ đúng nội dung chính — đã bổ sung **F1-18**.

## 1. Mục đích màn hình

Xem lại toàn bộ lịch sử bài đã nộp của người học, lọc theo kết quả (verdict) và ngôn ngữ, tìm theo tên/mã
bài, mở kết quả chi tiết hoặc phân tích bài giải (nếu `Accepted`) cho từng lượt nộp.

## 2. Nguồn yêu cầu (không lặp lại — chỉ trỏ)

| Hành vi | Mã | Nguồn |
| :--- | :--- | :--- |
| Tỉ lệ chấp thuận (Accepted / tổng số đã nộp) — chỉ số tổng, không phải danh sách | F1-07 | `01-rd/req/identity.md` — F1-07 |
| Chấm bài theo testcase, verdict AC/WA/TLE/CE... | F4-01 → F4-08 | `01-rd/req/judge-orchestration.md` — F4-01 → F4-08 |
| Xem lịch sử nộp bài, lọc verdict/ngôn ngữ, tìm kiếm | F1-18 | `01-rd/req/identity.md` — F1-18 (khối bổ sung 2026-08-25, sau F1-17) |
| Given-When-Then liên quan | — | `01-rd/req/user_stories/a1_student.md` (`US-A1-04`), thêm GWT F1-18 ở `US-A1-05` |

## 3. Trạng thái và cấu trúc màn (screen states)

Đối chiếu `09-layoutBase/Bài đã nộp.dc.html`:

1. **Dải chỉ số tổng** — Lượt nộp (`418`), Được chấp nhận (`182`, `43%` — khớp F1-07), Đúng ngay lần đầu
   (`61 / 182`), Dùng nhiều nhất (`Python 3`, `68%`) (dòng 238-243). Chỉ số thứ 2 khớp F1-07; ba chỉ số còn
   lại (Lượt nộp tổng, đúng ngay lần đầu, ngôn ngữ dùng nhiều nhất) không có mã `Fx-nn` — cùng nhóm Câu hỏi
   mở Q1 (đây là các phép tính trên chính bảng lịch sử nộp bài, không phải chỉ số độc lập).
2. **Thanh công cụ** — ô tìm kiếm theo tên bài/mã bài, tab lọc theo verdict (Tất cả/Accepted/Sai kết
   quả/Quá thời gian/Lỗi biên dịch), tab lọc theo ngôn ngữ (dòng 112-123).
3. **Bảng lịch sử** — mỗi dòng: thời điểm, mã bài + tên + độ khó, ngôn ngữ, verdict (badge màu), số testcase
   đạt/tổng, thời gian chạy, và khi rê chuột hiện hai nút "Kết quả" (luôn có) và "Phân tích" (chỉ khi verdict
   là `AC`, dòng 138-165) — nút "Phân tích" khớp đúng quy tắc "chỉ bài Accepted mới mở được phân tích" đã
   ghi rõ ở chân bảng (dòng 173, khớp F5-01).
4. **Chân bảng** — tóm tắt số dòng đang hiện / tổng, số Accepted trong tập đang lọc, và dòng nhắc quy tắc mở
   khoá phân tích/phỏng vấn (dòng 171-174).
5. Không có phân trang riêng trong prototype (toàn bộ 10 dòng mẫu hiện hết) — chưa rõ hành vi khi lịch sử có
   hàng trăm/hàng nghìn lượt nộp — xem Câu hỏi mở Q2.
6. Verdict mẫu trong dữ liệu bao gồm `AC`/`WA`/`TLE`/`CE` (dòng 182-191) — khớp các verdict đã đặc tả ở F4;
   không có mẫu nào cho `Runtime Error` (`RE`), dù `RE` là verdict tiêu chuẩn của các nền tảng chấm bài tương
   tự và không bị loại trừ ở đặc tả F4 — nhiều khả năng chỉ là thiếu trong dữ liệu mẫu, không phải chủ ý loại
   bỏ verdict này.

## 4. Given-When-Then bổ sung ở mức màn

- **Cho** tôi có ít nhất một lượt nộp `Accepted` trong danh sách, **Khi** tôi rê chuột qua dòng đó, **Thì**
  tôi thấy cả nút "Kết quả" và nút "Phân tích"; với các verdict khác chỉ có nút "Kết quả"
  [SoT: 09-layoutBase/Bài đã nộp.dc.html:156-163].
- **Cho** tôi lọc theo một verdict cụ thể (ví dụ `WA`), **Khi** bộ lọc áp dụng, **Thì** chỉ số "đang hiện /
  tổng" ở chân bảng cập nhật theo đúng tập đã lọc, không phải tổng toàn bộ lịch sử
  [SoT: 09-layoutBase/Bài đã nộp.dc.html:171, 266-268].

## 5. Câu hỏi mở — đã tự chốt 2026-08-25

> Tự quyết theo yêu cầu trực tiếp của chủ dự án — prototype là bản dựng tham khảo, phần thuần UI đánh dấu
> **[Đợi nextjs]** để dựng lại đúng khi có frontend Next.js thật.

| # | Câu hỏi | Quyết định | Ghi chú |
| :-: | :--- | :--- | :--- |
| Q1 | Không có mã `Fx-nn` nào đặc tả "xem lại lịch sử nộp bài, lọc theo verdict/ngôn ngữ, tìm kiếm" — nội dung chính của màn này. | **Bổ sung F1-18 — Xem lịch sử nộp bài của chính mình**, lọc theo verdict/ngôn ngữ, tìm theo tên/mã bài. Đã ghi vào `01-rd/req/identity.md` (mục F1, sau khối F1-17) và thêm GWT vào `01-rd/req/user_stories/a1_student.md` (`US-A1-05`); đã đồng bộ `system_survey.md:484`. | Đã chốt, không còn mở. |
| Q2 | Prototype không có phân trang cho bảng lịch sử — hành vi thật khi có hàng trăm lượt nộp là gì? | **Chốt: áp cùng kiểu phân trang cuối bảng như `problem_list`** (`01-rd/screens/users/problem_list.md` mục 3.5) để nhất quán UX giữa hai bảng danh sách. | Số dòng mỗi trang, style phân trang cụ thể **[Đợi nextjs]**. |

## 6. Ngoài phạm vi file này

- Bảng màu, spacing, component cụ thể, breakpoint responsive — thuộc BD (`02-bd/screens/users/my_submissions.md`,
  chưa viết).
- Hợp đồng API (lọc, tìm kiếm, phân trang danh sách nộp bài) — thuộc DD (`03-dd/api/judge-orchestration.md`,
  chưa viết).
- Việc bổ sung mã `Fx-nn` mới (Q1) — chỉ đề xuất ở đây, chưa tự sửa `identity.md`/`user_stories/a1_student.md` vì chưa có xác
  nhận trực tiếp của chủ dự án (khác với các trường hợp Phase 0-1 đã hỏi và chốt ngay).

## 7. Tham chiếu

- `01-rd/req/identity.md` — F1-07, F1-18. `01-rd/req/judge-orchestration.md` — F4-01→F4-08.
- `01-rd/req/user_stories/a1_student.md` — `US-A1-04`, `US-A1-05`.
- `01-rd/overview/system_survey.md:484` — dòng `my_submissions` trong bảng màn mục 7.
- `09-layoutBase/Bài đã nộp.dc.html` — prototype.
- `01-rd/screens/users/problem_list.md` — mẫu phân trang tham khảo cho Câu hỏi mở Q2.
- `06-plan/reports/260825-1500-report-ai1-phase2-conflicts.md` — báo cáo xung đột + quyết định đã chốt.
