# BD — Kiến trúc module `harness` (F3)

> Trạng thái: **hoàn thiện BD 2026-09-12** (module thứ ba theo `06-plan/260912-1055-bd-rollout-order.md`).
> Mục 1-5 giữ nguyên nội dung chốt sơ bộ 2026-08-24 (kiến trúc Strategy/Plugin, hai mô hình song song).
> Mục 6 trở đi là phần bổ sung của lượt BD này: lược đồ kiểu dữ liệu cụ thể (F3-01), chữ ký phương thức
> plugin ở mức khái niệm, chiến lược so khớp cho kiểu phức tạp, nguyên lý ánh xạ lỗi biên dịch. Đây là
> module đắt nhất nếu sai — một lược đồ kiểu sai hỏng cả ba template ngôn ngữ, nên mục 6 được viết dài hơn
> các module khác một cách chủ đích, đúng như bảng "Design decisions this project must make explicitly"
> trong `.claude/skills/bd-generation/SKILL.md` yêu cầu.

## 1. Bối cảnh

`01-rd/req/harness.md` mục F3 khoá cứng đúng ba ngôn ngữ nộp bài: Java, C++, Python
(`01-rd/req/harness.md` — F3-01 tới F3-04). Không mở rộng thêm ngôn ngữ ở tầng yêu cầu — xem quyết định giữ nguyên phạm vi
tại `06-plan/PROTOTYPE_DEBT.md` mục 1.1 (chốt 2026-08-24, chọn Phương án A: bỏ tính năng "Thêm ngôn ngữ"
khỏi giao diện Admin).

## 2. Chủ đích kiến trúc — sơ bộ

Dù phạm vi nghiệp vụ dừng ở ba ngôn ngữ, module sinh mã bọc hàm (`harness`) nên tổ chức theo
**Open-Closed Principle** thông qua **Strategy/Plugin pattern nội bộ**, thay vì rẽ nhánh `if/switch-case`
cứng theo tên ngôn ngữ:

- Mỗi ngôn ngữ (Java, C++, Python) là một plugin độc lập, cùng tuân theo một interface chung (tên gọi và
  chữ ký cụ thể chốt ở DD), tối thiểu bao gồm các trách nhiệm đã liệt kê ở `req.md` F3-01 tới F3-12:
  parse/khai báo kiểu (F3-01), sinh mã đọc input (F3-02), gọi hàm người dùng (F3-03), sinh mã in kết quả
  (F3-04), tiêm mã người dùng (F3-05), đóng gói theo định dạng adapter (F3-06), so khớp kết quả
  (F3-07 tới F3-10), ánh xạ lỗi biên dịch về đúng dòng người dùng (F3-11), che giấu lỗi thuộc mã harness
  (F3-12).
- Một registry nội bộ tra cứu plugin theo mã ngôn ngữ, thay cho `switch(language)` rải rác trong logic F4
  hoặc F5.
- Đây là **quyết định chất lượng kỹ thuật nội bộ của module `harness`**, không phơi ra giao diện Admin,
  không cần bản ghi quyết định (`DEC-`) mới, không đổi phạm vi đã khoá ở mục 1.

## 2b. Cập nhật 2026-08-24 (tiếp) — cả hai mô hình nộp bài luôn song song

`DEC-2026-0824-dual-submission-model-per-problem` sửa lại phạm vi F3-13: trước đây tài liệu này viết theo
khung "Standard I/O là đường lùi hiếm khi dùng" (mục 2 ở trên đếm F3-01 tới **F3-12**, bỏ sót F3-13). Thực
tế đã chốt: **mỗi bài toán (trừ trường hợp kiểu dữ liệu vượt lược đồ) luôn cần cả hai đầu ra codegen** —
mã Bọc hàm (F3-02 tới F3-06 áp dụng như cũ) và khung Standard I/O (đọc `stdin` theo định dạng khai báo ở
F2-03, ghi `stdout`, không cần tiêm mã người dùng qua khuôn vì người học tự viết `main()`).

- **Không đổi chiến lược Strategy/Plugin đã chọn ở mục 2** — mỗi plugin ngôn ngữ giờ có **hai phương thức
  sinh mã** (một cho Bọc hàm, một cho Standard I/O) thay vì một, cùng nằm trong một interface, không cần
  hai interface hay hai registry riêng.
- **So khớp kết quả (F3-07 tới F3-10) và ánh xạ lỗi biên dịch (F3-11, F3-12) áp dụng như nhau cho cả hai
  mô hình** — không có logic riêng theo mô hình ở tầng này.
- **Khối lượng công việc tăng gần gấp đôi** so với ước lượng "đường lùi hiếm khi dùng" trước đó — ghi nhận
  lại ở `01-rd/overview/system_survey.md` mục 5.7.
- **Còn mở, chuyển sang DD:** cơ chế ẩn tuỳ chọn Bọc hàm khi kiểu dữ liệu vượt lược đồ (F3-13 trường hợp
  còn lại) — cần một cờ hoặc kết quả tự kiểm tra ở tầng `problem-bank` khi giảng viên khai báo đặc tả
  (F2-03), báo trước cho A2 biết bài này sẽ chỉ hiện Standard I/O, tránh phát hiện muộn lúc học viên mở bài.

## 3. Không chọn (đã cân nhắc và loại)

- **Admin tự định nghĩa ngôn ngữ mới qua UI** (template engine + type mapping JSON + regex lỗi biên dịch tự
  nhập): rủi ro bảo mật — cho phép định nghĩa mã bọc hàm tuỳ ý ngoài kiểm soát của sandbox go-judge; đồng
  thời đòi Admin (actor A3) phải hiểu sâu lập trình, không khớp vai trò A3 trong RD.
- **Mở toàn bộ ngôn ngữ có sẵn của Judge0** (60+ ngôn ngữ): dựa trên giả định Judge0 là engine chính, trong
  khi `DEC-2026-0823-go-judge-default-engine` đã chốt go-judge là engine mặc định, Judge0 chỉ còn là
  adapter thay thế hợp lệ.

## 4. Lược đồ kiểu dữ liệu độc lập ngôn ngữ (F3-01) — quyết định trung tâm của BD này

### 4.1. Vị trí lưu trữ và sở hữu

**Chốt:** lược đồ kiểu (tập hằng số `TypeKind` + cấu trúc lồng nhau JSON) được định nghĩa **một lần** trong
`algoprep-common` như một shared kernel thuần dữ liệu (enum + record/DTO bất biến, không chứa logic sinh
mã) — khớp đề xuất đã nêu ở `02-bd/architecture/problem-bank.md` mục 4 `[SoT: Suy luận]`. `harness` là chủ
sở hữu **ngữ nghĩa** của lược đồ (thêm/bớt `TypeKind` là quyết định của module này), `problem-bank` chỉ là
bên tiêu thụ để validate chữ ký hàm lúc lưu (`function_wrapper_supported`, mục 5 file `architecture/problem-
bank.md`). Không chọn phương án "outbound port validate-only sang `harness`" vì lược đồ kiểu là **dữ liệu
bất biến dùng chung** (giống một enum toàn cục), gọi qua port cho mỗi lần validate là chi phí không cần
thiết cho một kiểm tra thuần cấu trúc, không có state, không có I/O.

`[SoT: Suy luận]` — đây là câu trả lời dứt khoát cho câu hỏi mở "shared kernel hay port" mà cả hai BD
(`harness`, `problem-bank`) cùng để ngỏ. Ghi nhận tại đây làm nguồn tham chiếu chính; `problem-bank` cập
nhật lại mục 4/7 của nó khi đọc file này.

### 4.2. Cấu trúc lược đồ — dạng AST tối giản

Mỗi kiểu là một node JSON có trường `kind` (bắt buộc) và các trường phụ tuỳ `kind`:

| `kind` | Trường phụ | Ví dụ JSON |
| :--- | :--- | :--- |
| `INT` / `LONG` / `DOUBLE` / `BOOLEAN` / `CHAR` | không có | `{"kind":"INT"}` |
| `STRING` | không có | `{"kind":"STRING"}` |
| `ARRAY` | `of` (kiểu phần tử), `dimensions` (số chiều, ≥1) | `{"kind":"ARRAY","of":{"kind":"INT"},"dimensions":2}` — tương ứng `int[][]`/`vector<vector<int>>`/`List[List[int]]` |
| `LIST` | `of` (kiểu phần tử, cho phép lồng `LIST` bên trong `LIST`) | `{"kind":"LIST","of":{"kind":"LIST","of":{"kind":"INT"}}}` — `List<List<Integer>>` |
| `LINKED_LIST` | `of` (kiểu giá trị mỗi node) | `{"kind":"LINKED_LIST","of":{"kind":"INT"}}` |
| `BINARY_TREE` | `of` (kiểu giá trị mỗi node) | `{"kind":"BINARY_TREE","of":{"kind":"INT"}}` |

Khác biệt `ARRAY` (mảng nhiều chiều, kích thước tĩnh theo ngôn ngữ đích — `int[][]`, `std::vector<std::
vector<int>>`, khai báo qua kiểu built-in) và `LIST` (ánh xạ sang kiểu danh sách động của ngôn ngữ — `List<
Integer>`/`ArrayList`, `std::vector` khi không cố ý dùng mảng tĩnh, `list`) — RD dùng cả hai thuật ngữ
"mảng nhiều chiều" và "danh sách lồng nhau" như hai khái niệm tách biệt
(`01-rd/req/harness.md:11`), BD giữ nguyên hai `kind` khác nhau thay vì gộp làm một, vì mã sinh ra khác
nhau đáng kể giữa ba ngôn ngữ (ví dụ Java: mảng nguyên thuỷ `int[]` so với `List<Integer>` có boxing).

`ARRAY`/`LIST` với `dimensions`/`of` lồng đủ sâu để biểu diễn "mảng nhiều chiều" và "danh sách lồng nhau"
(F3-01) mà không cần thêm `kind` riêng cho từng độ sâu.

### 4.3. Từng kiểu: khai báo, sinh mã đọc/in, rủi ro

#### a) Kiểu nguyên thuỷ (`INT`, `LONG`, `DOUBLE`, `BOOLEAN`, `CHAR`, `STRING`)
- **Khai báo trong `problem_spec`**: một node `{"kind":"..."}`không tham số, gắn trực tiếp làm `return_type`
  hoặc phần tử `parameters[].type` (`02-bd/database/problem-bank.md` mục 1.5).
- **Sinh mã đọc input** (khái niệm, không phải code thật): mỗi ngôn ngữ đọc một token/dòng theo đúng kiểu
  built-in tương ứng (Java `Scanner.nextInt()`/`nextLong()`/`nextDouble()`/`nextBoolean()` hoặc đọc dòng rồi
  parse; C++ `std::cin >>`; Python `input()` rồi ép kiểu qua `int()`/`float()`). `STRING` đọc nguyên một
  dòng (không tách theo khoảng trắng) để cho phép chuỗi có dấu cách.
- **Sinh mã in output**: in trực tiếp giá trị trả về qua toán tử/print mặc định của ngôn ngữ; `DOUBLE` in
  theo định dạng cố định số chữ số thập phân (chốt số chữ số cụ thể ở DD, liên quan chiến lược `EPSILON`
  mục 6).
- **Rủi ro/biên**: `CHAR` trong Python không có kiểu ký tự riêng — biểu diễn bằng `str` độ dài 1, template
  Python phải tự kiểm tra độ dài khi đọc input để bắt lỗi harness sớm (không phải lỗi của người học) thay
  vì để lỗi runtime khó hiểu lan tới người học. `BOOLEAN` cần thống nhất token input là gì (`true`/`false`
  chữ thường hay `0`/`1`) — chốt ở DD cùng lúc với `stdin_format_md` của `problem-bank`.

#### b) `ARRAY` (mảng nhiều chiều, `dimensions` ≥ 1)
- **Khai báo**: `{"kind":"ARRAY","of":<kiểu phần tử>,"dimensions":n}`. `dimensions=1` là mảng một chiều
  thông thường (`int[]`); `dimensions=2` là mảng hai chiều, v.v.
- **Sinh mã đọc input**: định dạng input chuẩn hoá theo quy ước "kích thước trước, dữ liệu sau" cho mỗi
  chiều (ví dụ dòng đầu `n` rồi `n` dòng tiếp mỗi dòng `m` số cách nhau bằng khoảng trắng cho mảng 2 chiều
  `n×m`) — quy ước cụ thể ở mức ký tự phân tách chốt ở DD, nhưng nguyên tắc BD khoá: **kích thước luôn khai
  tường minh trong input, không suy luận từ ký tự kết thúc dòng**, để cả ba ngôn ngữ đọc theo cùng một thuật
  toán duyệt (đọc kích thước → cấp phát mảng đúng kích thước → đọc phần tử).
- **Sinh mã in output**: in theo đúng cấu trúc lồng, phân tách phần tử bằng khoảng trắng và các chiều bằng
  xuống dòng — khớp chiến lược so khớp `TRIMMED` (mục 6) để tránh sai lệch do khoảng trắng thừa/thiếu.
- **Rủi ro/biên**: mảng rỗng (`n=0`) và mảng răng cưa (jagged array, các hàng độ dài khác nhau) — lược đồ
  `ARRAY` này giả định **mảng chữ nhật đều** (rectangular), không hỗ trợ jagged. Bài cần mảng răng cưa
  không biểu diễn được bằng `ARRAY` mà phải dùng `LIST` lồng `LIST` (mục c) — đây chính là ví dụ cụ thể của
  ranh giới lược đồ ở mục 4.5.

#### c) `LIST` (danh sách lồng nhau, kể cả jagged)
- **Khai báo**: `{"kind":"LIST","of":<kiểu phần tử>}`, lồng bao nhiêu lớp tuỳ ý bằng cách đặt `LIST` khác
  vào `of`.
- **Sinh mã đọc input**: đệ quy theo độ sâu lồng — đọc số phần tử của lớp ngoài, với mỗi phần tử đệ quy đọc
  tiếp theo `of`. Không giả định các danh sách con cùng độ dài (khác `ARRAY`), nên input mỗi danh sách con
  tự khai kích thước riêng — đây là điểm khác biệt cách đọc so với `ARRAY` dù cú pháp input trông tương tự.
- **Sinh mã in output**: in đệ quy theo cùng cấu trúc, dùng ký hiệu ngoặc vuông lồng nhau kiểu
  biểu diễn danh sách (ví dụ `[[1,2],[3],[4,5,6]]`) làm định dạng chuẩn hoá chung cho cả ba ngôn ngữ — chọn
  định dạng này (thay vì định dạng theo dòng như `ARRAY`) vì `LIST` lồng có thể không đều kích thước, biểu
  diễn theo dòng dễ mơ hồ khi các danh sách con độ dài khác nhau.
- **Rủi ro/biên**: danh sách rỗng ở bất kỳ lớp nào (`[]`), phân biệt danh sách rỗng với "không có phần tử vì
  lỗi parse" — template phải in tường minh `[]` chứ không in chuỗi rỗng.

#### d) `LINKED_LIST`
- **Khai báo**: `{"kind":"LINKED_LIST","of":<kiểu giá trị node>}`.
- **Sinh mã đọc input**: đọc một danh sách phẳng giá trị theo thứ tự (giống `LIST` một lớp), sau đó code
  sinh ra tự dựng chuỗi liên kết bằng cách khởi tạo node theo thứ tự và nối `next` tuần tự — người học không
  tự parse cấu trúc liên kết, chỉ nhận một tham số kiểu `ListNode` (Java)/`ListNode*` (C++)/`ListNode`
  (Python) đã dựng sẵn, đúng tinh thần mô hình Bọc hàm (F3-03: gọi hàm người dùng, không bắt người dùng viết
  code đọc input).
- **Sinh mã in output**: duyệt chuỗi liên kết trả về từ hàm người dùng, in ra dưới dạng danh sách phẳng cùng
  định dạng với input (đối xứng đọc/ghi).
- **Rủi ro/biên**: **không cần biểu diễn chu trình** — đây là input/output của bài toán thuật toán, không
  phải cấu trúc runtime tuỳ ý; RD/domain-registry không yêu cầu chu trình và không có bài toán chuẩn nào
  (theo phạm vi capstone) cần input là danh sách liên kết có chu trình, nên harness không đầu tư cơ chế biểu
  diễn chu trình cho input `[SoT: Suy luận]`. Danh sách rỗng biểu diễn bằng số phần tử `0`.

#### e) `BINARY_TREE`
- **Khai báo**: `{"kind":"BINARY_TREE","of":<kiểu giá trị node>}`.
- **Sinh mã đọc input**: **chốt định dạng tuần tự hoá theo thứ tự mức (level-order) kèm ký hiệu null
  tường minh** — quy ước phổ biến của LeetCode: liệt kê giá trị node theo BFS, dùng token cố định (đề xuất
  `#` hoặc `null` tuỳ ngôn ngữ hiển thị, chốt ký hiệu cụ thể ở DD) đánh dấu vị trí không có node con, và
  **không liệt kê tiếp con của một node null** (đúng ngữ nghĩa LeetCode, tránh cây bùng nổ kích thước khi
  cây lệch — ví dụ cây chỉ có nhánh trái sâu 20 tầng không cần liệt kê 2^20 vị trí null bên phải). Code sinh
  ra dựng cây bằng hàng đợi BFS y hệt thuật toán giải mã ngược của định dạng này.
- **Sinh mã in output**: duyệt BFS cây trả về, in theo cùng quy ước null-marker, cắt bỏ các node null ở
  "đuôi" dãy (trailing) để output gọn — quy tắc cắt đuôi cụ thể chốt ở DD vì ảnh hưởng trực tiếp tới so khớp
  chuỗi.
- **Rủi ro/biên đặc trưng nhất trong toàn bộ lược đồ**: (1) cây rỗng (`root = null`) biểu diễn bằng dãy chỉ
  chứa một token null; (2) hai cây có cùng tập giá trị nhưng khác hình dạng phải in ra hai chuỗi khác nhau
  — nếu không, so khớp `EXACT`/`TRIMMED` (mục 6) sẽ chấm sai (coi hai cây khác hình dạng là bằng nhau); (3)
  quy tắc "không liệt kê con của null" phải áp dụng nhất quán ở cả sinh input mẫu (`problem-bank`/AI sinh
  testcase F2-14) và sinh output (`harness`) — lệch quy tắc giữa hai module gây testcase không parse được;
  đây là rủi ro tích hợp liên module cần thống nhất tài liệu định dạng dùng chung, không chỉ là chi tiết nội
  bộ `harness`.

### 4.4. Kiểu tổ hợp không cần `kind` riêng

Tham số/kết quả nhiều giá trị (ví dụ hàm trả về `(int, int)`) biểu diễn bằng `parameters`/nhiều trường
`return_type` là một `LIST` tại tầng ngoài cùng nếu đồng kiểu, hoặc — nếu khác kiểu — RD hiện không yêu cầu
tuple dị kiểu làm giá trị trả về (không thấy nêu ở `01-rd/req/harness.md`), nên BD **không** thêm `kind:
TUPLE` ở lần chốt này; nếu phát sinh nhu cầu, đó là thay đổi lược đồ kiểu và bắt buộc một bản ghi quyết định
mới theo đúng quy tắc ở `.claude/skills/bd-generation/SKILL.md` mục "Design decisions...".

### 4.5. Khi kiểu dữ liệu vượt lược đồ — ai phát hiện, khi nào

**Chốt: phát hiện tại thời điểm A2/A3 khai chữ ký hàm ở `problem_authoring`, không phải lúc học viên mở bài
hay lúc harness cố sinh mã lúc chấm.** Cơ chế: khi lưu `function_signatures`, tầng application của
`problem-bank` validate cấu trúc JSONB của `return_type`/từng `parameters[].type` đệ quy theo đúng tập
`kind` hợp lệ ở mục 4.2 (đọc từ shared kernel `algoprep-common`, mục 4.1); nếu bất kỳ node nào có `kind`
không thuộc tập trên, hoặc `ARRAY`/`LIST`/`LINKED_LIST`/`BINARY_TREE` thiếu trường `of` bắt buộc, request
lưu đặc tả bị từ chối theo cấu trúc dữ liệu — **nhưng đây không phải lỗi cứng chặn A2**, mà tự động đặt
`function_wrapper_supported = false` trên `problems` (`02-bd/database/problem-bank.md` mục 1.1) và báo cho
A2 ngay trong màn `problem_authoring` rằng bài này sẽ chỉ hiển thị Standard I/O — khớp đúng quyết định đã
chốt ở `02-bd/architecture/problem-bank.md` mục 5.

Lý do chọn "phát hiện lúc khai spec" thay vì "lúc harness cố sinh mã": (1) tránh phát hiện muộn khi học viên
đã mở bài (đúng tinh thần F3-13 sửa đổi — RD nhấn mạnh "báo trước cho A2... tránh phát hiện muộn"); (2)
`harness` không cần chạy thử một lượt sinh mã "dò lỗi" tốn tài nguyên — việc kiểm tra cấu trúc JSONB khớp
`kind` hợp lệ là một phép kiểm tra thuần cấu trúc (structural validation), không cần thực thi codegen thật
để biết trước có sinh được hay không, vì lược đồ mục 4.2-4.3 là **đóng** (closed set of kinds) — nếu cấu
trúc khớp tập `kind` hợp lệ, harness luôn sinh được mã cho nó theo định nghĩa của lược đồ.

`harness` vẫn giữ một lớp kiểm tra cấu trúc riêng ở tầng của chính nó (không tin tưởng mù `problem-bank` đã
validate đúng, phòng trường hợp dữ liệu cũ hoặc bug ở phía kia) — nhưng đây là **phòng thủ theo chiều sâu**
(defense in depth), không phải điểm phát hiện chính thức đầu tiên.

## 5. Interface plugin — chốt ở mức khái niệm (chữ ký thật chốt ở DD)

Mỗi plugin ngôn ngữ (`JavaHarnessPlugin`, `CppHarnessPlugin`, `PythonHarnessPlugin` — tên đề xuất, DD chốt
tên thật) triển khai tối thiểu các trách nhiệm sau, tương ứng một phương thức khái niệm mỗi trách nhiệm
(không phải chữ ký Java thật):

| Trách nhiệm | Input (khái niệm) | Output (khái niệm) | Mã RD |
| :--- | :--- | :--- | :--- |
| Sinh khung Bọc hàm | `function_signature` (chữ ký hàm đã validate theo lược đồ mục 4), mã nguồn người học (đoạn thân hàm) | Mã nguồn hoàn chỉnh của một file (đọc input theo lược đồ → gọi hàm người học → in output theo lược đồ) | F3-02, F3-03, F3-04, F3-05 |
| Sinh khung Standard I/O | `stdin_format_md`/`stdout_format_md` (mô tả tự do), mã nguồn người học (đã có `main()` riêng) | Mã nguồn hoàn chỉnh — chủ yếu là ghép mã người học vào khung biên dịch tối thiểu, **không sinh logic đọc/ghi** (người học tự viết) | F3-13 |
| Đóng gói theo định dạng adapter | Mã nguồn hoàn chỉnh (một trong hai trên) | Payload đúng hình dạng `JudgeExecutionPort` yêu cầu (mặc định go-judge: `cmd[]`/`copyIn`-style, không phải Base64 — `DEC-2026-0823-go-judge-default-engine` điểm (4)) | F3-06 |
| Ánh xạ lỗi biên dịch | Output thô của compiler/interpreter (`stderr`), bảng offset dòng (số dòng mã khung chèn *trước* thân hàm người học) | Danh sách lỗi đã ánh xạ lại đúng số dòng trong mã người học, lỗi thuộc mã khung bị lọc bỏ hoàn toàn | F3-11, F3-12 |
| So khớp kết quả | Output chuẩn hoá của chương trình người học, expected-output, `matching_strategy` (+ `epsilon_value` nếu có) | `MATCH` / `NO_MATCH` (không phải diff chi tiết — diff không lộ ra Hidden testcase) | F3-07 → F3-10 |

Việc "đóng gói theo định dạng adapter" (hàng 3) là trách nhiệm **chung** không phụ thuộc ngôn ngữ (không
thuộc plugin theo ngôn ngữ) — BD ghi nhận ở đây vì domain-registry liệt kê nó trong scope `harness`, nhưng
DD có thể đặt nó ở một collaborator dùng chung (ví dụ `GoJudgePayloadBuilder`) thay vì lặp lại trong cả ba
plugin — quyết định tổ chức code cụ thể chốt ở `03-dd/logic/harness.md`.

## 6. Chiến lược so khớp kết quả (F3-07 → F3-10) — áp dụng cho kiểu phức tạp

Bốn chiến lược RD liệt kê (`EXACT`, `TRIMMED`, `EPSILON`, `UNORDERED_SET`) áp dụng trên **chuỗi output đã
chuẩn hoá theo định dạng in ấn của từng `kind`** (mục 4.3), không so khớp trên chuỗi thô byte-by-byte khi
kiểu dữ liệu có cấu trúc:

- **`EXACT`**: so sánh chuỗi output đã chuẩn hoá tuyệt đối bằng nhau. Với `BINARY_TREE`/`LINKED_LIST`/
  `LIST`, "chuỗi đã chuẩn hoá" là chuỗi đã qua bước in theo định dạng chuẩn ở mục 4.3 (ví dụ dạng
  `[[1,2],[3]]`) — hai cấu trúc **khác nhau về hình dạng nhưng cùng giá trị phẳng** vẫn bị coi là khác nhau
  đúng theo kỳ vọng (ví dụ cây nhị phân khác hình dạng, mục 4.3.e rủi ro (2)).
- **`TRIMMED`** (chuẩn hoá khoảng trắng): cắt khoảng trắng thừa ở đầu/cuối dòng và gộp nhiều khoảng trắng
  liên tiếp trước khi so `EXACT` — áp dụng tốt cho `ARRAY`/nguyên thuỷ (định dạng theo dòng), **ít ý nghĩa**
  với `LIST`/`BINARY_TREE` (định dạng ngoặc vuông không có khoảng trắng thừa để chuẩn hoá) — với hai kiểu
  này `TRIMMED` cho kết quả tương đương `EXACT`.
- **`EPSILON`** (số thực): không so sánh chuỗi mà **parse lại thành số** rồi so `|actual - expected| ≤
  epsilon_value`. Với cấu trúc chứa `DOUBLE` lồng bên trong (`LIST<DOUBLE>`, `ARRAY` phần tử `DOUBLE`),
  harness phải so khớp **theo từng phần tử tương ứng vị trí** (duyệt song song hai cấu trúc, so từng lá số
  thực bằng epsilon, so các node cấu trúc bao ngoài bằng khớp hình dạng chính xác) — không parse toàn chuỗi
  thành một số duy nhất. Đây là điểm phức tạp nhất trong so khớp, chốt thuật toán duyệt song song cụ thể ở
  DD.
- **`UNORDERED_SET`** (tập hợp không xét thứ tự): parse chuỗi output thành tập hợp phần tử rồi so sánh tập
  hợp (không quan tâm thứ tự) — chỉ có ý nghĩa với `LIST`/`ARRAY` một chiều chứa kiểu so sánh được bằng
  `equals` (nguyên thuỷ, `STRING`); **không định nghĩa cho `BINARY_TREE`/`LINKED_LIST`** vì khái niệm "không
  xét thứ tự" không có ngữ nghĩa rõ ràng cho cấu trúc cây/danh sách liên kết — nếu `problem-bank` khai
  `matching_strategy = UNORDERED_SET` cho một bài có kiểu trả về `BINARY_TREE`/`LINKED_LIST`, đây là tổ hợp
  không hợp lệ mà `problem-bank` nên chặn lúc lưu spec (bổ sung một quy tắc validate liên trường, ghi lại ở
  đây để `problem-bank` biết bổ sung — chốt ở DD của cả hai module).

## 7. Ánh xạ lỗi biên dịch về đúng dòng người dùng (F3-11, F3-12)

**Nguyên lý chung, áp dụng như nhau cho cả ba compiler/interpreter (`javac`, `g++`, `python -c`/AST-parse):**
mã khung (harness) luôn được sinh sao cho thân mã người học được chèn vào **một vùng liền mạch, có vị trí
dòng bắt đầu đã biết trước** (biến đếm `userCodeStartLine`, tính lúc sinh mã, không phải hằng số cố định vì
độ dài phần khung phía trước có thể thay đổi theo chữ ký hàm). Khi compiler trả lỗi kèm số dòng tuyệt đối
trong file đã ghép, harness:

1. Parse output lỗi của từng compiler theo định dạng riêng của nó (mỗi compiler một định dạng thông báo lỗi
   khác nhau — đây là phần thực sự khác biệt giữa ba plugin, không có nguyên lý chung ở mức cú pháp).
2. Trừ đi `userCodeStartLine - 1` để ra số dòng **tương đối trong mã người học**.
3. Nếu số dòng lỗi **nhỏ hơn** `userCodeStartLine` (lỗi nằm trong phần khung do harness sinh, không phải
   lỗi người học) → **che giấu hoàn toàn**, không trả về cho học viên (F3-12); thay vào đó ghi log nội bộ
   mức `ERROR` (đây là dấu hiệu bug của chính harness cần đội phát triển biết, không phải lỗi của người học)
   và trả về học viên một thông báo chung chung dạng "Lỗi hệ thống khi biên dịch, vui lòng thử lại hoặc báo
   quản trị viên" — không bao giờ lộ nguyên văn stderr chứa mã khung.
4. Nếu số dòng lỗi nằm trong vùng mã người học → trả về nguyên văn thông điệp lỗi của compiler (giữ nguyên
   text gốc, vì đây là thông tin hữu ích cho người học sửa lỗi), chỉ thay số dòng tuyệt đối bằng số dòng đã
   quy đổi ở bước 2.
5. Với mô hình Standard I/O (không có khung Bọc hàm bao quanh, chỉ ghép tối thiểu), `userCodeStartLine`
   thường là `1` hoặc rất nhỏ (chỉ có thể có vài dòng include/import bắt buộc phía trên) — nguyên lý ở trên
   áp dụng y hệt, không cần logic riêng theo mô hình (khớp khẳng định đã có ở mục 2b).

`[SoT: Suy luận]` — bước 1 (parse định dạng lỗi riêng từng compiler, ví dụ regex khớp
`Foo.java:12: error:` của `javac`, `foo.cpp:12:5: error:` của `g++`, `File "foo.py", line 12` của Python) là
chi tiết triển khai cụ thể theo compiler, chốt regex/parser thật ở DD — BD chỉ khoá **nguyên lý dịch số
dòng** (bước 2-4), đây là phần bất biến không đổi theo compiler.

## 8. Việc còn mở — chuyển sang DD

- Tên và chữ ký cụ thể của interface plugin, cơ chế đăng ký registry, vị trí đặt code (Maven module nào)
  — viết ở `03-dd/logic/harness.md` khi vào giai đoạn DD của module `harness`.
- Test golden-file so khớp mã sinh ra cho cả ba ngôn ngữ đã có yêu cầu ở `01-rd/req/nfr.md` (mục E) — DD cần chỉ rõ cách
  tổ chức test theo từng plugin.
- **Chốt 2026-09-13 (chủ dự án xác nhận dùng đề xuất mặc định của BD):** null-marker cho `BINARY_TREE` =
  `#`; token `BOOLEAN` = `true`/`false` (không dùng `0`/`1`); mảng nhiều chiều phân tách **mỗi chiều một
  dòng** (newline-separated), không dùng ký tự phân tách trong cùng dòng. Áp dụng khi viết
  `stdin_format_md`/`stdout_format_md` mẫu ở `problem-bank` và ví dụ hiển thị ở `problem_authoring` tab
  "Đặc tả". Xem `07-review/bd_open_questions_260913.md` mục 2 câu #8.
- Quy tắc validate liên trường "`UNORDERED_SET` không hợp lệ với `BINARY_TREE`/`LINKED_LIST`" (mục 6) — cần
  thêm ở tầng application của `problem-bank` khi lưu `problem_specs`.
- Định dạng regex/parser lỗi biên dịch cụ thể cho từng compiler (mục 7 bước 1).
- Danh sách `TypeKind` ở mục 4.2 là **đóng** cho lần chốt này; nếu capstone cần mở rộng (ví dụ `MATRIX`
  riêng biệt khỏi `ARRAY`, hay đồ thị dạng adjacency list) — đó là thay đổi lược đồ kiểu, bắt buộc một bản
  ghi quyết định mới trước khi sửa file này (theo đúng cảnh báo ở `.claude/skills/bd-generation/SKILL.md`).

## 9. Tham chiếu

- `01-rd/req/harness.md` — đặc tả F3-01 tới F3-13 (cập nhật 2026-08-24, xem mục 2b).
- `06-plan/PROTOTYPE_DEBT.md` mục 1.1 — quyết định giữ khoá 3 ngôn ngữ, Phương án A.
- `.nexa/control/decision-registry.md` — `DEC-2026-0823-go-judge-default-engine`,
  `DEC-2026-0824-dual-submission-model-per-problem`.
- `01-rd/screens/users/problem_detail.md` — RD theo trục màn phát hiện ra sai lệch dẫn tới quyết định ở mục 2b.
- `02-bd/architecture/problem-bank.md` mục 4, 5 — phía cung cấp đặc tả hàm và cờ `function_wrapper_supported`
  mà mục 4.1, 4.5 file này trả lời dứt khoát.
- `.nexa/control/dependency-map.md` mục 2 (harness = pure computation, chỉ Postgres cho template/lược đồ
  kiểu), mục "Open items" (câu hỏi "harness có cần schema riêng hay không" — trả lời ở
  `02-bd/database/harness.md`).
