# RD (Yêu cầu hệ thống mới) — Chi tiết lớp của tôi / `USR0702`

> Mã màn hình: `USR0702`, theo `02-bd/_rules/bd-template-9sheet.md` mục 8 (nhóm 07 "Lớp học" của khu USR).
> Slug chính tắc: `my_class_detail`, route `/classes/{classId}`. Đăng ký vào `01-rd/overview/system_survey.md`
> mục 7.1 cùng đợt viết file này (2026-10-23, `DEC-2026-1023-student-class-screens`).
> Phạm vi/Bounded Context: `identity` (F1) là chính; đọc danh sách bài được giao của `problem-bank` (F2-12) và
> trạng thái "đã xong" từ kết quả nộp bài của học viên.
> Actor: A1 (`STUDENT`). Màn **chỉ đọc**.
> Nguồn sự thật (SoT): `01-rd/req/F1_identity.md` (F1-23, F1-26, F1-28), `01-rd/req/F2_problem-bank.md` (F2-12),
> `01-rd/req/user_stories/a1_student.md` (`US-A1-10`, viết cùng đợt).
> Màn **không có mockup trong `09-layoutBase/`** — bản dựng prototype ngày 2026-10-23 là bằng chứng duy nhất về hình.
> Tài liệu này có thể đọc độc lập.

---

## 1. Mục đích và bối cảnh

Cho học viên xem **mọi thứ về một lớp mình đang học** ở một chỗ: thông tin lớp (giảng viên, lịch học, ngày tham gia),
tiến độ làm bài được giao và bảng các bài được giao kèm trạng thái đã xong / chưa làm, từ đó mở thẳng từng bài để
làm [SoT: 01-rd/req/F2_problem-bank.md:48-54].

RD hiện hành chỉ cho bài được giao hiện thành một khối trong danh sách bài toán và nhấn mạnh "không cần mở màn
khác" [SoT: 01-rd/req/user_stories/a1_student.md:41-42]. Màn này **mở rộng** chứ không thay thế: khối đó giữ nguyên;
màn này thêm chỗ xem đầy đủ theo từng lớp (mọi bài được giao chứ không bị cắt, kèm lịch học và tiến độ). Có mở rộng
F1-23/F2-12 hay cấp mã riêng — xem Q1 của `USR0701`.

File này mô tả **hành vi và UX ở mức yêu cầu**; không lặp lại đặc tả F2-12/F1-28, chỉ trỏ tới.

---

## 2. Phạm vi

| Hành vi | Mã | Nguồn |
| :--- | :--- | :--- |
| Bài được giao cho lớp, hiện cho học viên của lớp | F2-12 | `01-rd/req/F2_problem-bank.md:48-54` |
| Hoàn thành = bài được giao mà học viên đã `Accepted` ít nhất một lần | F1-28 (cùng cách tính) | `01-rd/req/F1_identity.md:169-171` |
| Học viên đã rời lớp không còn là thành viên | F1-26 | `01-rd/req/F1_identity.md:152` |
| Thông tin lớp: tên, mô tả, lịch học | F1-23 | `01-rd/req/F1_identity.md:129-132` |

Các khối màn, theo bản dựng prototype (`05-coding/frontend/src/views/users/my-class-detail/ui/my-class-detail-view.tsx`):

1. **Đầu trang** — nút mũi tên quay lại `/classes`, tên lớp, mô tả (nếu có) (`my-class-detail-view.tsx:28-30,100-110`).
2. **Thẻ "Thông tin lớp"** (cột trái) — giảng viên, lịch học (hiện "Chưa có lịch học" nếu trống), ngày tham gia
   (`:92-96,114-124`).
3. **Thẻ "Tiến độ bài được giao"** (cột trái) — thanh tiến độ và dòng "Đã xong x/y bài" (`:125-139`).
4. **Bảng "Bài được giao"** (cột phải) — cột Bài toán (liên kết sang bài), Ngày giao, Trạng thái (Đã xong / Chưa
   làm) (`:57-90,142-155`).
5. **Trạng thái rỗng** — khi giảng viên chưa giao bài: câu "Giảng viên chưa giao bài nào." thay cho bảng (`:144-145`).
6. **Trạng thái "không tìm thấy"** — một câu thông báo kèm nút quay lại (`:39-48`).

### 2.1 Cấu trúc hệ thống liên quan

| Màn/tài liệu | Vai trò | Bounded Context |
|---|---|---|
| Màn hiện tại | `USR0702` / `my_class_detail` | `identity`, `problem-bank` |
| Màn anh em | `USR0701` / `my_classes` — danh sách lớp và nhập mã mời | `identity` |
| Màn đích khi bấm tên bài | màn chi tiết bài toán `/problems/{id}` (`problem_overview`) | `problem-bank` |
| Màn phía giảng viên | `INS0202_class_assignments` — nơi giao và gỡ bài | `problem-bank` |

**Chi tiết trạng thái và cấu trúc màn**

| Thành phần | Vai trò | Nguồn |
| :--- | :--- | :--- |
| `my-class-detail-view.tsx` | View của màn | [SoT: 05-coding/frontend/src/views/users/my-class-detail/ui/my-class-detail-view.tsx] |
| `app/(student)/classes/[classId]/page.tsx` | Route `/classes/{classId}` | [SoT: 05-coding/frontend/src/app/(student)/classes/[classId]/page.tsx] |
| Entity `class` — `MyClass`, `MyClassAssignment` | Kiểu dữ liệu | [SoT: 05-coding/frontend/src/entities/class/model/types.ts:120-136] |
| Thông điệp `myClasses.detail`, `myClasses.card` | Câu chữ giao diện | [SoT: 05-coding/frontend/messages/vi.json:2990-3011] |

---

## 3. Ngoài phạm vi (Out of Scope)

- **Học viên tự rời lớp** — chưa có yêu cầu; F1-26 chỉ nói giảng viên gỡ học viên
  [SoT: 01-rd/req/F1_identity.md:152]. Màn không có nút rời lớp (`my-class-detail-view.tsx:3-4`).
- **Xem bạn cùng lớp hoặc điểm của học viên khác** — hồ sơ học viên chỉ cho giảng viên (F1-27, actor A2)
  [SoT: 01-rd/req/F1_identity.md:153-160].
- **Sửa thông tin lớp** — màn chỉ đọc.
- **Thông tin liên hệ giảng viên** ngoài tên hiển thị.
- **Hạn nộp bài** — bài giao theo lớp không có hạn nộp [SoT: 02-bd/screens/users/USR0101_problem_list.md:233]; bảng
  không có cột hạn.
- **Cập nhật thời gian thực** — không có yêu cầu; dữ liệu tải khi mở trang.
- Bảng màu, spacing, breakpoint — thuộc BD (`02-bd/screens/users/USR0702_my_class_detail.md`, chưa viết).
- Hợp đồng API — thuộc DD; endpoint đọc chi tiết lớp của học viên chưa có (xem Q2 của `USR0701`).

---

## 4. Tiền đề và ràng buộc

> Prototype dựng 2026-10-23 trước khi có RD (chủ dự án: dựng trước, viết tài liệu sau). Chủ sở hữu mọi câu hỏi là
> **chủ dự án**.

| # | Câu hỏi | Quyết định | Ghi chú |
| :-: | :--- | :--- | :--- |
| Q1 | Bài đã giao rồi bị giảng viên **rút xuống/ẩn** (`UNPUBLISHED` hoặc ẩn mềm) thì bảng của học viên hiện thế nào? Prototype **chưa xử lý**: mọi dòng đều là liên kết bấm được (`my-class-detail-view.tsx:61-67`). Ưu tiên: **cao** (bấm vào sẽ ra 404). | **Chờ chủ dự án.** Đề xuất: bài vẫn hiện trong bảng nhưng **làm mờ, nhãn "Đã rút xuống", không bấm được**, đúng cách `INS0202` đã chốt cho giảng viên; trạng thái đã xong/chưa làm của bài đó vẫn hiện nếu có dữ liệu. Tiêu chí ponytail bậc 4 (tái dùng nhãn và quy tắc đã có). | `INS0202` chốt: bài rút vẫn hiện với nhãn "Đã rút xuống", học viên không còn thấy bài ở `problem_list`, lượt nộp giữ nguyên [SoT: 02-bd/screens/teacher/INS0202_class_assignments.md:520,543]. Hai nơi cùng một bài nên cần thống nhất cách hiện; câu hỏi là học viên có nên **thấy** dòng này hay không (đề xuất: có, để tiến độ x/y không đổi bất ngờ). |
| Q2 | Tử số/mẫu số của "Đã xong x/y bài" có tính bài đã rút xuống không? Ưu tiên: trung bình. | **Chờ chủ dự án.** Đề xuất: tính theo đúng các dòng bảng đang hiện (bài rút xuống vẫn nằm trong y), để con số khớp bảng. | `[SoT: Suy luận]` — F2-12 chỉ nói gỡ bài khỏi lớp thì ẩn khỏi danh sách giao [SoT: 01-rd/req/F2_problem-bank.md:55-60]; "rút xuống" (bài bị xuất bản lại) là trường hợp khác, chưa có RD chốt công thức. |
| Q3 | Lớp bị giảng viên **xoá** (hard delete) trong lúc học viên đang mở trang. Ưu tiên: thấp. | **Chờ chủ dự án.** Đề xuất: lần tải kế tiếp rơi vào trạng thái "không tìm thấy" (REQ-06); không cần cảnh báo thời gian thực. Ponytail bậc 3. | F1-24 xoá thật [SoT: 01-rd/req/F1_identity.md:138-141]. |
| Q4 | Nguồn tên giảng viên và mức hiển thị: chỉ tên hiển thị của chủ lớp? Ưu tiên: thấp. | **Chờ chủ dự án.** Đề xuất: chỉ `display_name` của chủ lớp, như `USR0701` Q3. | Dùng chung câu hỏi với `USR0701` Q3. |
| Q5 | Lớp không tồn tại, học viên không thuộc lớp, hoặc học viên đã rời lớp: có phân biệt thông báo không? Ưu tiên: trung bình (an ninh — ponytail bậc 6). | **Chờ chủ dự án.** Đề xuất: **không phân biệt** — cả ba trường hợp cùng một trạng thái "không tìm thấy", để không lộ lớp có tồn tại hay không; đây là yêu cầu REQ-06. | Prototype đã làm vậy (`my-class-detail-view.tsx:39-48`) nhưng đó là mock; hợp đồng DD phải trả cùng một mã cho ba trường hợp. `[SoT: Suy luận]`. |

---

## 5. Danh sách yêu cầu (REQ)

| ID | Yêu cầu | Loại | Nguồn/SoT |
|---|---|---|---|
| REQ-01 | **Cho** tôi đang là thành viên của một lớp, **Khi** tôi mở `/classes/{classId}` của lớp đó, **Thì** thấy tên lớp, mô tả (nếu có), và thẻ "Thông tin lớp" gồm giảng viên, lịch học, ngày tham gia; lớp chưa có lịch thì dòng lịch học ghi "Chưa có lịch học" | Chức năng (GWT) | F1-23 [SoT: 01-rd/req/F1_identity.md:129-132]; [SoT: 05-coding/frontend/src/views/users/my-class-detail/ui/my-class-detail-view.tsx:92-124] |
| REQ-02 | **Cho** lớp đã được giao bài, **Khi** tôi nhìn thẻ "Tiến độ bài được giao", **Thì** thấy thanh tiến độ và "Đã xong x/y bài", y là số bài được giao cho lớp, x là số bài trong đó tôi đã `Accepted` ít nhất một lần | Chức năng (GWT) | F2-12 [SoT: 01-rd/req/F2_problem-bank.md:48-54]; cùng cách tính với F1-28 [SoT: 01-rd/req/F1_identity.md:169-171]; [SoT: my-class-detail-view.tsx:50-52,125-139] |
| REQ-03 | **Cho** lớp đã được giao bài, **Khi** tôi nhìn bảng "Bài được giao", **Thì** mỗi bài một dòng với tên bài (liên kết), ngày giao và trạng thái; bảng liệt kê **toàn bộ** bài được giao cho lớp, không cắt bớt | Chức năng (GWT) | F2-12 [SoT: 01-rd/req/F2_problem-bank.md:48-54]; [SoT: my-class-detail-view.tsx:57-90,147-153] |
| REQ-04 | **Cho** một bài được giao mà tôi đã có ít nhất một lượt nộp `Accepted`, **Khi** tôi nhìn cột Trạng thái, **Thì** dòng đó ghi "Đã xong"; bài chưa có lượt `Accepted` nào ghi "Chưa làm". Bài có lượt nộp sai mà chưa `Accepted` vẫn là "Chưa làm" | Chức năng (GWT) | Quy tắc hoàn thành F1-28 [SoT: 01-rd/req/F1_identity.md:169-171]; [SoT: my-class-detail-view.tsx:85-87] |
| REQ-05 | **Cho** tôi bấm tên một bài trong bảng, **Khi** liên kết mở, **Thì** tôi sang trang chi tiết bài đó (`/problems/{id}`); trạng thái của dòng cập nhật khi tôi quay lại sau khi nộp bài `Accepted` | Chức năng (GWT) | [SoT: my-class-detail-view.tsx:62-66]; việc cập nhật khi quay lại là `[SoT: Suy luận]` — suy từ REQ-04, prototype dùng dữ liệu mock tĩnh |
| REQ-06 | **Cho** lớp không tồn tại, hoặc tôi không phải thành viên của lớp đó, hoặc tôi đã rời lớp, **Khi** tôi mở `/classes/{classId}`, **Thì** thấy cùng một thông báo "Không tìm thấy lớp này trong các lớp bạn tham gia" kèm nút quay lại; màn không cho biết lớp có tồn tại hay không | Chức năng (GWT) | F1-26 [SoT: 01-rd/req/F1_identity.md:152]; [SoT: my-class-detail-view.tsx:39-48]; [SoT: 05-coding/frontend/messages/vi.json:3004] |
| REQ-07 | **Cho** giảng viên chưa giao bài nào cho lớp, **Khi** tôi mở chi tiết lớp, **Thì** khung bài hiện câu "Giảng viên chưa giao bài nào.", không có bảng; thẻ tiến độ hiện "Đã xong 0/0 bài" với thanh rỗng | Chức năng (GWT) | [SoT: my-class-detail-view.tsx:144-146,129-138]; [SoT: 05-coding/frontend/messages/vi.json:2995] |
| REQ-08 | **Cho** tôi đang ở chi tiết lớp, **Khi** tôi bấm nút mũi tên quay lại, **Thì** tôi về `/classes` (`USR0701`) | Chức năng (GWT) | [SoT: my-class-detail-view.tsx:28-30]; [SoT: 05-coding/frontend/messages/vi.json:3003] |
| REQ-09 | **Cho** tôi mở chi tiết lớp, **Khi** màn hiển thị, **Thì** mọi dữ liệu trên màn là của chính tôi trong lớp đó; màn không hiện tên hay điểm của học viên khác và không có thao tác sửa lớp hay rời lớp | Phi chức năng (GWT) | F1-27 chỉ cho giảng viên [SoT: 01-rd/req/F1_identity.md:153-160]; [SoT: my-class-detail-view.tsx:3-4] |
| REQ-10 | **Cho** hệ thống AI (F5/F6) không khả dụng, **Khi** tôi dùng màn này, **Thì** màn hoạt động đầy đủ — màn không phụ thuộc AI | Phi chức năng (GWT) | `[SoT: Suy luận]` — màn chỉ đọc `identity`, `problem-bank` và kết quả nộp bài; nguyên tắc AI suy giảm êm |

---

## 6. Yêu cầu dữ liệu

### 6.1 Dữ liệu tham chiếu

Chi tiết bảng, cột, kiểu dữ liệu và ràng buộc lưu trữ thuộc BD/DD; tài liệu này không tự suy luận dữ liệu chưa được
chốt. Dữ liệu màn theo prototype: tên, mô tả, giảng viên, lịch học, ngày tham gia; mỗi bài được giao gồm mã bài,
tên bài, ngày giao, cờ đã xong [SoT: 05-coding/frontend/src/entities/class/model/types.ts:120-136].

### 6.2 Luồng dữ liệu

Hợp đồng API, request/response và mã lỗi thuộc DD. Endpoint đọc chi tiết lớp của học viên **chưa có** trong
`03-dd/api/F1_identity.md` (xem `USR0701` Q2); ba trường hợp ở REQ-06 phải cùng một kết quả phía hợp đồng.

---

## 7. RACI và danh sách bàn giao

| Bàn giao | Trạng thái |
|---|---|
| RD màn hình | Có tài liệu này |
| BD màn hình | Chưa viết — `02-bd/screens/users/USR0702_my_class_detail.md` |
| DD/API liên quan | Endpoint đọc chi tiết lớp của học viên chưa có |

---

## 8. Kế hoạch và quy mô

| Chỉ số | Giá trị |
|---|---|
| Quy mô hành vi | 10 yêu cầu ở chương 5 (8 chức năng, 2 phi chức năng) |
| Lịch, nhân sự, công sức | Chưa xác định — không tự suy luận. |

---

## 9. Tham chiếu

| Loại | Tham chiếu |
| :--- | :--- |
| Yêu cầu module | `01-rd/req/F1_identity.md` — F1-23, F1-26, F1-28; `01-rd/req/F2_problem-bank.md` — F2-12. |
| User story | `01-rd/req/user_stories/a1_student.md` — `US-A1-10`. |
| Khảo sát hệ thống | `01-rd/overview/system_survey.md` — mục 7.1 dòng `my_class_detail`. |
| Quyết định | `DEC-2026-1023-student-class-screens` (ghi vào `.nexa/control/decision-registry.md` bởi chủ dự án/điều phối). |
| Màn anh em | `01-rd/screens/users/USR0701_my_classes.md`; `01-rd/screens/teacher/INS0202_class_assignments.md`. |
| Prototype | `05-coding/frontend/src/views/users/my-class-detail/ui/my-class-detail-view.tsx` (không có mockup `09-layoutBase`). |

**Lịch sử sửa đổi:** 2026-10-23 — tạo mới, viết sau khi prototype dựng (`DEC-2026-1023-student-class-screens`).
