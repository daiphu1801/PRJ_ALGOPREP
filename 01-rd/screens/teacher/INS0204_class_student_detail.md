# RD (Yêu cầu hệ thống mới) — Chi tiết học viên / `INS0204`

> Mã màn hình: `INS0204` [Nguồn: 02-bd/_rules/bd-template-9sheet.md — mục 8].
> Slug chính tắc: `class_student_detail` [SoT: 01-rd/overview/system_survey.md — mục 7.2 dòng `class_student_detail`].
> Phạm vi/Bounded Context: `identity` [SoT: 01-rd/overview/system_survey.md — mục 7.2 dòng `class_student_detail`]. Actor: A2 (Giáo viên).
> Nguồn sự thật (SoT): `01-rd/req/identity.md` (F1-27), `01-rd/req/user_stories/a2_instructor.md` (`US-A2-08`),
> `01-rd/screens/teacher/INS0201_class_management.md:95` (Câu hỏi mở Q4 nguồn gốc slug này).
> Tài liệu này có thể đọc độc lập.

---

## 1. Mục đích và bối cảnh

**Slug mới, chưa có prototype.** Thêm 2026-08-28, qua hỏi trực tiếp chủ dự án khi trả lời Câu hỏi mở Q4 của
`01-rd/screens/teacher/INS0201_class_management.md` [SoT: 01-rd/screens/teacher/INS0201_class_management.md:95;
01-rd/req/identity.md — F1-27]. Không có `09-layoutBase/*.dc.html` minh hoạ cho màn này — mọi chi tiết cấu
trúc UI dưới đây (ngoài các điểm đã chốt qua Q4) đánh dấu `[SoT: Suy luận]` hoặc `[Đợi nextjs]`, để BD/DD tự
thiết kế khi dựng UI thật, đúng tinh thần "viết vừa đủ, đúng lúc" của project (`CLAUDE.md` — RD → BD →
Prototype → DD). File này mô tả **hành vi và UX ở mức yêu cầu** — không lặp lại đặc tả chức năng chung đã
có ở `01-rd/req/identity.md` (F1) và `01-rd/req/user_stories/a2_instructor.md` (`US-A2-08`), chỉ trỏ tới và
bổ sung phần đặc thù của màn.

Cho giáo viên (A2) xem hồ sơ chi tiết của **một học viên cụ thể** trong lớp mình phụ trách — thông tin cơ
bản, tiến độ, lịch sử nộp bài — thay vì chỉ nhìn một dòng tóm tắt ở bảng danh sách học viên của
`class_management`
[SoT: 01-rd/req/identity.md — F1-27; 01-rd/req/user_stories/a2_instructor.md — US-A2-08]. Đây là một màn/route
riêng, không phải hiển thị mở rộng (expand row) ngay tại bảng — quyết định rõ ràng qua Q4 của
`class_management.md`, vì bốn nhóm dữ liệu (cơ bản, tiến độ, lịch sử nộp bài, thao tác quản lý) đủ nặng để
cần một trang riêng
[SoT: 01-rd/screens/teacher/INS0201_class_management.md:95].

---

## 2. Phạm vi

| Hành vi | Mã | Nguồn |
| :--- | :--- | :--- |
| Giáo viên xem hồ sơ chi tiết một học viên trong lớp mình phụ trách (thông tin cơ bản, tiến độ, lịch sử nộp bài) — chỉ trong phạm vi lớp phụ trách | F1-27 | `01-rd/req/identity.md` — F1-27 (bổ sung 2026-08-28) |
| Given-When-Then mở hồ sơ chi tiết từ bảng danh sách học viên; từ chối nếu không phụ trách lớp đó | — | `01-rd/req/user_stories/a2_instructor.md` (`US-A2-08`) |
| Ma trận phân quyền — Function `CLASS_MANAGEMENT` gác quyền, phạm vi theo lớp phụ trách | F1-12 | `01-rd/req/identity.md` — F1-12 |
| ~~Gỡ học viên khỏi lớp — xoá luôn lịch sử làm bài trong phạm vi lớp.~~ **ĐÃ ĐỔI 2026-10-14 (DEC-2026-1014-keep-learning-history-on-leave):** gỡ học viên khỏi lớp — giữ lịch sử làm bài, chỉ đánh dấu đã rời lớp. **Chốt 2026-09-01:** nút đặt ở cả bảng danh sách `class_management` và ở đây | F1-26 | `01-rd/req/identity.md` — F1-26 |
| Điểm tỷ lệ testcase mỗi bài, tính trên best-attempt | F4-13, F1-06 | `01-rd/req/judge-orchestration.md` — F4-13; `01-rd/req/identity.md` — F1-06 |

### 2.1 Cấu trúc hệ thống liên quan

| Màn/tài liệu | Vai trò | Bounded Context |
|---|---|---|
| Màn hiện tại | `INS0204` / `class_student_detail` | `identity` |
| Tài liệu yêu cầu | `01-rd/req/identity.md` (F1-27) | Nguồn SoT của màn |
| Prototype | Chưa có prototype (đợi Next.js) | Bằng chứng bố cục và trạng thái |

**Chi tiết trạng thái và cấu trúc màn**

Chưa có prototype để đối chiếu — cấu trúc dưới đây là đề xuất tối thiểu suy ra trực tiếp từ phạm vi dữ liệu
đã chốt ở F1-27 (thông tin cơ bản, tiến độ, lịch sử nộp bài), không thêm khối nào ngoài phạm vi đó
`[SoT: Suy luận]`:

1. **Header hồ sơ** — tên, email, ngày tham gia lớp (qua mã mời, F1-23), lớp hiện tại (link ngược lại
   `class_management`), nút "Gỡ khỏi lớp" (F1-26, chốt 2026-09-01 — cả hai nơi). `[SoT: Suy luận]`.
   **Chốt 2026-10-14 (DEC-2026-1014-instructor-batch3-4-contracts-and-removal):** gỡ xong ở màn này thì rời
   trang, quay lại màn đã gọi tới, và danh sách ở màn đó được tải lại
   [SoT: .nexa/control/decision-registry.md:3740-3741]. **Chốt 2026-10-14
   (`DEC-2026-1014-instructor-batch5-6-nav-pagination-rubric`):** ở màn này sidebar sáng mục của màn đã gọi tới
   [SoT: .nexa/control/decision-registry.md:3761-3762].
2. **Khối "Tiến độ"** — dẫn xuất từ F1-06/F1-07 nhưng thu hẹp về phạm vi một học viên: số bài đã giải theo
   chủ đề, tỉ lệ chấp thuận, điểm tốt nhất mỗi bài (F4-13, best-attempt) — cùng công thức đã dùng ở
   `class_progress` và `my_progress`, không phát sinh công thức mới
   [SoT: 01-rd/req/identity.md — F1-06, F1-07]. Trạng thái phân loại "Đang tốt/Cần hỗ trợ/Vắng bài" nêu ở
   `class_management` — **ngưỡng cụ thể để BD/DD tự đề xuất** (chốt lại 2026-09-01, giữ nguyên quyết định
   gốc) [SoT: 01-rd/screens/teacher/INS0201_class_management.md:95]. **Không hiện điểm AI tham khảo/chấm tay
   (F5-27, đã bỏ 2026-10-13)** ở khối này. ~~Liên kết "Xem chấm bài của học viên này" sang `instructor_grading`
   (chốt 2026-09-01)~~ **đã bỏ 2026-10-13** (`DEC-2026-1013-remove-instructor-manual-grading`).
3. **Khối "Lịch sử nộp bài"** — danh sách các lượt nộp của học viên này, **chỉ tính bài toán đã giao qua
   `class_assignments` (F2-12) của đúng lớp đang xem** (chốt 2026-09-01, không tính bài tự luyện ngoài
   phạm vi lớp); mỗi dòng dẫn sang `submission_result` của đúng bài nộp đó. **Chốt 2026-10-14
   (DEC-2026-1014-instructor-batch2-weekly-rules):** lượt nộp của một bài đã bị gỡ mềm khỏi lớp (xem
   `INS0202` Q9) vẫn được giữ trong lịch sử, kèm ghi chú "Bài đã gỡ khỏi lớp"
   [SoT: .nexa/control/decision-registry.md:3710-3711].
4. **Trạng thái rỗng/đang tải/lỗi** — theo cùng nguyên tắc ba trạng thái mỗi khối đã áp dụng ở
   `admin_overview`/F1-29 và `instructor_overview`/F1-30 `[SoT: Suy luận]`.
5. **Không có prototype minh hoạ** — mọi chi tiết bố cục, màu sắc, breakpoint thuộc BD, viết sau khi có
   nguyên mẫu thật hoặc khi dựng UI Next.js trực tiếp `[Đợi nextjs]`.

---

## 3. Ngoài phạm vi (Out of Scope)

- Bảng màu, spacing, breakpoint, component cụ thể — thuộc BD (`02-bd/screens/teacher/INS0204_class_student_detail.md`,
  chưa viết), viết sau khi có BD hoặc khi dựng UI Next.js trực tiếp (chưa có prototype trung gian).
- Hợp đồng API (lấy hồ sơ một học viên, lấy lịch sử nộp bài thu hẹp theo lớp) — thuộc DD
  (`03-dd/api/identity.md`, chưa viết).
- Công thức chính xác của F1-06/F1-07/F4-13 khi thu hẹp về một học viên — đã có công thức gốc ở
  `01-rd/req/identity.md`, chỉ áp dụng lại, không định nghĩa lại ở đây.

---

## 4. Tiền đề và ràng buộc

| # | Câu hỏi | Vì sao chưa trả lời được | Đề xuất | Chủ sở hữu |
| :-: | :--- | :--- | :--- | :--- |
| Q1 | ~~Nút "Gỡ học viên khỏi lớp" (F1-26) đặt ở đây, hay chỉ ở bảng danh sách của `class_management`, hay cả hai?~~ **ĐÃ CHỐT (2026-09-01, qua hỏi trực tiếp chủ dự án):** đặt ở **cả hai nơi** — bảng danh sách của `class_management` và hồ sơ chi tiết ở đây — điểm vào khác nhau nhưng cùng một hành vi F1-26, không cần mã mới. | — | Đã chốt. | Đã đóng |
| Q2 | ~~Hồ sơ chi tiết có hiện điểm AI tham khảo/chấm tay (F5-27) của học viên đó không, hay chỉ có ở `instructor_grading`?~~ **ĐÃ CHỐT (2026-09-01):** **không hiện ở đây** — chỉ thêm liên kết "Xem chấm bài của học viên này" sang `instructor_grading` (lọc theo học viên). Tránh trùng lặp UI, giữ hồ sơ chi tiết tập trung vào tiến độ/lịch sử. **Đã đóng: màn bị bỏ 2026-10-13** (liên kết và màn `instructor_grading` không còn, `DEC-2026-1013-remove-instructor-manual-grading`). | — | Đã chốt, nay vô hiệu. | Đã đóng |
| Q3 | ~~Nếu một học viên tham gia nhiều lớp, khối "Lịch sử nộp bài" ở đây chỉ tính bài nộp nào?~~ **ĐÃ CHỐT (2026-09-01):** chỉ tính bài nộp cho các bài toán đã giao qua `class_assignments` (F2-12) của đúng lớp đang xem — không tính bài tự luyện ngoài phạm vi lớp. | — | Đã chốt. | Đã đóng |
| Q4 | ~~Công thức phân loại trạng thái học viên (Đang tốt/Cần hỗ trợ/Vắng bài) — ngưỡng cụ thể là gì?~~ **ĐÃ CHỐT (2026-09-01):** giữ nguyên quyết định gốc — để BD/DD tự đề xuất ngưỡng, không phải yêu cầu chức năng cứng ở RD. | — | Đã chốt. | BD/DD |
| 204-Q9 | Lịch sử nộp có giữ lượt nộp của bài đã bị gỡ khỏi lớp không. **ĐÃ CHỐT 2026-10-14 (DEC-2026-1014-instructor-batch2-weekly-rules):** giữ, kèm ghi chú "Bài đã gỡ khỏi lớp" (khớp việc gỡ bài giữ nguyên lịch sử nộp, `INS0202` Q9). | — | Số thứ tự là của BD `02-bd/screens/teacher/INS0204_class_student_detail.md`; RD file này không có câu hỏi mang số đó trước đây. | Đã đóng |
| 204-Q10 | Sau khi gỡ học viên ở màn chi tiết thì đi đâu. **ĐÃ CHỐT 2026-10-14 (DEC-2026-1014-instructor-batch3-4-contracts-and-removal):** rời trang, quay lại màn gọi tới, danh sách tải lại. | — | Số thứ tự là của BD `02-bd/screens/teacher/INS0204_class_student_detail.md`; RD file này không có câu hỏi mang số đó trước đây. | Đã đóng |
| Q5 | **ĐÃ CHỐT 2026-10-14 (`DEC-2026-1014-instructor-overview-trim-and-nav-label`):** các lối vào trang đọc-chỉ `/instructor/submissions/{id}` giữ nguyên — nút "Xem kết quả" trong "Lịch sử nộp bài" của màn này và mục nav "Bài nộp"; không thêm lối vào mới (không thêm từ `INS0203`, `INS0201`). Câu hỏi nguyên văn bên dưới giữ làm lịch sử. [SoT: .nexa/control/decision-registry.md:3597-3598] ~~**CÂU HỎI MỞ (2026-10-13):** lối vào trang đọc-chỉ bài nộp của học viên cho giảng viên, route `/instructor/submissions/{id}` (`DEC-2026-1013-instructor-avatar-menu-and-read-source`). Trước đây lối vào là popup của màn chấm bài `INS0301`; màn đó đã bỏ nên hiện **chưa còn lối vào nào** [SoT: .nexa/control/decision-registry.md:3541-3545]. Đề xuất `[SoT: Suy luận]`: mỗi dòng của khối "Lịch sử nộp bài" ở đây dẫn tới trang đọc-chỉ đó (thay vì `submission_result` của người học, vốn dành cho chủ bài nộp). Ý này chưa được chủ dự án duyệt; các lối khác (từ `INS0203`, từ `INS0201`) cũng chưa xét. | Tránh làm lộ lối vào ngoài phạm vi lớp: mọi lối vào phải qua kiểm tra `CLASS_MANAGEMENT` theo lớp. | Chủ dự án quyết lối vào; ưu tiên Cao vì không có lối vào thì quyền đọc mã nguồn không dùng được.~~ | Đã đóng |

---

## 5. Danh sách yêu cầu (REQ)

| ID | Yêu cầu | Loại | Nguồn/SoT |
|---|---|---|---|
| REQ-01 | Giáo viên xem hồ sơ chi tiết một học viên trong lớp mình phụ trách (thông tin cơ bản, tiến độ, lịch sử nộp bài) — chỉ trong phạm vi lớp phụ trách (F1-27) | Chức năng | `01-rd/req/identity.md` — F1-27 (bổ sung 2026-08-28) |
| REQ-02 | Given-When-Then mở hồ sơ chi tiết từ bảng danh sách học viên; từ chối nếu không phụ trách lớp đó (—) | Chức năng | `01-rd/req/user_stories/a2_instructor.md` (`US-A2-08`) |
| REQ-03 | Ma trận phân quyền — Function `CLASS_MANAGEMENT` gác quyền, phạm vi theo lớp phụ trách (F1-12) | Chức năng | `01-rd/req/identity.md` — F1-12 |
| REQ-04 | ~~Gỡ học viên khỏi lớp — xoá luôn lịch sử làm bài trong phạm vi lớp.~~ **ĐÃ ĐỔI 2026-10-14 (DEC-2026-1014-keep-learning-history-on-leave):** gỡ học viên khỏi lớp — giữ lịch sử làm bài, chỉ đánh dấu đã rời lớp. **Chốt 2026-09-01:** nút đặt ở cả bảng danh sách `class_management` và ở đây (F1-26) | Chức năng | `01-rd/req/identity.md` — F1-26 |
| REQ-05 | Điểm tỷ lệ testcase mỗi bài, tính trên best-attempt (F4-13, F1-06) | Chức năng | `01-rd/req/judge-orchestration.md` — F4-13; `01-rd/req/identity.md` — F1-06 |

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
| Yêu cầu module | `01-rd/req/identity.md` — F1-06, F1-07, F1-10, F1-12, F1-26, F1-27. |
| Yêu cầu module | `01-rd/req/judge-orchestration.md` — F4-13. |
| User story | `01-rd/req/user_stories/a2_instructor.md` — `US-A2-08`. |
| Khảo sát hệ thống | `01-rd/overview/system_survey.md` — mục 7.2 dòng `class_student_detail`. |
| Màn hình liên quan | `01-rd/screens/teacher/INS0201_class_management.md` — màn nguồn, Câu hỏi mở Q4 (đã đóng, sinh ra slug này). |
| Quyết định | Quyết định gốc 2026-08-28 lấp Q4 của `class_management.md` (ghi trong `01-rd/req/identity.md` F1-27, không có `DEC-` riêng cho chính slug này). |
