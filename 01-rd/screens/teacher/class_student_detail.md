# RD — Màn `class_student_detail` (Hồ sơ chi tiết học viên)

> Slug: `class_student_detail` — khớp `01-rd/overview/system_survey.md` mục 7.2 dòng `class_student_detail`
> [SoT: 01-rd/overview/system_survey.md:576]. Mã liên quan: F1-27 [SoT: 01-rd/req/identity.md — F1-27].
> Bounded Context: `identity` [SoT: 01-rd/overview/system_survey.md:576]. Actor: A2 (Giáo viên).
>
> **Slug mới, chưa có prototype.** Thêm 2026-08-28, qua hỏi trực tiếp chủ dự án khi trả lời Câu hỏi mở Q4 của
> `01-rd/screens/teacher/class_management.md` [SoT: 01-rd/screens/teacher/class_management.md:95;
> 01-rd/req/identity.md — F1-27]. Không có `09-layoutBase/*.dc.html` minh hoạ cho màn này — mọi chi tiết cấu
> trúc UI dưới đây (ngoài các điểm đã chốt qua Q4) đánh dấu `[SoT: Suy luận]` hoặc `[Đợi nextjs]`, để BD/DD tự
> thiết kế khi dựng UI thật, đúng tinh thần "viết vừa đủ, đúng lúc" của project (`CLAUDE.md` — RD → BD →
> Prototype → DD).
>
> File này mô tả **hành vi và UX ở mức yêu cầu** — không lặp lại đặc tả chức năng chung đã có ở
> `01-rd/req/identity.md` (F1) và `01-rd/req/user_stories/a2_instructor.md` (`US-A2-08`), chỉ trỏ tới và bổ
> sung phần đặc thù của màn.

## 1. Mục đích màn hình

Cho giáo viên (A2) xem hồ sơ chi tiết của **một học viên cụ thể** trong lớp mình phụ trách — thông tin cơ
bản, tiến độ, lịch sử nộp bài — thay vì chỉ nhìn một dòng tóm tắt ở bảng danh sách học viên của
`class_management`
[SoT: 01-rd/req/identity.md — F1-27; 01-rd/req/user_stories/a2_instructor.md — US-A2-08]. Đây là một màn/route
riêng, không phải hiển thị mở rộng (expand row) ngay tại bảng — quyết định rõ ràng qua Q4 của
`class_management.md`, vì bốn nhóm dữ liệu (cơ bản, tiến độ, lịch sử nộp bài, thao tác quản lý) đủ nặng để
cần một trang riêng
[SoT: 01-rd/screens/teacher/class_management.md:95].

## 2. Nguồn yêu cầu (không lặp lại — chỉ trỏ)

| Hành vi | Mã | Nguồn |
| :--- | :--- | :--- |
| Giáo viên xem hồ sơ chi tiết một học viên trong lớp mình phụ trách (thông tin cơ bản, tiến độ, lịch sử nộp bài) — chỉ trong phạm vi lớp phụ trách | F1-27 | `01-rd/req/identity.md` — F1-27 (bổ sung 2026-08-28) |
| Given-When-Then mở hồ sơ chi tiết từ bảng danh sách học viên; từ chối nếu không phụ trách lớp đó | — | `01-rd/req/user_stories/a2_instructor.md` (`US-A2-08`) |
| Ma trận phân quyền — Function `CLASS_MANAGEMENT` gác quyền, phạm vi theo lớp phụ trách | F1-12 | `01-rd/req/identity.md` — F1-12 |
| Gỡ học viên khỏi lớp — xoá luôn lịch sử làm bài trong phạm vi lớp. **Chốt 2026-09-01:** nút đặt ở cả bảng danh sách `class_management` và ở đây | F1-26 | `01-rd/req/identity.md` — F1-26 |
| Điểm tỷ lệ testcase mỗi bài, tính trên best-attempt | F4-13, F1-06 | `01-rd/req/judge-orchestration.md` — F4-13; `01-rd/req/identity.md` — F1-06 |

## 3. Trạng thái và cấu trúc màn (screen states)

Chưa có prototype để đối chiếu — cấu trúc dưới đây là đề xuất tối thiểu suy ra trực tiếp từ phạm vi dữ liệu
đã chốt ở F1-27 (thông tin cơ bản, tiến độ, lịch sử nộp bài), không thêm khối nào ngoài phạm vi đó
`[SoT: Suy luận]`:

1. **Header hồ sơ** — tên, email, ngày tham gia lớp (qua mã mời, F1-23), lớp hiện tại (link ngược lại
   `class_management`), nút "Gỡ khỏi lớp" (F1-26, chốt 2026-09-01 — cả hai nơi). `[SoT: Suy luận]`.
2. **Khối "Tiến độ"** — dẫn xuất từ F1-06/F1-07 nhưng thu hẹp về phạm vi một học viên: số bài đã giải theo
   chủ đề, tỉ lệ chấp thuận, điểm tốt nhất mỗi bài (F4-13, best-attempt) — cùng công thức đã dùng ở
   `class_progress` và `my_progress`, không phát sinh công thức mới
   [SoT: 01-rd/req/identity.md — F1-06, F1-07]. Trạng thái phân loại "Đang tốt/Cần hỗ trợ/Vắng bài" nêu ở
   `class_management` — **ngưỡng cụ thể để BD/DD tự đề xuất** (chốt lại 2026-09-01, giữ nguyên quyết định
   gốc) [SoT: 01-rd/screens/teacher/class_management.md:95]. **Không hiện điểm AI tham khảo/chấm tay
   (F5-27)** ở khối này — chỉ một liên kết "Xem chấm bài của học viên này" sang `instructor_grading`
   (chốt 2026-09-01).
3. **Khối "Lịch sử nộp bài"** — danh sách các lượt nộp của học viên này, **chỉ tính bài toán đã giao qua
   `class_assignments` (F2-12) của đúng lớp đang xem** (chốt 2026-09-01, không tính bài tự luyện ngoài
   phạm vi lớp); mỗi dòng dẫn sang `submission_result` của đúng bài nộp đó.
4. **Trạng thái rỗng/đang tải/lỗi** — theo cùng nguyên tắc ba trạng thái mỗi khối đã áp dụng ở
   `admin_overview`/F1-29 và `instructor_overview`/F1-30 `[SoT: Suy luận]`.
5. **Không có prototype minh hoạ** — mọi chi tiết bố cục, màu sắc, breakpoint thuộc BD, viết sau khi có
   nguyên mẫu thật hoặc khi dựng UI Next.js trực tiếp `[Đợi nextjs]`.

## 4. Given-When-Then bổ sung ở mức màn (không trùng `US-A2-08`)

- **Cho** tôi đang xem hồ sơ chi tiết của một học viên trong lớp mình phụ trách, **Khi** trang tải xong,
  **Thì** tôi thấy thông tin cơ bản, khối tiến độ, và khối lịch sử nộp bài của đúng học viên đó, thu hẹp
  đúng phạm vi lớp tôi phụ trách [SoT: 01-rd/req/identity.md — F1-27].
- **Cho** học viên đó chưa nộp bài nào, **Khi** tôi xem khối "Lịch sử nộp bài", **Thì** tôi thấy trạng thái
  rỗng, không phải lỗi hay bảng trống không giải thích `[SoT: Suy luận]`.

## 5. Câu hỏi mở

| # | Câu hỏi | Vì sao chưa trả lời được | Đề xuất | Chủ sở hữu |
| :-: | :--- | :--- | :--- | :--- |
| Q1 | ~~Nút "Gỡ học viên khỏi lớp" (F1-26) đặt ở đây, hay chỉ ở bảng danh sách của `class_management`, hay cả hai?~~ **ĐÃ CHỐT (2026-09-01, qua hỏi trực tiếp chủ dự án):** đặt ở **cả hai nơi** — bảng danh sách của `class_management` và hồ sơ chi tiết ở đây — điểm vào khác nhau nhưng cùng một hành vi F1-26, không cần mã mới. | — | Đã chốt. | Đã đóng |
| Q2 | ~~Hồ sơ chi tiết có hiện điểm AI tham khảo/chấm tay (F5-27) của học viên đó không, hay chỉ có ở `instructor_grading`?~~ **ĐÃ CHỐT (2026-09-01):** **không hiện ở đây** — chỉ thêm liên kết "Xem chấm bài của học viên này" sang `instructor_grading` (lọc theo học viên). Tránh trùng lặp UI, giữ hồ sơ chi tiết tập trung vào tiến độ/lịch sử. | — | Đã chốt. | Đã đóng |
| Q3 | ~~Nếu một học viên tham gia nhiều lớp, khối "Lịch sử nộp bài" ở đây chỉ tính bài nộp nào?~~ **ĐÃ CHỐT (2026-09-01):** chỉ tính bài nộp cho các bài toán đã giao qua `class_assignments` (F2-12) của đúng lớp đang xem — không tính bài tự luyện ngoài phạm vi lớp. | — | Đã chốt. | Đã đóng |
| Q4 | ~~Công thức phân loại trạng thái học viên (Đang tốt/Cần hỗ trợ/Vắng bài) — ngưỡng cụ thể là gì?~~ **ĐÃ CHỐT (2026-09-01):** giữ nguyên quyết định gốc — để BD/DD tự đề xuất ngưỡng, không phải yêu cầu chức năng cứng ở RD. | — | Đã chốt. | BD/DD |

## 6. Ngoài phạm vi file này

- Bảng màu, spacing, breakpoint, component cụ thể — thuộc BD (`02-bd/screens/teacher/class_student_detail.md`,
  chưa viết), viết sau khi có BD hoặc khi dựng UI Next.js trực tiếp (chưa có prototype trung gian).
- Hợp đồng API (lấy hồ sơ một học viên, lấy lịch sử nộp bài thu hẹp theo lớp) — thuộc DD
  (`03-dd/api/identity.md`, chưa viết).
- Công thức chính xác của F1-06/F1-07/F4-13 khi thu hẹp về một học viên — đã có công thức gốc ở
  `01-rd/req/identity.md`, chỉ áp dụng lại, không định nghĩa lại ở đây.

## 7. Tham chiếu

- `01-rd/req/identity.md` — F1-06, F1-07, F1-10, F1-12, F1-26, F1-27.
- `01-rd/req/judge-orchestration.md` — F4-13.
- `01-rd/req/user_stories/a2_instructor.md` — `US-A2-08`.
- `01-rd/overview/system_survey.md:576` — dòng `class_student_detail` trong bảng màn mục 7.2.
- `01-rd/screens/teacher/class_management.md` — màn nguồn, Câu hỏi mở Q4 (đã đóng, sinh ra slug này).
- `.nexa/control/decision-registry.md` — quyết định gốc 2026-08-28 lấp Q4 của `class_management.md` (ghi
  trong `01-rd/req/identity.md` F1-27, không có `DEC-` riêng cho chính slug này).
