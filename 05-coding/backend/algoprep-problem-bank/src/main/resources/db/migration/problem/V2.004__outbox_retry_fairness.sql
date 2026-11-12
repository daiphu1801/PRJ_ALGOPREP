-- Failed/unroutable rows rotate behind untouched or older attempts, preventing batch starvation.
ALTER TABLE problem.outbox_events ADD COLUMN last_attempted_at TIMESTAMPTZ;
DROP INDEX problem.outbox_events_unpublished_idx;
CREATE INDEX outbox_events_unpublished_idx ON problem.outbox_events
    (COALESCE(last_attempted_at, created_at), created_at, id) WHERE published_at IS NULL;
