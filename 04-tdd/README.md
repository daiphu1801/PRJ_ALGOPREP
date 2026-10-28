# 04-tdd — Tiêu chí nghiệm thu

Xem [tổng quan TDD](overview.md) để tra ma trận 6 module, 40 màn, quy tắc QA và trạng thái truy vết trước khi sinh testcase.

TDD theo hai trục của BD và DD. Tiêu chí mô tả kết quả người nghiệm thu quan sát được; các chi tiết request, mã lỗi, bảng dữ liệu và thuật toán nằm ở DD. Test case QA có thể được thiết kế từ tài liệu trước khi code hoàn tất; mã test case được liên kết trong bảng truy vết khi kịch bản đã được review. Bằng chứng thực thi và trạng thái Đạt/Trượt chỉ ghi sau khi chạy trên hệ thống tương ứng.

| Trục | Đường dẫn | Nguồn chính | Phạm vi |
| :--- | :--- | :--- | :--- |
| Bounded Context | `modules/F{n}_{slug}.md` | `03-dd/api/`, `logic/`, `validation/`, `jobs/` | Hợp đồng máy chủ, quyền, lỗi, sự kiện và suy giảm |
| Màn hình | `screens/<nhóm>/<MÃ>_<slug>.md` | `03-dd/screens/<nhóm>/` | Trạng thái và luồng người dùng, điều hướng, phối hợp nhiều module |

Quy tắc truy vết: giữ nguyên mã `AC-nn` đã có trong từng file; tiêu chí mới lấy số tiếp theo. TDD màn trỏ đến TDD module cho nghiệp vụ đã được phủ, chỉ viết thêm hành vi của màn. Mỗi tiêu chí có tiền đề, hành động, kết quả mong đợi và nguồn; bảng truy vết nối tiêu chí với endpoint hoặc luồng DD và test case sau này. Không suy ra tiêu chí từ prototype khi RD/BD/DD chưa hỗ trợ.

## Tiến độ 2026-10-26

- Đã chuyển 6 bản TDD module và 17 bản TDD màn có sẵn vào hai trục, bổ sung 23 bản TDD màn còn thiếu. Hiện có **6/6 module và 40/40 màn**.
- Đã rà quyết định DD chốt ngày 2026-10-25/26, bổ sung AC cho Outbox F2/F5, quyền mã khung, chữ ký xem trước, snapshot testcase, hạn mức chạy thử, tự động mở rộng và các luồng màn thiếu. Cả 461 điểm kiểm DD màn đều có AC.
- Trạng thái TDD: **đợt 1 F2/F3/F4 đã đóng mốc thiết kế có điều kiện** theo [biên bản audit](testcases/AUDIT_DOT1.md); 476 ca chưa thực thi, golden P2 chờ codegen. Quy tắc `position` dùng chung cho hiển thị và chấm đã được chốt, có ca kiểm thử riêng.
