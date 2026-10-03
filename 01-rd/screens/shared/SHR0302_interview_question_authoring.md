# RD (Yêu cầu hệ thống mới) — Biên soạn câu hỏi phỏng vấn / `SHR0302`

> Mã màn hình: `SHR0302` [SoT: 02-bd/_rules/bd-template-9sheet.md — mục 8].
> Slug chính tắc: `interview_question_authoring` [SoT: 01-rd/overview/system_survey.md — mục 7.0 dòng `interview_question_authoring`].
> Phạm vi/Bounded Context: `interview-bank` (F6). Actor: A2, A3 (màn dùng chung, phạm vi dữ liệu theo quyền).
> Nguồn sự thật (SoT): `01-rd/req/interview-bank.md` (F6-13), `01-rd/req/identity.md` (F1-12, F1-14), `01-rd/req/user_stories/a2_instructor.md` (`US-A2-10`).
> Tài liệu này có thể đọc độc lập.

---

## 1. Mục đích và bối cảnh

Nơi A2/A3 tạo mới hoặc sửa **một câu hỏi phỏng vấn** trong ngân hàng dùng chung: nội dung + phân loại, danh
sách câu hỏi đào sâu, và bộ tiêu chí đánh giá có trọng số — rồi câu hỏi đó sẵn sàng cho học viên dùng ở Chế
độ học/Chế độ luyện [SoT: 01-rd/req/interview-bank.md — F6-13]. Đây là màn "thượng nguồn" của F6-08 (AI đối
chiếu Chế độ luyện): một câu hỏi thiếu bộ tiêu chí ở đây sẽ bị ẩn khỏi Chế độ luyện phía người học
[SoT: 01-rd/req/interview-bank.md — F6-13, mục "Câu hỏi thiếu tiêu chí đánh giá"].

**Actor: A2 và A3 — màn dùng chung, phạm vi dữ liệu theo quyền**, gác bởi Function
`INTERVIEW_BANK_MANAGEMENT` (`01-rd/req/identity.md` — F1-12), cùng cơ chế dùng chung đã áp dụng cho
`problem_authoring`/`problem_management` (`DEC-2026-0825-shared-content-authoring-screens`) — không có
phạm vi riêng theo lớp, vì F6-11 (bộ câu hỏi riêng theo lớp) đã loại khỏi phạm vi 2026-08-28
[SoT: 01-rd/req/interview-bank.md — F6-13].

**Slug mới, chưa có prototype (`[Đợi nextjs]`).** Chốt 2026-08-30, qua hỏi trực tiếp chủ dự án khi trả lời
Câu hỏi mở Q4 của `01-rd/screens/shared/SHR0301_interview_question_management.md`, cùng `DEC-2026-0830-interview-bank-crud`. Route: `/instructor/interview-questions/[id]`,
`/admin/interview-questions/[id]` — **chốt giữ nguyên đề xuất 2026-09-01** (Câu hỏi mở Q6).
**Cập nhật 2026-10-02 (`DEC-2026-1002-split-detail-and-edit-pages`):** khu Admin, `/admin/interview-questions/[id]` nay là
trang chỉ đọc `interview_question_info` (`SHR0303`); màn soạn này chuyển sang `/admin/interview-questions/[id]/edit`, và
`/admin/interview-questions/new` là chế độ tạo mới. **Cập nhật 2026-10-03:** khu Giảng viên làm theo, cùng cấu trúc (`/instructor/interview-questions/[id]` chỉ đọc, `/instructor/interview-questions/[id]/edit` và `/instructor/interview-questions/new` là màn soạn) — phần khoá route của Q6 không còn đúng ở cả hai khu [SoT: 05-coding/frontend/src/app/(instructor)/instructor/interview-questions/[questionId]/edit/page.tsx:1-10]. Cấu trúc màn dưới đây **suy diễn song song
theo tiền lệ `problem_authoring`** (một màn soạn riêng cho một bản ghi phức tạp, không phải modal/drawer)
— quyết định gốc đã dẫn rõ chính tiền lệ này khi cắt bỏ phương án modal, không phải suy diễn tự do.

---

## 2. Phạm vi

| Hành vi | Mã | Nguồn |
| :--- | :--- | :--- |
| Quản trị nội dung ngân hàng câu hỏi dùng chung: tạo, sửa, nhân bản, xoá | F6-13 | `01-rd/req/interview-bank.md` — F6-13 (bổ sung 2026-08-30) |
| Nội dung + phân loại theo chủ đề/độ khó (chủ đề đọc từ danh mục do ADMIN quản lý; 5 chủ đề khởi tạo: Lý thuyết CS, System design, Database, Ngôn ngữ, Hành vi — cập nhật 2026-10-01) | F6-01 | `01-rd/req/interview-bank.md` — F6-01 |
| Bộ tiêu chí đánh giá có trọng số phần trăm — tiêu chí chuẩn mà F6-08 đối chiếu khi chấm Chế độ luyện | F6-13, F6-08 | `01-rd/req/interview-bank.md` — F6-13, F6-08 |
| Câu hỏi thiếu tiêu chí đánh giá: vẫn hiện ở Chế độ học, ẩn khỏi Chế độ luyện | F6-13, F6-08 | `01-rd/req/interview-bank.md` — F6-13 |
| Xoá mềm: đánh dấu ngừng dùng, ẩn khỏi màn phía học viên, không cascade xoá phiên cũ | F6-13 | `01-rd/req/interview-bank.md` — F6-13 |
| Mọi thao tác thêm/sửa/nhân bản/xoá ghi vào Nhật ký hệ thống | F6-13, F1-14 | `01-rd/req/interview-bank.md` — F6-13; `01-rd/req/identity.md` — F1-14 |
| Given-When-Then liên quan | — | `01-rd/req/user_stories/a2_instructor.md` (`US-A2-10`) |
| Gác quyền: `INTERVIEW_BANK_MANAGEMENT` là một `FUNCTION` trong ma trận phân quyền, cùng nhóm F6-12 | F1-12 | `01-rd/req/identity.md` — F1-12 |

Chưa có prototype để đối chiếu — cấu trúc dưới đây suy diễn trực tiếp từ "4 nhóm trường lồng nhau" đã chốt ở
F6-13, theo đúng khuôn hình `problem_authoring` (thanh đầu trang + nội dung theo nhóm + hành động lưu/xuất
bản) `[SoT: Suy luận, song song tiền lệ problem_authoring — không tự thêm khối nào ngoài 4 nhóm đã chốt]`:

1. **Thanh đầu trang** — nút quay lại `interview_question_management` (chỉ có biểu tượng mũi tên, đặt
   **bên trái** tiêu đề, tooltip "Quay lại"); tiêu đề: "Soạn câu hỏi phỏng vấn" khi tạo mới, "Sửa câu hỏi
   {mã}" khi đang sửa; bên phải gồm trạng thái lưu, nút **"Xem như học viên"** (preview, chốt 2026-09-01 —
   cùng khuôn mẫu `problem_authoring`; chỉ có biểu tượng mắt, tooltip tên nút) và đúng một nút "Lưu" (không có khái niệm "xuất bản" riêng như `problem_authoring` — chốt
   2026-09-01: câu hỏi hiện ngay cho học viên khi lưu, trừ khi bị ẩn ở Chế độ luyện do thiếu tiêu chí, xem
   mục 2). `[SoT: Suy luận]`.
2. **Nhóm 1 — Nội dung và phân loại** — nội dung câu hỏi, chủ đề (chọn từ danh mục đọc từ dữ liệu, F6-01), độ khó
   [SoT: 01-rd/req/interview-bank.md — F6-01, F6-13]. **Cập nhật 2026-10-01
   (`DEC-2026-1001-admin-configurable-settings`):** danh mục chủ đề không còn cố định 5 giá trị — do ADMIN
   quản lý ở hộp thoại "Quản lý chủ đề" của `interview_question_management` (`SHR0301`). Màn soạn này chỉ
   **chọn** chủ đề có sẵn, không tạo chủ đề mới; A2 không có đường tạo chủ đề, ADMIN thấy gợi ý/liên kết
   "Quản lý chủ đề" ngay cạnh ô chọn để mở danh mục.
3. **Nhóm 2 — Câu hỏi đào sâu** — danh sách văn bản tự do, không giới hạn số lượng, không có độ khó riêng
   (chốt 2026-09-01) các câu hỏi truy vấn tiếp theo cùng câu hỏi gốc, dùng ở giai đoạn Phản biện của Phỏng
   vấn giả lập (F5-11) — dữ liệu này **không phải** F6-04/05/06 (khung trả lời/từ khoá của Chế độ học), độc
   lập hoàn toàn [SoT: 01-rd/req/interview-bank.md — F6-13, mục "danh sách câu hỏi đào sâu"].
4. **Nhóm 3 — Bộ tiêu chí đánh giá có trọng số** — danh sách tiêu chí, mỗi tiêu chí có trọng số phần trăm,
   **tổng bắt buộc = 100 (chặn lưu nếu sai, chốt 2026-09-01)**, dùng làm mốc tính điểm khi F6-08 đối chiếu
   Chế độ luyện — độc lập với hai rubric của F5 (F5-15, F5-23) [SoT: 01-rd/req/interview-bank.md — F6-13].
5. **Nhóm 4 — Hành động quản trị** — nhân bản (toàn bộ 4 nhóm, chốt 2026-09-01), xoá mềm (đánh dấu ngừng
   dùng) [SoT: 01-rd/req/interview-bank.md — F6-13].
6. **Không có thẻ chỉ số chất lượng nội dung** ở màn soạn này. Dải bốn thẻ chỉ số từng nằm ở màn danh sách
   `interview_question_management` và đã bỏ ngày 2026-10-01 (hệ thống chỉ giữ một trang tổng quan
   Admin/Giảng viên) — xem mục 2, điểm 3 của `01-rd/screens/shared/SHR0301_interview_question_management.md`.
   Nêu lại ở đây để tránh thêm nhầm vào màn soạn.
7. **Không có prototype HTML minh hoạ** — mọi chi tiết bố cục, màu sắc, breakpoint thuộc BD, viết khi dựng UI
   Next.js trực tiếp `[Đợi nextjs]`. **Cập nhật 2026-10-01 (owner instruction, đồng bộ theo bản dựng UI):**
   bố cục là lưới hai cột rộng toàn trang — cột rộng chứa Nhóm 1 (nội dung câu hỏi), Nhóm 2 (câu hỏi đào
   sâu) và Nhóm 3 (bộ tiêu chí đánh giá); cột hẹp cố định khi cuộn chứa phân loại (chủ đề, độ khó thuộc
   Nhóm 1) và, **chỉ ở chế độ sửa**, khối Hành động quản trị (Nhóm 4: Nhân bản, Ngừng dùng)
   [SoT: 05-coding/frontend/src/views/shared/interview-question-authoring/ui/interview-question-authoring-view.tsx:9-12, 160-161, 287, 305-321].
   Màn tải câu hỏi theo mã trên route; mã không tồn tại thì hiện trạng thái "không tìm thấy" kèm nút quay lại
   [SoT: 05-coding/frontend/src/views/shared/interview-question-authoring/ui/interview-question-authoring-view.tsx:124-136].

### 2.1 Cấu trúc hệ thống liên quan

| Màn/tài liệu | Vai trò | Bounded Context |
|---|---|---|
| Màn hiện tại | `SHR0302` / `interview_question_authoring` | `interview-bank` |
| Tài liệu yêu cầu | `01-rd/req/interview-bank.md` (F6-13) | Nguồn SoT của màn |
| Prototype | `[Đợi nextjs]` (chưa có prototype) | Bằng chứng bố cục và trạng thái |

---

## 3. Ngoài phạm vi (Out of Scope)

- Bảng màu, spacing, breakpoint, component cụ thể — thuộc BD
  (`02-bd/screens/shared/SHR0302_interview_question_authoring.md`), viết khi dựng UI Next.js trực tiếp
  (chưa có prototype trung gian).
- Hợp đồng API (tạo/sửa/nhân bản/xoá một câu hỏi, kiểm tra tổng trọng số) — thuộc DD
  (`03-dd/api/interview-bank.md`, chưa viết).
- Nội dung và bố cục của `interview_question_management` (màn cha, danh sách có lọc) — thuộc file RD
  riêng của nó, không lặp lại ở đây.
- Thuật toán AI đối chiếu Chế độ luyện dùng bộ tiêu chí này (F6-08) — thuộc logic của `ai-review`, không
  thuộc file theo trục màn này.

---

## 4. Tiền đề và ràng buộc

| # | Câu hỏi | Vì sao chưa trả lời được | Đề xuất | Chủ sở hữu |
| :-: | :--- | :--- | :--- | :--- |
| Q1 | ~~Tổng trọng số của bộ tiêu chí đánh giá có bắt buộc bằng 100 không?~~ **ĐÃ CHỐT (2026-09-01, qua hỏi trực tiếp chủ dự án):** bắt buộc tổng = 100, chặn lưu nếu sai. | — | Đã chốt. | Đã đóng |
| Q2 | ~~Có nút "Xem như học viên" (preview), giống `problem_authoring`, hay không?~~ **ĐÃ CHỐT (2026-09-01):** có, cùng khuôn mẫu — xem trước câu hỏi ở cả Chế độ học và Chế độ luyện trước khi lưu, không cần mã `Fx-nn` mới. | — | Đã chốt. | Đã đóng |
| Q3 | ~~Câu hỏi đào sâu (nhóm 2) là văn bản tự do hay có cấu trúc?~~ **ĐÃ CHỐT (2026-09-01):** danh sách văn bản tự do, không giới hạn số lượng, không có độ khó riêng — AI dùng nguyên văn khi truy vấn ở giai đoạn Phản biện (F5-11). | — | Đã chốt. | Đã đóng |
| Q4 | ~~Nhân bản một câu hỏi có nhân bản luôn cả bộ tiêu chí và câu hỏi đào sâu không?~~ **ĐÃ CHỐT (2026-09-01):** nhân bản toàn bộ 4 nhóm, giữ nguyên tiền lệ `problem_management`/F2-16 (nhân bản = sao chép toàn bộ nội dung). | — | Đã chốt. | Đã đóng |
| Q5 | ~~Nút "Lưu" có phân biệt "Lưu nháp" và "Lưu và công bố" như `problem_authoring`, hay câu hỏi hiện ngay khi lưu?~~ **ĐÃ CHỐT (2026-09-01):** không có vòng đời nháp/xuất bản riêng — câu hỏi hiện ngay cho học viên khi lưu (trừ khi tự động ẩn khỏi Chế độ luyện do thiếu tiêu chí, đã chốt ở mục 2). | — | Đã chốt. | Đã đóng |
| Q6 | ~~Route chính xác — giữ đề xuất hay đổi khi build FE?~~ **ĐÃ CHỐT (2026-09-01):** giữ đề xuất `/instructor/interview-questions/[id]`, `/admin/interview-questions/[id]` — khớp đúng khuôn mẫu route của `problem_authoring`. **Phần khoá route bị thay thế** bởi `DEC-2026-1002-split-detail-and-edit-pages`: khu Admin từ 2026-10-02, khu Giảng viên từ 2026-10-03 (`[id]` là trang chỉ đọc, form soạn ở `[id]/edit` và `/new`). | — | Đã chốt. | Đã đóng |

---

## 5. Danh sách yêu cầu (REQ)
| ID | Yêu cầu | Loại | Nguồn/SoT |
|---|---|---|---|
| REQ-01 | Quản trị nội dung ngân hàng câu hỏi dùng chung: tạo, sửa, nhân bản, xoá (F6-13) | Chức năng | `01-rd/req/interview-bank.md` — F6-13 (bổ sung 2026-08-30) |
| REQ-02 | Nội dung + phân loại theo chủ đề/độ khó (chủ đề chọn từ danh mục do ADMIN quản lý; 5 chủ đề khởi tạo: Lý thuyết CS, System design, Database, Ngôn ngữ, Hành vi) (F6-01) | Chức năng | `01-rd/req/interview-bank.md` — F6-01 |
| REQ-03 | Bộ tiêu chí đánh giá có trọng số phần trăm — tiêu chí chuẩn mà F6-08 đối chiếu khi chấm Chế độ luyện (F6-13, F6-08) | Chức năng | `01-rd/req/interview-bank.md` — F6-13, F6-08 |
| REQ-04 | Câu hỏi thiếu tiêu chí đánh giá: vẫn hiện ở Chế độ học, ẩn khỏi Chế độ luyện (F6-13, F6-08) | Chức năng | `01-rd/req/interview-bank.md` — F6-13 |
| REQ-05 | Xoá mềm: đánh dấu ngừng dùng, ẩn khỏi màn phía học viên, không cascade xoá phiên cũ (F6-13) | Chức năng | `01-rd/req/interview-bank.md` — F6-13 |
| REQ-06 | Mọi thao tác thêm/sửa/nhân bản/xoá ghi vào Nhật ký hệ thống (F6-13, F1-14) | Chức năng | `01-rd/req/interview-bank.md` — F6-13; `01-rd/req/identity.md` — F1-14 |
| REQ-07 | Given-When-Then liên quan (—) | Chức năng | `01-rd/req/user_stories/a2_instructor.md` (`US-A2-10`) |
| REQ-08 | Gác quyền: `INTERVIEW_BANK_MANAGEMENT` là một `FUNCTION` trong ma trận phân quyền, cùng nhóm F6-12 (F1-12) | Chức năng | `01-rd/req/identity.md` — F1-12 |

---

## 6. Yêu cầu dữ liệu

### 6.1 Dữ liệu tham chiếu

Chi tiết bảng, cột, kiểu dữ liệu và ràng buộc lưu trữ thuộc BD/DD; tài liệu này không tự suy luận dữ liệu chưa được chốt.

### 6.2 Luồng dữ liệu

Hợp đồng API, request/response và mã lỗi thuộc DD. Luồng dữ liệu mức yêu cầu được thể hiện bởi các hành vi ở chương 5.

---

## 7. RACI và danh sách bàn giao

| Bàn giao | Trạng thái |
|---|---|
| RD màn hình | Có tài liệu này |
| BD màn hình | Chưa xác định — không tự suy luận |
| DD/API liên quan | Chưa xác định — không tự suy luận |

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
| Yêu cầu module | `01-rd/req/interview-bank.md` — F6-01, F6-08, F6-12, F6-13; `01-rd/req/identity.md` — F1-12, F1-14. |
| User story | `01-rd/req/user_stories/a2_instructor.md` — `US-A2-10`. |
| Khảo sát hệ thống | `01-rd/overview/system_survey.md` — mục 7.0 dòng `interview_question_authoring`. |
| Quyết định | `DEC-2026-0830-interview-bank-crud`. |
| Màn liên quan | `01-rd/screens/shared/SHR0301_interview_question_management.md` — màn cha/nguồn; `01-rd/screens/shared/SHR0202_problem_authoring.md` — tiền lệ cấu trúc màn soạn riêng. |
