# RD — Màn `problem_list` (Ngân hàng bài toán)

> Slug: `problem_list` — khớp `01-rd/overview/system_survey.md` mục 7.1 dòng `problem_list`
> [SoT: 01-rd/overview/system_survey.md:474]. Bounded Context: `problem-bank` (F2), có đọc thêm từ
> `identity` (trạng thái đã giải theo người dùng) và `harness` (nhãn mô hình nộp bài của từng bài)
> [SoT: .nexa/control/dependency-map.md mục 5 — dòng `problem_list`]. Actor: A1 (chính), A2 (khi xem để giao
> bài — màn thật cho A2 giao bài là `class_management`, không phải file này).
>
> Đối chiếu prototype: `09-layoutBase/Ngân hàng bài toán.dc.html`. File này mô tả **hành vi và UX ở mức yêu
> cầu** — không lặp lại đặc tả chức năng đã có ở `01-rd/req/problem-bank.md` (mục F2) và
> `01-rd/req/user_stories/a1_student.md` (`US-A1-02`), chỉ trỏ tới và bổ sung phần đặc thù của màn.

## 1. Mục đích màn hình

Danh sách toàn bộ bài toán để người học tìm và chọn bài luyện tập, lọc theo chủ đề/độ khó/trạng thái đã
giải, kèm khu vực riêng hiển thị bài đã được giảng viên giao theo lớp
[SoT: 01-rd/req/problem-bank.md — F2-11, F2-12].

## 2. Nguồn yêu cầu (không lặp lại — chỉ trỏ)

| Hành vi | Mã | Nguồn |
| :--- | :--- | :--- |
| Tìm kiếm, lọc theo chủ đề/độ khó/trạng thái đã giải | F2-11 | `01-rd/req/problem-bank.md` — F2-11 |
| Bài giao theo lớp hiển thị thành nhóm riêng trong cùng màn | F2-12 | `01-rd/req/problem-bank.md` — F2-12 |
| Cả hai mô hình nộp bài luôn song song cho mỗi bài toán; học viên tự chọn mỗi lượt làm ở `problem_detail`, trừ khi lược đồ kiểu chưa phủ được thì chỉ còn Standard I/O | F3-13 | `01-rd/req/harness.md` — F3-13 |
| Given-When-Then đầy đủ | — | `01-rd/req/user_stories/a1_student.md` (`US-A1-02`) |

## 3. Trạng thái và cấu trúc màn (screen states)

Đối chiếu `09-layoutBase/Ngân hàng bài toán.dc.html` [SoT: 09-layoutBase/Ngân hàng bài toán.dc.html]:

1. **Danh sách chính** — bảng bài toán với cột: trạng thái đã giải (chấm tròn), mã số, tên, **nhãn mô hình
   nộp bài** (`function`/`stdio`, dòng 174), badge "đã có Solution Review" nếu bài đó người học từng phân
   tích qua F5.1 (dòng 175-177, trường `hasReview`), chủ đề, độ khó, tỉ lệ Accepted, và nút "Vào giải" hiện
   khi rê chuột qua dòng (dòng 167-189). **Cập nhật 2026-08-25 theo `DEC-2026-0824-dual-submission-model-per-problem`**:
   dữ liệu mẫu của prototype gán `mode` cố định 1-trong-2 cho từng bài (dòng 501-524), khớp cách hiểu **cũ**
   của F3-13 — theo quyết định mới, cả hai mô hình luôn song song cho mọi bài (trừ ngoại lệ lược đồ kiểu ở
   dưới). **Đã chốt (Câu hỏi mở Q3, đã đóng):** nhãn đổi thành "Cả hai" mặc định, chỉ "Chỉ Standard I/O" cho
   ngoại lệ — chưa dựng vào prototype, để lúc build FE Next.js thật.
2. **Bộ lọc** — tab trạng thái (Tất cả/Đã giải/Đang làm/Chưa làm), chip chủ đề, thanh tìm kiếm, sắp xếp theo
   độ khó hoặc tỉ lệ Accepted (dòng 546-563, 609-611) — khớp F2-11.
3. **Sidebar chủ đề** — danh sách chủ đề kèm tiến độ `đã giải/tổng số` riêng theo từng chủ đề (dòng 572-579)
   — không có mã `Fx-nn` riêng, là cách trình bày tổng hợp của F2-11 + F1-06 (tiến độ theo chủ đề).
4. **Khu "Bài tập lớp"** — một khối riêng trong cùng trang (không phải lồng vào từng dòng bảng), liệt kê tên
   lớp và các bài đã gán (dòng 222-236) — khớp tinh thần F2-12 ("không phải một màn tách biệt"), nhưng
   **hiển thị dạng khối tổng hợp cạnh bảng chính, không phải lồng trực tiếp vào từng dòng bài toán** như câu
   chữ ở `problem-bank.md` — F2-12 ("lồng thành một nhóm riêng ngay trong danh sách bài toán") có thể khiến người đọc
   hiểu nhầm là chèn xen kẽ vào bảng — xem Câu hỏi mở Q1.
5. **Phân trang** — cuối bảng, không phải infinite-scroll (dòng 194-199).
6. **`isSolve`** (trạng thái nội bộ trong file, dòng 242+) — có UI Run Code/Submit y hệt Workspace, nhưng
   hàm `open(p)` thực tế **luôn điều hướng sang file riêng** `Workspace giải bài.dc.html`
   (`window.location.href`, dòng 489-494), không bao giờ bật `isSolve` — có vẻ là code còn sót lại từ một
   thiết kế cũ (danh sách + giải bài trong cùng trang) đã đổi hướng sang trang riêng — **đã chốt ở Câu hỏi
   mở Q2 (đã đóng)**: coi là code thừa, không đưa vào BD.

## 4. Given-When-Then bổ sung ở mức màn (không trùng `US-A1-02`)

- **Cho** một bài toán rơi vào ngoại lệ lược đồ kiểu (kiểu dữ liệu vượt lược đồ độc lập ngôn ngữ của F3-01),
  **Khi** tôi xem bài đó trong danh sách, **Thì** nhãn mô hình cho biết bài này **chỉ còn Standard I/O**
  (Bọc hàm bị ẩn) — đây là ngoại lệ duy nhất còn mang tính cố định theo bài toán, không phải lựa chọn của
  người học [SoT: 01-rd/req/harness.md — F3-13 ngoại lệ R2].
- **Cho** một bài toán không rơi vào ngoại lệ trên (đa số các bài), **Khi** tôi xem bài đó trong danh sách,
  **Thì** nhãn mô hình cần thể hiện "cả hai mô hình khả dụng", không còn là một giá trị cố định `function`
  hoặc `stdio` — mô hình thực sự chỉ được chọn khi vào màn `problem_detail`, mỗi lượt làm một lần
  [SoT: 01-rd/req/harness.md — F3-13 sau `DEC-2026-0824-dual-submission-model-per-problem`;
  đã chốt ở Câu hỏi mở Q3 (đã đóng), prototype `Ngân hàng bài toán.dc.html` chưa dựng lại nhãn này — để lúc
  build FE Next.js thật].
- **Cho** tôi đã từng yêu cầu Solution Review cho một bài đã `Accepted` (F5-01), **Khi** tôi xem lại danh
  sách, **Thì** bài đó hiện thêm nhãn "đã có Solution Review" để tôi biết có thể mở lại báo cáo cũ mà không
  cần phân tích lại [SoT: 09-layoutBase/Ngân hàng bài toán.dc.html:175-177].

## 5. Câu hỏi mở

| # | Câu hỏi | Vì sao chưa trả lời được | Đề xuất | Chủ sở hữu |
| :-: | :--- | :--- | :--- | :--- |
| Q1 | ~~`problem-bank.md` — F2-12 mô tả bài giao theo lớp "lồng thành một nhóm riêng"...~~ **ĐÃ CHỐT (phiên 2026-08-25, chốt theo RD):** giữ nguyên dạng khối tổng hợp như prototype (đơn giản hơn khi có nhiều lớp/nhiều bài được giao cùng lúc); đã sửa câu chữ `01-rd/req/problem-bank.md` mục F2-12 cho khớp thực tế (đổi "lồng thành một nhóm riêng" thành "hiển thị thành một khối riêng ngay trong cùng trang danh sách bài toán"). | — | Đã sửa `problem-bank.md`, không cần hành động thêm ở màn này. | Đã đóng |
| Q2 | ~~Nhánh `isSolve`...~~ **ĐÃ CHỐT (phiên 2026-08-25, chốt theo RD):** coi là code thừa của một hướng thiết kế cũ (danh sách + giải bài trong cùng trang), không đưa vào BD. **Không dọn ở `09-layoutBase/Ngân hàng bài toán.dc.html`** — prototype chỉ tham khảo, khi dựng FE Next.js thật sẽ không mang theo nhánh này. | — | Không đưa vào BD; không cần dọn prototype (sẽ viết lại từ đầu ở FE thật). | Đã đóng |
| Q3 | ~~(Mới, phát hiện khi rà Phase 1...) Nhãn "mô hình nộp bài" trong bảng danh sách...~~ **ĐÃ CHỐT (phiên 2026-08-25, chốt theo RD):** đổi nhãn thành "Cả hai" mặc định cho mọi bài có cả hai mô hình song song; chỉ hiện "Chỉ Standard I/O" cho các bài rơi vào ngoại lệ lược đồ kiểu (R2, `harness.md` — F3-13) — giữ được thông tin hữu ích (biết trước bài nào bị giới hạn) mà không gây hiểu nhầm là chọn cố định. **Chưa dựng vào prototype** (dữ liệu mẫu vẫn gán `mode` cố định 1-trong-2, dòng 501-524) — để lúc build FE Next.js thật. | — | Đã chốt quyết định, chưa dựng prototype. | Đã đóng |

## 6. Ngoài phạm vi file này

- Bảng màu, spacing, component cụ thể, breakpoint responsive — thuộc BD (`02-bd/screens/users/problem_list.md`,
  chưa viết).
- Hợp đồng API (lọc, phân trang, sắp xếp) — thuộc DD (`03-dd/api/problem-bank.md`, chưa viết).
- Thuật toán tính tỉ lệ Accepted, cách đánh chỉ mục tìm kiếm — thuộc logic của `problem-bank`, không thuộc
  file theo trục màn này.

## 7. Tham chiếu

- `01-rd/req/problem-bank.md` — F2-11, F2-12. `01-rd/req/harness.md` — F3-13.
- `.nexa/control/decision-registry.md` — `DEC-2026-0824-dual-submission-model-per-problem` (ảnh hưởng gián
  tiếp tới cách hiển thị nhãn mô hình ở màn này, xem Câu hỏi mở Q3).
- `01-rd/req/user_stories/a1_student.md` — `US-A1-02`.
- `01-rd/overview/system_survey.md:474` — dòng `problem_list` trong bảng màn mục 7.1.
- `09-layoutBase/Ngân hàng bài toán.dc.html` — prototype.
- `06-plan/nexa-plan/260824-2043-bd-screens-common-first.md` — kế hoạch Phase 1 sinh ra file này.
