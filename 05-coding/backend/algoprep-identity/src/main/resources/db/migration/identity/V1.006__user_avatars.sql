-- F1-35 user avatar metadata (BD database section 1.16). One row per user who has an avatar; the bytes
-- live in object storage under object_key. users rows are never hard-deleted, so the FK is NO ACTION.
CREATE TABLE identity.user_avatars (
    user_id      UUID PRIMARY KEY REFERENCES identity.users (id),
    object_key   VARCHAR(200) NOT NULL,
    content_type VARCHAR(20)  NOT NULL CHECK (content_type IN ('image/png', 'image/jpeg', 'image/webp')),
    size_bytes   INTEGER      NOT NULL CHECK (size_bytes BETWEEN 1 AND 524288),
    updated_at   TIMESTAMPTZ  NOT NULL
);
