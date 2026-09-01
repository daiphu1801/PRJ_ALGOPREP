# RD — Màn `interview_question_authoring` (Soạn câu hỏi phỏng vấn)

> Slug: `interview_question_authoring` — khớp `01-rd/overview/system_survey.md` mục 7.0 dòng
> `interview_question_authoring` [SoT: 01-rd/overview/system_survey.md:491]. Mã: F6-13
> [SoT: 01-rd/req/interview-bank.md — F6-13]. Bounded Context: `interview-bank`.
> **Actor: A2 và A3 — màn dùng chung, phạm vi dữ liệu theo quyền**, gác bởi Function
> `INTERVIEW_BANK_MANAGEMENT` (`01-rd/req/identity.md` — F1-12), cùng cơ chế dùng chung đã áp dụng cho
> `problem_authoring`/`problem_management` (`DEC-2026-0825-shared-content-authoring-screens`) — không có
> phạm vi riêng theo lớp, vì F6-11 (bộ câu hỏi riêng theo lớp) đã loại khỏi phạm vi 2026-08-28
> [SoT: 01-rd/req/interview-bank.md — F6-13].
>
> **Slug mới, chưa có prototype (`[Đợi nextjs]`).** Chốt 2026-08-30, qua hỏi trực tiếp chủ dự án khi trả lời
> Câu hỏi mở Q4 của `01-rd/screens/shared/interview_question_management.md`, cùng `DEC-2026-0830-interview-bank-crud`
> [SoT: 01-rd/screens/shared/interview_question_management.md:132]. Route: `/instructor/interview-questions/[id]`,
> `/admin/interview-questions/[id]` — **chốt giữ nguyên đề xuất 2026-09-01** (Câu hỏi mở Q6)
> [SoT: 01-rd/overview/system_survey.md:491]. Cấu trúc màn dưới đây **suy diễn song song
> theo tiền lệ `problem_authoring`** (một màn soạn riêng cho một bản ghi phức tạp, không phải modal/drawer)
> — quyết định gốc đã dẫn rõ chính tiền lệ này khi cắt bỏ phương án modal
> [SoT: 01-rd/req/interview-bank.md — F6-13, dòng "song song tiền lệ problem_authoring"], không phải suy
> diễn tự do.
>
> File này mô tả **hành vi và UX ở mức yêu cầu** — không lặp lại đặc tả chức năng chung đã có ở
> `01-rd/req/interview-bank.md` (F6), chỉ trỏ tới và bổ sung phần đặc thù của màn.

## 1. Mục đích màn hình

Nơi A2/A3 tạo mới hoặc sửa **một câu hỏi phỏng vấn** trong ngân hàng dùng chung: nội dung + phân loại, danh
sách câu hỏi đào sâu, và bộ tiêu chí đánh giá có trọng số — rồi câu hỏi đó sẵn sàng cho học viên dùng ở Chế
độ học/Chế độ luyện [SoT: 01-rd/req/interview-bank.md — F6-13]. Đây là màn "thượng nguồn" của F6-08 (AI đối
chiếu Chế độ luyện): một câu hỏi thiếu bộ tiêu chí ở đây sẽ bị ẩn khỏi Chế độ luyện phía người học
[SoT: 01-rd/req/interview-bank.md — F6-13, mục "Câu hỏi thiếu tiêu chí đánh giá"].

## 2. Nguồn yêu cầu (không lặp lại — chỉ trỏ)

| Hành vi | Mã | Nguồn |
| :--- | :--- | :--- |
| Quản trị nội dung ngân hàng câu hỏi dùng chung: tạo, sửa, nhân bản, xoá | F6-13 | `01-rd/req/interview-bank.md` — F6-13 (bổ sung 2026-08-30) |
| Nội dung + phân loại theo chủ đề/độ khó (5 chủ đề: Lý thuyết CS, System design, Database, Ngôn ngữ, Hành vi) | F6-01 | `01-rd/req/interview-bank.md` — F6-01 |
| Bộ tiêu chí đánh giá có trọng số phần trăm — tiêu chí chuẩn mà F6-08 đối chiếu khi chấm Chế độ luyện | F6-13, F6-08 | `01-rd/req/interview-bank.md` — F6-13, F6-08 |
| Câu hỏi thiếu tiêu chí đánh giá: vẫn hiện ở Chế độ học, ẩn khỏi Chế độ luyện | F6-13, F6-08 | `01-rd/req/interview-bank.md` — F6-13 |
| Xoá mềm: đánh dấu ngừng dùng, ẩn khỏi màn phía học viên, không cascade xoá phiên cũ | F6-13 | `01-rd/req/interview-bank.md` — F6-13 |
| Mọi thao tác thêm/sửa/nhân bản/xoá ghi vào Nhật ký hệ thống | F6-13, F1-14 | `01-rd/req/interview-bank.md` — F6-13; `01-rd/req/identity.md` — F1-14 |
| Given-When-Then liên quan | — | `01-rd/req/user_stories/a2_instructor.md` (`US-A2-10`) |
| Gác quyền: `INTERVIEW_BANK_MANAGEMENT` là một `FUNCTION` trong ma trận phân quyền, cùng nhóm F6-12 | F1-12 | `01-rd/req/identity.md` — F1-12 |

## 3. Trạng thái và cấu trúc màn (screen states)

Chưa có prototype để đối chiếu — cấu trúc dưới đây suy diễn trực tiếp từ "4 nhóm trường lồng nhau" đã chốt ở
F6-13, theo đúng khuôn hình `problem_authoring` (thanh đầu trang + nội dung theo nhóm + hành động lưu/xuất
bản) `[SoT: Suy luận, song song tiền lệ problem_authoring — không tự thêm khối nào ngoài 4 nhóm đã chốt]`:

1. **Thanh đầu trang** — nút quay lại `interview_question_management`; mã/tiêu đề câu hỏi đang sửa (rỗng khi
   tạo mới); trạng thái lưu; nút **"Xem như học viên"** (preview, chốt 2026-09-01 — cùng khuôn mẫu
   `problem_authoring`); nút "Lưu" (không có khái niệm "xuất bản" riêng như `problem_authoring` — chốt
   2026-09-01: câu hỏi hiện ngay cho học viên khi lưu, trừ khi bị ẩn ở Chế độ luyện do thiếu tiêu chí, xem
   mục 2). `[SoT: Suy luận]`.
2. **Nhóm 1 — Nội dung và phân loại** — nội dung câu hỏi, chủ đề (5 giá trị cố định, F6-01), độ khó
   [SoT: 01-rd/req/interview-bank.md — F6-01, F6-13].
3. **Nhóm 2 — Câu hỏi đào sâu** — danh sách văn bản tự do, không giới hạn số lượng, không có độ khó riêng
   (chốt 2026-09-01) các câu hỏi truy vấn tiếp theo cùng câu hỏi gốc, dùng ở giai đoạn Phản biện của Phỏng
   vấn giả lập (F5-11) — dữ liệu này **không phải** F6-04/05/06 (khung trả lời/từ khoá của Chế độ học), độc
   lập hoàn toàn [SoT: 01-rd/req/interview-bank.md — F6-13, mục "danh sách câu hỏi đào sâu"].
4. **Nhóm 3 — Bộ tiêu chí đánh giá có trọng số** — danh sách tiêu chí, mỗi tiêu chí có trọng số phần trăm,
   **tổng bắt buộc = 100 (chặn lưu nếu sai, chốt 2026-09-01)**, dùng làm mốc tính điểm khi F6-08 đối chiếu
   Chế độ luyện — độc lập với hai rubric của F5 (F5-15, F5-23) [SoT: 01-rd/req/interview-bank.md — F6-13].
5. **Nhóm 4 — Hành động quản trị** — nhân bản (toàn bộ 4 nhóm, chốt 2026-09-01), xoá mềm (đánh dấu ngừng
   dùng) [SoT: 01-rd/req/interview-bank.md — F6-13].
6. **4 thẻ chỉ số chất lượng nội dung** (tổng câu hỏi, thiếu rubric, điểm trung bình 30 ngày, chưa dùng lần
   nào) — theo `interview_question_management.md` Q5, đây là dẫn xuất trình bày ở màn danh sách
   (`interview_question_management`), không phải ở màn soạn này; nêu lại ở đây để tránh trùng lặp nhầm
   [SoT: 01-rd/screens/shared/interview_question_management.md:133].
7. **Không có prototype minh hoạ** — mọi chi tiết bố cục, màu sắc, breakpoint thuộc BD, viết khi dựng UI
   Next.js trực tiếp `[Đợi nextjs]`.

## 4. Given-When-Then bổ sung ở mức màn (không trùng `US-A2-10`)

- **Cho** tôi có quyền `INTERVIEW_BANK_MANAGEMENT`, **Khi** tôi mở màn này để tạo câu hỏi mới, **Thì** tôi
  thấy đủ 4 nhóm trường rỗng, sẵn sàng nhập [SoT: 01-rd/req/interview-bank.md — F6-13].
- **Cho** tôi đang sửa một câu hỏi đã có bộ tiêu chí, **Khi** tôi xoá hết tiêu chí và lưu, **Thì** câu hỏi đó
  ngay lập tức bị ẩn khỏi Chế độ luyện phía học viên (vẫn hiện ở Chế độ học) [SoT: 01-rd/req/interview-bank.md
  — F6-13, F6-08].
- **Cho** một câu hỏi đã dùng trong ít nhất một phiên phỏng vấn, **Khi** tôi xoá câu hỏi đó, **Thì** hệ thống
  chỉ đánh dấu ngừng dùng (xoá mềm) — phiên phỏng vấn cũ vẫn giữ nguyên bản ghi câu hỏi tại thời điểm dùng,
  không cascade xoá [SoT: 01-rd/req/interview-bank.md — F6-13].

## 5. Câu hỏi mở

| # | Câu hỏi | Vì sao chưa trả lời được | Đề xuất | Chủ sở hữu |
| :-: | :--- | :--- | :--- | :--- |
| Q1 | ~~Tổng trọng số của bộ tiêu chí đánh giá có bắt buộc bằng 100 không?~~ **ĐÃ CHỐT (2026-09-01, qua hỏi trực tiếp chủ dự án):** bắt buộc tổng = 100, chặn lưu nếu sai. | — | Đã chốt. | Đã đóng |
| Q2 | ~~Có nút "Xem như học viên" (preview), giống `problem_authoring`, hay không?~~ **ĐÃ CHỐT (2026-09-01):** có, cùng khuôn mẫu — xem trước câu hỏi ở cả Chế độ học và Chế độ luyện trước khi lưu, không cần mã `Fx-nn` mới. | — | Đã chốt. | Đã đóng |
| Q3 | ~~Câu hỏi đào sâu (nhóm 2) là văn bản tự do hay có cấu trúc?~~ **ĐÃ CHỐT (2026-09-01):** danh sách văn bản tự do, không giới hạn số lượng, không có độ khó riêng — AI dùng nguyên văn khi truy vấn ở giai đoạn Phản biện (F5-11). | — | Đã chốt. | Đã đóng |
| Q4 | ~~Nhân bản một câu hỏi có nhân bản luôn cả bộ tiêu chí và câu hỏi đào sâu không?~~ **ĐÃ CHỐT (2026-09-01):** nhân bản toàn bộ 4 nhóm, giữ nguyên tiền lệ `problem_management`/F2-16 (nhân bản = sao chép toàn bộ nội dung). | — | Đã chốt. | Đã đóng |
| Q5 | ~~Nút "Lưu" có phân biệt "Lưu nháp" và "Lưu và công bố" như `problem_authoring`, hay câu hỏi hiện ngay khi lưu?~~ **ĐÃ CHỐT (2026-09-01):** không có vòng đời nháp/xuất bản riêng — câu hỏi hiện ngay cho học viên khi lưu (trừ khi tự động ẩn khỏi Chế độ luyện do thiếu tiêu chí, đã chốt ở mục 2). | — | Đã chốt. | Đã đóng |
| Q6 | ~~Route chính xác — giữ đề xuất hay đổi khi build FE?~~ **ĐÃ CHỐT (2026-09-01):** giữ đề xuất `/instructor/interview-questions/[id]`, `/admin/interview-questions/[id]` — khớp đúng khuôn mẫu route của `problem_authoring`. | — | Đã chốt. | Đã đóng |

## 6. Ngoài phạm vi file này

- Bảng màu, spacing, breakpoint, component cụ thể — thuộc BD
  (`02-bd/screens/shared/interview_question_authoring.md`, chưa viết), viết khi dựng UI Next.js trực tiếp
  (chưa có prototype trung gian).
- Hợp đồng API (tạo/sửa/nhân bản/xoá một câu hỏi, kiểm tra tổng trọng số) — thuộc DD
  (`03-dd/api/interview-bank.md`, chưa viết).
- Nội dung và bố cục của `interview_question_management` (màn cha, danh sách + 4 thẻ chỉ số) — thuộc file RD
  riêng của nó, không lặp lại ở đây.
- Thuật toán AI đối chiếu Chế độ luyện dùng bộ tiêu chí này (F6-08) — thuộc logic của `ai-review`, không
  thuộc file theo trục màn này.

## 7. Tham chiếu

- `01-rd/req/interview-bank.md` — F6-01, F6-08, F6-12, F6-13.
- `01-rd/req/identity.md` — F1-12, F1-14.
- `01-rd/req/user_stories/a2_instructor.md` — `US-A2-10`.
- `01-rd/overview/system_survey.md:491, 498-500` — dòng `interview_question_authoring` trong bảng màn mục 7.0.
- `01-rd/screens/shared/interview_question_management.md` — màn cha/nguồn, Câu hỏi mở Q2-Q7 (đã đóng, sinh
  ra slug này).
- `01-rd/screens/shared/problem_authoring.md` — tiền lệ cấu trúc màn soạn riêng (dẫn chiếu song song, không
  phải nguồn chức năng).
- `.nexa/control/decision-registry.md` → `DEC-2026-0830-interview-bank-crud`.
