# RD (Yêu cầu hệ thống mới) — Tiến độ lớp / `INS0203`

> Mã màn hình: `INS0203` [Nguồn: 02-bd/_rules/bd-template-9sheet.md — mục 8].
> Slug chính tắc: `class_progress` [SoT: 01-rd/overview/system_survey.md — mục 7.2 dòng `class_progress`].
> Phạm vi/Bounded Context: `identity` + `judge-orchestration`
> [SoT: 01-rd/overview/system_survey.md — mục 7.2 dòng `class_progress`]. Actor: A2 (Giảng viên).
> Nguồn sự thật (SoT): `01-rd/req/F1_identity.md` (F1-28), `01-rd/req/user_stories/a2_instructor.md` (`US-A2-09`),
> `09-layoutBase/Giáo viên - Tiến độ học viên.dc.html` (prototype).
> Tài liệu này có thể đọc độc lập.

---

## 1. Mục đích và bối cảnh

Prototype đối chiếu: `09-layoutBase/Giáo viên - Tiến độ học viên.dc.html` (đã dựng thật, chưa sửa nợ quy
chuẩn — xem mục 3 và Câu hỏi mở). File này mô tả **hành vi và UX ở mức yêu cầu** của một màn cụ thể — không
lặp lại đặc tả chức năng đã có ở `01-rd/req/F1_identity.md` hay `01-rd/req/user_stories/a2_instructor.md`, chỉ
trỏ tới và bổ sung phần đặc thù của **một màn**.

**Cập nhật 2026-08-30 (owner instruction — "tạo dashboard ở mức tốt là được"):** ràng buộc "không sửa
`F1_identity.md`/`user_stories/a2_instructor.md`" của phiên làm việc trước đã hết hiệu lực — chủ dự án giao
quyền chốt Câu hỏi mở Q1-Q6 ở mức hợp lý, không cần đàm phán từng ngưỡng số. Đã cấp mã `F1-28`
(`01-rd/req/F1_identity.md`) và `US-A2-09` (`01-rd/req/user_stories/a2_instructor.md`), xem mục 5.

Cho giảng viên (A2) xem tiến độ học tập **tổng hợp của nhiều học viên** trong (các) lớp mình phụ trách —
điểm trung bình theo lớp, danh sách học viên kèm điểm/tỉ lệ hoàn thành/chuỗi ngày hoạt động, và một khối
"Cần chú ý" nêu các học viên có dấu hiệu tụt lại — để giảng viên phát hiện sớm học viên cần hỗ trợ mà không
phải mở từng trang tiến độ cá nhân
[SoT: 09-layoutBase/Giáo viên - Tiến độ học viên.dc.html:117-118, 136-150].

**Khác với `my_progress` (A1 xem tiến độ của chính mình, dựa F1-06/F1-07/F1-08):** màn này là góc nhìn
**tổng hợp nhiều học viên** của A2, nay có mã trực tiếp **F1-28** (chốt 2026-08-30) — xem mục 2 và Câu hỏi
mở Q1 (đã đóng).

---

## 2. Phạm vi

| Hành vi | Mã | Nguồn |
| :--- | :--- | :--- |
| Bài đã giải theo chủ đề (định nghĩa gốc, viết cho A1 xem tiến độ cá nhân) | F1-06 | `01-rd/req/F1_identity.md` — F1-06 |
| Tỉ lệ chấp thuận — Accepted / tổng bài nộp (định nghĩa gốc, viết cho A1 xem tiến độ cá nhân) | F1-07 | `01-rd/req/F1_identity.md` — F1-07 |
| Tổng hợp tiến độ nhiều học viên trong lớp phụ trách (điểm TB, hoàn thành, chuỗi ngày, xu hướng, khối "Cần chú ý") | F1-28 | `01-rd/req/F1_identity.md` — F1-28 (bổ sung 2026-08-30) |
| ~~Bảng slug mục 7.2 ghi `class_progress` là "dẫn xuất từ F1-06, F1-07"~~ — đã thay bằng mã trực tiếp F1-28; `system_survey.md` cần cập nhật dòng này khi đợt rà soát tiếp theo chạm tới mục 7.2 | (lịch sử) | `01-rd/overview/system_survey.md` — mục 7.2 dòng `class_progress` |
| Cơ chế xác định "lớp giảng viên phụ trách" — giao bài theo lớp, chỉ sinh viên lớp đó thấy bài | F2-12 | `01-rd/req/user_stories/a2_instructor.md` (`US-A2-03`) |
| ~~Cùng cơ chế lớp giảng viên phụ trách áp dụng cho phạm vi hiển thị màn chấm bài (`instructor_grading`)~~ **Đã bỏ 2026-10-13** (`DEC-2026-1013-remove-instructor-manual-grading`): màn chấm bài không còn; `class_progress` vẫn dùng phạm vi lớp phụ trách của F2-12/F1-12 | ~~F5-27~~ | `01-rd/req/F5_ai-review.md` — F5-27 (mục đã bỏ) |
| Gác quyền mở màn bằng Function `CLASS_MANAGEMENT` trong ma trận phân quyền | F1-10, F1-12 | `01-rd/req/F1_identity.md` — F1-10, F1-12 |
| Given-When-Then của A2 xem lớp/bài tập, không có mục nào cho "xem tiến độ tổng hợp" | — | `01-rd/req/user_stories/a2_instructor.md` (`US-A2-03` tới `US-A2-05`, `US-A2-06` đã bỏ 2026-10-13 — không có US riêng cho `class_progress`, xem Câu hỏi mở Q1) |

**Đã giải quyết (2026-08-30):** `01-rd/req/F1_identity.md` mục F1 từng chỉ định nghĩa F1-06/F1-07 cho **A1 xem
chính mình** [SoT: 01-rd/req/F1_identity.md — F1-06, F1-07, đoạn mở đầu ghi rõ "Trang tiến độ cá nhân"], không có mã nào cho
A2 xem tổng hợp nhiều học viên. Khoảng trống này nay lấp bằng **F1-28** (`01-rd/req/F1_identity.md`, bổ sung
2026-08-30) và **US-A2-09** (`01-rd/req/user_stories/a2_instructor.md`) — xem Câu hỏi mở Q1.

### 2.1 Cấu trúc hệ thống liên quan

| Màn/tài liệu | Vai trò | Bounded Context |
|---|---|---|
| Màn hiện tại | `INS0203` / `class_progress` | `identity` + `judge-orchestration` |
| Tài liệu yêu cầu | `01-rd/req/F1_identity.md` (F1-28) | Nguồn SoT của màn |
| Prototype | `09-layoutBase/Giáo viên - Tiến độ học viên.dc.html` | Bằng chứng bố cục và trạng thái |

**Chi tiết trạng thái và cấu trúc màn**

Đối chiếu `09-layoutBase/Giáo viên - Tiến độ học viên.dc.html` — hành vi UX thật đã dựng, không phải suy
diễn:

1. **Trạng thái mặc định** — bố cục 3 khối xếp dọc [SoT: 09-layoutBase/Giáo viên - Tiến độ học viên.dc.html:112-190]:
   - Tiêu đề "Tiến độ học viên" kèm phụ đề "Theo dõi điểm số, hoàn thành và mức độ hoạt động" (dòng 117-118).
   - Khối trên gồm 2 cột: biểu đồ đường "Điểm trung bình theo lớp" 6 tuần gần nhất, vẽ 2 đường cho 2 lớp
     (dòng 122-134) — **ĐÃ CHỐT 2026-10-14 (DEC-2026-1014-instructor-batch2-weekly-rules):** biểu đồ chỉ vẽ
     **một đường**: tổng của mọi lớp ở tab "Tất cả", đường của lớp đó ở tab một lớp; tuần theo ISO (bắt đầu
     thứ Hai), chỉ vẽ các tuần đủ 7 ngày, tuần hiện tại chưa kết thúc không được tính; số liệu tuần tính lúc
     truy vấn, cache 15 phút, không có bảng hay job chụp số liệu tuần
     [SoT: .nexa/control/decision-registry.md:3701-3706]; và khối "Cần chú ý" liệt kê tối đa vài học viên có nhãn cảnh báo (dòng 136-150).
   - Bảng danh sách học viên: cột Học viên, Lớp, Điểm TB, Hoàn thành, Chuỗi ngày, Xu hướng (sparkline),
     Hoạt động gần nhất (dòng 168-186).
2. **Bộ lọc lớp** — tab ngang `Tất cả` / theo tên lớp cụ thể (`Thuật toán K21`, `CTDL K22`, `Luyện PV nâng
   cao`), lọc trực tiếp bảng học viên, không tải lại trang (dòng 160-163, 288-293, 296-300).
3. **Ô tìm kiếm học viên** theo tên, lọc cùng lúc với tab lớp (dòng 155-158, 295, 299). **ĐÃ CHỐT
   2026-10-14 (DEC-2026-1014-instructor-batch2-weekly-rules):** ô tìm kiếm chỉ lọc bảng học viên, không lọc
   khối "Cần chú ý" và không lọc biểu đồ [SoT: .nexa/control/decision-registry.md:3706-3707].
4. **Đếm kết quả** — hiển thị dạng "N / 82 học viên" ở góc phải thanh lọc (dòng 164, 320) — con số tổng
   "82" là dữ liệu giả lập cứng trong `data()`, không phải hành vi thật (dòng 226-234 chỉ có 8 bản ghi mẫu).
   **ĐÃ CHỐT 2026-10-14 (DEC-2026-1014-instructor-batch2-weekly-rules):** mẫu số "tổng học viên" theo tab lớp
   đang chọn; tab "Tất cả" đếm theo người (distinct) trên mọi lớp, cùng quy tắc
   `DEC-2026-1014-instructor-batch1-counting-rules` [SoT: .nexa/control/decision-registry.md:3707-3708].
   **ĐÃ CHỐT 2026-10-14 (DEC-2026-1014-class-leave-details):** mẫu số, bảng, điểm TB, hoàn thành và khối "Cần chú ý" chỉ tính học viên đang trong lớp; học viên đã bị gỡ (rời lớp) không được tính, học viên vào lại bằng mã mời thì được tính lại.
5. **Nhãn "Cần chú ý"** — 4 nhãn quan sát được trong dữ liệu mẫu, nay rút gọn còn 3 theo `F1-28` (chốt
   2026-08-30): "Vắng bài" (không có lượt nộp nào trong 7 ngày gần nhất), "Theo dõi" (hoàn thành dưới 50%),
   "Cần hỗ trợ" (điểm trung bình giảm liên tiếp từ hai mốc tuần trở lên) — ba ngưỡng này là giá trị mặc định
   đề xuất `[SoT: Suy luận]`, BD/DD chỉnh được. Đã đóng Câu hỏi mở Q2. **Chốt 2026-10-14
   (DEC-2026-1014-instructor-batch2-weekly-rules):** "mốc tuần" là tuần ISO bắt đầu thứ Hai, không tính tuần
   hiện tại chưa kết thúc; số liệu tuần tính lúc truy vấn, cache 15 phút (xem `F1-28`, `01-rd/req/F1_identity.md`).
   **Xác nhận 2026-10-14 (`DEC-2026-1014-instructor-batch5-6-nav-pagination-rubric`):** quy tắc tuần ISO này áp dụng
   cả cho sparkline "Xu hướng" của bảng học viên.
6. **Cột "Điểm TB"** hiển thị số thang 10 (ví dụ `8.9`) — theo `F1-28`: quy đổi từ tỉ lệ Accepted/tổng nộp
   của học viên đó (F1-07) ×10, **không** dùng điểm AI tham khảo của F5-27 (đã bỏ 2026-10-13) (điểm đó tính theo từng bài nộp,
   không phải theo học viên). Đã đóng Câu hỏi mở Q3.
7. **Cột "Hoàn thành"** hiển thị phần trăm (ví dụ `96%`) — theo `F1-28`: số bài được giao cho lớp (F2-12) mà
   học viên đã Accepted ít nhất một lần, chia cho tổng số bài được giao — không phải tỉ lệ Accepted/tổng nộp
   (đó là công thức của cột "Điểm TB" ở trên, dùng riêng). Đã đóng Câu hỏi mở Q3.
8. **Cột "Chuỗi ngày"** (streak) và cột "Xu hướng" (sparkline điểm 6 mốc gần nhất) — theo `F1-28`: "Chuỗi
   ngày" dùng chung khái niệm đã có ở F1-21 (không phải chỉ số mới); "Xu hướng" là điểm TB riêng của học viên
   đó theo 6 mốc tuần gần nhất (cùng công thức mục 6, tính theo cá nhân thay vì gộp lớp). Đã đóng Câu hỏi mở
   Q4.
9. **Điều hướng** — mục "Tiến độ học viên" trong sidebar khu Giảng viên, cạnh "Tổng quan" / "Lớp của tôi" /
   "Bài tập của tôi" / ~~"Chấm bài"~~ (mục nav đã bỏ 2026-10-13) [SoT: 09-layoutBase/Giáo viên - Tiến độ học viên.dc.html:244-250] — khớp
   layout riêng biệt khu Giảng viên đã chốt ở `01-rd/overview/system_survey.md` — mục 7.2, khối "Chốt 2026-08-24 ... Phương án B" (Phương án B, tách
   shell khỏi Admin).
10. Không thấy trạng thái rỗng (lớp chưa có học viên), trạng thái lỗi tải dữ liệu, hay phân trang cho danh
    sách học viên khi lớp đông — `data()` chỉ có 8 bản ghi mẫu trong khi đếm kết quả hiển thị "82 học viên"
    (dòng 226-234, 320). Theo `F1-28` (chốt 2026-08-30): phân trang cùng kiểu `problem_list` (F2-11); có
    trạng thái rỗng riêng và trạng thái lỗi kèm nút thử lại — chi tiết UI để BD/DD. Đã đóng Câu hỏi mở Q5.

---

## 3. Ngoài phạm vi (Out of Scope)

- Layout, vùng bố cục, bảng màu, component cụ thể — thuộc BD (`02-bd/screens/teacher/INS0203_class_progress.md`,
  chưa viết).
- Hợp đồng API (request/response lấy danh sách học viên, dữ liệu biểu đồ) — thuộc DD
  (`03-dd/api/F1_identity.md` hoặc `03-dd/api/F4_judge-orchestration.md`, chưa viết).
- Cơ chế RBAC/Function `CLASS_MANAGEMENT` chi tiết (định nghĩa Action, seed dữ liệu ma trận) — thuộc BD/DD
  của module `identity`, không thuộc file theo trục màn này.

---

## 4. Tiền đề và ràng buộc

| # | Câu hỏi | Vì sao chưa trả lời được | Đề xuất | Chủ sở hữu |
| :-: | :--- | :--- | :--- | :--- |
| Q1 | ~~`class_progress` (A2 xem tổng hợp nhiều học viên) hiện chỉ được ghi là "dẫn xuất từ F1-06, F1-07" — không có mã `Fx-nn`/`US-A2-nn` trực tiếp.~~ **ĐÃ CHỐT 2026-08-30 (owner instruction — "tạo dashboard ở mức tốt là được"):** cấp mã `F1-28` trong nhóm `CLASS_MANAGEMENT` (cạnh F1-23→27) và `US-A2-09` — xem `01-rd/req/F1_identity.md`, `01-rd/req/user_stories/a2_instructor.md`. | — | Đã chốt — xem `F1-28`. | Đã đóng |
| Q2 | ~~Nhãn "Cần chú ý" áp dụng ngưỡng nào?~~ **ĐÃ CHỐT 2026-08-30:** "Vắng bài" — không có lượt nộp nào trong 7 ngày gần nhất; "Theo dõi" — hoàn thành dưới 50%; "Cần hỗ trợ" — điểm trung bình giảm liên tiếp từ hai mốc tuần trở lên. Ba ngưỡng là giá trị mặc định đề xuất (`[SoT: Suy luận]` trong `F1_identity.md`), BD/DD chỉnh được, không phải hằng số cứng. | — | Đã chốt — xem `F1-28`. | Đã đóng |
| Q3 | ~~Cột "Điểm TB" và cột "Hoàn thành" tính từ công thức nào?~~ **ĐÃ CHỐT 2026-08-30:** Điểm TB = tỉ lệ Accepted/tổng nộp của học viên (F1-07) ×10 — không dùng điểm AI F5-27. Hoàn thành = số bài được giao (F2-12) đã Accepted ít nhất một lần, chia tổng số bài được giao. Hai cột dùng hai nguồn khác nhau, không trùng nhau. | — | Đã chốt — xem `F1-28`. | Đã đóng |
| Q4 | ~~"Chuỗi ngày" và "Xu hướng" không khớp mã nào — chỉ số mới hay trang trí prototype?~~ **ĐÃ CHỐT 2026-08-30:** giữ lại cả hai. "Chuỗi ngày" dùng chung khái niệm đã có ở F1-21 (không phải chỉ số mới). "Xu hướng" = điểm TB cá nhân theo 6 mốc tuần gần nhất (cùng công thức Q3, tính theo cá nhân). | — | Đã chốt — xem `F1-28`. | Đã đóng |
| Q5 | ~~Danh sách học viên khi lớp đông hiển thị thế nào — phân trang, cuộn, hay tải hết? Trạng thái rỗng/lỗi?~~ **ĐÃ CHỐT 2026-08-30:** phân trang cùng kiểu `problem_list` (F2-11); có trạng thái rỗng riêng (lớp chưa có học viên) và trạng thái lỗi kèm nút thử lại. Chi tiết UI để BD/DD. | — | Đã chốt — xem `F1-28`. | Đã đóng |
| Q6 | ~~Khối "Cần chú ý" có lọc theo tab lớp đang chọn hay luôn hiển thị toàn bộ lớp?~~ **ĐÃ CHỐT 2026-08-30:** phải lọc theo đúng tab lớp đang chọn, giống bảng học viên — hành vi hiện tại của prototype (không lọc) là thiếu sót cần sửa khi dựng UI thật, không phải chủ ý thiết kế. | — | Đã chốt — xem `F1-28`. | Đã đóng |
| 203-Q3, Q5, Q7, Q9 | Số liệu tuần và nhãn trạng thái tính thế nào (Q3); biểu đồ vẽ mấy đường (Q5); ô tìm kiếm lọc những khối nào (Q7); mẫu số "N / tổng học viên" (Q9). **ĐÃ CHỐT 2026-10-14 (DEC-2026-1014-instructor-batch2-weekly-rules):** tính lúc truy vấn, cache 15 phút, tuần ISO bắt đầu thứ Hai, bỏ tuần dở dang; một đường (tổng lớp ở "Tất cả", đường lớp ở tab lớp); tìm kiếm chỉ lọc bảng; mẫu số theo tab lớp, "Tất cả" đếm theo người. | — | Số thứ tự là của BD `02-bd/screens/teacher/INS0203_class_progress.md`; RD file này có Q3, Q5 nhưng nội dung khác (đã đóng 2026-08-30), nên không đánh đồng. Hành vi ghi ở mục 2.1 điểm 1, 3, 4, 5. | Đã đóng |

---

## 5. Danh sách yêu cầu (REQ)

| ID | Yêu cầu | Loại | Nguồn/SoT |
|---|---|---|---|
| REQ-01 | Bài đã giải theo chủ đề (định nghĩa gốc, viết cho A1 xem tiến độ cá nhân) (F1-06) | Chức năng | `01-rd/req/F1_identity.md` — F1-06 |
| REQ-02 | Tỉ lệ chấp thuận — Accepted / tổng bài nộp (định nghĩa gốc, viết cho A1 xem tiến độ cá nhân) (F1-07) | Chức năng | `01-rd/req/F1_identity.md` — F1-07 |
| REQ-03 | Tổng hợp tiến độ nhiều học viên trong lớp phụ trách (điểm TB, hoàn thành, chuỗi ngày, xu hướng, khối "Cần chú ý") (F1-28) | Chức năng | `01-rd/req/F1_identity.md` — F1-28 (bổ sung 2026-08-30) |
| REQ-04 | ~~Bảng slug mục 7.2 ghi `class_progress` là "dẫn xuất từ F1-06, F1-07"~~ — đã thay bằng mã trực tiếp F1-28; `system_survey.md` cần cập nhật dòng này khi đợt rà soát tiếp theo chạm tới mục 7.2 ((lịch sử)) | Chức năng | `01-rd/overview/system_survey.md` — mục 7.2 dòng `class_progress` |
| REQ-05 | Cơ chế xác định "lớp giảng viên phụ trách" — giao bài theo lớp, chỉ sinh viên lớp đó thấy bài (F2-12) | Chức năng | `01-rd/req/user_stories/a2_instructor.md` (`US-A2-03`) |
| REQ-06 | Cùng cơ chế lớp giảng viên phụ trách áp dụng cho phạm vi hiển thị màn chấm bài (`instructor_grading`) — dẫn chiếu để dùng chung cơ chế, không phát minh riêng cho `class_progress` (F5-27) **Đã bỏ 2026-10-13** | Chức năng | `01-rd/req/F5_ai-review.md` — F5-27 (mục đã bỏ) |
| REQ-07 | Gác quyền mở màn bằng Function `CLASS_MANAGEMENT` trong ma trận phân quyền (F1-10, F1-12) | Chức năng | `01-rd/req/F1_identity.md` — F1-10, F1-12 |
| REQ-08 | Given-When-Then của A2 xem lớp/bài tập, không có mục nào cho "xem tiến độ tổng hợp" (—) | Chức năng | `01-rd/req/user_stories/a2_instructor.md` (`US-A2-03` tới `US-A2-05`, `US-A2-06` đã bỏ 2026-10-13 — không có US riêng cho `class_progress`, xem Câu hỏi mở Q1) |

---

## 6. Yêu cầu dữ liệu

### 6.1 Dữ liệu tham chiếu

Chi tiết bảng, cột, kiểu dữ liệu và ràng buộc lưu trữ thuộc BD/DD; tài liệu này không tự suy luận dữ liệu chưa được chốt.

### 6.2 Luồng dữ liệu

Hợp đồng API, request/response và mã lỗi thuộc DD. Luồng dữ liệu mức yêu cầu được thể hiện bởi các hành vi ở chương 5.

---

## 7. RACI và danh sách bàn giao

| Bàn giao | Trạng thái |
|---|---|
| RD màn hình | Có tài liệu này |
| BD màn hình | Chưa xác định — không tự suy luận |
| DD/API liên quan | Chưa xác định — không tự suy luận |

---

## 8. Kế hoạch và quy mô

| Chỉ số | Giá trị |
|---|---|
| Quy mô hành vi | Xem các yêu cầu tại chương 5. |
| Lịch, nhân sự, công sức | Chưa xác định — không tự suy luận. |

---

## 9. Tham chiếu

| Loại | Tham chiếu |
|---|---|
| Khảo sát hệ thống | `01-rd/overview/system_survey.md` — mục 7.2 dòng `class_progress`. |
| Khảo sát hệ thống | `01-rd/overview/system_survey.md` — mục 7.2, khối "Chốt 2026-08-24 ... Phương án B". |
| Yêu cầu module | `01-rd/req/F1_identity.md` — F1-06, F1-07 (định nghĩa gốc, viết cho A1), F1-28 (chốt 2026-08-30, mã trực tiếp cho màn này), F1-12 (Function `CLASS_MANAGEMENT`). |
| Yêu cầu module | `01-rd/req/F5_ai-review.md` — F5-27 (đã bỏ 2026-10-13; cơ chế phạm vi lớp nay chỉ dựa F2-12/F1-12). |
| User story | `01-rd/req/user_stories/a2_instructor.md` — `US-A2-03` tới `US-A2-05`, `US-A2-09` (chốt 2026-08-30); `US-A2-06` đã bỏ 2026-10-13. |
| Quyết định | `DEC-2026-0830-class-progress-dashboard`. |
| Prototype | `09-layoutBase/Giáo viên - Tiến độ học viên.dc.html` — prototype đối chiếu. |
| Kế hoạch dự án | `06-plan/PROTOTYPE_DEBT.md` — mục 6.2.b, ghi nhận `class_progress` đã có prototype thật, đối chiếu tên file/slug. |
| Kế hoạch dự án | `06-plan/nexa-plan/260824-2043-bd-screens-common-first.md` — xếp `class_progress` vào nhóm màn giảng viên có thể cắt ở Stage 5 nếu thiếu thời gian. |
