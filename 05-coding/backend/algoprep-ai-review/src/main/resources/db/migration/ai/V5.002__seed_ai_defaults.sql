-- F5 ai-review: initial seed. Source: 02-bd/database/F5_ai-review.md section 9 (seed) and 1.8, 5, 6.
-- One-time seed (versioned, not repeatable): admins publish new prompt versions and edit the budget
-- afterwards, and a repeatable migration would overwrite their edits on the next start.
-- Created_by and updated_by stay NULL (no acting user yet). model_id 'default' is an alias that the
-- backend model whitelist must contain. ADMIN switches model by publishing a new prompt version.
-- The server appends the fixed anti-injection sentences itself (DD F5 logic BR-AI-01 step 2), so the
-- stored instructions only describe the task. No fixture or mock prompt is used here.

SET LOCAL search_path = ai, public;

INSERT INTO prompt_templates
    (id, feature_code, version, status, system_instruction, response_schema_version, model_id,
     response_guards, published_at)
VALUES
    ('50000000-0000-4000-8000-000000000001', 'SOLUTION_REVIEW', 'v1.0', 'ACTIVE',
     'Bạn là trợ lý phân tích bài giải thuật toán. Dựa vào bài toán trong khối problem và mã nguồn trong khối user_code, hãy nhận diện hướng giải, đánh giá độ phức tạp thời gian và không gian kèm lập luận, nêu điểm mạnh, điểm cần cải thiện, các ca biên và rủi ro, chấm độ dễ đọc từ 1 đến 5, và gợi ý chủ đề nên học tiếp. Trả về đúng lược đồ JSON được yêu cầu.',
     'v1', 'default',
     '{"citeLines": true, "answerInVietnamese": true, "noFullSolution": true}'::jsonb, now()),
    ('50000000-0000-4000-8000-000000000002', 'MOCK_INTERVIEW', 'v1.0', 'ACTIVE',
     'Bạn là người phỏng vấn kỹ thuật thuật toán, trò chuyện 1 đối 1 với ứng viên qua ba giai đoạn: giải trình, phản biện, mở rộng quy mô. Đọc trạng thái phiên trong khối session_state, mỗi lượt chỉ hỏi một ý, bám sát câu trả lời của ứng viên và không tự đưa lời giải hoàn chỉnh. Trả về đúng lược đồ JSON được yêu cầu.',
     'v1', 'default',
     '{"citeLines": true, "answerInVietnamese": true, "noFullSolution": true}'::jsonb, now()),
    ('50000000-0000-4000-8000-000000000003', 'GENERATE_TESTCASE', 'v1.0', 'ACTIVE',
     'Bạn viết một script Python sinh dữ liệu đầu vào cho testcase của bài toán trong khối problem. Script không dùng mạng, không đọc đầu vào, dùng hạt giống cố định lấy từ biến môi trường SEED và chỉ in dữ liệu đầu vào đúng định dạng đề bài. Trả về đúng lược đồ JSON được yêu cầu.',
     'v1', 'default',
     NULL, now());

-- limit_tokens = 0 means "not configured": no automatic lock until an admin sets a limit.
-- period_start is the first day of the current month in UTC+7.
INSERT INTO ai_token_budget_configs (id, period_start)
VALUES (1, date_trunc('month', now() AT TIME ZONE 'Asia/Ho_Chi_Minh')::date);
