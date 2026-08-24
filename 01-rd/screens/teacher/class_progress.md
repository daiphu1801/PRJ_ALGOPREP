# RD — Màn `class_progress` (Tiến độ lớp)

> Slug: `class_progress` — khớp `01-rd/overview/system_survey.md` mục 7.2, dòng bảng slug
> [SoT: 01-rd/overview/system_survey.md:524]. Bounded Context: `identity` + `judge-orchestration`
> [SoT: 01-rd/overview/system_survey.md:524]. Actor: A2 (Giảng viên).
>
> Prototype đối chiếu: `09-layoutBase/Giáo viên - Tiến độ học viên.dc.html` (đã dựng thật, chưa sửa nợ quy
> chuẩn — xem mục 3 và Câu hỏi mở).
>
> File này mô tả **hành vi và UX ở mức yêu cầu** của một màn cụ thể — không lặp lại đặc tả chức năng đã có
> ở `01-rd/req/req.md` hay `01-rd/req/user_stories.md`, chỉ trỏ tới và bổ sung phần đặc thù của **một màn**.
> Theo đúng ràng buộc phiên làm việc này: **không sửa** `01-rd/req/req.md`,
> `01-rd/req/user_stories.md`, `01-rd/overview/system_survey.md`, `.nexa/control/decision-registry.md` —
> mọi khoảng trống phát hiện được ghi ở mục 5 (Câu hỏi mở), không tự thêm mã hay tự quyết định kiến trúc.

## 1. Mục đích màn hình

Cho giảng viên (A2) xem tiến độ học tập **tổng hợp của nhiều học viên** trong (các) lớp mình phụ trách —
điểm trung bình theo lớp, danh sách học viên kèm điểm/tỉ lệ hoàn thành/chuỗi ngày hoạt động, và một khối
"Cần chú ý" nêu các học viên có dấu hiệu tụt lại — để giảng viên phát hiện sớm học viên cần hỗ trợ mà không
phải mở từng trang tiến độ cá nhân
[SoT: 09-layoutBase/Giáo viên - Tiến độ học viên.dc.html:117-118, 136-150].

**Khác với `my_progress` (A1 xem tiến độ của chính mình, dựa F1-06/F1-07/F1-08):** màn này là góc nhìn
**tổng hợp nhiều học viên** của A2, hiện **chưa có mã `Fx-nn` mô tả trực tiếp** — xem mục 2 và Câu hỏi mở
Q1.

## 2. Nguồn yêu cầu (không lặp lại — chỉ trỏ)

| Hành vi | Mã | Nguồn |
| :--- | :--- | :--- |
| Bài đã giải theo chủ đề (định nghĩa gốc, viết cho A1 xem tiến độ cá nhân) | F1-06 | `01-rd/req/req.md:38` |
| Tỉ lệ chấp thuận — Accepted / tổng bài nộp (định nghĩa gốc, viết cho A1 xem tiến độ cá nhân) | F1-07 | `01-rd/req/req.md:38-39` |
| Bảng slug mục 7.2 ghi `class_progress` là "**dẫn xuất từ F1-06, F1-07**" — không phải mã trực tiếp cho góc nhìn tổng hợp nhiều học viên của A2 | (dẫn xuất) | `01-rd/overview/system_survey.md:524` |
| Cơ chế xác định "lớp giảng viên phụ trách" — giao bài theo lớp, chỉ sinh viên lớp đó thấy bài | F2-12 | `01-rd/req/user_stories.md:230-236` (`US-A2-03`) |
| Cùng cơ chế lớp giảng viên phụ trách áp dụng cho phạm vi hiển thị màn chấm bài (`instructor_grading`) — dẫn chiếu để dùng chung cơ chế, không phát minh riêng cho `class_progress` | F5-27 | `01-rd/req/req.md:401` |
| Gác quyền mở màn bằng Function `CLASS_MANAGEMENT` trong ma trận phân quyền | F1-10, F1-12 | `01-rd/req/req.md:64`, `01-rd/req/req.md:401-402` |
| Given-When-Then của A2 xem lớp/bài tập, không có mục nào cho "xem tiến độ tổng hợp" | — | `01-rd/req/user_stories.md:230-278` (`US-A2-03` tới `US-A2-06` — không có US riêng cho `class_progress`, xem Câu hỏi mở Q1) |

**Nhận định về khoảng trống traceability:** `01-rd/req/req.md` mục F1 chỉ định nghĩa F1-06/F1-07 cho
**A1 xem chính mình** [SoT: 01-rd/req/req.md:37-39 — đoạn mở đầu ghi rõ "Trang tiến độ cá nhân"]. Không có
đoạn nào trong `req.md` hay mục `US-A2-*` của `user_stories.md` mô tả A2 xem **tổng hợp nhiều học viên**
(bảng điểm trung bình theo lớp, danh sách học viên, khối "Cần chú ý", biểu đồ xu hướng). Đây là dạng khoảng
trống giống việc phát hiện ở màn `my_submissions` (Phase 2) theo mô tả trong yêu cầu công việc — cần chủ dự
án xác nhận mã mới, **không tự suy diễn thành F1-06/F1-07 áp dụng cho A2**. Ghi ở Câu hỏi mở Q1, không tự
sửa `req.md`.

## 3. Trạng thái và cấu trúc màn (screen states)

Đối chiếu `09-layoutBase/Giáo viên - Tiến độ học viên.dc.html` — hành vi UX thật đã dựng, không phải suy
diễn:

1. **Trạng thái mặc định** — bố cục 3 khối xếp dọc [SoT: 09-layoutBase/Giáo viên - Tiến độ học viên.dc.html:112-190]:
   - Tiêu đề "Tiến độ học viên" kèm phụ đề "Theo dõi điểm số, hoàn thành và mức độ hoạt động" (dòng 117-118).
   - Khối trên gồm 2 cột: biểu đồ đường "Điểm trung bình theo lớp" 6 tuần gần nhất, vẽ 2 đường cho 2 lớp
     (dòng 122-134); và khối "Cần chú ý" liệt kê tối đa vài học viên có nhãn cảnh báo (dòng 136-150).
   - Bảng danh sách học viên: cột Học viên, Lớp, Điểm TB, Hoàn thành, Chuỗi ngày, Xu hướng (sparkline),
     Hoạt động gần nhất (dòng 168-186).
2. **Bộ lọc lớp** — tab ngang `Tất cả` / theo tên lớp cụ thể (`Thuật toán K21`, `CTDL K22`, `Luyện PV nâng
   cao`), lọc trực tiếp bảng học viên, không tải lại trang (dòng 160-163, 288-293, 296-300).
3. **Ô tìm kiếm học viên** theo tên, lọc cùng lúc với tab lớp (dòng 155-158, 295, 299).
4. **Đếm kết quả** — hiển thị dạng "N / 82 học viên" ở góc phải thanh lọc (dòng 164, 320) — con số tổng
   "82" là dữ liệu giả lập cứng trong `data()`, không phải hành vi thật (dòng 226-234 chỉ có 8 bản ghi mẫu).
5. **Nhãn "Cần chú ý"** — 4 loại nhãn quan sát được trong dữ liệu mẫu: "Cần hỗ trợ" (điểm giảm liên tiếp
   nhiều tuần), "Vắng bài" (không hoạt động nhiều ngày), "Theo dõi" (hoàn thành dưới ngưỡng hoặc điểm chững
   lại) — logic phát sinh nhãn này **được viết cứng trong mảng dữ liệu mẫu** (dòng 281-286), không có công
   thức/ngưỡng tường minh nào trong code lẫn trong `req.md`/`user_stories.md` — xem Câu hỏi mở Q2.
6. **Cột "Điểm TB"** hiển thị số thang 10 (ví dụ `8.9`), khác về đơn vị với "tỉ lệ chấp thuận" (%) ở F1-07 —
   giống cách trình bày điểm quy đổi thang 10 của F5-27 (điểm AI tham khảo cho từng bài nộp) hơn là F1-07,
   nhưng đây là điểm **theo học viên** (tổng hợp nhiều bài), còn F5-27 là điểm **theo từng bài nộp** — chưa
   rõ công thức tổng hợp. Xem Câu hỏi mở Q3.
7. **Cột "Hoàn thành"** hiển thị phần trăm (ví dụ `96%`) — chưa rõ là tỉ lệ Accepted/tổng nộp (gần khớp tên
   gọi F1-07) hay tỉ lệ số bài đã hoàn thành trên tổng số bài được giao trong lớp (một khái niệm khác, gần
   với F2-12 — giao bài theo lớp). Hai cách hiểu cho ra công thức khác nhau. Xem Câu hỏi mở Q3.
8. **Cột "Chuỗi ngày"** (streak) và cột "Xu hướng" (sparkline điểm 6 mốc gần nhất) — hai chỉ số này **không
   xuất hiện trong bất kỳ mã `Fx-nn` nào** đã đọc được ở `req.md` — khoảng trống mã riêng, xem Câu hỏi mở
   Q4.
9. **Điều hướng** — mục "Tiến độ học viên" trong sidebar khu Giảng viên, cạnh "Tổng quan" / "Lớp của tôi" /
   "Bài tập của tôi" / "Chấm bài" [SoT: 09-layoutBase/Giáo viên - Tiến độ học viên.dc.html:244-250] — khớp
   layout riêng biệt khu Giảng viên đã chốt ở `01-rd/overview/system_survey.md:508-512` (Phương án B, tách
   shell khỏi Admin).
10. Không thấy trạng thái rỗng (lớp chưa có học viên), trạng thái lỗi tải dữ liệu, hay phân trang cho danh
    sách học viên khi lớp đông — `data()` chỉ có 8 bản ghi mẫu trong khi đếm kết quả hiển thị "82 học viên"
    (dòng 226-234, 320) — chưa rõ hành vi thật khi danh sách dài. Xem Câu hỏi mở Q5.

## 4. Given-When-Then bổ sung ở mức màn

Vì không có US-A2 nào mô tả trực tiếp hành vi "xem tổng hợp nhiều học viên" (mục 2), các Given-When-Then
dưới đây được viết **từ hành vi UX quan sát được ở prototype**, đánh dấu rõ nguồn suy luận, chờ chủ dự án
xác nhận mã và gộp chính thức vào `req.md`/`user_stories.md` sau:

- **Cho** tôi là giảng viên đã đăng nhập và có ít nhất một lớp phụ trách, **Khi** tôi mở màn `class_progress`,
  **Thì** tôi chỉ thấy học viên thuộc (các) lớp mình phụ trách, dùng cùng cơ chế xác định phạm vi lớp đã
  dùng ở F2-12/F5-27 [SoT: 01-rd/req/req.md:401 — "Phạm vi hiển thị: theo lớp giảng viên phụ trách (cùng cơ
  chế với F2-12)"; `[SoT: Suy luận]` khi áp dụng sang màn này vì `req.md` không nêu đích danh `class_progress`].
- **Cho** danh sách học viên đang hiển thị, **Khi** tôi chọn một tab lớp cụ thể, **Thì** bảng và khối "Cần
  chú ý" đều lọc theo đúng lớp đó, không cần tải lại trang
  [SoT: 09-layoutBase/Giáo viên - Tiến độ học viên.dc.html:159-163, 288-300 — riêng việc khối "Cần chú ý"
  có lọc theo tab hay không **chưa thấy code xử lý** (mảng `attention` ở dòng 281-286 không đi qua bộ lọc
  `classFilter`) — khả năng là thiếu sót của prototype, xem Câu hỏi mở Q6].
- **Cho** tôi không có quyền `CLASS_MANAGEMENT` hoặc không phụ trách lớp nào, **Khi** tôi cố mở màn
  `class_progress`, **Thì** hệ thống từ chối và không hiển thị dữ liệu học viên của lớp khác
  [SoT: 01-rd/req/req.md:401-402 — áp dụng cùng cơ chế gác quyền của F5-27; `[SoT: Suy luận]` cho riêng màn
  này].

## 5. Câu hỏi mở (chưa trả lời — không tự chọn thay)

| # | Câu hỏi | Vì sao chưa trả lời được | Đề xuất | Chủ sở hữu |
| :-: | :--- | :--- | :--- | :--- |
| Q1 | `class_progress` (A2 xem tổng hợp nhiều học viên) hiện chỉ được ghi là "dẫn xuất từ F1-06, F1-07" — hai mã này định nghĩa gốc cho A1 xem **chính mình**, không có mã `Fx-nn` hay `US-A2-nn` nào mô tả trực tiếp góc nhìn tổng hợp của giảng viên. | `req.md` mục F1 (dòng 37-39) và `user_stories.md` (`US-A2-01` tới `US-A2-06`, dòng 203-278) đều không có đoạn nào cho hành vi này; đối chiếu prototype cho thấy đây là một màn có thật với nghiệp vụ riêng (biểu đồ điểm trung bình lớp, khối cảnh báo, bảng tổng hợp), không phải chỉ là hiển thị lại F1-06/F1-07 cho nhiều người. | Bổ sung mã mới, ví dụ `F1-18` (nhóm `identity`, cạnh F1-06/F1-07) hoặc gắn vào nhóm `CLASS_MANAGEMENT` như một hành vi con của F2-12 (ví dụ `F2-12a`) — chủ dự án quyết định nhóm và số hiệu, không tự thêm vào `req.md` trong phiên này. | Chủ dự án |
| Q2 | Nhãn "Cần chú ý" (Cần hỗ trợ / Vắng bài / Theo dõi) áp dụng ngưỡng nào? Ví dụ "không hoạt động bao nhiêu ngày" thì gắn "Vắng bài", "hoàn thành dưới bao nhiêu %" thì gắn "Theo dõi". | Prototype viết cứng nhãn theo từng bản ghi mẫu (`09-layoutBase/Giáo viên - Tiến độ học viên.dc.html:281-286`), không có công thức/ngưỡng tính toán nào trong code lẫn trong tài liệu yêu cầu đã đọc. | Định nghĩa ngưỡng cụ thể (số ngày không hoạt động, % hoàn thành, số tuần điểm giảm liên tiếp) khi chốt mã ở Q1. | Chủ dự án |
| Q3 | Cột "Điểm TB" (thang 10) và cột "Hoàn thành" (%) tính từ công thức nào — F1-07 (tỉ lệ Accepted/tổng nộp), quy đổi kiểu F5-27 (điểm AI tham khảo, nhưng đó là điểm theo bài nộp, không phải theo học viên), hay tỉ lệ hoàn thành bài được giao theo F2-12? | `req.md` không định nghĩa cách tổng hợp nhiều bài nộp/nhiều bài được giao thành một điểm/tỉ lệ đại diện cho một học viên trong một lớp; ba nguồn có thể (F1-07, F5-27, F2-12) đều không khớp hoàn toàn với mô tả cột. | Làm rõ công thức tổng hợp khi bổ sung mã ở Q1, đồng thời nêu rõ nếu điểm TB có dùng dữ liệu chấm tay/AI tham khảo của F5-27 hay không. | Chủ dự án |
| Q4 | Hai chỉ số "Chuỗi ngày" (streak hoạt động liên tục) và "Xu hướng" (sparkline điểm 6 mốc) không khớp mã `Fx-nn` nào đã đọc được. Đây là chỉ số mới cần thêm, hay chỉ là trang trí prototype không đưa vào bản chính thức? | Không tìm thấy khái niệm "chuỗi ngày hoạt động" hay "xu hướng điểm theo thời gian" ở `req.md`/`user_stories.md`/`system_survey.md` cho bất kỳ actor nào. | Nếu giữ lại, cần định nghĩa "hoạt động" là gì (có nộp bài trong ngày? có đăng nhập?) khi bổ sung mã ở Q1; nếu bỏ, ghi rõ trong `06-plan/PROTOTYPE_DEBT.md`. | Chủ dự án |
| Q5 | Danh sách học viên khi lớp đông (prototype ghi "82 học viên" nhưng chỉ có 8 bản ghi mẫu) hiển thị thế nào — phân trang, cuộn vô hạn, hay tải toàn bộ một lần? Trạng thái rỗng (lớp chưa có học viên) và trạng thái lỗi tải dữ liệu chưa thấy trong prototype. | `data()` trong prototype chỉ là mảng tĩnh 8 phần tử, không có logic phân trang hay xử lý lỗi (`09-layoutBase/Giáo viên - Tiến độ học viên.dc.html:224-235`). | Chốt cơ chế phân trang/cuộn khi làm BD; tối thiểu cần định nghĩa trạng thái rỗng và trạng thái lỗi trước khi viết DD. | Chủ dự án |
| Q6 | Khối "Cần chú ý" có lọc theo tab lớp đang chọn hay luôn hiển thị toàn bộ lớp bất kể tab? | Trong code, mảng `attention` (dòng 281-286) không đi qua biến `classFilter` như bảng học viên (dòng 296-300) — có thể là chủ ý (khối cảnh báo luôn nhìn toàn lớp) hoặc thiếu sót của prototype, không đủ căn cứ để khẳng định. | Xác nhận ý đồ: nếu chủ ý thì ghi rõ vào RD khi chốt Q1; nếu thiếu sót thì sửa ở lần dựng prototype tiếp theo. | Chủ dự án |

## 6. Ngoài phạm vi file này

- Mã yêu cầu mới, công thức tính điểm/tỉ lệ, ngưỡng cảnh báo — chờ chốt ở Câu hỏi mở Q1-Q4, sau đó ghi vào
  `01-rd/req/req.md`/`01-rd/req/user_stories.md` bởi chủ dự án hoặc phiên được giao quyền sửa các file đó
  (không thuộc phạm vi phiên này).
- Layout, vùng bố cục, bảng màu, component cụ thể — thuộc BD (`02-bd/screens/teacher/class_progress.md`,
  chưa viết).
- Hợp đồng API (request/response lấy danh sách học viên, dữ liệu biểu đồ) — thuộc DD
  (`03-dd/api/identity.md` hoặc `03-dd/api/judge-orchestration.md`, chưa viết).
- Cơ chế RBAC/Function `CLASS_MANAGEMENT` chi tiết (định nghĩa Action, seed dữ liệu ma trận) — thuộc BD/DD
  của module `identity`, không thuộc file theo trục màn này.

## 7. Tham chiếu

- `01-rd/overview/system_survey.md:524` — dòng `class_progress` trong bảng slug mục 7.2.
- `01-rd/overview/system_survey.md:508-512` — quyết định layout riêng biệt khu Giảng viên (Phương án B).
- `01-rd/req/req.md:37-39` — F1-06, F1-07 (định nghĩa gốc, viết cho A1).
- `01-rd/req/req.md:64`, `01-rd/req/req.md:401-402` — F1-12 (Function `CLASS_MANAGEMENT`), F5-27 (cơ chế
  phạm vi lớp giảng viên phụ trách).
- `01-rd/req/user_stories.md:230-278` — `US-A2-03` tới `US-A2-06`.
- `09-layoutBase/Giáo viên - Tiến độ học viên.dc.html` — prototype đối chiếu.
- `06-plan/PROTOTYPE_DEBT.md:534-545` — mục 6.2.b, ghi nhận `class_progress` đã có prototype thật, đối
  chiếu tên file/slug.
- `06-plan/nexa-plan/260824-2043-bd-screens-common-first.md:45` — xếp `class_progress` vào nhóm màn giảng
  viên có thể cắt ở Stage 5 nếu thiếu thời gian.
