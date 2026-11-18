-- Logout tombstones remain independent of refresh-token rotation/retention.
CREATE TABLE identity.session_revocations (
    family_id UUID PRIMARY KEY,
    revoked_at TIMESTAMPTZ NOT NULL
);
