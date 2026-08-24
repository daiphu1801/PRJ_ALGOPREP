# RD — Màn `problem_detail` (Workspace giải bài)

> Slug: `problem_detail` — khớp `01-rd/overview/system_survey.md` mục 7.1 dòng `problem_detail`
> [SoT: 01-rd/overview/system_survey.md:475]. Bounded Context: `problem-bank` + `harness` +
> `judge-orchestration` (+ `ai-review` ở điểm ra sau `Accepted`)
> [SoT: .nexa/control/dependency-map.md mục 5 — dòng `problem_detail`]. Actor: A1.
>
> **Màn nặng nhất trong 27 màn** — chạm bốn Bounded Context [SoT: 01-rd/overview/system_survey.md:495].
> Đối chiếu prototype: `09-layoutBase/Workspace giải bài.dc.html`.
>
> File này mô tả **hành vi và UX ở mức yêu cầu** — không lặp lại đặc tả chức năng đã có ở `01-rd/req/req.md`
> (mục F2, F3, F4) và `01-rd/req/user_stories.md` (`US-A1-03`), chỉ trỏ tới và bổ sung phần đặc thù của màn.
>
> **Quan trọng — đã sửa một quyết định phạm vi trong lúc viết file này:** bản trước của F3-13 coi mô hình
> Standard I/O là "đường lùi" do giảng viên chọn cố định theo từng bài. Đối chiếu prototype thật (mục 4 bên
> dưới) cho thấy học viên **tự do chọn mô hình mỗi lượt làm bài**, khớp đúng `README.md` mục 5 dòng 179
> ("Hỗ trợ cả 2 mô hình") hơn cách hiểu cũ. Đã chốt qua hỏi trực tiếp chủ dự án, ghi
> `DEC-2026-0824-dual-submission-model-per-problem`, sửa `req.md` (F2-03, F3-13), `system_survey.md`,
> `user_stories.md` (`US-A1-03`), và `02-bd/packages/harness/architecture.md`.

## 1. Mục đích màn hình

Nơi học viên đọc đề, viết mã (hoặc tải file mã nguồn lên), chạy thử với testcase mẫu, và nộp bài để chấm
với testcase ẩn — cho cả hai mô hình nộp bài song song
[SoT: 01-rd/req/req.md:159-199 — F3-01 tới F3-13; 01-rd/req/req.md:207-212 — F4-01, F4-02].

## 2. Nguồn yêu cầu (không lặp lại — chỉ trỏ)

| Hành vi | Mã | Nguồn |
| :--- | :--- | :--- |
| Đặc tả bài toán cho cả hai mô hình (chữ ký hàm + định dạng stdio) | F2-03 | `01-rd/req/req.md:109-117` |
| Chọn mô hình nộp bài tự do mỗi lượt làm; ẩn Bọc hàm nếu kiểu dữ liệu vượt lược đồ | F3-13 | `01-rd/req/req.md:179-198` |
| Sinh mã harness theo ngôn ngữ và mô hình (đọc input, gọi hàm, in kết quả) | F3-02→F3-04 | `01-rd/req/req.md:161-162` |
| Ánh xạ lỗi biên dịch về đúng dòng người dùng; che giấu lỗi harness | F3-11, F3-12 | `01-rd/req/req.md:168-169` |
| Nộp bài trả `submissionId` ngay, không chờ; hoặc tải file mã nguồn lên | F4-01 | `01-rd/req/req.md:207-211` |
| Chạy thử với testcase Sample, không ghi nhận tiến độ | F4-02 | `01-rd/req/req.md:212` |
| Given-When-Then đầy đủ | — | `01-rd/req/user_stories.md:65-77` (`US-A1-03`) |

## 3. Trạng thái và cấu trúc màn (screen states)

Đối chiếu `09-layoutBase/Workspace giải bài.dc.html` [SoT: 09-layoutBase/Workspace giải bài.dc.html]:

1. **Chưa chọn bài** (`hasProblem = false`) — màn gợi ý chọn bài đang làm dở hoặc chọn từ danh sách gợi ý
   (dòng 113-150).
2. **Bố cục hai cột chia đôi được (resizable split)** khi đã có bài — cột trái là panel Đề bài/Gợi ý/Bài
   nộp/Solution Review (tab, dòng 160-247); cột phải chia hai hàng: Editor (trên) và Console kết quả (dưới)
   (dòng 156-330).
3. **Tab "Đề bài"** — tiêu đề, badge độ khó/chủ đề/**mô hình nộp bài hiện tại** (`{{ problem.mode }}`, dòng
   172), câu giải thích rõ ràng mô hình đang chọn ("Bài này đang ở mô hình bọc hàm: bạn chỉ viết thân hàm…",
   dòng 178), ví dụ, ràng buộc, câu hỏi mở rộng gợi mở Solution Review (dòng 166-202).
4. **Tab "Gợi ý"**, **Tab "Bài nộp"** (lịch sử nộp bài cho riêng bài này: verdict, ngôn ngữ, thời gian chạy,
   thời điểm — dòng 216-227), **Tab "Solution Review"** (số liệu tóm tắt + nhận xét — dòng 229-247) — xem
   Câu hỏi mở Q1 về tab này.
5. **Chọn ngôn ngữ** (3 tab Java/C++/Python, dòng 268-270), **chọn mô hình** (2 nút "Có hàm main"/"Bọc hàm",
   dòng 272-276, 596-601) **và chọn phương thức nhập mã** (2 nút "Gõ trực tiếp"/"Tải file", tách riêng bằng
   một đường kẻ dọc khỏi nhóm chọn mô hình, dòng 277-282, 602-608) — đổi mã khung (starter code) ngay khi đổi
   mô hình hoặc ngôn ngữ; đổi phương thức nhập mã không đổi mã khung, chỉ đổi vùng hiển thị (editor hoặc vùng
   tải file). **Đã sửa xong Câu hỏi mở Q2** (đã chốt qua hỏi trực tiếp chủ dự án, đúng theo đề xuất trong
   bảng câu hỏi mở): trước đây "Tải file" là giá trị thứ ba của cùng biến mô hình (`mode`), nay tách thành
   biến `inputMethod` riêng, áp dụng cho mô hình đang chọn.
6. **`isEditorMode`** — editor dòng-số + textarea gõ trực tiếp (dòng 281-294). **`isUploadMode`** (khi chọn
   "Tải file") — vùng kéo-thả hoặc chọn file, giới hạn đúng phần mở rộng ngôn ngữ và tối đa 64 KB, xem lại
   nội dung đã tải (dòng 297-312) — khớp F4-01 mục bổ sung "tải file, cùng luồng với gõ trực tiếp".
7. **Console kết quả** — tab "Testcase" (xem input mẫu theo từng case, dòng 333-350) và tab "Kết quả" (sau
   khi Run/Submit): dải ô màu theo từng testcase, chỉ số thời gian/bộ nhớ, output, và — **chỉ khi
   Accepted** — ba liên kết "Xem kết quả chấm" (`submission_result`), "Phân tích bài giải" (`solution_review`,
   F5-01), "Phỏng vấn giả lập" (`mock_interview`, F5-09) (dòng 353-379).
8. **Chưa chạy gì** — thông báo hướng dẫn bấm Run Code (3 testcase mẫu) hoặc Submit (40 testcase ẩn trong dữ
   liệu mẫu) (dòng 380-381).

## 4. Given-When-Then bổ sung ở mức màn (không trùng `US-A1-03`)

- **Cho** tôi đang xem một bài toán bất kỳ, **Khi** tôi bấm nút chuyển mô hình từ "Bọc hàm" sang "Có hàm
  main" (hoặc ngược lại), **Thì** mã khung đổi ngay theo mô hình và ngôn ngữ đang chọn, đề bài (tab "Đề bài")
  cập nhật câu giải thích mô hình đang áp dụng — tôi không cần rời khỏi bài để đổi mô hình
  [SoT: 09-layoutBase/Workspace giải bài.dc.html:461-470, 596-601 — hàm `starter(lang, mode)` sinh lại mã
  khung theo cặp (ngôn ngữ, mô hình) bất kỳ lúc nào; nay `mode` chỉ còn hai giá trị (Có hàm main/Bọc hàm) sau
  khi tách "Tải file" thành `inputMethod` riêng, xem Câu hỏi mở Q2 đã đóng].
- **Cho** bài nộp của tôi đạt `Accepted`, **Khi** tôi xem console kết quả, **Thì** tôi thấy đồng thời ba lối
  đi tiếp theo (xem kết quả đầy đủ, phân tích bài giải, phỏng vấn giả lập) ngay tại đây, không cần quay lại
  danh sách bài toán [SoT: 09-layoutBase/Workspace giải bài.dc.html:371-377].
- **Cho** tôi chọn "Tải file" làm phương thức nhập mã, **Khi** tôi chọn file không đúng phần mở rộng của
  ngôn ngữ đang chọn hoặc file lớn hơn 64 KB, **Thì** hệ thống từ chối và báo lý do — hành vi từ chối cụ thể
  **chưa được dựng trong prototype** (chỉ có state đã tải thành công) — đã chốt ở Câu hỏi mở Q3 (đã đóng):
  báo lỗi inline ngay trong vùng tải.
- **Cho** tôi đang ở phương thức "Tải file", **Khi** tôi bấm đổi mô hình (Có hàm main / Bọc hàm), **Thì** mô
  hình đổi ngay nhưng phương thức nhập mã vẫn giữ nguyên "Tải file" — hai lựa chọn độc lập nhau, đổi cái này
  không reset cái kia [SoT: 09-layoutBase/Workspace giải bài.dc.html:596-608 — `mode` và `inputMethod` là hai
  biến state tách rời].

## 5. Câu hỏi mở

| # | Câu hỏi | Vì sao chưa trả lời được | Đề xuất | Chủ sở hữu |
| :-: | :--- | :--- | :--- | :--- |
| Q1 | ~~Tab "Solution Review" bên trong Workspace hiện cùng loại nội dung với màn `solution_review` riêng...~~ **ĐÃ CHỐT (phiên 2026-08-25)**: giữ nguyên hai nơi hiển thị — tab trong Workspace là bản tóm tắt nhanh (Quick Insight: độ phức tạp, 1-2 nhận xét chính) đọc lại từ báo cáo đã có sẵn (khi bài đã `Accepted` và đã có báo cáo F5-07/08); trang `solution_review` riêng là bản đầy đủ, mở từ nút trong tab hoặc từ console kết quả. Không tạo dữ liệu mới, cùng nguồn F5-07/08 — không cần sửa prototype. | — | Giữ nguyên như hiện tại, không cần hành động thêm. | Đã đóng |
| Q2 | ~~Ba nút "Có hàm main" / "Bọc hàm" / "Tải file" hiện nằm chung một hàng nút...~~ **ĐÃ CHỐT (phiên 2026-08-25)**: đúng là hai trục khác nhau, cần tách. Đã sửa trực tiếp `09-layoutBase/Workspace giải bài.dc.html`: tách biến `mode` (2 giá trị: Có hàm main/Bọc hàm) khỏi biến `inputMethod` mới (2 giá trị: Gõ trực tiếp/Tải file), hai nhóm nút đặt cạnh nhau trong cùng thanh công cụ editor, ngăn cách bằng một đường kẻ dọc [SoT: 09-layoutBase/Workspace giải bài.dc.html:272-282, 596-612]. | — | Đã sửa, không cần hành động thêm. | Đã đóng |
| Q3 | ~~Từ chối file tải lên sai định dạng/quá kích thước — thông báo lỗi cụ thể thế nào...~~ **ĐÃ CHỐT (phiên 2026-08-25, chốt theo RD):** thêm state lỗi inline ngay trong vùng tải (không dùng dialog), cùng phong cách với các state lỗi khác của hệ thống — nhất quán với quyết định ở Câu hỏi mở Q1 của màn `auth`. **Chưa dựng vào `09-layoutBase/Workspace giải bài.dc.html`** (chỉ có state "đã tải thành công") — để lúc build FE Next.js thật. | — | Đã chốt quyết định, chưa dựng prototype. | Đã đóng |

## 6. Ngoài phạm vi file này

- Bảng màu, spacing, chi tiết component Monaco Editor thật (prototype dùng textarea giả lập) — thuộc BD
  (`02-bd/screens/users/problem_detail.md`, chưa viết).
- Hợp đồng API (nộp bài, chạy thử, WebSocket per-testcase) — thuộc DD (`03-dd/api/judge-orchestration.md`,
  `03-dd/api/harness.md`, chưa viết).
- Thuật toán sinh mã harness theo từng ngôn ngữ và từng mô hình — thuộc logic của `harness`
  (`02-bd/packages/harness/architecture.md` mục 2b), không thuộc file theo trục màn này.
- Luồng cập nhật kết quả testcase theo thời gian thực sau khi Submit — thuộc màn `submission_result`
  (`01-rd/screens/users/submission_result.md`), không lặp lại ở đây theo đúng ghi chú "đừng gộp hai màn"
  [SoT: 01-rd/overview/system_survey.md:498].

## 7. Tham chiếu

- `01-rd/req/req.md:107-117, 153-199, 200-212` — F2-03, F3-01 tới F3-13, F4-01, F4-02.
- `01-rd/req/user_stories.md:65-77` — `US-A1-03`.
- `01-rd/overview/system_survey.md:475, 495, 498` — dòng `problem_detail`, ghi chú "màn nặng nhất", ghi chú
  không gộp với `submission_result`.
- `09-layoutBase/Workspace giải bài.dc.html` — prototype.
- `.nexa/control/decision-registry.md` — `DEC-2026-0824-dual-submission-model-per-problem` (phát sinh từ
  chính việc viết file này).
- `02-bd/packages/harness/architecture.md` mục 2b — hệ quả kiến trúc của quyết định trên.
- `06-plan/nexa-plan/260824-2043-bd-screens-common-first.md` — kế hoạch Phase 1 sinh ra file này.
