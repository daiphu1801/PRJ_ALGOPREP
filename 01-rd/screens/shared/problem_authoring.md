# RD — Màn `problem_authoring` (Soạn đề bài)

> Slug: `problem_authoring`. Bounded Context: `problem-bank` + `harness`.
> **Actor: A2 và A3 — màn dùng chung, phạm vi dữ liệu theo quyền.** Đã chốt ngày 2026-08-25 (owner
> instruction, kết thúc Câu hỏi mở Q1 cũ): mount ở cả `/instructor/problems/[id]` và `/admin/problems/[id]`
> (tiền tố theo `DEC-2026-0825-frontend-base-architecture`), cùng một view/BD/DD, A2 soạn/sửa bài của
> mình, A3 quản toàn bộ kho — cơ chế gác cửa có sẵn qua `PROBLEM_AUTHORING`/`TESTCASE_MANAGEMENT`
> (`req.md:64`, F1-10 tới F1-12). Ghi quyết định: `DEC-2026-0825-shared-content-authoring-screens`. File
> này đã chuyển từ `01-rd/screens/teacher/` sang `01-rd/screens/shared/` cùng ngày.
>
> **Đối chiếu prototype: `09-layoutBase/Admin - Soạn đề bài.dc.html` (736 dòng).**
>
> **Phát hiện chính của đợt đối chiếu này — ô "Prototype" của slug này từng ghi "Chưa có" là SAI.** Prototype
> có thật, dựng khá đầy đủ (4 tab nội dung, bảng testcase, panel thuộc tính); đã sửa trong
> `system_survey.md`. Kế hoạch "chờ chủ dự án dựng UI trước rồi mới viết RD" cho slug này không còn lý do
> tồn tại [SoT: 06-plan/nexa-plan/260824-2043-bd-screens-common-first.md:165].
>
> **Phát hiện thứ hai — `testcase_management` không phải một màn riêng, đã gộp (Q2, đã chốt).** Toàn bộ phần
> testcase (bảng testcase, "Tải lên hàng loạt", "Sinh tự động", "Chạy với đáp án mẫu") là **một tab bên
> trong chính màn này** [SoT: 09-layoutBase/Admin - Soạn đề bài.dc.html:251-305, 570].
>
> **Phát hiện thứ ba — màn này là màn con của `problem_management`**, không phải màn gốc: prototype đặt
> `activeKey = 'problems'` (mục nav "Quản lý bài tập")
> [SoT: 09-layoutBase/Admin - Soạn đề bài.dc.html:456, 463] và có nút quay lại
> `./Admin - Quản lý bài tập.dc.html` [SoT: 09-layoutBase/Admin - Soạn đề bài.dc.html:142].
>
> File này mô tả **hành vi và UX ở mức yêu cầu** — không lặp lại đặc tả chức năng đã có ở `01-rd/req/req.md`
> mục F2, chỉ trỏ tới và bổ sung phần đặc thù của màn.

## 1. Mục đích màn hình

Nơi người soạn nội dung tạo và sửa một bài toán trọn vẹn trong một màn: đề bài Markdown, phân loại độ khó và
chủ đề, ràng buộc/giới hạn tài nguyên, ví dụ mẫu, đáp án mẫu, bộ testcase và các gợi ý kèm theo — rồi xuất
bản cho người học [SoT: 01-rd/req/req.md:138-139]. Đây là màn "thượng nguồn" của toàn hệ thống: mọi thứ F3
(sinh mã khung) và F4 (chấm bài) làm được đều phụ thuộc dữ liệu khai ở đây [SoT: 01-rd/req/req.md:144-147].

## 2. Nguồn yêu cầu (không lặp lại — chỉ trỏ)

| Hành vi | Mã | Nguồn |
| :--- | :--- | :--- |
| Soạn đề bài Markdown kèm LaTeX | F2-01 | `01-rd/req/req.md:139` |
| Phân loại theo độ khó và chủ đề | F2-02 | `01-rd/req/req.md:139` |
| Khai đặc tả bài toán cho cả hai mô hình nộp bài (chữ ký hàm theo ngôn ngữ + định dạng I/O chuẩn) | F2-03 | `01-rd/req/req.md:140-150` |
| Chiến lược so khớp kết quả `EXACT`/`TRIMMED`/`EPSILON`/`UNORDERED_SET` | F2-04 | `01-rd/req/req.md:151-152` |
| Testcase Sample (công khai, dùng cho Chạy thử) và Hidden (ẩn, dùng cho Nộp bài) | F2-05, F2-06 | `01-rd/req/req.md:154` |
| Tải lên bộ testcase theo lô, bộ lớn lưu MinIO | F2-07 | `01-rd/req/req.md:155` |
| Phiên bản hoá bộ testcase cho Re-judge | F2-09 | `01-rd/req/req.md:159` |
| Giới hạn thời gian/bộ nhớ theo bài kèm hệ số nhân theo ngôn ngữ | F2-10 | `01-rd/req/req.md:160-162` |
| AI sinh testcase: AI chỉ sinh input, output lấy từ chạy thật Đáp án mẫu qua go-judge; testcase ở trạng thái nháp chờ xác nhận | F2-14 | `01-rd/req/req.md:182-195` |
| Given-When-Then liên quan | — | `01-rd/req/user_stories.md:203-214` (`US-A2-01`), `216-228` (`US-A2-02`), `251-264` (`US-A2-05`) |
| Gác quyền: `PROBLEM_AUTHORING` và `TESTCASE_MANAGEMENT` là hai `FUNCTION` trong ma trận phân quyền | F1-10 tới F1-12 | `01-rd/req/req.md:55-68` |

## 3. Trạng thái và cấu trúc màn (screen states)

Đối chiếu `09-layoutBase/Admin - Soạn đề bài.dc.html`. Cấu trúc: thanh đầu trang dính (sticky) + vùng nội
dung 4 tab bên trái + cột thuộc tính dính bên phải [SoT: 09-layoutBase/Admin - Soạn đề bài.dc.html:141-159,
344].

### 3.1. Thanh đầu trang

1. **Nút quay lại danh sách bài tập** — về `problem_management`
   (dòng 142). Xác nhận quan hệ màn cha/màn con.
2. **Mã và tiêu đề bài** (`#76 · Minimum Window Substring`, dòng 145-146) — định danh bài đang sửa.
3. **Trạng thái lưu** — `saveHint`: "Đang hiển thị cho người học · lưu nháp tự động 2 phút trước" khi đã xuất
   bản, "Bản nháp · chưa hiển thị cho người học" khi chưa (dòng 148, 724). Ngụ ý **lưu nháp tự động**; không
   có mã `Fx-nn` nào nói tới auto-save — xem Q7.
4. **"Xem như người học"** (dòng 155) — xem trước đề bài đúng như người học thấy. Chưa gắn mã — xem Q7.
5. **"Lưu và xuất bản"** (dòng 156) — đặt trạng thái bài thành "Đã xuất bản" (dòng 725), tức là hành động
   chuyển vòng đời nháp → công bố. Xem Q7.

### 3.2. Bốn tab nội dung

Tab và số đếm: "Nội dung đề" · "Ví dụ mẫu" (theo số ví dụ) · "Testcase" (theo số testcase) · "Gợi ý AI"
(dòng 567-572, 163-165).

**Tab 1 — Nội dung đề** (dòng 168-213):

- **Tiêu đề** (dòng 171-172) — F2-01.
- **"Nội dung đề · Markdown"** kèm bộ đếm ký tự (dòng 174-177) — F2-01. **Cảnh báo:** prototype chỉ có ô
  textarea thuần, **không có vùng xem trước Markdown và không có bất cứ dấu vết nào của LaTeX**
  (grep `LaTeX` trên toàn file: không có kết quả), trong khi F2-01 yêu cầu "Markdown kèm công thức LaTeX"
  [SoT: 01-rd/req/req.md:139]. Cần bổ sung khi dựng UI thật — xem Q7.
- **"Ràng buộc và giới hạn"** (dòng 181-193, giá trị mẫu dòng 693-698) — 4 trường: Giới hạn thời gian
  (2.0 giây), Giới hạn bộ nhớ (256 MB), **Kích thước đầu ra (64 KB)**, **Số lần nộp / giờ (30 lượt)**. Hai
  trường đầu khớp F2-10; hai trường sau chưa có mã — xem Q7. Chú thích trên màn: "Áp dụng cho toàn bộ ngôn
  ngữ, có thể ghi đè riêng ở màn Ngôn ngữ và giới hạn" (dòng 182) — khớp đúng phân vai: **hệ số nhân theo
  ngôn ngữ của F2-10 không nằm ở màn này**, nằm ở `admin_language_config` (F4-11)
  [SoT: 01-rd/overview/system_survey.md:552].
- **"Ràng buộc dữ liệu"** — textarea tự do (`1 <= s.length, t.length <= 10^5 ...`, dòng 194-195, 416). Vừa là
  phần đề bài (F2-01), vừa là **đầu vào bắt buộc cho F2-14**: AI sinh input "dựa trên đề bài Markdown và Ràng
  buộc dữ liệu Admin đã khai báo" [SoT: 01-rd/req/req.md:185-186].
- **"Đáp án mẫu"** kèm chọn ngôn ngữ Python/C++/Java (dòng 199-210, 690), phụ đề trên màn: "Dùng để sinh kết
  quả mong đợi cho testcase" (dòng 202) — khớp chính xác cơ chế an toàn của F2-14
  [SoT: 01-rd/req/req.md:186-190]. `req.md` mới chỉ nhắc Đáp án mẫu **bên trong** F2-14 như một điều kiện
  tiên quyết [SoT: 01-rd/req/req.md:193-195], chưa có mã riêng cho chính việc khai Đáp án mẫu — xem Q7.

**Tab 2 — Ví dụ mẫu** (dòng 215-249, 629-632): danh sách ví dụ, mỗi ví dụ gồm Đầu vào / Kết quả / Giải thích,
kèm "Thêm ví dụ" và xoá từng ví dụ. Phụ đề: "Hiển thị công khai trong đề bài, kèm lời giải thích" (dòng 220).
Đây là phần đề bài (F2-01), **khác testcase Sample** (ví dụ mẫu là văn bản minh hoạ trong đề, không phải dữ
liệu chạy máy) — prototype tách hai tab riêng và giữ đúng phân biệt đó.

**Tab 3 — Testcase** (dòng 251-305) — đây chính là phần mà survey đang xếp thành slug
`testcase_management` riêng (Q2):

- Tóm tắt "N testcase · M công khai · tổng trọng số X" (dòng 257, 708).
- **"Tải lên hàng loạt"** (dòng 259) — F2-07.
- **"Sinh tự động"** (dòng 260) — F2-14. Prototype chưa gắn `onClick` (còn là placeholder tĩnh), đúng như
  `06-plan/PROTOTYPE_DEBT.md` đã ghi nhận [SoT: 06-plan/PROTOTYPE_DEBT.md:266-267].
- **"Chạy với đáp án mẫu"** (dòng 261, 709-710) + dải kết quả chạy ("Đúng 8/8 testcase" hoặc "Thất bại ở
  testcase #k", kèm thời gian tối đa, dòng 264-269, 711-716) — hiện thực điều kiện tiên quyết của F2-14
  ("Đáp án mẫu đã chạy Pass với testcase hiện có") [SoT: 01-rd/req/req.md:193-195].
- **Bảng testcase** (dòng 272-291): cột `#`, Đầu vào, Kết quả mong đợi, **Hiển thị**, **Điểm**, Chạy thử,
  xoá. Cột "Hiển thị" là nút bật/tắt hai giá trị **"Công khai" / "Ẩn"** (dòng 282, 604-608) — ánh xạ đúng
  F2-05 / F2-06, chỉ khác chữ: `req.md` dùng thuật ngữ **Sample / Hidden**
  [SoT: 01-rd/req/req.md:154]. Đề nghị khi dựng UI thật giữ nhãn tiếng Việt "Công khai/Ẩn" trên giao diện
  nhưng dùng `SAMPLE`/`HIDDEN` làm giá trị dữ liệu, để không sinh ra thuật ngữ thứ hai trong lược đồ DB.
  **Cột "Điểm" là một xung đột — xem Q3.**
- **Kéo-thả sắp lại thứ tự testcase** (dòng 278, 610-622) và **"+ Thêm testcase"** (dòng 290, 707). Thứ tự
  testcase là dữ liệu có nghĩa với F4 vì fail-fast dừng ở testcase sai đầu tiên
  [SoT: 01-rd/req/req.md:249-250] — việc cho sắp lại thứ tự là hợp lý, không có mã riêng nhưng nằm gọn trong
  F2-05/F2-06 [SoT: Suy luận].
- **Khối "Chấm điểm từng phần"** (dòng 294-303, 626, 718-721) — thanh trọng số, yêu cầu "Tổng trọng số phải
  bằng 100 để bài được xuất bản". **Xung đột — xem Q3.**
- Ghi chú: F2-08 (chống rò rỉ testcase ẩn) **không hạn chế màn này** — F2-08 ràng buộc phản hồi của luồng
  nộp bài phía người học [SoT: 01-rd/req/req.md:156-158], còn người soạn đề đương nhiên thấy input/output của
  testcase ẩn.

**Tab 4 — Gợi ý AI** (dòng 307-341), gồm hai khối tách biệt:

- **"Gợi ý theo cấp độ"** (dòng 309-326, 634-642) — 3 mức gợi ý mở dần, phụ đề "Mở dần khi người học bế tắc,
  **mỗi lần mở trừ điểm gợi ý**" (dòng 314), nhãn chi phí từng mức: "Không trừ điểm" / "−5 điểm" / "−12 điểm"
  (dòng 635-637). Kèm nút **"Nhờ AI soạn nháp"** (dòng 312). **Không có mã `Fx-nn` nào phủ khối này** — xem
  Q5. Đối ứng phía người học là tab "Gợi ý" ở `problem_detail`, cũng chưa gắn mã
  [SoT: 01-rd/screens/users/problem_detail.md:51].
- **"Chỉ dẫn cho trợ lý AI"** (dòng 328-339, 644-662, 722) — textarea ngữ cảnh riêng của bài, phụ đề "nối vào
  prompt hệ thống ở màn Cấu hình AI" (dòng 330), kèm 3 cờ: "Không đưa mã hoàn chỉnh", "Chỉ hỏi ngược, không
  giải hộ", "Cho phép AI mở gợi ý ẩn" (dòng 645-647). **Không có mã nào phủ** — F5-23 chỉ nói cấu hình prompt
  và rubric ở cấp hệ thống, actor A3 [SoT: 01-rd/req/req.md:360-365]. Xem Q6.

### 3.3. Cột thuộc tính bên phải

1. **"Thuộc tính"** (dòng 345-378): Chủ đề (dòng 349-351) và Độ khó Easy/Medium/Hard (dòng 353-358, 691) —
   F2-02. **Trạng thái** Nháp / Đã xuất bản / Ẩn (dòng 360-367, 692) và **Thẻ** tự do (dòng 368-376, 699) —
   chưa có mã, xem Q7.
2. **"Sẵn sàng xuất bản"** (dòng 380-390, 664-677) — checklist 5 điều kiện tính tự động: ≥ 8 testcase, tổng
   trọng số bằng 100, ≥ 2 testcase công khai, đáp án mẫu chạy đúng mọi testcase, ≥ 2 ví dụ mẫu. Đây là **cửa
   chất lượng trước khi xuất bản**; không có mã nào định nghĩa các ngưỡng này (8, 2, 2) — xem Q7. Điều kiện
   "tổng trọng số bằng 100" thuộc phần đã quyết bỏ ở Q3.
3. **"Số liệu bài"** (dòng 392-403, 700-705) — Lượt nộp, Tỉ lệ AC, Thời gian giải trung bình, **Lượt xin gợi
   ý**; kèm dòng "Sửa lần cuối 5 giờ trước bởi Phú Đại" (dòng 402). Chưa có mã; "Lượt xin gợi ý" lại phụ
   thuộc tính năng gợi ý ở Q5. Xem Q7.

## 4. Given-When-Then bổ sung ở mức màn (không trùng `US-A2-01`, `US-A2-02`, `US-A2-05`)

- **Cho** một bài toán chưa có Đáp án mẫu chạy Pass, **Khi** người soạn mở tab Testcase, **Thì** nút "Sinh tự
  động" ở trạng thái vô hiệu kèm lý do ngay tại chỗ, thay vì bấm được rồi mới báo lỗi — F2-14 đã chốt là
  "chưa có đáp án mẫu hợp lệ thì tính năng vô hiệu" [SoT: 01-rd/req/req.md:193-195], và
  `US-A2-05` mô tả hành vi từ chối sau khi bấm [SoT: 01-rd/req/user_stories.md:260-261]; đề xuất chặn sớm ở
  mức giao diện thay vì chỉ báo lỗi sau. Prototype hiện chưa gắn hành vi nào cho nút này
  [SoT: 06-plan/PROTOTYPE_DEBT.md:266-267].
- **Cho** người soạn bấm "Chạy với đáp án mẫu" trên bộ testcase hiện có, **Khi** một testcase không đạt,
  **Thì** màn chỉ ra đúng testcase đầu tiên sai kèm nguyên nhân (ví dụ "Vượt giờ") và bài **không** đạt điều
  kiện "Đáp án mẫu chạy đúng mọi testcase" trong checklist Sẵn sàng xuất bản
  [SoT: 09-layoutBase/Admin - Soạn đề bài.dc.html:544-550, 592, 668].
- **Cho** người soạn đổi trạng thái bài từ "Đã xuất bản" về "Nháp" hoặc "Ẩn", **Khi** lưu, **Thì** bài không
  còn hiển thị cho người học nhưng các lượt nộp đã có không bị xoá — nguyên tắc "không phá dữ liệu đã có" mà
  hệ thống đã áp cho khoá tài khoản (F1-16) và chấm lại (F4-09c) [SoT: Suy luận: prototype chỉ hiện câu "Bản
  nháp · chưa hiển thị cho người học" (dòng 724) mà không nói gì về lượt nộp cũ; suy ra từ hai tiền lệ đã
  chốt: F1-16 khoá mềm không xoá bài nộp [SoT: 06-plan/PROTOTYPE_DEBT.md:62-65] và F4-09c không hạ điểm đã
  công bố [SoT: 01-rd/req/user_stories.md:240-241]. Cần chủ dự án xác nhận, gộp vào Q7].
- **Cho** người soạn sửa bộ testcase của một bài **đã có người nộp**, **Khi** lưu, **Thì** hệ thống tăng
  phiên bản bộ testcase và cho biết phiên bản mới là bao nhiêu — yêu cầu F2-09 đã có
  [SoT: 01-rd/req/user_stories.md:227-228] nhưng **prototype không có bất cứ chỗ nào hiển thị phiên bản** (grep
  "phiên bản" trên file prototype: không có kết quả). Xem Q4.

## 5. Câu hỏi mở

| # | Ưu tiên | Câu hỏi | Vì sao chưa trả lời được | Đề xuất (chưa chốt) | Chủ sở hữu |
| :-: | :-: | :--- | :--- | :--- | :--- |
| Q1 | — | ~~Màn này thuộc khu Giảng viên hay khu Admin — hay cả hai?~~ **ĐÃ CHỐT 2026-08-25 (owner instruction):** dùng chung một màn, mount ở cả `/instructor/problems/[id]` và `/admin/problems/[id]` (tiền tố theo `DEC-2026-0825-frontend-base-architecture`), phạm vi dữ liệu do ma trận phân quyền F1-10 tới F1-12 quyết định — A2 soạn/sửa bài của mình, A3 quản toàn bộ. | — | Ghi quyết định `DEC-2026-0825-shared-content-authoring-screens`, chốt cùng lúc với `interview_question_management` (cùng dạng lệch). Không đè Phương án B (mục 7.2) — khu Giảng viên vẫn giữ layout riêng, đây chỉ là một view mount ở hai route. | Đã đóng |
| Q2 | — | ~~`testcase_management` có nên bị gộp vào `problem_authoring`?~~ **ĐÃ CHỐT 2026-08-25 (owner instruction):** gộp hoàn toàn — `testcase_management` không còn là slug riêng. | — | Phiên bản bộ testcase (F2-09) là **panel/drawer bên trong tab Testcase** của màn này, không tách hộp thoại hay màn riêng. Tổng số màn dự kiến giảm theo (xem `system_survey.md`). | Đã đóng |
| Q3 | Cao | **Cột "Điểm" và khối "Chấm điểm từng phần" vẫn còn nguyên trong prototype, trái với nợ đã ghi là "đã xử lý".** `06-plan/PROTOTYPE_DEBT.md` mục 2.6 phần 1 tuyên bố **BỎ** chấm điểm từng phần vì mâu thuẫn fail-fast F4-04, và ghi rõ "**Đã sửa prototype** — bỏ cột Điểm trong bảng testcase, bỏ khối Chấm điểm từng phần, bỏ trường trọng số khỏi dữ liệu mẫu `tcs` và state liên quan (`totalWeight`, `weightPct`, `weightLabel`, `weightFg`, `weightBar`, điều kiện Tổng trọng số bằng 100 trong checklist)" [SoT: 06-plan/PROTOTYPE_DEBT.md:69, 249-256]. **Đã đọc và kiểm cả hai nơi — việc sửa CHƯA được áp dụng:** cột "Điểm" còn ở [SoT: 09-layoutBase/Admin - Soạn đề bài.dc.html:273], giá trị trọng số từng dòng còn ở [SoT: 09-layoutBase/Admin - Soạn đề bài.dc.html:283, 419-426, 596], khối "Chấm điểm từng phần" còn nguyên ở [SoT: 09-layoutBase/Admin - Soạn đề bài.dc.html:294-303], `totalWeight`/`weightPct`/`weightLabel`/`weightFg`/`weightBar` còn ở [SoT: 09-layoutBase/Admin - Soạn đề bài.dc.html:626, 718-721], và điều kiện "Tổng trọng số bằng 100" còn trong checklist xuất bản [SoT: 09-layoutBase/Admin - Soạn đề bài.dc.html:666]. Đây là **nợ đã đánh dấu xong nhưng chưa làm**, không phải một tính năng được giữ có chủ đích: nếu giữ có chủ đích thì mục 2.6 sẽ không ghi "đã sửa". | Bản thân quyết định đã rõ (bỏ, vì fail-fast F4-04 dừng ở testcase sai đầu tiên nên không thể cộng điểm theo trọng số [SoT: 01-rd/req/req.md:249-250]) — cái chưa rõ chỉ là **ai áp dụng và khi nào**: sửa prototype ngay bây giờ, hay để tới lúc dựng UI Next.js thật rồi bỏ luôn. RD này không tự sửa prototype. | Không mở lại quyết định — giữ Pass/Fail toàn phần. Xoá cột "Điểm", khối "Chấm điểm từng phần" và điều kiện "Tổng trọng số bằng 100" **khi dựng UI thật**, và **sửa lại câu chữ ở `PROTOTYPE_DEBT.md` mục 2.6** từ "Đã sửa prototype" thành "chờ áp dụng khi dựng UI" để nợ không bị coi là đã trả. RD này **đã đặc tả theo bản đã bỏ trọng số**: mục 3.2 chỉ mô tả cột "Điểm" như xung đột, không coi là yêu cầu. | Chủ dự án (xác nhận), điều phối RD (sửa câu chữ nợ) |
| Q4 | Cao | **Prototype thiếu hoàn toàn UI cho F2-03, F2-04 và F2-09 — ba yêu cầu đã chốt.** Survey gán slug này cho "F2-01 tới F2-04, F2-14" [SoT: 01-rd/overview/system_survey.md:527], nhưng grep trên toàn bộ prototype cho các từ "hàm", "chữ ký", "so khớp", "EXACT", "TRIMMED", "EPSILON", "UNORDERED", "stdin", "phiên bản" **không trả về kết quả nào**. Nghĩa là: không có chỗ khai **chữ ký hàm theo từng ngôn ngữ + kiểu tham số/kiểu trả về** (F2-03 — đầu vào **bắt buộc** để F3 sinh mã khung [SoT: 01-rd/req/req.md:144-145]), không có chỗ khai **định dạng I/O chuẩn** cho mô hình Standard I/O (F2-03 [SoT: 01-rd/req/req.md:146-147]), không có chỗ chọn **chiến lược so khớp** (F2-04 [SoT: 01-rd/req/req.md:151-152]), không có chỗ nào thấy **phiên bản bộ testcase** (F2-09 [SoT: 01-rd/req/req.md:159]). | Đây là khoảng trống prototype, không phải khoảng trống yêu cầu — ba mã đều đã chốt trong `req.md` và F2-03 còn có một quyết định kiến trúc chống lưng (`DEC-2026-0824-dual-submission-model-per-problem`). Việc cần chủ dự án quyết là **hình dạng UI**: thêm tab thứ năm, hay nhét vào tab "Nội dung đề". | Thêm **một tab "Đặc tả" riêng** (tab thứ năm) chứa: chữ ký hàm cho từng ngôn ngữ trong ba ngôn ngữ Java/C++/Python, lược đồ kiểu tham số và kiểu trả về, định dạng đọc `stdin`/in `stdout` cho mô hình Standard I/O, và một trường chọn chiến lược so khớp `EXACT`/`TRIMMED`/`EPSILON`/`UNORDERED_SET`. Lý do tách tab: đây là dữ liệu **F3 đọc để sinh mã**, sai một chữ là mã khung sai cho cả ba ngôn ngữ — trộn vào tab đề bài dễ bị bỏ sót. Phiên bản bộ testcase (F2-09) hiển thị ở đầu tab Testcase kèm liên kết mở lịch sử phiên bản (gắn với Q2). **Không xuất bản được bài nếu tab này còn trống** — thêm vào checklist "Sẵn sàng xuất bản". | Chủ dự án |
| Q5 | Trung bình | **"Gợi ý theo cấp độ" kèm trừ điểm là một tính năng hoàn chỉnh trên prototype nhưng không có mã `Fx-nn` nào phủ** [SoT: 09-layoutBase/Admin - Soạn đề bài.dc.html:309-326, 635-637]. Nặng hơn: nhãn "−5 điểm" / "−12 điểm" giả định hệ thống có **điểm số cho một bài toán**, trong khi F4 chỉ có Pass/Fail toàn phần và fail-fast [SoT: 01-rd/req/req.md:249-250], và chính vì thế mà chấm điểm từng phần đã bị bỏ (Q3). Phía người học cũng đã có tab "Gợi ý" trong Workspace mà chưa gắn mã [SoT: 01-rd/screens/users/problem_detail.md:51], và ô "Lượt xin gợi ý" trong Số liệu bài [SoT: 09-layoutBase/Admin - Soạn đề bài.dc.html:704] — cùng một tính năng, ba chỗ, không mã nào. | Không được tự phát minh mã mới, và cũng không thể coi đây là chi tiết vận hành của một mã có sẵn: gợi ý theo cấp độ là dữ liệu mới (nội dung gợi ý, thứ tự mở, chi phí), luồng mới (người học mở dần), và có hệ quả tính điểm. | Hai bước, tách rời: (a) **Giữ tính năng gợi ý theo cấp độ** — nó có mặt ở cả hai đầu (soạn và học) nên rất khó là dữ liệu mẫu ngẫu nhiên — và mở **một mã F2 mới** cho nó khi chủ dự án đồng ý (RD này không tự đặt số hiệu). (b) **Bỏ phần "trừ điểm"**: đổi nhãn chi phí thành nhãn trung tính (ví dụ "Gợi ý nhẹ / vừa / mạnh" hoặc "mức 1/2/3"), vì mô hình chấm hiện tại không có điểm số để trừ — cùng lý do đã bỏ chấm điểm từng phần ở Q3. Nếu chủ dự án muốn giữ trừ điểm thật thì đó là một hệ điểm mới cho toàn hệ thống, phải mở `DEC-` riêng. | Chủ dự án |
| Q6 | Trung bình | **"Chỉ dẫn cho trợ lý AI" theo từng bài + 3 cờ hành vi AI — không có mã phủ, và có mặt rủi ro bảo mật.** Prototype ghi rõ nội dung này "nối vào prompt hệ thống ở màn Cấu hình AI" [SoT: 09-layoutBase/Admin - Soạn đề bài.dc.html:330], nhưng F5-23 chỉ định nghĩa cấu hình prompt/rubric **ở cấp hệ thống, actor A3** [SoT: 01-rd/req/req.md:360-365] — không có mã nào cho ngữ cảnh AI **theo từng bài toán** do người soạn nhập. Riêng cờ "Cho phép AI mở gợi ý ẩn" [SoT: 09-layoutBase/Admin - Soạn đề bài.dc.html:647] còn phụ thuộc tính năng ở Q5. | Hai câu hỏi lồng nhau: (1) phạm vi — có cho phép ngữ cảnh AI theo bài không; (2) an toàn — F5-17 yêu cầu "mã nguồn và câu trả lời người dùng là tham số dữ liệu, tách hoàn toàn khỏi chỉ thị hệ thống" [SoT: 01-rd/req/req.md:343-344], nhưng **không nói gì về văn bản do giảng viên nhập rồi ghép vào system prompt** — đây là một lỗ trống trong ràng buộc F5-17, không tự lấp được. | Giữ tính năng (ngữ cảnh theo bài làm chất lượng phỏng vấn/phân tích tốt hơn rõ rệt) nhưng **coi nó là chỉ thị bậc hai, không phải chỉ thị hệ thống**: nối vào prompt trong một khối có nhãn riêng, không được ghi đè các ràng buộc cứng của F5-17/F5-18, và chỉ vai trò có `PROBLEM_AUTHORING:UPDATE` trong ma trận F1-10 mới sửa được. Cần chủ dự án chốt phạm vi rồi mới mở mã (F2 hay F5 — RD này không tự chọn). Nút **"Nhờ AI soạn nháp"** [SoT: 09-layoutBase/Admin - Soạn đề bài.dc.html:312] cùng nhóm câu hỏi: F2-14 chỉ phủ sinh **testcase**, không phủ AI soạn **gợi ý/đề bài** — đề xuất quyết chung một lượt với Q5 để không mở ba mã rời rạc cho cùng một ý "AI hỗ trợ người soạn đề". | Chủ dự án |
| Q7 | Thấp | **Nhóm các phần trên màn chưa gắn được mã nào, mức ảnh hưởng nhỏ, gom lại quyết một lượt:** (a) **Vòng đời bài toán** Nháp/Đã xuất bản/Ẩn + nút "Lưu và xuất bản" + lưu nháp tự động [SoT: 09-layoutBase/Admin - Soạn đề bài.dc.html:692, 724, 156]; (b) **checklist "Sẵn sàng xuất bản"** với các ngưỡng cụ thể ≥ 8 testcase, ≥ 2 công khai, ≥ 2 ví dụ [SoT: 09-layoutBase/Admin - Soạn đề bài.dc.html:664-670]; (c) **"Xem như người học"** [SoT: 09-layoutBase/Admin - Soạn đề bài.dc.html:155]; (d) **"Thẻ" tự do** — F2-02 chỉ nói độ khó và chủ đề, F2-11 chỉ lọc theo chủ đề/độ khó/trạng thái đã giải [SoT: 01-rd/req/req.md:139, 164], không mã nào nói tới thẻ; (e) **hai trường giới hạn "Kích thước đầu ra" và "Số lần nộp / giờ"** [SoT: 09-layoutBase/Admin - Soạn đề bài.dc.html:696-697] — F2-10 chỉ nói thời gian và bộ nhớ; (f) **"Số liệu bài"** (Lượt nộp, Tỉ lệ AC, Thời gian giải TB) [SoT: 09-layoutBase/Admin - Soạn đề bài.dc.html:700-705]; (g) **"Sửa lần cuối ... bởi Phú Đại"** [SoT: 09-layoutBase/Admin - Soạn đề bài.dc.html:402] — audit sửa nội dung, trong khi F1-14 đã bị thu hẹp chỉ còn hành động quản trị [SoT: 06-plan/PROTOTYPE_DEBT.md:74-77]. | Mỗi mục đều nhỏ và nhìn qua thì "hiển nhiên phải có", nhưng theo luật của dự án thì không có mã nghĩa là không có yêu cầu, và các con số ngưỡng (8/2/2) là quyết định nghiệp vụ chứ không phải chi tiết UI. | (a) và (b): coi là hệ quả vận hành trực tiếp của F2-01 (soạn đề thì phải có nháp và điều kiện xuất bản), **không mở mã mới**, nhưng **các ngưỡng 8/2/2 phải do chủ dự án chốt** rồi ghi thẳng vào `req.md` mục F2 như tham số của F2-01. (c) và (f): không mở mã — (c) là chế độ xem trước của cùng dữ liệu F2-01, (f) dùng lại nguồn số liệu của `admin_overview` [SoT: 01-rd/overview/system_survey.md:547]. (d): xác nhận có giữ "thẻ" hay không; nếu giữ thì F2-02 và F2-11 phải được mở rộng câu chữ, vì lọc theo thẻ là kỳ vọng đương nhiên của người học. (e): xác nhận hai trường này thuộc F2-10 (mở rộng câu chữ) hay bỏ khỏi màn để tránh trùng `admin_language_config` (F4-11). (g): xác nhận có cần audit "ai sửa bài toán lúc nào" không; nếu cần thì đây là audit **nội dung**, khác F1-14 (audit **hành động quản trị**) và cần chỗ riêng. | Chủ dự án |

## 6. Ngoài phạm vi file này

- Bố cục, bảng màu, spacing, cấu trúc component (thanh sticky, 4 tab, cột thuộc tính dính, kéo-thả bảng
  testcase) — thuộc BD (`02-bd/screens/shared/problem_authoring.md`, chưa viết).
- Hợp đồng API (CRUD bài toán, CRUD testcase, tải lên hàng loạt, chạy đáp án mẫu, sinh testcase bằng AI) —
  thuộc DD (`03-dd/api/problem-bank.md`, `03-dd/api/harness.md`, `03-dd/api/ai-review.md`, chưa viết).
- Lược đồ kiểu dữ liệu độc lập ngôn ngữ và thuật toán sinh mã khung từ chữ ký hàm — thuộc `harness` (F3-01
  tới F3-06, `01-rd/req/req.md:203-209`), không thuộc trục màn.
- Lưu trữ bộ testcase lớn trên MinIO (F2-07) — thuộc BD storage (`02-bd/storage/problem-bank.md`, chưa
  viết).
- Ma trận phân quyền và các ô `PROBLEM_AUTHORING`/`TESTCASE_MANAGEMENT` — thuộc màn
  `admin_permission_matrix` (`01-rd/screens/admin/admin_permission_matrix.md`), không lặp lại ở đây.
- Bảng danh sách bài toán, tìm kiếm, phân trang, nút "Bài tập mới" — thuộc màn cha
  `01-rd/screens/shared/problem_management.md`.
- Hệ số nhân giới hạn theo ngôn ngữ (F2-10 phần hệ số) và cấu hình ngôn ngữ (F4-11) — thuộc
  `01-rd/screens/admin/admin_language_config.md`.

## 7. Tham chiếu

- `01-rd/req/req.md:55-68` — F1-10 tới F1-12 (ma trận phân quyền, `PROBLEM_AUTHORING`, `TESTCASE_MANAGEMENT`).
- `01-rd/req/req.md:136-195` — toàn bộ mục F2: F2-01 tới F2-14.
- `01-rd/req/req.md:249-250` — F4-04 fail-fast (căn cứ bỏ chấm điểm từng phần, Q3).
- `01-rd/req/req.md:343-344, 360-365` — F5-17 (chống prompt injection), F5-23 (cấu hình prompt cấp hệ thống).
- `01-rd/req/user_stories.md:203-264` — `US-A2-01`, `US-A2-02`, `US-A2-05`.
- `01-rd/overview/system_survey.md` mục 7.2 (khu Giảng viên) và mục 7.0 (khu dùng chung).
- `01-rd/screens/shared/problem_management.md` — màn cha, cửa vào màn này.
- `01-rd/screens/users/problem_detail.md:51` — tab "Gợi ý" phía người học (đối ứng của Q5).
- `01-rd/screens/admin/admin_queue_monitor.md` — khuôn mẫu cấu trúc của file này.
- `06-plan/PROTOTYPE_DEBT.md` mục 2.6 (chấm điểm từng phần và AI sinh testcase) và mục 7 (đợt đối chiếu).
- `09-layoutBase/Admin - Soạn đề bài.dc.html` — prototype (736 dòng).
- `09-layoutBase/Admin - Quản lý bài tập.dc.html` — màn cha, cửa vào màn này.
- `.nexa/control/decision-registry.md` — `DEC-2026-0824-dual-submission-model-per-problem` (nền của F2-03),
  `DEC-2026-0825-frontend-base-architecture` (tiền tố `/instructor` và `/admin`),
  `DEC-2026-0825-shared-content-authoring-screens` (dùng chung màn, chốt Q1/Q2).
