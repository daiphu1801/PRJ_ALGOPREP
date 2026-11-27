-- Class events are persisted with the business transaction.
CREATE TABLE identity.outbox_events (
 id UUID PRIMARY KEY, aggregate_type VARCHAR(40) NOT NULL, aggregate_id UUID NOT NULL,
 event_type VARCHAR(64) NOT NULL, routing_key VARCHAR(100) NOT NULL, payload JSONB NOT NULL,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(), published_at TIMESTAMPTZ,
 publish_attempts INT NOT NULL DEFAULT 0 CHECK (publish_attempts >= 0)
);
CREATE INDEX identity_outbox_unpublished_idx ON identity.outbox_events (created_at, id)
 WHERE published_at IS NULL;
CREATE INDEX identity_outbox_published_idx ON identity.outbox_events (published_at, id)
 WHERE published_at IS NOT NULL;
