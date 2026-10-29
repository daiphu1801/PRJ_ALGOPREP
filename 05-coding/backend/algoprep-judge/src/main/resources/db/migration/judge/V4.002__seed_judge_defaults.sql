-- F4 judge-orchestration: initial configuration seed. Source: 02-bd/database/F4_judge-orchestration.md
-- sections 1.5, 1.7-1.10 and the DDL contract in section 8 (defaults confirmed 2026-10-23).
-- One-time seed (versioned, not repeatable): admins edit these rows afterwards, and a repeatable
-- migration would overwrite their edits on the next start. updated_by stays NULL (no acting user yet).
-- No submissions, tokens, results, callback logs or outbox events are seeded.

SET LOCAL search_path = judge, public;

INSERT INTO language_configs (language, time_limit_multiplier, memory_limit_multiplier, enabled) VALUES
    ('JAVA',   2.00, 2.00, TRUE),
    ('CPP',    1.00, 1.00, TRUE),
    ('PYTHON', 3.00, 1.00, TRUE);

-- Column defaults of each singleton table carry the BD values.
INSERT INTO grading_defaults (id) VALUES (1);
INSERT INTO sandbox_settings (id) VALUES (1);
INSERT INTO generator_sandbox_settings (id) VALUES (1);
INSERT INTO cluster_settings (id) VALUES (1);
