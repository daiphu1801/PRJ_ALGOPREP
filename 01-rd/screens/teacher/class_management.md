# RD — Màn `class_management` (Quản lý lớp và giao bài tập)

> Slug: `class_management` — khớp `01-rd/overview/system_survey.md` mục 7.2 dòng `class_management`
> [SoT: 01-rd/overview/system_survey.md:523]. Mô tả: "Quản lý lớp và giao bài tập", mã liên quan F2-12,
> F6-11 [SoT: 01-rd/overview/system_survey.md:523]. Bounded Context: `identity` (danh sách học viên theo
> lớp, phân quyền A2), `problem-bank` (gán bài toán cho lớp), `interview-bank` (F6-11 — bộ câu hỏi riêng
> theo lớp) [SoT: 01-rd/overview/system_survey.md:523]. Actor: A2 (Giáo viên).
>
> **Đối chiếu HAI prototype khác nhau cho cùng một slug** [SoT: 01-rd/overview/system_survey.md:523]:
> `09-layoutBase/Giáo viên - Lớp của tôi.dc.html` và `09-layoutBase/Giáo viên - Bài tập của tôi.dc.html`.
> Quan hệ thật giữa hai file, đọc trực tiếp từ code: đây là **hai mục điều hướng (nav item) độc lập trong
> cùng sidebar giáo viên**, không phải hai tab của một trang — mỗi file có route riêng
> (`./Giáo viên - Lớp của tôi.dc.html`, `./Giáo viên - Bài tập của tôi.dc.html`) và mục nav riêng biệt
> ("Lớp của tôi" mã `LH`, "Bài tập của tôi" mã `BT`) [SoT: 09-layoutBase/Giáo viên - Lớp của tôi.dc.html:221-222].
> Slug `class_management` trong khảo sát hệ thống gộp chung hai màn vì cùng nói về "quản lý lớp + giao bài",
> nhưng ở mức prototype thật đây là **hai màn riêng, điều hướng độc lập** — xem Câu hỏi mở Q1 về việc có nên
> tách slug hay giữ gộp khi viết BD/DD.
>
> File này mô tả **hành vi và UX ở mức yêu cầu** của cả hai màn — không lặp lại đặc tả chức năng chung đã có
> ở `01-rd/req/req.md` (F2, F6) và `01-rd/req/user_stories.md` (`US-A2-03`, `US-A2-04`), chỉ trỏ tới và bổ
> sung phần đặc thù của màn: trạng thái màn, cấu trúc UI, và các câu hỏi mở phát sinh khi đối chiếu với
> prototype thật.

## 1. Mục đích màn hình

Cho giáo viên (A2) hai việc trong phạm vi lớp mình phụ trách: (a) xem tổng quan các lớp, danh sách học
viên và tiến độ của từng lớp; (b) quản lý danh sách bài toán đã gán cho lớp, gán thêm bài từ ngân hàng bài
toán chung [SoT: 01-rd/overview/system_survey.md:523; 01-rd/req/req.md:160]. Mọi phạm vi hiển thị và thao
tác đều giới hạn trong lớp giáo viên đó phụ trách, gác bởi Function `CLASS_MANAGEMENT` trong ma trận phân
quyền [SoT: 01-rd/req/req.md:64, 401-402].

## 2. Nguồn yêu cầu (không lặp lại — chỉ trỏ)

| Hành vi | Mã | Nguồn |
| :--- | :--- | :--- |
| Giao bài toán đã công bố cho một lớp; chỉ sinh viên lớp đó thấy bài được giao | F2-12 | `01-rd/req/req.md:160-166` |
| Yêu cầu chấm lại theo phạm vi lớp mình phụ trách, xem ước lượng ảnh hưởng trước khi chạy, không tự hạ điểm đã công bố | F4-09a, F4-09b, F4-09c | `01-rd/req/user_stories.md:237-241` (`US-A2-03`) |
| Giáo viên tạo bộ câu hỏi phỏng vấn riêng từ ngân hàng và gán cho lớp | F6-11 | `01-rd/req/req.md:417` |
| Ma trận phân quyền — Function `CLASS_MANAGEMENT` gác quyền A2 theo phạm vi lớp phụ trách | F1-10, F1-12 | `01-rd/req/req.md:55-68, 401-402` |
| Given-When-Then giao bài theo lớp và yêu cầu chấm lại | — | `01-rd/req/user_stories.md:230-241` (`US-A2-03`) |
| Given-When-Then tạo bộ câu hỏi phỏng vấn riêng cho lớp | — | `01-rd/req/user_stories.md:243-249` (`US-A2-04`) |

## 3. Trạng thái và cấu trúc màn (screen states)

### 3.1. Màn "Lớp của tôi" — `09-layoutBase/Giáo viên - Lớp của tôi.dc.html`

1. **Tổng quan lớp** — tiêu đề "Lớp của tôi" kèm số liệu tổng "3 lớp · 82 học viên"; 4 thẻ thống kê: tổng số
   lớp, hoàn thành trung bình (%, so với tuần trước), số bài cần chấm tay, số học viên vắng bài
   [SoT: 09-layoutBase/Giáo viên - Lớp của tôi.dc.html:117-121, 253-258].
2. **Nút "+ Tạo lớp mới"** — hiện ở đầu trang nhưng **không có `onClick` hay hành vi nào được lập trình**
   trong `renderVals()`/`Component` (không phải link, không mở modal, không set state) — chỉ là phần tử
   tĩnh chưa nối hành vi [SoT: 09-layoutBase/Giáo viên - Lớp của tôi.dc.html:120]. Đây là bằng chứng
   prototype chưa dựng luồng tạo lớp — xem Câu hỏi mở Q2.
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

### 3.2. Màn "Bài tập của tôi" — `09-layoutBase/Giáo viên - Bài tập của tôi.dc.html`

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
8. **Không có UI nào cho yêu cầu chấm lại** (F4-09a/b/c) trong file này, tương tự mục 3.1.6.

### 3.3. Đối chiếu hai chiều với `problem_list` (học viên)

`01-rd/screens/users/problem_list.md` mục 3.4 mô tả khu "Bài tập lớp" ở phía học viên là **một khối tổng
hợp riêng cạnh bảng chính**, không lồng vào từng dòng bài toán
[SoT: 01-rd/screens/users/problem_list.md:44-48]. Ở phía giáo viên, cột "Gán cho lớp" trong bảng
`Bài tập của tôi.dc.html` liệt kê tên lớp dưới dạng text tĩnh trong từng dòng bài toán
[SoT: 09-layoutBase/Giáo viên - Bài tập của tôi.dc.html:161]. Hai cách trình bày này **không mâu thuẫn**
nhau về nghiệp vụ (cùng phản ánh quan hệ N-N bài toán ↔ lớp của F2-12) vì mỗi bên nhìn từ góc riêng
(học viên nhìn theo lớp mình học; giáo viên nhìn theo bài toán mình quản lý), nhưng **chưa có màn nào cho
giáo viên xem/gỡ chi tiết theo từng lớp** như phía học viên xem theo lớp — liên quan Câu hỏi mở Q9.

## 4. Given-When-Then bổ sung ở mức màn (không trùng `US-A2-03`, `US-A2-04`)

`US-A2-03` đã có Given-When-Then cho giao bài, ước lượng ảnh hưởng chấm lại, và chính sách không hạ điểm
[SoT: 01-rd/req/user_stories.md:230-241]. `US-A2-04` đã có Given-When-Then cho tạo bộ câu hỏi riêng
[SoT: 01-rd/req/user_stories.md:243-249]. Các mục dưới đây là hành vi **riêng của màn hình**, phát hiện khi
đối chiếu prototype:

- **Cho** tôi là giáo viên đang ở màn "Lớp của tôi", **Khi** tôi lọc bảng học viên theo tab một lớp cụ thể,
  **Thì** bảng chỉ còn hiển thị học viên thuộc lớp đó, không tải lại trang
  [SoT: 09-layoutBase/Giáo viên - Lớp của tôi.dc.html:163-166, 284].
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
| Q1 | `system_survey.md` gộp một slug `class_management` cho hai file prototype có route/nav riêng biệt ("Lớp của tôi" và "Bài tập của tôi"). RD này viết chung một file theo đúng yêu cầu công việc, nhưng khi lên BD/DD theo trục màn hình (mỗi màn có route và component riêng ở FE Next.js) thì có nên tách thành hai slug riêng (ví dụ `class_management` + `class_assignments`) không? | Đây là quyết định về cấu trúc màn, ảnh hưởng cách tổ chức `02-bd/screens/` và `03-dd/screens/` — không phải nội dung nghiệp vụ, cần chủ dự án chốt vì đụng tới cấu trúc tài liệu hai trục đã quy định ở `CLAUDE.md`. | Tách thành hai slug ở BD/DD (`class_management` cho "Lớp của tôi", `class_assignments` cho "Bài tập của tôi") vì hai route Next.js khác nhau, tránh một file BD/DD phải mô tả hai component không liên quan trực tiếp. | Chủ dự án |
| Q2 | Nút "+ Tạo lớp mới" ở "Lớp của tôi" không có hành vi nào được lập trình trong prototype (không `onClick`, không link). Luồng tạo lớp (nhập tên, lịch học, mã lớp để học viên tự tham gia hay giáo viên tự thêm học viên?) chưa có mã `Fx-nn` nào trong `req.md` mô tả. `US-A2-03` giả định "bài toán đã công bố... giao cho một lớp" nhưng không nói lớp được tạo/quản lý (CRUD) thế nào. | Không có mã yêu cầu chức năng nào cho việc **tạo/sửa/xoá lớp** — chỉ có yêu cầu về **giao bài cho lớp đã tồn tại** (F2-12). Đây là khoảng trống thật trong `req.md`, không phải suy diễn được từ tài liệu hiện có. | Bổ sung mã yêu cầu (ví dụ F1-1x hoặc F2-1x tuỳ Bounded Context) mô tả: ai tạo lớp (A2 tự tạo hay A3 tạo rồi gán giáo viên phụ trách?), cách học viên vào lớp (mã mời, A3 gán thủ công, hay A1 tự đăng ký bằng mã lớp?). Việc này chạm `identity` (thực thể `Class`, quan hệ user-class) nên cần ghi thêm vào `req.md` mục F1 hoặc F2 trước khi viết BD. | Chủ dự án |
| Q3 | Thẻ lớp trong "Lớp của tôi" không có hành vi bấm để xem chi tiết lớp (danh sách đầy đủ bài đã giao riêng cho lớp đó, lịch sử chấm lại...). Bảng học viên bên dưới đã lọc được theo lớp nhưng chưa rõ đây có phải là toàn bộ "chi tiết lớp" cần có hay còn thiếu (ví dụ xem theo từng buổi học, điểm danh). | `req.md` không có mã nào mô tả nội dung "trang chi tiết một lớp" cụ thể gồm những gì — chỉ có F2-12 (giao bài) và các mã liên quan chấm bài/tiến độ ở màn khác (`grading`, `progress`, ngoài phạm vi slug này). | Làm rõ: "Lớp của tôi" chỉ cần dạng tổng quan + bảng học viên lọc theo tab (đã đủ theo prototype), hay cần thêm trang con `/class/:id` chi tiết hơn. Nếu không cần thêm, không phải hành động gì; nếu cần, bổ sung mã yêu cầu. | Chủ dự án |
| Q4 | Bảng "Danh sách học viên" không có nút thêm/xoá học viên khỏi lớp, không có hồ sơ chi tiết từng học viên (link sang màn nào?). Ngưỡng phân loại trạng thái "Đang tốt/Cần hỗ trợ/Vắng bài" trong dữ liệu mẫu không có công thức nào trong `req.md`. | Không có mã `Fx-nn` nào định nghĩa quản lý thành viên lớp (thêm/xoá học viên) hay công thức phân loại trạng thái học viên — đây là khoảng trống nghiệp vụ, không phải chi tiết trình bày có thể tự suy ra. | Bổ sung mã yêu cầu cho: (a) cơ chế thêm/xoá học viên khỏi lớp (liên quan Q2 — ai quản lý thành viên lớp), (b) công thức phân loại trạng thái học viên nếu cần hiển thị chính thức (có thể là suy luận UI đơn giản không cần mã riêng, cần chủ dự án xác nhận mức độ chính thức). | Chủ dự án |
| Q5 | Màn "Lớp của tôi" không có bất kỳ UI nào cho F6-11 (tạo bộ câu hỏi phỏng vấn riêng và gán cho lớp), dù `system_survey.md` liệt kê F6-11 là một trong các mã liên quan của slug `class_management`. `US-A2-04` có Given-When-Then cho hành vi này nhưng không có màn cụ thể nào trong hai prototype hiện có dựng nó. | Đây là khoảng trống prototype thật: F6-11 có yêu cầu chức năng và user story rõ ràng, nhưng không có màn hình nào (kể cả `Câu hỏi phỏng vấn.dc.html` phía học viên) dựng luồng "giáo viên tạo bộ câu hỏi riêng, gán cho lớp". Không thể suy diễn UI khi không có prototype nào minh hoạ. | Hoặc (a) dựng thêm một khu vực/tab trong chính màn `class_management` cho F6-11 khi làm FE Next.js thật, hoặc (b) tách thành slug màn riêng (ví dụ `class_interview_sets`) nếu độ phức tạp UI đủ lớn (chọn câu hỏi từ ngân hàng, xem trước, gán lớp). Cần chủ dự án chọn hướng trước khi viết BD. | Chủ dự án |
| Q6 | Không có UI nào cho yêu cầu chấm lại theo lớp (F4-09a/b/c, đã có Given-When-Then ở `US-A2-03`) trong cả hai prototype của slug này. | Tương tự Q5 — có yêu cầu chức năng và user story, nhưng không có màn nào minh hoạ giao diện (chọn phiên bản testcase, xem ước lượng ảnh hưởng, nút xác nhận chạy). | Bổ sung khu vực "Yêu cầu chấm lại" vào màn "Bài tập của tôi" (gắn theo từng bài toán, hợp lý hơn gắn theo lớp vì testcase thuộc về bài toán) khi làm FE Next.js thật — cần chủ dự án xác nhận đặt ở đây hay ở một màn khác (ví dụ trang chi tiết bài toán phía Admin/Giáo viên, `problem-bank`). | Chủ dự án |
| Q7 | Nút "+ Gán từ ngân hàng bài toán" chỉ điều hướng sang `Ngân hàng bài toán.dc.html` mà không mang theo ngữ cảnh lớp nào, và màn ngân hàng bài toán phía học viên (`problem_list`) không có hành vi "gán cho lớp" nào được dựng. Luồng gán bài thực tế (chọn bài → chọn lớp → xác nhận) chưa có nơi nào minh hoạ trọn vẹn. | Khoảng trống prototype — có mã yêu cầu (F2-12) và user story (`US-A2-03` GWT đầu tiên: "Cho bài toán đã công bố, Khi tôi giao bài cho một lớp") nhưng không có UI cụ thể cho bước "chọn lớp để giao" nằm ở đâu: modal ngay tại `class_management`, hay một view giáo viên riêng của ngân hàng bài toán có thêm nút "Giao cho lớp" trên mỗi dòng? | Đề xuất: khi làm FE Next.js thật, thêm nút "Giao cho lớp này" ngay trên mỗi dòng bài toán khi giáo viên xem `problem_list` (view có phân biệt vai trò), mở modal chọn lớp — thay vì điều hướng rời trang như prototype hiện tại. Cần chủ dự án xác nhận vì đụng tới cách `problem_list` hiển thị khác nhau theo vai trò (hiện `01-rd/screens/users/problem_list.md` chỉ mô tả góc nhìn A1). | Chủ dự án |
| Q8 | Nút "Chi tiết" của mỗi dòng bài toán trong "Bài tập của tôi" trỏ nhầm sang `Câu hỏi phỏng vấn.dc.html` (F6) thay vì màn chi tiết bài toán (`problem_detail`, F2/F3). Đây rõ ràng là lỗi dựng prototype (copy-paste), không phải một liên kết nghiệp vụ. | Không cần chủ dự án quyết định nội dung — chỉ cần xác nhận rằng đây là lỗi kỹ thuật của prototype (không mang ý nghĩa business), để BD/DD không vô tình kế thừa liên kết sai này. | Ghi nhận là lỗi prototype, không sửa `09-layoutBase/Giáo viên - Bài tập của tôi.dc.html` (nằm ngoài phạm vi RD); khi viết BD/DD, nút "Chi tiết" của giáo viên trên mỗi bài toán nên trỏ tới `problem_detail` (góc nhìn giáo viên, nếu có) hoặc một view xem-trước bài toán, không phải `interview-bank`. | Chủ dự án (xác nhận cách hiểu, không cần quyết định nghiệp vụ mới) |
| Q9 | Không có nút gỡ/thu hồi bài đã gán khỏi lớp trong bảng "Bài tập của tôi", và không có cách xem/gỡ theo từng lớp cụ thể (chỉ xem gộp dạng text ở cột "Gán cho lớp"). Phía học viên (`problem_list`) cũng không có mã nào mô tả việc gỡ bài khỏi lớp ảnh hưởng thế nào tới học viên đang làm dở. | F2-12 chỉ mô tả hành vi giao bài, không mô tả hành vi thu hồi/gỡ bài đã giao — đây là khoảng trống, không phải chi tiết có thể suy luận an toàn (ảnh hưởng tới bài đã giao dở, lịch sử nộp bài đã có). | Bổ sung mã yêu cầu con của F2-12 (hoặc mã mới) mô tả: giáo viên có gỡ được bài đã giao không, gỡ rồi thì các lượt nộp cũ của học viên (điểm, lịch sử) có giữ nguyên không. | Chủ dự án |

## 6. Ngoài phạm vi file này

- Layout, spacing, bảng màu, component cụ thể (glass-morphism, breakpoint responsive) — thuộc BD
  (`02-bd/screens/teacher/class_management.md`, chưa viết).
- Hợp đồng API (danh sách lớp, danh sách học viên theo lớp, gán/gỡ bài, tạo bộ câu hỏi theo lớp) — thuộc DD
  (`03-dd/api/identity.md`, `03-dd/api/problem-bank.md`, `03-dd/api/interview-bank.md`, chưa viết).
- Màn "Chấm bài" (`Giáo viên - Chấm bài.dc.html`, F5-27, `US-A2-06`) và "Tiến độ học viên"
  (`Giáo viên - Tiến độ học viên.dc.html`) — là các slug màn khác theo mục 7.2 của `system_survey.md`,
  không thuộc phạm vi file này dù cùng actor A2 và có liên hệ số liệu (ví dụ thẻ "Cần chấm tay" xuất hiện ở
  cả hai màn của slug này lẫn ở màn Chấm bài).
- Thuật toán tính tỉ lệ hoàn thành, điểm trung bình, phân loại trạng thái học viên — thuộc logic của
  `identity`/`problem-bank`, không thuộc file theo trục màn này (xem Câu hỏi mở Q4 về việc có cần mã yêu
  cầu chính thức cho công thức phân loại hay không).
- Luồng CRUD lớp học (tạo/sửa/xoá lớp, thêm/xoá học viên) — **chưa có mã yêu cầu** trong `req.md`, xem Câu
  hỏi mở Q2 và Q4; không tự suy diễn hành vi trong file này.

## 7. Tham chiếu

- `01-rd/overview/system_survey.md:523` — dòng `class_management` trong bảng màn mục 7.2.
- `01-rd/req/req.md:55-68, 158-166, 390-402, 417` — F1-10 tới F1-12, F2-12, F5-27, F6-11.
- `01-rd/req/user_stories.md:230-249` — `US-A2-03`, `US-A2-04`.
- `01-rd/screens/users/problem_list.md:44-48` — mô tả khu "Bài tập lớp" phía học viên, đối chiếu hai chiều
  mục 3.3 của file này.
- `09-layoutBase/Giáo viên - Lớp của tôi.dc.html` — prototype 1.
- `09-layoutBase/Giáo viên - Bài tập của tôi.dc.html` — prototype 2.
- `.nexa/domain-registry.json` — định nghĩa Bounded Context và actor.
