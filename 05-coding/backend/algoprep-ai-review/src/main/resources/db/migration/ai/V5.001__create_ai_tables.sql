-- F5 ai-review: schema `ai`, all 11 tables.
-- Source: 02-bd/database/F5_ai-review.md (tables 1.1-1.10, indexes section 2, DDL contract section 9).
-- Schema `ai` is created by 05-coding/database/init/01-schemas.sql, not here.
-- DEC-2026-1029-flyway-migration-tool: ai-review owns version range V5.NNN. Seed rows live in V5.002.
--
-- Conventions (BD 9):
--   * BD says ENUM; we use VARCHAR + CHECK with the same value sets.
--   * Primary keys have no default: ids come from IdGeneratorPort. Seed rows use fixed UUIDs.
--   * Every foreign key inside this schema is NO ACTION. Prompts are archived and reviews and
--     sessions are kept, so nothing here is deleted by a cascade.
--   * user_id, problem_id, submission_id, owner_id, created_by, updated_by point at other schemas or
--     are polymorphic and are plain UUIDs, no FK.
--   * Cross-row rules (four rubric rows per session, one OPEN alert per user) live in the application.

SET LOCAL search_path = ai, public;

-- 1.1 prompt_templates ------------------------------------------------------------------------------
CREATE TABLE prompt_templates (
    id                       UUID         PRIMARY KEY,
    feature_code             VARCHAR(20)  NOT NULL,
    version                  VARCHAR(20)  NOT NULL,
    status                   VARCHAR(10)  NOT NULL,
    system_instruction       TEXT         NOT NULL,
    response_schema_version  VARCHAR(20)  NOT NULL,
    model_id                 VARCHAR(50)  NOT NULL,
    response_guards          JSONB,
    created_by               UUID,
    created_at               TIMESTAMPTZ  NOT NULL DEFAULT now(),
    published_at             TIMESTAMPTZ,
    CONSTRAINT prompt_templates_feature_ck CHECK (
        feature_code IN ('SOLUTION_REVIEW', 'MOCK_INTERVIEW', 'GENERATE_TESTCASE')),
    CONSTRAINT prompt_templates_status_ck CHECK (status IN ('DRAFT', 'ACTIVE', 'ARCHIVED')),
    CONSTRAINT prompt_templates_instruction_ck CHECK (length(system_instruction) > 0),
    CONSTRAINT prompt_templates_guards_ck CHECK (
        (feature_code = 'GENERATE_TESTCASE') = (response_guards IS NULL)),
    CONSTRAINT prompt_templates_published_ck CHECK ((status = 'DRAFT') = (published_at IS NULL)),
    CONSTRAINT prompt_templates_version_uk UNIQUE (feature_code, version)
);
-- At most one ACTIVE and at most one DRAFT per feature. The seed provides the first ACTIVE rows.
CREATE UNIQUE INDEX prompt_templates_active_uk ON prompt_templates (feature_code)
    WHERE status = 'ACTIVE';
CREATE UNIQUE INDEX prompt_templates_draft_uk ON prompt_templates (feature_code)
    WHERE status = 'DRAFT';

-- 1.8 ai_token_budget_configs (singleton) -----------------------------------------------------------
CREATE TABLE ai_token_budget_configs (
    id                  SMALLINT    PRIMARY KEY,
    period              VARCHAR(10) NOT NULL DEFAULT 'MONTHLY',
    limit_tokens        BIGINT      NOT NULL DEFAULT 0,
    period_start        DATE        NOT NULL,
    locked_at           TIMESTAMPTZ,
    warn_percent        SMALLINT    NOT NULL DEFAULT 70,
    review_per_hour     SMALLINT    NOT NULL DEFAULT 10,
    interview_per_hour  SMALLINT    NOT NULL DEFAULT 3,
    cooldown_seconds    SMALLINT    NOT NULL DEFAULT 20,
    updated_by          UUID,
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ai_token_budget_configs_singleton_ck CHECK (id = 1),
    CONSTRAINT ai_token_budget_configs_period_ck CHECK (period = 'MONTHLY'),
    -- 0 means "not configured": no lock, no forecast.
    CONSTRAINT ai_token_budget_configs_limit_ck CHECK (limit_tokens >= 0),
    CONSTRAINT ai_token_budget_configs_warn_ck CHECK (warn_percent BETWEEN 1 AND 100),
    CONSTRAINT ai_token_budget_configs_review_ck CHECK (review_per_hour BETWEEN 1 AND 999),
    CONSTRAINT ai_token_budget_configs_interview_ck CHECK (interview_per_hour BETWEEN 1 AND 999),
    CONSTRAINT ai_token_budget_configs_cooldown_ck CHECK (cooldown_seconds BETWEEN 1 AND 999)
);

-- 1.8a ai_model_prices (seeded empty) ---------------------------------------------------------------
CREATE TABLE ai_model_prices (
    id                      UUID          PRIMARY KEY,
    model_name              VARCHAR(100)  NOT NULL,
    usd_per_million_tokens  NUMERIC(10,4) NOT NULL DEFAULT 0,
    updated_at              TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT ai_model_prices_price_ck CHECK (usd_per_million_tokens >= 0)
);
CREATE UNIQUE INDEX ai_model_prices_name_lower_uk ON ai_model_prices (lower(model_name));

-- 1.3 solution_reviews ------------------------------------------------------------------------------
CREATE TABLE solution_reviews (
    id                  UUID        PRIMARY KEY,
    submission_id       UUID        NOT NULL,
    user_id             UUID        NOT NULL,
    problem_id          UUID        NOT NULL,
    language            VARCHAR(10) NOT NULL,
    source_hash         CHAR(64)    NOT NULL,
    prompt_template_id  UUID        NOT NULL REFERENCES prompt_templates (id),
    result_json         JSONB       NOT NULL,
    readability_score   SMALLINT    NOT NULL,
    served_from_cache   BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT solution_reviews_submission_uk UNIQUE (submission_id),
    CONSTRAINT solution_reviews_language_ck CHECK (language IN ('JAVA', 'CPP', 'PYTHON')),
    CONSTRAINT solution_reviews_hash_ck CHECK (source_hash ~ '^[0-9a-f]{64}$'),
    CONSTRAINT solution_reviews_readability_ck CHECK (readability_score BETWEEN 1 AND 5)
);
CREATE INDEX solution_reviews_user_created_idx ON solution_reviews (user_id, created_at DESC);

-- 1.5 rubric_scores (Mock Interview only; owner_id is the session id, no FK) -------------------------
CREATE TABLE rubric_scores (
    id              UUID         PRIMARY KEY,
    owner_type      VARCHAR(20)  NOT NULL,
    owner_id        UUID         NOT NULL,
    criterion_code  VARCHAR(30)  NOT NULL,
    score           NUMERIC(4,2) NOT NULL,
    comment         TEXT         NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT rubric_scores_owner_uk UNIQUE (owner_type, owner_id, criterion_code),
    CONSTRAINT rubric_scores_owner_type_ck CHECK (owner_type = 'MOCK_INTERVIEW'),
    CONSTRAINT rubric_scores_criterion_ck CHECK (criterion_code IN (
        'CLARITY', 'TECHNICAL_ACCURACY', 'PUSHBACK_HANDLING', 'COMPLEXITY_AWARENESS')),
    CONSTRAINT rubric_scores_score_ck CHECK (score BETWEEN 0 AND 10)
);

-- 1.4 interview_sessions ----------------------------------------------------------------------------
CREATE TABLE interview_sessions (
    id                 UUID          PRIMARY KEY,
    user_id            UUID          NOT NULL,
    entry_type         VARCHAR(20)   NOT NULL,
    submission_id      UUID,
    question_ref       VARCHAR(64),
    question_text      VARCHAR(2000),
    topics             JSONB,
    interviewer_level  VARCHAR(10),
    max_turns          SMALLINT,
    hint_allowed       BOOLEAN,
    hint_count         SMALLINT      NOT NULL DEFAULT 0,
    stage              VARCHAR(10)   NOT NULL,
    turn_count         INT           NOT NULL DEFAULT 0,
    started_at         TIMESTAMPTZ   NOT NULL DEFAULT now(),
    ended_at           TIMESTAMPTZ,
    updated_at         TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT interview_sessions_entry_ck CHECK (
        entry_type IN ('SUBMISSION', 'QUESTION_BANK', 'TOPIC_SELECTION')),
    CONSTRAINT interview_sessions_level_ck CHECK (
        interviewer_level IS NULL OR interviewer_level IN ('INTERN', 'JUNIOR', 'MIDDLE', 'SENIOR')),
    CONSTRAINT interview_sessions_stage_ck CHECK (
        stage IN ('EXPLAIN', 'CHALLENGE', 'SCALE_UP', 'COMPLETED', 'ABANDONED')),
    -- BD 1.4: the fields that apply depend on the entry type. Same-row rule, so a CHECK.
    CONSTRAINT interview_sessions_entry_fields_ck CHECK (
        CASE entry_type
            WHEN 'SUBMISSION' THEN
                submission_id IS NOT NULL AND question_ref IS NULL AND question_text IS NULL
                AND topics IS NULL AND interviewer_level IS NULL AND max_turns IS NULL
                AND hint_allowed IS NULL
            WHEN 'QUESTION_BANK' THEN
                submission_id IS NULL AND question_ref IS NOT NULL AND question_text IS NOT NULL
                AND topics IS NULL AND interviewer_level IS NOT NULL AND max_turns IS NOT NULL
                AND hint_allowed IS NOT NULL
            WHEN 'TOPIC_SELECTION' THEN
                submission_id IS NULL AND question_ref IS NULL AND question_text IS NULL
                AND topics IS NOT NULL AND interviewer_level IS NOT NULL AND max_turns IS NOT NULL
                AND hint_allowed IS NOT NULL
        END),
    CONSTRAINT interview_sessions_topics_ck CHECK (topics IS NULL OR jsonb_typeof(topics) = 'array'),
    CONSTRAINT interview_sessions_max_turns_ck CHECK (max_turns IS NULL OR max_turns > 0),
    CONSTRAINT interview_sessions_turn_count_ck CHECK (
        turn_count >= 0 AND (max_turns IS NULL OR turn_count <= max_turns)),
    CONSTRAINT interview_sessions_hint_count_ck CHECK (
        hint_count >= 0 AND (hint_count = 0 OR hint_allowed IS TRUE)),
    CONSTRAINT interview_sessions_ended_ck CHECK (
        (stage IN ('COMPLETED', 'ABANDONED')) = (ended_at IS NOT NULL))
);
CREATE INDEX interview_sessions_user_started_idx ON interview_sessions (user_id, started_at DESC);
CREATE INDEX interview_sessions_stage_updated_idx ON interview_sessions (stage, updated_at);

-- 1.4a interview_turns (durable transcript) ---------------------------------------------------------
CREATE TABLE interview_turns (
    id                    UUID        PRIMARY KEY,
    interview_session_id  UUID        NOT NULL REFERENCES interview_sessions (id),
    turn_index            INT         NOT NULL,
    role                  VARCHAR(4)  NOT NULL,
    stage_at_turn         VARCHAR(10) NOT NULL,
    content               TEXT        NOT NULL,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT interview_turns_index_uk UNIQUE (interview_session_id, turn_index),
    CONSTRAINT interview_turns_index_ck CHECK (turn_index > 0),
    CONSTRAINT interview_turns_role_ck CHECK (role IN ('USER', 'AI')),
    CONSTRAINT interview_turns_stage_ck CHECK (stage_at_turn IN ('EXPLAIN', 'CHALLENGE', 'SCALE_UP'))
);

-- 1.6 token_usage (ledger; call_id makes a retried write idempotent) ---------------------------------
CREATE TABLE token_usage (
    id                 UUID         PRIMARY KEY,
    call_id            UUID         NOT NULL,
    user_id            UUID         NOT NULL,
    feature_code       VARCHAR(20)  NOT NULL,
    problem_id         UUID,
    owner_id           UUID         NOT NULL,
    tokens_prompt      INT          NOT NULL,
    tokens_completion  INT          NOT NULL,
    model_name         VARCHAR(100) NOT NULL,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT token_usage_call_uk UNIQUE (call_id),
    CONSTRAINT token_usage_feature_ck CHECK (feature_code IN (
        'SOLUTION_REVIEW', 'MOCK_INTERVIEW', 'GENERATE_TESTCASE', 'PRACTICE_FEEDBACK')),
    CONSTRAINT token_usage_tokens_ck CHECK (tokens_prompt >= 0 AND tokens_completion >= 0),
    CONSTRAINT token_usage_problem_ck CHECK (
        CASE feature_code
            WHEN 'SOLUTION_REVIEW' THEN problem_id IS NOT NULL
            WHEN 'GENERATE_TESTCASE' THEN problem_id IS NOT NULL AND problem_id = owner_id
            ELSE problem_id IS NULL
        END)
);
CREATE INDEX token_usage_created_feature_idx ON token_usage (created_at, feature_code);
CREATE INDEX token_usage_user_created_idx ON token_usage (user_id, created_at);

-- 1.7 ai_cache_entries ------------------------------------------------------------------------------
CREATE TABLE ai_cache_entries (
    id                  UUID        PRIMARY KEY,
    problem_id          UUID        NOT NULL,
    language            VARCHAR(10) NOT NULL,
    source_hash         CHAR(64)    NOT NULL,
    prompt_template_id  UUID        NOT NULL REFERENCES prompt_templates (id),
    solution_review_id  UUID        NOT NULL REFERENCES solution_reviews (id),
    hit_count           INT         NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ai_cache_entries_key_uk UNIQUE (problem_id, language, source_hash, prompt_template_id),
    CONSTRAINT ai_cache_entries_language_ck CHECK (language IN ('JAVA', 'CPP', 'PYTHON')),
    CONSTRAINT ai_cache_entries_hash_ck CHECK (source_hash ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ai_cache_entries_hits_ck CHECK (hit_count >= 0)
);

-- 1.9 ai_usage_anomaly_alerts (soft alerts, never lock an account) -----------------------------------
CREATE TABLE ai_usage_anomaly_alerts (
    id             UUID         PRIMARY KEY,
    user_id        UUID         NOT NULL,
    window_start   TIMESTAMPTZ  NOT NULL,
    window_end     TIMESTAMPTZ  NOT NULL,
    call_count     INT          NOT NULL,
    baseline_avg   NUMERIC(8,2) NOT NULL,
    ratio          NUMERIC(5,2) NOT NULL,
    status         VARCHAR(10)  NOT NULL DEFAULT 'OPEN',
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    reviewed_at    TIMESTAMPTZ,
    CONSTRAINT ai_usage_anomaly_alerts_status_ck CHECK (status IN ('OPEN', 'REVIEWED')),
    CONSTRAINT ai_usage_anomaly_alerts_window_ck CHECK (window_end > window_start),
    CONSTRAINT ai_usage_anomaly_alerts_numbers_ck CHECK (
        call_count >= 0 AND baseline_avg >= 0 AND ratio >= 0),
    CONSTRAINT ai_usage_anomaly_alerts_reviewed_ck CHECK (
        (status = 'REVIEWED') = (reviewed_at IS NOT NULL))
);

-- 1.10 outbox_events --------------------------------------------------------------------------------
CREATE TABLE outbox_events (
    id                UUID        PRIMARY KEY,
    aggregate_type    VARCHAR(20) NOT NULL,
    aggregate_id      UUID        NOT NULL,
    event_type        VARCHAR(32) NOT NULL,
    payload           JSONB       NOT NULL,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    published_at      TIMESTAMPTZ,
    publish_attempts  INT         NOT NULL DEFAULT 0,
    CONSTRAINT outbox_events_pair_ck CHECK (
        (aggregate_type = 'SolutionReview' AND event_type = 'SolutionReviewCompleted')
        OR (aggregate_type = 'InterviewSession' AND event_type = 'MockInterviewFinished')),
    CONSTRAINT outbox_events_attempts_ck CHECK (publish_attempts >= 0)
);
CREATE INDEX outbox_events_pending_idx ON outbox_events (created_at, id) WHERE published_at IS NULL;
CREATE INDEX outbox_events_published_idx ON outbox_events (published_at) WHERE published_at IS NOT NULL;
