# A1 — Sinh viên / người dùng cuối

> Tách ra từ `01-rd/req/user_stories.md` ngày 2026-08-31 để dễ đọc (theo actor). Nội dung dưới đây được
> chuyển nguyên văn từ `user_stories.md` (không đổi ý nghĩa, không thêm/bớt), chỉ hạ heading `##` xuống `#`
> và `###` giữ nguyên cho từng story. Ký hiệu và quy ước INVEST/Given-When-Then: xem
> `01-rd/req/user_stories.md` (file mục lục).

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
- **Cho** tôi quên mật khẩu, **Khi** tôi bấm "Quên mật khẩu?" và nhập email, **Thì** hệ thống gửi một mã 6
  chữ số ngẫu nhiên tới email đó qua Gmail, và luôn báo một câu chung ("nếu email tồn tại, mã đã được gửi")
  bất kể email có tài khoản hay không (F1-17).
- **Cho** mã 6 số đã được gửi, **Khi** tôi nhập đúng mã trong thời hạn hiệu lực, **Thì** tôi được đặt mật
  khẩu mới, và mã đó không dùng lại được nữa (F1-17).
- **Cho** mã 6 số đã hết hạn hoặc tôi nhập sai quá số lần cho phép, **Khi** tôi nhập mã, **Thì** hệ thống từ
  chối và yêu cầu tôi bấm gửi lại mã mới (F1-17).
- **Cho** tài khoản của tôi chỉ từng đăng nhập bằng GitHub/Google, chưa bao giờ đặt mật khẩu, **Khi** tôi
  bấm "Quên mật khẩu?" và nhập email đó, **Thì** hệ thống không gửi mã 6 số mà gửi một email báo tài khoản
  đang đăng nhập bằng OAuth và hướng dẫn tôi quay lại đăng nhập bằng đúng provider cũ (F1-17).

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
- **Cho** một bài toán bất kỳ, **Khi** tôi mở bài đó, **Thì** tôi thấy cả hai lựa chọn mô hình nộp bài —
  Bọc hàm và Standard I/O — và tự chọn mô hình nào cũng được, chuyển đổi qua lại không mất phần đã viết ở
  mô hình kia (F3-13, sửa 2026-08-24 theo `DEC-2026-0824-dual-submission-model-per-problem`).
- **Cho** bài toán có kiểu dữ liệu vượt quá lược đồ kiểu của F3, **Khi** tôi mở bài đó, **Thì** tuỳ chọn Bọc
  hàm bị ẩn, tôi chỉ thấy và dùng được Standard I/O — tự đọc `stdin`/ghi `stdout` (F3-13).
- **Cho** tôi đã có sẵn file mã nguồn trên máy, **Khi** tôi tải file đó lên thay vì gõ trực tiếp, **Thì** nội
  dung file nạp vào trình soạn thảo và tôi nộp/chạy thử được như bình thường (F4-01).

### US-A1-04: Nộp bài và xem kết quả theo thời gian thực

Là **sinh viên**, tôi muốn nộp bài và thấy kết quả từng testcase ngay khi có, để biết mình sai ở đâu sớm
nhất có thể, không phải chờ toàn bộ chấm xong.

- **Cho** tôi đã sửa mã sau khi chạy thử, **Khi** tôi bấm Nộp bài, **Thì** hệ thống trả về ngay một
  `submissionId` và không bắt tôi chờ màn hình (F4-01).
- **Cho** bài nộp đang được chấm, **Khi** một testcase ẩn có kết quả, **Thì** trạng thái testcase đó cập
  nhật trên giao diện của tôi qua WebSocket, không cần tôi tải lại trang (F4-08).
- **Cho** một testcase ẩn sai hoặc lỗi, **Khi** hệ thống ghi nhận, **Thì** các testcase ẩn còn lại **vẫn
  được chạy tiếp** — tôi thấy đủ trạng thái của toàn bộ testcase và một điểm tỷ lệ testcase đạt (F4-13).
  **Sửa 2026-09-05:** tiêu chí này trước ghi ngược lại — "các testcase còn lại không được chạy tiếp
  (F4-04)". `F4-04` (fail-fast) đã hết hiệu lực theo `DEC-2026-0831-partial-score-testcase-ratio`, và bản
  cũ **mâu thuẫn trực tiếp** với tiêu chí tương ứng ở `01-rd/req/user_stories/a4_system.md` (`US-A4-02`) vốn
  đã sửa đúng từ 2026-08-31. Nếu không sửa, test sinh ra từ tiêu chí này sẽ khẳng định đúng cái ngược lại
  với hành vi thật.
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
- **Cho** tôi đã đăng nhập, **Khi** tôi nhập đúng mật khẩu hiện tại kèm mật khẩu mới ở mục Bảo mật của trang
  cá nhân, **Thì** mật khẩu được đổi ngay, không cần xác nhận qua email (F1-19) — khác luồng quên mật khẩu
  (F1-17) chỉ dùng khi chưa đăng nhập.
- **Cho** tôi muốn xem lại toàn bộ lượt nộp của mình, **Khi** tôi mở trang "Bài đã nộp" và lọc theo kết quả
  hoặc ngôn ngữ, hoặc tìm theo tên/mã bài, **Thì** danh sách chỉ hiện đúng các lượt nộp khớp điều kiện,
  kèm liên kết mở kết quả chi tiết và (nếu `Accepted`) mở phân tích bài giải (F1-18).
- **Cho** tôi mở trang Cài đặt, **Khi** tôi đổi ngôn ngữ mặc định/cỡ chữ editor/tự lưu bản nháp/phím tắt Vim
  của Workspace, **Thì** lựa chọn được lưu theo tài khoản và áp dụng lần sau tôi mở một bài toán để giải
  (F1-20).
- **Cho** tôi mở trang Cài đặt, **Khi** tôi bật thông báo nhắc luyện tập hoặc báo cáo hằng tuần, **Thì** tôi
  nhận đúng email đó theo lịch đã định (F1-21).
- **Cho** tôi mở trang Cài đặt, **Khi** tôi bấm xuất lượt nộp hoặc xuất hội thoại phỏng vấn, **Thì** tôi nhận
  được file chỉ chứa dữ liệu của chính tôi, đúng định dạng đã chọn (CSV/JSON) (F1-22).

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
- **Cho** tôi mở phiên tự luyện (không phải từ bài nộp `Accepted`), **Khi** tôi đã đặt trước ở trang Cài đặt
  mức người phỏng vấn, số lượt tối đa, và có cho phép gợi ý hay không, **Thì** phiên áp đúng các tham số đó;
  phiên mở từ một bài nộp `Accepted` không dùng các tham số tự chỉnh này (F5-28).

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
