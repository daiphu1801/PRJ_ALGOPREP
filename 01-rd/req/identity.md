# F1 — Danh tính và phân quyền (`identity`)

> Tách ra từ `01-rd/req/req.md` ngày 2026-08-31 để dễ đọc (theo Bounded Context). Nội dung dưới đây được
> chuyển nguyên văn từ `req.md` (không đổi ý nghĩa, không thêm/bớt), chỉ hạ heading `###` xuống `#`.

- **Đăng ký và xác thực:**
  - Đăng ký tài khoản bằng email và mật khẩu (F1-01). Vai trò mặc định khi đăng ký là `STUDENT` (F1-05).
  - Đăng nhập cấp **Access Token** thời hạn ngắn và **Refresh Token** trong cookie HTTP-Only (F1-02,
    `README.md` mục 4-F1). Client tự làm mới Access Token bằng Refresh Token khi gặp `401` (F1-03).
  - Đăng xuất vô hiệu hoá Refresh Token của phiên hiện tại (F1-04).
- **Phân quyền theo vai trò (RBAC):**
  - Ba vai trò: `STUDENT` (A1) · `INSTRUCTOR` (A2) · `ADMIN` (A3) (F1-05, `overview.md` mục 1.I). Kiểm quyền
    ở **tầng ứng dụng**, không chỉ ở giao diện, và kiểm cả quyền sở hữu dữ liệu — một `STUDENT` không đọc
    được bài nộp hay phiên phỏng vấn của người khác.
  - **`ADMIN` và `INSTRUCTOR` là hai vai trò tách riêng, không lồng nhau** — chốt 2026-08-23 theo
    `06-plan/PROTOTYPE_DEBT.md` mục 1.2, thay cho đề xuất suy luận trước đó. `ADMIN` **không** tự động có
    quyền soạn nội dung của `INSTRUCTOR`; quyền của từng vai trò với từng nhóm chức năng quản trị/nội dung
    do **ma trận phân quyền** ở F1-10 quyết định, không phải do phân cấp vai trò.
- **Hai lớp quyền — đừng nhầm lẫn:**
  - **Lớp 1 — quyền học tập cơ bản.** Mọi tài khoản `STUDENT` có sẵn toàn bộ quyền dùng F2 (xem/nộp bài),
    F3/F4 (giải bài, chạy thử, nộp bài), F5 (phân tích bài giải, phỏng vấn giả lập), F6 (ôn câu hỏi) ngay
    khi đăng ký — đây là quyền theo **vai trò** (F1-05), không đi qua ma trận phân quyền ở F1-10. Bật/tắt ô
    trong ma trận phân quyền **không bao giờ** ảnh hưởng tới quyền học tập cơ bản này.
  - **Lớp 2 — ma trận phân quyền quản trị/nội dung.** Chỉ gác các thao tác thuộc nhóm quản lý (soạn đề,
    quản lý lớp, quản lý người dùng, cấu hình AI, giám sát hệ thống...) — xem F1-10 tới F1-14.
- **Trang tiến độ cá nhân:**
  - Bài đã giải theo chủ đề (F1-06), tỉ lệ chấp thuận — số bài nộp đạt `Accepted` trên tổng bài nộp
    (F1-07), lịch sử phỏng vấn mở lại được rubric của phiên cũ (F1-08). **Amendment 2026-08-31**
    (`DEC-2026-0831-partial-score-testcase-ratio`): F1-06 hiển thị thêm **điểm tốt nhất mỗi bài toán** — lấy
    giá trị lớn nhất của điểm tỷ lệ testcase (F4-13) trong toàn bộ lịch sử nộp của học viên cho bài đó
    (best-attempt, không phải lần nộp gần nhất), không đổi ý nghĩa "đã giải" (vẫn là có ít nhất một lần
    `Accepted`).
- **Quản lý hồ sơ:**
  - Sửa thông tin cá nhân (F1-09) — `[SoT: Suy luận — README.md không nêu, nhưng đăng ký mà không sửa được
gì là thiếu]`. **Chốt 2026-08-25** (đối chiếu `09-layoutBase/Trang cá nhân.dc.html`, tự quyết theo yêu cầu
    trực tiếp của chủ dự án vì prototype hiện tại chỉ là bản dựng tham khảo — hành vi chi tiết sẽ dựng lại
    khi có frontend Next.js thật): 6 trường sửa được — Họ và tên, Email, Trường/công ty, Vai trò hiện tại,
    Ngôn ngữ mặc định, Vị trí mục tiêu. **Đổi Email** bắt buộc xác thực lại bằng mã 6 số gửi tới email mới
    (tái dùng cơ chế của F1-17), email cũ còn hiệu lực cho tới khi xác thực xong. Trường "Gói" (`Pro`) trong
    prototype là dữ liệu mẫu không có ý nghĩa — AlgoPrep không có mô hình phân hạng tài khoản, bỏ khỏi giao
    diện thật **[Đợi nextjs]**. Mục "Xác thực hai lớp (2FA)" trong prototype nằm ngoài phạm vi đồ án — bỏ
    khỏi giao diện thật **[Đợi nextjs]**.
  - **F1-19 — Tự đổi mật khẩu khi đã đăng nhập.** Khác F1-17 (quên mật khẩu, chưa đăng nhập): người dùng nhập
    mật khẩu hiện tại + mật khẩu mới ngay tại trang cá nhân/bảo mật, không qua email. **Chốt 2026-08-25.**
- **Ma trận phân quyền Role × Function × Action** — chốt 2026-08-23 theo `06-plan/PROTOTYPE_DEBT.md` mục
  1.2. Giải quyết việc A2 (Giảng viên) và A3 (Quản trị viên) dùng **chung một khu Admin** trên giao diện,
  nhưng được tách quyền thật ở tầng ứng dụng thay vì chỉ ẩn/hiện menu:
  - **F1-10 — Ma trận phân quyền theo vai trò.** `ADMIN` xem và chỉnh ma trận quyền `CREATE`/`READ`/
    `UPDATE`/`DELETE` cho từng cặp (vai trò, chức năng quản trị). Đổi một ô có hiệu lực ngay, không cần
    khởi động lại dịch vụ.
  - **F1-11 — Function và Action là dữ liệu seed cố định, chỉ đọc trên giao diện; Role tạo/sửa/xoá được.**
    Lý do: mỗi `FUNCTION:ACTION` phải có một `@PreAuthorize` tương ứng trong mã nguồn mới có tác dụng thật
    — cho phép thêm Function tự do trên giao diện là tạo ra một ô tích không gác cửa gì cả. Không xoá được
    vai trò hệ thống (`STUDENT`, `INSTRUCTOR`, `ADMIN`) và không xoá được vai trò đang có người dùng.
  - **F1-12 — Danh sách chức năng quản trị/nội dung nằm trong phạm vi ma trận** (đề xuất khởi điểm,
    `[SoT: Suy luận]` — chốt số lượng chính xác khi viết BD):
    `PROBLEM_AUTHORING` (F2-01→04) · `TESTCASE_MANAGEMENT` (F2-05→09, F2-14) · `CLASS_MANAGEMENT` (F1-23→28, F2-12, F5-27) ·
    `USER_MANAGEMENT` (F1-13) · `JUDGE_QUEUE_MONITOR` (F4-10) ·
    `AI_CONFIG` (F5-23) ·
    `AI_TOKEN_BUDGET` (F5-21, F5-25) · `SYSTEM_AUDIT_LOG` (F1-14) · `INTERVIEW_BANK_MANAGEMENT` (F6-12, F6-13) ·
    `PERMISSION_MATRIX` (chính F1-10 — tự tham chiếu, mặc định chỉ `ADMIN` có toàn quyền).
  - **F1-13 — Quản lý tài khoản người dùng.** `ADMIN` đổi vai trò, khoá/mở khoá tài khoản, reset mật khẩu
    của người dùng khác — gác bởi `USER_MANAGEMENT` trong ma trận F1-10. Actor A3. **Chốt 2026-08-25** (qua
    hỏi trực tiếp chủ dự án khi viết `01-rd/screens/admin/admin_user_management.md` — prototype có mục "Đề
    nghị cấp quyền giảng viên" gây hiểu nhầm): **không có luồng tự yêu cầu nâng vai trò (self-service)** —
    đổi vai trò luôn là hành động đơn phương của `ADMIN`, không có bước "người dùng xin, ADMIN duyệt". Nếu
    một `STUDENT` muốn trở thành `INSTRUCTOR`, việc trao đổi diễn ra ngoài hệ thống (email, gặp trực tiếp),
    rồi `ADMIN` tự vào đổi vai trò qua chính F1-13, không qua một hàng đợi phê duyệt riêng.
  - **F1-14 — Mọi thay đổi ma trận phân quyền và mọi thao tác quản trị đều ghi vào Nhật ký hệ thống**, kèm
    ai đổi, đổi gì, đổi lúc nào — không có ngoại lệ cho chính thao tác đổi quyền. **Chốt phạm vi 2026-08-24
    (`06-plan/PROTOTYPE_DEBT.md` mục 2.5):** F1-14 chỉ ghi **hành động quản trị của người** (đổi ma trận
    quyền, đổi vai trò, khoá/mở khoá, reset mật khẩu...) — **không gộp** sự kiện hạ tầng/dịch vụ (lỗi judge
    engine, worker mất kết nối, timeout sweep...). Hai luồng dữ liệu tách theo đúng Bounded Context sinh ra
    chúng: hành động quản trị thuộc `identity` (F1-14), sự kiện hạ tầng thuộc `judge-orchestration` và đã có
    chỗ riêng ở giám sát hàng đợi (F4-10), không cần một mã `Fx-nn` mới.
- **Tạo lớp học và tham gia lớp bằng mã mời** — bổ sung 2026-08-28, lấp khoảng trống nêu ở
  `01-rd/screens/teacher/class_management.md` Câu hỏi mở Q2 (chưa có mã `Fx-nn` nào cho CRUD lớp học). Chốt
  qua hỏi trực tiếp chủ dự án:
  - **F1-23 — Giáo viên (A2) tạo lớp học mới** với thông tin cơ bản (tên lớp, có thể kèm mô tả/lịch học —
    danh sách trường cụ thể để BD/DD quyết định). Khi tạo xong, hệ thống sinh một **mã mời (invite code)**
    duy nhất gắn với lớp đó. **Chốt 2026-08-28 — qua hỏi trực tiếp chủ dự án:** học viên (A1) **tự tham
    gia lớp bằng cách nhập mã mời** (self-service join) — không phải giáo viên hay quản trị viên thêm thủ
    công từng học viên. Gác bởi Function `CLASS_MANAGEMENT` trong ma trận phân quyền (F1-12) cho thao tác
    tạo lớp (A2); việc học viên nhập mã mời để tham gia không đi qua ma trận phân quyền, tương tự cách
    F1-05 gán vai trò mặc định khi đăng ký — có sẵn tài khoản hợp lệ và biết mã mời là đủ điều kiện.
  - **F1-24 — Giáo viên xoá lớp học.** **Chốt 2026-08-28 — qua hỏi trực tiếp chủ dự án:** xoá **thật** (hard
    delete), không phải khoá mềm/ẩn danh hoá như cách F1-16 xử lý xoá tài khoản — đây là một quyết định có
    chủ đích khác với F1-16, không phải sơ suất thiếu nhất quán. Sửa thông tin lớp (đổi tên, mô tả, lịch
    học) dùng chung API/luồng với F1-23, không cần mã riêng.
  - **F1-25 — Mã mời có thời hạn dùng; giáo viên tạo được nhiều mã mời mới cho cùng một lớp.** **Chốt
    2026-08-28:** mã mời sinh ra ở F1-23 hết hạn sau một khoảng thời gian `[SoT: Suy luận — số ngày/giờ cụ
    thể chưa chốt, để BD quyết định giá trị mặc định]`; mã hết hạn thì học viên nhập vào bị từ chối tham
    gia. **Chốt 2026-08-28 (tiếp) — qua hỏi trực tiếp chủ dự án:** giáo viên có quyền **tạo nhiều mã mời
    mới** cho cùng một lớp bất cứ lúc nào (không giới hạn tạo lại một lần); **mỗi mã mời mới có thời hạn
    dùng riêng của nó**, độc lập với các mã cũ (mã cũ hết hạn không tự động ảnh hưởng mã mới). Không nói rõ
    mã cũ còn hiệu lực có bị vô hiệu hoá khi tạo mã mới hay không — `[SoT: Suy luận — để BD quyết định, hợp
    lý là cho phép nhiều mã cùng hiệu lực song song, không cần thu hồi mã cũ]`.
  - **F1-26 — Giáo viên gỡ một học viên khỏi lớp.** **Chốt 2026-08-28 — qua hỏi trực tiếp chủ dự án:** gỡ
    học viên khỏi lớp thì **xoá luôn lịch sử làm bài của học viên đó trong phạm vi lớp này** (lượt nộp, điểm,
    tiến độ gắn với lớp) — không giữ lại, vì "đã gỡ rồi thì giữ cũng không làm gì". Đây là **xoá thật, có
    cascade**, khác hẳn cách dữ liệu người dùng thường được giữ lại ở nơi khác trong hệ thống (ví dụ F1-16 —
    xoá tài khoản chỉ khoá mềm) — ghi rõ ở đây để BD/DD không lỡ áp cùng một khuôn mẫu "giữ lại dữ liệu" cho
    mọi thao tác xoá.
  - **F1-27 — Giáo viên xem hồ sơ chi tiết một học viên trong lớp mình phụ trách** (thông tin cơ bản, tiến
    độ, lịch sử nộp bài — phạm vi giới hạn trong lớp giáo viên đó quản lý). **Chốt 2026-08-28 — qua hỏi
    trực tiếp chủ dự án, lấp Q4 của `class_management.md`:** đây **cần một màn/route riêng**, không phải
    hiển thị mở rộng ngay tại bảng danh sách học viên — slug `class_student_detail` ở khu giảng viên, RD ở
    `01-rd/screens/teacher/class_student_detail.md` (viết 2026-09-01); chưa có prototype minh hoạ
    `[Đợi nextjs]`. Công thức phân loại trạng thái học viên ("Đang tốt"/"Cần hỗ trợ"/"Vắng bài") **chưa
    chốt ngưỡng cụ thể** — chủ dự án chọn để BD/DD tự đề xuất `[SoT: Suy luận — ngưỡng cụ thể quyết định
    khi thiết kế, không phải yêu cầu chức năng cứng]`.
  - **F1-28 — Giảng viên xem tổng hợp tiến độ nhiều học viên trong (các) lớp mình phụ trách** (dashboard
    `class_progress`) — trả lời Câu hỏi mở Q1-Q6 của `01-rd/screens/teacher/class_progress.md`. **Chốt
    2026-08-30 (owner instruction — "tạo dashboard ở mức tốt là được", tức chốt ở mức hợp lý, không cần đàm
    phán từng ngưỡng số; các con số cụ thể đánh dấu `[SoT: Suy luận]` để BD/DD tinh chỉnh):**
    - **Điểm trung bình (thang 10, theo học viên)** = tỉ lệ Accepted/tổng lượt nộp của học viên đó trong lớp
      (F1-07) quy đổi ×10 — không dùng điểm AI tham khảo của F5-27 (đó là điểm theo từng bài nộp, không phải
      điểm đại diện một học viên). Biểu đồ đường "Điểm trung bình theo lớp" = trung bình cộng điểm này của
      mọi học viên trong lớp, vẽ theo từng mốc tuần.
    - **Hoàn thành (%)** = số bài trong danh sách được giao cho lớp (F2-12) mà học viên đã Accepted ít nhất
      một lần, chia cho tổng số bài được giao cho lớp đó — không phải tỉ lệ Accepted/tổng nộp (đó là F1-07,
      dùng riêng cho Điểm trung bình ở trên).
    - **Chuỗi ngày (streak)** = số ngày liên tiếp gần nhất tính đến hôm nay mà học viên có ít nhất một lượt
      nộp bài — cùng khái niệm "chuỗi ngày luyện tập" đã dùng ở F1-21 (nhắc nhở qua email khi chuỗi sắp
      mất), không phải khái niệm mới cần định nghĩa lại.
    - **Xu hướng (sparkline)** = điểm trung bình của riêng học viên đó theo 6 mốc tuần gần nhất — cùng nguồn
      dữ liệu với biểu đồ đường ở trên nhưng theo từng cá nhân thay vì gộp lớp; cơ chế lưu (snapshot định kỳ
      hay tính runtime từ lịch sử lượt nộp) để BD/DD quyết định.
    - **Nhãn "Cần chú ý"** — ba loại, ngưỡng đề xuất mặc định, BD/DD chỉnh được: "Vắng bài" — không có lượt
      nộp nào trong 7 ngày gần nhất `[SoT: Suy luận]`; "Theo dõi" — Hoàn thành dưới 50% và đã được giao ít
      nhất một bài `[SoT: Suy luận]`; "Cần hỗ trợ" — điểm trung bình giảm liên tiếp qua từ hai mốc tuần gần
      nhất trở lên `[SoT: Suy luận]`. Khối "Cần chú ý" **luôn lọc theo đúng tab lớp đang chọn**, giống bảng
      danh sách học viên (sửa thiếu sót của prototype, nơi khối này bỏ qua bộ lọc lớp).
    - **Danh sách học viên khi lớp đông**: phân trang cùng kiểu với `problem_list` (F2-11); có trạng thái
      rỗng riêng khi lớp chưa có học viên, và trạng thái lỗi tải dữ liệu kèm nút thử lại — chi tiết UI để
      BD/DD.
    - Gác bởi Function `CLASS_MANAGEMENT` (F1-12); phạm vi hiển thị theo lớp giảng viên phụ trách, cùng cơ
      chế đã dùng ở F2-12/F5-27.
- **Quản trị viên (A3) xem tổng quan vận hành hệ thống** — bổ sung 2026-08-31, lấp Câu hỏi mở Q2 và Q5 của
  `01-rd/screens/admin/admin_overview.md` (`07-review/rd_review_report_260826.html` mục 6.2, mức Cao). Chốt
  qua hỏi trực tiếp chủ dự án, cả hai theo phương án khuyến nghị:
  - **F1-29 — Dashboard tổng quan vận hành** (`admin_overview`) bao trùm 8/9 khối thống kê trước đó không có
    mã: 2 thẻ chỉ số tổng (Tổng lượt nộp, Người dùng hoạt động), Lượt nộp theo ngôn ngữ/theo ngày/theo tháng,
    Kết quả chấm, Độ khó bài toán, Bài phổ biến nhất, Người dùng mới/quay lại — **một mã tổng hợp duy nhất**
    thay vì cấp mã rời cho từng khối, cùng cách đã làm cho `class_progress` (F1-28). Không phải một `FUNCTION`
    riêng trong ma trận F1-10 — mọi vai trò `ADMIN` đều thấy trang này vì nó là đích mặc định sau đăng nhập
    (`auth.md` Q3 đã chốt), không bị gác quyền như các màn con.
    - **Người dùng hoạt động** = tài khoản có đăng nhập trong 30 ngày gần nhất `[SoT: Suy luận — số ngày cụ
      thể chốt ở BD]`, loại trừ tài khoản `DEACTIVATED` (F1-16).
    - Khung thời gian tính delta so sánh và đơn vị trục của biểu đồ theo ngôn ngữ để BD/DD quyết định cụ thể,
      không phải yêu cầu chức năng cứng.
    - **Amendment 2026-08-31** (lấp Q3, Q4, Q6, Q8 của `admin_overview.md`,
      `DEC-2026-0831-admin-overview-ui-decisions`): widget "Kết quả chấm" hiện **đủ 5 verdict thật**
      (`AC`/`WA`/`TLE`/`CE`/`RE`, cùng bộ verdict của F1-18) thay vì gộp 3 nhóm — giữ tín hiệu CE (lỗi
      harness F3) và RE (lỗi sandbox) tách biệt. Ô tìm kiếm liên thực thể ở thanh công cụ đầu trang **bỏ
      khỏi UI thật** (chưa cần cho phạm vi đồ án). Widget "Bài phổ biến nhất" có liên kết "Xem tất cả" dẫn
      sang `problem_management`; các widget biểu đồ còn lại giữ nguyên là tóm tắt, không thêm liên kết. Màn
      cần 3 trạng thái — `empty`, `loading`, `error` — theo **từng khối riêng** (không chặn cả màn khi một
      khối/nguồn dữ liệu lỗi), khớp nguyên tắc AI degrade gracefully khi khối đọc từ `ai-review` gặp sự cố.
  - **Trục "Độ khó bài toán" — sửa nhãn, không mở tính năng mới.** Legend `AI sinh` / `Giảng viên soạn` trong
    prototype là **nhãn dữ liệu mẫu vẽ sai** — `req.md` chỉ có F2-14 (AI sinh **testcase**, không sinh đề
    bài), không có mã nào cho AI sinh đề bài và không có thuộc tính "nguồn gốc bài toán" trong `problem-bank`.
    Khi dựng UI thật: đổi trục thành một phân loại có thật (ví dụ tỉ lệ Accepted theo độ khó)
    `[SoT: Suy luận — BD chọn trục cụ thể]`, không thêm mã F2 mới, không thêm thuộc tính tác giả bài toán.
- **Giảng viên (A2) xem tổng quan khối lượng công việc** — bổ sung 2026-08-31, lấp Câu hỏi mở Q1-Q5, Q7 của
  `01-rd/screens/teacher/instructor_overview.md` (Q6 đã đóng riêng qua `DEC-2026-0831-i18n-scope-expansion`).
  Chốt theo phương án khuyến nghị đã đề xuất sẵn trong RD (mẫu hình giống cách `class_progress`/F1-28 và
  `admin_overview`/F1-29 đã chốt trong phiên này — chủ dự án uỷ quyền áp phương án hợp lý nhất thay vì
  duyệt từng dòng, đánh dấu rõ `[SoT: Suy luận]` ở mọi con số chưa có căn cứ cứng):
  - **F1-30 — Dashboard tổng quan khối lượng công việc** (`instructor_overview`) bao trùm 4 thẻ thống kê
    (Lớp phụ trách, Tổng học viên, Cần chấm tay, Điểm TB lớp), widget "Cần chấm tay", "Hoạt động gần đây",
    "Lớp của tôi", "Tiến độ học viên", "Bài tập của tôi" — một mã tổng hợp duy nhất, không phải một `FUNCTION`
    riêng trong ma trận F1-10 (mọi vai trò `INSTRUCTOR` đều thấy trang này, là đích mặc định sau đăng nhập,
    `auth.md` Q3 đã chốt). Trả lời `US-A2-11` mới trong `01-rd/req/user_stories/a2_instructor.md`.
    - **Điểm TB lớp** = điểm AI tham khảo (F5-27), gộp trung bình trên mọi bài `Accepted` có báo cáo F5.1 của
      (các) lớp phụ trách — sẵn có cho mọi bài `Accepted`, không phụ thuộc giảng viên đã chấm tay hay chưa.
    - **Badge số trên nav** ("3" lớp, "18" bài tập, "9" cần chấm) làm mới khi tải lại trang/chuyển màn, không
      cần kênh WebSocket riêng cho badge dashboard.
    - **"Hoạt động gần đây"** là một **read model riêng của `identity`** tổng hợp sự kiện từ `problem-bank`/
      `judge-orchestration`/`ai-review` — không tái dùng bảng Nhật ký hệ thống (F1-14, vốn dành cho audit
      hành động quản trị của A3, khác mục đích và khác actor xem). Giới hạn số lượng/thời gian giữ log để
      BD/DD quyết định.
    - Widget "Tiến độ học viên" có liên kết "Xem tất cả" dẫn sang `class_progress`, nhất quán với hai widget
      còn lại ("Lớp của tôi" → `class_management`, "Bài tập của tôi" → `class_assignments`).
    - Màn cần 3 trạng thái — `empty`, `loading`, `error` — theo **từng khối riêng**, cùng nguyên tắc đã áp
      cho `admin_overview` (F1-29).
- **Đăng nhập qua nhà cung cấp bên thứ ba (OAuth)** — bổ sung theo `06-plan/PROTOTYPE_DEBT.md` mục 2.2,
  đối chiếu `09-layoutBase/Đăng nhập & Đăng ký.dc.html`:
  - **F1-15 — Đăng nhập/đăng ký bằng OAuth (GitHub, Google)**, song song với F1-01/F1-02 (email + mật khẩu),
    vai trò mặc định khi tạo tài khoản mới qua OAuth vẫn là `STUDENT` (F1-05). **Xung đột email — chốt
    2026-08-24:** nếu email do provider OAuth trả về khớp với email của một tài khoản email/mật khẩu đã
    tồn tại, hệ thống **tự động liên kết** đăng nhập OAuth vào tài khoản đó (coi email đã được provider xác
    thực là đủ tin cậy) — không tạo tài khoản thứ hai, không hỏi lại người dùng. Người dùng sau đó đăng
    nhập được bằng cả hai cách vào cùng một tài khoản.
- **Tự xoá tài khoản (danger zone)** — bổ sung theo `06-plan/PROTOTYPE_DEBT.md` mục 2.3, đối chiếu
  `09-layoutBase/Cài đặt.dc.html`:
  - **F1-16 — Tự xoá tài khoản.** Chốt 2026-08-24: **khoá mềm rồi ẩn danh hoá sau khoảng ân hạn**, không
    xoá thật ngay. Bấm xoá → tài khoản chuyển trạng thái `DEACTIVATED` ngay lập tức, không đăng nhập lại
    được. Sau một khoảng ân hạn (đề xuất 30 ngày, `[SoT: Suy luận]` — chốt số ngày chính xác khi viết BD),
    một job định kỳ ẩn danh hoá thông tin định danh (email, tên hiển thị) của tài khoản — bài nộp, bài giải
    đã lưu, phiên phỏng vấn **không bị xoá**, chỉ gỡ liên kết tới danh tính cá nhân, để không phá vỡ thống
    kê tiến độ lớp của giảng viên khi một học viên xoá tài khoản giữa kỳ.
- **Tự đặt lại mật khẩu (quên mật khẩu)** — bổ sung theo `01-rd/screens/shared/auth.md` mục 5 câu hỏi mở Q4, phát
  hiện khi đối chiếu `09-layoutBase/Đăng nhập & Đăng ký.dc.html` (liên kết "Quên mật khẩu?" có thật ở màn
  đăng nhập nhưng trước đó chưa có mã `Fx-nn` nào phủ luồng này — khác `F1-13` là ADMIN reset hộ người
  khác):
  - **F1-17 — Tự đặt lại mật khẩu bằng mã xác nhận 6 chữ số gửi qua email.** Chốt 2026-08-24 qua hỏi trực
    tiếp chủ dự án: người dùng chưa đăng nhập nhập email tại màn `auth`; hệ thống gửi một mã gồm **6 chữ số
    ngẫu nhiên** tới email đó qua Gmail; người dùng nhập đúng mã trong thời hạn hiệu lực (đề xuất 10 phút,
    `[SoT: Suy luận]` — chốt số phút chính xác khi viết BD) thì được đặt mật khẩu mới. Mã chỉ dùng được một
    lần — dùng xong hoặc hết hạn thì vô hiệu ngay, phải yêu cầu gửi mã mới. Giới hạn số lần nhập sai liên
    tiếp (`[SoT: Suy luận]` — đề xuất 5 lần, chốt ở BD) và giới hạn tần suất gửi lại mã (chống spam email,
    `[SoT: Suy luận]` — đề xuất tối thiểu 60 giây giữa hai lần gửi) để tránh lạm dụng.
  - **Không tiết lộ email có tồn tại hay không:** thông báo sau khi bấm gửi mã luôn là một câu chung
    ("Nếu email tồn tại trong hệ thống, mã xác nhận đã được gửi") bất kể email đó có tài khoản hay không —
    tránh lộ thông tin cho kẻ dò email đã đăng ký (OWASP — user enumeration), khớp yêu cầu OWASP top 10 đã
    nêu ở `CLAUDE.md` mục Rules.
  - **Tài khoản chỉ đăng ký qua OAuth (chưa từng đặt mật khẩu, F1-15) — chốt 2026-08-24 qua hỏi trực tiếp
    chủ dự án:** vì tài khoản này chưa từng có mật khẩu, "quên mật khẩu" không áp dụng được — hệ thống
    **từ chối tạo mật khẩu mới qua luồng này**, chỉ gửi một email (không phải mã 6 số) báo tài khoản đang
    đăng nhập bằng GitHub/Google và hướng dẫn quay lại đăng nhập bằng đúng provider đó. Bước nhập email vẫn
    giữ nguyên thông báo chung ("nếu email tồn tại, mã đã được gửi") — không lộ việc tài khoản có tồn tại
    hay không, chỉ khác nội dung email thực sự nhận được tuỳ loại tài khoản.
- **Lịch sử nộp bài, tuỳ chọn cá nhân hoá, thông báo, và xuất dữ liệu** — bổ sung 2026-08-25 theo
  `06-plan/reports/260825-1500-report-ai1-phase2-conflicts.md`, đối chiếu
  `09-layoutBase/Bài đã nộp.dc.html` và `09-layoutBase/Cài đặt.dc.html`. Tự chốt theo yêu cầu trực tiếp của
  chủ dự án — prototype là bản dựng tham khảo, chi tiết hành vi UI sẽ dựng lại khi có frontend Next.js thật
  (đánh dấu **[Đợi nextjs]** ở phần thuần UI):
  - **F1-18 — Xem lịch sử nộp bài của chính mình**, lọc theo verdict (AC/WA/TLE/CE/RE...) và ngôn ngữ, tìm
    theo tên/mã bài; phân trang cuối bảng giống `problem_list` (F2-11) **[Đợi nextjs]** cho chi tiết UI.
  - **F1-20 — Tuỳ chọn cá nhân hoá Workspace lưu theo tài khoản**: ngôn ngữ mặc định, cỡ chữ editor, tự lưu
    bản nháp, phím tắt Vim — áp dụng khi mở màn giải bài (F3/F4), không ảnh hưởng chấm bài.
  - **F1-21 — Thông báo email định kỳ**: nhắc luyện tập khi chuỗi ngày sắp mất, báo cáo tiến độ hằng tuần
    theo chủ đề — job định kỳ thuộc `identity`, tắt/mở được theo từng loại. Không thuộc luồng OTP của F1-17.
  - **F1-22 — Xuất dữ liệu cá nhân**: lượt nộp (CSV), hội thoại phỏng vấn (JSON) — chỉ xuất dữ liệu của
    chính người dùng đang đăng nhập.
  - **Câu chữ "Vùng nguy hiểm" ở màn Cài đặt phải khớp hành vi F1-16 đã chốt** (khoá mềm rồi ẩn danh hoá, dữ
    liệu không mất) — prototype hiện ghi "xoá vĩnh viễn" là sai, sửa lại khi dựng UI thật **[Đợi nextjs]**.
