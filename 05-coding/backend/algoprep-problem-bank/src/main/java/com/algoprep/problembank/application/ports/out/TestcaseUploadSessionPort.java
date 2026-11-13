package com.algoprep.problembank.application.ports.out;

import com.algoprep.problembank.application.dto.TestcaseUploadSession.Session;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * Short-lived pending bulk uploads. Any failure of the store is an unchecked exception that the
 * caller turns into 503; a session that is missing or expired is simply empty.
 */
public interface TestcaseUploadSessionPort {
  void save(Session session, Duration ttl);

  Optional<Session> find(UUID uploadId);

  /** Rewrites a session that still exists and keeps the time it has left. */
  void update(Session session);
}
