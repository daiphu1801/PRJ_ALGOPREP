# RD — Màn `class_progress` (Tiến độ lớp)

> Slug: `class_progress` — khớp `01-rd/overview/system_survey.md` mục 7.2, dòng bảng slug
> [SoT: 01-rd/overview/system_survey.md:524]. Bounded Context: `identity` + `judge-orchestration`
> [SoT: 01-rd/overview/system_survey.md:524]. Actor: A2 (Giảng viên).
>
> Prototype đối chiếu: `09-layoutBase/Giáo viên - Tiến độ học viên.dc.html` (đã dựng thật, chưa sửa nợ quy
> chuẩn — xem mục 3 và Câu hỏi mở).
>
> File này mô tả **hành vi và UX ở mức yêu cầu** của một màn cụ thể — không lặp lại đặc tả chức năng đã có
> ở `01-rd/req/identity.md` hay `01-rd/req/user_stories/a2_instructor.md`, chỉ trỏ tới và bổ sung phần đặc thù của **một màn**.
>
> **Cập nhật 2026-08-30 (owner instruction — "tạo dashboard ở mức tốt là được"):** ràng buộc "không sửa
> `identity.md`/`user_stories/a2_instructor.md`" của phiên làm việc trước đã hết hiệu lực — chủ dự án giao quyền chốt Câu hỏi
> mở Q1-Q6 ở mức hợp lý, không cần đàm phán từng ngưỡng số. Đã cấp mã `F1-28`
> (`01-rd/req/identity.md`) và `US-A2-09` (`01-rd/req/user_stories/a2_instructor.md`), xem mục 5.

## 1. Mục đích màn hình

Cho giảng viên (A2) xem tiến độ học tập **tổng hợp của nhiều học viên** trong (các) lớp mình phụ trách —
điểm trung bình theo lớp, danh sách học viên kèm điểm/tỉ lệ hoàn thành/chuỗi ngày hoạt động, và một khối
"Cần chú ý" nêu các học viên có dấu hiệu tụt lại — để giảng viên phát hiện sớm học viên cần hỗ trợ mà không
phải mở từng trang tiến độ cá nhân
[SoT: 09-layoutBase/Giáo viên - Tiến độ học viên.dc.html:117-118, 136-150].

**Khác với `my_progress` (A1 xem tiến độ của chính mình, dựa F1-06/F1-07/F1-08):** màn này là góc nhìn
**tổng hợp nhiều học viên** của A2, nay có mã trực tiếp **F1-28** (chốt 2026-08-30) — xem mục 2 và Câu hỏi
mở Q1 (đã đóng).

## 2. Nguồn yêu cầu (không lặp lại — chỉ trỏ)

| Hành vi | Mã | Nguồn |
| :--- | :--- | :--- |
| Bài đã giải theo chủ đề (định nghĩa gốc, viết cho A1 xem tiến độ cá nhân) | F1-06 | `01-rd/req/identity.md` — F1-06 |
| Tỉ lệ chấp thuận — Accepted / tổng bài nộp (định nghĩa gốc, viết cho A1 xem tiến độ cá nhân) | F1-07 | `01-rd/req/identity.md` — F1-07 |
| Tổng hợp tiến độ nhiều học viên trong lớp phụ trách (điểm TB, hoàn thành, chuỗi ngày, xu hướng, khối "Cần chú ý") | F1-28 | `01-rd/req/identity.md` — F1-28 (bổ sung 2026-08-30) |
| ~~Bảng slug mục 7.2 ghi `class_progress` là "dẫn xuất từ F1-06, F1-07"~~ — đã thay bằng mã trực tiếp F1-28; `system_survey.md` cần cập nhật dòng này khi đợt rà soát tiếp theo chạm tới mục 7.2 | (lịch sử) | `01-rd/overview/system_survey.md:524` |
| Cơ chế xác định "lớp giảng viên phụ trách" — giao bài theo lớp, chỉ sinh viên lớp đó thấy bài | F2-12 | `01-rd/req/user_stories/a2_instructor.md` (`US-A2-03`) |
| Cùng cơ chế lớp giảng viên phụ trách áp dụng cho phạm vi hiển thị màn chấm bài (`instructor_grading`) — dẫn chiếu để dùng chung cơ chế, không phát minh riêng cho `class_progress` | F5-27 | `01-rd/req/ai-review.md` — F5-27 |
| Gác quyền mở màn bằng Function `CLASS_MANAGEMENT` trong ma trận phân quyền | F1-10, F1-12 | `01-rd/req/identity.md` — F1-10, F1-12 |
| Given-When-Then của A2 xem lớp/bài tập, không có mục nào cho "xem tiến độ tổng hợp" | — | `01-rd/req/user_stories/a2_instructor.md` (`US-A2-03` tới `US-A2-06` — không có US riêng cho `class_progress`, xem Câu hỏi mở Q1) |

**Đã giải quyết (2026-08-30):** `01-rd/req/identity.md` mục F1 từng chỉ định nghĩa F1-06/F1-07 cho **A1 xem
chính mình** [SoT: 01-rd/req/identity.md — F1-06, F1-07, đoạn mở đầu ghi rõ "Trang tiến độ cá nhân"], không có mã nào cho
A2 xem tổng hợp nhiều học viên. Khoảng trống này nay lấp bằng **F1-28** (`01-rd/req/identity.md`, bổ sung
2026-08-30) và **US-A2-09** (`01-rd/req/user_stories/a2_instructor.md`) — xem Câu hỏi mở Q1.

## 3. Trạng thái và cấu trúc màn (screen states)

Đối chiếu `09-layoutBase/Giáo viên - Tiến độ học viên.dc.html` — hành vi UX thật đã dựng, không phải suy
diễn:

1. **Trạng thái mặc định** — bố cục 3 khối xếp dọc [SoT: 09-layoutBase/Giáo viên - Tiến độ học viên.dc.html:112-190]:
   - Tiêu đề "Tiến độ học viên" kèm phụ đề "Theo dõi điểm số, hoàn thành và mức độ hoạt động" (dòng 117-118).
   - Khối trên gồm 2 cột: biểu đồ đường "Điểm trung bình theo lớp" 6 tuần gần nhất, vẽ 2 đường cho 2 lớp
     (dòng 122-134); và khối "Cần chú ý" liệt kê tối đa vài học viên có nhãn cảnh báo (dòng 136-150).
   - Bảng danh sách học viên: cột Học viên, Lớp, Điểm TB, Hoàn thành, Chuỗi ngày, Xu hướng (sparkline),
     Hoạt động gần nhất (dòng 168-186).
2. **Bộ lọc lớp** — tab ngang `Tất cả` / theo tên lớp cụ thể (`Thuật toán K21`, `CTDL K22`, `Luyện PV nâng
   cao`), lọc trực tiếp bảng học viên, không tải lại trang (dòng 160-163, 288-293, 296-300).
3. **Ô tìm kiếm học viên** theo tên, lọc cùng lúc với tab lớp (dòng 155-158, 295, 299).
4. **Đếm kết quả** — hiển thị dạng "N / 82 học viên" ở góc phải thanh lọc (dòng 164, 320) — con số tổng
   "82" là dữ liệu giả lập cứng trong `data()`, không phải hành vi thật (dòng 226-234 chỉ có 8 bản ghi mẫu).
5. **Nhãn "Cần chú ý"** — 4 nhãn quan sát được trong dữ liệu mẫu, nay rút gọn còn 3 theo `F1-28` (chốt
   2026-08-30): "Vắng bài" (không có lượt nộp nào trong 7 ngày gần nhất), "Theo dõi" (hoàn thành dưới 50%),
   "Cần hỗ trợ" (điểm trung bình giảm liên tiếp từ hai mốc tuần trở lên) — ba ngưỡng này là giá trị mặc định
   đề xuất `[SoT: Suy luận]`, BD/DD chỉnh được. Đã đóng Câu hỏi mở Q2.
6. **Cột "Điểm TB"** hiển thị số thang 10 (ví dụ `8.9`) — theo `F1-28`: quy đổi từ tỉ lệ Accepted/tổng nộp
   của học viên đó (F1-07) ×10, **không** dùng điểm AI tham khảo của F5-27 (điểm đó tính theo từng bài nộp,
   không phải theo học viên). Đã đóng Câu hỏi mở Q3.
7. **Cột "Hoàn thành"** hiển thị phần trăm (ví dụ `96%`) — theo `F1-28`: số bài được giao cho lớp (F2-12) mà
   học viên đã Accepted ít nhất một lần, chia cho tổng số bài được giao — không phải tỉ lệ Accepted/tổng nộp
   (đó là công thức của cột "Điểm TB" ở trên, dùng riêng). Đã đóng Câu hỏi mở Q3.
8. **Cột "Chuỗi ngày"** (streak) và cột "Xu hướng" (sparkline điểm 6 mốc gần nhất) — theo `F1-28`: "Chuỗi
   ngày" dùng chung khái niệm đã có ở F1-21 (không phải chỉ số mới); "Xu hướng" là điểm TB riêng của học viên
   đó theo 6 mốc tuần gần nhất (cùng công thức mục 6, tính theo cá nhân thay vì gộp lớp). Đã đóng Câu hỏi mở
   Q4.
9. **Điều hướng** — mục "Tiến độ học viên" trong sidebar khu Giảng viên, cạnh "Tổng quan" / "Lớp của tôi" /
   "Bài tập của tôi" / "Chấm bài" [SoT: 09-layoutBase/Giáo viên - Tiến độ học viên.dc.html:244-250] — khớp
   layout riêng biệt khu Giảng viên đã chốt ở `01-rd/overview/system_survey.md:508-512` (Phương án B, tách
   shell khỏi Admin).
10. Không thấy trạng thái rỗng (lớp chưa có học viên), trạng thái lỗi tải dữ liệu, hay phân trang cho danh
    sách học viên khi lớp đông — `data()` chỉ có 8 bản ghi mẫu trong khi đếm kết quả hiển thị "82 học viên"
    (dòng 226-234, 320). Theo `F1-28` (chốt 2026-08-30): phân trang cùng kiểu `problem_list` (F2-11); có
    trạng thái rỗng riêng và trạng thái lỗi kèm nút thử lại — chi tiết UI để BD/DD. Đã đóng Câu hỏi mở Q5.

## 4. Given-When-Then bổ sung ở mức màn

Vì không có US-A2 nào mô tả trực tiếp hành vi "xem tổng hợp nhiều học viên" (mục 2), các Given-When-Then
dưới đây được viết **từ hành vi UX quan sát được ở prototype**, đánh dấu rõ nguồn suy luận, chờ chủ dự án
xác nhận mã và gộp chính thức vào `identity.md`/`user_stories/a2_instructor.md` sau:

- **Cho** tôi là giảng viên đã đăng nhập và có ít nhất một lớp phụ trách, **Khi** tôi mở màn `class_progress`,
  **Thì** tôi chỉ thấy học viên thuộc (các) lớp mình phụ trách, dùng cùng cơ chế xác định phạm vi lớp đã
  dùng ở F2-12/F5-27 [SoT: 01-rd/req/ai-review.md — F5-27, "Phạm vi hiển thị: theo lớp giảng viên phụ trách (cùng cơ
  chế với F2-12)"; `[SoT: Suy luận]` khi áp dụng sang màn này vì `ai-review.md` không nêu đích danh `class_progress`].
- **Cho** danh sách học viên đang hiển thị, **Khi** tôi chọn một tab lớp cụ thể, **Thì** bảng và khối "Cần
  chú ý" đều lọc theo đúng lớp đó, không cần tải lại trang (F1-28, chốt 2026-08-30 — đóng Câu hỏi mở Q6:
  khối "Cần chú ý" **phải lọc theo tab lớp**, hành vi hiện tại của prototype — không lọc khối này — là thiếu
  sót cần sửa khi dựng UI thật, không phải chủ ý thiết kế)
  [SoT: 09-layoutBase/Giáo viên - Tiến độ học viên.dc.html:159-163, 288-300].
- **Cho** tôi không có quyền `CLASS_MANAGEMENT` hoặc không phụ trách lớp nào, **Khi** tôi cố mở màn
  `class_progress`, **Thì** hệ thống từ chối và không hiển thị dữ liệu học viên của lớp khác
  [SoT: 01-rd/req/ai-review.md — F5-27, áp dụng cùng cơ chế gác quyền; `[SoT: Suy luận]` cho riêng màn
  này].

## 5. Câu hỏi mở (chưa trả lời — không tự chọn thay)

| # | Câu hỏi | Vì sao chưa trả lời được | Đề xuất | Chủ sở hữu |
| :-: | :--- | :--- | :--- | :--- |
| Q1 | ~~`class_progress` (A2 xem tổng hợp nhiều học viên) hiện chỉ được ghi là "dẫn xuất từ F1-06, F1-07" — không có mã `Fx-nn`/`US-A2-nn` trực tiếp.~~ **ĐÃ CHỐT 2026-08-30 (owner instruction — "tạo dashboard ở mức tốt là được"):** cấp mã `F1-28` trong nhóm `CLASS_MANAGEMENT` (cạnh F1-23→27) và `US-A2-09` — xem `01-rd/req/identity.md`, `01-rd/req/user_stories/a2_instructor.md`. | — | Đã chốt — xem `F1-28`. | Đã đóng |
| Q2 | ~~Nhãn "Cần chú ý" áp dụng ngưỡng nào?~~ **ĐÃ CHỐT 2026-08-30:** "Vắng bài" — không có lượt nộp nào trong 7 ngày gần nhất; "Theo dõi" — hoàn thành dưới 50%; "Cần hỗ trợ" — điểm trung bình giảm liên tiếp từ hai mốc tuần trở lên. Ba ngưỡng là giá trị mặc định đề xuất (`[SoT: Suy luận]` trong `identity.md`), BD/DD chỉnh được, không phải hằng số cứng. | — | Đã chốt — xem `F1-28`. | Đã đóng |
| Q3 | ~~Cột "Điểm TB" và cột "Hoàn thành" tính từ công thức nào?~~ **ĐÃ CHỐT 2026-08-30:** Điểm TB = tỉ lệ Accepted/tổng nộp của học viên (F1-07) ×10 — không dùng điểm AI F5-27. Hoàn thành = số bài được giao (F2-12) đã Accepted ít nhất một lần, chia tổng số bài được giao. Hai cột dùng hai nguồn khác nhau, không trùng nhau. | — | Đã chốt — xem `F1-28`. | Đã đóng |
| Q4 | ~~"Chuỗi ngày" và "Xu hướng" không khớp mã nào — chỉ số mới hay trang trí prototype?~~ **ĐÃ CHỐT 2026-08-30:** giữ lại cả hai. "Chuỗi ngày" dùng chung khái niệm đã có ở F1-21 (không phải chỉ số mới). "Xu hướng" = điểm TB cá nhân theo 6 mốc tuần gần nhất (cùng công thức Q3, tính theo cá nhân). | — | Đã chốt — xem `F1-28`. | Đã đóng |
| Q5 | ~~Danh sách học viên khi lớp đông hiển thị thế nào — phân trang, cuộn, hay tải hết? Trạng thái rỗng/lỗi?~~ **ĐÃ CHỐT 2026-08-30:** phân trang cùng kiểu `problem_list` (F2-11); có trạng thái rỗng riêng (lớp chưa có học viên) và trạng thái lỗi kèm nút thử lại. Chi tiết UI để BD/DD. | — | Đã chốt — xem `F1-28`. | Đã đóng |
| Q6 | ~~Khối "Cần chú ý" có lọc theo tab lớp đang chọn hay luôn hiển thị toàn bộ lớp?~~ **ĐÃ CHỐT 2026-08-30:** phải lọc theo đúng tab lớp đang chọn, giống bảng học viên — hành vi hiện tại của prototype (không lọc) là thiếu sót cần sửa khi dựng UI thật, không phải chủ ý thiết kế. | — | Đã chốt — xem `F1-28`. | Đã đóng |

## 6. Ngoài phạm vi file này

- Layout, vùng bố cục, bảng màu, component cụ thể — thuộc BD (`02-bd/screens/teacher/class_progress.md`,
  chưa viết).
- Hợp đồng API (request/response lấy danh sách học viên, dữ liệu biểu đồ) — thuộc DD
  (`03-dd/api/identity.md` hoặc `03-dd/api/judge-orchestration.md`, chưa viết).
- Cơ chế RBAC/Function `CLASS_MANAGEMENT` chi tiết (định nghĩa Action, seed dữ liệu ma trận) — thuộc BD/DD
  của module `identity`, không thuộc file theo trục màn này.

## 7. Tham chiếu

- `01-rd/overview/system_survey.md:524` — dòng `class_progress` trong bảng slug mục 7.2.
- `01-rd/overview/system_survey.md:508-512` — quyết định layout riêng biệt khu Giảng viên (Phương án B).
- `01-rd/req/identity.md` — F1-06, F1-07 (định nghĩa gốc, viết cho A1), F1-28 (chốt 2026-08-30, mã trực tiếp cho màn này), F1-12 (Function `CLASS_MANAGEMENT`).
- `01-rd/req/ai-review.md` — F5-27 (cơ chế phạm vi lớp giảng viên phụ trách).
- `01-rd/req/user_stories/a2_instructor.md` — `US-A2-03` tới `US-A2-06`, `US-A2-09` (chốt 2026-08-30).
- Quyết định: `DEC-2026-0830-class-progress-dashboard`.
- `09-layoutBase/Giáo viên - Tiến độ học viên.dc.html` — prototype đối chiếu.
- `06-plan/PROTOTYPE_DEBT.md:534-545` — mục 6.2.b, ghi nhận `class_progress` đã có prototype thật, đối
  chiếu tên file/slug.
- `06-plan/nexa-plan/260824-2043-bd-screens-common-first.md:45` — xếp `class_progress` vào nhóm màn giảng
  viên có thể cắt ở Stage 5 nếu thiếu thời gian.
