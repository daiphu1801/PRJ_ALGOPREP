# RD (Yêu cầu hệ thống mới) — Chi tiết lớp / `INS0205`

> Mã màn hình: `INS0205` (nối tiếp `INS0204` trong khu Giảng viên, bảng mã ở `02-bd/_rules/bd-template-9sheet.md` mục 8; dòng của `INS0205` được thêm cùng đợt với file này).
> Slug chính tắc: `class_detail` (thêm 2026-10-11, `DEC-2026-1011-class-detail-page`).
> Phạm vi/Bounded Context: `identity`. Actor: A2 (Giáo viên).
> Nguồn sự thật (SoT): `01-rd/req/F1_identity.md` (F1-23 tới F1-27), `DEC-2026-1011-class-detail-page`,
> `DEC-2026-1011-class-schedule-structured`, chỉ đạo trực tiếp của chủ dự án ngày 2026-10-11.
> Tài liệu này có thể đọc độc lập.

---

## 1. Mục đích và bối cảnh

**Slug mới, chưa có prototype `.dc.html`.** Chủ dự án chỉ đạo ngày 2026-10-11: màn "Lớp của tôi" (`INS0201`)
thêm một **trang chi tiết lớp**, vì "3 nút với popup không cung cấp đủ thông tin" — tức là ba thao tác
trước đó của mỗi dòng lớp (xem học viên, mã mời, sửa) mỗi cái chỉ mở một popup nhỏ, giảng viên không có chỗ
nào nhìn một lớp trọn vẹn. Quyết định này **đảo ngược** câu trả lời ngày 2026-08-28 ở Câu hỏi mở Q3 của
`01-rd/screens/teacher/INS0201_class_management.md` ("không cần trang con `/class/:id` riêng")
[SoT: 01-rd/screens/teacher/INS0201_class_management.md:127; `DEC-2026-1011-class-detail-page`].

Màn này **không có mã yêu cầu `Fx-nn` riêng**: nó gom các hành vi đã có ở F1-23 tới F1-27 về một trang
[SoT: 01-rd/req/F1_identity.md:105-132]. Cái mới chỉ là chỗ đặt (một trang con thay cho ba popup) và đường vào.

File này mô tả **hành vi và UX ở mức yêu cầu**; không lặp lại đặc tả chức năng đã có ở `01-rd/req/F1_identity.md`
mà chỉ trỏ tới.

---

## 2. Phạm vi

| Hành vi | Mã | Nguồn |
| :--- | :--- | :--- |
| Xem thông tin lớp; sửa lớp (dùng chung popup Tạo/Sửa lớp); xoá lớp (xoá thật) | F1-23, F1-24 | `01-rd/req/F1_identity.md` — F1-23, F1-24 |
| Xem lịch học của lớp, đọc từng phần (ngày học, giờ, phòng, thời gian, ghi chú) | F1-23 (ghi chú 2026-10-11) | `01-rd/req/F1_identity.md` — F1-23; `DEC-2026-1011-class-schedule-structured` |
| Xem các mã mời còn hiệu lực kèm hạn dùng; tạo mã mời mới ngay trên trang; nhiều mã song song hợp lệ | F1-25 | `01-rd/req/F1_identity.md` — F1-25 |
| Xem danh sách học viên của lớp; gỡ học viên khỏi lớp kèm xác nhận (~~xoá luôn lịch sử làm bài trong lớp~~ **ĐÃ ĐỔI 2026-10-14 (DEC-2026-1014-keep-learning-history-on-leave):** popup xác nhận nói rõ học viên được đánh dấu đã rời lớp và lịch sử làm bài vẫn được giữ, không hứa xoá; **ĐÃ CHỐT 2026-10-14 (DEC-2026-1014-class-leave-details):** học viên đã rời lớp không được tính vào số học viên và bảng của màn này; vào lại bằng mã mời thì dùng lại dòng ghi danh cũ và hiện lại) | F1-26 | `01-rd/req/F1_identity.md` — F1-26 |
| Từ tên học viên sang hồ sơ chi tiết học viên trong lớp | F1-27 | `01-rd/screens/teacher/INS0204_class_student_detail.md` |
| Ma trận phân quyền: Function `CLASS_MANAGEMENT`, phạm vi theo lớp phụ trách | F1-10, F1-12 | `01-rd/req/F1_identity.md` — F1-10, F1-12 |
| Given-When-Then xem chi tiết lớp trên một trang | — | `01-rd/req/user_stories/a2_instructor.md` (`US-A2-07`, mục thêm 2026-10-11) |

### 2.1 Cấu trúc hệ thống liên quan

| Màn/tài liệu | Vai trò | Bounded Context |
|---|---|---|
| Màn hiện tại | `INS0205` / `class_detail` | `identity` |
| Màn cha | `INS0201` / `class_management` — danh sách lớp, popup Tạo/Sửa lớp dùng chung | `identity` |
| Màn con | `INS0204` / `class_student_detail` — hồ sơ một học viên trong lớp | `identity` |
| Màn chị em | `/instructor/students` (`class_progress`, `INS0203`) — học viên toàn bộ, lọc theo lớp, **vẫn còn** | `identity` |
| Prototype | Chưa có `.dc.html`; prototype Next.js sẽ dựng route `/instructor/classes/[classId]` `[Đợi nextjs]` | Bằng chứng bố cục và trạng thái |

**Cấu trúc nội dung màn** (do chủ dự án chỉ đạo 2026-10-11; thứ tự dọc và chi tiết hình thức do BD đề xuất):

1. **Đầu trang** — tên lớp, mô tả, nút Quay lại (về `INS0201`), nút Sửa (mở popup Tạo/Sửa lớp dùng chung),
   nút Xoá (xác nhận rồi xoá thật, sau đó về `INS0201`).
2. ~~**Dải số liệu** — số học viên, tỉ lệ hoàn thành, điểm trung bình, số cần chấm, số học viên vắng bài~~
   **Đã bỏ (chủ dự án, 2026-10-11, `DEC-2026-1011-class-detail-no-stat-strip`).** Năm số này đã hiện ở danh sách lớp `INS0201` (ponytail bậc 4); trang chi
   tiết không lặp lại. Số thứ tự các mục giữ nguyên để không đánh số lại.
3. **Thẻ "Lịch học"** — hiển thị **từng phần**: ngày học trong tuần, giờ học, phòng, thời gian áp dụng
   (từ ngày đến ngày), ghi chú. Mọi phần tuỳ chọn; lớp không có lịch thì thẻ hiện trạng thái rỗng.
4. **Thẻ "Mã mời"** — danh sách mã còn hiệu lực kèm hạn dùng, nút sao chép từng mã, nút "Tạo mã mới" ngay
   trên trang. Nhiều mã song song hợp lệ, không thu hồi mã cũ (F1-25)
   [SoT: 01-rd/req/F1_identity.md:118-125]. **Chốt 2026-10-14
   (DEC-2026-1014-instructor-batch3-4-contracts-and-removal):** hạn dùng mỗi mã cố định 7 ngày, không có ô
   nhập để chọn hạn, tạo mã mới được bất cứ lúc nào [SoT: .nexa/control/decision-registry.md:3734-3735].
5. **Bảng "Học viên của lớp"** — tên (liên kết sang `INS0204`), trạng thái, điểm TB, tỉ lệ hoàn thành, nút
   "Gỡ khỏi lớp" kèm popup xác nhận (F1-26). Nút "Gỡ khỏi lớp" ở cuối mỗi dòng và luôn hiện rõ
   (`DEC-2026-1014-instructor-batch3-4-contracts-and-removal`, áp cùng quy tắc của `INS0201`).

**Điểm vào:** bấm tên lớp hoặc icon "Xem chi tiết" ở danh sách `INS0201`; và **sau khi tạo lớp thành công**
thì chuyển thẳng sang trang này (thay cho popup hiện mã mời đầu tiên như bản trước).

**Chốt 2026-10-14 (`DEC-2026-1014-instructor-batch5-6-nav-pagination-rubric`)** [SoT: .nexa/control/decision-registry.md:3761-3766]:
(1) sau khi tạo lớp, nút Quay lại ở đầu trang về danh sách lớp `INS0201`; (2) sidebar sáng mục của màn đã gọi tới
trang này; (3) bảng "Học viên của lớp" trong một lớp **không phân trang** (khác bảng gộp ở `INS0201`, phân trang 20 dòng).

**Thay đổi ở `INS0201` đi kèm:** bỏ popup Mã mời và icon "Xem học viên"; chỉ còn icon Xem chi tiết, Sửa,
Xoá. Popup Tạo/Sửa lớp giữ nguyên, dùng chung giữa hai màn.

---

## 3. Ngoài phạm vi (Out of Scope)

- Layout, spacing, bảng màu, component cụ thể — thuộc BD (`02-bd/screens/teacher/INS0205_class_detail.md`).
- Hợp đồng API — thuộc DD. **DD của màn này chưa viết**: DD màn hình viết sau prototype
  (`CLAUDE.md`, mục Process). Màn này tái dùng endpoint của `INS0201`/`INS0204`, không thêm endpoint mới ở mức BD.
- Giao/gỡ bài tập cho lớp — thuộc `class_assignments` (`INS0202`).
- Hồ sơ chi tiết một học viên — thuộc `class_student_detail` (`INS0204`).
- Chấm lại, bộ câu hỏi phỏng vấn riêng theo lớp — đã loại khỏi phạm vi
  (`DEC-2026-0828-remove-rejudge-scope`, `DEC-2026-0828-remove-per-class-interview-set`).
- Thu hồi mã mời, đặt hạn dùng mã mời tuỳ ý: F1-25 chỉ chốt "mỗi mã có hạn riêng"
  `[SoT: 01-rd/req/F1_identity.md:118-123]`; giá trị hạn đã chốt cố định 7 ngày
  (`DEC-2026-1014-instructor-batch3-4-contracts-and-removal`).

---

## 4. Tiền đề và ràng buộc

| # | Câu hỏi | Vì sao chưa trả lời được | Đề xuất | Chủ sở hữu |
| :-: | :--- | :--- | :--- | :--- |
| Q1 | ~~Có cần trang chi tiết lớp riêng không?~~ **ĐÃ CHỐT (2026-10-11, chỉ đạo của chủ dự án):** có, mã `INS0205` / `class_detail`, route prototype `/instructor/classes/[classId]`; đảo ngược Q3 của `INS0201`. Ghi `DEC-2026-1011-class-detail-page`. | — | Đã chốt. | (đã đóng) |
| Q2 | ~~Lịch học là ghi chú tự do hay có cấu trúc?~~ **ĐÃ CHỐT (2026-10-11):** có cấu trúc (ngày trong tuần, giờ bắt đầu/kết thúc, phòng, từ ngày, đến ngày, ghi chú); `DEC-2026-1011-class-schedule-structured`. Hiển thị một dòng khi thu gọn, từng phần ở thẻ Lịch học. | — | Đã chốt. Giới hạn độ dài `room` 100 và `note` 200 ký tự là **đề xuất, chưa chốt**. | Chủ dự án (chỉ giới hạn độ dài) |
| 205-Q1, 205-Q5 | Mô tả lớp lấy từ đâu; hạn dùng mã mời. **ĐÃ CHỐT 2026-10-14 (DEC-2026-1014-instructor-batch3-4-contracts-and-removal):** mô tả lớp nằm trong dữ liệu thẻ lớp của danh sách lớp, trang chi tiết lọc theo mã lớp phía client, không có endpoint chi tiết lớp riêng; hạn mã mời cố định 7 ngày. | — | Số thứ tự là của BD `02-bd/screens/teacher/INS0205_class_detail.md`; RD file này chỉ có Q1, Q2 mang nghĩa khác. | Đã đóng |

Các câu hỏi về hình thức (vị trí thẻ, thứ tự khối, hiển thị khi lớp rỗng) thuộc BD
(`02-bd/screens/teacher/INS0205_class_detail.md`, mục Câu hỏi mở), không phải câu hỏi yêu cầu.

---

## 5. Danh sách yêu cầu (REQ)

| ID | Yêu cầu | Loại | Nguồn/SoT |
|---|---|---|---|
| REQ-01 | Trang chi tiết lớp riêng, route `/instructor/classes/[classId]`, vào từ tên lớp / icon Xem chi tiết ở `INS0201` và tự động sau khi tạo lớp thành công | Chức năng | `DEC-2026-1011-class-detail-page` |
| REQ-02 | Đầu trang: tên lớp, mô tả, Quay lại, Sửa (popup dùng chung), Xoá (xoá thật, có xác nhận) (F1-24) | Chức năng | `01-rd/req/F1_identity.md` — F1-23, F1-24 |
| REQ-03 | ~~Dải số liệu của lớp: số học viên, tỉ lệ hoàn thành, điểm TB, cần chấm, vắng bài~~ **Đã bỏ 2026-10-11 (`DEC-2026-1011-class-detail-no-stat-strip`)**; số liệu của lớp xem ở danh sách `INS0201` | Chức năng | `DEC-2026-1011-class-detail-page`; công thức ở `INS0201` BD |
| REQ-04 | Thẻ Lịch học đọc từng phần từ lịch có cấu trúc; lớp không có lịch thì hiện trạng thái rỗng | Chức năng | `DEC-2026-1011-class-schedule-structured` |
| REQ-05 | Thẻ Mã mời: danh sách mã còn hiệu lực kèm hạn dùng, tạo mã mới ngay trên trang, nhiều mã song song hợp lệ (F1-25) | Chức năng | `01-rd/req/F1_identity.md` — F1-25 |
| REQ-06 | Bảng Học viên của lớp: tên liên kết sang `INS0204`, trạng thái, điểm TB, tỉ lệ hoàn thành, gỡ khỏi lớp có xác nhận (F1-26, F1-27) | Chức năng | `01-rd/req/F1_identity.md` — F1-26, F1-27 |
| REQ-07 | Chỉ lớp do chính giảng viên phụ trách; lớp khác bị từ chối, gác bởi Function `CLASS_MANAGEMENT` (F1-12) | Chức năng | `01-rd/req/F1_identity.md` — F1-10, F1-12 |
| REQ-08 | Given-When-Then xem chi tiết lớp trên một trang | Chức năng | `01-rd/req/user_stories/a2_instructor.md` (`US-A2-07`) |

---

## 6. Yêu cầu dữ liệu

### 6.1 Dữ liệu tham chiếu

Chi tiết bảng, cột, kiểu dữ liệu thuộc BD/DD. Riêng lịch học: cột `classes.schedule` là dữ liệu có cấu trúc
(`02-bd/database/F1_identity.md` mục 1.14); tài liệu này không tự suy luận thêm.

### 6.2 Luồng dữ liệu

Hợp đồng API, request/response và mã lỗi thuộc DD (chưa viết cho màn này). Luồng dữ liệu mức yêu cầu thể
hiện bởi các hành vi ở chương 5.

---

## 7. RACI và danh sách bàn giao

| Bàn giao | Trạng thái |
|---|---|
| RD màn hình | Có tài liệu này |
| BD màn hình | `02-bd/screens/teacher/INS0205_class_detail.md` (V0.1, 2026-10-11) |
| DD/API liên quan | Chưa viết — DD màn hình viết sau prototype |

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
| Yêu cầu module | `01-rd/req/F1_identity.md` — F1-10, F1-12, F1-23 tới F1-27. |
| User story | `01-rd/req/user_stories/a2_instructor.md` — `US-A2-07` (mục xem chi tiết lớp thêm 2026-10-11). |
| Màn hình liên quan | `01-rd/screens/teacher/INS0201_class_management.md` — màn cha; Q3 đảo ngược. |
| Màn hình liên quan | `01-rd/screens/teacher/INS0204_class_student_detail.md` — màn con của bảng học viên. |
| Quyết định | `DEC-2026-1011-class-detail-page`, `DEC-2026-1011-class-schedule-structured`. |
| Tài liệu gốc | `README.md` mục 3 và mục 4 — định nghĩa Bounded Context và actor. |
