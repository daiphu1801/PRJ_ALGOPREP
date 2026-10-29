-- F6 interview-bank: initial catalogue seed. Source: 02-bd/database/F6_interview-bank.md section 9 (seed)
-- and 1.1, 1.1a. One-time seed (versioned, not repeatable): admins manage these rows afterwards, and a
-- repeatable migration would overwrite their edits on the next start.
-- No questions, rubrics, bookmarks, answers, progress or ratings are seeded.

SET LOCAL search_path = interview_bank, public;

INSERT INTO question_topics (id, code, display_name, sort_order, uses_star_framework) VALUES
    ('60000000-0000-4000-8000-000000000101', 'CS_THEORY',     'Lý thuyết CS', 1, FALSE),
    ('60000000-0000-4000-8000-000000000102', 'SYSTEM_DESIGN', 'System design', 2, FALSE),
    ('60000000-0000-4000-8000-000000000103', 'DATABASE',      'Database',      3, FALSE),
    ('60000000-0000-4000-8000-000000000104', 'LANGUAGE',      'Ngôn ngữ',      4, FALSE),
    ('60000000-0000-4000-8000-000000000105', 'BEHAVIORAL',    'Hành vi',       5, TRUE);

INSERT INTO question_levels (id, code, display_name, sort_order) VALUES
    ('60000000-0000-4000-8000-000000000001', 'EASY',   'Dễ',         1),
    ('60000000-0000-4000-8000-000000000002', 'MEDIUM', 'Trung bình', 2),
    ('60000000-0000-4000-8000-000000000003', 'HARD',   'Khó',        3);
