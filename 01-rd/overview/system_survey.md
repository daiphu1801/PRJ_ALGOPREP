# Báo Cáo Khảo Sát Hệ Thống (System Survey Document)
## Dự án: AlgoPrep — Nền tảng luyện tập thuật toán lập trình và ôn tập phỏng vấn tích hợp AI

| Thuộc tính | Giá trị |
| :--- | :--- |
| Mã tài liệu | `01-rd/overview/system_survey.md` |
| Giai đoạn | Requirements Definition (RD) |
| Ngày cập nhật | 2026-08-20 |
| Nguồn cấp trên | `README.md` (bản khảo sát gọn, tầng P2 của thang SoT) |

**Quan hệ với `README.md`.** `README.md` là bản khảo sát **gọn** — nó chốt phạm vi. Tài liệu này là bản
khảo sát **chi tiết**: cùng phạm vi đó nhưng bung tới mức **từng chức năng có mã số** và **từng màn hình**,
để `02-bd/` có sẵn danh sách đầu việc mà phân tích theo screen và theo feature.

Khi hai bên lệch: `README.md` thắng về phạm vi (cái gì làm, cái gì không); tài liệu này thắng về độ chi tiết
(chức năng gồm những bước nào). Chi tiết nào không suy ra được từ `README.md` thì đánh dấu `[SoT: Suy luận]`
— đó là đề xuất của phân tích, chưa phải điều đã chốt.

**Tài liệu này không nói về công nghệ và kiến trúc.** Xem `01-rd/overview/overview.md` (nền tảng lý thuyết),
`01-rd/system/backend_architecture.md`, `01-rd/system/frontend_architecture.md`.

---

## 1. Giới thiệu và mục đích khảo sát

### 1.1. Bối cảnh

Sinh viên ngành công nghệ thông tin và người đi làm chuyển ngành đều phải qua một cửa giống nhau khi xin việc:
**vòng phỏng vấn kỹ thuật**, nơi ứng viên vừa phải giải được bài toán thuật toán, vừa phải **giải trình được
lời giải của mình** trước người phỏng vấn — nói rõ vì sao chọn cấu trúc dữ liệu đó, độ phức tạp là bao nhiêu,
sai ở đâu khi dữ liệu lớn hơn.

Các nền tảng luyện tập hiện có giải quyết tốt nửa đầu (chấm đúng/sai tự động) và bỏ trống nửa sau. Người học
tự luyện xong một trăm bài vẫn có thể trượt phỏng vấn vì chưa từng phải nói ra suy nghĩ của mình cho ai nghe.

### 1.2. Mục đích của khảo sát

1. Định vị đề tài giữa các nền tảng thương mại và các judge mã nguồn mở: cái gì đã có, cái gì còn trống
   (mục 2).
2. Hệ thống hoá toàn bộ chức năng dự tính thành **bảng chức năng có mã số** để truy vết được sang BD, DD,
   tiêu chí nghiệm thu và test (mục 5).
3. Khảo sát nhu cầu của bốn nhóm đối tượng và ánh xạ sang quyền hạn cụ thể (mục 4).
4. Mô tả các **quy trình nghiệp vụ chính** ở mức bước-đi-bước, làm đầu vào cho sơ đồ tuần tự và sơ đồ hoạt
   động của BD (mục 6).
5. Liệt kê **danh sách màn hình dự kiến** để trục screen của BD có điểm bắt đầu (mục 7).
6. Ghi lại giới hạn phạm vi, rủi ro và tiêu chí thành công (mục 8, 9, 10).

---

## 2. Khảo sát hệ thống tham khảo

### 2.1. Nền tảng thương mại

Bảng 1.1 đối chiếu ba nền tảng luyện tập phổ biến nhất theo bốn tiêu chí quan trọng với đề tài
(`README.md` mục 1.1).

| Tiêu chí | LeetCode | Codeforces | HackerRank |
| :--- | :--- | :--- | :--- |
| Mô hình nộp bài | Bọc hàm | Nhập/xuất chuẩn | Cả hai |
| Phản hồi khi giải đúng | Percentile, gợi ý code (trả phí) | Chỉ trạng thái Accepted | Rubric tĩnh |
| Rèn kỹ năng giải trình | Không | Không | Theo mẫu cố định |
| Triển khai nội bộ | Không | Không | Chỉ qua hợp đồng B2B |

Đọc bảng trên theo hai chiều:

* **Chiều mô hình nộp bài.** LeetCode dùng bọc hàm — người học chỉ viết một hàm, hệ thống lo dữ liệu vào ra.
  Đây là hình thức giống phỏng vấn thật nhất, vì người phỏng vấn cũng chỉ đưa một chữ ký hàm. Codeforces dùng
  nhập/xuất chuẩn — phù hợp thi đấu, nhưng buộc người học viết thêm phần đọc dữ liệu chẳng liên quan tới
  thuật toán.
* **Chiều phản hồi.** Cả ba đều dừng lại ở "đúng hay sai". Phản hồi định tính — bài giải này đã tối ưu chưa,
  còn lỗ hổng gì — hoặc là tính năng trả phí, hoặc là rubric tĩnh viết sẵn cho mọi bài giống nhau.
* **Chiều triển khai.** Không nền tảng nào cho một trường đại học tự dựng bản nội bộ, nên giảng viên không
  gán được bài tập và không xem được tiến độ lớp mình.

### 2.2. Judge mã nguồn mở

Bảng 1.2 đối chiếu bốn hệ mã nguồn mở theo hai câu hỏi: nó giải xong việc gì, và còn thiếu gì so với nhu cầu
của đề tài (`README.md` mục 1.2).

| Hệ thống | Giải quyết được | Còn thiếu |
| :--- | :--- | :--- |
| `isolate` | Cô lập tiến trình ở cấp kernel | Chỉ là thư viện sandbox, không có tầng ứng dụng |
| Judge0 | REST API thực thi mã đa ngôn ngữ | Chỉ nhận stdin/stdout; không hỗ trợ bọc hàm; không có phản hồi định tính; chỉ chạy cgroup v1 |
| go-judge | REST/gRPC thực thi mã đa ngôn ngữ, sandbox tự viết `go-sandbox` | Chỉ nhận stdin/stdout; không hỗ trợ bọc hàm; không có phản hồi định tính |
| DOMjudge / CMS | Judge cho thi đấu ICPC, IOI | Thiết kế cho thi đấu, không cho tự học |
| QDUOJ / HUSTOJ | Judge kèm giao diện cho trường học | Chỉ stdin/stdout, không có tầng đánh giá chuyên sâu |

Năm hệ này xếp thành một cái thang theo mức trọn gói: `isolate` ở dưới cùng (chỉ cô lập tiến trình), Judge0
và go-judge ở giữa (API thực thi — cùng vai trò, khác cách cô lập tiến trình), DOMjudge và HUSTOJ ở trên
(trọn gói cả nghiệp vụ). Đề tài cần **chỗ ở giữa** — có sẵn phần khó nhất là sandbox, nhưng chưa áp đặt mô
hình bài toán.

### 2.3. Vì sao chọn go-judge làm engine mặc định

Theo `README.md` mục 1.2.1 và `DEC-2026-0823-go-judge-default-engine`:

1. **Không xây lại sandbox.** Cô lập tiến trình khi chạy mã không tin cậy là bài toán cấp kernel —
   `go-sandbox` (lõi của go-judge) đã giải quyết bằng namespaces và cgroups, và đang chạy thật trong Hydro
   (một online judge đang hoạt động). Tự làm lại chỉ thêm rủi ro bảo mật mà không tạo đóng góp mới.
2. **Đúng tầng cần tích hợp.** go-judge chỉ là API thực thi, không áp đặt mô hình bài toán hay quy trình
   nghiệp vụ, nên ba tầng đóng góp của đề tài xây trực tiếp lên trên được — khác DOMjudge hay HUSTOJ vốn
   trọn gói và phải gỡ bớt mới dùng được.
3. **Không kén cgroup version.** Hỗ trợ cả cgroup v1 và v2 — Judge0 chỉ hỗ trợ v1, đây là lý do rủi ro R1
   (mục 8) đóng được cho luồng mặc định.
4. **Đóng gói sẵn, self-host được.** Ba ngôn ngữ trong phạm vi (Java, C++, Python) tự cấu hình compiler/
   runtime trong image chạy go-judge; mã nguồn mở, chạy nội bộ bằng Docker.

**Giá phải trả khi chọn go-judge, ghi rõ ngay đây — không đổi so với Judge0:** go-judge chỉ nhận
stdin/stdout. Muốn có mô hình bọc hàm thì phải tự xây bộ sinh mã — chính là F3, và đó là lý do F3 thành
trọng tâm kỹ thuật của đề tài chứ không phải
một tiện ích phụ.

### 2.4. Khoảng trống và định vị đề tài

Ba khoảng trống rút ra từ mục 2.1 và 2.2 (`README.md` mục 1.3):

| Khoảng trống | Hệ quả với người học | Phân hệ lấp |
| :--- | :--- | :--- |
| Không hệ mã nguồn mở nào hỗ trợ mô hình bọc hàm | Luyện tập bằng hình thức khác với hình thức phỏng vấn thật | F3 |
| Không hệ nào có phản hồi định tính sau khi bài nộp được chấp nhận | Biết mình đúng, không biết mình đã tốt chưa | F5.1 |
| Không có môi trường luyện kỹ năng giải trình, triển khai được nội bộ | Lần đầu phải nói ra suy nghĩ là ở phòng phỏng vấn thật | F5.2, F6 |

**Định vị.** Đề tài **không xây lại bộ máy chấm bài**. Đóng góp nằm ở ba tầng phía trên: mô hình bọc hàm đa
ngôn ngữ, phản hồi thời gian thực, và tầng đánh giá bằng AI.

---

## 3. Tổng quan hệ thống

### 3.1. Mô tả một câu

AlgoPrep là nền tảng web để người học luyện thuật toán và rèn kỹ năng phỏng vấn kỹ thuật: chọn bài toán, viết
mã trên trình soạn thảo trực tuyến theo một trong hai hình thức nộp bài, mã được thực thi trong môi trường
cô lập, kết quả từng testcase hiện về theo thời gian thực — và **sau khi đạt Accepted thì mở ra tầng đánh giá
định tính bằng AI** (`README.md` mục 2).

### 3.2. Điểm khác biệt nằm ở đâu

Đường đi của một người học trên các nền tảng hiện có kết thúc ở ô chữ `Accepted`. AlgoPrep coi đó là **điểm
giữa**, không phải điểm cuối, và mở ra hai hướng mà người học tự chọn:

| Hướng | Hình thái | Người học làm gì | Nhận được gì |
| :--- | :--- | :--- | :--- |
| **Phân tích bài giải** (F5.1) | Một lượt, không hội thoại | Bấm yêu cầu phân tích | Báo cáo có cấu trúc: độ phức tạp thực tế, đối chiếu với tối ưu đã biết, lỗ hổng và trường hợp biên, chất lượng mã, câu hỏi mở rộng |
| **Phỏng vấn giả lập 1:1** (F5.2) | Nhiều lượt, hội thoại | Bảo vệ bài giải của chính mình qua ba giai đoạn | Bảng đánh giá năng lực theo rubric kèm nhận xét từng tiêu chí |

Ngoài luồng gắn với bài nộp, hệ thống có **ngân hàng câu hỏi phỏng vấn dạng lý thuyết** (F6) để người học ôn
tập và luyện trả lời độc lập, không cần viết mã.

### 3.3. Ba phần tự xây và một phần tích hợp

| Thành phần | Nguồn | Ghi chú |
| :--- | :--- | :--- |
| Tầng thực thi mã | **Tích hợp** go-judge self-hosted (mặc định, cổng trung lập theo engine) | Dịch vụ ngoài, không phải module trong hệ thống — `DEC-2026-0823-go-judge-default-engine` |
| Bộ sinh mã bọc hàm | **Tự xây** (F3) | Trọng tâm kỹ thuật |
| Tầng điều phối chấm bài | **Tự xây** (F4) | Hàng đợi, timeout sweep, realtime |
| Phân hệ AI | **Tự xây** (F5, F6) | Trên nền Spring AI |

Toàn bộ hệ thống là mã nguồn mở, tự triển khai được bằng Docker Compose (`README.md` mục 2).

---

## 4. Đối tượng của hệ thống

### 4.1. Bốn actor

Theo `README.md` mục 3:

| Actor | Tên đối tượng | Vai trò chính | Nhu cầu cốt lõi |
| :--- | :--- | :--- | :--- |
| **A1** | Sinh viên / người dùng cuối | Tìm và giải bài toán, chạy thử và nộp bài, xem kết quả từng testcase, chọn phân tích bài giải hoặc phỏng vấn giả lập sau khi đạt Accepted, theo dõi tiến độ cá nhân | Biết mình sai ở đâu, và sau khi đúng thì biết mình đã tốt chưa |
| **A2** | Giảng viên (hoặc quản trị viên nội dung) | Soạn đề bài và đặc tả hàm, tải lên bộ testcase, quản lý câu hỏi và giao bài tập theo lớp | Gán được bài tập và thấy được lớp mình đang tắc ở đâu |
| **A3** | Quản trị viên hệ thống | Giám sát cụm judge engine (go-judge) và hàng đợi, kích hoạt chấm lại, quản lý ngôn ngữ và giới hạn tài nguyên, cấu hình prompt và rubric AI | Biết hệ thống có đang khoẻ không, và sửa được khi không |
| **A4** | Hệ thống tự động | Điều phối bài nộp sang judge engine qua `JudgeExecutionPort`, phát hiện bài nộp bị treo (timeout sweep), sinh báo cáo phân tích và điều phối phiên phỏng vấn | (không phải người — là các job và consumer) |

**A4 không phải người dùng.** Nó được liệt kê như một actor vì nó **khởi phát hành động** trong hệ thống mà
không do ai bấm: job timeout sweep tự chạy, consumer tự lấy việc từ hàng đợi. Mọi luồng ở mục 6 có A4 tham gia đều
là luồng phải hoạt động đúng khi không có ai đang mở trình duyệt.

### 4.2. Ánh xạ actor sang vai trò trong hệ thống

| Actor | Vai trò trong code | Ghi chú |
| :--- | :--- | :--- |
| A1 | `STUDENT` | Vai trò mặc định khi đăng ký |
| A2 | `INSTRUCTOR` | Có toàn bộ quyền của `STUDENT`, thêm quyền soạn nội dung và quản lý lớp theo ma trận F1-10 |
| A3 | `ADMIN` | Quyền vận hành hệ thống. **Không** tự động có quyền soạn nội dung — cấp qua ma trận F1-10 nếu cần |
| A4 | (không có vai trò) | Chạy bằng cơ chế nội bộ, không đi qua đăng nhập |

Vai trò lấy từ `README.md` mục 4 (F1). **`ADMIN` và `INSTRUCTOR` là hai vai trò tách riêng, không lồng
nhau** — chốt 2026-08-23 theo `06-plan/PROTOTYPE_DEBT.md` mục 1.2 (thay cho đề xuất suy luận trước đó).
A2 và A3 dùng chung một khu Admin trên giao diện, nhưng quyền với từng nhóm chức năng quản trị/nội dung do
ma trận Role × Function × Action (F1-10 tới F1-14) quyết định ở tầng ứng dụng, không phải bằng cách ẩn/hiện
menu. Ma trận này **không** ảnh hưởng quyền học tập cơ bản của `STUDENT` (F2-F6) — xem `req.md` mục F1,
"Hai lớp quyền".

### 4.3. Ma trận quyền dự kiến

[SoT: Suy luận — bảng này là đề xuất phân tích; chốt chính thức ở `02-bd/security/identity.md`]

| Nhóm chức năng | `STUDENT` | `INSTRUCTOR` | `ADMIN` |
| :--- | :---: | :---: | :---: |
| Xem danh sách và chi tiết bài toán | Có | Có | Có |
| Chạy thử, nộp bài | Có | Có | Có |
| Xem bài nộp của chính mình | Có | Có | Có |
| Xem bài nộp của người khác | Không | Chỉ trong lớp mình phụ trách | Có |
| Dùng phân tích bài giải và phỏng vấn giả lập | Có | Có | Có |
| Soạn đề bài, đặc tả hàm, tải testcase | Không | Có | Không |
| Tạo lớp, giao bài tập | Không | Có | Không |
| Tạo bộ câu hỏi phỏng vấn | Không | Có | Không |
| Giám sát hàng đợi và cụm judge engine | Không | Không | Có |
| Kích hoạt chấm lại | Không | Chỉ bài tập của lớp mình | Có |
| Cấu hình ngôn ngữ và giới hạn tài nguyên | Không | Không | Có |
| Cấu hình prompt và rubric AI | Không | Không | Có |

---

## 5. Bảng chức năng chi tiết

Sáu phân hệ theo `README.md` mục 4, bung thành từng chức năng có mã số. **Mã `Fx-nn` là mã truy vết** — nó
phải xuất hiện lại ở BD, DD, tiêu chí nghiệm thu `04-tdd/` và test. Cột "Actor" ghi ai khởi phát.

Việc chia nhỏ và đánh mã là [SoT: Suy luận]; **nội dung** từng chức năng thì lấy nguyên từ `README.md`.

### 5.1. F1 — Danh tính và phân quyền

| Mã | Chức năng | Actor | Ghi chú |
| :--- | :--- | :--- | :--- |
| F1-01 | Đăng ký tài khoản | A1 | Vai trò mặc định `STUDENT` |
| F1-02 | Đăng nhập, cấp Access Token thời hạn ngắn và Refresh Token trong cookie HTTP-Only | A1 A2 A3 | |
| F1-03 | Làm mới Access Token bằng Refresh Token | A4 | Client tự gọi khi gặp 401 |
| F1-04 | Đăng xuất, vô hiệu hoá Refresh Token | A1 A2 A3 | |
| F1-05 | Phân quyền theo vai trò `STUDENT` / `INSTRUCTOR` / `ADMIN` | A4 | Kiểm ở tầng ứng dụng, không chỉ ở giao diện |
| F1-06 | Trang tiến độ cá nhân: bài đã giải theo chủ đề | A1 | |
| F1-07 | Trang tiến độ cá nhân: tỉ lệ chấp thuận | A1 | Số bài nộp đạt Accepted trên tổng bài nộp |
| F1-08 | Trang tiến độ cá nhân: lịch sử phỏng vấn | A1 | Mở lại được rubric của phiên cũ |
| F1-09 | Quản lý thông tin cá nhân | A1 | [SoT: Suy luận — `README.md` không nêu, nhưng đăng ký mà không sửa được gì là thiếu] |
| F1-10 | Ma trận phân quyền Role × Function × Action, đổi tức thời | A3 | Chốt 2026-08-23, `06-plan/PROTOTYPE_DEBT.md` mục 1.2. Giải quyết A2/A3 dùng chung một khu Admin |
| F1-11 | Function/Action là dữ liệu seed chỉ đọc; Role tạo/sửa/xoá được trên giao diện | A3 | Không xoá được vai trò hệ thống hoặc vai trò đang có người dùng |
| F1-12 | Danh sách chức năng nằm trong phạm vi ma trận (`PROBLEM_AUTHORING`, `TESTCASE_MANAGEMENT`, `CLASS_MANAGEMENT`, `USER_MANAGEMENT`, `JUDGE_QUEUE_MONITOR`, `AI_CONFIG`, `AI_TOKEN_BUDGET`, `SYSTEM_AUDIT_LOG`, `INTERVIEW_BANK_MANAGEMENT`, `PERMISSION_MATRIX`, `REJUDGE_MANAGEMENT`) | A3 | [SoT: Suy luận — danh sách khởi điểm, chốt số lượng chính xác ở BD]. `AI_TOKEN_BUDGET` nay gồm cả F5-21 và F5-25; `INTERVIEW_BANK_MANAGEMENT` nay gồm cả F6-11 và F6-12 |
| F1-13 | Quản lý tài khoản người dùng: đổi vai trò, khoá/mở khoá, reset mật khẩu | A3 | Gác bởi `USER_MANAGEMENT` trong ma trận F1-10 |
| F1-14 | Ghi Nhật ký hệ thống cho mọi thay đổi ma trận phân quyền và mọi thao tác quản trị | A4 | Không có ngoại lệ, kể cả đổi quyền. Chốt 2026-08-24 (mục 2.5): chỉ hành động quản trị của người, không gộp sự kiện hạ tầng — sự kiện hạ tầng xem ở `admin_queue_monitor` (F4-10) |
| F1-15 | Đăng nhập/đăng ký qua OAuth (GitHub, Google) | A1 A2 A3 | Bổ sung `06-plan/PROTOTYPE_DEBT.md` mục 2.2. Trùng email với tài khoản email/mật khẩu → tự động liên kết, không tạo tài khoản thứ hai |
| F1-16 | Tự xoá tài khoản (danger zone) | A1 A2 A3 | Bổ sung mục 2.3. Khoá mềm (`DEACTIVATED`) ngay, ẩn danh hoá thông tin định danh sau khoảng ân hạn; bài nộp/bài giải/phiên phỏng vấn không bị xoá |
| F1-17 | Tự đặt lại mật khẩu bằng mã 6 chữ số gửi qua email (Gmail) | A1 A2 A3 | Bổ sung `01-rd/screens/shared/auth.md` mục 5 câu hỏi mở Q4, chốt 2026-08-24. Mã dùng một lần, có hạn hiệu lực; không tiết lộ email có tồn tại hay không (OWASP) |

### 5.2. F2 — Ngân hàng bài toán và testcase

| Mã | Chức năng | Actor | Ghi chú |
| :--- | :--- | :--- | :--- |
| F2-01 | Soạn đề bài bằng Markdown kèm công thức LaTeX | A2 | |
| F2-02 | Phân loại bài toán theo độ khó và chủ đề | A2 | |
| F2-03 | Khai báo đặc tả bài toán cho CẢ HAI mô hình: chữ ký hàm theo từng ngôn ngữ (Bọc hàm) và định dạng input/output theo dòng chuẩn (Standard I/O) | A2 | Sửa 2026-08-24 (`DEC-2026-0824-dual-submission-model-per-problem`) — trước chỉ có chữ ký hàm, giờ bắt buộc cả hai vì học viên tự chọn mô hình lúc làm bài (F3-13) |
| F2-04 | Khai báo chiến lược so khớp kết quả cho bài toán | A2 | exact, chuẩn hoá khoảng trắng, epsilon, tập không thứ tự |
| F2-05 | Tạo testcase mẫu (Sample) — công khai, dùng cho chạy thử | A2 | |
| F2-06 | Tạo testcase ẩn (Hidden) — dùng cho nộp bài | A2 | |
| F2-07 | Tải lên bộ testcase theo lô | A2 | Bộ lớn lưu trên MinIO |
| F2-08 | Chống rò rỉ testcase ẩn: chỉ trả trạng thái và chỉ số, không trả input và không trả diff | A4 | Ràng buộc bảo mật nghiệp vụ |
| F2-09 | Phiên bản hoá bộ testcase | A2 A4 | Để chấm lại biết chấm theo phiên bản nào |
| F2-10 | Đặt giới hạn thời gian và bộ nhớ theo bài, kèm hệ số nhân theo ngôn ngữ | A2 A3 | Java chậm hơn C++ nên cùng một bài phải khác hệ số |
| F2-11 | Tìm kiếm và lọc danh sách bài toán theo chủ đề, độ khó, trạng thái đã giải | A1 | |
| F2-12 | Giao bài tập theo lớp | A2 | Bổ sung mục 2.11: bài đã gán theo lớp hiển thị lồng thành nhóm riêng ngay trong `problem_list`, không phải màn tách biệt |
| F2-13 | Bài đã lưu (bookmark) kèm ghi chú riêng tư theo `(user_id, problem_id)` | A1 | Bổ sung mục 2.1. Riêng tư tuyệt đối, chỉ chủ tài khoản đọc được; lưu server-side nên tự đồng bộ đa thiết bị |
| F2-14 | AI sinh testcase tự động — chỉ sinh input, output lấy từ chạy thật Đáp án mẫu qua go-judge | A2 | Bổ sung mục 2.6. Testcase sinh ra ở trạng thái nháp, cần Admin xác nhận; điều kiện tiên quyết là bài toán đã có Đáp án mẫu chạy Pass |

### 5.3. F3 — Bộ sinh mã bọc hàm

*Trọng tâm kỹ thuật của đề tài.* Phân hệ này cho phép mô hình bọc hàm hoạt động trên judge engine (go-judge
mặc định) — vốn chỉ nhận stdin/stdout, y như Judge0.

| Mã | Chức năng | Actor | Ghi chú |
| :--- | :--- | :--- | :--- |
| F3-01 | Lược đồ đặc tả kiểu dữ liệu độc lập ngôn ngữ | A4 | Kiểu nguyên thuỷ, chuỗi, mảng nhiều chiều, danh sách lồng nhau, danh sách liên kết, cây nhị phân |
| F3-02 | Sinh mã đọc dữ liệu vào cho Java, C++, Python | A4 | Từ một đặc tả duy nhất |
| F3-03 | Sinh mã gọi hàm người dùng | A4 | |
| F3-04 | Sinh mã in kết quả | A4 | |
| F3-05 | Tiêm mã người dùng vào vùng chèn của mã khung | A4 | |
| F3-06 | Đóng gói mã nguồn theo đúng định dạng adapter đang dùng yêu cầu trước khi gửi qua `JudgeExecutionPort` | A4 | Không hardcode Base64 — đó là yêu cầu riêng của Judge0, không phải yêu cầu chung của mọi engine (`DEC-2026-0823-go-judge-default-engine`) |
| F3-07 | So khớp exact | A4 | |
| F3-08 | So khớp có chuẩn hoá khoảng trắng | A4 | |
| F3-09 | So khớp số thực theo sai số epsilon | A4 | |
| F3-10 | So khớp tập hợp không xét thứ tự | A4 | |
| F3-11 | Ánh xạ lỗi biên dịch về đúng dòng trong mã người dùng | A4 | Trừ độ lệch dòng của vùng chèn |
| F3-12 | Che giấu lỗi thuộc phần mã harness | A4 | Người học không được thấy mã hệ thống |
| F3-13 | Cả hai mô hình nộp bài luôn song song cho mọi bài; học viên tự chọn lúc làm bài, không phải instructor chọn theo bài | A1 | Sửa 2026-08-24 (`DEC-2026-0824-dual-submission-model-per-problem`) — trước là "đường lùi" do A2 quyết theo bài, nay là lựa chọn của A1 mỗi lượt làm. Chỉ khi kiểu dữ liệu vượt lược đồ F3 mới ẩn hẳn Bọc hàm, còn Standard I/O luôn khả dụng |

### 5.4. F4 — Điều phối và giao tiếp judge engine

Gọi qua cổng ra trung lập theo engine (`JudgeExecutionPort`), adapter mặc định là go-judge — xem
`DEC-2026-0823-go-judge-default-engine`.

| Mã | Chức năng | Actor | Ghi chú |
| :--- | :--- | :--- | :--- |
| F4-01 | Tiếp nhận bài nộp: ghi trạng thái PENDING, đẩy vào hàng đợi, phản hồi ngay | A1 | Không chờ kết quả chấm. Bổ sung mục 2.12: nộp được cả bằng tải file lên (`.java`/`.cpp`/`.py`), cùng luồng với gõ trực tiếp |
| F4-02 | Chạy thử với testcase mẫu | A1 | Không ghi nhận vào tiến độ |
| F4-03 | Gọi `JudgeExecutionPort` một lần cho mỗi testcase | A4 | Không dồn batch — giữ đúng trải nghiệm realtime, khớp cả engine đồng bộ và bất đồng bộ |
| F4-04 | Dừng sớm: testcase trước sai hoặc lỗi theo điều kiện dừng thì không gọi testcase sau | A4 | Tiết kiệm tài nguyên, phản hồi nhanh hơn |
| F4-05 | Xác thực webhook bằng token bí mật riêng theo từng bài nộp | A4 | Chỉ áp dụng nếu một adapter bất đồng bộ (ví dụ Judge0Adapter) được bật — với adapter mặc định (go-judge, đồng bộ), không có callback nên không cần chức năng này; chi tiết thuộc nội bộ adapter, không phải yêu cầu ở port |
| F4-06 | Chống callback trùng: khoá xử lý theo token của engine, không ghi đè trạng thái cuối | A4 | Cùng điều kiện áp dụng như F4-05 — chỉ cần khi có adapter bất đồng bộ. Với adapter đồng bộ, "không ghi đè trạng thái cuối" vẫn là bất biến bắt buộc nhưng không cần khoá theo token vì không có callback trùng |
| F4-07 | Timeout sweep: quét bài nộp treo quá ngưỡng thời gian | A4 | Bản nhẹ của job đối soát cũ — với adapter đồng bộ + RabbitMQ ack/nack/redelivery, nguyên nhân treo chủ yếu là worker crash, không phải mất callback từ hệ ngoài, nên không cần chủ động truy vấn lại engine |
| F4-08 | Đẩy trạng thái từng testcase qua WebSocket theo kênh riêng của từng bài nộp | A4 | Ngay sau mỗi lần gọi cổng ra trả kết quả |
| F4-09a | Chọn phạm vi chấm lại: theo bài toán, theo danh sách lượt nộp, hoặc theo khoảng thời gian | A2 A3 | Chốt 2026-08-24 (`06-plan/PROTOTYPE_DEBT.md` mục 2.7), thay cho dòng F4-09 cũ chỉ ghi một câu |
| F4-09b | Ước lượng ảnh hưởng trước khi chạy thật (dry-run), có tuỳ chọn chạy thử trên mẫu nhỏ | A2 A3 | Số lượt bị ảnh hưởng, thời gian ước tính, số lần gọi `JudgeExecutionPort` dự kiến |
| F4-09c | Tuỳ chọn khi chạy: giữ điểm cũ nếu điểm mới thấp hơn, thông báo người học, chạy nền không chèn trước job trực tiếp | A2 A3 | |
| F4-09d | Tạm dừng/tiếp tục/huỷ phiên đang chạy; chỉ chấm lại lượt đã có kết quả cuối; kết quả cũ khôi phục được trong một khoảng thời gian | A2 A3 | Số ngày khôi phục `[SoT: Suy luận]`, chốt ở BD |
| F4-09e | Ghi mỗi phiên chấm lại vào Nhật ký hệ thống, xuất CSV | A4 | Nguồn ghi log cụ thể cho `SYSTEM_AUDIT_LOG` (F1-12), liên kết F1-14 |
| F4-10 | Giám sát hàng đợi và tình trạng cụm judge engine | A3 | |
| F4-11 | Quản lý cấu hình ngôn ngữ và giới hạn tài nguyên | A3 | |

### 5.5. F5 — Phân hệ AI

Kích hoạt **sau khi** bài nộp đạt Accepted. Hai chức năng độc lập, người dùng chủ động chọn.

#### F5.1 — Phân tích bài giải (một lượt)

| Mã | Chức năng | Actor | Ghi chú |
| :--- | :--- | :--- | :--- |
| F5-01 | Yêu cầu phân tích bài giải vừa nộp | A1 | |
| F5-02 | Phân tích độ phức tạp thời gian và bộ nhớ thực tế của mã đã nộp, kèm lập luận | A4 | |
| F5-03 | Đối chiếu với độ phức tạp tối ưu đã biết của bài toán; nếu chưa tối ưu thì gợi ý hướng tiếp cận tốt hơn | A4 | |
| F5-04 | Chỉ ra trường hợp biên bộ test chưa phủ, giả định ngầm trong mã, nguy cơ tràn số, rủi ro khi dữ liệu lớn hơn ràng buộc | A4 | |
| F5-05 | Nhận xét chất lượng mã: đặt tên, phân rã, trùng lặp, độ dễ đọc | A4 | Chốt 2026-08-25: kèm điểm số dễ đọc thang 1-5, AI tự chấm trong cùng lượt phân tích, là một trường của F5-07 |
| F5-06 | Đưa ra câu hỏi mở rộng để người học tự củng cố | A4 | Chốt 2026-08-25: dạng chủ đề/từ khoá gợi ý, không liên kết cố định tới câu hỏi cụ thể trong `interview-bank` — tránh đọc chéo module (`DEC-2026-0820-architecture-baseline`); UI dựng liên kết tìm kiếm sang `interview_bank_list` theo chủ đề. Cách trình bày cụ thể **[Đợi nextjs]** |
| F5-07 | Trả kết quả dưới dạng dữ liệu có cấu trúc (JSON theo lược đồ) | A4 | Để giao diện render báo cáo tĩnh |
| F5-08 | Lưu báo cáo kèm bài nộp, tra cứu lại được từ trang tiến độ | A4 | |
| F5-26 | Áp dụng bản mã AI đề xuất vào Workspace, ghi đè mã đang có | A1 | Bổ sung mục 2.13. Bắt buộc xác nhận trước khi ghi đè; giữ lại bản mã cũ (khôi phục được), không mất luôn |
| F5-27 | Điểm quy đổi trên thang 10 từ báo cáo F5.1, cho giảng viên xem nhanh + chấm tay đè lên trong lớp mình phụ trách | A2 A4 | Bổ sung mục 6.2.a (`06-plan/PROTOTYPE_DEBT.md`). Chốt 2026-08-24: chỉ là lớp tham khảo nội bộ của giảng viên, tách bạch khỏi Pass/Fail chính thức (F4-04) và không mâu thuẫn F5-18 vì không hiện cho người học như điểm chính thức |

#### F5.2 — Phỏng vấn giả lập 1:1 (nhiều lượt)

| Mã | Chức năng | Actor | Ghi chú |
| :--- | :--- | :--- | :--- |
| F5-09 | Mở phiên phỏng vấn cho một bài nộp đã Accepted | A1 | |
| F5-10 | Giai đoạn 1 — giải trình thuật toán: người học trình bày ý tưởng và lý do chọn cấu trúc dữ liệu | A1 A4 | |
| F5-11 | Giai đoạn 2 — phản biện: người phỏng vấn chất vấn điểm chưa tối ưu, hỏi xoáy vào trường hợp biên | A4 | |
| F5-12 | Giai đoạn 3 — mở rộng quy mô: tình huống dữ liệu tăng đột biến hoặc ràng buộc hệ thống đổi | A4 | |
| F5-13 | Duy trì ngữ cảnh phiên qua `ChatMemory` trên Redis | A4 | |
| F5-14 | Stream phản hồi về giao diện qua SSE | A4 | |
| F5-15 | Xuất bảng đánh giá rubric khi kết phiên: độ rõ ràng khi trình bày, độ chính xác kỹ thuật, khả năng phản biện, nhận thức về độ phức tạp — kèm nhận xét từng tiêu chí | A4 | |
| F5-16 | Lưu phiên và rubric, mở lại được từ trang tiến độ | A4 | |
| F5-24 | Ba lối vào phiên phỏng vấn: từ bài nộp Accepted, từ kho câu hỏi F6, hoặc tự chọn chủ đề | A1 | Chốt 2026-08-24 (mục 1.4). Cùng chung luồng 3 giai đoạn và mọi ràng buộc AI, chỉ khác nguồn đề bài/chủ đề |

#### Ràng buộc chung của phân hệ AI

| Mã | Ràng buộc | Ghi chú |
| :--- | :--- | :--- |
| F5-17 | Chống prompt injection: mã nguồn và câu trả lời người dùng là **tham số dữ liệu**, tách hoàn toàn khỏi chỉ thị hệ thống | Áp cho cả F5 và F6 |
| F5-18 | Định hướng giáo dục: rubric và báo cáo là phản hồi hỗ trợ học tập, **không phải điểm số chính thức** | Phải hiện lên giao diện |
| F5-19 | Giới hạn tần suất gọi theo người dùng | Kiểm soát chi phí |
| F5-20 | Cache kết quả F5.1 theo hash mã nguồn | Nộp lại đúng mã đó thì không gọi lại API |
| F5-21 | Theo dõi và ghi nhận lượng token tiêu thụ theo từng phiên | |
| F5-25 | Ngân sách token AI theo khoảng thời gian, dự báo cạn quota, bảng xếp hạng người dùng/bài toán tốn token nhiều nhất | Bổ sung mục 2.9. Chốt 2026-08-24: vượt ngân sách → tự động tạm khoá gọi AI cho STUDENT/INSTRUCTOR; ADMIN không bị khoá |
| F5-22 | Suy giảm có kiểm soát: AI hỏng hoặc hết quota thì F1-F4 vẫn hoạt động bình thường | Ràng buộc kiến trúc, không phải lời hứa |
| F5-23 | Cấu hình prompt (có phiên bản, xem lại/khôi phục), trọng số rubric, giới hạn tần suất | Actor A3. Chốt 2026-08-24 (mục 2.8): "Chạy đối chiếu" (regression test 30 bài mẫu) giữ trên UI, backend thật hoãn sang giai đoạn mở rộng |

### 5.6. F6 — Ngân hàng câu hỏi phỏng vấn

*Màn hình độc lập, không gắn với bài nộp code.*

| Mã | Chức năng | Actor | Ghi chú |
| :--- | :--- | :--- | :--- |
| F6-01 | Danh sách câu hỏi phân loại theo chủ đề và mức độ khó | A1 | Cấu trúc dữ liệu, thuật toán, thiết kế hệ thống, câu hỏi hành vi |
| F6-02 | Tìm kiếm và lọc câu hỏi | A1 | |
| F6-03 | Đánh dấu câu hỏi để xem lại | A1 | |
| F6-04 | Chế độ học: xem gợi ý hướng tiếp cận | A1 | |
| F6-05 | Chế độ học: xem khung trả lời chuẩn, áp dụng mô hình STAR cho câu hỏi hành vi | A1 | |
| F6-06 | Chế độ học: xem danh sách từ khoá kỹ thuật cốt lõi cần nêu | A1 | |
| F6-07 | Chế độ luyện: người dùng tự soạn câu trả lời | A1 | |
| F6-08 | Chế độ luyện: AI đối chiếu câu trả lời với tiêu chí chuẩn, trả phản hồi ngắn — điểm đã đạt, điểm còn thiếu, hướng bổ sung | A4 | Đi qua phân hệ AI, không tự gọi LLM |
| F6-09 | Lịch sử luyện tập và danh sách câu hỏi cần ôn lại | A1 | |
| F6-10 | Tỉ lệ hoàn thành theo từng chủ đề | A1 | |
| F6-11 | Giảng viên tạo bộ câu hỏi riêng và gán cho lớp phụ trách | A2 | |
| F6-12 | Tự chấm mức độ thuộc bài (Biết rõ / Mơ hồ / Quên), hệ thống tự xếp lịch ôn lại | A1 | Bổ sung mục 2.10. Thuật toán kiểu spaced-repetition đơn giản, chốt công thức chính xác ở DD |

### 5.7. Tổng hợp số lượng

Cập nhật 2026-08-24 sau khi đồng bộ với prototype `09-layoutBase/` (F1-10..17, F2-13/14, F4-09a..e, F5-24..27, F6-12):

| Phân hệ | Số chức năng | Trọng số công việc dự kiến |
| :--- | :---: | :--- |
| F1 — Danh tính và phân quyền | 17 | Trung bình. 17 mã (F1-01 tới F1-17): bao gồm ma trận quyền Role × Function × Action, OAuth, danger zone xoá tài khoản, tự đặt lại mật khẩu qua email |
| F2 — Ngân hàng bài toán và testcase | 14 | Trung bình. 14 mã (F2-01 tới F2-14): giao diện soạn đề A2, bookmark note riêng tư, AI sinh input testcase |
| F3 — Bộ sinh mã bọc hàm | 13 | **Cao nhất, tăng thêm sau 2026-08-24.** 13 mã (F3-01 tới F3-13): trọng tâm kỹ thuật, nhân ba theo số ngôn ngữ — và từ `DEC-2026-0824-dual-submission-model-per-problem`, codegen tăng gần gấp đôi vì mỗi ngôn ngữ giờ cần sinh cả mã Bọc hàm lẫn khung Standard I/O cho hầu hết bài toán, không còn là trường hợp hiếm |
| F4 — Điều phối judge engine | 15 | **Cao.** 15 mã (F4-01 tới F4-08, F4-09a tới F4-09e, F4-10, F4-11): điều phối concurrency Virtual Threads, realtime WebSocket từng testcase, quy trình chấm lại 5 bước có dry-run/pause/audit |
| F5 — Phân hệ AI | 27 | Cao. 27 mã (F5-01 tới F5-27): hai luồng phân tích bài giải & phỏng vấn giả lập, ngân sách token tự khoá, điểm AI tham khảo & chấm tay F5-27 |
| F6 — Ngân hàng câu hỏi | 12 | Thấp. 12 mã (F6-01 tới F6-12): chế độ học & luyện,spaced-repetition tự chấm |
| **Tổng** | **97** | *(Hoặc 93 nếu tính nhóm F4-09 là 1 chức năng)* |

---

## 6. Quy trình nghiệp vụ chính

Bốn luồng dưới đây là đầu vào để BD vẽ sơ đồ tuần tự và sơ đồ hoạt động. Mỗi bước ghi actor thực hiện.
[SoT: Suy luận về thứ tự bước; nội dung từng bước lấy từ `README.md` mục 4]

### 6.1. Luồng chính — giải một bài toán từ đầu tới khi có phản hồi AI

1. **A1** mở danh sách bài toán, lọc theo chủ đề và độ khó, chọn một bài (F2-11).
2. **A1** đọc đề bài, chọn ngôn ngữ trong ba ngôn ngữ hỗ trợ và chọn mô hình nộp bài (Bọc hàm hoặc Standard
   I/O, F3-13). Hệ thống hiện mã khung tương ứng chữ ký hàm hoặc định dạng input/output của bài (F2-03).
3. **A1** viết mã, bấm chạy thử. **A4** sinh mã harness (F3-02 tới F3-06), gọi judge engine qua
   `JudgeExecutionPort` với **testcase mẫu**, trả kết quả kèm dữ liệu vào ra thấy được (F4-02).
4. **A1** sửa mã, bấm nộp bài. **A4** ghi trạng thái PENDING, đẩy vào hàng đợi, trả về ngay (F4-01).
5. **A4** lấy việc khỏi hàng đợi, sinh mã harness, gọi `JudgeExecutionPort` cho từng **testcase ẩn**, một
   lần mỗi testcase, dừng sớm nếu gặp điều kiện dừng (F4-03, F4-04).
6. Judge engine (go-judge) chạy testcase trong sandbox, trả kết quả ngay trong response — không có bước
   callback riêng như Judge0.
7. **A4** đẩy trạng thái từng testcase về giao diện qua WebSocket ngay sau mỗi lần gọi trả kết quả (F4-08).
8. Có testcase sai hoặc lỗi thì **A4** dừng gửi đợt sau (F4-04). Giao diện chỉ hiện **chỉ số và trạng thái**
   của testcase ẩn, không hiện dữ liệu vào (F2-08).
9. Toàn bộ testcase đúng thì trạng thái tổng thành Accepted. Giao diện mở ra hai lựa chọn AI.
10. **A1** chọn một trong hai:
    * **Phân tích bài giải** — **A4** kiểm cache theo hash mã nguồn (F5-20); chưa có thì gọi AI, nhận JSON
      có cấu trúc, lưu kèm bài nộp và hiện báo cáo (F5-01 tới F5-08).
    * **Phỏng vấn giả lập** — sang luồng 6.2.

**Điểm dễ vỡ của luồng này:** bước 5 và 7. Với adapter mặc định (go-judge, đồng bộ), rủi ro chính là worker
crash giữa lúc đang gọi `JudgeExecutionPort` — message chưa ack bị RabbitMQ redeliver, và bất biến "không ghi
đè trạng thái cuối" phải giữ nguyên qua lần chạy lại đó; nếu vẫn treo quá ngưỡng thời gian, timeout sweep
(F4-07) phát hiện. Nếu một adapter bất đồng bộ (ví dụ `Judge0Adapter`) được cắm lại sau này, thêm rủi ro cũ:
callback đến muộn của một testcase không được đưa một bài nộp đã có trạng thái cuối về lại trạng thái trung
gian — xử lý ở tầng adapter, không lộ ra domain.

### 6.2. Luồng phỏng vấn giả lập

1. **A1** mở phiên phỏng vấn cho một bài nộp đã Accepted (F5-09).
2. **A4** dựng ngữ cảnh phiên: đề bài, đặc tả hàm, mã nguồn đã nộp — tất cả dưới dạng **tham số dữ liệu**,
   không phải chỉ thị (F5-17).
3. **Giai đoạn 1.** AI hỏi về ý tưởng tiếp cận; **A1** trả lời; phản hồi stream về qua SSE (F5-10, F5-14).
4. **Giai đoạn 2.** AI chất vấn điểm chưa tối ưu và trường hợp biên (F5-11).
5. **Giai đoạn 3.** AI đặt tình huống quy mô dữ liệu tăng hoặc ràng buộc đổi (F5-12).
6. **A4** giữ ngữ cảnh xuyên ba giai đoạn qua `ChatMemory` trên Redis (F5-13).
7. Kết phiên: **A4** xuất rubric bốn tiêu chí kèm nhận xét, lưu lại (F5-15, F5-16).
8. Giao diện hiện rõ rubric là **phản hồi học tập, không phải điểm** (F5-18).

**Nhánh lỗi bắt buộc có:** AI hết quota hoặc lỗi ở bất kỳ bước nào từ 3 tới 7 thì phiên kết thúc có kiểm
soát, người học nhận thông báo rõ ràng, **và toàn bộ bài nộp cùng kết quả chấm ở luồng 6.1 không bị ảnh
hưởng** (F5-22).

### 6.3. Luồng soạn nội dung của giảng viên

1. **A2** tạo bài toán mới: đề bài Markdown kèm LaTeX, độ khó, chủ đề (F2-01, F2-02).
2. **A2** khai báo đặc tả: chữ ký hàm cho từng ngôn ngữ, kiểu tham số và kiểu trả về (F2-03).
3. **A2** chọn chiến lược so khớp kết quả (F2-04).
4. **A2** tạo testcase mẫu và tải lên bộ testcase ẩn (F2-05 tới F2-07).
5. **A2** tự chạy thử một bài giải mẫu để xác nhận harness sinh mã đúng cho cả ba ngôn ngữ.
   [SoT: Suy luận — `README.md` không nêu bước này, nhưng không có nó thì lỗi đặc tả chỉ bị phát hiện khi
   người học đã nộp bài]
6. **A2** công bố bài toán, gán vào lớp nếu cần (F2-12).
7. Sửa bộ testcase sau khi đã có người nộp bài thì **A2** tăng phiên bản bộ testcase (F2-09) và yêu cầu
   chấm lại (F4-09a tới F4-09e).

### 6.4. Luồng vận hành của quản trị viên

1. **A3** xem bảng giám sát: độ dài hàng đợi, số bài nộp đang chạy, tình trạng cụm judge engine (F4-10).
2. Phát hiện bài nộp treo: kiểm tra timeout sweep (F4-07), kích hoạt chấm lại nếu cần (F4-09a tới F4-09e).
3. Quản lý cấu hình ngôn ngữ, giới hạn thời gian và bộ nhớ, hệ số nhân theo ngôn ngữ (F4-11, F2-10).
4. Theo dõi lượng token AI đã tiêu thụ, điều chỉnh giới hạn tần suất (F5-19, F5-21).
5. Cập nhật prompt và rubric của phân hệ AI (F5-23).

---

## 7. Danh sách màn hình dự kiến

[SoT: Suy luận — đây là **danh sách hạt giống** cho trục screen; chốt chính thức ở `01-rd/screens/<khu
vực>/<slug>.md`, chia thư mục con theo khu vực actor: `shared/` (dùng chung mọi vai trò, ví dụ `auth`),
`users/` (A1), `teacher/` (A2), `admin/` (A3)]

Slug viết `snake_case`; slice frontend tương ứng viết `kebab-case` (`01-rd/system/codebase_structure.md`
mục 3). Cột "Chức năng" trỏ về mã ở mục 5 — đó là cách BD theo screen truy về được BD theo module.

### 7.1. Khu vực người học

| Slug | Tên màn | Chức năng chính | Bounded Context liên quan |
| :--- | :--- | :--- | :--- |
| `auth` | Đăng nhập và đăng ký | F1-01, F1-02 | `identity` |
| `problem_list` | Danh sách bài toán | F2-11 | `problem-bank`, `identity` |
| `problem_detail` | Chi tiết bài toán: đề bài, soạn mã, chạy thử, nộp bài | F2-01, F3-*, F4-01, F4-02, F4-08 | `problem-bank`, `harness`, `judge-orchestration` |
| `submission_result` | Kết quả một bài nộp | F4-08, F2-08 | `judge-orchestration` |
| `solution_review` | Báo cáo phân tích bài giải | F5-01 tới F5-08 | `ai-review` |
| `mock_interview` | Phiên phỏng vấn giả lập | F5-09 tới F5-16 | `ai-review` |
| `interview_bank_list` | Danh sách câu hỏi phỏng vấn | F6-01 tới F6-03 | `interview-bank` |
| `interview_question_detail` | Chi tiết câu hỏi: chế độ học và chế độ luyện | F6-04 tới F6-08 | `interview-bank`, `ai-review` |
| `my_progress` | Tiến độ cá nhân | F1-06 tới F1-08, F6-09, F6-10 | `identity`, `interview-bank` |
| `my_submissions` | Lịch sử bài nộp của tôi | F1-07, F1-18 | `judge-orchestration` |
| `profile` | Trang cá nhân — thông tin hiển thị, đổi mật khẩu | F1-09, F1-19 | `identity` |
| `settings` | Cài đặt — chủ đề màu, ngôn ngữ giao diện, Workspace, phỏng vấn tự luyện, thông báo, xuất dữ liệu, xoá tài khoản | F1-16, F1-20, F1-21, F1-22, F5-28 | `identity`, `ai-review` |
| `saved_problems` | Bài đã lưu — bookmark kèm ghi chú riêng tư | F2-13 | `problem-bank` |

**Sửa 2026-08-24 theo `06-plan/PROTOTYPE_DEBT.md` mục 3.1/3.2:** `profile_settings` (1 slug suy luận ban
đầu) tách thành 2 màn thật `profile` và `settings` theo đúng prototype (`Trang cá nhân.dc.html`,
`Cài đặt.dc.html`); thêm `saved_problems` (`Bài đã lưu.dc.html`) — có thật trong prototype nhưng chưa từng
liệt kê.

**Cập nhật 2026-08-25 theo `06-plan/reports/260825-1500-report-ai1-phase2-conflicts.md`:** khoảng trống mã
`Fx-nn` của `settings` (trước đó "chỉ gán được F1-16") đã lấp bằng F1-20/F1-21/F1-22/F5-28; `my_submissions`
(trước đó chỉ F1-07, một chỉ số, không phủ chính nội dung màn) lấp bằng F1-18; `profile` bổ sung F1-19 (tự
đổi mật khẩu). Chủ đề màu/ngôn ngữ giao diện trên `settings` tiếp tục không cần mã riêng — đã có quyết định
(`DEC-2026-0824-dark-light-theme`, `DEC-2026-0824-i18n-vi-en`).

**Màn nặng nhất là `problem_detail`** — nó chạm bốn Bounded Context và chứa Monaco Editor, bảng testcase
realtime, và cửa vào hai luồng AI. Đây là màn nên làm prototype trước tiên.

**`problem_detail` và `submission_result` là hai màn riêng, đừng gộp:** kết quả một bài nộp cần URL riêng để
mở lại được từ trang tiến độ.

### 7.2. Khu vực giảng viên

**Chốt 2026-08-24 theo `06-plan/PROTOTYPE_DEBT.md` mục 3.3 — Phương án B:** các slug dưới đây có layout/route
**riêng biệt khỏi khu Admin**, phản ánh đúng trải nghiệm riêng cho A2 — không dùng chung shell với A3 như khu
Admin ở mục 7.3. Đây là quyết định về **bố cục màn**, tách bạch khỏi quyết định ở mục 1.2 (đã chốt trước đó)
vốn chỉ nói về **cơ chế phân quyền** (ma trận Role × Function × Action) — hai quyết định độc lập, không mâu
thuẫn nhau.

**Cập nhật 2026-08-24 (tiếp) theo `06-plan/PROTOTYPE_DEBT.md` mục 6.2.b:** 5 màn Giáo viên đã dựng prototype
thật (`09-layoutBase/Giáo viên - *.dc.html`), phủ 2/4 slug hạt giống ban đầu (`class_management`,
`class_progress`) và phát sinh thêm 2 slug ngoài dự kiến (`instructor_overview`, `instructor_grading`).
`problem_authoring` và `testcase_management` **vẫn chưa có prototype**, còn là hạt giống `[SoT: Suy luận]`.

| Slug | Tên màn | Chức năng chính | Bounded Context liên quan | Prototype |
| :--- | :--- | :--- | :--- | :--- |
| `problem_authoring` | Soạn bài toán và đặc tả hàm | F2-01 tới F2-04, F2-14 | `problem-bank`, `harness` | Chưa có |
| `testcase_management` | Quản lý testcase và phiên bản bộ testcase | F2-05 tới F2-09 | `problem-bank` | Chưa có |
| `class_management` | Quản lý lớp và giao bài tập | F2-12, F6-11 | `identity`, `problem-bank`, `interview-bank` | `Giáo viên - Lớp của tôi.dc.html`, `Giáo viên - Bài tập của tôi.dc.html` (gán bài từ ngân hàng cho lớp) |
| `class_progress` | Tiến độ lớp | (dẫn xuất từ F1-06, F1-07) | `identity`, `judge-orchestration` | `Giáo viên - Tiến độ học viên.dc.html` |
| `instructor_overview` | Tổng quan khu Giảng viên | (tổng hợp F2-12, F5-27, F6-11) | `identity` | `Giáo viên - Tổng quan.dc.html`. Slug mới phát sinh khi dựng prototype, không nằm trong 4 slug hạt giống ban đầu — hợp lý vì mỗi khu vực có shell riêng thường cần một dashboard riêng |
| `instructor_grading` | Điểm AI tham khảo và chấm tay theo lớp | F5-27 | `ai-review`, `problem-bank` | `Giáo viên - Chấm bài.dc.html`. Slug mới, gắn mã F5-27 (chốt 2026-08-24, mục 6.2.a) |

### 7.3. Khu vực quản trị

| Slug | Tên màn | Chức năng chính | Bounded Context liên quan |
| :--- | :--- | :--- | :--- |
| `admin_queue_monitor` | Giám sát hàng đợi và cụm judge engine | F4-10 | `judge-orchestration` |
| `admin_rejudge` | Kích hoạt và theo dõi chấm lại | F4-09a tới F4-09e | `judge-orchestration` |
| `admin_language_config` | Cấu hình ngôn ngữ và giới hạn tài nguyên | F4-11, F2-10 | `judge-orchestration`, `problem-bank` |
| `admin_ai_config` | Cấu hình prompt, rubric và giới hạn tần suất AI | F5-19, F5-23 | `ai-review` |
| `admin_ai_usage` | Theo dõi lượng token tiêu thụ, ngân sách và dự báo cạn quota | F5-21, F5-25 | `ai-review` |
| `admin_permission_matrix` | Ma trận phân quyền Role × Function × Action | F1-10, F1-11, F1-12 | `identity` |
| `admin_user_management` | Quản lý tài khoản: đổi vai trò, khoá/mở khoá, reset mật khẩu | F1-13 | `identity` |
| `admin_system_log` | Nhật ký hệ thống: audit hành động quản trị | F1-14 | `identity` |

Tổng: **27 màn dự kiến** — 13 người học, 6 giảng viên, 8 quản trị. Cập nhật 2026-08-24 (tiếp) theo
`06-plan/PROTOTYPE_DEBT.md` mục 6.2.b: thêm `instructor_overview` và `instructor_grading` (2 slug phát sinh
khi dựng prototype khu Giảng viên), khu giảng viên từ 4 lên 6 slug. Trước đó là 25 màn (mục 3.1/3.2: tách
`profile_settings` thành `profile`/`settings`, thêm `saved_problems`; mục 2.5: thêm `admin_system_log`) —
trước nữa là 22 (thêm `admin_permission_matrix`/`admin_user_management` theo mục 1.2, 2026-08-23).
`problem_authoring` và `testcase_management` ở mục 7.2 vẫn ở dạng hạt giống, chưa có prototype dựng thật.

---

## 8. Giới hạn phạm vi

### 8.1. Nằm trong phạm vi

Theo `README.md` mục 6:

| Hạng mục | Chi tiết |
| :--- | :--- |
| Ngôn ngữ nộp bài | **Đúng ba ngôn ngữ**: Java, C++, Python. Không nhiều hơn |
| Mô hình nộp bài | **Cả hai**: bọc hàm và nhập/xuất chuẩn |
| Môi trường chạy | Thực thi đơn luồng cho mỗi bài nộp, trong sandbox cô lập |
| Tương tác AI | Phỏng vấn dạng văn bản, có đầy đủ ngữ cảnh mã nguồn vừa nộp |
| Quản lý học tập | Giao bài và quản lý bài tập theo lớp cho giảng viên |

### 8.2. Ngoài phạm vi

| Hạng mục bị loại | Vì sao loại |
| :--- | :--- |
| Cloud IDE hoàn chỉnh | Không phải đóng gói của đề tài; Monaco Editor đủ cho việc viết một hàm |
| Bài toán tương tác (Interactive Problems) | Cần giao thức hai chiều với tiến trình bị chấm — judge engine (go-judge cũng như Judge0) không hỗ trợ |
| Phỏng vấn bằng giọng nói | Thêm một trục kỹ thuật (nhận dạng và tổng hợp tiếng nói) không liên quan tới đóng góp chính |
| Hệ thống giải đấu và bảng xếp hạng thời gian thực | Thiết kế cho thi đấu, không cho tự học. **Lưu ý: chấm lại vẫn nằm trong phạm vi** |
| Trình chấm tuỳ biến do người ra đề tải lên | Cho phép chạy mã của A2 trong tầng chấm là mở một bề mặt bảo mật mới |

**Đã chốt 2026-08-24** (`06-plan/PROTOTYPE_DEBT.md` mục 4.1/4.2, qua hỏi trực tiếp chủ dự án) — hai trong ba
mục dưới đây không còn là "chưa quyết":
- **Đa ngôn ngữ giao diện (i18n): CÓ**, tiếng Việt + tiếng Anh (`DEC-2026-0824-i18n-vi-en`). Bốn màn Admin
  hiện thiếu khung song ngữ (`Chấm lại`, `Cấu hình AI`, `Hàng đợi chấm`, `Ngôn ngữ và giới hạn`) cần bổ sung
  khi dựng UI thật, không chặn RD.
- **Chế độ tối (Dark/Light theme): CÓ cả hai**, Light là mặc định (`DEC-2026-0824-dark-light-theme`).

**Một thứ vẫn chưa quyết, không phải đã loại** [SoT: Suy luận]: thông báo qua email. Ghi ở đây để không ai
coi im lặng là đã loại.

**Đã chốt 2026-08-24** (`06-plan/PROTOTYPE_DEBT.md` mục 2.8, qua hỏi trực tiếp chủ dự án) — một mục hoãn lại
chứ không loại hẳn: **backend thật cho "Chạy đối chiếu" (regression test) prompt AI trước khi publish**
(F5-23) — chạy 30 bài giải mẫu qua AI để so sánh điểm rubric giữa bản nháp và bản đang chạy. Nút này **giữ
nguyên trên `09-layoutBase/Admin - Cấu hình AI.dc.html`** để thể hiện tầm nhìn thiết kế, nhưng phần logic
chạy batch AI thật xếp vào hướng phát triển mở rộng — không nằm trong cam kết DD/test đợt này, theo đúng thứ
tự cắt giảm để tránh vỡ tiến độ đã cảnh báo ở rủi ro R3 (mục 9 dưới đây).

---

## 9. Rủi ro và phương án xử lý

Theo `README.md` mục 7, xếp lại theo mức nghiêm trọng.

| # | Rủi ro | Mức | Phương án xử lý | Ai theo dõi |
| :--- | :--- | :--- | :--- | :--- |
| R1 | ~~Judge0 yêu cầu cgroup v1, Linux hiện đại mặc định v2~~ — **đã đóng cho luồng mặc định** | Trước đây: Chặn cả đề tài | Giải quyết bằng `DEC-2026-0823-go-judge-default-engine`: đổi engine mặc định sang go-judge, hỗ trợ cả cgroup v1/v2. Rủi ro chỉ còn nếu Judge0Adapter được cắm lại | Đóng — theo dõi lại chỉ nếu đổi engine |
| R2 | Bộ sinh mã không bao phủ hết kiểu dữ liệu | Cao | Xác định trước tập kiểu ưu tiên; bài có cấu trúc phức tạp chưa hỗ trợ **Bọc hàm** thì chỉ còn hiện Standard I/O (F3-13 — sửa 2026-08-24, `DEC-2026-0824-dual-submission-model-per-problem`: giờ là ẩn bớt một trong hai mô hình luôn song song, không phải chuyển hẳn sang mô hình khác) | Khi thiết kế lược đồ kiểu |
| R3 | Khối lượng công việc vượt tiến độ | Cao | Cố định hoàn thiện F1-F4; thứ tự cắt giảm: phỏng vấn giả lập → công cụ lớp học → dashboard phân tích nâng cao | Suốt dự án |
| R4 | Chi phí API AI vượt dự kiến | Trung bình | Giới hạn tần suất theo người dùng, cache theo hash mã nguồn, kiểm soát quota token theo phiên (F5-19 tới F5-21) | Khi làm F5 |
| R5 | Không đủ người dùng thật để kiểm thử | Trung bình | Phối hợp với giảng viên môn Cấu trúc dữ liệu và Giải thuật, đưa vào bài tập thực hành trên lớp | Trước khi báo cáo |

**R1 từng là rủi ro phải xử lý trước tiên.** Nếu judge engine không chạy được thì F3 không kiểm chứng được,
F4 không có gì để điều phối, và F5 không có bài nộp Accepted nào để phân tích — nghĩa là toàn bộ đề tài
đứng. Đã đóng bằng việc đổi engine mặc định sang go-judge (không kén cgroup version).

---

## 10. Tiêu chí thành công

[SoT: Suy luận — đề xuất của phân tích; cần chủ nhiệm đề tài xác nhận]

Để tránh tình trạng "làm xong" mà không ai nói được là đạt hay chưa:

| # | Tiêu chí | Cách đo |
| :--- | :--- | :--- |
| S1 | Một bài toán bọc hàm chạy đúng trên **cả ba** ngôn ngữ từ **một** đặc tả duy nhất | Test so khớp mã sinh ra cho ba ngôn ngữ, cộng một lần chạy thật qua judge engine (go-judge) |
| S2 | Lỗi biên dịch được ánh xạ về đúng dòng trong mã người học, không lộ mã harness | Bộ test có sẵn mã sai cố ý, so số dòng báo lỗi |
| S3 | Trạng thái từng testcase hiện về giao diện theo thời gian thực | Chạy luồng E2E, đo độ trễ từ khi `JudgeExecutionPort` trả kết quả tới lúc giao diện đổi |
| S4 | Bài nộp không bao giờ treo vĩnh viễn | Cố ý cho worker crash giữa lúc đang gọi engine, xác nhận RabbitMQ redeliver và timeout sweep (F4-07) dọn được. Nếu `Judge0Adapter` đang bật: thêm kịch bản cố ý bỏ một callback |
| S5 | Message bị xử lý lại (redelivery) không làm sai kết quả | Gửi lại cùng một message đã ack một phần, xác nhận không ghi đè trạng thái cuối. Nếu `Judge0Adapter` đang bật: thêm kịch bản callback trùng và callback giả mạo token |
| S6 | Testcase ẩn không rò rỉ qua bất kỳ bề mặt nào | Rà toàn bộ phản hồi API và log của luồng nộp bài |
| S7 | Tắt hoàn toàn phân hệ AI thì F1-F4 vẫn chạy đủ | Chạy lại luồng E2E chính với AI bị vô hiệu hoá |
| S8 | Mã nguồn người dùng không thể thành chỉ thị cho AI | Bộ test có mã chứa câu lệnh tiêm prompt |

---

## 11. Đọc tiếp gì

| Muốn biết | Đọc |
| :--- | :--- |
| Nền tảng lý thuyết và lý do chọn từng nguyên lý | `01-rd/overview/overview.md` |
| Từ vựng chuẩn của dự án | `01-rd/overview/glossary.md` |
| Kiến trúc backend và frontend | `01-rd/system/backend_architecture.md` · `01-rd/system/frontend_architecture.md` |
| Yêu cầu chức năng dạng đặc tả và yêu cầu phi chức năng | `01-rd/req/req.md` (**còn là nội dung dự án cũ**, xem `01-rd/README.md` mục 3) |
| Thiết kế theo module và theo màn | `02-bd/` |
