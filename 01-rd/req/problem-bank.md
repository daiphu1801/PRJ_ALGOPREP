# F2 — Ngân hàng bài toán và testcase (`problem-bank`)

> Tách ra từ `01-rd/req/req.md` ngày 2026-08-31 để dễ đọc (theo Bounded Context). Nội dung dưới đây được
> chuyển nguyên văn từ `req.md` (không đổi ý nghĩa, không thêm/bớt), chỉ hạ heading `###` xuống `#`.

- **Soạn đề bài (A2):**
  - Soạn đề bài bằng Markdown kèm công thức LaTeX (F2-01); phân loại theo độ khó và chủ đề (F2-02).
    **Amendment 2026-08-31** (Câu hỏi mở Q7(d) của `01-rd/screens/shared/problem_authoring.md`,
    `DEC-2026-0831-problem-authoring-round2`): F2-02 còn bao gồm **thẻ (tag) tự do**, nhiều thẻ mỗi bài,
    không giới hạn danh mục cố định như chủ đề — dùng để lọc chi tiết hơn ở F2-11.
  - **Khai báo đặc tả bài toán cho CẢ HAI mô hình nộp bài song song (F2-03).** Sửa lại 2026-08-24 qua hỏi
    trực tiếp chủ dự án khi viết `01-rd/screens/users/problem_detail.md` — bản trước chỉ nói tới đặc tả cho mô
    hình Bọc hàm, thu hẹp hơn phạm vi đã chốt ở `README.md` mục 5 dòng 179 ("Hỗ trợ cả 2 mô hình: Bọc hàm và
    Nhập/Xuất chuẩn") và mục 1.1 dòng 15 (so với HackerRank — "Cả hai"): mỗi bài toán khai báo
    - **chữ ký hàm** theo từng ngôn ngữ trong ba ngôn ngữ (Java, C++, Python), kiểu tham số và kiểu trả về —
      đầu vào bắt buộc cho mô hình **Bọc hàm** (bộ sinh mã F3), **và**
    - **định dạng input/output theo dòng chuẩn** (thứ tự đọc từ `stdin`, định dạng in ra `stdout`) — đầu
      vào bắt buộc cho mô hình **Standard I/O**, học viên tự đọc/ghi theo đúng định dạng này.
    Học viên **tự chọn mô hình nào cũng được cho cùng một bài** ở màn `problem_detail` (F3-13 sửa lại — xem
    dưới) — đây không phải một trong hai lựa chọn xung khắc của giảng viên, mà là input kép bắt buộc cho mọi
    bài toán, trừ trường hợp F3-13 áp dụng.
  - Khai báo chiến lược so khớp kết quả cho bài toán: `EXACT` · `TRIMMED` · `EPSILON` · `UNORDERED_SET`
    (F2-04, `glossary.md` mục 2).
- **Testcase:**
  - Hai loại: **Sample** — công khai, dùng cho Chạy thử (F2-05); **Hidden** — ẩn, dùng cho Nộp bài (F2-06).
  - Tải lên bộ testcase theo lô; bộ lớn lưu trên MinIO (F2-07).
  - **Chống rò rỉ testcase ẩn** (F2-08): phản hồi chỉ trả **trạng thái và chỉ số** của testcase sai, không
    trả input và không trả diff chi tiết — ràng buộc bảo mật nghiệp vụ, áp dụng cho mọi API và log của
    luồng nộp bài (`README.md` mục 4-F2).
  - Phiên bản hoá bộ testcase để **truy vết** một lượt nộp cũ đã chấm theo phiên bản nào (F2-09). (Sửa
    2026-09-03: câu trước ghi "để Re-judge biết chấm lại theo phiên bản nào" — cơ chế chấm lại đã loại khỏi
    phạm vi từ 2026-08-28, `DEC-2026-0828-remove-rejudge-scope` — nên phiên bản bộ testcase giờ chỉ còn tác
    dụng truy vết, không phải đầu vào cho một luồng chấm lại. Khớp với bản đã sửa cùng lúc trong
    `01-rd/req/user_stories/a2_instructor.md`, `US-A2-02`.)
- **Giới hạn tài nguyên:**
  - Giới hạn thời gian và bộ nhớ theo bài, kèm **hệ số nhân theo ngôn ngữ** — Java chậm hơn C++ nên cùng
    một bài phải khác hệ số (F2-10). **Amendment 2026-08-31** (Câu hỏi mở Q7(e) của
    `01-rd/screens/shared/problem_authoring.md`, `DEC-2026-0831-problem-authoring-round2`): F2-10 còn bao
    gồm **kích thước đầu ra tối đa** và **số lần nộp tối đa mỗi giờ**, khai theo từng bài toán (khác hệ số
    theo ngôn ngữ ở `admin_language_config`/F4-11, vốn là giới hạn mặc định toàn hệ thống).
- **Khám phá và quản lý lớp:**
  - Tìm kiếm và lọc bài toán theo chủ đề, độ khó, trạng thái đã giải (F2-11, actor A1). **Amendment
    2026-08-31** (cùng Q7(d), `DEC-2026-0831-problem-authoring-round2`): thêm lọc theo **thẻ** (F2-02).
  - Giao bài tập theo lớp (F2-12, actor A2). **Bổ sung 2026-08-24** (`06-plan/PROTOTYPE_DEBT.md` mục 2.11,
    đối chiếu `09-layoutBase/Ngân hàng bài toán.dc.html`): bài toán đã gán theo lớp **hiển thị thành một khối
    riêng ngay trong cùng trang danh sách bài toán** (`problem_list`) của học viên thuộc lớp đó, không phải
    một màn tách biệt — không cần mã mới, chỉ là cách trình bày của cùng F2-12. **Sửa câu chữ 2026-08-25**
    (chốt qua `01-rd/screens/users/problem_list.md` Câu hỏi mở Q1, đã đóng): câu trước dùng chữ "lồng thành
    một nhóm riêng" dễ hiểu nhầm là chèn xen kẽ vào từng dòng bảng; prototype thật là một khối tổng hợp cạnh
    bảng chính (sidebar), không lồng vào từng dòng — giữ đúng theo prototype, chỉ sửa lại câu chữ mô tả.
    **Amendment 2026-08-31** (lấp Câu hỏi mở Q9 của `01-rd/screens/teacher/class_assignments.md`, chốt theo
    phương án khuyến nghị): F2-12 còn bao gồm **gỡ bài đã giao khỏi lớp**, actor A2, cùng quyền
    `CLASS_MANAGEMENT:UPDATE`. Gỡ chỉ ẩn bài khỏi danh sách được giao cho lớp đó **từ thời điểm gỡ trở đi** —
    **không xoá** lượt nộp/điểm/tiến độ cũ của học viên gắn với bài đó trong lớp, khác hẳn cách F1-26 xoá
    cascade khi gỡ một **học viên** khỏi lớp (hai thao tác gỡ khác đối tượng, không dùng chung một khuôn mẫu
    xoá dữ liệu).
  - **Bài đã lưu (bookmark) kèm ghi chú riêng tư** (F2-13) — bổ sung theo `06-plan/PROTOTYPE_DEBT.md` mục
    2.1, đối chiếu `09-layoutBase/Bài đã lưu.dc.html`. Người học đánh dấu bài toán để xem lại, kèm ghi chú
    tự do theo từng bookmark. **Chốt 2026-08-24:** ghi chú lưu ở server theo (`user_id`, `problem_id`) —
    **riêng tư tuyệt đối**, chỉ chủ tài khoản đọc được, không có ngoại lệ cho `INSTRUCTOR`/`ADMIN` dù có
    toàn quyền qua ma trận phân quyền F1-10 (ghi chú bookmark không thuộc phạm vi ma trận, không phải một
    `FUNCTION` quản trị). Vì lưu server-side nên tự đồng bộ trên mọi thiết bị đăng nhập, không cần cơ chế
    đồng bộ riêng. **Mở rộng 2026-08-25** (tự chốt theo yêu cầu chủ dự án, đối chiếu
    `01-rd/screens/users/saved_problems.md` mục 5 Q1): mức riêng tư tuyệt đối áp cho **cả chính việc đã
    bookmark bài nào** (không chỉ nội dung ghi chú) — `INSTRUCTOR`/`ADMIN` không biết một học viên đã lưu
    bài toán nào.
  - **F2-14 — AI hỗ trợ sinh testcase tự động, output lấy từ chạy thật Đáp án mẫu (không để AI tự bịa
    output)** — bổ sung theo `06-plan/PROTOTYPE_DEBT.md` mục 2.6, đối chiếu nút "Sinh tự động" ở
    `09-layoutBase/Admin - Soạn đề bài.dc.html`. **Chốt 2026-08-24 — giữ tính năng, cơ chế an toàn bắt
    buộc:** AI chỉ sinh **input** (dựa trên đề bài Markdown và Ràng buộc dữ liệu Admin đã khai báo);
    **output không do AI tạo ra** — hệ thống lấy input đó chạy qua **Đáp án mẫu** của chính bài toán trên
    go-judge (cùng `JudgeExecutionPort` dùng để chấm bài, F4-03) để lấy output thật, rồi mới ghép cặp
    input/output vào danh sách testcase nháp. Cơ chế này loại bỏ rủi ro "AI bịa sai expected-output khiến
    bài nộp đúng bị chấm Wrong Answer" đã nêu ở `PROTOTYPE_DEBT.md` mục 2.6 — output luôn là kết quả chạy
    chương trình thật, không phải văn bản do AI viết ra.
    - Testcase do F2-14 sinh ra ở trạng thái **nháp, chưa dùng để chấm bài** cho tới khi Admin xác nhận —
      không tự động gộp vào bộ Hidden testcase khi xuất bản.
    - Điều kiện tiên quyết: bài toán phải có Đáp án mẫu đã chạy Pass với testcase hiện có (nút "Chạy với
      đáp án mẫu" trong cùng màn); chưa có đáp án mẫu hợp lệ thì không có gì để chạy input qua, tính năng vô
      hiệu.
  - **F2-15 — Vòng đời bài toán: hai trạng thái `Chưa xuất bản` / `Đã xuất bản`** — trả lời Câu hỏi mở Q2 của
    `01-rd/screens/shared/problem_management.md`. **Chốt 2026-08-30 (owner instruction):** rút gọn còn đúng
    hai trạng thái, bỏ trạng thái thứ ba `Đã ẩn` mà prototype từng dùng — một bài từng xuất bản rồi bị rút
    xuống quay lại đúng trạng thái `Chưa xuất bản`, không giữ trạng thái riêng để phân biệt "chưa từng xuất
    bản" với "đã xuất bản rồi rút xuống". Xem `DEC-2026-0830-problem-lifecycle-two-states`.
    - Mặc định khi tạo bài toán mới: `Chưa xuất bản`.
    - Chuyển `Chưa xuất bản` → `Đã xuất bản`: actor A2/A3 có quyền `PROBLEM_AUTHORING:UPDATE`; điều kiện tiên
      quyết — bài toán phải có ít nhất một testcase Hidden (F2-06) và đặc tả đủ theo mô hình nộp bài đã khai
      báo (F2-03); thiếu điều kiện thì hệ thống chặn kèm lý do, không cho xuất bản một bài không chấm được.
    - Chuyển `Đã xuất bản` → `Chưa xuất bản` ("rút xuống"): cùng quyền, không điều kiện tiên quyết; bài rút
      xuống biến mất khỏi `problem_list` của A1 ngay lập tức, nhưng **lượt nộp cũ, bookmark, phiên phỏng vấn
      liên quan không bị xoá** — chỉ ẩn khỏi tầm nhìn học viên (cùng nguyên tắc "ẩn mềm, dữ liệu không mất"
      đã áp cho F1-16).
    - Chỉ bài `Đã xuất bản` xuất hiện ở `problem_list` (F2-11, phạm vi A1); `problem_management` (màn quản
      trị, A2/A3) luôn thấy cả hai trạng thái.
    - **Xoá bài toán — amendment 2026-08-31 (Câu hỏi mở Q3 của `problem_management.md`,
      `DEC-2026-0831-problem-management-lifecycle-details`):** ẩn mềm, không phải trạng thái thứ ba —
      xoá chuyển bài về `Chưa xuất bản` **cộng thêm một cờ `deleted` riêng**, để phân biệt "chưa xuất bản vì
      đang soạn" với "đã xoá". Dữ liệu liên quan (lượt nộp, bookmark, phiên phỏng vấn) không bị xoá — cùng
      nguyên tắc F1-16. Áp dụng cho cả xoá một dòng và xoá theo lô.
    - **Điều kiện xuất bản chi tiết — amendment 2026-08-31** (Câu hỏi mở Q7(b) của
      `01-rd/screens/shared/problem_authoring.md`, cùng DEC trên): ngoài testcase Hidden (F2-06) và đặc tả
      đủ (F2-03), checklist "Sẵn sàng xuất bản" của `problem_authoring` chặn cứng nếu chưa đạt đủ **cả 5**:
      tối thiểu 8 testcase, tối thiểu 2 testcase công khai (Sample), tối thiểu 2 ví dụ mẫu, đáp án mẫu chạy
      Pass trên mọi testcase hiện có, và (đã bỏ ở Q3 riêng của `problem_authoring.md`) tổng trọng số 100 —
      **không còn áp dụng**. Cả 4 ngưỡng còn lại đánh dấu `[SoT: Suy luận]`, BD/DD chỉnh được.
    - **Ngưỡng thống kê kho bài — amendment 2026-08-31** (Câu hỏi mở Q5 của `problem_management.md`, cùng
      DEC trên): khối "Bài cần chú ý" là **dẫn xuất trình bày**, không cần mã riêng, dùng hai ngưỡng cấu
      hình được: tỉ lệ AC dưới **30%**, và `Chưa xuất bản` quá **7 ngày** kể từ lần sửa gần nhất — cả hai
      `[SoT: Suy luận]`, khớp đúng số liệu prototype đang dùng.
  - **F2-16 — Nhân bản bài toán.** Bổ sung 2026-08-31, lấp Câu hỏi mở Q4 của
    `01-rd/screens/shared/problem_management.md` (`DEC-2026-0831-problem-management-lifecycle-details`).
    Sao chép toàn bộ nội dung một bài toán (đề, đặc tả F2-03/F2-04, testcase, ví dụ mẫu, đáp án mẫu) thành
    một bài mới ở trạng thái `Chưa xuất bản`, actor A2/A3 có quyền `PROBLEM_AUTHORING:CREATE`. Hành động
    theo lô (nhiều bài cùng lúc) được.
  - **F2-17 — Xuất CSV danh sách bài toán.** Bổ sung 2026-08-31, cùng DEC trên. Xuất **chỉ phần dữ liệu
    bảng** (mã, tiêu đề, chủ đề, độ khó, trạng thái, lượt nộp, tỉ lệ AC, số testcase) của bài đã chọn hoặc
    toàn bộ kết quả đang lọc — **không xuất nội dung đề bài, đặc tả, hay testcase**. **Nhập CSV bài toán
    (nút "Nhập CSV" ở thanh tiêu đề `problem_management`) đã cắt khỏi phạm vi** — một CSV phẳng không chở
    nổi đặc tả F2-03 (chữ ký hàm ba ngôn ngữ) hay nội dung Markdown/LaTeX của đề bài; xoá nút này khi dựng
    UI thật.
