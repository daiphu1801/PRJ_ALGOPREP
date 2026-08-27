# RD — Màn `class_management` (Quản lý lớp — Lớp của tôi)

> Slug: `class_management` — khớp `01-rd/overview/system_survey.md` mục 7.2 dòng `class_management`
> [SoT: 01-rd/overview/system_survey.md:547]. Mô tả: "Quản lý lớp — tổng quan và danh sách học viên",
> mã liên quan F1-10, F1-12, F1-23, F6-11 [SoT: 01-rd/overview/system_survey.md:547; F1-23 bổ sung
> 2026-08-28]. Bounded Context: `identity`
> (danh sách học viên theo lớp, phân quyền A2), `interview-bank` (F6-11 — bộ câu hỏi riêng theo lớp)
> [SoT: 01-rd/overview/system_survey.md:547]. Actor: A2 (Giáo viên).
>
> **Tách slug 2026-08-28 (`DEC-2026-0828-split-class-management-assignments`):** file này trước đó gộp
> chung hai prototype có route/nav riêng biệt — `09-layoutBase/Giáo viên - Lớp của tôi.dc.html` và
> `09-layoutBase/Giáo viên - Bài tập của tôi.dc.html` — dưới một slug `class_management`, theo đúng câu
> hỏi mở Q1 nêu ra ở bản trước của file này. Chủ dự án đã chốt tách theo đề xuất: **file này giờ chỉ mô
> tả "Lớp của tôi"**; phần "Bài tập của tôi" chuyển sang slug mới
> `01-rd/screens/teacher/class_assignments.md`. Lý do và phạm vi ảnh hưởng đầy đủ ở
> `.nexa/control/decision-registry.md` → `DEC-2026-0828-split-class-management-assignments`.
>
> File này mô tả **hành vi và UX ở mức yêu cầu** của màn "Lớp của tôi" — không lặp lại đặc tả chức năng
> chung đã có ở `01-rd/req/req.md` (F1, F6) và `01-rd/req/user_stories.md` (`US-A2-04`), chỉ trỏ tới và
> bổ sung phần đặc thù của màn: trạng thái màn, cấu trúc UI, và các câu hỏi mở phát sinh khi đối chiếu với
> prototype thật.

## 1. Mục đích màn hình

Cho giáo viên (A2) xem tổng quan các lớp mình phụ trách, danh sách học viên và tiến độ của từng lớp
[SoT: 01-rd/overview/system_survey.md:547; 01-rd/req/req.md:160]. Mọi phạm vi hiển thị và thao tác đều
giới hạn trong lớp giáo viên đó phụ trách, gác bởi Function `CLASS_MANAGEMENT` trong ma trận phân quyền
[SoT: 01-rd/req/req.md:64, 401-402].

## 2. Nguồn yêu cầu (không lặp lại — chỉ trỏ)

| Hành vi | Mã | Nguồn |
| :--- | :--- | :--- |
| Tạo lớp học mới với thông tin cơ bản; sinh mã mời; học viên tự tham gia lớp bằng mã mời | F1-23 | `01-rd/req/req.md` (bổ sung 2026-08-28) |
| Giáo viên tạo bộ câu hỏi phỏng vấn riêng từ ngân hàng và gán cho lớp | F6-11 | `01-rd/req/req.md:417` |
| Ma trận phân quyền — Function `CLASS_MANAGEMENT` gác quyền A2 theo phạm vi lớp phụ trách | F1-10, F1-12 | `01-rd/req/req.md:55-68, 401-402` |
| Given-When-Then tạo lớp và tham gia bằng mã mời | — | `01-rd/req/user_stories.md` (`US-A2-07`) |
| Given-When-Then tạo bộ câu hỏi phỏng vấn riêng cho lớp | — | `01-rd/req/user_stories.md:243-249` (`US-A2-04`) |

## 3. Trạng thái và cấu trúc màn (screen states)

### 3.1. Màn "Lớp của tôi" — `09-layoutBase/Giáo viên - Lớp của tôi.dc.html`

1. **Tổng quan lớp** — tiêu đề "Lớp của tôi" kèm số liệu tổng "3 lớp · 82 học viên"; 4 thẻ thống kê: tổng số
   lớp, hoàn thành trung bình (%, so với tuần trước), số bài cần chấm tay, số học viên vắng bài
   [SoT: 09-layoutBase/Giáo viên - Lớp của tôi.dc.html:117-121, 253-258].
2. **Nút "+ Tạo lớp mới"** — hiện ở đầu trang nhưng **không có `onClick` hay hành vi nào được lập trình**
   trong `renderVals()`/`Component` (không phải link, không mở modal, không set state) — chỉ là phần tử
   tĩnh chưa nối hành vi [SoT: 09-layoutBase/Giáo viên - Lớp của tôi.dc.html:120]. Đây là bằng chứng
   prototype chưa dựng luồng tạo lớp. **Chốt 2026-08-28 (F1-23, `US-A2-07`):** bấm nút này mở luồng tạo lớp
   với thông tin cơ bản; sau khi tạo, hệ thống trả về một mã mời để giáo viên chia sẻ cho học viên — chưa
   dựng vào prototype (`fakeAuth()`-kiểu, để lúc build FE thật). Chi tiết còn mở (sửa/xoá lớp, thu hồi mã
   mời, gỡ học viên thủ công) — xem Câu hỏi mở Q2b.
3. **Danh sách thẻ lớp** — mỗi thẻ hiển thị tên lớp, lịch học, số học viên, thanh tiến độ hoàn thành trung
   bình, điểm trung bình, số cần chấm, số vắng bài; dữ liệu mẫu 3 lớp cố định trong code
   (`classDefs`) [SoT: 09-layoutBase/Giáo viên - Lớp của tôi.dc.html:138-155, 260-264]. **Không có hành vi
   bấm vào thẻ để xem chi tiết lớp** — không có `onClick`, không có link — xem Câu hỏi mở Q3.
4. **Bảng "Danh sách học viên"** — tab lọc theo lớp (Tất cả + tên từng lớp), bảng cột: học viên, lớp, điểm
   trung bình, hoàn thành, trạng thái (Đang tốt/Cần hỗ trợ/Vắng bài — suy ra từ điểm/tỉ lệ hoàn thành trong
   dữ liệu mẫu, không có mã Fx-nn nào định nghĩa ngưỡng phân loại này), hoạt động gần nhất
   [SoT: 09-layoutBase/Giáo viên - Lớp của tôi.dc.html:160-186, 274-284]. **Không có hành vi thêm/xoá học
   viên khỏi lớp, không có nút xem hồ sơ chi tiết từng học viên** trong bảng này — xem Câu hỏi mở Q4.
5. **Không có bất kỳ UI nào cho F6-11** (tạo bộ câu hỏi phỏng vấn riêng và gán cho lớp) trong file này —
   toàn bộ nội dung trang chỉ xoay quanh lớp học và học viên, không có mục câu hỏi phỏng vấn
   [SoT: 09-layoutBase/Giáo viên - Lớp của tôi.dc.html — không có `sc-for` hay khối nào liên quan câu hỏi
   phỏng vấn trong toàn bộ file]. Xem Câu hỏi mở Q5.
6. **Không có UI cho yêu cầu chấm lại** (F4-09a/b/c) trong file này — không có nút hay bảng nào về phiên
   bản testcase hoặc chấm lại [SoT: 09-layoutBase/Giáo viên - Lớp của tôi.dc.html — toàn bộ file]. Xem Câu
   hỏi mở Q6.

## 4. Given-When-Then bổ sung ở mức màn (không trùng `US-A2-04`)

`US-A2-04` đã có Given-When-Then cho tạo bộ câu hỏi riêng [SoT: 01-rd/req/user_stories.md:243-249]. Mục
dưới đây là hành vi **riêng của màn hình**, phát hiện khi đối chiếu prototype:

- **Cho** tôi là giáo viên đang ở màn "Lớp của tôi", **Khi** tôi lọc bảng học viên theo tab một lớp cụ thể,
  **Thì** bảng chỉ còn hiển thị học viên thuộc lớp đó, không tải lại trang
  [SoT: 09-layoutBase/Giáo viên - Lớp của tôi.dc.html:163-166, 284].

## 5. Câu hỏi mở

| # | Câu hỏi | Vì sao chưa trả lời được | Đề xuất | Chủ sở hữu |
| :-: | :--- | :--- | :--- | :--- |
| Q1 | ~~`system_survey.md` gộp một slug `class_management` cho hai file prototype có route/nav riêng biệt...~~ **ĐÃ CHỐT (2026-08-28):** tách thành hai slug — `class_management` (file này, "Lớp của tôi") và `class_assignments` (`01-rd/screens/teacher/class_assignments.md`, "Bài tập của tôi") — theo đúng đề xuất ban đầu, vì hai route Next.js khác nhau. Ghi thành `DEC-2026-0828-split-class-management-assignments`. | — | Đã chốt, đã tách file. | (đã đóng) |
| Q2 | ~~Nút "+ Tạo lớp mới" ở "Lớp của tôi" không có hành vi nào được lập trình trong prototype... Luồng tạo lớp chưa có mã `Fx-nn` nào.~~ **ĐÃ CHỐT MỘT PHẦN (2026-08-28, qua hỏi trực tiếp chủ dự án):** giáo viên (A2) tạo lớp mới với thông tin cơ bản; hệ thống sinh một **mã mời (invite code)** riêng cho lớp; học viên (A1) **tự tham gia lớp bằng cách nhập mã mời** — không phải A2/A3 thêm thủ công. Ghi thành `F1-23` (`01-rd/req/req.md`), Given-When-Then ở `US-A2-07` (`01-rd/req/user_stories.md`). **Chưa dựng vào prototype** — để lúc build FE. Phần **CRUD lớp còn lại chưa chốt** — xem Q2b. | — | Đã chốt cơ chế tạo lớp + tham gia bằng mã mời. Phần còn lại xem Q2b. | Q2 đóng một phần, phát sinh Q2b |
| Q2b | Sau khi chốt Q2 (tạo lớp + mã mời), vẫn còn: giáo viên có **sửa/xoá lớp** sau khi tạo không? Mã mời có **thu hồi/tạo lại** được không (ví dụ lỡ chia sẻ nhầm)? Giáo viên có **gỡ một học viên cụ thể** khỏi lớp (không qua mã mời) không, và gỡ rồi thì lịch sử bài nộp của học viên đó có giữ nguyên không? | Chủ dự án chỉ xác nhận cơ chế tạo lớp và tham gia bằng mã mời (Q2), chưa được hỏi về sửa/xoá lớp, vòng đời mã mời, hay gỡ học viên thủ công — đây là khoảng trống mới lộ ra sau khi Q2 được trả lời một phần, không phải suy diễn được. | Hỏi chủ dự án ba điểm: (a) sửa/xoá lớp — xoá thật hay ẩn mềm (nhất quán với cách F1-16 xử lý xoá tài khoản)? (b) mã mời có hạn dùng/số lần dùng hay dùng mãi? có nút "Tạo lại mã" không? (c) giáo viên gỡ học viên khỏi lớp — có, và có giữ nguyên lịch sử nộp bài cũ của học viên đó không (nhất quán với Câu hỏi mở Q9 ở `class_assignments.md` về gỡ bài khỏi lớp). | Chủ dự án |
| Q3 | Thẻ lớp trong "Lớp của tôi" không có hành vi bấm để xem chi tiết lớp (danh sách đầy đủ bài đã giao riêng cho lớp đó, lịch sử chấm lại...). Bảng học viên bên dưới đã lọc được theo lớp nhưng chưa rõ đây có phải là toàn bộ "chi tiết lớp" cần có hay còn thiếu (ví dụ xem theo từng buổi học, điểm danh). | `req.md` không có mã nào mô tả nội dung "trang chi tiết một lớp" cụ thể gồm những gì — chỉ có F2-12 (giao bài, `class_assignments.md`) và các mã liên quan chấm bài/tiến độ ở màn khác (`grading`, `progress`, ngoài phạm vi slug này). | Làm rõ: "Lớp của tôi" chỉ cần dạng tổng quan + bảng học viên lọc theo tab (đã đủ theo prototype), hay cần thêm trang con `/class/:id` chi tiết hơn. Nếu không cần thêm, không phải hành động gì; nếu cần, bổ sung mã yêu cầu. | Chủ dự án |
| Q4 | Bảng "Danh sách học viên" không có nút thêm/xoá học viên khỏi lớp, không có hồ sơ chi tiết từng học viên (link sang màn nào?). Ngưỡng phân loại trạng thái "Đang tốt/Cần hỗ trợ/Vắng bài" trong dữ liệu mẫu không có công thức nào trong `req.md`. | Không có mã `Fx-nn` nào định nghĩa quản lý thành viên lớp (thêm/xoá học viên) hay công thức phân loại trạng thái học viên — đây là khoảng trống nghiệp vụ, không phải chi tiết trình bày có thể tự suy ra. | Bổ sung mã yêu cầu cho: (a) cơ chế thêm/xoá học viên khỏi lớp (liên quan Q2 — ai quản lý thành viên lớp), (b) công thức phân loại trạng thái học viên nếu cần hiển thị chính thức (có thể là suy luận UI đơn giản không cần mã riêng, cần chủ dự án xác nhận mức độ chính thức). | Chủ dự án |
| Q5 | Màn "Lớp của tôi" không có bất kỳ UI nào cho F6-11 (tạo bộ câu hỏi phỏng vấn riêng và gán cho lớp), dù `system_survey.md` liệt kê F6-11 là một trong các mã liên quan của slug `class_management`. `US-A2-04` có Given-When-Then cho hành vi này nhưng không có màn cụ thể nào trong hai prototype hiện có dựng nó. | Đây là khoảng trống prototype thật: F6-11 có yêu cầu chức năng và user story rõ ràng, nhưng không có màn hình nào (kể cả `Câu hỏi phỏng vấn.dc.html` phía học viên) dựng luồng "giáo viên tạo bộ câu hỏi riêng, gán cho lớp". Không thể suy diễn UI khi không có prototype nào minh hoạ. | Hoặc (a) dựng thêm một khu vực/tab trong chính màn `class_management` cho F6-11 khi làm FE Next.js thật, hoặc (b) tách thành slug màn riêng (ví dụ `class_interview_sets`) nếu độ phức tạp UI đủ lớn (chọn câu hỏi từ ngân hàng, xem trước, gán lớp). Cần chủ dự án chọn hướng trước khi viết BD. | Chủ dự án |
| Q6 | Không có UI nào cho yêu cầu chấm lại theo lớp (F4-09a/b/c, đã có Given-When-Then ở `US-A2-03`, nay ở `class_assignments.md`) trong cả hai prototype của slug này. | Tương tự Q5 — có yêu cầu chức năng và user story, nhưng không có màn nào minh hoạ giao diện (chọn phiên bản testcase, xem ước lượng ảnh hưởng, nút xác nhận chạy). | Bổ sung khu vực "Yêu cầu chấm lại" vào màn "Bài tập của tôi" (`class_assignments.md`, gắn theo từng bài toán, hợp lý hơn gắn theo lớp vì testcase thuộc về bài toán) khi làm FE Next.js thật — cần chủ dự án xác nhận đặt ở đây hay ở một màn khác (ví dụ trang chi tiết bài toán phía Admin/Giáo viên, `problem-bank`). | Chủ dự án |

## 6. Ngoài phạm vi file này

- Layout, spacing, bảng màu, component cụ thể (glass-morphism, breakpoint responsive) — thuộc BD
  (`02-bd/screens/teacher/class_management.md`, chưa viết).
- Hợp đồng API (danh sách lớp, danh sách học viên theo lớp, tạo bộ câu hỏi theo lớp) — thuộc DD
  (`03-dd/api/identity.md`, `03-dd/api/interview-bank.md`, chưa viết).
- Màn "Bài tập của tôi" (giao/gán bài từ ngân hàng cho lớp) — tách thành slug riêng
  `01-rd/screens/teacher/class_assignments.md` (`DEC-2026-0828-split-class-management-assignments`).
- Màn "Chấm bài" (`Giáo viên - Chấm bài.dc.html`, F5-27, `US-A2-06`) và "Tiến độ học viên"
  (`Giáo viên - Tiến độ học viên.dc.html`) — là các slug màn khác theo mục 7.2 của `system_survey.md`,
  không thuộc phạm vi file này dù cùng actor A2 và có liên hệ số liệu (ví dụ thẻ "Cần chấm tay" xuất hiện ở
  cả màn này lẫn ở màn Chấm bài).
- Thuật toán tính tỉ lệ hoàn thành, điểm trung bình, phân loại trạng thái học viên — thuộc logic của
  `identity`, không thuộc file theo trục màn này (xem Câu hỏi mở Q4 về việc có cần mã yêu cầu chính thức
  cho công thức phân loại hay không).
- Luồng CRUD lớp học: **tạo lớp + tham gia bằng mã mời đã có mã yêu cầu** (F1-23, 2026-08-28); **sửa/xoá
  lớp, thu hồi mã mời, gỡ học viên thủ công vẫn chưa có mã yêu cầu** trong `req.md` — xem Câu hỏi mở Q2b và
  Q4; không tự suy diễn hành vi trong file này.

## 7. Tham chiếu

- `01-rd/overview/system_survey.md:547` — dòng `class_management` trong bảng màn mục 7.2.
- `01-rd/req/req.md:55-68, 417` — F1-10 tới F1-12, F6-11; F1-23 (tạo lớp + mã mời, bổ sung 2026-08-28).
- `01-rd/req/user_stories.md:243-249` — `US-A2-04`; `US-A2-07` (tạo lớp + mã mời).
- `01-rd/screens/teacher/class_assignments.md` — slug chị em, tách ra 2026-08-28, mô tả "Bài tập của tôi".
- `09-layoutBase/Giáo viên - Lớp của tôi.dc.html` — prototype.
- `.nexa/control/decision-registry.md` → `DEC-2026-0828-split-class-management-assignments`.
- `.nexa/domain-registry.json` — định nghĩa Bounded Context và actor.
