-- F2 problem-bank: initial catalogue seed. Source: 02-bd/database/F2_problem-bank.md section 7.2.
-- One-time seed (versioned, not repeatable): admins manage these rows afterwards, and a repeatable
-- migration would overwrite their edits on the next start.
-- No problems, testcases, tags, bookmarks, assignments, statistics, snapshots or events are seeded.

SET LOCAL search_path = problem, public;

INSERT INTO problem_levels (id, code, display_name, sort_order) VALUES
    ('20000000-0000-4000-8000-000000000001', 'EASY',   'Dễ',         1),
    ('20000000-0000-4000-8000-000000000002', 'MEDIUM', 'Trung bình', 2),
    ('20000000-0000-4000-8000-000000000003', 'HARD',   'Khó',        3);

INSERT INTO topics (id, code, name, sort_order) VALUES
    ('20000000-0000-4000-8000-000000000101', 'MANG',               'Mảng',               1),
    ('20000000-0000-4000-8000-000000000102', 'CHUOI',              'Chuỗi',              2),
    ('20000000-0000-4000-8000-000000000103', 'DANH-SACH-LIEN-KET', 'Danh sách liên kết', 3),
    ('20000000-0000-4000-8000-000000000104', 'CAY',                'Cây',                4),
    ('20000000-0000-4000-8000-000000000105', 'DO-THI',             'Đồ thị',             5),
    ('20000000-0000-4000-8000-000000000106', 'QUY-HOACH-DONG',     'Quy hoạch động',     6);

-- Singleton: id = 1, at most 2 successful AI testcase generations per problem. updated_by stays NULL
-- (no acting user yet).
INSERT INTO problem_bank_settings (id, max_ai_generations_per_problem) VALUES (1, 2);
