package com.algoprep.identity.domain.ports.out;

import java.util.UUID;

/** De-duplication of consumed domain events; one row per event id (DD api F1 section 2). */
public interface ProcessedEventRepository {

  /**
   * Records the event as applied, in the caller's transaction. False when the id was already there,
   * meaning the whole event was handled before and must be skipped.
   */
  boolean markProcessed(UUID eventId, String eventType);
}
