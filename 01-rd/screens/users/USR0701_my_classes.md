# RD (Yêu cầu hệ thống mới) — Lớp của tôi / `USR0701`

> Mã màn hình: `USR0701`, theo `02-bd/_rules/bd-template-9sheet.md` mục 8 (nhóm mới 07 "Lớp học" của khu USR).
> Slug chính tắc: `my_classes`, route `/classes`. Đăng ký vào `01-rd/overview/system_survey.md` mục 7.1
> cùng đợt viết file này (2026-10-23, `DEC-2026-1023-student-class-screens`).
> Phạm vi/Bounded Context: `identity` (F1) là chính; đọc thêm danh sách bài được giao của `problem-bank` (F2-12).
> Actor: A1 (`STUDENT`).
> Nguồn sự thật (SoT): `01-rd/req/F1_identity.md` (F1-23, F1-25, F1-26), `01-rd/req/F2_problem-bank.md` (F2-12),
> `01-rd/req/user_stories/a1_student.md` (`US-A1-10`, viết cùng đợt).
> Màn **không có mockup trong `09-layoutBase/`** — bản dựng prototype ngày 2026-10-23 là bằng chứng duy nhất về hình.
> Tài liệu này có thể đọc độc lập.

---

## 1. Mục đích và bối cảnh

Cho học viên một chỗ để **nhập mã mời của giảng viên và tham gia lớp**, rồi xem các lớp mình đang học: tên lớp,
giảng viên, lịch học, ngày tham gia và tiến độ làm các bài được giao [SoT: 01-rd/req/F1_identity.md:126-137].

**Khoảng trống màn này lấp.** RD đã chốt học viên tự tham gia lớp bằng cách nhập mã mời
[SoT: 01-rd/req/F1_identity.md:133-137] và bài được giao hiện thành một khối ngay trong danh sách bài toán
[SoT: 01-rd/req/F2_problem-bank.md:48-54]. Nhưng không tài liệu nào nói **nhập mã ở đâu** và **xem lớp của mình ở
đâu**: trước màn này học viên không có giao diện để nhập mã mời, cũng không có màn xem lớp. Hợp đồng API
`POST /classes/join` đã có từ trước và ghi nơi dùng là `USR0101_problem_list` "luồng nhập mã"
[SoT: 03-dd/api/F1_identity.md:70], nhưng `USR0101` không có ô nhập nào.

**Đối chiếu với RD hiện hành.** `01-rd/req/user_stories/a1_student.md:41-42` ghi bài đã giao hiện thành nhóm
riêng trong danh sách bài toán "không cần mở màn khác". Hai màn `USR0701`/`USR0702` **mở rộng** phạm vi đó chứ
không thay thế: khối "Bài tập lớp" trong `problem_list` giữ nguyên. Chỗ cần chủ dự án chốt: có cấp mã `Fx-nn` riêng
hay mở rộng F1-23/F2-12 — xem Q1.

File này mô tả **hành vi và UX ở mức yêu cầu**; không lặp lại đặc tả F1-23/F1-25/F1-26, chỉ trỏ tới.

---

## 2. Phạm vi

| Hành vi | Mã | Nguồn |
| :--- | :--- | :--- |
| Học viên tự tham gia lớp bằng mã mời | F1-23 | `01-rd/req/F1_identity.md:133-137` |
| Mã mời hết hạn sau 7 ngày, nhiều mã hiệu lực song song | F1-25 | `01-rd/req/F1_identity.md:142-151` |
| Học viên bị gỡ chỉ bị đánh dấu "đã rời lớp", lịch sử giữ lại | F1-26 | `01-rd/req/F1_identity.md:152` |
| Bài được giao cho lớp (nguồn dữ liệu tiến độ trên thẻ lớp) | F2-12 | `01-rd/req/F2_problem-bank.md:48-54` |

Các khối màn, theo bản dựng prototype (`05-coding/frontend/src/views/users/my-classes/ui/my-classes-view.tsx`):

1. **Đầu trang** — tiêu đề "Lớp của tôi" và một dòng mô tả (`my-classes-view.tsx:52-57`).
2. **Cột trái, danh sách lớp** — mỗi lớp học viên **đang** tham gia là một thẻ: tên lớp, mô tả (nếu có), giảng viên,
   lịch học một dòng chữ (ẩn nếu lớp chưa có lịch), ngày tham gia, thanh tiến độ "Đã xong x/y bài" (ẩn khi lớp chưa
   giao bài), liên kết "Xem chi tiết lớp" sang `USR0702` (`my-classes-view.tsx:102-153`).
3. **Trạng thái rỗng** — khi học viên chưa thuộc lớp nào, hiện câu hướng dẫn nhập mã ở khung bên cạnh
   (`my-classes-view.tsx:63-66`, thông điệp `myClasses.empty` tại `05-coding/frontend/messages/vi.json:2980`).
4. **Cột phải, khung "Tham gia lớp"** — một ô nhập mã mời, một nút "Tham gia", dòng gợi ý "Mã có hạn dùng 7 ngày"
   (`my-classes-view.tsx:78-96`, `vi.json:2981-2989`).
5. **Lối vào** — mục thứ 7 "Lớp của tôi" trên thanh điều hướng đầu trang (không đặt trong menu avatar), chủ dự án chốt
   2026-10-23 (`05-coding/frontend/src/widgets/app-shell/model/student-nav.ts:42-43`).

### 2.1 Cấu trúc hệ thống liên quan

| Màn/tài liệu | Vai trò | Bounded Context |
|---|---|---|
| Màn hiện tại | `USR0701` / `my_classes` | `identity` |
| Màn anh em | `USR0702` / `my_class_detail` — chi tiết một lớp | `identity`, `problem-bank` |
| Màn liên quan | `USR0101_problem_list` — khối "Bài tập lớp" (F2-12) | `problem-bank`, `identity` |
| Màn phía giảng viên | `INS0201_class_management` — nơi sinh mã mời | `identity` |

**Chi tiết trạng thái và cấu trúc màn**

| Thành phần | Vai trò | Nguồn |
| :--- | :--- | :--- |
| `my-classes-view.tsx` | View của màn | [SoT: 05-coding/frontend/src/views/users/my-classes/ui/my-classes-view.tsx] |
| `app/(student)/classes/page.tsx` | Route `/classes` | [SoT: 05-coding/frontend/src/app/(student)/classes/page.tsx] |
| Entity `class` — `MyClass`, `MyClassAssignment` | Kiểu dữ liệu của thẻ lớp | [SoT: 05-coding/frontend/src/entities/class/model/types.ts:120-136] |
| `listMyClasses`, `joinClassByCode` (mock) | Nguồn dữ liệu prototype | [SoT: 05-coding/frontend/src/entities/class/api/__mock__/class-mocks.ts:504-527] |
| Thông điệp `myClasses` | Câu chữ giao diện | [SoT: 05-coding/frontend/messages/vi.json:2976-3012] |

---

## 3. Ngoài phạm vi (Out of Scope)

- **Học viên tự rời lớp** — chưa có yêu cầu nào; F1-26 chỉ nói giảng viên gỡ học viên
  [SoT: 01-rd/req/F1_identity.md:152]. Không thêm nút "Rời lớp".
- **Xem danh sách bạn cùng lớp hoặc điểm của học viên khác** — hồ sơ học viên chỉ dành cho giảng viên (F1-27,
  actor A2) [SoT: 01-rd/req/F1_identity.md:153-160]; màn này chỉ hiện dữ liệu của chính học viên.
- **Sửa thông tin lớp** — thuộc giảng viên (F1-23, `INS0201`).
- **Thông tin liên hệ giảng viên** ngoài tên hiển thị.
- **Hạn nộp bài** — bài giao theo lớp không có hạn nộp [SoT: 02-bd/screens/users/USR0101_problem_list.md:233].
- **Cập nhật thời gian thực** (WebSocket/SSE) — không có yêu cầu; danh sách làm mới sau khi tham gia lớp thành công.
- **Huy hiệu số lớp trên thanh điều hướng** — xem Q5.
- Bảng màu, spacing, breakpoint — thuộc BD (`02-bd/screens/users/USR0701_my_classes.md`, chưa viết).
- Hợp đồng API (danh sách lớp của học viên, chi tiết, tham gia) — thuộc DD. Riêng `POST /classes/join` đã có
  [SoT: 03-dd/api/F1_identity.md:70].

---

## 4. Tiền đề và ràng buộc

> Prototype dựng 2026-10-23 trước khi có RD (chủ dự án: dựng trước, viết tài liệu sau). Các điểm chưa chốt ghi dưới
> đây, chủ sở hữu mọi câu hỏi là **chủ dự án**.

| # | Câu hỏi | Quyết định | Ghi chú |
| :-: | :--- | :--- | :--- |
| Q1 | Hai màn `USR0701`/`USR0702` có cần mã `Fx-nn` riêng (ví dụ `F1-32`) hay mở rộng F1-23/F2-12? Ưu tiên: **cao** (chặn đánh số REQ trong BD). | **Chờ chủ dự án.** Đề xuất: **mở rộng F1-23 (tham gia và xem lớp) và F2-12 (bài được giao), không cấp mã mới** — ponytail bậc 1 (yêu cầu nguồn đã có) và bậc 4 (tái dùng mã sẵn). Trước mắt tài liệu trích F1-23/F2-12. | Cùng cách đã chốt ở `USR0103` Q1 (mở rộng F2-13, không mã mới) và F2-12 "không cần mã mới" [SoT: 01-rd/req/F2_problem-bank.md:51]. |
| Q2 | Học viên cần API đọc danh sách lớp của mình. `03-dd/api/F1_identity.md` chỉ có `GET /classes/my` cho **giảng viên** và `POST /classes/join`; chưa có endpoint lớp của học viên [SoT: 03-dd/api/F1_identity.md:64,70]. Ưu tiên: **cao** (chặn DD). | **Chờ chủ dự án.** Đề xuất: thêm endpoint đọc lớp của học viên vào DD khi viết (một lần gọi trả cả giảng viên, lịch, bài được giao, trạng thái đã xong — đúng hình `MyClass` của prototype). Không phải câu hỏi cho RD; ghi để BD/DD nhận. | `[SoT: Suy luận]` — chỉ đọc hai dòng 64 và 70 của DD, không tìm thấy endpoint thứ ba cho học viên; chưa rà toàn bộ DD. |
| Q3 | Nguồn tên giảng viên hiển thị trên thẻ lớp: lấy `display_name` của chủ lớp (`classes.instructor_id`)? Prototype dùng hằng số mock (`class-mocks.ts:467`). Ưu tiên: thấp. | **Chờ chủ dự án.** Đề xuất: `display_name` của chủ lớp, không hiển thị email hay thông tin khác (đúng "ngoài phạm vi"). Nếu giảng viên đã ẩn danh hoá/khoá tài khoản thì xử lý thuộc BD. | `[SoT: Suy luận]` — dữ liệu `instructor_id` có ở `PATCH /classes` kiểm sở hữu [SoT: 03-dd/api/F1_identity.md:66]. |
| Q4 | Lớp bị giảng viên **xoá** (hard delete, F1-24) trong lúc học viên đang mở trang. Ưu tiên: thấp. | **Chờ chủ dự án.** Đề xuất: lần tải lại kế tiếp lớp biến mất khỏi danh sách; thẻ cũ đang hiện không cần xử lý riêng (không có yêu cầu thời gian thực). Với `USR0702` xem Q3 của file đó. | F1-24 xoá thật [SoT: 01-rd/req/F1_identity.md:138-141]. Ponytail bậc 3. |
| Q5 | Mục "Lớp của tôi" trên thanh điều hướng có hiện huy hiệu số lớp không? Ưu tiên: thấp. | **Chờ chủ dự án.** Đề xuất: **không** — ponytail bậc 3 (không ai cần số này khi demo). Bản dựng hiện không có huy hiệu (`student-nav.ts:42-43`). | |
| Q6 | Nhập mã: có tự cắt khoảng trắng và không phân biệt hoa thường không, và có giới hạn số lần nhập sai (chống dò mã) không? Ưu tiên: **trung bình** (an ninh — ponytail bậc 6). | **Chờ chủ dự án.** Đề xuất: cắt khoảng trắng đầu/cuối và quy về chữ hoa trước khi gửi (mock đã làm: `class-mocks.ts:515`); áp giới hạn tần suất cho `POST /classes/join` bằng Redis rate limit sẵn có của hệ thống [SoT: README.md:166], ngưỡng số cụ thể để DD quyết. | Mã chỉ 8 ký tự chữ hoa và số [SoT: 02-bd/database/F1_identity.md:119], nên dò mã là rủi ro có thật. `[SoT: Suy luận]` về ngưỡng. |
| Q7 | Prototype báo **một** thông điệp lỗi cho mọi lỗi tham gia (`join.invalid`), kể cả mất mạng (`my-classes-view.tsx:43`). Hợp đồng chỉ có lỗi `IDT-410` cho mã sai/hết hạn [SoT: 03-dd/logic/F1_identity.md:187]. Ưu tiên: thấp. | **Chờ chủ dự án.** Đề xuất: giữ một thông điệp cho `IDT-410`, lỗi khác (mạng, `5xx`) dùng thông điệp lỗi chung của hệ thống. Thuộc BD/DD; ghi để bản dựng thật sửa. | Lệch của prototype so với hợp đồng, chưa phải lỗi yêu cầu. |

---

## 5. Danh sách yêu cầu (REQ)

| ID | Yêu cầu | Loại | Nguồn/SoT |
|---|---|---|---|
| REQ-01 | **Cho** tôi có tài khoản `STUDENT` và có một mã mời còn hạn, **Khi** tôi nhập mã vào ô "Mã mời" và bấm "Tham gia", **Thì** tôi vào lớp đó, màn báo thành công kèm tên lớp, ô nhập được xoá, và danh sách lớp làm mới có thêm thẻ của lớp vừa vào | Chức năng (GWT) | F1-23 [SoT: 01-rd/req/F1_identity.md:133-137]; [SoT: 05-coding/frontend/src/views/users/my-classes/ui/my-classes-view.tsx:35-45] |
| REQ-02 | **Cho** tôi nhập một mã không tồn tại hoặc đã hết hạn (quá 7 ngày), **Khi** tôi bấm "Tham gia", **Thì** màn báo lỗi "Mã mời không đúng hoặc đã hết hạn", tôi không vào lớp nào và danh sách không đổi; màn không cho biết mã đúng ở chỗ nào hay đã hết hạn lúc nào | Chức năng (GWT) | F1-25 [SoT: 01-rd/req/F1_identity.md:142-146]; [SoT: 03-dd/logic/F1_identity.md:187]; [SoT: 05-coding/frontend/messages/vi.json:2988] |
| REQ-03 | **Cho** tôi đã đang ở trong lớp đó, **Khi** tôi nhập lại mã mời của chính lớp ấy, **Thì** hệ thống coi là thành công, không báo lỗi trùng, danh sách lớp giữ nguyên, không sinh thêm thẻ nào | Chức năng (GWT) | [SoT: 03-dd/logic/F1_identity.md:188 (trường hợp b)]; [SoT: 03-dd/api/F1_identity.md:70] |
| REQ-04 | **Cho** tôi từng bị giảng viên gỡ khỏi lớp (đã rời lớp), **Khi** tôi nhập mã mời còn hạn của lớp đó, **Thì** tôi vào lại lớp, lớp hiện lại trong danh sách của tôi, ngày tham gia giữ ngày ban đầu | Chức năng (GWT) | F1-26 [SoT: 01-rd/req/F1_identity.md:152]; [SoT: 03-dd/logic/F1_identity.md:188 (trường hợp c)] |
| REQ-05 | **Cho** ô "Mã mời" đang trống hoặc chỉ có khoảng trắng, **Khi** tôi bấm "Tham gia", **Thì** màn báo lỗi "Nhập mã mời trước khi tham gia" và **không gửi yêu cầu nào** lên hệ thống | Chức năng (GWT) | [SoT: 05-coding/frontend/src/views/users/my-classes/ui/my-classes-view.tsx:37]; [SoT: 05-coding/frontend/messages/vi.json:2986] |
| REQ-06 | **Cho** tôi đang tham gia một hoặc nhiều lớp, **Khi** tôi mở "Lớp của tôi", **Thì** mỗi lớp là một thẻ riêng gồm tên lớp, mô tả (nếu có), giảng viên, lịch học (nếu có), ngày tham gia; tôi ở nhiều lớp thì thấy nhiều thẻ | Chức năng (GWT) | F1-23 [SoT: 01-rd/req/F1_identity.md:129-132]; [SoT: 05-coding/frontend/src/views/users/my-classes/ui/my-classes-view.tsx:68-74,102-129] |
| REQ-07 | **Cho** tôi đã bị gỡ khỏi một lớp (đã rời lớp), **Khi** tôi mở "Lớp của tôi", **Thì** lớp đó **không** có thẻ trong danh sách (chỉ lớp tôi đang tham gia mới hiện); lịch sử làm bài của tôi vẫn giữ nguyên | Chức năng (GWT) | F1-26 [SoT: 01-rd/req/F1_identity.md:152] cho việc giữ lịch sử; phần "không hiện thẻ" là `[SoT: Suy luận]` — F1-26 chỉ nói học viên đã rời không còn tính vào tổng số và tiến độ lớp, không nói rõ việc hiển thị phía học viên; mock chưa mô phỏng (`class-mocks.ts:504-508` chỉ lọc theo lớp đã tham gia) |
| REQ-08 | **Cho** lớp của tôi đã được giao bài, **Khi** tôi nhìn thẻ lớp, **Thì** thấy thanh tiến độ và dòng "Đã xong x/y bài", với y là số bài được giao cho lớp và x là số bài tôi đã `Accepted` ít nhất một lần; lớp chưa giao bài thì không hiện thanh tiến độ | Chức năng (GWT) | F2-12 [SoT: 01-rd/req/F2_problem-bank.md:48-54]; cùng cách tính hoàn thành với F1-28 [SoT: 01-rd/req/F1_identity.md:169-171]; [SoT: 05-coding/frontend/src/views/users/my-classes/ui/my-classes-view.tsx:131-144] |
| REQ-09 | **Cho** tôi chưa thuộc lớp nào, **Khi** tôi mở "Lớp của tôi", **Thì** cột trái hiện trạng thái rỗng hướng dẫn nhập mã ở khung bên cạnh, khung "Tham gia lớp" vẫn dùng được | Chức năng (GWT) | [SoT: 05-coding/frontend/src/views/users/my-classes/ui/my-classes-view.tsx:63-66]; [SoT: 05-coding/frontend/messages/vi.json:2980] |
| REQ-10 | **Cho** tôi bấm "Xem chi tiết lớp" trên một thẻ, **Khi** liên kết mở, **Thì** tôi sang `/classes/{classId}` của đúng lớp đó (màn `USR0702`) | Chức năng (GWT) | [SoT: 05-coding/frontend/src/views/users/my-classes/ui/my-classes-view.tsx:145-150] |
| REQ-11 | **Cho** tôi đang ở bất kỳ màn nào của khu người học, **Khi** tôi nhìn thanh điều hướng đầu trang, **Thì** có mục "Lớp của tôi" dẫn tới `/classes` (mục thứ 7; không nằm trong menu avatar) | Chức năng (GWT) | Chủ dự án chốt 2026-10-23 [SoT: 05-coding/frontend/src/widgets/app-shell/model/student-nav.ts:42-43] |
| REQ-12 | **Cho** hệ thống AI (F5/F6) không khả dụng, **Khi** tôi dùng màn này, **Thì** màn hoạt động đầy đủ — màn không phụ thuộc AI | Phi chức năng (GWT) | `[SoT: Suy luận]` — màn chỉ đọc `identity` và `problem-bank`; nguyên tắc AI suy giảm êm của hệ thống (F1-F4 độc lập F5/F6) |

---

## 6. Yêu cầu dữ liệu

### 6.1 Dữ liệu tham chiếu

Chi tiết bảng, cột, kiểu dữ liệu và ràng buộc lưu trữ thuộc BD/DD; tài liệu này không tự suy luận dữ liệu chưa được
chốt. Dữ liệu một thẻ lớp theo prototype gồm: tên, mô tả, giảng viên, lịch học, ngày tham gia, danh sách bài được
giao kèm cờ đã xong [SoT: 05-coding/frontend/src/entities/class/model/types.ts:120-136]. Mã mời 8 ký tự
[SoT: 02-bd/database/F1_identity.md:119].

### 6.2 Luồng dữ liệu

Hợp đồng API, request/response và mã lỗi thuộc DD. Luồng mức yêu cầu thể hiện ở các hành vi chương 5. Chỗ DD còn
thiếu: endpoint đọc lớp của học viên (Q2).

---

## 7. RACI và danh sách bàn giao

| Bàn giao | Trạng thái |
|---|---|
| RD màn hình | Có tài liệu này |
| BD màn hình | Chưa viết — `02-bd/screens/users/USR0701_my_classes.md` |
| DD/API liên quan | `POST /classes/join` đã có; endpoint đọc lớp của học viên chưa có (Q2) |

---

## 8. Kế hoạch và quy mô

| Chỉ số | Giá trị |
|---|---|
| Quy mô hành vi | 12 yêu cầu ở chương 5 (11 chức năng, 1 phi chức năng) |
| Lịch, nhân sự, công sức | Chưa xác định — không tự suy luận. |

---

## 9. Tham chiếu

| Loại | Tham chiếu |
| :--- | :--- |
| Yêu cầu module | `01-rd/req/F1_identity.md` — F1-23, F1-25, F1-26; `01-rd/req/F2_problem-bank.md` — F2-12. |
| User story | `01-rd/req/user_stories/a1_student.md` — `US-A1-10`. |
| Khảo sát hệ thống | `01-rd/overview/system_survey.md` — mục 7.1 dòng `my_classes`. |
| Quyết định | `DEC-2026-1023-student-class-screens` (ghi vào `.nexa/control/decision-registry.md` bởi chủ dự án/điều phối). |
| Màn anh em | `01-rd/screens/users/USR0702_my_class_detail.md`; `01-rd/screens/users/USR0101_problem_list.md` (khối "Bài tập lớp"). |
| Prototype | `05-coding/frontend/src/views/users/my-classes/ui/my-classes-view.tsx` (không có mockup `09-layoutBase`). |

**Lịch sử sửa đổi:** 2026-10-23 — tạo mới, viết sau khi prototype dựng (`DEC-2026-1023-student-class-screens`).
