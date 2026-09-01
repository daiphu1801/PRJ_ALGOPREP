# F3 — Bộ sinh mã bọc hàm (`harness`) — trọng tâm kỹ thuật

> Tách ra từ `01-rd/req/req.md` ngày 2026-08-31 để dễ đọc (theo Bounded Context). Nội dung dưới đây được
> chuyển nguyên văn từ `req.md` (không đổi ý nghĩa, không thêm/bớt), chỉ hạ heading `###` xuống `#`.

Phân hệ cho phép mô hình bọc hàm hoạt động trên judge engine (go-judge mặc định) — vốn chỉ nhận stdin/stdout
(`overview.md` mục 1.G). Chi tiết codegen theo ngôn ngữ, ánh xạ lỗi biên dịch và đường lùi: xem
`.claude/skills/dd-generation/references/harness-codegen.md` khi viết DD.

- **Lược đồ kiểu dữ liệu độc lập ngôn ngữ** (F3-01): nguyên thuỷ, chuỗi, mảng nhiều chiều, danh sách lồng
  nhau, danh sách liên kết, cây nhị phân.
- **Sinh mã đa ngôn ngữ từ một đặc tả duy nhất** cho cả ba ngôn ngữ: đọc dữ liệu vào (F3-02), gọi hàm người
  dùng (F3-03), in kết quả (F3-04).
- **Tiêm mã và đóng gói:** ghép mã người dùng vào vùng chèn của mã khung (F3-05), đóng gói theo đúng định
  dạng adapter đang dùng yêu cầu trước khi gửi qua `JudgeExecutionPort` (F3-06; không hardcode Base64 —
  `DEC-2026-0823-go-judge-default-engine`).
- **So khớp kết quả linh hoạt:** exact (F3-07), chuẩn hoá khoảng trắng (F3-08), số thực theo sai số epsilon
  (F3-09), tập hợp không xét thứ tự (F3-10).
- **Ánh xạ lỗi biên dịch:** trả lỗi về đúng dòng trong mã người dùng (F3-11); **che giấu hoàn toàn** lỗi
  thuộc phần mã harness — người học không được thấy mã hệ thống (F3-12).
- **Cả hai mô hình nộp bài luôn song song, học viên tự chọn (F3-13) — sửa lại 2026-08-24 qua hỏi trực tiếp
  chủ dự án.** Bản trước coi Standard I/O là "đường lùi" chỉ dùng khi lược đồ kiểu chưa phủ được kiểu dữ
  liệu của bài — thu hẹp hơn phạm vi đã chốt ở `README.md` mục 5 dòng 179 ("Hỗ trợ cả 2 mô hình") và khác
  với hành vi thật của prototype (`09-layoutBase/Workspace giải bài.dc.html` dòng 596-601: học viên chọn tự
  do giữa "Bọc hàm" và "Có hàm main" ngay trong Workspace cho cùng một bài, không khoá theo bài toán).
  - **Mặc định:** mọi bài toán hiển thị cả hai mô hình ở màn `problem_detail`; học viên bấm chuyển đổi lúc
    làm bài, không phải lựa chọn một lần cố định — mã khung (starter code) đổi theo đúng mô hình đang chọn
    và ngôn ngữ đang chọn.
  - **Trường hợp duy nhất chỉ còn một mô hình:** nếu kiểu dữ liệu của bài toán vượt quá lược đồ kiểu độc lập
    ngôn ngữ của F3 (F3-01) — ví dụ cấu trúc dữ liệu tuỳ biến quá phức tạp để sinh mã bọc hàm tự động — thì
    **ẩn hẳn tuỳ chọn Bọc hàm cho bài đó**, chỉ còn Standard I/O (luôn khả dụng cho mọi bài vì học viên tự
    đọc/ghi theo định dạng khai báo ở F2-03, không phụ thuộc lược đồ kiểu). Đây là phương án xử lý rủi ro R2
    (`README.md` mục 7), không phải hành vi mặc định.
  - **Tác động tới khối lượng công việc:** vì mọi bài toán (trừ trường hợp trên) cần bộ sinh mã hoạt động
    cho cả hai mô hình, khối lượng codegen của F3 tăng gần gấp đôi so với cách hiểu "đường lùi hiếm khi
    dùng" trước đây. **Đã xác nhận 2026-08-24** ở `02-bd/packages/harness/architecture.md` mục 2b (viết cùng
    lúc với quyết định `DEC-2026-0824-dual-submission-model-per-problem`): chiến lược Strategy/Plugin **giữ
    nguyên**, không đổi kiến trúc — mỗi plugin ngôn ngữ có hai phương thức sinh mã (Bọc hàm, Standard I/O)
    thay vì một, cùng một interface, không cần registry riêng. Câu hỏi "cần chủ dự án xác nhận lại" trước đó
    trong `07-review/rd_review_report_260826.html` mục 6.5 vốn đã lỗi thời — BD đã trả lời từ trước khi
    report ghi nhận nó là câu hỏi mở.
