# RD — Màn `class_assignments` (Giao bài tập theo lớp — Bài tập của tôi)

> Slug: `class_assignments` — slug mới, tách khỏi `class_management` ngày 2026-08-28
> (`DEC-2026-0828-split-class-management-assignments`). Mô tả: "Giao bài tập theo lớp", mã liên quan F2-12.
> Bounded Context: `problem-bank` (gán bài toán cho lớp). Actor: A2 (Giáo viên). (F4-09a tới F4-09c và
> Bounded Context `judge-orchestration` từng gắn với slug này đã loại khỏi phạm vi 2026-08-28 —
> `DEC-2026-0828-remove-rejudge-scope`.)
>
> **Nguồn gốc:** trước 2026-08-28, nội dung file này nằm chung với `class_management` trong một file RD vì
> `01-rd/overview/system_survey.md` mục 7.2 gộp hai prototype có route/nav riêng biệt vào một slug hạt
> giống. Chủ dự án chốt tách theo đề xuất ở Câu hỏi mở Q1 (bản cũ của `class_management.md`): mỗi màn có
> route và component riêng ở FE Next.js nên mỗi màn một slug BD/DD. Lý do và phạm vi ảnh hưởng đầy đủ ở
> `.nexa/control/decision-registry.md` → `DEC-2026-0828-split-class-management-assignments`.
>
> Quan hệ thật giữa hai màn, đọc trực tiếp từ code: đây là **hai mục điều hướng (nav item) độc lập trong
> cùng sidebar giáo viên**, không phải hai tab của một trang — mỗi file prototype có route riêng
> (`./Giáo viên - Lớp của tôi.dc.html`, `./Giáo viên - Bài tập của tôi.dc.html`) và mục nav riêng biệt
> ("Lớp của tôi" mã `LH`, "Bài tập của tôi" mã `BT`) [SoT: 09-layoutBase/Giáo viên - Lớp của tôi.dc.html:221-222].
>
> File này mô tả **hành vi và UX ở mức yêu cầu** của màn "Bài tập của tôi" — không lặp lại đặc tả chức năng
> chung đã có ở `01-rd/req/problem-bank.md` (F2) và `01-rd/req/user_stories/a2_instructor.md` (`US-A2-03`), chỉ trỏ tới và bổ
> sung phần đặc thù của màn: trạng thái màn, cấu trúc UI, và các câu hỏi mở phát sinh khi đối chiếu với
> prototype thật.

## 1. Mục đích màn hình

Cho giáo viên (A2) quản lý danh sách bài toán đã gán cho lớp mình phụ trách, gán thêm bài từ ngân hàng bài
toán chung [SoT: 01-rd/overview/system_survey.md:547 (trước tách); 01-rd/req/problem-bank.md — F2-12]. Mọi phạm vi hiển
thị và thao tác đều giới hạn trong lớp giáo viên đó phụ trách, gác bởi Function `CLASS_MANAGEMENT` trong ma
trận phân quyền [SoT: 01-rd/req/identity.md — F1-12; 01-rd/req/ai-review.md — F5-27]. (Yêu cầu chấm lại theo phạm vi lớp đã loại khỏi phạm
vi 2026-08-28 — `DEC-2026-0828-remove-rejudge-scope`.)

## 2. Nguồn yêu cầu (không lặp lại — chỉ trỏ)

| Hành vi | Mã | Nguồn |
| :--- | :--- | :--- |
| Giao bài toán đã công bố cho một lớp; chỉ sinh viên lớp đó thấy bài được giao | F2-12 | `01-rd/req/problem-bank.md` — F2-12 |
| Ma trận phân quyền — Function `CLASS_MANAGEMENT` gác quyền A2 theo phạm vi lớp phụ trách | F1-10, F1-12 | `01-rd/req/identity.md` — F1-10, F1-12 |
| Given-When-Then giao bài theo lớp | — | `01-rd/req/user_stories/a2_instructor.md` (`US-A2-03`) |

**Đã loại khỏi phạm vi 2026-08-28:** yêu cầu chấm lại (F4-09a→c) — xem
`DEC-2026-0828-remove-rejudge-scope`.

## 3. Trạng thái và cấu trúc màn (screen states)

### 3.1. Màn "Bài tập của tôi" — `09-layoutBase/Giáo viên - Bài tập của tôi.dc.html`

1. **Tiêu đề và tổng quan** — "Bài tập của tôi", phụ đề "18 bài đã gán cho lớp · từ ngân hàng bài toán
   chung" [SoT: 09-layoutBase/Giáo viên - Bài tập của tôi.dc.html:117-118] — xác nhận đúng tinh thần F2-12:
   bài toán được **gán từ** ngân hàng chung (`problem_list`/`problem_bank`), không tạo bài riêng ở đây.
2. **Nút "+ Gán từ ngân hàng bài toán"** — là một **thẻ `<a href>` điều hướng thẳng sang
   `./Ngân hàng bài toán.dc.html`**, không mở modal chọn lớp/bài ngay tại chỗ, không có tham số nào được
   truyền đi kèm [SoT: 09-layoutBase/Giáo viên - Bài tập của tôi.dc.html:120]. Nghĩa là luồng gán bài theo
   prototype hiện tại là: giáo viên rời màn này, sang màn ngân hàng bài toán, rồi (không rõ bằng cách nào,
   `problem_list.dc.html` cho học viên không có hành vi "gán cho lớp" nào) hoàn tất việc gán — luồng này
   chưa được dựng đầy đủ ở prototype. Xem Câu hỏi mở Q7.
3. **4 thẻ thống kê** — bài đã gán, lượt nộp tuần này (kèm delta), tỉ lệ AC trung bình 90 ngày (kèm delta),
   số cần chấm tay (AI chấm điểm thấp) [SoT: 09-layoutBase/Giáo viên - Bài tập của tôi.dc.html:259-264].
4. **Bộ lọc** — ô tìm theo tên bài, tab lọc theo lớp (Tất cả + 3 lớp), đếm số kết quả
   [SoT: 09-layoutBase/Giáo viên - Bài tập của tôi.dc.html:139-148, 266-271].
5. **Bảng danh sách bài đã gán** — cột: bài tập, chủ đề, độ khó (badge màu theo mức), **"Gán cho lớp"**
   hiển thị dạng chuỗi tĩnh nối tên các lớp được gán (ví dụ "Thuật toán K21, CTDL K22") — không phải danh
   sách có thể bấm vào từng lớp, lượt nộp, tỉ lệ AC, và nút "Chi tiết"
   [SoT: 09-layoutBase/Giáo viên - Bài tập của tôi.dc.html:152-167, 286-292].
6. **Sai liên kết trong nút "Chi tiết"** — nút "Chi tiết" của mỗi dòng bài toán trỏ tới
   `./Câu hỏi phỏng vấn.dc.html` (màn ngân hàng câu hỏi phỏng vấn, F6), không phải màn chi tiết bài toán
   (`problem_detail`) — đây là lỗi copy-paste rõ ràng ở prototype, không phải một liên kết nghiệp vụ thật
   [SoT: 09-layoutBase/Giáo viên - Bài tập của tôi.dc.html:165]. Không sửa prototype (chỉ tham khảo), nhưng
   ghi nhận ở đây để không mang lỗi này sang BD/DD — xem Câu hỏi mở Q8.
7. **Không có nút gỡ/thu hồi bài đã gán khỏi lớp** trong bảng — chỉ có "Chi tiết"
   [SoT: 09-layoutBase/Giáo viên - Bài tập của tôi.dc.html:164-166]. Xem Câu hỏi mở Q9.
8. **Không có UI cho chấm lại — và sẽ không bao giờ có:** tính năng chấm lại đã loại khỏi phạm vi 2026-08-28
   (`DEC-2026-0828-remove-rejudge-scope`).

### 3.2. Đối chiếu hai chiều với `problem_list` (học viên)

`01-rd/screens/users/problem_list.md` mục 3.4 mô tả khu "Bài tập lớp" ở phía học viên là **một khối tổng
hợp riêng cạnh bảng chính**, không lồng vào từng dòng bài toán
[SoT: 01-rd/screens/users/problem_list.md:44-48]. Ở phía giáo viên, cột "Gán cho lớp" trong bảng
`Bài tập của tôi.dc.html` liệt kê tên lớp dưới dạng text tĩnh trong từng dòng bài toán
[SoT: 09-layoutBase/Giáo viên - Bài tập của tôi.dc.html:161]. Hai cách trình bày này **không mâu thuẫn**
nhau về nghiệp vụ (cùng phản ánh quan hệ N-N bài toán ↔ lớp của F2-12) vì mỗi bên nhìn từ góc riêng
(học viên nhìn theo lớp mình học; giáo viên nhìn theo bài toán mình quản lý), nhưng **chưa có màn nào cho
giáo viên xem/gỡ chi tiết theo từng lớp** như phía học viên xem theo lớp — liên quan Câu hỏi mở Q9.

## 4. Given-When-Then bổ sung ở mức màn (không trùng `US-A2-03`)

`US-A2-03` đã có Given-When-Then cho giao bài [SoT: 01-rd/req/user_stories/a2_instructor.md — US-A2-03]. Các mục dưới đây là
hành vi **riêng của màn hình**, phát hiện khi đối chiếu prototype:

- **Cho** tôi đang ở màn "Bài tập của tôi", **Khi** tôi gõ từ khoá tìm kiếm hoặc chọn tab lớp, **Thì** bảng
  lọc đồng thời theo cả hai điều kiện (tên bài chứa từ khoá **và** thuộc lớp đang chọn), số kết quả cập
  nhật ngay [SoT: 09-layoutBase/Giáo viên - Bài tập của tôi.dc.html:279-284, 301].
- **Cho** một bài toán được gán cho nhiều hơn một lớp của cùng giáo viên, **Khi** tôi xem cột "Gán cho lớp",
  **Thì** tên tất cả các lớp được gán hiển thị trên cùng một dòng (nối bằng dấu phẩy), phản ánh đúng quan
  hệ N-N bài toán ↔ lớp của F2-12 [SoT: 09-layoutBase/Giáo viên - Bài tập của tôi.dc.html:197 (dòng dữ liệu
  mẫu "Two Sum" gán cho "Thuật toán K21, CTDL K22")].

## 5. Câu hỏi mở

| # | Câu hỏi | Vì sao chưa trả lời được | Đề xuất | Chủ sở hữu |
| :-: | :--- | :--- | :--- | :--- |
| Q7 | ~~Nút "+ Gán từ ngân hàng bài toán" chỉ điều hướng sang `Ngân hàng bài toán.dc.html`...~~ **ĐÃ CHỐT 2026-08-31 (owner instruction, theo đúng đề xuất):** nút "Giao cho lớp này" + modal chọn lớp ngay tại `problem_list` (góc nhìn giáo viên), không điều hướng rời trang. | — | Xem `DEC-2026-0831-class-assignments-round2`. Đụng tới cách `problem_list` hiển thị khác nhau theo vai trò — ghi nhận khi viết BD/DD cho cả hai màn. | Đã đóng |
| Q8 | ~~Nút "Chi tiết"...trỏ nhầm sang `Câu hỏi phỏng vấn.dc.html`...~~ **Xác nhận: lỗi kỹ thuật prototype (copy-paste), không phải liên kết nghiệp vụ.** | — | Không sửa prototype. Khi viết BD/DD, nút "Chi tiết" của giáo viên trên mỗi bài toán trỏ tới `problem_detail` (góc nhìn giáo viên) hoặc view xem-trước, không phải `interview-bank`. | Đã đóng |
| Q9 | ~~Không có nút gỡ/thu hồi bài đã gán khỏi lớp...~~ **ĐÃ CHỐT 2026-08-31 (owner instruction, theo đúng đề xuất):** bổ sung hành động gỡ, **giữ nguyên lịch sử nộp bài cũ** — chỉ ẩn khỏi danh sách được giao từ thời điểm gỡ. | — | Đã ghi vào `01-rd/req/problem-bank.md` (amendment F2-12), khác cách F1-26 xoá cascade khi gỡ học viên. Xem `DEC-2026-0831-class-assignments-round2`. | Đã đóng |

## 6. Ngoài phạm vi file này

- Layout, spacing, bảng màu, component cụ thể (glass-morphism, breakpoint responsive) — thuộc BD
  (`02-bd/screens/teacher/class_assignments.md`, chưa viết).
- Hợp đồng API (danh sách bài đã gán, gán/gỡ bài) — thuộc DD (`03-dd/api/problem-bank.md`, chưa viết).
- Màn "Lớp của tôi" (tổng quan lớp, danh sách học viên) — slug chị em
  `01-rd/screens/teacher/class_management.md` (`DEC-2026-0828-split-class-management-assignments`).
- Màn "Chấm bài" (`Giáo viên - Chấm bài.dc.html`, F5-27, `US-A2-06`) và "Tiến độ học viên"
  (`Giáo viên - Tiến độ học viên.dc.html`) — là các slug màn khác theo mục 7.2 của `system_survey.md`,
  không thuộc phạm vi file này dù cùng actor A2 và có liên hệ số liệu.
- Luồng CRUD lớp học (tạo/sửa/xoá lớp, thêm/xoá học viên) — thuộc `class_management.md` Câu hỏi mở Q2/Q4,
  không thuộc file này.

## 7. Tham chiếu

- `01-rd/overview/system_survey.md:547` — dòng `class_management` (trước tách) trong bảng màn mục 7.2; cần
  cập nhật thành hai dòng theo `DEC-2026-0828-split-class-management-assignments`.
- `01-rd/req/identity.md` — F1-10 tới F1-12. `01-rd/req/problem-bank.md` — F2-12. `01-rd/req/ai-review.md` — F5-27.
- `01-rd/req/user_stories/a2_instructor.md` — `US-A2-03`.
- `01-rd/screens/users/problem_list.md:44-48` — mô tả khu "Bài tập lớp" phía học viên, đối chiếu hai chiều
  mục 3.2 của file này.
- `01-rd/screens/teacher/class_management.md` — slug chị em, tách ra 2026-08-28, mô tả "Lớp của tôi".
- `09-layoutBase/Giáo viên - Bài tập của tôi.dc.html` — prototype.
- `.nexa/control/decision-registry.md` → `DEC-2026-0828-split-class-management-assignments`,
  `DEC-2026-0828-remove-rejudge-scope` (F4-09a→c loại khỏi phạm vi).
- `.nexa/domain-registry.json` — định nghĩa Bounded Context và actor.
