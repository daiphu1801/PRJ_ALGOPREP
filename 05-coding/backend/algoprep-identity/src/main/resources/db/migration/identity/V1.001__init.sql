-- F1 identity: initial schema.
-- Source: 02-bd/database/F1_identity.md (tables 1.1-1.13, indexes section 2, migration order section 4).
-- Schema `identity` is created by 05-coding/database/init/01-schemas.sql, not here.
-- DEC-2026-1029-flyway-migration-tool: identity owns version range V1.NNN.
--
-- Conventions:
--   * Every reference column is UUID. Primary keys have no default: ids come from IdGeneratorPort.
--     Only seed rows below use gen_random_uuid().
--   * BD says ENUM; we use VARCHAR + CHECK with the same value sets so adding a value is a
--     one-line migration and JPA needs no PostgreSQL enum mapping.
--   * ON DELETE: foreign keys to users are NO ACTION (users are never hard-deleted); users.role_id is
--     RESTRICT; children of classes and permissions-by-role are CASCADE (BD intro of section 1).
--   * Read-model tables (section 1.12) carry no foreign keys: they are rebuilt from domain events.

SET LOCAL search_path = identity, public;

-- 1.2 roles ---------------------------------------------------------------------------------------
CREATE TABLE roles (
    id                  UUID PRIMARY KEY,
    code                VARCHAR(64)  NOT NULL UNIQUE,
    name                VARCHAR(100) NOT NULL,
    base_category       VARCHAR(16)  NOT NULL CHECK (base_category IN ('STUDENT', 'INSTRUCTOR', 'ADMIN')),
    is_system           BOOLEAN      NOT NULL DEFAULT false,
    permission_version  BIGINT       NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- 1.3 functions / actions (fixed seed data, read-only through the API) -------------------------------
CREATE TABLE functions (
    id    UUID PRIMARY KEY,
    code  VARCHAR(64)  NOT NULL UNIQUE,
    name  VARCHAR(100) NOT NULL
);

CREATE TABLE actions (
    id    UUID PRIMARY KEY,
    code  VARCHAR(16) NOT NULL UNIQUE
);

INSERT INTO roles (id, code, name, base_category, is_system) VALUES
    (gen_random_uuid(), 'STUDENT',    'Học viên',   'STUDENT',    true),
    (gen_random_uuid(), 'INSTRUCTOR', 'Giảng viên', 'INSTRUCTOR', true),
    (gen_random_uuid(), 'ADMIN',      'Quản trị',   'ADMIN',      true);

-- Display names are not specified by BD (only codes are); these are working labels [SoT: Suy luận].
INSERT INTO functions (id, code, name) VALUES
    (gen_random_uuid(), 'PROBLEM_AUTHORING',          'Soạn bài tập'),
    (gen_random_uuid(), 'TESTCASE_MANAGEMENT',        'Quản lý testcase'),
    (gen_random_uuid(), 'CLASS_MANAGEMENT',           'Quản lý lớp học'),
    (gen_random_uuid(), 'USER_MANAGEMENT',            'Quản lý người dùng'),
    (gen_random_uuid(), 'JUDGE_QUEUE_MONITOR',        'Giám sát hàng đợi chấm bài'),
    (gen_random_uuid(), 'LANGUAGE_CONFIG',            'Cấu hình ngôn ngữ'),
    (gen_random_uuid(), 'AI_CONFIG',                  'Cấu hình AI'),
    (gen_random_uuid(), 'AI_TOKEN_BUDGET',            'Ngân sách token AI'),
    (gen_random_uuid(), 'SYSTEM_AUDIT_LOG',           'Nhật ký hệ thống'),
    (gen_random_uuid(), 'INTERVIEW_BANK_MANAGEMENT',  'Quản lý ngân hàng câu hỏi phỏng vấn'),
    (gen_random_uuid(), 'PERMISSION_MATRIX',          'Ma trận phân quyền');

INSERT INTO actions (id, code) VALUES
    (gen_random_uuid(), 'CREATE'),
    (gen_random_uuid(), 'READ'),
    (gen_random_uuid(), 'UPDATE'),
    (gen_random_uuid(), 'DELETE');

-- 1.1 users ---------------------------------------------------------------------------------------
CREATE TABLE users (
    id                    UUID PRIMARY KEY,
    email                 VARCHAR(254) NOT NULL,
    password_hash         VARCHAR(100),
    display_name          VARCHAR(100) NOT NULL,
    role_id               UUID         NOT NULL REFERENCES roles (id) ON DELETE RESTRICT,
    current_position      VARCHAR(100),
    target_position       VARCHAR(100),
    password_changed_at   TIMESTAMPTZ,
    school_or_company     VARCHAR(150),
    phone                 VARCHAR(20),
    default_language      VARCHAR(16)  NOT NULL DEFAULT 'PYTHON',
    status                VARCHAR(24)  NOT NULL CHECK (status IN ('ACTIVE', 'DEACTIVATED', 'PENDING_VERIFICATION')),
    last_active_at        TIMESTAMPTZ,
    must_change_password  BOOLEAN      NOT NULL DEFAULT false,
    deactivation_source   VARCHAR(8)   CHECK (deactivation_source IN ('SELF', 'ADMIN')),
    deactivated_at        TIMESTAMPTZ,
    anonymized_at         TIMESTAMPTZ,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    -- deactivation_source is NULL unless the account is DEACTIVATED (BD 1.1).
    CONSTRAINT users_deactivation_source_only_when_deactivated
        CHECK (status = 'DEACTIVATED' OR deactivation_source IS NULL),
    -- Only self-deleted accounts are ever anonymized (BD 1.1).
    CONSTRAINT users_anonymized_only_when_self_deleted
        CHECK (anonymized_at IS NULL OR deactivation_source = 'SELF')
);

CREATE UNIQUE INDEX users_email_lower_uk ON users (lower(email));

-- 1.4 permissions ---------------------------------------------------------------------------------
CREATE TABLE permissions (
    id           UUID PRIMARY KEY,
    role_id      UUID        NOT NULL REFERENCES roles (id) ON DELETE CASCADE,
    function_id  UUID        NOT NULL REFERENCES functions (id) ON DELETE RESTRICT,
    action_id    UUID        NOT NULL REFERENCES actions (id) ON DELETE RESTRICT,
    granted      BOOLEAN     NOT NULL,
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by   UUID        REFERENCES users (id),
    CONSTRAINT permissions_role_function_action_uk UNIQUE (role_id, function_id, action_id)
);

-- Default matrix: 3 roles x 11 functions x 4 actions = 132 rows (BD 1.4).
--   ADMIN: everything granted. STUDENT: nothing (student rights follow the layer-1 role, not the matrix).
--   INSTRUCTOR: all four actions on CLASS_MANAGEMENT, PROBLEM_AUTHORING, TESTCASE_MANAGEMENT.
-- updated_by is NULL for seed rows: there is no acting user yet.
INSERT INTO permissions (id, role_id, function_id, action_id, granted)
SELECT gen_random_uuid(), r.id, f.id, a.id,
       CASE r.code
           WHEN 'ADMIN'      THEN true
           WHEN 'INSTRUCTOR' THEN f.code IN ('CLASS_MANAGEMENT', 'PROBLEM_AUTHORING', 'TESTCASE_MANAGEMENT')
           ELSE false
       END
FROM roles r
CROSS JOIN functions f
CROSS JOIN actions a;

-- 1.12 user_preferences ---------------------------------------------------------------------------
CREATE TABLE user_preferences (
    user_id                    UUID PRIMARY KEY REFERENCES users (id),
    streak_reminder_enabled    BOOLEAN     NOT NULL DEFAULT true,
    weekly_report_enabled      BOOLEAN     NOT NULL DEFAULT false,
    preferred_interview_level  VARCHAR(16) NOT NULL DEFAULT 'MIDDLE'
        CHECK (preferred_interview_level IN ('INTERN', 'JUNIOR', 'MIDDLE', 'SENIOR')),
    preferred_max_turns        SMALLINT    NOT NULL DEFAULT 12 CHECK (preferred_max_turns IN (8, 12, 16)),
    preferred_hint_allowed     BOOLEAN     NOT NULL DEFAULT true,
    updated_at                 TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 1.5 oauth_identities ----------------------------------------------------------------------------
CREATE TABLE oauth_identities (
    id                UUID PRIMARY KEY,
    user_id           UUID         NOT NULL REFERENCES users (id),
    provider          VARCHAR(16)  NOT NULL CHECK (provider IN ('GITHUB', 'GOOGLE')),
    provider_user_id  VARCHAR(128) NOT NULL,
    provider_email    VARCHAR(254),
    linked_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT oauth_identities_provider_user_uk UNIQUE (provider, provider_user_id)
);

-- Anonymization and "unlink" read by user.
CREATE INDEX oauth_identities_user_idx ON oauth_identities (user_id);

-- 1.6 refresh_tokens ------------------------------------------------------------------------------
CREATE TABLE refresh_tokens (
    id          UUID PRIMARY KEY,
    user_id     UUID         NOT NULL REFERENCES users (id),
    token_hash  VARCHAR(128) NOT NULL,
    family_id   UUID         NOT NULL,
    issued_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    expires_at  TIMESTAMPTZ  NOT NULL,
    revoked_at  TIMESTAMPTZ
);

CREATE UNIQUE INDEX refresh_tokens_token_hash_uk ON refresh_tokens (token_hash);
CREATE INDEX refresh_tokens_family_idx ON refresh_tokens (family_id);
CREATE INDEX refresh_tokens_user_revoked_idx ON refresh_tokens (user_id, revoked_at);
-- ADM0101 active-user counts and the 6-month retention job (BD 1.6).
CREATE INDEX refresh_tokens_issued_at_idx ON refresh_tokens (issued_at);

-- 1.8 classes -------------------------------------------------------------------------------------
CREATE TABLE classes (
    id             UUID PRIMARY KEY,
    instructor_id  UUID         NOT NULL REFERENCES users (id),
    name           VARCHAR(120) NOT NULL,
    description    VARCHAR(500),
    schedule       JSONB,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- "My classes" for an instructor; not listed in BD section 2 [SoT: Suy luận], FK column is queried by owner.
CREATE INDEX classes_instructor_idx ON classes (instructor_id);

-- 1.9 class_invite_codes --------------------------------------------------------------------------
CREATE TABLE class_invite_codes (
    id          UUID PRIMARY KEY,
    class_id    UUID        NOT NULL REFERENCES classes (id) ON DELETE CASCADE,
    code        VARCHAR(8)  NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ NOT NULL,
    created_by  UUID        NOT NULL REFERENCES users (id),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX class_invite_codes_class_idx ON class_invite_codes (class_id);

-- 1.10 class_enrollments --------------------------------------------------------------------------
CREATE TABLE class_enrollments (
    id          UUID PRIMARY KEY,
    class_id    UUID        NOT NULL REFERENCES classes (id) ON DELETE CASCADE,
    student_id  UUID        NOT NULL REFERENCES users (id),
    joined_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    left_at     TIMESTAMPTZ,
    CONSTRAINT class_enrollments_class_student_uk UNIQUE (class_id, student_id)
);

-- Active enrollments only (left_at IS NULL), per BD 1.10 and section 2.
CREATE INDEX class_enrollments_student_active_idx ON class_enrollments (student_id) WHERE left_at IS NULL;
CREATE INDEX class_enrollments_class_active_idx ON class_enrollments (class_id) WHERE left_at IS NULL;

-- 1.11 system_audit_logs --------------------------------------------------------------------------
CREATE TABLE system_audit_logs (
    id             UUID PRIMARY KEY,
    actor_user_id  UUID         NOT NULL REFERENCES users (id),
    action_type    VARCHAR(64)  NOT NULL,
    target_type    VARCHAR(32)  NOT NULL,
    target_id      VARCHAR(64),
    before_json    JSONB,
    after_json     JSONB,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX system_audit_logs_created_idx ON system_audit_logs (created_at DESC);
CREATE INDEX system_audit_logs_actor_created_idx ON system_audit_logs (actor_user_id, created_at DESC);
CREATE INDEX system_audit_logs_action_created_idx ON system_audit_logs (action_type, created_at DESC);

-- 1.13 system_settings ----------------------------------------------------------------------------
CREATE TABLE system_settings (
    key         VARCHAR(100) PRIMARY KEY,
    value_json  JSONB       NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by  UUID        REFERENCES users (id)
);

INSERT INTO system_settings (key, value_json) VALUES
    ('audit_log.retention_days', '90'::jsonb),
    ('audit_log.expired_policy', '"DELETE"'::jsonb);

-- 1.12 read models fed by domain events -----------------------------------------------------------
CREATE TABLE user_problem_best_score (
    user_id       UUID          NOT NULL,
    problem_id    UUID          NOT NULL,
    best_ratio    NUMERIC(5, 4) NOT NULL CHECK (best_ratio BETWEEN 0 AND 1),
    best_verdict  VARCHAR(32)   NOT NULL,
    updated_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, problem_id)
);

CREATE TABLE user_submission_stats (
    user_id                   UUID PRIMARY KEY,
    total_submissions         INT         NOT NULL DEFAULT 0,
    ac_eligible_count         INT         NOT NULL DEFAULT 0,
    accepted_count            INT         NOT NULL DEFAULT 0,
    first_try_accepted_count  INT         NOT NULL DEFAULT 0,
    top_language              VARCHAR(16),
    language_counts           JSONB       NOT NULL DEFAULT '{}'::jsonb,
    updated_at                TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- `day` is the UTC+7 calendar day (BD 1.15, section "Múi giờ cắt ngày").
CREATE TABLE user_daily_submission_stats (
    user_id           UUID NOT NULL,
    day               DATE NOT NULL,
    submission_count  INT  NOT NULL,
    PRIMARY KEY (user_id, day)
);

CREATE TABLE identity_recent_activity (
    id               UUID PRIMARY KEY,
    actor_user_id    UUID         NOT NULL REFERENCES users (id),
    scope_class_id   UUID         REFERENCES classes (id) ON DELETE SET NULL,
    activity_type    VARCHAR(40)  NOT NULL CHECK (activity_type IN (
        'SUBMISSION_ACCEPTED', 'SOLUTION_REVIEW_COMPLETED', 'MOCK_INTERVIEW_FINISHED',
        'CLASS_PROBLEM_PUBLISHED', 'STUDENT_STREAK_ACHIEVED')),
    ref_type         VARCHAR(32),
    ref_id           UUID,
    occurred_at      TIMESTAMPTZ  NOT NULL
);

CREATE INDEX identity_recent_activity_actor_idx ON identity_recent_activity (actor_user_id, occurred_at DESC);
CREATE INDEX identity_recent_activity_class_idx ON identity_recent_activity (scope_class_id, occurred_at DESC)
    WHERE scope_class_id IS NOT NULL;
