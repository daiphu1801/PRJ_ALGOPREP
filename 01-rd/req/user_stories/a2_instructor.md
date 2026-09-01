# A2 — Giảng viên

> Tách ra từ `01-rd/req/user_stories.md` ngày 2026-08-31 để dễ đọc (theo actor). Nội dung dưới đây được
> chuyển nguyên văn từ `user_stories.md` (không đổi ý nghĩa, không thêm/bớt), chỉ hạ heading `##` xuống `#`
> và `###` giữ nguyên cho từng story. Ký hiệu và quy ước INVEST/Given-When-Then: xem
> `01-rd/req/user_stories.md` (file mục lục).

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

### US-A2-01b: Xuất bản và rút xuống bài toán

Là **giảng viên hoặc quản trị viên**, tôi muốn chuyển một bài toán giữa `Chưa xuất bản` và `Đã xuất bản`, để
kiểm soát bài nào học viên nhìn thấy trong ngân hàng bài toán (F2-15, bổ sung 2026-08-30 —
`DEC-2026-0830-problem-lifecycle-two-states`).

- **Cho** một bài toán mới tạo, **Khi** tôi chưa xuất bản, **Thì** bài ở trạng thái `Chưa xuất bản` và không
  xuất hiện ở `problem_list` của học viên (F2-15).
- **Cho** một bài toán `Chưa xuất bản` đã có testcase Hidden và đặc tả đủ theo mô hình nộp bài, **Khi** tôi
  bấm xuất bản, **Thì** bài chuyển sang `Đã xuất bản` và xuất hiện ngay ở `problem_list` (F2-15).
- **Cho** một bài toán `Chưa xuất bản` còn thiếu testcase Hidden hoặc thiếu đặc tả, **Khi** tôi cố xuất bản,
  **Thì** hệ thống chặn và nêu rõ lý do thiếu gì (F2-15).
- **Cho** một bài toán `Đã xuất bản` đã có lượt nộp, **Khi** tôi rút xuống, **Thì** bài quay về `Chưa xuất
  bản`, biến mất khỏi `problem_list`, nhưng lượt nộp cũ, bookmark và phiên phỏng vấn liên quan của học viên
  không bị xoá (F2-15).

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
  phiên bản bộ testcase để biết lượt nộp cũ đã chấm theo phiên bản nào (F2-09). **Cập nhật 2026-08-28:**
  không có luồng tự động chấm lại các lượt nộp cũ theo phiên bản mới — xem `DEC-2026-0828-remove-rejudge-scope`.

### US-A2-03: Giao bài theo lớp

Là **giảng viên**, tôi muốn giao bài theo lớp mình phụ trách, để quản lý bài tập của lớp.

- **Cho** bài toán đã công bố, **Khi** tôi giao bài cho một lớp, **Thì** chỉ sinh viên của lớp đó thấy bài
  được giao (F2-12).

**Cập nhật 2026-08-28:** hai GWT trước đây về yêu cầu chấm lại (chọn phạm vi + dry-run, không hạ điểm sau
khi chấm lại) đã bỏ cùng việc loại F4-09a→e khỏi phạm vi — xem `DEC-2026-0828-remove-rejudge-scope`. Nếu
testcase từng sai, học viên báo lại cho giảng viên và giảng viên tự sửa (F2-09 tăng phiên bản); lượt nộp cũ
giữ nguyên kết quả.

### ~~US-A2-04: Tạo bộ câu hỏi phỏng vấn riêng cho lớp~~ — ĐÃ LOẠI BỎ KHỎI PHẠM VI

**Loại bỏ 2026-08-28** (qua hỏi trực tiếp chủ dự án) — xem `DEC-2026-0828-remove-per-class-interview-set`.
Học viên dùng chung một ngân hàng câu hỏi phỏng vấn duy nhất ở cấp hệ thống, không có bộ câu hỏi riêng theo
lớp. Mã `F6-11` không còn hiệu lực.

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

### US-A2-06: Xem điểm AI tham khảo và chấm tay cho bài nộp của lớp mình

Là **giảng viên**, tôi muốn xem một điểm tham khảo nhanh cho từng bài nộp của lớp mình và tự chấm tay khi
cần, để lướt qua chất lượng bài làm mà không phải đọc lại toàn bộ báo cáo phân tích của từng bài.

- **Cho** một bài nộp `Accepted` đã có báo cáo phân tích bài giải (F5.1), **Khi** tôi mở màn Chấm bài của lớp
  mình phụ trách, **Thì** tôi thấy một điểm quy đổi trên thang 10 từ báo cáo đó, gắn nhãn rõ là điểm tham
  khảo (F5-27) — không phải kết quả Pass/Fail chính thức của bài nộp (F4-04 không đổi).
- **Cho** điểm tham khảo đang hiển thị, **Khi** tôi bấm "Chấm ngay" và nhập điểm 0-10 kèm nhận xét, **Thì**
  điểm chấm tay của tôi được lưu lại cạnh điểm AI, không ghi đè hay ảnh hưởng trạng thái submission của
  người học (F5-27).
- **Cho** tôi không có quyền trên lớp đó, **Khi** tôi mở màn Chấm bài, **Thì** tôi chỉ thấy bài nộp của lớp
  mình phụ trách — gác bởi Function `CLASS_MANAGEMENT` trong ma trận quyền (F1-10, F1-12).

### US-A2-07: Tạo lớp học và mời học viên tham gia bằng mã mời

Là **giảng viên**, tôi muốn tạo lớp học mới và có một mã mời để chia sẻ, để học viên tự tham gia lớp mà tôi
không phải thêm thủ công từng người.

- **Cho** tôi mở màn "Lớp của tôi", **Khi** tôi tạo lớp học mới với thông tin cơ bản, **Thì** hệ thống tạo
  lớp và sinh cho tôi một mã mời duy nhất gắn với lớp đó (F1-23).
- **Cho** lớp đã có mã mời, **Khi** tôi chia sẻ mã đó cho học viên, **Thì** học viên nhập đúng mã vào hệ
  thống thì tự động trở thành thành viên của lớp, không cần tôi hay quản trị viên xác nhận thêm (F1-23).
- **Cho** tôi là học viên nhập một mã mời không tồn tại hoặc đã hết hiệu lực, **Khi** tôi bấm tham gia lớp,
  **Thì** hệ thống báo lỗi rõ ràng và không thêm tôi vào lớp nào `[SoT: Suy luận — hành vi lỗi hợp lý, chưa
  hỏi chủ dự án]`.
- **Cho** tôi không còn cần một lớp đã tạo, **Khi** tôi xoá lớp, **Thì** hệ thống xoá lớp đó thật sự (hard
  delete), không phải khoá mềm (F1-24).
- **Cho** mã mời của một lớp đã sinh ra quá lâu, **Khi** thời hạn dùng của mã đó hết, **Thì** học viên nhập
  mã đã hết hạn bị từ chối tham gia (F1-25).
- **Cho** mã mời hiện tại của lớp đã hết hạn hoặc tôi muốn có mã khác để chia sẻ, **Khi** tôi tạo một mã mời
  mới cho cùng lớp, **Thì** hệ thống sinh mã mới với thời hạn dùng riêng của nó, không phụ thuộc các mã mời
  cũ (F1-25).
- **Cho** một học viên đang là thành viên của lớp tôi phụ trách, **Khi** tôi gỡ học viên đó khỏi lớp, **Thì**
  hệ thống xoá luôn lịch sử làm bài (lượt nộp, điểm, tiến độ) của học viên đó gắn với lớp này — không giữ
  lại (F1-26).

### US-A2-08: Xem hồ sơ chi tiết một học viên trong lớp mình phụ trách

Là **giảng viên**, tôi muốn xem hồ sơ chi tiết của một học viên trong lớp mình, để hiểu rõ hơn tình hình học
tập của em đó thay vì chỉ nhìn một dòng tóm tắt trong bảng.

- **Cho** tôi đang xem bảng danh sách học viên ở "Lớp của tôi", **Khi** tôi bấm vào một học viên, **Thì** hệ
  thống đưa tôi sang một màn hồ sơ chi tiết riêng cho học viên đó (thông tin cơ bản, tiến độ, lịch sử nộp
  bài) — chỉ trong phạm vi lớp tôi phụ trách (F1-27).
- **Cho** tôi không phụ trách lớp của học viên đó, **Khi** tôi cố mở hồ sơ chi tiết của học viên đó, **Thì**
  hệ thống từ chối — gác bởi Function `CLASS_MANAGEMENT` (F1-27, F1-12).

**Còn mở:** công thức phân loại trạng thái "Đang tốt/Cần hỗ trợ/Vắng bài" — để BD/DD quyết định khi thiết
kế, xem `01-rd/req/identity.md` F1-27. Slug/route đã chốt: `class_student_detail`, RD ở
`01-rd/screens/teacher/class_student_detail.md`.

### US-A2-09: Xem tổng hợp tiến độ nhiều học viên trong lớp

Là **giảng viên**, tôi muốn xem tiến độ tổng hợp của mọi học viên trong (các) lớp mình phụ trách trên một
màn, để phát hiện sớm học viên tụt lại mà không phải mở từng trang tiến độ cá nhân (F1-28, bổ sung
2026-08-30).

- **Cho** tôi mở màn tiến độ lớp, **Khi** dữ liệu tải xong, **Thì** tôi thấy điểm trung bình theo lớp (quy
  đổi từ tỉ lệ Accepted của từng học viên, thang 10) và danh sách học viên kèm điểm, tỉ lệ hoàn thành, chuỗi
  ngày và xu hướng của từng em (F1-28).
- **Cho** danh sách học viên đang hiển thị, **Khi** tôi chọn một lớp cụ thể, **Thì** cả bảng học viên và
  khối "Cần chú ý" đều lọc theo đúng lớp đó (F1-28).
- **Cho** một học viên không có lượt nộp nào trong 7 ngày gần nhất, hoặc tỉ lệ hoàn thành dưới 50%, hoặc
  điểm trung bình giảm liên tiếp từ hai tuần trở lên, **Khi** tôi xem khối "Cần chú ý", **Thì** học viên đó
  được nêu tên kèm nhãn tương ứng (Vắng bài / Theo dõi / Cần hỗ trợ) (F1-28).
- **Cho** tôi không phụ trách lớp nào, **Khi** tôi mở màn này, **Thì** hệ thống không hiển thị dữ liệu học
  viên của lớp khác — gác bởi Function `CLASS_MANAGEMENT` (F1-28, F1-12).

### US-A2-10: Soạn và bảo trì ngân hàng câu hỏi phỏng vấn dùng chung

Là **giảng viên hoặc quản trị viên**, tôi muốn tạo, sửa, nhân bản và xoá câu hỏi trong ngân hàng câu hỏi
phỏng vấn dùng chung, để có nội dung cho học viên học và luyện (F6-13, bổ sung 2026-08-30, gác bởi Function
`INTERVIEW_BANK_MANAGEMENT`).

- **Cho** tôi có quyền `INTERVIEW_BANK_MANAGEMENT`, **Khi** tôi tạo một câu hỏi mới, **Thì** tôi khai báo
  được nội dung, phân loại (chủ đề/cấp độ), danh sách câu hỏi đào sâu, và bộ tiêu chí đánh giá có trọng số ở
  một màn soạn riêng (F6-13).
- **Cho** một câu hỏi đã có, **Khi** tôi sửa hoặc nhân bản, **Thì** thao tác được ghi vào Nhật ký hệ thống
  kèm ai làm, làm gì, lúc nào (F6-13, F1-14).
- **Cho** một câu hỏi thiếu bộ tiêu chí đánh giá, **Khi** học viên mở Chế độ luyện cho câu hỏi đó, **Thì**
  câu hỏi bị ẩn khỏi Chế độ luyện (vẫn hiện ở Chế độ học), vì AI không có mốc đối chiếu để chấm (F6-13,
  F6-08).
- **Cho** một câu hỏi đã được dùng trong các phiên phỏng vấn trước, **Khi** tôi xoá câu hỏi đó, **Thì** hệ
  thống chỉ đánh dấu ngừng dùng và ẩn khỏi mọi màn phía học viên — các phiên phỏng vấn cũ vẫn giữ nguyên bản
  ghi, không bị ảnh hưởng (F6-13).

### US-A2-11: Xem tổng quan khối lượng công việc

Là **giảng viên**, tôi muốn thấy ngay số lớp, số học viên, số bài cần chấm và điểm trung bình lớp khi vừa
đăng nhập, để biết việc cần làm hôm nay mà không phải mở lần lượt từng màn con (F1-30, bổ sung 2026-08-31).

- **Cho** tôi vừa đăng nhập với vai trò `INSTRUCTOR`, **Khi** hệ thống điều hướng theo vai trò, **Thì** tôi
  được đưa thẳng tới màn tổng quan, thấy 4 thẻ thống kê và các widget tóm tắt lớp/bài tập/việc cần chấm
  (F1-30).
- **Cho** tôi phụ trách nhiều hơn một lớp, **Khi** các thẻ thống kê hiển thị, **Thì** số liệu là tổng hợp
  trên tất cả các lớp tôi phụ trách (F1-30, F2-12, F5-27).
- **Cho** một nguồn số liệu (`problem-bank`/`judge-orchestration`/`ai-review`) không phản hồi, **Khi** tôi mở
  màn, **Thì** các khối còn lại vẫn hiển thị bình thường, chỉ khối bị ảnh hưởng báo lỗi (F1-30).
