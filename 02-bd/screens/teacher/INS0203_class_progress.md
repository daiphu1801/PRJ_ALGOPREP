# Tài liệu thiết kế cơ bản (BD) — Tiến độ lớp (`INS0203`)

## Phương châm tài liệu [Nội bộ — không cần khách duyệt]

- Viết theo **mẫu 9 sheet**. Bộ thẻ đóng, quy ước ID, ký hiệu chuẩn, ranh giới với tài liệu khác và
  checklist kiểm toán nằm ở `02-bd/_rules/bd-template-9sheet.md` — file này không chép lại.
- Phần gắn nhãn `[Nội bộ]` phục vụ sinh mã và tự kiểm toán, không phải yêu cầu của khách hàng: mục 4.5,
  mục 7.3 và các ghi chú quy ước.
- Mã màn `INS0203` theo bảng mã ở `02-bd/_rules/bd-template-9sheet.md` mục 8 dòng 153; tên file mang tiền
  tố mã.
- Màn này **không có popup nào**. Chỉ có một đường khoan sâu sang màn `class_student_detail` (`INS0204`).
- Khung điều hướng bên trái **không mô tả lại ở đây** — dùng chung `02-bd/screens/teacher/_shell.md`.
  Khu Giảng viên **không có toolbar dùng chung**, nên hàng đầu tiên trong `<main>` là item của chính màn
  này và được mô tả ở Khu vực A [Nguồn: 02-bd/screens/teacher/_shell.md:30,113-114].

> Đọc cùng `01-rd/screens/teacher/INS0203_class_progress.md` (hành vi ở mức yêu cầu, không lặp lại ở
> đây), `01-rd/req/identity.md:112-138` (F1-28 — mã yêu cầu trực tiếp của màn) và
> `02-bd/database/identity.md` mục 1.8 tới 1.12.
>
> **Không thiết kế lại** vòng đời lớp (tạo/sửa/xoá lớp, mã mời, gỡ học viên) — thuộc `class_management`
> (`INS0201`) [Nguồn: 02-bd/screens/teacher/INS0201_class_management.md:195-197].
> **Không thiết kế** phần giao/gỡ bài toán cho lớp — thuộc `class_assignments` (`INS0202`)
> [Nguồn: DEC-2026-0828-split-class-management-assignments].
> **Không thiết kế** giao diện chấm lại [Nguồn: DEC-2026-0828-remove-rejudge-scope] và **không** có
> luồng "Yêu cầu review" của học viên [Nguồn: DEC-2026-0830-remove-student-review-request].
> **Không thiết kế** phần chi tiết một học viên — thuộc `class_student_detail` (`INS0204`).

> **Quy ước đặt tên khối** [Nội bộ]. Kế thừa chuẩn đặt tên của cụm lớp do `class_management` chốt
> [Nguồn: 02-bd/screens/teacher/INS0201_class_management.md:27-30]: `header` (hàng đầu `<main>`),
> `studentList` (bảng danh sách học viên, kèm cụm tab lọc, ô tìm kiếm và phân trang). Màn này bổ sung
> **hai khối riêng** không có ở màn anh em: `scoreTrend` (biểu đồ đường điểm trung bình theo lớp) và
> `attention` (khối "Cần chú ý"). Màn này **không có** khối `stats` và **không có** khối `popup`.

---

## Sheet 1. Bìa

| Mục | Giá trị |
| :--- | :--- |
| Hệ thống | AlgoPrep |
| Phân hệ | `identity` (F1) — có phụ thuộc đọc sang `problem-bank` (F2) và `judge-orchestration` (F4) |
| Công đoạn | BD — Thiết kế cơ bản |
| Phân loại | Màn hình |
| Tên màn | Tiến độ lớp ("Tiến độ học viên") |
| Mã màn hình | `INS0203` |
| Tên vật lý (slug) | `class_progress` |
| Trục tài liệu | Màn hình (`02-bd/screens/`) |
| Actor | A2 (`INSTRUCTOR`) |
| Phiên bản | V0.1 |
| Người tạo | Nhóm phát triển AlgoPrep |
| Ngày tạo | 2026/09/21 |
| Người cập nhật | Nhóm phát triển AlgoPrep |
| Ngày cập nhật | 2026/09/21 |

---

## Sheet 2. Lịch sử sửa đổi

| Ver | Sheet bị sửa | Nội dung sửa | Ngày | Người sửa |
| :--- | :--- | :--- | :--- | :--- |
| V0.1 | Toàn bộ | Tạo mới theo mẫu 9 sheet. Chốt nguồn dữ liệu của mọi trường hiển thị; lấy bộ nhãn trạng thái và ngưỡng theo `DEC-2026-0830-class-progress-dashboard`. Thiết kế bổ sung phân trang, trạng thái rỗng và trạng thái lỗi mà prototype chưa dựng; sửa thiếu sót khối "Cần chú ý" không lọc theo tab lớp. Phát sinh 9 câu hỏi mở | 2026/09/21 | Nhóm phát triển AlgoPrep |
| V0.2 | 4.3, 5, 7.1, 7.2, Câu hỏi mở | Áp `DEC-2026-0921-teacher-screens-conflict-resolutions` và `DEC-2026-0921-class-completion-owned-by-identity`: Điểm TB đổi nguồn sang `judge.submissions` tính theo phạm vi lớp; Hoàn thành tính tại `identity` từ `ListClassAssignments` + `user_problem_best_score`, bỏ `GetClassAssignmentCompletion`. Đóng Q1, Q8 | 2026-09-21 | AI |
| V0.3 | 7.1, Câu hỏi mở | Backport bộ 5 nhãn trạng thái học viên (`INSUFFICIENT_DATA`/`ABSENT`/`NEEDS_SUPPORT`/`WATCH`/`ON_TRACK`) từ `DEC-2026-0921-teacher-screens-conflict-resolutions`, thay đề xuất 4 nhãn cũ thiếu `INSUFFICIENT_DATA`; đồng bộ với `INS0201_class_management.md`. Đóng Q4 | 2026-09-24 | AI |

---

## Sheet 3. Sơ đồ chuyển màn

> [Nội bộ] Quy ước vẽ: bố trí từ trái sang phải. Màn gọi tới tô tím, màn hiện tại tô xanh nhạt. Màn này
> không có popup nên không dùng màu vàng. Màu và `[Nguồn:]` dùng để truy vết, không phải yêu cầu của
> khách hàng.

### 3.1 Danh sách chuyển màn

#### Khung điều hướng Giảng viên → Tiến độ lớp

[Điều kiện mở] Chọn mục "Tiến độ học viên" (mục nav thứ 5) trên thanh điều hướng bên trái của khu Giảng
viên [Nguồn: 02-bd/screens/teacher/_shell.md:76].

[Chế độ mở] Không có.

[Thông tin truyền] Không có.

[Giá trị trả về] Không có.

[Khi thành công] Tải ba khối dữ liệu của bộ lọc mặc định "Tất cả": biểu đồ điểm trung bình 6 mốc tuần
gần nhất, khối "Cần chú ý", và trang đầu của bảng danh sách học viên.

[Khi huỷ] Không có.

#### Tiến độ lớp → Chi tiết học viên (từ bảng danh sách)

[Điều kiện mở] Bấm vào tên một học viên trong bảng danh sách học viên.

[Chế độ mở] Không có.

[Thông tin truyền] `id` học viên và `id` lớp của chính dòng đó — phạm vi hiển thị của màn đích giới hạn
trong lớp này [Nguồn: 01-rd/req/identity.md:104-108]. Tham số truyền giống hệt `class_management`
[Nguồn: 02-bd/screens/teacher/INS0201_class_management.md:153-154], để hai đường vào cùng một màn đích
không sinh hai hợp đồng khác nhau.

[Giá trị trả về] Không có.

[Khi thành công] Điều hướng sang màn `class_student_detail` (`INS0204`). Màn này chỉ đọc, không có thay
đổi chưa lưu, nên không hỏi xác nhận trước khi rời.

[Khi huỷ] Không có.

#### Tiến độ lớp → Chi tiết học viên (từ khối "Cần chú ý")

[Điều kiện mở] Bấm vào tên một học viên trong khối "Cần chú ý".

[Chế độ mở] Không có.

[Thông tin truyền] `id` học viên và `id` lớp của mục đó — giống hệt đường vào từ bảng.

[Giá trị trả về] Không có.

[Khi thành công] Mở đúng màn `class_student_detail` với đúng bộ tham số như đường vào từ bảng; hai đường
vào không được cho ra hai màn khác nhau.

[Khi huỷ] Không có.

### 3.2 Sơ đồ

```mermaid
flowchart LR
    nav["Khung điều hướng Giảng viên<br/>mục Tiến độ học viên"] -->|"chọn Tiến độ học viên"| main["Tiến độ lớp<br/>class_progress"]
    main -->|"bấm tên học viên trong bảng"| detail["Chi tiết học viên<br/>class_student_detail"]
    main -->|"bấm tên học viên trong khối Cần chú ý"| detail

    classDef source fill:#F3E5F5,stroke:#9C5CC4,color:#000
    classDef screen fill:#E3F2FD,stroke:#3B82F6,color:#000

    class nav,detail source
    class main screen
```

[Nguồn: 09-layoutBase/Giáo viên - Tiến độ học viên.dc.html:244-250,136-150,172-186; 01-rd/screens/teacher/INS0203_class_progress.md:68-100]

---

## Sheet 4. Bố cục màn hình

### 4.1 Tổng quan màn

[Mục đích màn] Cho giảng viên nhìn tiến độ **tổng hợp của nhiều học viên** trong các lớp mình phụ trách
— xu hướng điểm trung bình theo lớp, danh sách học viên kèm điểm và tỉ lệ hoàn thành, và một khối nêu
học viên có dấu hiệu tụt lại — để phát hiện sớm người cần hỗ trợ mà không phải mở từng trang tiến độ cá
nhân [Nguồn: 01-rd/screens/teacher/INS0203_class_progress.md:25-29; 01-rd/req/identity.md:112-115].

[Luồng nghiệp vụ chính]

1. **Hiển thị ban đầu**: vào màn, hệ thống tải song song bốn nhóm dữ liệu — danh sách lớp phụ trách (để
   dựng cụm tab), chuỗi điểm 6 mốc tuần, khối "Cần chú ý", và trang đầu bảng học viên. Bộ lọc mặc định
   là "Tất cả". Trong lúc chờ, mỗi khối hiển thị khung chờ riêng.
2. **Lọc theo lớp**: chọn một tab lớp. Cả ba khối — biểu đồ, "Cần chú ý", bảng — cùng đổi theo tab; khối
   "Cần chú ý" **bắt buộc lọc theo tab**, prototype không lọc là thiếu sót cần sửa, không phải chủ ý
   [Nguồn: DEC-2026-0830-class-progress-dashboard].
3. **Tìm kiếm học viên**: gõ vào ô tìm kiếm, lọc theo tên. Tìm kiếm và tab lớp áp dụng đồng thời, và chỉ
   tác động lên bảng học viên (xem Câu hỏi mở Q7).
4. **Phân trang**: bảng học viên phân trang cùng kiểu `problem_list` (F2-11)
   [Nguồn: DEC-2026-0830-class-progress-dashboard].
5. **Khoan sâu**: bấm tên học viên ở bảng hoặc ở khối "Cần chú ý" để sang `class_student_detail`.

[Người dùng] Giảng viên đã đăng nhập, có Function `CLASS_MANAGEMENT` [Nguồn: 01-rd/req/identity.md:137-138].

[Tệp liên quan] Không có. Màn này không xuất hay nhập tệp.

[Phạm vi]
- Chỉ học viên thuộc các lớp có `instructor_id` bằng người đang đăng nhập. Không có chế độ xem toàn hệ
  thống ở màn này.
- **Chỉ đọc**. Màn này không tạo, không sửa, không xoá bất cứ dữ liệu nghiệp vụ nào — không có nút nào
  ghi dữ liệu, không có popup xác nhận.
- **Không** quản lý vòng đời lớp, mã mời, gỡ học viên — thuộc `class_management`.
- **Không** giao/gỡ bài toán cho lớp — thuộc `class_assignments`
  [Nguồn: DEC-2026-0828-split-class-management-assignments].
- **Không** hiển thị điểm AI tham khảo của F5-27 ở cột "Điểm TB" — F1-28 loại trừ rõ ràng
  [Nguồn: 01-rd/req/identity.md:116-118].
- **Không** hiển thị nội dung bài nộp, dữ liệu testcase ẩn hay kết quả so khớp chi tiết ở bất kỳ trạng
  thái nào của màn.

[Quyền sử dụng]
- Xem: được, khi có `CLASS_MANAGEMENT:READ`.
- Thêm: không có.
- Sửa: không có.
- Xoá: không có.

[Số bản ghi tối đa] Biểu đồ: đúng 6 mốc tuần, mỗi lớp một đường (xem Câu hỏi mở Q5 về số lớp tối đa vẽ
cùng lúc). Khối "Cần chú ý": tối đa 5 mục, ưu tiên mức nghiêm trọng (xem Câu hỏi mở Q4). Cụm tab lọc:
1 + số lớp phụ trách. Bảng học viên: 20 dòng mỗi trang `[Suy luận]`, cùng con số mà `class_management`
đã đề xuất cho bảng học viên của nó [Nguồn: 02-bd/screens/teacher/INS0201_class_management.md:600].

[Nguồn: 01-rd/screens/teacher/INS0203_class_progress.md:68-100; 01-rd/req/identity.md:112-138]

### 4.2 DTO liên quan

- `ClassScoreTrendDto` — một đường trên biểu đồ: một lớp kèm 6 mốc tuần.
- `ClassAttentionStudentDto` — một mục trong khối "Cần chú ý".
- `ClassProgressStudentRowDto` — một dòng của bảng danh sách học viên.
- `ClassCardDto` — **dùng lại** của `class_management`, chỉ lấy `id` và `name` để dựng cụm tab lọc
  [Nguồn: 02-bd/screens/teacher/INS0201_class_management.md:244-245].

`[Suy luận]` — tên DTO do BD này đề xuất, `03-dd/api/identity.md` chốt lại.

### 4.3 Bảng dữ liệu liên quan (5)

| NO | Bảng | Ghi chú |
| --: | :--- | :--- |
| 1 | `identity.classes` | Tên lớp cho cụm tab, cột "Lớp" và tên đường trên biểu đồ [Nguồn: 02-bd/database/identity.md:85-88] |
| 2 | `identity.class_enrollments` | Tập học viên của từng lớp [Nguồn: 02-bd/database/identity.md:95-101] |
| 3 | `identity.users` | Tên hiển thị học viên — cột `display_name` [Nguồn: 02-bd/database/identity.md:16] |
| 4 | `judge.submissions` | Nguồn tính Điểm TB **theo phạm vi lớp**, đọc qua `GetStudentSubmissionMetrics` của `judge-orchestration`, không truy vấn chéo schema. Thay cho `identity.user_submission_stats` (read model toàn cục, không có chiều `class_id`) từ 2026-09-21 theo `DEC-2026-0921-teacher-screens-conflict-resolutions` |
| 5 | `identity.identity_recent_activity` | Cột "Hoạt động" của bảng học viên [Nguồn: 02-bd/database/identity.md:115-116] |

Ba nhóm số liệu còn lại **không đọc từ bảng của `identity`**, nên không tính vào bảng trên và không
truy vấn chéo schema:

| Số liệu | Nguồn thật | Cách lấy |
| :--- | :--- | :--- |
| Tỉ lệ "Hoàn thành" | `problem.class_assignments` (F2-12, mẫu số) [Nguồn: 02-bd/database/problem-bank.md:116-128] + `identity.user_problem_best_score` (tử số) [Nguồn: 02-bd/database/identity.md:111] | `identity` lấy danh sách bài đã giao qua `ListClassAssignments` rồi **tự tính** tỉ lệ; không gọi `GetClassAssignmentCompletion` (endpoint đó đã bỏ theo `DEC-2026-0921-class-completion-owned-by-identity`) |
| "Chuỗi ngày", "Xu hướng", chuỗi điểm theo tuần, mốc nộp gần nhất để xét "Vắng bài" | `judge.submissions` [Nguồn: 02-bd/database/judge-orchestration.md:8-34] | Endpoint `GetStudentSubmissionMetrics` của `judge-orchestration`, xem Câu hỏi mở Q2 |
| Điểm TB **theo phạm vi lớp** | `judge.submissions` giới hạn theo tập bài đã giao cho lớp | Cùng endpoint trên, xem Câu hỏi mở Q1 |

### 4.4 Vùng bố cục

Đối chiếu `09-layoutBase/Giáo viên - Tiến độ học viên.dc.html` — bằng chứng bố cục chỉ-đọc, **không phải**
design system cuối cùng.

| Vùng | Vị trí trong prototype | Nội dung |
| :--- | :--- | :--- |
| Thanh điều hướng bên trái (khung chung Giảng viên) | `:62-110` | Mục "Tiến độ học viên" đang chọn, không có badge — dùng lại khung chung, không mô tả lại ở đây |
| Container nội dung | `:112-113` | `max-width: 1320px` căn giữa, các khối xếp dọc, khoảng cách đều |
| Hàng đầu — tiêu đề và phụ đề | `:115-120` | Tiêu đề "Tiến độ học viên" (`:117`), phụ đề "Theo dõi điểm số, hoàn thành và mức độ hoạt động" (`:118`). **Không có nút thao tác nào** — khác hẳn hàng đầu của `class_management`. **Không phải toolbar dùng chung** [Nguồn: 02-bd/screens/teacher/_shell.md:30] |
| Hàng hai — hai cột | `:122` | Lưới `1.3fr 1fr`, `align-items: start` |
| Cột trái — biểu đồ "Điểm trung bình theo lớp" | `:123-134`, dữ liệu `:278-279` | Tiêu đề (`:124`), dòng phụ "6 tuần gần nhất" (`:125`), `<svg viewBox="0 0 500 140">` vẽ 2 đường (`:126-129`), chú giải tên lớp kèm chấm màu (`:130-133`) |
| Cột phải — khối "Cần chú ý" | `:136-150`, dữ liệu `:281-286` | Tiêu đề (`:137`), danh sách mục: tên học viên (`:142`), dòng lý do (`:143`), nhãn trạng thái bên phải (`:145`). Prototype dựng 4 mục |
| Khối "Danh sách học viên" — hàng lọc | `:154-165`, dữ liệu `:288-293,320` | Ô tìm kiếm "Tìm học viên" (`:155-158`), cụm tab lọc lớp (`:159-163`), dòng đếm kết quả `{N} / 82 học viên` (`:164`) |
| Khối "Danh sách học viên" — bảng | `:167-187`, dữ liệu `:226-234` | Lưới 7 cột `minmax(160px,1.3fr) 130px 90px 100px 90px 120px 110px`, `min-width: 860px`, cuộn ngang khi hẹp. Cột: Học viên, Lớp, Điểm TB, Hoàn thành, Chuỗi ngày, Xu hướng, Hoạt động (`:169`) |
| Phân trang | không có | Prototype không dựng; BD thiết kế bổ sung theo F1-28 [Nguồn: 01-rd/req/identity.md:134-136] |
| Chân trang | không có | Cả 5 prototype khu Giảng viên đều không có `<footer>` [Nguồn: 02-bd/screens/teacher/_shell.md:31] |

Bốn điểm dưới đây **không có trong prototype** và do BD này thiết kế bổ sung, vì F1-28 chốt sau khi
prototype đã vẽ [Nguồn: 01-rd/req/identity.md:112-115]:

1. **Phân trang bảng học viên** — prototype chỉ có 8 bản ghi mẫu nhưng dòng đếm ghi "82 học viên"
   [Nguồn: 09-layoutBase/Giáo viên - Tiến độ học viên.dc.html:226-234,320].
2. **Trạng thái rỗng và trạng thái lỗi kèm nút "Thử lại"** cho từng khối
   [Nguồn: 01-rd/req/identity.md:134-136].
3. **Liên kết khoan sâu** từ tên học viên (ở bảng và ở khối "Cần chú ý") sang `class_student_detail` —
   prototype vẽ tên học viên là chữ thường, không phải liên kết
   [Nguồn: 09-layoutBase/Giáo viên - Tiến độ học viên.dc.html:176,142].
4. **Khối "Cần chú ý" lọc theo tab lớp** — prototype dựng khối này là hằng số, hoàn toàn bỏ qua
   `classFilter` [Nguồn: 09-layoutBase/Giáo viên - Tiến độ học viên.dc.html:281-286,296-300].

### 4.5 Cấu trúc slice FSD [Nội bộ]

| Khối | Slice dự kiến | Căn cứ |
| :--- | :--- | :--- |
| Trang | `views/class-progress` | Quy ước FSD của dự án; slice đã được tạo sẵn dạng stub [Nguồn: 02-bd/screens/teacher/_shell.md:13-14] |
| Khung Giảng viên | Dùng lại `widgets/app-shell` (biến thể `instructor-sidebar`) | `02-bd/screens/teacher/_shell.md` mục 1 |
| Biểu đồ điểm theo lớp | `widgets/class-score-trend-chart` | Prototype `:123-134` |
| Khối "Cần chú ý" | `widgets/class-attention-list` | Prototype `:136-150` |
| Bảng học viên | `widgets/class-progress-table` | Prototype `:152-188` |
| Thực thể dùng chung | Dùng lại `entities/class` và `entities/class-student` | Đã khai ở `class_management`, **không định nghĩa lại** [Nguồn: 02-bd/screens/teacher/INS0201_class_management.md:298-300] |

`[Suy luận]` — ánh xạ slice do BD đề xuất, DD màn hình chốt lại. Cụm tab lọc lớp xuất hiện ở cả
`class_management` và màn này, nên nên nằm ở một component dùng chung của `entities/class` thay vì dựng
hai lần.

> [Nội bộ] Ảnh minh hoạ đặt ở `08-diagram/02-bd/screens/teacher/`, chụp bằng Playwright trên ứng dụng
> Next.js thật khi đã có mã chạy được. Chưa có thì tham chiếu prototype, không vẽ tay.

---

## Sheet 5. Danh sách item màn hình

> Quy ước ID, bộ thẻ và giá trị chuẩn: `02-bd/_rules/bd-template-9sheet.md` mục 2, 3, 4.

### Khu vực A — Hàng đầu

| Khu vực | NO | Tên item | ID item | Bảng DB | Cột DB | Loại UI | Kiểu | Độ dài | Bắt buộc | I/O | Giá trị mặc định | Định dạng | Ghi chú |
| :--- | --: | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :-: | :-: | :--- | :--- | :--- |
| Hàng đầu | | | | | | | | | | | | | |
| | 1 | Tiêu đề màn | `classProgress.header.title` | - | - | Label | String | - | - | O | Tiến độ học viên | - | Tên màn hiển thị cố định<br>[Nguồn giá trị] Nhãn tĩnh i18n [Nguồn: 09-layoutBase/Giáo viên - Tiến độ học viên.dc.html:117]<br>[EVT liên quan] - |
| | 2 | Phụ đề màn | `classProgress.header.subtitle` | - | - | Label | String | - | - | O | Theo dõi điểm số, hoàn thành và mức độ hoạt động | - | Một dòng giải thích mục đích màn<br>[Nguồn giá trị] Nhãn tĩnh i18n [Nguồn: 09-layoutBase/Giáo viên - Tiến độ học viên.dc.html:118]<br>[EVT liên quan] - |

### Khu vực B — Biểu đồ điểm trung bình theo lớp

| Khu vực | NO | Tên item | ID item | Bảng DB | Cột DB | Loại UI | Kiểu | Độ dài | Bắt buộc | I/O | Giá trị mặc định | Định dạng | Ghi chú |
| :--- | --: | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :-: | :-: | :--- | :--- | :--- |
| Biểu đồ điểm | | | | | | | | | | | | | |
| | 1 | Tiêu đề khối | `classProgress.scoreTrend.title` | - | - | Label | String | - | - | O | Điểm trung bình theo lớp | - | Tiêu đề khối biểu đồ<br>[Nguồn giá trị] Nhãn tĩnh i18n [Nguồn: 09-layoutBase/Giáo viên - Tiến độ học viên.dc.html:124]<br>[EVT liên quan] - |
| | 2 | Phạm vi thời gian | `classProgress.scoreTrend.rangeLabel` | - | - | Label | String | - | - | O | 6 tuần gần nhất | - | Nhãn cho biết biểu đồ vẽ 6 mốc tuần gần nhất<br>[Nguồn giá trị] Nhãn tĩnh i18n; số 6 chốt bởi F1-28 [Nguồn: 01-rd/req/identity.md:126-128]<br>[EVT liên quan] - |
| | 3 | Đường điểm của lớp | `classProgress.scoreTrend.line` | - | - | List | List | - | - | O | rỗng | Đường cong 6 điểm | Mỗi lớp trong phạm vi lọc là một đường<br>[Công thức] Mỗi mốc tuần = trung bình cộng Điểm TB của mọi học viên trong lớp tại mốc đó [Nguồn: 01-rd/req/identity.md:118-119]. Nguồn dữ liệu theo mốc tuần **chưa có bảng nào lưu** — xem Câu hỏi mở Q3<br>[EVT liên quan] EVT-1, EVT-2 |
| | 4 | Chú giải lớp | `classProgress.scoreTrend.legend` | `identity.classes` | `name` | Label | List | 120 | - | O | - | Chấm màu + tên lớp | Một mục chú giải cho mỗi đường<br>[Nguồn giá trị] Cột `name` của các lớp đang được vẽ [Nguồn: 09-layoutBase/Giáo viên - Tiến độ học viên.dc.html:130-133]<br>[EVT liên quan] EVT-2 |

### Khu vực C — Cần chú ý

| Khu vực | NO | Tên item | ID item | Bảng DB | Cột DB | Loại UI | Kiểu | Độ dài | Bắt buộc | I/O | Giá trị mặc định | Định dạng | Ghi chú |
| :--- | --: | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :-: | :-: | :--- | :--- | :--- |
| Cần chú ý | | | | | | | | | | | | | |
| | 1 | Tiêu đề khối | `classProgress.attention.title` | - | - | Label | String | - | - | O | Cần chú ý | - | Tiêu đề khối<br>[Nguồn giá trị] Nhãn tĩnh i18n [Nguồn: 09-layoutBase/Giáo viên - Tiến độ học viên.dc.html:137]<br>[EVT liên quan] - |
| | 2 | Danh sách cần chú ý | `classProgress.attention.list` | `identity.class_enrollments` | - | List | List | - | - | O | rỗng | Tối đa 5 mục | Học viên có ít nhất một dấu hiệu tụt lại, trong phạm vi tab lớp đang chọn<br>[Nguồn giá trị] Kết quả gọi `ListClassAttentionStudents`<br>[EVT liên quan] EVT-1, EVT-2 |
| | 3 | Học viên | `classProgress.attention.col.studentName` | `identity.users` | `display_name` | Link | String | 100 | - | O | - | - | Tên hiển thị học viên; bấm vào mở màn `class_student_detail`<br>[Nguồn giá trị] Cột `display_name`<br>[EVT liên quan] EVT-7 |
| | 4 | Lý do cần chú ý | `classProgress.attention.col.reason` | - | - | ListColumn | String | 60 | - | O | - | Câu ngắn | Giải thích vì sao học viên vào khối này, ví dụ "Không nộp bài 9 ngày", "Hoàn thành 42%", "Điểm giảm 3 tuần liên tiếp"<br>[Công thức] Sinh từ chính ngưỡng đã kích hoạt ở NO 5, kèm con số thật của học viên đó; không phải chuỗi tự do [Nguồn: 09-layoutBase/Giáo viên - Tiến độ học viên.dc.html:143,282-285]<br>[EVT liên quan] - |
| | 5 | Trạng thái | `classProgress.attention.col.status` | - | - | Badge | Enum | - | - | O | - | Nhãn tiếng Việt | Ba nhãn: "Vắng bài", "Theo dõi", "Cần hỗ trợ"<br>[Công thức] "Vắng bài" = không có lượt nộp nào trong 7 ngày gần nhất; "Theo dõi" = Hoàn thành dưới 50% **và** đã được giao ít nhất một bài; "Cần hỗ trợ" = Điểm TB giảm liên tiếp qua từ hai mốc tuần trở lên [Nguồn: DEC-2026-0830-class-progress-dashboard; 01-rd/req/identity.md:129-133]. Học viên khớp nhiều ngưỡng thì lấy nhãn nghiêm trọng nhất theo thứ tự "Vắng bài" > "Cần hỗ trợ" > "Theo dõi" `[Suy luận]`, xem Câu hỏi mở Q4<br>[EVT liên quan] - |

### Khu vực D — Danh sách học viên

| Khu vực | NO | Tên item | ID item | Bảng DB | Cột DB | Loại UI | Kiểu | Độ dài | Bắt buộc | I/O | Giá trị mặc định | Định dạng | Ghi chú |
| :--- | --: | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :-: | :-: | :--- | :--- | :--- |
| Danh sách học viên | | | | | | | | | | | | | |
| | 1 | Ô tìm kiếm học viên | `classProgress.studentList.searchBox` | `identity.users` | `display_name` | TextBox | String | 100 | - | I | rỗng | Chuỗi tự do | Lọc bảng theo tên học viên, khớp một phần và không phân biệt hoa thường<br>[Nguồn giá trị] Người dùng nhập; đối chiếu `display_name` [Nguồn: 09-layoutBase/Giáo viên - Tiến độ học viên.dc.html:155-158,295-300]<br>[EVT liên quan] EVT-3 |
| | 2 | Tab lọc theo lớp | `classProgress.studentList.classTabs` | `identity.classes` | `id`, `name` | Button | List | - | - | I/O | Tất cả | - | Cụm tab: mục "Tất cả" cố định cộng một mục cho mỗi lớp phụ trách<br>[Nguồn giá trị] Mục "Tất cả" là nhãn tĩnh i18n; các mục còn lại lấy `classes.name` qua `ListInstructorClasses`<br>[EVT liên quan] EVT-2 |
| | 3 | Đếm kết quả | `classProgress.studentList.resultCount` | - | - | Label | String | 24 | - | O | - | `{N} / {tổng} học viên` | Số học viên khớp bộ lọc trên tổng số học viên trong phạm vi tab đang chọn<br>[Công thức] Tử số = tổng số dòng khớp cả tab và ô tìm kiếm (**không** phải số dòng của trang hiện tại); mẫu số = số học viên thuộc phạm vi tab, bỏ qua ô tìm kiếm [Nguồn: 09-layoutBase/Giáo viên - Tiến độ học viên.dc.html:164,320]<br>[EVT liên quan] EVT-2, EVT-3 |
| | 4 | Bảng học viên | `classProgress.studentList.table` | `identity.class_enrollments` | - | List | List | - | - | O | rỗng | 20 dòng mỗi trang | Học viên thuộc các lớp phụ trách, lọc theo tab và ô tìm kiếm<br>[Nguồn giá trị] Kết quả gọi `ListClassStudents`<br>[EVT liên quan] EVT-1, EVT-2, EVT-3, EVT-4 |
| | 5 | Học viên | `classProgress.studentList.col.studentName` | `identity.users` | `display_name` | Link | String | 100 | - | O | - | Chữ cái đầu + tên | Tên hiển thị kèm ô chữ cái đầu; bấm vào mở màn `class_student_detail`<br>[Công thức] Ô chữ cái đầu ghép từ hai ký tự đầu của hai từ cuối trong `display_name`, cùng quy tắc `class_management` [Nguồn: 02-bd/screens/teacher/INS0201_class_management.md:358]; ảnh đại diện thật chưa có nguồn `[Suy luận]`<br>[EVT liên quan] EVT-6 |
| | 6 | Lớp | `classProgress.studentList.col.className` | `identity.classes` | `name` | ListColumn | String | 120 | - | O | - | - | Lớp mà dòng này thuộc về<br>[Nguồn giá trị] Cột `name` của lớp gắn với dòng `class_enrollments`<br>[EVT liên quan] - |
| | 7 | Điểm TB | `classProgress.studentList.col.avgScore` | `judge.submissions` | `status`, `problem_id`, `user_id` | ListColumn | Number | 4 | - | O | - | Một chữ số thập phân, thang 10 | Điểm trung bình của học viên<br>[Công thức] `accepted_count` chia `total_submissions` rồi nhân 10 [Nguồn: DEC-2026-0830-class-progress-dashboard]. Chưa có lượt nộp nào thì hiển thị `-`. F1-28 nói "trong lớp" nhưng read model là toàn cục — xem Câu hỏi mở Q1<br>[EVT liên quan] - |
| | 8 | Hoàn thành | `classProgress.studentList.col.completion` | - | - | ListColumn | Number | 3 | - | O | - | `{số}%` | Tỉ lệ bài đã giao mà học viên đã Accepted<br>[Công thức] Số bài đã giao cho lớp mà học viên Accepted ít nhất một lần chia tổng số bài đã giao cho lớp [Nguồn: DEC-2026-0830-class-progress-dashboard]. Nguồn: mẫu số là danh sách bài đã giao lấy qua `ListClassAssignments` (`problem-bank`), tử số là `identity.user_problem_best_score.best_verdict = ACCEPTED`; `identity` tự tính, endpoint `GetClassAssignmentCompletion` đã bỏ theo `DEC-2026-0921-class-completion-owned-by-identity`<br>[EVT liên quan] - |
| | 9 | Chuỗi ngày | `classProgress.studentList.col.streak` | - | - | ListColumn | Number | 4 | - | O | - | `{số} ngày`, bằng 0 thì `-` | Số ngày liên tiếp gần nhất tính tới hôm nay mà học viên có ít nhất một lượt nộp<br>[Công thức] Đếm ngày liên tiếp có lượt nộp trong `judge.submissions`, cùng khái niệm chuỗi ngày của F1-21 [Nguồn: 01-rd/req/identity.md:123-125]. **Không có cột nào lưu sẵn** giá trị này — xem Câu hỏi mở Q2<br>[EVT liên quan] - |
| | 10 | Xu hướng | `classProgress.studentList.col.trend` | - | - | ListColumn | List | - | - | O | rỗng | Đường cong 6 điểm | Sparkline điểm TB của riêng học viên theo 6 mốc tuần gần nhất<br>[Công thức] Cùng công thức Điểm TB ở NO 7 nhưng tính theo từng mốc tuần [Nguồn: 01-rd/req/identity.md:126-128]. Cùng nguồn với Khu vực B NO 3 — xem Câu hỏi mở Q3<br>[EVT liên quan] - |
| | 11 | Hoạt động | `classProgress.studentList.col.lastActive` | `identity.identity_recent_activity` | `occurred_at` | ListColumn | String | 20 | - | O | - | Khoảng thời gian tương đối | Thời điểm hoạt động gần nhất của học viên<br>[Công thức] `occurred_at` lớn nhất của học viên, hiển thị dạng tương đối ("12 phút trước", "Hôm qua"); chưa có hoạt động nào thì hiển thị `-`. Cùng quy tắc `class_management` [Nguồn: 02-bd/screens/teacher/INS0201_class_management.md:363]<br>[EVT liên quan] - |
| | 12 | Phân trang | `classProgress.studentList.pagination` | - | - | Button | Number | 4 | - | I/O | 1 | `Trang {n} / {tổng}` | Điều khiển chuyển trang bảng học viên, cùng kiểu `problem_list` (F2-11)<br>[Nguồn giá trị] Tổng số trang do máy chủ trả về cùng kết quả `ListClassStudents` [Nguồn: DEC-2026-0830-class-progress-dashboard]<br>[EVT liên quan] EVT-4 |
| | 13 | Thử lại | `classProgress.studentList.btnRetry` | - | - | Button | - | - | - | I | - | Thử lại | Nút tải lại khối đang ở trạng thái lỗi; mỗi khối có nút riêng của nó<br>[Nguồn giá trị] Nhãn tĩnh i18n; yêu cầu "trạng thái lỗi kèm nút thử lại" của F1-28 [Nguồn: 01-rd/req/identity.md:134-136]<br>[EVT liên quan] EVT-5 |

[Nguồn: 09-layoutBase/Giáo viên - Tiến độ học viên.dc.html:115-120,122-134,136-150,152-188,226-234,281-293,302-311,320; 02-bd/database/identity.md:16,85-101,113-116; 01-rd/req/identity.md:112-138]

---

## Sheet 6. Đặc tả điều khiển item

> Ký hiệu cột `Hiển thị` và thứ tự thẻ: `02-bd/_rules/bd-template-9sheet.md` mục 2, 3. Dùng đúng NO, tên
> item và thứ tự của Sheet 5. Lỗi nhập liệu ghi ở Sheet 9, xử lý nghiệp vụ ghi ở Sheet 8.

### Khu vực A — Hàng đầu

| Khu vực | NO | Tên item | Hiển thị | Ghi chú |
| :--- | --: | :--- | :-: | :--- |
| Hàng đầu | | | | |
| | 1 | Tiêu đề màn | Có | - |
| | 2 | Phụ đề màn | Có | - |

### Khu vực B — Biểu đồ điểm trung bình theo lớp

| Khu vực | NO | Tên item | Hiển thị | Ghi chú |
| :--- | --: | :--- | :-: | :--- |
| Biểu đồ điểm | | | | |
| | 1 | Tiêu đề khối | Có | - |
| | 2 | Phạm vi thời gian | Có | - |
| | 3 | Đường điểm của lớp | Điều kiện | [Điều kiện hiển thị] Trong lúc tải hiển thị khung chờ đúng kích thước khối biểu đồ. Chưa đủ hai mốc tuần có dữ liệu thì không vẽ đường mà hiển thị "Chưa đủ dữ liệu để vẽ xu hướng" — một điểm đơn lẻ không phải xu hướng. Tab đang chọn là một lớp cụ thể thì chỉ vẽ đúng một đường.<br>[Tự động đặt] Vẽ lại mỗi lần đổi tab lớp. |
| | 4 | Chú giải lớp | Điều kiện | [Điều kiện hiển thị] Chỉ hiển thị khi có ít nhất một đường được vẽ; số mục chú giải luôn bằng số đường. |

### Khu vực C — Cần chú ý

| Khu vực | NO | Tên item | Hiển thị | Ghi chú |
| :--- | --: | :--- | :-: | :--- |
| Cần chú ý | | | | |
| | 1 | Tiêu đề khối | Có | - |
| | 2 | Danh sách cần chú ý | Có | [Điều kiện hiển thị] Trong lúc tải hiển thị khung chờ 4 mục. Không có học viên nào khớp ngưỡng thì hiển thị "Không có học viên nào cần chú ý" — **không** ẩn khối, vì khối trống chính là thông tin tốt cho giảng viên.<br>[Tự động đặt] Tải lại theo tab lớp đang chọn; **không** chịu ảnh hưởng của ô tìm kiếm, xem Câu hỏi mở Q7. |
| | 3 | Học viên | Có | [Điều kiện kích hoạt] Luôn kích hoạt sau khi tải xong; liên kết mở màn `class_student_detail` với đúng cặp tham số như liên kết ở bảng. |
| | 4 | Lý do cần chú ý | Có | - |
| | 5 | Trạng thái | Có | [Tự động đặt] Nhãn tính lại theo đúng công thức ở Sheet 5 mỗi lần tải khối; không phải giá trị lưu sẵn trong DB. |

### Khu vực D — Danh sách học viên

| Khu vực | NO | Tên item | Hiển thị | Ghi chú |
| :--- | --: | :--- | :-: | :--- |
| Danh sách học viên | | | | |
| | 1 | Ô tìm kiếm học viên | Có | [Điều kiện kích hoạt] Không kích hoạt trong lúc đang tải trang đầu; kích hoạt sau khi bảng tải xong.<br>[Tự động xoá] Đổi tab lớp thì **giữ nguyên** nội dung ô tìm kiếm — giảng viên tìm một học viên rồi chuyển lớp thường vẫn muốn tìm người đó `[Suy luận]`. |
| | 2 | Tab lọc theo lớp | Có | [Điều kiện hiển thị] Chỉ hiển thị khi có từ 1 lớp trở lên; không có lớp nào thì ẩn cả khối bảng và khối "Cần chú ý", thay bằng trạng thái rỗng cấp màn.<br>[Tự động đặt] Mặc định "Tất cả" mỗi lần vào màn; không ghi nhớ giữa hai lần vào. |
| | 3 | Đếm kết quả | Điều kiện | [Điều kiện hiển thị] Chỉ hiển thị khi bảng đã tải xong; trong lúc tải và khi lỗi thì ẩn, không hiển thị `0 / 0`. |
| | 4 | Bảng học viên | Có | [Điều kiện hiển thị] Trong lúc tải hiển thị khung chờ 8 dòng. Lớp chưa có học viên nào thì hiển thị "Lớp này chưa có học viên nào — chia sẻ mã mời để học viên tham gia"; có học viên nhưng ô tìm kiếm không khớp thì hiển thị "Không tìm thấy học viên nào khớp từ khoá" — hai trạng thái rỗng khác nhau, không dùng chung một câu. |
| | 5 | Học viên | Có | [Điều kiện kích hoạt] Luôn kích hoạt sau khi tải xong; liên kết mở màn `class_student_detail`. |
| | 6 | Lớp | Điều kiện | [Điều kiện hiển thị] Chỉ hiển thị khi tab đang chọn là "Tất cả"; đang lọc theo một lớp cụ thể thì cột này thừa, ẩn đi để trả chỗ cho các cột số. Cùng quy tắc `class_management` [Nguồn: 02-bd/screens/teacher/INS0201_class_management.md:434]. |
| | 7 | Điểm TB | Có | [Điều kiện hiển thị] Học viên chưa có lượt nộp nào thì hiển thị `-`, không hiển thị `0.0`. |
| | 8 | Hoàn thành | Điều kiện | [Điều kiện hiển thị] Lớp chưa được giao bài nào thì hiển thị `-`, vì mẫu số bằng 0. Gọi `problem-bank` thất bại thì cột hiển thị `-` và bảng vẫn hiện đủ các cột còn lại. |
| | 9 | Chuỗi ngày | Có | [Điều kiện hiển thị] Chuỗi bằng 0 thì hiển thị `-`, không hiển thị `0 ngày` [Nguồn: 09-layoutBase/Giáo viên - Tiến độ học viên.dc.html:306]. |
| | 10 | Xu hướng | Điều kiện | [Điều kiện hiển thị] Chưa đủ hai mốc tuần có dữ liệu thì để ô trống, không vẽ đường thẳng giả. |
| | 11 | Hoạt động | Có | - |
| | 12 | Phân trang | Điều kiện | [Điều kiện hiển thị] Chỉ hiển thị khi tổng số dòng khớp bộ lọc vượt một trang.<br>[Tự động đặt] Đổi tab lớp hoặc đổi từ khoá tìm kiếm thì đưa về trang 1. |
| | 13 | Thử lại | Điều kiện | [Điều kiện hiển thị] Chỉ hiển thị trong khối đang ở trạng thái lỗi tải dữ liệu.<br>[Điều kiện kích hoạt] Không kích hoạt trong lúc lần tải lại đang chạy. |

[Nguồn: 09-layoutBase/Giáo viên - Tiến độ học viên.dc.html:164,288-300,306; 01-rd/req/identity.md:129-136]

---

## Sheet 7. Danh sách truyền dữ liệu

### 7.1 Trường DTO

| NO | DTO | Trường DTO | Kiểu | Bảng DB | Cột DB | Item màn | Hiển thị | Ghi chú |
| --: | :--- | :--- | :--- | :--- | :--- | :--- | :-: | :--- |
| 1 | `ClassCardDto` | `id` | UUID | `identity.classes` | `id` | Tab lọc theo lớp | Không | [Đích] Tham số lọc của `ListClassStudents`, `ListClassAttentionStudents`, `GetClassScoreTrend`. DTO dùng lại của `class_management`, không định nghĩa mới. |
| 2 | `ClassCardDto` | `name` | String | `identity.classes` | `name` | Tab lọc, cột "Lớp", chú giải biểu đồ | Có | - |
| 3 | `ClassScoreTrendDto` | `classId`, `className` | UUID, String | `identity.classes` | `id`, `name` | Chú giải lớp | Có | [Nguồn] Phản hồi của `GetClassScoreTrend`. |
| 4 | `ClassScoreTrendDto` | `points` | List | - | - | Đường điểm của lớp | Có | [Nguồn] 6 phần tử, mỗi phần tử gồm mốc tuần và điểm trung bình lớp tại mốc đó. Cơ chế sinh mốc tuần chưa chốt — xem Câu hỏi mở Q3.<br>[Chuyển đổi] Giao diện vẽ đường cong trơn qua 6 điểm. |
| 5 | `ClassAttentionStudentDto` | `studentId`, `classId` | UUID | `identity.class_enrollments` | `student_id`, `class_id` | - | Không | [Đích] Tham số điều hướng sang `class_student_detail`. |
| 6 | `ClassAttentionStudentDto` | `displayName` | String | `identity.users` | `display_name` | Khối "Cần chú ý" — "Học viên" | Có | - |
| 7 | `ClassAttentionStudentDto` | `status` | Enum | - | - | Khối "Cần chú ý" — "Trạng thái" | Có | [Chuyển đổi] Máy chủ trả một trong năm mã thống nhất cả cụm lớp — `INSUFFICIENT_DATA` / `ABSENT` / `NEEDS_SUPPORT` / `WATCH` / `ON_TRACK` — xét theo đúng thứ tự nghiêm trọng đó, dừng ở điều kiện đầu tiên khớp; giao diện đổi sang "Chưa đủ dữ liệu" / "Vắng bài" / "Cần hỗ trợ" / "Theo dõi" / "Đang tốt". Khối "Cần chú ý" chỉ liệt kê học viên rơi vào bốn mã khác `ON_TRACK` (học viên "Đang tốt" không cần chú ý). Bộ mã, tên và thứ tự chốt theo `DEC-2026-0921-teacher-screens-conflict-resolutions`, đồng bộ với `class_management` [Nguồn: 02-bd/screens/teacher/INS0201_class_management.md:363] — đóng Câu hỏi mở Q4. |
| 8 | `ClassAttentionStudentDto` | `reasonMetric` | Number | - | - | Khối "Cần chú ý" — "Lý do cần chú ý" | Có | [Chuyển đổi] Máy chủ trả con số thật đã kích hoạt ngưỡng (số ngày vắng, phần trăm hoàn thành, hoặc số tuần giảm liên tiếp); giao diện ghép thành câu theo `status`. Không trả câu tiếng Việt dựng sẵn từ máy chủ, để còn đa ngôn ngữ. |
| 9 | `ClassProgressStudentRowDto` | `studentId` | UUID | `identity.class_enrollments` | `student_id` | - | Không | [Đích] Tham số điều hướng sang `class_student_detail`. |
| 10 | `ClassProgressStudentRowDto` | `displayName` | String | `identity.users` | `display_name` | Bảng "Học viên" | Có | [Chuyển đổi] Sinh thêm ô chữ cái đầu ở phía giao diện, không lưu trong DB. |
| 11 | `ClassProgressStudentRowDto` | `classId`, `className` | UUID, String | `identity.classes` | `id`, `name` | Bảng "Lớp" | Có | [Chuyển đổi] Cột ẩn khi đang lọc theo một lớp cụ thể. |
| 12 | `ClassProgressStudentRowDto` | `avgScore` | Number | `judge.submissions` | `status`, `problem_id`, `user_id` | Bảng "Điểm TB" | Có | [Chuyển đổi] Làm tròn một chữ số thập phân, thang 10; chưa có lượt nộp nào thì trả rỗng và giao diện hiển thị `-`. Phạm vi tính chưa khớp F1-28 — xem Câu hỏi mở Q1. |
| 13 | `ClassProgressStudentRowDto` | `completionPercent` | Number | `identity.user_problem_best_score` | `best_verdict` | Bảng "Hoàn thành" | Có | [Công thức] `identity` tính tại chỗ: mẫu số là danh sách bài đã giao lấy qua `ListClassAssignments`, tử số là số bài trong danh sách đó có `best_verdict = ACCEPTED` của học viên. Không gọi `GetClassAssignmentCompletion` — endpoint đó đã bỏ theo `DEC-2026-0921-class-completion-owned-by-identity`. |
| 14 | `ClassProgressStudentRowDto` | `streakDays` | Number | - | - | Bảng "Chuỗi ngày" | Có | [Nguồn] Phản hồi của `GetStudentSubmissionMetrics` (`judge-orchestration`), xem Câu hỏi mở Q2. |
| 15 | `ClassProgressStudentRowDto` | `trendPoints` | List | - | - | Bảng "Xu hướng" | Có | [Nguồn] Cùng nguồn với NO 4, tính theo cá nhân thay vì gộp lớp.<br>[Chuyển đổi] 6 phần tử; dưới 2 phần tử thì giao diện để ô trống. |
| 16 | `ClassProgressStudentRowDto` | `lastActiveAt` | Date | `identity.identity_recent_activity` | `occurred_at` | Bảng "Hoạt động" | Có | [Chuyển đổi] Giao diện đổi sang khoảng thời gian tương đối. |
| 17 | `ClassProgressStudentRowDto` | `totalCount`, `pageIndex`, `pageSize` | Number | - | - | "Đếm kết quả", "Phân trang" | Có | [Nguồn] Máy chủ trả kèm mỗi trang. `totalCount` là số dòng khớp **cả** tab và từ khoá; mẫu số của dòng đếm kết quả là số học viên trong phạm vi tab, trả riêng. |

### 7.2 Truy cập bảng dữ liệu (5)

| NO | Tên logic | Bảng | Repository | CRUD | Mục đích | Ghi chú |
| --: | :--- | :--- | :--- | :-: | :--- | :--- |
| 1 | Lớp học | `identity.classes` | `ClassRepository` | R | Dựng cụm tab lọc, tên lớp ở cột "Lớp" và chú giải biểu đồ | `ListInstructorClasses`: R |
| 2 | Ghi danh lớp | `identity.class_enrollments` | `ClassEnrollmentRepository` | R | Xác định tập học viên của từng lớp phụ trách | `ListClassStudents`: R<br>`ListClassAttentionStudents`: R<br>`GetClassScoreTrend`: R |
| 3 | Người dùng | `identity.users` | `UserRepository` | R | Tên hiển thị học viên và lọc theo từ khoá tìm kiếm | `ListClassStudents`: R<br>`ListClassAttentionStudents`: R |
| 4 | Lượt nộp bài | `judge.submissions` | qua cổng ra `StudentSubmissionMetricsPort` | R | Tính Điểm TB trong phạm vi lớp (giới hạn theo tập bài đã giao) | `GetStudentSubmissionMetrics`: R |
| 5 | Hoạt động gần đây | `identity.identity_recent_activity` | `IdentityRecentActivityRepository` | R | Mốc hoạt động gần nhất cho cột "Hoạt động" | `ListClassStudents`: R |

Màn này **chỉ đọc** — không có dòng nào mang `C`, `U` hay `D`. Đây là khác biệt lớn nhất so với
`class_management`, nơi có hai thao tác xoá thật.

`[Suy luận]` — tên repository do BD này đề xuất, trùng tên với đề xuất của `class_management`
[Nguồn: 02-bd/screens/teacher/INS0201_class_management.md:494-499]; DD module `identity` chốt lại.

### 7.3 Danh sách endpoint [Nội bộ]

> Chỉ tên nghiệp vụ và BC sở hữu. Hợp đồng chi tiết thuộc `03-dd/api/identity.md`.

| NO | Endpoint (tên nghiệp vụ) | Mục đích | BC sở hữu |
| --: | :--- | :--- | :--- |
| 1 | `ListInstructorClasses` | Tải danh sách lớp phụ trách để dựng cụm tab lọc | `identity` |
| 2 | `ListClassStudents` | Tải danh sách học viên theo lớp hoặc toàn bộ lớp phụ trách, có tìm kiếm và phân trang | `identity` |
| 3 | `ListClassAttentionStudents` | Tải danh sách học viên có dấu hiệu tụt lại cho khối "Cần chú ý" | `identity` |
| 4 | `GetClassScoreTrend` | Tải chuỗi điểm trung bình theo 6 mốc tuần của từng lớp | `identity` |
| 5 | `ListClassAssignments` | Cung cấp danh sách bài đã giao cho lớp; `identity` tự tính tỉ lệ Hoàn thành từ danh sách này kết hợp `user_problem_best_score`. Thay cho `GetClassAssignmentCompletion` (đã bỏ, `DEC-2026-0921-class-completion-owned-by-identity`) | `problem-bank` |
| 6 | `GetStudentSubmissionMetrics` | Cung cấp chuỗi ngày, mốc nộp gần nhất và chuỗi điểm theo tuần của một tập học viên | `judge-orchestration` |

Ghi chú ranh giới:

- Endpoint 1, 2 và 5 **dùng lại đúng tên nghiệp vụ** mà `class_management` đã đặt
  [Nguồn: 02-bd/screens/teacher/INS0201_class_management.md:514,520,522] — cùng một nghiệp vụ thì cùng
  một tên, không tách endpoint riêng cho màn này. Màn này cần thêm ba tham số cho endpoint 2 (từ khoá
  tìm kiếm, phân trang, yêu cầu kèm chuỗi ngày và xu hướng); DD quyết định là tham số tuỳ chọn trên cùng
  một endpoint hay hai biến thể.
- Endpoint 3 và 4 là **mới**, chỉ màn này dùng.
- Endpoint 5 và 6 thuộc module khác, gọi qua cổng ra, **không truy vấn chéo schema**. Cả hai chưa được
  thiết kế ở BD của module sở hữu — xem Câu hỏi mở Q2 và Q8.
- Màn này **không gọi** `ai-review`, nên không chịu ảnh hưởng khi phân hệ AI hỏng.

[Nguồn: 02-bd/database/identity.md:85-101,113-116; 02-bd/database/problem-bank.md:116-128; 02-bd/database/judge-orchestration.md:8-34]

---

## Sheet 8. Danh sách sự kiện

> Bộ thẻ và quy ước cột `Chuyển màn`: `02-bd/_rules/bd-template-9sheet.md` mục 2, 3.

### Màn chính: Tiến độ lớp

| NO | Loại | Sự kiện | Chi tiết | Chuyển màn | Gọi API | Tên xử lý | Ghi chú |
| --: | :--- | :--- | :--- | :-: | :-: | :--- | :--- |
| 1 | Màn hình | Khởi tạo màn | Vào màn thì tải danh sách lớp, biểu đồ, khối "Cần chú ý" và trang đầu bảng học viên. | Không | Có | `ListInstructorClasses`, `GetClassScoreTrend`, `ListClassAttentionStudents`, `ListClassStudents` | [Các bước]<br>1. Kiểm tra quyền `CLASS_MANAGEMENT:READ`.<br>2. Hiển thị khung chờ cho ba khối nội dung.<br>3. Tải danh sách lớp trước để dựng cụm tab, rồi tải song song ba khối còn lại ở bộ lọc "Tất cả", trang 1, từ khoá rỗng.<br>[Khi thành công] Hiển thị đủ hàng đầu, biểu đồ, khối "Cần chú ý" và bảng học viên.<br>[Khi lỗi] Hiển thị lỗi **tại đúng khối tải thất bại** kèm nút "Thử lại"; các khối tải được vẫn hiển thị bình thường, không rời màn. Không có lớp nào thì thay cả ba khối bằng trạng thái rỗng "Chưa có lớp nào — tạo lớp ở màn Lớp của tôi". |
| 2 | Nút | Chọn tab lọc lớp | Bấm một tab trong cụm tab lọc. | Không | Có | `GetClassScoreTrend`, `ListClassAttentionStudents`, `ListClassStudents` | [Các bước]<br>1. Đặt trạng thái lọc bằng tab được chọn, đưa phân trang về trang 1, giữ nguyên từ khoá tìm kiếm.<br>2. Tải lại **cả ba khối** theo phạm vi mới.<br>[Khi thành công] Biểu đồ chỉ còn đường của lớp đang chọn, khối "Cần chú ý" chỉ còn học viên của lớp đó [Nguồn: DEC-2026-0830-class-progress-dashboard], bảng đổi phạm vi và cột "Lớp" ẩn đi. Chọn "Tất cả" thì hiện lại cột "Lớp" và vẽ lại mọi đường.<br>[Khi lỗi] Giữ nguyên tab đã chọn, hiển thị lỗi ở khối thất bại kèm nút "Thử lại"; không tự quay về tab cũ vì như vậy người dùng không hiểu chuyện gì xảy ra. |
| 3 | Ô nhập | Tìm kiếm học viên | Gõ vào ô "Tìm học viên". | Không | Có | `ListClassStudents` | [Các bước]<br>1. Chờ ngừng gõ 300 mili giây `[Suy luận]` rồi mới gửi yêu cầu, tránh gọi máy chủ mỗi phím.<br>2. Đưa phân trang về trang 1, giữ nguyên tab lớp.<br>3. Tải lại **chỉ bảng học viên**; biểu đồ và khối "Cần chú ý" không đổi, xem Câu hỏi mở Q7.<br>[Khi thành công] Bảng chỉ còn học viên khớp từ khoá, dòng đếm kết quả cập nhật tử số.<br>[Khi lỗi] Giữ nguyên nội dung ô tìm kiếm, hiển thị lỗi trong khối bảng kèm nút "Thử lại". |
| 4 | Nút | Chuyển trang bảng học viên | Bấm số trang hoặc nút trước/sau ở phân trang. | Không | Có | `ListClassStudents` | [Các bước]<br>1. Tải trang được chọn với nguyên tab lớp và từ khoá hiện tại.<br>2. Cuộn khối bảng về đầu bảng.<br>[Khi thành công] Bảng đổi nội dung tại chỗ; biểu đồ và khối "Cần chú ý" không tải lại vì chúng tính trên toàn bộ phạm vi, không theo trang.<br>[Khi lỗi] Giữ nguyên trang đang hiển thị, hiển thị lỗi kèm nút "Thử lại". |
| 5 | Nút | Thử lại một khối | Bấm "Thử lại" trong khối đang lỗi. | Không | Có | Đúng endpoint của khối đó | [Các bước]<br>1. Chuyển khối đó về trạng thái khung chờ.<br>2. Gọi lại đúng endpoint của khối, giữ nguyên tab lớp, từ khoá và số trang hiện tại.<br>[Khi thành công] Khối hiển thị dữ liệu, các khối khác không bị tải lại.<br>[Khi lỗi] Giữ trạng thái lỗi, nút "Thử lại" kích hoạt trở lại. |
| 6 | Liên kết | Mở hồ sơ học viên từ bảng | Bấm tên một học viên trong bảng danh sách. | Có | Không | - | [Các bước]<br>1. Điều hướng sang màn `class_student_detail` kèm `id` học viên và `id` lớp của chính dòng đó.<br>[Khi thành công] Mở màn chi tiết học viên trong phạm vi đúng lớp. Màn này chỉ đọc, không có thay đổi chưa lưu, nên **không** hỏi xác nhận trước khi rời; tab lọc, từ khoá và số trang hiện tại không được khôi phục khi quay lại, xem Câu hỏi mở Q6. |
| 7 | Liên kết | Mở hồ sơ học viên từ khối "Cần chú ý" | Bấm tên một học viên trong khối "Cần chú ý". | Có | Không | - | [Các bước]<br>1. Điều hướng sang màn `class_student_detail` kèm `id` học viên và `id` lớp của mục đó.<br>[Khi thành công] Mở đúng màn với đúng bộ tham số như EVT-6 — hai đường vào không được cho ra hai màn khác nhau. Không hỏi xác nhận trước khi rời. |

[Nguồn: 09-layoutBase/Giáo viên - Tiến độ học viên.dc.html:157,161,288-300; 01-rd/req/identity.md:129-136; DEC-2026-0830-class-progress-dashboard]

---

## Sheet 9. Đặc tả kiểm tra

> Bộ thẻ và quy ước mã thông báo: `02-bd/_rules/bd-template-9sheet.md` mục 2, 4. Sheet này ghi **điều kiện
> kiểm và thông báo**; tiêu chí nghiệm thu `AC-nn` thuộc `04-tdd/class_progress.md`, không lặp lại ở đây.

| NO | Loại | Tóm tắt kiểm | Chi tiết | Mức | Mã thông báo | Ghi chú | EVT gọi | Thứ tự |
| --: | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :-: |
| 1 | Kiểm quyền | Quyền truy cập màn | [Nội dung kiểm] Người dùng không có Function `CLASS_MANAGEMENT` thì không được vào màn.<br>[Nơi thực thi] Chặn ở cả tầng định tuyến phía giao diện và tầng phân quyền phía máy chủ, không chỉ ẩn giao diện. | Lỗi | Chưa có mã thông báo | Nội dung "Bạn không có quyền truy cập chức năng này." [Nguồn: 01-rd/req/identity.md:137-138] | EVT-1 | 1 |
| 2 | Kiểm quyền | Phạm vi lớp phụ trách | [Nội dung kiểm] Mọi truy vấn chỉ trả dữ liệu của lớp có `instructor_id` bằng người đang đăng nhập; truyền `class_id` của lớp không phụ trách thì từ chối.<br>[Nơi thực thi] Máy chủ, trên từng yêu cầu — không dựa vào việc giao diện chỉ dựng tab của lớp mình. | Lỗi | Chưa có mã thông báo | Nội dung "Bạn không phụ trách lớp này." Áp cho `ListClassStudents`, `ListClassAttentionStudents`, `GetClassScoreTrend` và cho cả hai endpoint liên module. | EVT-2, EVT-3, EVT-4, EVT-5 | 1 |
| 3 | Kiểm quyền | Không lộ dữ liệu chấm chi tiết | [Nội dung kiểm] Phản hồi của màn này **chỉ** chứa số liệu tổng hợp; không chứa mã nguồn bài nộp, dữ liệu testcase ẩn, hay kết quả so khớp từng testcase.<br>[Nơi thực thi] Máy chủ, ở tầng dựng DTO. | Lỗi | Mã lỗi trong phản hồi | Ràng buộc **Bắt buộc** của dự án; endpoint `GetStudentSubmissionMetrics` chỉ được trả số đếm và mốc thời gian, không trả nội dung lượt nộp. | EVT-1, EVT-2, EVT-3, EVT-4 | 2 |
| 4 | Kiểm nhập liệu | Độ dài từ khoá tìm kiếm | [Nội dung kiểm] Từ khoá quá 100 ký tự thì cắt bớt và không gửi phần thừa.<br>[Nơi thực thi] Màn hình và máy chủ.<br>[Tiêu điểm] Ô tìm kiếm học viên. | Cảnh báo | Chưa có mã thông báo | Nội dung "Từ khoá tìm kiếm quá dài." Giới hạn 100 lấy theo độ dài tên hiển thị `[Suy luận]`, DD chốt lại. | EVT-3 | 1 |
| 5 | Kiểm nhập liệu | Số trang hợp lệ | [Nội dung kiểm] Số trang nhỏ hơn 1 hoặc lớn hơn tổng số trang thì đưa về trang hợp lệ gần nhất, không gọi máy chủ với giá trị sai.<br>[Nơi thực thi] Màn hình và máy chủ.<br>[Tiêu điểm] Điều khiển phân trang. | Cảnh báo | Chưa có mã thông báo | Xảy ra thật khi bộ lọc thu hẹp làm tổng số trang giảm trong lúc người dùng đang ở trang cuối. | EVT-4 | 1 |
| 6 | Kiểm nghiệp vụ | Mẫu số bằng 0 | [Nội dung kiểm] Không được chia cho 0 ở cả hai công thức: "Điểm TB" khi `total_submissions` bằng 0, và "Hoàn thành" khi lớp chưa được giao bài nào. Cả hai trường hợp hiển thị `-`.<br>[Nơi thực thi] Máy chủ trả rỗng, màn hình hiển thị `-`. | Cảnh báo | Chưa có mã thông báo | Hiển thị `0.0` hay `0%` trong hai trường hợp này là sai nghiệp vụ — nó khiến giảng viên tưởng học viên làm bài mà không đạt, trong khi thực tế chưa có gì để tính. | EVT-1, EVT-2, EVT-3, EVT-4 | 1 |
| 7 | Kiểm nghiệp vụ | Dữ liệu chưa đủ để vẽ xu hướng | [Nội dung kiểm] Dưới hai mốc tuần có dữ liệu thì không vẽ đường và không tính nhãn "Cần hỗ trợ" — điểm giảm liên tiếp cần tối thiểu hai mốc.<br>[Nơi thực thi] Máy chủ khi tính nhãn, màn hình khi vẽ. | Cảnh báo | Chưa có mã thông báo | Nội dung "Chưa đủ dữ liệu để vẽ xu hướng." Lớp mới mở luôn rơi vào trường hợp này trong 2 tuần đầu. | EVT-1, EVT-2 | 2 |
| 8 | Kiểm nghiệp vụ | Học viên bị gỡ khỏi lớp ở phiên khác | [Nội dung kiểm] Học viên đã bị gỡ khỏi lớp (F1-26, xoá thật) thì biến mất khỏi mọi khối ở lần tải tiếp theo; bấm vào một mục đã cũ thì màn đích báo không tìm thấy.<br>[Nơi thực thi] Máy chủ. | Lỗi | Mã lỗi trong phản hồi | Nội dung "Học viên này không còn thuộc lớp. Vui lòng tải lại màn hình." Cần vì gỡ học viên là xoá thật, không có bản ghi mềm để phát hiện [Nguồn: 02-bd/database/identity.md:95-101]. | EVT-6, EVT-7 | 1 |
| 9 | Kiểm nghiệp vụ | Một khối lỗi không kéo đổ cả màn | [Nội dung kiểm] Gọi `problem-bank` hoặc `judge-orchestration` thất bại thì chỉ các cột phụ thuộc nguồn đó hiển thị `-`; bảng, biểu đồ và khối "Cần chú ý" vẫn hiển thị phần dữ liệu lấy được.<br>[Nơi thực thi] Màn hình. | Cảnh báo | Chưa có mã thông báo | Không hiện hộp lỗi toàn màn. Thiếu dữ liệu chuỗi ngày và xu hướng thì nhãn "Cần hỗ trợ" tạm không tính được — khối "Cần chú ý" phải nói rõ "Thiếu dữ liệu xu hướng" thay vì im lặng bỏ sót học viên. | EVT-1, EVT-2, EVT-5 | 2 |
| 10 | Kiểm nghiệp vụ | Lỗi hệ thống hoặc lỗi gọi máy chủ | [Nội dung kiểm] Gọi máy chủ thất bại hoặc trả lỗi nghiệp vụ thì dừng thao tác, giữ nguyên dữ liệu đang hiển thị và hiện nút "Thử lại" ở đúng khối.<br>[Nơi thực thi] Màn hình. | Lỗi | Mã lỗi trong phản hồi | Phản hồi có mã lỗi đã đăng ký thì hiển thị nội dung tương ứng; chưa đăng ký thì hiển thị "Không kết nối được máy chủ." | EVT-1, EVT-2, EVT-3, EVT-4, EVT-5 | 1 |

Cột `Thứ tự` là thứ tự kiểm trong cùng một sự kiện.

[Nguồn: 01-rd/req/identity.md:129-138; 02-bd/database/identity.md:95-101,113-116]

---

## Câu hỏi mở

| # | Câu hỏi | Vì sao chưa trả lời được | Chủ sở hữu |
| :-: | :--- | :--- | :--- |
| Q1 | ~~Điểm TB là điểm toàn hệ thống hay điểm trong phạm vi lớp?~~ **ĐÃ CHỐT 2026-09-21** bởi `DEC-2026-0921-teacher-screens-conflict-resolutions`: Điểm TB tính **đúng theo phạm vi lớp**, theo đúng câu chữ của F1-28 — RD đứng bậc P2 trên thang SoT, cao hơn BD (P3), nên khi hai bên lệch thì BD phải theo RD chứ không phải ngược lại. Cách tính: giới hạn tập lượt nộp theo danh sách bài đã giao cho lớp rồi đếm qua `GetStudentSubmissionMetrics` của `judge-orchestration`. **Không** thêm chiều `class_id` vào `user_submission_stats`: danh sách bài đã giao thay đổi theo thời gian nên read model sẽ sai ngay khi giảng viên gỡ một bài. `user_submission_stats` vẫn giữ nguyên cho mục đích toàn cục của nó (F1-07), chỉ là không còn là nguồn của màn này. Mục 4.3, Sheet 5, Sheet 7.1 và 7.2 đã cập nhật. | Đã đóng | Đã đóng |
| Q2 | **Chuỗi ngày, mốc nộp gần nhất và chuỗi điểm theo tuần lấy từ đâu?** Không bảng nào trong `02-bd/database/identity.md` mục 1.12 lưu ba giá trị này; nguồn duy nhất là `judge.submissions` [Nguồn: 02-bd/database/judge-orchestration.md:8-34], thuộc module khác. Đề xuất: `judge-orchestration` cung cấp **một** endpoint `GetStudentSubmissionMetrics` nhận danh sách `user_id` kèm tập `problem_id` giới hạn theo lớp, trả về gộp: tổng nộp, số Accepted, chuỗi ngày, mốc nộp gần nhất và chuỗi điểm 6 mốc tuần. Gộp một lần gọi thay vì bốn, vì cả bốn con số đều quét cùng một tập dòng. Endpoint này chưa tồn tại ở BD của `judge-orchestration`. | Màn này cần 4 chỉ số mà `identity` không sở hữu dữ liệu gốc | BD/DD `judge-orchestration` |
| Q3 | **"Mốc tuần" là ảnh chụp định kỳ hay tính lại lúc chạy?** F1-28 để ngỏ [Nguồn: 01-rd/req/identity.md:126-128] và không bảng nào đang lưu ảnh chụp theo tuần. Đề xuất: **tính lại lúc chạy** từ `judge.submissions` gom nhóm theo tuần, kèm cache Redis TTL 15 phút — 6 mốc tuần trên vài chục học viên là truy vấn nhỏ, còn thêm bảng ảnh chụp là thêm bảng, thêm job định kỳ và thêm rủi ro job chết thì biểu đồ đứng im. Chỉ chuyển sang ảnh chụp khi đo được là truy vấn chậm. Kèm theo: phải chốt tuần bắt đầu từ thứ Hai hay Chủ nhật, và mốc tuần hiện tại có tính phần dở dang hay không. | Cơ chế lưu quyết định cả chi phí truy vấn lẫn độ chính xác của nhãn "Cần hỗ trợ" | BD `judge-orchestration` + Chủ dự án |
| Q4 | ~~Bộ nhãn trạng thái học viên đang lệch giữa hai màn cùng cụm.~~ **ĐÃ CHỐT** theo `DEC-2026-0921-teacher-screens-conflict-resolutions`, đồng bộ với `INS0201_class_management.md`: bộ nhãn dùng chung cho cả cụm lớp là **năm** giá trị, xét theo đúng thứ tự nghiêm trọng, dừng ở điều kiện đầu tiên khớp — `INSUFFICIENT_DATA` "Chưa đủ dữ liệu" (chưa có lượt nộp nào trong lớp), `ABSENT` "Vắng bài" (không nộp trong 7 ngày gần nhất), `NEEDS_SUPPORT` "Cần hỗ trợ" (Điểm TB giảm từ 2 mốc tuần liên tiếp), `WATCH` "Theo dõi" (Hoàn thành dưới 50%), còn lại `ON_TRACK` "Đang tốt" [Nguồn: 02-bd/screens/teacher/INS0201_class_management.md:363]. Đề xuất bốn giá trị trước đây của BD này (thiếu `INSUFFICIENT_DATA`) bị thay thế. Khối "Cần chú ý" của màn này chỉ liệt kê bốn giá trị khác `ON_TRACK`; Sheet 7.1 đã cập nhật. | Đã đóng | Đã đóng |
| Q5 | **Biểu đồ vẽ tối đa bao nhiêu đường khi chọn tab "Tất cả"?** Prototype chỉ vẽ 2 đường cho 3 lớp [Nguồn: 09-layoutBase/Giáo viên - Tiến độ học viên.dc.html:131-132,288] — bản thân prototype đã không nhất quán. Một giảng viên 8 lớp thì biểu đồ 8 đường không đọc được. Đề xuất: ở tab "Tất cả" vẽ **một đường tổng** của toàn bộ học viên phụ trách thay vì mỗi lớp một đường; chọn tab lớp cụ thể thì vẽ đúng một đường của lớp đó. Như vậy biểu đồ luôn có đúng một đường, chú giải cũng đơn giản theo. | Prototype vẽ số lớp khác số đường, không có nguồn nào chốt giới hạn | Chủ dự án |
| Q6 | **Trạng thái lọc có ghi vào URL không?** Màn này có ba trạng thái lọc (tab lớp, từ khoá, số trang) và có liên kết rời màn sang `class_student_detail`. Không ghi vào URL thì quay lại bằng nút Back của trình duyệt sẽ mất hết bộ lọc, giảng viên đang duyệt trang 4 phải lọc lại từ đầu. Đề xuất: đưa cả ba vào query string, giống cách `problem_list` cần làm cho F2-11. | Chưa có quy ước chung về trạng thái lọc trên URL cho toàn dự án | DD màn hình |
| Q7 | **Ô tìm kiếm có tác động lên khối "Cần chú ý" và biểu đồ không?** `DEC-2026-0830` chỉ nói khối "Cần chú ý" phải lọc theo **tab lớp** [Nguồn: DEC-2026-0830-class-progress-dashboard], không nói gì về ô tìm kiếm. BD này thiết kế: ô tìm kiếm **chỉ** tác động lên bảng `[Suy luận]` — khối "Cần chú ý" là danh sách cảnh báo, lọc nó theo từ khoá sẽ giấu mất đúng những học viên giảng viên cần thấy. Cần xác nhận. | Quyết định không nhắc tới tương tác giữa hai bộ lọc | Chủ dự án |
| Q8 | ~~`GetClassAssignmentCompletion` vẫn chưa tồn tại.~~ **ĐÃ CHỐT 2026-09-21** bởi `DEC-2026-0921-class-completion-owned-by-identity`: **bỏ hẳn** `GetClassAssignmentCompletion`. `problem-bank` chỉ cung cấp danh sách bài đã giao qua `ListClassAssignments` (endpoint đã tồn tại sẵn, không phải viết mới), `identity` tự tính tỉ lệ từ danh sách đó kết hợp read model `user_problem_best_score` mà chính nó đã sở hữu. Ba lý do: (1) không đẻ endpoint liên module mới; (2) `identity` đằng nào cũng phải lấy danh sách bài đã giao để tính Điểm TB theo lớp (`DEC-2026-0921-teacher-screens-conflict-resolutions` mục 3), nên tính thêm Hoàn thành từ cùng danh sách là miễn phí; (3) đúng chủ sở hữu nghiệp vụ — tiến độ học viên là việc của `identity` (F1-06, F1-28), `problem-bank` chỉ sở hữu "lớp này được giao bài nào"; phương án cũ bắt `problem-bank` đọc ngược hai bảng của `identity`. | Đã đóng | Đã đóng |
| Q9 | **Dòng đếm kết quả lấy mẫu số ở đâu?** Prototype ghi cứng `students.length + ' / 82 học viên'` [Nguồn: 09-layoutBase/Giáo viên - Tiến độ học viên.dc.html:320] — số 82 là hằng số, không tính từ dữ liệu. BD này thiết kế mẫu số là số học viên trong phạm vi tab đang chọn (đổi theo tab), tức là chọn tab một lớp thì mẫu số giảm. Cách hiểu khác là mẫu số luôn là tổng học viên toàn bộ lớp phụ trách. Đề xuất: theo phạm vi tab — "12 / 30 học viên" trong một lớp 30 người dễ hiểu hơn "12 / 82". | Prototype dùng hằng số nên không suy ra được ý định | Chủ dự án |
