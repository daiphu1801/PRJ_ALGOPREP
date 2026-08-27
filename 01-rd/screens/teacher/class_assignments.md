# RD — Màn `class_assignments` (Giao bài tập theo lớp — Bài tập của tôi)

> Slug: `class_assignments` — slug mới, tách khỏi `class_management` ngày 2026-08-28
> (`DEC-2026-0828-split-class-management-assignments`). Mô tả: "Giao bài tập theo lớp", mã liên quan F2-12,
> F4-09a tới F4-09c. Bounded Context: `problem-bank` (gán bài toán cho lớp), `judge-orchestration` (yêu
> cầu chấm lại theo phạm vi lớp). Actor: A2 (Giáo viên).
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
> chung đã có ở `01-rd/req/req.md` (F2, F4) và `01-rd/req/user_stories.md` (`US-A2-03`), chỉ trỏ tới và bổ
> sung phần đặc thù của màn: trạng thái màn, cấu trúc UI, và các câu hỏi mở phát sinh khi đối chiếu với
> prototype thật.

## 1. Mục đích màn hình

Cho giáo viên (A2) quản lý danh sách bài toán đã gán cho lớp mình phụ trách, gán thêm bài từ ngân hàng bài
toán chung, và yêu cầu chấm lại khi cần [SoT: 01-rd/overview/system_survey.md:547 (trước tách);
01-rd/req/req.md:160]. Mọi phạm vi hiển thị và thao tác đều giới hạn trong lớp giáo viên đó phụ trách, gác
bởi Function `CLASS_MANAGEMENT` trong ma trận phân quyền [SoT: 01-rd/req/req.md:64, 401-402].

## 2. Nguồn yêu cầu (không lặp lại — chỉ trỏ)

| Hành vi | Mã | Nguồn |
| :--- | :--- | :--- |
| Giao bài toán đã công bố cho một lớp; chỉ sinh viên lớp đó thấy bài được giao | F2-12 | `01-rd/req/req.md:160-166` |
| Yêu cầu chấm lại theo phạm vi lớp mình phụ trách, xem ước lượng ảnh hưởng trước khi chạy, không tự hạ điểm đã công bố | F4-09a, F4-09b, F4-09c | `01-rd/req/user_stories.md:237-241` (`US-A2-03`) |
| Ma trận phân quyền — Function `CLASS_MANAGEMENT` gác quyền A2 theo phạm vi lớp phụ trách | F1-10, F1-12 | `01-rd/req/req.md:55-68, 401-402` |
| Given-When-Then giao bài theo lớp và yêu cầu chấm lại | — | `01-rd/req/user_stories.md:230-241` (`US-A2-03`) |

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
8. **Không có UI nào cho yêu cầu chấm lại** (F4-09a/b/c) trong file này.

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

`US-A2-03` đã có Given-When-Then cho giao bài, ước lượng ảnh hưởng chấm lại, và chính sách không hạ điểm
[SoT: 01-rd/req/user_stories.md:230-241]. Các mục dưới đây là hành vi **riêng của màn hình**, phát hiện khi
đối chiếu prototype:

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
| Q7 | Nút "+ Gán từ ngân hàng bài toán" chỉ điều hướng sang `Ngân hàng bài toán.dc.html` mà không mang theo ngữ cảnh lớp nào, và màn ngân hàng bài toán phía học viên (`problem_list`) không có hành vi "gán cho lớp" nào được dựng. Luồng gán bài thực tế (chọn bài → chọn lớp → xác nhận) chưa có nơi nào minh hoạ trọn vẹn. | Khoảng trống prototype — có mã yêu cầu (F2-12) và user story (`US-A2-03` GWT đầu tiên: "Cho bài toán đã công bố, Khi tôi giao bài cho một lớp") nhưng không có UI cụ thể cho bước "chọn lớp để giao" nằm ở đâu: modal ngay tại `class_assignments`, hay một view giáo viên riêng của ngân hàng bài toán có thêm nút "Giao cho lớp" trên mỗi dòng? | Đề xuất: khi làm FE Next.js thật, thêm nút "Giao cho lớp này" ngay trên mỗi dòng bài toán khi giáo viên xem `problem_list` (view có phân biệt vai trò), mở modal chọn lớp — thay vì điều hướng rời trang như prototype hiện tại. Cần chủ dự án xác nhận vì đụng tới cách `problem_list` hiển thị khác nhau theo vai trò (hiện `01-rd/screens/users/problem_list.md` chỉ mô tả góc nhìn A1). | Chủ dự án |
| Q8 | Nút "Chi tiết" của mỗi dòng bài toán trong "Bài tập của tôi" trỏ nhầm sang `Câu hỏi phỏng vấn.dc.html` (F6) thay vì màn chi tiết bài toán (`problem_detail`, F2/F3). Đây rõ ràng là lỗi dựng prototype (copy-paste), không phải một liên kết nghiệp vụ. | Không cần chủ dự án quyết định nội dung — chỉ cần xác nhận rằng đây là lỗi kỹ thuật của prototype (không mang ý nghĩa business), để BD/DD không vô tình kế thừa liên kết sai này. | Ghi nhận là lỗi prototype, không sửa `09-layoutBase/Giáo viên - Bài tập của tôi.dc.html` (nằm ngoài phạm vi RD); khi viết BD/DD, nút "Chi tiết" của giáo viên trên mỗi bài toán nên trỏ tới `problem_detail` (góc nhìn giáo viên, nếu có) hoặc một view xem-trước bài toán, không phải `interview-bank`. | Chủ dự án (xác nhận cách hiểu, không cần quyết định nghiệp vụ mới) |
| Q9 | Không có nút gỡ/thu hồi bài đã gán khỏi lớp trong bảng "Bài tập của tôi", và không có cách xem/gỡ theo từng lớp cụ thể (chỉ xem gộp dạng text ở cột "Gán cho lớp"). Phía học viên (`problem_list`) cũng không có mã nào mô tả việc gỡ bài khỏi lớp ảnh hưởng thế nào tới học viên đang làm dở. | F2-12 chỉ mô tả hành vi giao bài, không mô tả hành vi thu hồi/gỡ bài đã giao — đây là khoảng trống, không phải chi tiết có thể suy luận an toàn (ảnh hưởng tới bài đã giao dở, lịch sử nộp bài đã có). | Bổ sung mã yêu cầu con của F2-12 (hoặc mã mới) mô tả: giáo viên có gỡ được bài đã giao không, gỡ rồi thì các lượt nộp cũ của học viên (điểm, lịch sử) có giữ nguyên không. | Chủ dự án |

## 6. Ngoài phạm vi file này

- Layout, spacing, bảng màu, component cụ thể (glass-morphism, breakpoint responsive) — thuộc BD
  (`02-bd/screens/teacher/class_assignments.md`, chưa viết).
- Hợp đồng API (danh sách bài đã gán, gán/gỡ bài, yêu cầu chấm lại) — thuộc DD (`03-dd/api/problem-bank.md`,
  `03-dd/api/judge-orchestration.md`, chưa viết).
- Màn "Lớp của tôi" (tổng quan lớp, danh sách học viên, F6-11) — slug chị em
  `01-rd/screens/teacher/class_management.md` (`DEC-2026-0828-split-class-management-assignments`).
- Màn "Chấm bài" (`Giáo viên - Chấm bài.dc.html`, F5-27, `US-A2-06`) và "Tiến độ học viên"
  (`Giáo viên - Tiến độ học viên.dc.html`) — là các slug màn khác theo mục 7.2 của `system_survey.md`,
  không thuộc phạm vi file này dù cùng actor A2 và có liên hệ số liệu.
- Luồng CRUD lớp học (tạo/sửa/xoá lớp, thêm/xoá học viên) — thuộc `class_management.md` Câu hỏi mở Q2/Q4,
  không thuộc file này.

## 7. Tham chiếu

- `01-rd/overview/system_survey.md:547` — dòng `class_management` (trước tách) trong bảng màn mục 7.2; cần
  cập nhật thành hai dòng theo `DEC-2026-0828-split-class-management-assignments`.
- `01-rd/req/req.md:55-68, 158-166, 390-402` — F1-10 tới F1-12, F2-12.
- `01-rd/req/user_stories.md:230-241` — `US-A2-03`.
- `01-rd/screens/users/problem_list.md:44-48` — mô tả khu "Bài tập lớp" phía học viên, đối chiếu hai chiều
  mục 3.2 của file này.
- `01-rd/screens/teacher/class_management.md` — slug chị em, tách ra 2026-08-28, mô tả "Lớp của tôi".
- `09-layoutBase/Giáo viên - Bài tập của tôi.dc.html` — prototype.
- `.nexa/control/decision-registry.md` → `DEC-2026-0828-split-class-management-assignments`.
- `.nexa/domain-registry.json` — định nghĩa Bounded Context và actor.
