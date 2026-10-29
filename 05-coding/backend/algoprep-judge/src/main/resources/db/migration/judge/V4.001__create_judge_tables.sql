-- F4 judge-orchestration: schema `judge`, all 10 tables.
-- Source: 02-bd/database/F4_judge-orchestration.md (tables 1.1-1.10, indexes section 2, DDL contract section 8).
-- Schema `judge` is created by 05-coding/database/init/01-schemas.sql, not here.
-- DEC-2026-1029-flyway-migration-tool: judge owns version range V4.NNN. Seed rows live in V4.002.
--
-- Conventions (BD 8):
--   * BD says ENUM; we use VARCHAR + CHECK with the same value sets.
--   * Primary keys have no default: ids come from IdGeneratorPort. Singleton tables use id = 1.
--   * Every foreign key inside this schema is NO ACTION: a submission is never deleted.
--   * user_id, problem_id, testcase_id, updated_by point at other schemas and are plain UUIDs, no FK.
--   * Required timestamps default to now(); application code writes updated_at on every change.

SET LOCAL search_path = judge, public;

-- 1.5 language_configs (three fixed rows, seeded in V4.002) ---------------------------------------
CREATE TABLE language_configs (
    language                 VARCHAR(10)  PRIMARY KEY,
    time_limit_multiplier    NUMERIC(3,2) NOT NULL,
    memory_limit_multiplier  NUMERIC(3,2) NOT NULL DEFAULT 1.00,
    enabled                  BOOLEAN      NOT NULL DEFAULT TRUE,
    updated_at               TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_by               UUID,
    CONSTRAINT language_configs_language_ck CHECK (language IN ('JAVA', 'CPP', 'PYTHON')),
    CONSTRAINT language_configs_time_mult_ck CHECK (time_limit_multiplier BETWEEN 0.10 AND 9.99),
    CONSTRAINT language_configs_memory_mult_ck CHECK (memory_limit_multiplier BETWEEN 0.10 AND 9.99)
);

-- 1.9 grading_defaults (singleton) ----------------------------------------------------------------
CREATE TABLE grading_defaults (
    id                   SMALLINT    PRIMARY KEY,
    time_limit_ms        INT         NOT NULL DEFAULT 1000,
    memory_limit_mb      INT         NOT NULL DEFAULT 256,
    output_limit_kb      INT         NOT NULL DEFAULT 64,
    compile_timeout_sec  INT         NOT NULL DEFAULT 10,
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by           UUID,
    CONSTRAINT grading_defaults_singleton_ck CHECK (id = 1),
    CONSTRAINT grading_defaults_time_ck CHECK (time_limit_ms > 0),
    CONSTRAINT grading_defaults_memory_ck CHECK (memory_limit_mb > 0),
    CONSTRAINT grading_defaults_output_ck CHECK (output_limit_kb > 0),
    CONSTRAINT grading_defaults_compile_ck CHECK (compile_timeout_sec > 0)
);

-- 1.10 sandbox_settings (singleton) ---------------------------------------------------------------
CREATE TABLE sandbox_settings (
    id                           SMALLINT    PRIMARY KEY,
    network_access_enabled       BOOLEAN     NOT NULL DEFAULT FALSE,
    child_process_limit_enabled  BOOLEAN     NOT NULL DEFAULT TRUE,
    return_stderr_to_student     BOOLEAN     NOT NULL DEFAULT TRUE,
    updated_at                   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by                   UUID,
    CONSTRAINT sandbox_settings_singleton_ck CHECK (id = 1)
);

-- 1.8 generator_sandbox_settings (singleton) ------------------------------------------------------
CREATE TABLE generator_sandbox_settings (
    id                   SMALLINT    PRIMARY KEY,
    max_runtime_seconds  INT         NOT NULL DEFAULT 30,
    max_output_mb        INT         NOT NULL DEFAULT 100,
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by           UUID,
    CONSTRAINT generator_sandbox_settings_singleton_ck CHECK (id = 1),
    CONSTRAINT generator_sandbox_settings_runtime_ck CHECK (max_runtime_seconds BETWEEN 1 AND 300),
    CONSTRAINT generator_sandbox_settings_output_ck CHECK (max_output_mb BETWEEN 1 AND 1024)
);

-- 1.7 cluster_settings (singleton) ----------------------------------------------------------------
CREATE TABLE cluster_settings (
    id                       SMALLINT    PRIMARY KEY,
    queue_paused             BOOLEAN     NOT NULL DEFAULT FALSE,
    autoscale_enabled        BOOLEAN     NOT NULL DEFAULT TRUE,
    autoscale_job_threshold  INT         NOT NULL DEFAULT 20,
    jobs_per_page            INT         NOT NULL DEFAULT 20,
    error_retention_hours    INT         NOT NULL DEFAULT 24,
    latency_cap_seconds      INT         NOT NULL DEFAULT 10,
    updated_at               TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by               UUID,
    CONSTRAINT cluster_settings_singleton_ck CHECK (id = 1),
    CONSTRAINT cluster_settings_threshold_ck CHECK (autoscale_job_threshold > 0),
    CONSTRAINT cluster_settings_page_ck CHECK (jobs_per_page > 0),
    CONSTRAINT cluster_settings_retention_ck CHECK (error_retention_hours > 0),
    CONSTRAINT cluster_settings_latency_ck CHECK (latency_cap_seconds > 0)
);

-- 1.1 submissions ---------------------------------------------------------------------------------
CREATE TABLE submissions (
    id                               UUID         PRIMARY KEY,
    user_id                          UUID         NOT NULL,
    problem_id                       UUID         NOT NULL,
    spec_version                     INT,
    language                         VARCHAR(10)  NOT NULL,
    submission_model                 VARCHAR(20)  NOT NULL,
    source_code                      TEXT         NOT NULL,
    source_input_method              VARCHAR(12)  NOT NULL,
    status                           VARCHAR(20)  NOT NULL,
    failure_reason                   VARCHAR(20),
    compile_error_message            TEXT,
    passed_testcase_count            INT          NOT NULL DEFAULT 0,
    total_testcase_count             INT,
    score_ratio                      NUMERIC(4,3),
    runtime_ms                       INT,
    memory_kb                        INT,
    testcase_set_version_at_grading  INT          NOT NULL,
    submitted_at                     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    started_grading_at               TIMESTAMPTZ,
    finished_at                      TIMESTAMPTZ,
    updated_at                       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT submissions_language_ck CHECK (language IN ('JAVA', 'CPP', 'PYTHON')),
    CONSTRAINT submissions_model_ck CHECK (submission_model IN ('FUNCTION_WRAPPER', 'STANDARD_IO')),
    CONSTRAINT submissions_input_method_ck CHECK (source_input_method IN ('EDITOR', 'FILE_UPLOAD')),
    CONSTRAINT submissions_status_ck CHECK (status IN (
        'PENDING', 'COMPILING', 'JUDGING', 'ACCEPTED', 'WRONG_ANSWER',
        'TIME_LIMIT_EXCEEDED', 'RUNTIME_ERROR', 'COMPILE_ERROR', 'SYSTEM_ERROR')),
    CONSTRAINT submissions_failure_reason_ck CHECK (failure_reason IN (
        'WORKER_TIMEOUT', 'SANDBOX_FAULT', 'QUEUE_UNAVAILABLE', 'SPEC_UNAVAILABLE')),
    CONSTRAINT submissions_source_size_ck CHECK (octet_length(source_code) <= 65536),
    CONSTRAINT submissions_testcase_version_ck CHECK (testcase_set_version_at_grading > 0),
    CONSTRAINT submissions_spec_version_ck CHECK (spec_version IS NULL OR spec_version > 0),
    CONSTRAINT submissions_passed_ck CHECK (passed_testcase_count >= 0),
    CONSTRAINT submissions_total_ck CHECK (total_testcase_count IS NULL OR total_testcase_count >= 0),
    CONSTRAINT submissions_passed_le_total_ck CHECK (
        total_testcase_count IS NULL OR passed_testcase_count <= total_testcase_count),
    CONSTRAINT submissions_score_ck CHECK (score_ratio IS NULL OR score_ratio BETWEEN 0 AND 1),
    CONSTRAINT submissions_runtime_ck CHECK (runtime_ms IS NULL OR runtime_ms >= 0),
    CONSTRAINT submissions_memory_ck CHECK (memory_kb IS NULL OR memory_kb >= 0),
    CONSTRAINT submissions_failure_iff_system_error_ck CHECK (
        (status = 'SYSTEM_ERROR') = (failure_reason IS NOT NULL)),
    CONSTRAINT submissions_compile_message_ck CHECK (
        compile_error_message IS NULL OR status = 'COMPILE_ERROR'),
    CONSTRAINT submissions_finished_iff_terminal_ck CHECK (
        (status IN ('ACCEPTED', 'WRONG_ANSWER', 'TIME_LIMIT_EXCEEDED', 'RUNTIME_ERROR',
                    'COMPILE_ERROR', 'SYSTEM_ERROR')) = (finished_at IS NOT NULL))
);
CREATE INDEX submissions_status_updated_idx ON submissions (status, updated_at);
CREATE INDEX submissions_user_problem_idx ON submissions (user_id, problem_id, submitted_at DESC);
CREATE INDEX submissions_user_submitted_idx ON submissions (user_id, submitted_at DESC);
CREATE INDEX submissions_accepted_idx ON submissions (problem_id, language, status)
    WHERE status = 'ACCEPTED';

-- 1.3 judge_run_tokens ----------------------------------------------------------------------------
CREATE TABLE judge_run_tokens (
    id             UUID        PRIMARY KEY,
    submission_id  UUID        NOT NULL REFERENCES submissions (id),
    testcase_id    UUID        NOT NULL,
    token          UUID        NOT NULL,
    issued_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    consumed_at    TIMESTAMPTZ,
    CONSTRAINT judge_run_tokens_token_uk UNIQUE (token)
);
-- Exactly one consumed token per (submission, testcase); several unconsumed ones are allowed.
CREATE UNIQUE INDEX judge_run_tokens_consumed_uk ON judge_run_tokens (submission_id, testcase_id)
    WHERE consumed_at IS NOT NULL;

-- 1.2 submission_testcase_results -----------------------------------------------------------------
CREATE TABLE submission_testcase_results (
    id                     UUID         PRIMARY KEY,
    submission_id          UUID         NOT NULL REFERENCES submissions (id),
    testcase_id            UUID         NOT NULL,
    is_sample              BOOLEAN      NOT NULL,
    testcase_order         INT          NOT NULL,
    verdict                VARCHAR(3)   NOT NULL,
    runtime_ms             INT          NOT NULL,
    memory_kb              INT          NOT NULL,
    stderr_snippet         VARCHAR(4096),
    actual_output_snippet  VARCHAR(4096),
    judge_run_token_id     UUID         NOT NULL REFERENCES judge_run_tokens (id),
    created_at             TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT submission_testcase_results_unique_uk UNIQUE (submission_id, testcase_id),
    CONSTRAINT submission_testcase_results_verdict_ck CHECK (verdict IN ('AC', 'WA', 'TLE', 'MLE', 'RE')),
    CONSTRAINT submission_testcase_results_order_ck CHECK (testcase_order > 0),
    CONSTRAINT submission_testcase_results_runtime_ck CHECK (runtime_ms >= 0),
    CONSTRAINT submission_testcase_results_memory_ck CHECK (memory_kb >= 0),
    CONSTRAINT submission_testcase_results_output_sample_ck CHECK (
        actual_output_snippet IS NULL OR is_sample),
    CONSTRAINT submission_testcase_results_stderr_ck CHECK (
        stderr_snippet IS NULL OR (is_sample AND verdict <> 'AC'))
);
CREATE INDEX submission_testcase_results_order_idx
    ON submission_testcase_results (submission_id, testcase_order);
CREATE INDEX submission_testcase_results_created_idx
    ON submission_testcase_results (created_at, id);

-- 1.4 callback_logs (stays empty with the default go-judge adapter) -------------------------------
CREATE TABLE callback_logs (
    id                  UUID        PRIMARY KEY,
    judge_run_token_id  UUID        REFERENCES judge_run_tokens (id),
    raw_payload         JSONB       NOT NULL,
    received_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    outcome             VARCHAR(20) NOT NULL,
    CONSTRAINT callback_logs_outcome_ck CHECK (outcome IN (
        'ACCEPTED', 'DUPLICATE_IGNORED', 'INVALID_TOKEN', 'STATE_ALREADY_FINAL'))
);

-- 1.6 outbox_events -------------------------------------------------------------------------------
CREATE TABLE outbox_events (
    id                UUID        PRIMARY KEY,
    aggregate_type    VARCHAR(32) NOT NULL,
    aggregate_id      UUID        NOT NULL,
    event_type        VARCHAR(32) NOT NULL,
    payload           JSONB       NOT NULL,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    published_at      TIMESTAMPTZ,
    publish_attempts  INT         NOT NULL DEFAULT 0,
    CONSTRAINT outbox_events_aggregate_ck CHECK (aggregate_type = 'Submission'),
    CONSTRAINT outbox_events_type_ck CHECK (event_type IN ('SubmissionAccepted', 'SubmissionGraded')),
    CONSTRAINT outbox_events_attempts_ck CHECK (publish_attempts >= 0)
);
CREATE INDEX outbox_events_pending_idx ON outbox_events (created_at, id) WHERE published_at IS NULL;
CREATE INDEX outbox_events_published_idx ON outbox_events (published_at) WHERE published_at IS NOT NULL;
