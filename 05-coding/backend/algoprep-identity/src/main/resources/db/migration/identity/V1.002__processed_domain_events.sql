-- F1: de-duplication of consumed domain events (DD api F1 section 2: idempotent by eventId).
-- One row per event already applied; the row is written in the same transaction as the read-model
-- update, so a redelivery after a crash either finds the row or finds nothing applied.
-- Decision: DEC-2026-1103-f1-processed-domain-events.
SET LOCAL search_path = identity, public;

CREATE TABLE processed_domain_events (
    event_id      UUID        PRIMARY KEY,
    event_type    VARCHAR(40) NOT NULL,
    processed_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX processed_domain_events_processed_at_idx ON processed_domain_events (processed_at);
