-- Owner decision 2026-11-11 (DEC-2026-1111-instructor-interview-bank-default-grant): the default matrix
-- grants role INSTRUCTOR all four actions on INTERVIEW_BANK_MANAGEMENT. V1.001 seeded those four cells
-- as false, so an A2 account could not open SHR0301 on a fresh database.
-- Idempotent: only rows still false are touched; the 132-row count is unchanged. updated_by stays NULL
-- (seed, no acting user). updated_at is bumped to now(). Existing databases get the grant when this runs;
-- a custom-edited cell for this one role/function pair is overwritten to true.
WITH changed AS (
    UPDATE permissions
    SET granted = true, updated_at = now()
    WHERE granted = false
      AND role_id = (SELECT id FROM roles WHERE code = 'INSTRUCTOR')
      AND function_id = (SELECT id FROM functions WHERE code = 'INTERVIEW_BANK_MANAGEMENT')
    RETURNING 1
)
-- Bump the role version so cached permission snapshots are dropped (only when a cell really changed).
UPDATE roles SET permission_version = permission_version + 1
WHERE code = 'INSTRUCTOR' AND EXISTS (SELECT 1 FROM changed);
