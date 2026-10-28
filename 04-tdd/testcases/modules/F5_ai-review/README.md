# Testcase QA F5 — AI review và mock interview

Nguồn: [TDD F5](../../../modules/F5_ai-review.md), DD F5 API/logic/validation/jobs. Đây là **144 ca thiết kế trong 7 cụm**, phủ 58/58 AC. Đã review cả 7 cụm; còn oracle tràn mảng10 phần tử chờ owner. Chưa có run record hay kết quả thực thi.

| Cụm | AC | Ca |
| :--- | :--- | ---: |
| [Phân tích bài giải](F5_solution_review.md) | 01–17 | 29 |
| [Khởi tạo phiên phỏng vấn](F5_mock_session_start.md) | 18–23 | 21 |
| [Lượt hỏi, trả lời và gợi ý](F5_mock_turns_hints.md) | 24–31 | 17 |
| [Chấm điểm và vòng đời phiên](F5_mock_scoring_lifecycle.md) | 32–37 | 15 |
| [Prompt, ngân sách và usage](F5_admin_prompt_budget_usage.md) | 38–48 | 35 |
| [Cổng tích hợp, sự kiện và suy giảm](F5_admin_ports_events_resilience.md) | 49–54 | 17 |
| [Job, ngân sách và vòng đời](F5_admin_jobs_budget_lifecycle.md) | 55–58 | 10 |

Trace từng AC nằm trong [TDD F5](../../../modules/F5_ai-review.md). [Audit đợt3](../../AUDIT_DOT3_F5_F6.md) ghi inventory và findings từng file. Ca AC01-04 có oracle11 phần tử chưa chốt; các nhánh khác đã review thiết kế. Kiểm hợp đồng bằng LLM stub có thể chuẩn bị trước; chất lượng AI thật và thực thi cần build/môi trường.
