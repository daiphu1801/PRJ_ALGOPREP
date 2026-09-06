# F6 — Ngân hàng câu hỏi phỏng vấn (`interview-bank`)

> Tách ra từ `01-rd/req/req.md` ngày 2026-08-31 để dễ đọc (theo Bounded Context). Nội dung dưới đây được
> chuyển nguyên văn từ `req.md` (không đổi ý nghĩa, không thêm/bớt), chỉ hạ heading `###` xuống `#`.

Màn hình độc lập, không gắn với bài nộp code (`README.md` mục 4-F6).

- **Danh sách và khám phá:** câu hỏi phân loại theo chủ đề và mức độ khó (F6-01); tìm kiếm và lọc (F6-02);
  đánh dấu để xem lại (F6-03). **Danh mục chủ đề sửa lại 2026-08-30 (owner instruction, trả lời Câu hỏi mở
  Q6 của `01-rd/screens/shared/interview_question_management.md`):** 5 chủ đề — Lý thuyết CS, System
  design, Database, Ngôn ngữ, Hành vi — thay cho danh mục 4 chủ đề cũ (Cấu trúc dữ liệu, Thuật toán, Thiết
  kế hệ thống, Câu hỏi hành vi), lấy theo prototype vì bao phủ rộng hơn (có thêm Database và Ngôn ngữ). Áp
  dụng cho cả bộ lọc ở `interview_bank_list`, `interview_question_detail` lẫn màn quản trị.
- **Chế độ học (`STUDY`):** xem gợi ý hướng tiếp cận (F6-04); xem khung trả lời chuẩn, áp dụng mô hình STAR
  cho câu hỏi hành vi (F6-05); xem danh sách từ khoá kỹ thuật cốt lõi cần nêu (F6-06).
- **Chế độ luyện (`PRACTICE`):** người dùng tự soạn câu trả lời (F6-07); AI đối chiếu với tiêu chí chuẩn,
  trả phản hồi ngắn — điểm đã đạt, điểm còn thiếu, hướng bổ sung (F6-08) — **đi qua phân hệ AI**, không tự
  gọi LLM, nên chịu chung ràng buộc F5-17 tới F5-22. **Amendment 2026-08-31** (lấp Câu hỏi mở Q1 của
  `01-rd/req/user_stories.md`, cùng Câu hỏi mở Q2 của
  `01-rd/screens/users/interview_question_detail.md`, `DEC-2026-0831-outside-screens-closures`): câu trả
  lời đã nộp ở Chế độ luyện **không sửa lại được** — mỗi lần nộp là một lượt (attempt) độc lập, muốn thử lại
  thì nộp một lượt mới; giữ đầy đủ lịch sử từng lượt cho F6-09/F6-10 theo dõi tiến bộ qua thời gian.
- **Theo dõi tiến độ:** lịch sử luyện tập và danh sách câu hỏi cần ôn lại (F6-09); tỉ lệ hoàn thành theo
  từng chủ đề (F6-10).
- **~~Quản lý theo lớp: giảng viên tạo bộ câu hỏi riêng và gán cho lớp phụ trách~~ (F6-11) — ĐÃ LOẠI BỎ
  KHỎI PHẠM VI (2026-08-28, qua hỏi trực tiếp chủ dự án).** Học viên dùng chung một ngân hàng câu hỏi phỏng
  vấn duy nhất ở cấp hệ thống (`interview_bank_list`, `interview_question_management`) — không có khái niệm
  "bộ câu hỏi riêng theo lớp". Lý do: giảm chức năng không cần thiết cho phạm vi đồ án. Mã `F6-11` và Function
  `INTERVIEW_BANK_MANAGEMENT` phần liên quan tới lớp không còn hiệu lực — xem quyết định
  `DEC-2026-0828-remove-per-class-interview-set`.
- **Tự chấm mức độ thuộc bài (spaced repetition)** (F6-12) — bổ sung theo `06-plan/PROTOTYPE_DEBT.md` mục
  2.10, đối chiếu `09-layoutBase/Câu hỏi phỏng vấn.dc.html`. **Chốt 2026-08-24 — qua hỏi trực tiếp chủ dự
  án:** sau khi xem gợi ý hướng tiếp cận/khung trả lời chuẩn ở Chế độ học (F6-04, F6-05), người học tự chấm
  mức độ nhớ câu hỏi đó (ví dụ ba mức: Biết rõ / Mơ hồ / Quên); hệ thống dùng lần tự chấm gần nhất để **tự
  xếp lịch** câu hỏi nào nên hiện lại sớm hơn trong danh sách "cần ôn lại" (F6-09) — thuật toán cụ thể (kiểu
  SM-2 đơn giản: khoảng cách ôn lại tăng dần nếu tự chấm tốt, rút ngắn lại nếu tự chấm kém) `[SoT: Suy
  luận]`, chốt công thức chính xác khi viết DD cho F6.
- **F6-13 — Quản trị nội dung ngân hàng câu hỏi phỏng vấn dùng chung: tạo, sửa, nhân bản, xoá** — trả lời
  Câu hỏi mở Q2 (và gói theo Q3, Q4, Q5, Q7) của
  `01-rd/screens/shared/interview_question_management.md`. **Chốt 2026-08-30 (owner instruction):**
  - Actor A2 và A3, gác bởi Function `INTERVIEW_BANK_MANAGEMENT` (F1-12) cùng F6-12 — phạm vi theo quyền,
    không phải mã riêng theo lớp (F6-11 đã loại bỏ).
  - Mỗi câu hỏi có 4 nhóm trường: nội dung + phân loại (F6-01), danh sách **câu hỏi đào sâu** (truy vấn tiếp
    theo cùng câu hỏi gốc — không phải F6-04/05/06, đó là dữ liệu riêng phục vụ AI chất vấn sâu hơn ở giai
    đoạn Phản biện, F5-11), và bộ **tiêu chí đánh giá có trọng số phần trăm** — chính là "tiêu chí chuẩn" mà
    F6-08 đối chiếu khi chấm Chế độ luyện, độc lập với hai rubric của F5 (F5-15, F5-23). Trọng số giữ lại vì
    Chế độ luyện cần một mốc để tính điểm theo từng tiêu chí.
  - **Việc soạn/sửa câu hỏi diễn ra ở một màn riêng** (không phải modal/drawer) — 4 nhóm trường lồng nhau
    quá nặng cho modal. Slug `interview_question_authoring`, RD ở
    `01-rd/screens/shared/interview_question_authoring.md` (viết 2026-09-01), chưa có prototype minh hoạ
    **[Đợi nextjs]**.
  - **Nhập CSV theo lô: ngoài phạm vi bản đầu** — không cấp mã, giữ nút ở mức tham khảo, không cam kết hiện
    thực trong đồ án này.
  - **4 thẻ chỉ số chất lượng nội dung** (tổng câu hỏi, thiếu rubric, điểm trung bình 30 ngày, chưa dùng lần
    nào) là **dẫn xuất trình bày** từ dữ liệu đã có, không cần mã riêng.
  - **Câu hỏi thiếu tiêu chí đánh giá:** vẫn hiện ở Chế độ học, nhưng **ẩn khỏi Chế độ luyện** (F6-08) vì AI
    không có mốc đối chiếu để chấm.
  - **Xoá câu hỏi: xoá mềm** — đánh dấu ngừng dùng, ẩn khỏi mọi màn phía học viên; phiên phỏng vấn đã dùng
    câu hỏi đó trước khi xoá vẫn giữ nguyên bản ghi cũ, không cascade xoá.
  - **Mọi thao tác thêm/sửa/nhân bản/xoá ghi vào Nhật ký hệ thống** (F1-14) — đúng phạm vi "hành động quản
    trị của người", cùng nhóm với các thao tác quản trị khác đã liệt kê ở F1-14.
