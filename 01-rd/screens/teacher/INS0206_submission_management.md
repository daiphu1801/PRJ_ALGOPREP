# RD (Yêu cầu hệ thống mới) — Danh sách bài nộp / `INS0206`

> Mã màn hình: `INS0206` (nối tiếp `INS0205` trong khu Giảng viên; mã do `DEC-2026-1013-instructor-submissions-list` đề xuất
> "proposed slug `INS0206` `submission_management`" [SoT: .nexa/control/decision-registry.md:3476-3477]).
> Slug: `submission_management`. Route: `/instructor/submissions`. Mục sidebar: "Bài nộp".
> Phạm vi/Bounded Context: `judge-orchestration` (dữ liệu bài nộp) với phạm vi theo lớp của `identity`. Actor: A2 (Giảng viên).
> Nguồn sự thật (SoT): `DEC-2026-1013-instructor-submissions-list`, `DEC-2026-1013-instructor-avatar-menu-and-read-source`,
> `DEC-2026-1013-teacher-open-questions-closed-round2`, `DEC-2026-1014-instructor-batch5-6-nav-pagination-rubric`,
> chỉ đạo trực tiếp của chủ dự án ngày 2026-10-13.
> Chủ dự án đã chốt 10 câu hỏi mở ngày 2026-10-14: `DEC-2026-1014-instructor-submissions-list-rules`
> [SoT: .nexa/control/decision-registry.md:3891-3926].
> Tài liệu này có thể đọc độc lập. **Trạng thái: V0.3, có BD (V0.2), chưa có DD.**
>
> | Phiên bản | Ngày | Thay đổi |
> | :-: | :-: | :--- |
> | V0.1 | 2026-10-14 | Bản đầu, 10 câu hỏi mở (206-Q1 đến Q10) kèm đề xuất |
> | V0.2 | 2026-10-14 | Áp `DEC-2026-1014-instructor-submissions-list-rules`: đóng 206-Q1 đến Q10, viết lại REQ-04, REQ-02, REQ-08 và thêm REQ-13 đến REQ-19 (có GWT). 206-Q5 là lựa chọn của chủ dự án, khác đề xuất V0.1: học viên đã rời lớp vẫn hiện, phải giữ lịch sử ghi danh |
| V0.3 | 2026-10-14 | Áp `DEC-2026-1014-instructor-submissions-list-details` [SoT: .nexa/control/decision-registry.md:3975-4004]: REQ-04 (`SYSTEM_ERROR` chỉ ở "Tất cả"), REQ-07 và REQ-08 (khôi phục bộ lọc khi quay lại, tham số `from`), REQ-13 (rời rồi vào lại, khoảng đã rời tính là trong lớp), REQ-15 (lớp có lần giao mới nhất không sau lúc nộp). Không đổi số lượng REQ |

---

## 1. Mục đích và bối cảnh

Trước quyết định này, giảng viên chỉ vào được một bài nộp qua hồ sơ một học viên (`INS0204`) hoặc qua popup chấm bài,
nên sidebar khu Giảng viên không có đường vào cho tính năng "đọc bài nộp"
[SoT: .nexa/control/decision-registry.md:3472-3474]. Chủ dự án chỉ đạo ngày 2026-10-13 thêm một mục sidebar "Bài nộp" mở
**danh sách mọi lượt nộp của học viên trong các lớp mình phụ trách**, mỗi dòng mở trang kết quả chỉ đọc
[SoT: .nexa/control/decision-registry.md:3462-3470].

Việc giảng viên đọc được mã nguồn bài nộp của học viên trong lớp mình (chỉ đọc, ngoài lớp trả 404) đã được chốt ở
`DEC-2026-1013-instructor-avatar-menu-and-read-source` [SoT: .nexa/control/decision-registry.md:3412-3415]; màn này chỉ
thêm **danh sách dẫn tới** trang kết quả đó.

**Màn này không có mã yêu cầu `Fx-nn` riêng.** Chủ dự án đã chốt **chưa cấp mã F nào ở thời điểm này**, có thể cấp sau
(206-Q10, `DEC-2026-1014-instructor-submissions-list-rules`, mục (10) [SoT: .nexa/control/decision-registry.md:3910-3911]).
Tài liệu này **không tự đặt mã** và **không đổi bất kỳ tổng số nào** ở `feature_catalog.*` hay `system_survey.md`.

File này mô tả **hành vi và UX ở mức yêu cầu**; không lặp lại đặc tả trang kết quả đã có ở
`01-rd/screens/users/USR0201_submission_result.md` mà chỉ trỏ tới.

---

## 2. Phạm vi

| Hành vi | Mã | Nguồn |
| :--- | :--- | :--- |
| Liệt kê mọi lượt nộp của học viên thuộc các lớp giảng viên phụ trách, chỉ bài đã giao cho lớp, một dòng mỗi lượt nộp, mới nhất trước | Chưa cấp mã F (206-Q10) | `DEC-2026-1013-instructor-submissions-list`; `DEC-2026-1014-instructor-submissions-list-rules` |
| Bài nộp của học viên đã rời lớp vẫn hiện kèm ghi chú "Đã rời lớp" (cần giữ lịch sử ghi danh) | Chưa cấp mã F | `DEC-2026-1014-instructor-submissions-list-rules` mục (5) |
| Lọc theo lớp; lọc theo kết quả (Tất cả / Đã chấp nhận / Chưa đạt); tìm theo học viên hoặc bài toán | Suy luận | chỉ đạo chủ dự án 2026-10-13; `DEC-2026-1013-instructor-submissions-list` |
| Phân trang 20 dòng mỗi trang | Suy luận | `DEC-2026-1014-instructor-batch5-6-nav-pagination-rubric` mục (2) |
| Trạng thái lọc nằm trong URL `?classId=&result=&q=&page=` | Suy luận | `DEC-2026-1013-teacher-open-questions-closed-round2` mục (1) |
| Mở trang kết quả chỉ đọc `/instructor/submissions/{id}` (mang học viên và lớp trên query) | Suy luận | `DEC-2026-1013-instructor-avatar-menu-and-read-source` |
| Phạm vi theo lớp phụ trách, ngoài lớp trả 404 (trừ học viên đã rời lớp, với bài nộp lúc còn trong lớp) | F1-10, F1-12 | `01-rd/req/F1_identity.md:51-64`, `:177-178`; `DEC-2026-1014-instructor-submissions-list-rules` |

### 2.1 Cấu trúc hệ thống liên quan

| Màn/tài liệu | Vai trò | Bounded Context |
|---|---|---|
| Màn hiện tại | `INS0206` / `submission_management` | `judge-orchestration` |
| Màn đích của mỗi dòng | `USR0201` / `submission_result`, vùng `instructor` (chỉ đọc) | `judge-orchestration` |
| Màn chị em | `INS0204` / `class_student_detail` — lịch sử nộp của một học viên, dùng chung trang kết quả chỉ đọc | `identity` |
| Màn chị em | `INS0203` / `class_progress` — danh sách học viên, cùng quy ước lọc ghi vào URL | `identity` |
| Prototype | `05-coding/frontend/src/views/teacher/submission-management/` (bằng chứng, không phải nguồn yêu cầu) | Bằng chứng bố cục và trạng thái |

**Cấu trúc nội dung màn** (thứ tự dọc và chi tiết hình thức do BD đề xuất):

1. **Đầu trang** — tiêu đề "Bài nộp" và dòng mô tả. Không có dải thẻ số liệu (xem REQ-09).
2. **Thanh lọc** — ô tìm kiếm (học viên hoặc bài toán), bộ lọc Lớp, bộ lọc Kết quả, số dòng khớp.
3. **Bảng bài nộp** — mỗi dòng một lượt nộp, mới nhất trước; có cột tỉ lệ testcase đạt; tên học viên là liên kết sang
   `INS0204` (trừ học viên đã rời lớp, hiện ghi chú "Đã rời lớp"); mở được trang kết quả chỉ đọc.
4. **Phân trang** — 20 dòng mỗi trang.

**Điểm vào:** mục "Bài nộp" ở sidebar khu Giảng viên, nằm trong nhóm "Quản lý bài tập đã nộp" cùng mục "Bài giao cho lớp"
(REQ-18). **Sáng sidebar:** mục "Bài nộp" sáng ở màn này và cả ở trang kết
quả mở từ đây, theo quy tắc "sidebar sáng mục của màn gọi tới" [SoT: .nexa/control/decision-registry.md:3761-3762].

---

## 3. Ngoài phạm vi (Out of Scope)

- Đặc tả trang kết quả chỉ đọc `/instructor/submissions/{id}`: thuộc mục "Vùng giảng viên (A2) đọc bài nộp của học viên"
  trong `01-rd/screens/users/USR0201_submission_result.md` (chương 10); màn này chỉ liên kết tới, không mô tả lại.
- Bài nộp tự luyện của học viên (bài không được giao cho lớp): không hiện ở danh sách (REQ-15).
- Cấp mã chức năng `Fx-nn` cho màn: chưa làm (206-Q10).
- Chạy lại, sửa mã, gọi AI (Solution Review, Mock Interview) trên bất kỳ bài nộp nào tại đây: giảng viên chỉ đọc
  [SoT: .nexa/control/decision-registry.md:3412-3415]; chấm lại đã loại khỏi phạm vi (`DEC-2026-0828-remove-rejudge-scope`,
  trích ở `01-rd/screens/teacher/INS0205_class_detail.md:88-89`).
- Chấm tay: không thuộc màn này (hàng đợi chấm tay có tài liệu riêng, `INS0301`).
- Xuất file (CSV...), thao tác hàng loạt, sắp xếp theo cột tuỳ chọn: không có yêu cầu nguồn (tiêu chí ponytail bậc 1).
- Bài nộp của học viên ngoài các lớp giảng viên phụ trách: không hiện ở danh sách; mở thẳng URL trả 404. Học viên đã rời
  lớp không thuộc nhóm "ngoài lớp" đối với bài nộp họ làm lúc còn trong lớp (REQ-02, REQ-13).
- Layout, spacing, bảng màu, component cụ thể: thuộc BD. Hợp đồng API: thuộc DD (chưa viết; DD màn hình viết sau
  prototype, `CLAUDE.md` mục Process).

---

## 4. Tiền đề và ràng buộc

Cả 10 câu hỏi dưới đây đã được chủ dự án chốt ngày 2026-10-14 qua `DEC-2026-1014-instructor-submissions-list-rules`
[SoT: .nexa/control/decision-registry.md:3891-3926]; câu hỏi cũ giữ lại dạng gạch ngang để truy vết, cột "Vì sao chưa trả lời được"
của V0.1 không còn áp dụng. Chưa còn câu hỏi mở nào cho màn này (Bảng 4.1).

| # | Câu hỏi (cũ) | Ghi chú | Quyết định | Chủ sở hữu | Trạng thái |
| :-: | :--- | :--- | :--- | :--- | :-: |
| 206-Q1 | ~~Ngoài "Đã chấp nhận" và "Chưa đạt", các trạng thái đang chờ (`PENDING`, `COMPILING`, `JUDGING`) có hiện trong danh sách không, và nằm dưới lựa chọn lọc nào?~~ | ~~Xem V0.1~~ | **ĐÃ CHỐT 2026-10-14 (DEC-2026-1014-instructor-submissions-list-rules):** Bài đang chờ, đang biên dịch hoặc đang chấm chỉ hiện ở lựa chọn "Tất cả", kèm nhãn trạng thái; không thêm lựa chọn lọc thứ tư; "Chưa đạt" nghĩa là đã chấm xong và không đạt (REQ-04). Ponytail bậc 5. | Chủ dự án | Đã đóng |
| 206-Q2 | ~~Thứ tự mặc định của danh sách.~~ | ~~Xem V0.1~~ | **ĐÃ CHỐT 2026-10-14 (DEC-2026-1014-instructor-submissions-list-rules):** Mới nhất trước theo thời điểm nộp, cố định, không có điều khiển sắp xếp (REQ-14). Ponytail bậc 5. | Chủ dự án | Đã đóng |
| 206-Q3 | ~~Mỗi dòng có hiện điểm tỷ lệ testcase (F4-13, dạng "đạt/tổng") không?~~ | ~~Xem V0.1~~ | **ĐÃ CHỐT 2026-10-14 (DEC-2026-1014-instructor-submissions-list-rules):** Có một cột nhỏ hiện tỉ lệ testcase đạt (F4-13); bài chưa chấm xong hiện "-" (REQ-16). Ponytail bậc 4. | Chủ dự án | Đã đóng |
| 206-Q4 | ~~Tên học viên ở mỗi dòng có là liên kết sang `INS0204` (hồ sơ học viên trong lớp) không?~~ | ~~Xem V0.1~~ | **ĐÃ CHỐT 2026-10-14 (DEC-2026-1014-instructor-submissions-list-rules):** Có: tên học viên liên kết sang `INS0204` của đúng lớp trên dòng đó; học viên đã rời lớp thì không có liên kết (REQ-17). | Chủ dự án | Đã đóng |
| 206-Q5 | ~~Học viên đã rời lớp (bị gỡ hoặc tự rời) thì bài nộp cũ hiện thế nào?~~ | ~~Xem V0.1~~ | **ĐÃ CHỐT 2026-10-14 (DEC-2026-1014-instructor-submissions-list-rules):** **Không theo đề xuất V0.1.** Bài nộp của học viên đã rời lớp VẪN nằm trong danh sách, kèm ghi chú "Đã rời lớp", không có liên kết sang `INS0204`; giảng viên vẫn đọc được các bài nộp làm lúc học viên còn trong lớp (REQ-02, REQ-13). Hệ quả: lịch sử ghi danh lớp phải được giữ lại để phạm vi lớp vẫn đúng với bài nộp lúc còn trong lớp [SoT: Suy luận, hệ quả thiết kế cho BD, đúng như ghi ở .nexa/control/decision-registry.md:3903-3904]. | Chủ dự án | Đã đóng |
| 206-Q6 | ~~Học viên nộp một bài ngoài phạm vi bài giao của lớp (tự luyện) có hiện không?~~ | ~~Xem V0.1~~ | **ĐÃ CHỐT 2026-10-14 (DEC-2026-1014-instructor-submissions-list-rules):** Không: danh sách chỉ gồm bài thuộc các bài đã giao cho lớp, cùng quy tắc với `INS0204`, không gồm bài tự luyện (REQ-15). | Chủ dự án | Đã đóng |
| 206-Q7 | ~~Học viên thuộc nhiều lớp của cùng giảng viên: một lượt nộp hiện một dòng hay một dòng mỗi lớp?~~ | ~~Xem V0.1~~ | **ĐÃ CHỐT 2026-10-14 (DEC-2026-1014-instructor-submissions-list-rules):** Một dòng mỗi lượt nộp, gắn với lớp đang giữ bài giao tương ứng (REQ-15). | Chủ dự án | Đã đóng |
| 206-Q8 | ~~Tên nhóm sidebar chứa mục "Bài nộp".~~ | ~~Xem V0.1~~ | **ĐÃ CHỐT 2026-10-14 (DEC-2026-1014-instructor-submissions-list-rules):** Nhóm là "Quản lý bài tập đã nộp", gồm hai mục "Bài giao cho lớp" và "Bài nộp" (REQ-18). Việc đặt nhãn thuộc BD `_shell`. | Chủ dự án | Đã đóng |
| 206-Q9 | ~~Trang kết quả vùng `instructor` chưa có trong RD `USR0201`.~~ | ~~Xem V0.1~~ | **ĐÃ CHỐT 2026-10-14 (DEC-2026-1014-instructor-submissions-list-rules):** Đặc tả thành một mục ngắn "Vùng giảng viên (A2) đọc bài nộp của học viên" ngay trong RD `USR0201` (chương 10, cùng trang, không tạo tài liệu trùng); BD `USR0201` sẽ bổ sung tương ứng (REQ-08). | Trợ lý RD | Đã đóng |
| 206-Q10 | ~~Mã chức năng `Fx-nn` cho màn này.~~ | ~~Xem V0.1~~ | **ĐÃ CHỐT 2026-10-14 (DEC-2026-1014-instructor-submissions-list-rules):** Chưa cấp mã F ở thời điểm này; chủ dự án có thể cấp sau và khi đó mới cập nhật tổng số ở `feature_catalog.*`. | Chủ dự án | Đã đóng |

---

## 5. Danh sách yêu cầu (REQ)

| ID | Yêu cầu | Loại | Nguồn/SoT |
|---|---|---|---|
| REQ-01 | Có màn danh sách bài nộp cho giảng viên tại `/instructor/submissions`, mục sidebar "Bài nộp" | Chức năng | `DEC-2026-1013-instructor-submissions-list` |
| REQ-02 | Danh sách chứa mọi lượt nộp của học viên trong các lớp giảng viên đang phụ trách (A2), gác bởi Function `CLASS_MANAGEMENT`, phạm vi theo lớp, cùng cơ chế với các màn Giảng viên khác. Mở trang kết quả của bài ngoài các lớp đó trả **404**, không phải 403. **Ngoại lệ:** học viên đã rời lớp vẫn thuộc phạm vi đối với các bài nộp làm lúc còn trong lớp; giảng viên giữ quyền đọc các bài nộp đó (xem REQ-13) | Chức năng, bảo mật | `01-rd/req/F1_identity.md:177-178`; `.nexa/control/decision-registry.md:3412-3415`; `DEC-2026-1014-instructor-submissions-list-rules` mục (5) |
| REQ-03 | Bộ lọc Lớp: "Tất cả" hoặc một lớp trong các lớp giảng viên phụ trách | Chức năng | `DEC-2026-1013-instructor-submissions-list` |
| REQ-04 | Bộ lọc Kết quả có ba lựa chọn: Tất cả, Đã chấp nhận, Chưa đạt. "Chưa đạt" nghĩa là đã chấm xong và không đạt. Bài đang chờ, đang biên dịch hoặc đang chấm chỉ hiện ở "Tất cả", kèm nhãn trạng thái, không thuộc "Đã chấp nhận" hay "Chưa đạt"; không có lựa chọn lọc thứ tư. **ĐÃ CHỐT 2026-10-14 (DEC-2026-1014-instructor-submissions-list-details)**: lượt nộp ở trạng thái cuối nhưng do hệ thống (`SYSTEM_ERROR`) cũng chỉ hiện ở "Tất cả", không bao giờ thuộc "Chưa đạt"; bộ trạng thái là của BD/DB (có `SYSTEM_ERROR`, không có `MEMORY_LIMIT_EXCEEDED`), bản dựng sẽ sửa theo. **GWT:** Cho một bài nộp đang chấm, khi tôi chọn "Chưa đạt", thì dòng đó không hiện; khi tôi chọn "Tất cả", thì dòng đó hiện với nhãn trạng thái đang chấm | Chức năng (GWT) | chỉ đạo chủ dự án 2026-10-13; `DEC-2026-1014-instructor-submissions-list-rules` mục (1); `DEC-2026-1014-instructor-submissions-list-details` mục (3) |
| REQ-05 | Ô tìm kiếm khớp theo tên học viên hoặc tên bài toán | Chức năng | `DEC-2026-1013-instructor-submissions-list` |
| REQ-06 | Phân trang 20 dòng mỗi trang, theo quy ước bảng gộp nhiều dòng của khu Giảng viên | Chức năng | `.nexa/control/decision-registry.md:3763-3766` |
| REQ-07 | Trạng thái danh sách ghi vào URL `?classId=&result=&q=&page=`; tải lại hoặc quay lại bằng trình duyệt giữ nguyên bộ lọc. Đổi bộ lọc hoặc từ khoá thì về trang 1. **ĐÃ CHỐT 2026-10-14 (DEC-2026-1014-instructor-submissions-list-details)**: từ trang kết quả quay lại danh sách (khi đã mở từ danh sách) thì khôi phục đúng `?classId=&result=&q=&page=` như lúc rời | Chức năng | `.nexa/control/decision-registry.md:3437-3440`; "về trang 1" là `[SoT: Suy luận]` (prototype `submission-management-view.tsx:175,185,202` làm vậy, chưa có quyết định) |
| REQ-08 | Mỗi dòng mở trang kết quả chỉ đọc `/instructor/submissions/{id}`, mang học viên và lớp trên query. Trang này được đặc tả thành một mục ngắn trong `USR0201_submission_result` (chương 10, "Vùng giảng viên (A2) đọc bài nộp của học viên"), màn này chỉ liên kết. **ĐÃ CHỐT 2026-10-14 (DEC-2026-1014-instructor-submissions-list-details)**: liên kết mang thêm `from=submissions` để trang kết quả biết màn gọi tới (breadcrumb và sidebar); breadcrumb về danh sách bằng cách lùi lịch sử trình duyệt; trang kết quả không realtime và máy chủ lấy học viên, lớp từ lượt nộp (REQ-17 đến REQ-19 của `USR0201` chương 10) | Chức năng | `.nexa/control/decision-registry.md:3467-3469`; `DEC-2026-1014-instructor-submissions-list-rules` mục (9) |
| REQ-09 | Không có dải thẻ số liệu (stat-card strip) ở đầu màn; màn là danh sách, không lặp số đã hiện ở màn khác | Phi chức năng, UX | Quy tắc chủ dự án 2026-10-13 (`CLAUDE.md`, mục Rules); `.nexa/control/decision-registry.md:3469-3470` |
| REQ-10 | Mục sidebar "Bài nộp" sáng ở màn này và ở trang kết quả mở từ màn này | Chức năng | `.nexa/control/decision-registry.md:3761-3762` |
| REQ-11 | Chỉ đọc: giảng viên không chạy lại, không sửa mã, không gọi AI trên bất kỳ bài nào ở màn này hay ở trang kết quả mở từ đây | Chức năng, bảo mật | `.nexa/control/decision-registry.md:3412-3415` |
| REQ-12 | Màn có đủ trạng thái: **đang tải**; **rỗng** gồm ba trường hợp tách bạch (giảng viên chưa có lớp nào, lớp chưa có bài nộp nào, bộ lọc/tìm kiếm không khớp dòng nào); **lỗi tải** kèm hành động thử lại | Chức năng, UX | Yêu cầu chủ phiên giao; tương tự trạng thái rỗng/lỗi ở `01-rd/req/F1_identity.md:174-176` |
| REQ-13 | Học viên đã rời lớp (bị gỡ hoặc tự rời): các bài nộp của họ làm lúc còn trong lớp **vẫn nằm trong danh sách**, kèm ghi chú "Đã rời lớp"; giảng viên vẫn mở đọc được các bài nộp đó. Vì thế lịch sử ghi danh lớp phải được giữ lại (không xoá khi học viên rời lớp) để phạm vi lớp còn đúng với bài nộp lúc học viên còn trong lớp. **ĐÃ CHỐT 2026-10-14 (DEC-2026-1014-instructor-submissions-list-details)**: học viên rời rồi vào lại cùng lớp thì khoảng thời gian đã rời **tính là "trong lớp"** (dòng ghi danh cũ được dùng lại, ngày vào giữ nguyên, ngày rời về rỗng); không có lịch sử từng đoạn tham gia. **GWT:** Cho học viên H đã nộp bài B khi còn trong lớp L của tôi rồi rời lớp, khi tôi mở danh sách bài nộp, thì dòng của B vẫn hiện với ghi chú "Đã rời lớp"; khi tôi mở dòng đó, thì trang kết quả hiện bình thường, không trả 404 | Chức năng, dữ liệu | "+DEC+" mục (5); phần "giữ lịch sử ghi danh" là hệ quả thiết kế [SoT: Suy luận, đúng như ghi ở `.nexa/control/decision-registry.md:3903-3904`] |
| REQ-14 | Thứ tự mặc định: mới nhất trước theo thời điểm nộp, cố định, không có điều khiển sắp xếp | Chức năng | "+DEC+" mục (2) |
| REQ-15 | Danh sách chỉ gồm bài nộp của các bài **đã giao cho lớp** (cùng quy tắc với `INS0204`), không gồm bài tự luyện. Mỗi lượt nộp đúng một dòng, gắn với lớp đang giữ bài giao tương ứng; lọc theo lớp chỉ hiện lượt nộp của bài giao cho lớp đó. **ĐÃ CHỐT 2026-10-14 (DEC-2026-1014-instructor-submissions-list-details)**: khi học viên ở hai lớp của cùng giảng viên và cả hai lớp cùng giao một bài, lượt nộp gắn với lớp có lần giao mới nhất không sau thời điểm nộp. **GWT:** Cho học viên vừa nộp một bài không được giao cho lớp nào của tôi, khi tôi mở danh sách, thì không thấy lượt nộp đó | Chức năng (GWT) | "+DEC+" mục (6), (7); `DEC-2026-1014-instructor-submissions-list-details` mục (1); `01-rd/screens/teacher/INS0204_class_student_detail.md:103` |
| REQ-16 | Mỗi dòng có một cột nhỏ hiện tỉ lệ testcase đạt (F4-13, dạng "đạt/tổng"); bài chưa chấm xong hiện "-". **GWT:** Cho bài nộp đã chấm 6/10 testcase đạt, khi tôi xem dòng đó, thì cột tỉ lệ hiện "6/10"; cho bài đang chấm, thì cột hiện "-" | Chức năng (GWT) | "+DEC+" mục (3); `01-rd/req/F4_judge-orchestration.md:23-25` |
| REQ-17 | Tên học viên ở mỗi dòng là liên kết sang `INS0204` của đúng lớp trên dòng đó. Học viên đã rời lớp thì **không có liên kết** (chỉ hiện tên và ghi chú "Đã rời lớp"), vì `INS0204` không còn hồ sơ trong lớp cho học viên đó. Cả dòng vẫn mở trang kết quả. **GWT:** Cho dòng của học viên còn trong lớp, khi tôi bấm tên, thì sang `INS0204` của lớp đó; cho dòng của học viên đã rời lớp, thì tên không bấm được | Chức năng (GWT) | "+DEC+" mục (4) |
| REQ-18 | Sidebar khu Giảng viên có nhóm "Quản lý bài tập đã nộp" gồm hai mục "Bài giao cho lớp" và "Bài nộp" | Chức năng | "+DEC+" mục (8) |
| REQ-19 | Màn không cấp mã chức năng `Fx-nn` ở thời điểm này; chủ dự án có thể cấp sau | Quy trình | "+DEC+" mục (10) |

---

## 6. Yêu cầu dữ liệu

### 6.1 Dữ liệu tham chiếu

Mỗi dòng cần tối thiểu, ở mức yêu cầu: học viên, lớp, bài toán, ngôn ngữ nộp, kết quả/trạng thái, thời điểm nộp, tỉ lệ
testcase đạt (REQ-16) và cờ "học viên đã rời lớp" (REQ-13). Danh sách trạng thái chấm dùng chung bộ trạng thái của trang kết quả
(prototype: `05-coding/frontend/src/entities/submission/model/status-variant.ts:5-16`, chín trạng thái). Bảng, cột, kiểu
dữ liệu thuộc BD/DD.

### 6.2 Luồng dữ liệu

Quyết định ghi hệ quả: DD `judge-orchestration` cần **một endpoint liệt kê bài nộp theo tập mã lớp** của giảng viên, và
kiểm tra phạm vi lớp ở trang kết quả (404 ngoài lớp) [SoT: .nexa/control/decision-registry.md:3476-3478]. Theo `DEC-2026-1014-instructor-submissions-list-rules` phần hệ quả, endpoint này còn trả thuộc tính "đã rời lớp", và `identity` phải giữ lịch sử ghi danh [SoT: .nexa/control/decision-registry.md:3917-3921]. Request,
response, mã lỗi thuộc DD (chưa viết).

### 6.3 Bằng chứng từ prototype (không phải yêu cầu)

| Điều quan sát | Vị trí | Ghi chú |
|---|---|---|
| Trạng thái lớp/kết quả/tìm/trang đọc ghi qua URL | `submission-management-view.tsx:50-63` | Khớp REQ-07 |
| Kích thước trang mặc định 10, chọn được 10/20/50 | `submission-management-view.tsx:39`, `:64-68` | **Lệch** REQ-06 (20 dòng cố định); chủ dự án chốt ghi nợ prototype, không đổi yêu cầu (`DEC-2026-1014-instructor-submissions-list-rules`) |
| Số dòng khớp hiển thị cạnh bộ lọc | `submission-management-view.tsx:211-213` | Chi tiết hình thức, thuộc BD |
| Không có dải thẻ số liệu | `submission-management-view.tsx:159-252` | Khớp REQ-09 |
| Bài ngoài lớp: "Không tìm thấy bài nộp." | `05-coding/frontend/e2e/instructor-submission-readonly.spec.ts:6-11` | Khớp REQ-02 |
| Điều hướng từ sidebar, sáng mục `aria-current`, lọc ghi URL, mở dòng sang kết quả | `instructor-submission-readonly.spec.ts:49-89` | Khớp REQ-07, REQ-08, REQ-10 |
| Dữ liệu dòng là mock: mỗi học viên hai bài nộp đọc được | `model/rows.ts:3-5`, `:30-34` | Chỉ là dữ liệu mock |
| Trang kết quả vùng instructor: 404 ngoài lớp, breadcrumb về `/instructor/submissions`, ẩn nút hành động học viên | `submission-result-view.tsx:56-57`, `:74`, `:115`, `:312` | Bằng chứng cho 206-Q9 |

---

## 7. RACI và danh sách bàn giao

| Bàn giao | Trạng thái |
|---|---|
| RD màn hình | Có tài liệu này (V0.2, 2026-10-14, đã đóng 206-Q1 đến Q10) |
| BD màn hình | Có (`02-bd/screens/teacher/INS0206_submission_management.md`, V0.2) |
| DD/API liên quan | Chưa viết — DD màn hình viết sau prototype; DD `judge-orchestration` cần endpoint liệt kê theo tập lớp |
| Bổ sung RD `USR0201` cho vùng `instructor` | Xong: chương 10 của `USR0201` (206-Q9) |
| Bổ sung BD `USR0201` cho vùng `instructor`; hàng "Bài nộp" ở `_shell.md` | Xong (BD `USR0201` V0.5 và V0.6; `_shell.md` do phía khác phụ trách) |

---

## 8. Kế hoạch và quy mô

| Chỉ số | Giá trị |
|---|---|
| Quy mô hành vi | 19 yêu cầu ở chương 5. |
| Lịch, nhân sự, công sức | Chưa xác định — không tự suy luận. |

---

## 9. Tham chiếu

| Loại | Tham chiếu |
|---|---|
| Yêu cầu module | `01-rd/req/F1_identity.md` — F1-10, F1-12 (phạm vi theo lớp); `01-rd/req/F4_judge-orchestration.md` — F4-13 (điểm tỷ lệ testcase). |
| Màn hình liên quan | `01-rd/screens/users/USR0201_submission_result.md` — trang kết quả, đích của mỗi dòng; chương 10 là vùng giảng viên. |
| Màn hình liên quan | `01-rd/screens/teacher/INS0204_class_student_detail.md` — lịch sử nộp một học viên; `01-rd/screens/teacher/INS0205_class_detail.md` — màn chị em, cấu trúc tài liệu mẫu. |
| Quyết định | `DEC-2026-1014-instructor-submissions-list-details` (chi tiết cuối: lớp của lượt nộp, quay lại danh sách, `SYSTEM_ERROR`, rời rồi vào lại, `from`), `DEC-2026-1014-instructor-submissions-list-rules` (chốt 206-Q1 đến Q10), `DEC-2026-1013-instructor-submissions-list`, `DEC-2026-1013-instructor-avatar-menu-and-read-source`, `DEC-2026-1013-teacher-open-questions-closed-round2`, `DEC-2026-1014-instructor-batch5-6-nav-pagination-rubric`, `DEC-2026-1014-instructor-batch1-counting-rules`, `DEC-2026-1014-instructor-batch2-weekly-rules`. |
| Bằng chứng prototype | `05-coding/frontend/src/views/teacher/submission-management/`, `05-coding/frontend/e2e/instructor-submission-readonly.spec.ts`, `05-coding/frontend/src/views/users/submission-result/`. |
| Tài liệu gốc | `README.md` mục 3 và mục 4 — định nghĩa Bounded Context và actor. |
