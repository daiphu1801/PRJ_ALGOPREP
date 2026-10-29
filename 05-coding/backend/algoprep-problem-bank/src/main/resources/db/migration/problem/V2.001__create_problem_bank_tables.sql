-- F2 problem-bank: schema `problem`, all 18 tables.
-- Source: 02-bd/database/F2_problem-bank.md (tables 1.1-1.16, indexes section 2, DDL contract section 7.1).
-- Schema `problem` is created by 05-coding/database/init/01-schemas.sql, not here.
-- DEC-2026-1029-flyway-migration-tool: problem-bank owns version range V2.NNN.
-- Seed rows live in V2.002; this file creates no data.
--
-- Conventions (BD 7.1):
--   * BD says ENUM; we use VARCHAR + CHECK with the same value sets (no PostgreSQL enum types).
--   * Primary keys have no default: ids come from IdGeneratorPort. Seed rows use fixed UUIDs (V2.002).
--   * Every foreign key inside this schema is NO ACTION. A problem is soft-deleted (deleted = true), never
--     removed, so no cascade is needed and the FKs are the safety net behind the use-case checks.
--   * Columns pointing at another schema (identity.users, identity.classes) are plain UUIDs, no FK.
--   * No trigger: immutability of author/code/snapshots is enforced by the use cases.
--   * Required timestamps default to now(); nullable ones (published_at, removed_at) have no default.
--   * Cross-row rules (at least one level left, checklist 8/2/2, AI quota) live in transactions, not CHECK.

SET LOCAL search_path = problem, public;

-- 1.2 topics (admin-managed catalogue) --------------------------------------------------------------
CREATE TABLE topics (
    id          UUID PRIMARY KEY,
    code        VARCHAR(64) NOT NULL,
    name        VARCHAR(60) NOT NULL,
    sort_order  SMALLINT    NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT topics_code_uk UNIQUE (code)
);
CREATE UNIQUE INDEX topics_name_lower_uk ON topics (lower(name));

-- 1.2a problem_levels (admin-managed catalogue, replaces the old difficulty enum) ---------------------
CREATE TABLE problem_levels (
    id            UUID PRIMARY KEY,
    code          VARCHAR(64) NOT NULL,
    display_name  VARCHAR(60) NOT NULL,
    sort_order    SMALLINT    NOT NULL,
    CONSTRAINT problem_levels_code_uk UNIQUE (code)
);
CREATE UNIQUE INDEX problem_levels_display_name_lower_uk ON problem_levels (lower(display_name));

-- 1.3 tags (free text, case-insensitive unique) -----------------------------------------------------
CREATE TABLE tags (
    id          UUID PRIMARY KEY,
    name        VARCHAR(50) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX tags_name_lower_uk ON tags (lower(name));

-- 1.1 problems --------------------------------------------------------------------------------------
CREATE TABLE problems (
    id                              UUID PRIMARY KEY,
    code                            VARCHAR(64)  NOT NULL,
    title                           VARCHAR(255) NOT NULL,
    statement_md                    TEXT,
    constraints_md                  TEXT         NOT NULL DEFAULT '',
    level_id                        UUID         NOT NULL REFERENCES problem_levels (id),
    status                          VARCHAR(16)  NOT NULL DEFAULT 'UNPUBLISHED'
        CONSTRAINT problems_status_ck CHECK (status IN ('UNPUBLISHED', 'PUBLISHED')),
    deleted                         BOOLEAN      NOT NULL DEFAULT false,
    function_wrapper_supported      BOOLEAN      NOT NULL DEFAULT true,
    time_limit_ms                   INT          NOT NULL CONSTRAINT problems_time_limit_ck CHECK (time_limit_ms > 0),
    memory_limit_mb                 INT          NOT NULL CONSTRAINT problems_memory_limit_ck CHECK (memory_limit_mb > 0),
    max_output_size_kb              INT          NOT NULL CONSTRAINT problems_max_output_ck CHECK (max_output_size_kb > 0),
    -- NULL means "not set for this problem": the 60/hour system default is read at check time, never stored.
    max_submissions_per_hour        INT
        CONSTRAINT problems_max_submissions_ck CHECK (max_submissions_per_hour > 0),
    duplicated_from_problem_id      UUID         REFERENCES problems (id),
    -- Length is not stated in BD; Big-O notation strings are short [SoT: Suy luan].
    known_optimal_complexity        VARCHAR(100),
    author_id                       UUID         NOT NULL,
    updated_by                      UUID,
    current_testcase_set_version    INT          NOT NULL DEFAULT 1
        CONSTRAINT problems_testcase_set_version_ck CHECK (current_testcase_set_version > 0),
    created_at                      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at                      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    published_at                    TIMESTAMPTZ,
    ai_testcase_generations_used    INT          NOT NULL DEFAULT 0
        CONSTRAINT problems_ai_generations_used_ck CHECK (ai_testcase_generations_used >= 0),
    ai_brief                        TEXT,
    no_full_code                    BOOLEAN      NOT NULL DEFAULT true,
    socratic_only                   BOOLEAN      NOT NULL DEFAULT true,
    -- code is unique across deleted problems too: a withdrawn slug keeps its place (BD 1.1).
    CONSTRAINT problems_code_uk UNIQUE (code),
    -- Soft delete always goes with UNPUBLISHED (BD 1.1).
    CONSTRAINT problems_deleted_unpublished_ck CHECK (NOT deleted OR status = 'UNPUBLISHED')
);
CREATE INDEX problems_status_deleted_idx ON problems (status, deleted);
-- Filtering by level and counting references when an admin deletes a level (BD 1.2a).
CREATE INDEX problems_level_idx ON problems (level_id);
-- "My problems" filter for instructors (BD 1.1).
CREATE INDEX problems_author_idx ON problems (author_id);

-- 1.2 / 1.3 many-to-many links ------------------------------------------------------------------------
CREATE TABLE problem_topics (
    problem_id  UUID NOT NULL REFERENCES problems (id),
    topic_id    UUID NOT NULL REFERENCES topics (id),
    PRIMARY KEY (problem_id, topic_id)
);
CREATE INDEX problem_topics_topic_idx ON problem_topics (topic_id);

CREATE TABLE problem_tags (
    problem_id  UUID NOT NULL REFERENCES problems (id),
    tag_id      UUID NOT NULL REFERENCES tags (id),
    PRIMARY KEY (problem_id, tag_id)
);
CREATE INDEX problem_tags_tag_idx ON problem_tags (tag_id);

-- 1.4 problem_specs (matching strategy + one shared function signature) ---------------------------------
CREATE TABLE problem_specs (
    id                 UUID PRIMARY KEY,
    problem_id         UUID         NOT NULL REFERENCES problems (id),
    matching_strategy  VARCHAR(32)  NOT NULL DEFAULT 'EXACT'
        CONSTRAINT problem_specs_matching_strategy_ck
        CHECK (matching_strategy IN ('EXACT', 'TRIMMED', 'EPSILON', 'UNORDERED_SET')),
    -- Up to 6 integer digits and 12 decimals. The application rejects values it cannot represent exactly
    -- BEFORE the database rounds them (BD 1.4), so this CHECK is a backstop, not the validator.
    epsilon_value      NUMERIC(18, 12)
        CONSTRAINT problem_specs_epsilon_positive_ck CHECK (epsilon_value > 0),
    stdin_format_md    TEXT,
    stdout_format_md   TEXT,
    -- Length is not stated in BD [SoT: Suy luan].
    function_name      VARCHAR(100),
    return_type        JSONB,
    parameters         JSONB,
    name_overrides     JSONB,
    spec_version       INT          NOT NULL DEFAULT 1
        CONSTRAINT problem_specs_spec_version_ck CHECK (spec_version > 0),
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT problem_specs_problem_uk UNIQUE (problem_id),
    -- EPSILON strategy requires a value (a positive CHECK alone would let NULL through).
    CONSTRAINT problem_specs_epsilon_required_ck CHECK (matching_strategy <> 'EPSILON' OR epsilon_value IS NOT NULL),
    -- Signature is all-or-nothing: either no signature yet, or name + return type + parameters together.
    -- parameters = '[]' is a valid (zero-argument) signature and is NOT NULL.
    CONSTRAINT problem_specs_signature_all_or_none_ck CHECK (
        (function_name IS NULL AND return_type IS NULL AND parameters IS NULL)
        OR (function_name IS NOT NULL AND return_type IS NOT NULL AND parameters IS NOT NULL))
);

-- 1.6 testcases -----------------------------------------------------------------------------------------
CREATE TABLE testcases (
    id                          UUID PRIMARY KEY,
    problem_id                  UUID        NOT NULL REFERENCES problems (id),
    visibility                  VARCHAR(16) NOT NULL
        CONSTRAINT testcases_visibility_ck CHECK (visibility IN ('SAMPLE', 'HIDDEN')),
    input_storage               VARCHAR(16) NOT NULL
        CONSTRAINT testcases_input_storage_ck CHECK (input_storage IN ('INLINE', 'MINIO')),
    input_inline                TEXT,
    -- Length is not stated in BD; object keys are short paths [SoT: Suy luan].
    input_object_key            VARCHAR(512),
    expected_output_storage     VARCHAR(16) NOT NULL
        CONSTRAINT testcases_output_storage_ck CHECK (expected_output_storage IN ('INLINE', 'MINIO')),
    expected_output_inline      TEXT,
    expected_output_object_key  VARCHAR(512),
    origin                      VARCHAR(16) NOT NULL DEFAULT 'MANUAL'
        CONSTRAINT testcases_origin_ck CHECK (origin IN ('MANUAL', 'AI')),
    -- No default on purpose: the application sets true for manual testcases and false for AI drafts.
    approved                    BOOLEAN     NOT NULL,
    category                    VARCHAR(32)
        CONSTRAINT testcases_category_ck CHECK (category IN (
            'TYPICAL', 'BOUNDARY_LOW', 'BOUNDARY_HIGH', 'DEGENERATE', 'DUPLICATE', 'SORTED', 'EXTREME')),
    position                    INT         NOT NULL CONSTRAINT testcases_position_ck CHECK (position > 0),
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                  TIMESTAMPTZ NOT NULL DEFAULT now(),
    -- Each side holds exactly one form; an empty inline string is valid data (BD 1.6).
    CONSTRAINT testcases_input_storage_branch_ck CHECK (
        (input_storage = 'INLINE' AND input_inline IS NOT NULL AND input_object_key IS NULL)
        OR (input_storage = 'MINIO' AND input_object_key IS NOT NULL AND input_inline IS NULL)),
    CONSTRAINT testcases_output_storage_branch_ck CHECK (
        (expected_output_storage = 'INLINE' AND expected_output_inline IS NOT NULL
            AND expected_output_object_key IS NULL)
        OR (expected_output_storage = 'MINIO' AND expected_output_object_key IS NOT NULL
            AND expected_output_inline IS NULL))
);
CREATE INDEX testcases_problem_visibility_idx ON testcases (problem_id, visibility);
-- Not unique: reordering renumbers the whole set inside one transaction.
CREATE INDEX testcases_problem_position_idx ON testcases (problem_id, position);

-- 1.11 problem_reference_solutions (one golden solution per problem) ------------------------------------
CREATE TABLE problem_reference_solutions (
    id          UUID PRIMARY KEY,
    problem_id  UUID        NOT NULL REFERENCES problems (id),
    language    VARCHAR(16) NOT NULL
        CONSTRAINT problem_reference_solutions_language_ck CHECK (language IN ('JAVA', 'CPP', 'PYTHON')),
    source_code TEXT        NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT problem_reference_solutions_problem_uk UNIQUE (problem_id)
);

-- 1.12 worked_examples (shown with an explanation, not part of grading) ---------------------------------
CREATE TABLE worked_examples (
    id              UUID PRIMARY KEY,
    problem_id      UUID NOT NULL REFERENCES problems (id),
    position        INT  NOT NULL CONSTRAINT worked_examples_position_ck CHECK (position > 0),
    input           TEXT NOT NULL,
    output          TEXT NOT NULL,
    explanation_md  TEXT
);
CREATE INDEX worked_examples_problem_position_idx ON worked_examples (problem_id, position);

-- 1.7 class_assignments -----------------------------------------------------------------------------------
CREATE TABLE class_assignments (
    id           UUID PRIMARY KEY,
    problem_id   UUID        NOT NULL REFERENCES problems (id),
    class_id     UUID        NOT NULL,
    assigned_by  UUID        NOT NULL,
    assigned_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    removed_at   TIMESTAMPTZ
);
-- At most one ACTIVE assignment per (problem, class); re-assigning after removal inserts a new row.
CREATE UNIQUE INDEX class_assignments_active_uk ON class_assignments (problem_id, class_id)
    WHERE removed_at IS NULL;
CREATE INDEX class_assignments_class_active_idx ON class_assignments (class_id)
    WHERE removed_at IS NULL;

-- 1.8 bookmarks -------------------------------------------------------------------------------------------
CREATE TABLE bookmarks (
    user_id     UUID        NOT NULL,
    problem_id  UUID        NOT NULL REFERENCES problems (id),
    note        TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, problem_id)
);

-- 1.9 problem_stats (read model fed by SubmissionGraded) -------------------------------------------------
CREATE TABLE problem_stats (
    problem_id        UUID PRIMARY KEY REFERENCES problems (id),
    submission_count  INT          NOT NULL DEFAULT 0
        CONSTRAINT problem_stats_submission_count_ck CHECK (submission_count >= 0),
    accepted_count    INT          NOT NULL DEFAULT 0
        CONSTRAINT problem_stats_accepted_count_ck CHECK (accepted_count >= 0),
    -- Stored derived value (accepted * 100.0 / submission_count), NULL while there are no submissions.
    ac_rate           NUMERIC(5, 2),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT problem_stats_accepted_le_submission_ck CHECK (accepted_count <= submission_count)
);
-- "AC rate below 30%" listing and ORDER BY ac_rate (BD 7.1).
CREATE INDEX problem_stats_ac_rate_idx ON problem_stats (ac_rate, problem_id) WHERE ac_rate IS NOT NULL;

-- 1.10 problem_bank_settings (singleton) -----------------------------------------------------------------
CREATE TABLE problem_bank_settings (
    id                              SMALLINT PRIMARY KEY CONSTRAINT problem_bank_settings_singleton_ck CHECK (id = 1),
    max_ai_generations_per_problem  INT         NOT NULL DEFAULT 2
        CONSTRAINT problem_bank_settings_max_ai_ck CHECK (max_ai_generations_per_problem BETWEEN 1 AND 10),
    updated_by                      UUID,
    updated_at                      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 1.13 testcase_generation_runs --------------------------------------------------------------------------
CREATE TABLE testcase_generation_runs (
    id                UUID PRIMARY KEY,
    problem_id        UUID         NOT NULL REFERENCES problems (id),
    generator_script  TEXT         NOT NULL,
    validator_script  TEXT         NOT NULL,
    seed              BIGINT       NOT NULL,
    model_id          VARCHAR(128) NOT NULL,
    prompt_version    VARCHAR(64)  NOT NULL,
    requested_by      UUID         NOT NULL,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    added_count       INT          NOT NULL CONSTRAINT testcase_generation_runs_added_count_ck CHECK (added_count > 0),
    dropped_json      JSONB        NOT NULL DEFAULT '[]'::jsonb
);
CREATE INDEX testcase_generation_runs_problem_created_idx ON testcase_generation_runs (problem_id, created_at);

-- 1.14 consumed_events (dedupe keys for problem_stats) ---------------------------------------------------
CREATE TABLE consumed_events (
    event_id     UUID PRIMARY KEY,
    consumed_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 1.15 testcase_set_snapshots (immutable published bundles) ---------------------------------------------
CREATE TABLE testcase_set_snapshots (
    problem_id      UUID         NOT NULL REFERENCES problems (id),
    version         INT          NOT NULL CONSTRAINT testcase_set_snapshots_version_ck CHECK (version > 0),
    -- Length is not stated in BD [SoT: Suy luan].
    object_key      VARCHAR(512) NOT NULL,
    testcase_count  INT          NOT NULL CONSTRAINT testcase_set_snapshots_count_ck CHECK (testcase_count > 0),
    content_sha256  CHAR(64)     NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    PRIMARY KEY (problem_id, version),
    CONSTRAINT testcase_set_snapshots_object_key_uk UNIQUE (object_key)
);

-- 1.16 outbox_events -------------------------------------------------------------------------------------
CREATE TABLE outbox_events (
    id                UUID PRIMARY KEY,
    aggregate_type    VARCHAR(40)  NOT NULL,
    aggregate_id      UUID         NOT NULL,
    event_type        VARCHAR(64)  NOT NULL,
    routing_key       VARCHAR(100) NOT NULL,
    payload           JSONB        NOT NULL,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    published_at      TIMESTAMPTZ,
    publish_attempts  INT          NOT NULL DEFAULT 0
        CONSTRAINT outbox_events_publish_attempts_ck CHECK (publish_attempts >= 0)
);
-- Relay reads unpublished rows in creation order; cleanup reads published rows by age (BD 1.16, 7.1).
CREATE INDEX outbox_events_unpublished_idx ON outbox_events (created_at, id) WHERE published_at IS NULL;
CREATE INDEX outbox_events_published_idx ON outbox_events (published_at, id) WHERE published_at IS NOT NULL;
