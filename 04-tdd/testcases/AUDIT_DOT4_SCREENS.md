# Audit thiết kế đợt 4 — 40 màn hình

> Cập nhật 2026-10-28. **Đóng review thiết kế theo AC/DD hiện hành**; **0 ca thực thi**.

Đã đối chiếu mệnh đề AC với Steps/Expected, fixture và điểm kiểm DD của **40 màn, 152 AC, 212 ca**. [Inventory 40 màn](screens/AUDIT_INVENTORY.md) dẫn riêng tới AC, DD, ca và ghi điểm đã review từng màn. [Quy ước fixture](screens/FIXTURE_CONVENTIONS.md) xác định alias UUID, actor/quyền, clock, biến thể chạy độc lập và ranh giới FE mock/tích hợp.

| Nhóm | Màn | AC | Ca / trace | Kết quả audit thiết kế |
| :--- | ---: | ---: | :--- | :--- |
| [Shared](screens/shared/README.md) | 7 | 24 | 53/53 | Đã review |
| [Users](screens/users/README.md) | 16 | 82 | 95/95 | Đã review |
| [Teacher](screens/teacher/README.md) | 7 | 17 | 28/28 | Đã review |
| [Admin](screens/admin/README.md) | 10 | 29 | 36/36 | Đã review |
| Tổng | 40 | 152 | 212/212 | Không có oracle sản phẩm cần quyết thêm trong phạm vi màn |

## Phát hiện đã sửa

| Cụm | Sửa oracle theo AC/DD |
| :--- | :--- |
| [Workspace USR0102](screens/users/USR0102_problem_detail.md) | Chặn gửi theo byte/file validation, không tự biến syntax thành kiểm trình duyệt; phân biệt lock/poll/reconnect; AI phụ lỗi ẩn phần phụ và không toast; nút sau chấm đúng hành trình kết quả/phân tích/phỏng vấn. |
| [Phân tích USR0301](screens/users/USR0301_solution_review.md), [phỏng vấn USR0302](screens/users/USR0302_mock_interview.md) | Một initial request phân tích theo AC; không gọi lại AI ngầm; nháp/backup trong localStorage, không giả API/DB draft; sửa setup lượt và trạng thái đóng. |
| [Cài đặt USR0503](screens/users/USR0503_settings.md) | defaultLanguage là một giá trị còn enabled; xuất hai scope submissions/transcripts với cooldown 5 phút, không tự thêm CSV/JSON quota. |
| [Luyện USR0402](screens/users/USR0402_interview_question_detail.md) | Phân biệt 201 FAILED và 429 không lưu; lịch sử/cursor/tổng lượt khớp; không tự coi POST retry là idempotent. |
| [Dashboard USR0601](screens/users/USR0601_dashboard.md), [kết quả USR0201](screens/users/USR0201_submission_result.md) | Thiếu ngày trong grid là lỗi dữ liệu theo AC; fixture Accepted phải all-pass; Sample/Hidden và dữ liệu lịch sử theo hợp đồng. |
| [Ngôn ngữ ADM0501](screens/admin/admin_screens_phase4.md) | Hai tab, ba dòng ngôn ngữ và đủ bốn khối snapshot khi lưu; dùng updatedAt phản hồi làm baseline, kiểm validation/conflict độc lập. |
| Shared/teacher/admin | Đối chiếu phạm vi quyền, giữ nháp, partial failure, pagination, sự kiện hạ tầng làm mới thủ công, CSV audit theo locale/che dữ liệu, self-impact và thao tác bị từ chối không ghi thành công. Chi tiết theo từng dòng inventory. |

TDD PL-AC-01 và AL-AC-01 đã làm rõ theo DD hiện hành về phần phụ im lặng khi lỗi và hai tab/four-block snapshot; không tạo quy tắc sản phẩm mới. Giữ nguyên ID testcase.

## Cổng sau coding

Checker xác nhận 212 ID/212 trace, không lỗi cấu trúc; kết luận ngữ nghĩa dựa trên đối chiếu nguồn, không dựa riêng vào checker. Chưa có seed thực, fault controls, build hoặc evidence thực thi. Khi FE chạy được có thể ghi run mock riêng; API/DB/event/job/quyền và E2E cần backend/hạ tầng. Không ca nào được ghi pass/fail.
