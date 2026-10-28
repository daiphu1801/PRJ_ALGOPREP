# Mẫu run record testcase QA

> Chỉ sao chép mẫu này khi **thực sự chạy** testcase. File hiện tại là mẫu trống, không phải bằng chứng pass/fail.

| Trường | Giá trị khi thực thi |
| :--- | :--- |
| Run ID / thời điểm | `RUN-...`; UTC+7 và UTC |
| Testcase ID / phiên bản tài liệu | `TC-...`; commit SHA của bộ testcase |
| Build | Frontend SHA; backend SHA; DB migration; judge worker image/compiler; config version |
| Môi trường | URL/môi trường cô lập; loại AI stub/provider; Redis/RabbitMQ/MinIO; clock |
| Dữ liệu khởi tạo | Fixture IDs, seed version, tài khoản/vai trò, testcase-set snapshot; không ghi secret |
| Thao tác thực tế | Bước đã chạy và sai khác so với testcase nguồn |
| Kết quả thực tế | Response/UI/DB/event/job/log quan sát được, kèm thời điểm và request/correlation ID |
| Đối chiếu oracle | Expected nào đạt/trượt/chưa quan sát được, từng checkpoint độc lập |
| Trạng thái | `PASS`, `FAIL`, `BLOCKED`, hoặc `NOT_RUN`; lý do nếu blocked |
| Evidence | Link ảnh/log/query đã che secret/Hidden; hash nếu tệp lớn |
| Defect / rerun | ID defect, mức ảnh hưởng, build sửa, run ID chạy lại |
| Cleanup | Dữ liệu/queue/object/session đã xoá hoặc khôi phục, evidence diff |
| Người thực hiện / reviewer | Tên hoặc định danh nhóm QA |

Quy trình tối thiểu: chạy [smoke P0](PHASE5_SMOKE_E2E_SELECTION.md), ghi record từng ID; nếu fail thì mở defect và dừng nhánh phụ thuộc, sau đó chạy integration/E2E/hồi quy đã chọn từ [ma trận](PHASE5_REGRESSION_MATRIX.md). FE mock có loại môi trường riêng và không được ghi là pass backend/E2E. Các ca P0/P1 còn `NOT_RUN` hoặc `BLOCKED` phải được tổng hợp riêng trong báo cáo nghiệm thu.
