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
  Sửa 2026-10-22 (đồng bộ DB vòng 2, DEC-2026-1022-db-sync-round2), mục 10: **giới hạn phạm vi** — chủ dự án chốt 2026-10-22: không có ô nhập độ phức tạp tối ưu ở bất kỳ màn nào (`problems.known_optimal_complexity` giữ NULL), nên bước đối chiếu với optimum bị bỏ qua khi giá trị là NULL; báo cáo vẫn có phân tích độ phức tạp thực tế (F5-02) và gợi ý hướng tốt hơn do AI đưa ra.
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
  2026-08-31** (lấp Câu hỏi mở Q6 của `01-rd/screens/shared/SHR0202_problem_authoring.md`, mức Cao): quy tắc này
  cũng áp dụng cho **văn bản do người soạn đề (A2/A3) nhập ở khối "Chỉ dẫn cho trợ lý AI" theo từng bài
  toán** rồi nối vào prompt hệ thống của `ai-review`. Văn bản này **không được coi là chỉ thị hệ thống** dù
  do một actor có quyền nhập — nối vào prompt trong một khối có nhãn riêng (ví dụ `<per_problem_context>`),
  không được ghi đè các ràng buộc cứng của chính F5-17 hoặc F5-18, và chỉ vai trò có quyền
  `PROBLEM_AUTHORING:UPDATE` (ma trận F1-10) mới sửa được nội dung này.
- **Định hướng giáo dục** (F5-18): báo cáo phân tích bài giải (F5.1) và bảng đánh giá rubric của Mock Interview
  (F5-15) là phản hồi hỗ trợ học tập, **không phải điểm số chính thức** — phải hiện rõ trên giao diện. (Rubric chấm
  bài giải F5.1 đã bỏ, `DEC-2026-1014-drop-solution-review-rubric`, nên F5.1 chỉ còn "báo cáo".)
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
  **Amendment 2026-08-31** (lấp Câu hỏi mở Q1 của `01-rd/screens/admin/ADM0302_ai_usage.md`,
  `DEC-2026-0831-ai-usage-anomaly-alert`): F5-19/F5-25 còn bao gồm một **cảnh báo mềm cấp tài khoản** — khi
  một tài khoản gọi AI với tần suất lệch bất thường so với trung bình (ví dụ gấp nhiều lần), hệ thống hiện
  cảnh báo cho `ADMIN` xem xét ở `admin_ai_usage`, **không tự khoá tài khoản đó** (khác hẳn khoá do vượt
  ngân sách chung ở trên) — không cần mã `Fx-nn` riêng, ngưỡng cụ thể chốt khi viết BD.
  **Amendment 2026-10-23** (`DEC-2026-1023-dd-f5-budget-lock-and-breaker`): khi ngân sách bị khoá, hệ thống chỉ từ chối phiên và báo cáo **mới**; phiên Mock Interview đang mở vẫn tiếp tục lượt và chấm điểm bình thường.
- **Suy giảm có kiểm soát** (F5-22): AI hỏng hoặc hết quota thì F1-F4 vẫn hoạt động bình thường — ràng buộc
  kiến trúc, không phải lời hứa. Không được gọi AI đồng bộ trong luồng ghi nhận kết quả chấm.
- **Cấu hình prompt ~~và rubric~~ AI (F5-23, actor A3)** (phần "rubric" ĐÃ BỎ 2026-10-14,
  DEC-2026-1014-drop-solution-review-rubric) — chi tiết hoá theo `06-plan/PROTOTYPE_DEBT.md` mục
  2.8, đối chiếu `09-layoutBase/Admin - Cấu hình AI.dc.html`. **Chốt 2026-08-24:**
  - Mỗi tính năng AI (Phân tích bài giải, Phỏng vấn giả lập...) dùng **một bản prompt riêng, có phiên bản**
    (ví dụ `v3.8`), lưu lại lịch sử các phiên bản đã có (nháp/đang chạy) và xem lại/khôi phục bản cũ được.
  - **Cập nhật 2026-10-10 (`DEC-2026-1010-admin-selects-model-per-prompt`):** ADMIN chọn **model** cho từng bản prompt, trong danh sách model do dev khai báo ở cấu hình backend; khoá API của nhà cung cấp không bao giờ qua giao diện. Nhiệt độ không hiển thị.
  - ~~Cấu hình trọng số từng tiêu chí của **rubric chấm bài giải** (dùng cho điểm tổng trong báo cáo phân
    tích)~~ **ĐÃ BỎ 2026-10-14 (DEC-2026-1014-instructor-batch5-6-nav-pagination-rubric):** không còn trọng số phần
    trăm, không còn quy tắc "tổng phải bằng 100" và không còn quy đổi có trọng số. ~~ADMIN vẫn quản lý một **danh sách
    tiêu chí chỉnh được** (thêm, sửa, bật hoặc tắt từng tiêu chí) của rubric chấm bài giải.~~ **ĐÃ BỎ 2026-10-14
    (DEC-2026-1014-drop-admin-rubric-config):** ADMIN **không** cấu hình rubric chấm bài giải nữa (không trọng số, không
    danh sách tiêu chí chỉnh được). ~~Báo cáo học viên có **năm tiêu chí cố định** (`CORRECTNESS`, `PERFORMANCE`,
    `CLEAN_CODE`, `SCALABILITY`, `DATA_STRUCTURE`), mỗi tiêu chí một điểm và một nhận xét, là một phần của lược đồ phản hồi
    F5.1, **đánh phiên bản cùng phiên bản prompt** mà ADMIN đã quản lý; muốn đổi tiêu chí thì phát hành phiên bản
    prompt/lược đồ mới.~~ **ĐÃ BỎ 2026-10-14 (DEC-2026-1014-drop-solution-review-rubric):** kết quả cuối cùng là
    **không còn rubric chấm bài giải nào**: không mục rubric ở `ADM0301`, không năm thẻ tiêu chí
    (`CORRECTNESS`, `PERFORMANCE`, `CLEAN_CODE`, `SCALABILITY`, `DATA_STRUCTURE`) trong báo cáo học viên, không trường
    `criteriaScores` trong lược đồ F5-07, không dòng `rubric_scores` của bài giải. **Giữ** phần còn lại của báo cáo F5.1
    (độ phức tạp F5-02/F5-03, trường hợp biên và rủi ro F5-04, nhận xét chất lượng mã kèm điểm dễ đọc 1-5 F5-05, chủ đề
    nên ôn F5-06). Phần còn lại của F5-23 (prompt có phiên bản, chọn model, giới hạn tần suất) giữ
    nguyên; riêng "chạy đối chiếu" đã bỏ hẳn 2026-10-23 (`DEC-2026-1023-drop-ai-regression-run`, xem mục cuối của F5-23) [SoT: .nexa/control/decision-registry.md:3825-3857].
  - **Giới hạn tần suất** gọi AI theo cấu hình admin đặt (ngưỡng thực thi bởi F5-19). ~~F5-23 chỉ
    phủ rubric của F5.1 (Solution Review). Hệ thống có **ba rubric độc lập**, đừng gộp: F5-23 (rubric chấm
    bài giải, năm tiêu chí cố định theo lược đồ F5.1, admin không cấu hình, không trọng số) · **F5-15** (rubric bốn tiêu chí kết phiên Mock Interview) · **F6-13**
    (tiêu chí đối chiếu Chế độ luyện F6-08).~~ **ĐÃ SỬA 2026-10-14 (DEC-2026-1014-drop-solution-review-rubric):** hệ thống
    chỉ còn **hai rubric độc lập**, đừng gộp: **F5-15** (rubric bốn tiêu chí kết phiên Mock Interview) · **F6-13**
    (tiêu chí đối chiếu Chế độ luyện F6-08); rubric chấm bài giải (từng gọi là F5-23) đã bỏ. Trọng số cố định 25/30/25/20% của rubric F5-15 là một quy tắc riêng,
    **không** thuộc quyết định bỏ trọng số này; chủ BD của F5-15 kiểm tra lại (quyết định không sửa F5-15). Chốt khi viết DD cho `ai-review` — xem `01-rd/screens/users/USR0302_mock_interview.md` Câu hỏi mở Q1.
  - **ĐÃ BỎ HẲN 2026-10-23 (`DEC-2026-1023-drop-ai-regression-run`, tiêu chí ponytail bậc 2 và 3): không còn "Chạy đối chiếu", cả giao diện lẫn backend.**
    Lý do: mục đích gốc là so điểm rubric, rubric bài giải đã bỏ nên không còn gì để so; backend chưa từng được thiết kế và
    không ai bấm nút vô hiệu hoá lúc demo. Đoạn dưới giữ lại làm lịch sử.
  - ~~**Chạy đối chiếu (regression test) trước khi publish prompt mới: giữ ở tầng giao diện, chưa cam kết
    logic backend trong phạm vi đồ án.** Nút "Chạy đối chiếu" (chạy 30 bài giải mẫu, so sánh ~~điểm rubric~~ kết quả báo cáo
    giữa bản nháp và bản đang chạy; chữ "điểm rubric" ĐÃ BỎ 2026-10-14, DEC-2026-1014-drop-solution-review-rubric) thể hiện đúng tầm nhìn kiểm thử chất lượng prompt trước khi thay đổi
    ảnh hưởng người dùng thật, nhưng phần chạy batch AI thật cho 30 bài kèm so sánh kết quả là khối lượng
    việc lớn, rủi ro vỡ tiến độ đã cảnh báo ở rủi ro R3 (`system_survey.md` mục 9) và tốn token API không
    cần thiết trong giai đoạn thực nghiệm. Không xoá khỏi prototype — giữ nguyên để thể hiện tầm nhìn thiết
    kế; backend cho tính năng này xếp vào hướng phát triển mở rộng, không nằm trong cam kết của DD/test cho
    đợt này.~~
- **Ba lối vào phiên phỏng vấn** (F5-24, `[SoT: Suy luận — đối chiếu 09-layoutBase/Phỏng vấn giả lập.dc.html
  với F5-09, chốt cùng phiên với quyết định mục 1.3 của `06-plan/PROTOTYPE_DEBT.md`, 2026-08-24]`): ngoài
  lối vào từ bài nộp `Accepted` (F5-09), người học còn tự mở phiên từ (a) **kho câu hỏi** có sẵn của F6, hoặc
  (b) **tự chọn chủ đề** (tick nhiều chủ đề thuật toán, chọn mức độ; **không chọn ngôn ngữ**, sửa 2026-10-29, F5-09, owner chốt: luyện theo chủ đề độc lập ngôn ngữ, khớp DD và `interview_sessions` không có cột ngôn ngữ) khi chưa có bài nộp phù hợp
  để luyện. Cả ba lối vào cùng chạy chung một luồng ba giai đoạn (F5-10 tới F5-16) và chung mọi ràng buộc AI
  (F5-17 tới F5-22) — khác biệt duy nhất là nguồn đề bài/chủ đề nạp vào phiên, không phải luồng hội thoại.
  Việc phỏng vấn không còn bị khoá cứng sau `Accepted` cần phản ánh lại ở câu mô tả tổng quan "sau khi
  Accepted" trong `01-rd/overview/overview.md` khi module này được viết BD/DD — F5.2 giờ là **có thể mở sau
  Accepted, hoặc mở độc lập để tự luyện**, không phải điều kiện bắt buộc.
- **F5-28 — Tự chỉnh tham số phiên tự luyện** (chốt 2026-08-25, tự quyết theo yêu cầu trực tiếp của chủ dự
  án, đối chiếu `09-layoutBase/Cài đặt.dc.html` mục "Phỏng vấn giả lập"): người học đặt trước Mức người
  phỏng vấn (danh sách cố định 4 mức `INTERN`/`JUNIOR`/`MIDDLE`/`SENIOR`; **sửa 2026-10-22**, `DEC-2026-1022-ai-judge-schema-sync` mục 1: ~~danh sách mức do admin cấu hình động, không cố định 3 mức; chốt 2026-10-19, `DEC-2026-1001-admin-configurable-settings`~~, admin không thêm/bớt mức), Số lượt tối đa mỗi phiên, và Cho phép gợi ý khi bí — **chỉ áp dụng cho
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
- **ĐÃ BỎ 2026-10-13 (`DEC-2026-1013-remove-instructor-manual-grading`) — F5-27, điểm AI tham khảo và chấm tay của giảng viên.**
  Mã `F5-27` không được cấp lại. Nội dung cũ (bổ sung theo `06-plan/PROTOTYPE_DEBT.md` mục 6.2.a): quy đổi báo
  cáo F5.1 thành điểm tham khảo thang 10, giảng viên xem trong lớp mình phụ trách và chấm tay đè lên kèm nhận
  xét, qua màn `instructor_grading` (`INS0301`). Chủ dự án bỏ hẳn chấm tay của giảng viên (tiêu chí ponytail bậc
  3: giảng viên không đọc nổi mã của mọi học viên, hàng đợi sẽ đầy và không ai dùng khi demo; bậc 2: điểm chấm
  tay không nuôi màn hay báo cáo nào khác). Các amendment 2026-08-31 (Q2 hàng đợi lọc điểm AI dưới 6/10, đảo
  2026-10-13 thành mọi bài `Accepted`; Q3 hiển thị song song; Q4 thẻ thống kê 7 ngày; Q5 bộ lọc lớp) và các quyết
  định `DEC-2026-0831-instructor-grading-round2`, `DEC-2026-1013-grading-queue-all-accepted` đều hết hiệu lực.
  **Giữ nguyên:** báo cáo phân tích bài giải do học viên tự xin (F5.1, F5-01 tới F5-08), Function
  `CLASS_MANAGEMENT`, và quyền đọc mã nguồn của học viên trong lớp (`DEC-2026-1013-instructor-avatar-menu-and-read-source`).
  **Không nhầm với F4-13** (điểm tỷ lệ testcase, thang 1 điểm, hiện cho người học ở `submission_result`) — F4-13 không đổi.
  - ~~**Câu hỏi mở (chủ dự án quyết, chưa chốt) — OQ-F5-A:** trường `ai_score_10` (điểm quy đổi thang 10 của báo cáo
    F5.1, trước đây "chỉ để giảng viên tham khảo") nay **không còn màn nào hiển thị**. Giữ hay bỏ? Chủ sở hữu:
    chủ dự án. Ưu tiên: Thấp (không chặn BD/DD khác; ảnh hưởng cột `ai.solution_reviews.ai_score_10` và công thức
    "Điểm TB lớp" từng dựa trên nó, xem `01-rd/req/F1_identity.md` F1-30). Nguồn: `.nexa/control/decision-registry.md:3541-3545`.
    **Cập nhật 2026-10-14:** sau `DEC-2026-1014-instructor-overview-trim-and-nav-label`, `INS0101` không còn đọc
    `ai_score_10` (thẻ "Điểm TB lớp" và widget "Tiến độ học viên" đã bỏ); các màn khác chưa kiểm tra. Câu hỏi
    vẫn **MỞ**, chưa quyết.~~
    **OQ-F5-A ĐÃ ĐÓNG 2026-10-14 (`DEC-2026-1014-drop-ai-score-10`, tiêu chí ponytail bậc 3, 2):** chủ dự án quyết **bỏ**
    cột dẫn xuất `ai.solution_reviews.ai_score_10` và quy tắc quy đổi điểm AI sang thang 10 theo trọng số rubric admin.
    **Giữ:** báo cáo F5.1 (`result_json`), `readability_score`, ~~các dòng `rubric_scores` theo từng tiêu chí kèm nhận xét,
    và `rubric_configs`~~ (cả hai sau đó bỏ cho bài giải: `rubric_configs` theo `DEC-2026-1014-drop-admin-rubric-config`, dòng
    `rubric_scores` của bài giải theo `DEC-2026-1014-drop-solution-review-rubric`). Mã `F5-27` vẫn không cấp lại [SoT: .nexa/control/decision-registry.md:3615-3621].
  - ~~**Câu hỏi mở OQ-F5-B (mới 2026-10-14) — số phận trọng số rubric admin của F5-23:** trọng số rubric cấu hình ở
    `ADM0301` (F5-23) vốn dùng để tính điểm quy đổi có trọng số; khi `ai_score_10` đã bỏ, trọng số chỉ còn gắn với từng
    dòng tiêu chí. Chọn một trong ba: giữ trọng số, chuyển thành không trọng số, hoặc bỏ hẳn. **Chưa quyết, không tự
    giả định.** Chủ sở hữu: chủ dự án. Ưu tiên: Trung bình. Ảnh hưởng: màn `ADM0301` (`01-rd/screens/admin/ADM0301_ai_config.md`)
    và bảng `rubric_configs`.~~
    **OQ-F5-B ĐÃ ĐÓNG 2026-10-14 (`DEC-2026-1014-instructor-batch5-6-nav-pagination-rubric`, tiêu chí ponytail bậc 3, 2):**
    chủ dự án quyết **bỏ trọng số**; ~~giữ danh sách tiêu chí chỉnh được (thêm, sửa, bật hoặc tắt), theo
    `DEC-2026-1001-admin-configurable-settings`~~ **ĐÃ BỎ 2026-10-14 (DEC-2026-1014-drop-admin-rubric-config):** kết quả
    cuối cùng là admin **không** cấu hình rubric chấm bài giải; ~~giữ năm tiêu chí cố định trong lược đồ F5.1, đánh phiên
    bản cùng prompt~~ **ĐÃ BỎ 2026-10-14 (DEC-2026-1014-drop-solution-review-rubric):** năm tiêu chí cố định cũng bỏ, không còn
    rubric chấm bài giải nào; bảng `rubric_configs` cũng bỏ. ~~Cột `rubric_scores.weight_percent_snapshot` và `rubric_configs.weight_percent`
    bỏ ở BD (ngoài phạm vi RD)~~ **ĐÃ CHỐT 2026-10-14 (DEC-2026-1014-batch7-remaining-open-questions):** cả hai
    cột đều bỏ; trọng số 25/30/25/20 của rubric Mock Interview F5-15 là hằng số trong mã ứng dụng, gắn với phiên bản
    phản hồi/lược đồ, không lưu thành cột [SoT: .nexa/control/decision-registry.md:3866-3868].
