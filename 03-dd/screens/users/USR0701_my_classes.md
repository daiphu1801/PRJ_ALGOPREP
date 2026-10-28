# DD màn hình — Lớp của tôi (`USR0701`)

## 0. Thông tin tài liệu

Bảng 0.1 là bìa và nguồn của hợp đồng màn hình.

| Mục | Giá trị |
| :--- | :--- |
| Mã, tên, actor | `USR0701`, Lớp của tôi, STUDENT |
| Route | `/classes` |
| RD, BD | `01-rd/screens/users/USR0701_my_classes.md`; `02-bd/screens/users/USR0701_my_classes.md` |
| Prototype | `05-coding/frontend/src/views/users/my-classes/` |
| Module | F1 identity (#25, #25A), F2 problem-bank (§5A) |
| TDD | `04-tdd/modules/F1_identity.md` AC-19, AC-20, AC-20b, AC-49, AC-50; `04-tdd/modules/F2_problem-bank.md` AC-88; `04-tdd/screens/users/USR0701_my_classes.md` |
| Phiên bản, ngày | V0.1, 2026-10-24 |

Bảng 0.2 ghi lịch sử sửa.

| Ver | Nội dung | Ngày |
| :--- | :--- | :--- |
| V0.1 | Tạo hợp đồng DD theo quyết định tách F1/F2 và giới hạn mã mời | 2026-10-24 |

Bảng 0.3 quy ước nguồn trích dẫn.

| Ký hiệu | Nguồn |
| :--- | :--- |
| BD | `02-bd/screens/users/USR0701_my_classes.md` (Sheet 5, 8, 9) |
| F1, F2 | `03-dd/api/F1_identity.md` mục 0.4.1; `03-dd/api/F2_problem-bank.md` mục 5A |
| V | `05-coding/frontend/src/views/users/my-classes/ui/my-classes-view.tsx`; `05-coding/frontend/src/entities/class/api/queries.ts` |
| DEC | `DEC-2026-1024-dd-student-class-contracts` |

## 1. Bản đồ slice FSD

Bảng 1.1 là vị trí các thành phần; đường dẫn tính từ `05-coding/frontend/src/`.

| Lớp | Đường dẫn | Vai trò | Trạng thái |
| :--- | :--- | :--- | :--- |
| app | `app/(student)/classes/page.tsx` | Gắn route với view | Đã có |
| views | `views/users/my-classes/ui/my-classes-view.tsx` | Ghép lớp và bài theo `classId`, form mã mời, điều hướng | Đã có, dùng mock |
| views | `views/users/my-classes/model/` | Adapter hai query và trạng thái từng vùng | Đích DD, chưa có |
| entities | `entities/class/api/queries.ts` | `useMyClasses`, `useJoinClass`; tách `useMyClassProblems` về entity phù hợp F2 | Mock đang gộp lớp và bài |
| entities | `entities/class/model/types.ts` | `MyClass` và bài giao; bỏ `assignments` gộp khỏi DTO F1 | Mock chưa khớp |
| shared | `shared/ui` Card, Button, ProgressBar, ErrorState, toast; `shared/lib` định dạng lịch/ngày | Tái dùng; không thêm thành phần shared | Có |

View chỉ ghép dữ liệu, không truy cập schema ngoài module. Không tạo widget hoặc feature riêng cho form một ô. Chiều import `app → views → entities → shared`. Props thẻ lớp là thông tin F1 và tiến độ tính từ F2; khi F2 lỗi props tiến độ vắng, thẻ vẫn render. `[SoT: BD Sheet 4.5; V:61-75,105-150]`

## 2. API tiêu thụ

Bảng 2.1 nối toàn bộ sự kiện Sheet 8 với hợp đồng module.

| EVT | API ID | Hook/query đích | Hiện trạng |
| :--- | :--- | :--- | :--- |
| EVT-1 tải trang | F1 #25A `ListMyClasses` và F2 #13A/§5A `ListMyClassProblems`, gọi song song không truyền `classId` | `useMyClasses`, `useMyClassProblems` | `useMyClasses` mock gộp hai nguồn |
| EVT-2 gõ mã | Không gọi API | State form cục bộ | Có |
| EVT-3 tham gia | F1 #25 `JoinClassByCode`; thành công invalidate lại hai query EVT-1 | `useJoinClass` | Mock tự chuẩn hoá mã, cần chuyển lên client |
| EVT-4 xem chi tiết | Không gọi API; `/classes/{classId}` | `next/link` | Có |
| EVT-5 thử lại | Gọi lại F1 #25A; F2 #13A nếu query này lỗi | `refetch` theo vùng | Chưa có vùng lỗi riêng |

Không có realtime. #25A chỉ trả thông tin lớp; #13A trả bài của toàn bộ lớp đang ghi danh, kể cả bài đã rút. Màn tính `solvedCount = count(solveState === SOLVED)`, `totalCount = số dòng bài của lớp`, không chia cho 0. Bài đã rút vẫn ở mẫu số; bài có `removed_at` không nằm trong response. `[SoT: BD Sheet 7.1; DEC]`

## 3. Trạng thái màn

Bảng 3.1 là trạng thái và ánh xạ lỗi; frontend chỉ dùng `error.code`, không đọc `error.message` (`03-dd/api/api.md` mục 1.1).

| Vùng | Trạng thái/mã | Xử lý | Khoá i18n |
| :--- | :--- | :--- | :--- |
| Danh sách | Đang tải | Dòng tải thay danh sách, khung mã mời vẫn dùng được | `myClasses.loading` |
| Danh sách | `items=[]` | Hướng dẫn nhập mã | `myClasses.empty` |
| Danh sách | F1 lỗi mạng/5xx | Vùng lỗi kèm Thử lại; sau retry thất bại thêm toast | `apiError.*`, cần khoá retry chung |
| Tiến độ | F2 lỗi mạng/5xx | Ẩn riêng progress ở mọi thẻ, vẫn giữ lớp và link | Không có nhãn lỗi [SoT: BD Sheet 9 NO 9] |
| Mã mời | Rỗng sau trim | Không gửi, focus ô, toast | `myClasses.join.empty` |
| Mã mời | `IDT-410` | Giữ ô, đánh dấu sai, toast chung cho mã sai/hết hạn | `myClasses.join.invalid` |
| Mã mời | `IDT-412` | Giữ ô, báo chờ 15 phút, không tự gửi lại | `apiError.IDT-412` cần bổ sung |
| Mã mời | Lỗi mạng/5xx | Giữ ô, toast lỗi kết nối; không gán là mã sai | `apiError.*` |
| Mã mời | Thành công | Xoá ô, toast và refetch hai nguồn | `myClasses.join.success` |

Thiếu phiên chuyển về auth theo shell; màn không có trạng thái khách. Lớp không có bài hiện `0/0`, thanh rỗng. Khi F1 lỗi lần đầu không hiển thị toast; toast chỉ dùng ở retry thất bại. `[SoT: BD Sheet 8 EVT-1, EVT-5]`

## 4. Quy tắc đặc thù màn

**BR-SC-01 — Ghép dữ liệu độc lập.** Kích hoạt EVT-1 sau khi đã xác thực. Gọi F1 #25A và F2 #13A song song. Chỉ F1 quyết định thẻ lớp và trạng thái tải/lỗi của danh sách; nhóm bài F2 theo `classId` rồi tính x/y cho từng lớp. Nếu F2 lỗi, không giữ progress từ dữ liệu cũ. Hậu điều kiện: thẻ lớp và link chi tiết vẫn hoạt động. `[SoT: BD Sheet 8 EVT-1]`

**BR-SC-02 — Gửi mã mời và làm mới.** Kích hoạt EVT-3 với ô không rỗng, không có mutation đang chờ. Trim và uppercase ngay trước khi gửi, khoá nút; mã rỗng không gọi API. Thành công xoá ô, thông báo bằng `className` từ F1 #25 và invalidate F1/F2. Lỗi giữ nguyên chuỗi đã gõ để sửa, mở khoá nút, phân biệt `IDT-410`, `IDT-412` và lỗi hệ thống. Hậu điều kiện: lớp vừa tham gia xuất hiện sau refetch, không cần tải lại trang. `[SoT: BD Sheet 8 EVT-3]`

**BR-SC-03 — Điều hướng.** Kích hoạt EVT-4 từ thẻ có UUID thật. Link tới `/classes/{classId}`; ô mã chưa gửi bị bỏ khi rời trang, không cần hộp xác nhận. `[SoT: BD Sheet 8 EVT-4]`

## 5. Kiểm tra phía client

Bảng 5.1 đối chiếu zod với module validation; máy chủ luôn kiểm lại.

| Trường | Zod/client | Nguồn | Khớp |
| :--- | :--- | :--- | :--- |
| `code` | `z.string().max(16).transform(trim).transform(uppercase).refine(nonempty)`; không chặn 8 ký tự tại client để sai dạng nhận cùng lỗi mã | `03-dd/validation/F1_identity.md` mục 9; BD Sheet 9 NO 3–5 | Có |
| `classId` từ thẻ | `z.string().uuid()` khi dựng link | F1 #25A; `03-dd/validation/F1_identity.md` mục 9 | Có |

## 6. i18n và truy cập

Tiền tố `myClasses.*` có trong `messages/vi.json` và `en.json`; bổ sung `apiError.IDT-412` và khoá retry nếu chưa có. Ô mã có label thực, Enter gửi form như nút, lỗi liên kết bằng `aria-describedby`; khi lỗi focus ô. Toast thành công/lỗi dùng vùng `aria-live`, tránh đọc hai lần. Tên truy cập của link thẻ dùng `myClasses.card.viewDetailOf` kèm tên lớp. Thanh tiến độ có văn bản `myClasses.card.progress`, không chỉ biểu diễn bằng màu. `[SoT: BD Sheet 5; V:83-95,105-150]`

## 7. Điểm kiểm thử

Bảng 7.1 bao phủ EVT-1 đến EVT-5 và luồng kết hợp.

| ID | Loại | Nội dung | Công cụ | AC |
| :--- | :--- | :--- | :--- | :--- |
| SC-01 | Normal | EVT-1 bốn lớp đều hiện; ghép đúng x/y theo classId, kể cả bài rút | Vitest | F1 AC-49; F2 AC-88; `my_classes` AC-SC-01 |
| SC-02 | Abnormal | EVT-1 F2 lỗi thì thẻ lớp vẫn có, progress ẩn | Vitest | `my_classes` AC-SC-01 |
| SC-03 | Boundary | EVT-2 nhập 16 ký tự; EVT-3 rỗng sau trim không gửi, focus ô | Vitest | `my_classes` AC-SC-02 |
| SC-04 | Normal | EVT-3 mã hợp lệ/đã thuộc lớp/vào lại: toast, xoá ô, refetch | Vitest | F1 AC-20, AC-20b; `my_classes` AC-SC-02 |
| SC-05 | Abnormal | EVT-3 mã sai/hết hạn và lỗi mạng hiển thị hai loại toast khác nhau | Vitest | F1 AC-19; `my_classes` AC-SC-02 |
| SC-06 | Boundary | EVT-3 sai năm lần, lần sau nhận `IDT-412`, không mất mã | Vitest | F1 AC-50 |
| SC-07 | Normal | EVT-4 mở đúng `/classes/{classId}`, tên link đọc được | Playwright | `my_classes` AC-SC-03 |
| SC-08 | Abnormal | EVT-5 retry F1 lỗi rồi thành công; chỉ retry lỗi mới toast | Vitest | `my_classes` AC-SC-01 |

## 8. Nợ prototype và câu hỏi mở

**Nợ prototype:** `entities/class` đang trả `MyClass.assignments` từ một mock, giảng viên là hằng và ngày là nhãn đã định dạng; cần tách hai response thật, dùng ISO `joinedAt`, xử lý `withdrawn`, sửa hook `useMyClasses`/`useJoinClass`, thêm vùng lỗi và `IDT-412`. Dòng tương ứng ở `06-plan/PROTOTYPE_DEBT.md` mục màn lớp còn mở cho đến khi tích hợp code. `[SoT: BD Sheet 4.4; V]`

**Câu hỏi mở:** không còn lựa chọn chặn. F1/F2 split, bài rút và Redis rate limit đã chốt tại `DEC-2026-1024-dd-student-class-contracts`. Thứ tự lớp mới tham gia trước, tên giảng viên chỉ là `display_name`, không có badge đếm nav theo đề xuất BD; nếu yêu cầu sản phẩm đổi thì cập nhật RD/BD trước. `[SoT: BD mục Câu hỏi mở Q3, Q5, Q8, Q9]`
