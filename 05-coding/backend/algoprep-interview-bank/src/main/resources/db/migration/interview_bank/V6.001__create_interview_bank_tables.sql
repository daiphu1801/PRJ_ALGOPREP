-- F6 interview-bank: schema `interview_bank`, all 8 tables plus sequence, unaccent search objects.
-- Source: 02-bd/database/F6_interview-bank.md (tables 1.1-1.7, indexes section 2, DDL contract section 9).
-- Schema `interview_bank` is created by 05-coding/database/init/01-schemas.sql, not here.
-- DEC-2026-1029-flyway-migration-tool: interview-bank owns version range V6.NNN. Seed rows live in V6.002.
--
-- Conventions (BD 9):
--   * BD says ENUM; we use VARCHAR + CHECK with the same value sets.
--   * Primary keys have no default: ids come from IdGeneratorPort. display_no is the one exception, it
--     takes its value from a sequence.
--   * Every foreign key inside this schema is NO ACTION, with one documented exception:
--     practice_history.topic_id is ON DELETE CASCADE (derived read model, BD 1.6).
--   * user_id and created_by point at identity.users and are plain UUIDs, no FK.
--   * user_answers has no updated_at and is only ever inserted.

SET LOCAL search_path = interview_bank, public;

-- Accent-insensitive search objects ---------------------------------------------------------------
-- unaccent is a trusted extension, so the database owner can create it. It installs into the schema
-- named here, not into public. f_unaccent names the dictionary with its schema so it does not depend on
-- the caller's search_path. Reindex the FTS index if the unaccent rules ever change.
CREATE EXTENSION unaccent SCHEMA interview_bank;

CREATE FUNCTION interview_bank.f_unaccent(text) RETURNS text
    LANGUAGE sql IMMUTABLE PARALLEL SAFE STRICT
    AS $$ SELECT interview_bank.unaccent('interview_bank.unaccent', $1) $$;

CREATE SEQUENCE interview_bank.interview_question_display_no_seq START WITH 1;

-- 1.1 question_topics (admin-managed catalogue) ---------------------------------------------------
CREATE TABLE question_topics (
    id                   UUID        PRIMARY KEY,
    code                 VARCHAR(64) NOT NULL,
    display_name         VARCHAR(60) NOT NULL,
    sort_order           SMALLINT    NOT NULL,
    uses_star_framework  BOOLEAN     NOT NULL DEFAULT FALSE,
    CONSTRAINT question_topics_code_uk UNIQUE (code)
);
CREATE UNIQUE INDEX question_topics_name_lower_uk ON question_topics (lower(display_name));

-- 1.1a question_levels (admin-managed catalogue, separate from the F2 difficulty catalogue) --------
CREATE TABLE question_levels (
    id            UUID        PRIMARY KEY,
    code          VARCHAR(64) NOT NULL,
    display_name  VARCHAR(60) NOT NULL,
    sort_order    SMALLINT    NOT NULL,
    CONSTRAINT question_levels_code_uk UNIQUE (code)
);
CREATE UNIQUE INDEX question_levels_name_lower_uk ON question_levels (lower(display_name));

-- 1.2 interview_questions -------------------------------------------------------------------------
CREATE TABLE interview_questions (
    id                       UUID         PRIMARY KEY,
    display_no               INT          NOT NULL
        DEFAULT nextval('interview_bank.interview_question_display_no_seq'),
    topic_id                 UUID         NOT NULL REFERENCES question_topics (id),
    level_id                 UUID         NOT NULL REFERENCES question_levels (id),
    title                    VARCHAR(255) NOT NULL,
    content_markdown         TEXT,
    suggested_approach       TEXT,
    sample_answer_framework  TEXT,
    core_keywords            JSONB,
    follow_up_questions      JSONB        NOT NULL DEFAULT '[]'::jsonb,
    status                   VARCHAR(7)   NOT NULL DEFAULT 'ACTIVE',
    created_by               UUID         NOT NULL,
    created_at               TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at               TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT interview_questions_display_no_uk UNIQUE (display_no),
    CONSTRAINT interview_questions_title_ck CHECK (length(btrim(title)) > 0),
    CONSTRAINT interview_questions_status_ck CHECK (status IN ('ACTIVE', 'RETIRED')),
    CONSTRAINT interview_questions_keywords_ck CHECK (
        core_keywords IS NULL OR jsonb_typeof(core_keywords) = 'array'),
    CONSTRAINT interview_questions_follow_up_ck CHECK (jsonb_typeof(follow_up_questions) = 'array')
);
ALTER SEQUENCE interview_bank.interview_question_display_no_seq
    OWNED BY interview_bank.interview_questions.display_no;
CREATE INDEX interview_questions_filter_idx ON interview_questions (topic_id, level_id, status);
-- 'simple' because PostgreSQL has no Vietnamese configuration; coalesce because content is optional.
CREATE INDEX interview_questions_fts_idx ON interview_questions USING gin (
    to_tsvector('simple', interview_bank.f_unaccent(title || ' ' || coalesce(content_markdown, ''))));

-- 1.3 answer_rubrics (independent of the F5 Mock Interview rubric) --------------------------------
CREATE TABLE answer_rubrics (
    id              UUID         PRIMARY KEY,
    question_id     UUID         NOT NULL REFERENCES interview_questions (id),
    criterion_code  VARCHAR(100) NOT NULL,
    description     TEXT,
    weight_percent  NUMERIC(5,2) NOT NULL,
    sort_order      SMALLINT     NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT answer_rubrics_weight_ck CHECK (weight_percent BETWEEN 0 AND 100),
    CONSTRAINT answer_rubrics_sort_ck CHECK (sort_order > 0),
    CONSTRAINT answer_rubrics_sort_uk UNIQUE (question_id, sort_order)
);
-- A unique index, not a UNIQUE constraint, because the key contains an expression.
CREATE UNIQUE INDEX answer_rubrics_code_lower_uk ON answer_rubrics (question_id, lower(criterion_code));

-- 1.4 bookmarks -----------------------------------------------------------------------------------
CREATE TABLE bookmarks (
    id           UUID        PRIMARY KEY,
    user_id      UUID        NOT NULL,
    question_id  UUID        NOT NULL REFERENCES interview_questions (id),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT bookmarks_user_question_uk UNIQUE (user_id, question_id)
);

-- 1.5 user_answers (immutable attempts) -----------------------------------------------------------
CREATE TABLE user_answers (
    id                    UUID        PRIMARY KEY,
    user_id               UUID        NOT NULL,
    question_id           UUID        NOT NULL REFERENCES interview_questions (id),
    answer_text           TEXT        NOT NULL,
    attempt_no            INT         NOT NULL,
    feedback_status       VARCHAR(9)  NOT NULL,
    feedback_result_json  JSONB,
    rubric_snapshot_json  JSONB       NOT NULL,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT user_answers_attempt_uk UNIQUE (user_id, question_id, attempt_no),
    CONSTRAINT user_answers_attempt_ck CHECK (attempt_no > 0),
    CONSTRAINT user_answers_status_ck CHECK (feedback_status IN ('COMPLETED', 'FAILED')),
    CONSTRAINT user_answers_feedback_ck CHECK (
        (feedback_status = 'COMPLETED') = (feedback_result_json IS NOT NULL)),
    CONSTRAINT user_answers_snapshot_ck CHECK (jsonb_typeof(rubric_snapshot_json) = 'array')
);
CREATE INDEX user_answers_user_created_idx ON user_answers (user_id, created_at DESC);
CREATE INDEX user_answers_question_idx ON user_answers (question_id);

-- 1.6 practice_history (derived progress read model) ----------------------------------------------
CREATE TABLE practice_history (
    id                   UUID        PRIMARY KEY,
    user_id              UUID        NOT NULL,
    topic_id             UUID        NOT NULL REFERENCES question_topics (id) ON DELETE CASCADE,
    questions_attempted  INT         NOT NULL DEFAULT 0,
    last_practiced_at    TIMESTAMPTZ,
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT practice_history_user_topic_uk UNIQUE (user_id, topic_id),
    CONSTRAINT practice_history_attempted_ck CHECK (questions_attempted >= 0),
    CONSTRAINT practice_history_last_ck CHECK (
        questions_attempted = 0 OR last_practiced_at IS NOT NULL)
);

-- 1.7 recall_ratings (latest self rating, overwritten in place) -----------------------------------
CREATE TABLE recall_ratings (
    id              UUID        PRIMARY KEY,
    user_id         UUID        NOT NULL,
    question_id     UUID        NOT NULL REFERENCES interview_questions (id),
    rating          VARCHAR(9)  NOT NULL,
    interval_days   INT         NOT NULL,
    next_review_at  TIMESTAMPTZ NOT NULL,
    rated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT recall_ratings_user_question_uk UNIQUE (user_id, question_id),
    CONSTRAINT recall_ratings_rating_ck CHECK (rating IN ('KNOWN', 'VAGUE', 'FORGOTTEN')),
    -- The rating to interval table is a fixed constant of the design (DD F6 BR-IB-06).
    CONSTRAINT recall_ratings_interval_ck CHECK (
        (rating = 'FORGOTTEN' AND interval_days = 1)
        OR (rating = 'VAGUE' AND interval_days = 3)
        OR (rating = 'KNOWN' AND interval_days = 7))
);
-- Not partial: now() is not IMMUTABLE, so PostgreSQL would refuse a WHERE next_review_at <= now() index.
CREATE INDEX recall_ratings_due_idx ON recall_ratings (user_id, next_review_at);
