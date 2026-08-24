# User Story và Tiêu Chí Chấp Nhận

`req.md` liệt kê **tính năng** theo phân hệ `Fx-nn`; file này diễn đạt cùng phạm vi đó thành **việc mỗi
actor cần làm được** và điều kiện để coi là làm được. Đây là đầu vào để viết BD/DD theo màn và để viết tiêu
chí nghiệm thu.

> **Viết lại 2026-08-24.** Bản trước là nội dung của dự án cũ NestGame v2 (module IAM/Catalog/SaveState,
> chơi game retro) — nợ tài liệu đã ghi ở `01-rd/README.md` mục 3/4. Bản này viết theo bốn actor A1-A4 của
> `README.md` mục 3, bám đúng bảng chức năng có mã `Fx-nn` ở `01-rd/overview/system_survey.md` mục 5 —
> **không đánh số lại**, mỗi story trỏ về đúng mã `Fx-nn` liên quan để truy vết sang BD, DD, `04-tdd/` và
> test. Nội dung đồng bộ với `req.md` (viết lại cùng đợt) — không lặp lại phần đặc tả kỹ thuật, chỉ diễn đạt
> lại dưới góc nhìn actor.
>
> **Quan hệ với `04-tdd/`.** File này ở tầng RD, viết theo ngôn ngữ nghiệp vụ — một tiêu chí chấp nhận là
> một câu Cho/Khi/Thì. `04-tdd/<module>.md` là bộ tiêu chí nghiệm thu chi tiết hơn (`AC-nn` dạng Precondition
> / Action / Expected result), viết **cùng lúc với DD của module đó** (`.claude/rules/tdd-mode.md`). Chưa
> file `04-tdd/` nào tồn tại — viết dần theo từng slice, không viết trước hàng loạt.
>
> Ký hiệu: **Cho** (bối cảnh) · **Khi** (hành động) · **Thì** (kết quả) — Given/When/Then.

Mỗi story theo chuẩn INVEST (Independent, Negotiable, Valuable, Estimable, Small, Testable). Story của A4
(hệ thống tự động) không có "tôi muốn" theo nghĩa người — A4 là job/consumer, viết dưới dạng "hệ thống tự
động phải" để giữ đúng ngữ pháp nhưng không giả vờ A4 là người.

---

## 1. A1 — Sinh viên / người dùng cuối

### US-A1-01: Đăng ký và đăng nhập

Là **sinh viên**, tôi muốn tạo tài khoản và đăng nhập, để bắt đầu luyện tập.

- **Cho** tôi chưa có tài khoản, **Khi** tôi đăng ký bằng email và mật khẩu, **Thì** tài khoản được tạo với
  vai trò mặc định `STUDENT` (F1-01, F1-05).
- **Cho** tôi đã có tài khoản, **Khi** tôi đăng nhập đúng thông tin, **Thì** tôi nhận Access Token thời hạn
  ngắn và Refresh Token trong cookie HTTP-Only (F1-02).
- **Cho** Access Token đã hết hạn, **Khi** tôi gọi một API và nhận `401`, **Thì** client tự làm mới Access
  Token bằng Refresh Token mà tôi không phải đăng nhập lại (F1-03).
- **Cho** tôi đang đăng nhập, **Khi** tôi đăng xuất, **Thì** Refresh Token của phiên đó bị vô hiệu hoá
  (F1-04).
- **Cho** tôi chưa có tài khoản, **Khi** tôi đăng nhập bằng GitHub hoặc Google, **Thì** tài khoản được tạo tự
  động với vai trò `STUDENT` nếu email đó chưa từng đăng ký; **Cho** email do OAuth trả về khớp với một tài
  khoản email/mật khẩu đã có, **Khi** tôi đăng nhập bằng OAuth lần đầu, **Thì** hệ thống tự liên kết vào
  chính tài khoản đó — tôi đăng nhập được bằng cả hai cách vào cùng một tài khoản (F1-15).

### US-A1-02: Tìm và chọn bài toán

Là **sinh viên**, tôi muốn tìm bài toán theo chủ đề và độ khó, để chọn đúng bài cần luyện.

- **Cho** danh sách bài toán đã có, **Khi** tôi lọc theo chủ đề, độ khó, hoặc trạng thái đã giải, **Thì**
  danh sách chỉ hiện những bài khớp bộ lọc (F2-11).
- **Cho** tôi thuộc một lớp được giảng viên giao bài, **Khi** tôi mở danh sách bài toán, **Thì** các bài đã
  gán cho lớp tôi hiện thành một nhóm riêng ngay trong danh sách, không cần mở màn khác (F2-12).

### US-A1-03: Viết mã và chạy thử

Là **sinh viên**, tôi muốn chạy thử mã của mình với testcase mẫu trước khi nộp, để biết mã có chạy đúng ý
không mà chưa tốn một lần nộp thật.

- **Cho** tôi đang xem chi tiết một bài toán, **Khi** tôi chọn một trong ba ngôn ngữ (Java, C++, Python),
  **Thì** trình soạn thảo hiện mã khung khớp chữ ký hàm của bài bằng ngôn ngữ đó (F2-03).
- **Cho** tôi đã viết mã, **Khi** tôi bấm Chạy thử, **Thì** hệ thống chạy mã với testcase Sample và trả kết
  quả kèm dữ liệu vào/ra thấy được, **không** ghi nhận vào tiến độ của tôi (F4-02).
- **Cho** bài toán dùng mô hình bọc hàm nhưng kiểu dữ liệu của bài chưa được lược đồ F3 hỗ trợ, **Khi** tôi
  mở bài đó, **Thì** hệ thống dùng mô hình Standard I/O cho bài này — tôi tự đọc `stdin`/ghi `stdout` (F3-13).
- **Cho** tôi đã có sẵn file mã nguồn trên máy, **Khi** tôi tải file đó lên thay vì gõ trực tiếp, **Thì** nội
  dung file nạp vào trình soạn thảo và tôi nộp/chạy thử được như bình thường (F4-01).

### US-A1-04: Nộp bài và xem kết quả theo thời gian thực

Là **sinh viên**, tôi muốn nộp bài và thấy kết quả từng testcase ngay khi có, để biết mình sai ở đâu sớm
nhất có thể, không phải chờ toàn bộ chấm xong.

- **Cho** tôi đã sửa mã sau khi chạy thử, **Khi** tôi bấm Nộp bài, **Thì** hệ thống trả về ngay một
  `submissionId` và không bắt tôi chờ màn hình (F4-01).
- **Cho** bài nộp đang được chấm, **Khi** một testcase ẩn có kết quả, **Thì** trạng thái testcase đó cập
  nhật trên giao diện của tôi qua WebSocket, không cần tôi tải lại trang (F4-08).
- **Cho** một testcase ẩn sai hoặc lỗi theo điều kiện dừng, **Khi** hệ thống phát hiện, **Thì** các testcase
  ẩn còn lại của bài nộp đó không được chạy tiếp (F4-04) — tôi thấy trạng thái dừng ở đúng testcase đó.
- **Cho** một testcase ẩn sai, **Khi** tôi xem kết quả, **Thì** tôi chỉ thấy **trạng thái và chỉ số** của
  testcase đó, không thấy dữ liệu vào và không thấy diff chi tiết (F2-08).
- **Cho** toàn bộ testcase ẩn đều đúng, **Khi** bài nộp đạt `Accepted`, **Thì** giao diện mở ra hai lựa chọn:
  Phân tích bài giải hoặc Phỏng vấn giả lập (F5-01, F5-09).

### US-A1-05: Xem tiến độ cá nhân và quản lý hồ sơ

Là **sinh viên**, tôi muốn xem lại quá trình luyện tập của mình và sửa thông tin cá nhân, để theo dõi mình
đã tiến bộ ra sao.

- **Cho** tôi đã nộp một số bài, **Khi** tôi mở trang tiến độ cá nhân, **Thì** tôi thấy bài đã giải theo chủ
  đề (F1-06) và tỉ lệ chấp thuận — số bài đạt `Accepted` trên tổng số đã nộp (F1-07).
- **Cho** tôi đã có phiên phỏng vấn giả lập trước đó, **Khi** tôi mở lịch sử phỏng vấn, **Thì** tôi mở lại
  được đúng rubric của phiên đó (F1-08).
- **Cho** thông tin cá nhân của tôi sai hoặc cần cập nhật, **Khi** tôi sửa ở trang hồ sơ, **Thì** thông tin
  mới được lưu (F1-09) `[SoT: Suy luận — README.md không nêu, nhưng đăng ký mà không sửa được gì là thiếu]`.
- **Cho** tôi muốn ngừng sử dụng hệ thống hẳn, **Khi** tôi bấm xoá tài khoản ở khu "danger zone" của trang
  Cài đặt và xác nhận, **Thì** tài khoản chuyển trạng thái `DEACTIVATED` ngay, tôi không đăng nhập lại được
  nữa; sau một khoảng ân hạn, thông tin định danh của tôi (email, tên hiển thị) bị ẩn danh hoá, còn bài nộp,
  bài đã lưu, và phiên phỏng vấn của tôi vẫn được giữ lại nhưng không còn gắn với danh tính cá nhân (F1-16).

### US-A1-06: Nhận phân tích bài giải sau khi Accepted

Là **sinh viên**, tôi muốn được phân tích bài giải của mình sau khi đã đúng, để biết bài giải đã tối ưu chưa
và còn thiếu gì.

- **Cho** bài nộp của tôi đã `Accepted`, **Khi** tôi yêu cầu phân tích bài giải, **Thì** hệ thống trả về báo
  cáo có cấu trúc: độ phức tạp thời gian/bộ nhớ thực tế kèm lập luận (F5-02), đối chiếu với độ phức tạp tối
  ưu đã biết (F5-03), trường hợp biên chưa phủ và rủi ro tràn số (F5-04), nhận xét chất lượng mã (F5-05), và
  câu hỏi mở rộng để tôi tự củng cố (F5-06).
- **Cho** tôi đã nộp đúng nguyên mã nguồn đó trước đây và đã có báo cáo, **Khi** tôi yêu cầu phân tích lại,
  **Thì** hệ thống trả kết quả từ cache theo hash mã nguồn, không gọi lại API AI (F5-20).
- **Cho** báo cáo đã được sinh, **Khi** tôi rời trang rồi quay lại trang tiến độ, **Thì** tôi tra cứu lại
  được đúng báo cáo đó kèm bài nộp (F5-08).
- **Cho** báo cáo có đề xuất một đoạn mã cải tiến, **Khi** tôi bấm áp dụng đoạn mã đó vào Workspace, **Thì**
  hệ thống hỏi xác nhận trước khi ghi đè, và giữ lại bản mã cũ của tôi để khôi phục lại được nếu tôi đổi ý
  (F5-26).

### US-A1-07: Phỏng vấn giả lập bảo vệ bài giải

Là **sinh viên**, tôi muốn được phỏng vấn giả lập về bài giải của mình, để luyện kỹ năng giải trình như một
buổi phỏng vấn thật.

- **Cho** bài nộp của tôi đã `Accepted`, **Khi** tôi mở phỏng vấn giả lập, **Thì** phiên bắt đầu ở giai đoạn
  giải trình thuật toán — tôi trình bày ý tưởng và lý do chọn cấu trúc dữ liệu (F5-09, F5-10).
- **Cho** tôi đã trình bày xong giai đoạn 1, **Khi** phiên chuyển giai đoạn, **Thì** AI chất vấn điểm chưa
  tối ưu và hỏi vào trường hợp biên (F5-11), rồi đặt tình huống quy mô dữ liệu tăng đột biến ở giai đoạn cuối
  (F5-12).
- **Cho** phiên đang diễn ra, **Khi** AI trả lời, **Thì** phản hồi hiện dần trên giao diện qua SSE, không
  phải chờ trọn câu trả lời mới thấy (F5-14).
- **Cho** phiên kết thúc đủ ba giai đoạn, **Khi** tôi xem kết quả, **Thì** tôi thấy bảng đánh giá rubric kèm
  nhận xét từng tiêu chí, và giao diện ghi rõ đây là **phản hồi hỗ trợ học tập, không phải điểm số chính
  thức** (F5-15, F5-18).
- **Cho** AI hết quota hoặc lỗi giữa phiên, **Khi** sự cố xảy ra, **Thì** phiên kết thúc có kiểm soát, tôi
  nhận thông báo rõ ràng, và **bài nộp cùng kết quả chấm của tôi không bị ảnh hưởng** (F5-22).
- **Cho** tôi chưa có bài nộp `Accepted` nào phù hợp muốn luyện, **Khi** tôi chọn mở phiên từ kho câu hỏi có
  sẵn hoặc tự chọn một hoặc nhiều chủ đề cùng mức độ và ngôn ngữ, **Thì** phiên vẫn chạy đủ ba giai đoạn như
  lối vào từ bài nộp, chỉ khác nguồn đề bài nạp vào (F5-24).

### US-A1-08: Ôn tập ngân hàng câu hỏi phỏng vấn

Là **sinh viên**, tôi muốn ôn câu hỏi phỏng vấn lý thuyết độc lập với việc nộp code, để chuẩn bị cho cả phần
không phải viết mã của buổi phỏng vấn thật.

- **Cho** tôi mở ngân hàng câu hỏi, **Khi** tôi lọc theo chủ đề hoặc mức độ khó, **Thì** danh sách chỉ hiện
  câu hỏi khớp (F6-01, F6-02); tôi đánh dấu được câu hỏi để xem lại sau (F6-03).
- **Cho** tôi chọn Chế độ học, **Khi** tôi mở một câu hỏi, **Thì** tôi thấy gợi ý hướng tiếp cận, khung trả
  lời chuẩn (áp dụng STAR nếu là câu hỏi hành vi), và danh sách từ khoá kỹ thuật cốt lõi (F6-04, F6-05,
  F6-06).
- **Cho** tôi chọn Chế độ luyện, **Khi** tôi tự soạn câu trả lời và gửi, **Thì** AI đối chiếu với tiêu chí
  chuẩn của câu hỏi và trả về điểm đã đạt, điểm còn thiếu, hướng bổ sung (F6-07, F6-08).
- **Cho** tôi đã luyện một số câu, **Khi** tôi mở trang theo dõi, **Thì** tôi thấy lịch sử luyện tập, danh
  sách câu cần ôn lại, và tỉ lệ hoàn thành theo chủ đề (F6-09, F6-10).
- **Cho** tôi vừa xem gợi ý/khung trả lời chuẩn ở Chế độ học, **Khi** tôi tự chấm mức độ nhớ câu hỏi đó (Biết
  rõ / Mơ hồ / Quên), **Thì** hệ thống dùng lần tự chấm gần nhất để xếp lại câu hỏi nào nên hiện sớm hơn
  trong danh sách cần ôn lại (F6-12).

### US-A1-09: Lưu bài toán kèm ghi chú riêng tư

Là **sinh viên**, tôi muốn đánh dấu bài toán để xem lại sau kèm ghi chú riêng, để không quên những bài mình
định làm lại hoặc những điều mình nhận ra khi giải.

- **Cho** tôi đang xem một bài toán, **Khi** tôi bấm lưu (bookmark), **Thì** bài đó xuất hiện trong danh sách
  "Bài đã lưu" của tôi, kèm ô ghi chú tự do tôi có thể sửa bất cứ lúc nào (F2-13).
- **Cho** tôi đã ghi chú vào một bookmark, **Khi** người khác (kể cả giảng viên hoặc quản trị viên) xem cùng
  bài toán đó, **Thì** họ không thấy được ghi chú của tôi — ghi chú riêng tư tuyệt đối theo tài khoản
  (F2-13).
- **Cho** tôi đăng nhập từ một thiết bị khác, **Khi** tôi mở "Bài đã lưu", **Thì** tôi thấy đúng danh sách và
  ghi chú như trên thiết bị cũ, vì dữ liệu lưu ở server (F2-13).

---

## 2. A2 — Giảng viên

### US-A2-01: Soạn bài toán và đặc tả hàm

Là **giảng viên**, tôi muốn soạn đề bài và khai báo đặc tả hàm, để sinh viên luyện đúng bài tôi thiết kế và
để hệ thống sinh mã khung đúng.

- **Cho** tôi tạo bài toán mới, **Khi** tôi soạn đề bài, **Thì** tôi viết được bằng Markdown kèm công thức
  LaTeX, và phân loại bài theo độ khó và chủ đề (F2-01, F2-02).
- **Cho** đề bài đã có, **Khi** tôi khai báo đặc tả bài toán, **Thì** tôi khai được chữ ký hàm riêng cho từng
  ngôn ngữ trong ba ngôn ngữ, kiểu tham số và kiểu trả về (F2-03) — đây là đầu vào bắt buộc để F3 sinh mã
  khung.
- **Cho** đặc tả đã có, **Khi** tôi chọn chiến lược so khớp kết quả, **Thì** tôi chọn được một trong
  `EXACT`/`TRIMMED`/`EPSILON`/`UNORDERED_SET` cho bài đó (F2-04).

### US-A2-02: Quản lý testcase và giới hạn tài nguyên

Là **giảng viên**, tôi muốn tạo và quản lý testcase cùng giới hạn tài nguyên của bài toán, để việc chấm diễn
ra đúng và công bằng giữa các ngôn ngữ.

- **Cho** bài toán đã có đặc tả, **Khi** tôi tạo testcase, **Thì** tôi tạo được testcase Sample (công khai,
  dùng cho Chạy thử) và testcase Hidden (ẩn, dùng cho Nộp bài) (F2-05, F2-06).
- **Cho** bộ testcase lớn, **Khi** tôi tải lên theo lô, **Thì** hệ thống lưu bộ lớn trên MinIO mà không chặn
  giao diện (F2-07).
- **Cho** tôi cần đặt giới hạn thời gian và bộ nhớ, **Khi** tôi cấu hình cho bài toán, **Thì** tôi đặt được
  hệ số nhân riêng theo ngôn ngữ (Java cần hệ số cao hơn C++ cho cùng một bài) (F2-10).
- **Cho** tôi sửa bộ testcase sau khi đã có người nộp bài, **Khi** tôi lưu thay đổi, **Thì** hệ thống tăng
  phiên bản bộ testcase để việc chấm lại biết chấm theo phiên bản nào (F2-09).

### US-A2-03: Giao bài theo lớp và yêu cầu chấm lại

Là **giảng viên**, tôi muốn giao bài theo lớp mình phụ trách và yêu cầu chấm lại khi cần, để quản lý bài tập
của lớp và sửa sai nếu testcase từng có lỗi.

- **Cho** bài toán đã công bố, **Khi** tôi giao bài cho một lớp, **Thì** chỉ sinh viên của lớp đó thấy bài
  được giao (F2-12).
- **Cho** một phiên bản bộ testcase mới đã có, **Khi** tôi chọn phạm vi chấm lại theo bài toán của lớp mình,
  **Thì** tôi thấy ước lượng số lượt nộp bị ảnh hưởng và thời gian dự kiến trước khi bấm chạy thật (F4-09a,
  F4-09b) — tôi không chọn được phạm vi ngoài lớp mình phụ trách (theo ma trận quyền F1-10).
- **Cho** phiên chấm lại đã chạy xong, **Khi** kết quả một bài nộp thay đổi, **Thì** hệ thống không hạ điểm
  đã công bố nếu điểm mới thấp hơn, trừ khi tôi tắt tuỳ chọn đó (F4-09c).

### US-A2-04: Tạo bộ câu hỏi phỏng vấn riêng cho lớp

Là **giảng viên**, tôi muốn tạo bộ câu hỏi phỏng vấn riêng và gán cho lớp, để lớp tôi ôn đúng trọng tâm tôi
muốn.

- **Cho** tôi đã chọn một tập câu hỏi từ ngân hàng, **Khi** tôi tạo bộ câu hỏi riêng và gán cho lớp, **Thì**
  sinh viên trong lớp đó thấy bộ câu hỏi này (F6-11).

### US-A2-05: Nhờ AI sinh testcase tự động, output lấy từ chạy thật đáp án mẫu

Là **giảng viên**, tôi muốn nhờ AI sinh thêm testcase khi soạn đề, để rút ngắn thời gian tự viết tay nhiều
input mà vẫn tin tưởng được output là chính xác.

- **Cho** bài toán đã có Đáp án mẫu chạy Pass với testcase hiện có, **Khi** tôi bấm "Sinh tự động" ở màn
  Soạn đề bài, **Thì** AI sinh ra các input dựa trên đề bài và Ràng buộc dữ liệu tôi đã khai báo, hệ thống
  chạy từng input đó qua Đáp án mẫu trên go-judge để lấy output thật, rồi thêm cặp input/output vào danh
  sách testcase ở trạng thái nháp (F2-14).
- **Cho** bài toán chưa có Đáp án mẫu chạy Pass, **Khi** tôi bấm "Sinh tự động", **Thì** hệ thống từ chối và
  báo tôi cần có đáp án mẫu hợp lệ trước (F2-14) — không có gì để chạy input qua nên không sinh testcase.
- **Cho** testcase do AI sinh đã nằm trong danh sách nháp, **Khi** tôi chưa xác nhận, **Thì** testcase đó
  chưa được gộp vào bộ Hidden dùng để chấm bài (F2-14) — tôi luôn là người quyết định cuối cùng có dùng hay
  không.

---

## 3. A3 — Quản trị viên

### US-A3-01: Quản lý ma trận phân quyền

Là **quản trị viên**, tôi muốn cấu hình ma trận phân quyền theo vai trò và chức năng, để kiểm soát ai được
làm gì trong khu vực quản trị mà không phải sửa mã.

- **Cho** tôi mở màn ma trận phân quyền, **Khi** tôi tích/bỏ tích một ô `CREATE`/`READ`/`UPDATE`/`DELETE`
  cho một cặp (vai trò, chức năng), **Thì** quyền đó có hiệu lực ngay, không cần khởi động lại dịch vụ
  (F1-10).
- **Cho** danh sách Function/Action là dữ liệu seed cố định, **Khi** tôi mở màn ma trận, **Thì** tôi thấy
  chúng ở dạng chỉ đọc; tôi tạo/sửa/xoá được Role, nhưng **không xoá được** vai trò hệ thống
  (`STUDENT`/`INSTRUCTOR`/`ADMIN`) hoặc vai trò đang có người dùng (F1-11).
- **Cho** tôi đổi một ô trong ma trận, **Khi** thay đổi được lưu, **Thì** hệ thống ghi vào Nhật ký hệ thống
  ai đổi, đổi gì, đổi lúc nào — không có ngoại lệ (F1-14).

### US-A3-02: Quản lý tài khoản người dùng

Là **quản trị viên**, tôi muốn quản lý tài khoản người dùng, để xử lý khi có sự cố tài khoản (quên mật khẩu,
vi phạm quy định...).

- **Cho** một tài khoản cần xử lý, **Khi** tôi đổi vai trò, khoá/mở khoá, hoặc reset mật khẩu tài khoản đó,
  **Thì** thay đổi có hiệu lực ngay và được ghi vào Nhật ký hệ thống (F1-13, F1-14) — hành động này bị gác
  bởi quyền `USER_MANAGEMENT` trong ma trận F1-10.

### US-A3-03: Giám sát cụm judge engine và hàng đợi

Là **quản trị viên**, tôi muốn giám sát hàng đợi bài nộp và tình trạng cụm judge engine, để biết hệ thống có
đang khoẻ không và xử lý kịp khi có bài nộp treo.

- **Cho** tôi mở bảng giám sát, **Khi** tôi xem, **Thì** tôi thấy độ dài hàng đợi, số bài nộp đang chạy, và
  tình trạng cụm judge engine (mặc định go-judge) (F4-10).
- **Cho** một bài nộp bị treo quá ngưỡng thời gian, **Khi** timeout sweep phát hiện, **Thì** tôi thấy nó
  trên bảng giám sát và kích hoạt chấm lại nếu cần (F4-07, F4-09a).

### US-A3-04: Cấu hình ngôn ngữ, giới hạn tài nguyên, và AI

Là **quản trị viên**, tôi muốn cấu hình ngôn ngữ, giới hạn tài nguyên mặc định, và prompt/rubric của AI, để
vận hành hệ thống đúng với năng lực hạ tầng thật.

- **Cho** tôi mở cấu hình hệ thống, **Khi** tôi đổi cấu hình ngôn ngữ hoặc giới hạn tài nguyên mặc định,
  **Thì** thay đổi áp dụng cho các bài toán mới hoặc theo phạm vi tôi chọn (F4-11).
- **Cho** tôi mở cấu hình AI, **Khi** tôi sửa prompt hoặc trọng số rubric, **Thì** một phiên bản mới được
  lưu lại (kèm lịch sử các phiên bản trước) và dùng cho các yêu cầu AI tiếp theo — tôi xem lại hoặc khôi
  phục được một phiên bản cũ nếu bản mới có vấn đề (F5-23).
- **Cho** tôi muốn kiểm soát chi phí AI, **Khi** tôi xem trang theo dõi token, **Thì** tôi thấy lượng token
  đã tiêu thụ, ngân sách còn lại, dự báo thời điểm cạn quota, và bảng xếp hạng người dùng/bài toán tốn token
  nhiều nhất, để biết chỗ cần điều chỉnh giới hạn tần suất gọi (F5-19, F5-21, F5-25).
- **Cho** ngân sách token đã cạn, **Khi** một `STUDENT` hoặc `INSTRUCTOR` gọi một tính năng AI, **Thì** lời
  gọi bị tạm khoá và họ nhận thông báo rõ ràng; **Cho** tôi là `ADMIN`, **Khi** ngân sách cạn, **Thì** tôi
  không bị khoá và là người duy nhất mở lại hoặc tăng ngân sách (F5-25).

### US-A3-05: Vận hành phiên chấm lại quy mô lớn

Là **quản trị viên**, tôi muốn vận hành một phiên chấm lại quy mô toàn hệ thống (ví dụ nâng cấp trình biên
dịch), để áp dụng thay đổi cho hàng nghìn bài nộp cũ một cách an toàn và có thể kiểm soát giữa chừng.

- **Cho** tôi chọn phạm vi "theo khoảng thời gian" hoặc toàn hệ thống, **Khi** tôi xem ước lượng, **Thì**
  tôi thấy số lượt nộp bị ảnh hưởng và có thể "chạy thử" trên một mẫu nhỏ trước khi cam kết chạy toàn bộ
  (F4-09a, F4-09b).
- **Cho** một phiên chấm lại đang chạy, **Khi** tôi phát hiện vấn đề, **Thì** tôi tạm dừng hoặc huỷ được
  ngay, và lượt nộp đang chấm dở không bị chen vào giữa chừng (F4-09d).
- **Cho** một phiên chấm lại đã hoàn tất, tạm dừng, hoặc bị huỷ, **Khi** tôi mở lịch sử chấm lại, **Thì** tôi
  thấy ai chạy, phạm vi nào, lý do gì, và bao nhiêu lượt nộp đổi kết quả — đồng thời bản ghi này cũng nằm
  trong Nhật ký hệ thống chung (F4-09e, F1-14).

Các mục dưới đây là hành vi **hệ thống phải tự thực hiện** mà không do ai bấm — job định kỳ và consumer lấy
việc từ hàng đợi. Giữ dạng "Cho/Khi/Thì" để nhất quán với phần trên, nhưng chủ thể là hệ thống, không phải
người.

### US-A4-01: Sinh mã bọc hàm cho bài nộp (F3)

- **Cho** một đặc tả bài toán và mã nguồn người dùng cho một trong ba ngôn ngữ, **Khi** hệ thống cần chấm
  bài nộp đó, **Thì** hệ thống sinh mã đọc dữ liệu vào, gọi hàm người dùng, và in kết quả từ đúng một đặc tả
  (F3-01 tới F3-04), tiêm mã người dùng vào vùng chèn của mã khung (F3-05), và đóng gói theo đúng định dạng
  mà adapter engine đang dùng yêu cầu — không hardcode Base64, vì đó là yêu cầu riêng của Judge0, không phải
  yêu cầu chung của mọi engine (F3-06, `DEC-2026-0823-go-judge-default-engine`).
- **Cho** trình biên dịch báo lỗi ở mã đã ghép, **Khi** hệ thống trả lỗi về cho người học, **Thì** số dòng
  lỗi được ánh xạ về đúng dòng trong mã người dùng, và lỗi thuộc phần mã harness bị che giấu hoàn toàn
  (F3-11, F3-12).

### US-A4-02: Điều phối bài nộp qua judge engine (F4)

- **Cho** một bài nộp mới, **Khi** hệ thống tiếp nhận, **Thì** hệ thống ghi trạng thái `PENDING`, đẩy việc
  vào hàng đợi RabbitMQ, và phản hồi ngay cho người dùng — không chờ kết quả chấm (F4-01).
- **Cho** một testcase cần chấm, **Khi** worker lấy việc khỏi hàng đợi, **Thì** hệ thống gọi
  `JudgeExecutionPort` **một lần cho mỗi testcase** (không dồn batch), qua adapter mặc định là go-judge,
  đồng bộ — nhận kết quả ngay trong lời gọi (F4-03, `DEC-2026-0823-go-judge-default-engine`).
- **Cho** một testcase sai hoặc lỗi theo điều kiện dừng, **Khi** hệ thống phát hiện, **Thì** hệ thống dừng
  gọi các testcase còn lại của bài nộp đó (fail-fast, F4-04).
- **Cho** một kết quả testcase vừa nhận, **Khi** hệ thống ghi nhận, **Thì** hệ thống đẩy trạng thái đó qua
  WebSocket theo kênh riêng của bài nộp ngay sau khi nhận, và **không bao giờ ghi đè một trạng thái đã là
  trạng thái cuối** (F4-08, bất biến giữ nguyên bất kể adapter đồng bộ hay bất đồng bộ).
- **Cho** một message bị xử lý lại do RabbitMQ redeliver (worker crash giữa lúc xử lý), **Khi** hệ thống xử
  lý lại message đó, **Thì** kết quả cuối không đổi so với nếu chỉ xử lý một lần (idempotency theo bất biến
  trạng thái cuối, không phải theo token — token chỉ cần nếu một adapter bất đồng bộ như Judge0 được cắm lại
  sau này, F4-05/F4-06).
- **Cho** một bài nộp treo ở trạng thái trung gian quá ngưỡng thời gian, **Khi** timeout sweep định kỳ chạy,
  **Thì** hệ thống phát hiện và đánh dấu để quản trị viên xử lý (F4-07) — bản nhẹ, dựa vào ack/nack và
  redelivery của RabbitMQ, không cần chủ động hỏi lại một hệ ngoài như khi dùng Judge0.

### US-A4-03: Bảo vệ testcase ẩn (F2-08)

- **Cho** một testcase ẩn được dùng để chấm, **Khi** hệ thống trả kết quả qua bất kỳ API hoặc ghi log,
  **Thì** chỉ trạng thái và chỉ số của testcase được lộ ra — không bao giờ lộ dữ liệu vào hoặc diff chi
  tiết.

### US-A4-04: Sinh phản hồi AI và điều phối phiên phỏng vấn (F5)

- **Cho** một bài nộp đã `Accepted` và người dùng yêu cầu phân tích, **Khi** hệ thống xử lý, **Thì** hệ
  thống kiểm cache theo hash mã nguồn trước (F5-20); nếu chưa có, gọi AI, nhận JSON theo lược đồ, và lưu kèm
  bài nộp (F5-01 tới F5-08).
- **Cho** một phiên phỏng vấn giả lập đang mở, **Khi** hệ thống điều phối ba giai đoạn, **Thì** hệ thống giữ
  ngữ cảnh phiên qua `ChatMemory` trên Redis xuyên cả ba giai đoạn (F5-13) và stream phản hồi AI qua SSE
  (F5-14).
- **Cho** mọi lời gọi AI ở F5 hoặc F6, **Khi** hệ thống dựng prompt, **Thì** mã nguồn và câu trả lời của
  người dùng luôn được đưa vào dưới dạng **tham số dữ liệu**, tách hoàn toàn khỏi chỉ thị hệ thống — không
  bao giờ ghép trực tiếp vào chuỗi chỉ thị (F5-17, chống prompt injection).
- **Cho** phân hệ AI gặp sự cố hoặc hết quota, **Khi** hệ thống phát hiện, **Thì** F1-F4 vẫn tiếp tục hoạt
  động bình thường — không có lời gọi AI nào nằm trong luồng ghi nhận kết quả chấm (F5-22).

### US-A4-05: Đối chiếu câu trả lời ở ngân hàng câu hỏi (F6-08)

- **Cho** người dùng gửi câu trả lời ở Chế độ luyện, **Khi** hệ thống xử lý, **Thì** hệ thống gọi qua phân
  hệ AI (không tự gọi LLM riêng) để đối chiếu với tiêu chí chuẩn của câu hỏi, và trả về điểm đã đạt, điểm
  còn thiếu, hướng bổ sung — chịu chung ràng buộc chống prompt injection và suy giảm có kiểm soát như F5.

---

## 5. Câu hỏi mở

Giữ nguyên tinh thần "ghi ra thay vì lặng lẽ giả định" của `rd-analysis`:

| # | Câu hỏi | Vì sao chưa trả lời được | Người trả lời |
| :--- | :--- | :--- | :--- |
| Q1 | Sinh viên có sửa được câu trả lời đã nộp ở Chế độ luyện (F6-07) sau khi đã nhận phản hồi AI không, hay mỗi lần chỉ nộp một lần? | `README.md` không nêu chi tiết luồng này | Chủ nhiệm đề tài |
| ~~Q2~~ | ~~Giảng viên yêu cầu chấm lại (F4-09a) có giới hạn số lần hoặc cần quản trị viên duyệt không?~~ **Đã trả lời 2026-08-24: không cần duyệt, chỉ cần audit.** Phạm vi giảng viên vốn đã bị chặn trong lớp mình phụ trách (ma trận quyền F1-10, xem `US-A2-03`); thao tác quy mô lớn (toàn hệ thống) tách riêng cho quản trị viên (`US-A3-05`) và đã có dry-run + tạm dừng/huỷ giữa chừng (F4-09b, F4-09d) làm lớp an toàn trước/trong khi chạy. Audit trail (F4-09e) chỉ cần để truy vết sau khi chạy, không cần thêm bước duyệt trước. | (đã trả lời) | Chủ nhiệm đề tài |
| Q3 | US-A1-06 giả định "yêu cầu phân tích" là hành động chủ động của sinh viên (bấm nút) — có cần tự động gợi ý ngay khi `Accepted` hay chỉ hiện lựa chọn? | Suy luận từ `README.md` mục 2, chưa có màn hình cụ thể để xác nhận | Người viết BD màn `submission_result` |
| Q4 | Ngưỡng thời gian cụ thể để timeout sweep (F4-07) coi một bài nộp là "treo" chưa được chốt số liệu | `environment.md`/`README.md` không có con số, chỉ có nguyên lý | Chủ nhiệm đề tài, chốt khi viết DD cho F4 |

---

Đọc thêm: yêu cầu ở mức đặc tả kỹ thuật `01-rd/req/req.md` · từ vựng chuẩn `01-rd/overview/glossary.md` ·
bảng chức năng đầy đủ `01-rd/overview/system_survey.md` mục 5.
