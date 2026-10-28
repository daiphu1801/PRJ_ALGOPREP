# Testcase QA F6 — ngân hàng phỏng vấn

Nguồn: [TDD F6](../../../modules/F6_interview-bank.md), DD F6 API/logic/validation. Có **110 ca thiết kế trong 7 cụm**, phủ 41/41 AC. Đã review cả7 cụm; còn oracle tràn mảng10 phần tử chờ owner; **chưa thực thi**.

| Cụm | AC | Ca |
| :--- | :--- | ---: |
| [Duyệt, tìm, đánh dấu, tự chấm](F6_learner_browse_recall.md) | 01–09 | 13 |
| [Nộp luyện và lịch sử](F6_learner_practice.md) | 10–21 | 23 |
| [Tiến độ và tổng hợp](F6_learner_progress.md) | 22–23 | 4 |
| [Danh mục chủ đề và độ khó](F6_admin_catalog.md) | 24–29 | 24 |
| [Quản trị câu hỏi](F6_admin_questions.md) | 30–35 | 19 |
| [Nhập CSV và nhật ký](F6_admin_csv_audit.md) | 36–38 | 14 |
| [Ngân sách, suy giảm và cổng tiến độ](F6_admin_budget_resilience_progress.md) | 39–41 | 13 |

Trace theo AC nằm trong [TDD F6](../../../modules/F6_interview-bank.md). `totalAttempts` độc lập before; trần100 ID /512 KiB /5 MiB đã có ca biên. [Audit đợt3](../../AUDIT_DOT3_F5_F6.md) ghi inventory/finding từng file; ca AC10-02 còn oracle11 phần tử chưa chốt. Các nhánh khác đã review thiết kế; chưa có kết quả thực thi.
