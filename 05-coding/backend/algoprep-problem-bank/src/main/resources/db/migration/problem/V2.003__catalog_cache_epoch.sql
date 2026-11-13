-- Durable invalidation prevents Redis recovery from resurrecting withdrawn catalog entries.
CREATE TABLE problem.catalog_cache_epoch (
 id SMALLINT PRIMARY KEY CHECK (id = 1), version BIGINT NOT NULL DEFAULT 0
);
INSERT INTO problem.catalog_cache_epoch (id, version) VALUES (1, 0);
