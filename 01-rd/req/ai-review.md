# F5 — Phân hệ AI (`ai-review`)

> Tách ra từ `01-rd/req/req.md` ngày 2026-08-31 để dễ đọc (theo Bounded Context). Nội dung dưới đây được
> chuyển nguyên văn từ `req.md` (không đổi ý nghĩa, không thêm/bớt), chỉ hạ heading `###` xuống `#`.

Hai chức năng độc lập, người dùng chủ động chọn (`README.md` mục 4-F5). F5.1 (Solution Review) luôn kích
hoạt **sau khi** bài nộp đạt `Accepted`. F5.2 (Mock Interview) có lối vào mặc định sau khi bài nộp đạt
`Accepted`, cộng hai lối vào tự luyện không đòi hỏi `Accepted` — xem F5-24.

**F5.1 — Phân tích bài giải (một lượt, không hội thoại):**

- Yêu cầu phân tích bài giải vừa nộp (F5-01). **Amendment 2026-08-31** (lấp Câu hỏi mở Q3 của
  `01-rd/req/user_stories/open_questions.md`, `DEC-2026-0831-outside-screens-closures`): đây là **hành động chủ động của
  người học** — hệ thống chỉ hiện lựa chọn/nút bấm sau khi bài nộp đạt `Accepted`, không tự động chạy phân
  tích ngay. Tiết kiệm chi phí gọi AI (khớp tinh thần rate-limit F5-19 và cache F5-20), khớp đúng cách
  prototype `submission_result` đã dựng (nút bấm, không tự chạy).
- Phân tích độ phức tạp thời gian và bộ nhớ thực tế kèm lập luận (F5-02); đối chiếu với độ phức tạp tối ưu
  đã biết của bài toán, gợi ý hướng tiếp cận tốt hơn nếu chưa tối ưu (F5-03).
- Chỉ ra trường hợp biên bộ test chưa phủ, giả định ngầm trong mã, nguy cơ tràn số, rủi ro khi dữ liệu lớn
  hơn ràng buộc (F5-04); nhận xét chất lượng mã — đặt tên, phân rã, trùng lặp, độ dễ đọc, **kèm một điểm số
  dễ đọc quy ước thang 1-5 do chính AI tự chấm trong cùng lượt phân tích** (không gọi thêm lần nào, chỉ là
  một trường bổ sung trong JSON trả về) (F5-05); đưa ra câu hỏi mở rộng dưới dạng **chủ đề/từ khoá gợi ý**,
  không phải liên kết cố định tới một câu hỏi cụ thể nào trong kho `interview-bank` — tránh `ai-review` phải
  đọc dữ liệu của `interview-bank` (`DEC-2026-0820-architecture-baseline`, modules không import lẫn nhau);
  giao diện tự dựng liên kết tìm kiếm sang `interview_bank_list` lọc theo đúng chủ đề/từ khoá đó (F5-06).
  Chốt 2026-08-25 qua `06-plan/reports/260825-2100-ai2-solution-review-conflicts.md`, tự quyết theo yêu cầu
  trực tiếp của chủ dự án khi review Phase 3 — cách trình bày cụ thể trên giao diện (số liên kết hiện ra,
  bố cục) **[Đợi nextjs]**, để dựng lại khi có frontend thật thay vì đối chiếu tiếp prototype tĩnh.
- Trả kết quả dưới dạng **JSON có lược đồ** để giao diện render báo cáo tĩnh (F5-07); lưu kèm bài nộp, tra
  cứu lại được từ trang tiến độ (F5-08).

**F5.2 — Phỏng vấn giả lập 1:1 (nhiều lượt, hội thoại):**

- Mở phiên phỏng vấn cho một bài nộp đã `Accepted` (F5-09). Đây là lối vào mặc định, nhưng **không phải lối
  vào duy nhất** — F5-24 mở thêm hai lối vào tự luyện, không đòi hỏi bài nộp `Accepted` nào.
- Ba giai đoạn: **Giải trình thuật toán** — người học trình bày ý tưởng và lý do chọn cấu trúc dữ liệu
  (F5-10); **Phản biện** — AI chất vấn điểm chưa tối ưu và trường hợp biên (F5-11); **Mở rộng quy mô** —
  tình huống dữ liệu tăng đột biến hoặc ràng buộc hệ thống đổi (F5-12). Ba giai đoạn này áp dụng cho phiên mở
  từ cả ba lối vào ở F5-09/F5-24, không riêng gì lối vào từ bài nộp.
- Duy trì ngữ cảnh phiên qua `ChatMemory` trên Redis xuyên ba giai đoạn (F5-13); stream phản hồi qua SSE
  (F5-14).
- Kết phiên xuất bảng đánh giá rubric bốn tiêu chí (độ rõ ràng, độ chính xác kỹ thuật, khả năng phản biện,
  nhận thức về độ phức tạp) kèm nhận xét từng tiêu chí (F5-15); lưu phiên và rubric, mở lại được từ trang
  tiến độ (F5-16).

**Ràng buộc chung của phân hệ AI (áp cho cả F5 và F6):**

- **Chống prompt injection** (F5-17): mã nguồn và câu trả lời người dùng là **tham số dữ liệu**, tách hoàn
  toàn khỏi chỉ thị hệ thống — OWASP LLM01 (`overview.md` mục 1.J, `CLAUDE.md` mục Rules). **Amendment
  2026-08-31** (lấp Câu hỏi mở Q6 của `01-rd/screens/shared/problem_authoring.md`, mức Cao): quy tắc này
  cũng áp dụng cho **văn bản do người soạn đề (A2/A3) nhập ở khối "Chỉ dẫn cho trợ lý AI" theo từng bài
  toán** rồi nối vào prompt hệ thống của `ai-review`. Văn bản này **không được coi là chỉ thị hệ thống** dù
  do một actor có quyền nhập — nối vào prompt trong một khối có nhãn riêng (ví dụ `<per_problem_context>`),
  không được ghi đè các ràng buộc cứng của chính F5-17 hoặc F5-18, và chỉ vai trò có quyền
  `PROBLEM_AUTHORING:UPDATE` (ma trận F1-10) mới sửa được nội dung này.
- **Định hướng giáo dục** (F5-18): rubric và báo cáo là phản hồi hỗ trợ học tập, **không phải điểm số chính
  thức** — phải hiện rõ trên giao diện.
- **Kiểm soát chi phí:** giới hạn tần suất gọi theo người dùng (F5-19); cache kết quả F5.1 theo hash mã
  nguồn — nộp lại đúng mã đó thì không gọi lại API (F5-20); theo dõi lượng token tiêu thụ theo từng phiên
  (F5-21).
- **Ngân sách token AI: dự báo cạn quota và tự động khoá khi vượt** (F5-25) — bổ sung theo
  `06-plan/PROTOTYPE_DEBT.md` mục 2.9, đối chiếu `09-layoutBase/Admin - Token AI.dc.html`. Mở rộng F5-21 từ
  "chỉ theo dõi" sang có **ngân sách** (hạn mức token theo khoảng thời gian, ví dụ theo tháng) và **dự báo**
  thời điểm cạn quota theo tốc độ tiêu thụ hiện tại; bảng xếp hạng người dùng/bài toán tốn token nhiều nhất
  để Admin biết chặn ở đâu trước. **Chốt 2026-08-24 — hành vi khi chạm ngân sách:** hệ thống **tự động tạm
  khoá gọi AI** (F5.1, F5.2, F6-08) cho vai trò `STUDENT` và `INSTRUCTOR` ngay khi vượt ngân sách đã đặt;
  `ADMIN` không bị khoá và là người duy nhất mở lại/tăng ngân sách. F1-F4 (giải bài, chạy thử, nộp bài,
  chấm) không bị ảnh hưởng — khoá chỉ chặn lời gọi AI, đúng nguyên tắc suy giảm có kiểm soát (F5-22).
  **Amendment 2026-08-31** (lấp Câu hỏi mở Q1 của `01-rd/screens/admin/admin_ai_usage.md`,
  `DEC-2026-0831-ai-usage-anomaly-alert`): F5-19/F5-25 còn bao gồm một **cảnh báo mềm cấp tài khoản** — khi
  một tài khoản gọi AI với tần suất lệch bất thường so với trung bình (ví dụ gấp nhiều lần), hệ thống hiện
  cảnh báo cho `ADMIN` xem xét ở `admin_ai_usage`, **không tự khoá tài khoản đó** (khác hẳn khoá do vượt
  ngân sách chung ở trên) — không cần mã `Fx-nn` riêng, ngưỡng cụ thể chốt khi viết BD.
- **Suy giảm có kiểm soát** (F5-22): AI hỏng hoặc hết quota thì F1-F4 vẫn hoạt động bình thường — ràng buộc
  kiến trúc, không phải lời hứa. Không được gọi AI đồng bộ trong luồng ghi nhận kết quả chấm.
- **Cấu hình prompt và rubric AI (F5-23, actor A3)** — chi tiết hoá theo `06-plan/PROTOTYPE_DEBT.md` mục
  2.8, đối chiếu `09-layoutBase/Admin - Cấu hình AI.dc.html`. **Chốt 2026-08-24:**
  - Mỗi tính năng AI (Phân tích bài giải, Phỏng vấn giả lập...) dùng **một bản prompt riêng, có phiên bản**
    (ví dụ `v3.8`), lưu lại lịch sử các phiên bản đã có (nháp/đang chạy) và xem lại/khôi phục bản cũ được.
  - Cấu hình trọng số từng tiêu chí của **rubric chấm bài giải** (dùng cho điểm tổng trong báo cáo phân
    tích), và **giới hạn tần suất** gọi AI theo cấu hình admin đặt (ngưỡng thực thi bởi F5-19). F5-23 chỉ
    phủ rubric của F5.1 (Solution Review). Hệ thống có **ba rubric độc lập**, đừng gộp: F5-23 (rubric chấm
    bài giải, admin cấu hình trọng số) · **F5-15** (rubric bốn tiêu chí kết phiên Mock Interview) · **F6-13**
    (tiêu chí đối chiếu Chế độ luyện F6-08). Trọng số 25/30/25/20% của rubric F5-15 có cấu hình được hay cố
    định thì chốt khi viết DD cho `ai-review` — xem `01-rd/screens/users/mock_interview.md` Câu hỏi mở Q1.
  - **Chạy đối chiếu (regression test) trước khi publish prompt mới: giữ ở tầng giao diện, chưa cam kết
    logic backend trong phạm vi đồ án.** Nút "Chạy đối chiếu" (chạy 30 bài giải mẫu, so sánh điểm rubric
    giữa bản nháp và bản đang chạy) thể hiện đúng tầm nhìn kiểm thử chất lượng prompt trước khi thay đổi
    ảnh hưởng người dùng thật, nhưng phần chạy batch AI thật cho 30 bài kèm so sánh kết quả là khối lượng
    việc lớn, rủi ro vỡ tiến độ đã cảnh báo ở rủi ro R3 (`system_survey.md` mục 9) và tốn token API không
    cần thiết trong giai đoạn thực nghiệm. Không xoá khỏi prototype — giữ nguyên để thể hiện tầm nhìn thiết
    kế; backend cho tính năng này xếp vào hướng phát triển mở rộng, không nằm trong cam kết của DD/test cho
    đợt này.
- **Ba lối vào phiên phỏng vấn** (F5-24, `[SoT: Suy luận — đối chiếu 09-layoutBase/Phỏng vấn giả lập.dc.html
  với F5-09, chốt cùng phiên với quyết định mục 1.3 của `06-plan/PROTOTYPE_DEBT.md`, 2026-08-24]`): ngoài
  lối vào từ bài nộp `Accepted` (F5-09), người học còn tự mở phiên từ (a) **kho câu hỏi** có sẵn của F6, hoặc
  (b) **tự chọn chủ đề** (tick nhiều chủ đề thuật toán, chọn mức độ và ngôn ngữ) khi chưa có bài nộp phù hợp
  để luyện. Cả ba lối vào cùng chạy chung một luồng ba giai đoạn (F5-10 tới F5-16) và chung mọi ràng buộc AI
  (F5-17 tới F5-22) — khác biệt duy nhất là nguồn đề bài/chủ đề nạp vào phiên, không phải luồng hội thoại.
  Việc phỏng vấn không còn bị khoá cứng sau `Accepted` cần phản ánh lại ở câu mô tả tổng quan "sau khi
  Accepted" trong `01-rd/overview/overview.md` khi module này được viết BD/DD — F5.2 giờ là **có thể mở sau
  Accepted, hoặc mở độc lập để tự luyện**, không phải điều kiện bắt buộc.
- **F5-28 — Tự chỉnh tham số phiên tự luyện** (chốt 2026-08-25, tự quyết theo yêu cầu trực tiếp của chủ dự
  án, đối chiếu `09-layoutBase/Cài đặt.dc.html` mục "Phỏng vấn giả lập"): người học đặt trước Mức người
  phỏng vấn (Junior/Middle/Senior), Số lượt tối đa mỗi phiên, và Cho phép gợi ý khi bí — **chỉ áp dụng cho
  hai lối vào tự luyện của F5-24** (kho câu hỏi/tự chọn chủ đề), không áp dụng cho lối vào từ bài nộp
  `Accepted` (F5-09) để giữ tính khách quan của phiên gắn với một bài giải thật.
- **Áp dụng bản mã AI đề xuất vào Workspace** (F5-26) — bổ sung theo `06-plan/PROTOTYPE_DEBT.md` mục 2.13,
  đối chiếu `09-layoutBase/Phân tích bài giải.dc.html`. Sau khi nhận báo cáo phân tích bài giải (F5.1),
  người học bấm áp dụng một đoạn mã AI đề xuất trực tiếp vào Workspace, ghi đè mã đang có. **Chốt
  2026-08-24 — qua hỏi trực tiếp chủ dự án:** bắt buộc **xác nhận trước khi ghi đè** (dialog xác nhận, không
  áp dụng ngầm), và **giữ lại bản mã cũ** của người học trước khi ghi đè (snapshot, khôi phục lại được) —
  không ghi đè mất luôn, đúng nguyên tắc "mã người học là của người học" (`01-rd/overview/overview.md` mục
  2). Không gộp vào F5-07/F5-08 vì đây là một hành động **ghi** vào Workspace của người dùng, khác bản chất
  với việc chỉ đọc/hiển thị báo cáo.
- **Điểm AI tham khảo và chấm tay của giảng viên** (F5-27) — bổ sung theo `06-plan/PROTOTYPE_DEBT.md` mục
  6.2.a, đối chiếu `09-layoutBase/Giáo viên - Chấm bài.dc.html`. Từ báo cáo phân tích bài giải (F5.1), hệ
  thống quy đổi thêm một **điểm tham khảo trên thang 10** (không phải trường mới của F5-07, chỉ là một cách
  hiển thị tổng hợp của cùng báo cáo) để giảng viên lướt nhanh chất lượng bài làm của học viên trong lớp mình
  phụ trách; giảng viên chấm tay đè lên điểm này kèm nhận xét. **Chốt 2026-08-24 — qua hỏi trực tiếp chủ dự
  án:** điểm AI 0-10 và điểm chấm tay của giảng viên **chỉ là lớp tham khảo nội bộ dành cho giảng viên**,
  tách bạch hoàn toàn khỏi kết quả Pass/Fail chính thức của bài nộp — không ghi đè, không ảnh hưởng trạng
  thái submission, không phải "chấm điểm từng phần theo trọng số do người soạn đề khai báo" đã bị loại ở mục
  2.6. Không mâu thuẫn F5-18 (rubric/báo cáo AI vẫn chỉ là phản hồi hỗ trợ học tập) vì điểm 0-10 này chưa bao
  giờ được trình bày cho người học như điểm chính thức, chỉ hiện trong màn quản lý riêng của giảng viên.
  **Không nhầm với F4-13** (điểm tỷ lệ testcase, thang 1đ, hiện cho người học ở `submission_result`) —
  hai điểm số độc lập, một dành cho giảng viên (chất lượng lời giải qua AI, thang 10), một dành cho người
  học (tỷ lệ testcase pass, thang 1), không quy đổi qua nhau.
  Phạm vi hiển thị: theo lớp giảng viên phụ trách (cùng cơ chế với F2-12). Actor A2, gác bởi Function
  `CLASS_MANAGEMENT` trong ma trận phân quyền (F1-12). **Amendment 2026-08-31** (lấp Câu hỏi mở Q2, Q3, Q4
  của `01-rd/screens/teacher/instructor_grading.md`, chốt theo phương án khuyến nghị):
  - **Q2 — màn `instructor_grading` là hàng đợi đã lọc sẵn**, không hiển thị toàn bộ bài nộp của lớp: chỉ
    liệt kê bài `Accepted` có điểm AI tham khảo **dưới 6/10** `[SoT: Suy luận — BD/DD chỉnh được]`.
  - **Q3 — hiển thị song song điểm AI gốc và điểm chấm tay** sau khi giảng viên đã chấm (ví dụ "AI: 4/10 ·
    GV: 7/10"), không ghi đè cách hiển thị điểm AI gốc — khớp đúng "lưu lại cạnh điểm AI, không ghi đè" của
    `US-A2-06`, áp dụng luôn cho tầng hiển thị chứ không chỉ tầng lưu trữ.
  - **Q4 — công thức 4 thẻ thống kê đầu trang**, không cần mã riêng: "Chờ chấm" = số bài đạt điều kiện Q2 mà
    giảng viên chưa chấm tay; "Đã chấm tuần này" = số bài giảng viên đã chấm trong **7 ngày gần nhất** (không
    phải tuần lịch); "Điểm TB sau chấm" = trung bình cộng điểm chấm tay của các bài đã chấm trong cùng cửa sổ
    7 ngày đó.
  - **Q5 — bộ lọc theo lớp**: màn có thêm bộ lọc dropdown/tab theo từng lớp phụ trách, cùng kiểu với
    `class_progress` — không cần mã riêng, chỉ là chi tiết UI cho giảng viên phụ trách nhiều lớp.
