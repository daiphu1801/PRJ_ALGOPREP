package com.algoprep.problembank.domain.ports.out;

import java.time.Instant;
import java.util.UUID;

/**
 * Single-row writes of the bulk actions (DD F2 sections 31 to 33); each joins the caller's
 * transaction.
 */
public interface ProblemBulkRepository {

  boolean levelExists(UUID levelId);

  boolean topicExists(UUID topicId);

  /** Sets the level and stamps {@code updated_by} and {@code updated_at}. */
  void setLevel(UUID problemId, UUID actorId, UUID levelId);

  /** Adds the link if absent and stamps the problem; false when it already existed (no change). */
  boolean addTopic(UUID problemId, UUID actorId, UUID topicId);

  /**
   * Soft delete (BR-09): flips {@code deleted} and withdraws the problem. Nothing that refers to
   * the problem is removed. Returns the new {@code updated_at}.
   */
  Instant markDeleted(UUID problemId, UUID actorId);
}
