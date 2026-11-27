-- F4 judge-orchestration: one-row status of the grading consumers, written by the elected grading worker
-- after each scale poll and read by the admin queue snapshot (ADM0401). Decision:
-- DEC-2026-1122-f4-queue-consumers-and-throughput-on-adm0401. The API treats a row older than 60 seconds
-- as stale, so no cleanup job is needed. No data is seeded: the worker inserts the row on its first report.

SET LOCAL search_path = judge, public;

CREATE TABLE worker_status (
    id                  SMALLINT    PRIMARY KEY,
    consumers_target    INT         NOT NULL,
    consumers_active    INT         NOT NULL,
    reported_at         TIMESTAMPTZ NOT NULL,
    last_scale_at       TIMESTAMPTZ,
    last_scale_from     INT,
    last_scale_to       INT,
    last_scale_reason   VARCHAR(16),
    last_scale_pending  BIGINT,
    CONSTRAINT worker_status_singleton_ck CHECK (id = 1),
    CONSTRAINT worker_status_target_ck CHECK (consumers_target >= 1),
    CONSTRAINT worker_status_active_ck CHECK (consumers_active >= 0),
    CONSTRAINT worker_status_reason_ck CHECK (last_scale_reason IN ('BACKLOG', 'IDLE', 'DISABLED')),
    CONSTRAINT worker_status_last_scale_ck CHECK (
        (last_scale_at IS NULL AND last_scale_from IS NULL AND last_scale_to IS NULL
            AND last_scale_reason IS NULL AND last_scale_pending IS NULL)
        OR (last_scale_at IS NOT NULL AND last_scale_from IS NOT NULL AND last_scale_to IS NOT NULL
            AND last_scale_reason IS NOT NULL AND last_scale_pending IS NOT NULL))
);
