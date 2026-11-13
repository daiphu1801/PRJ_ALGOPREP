package com.algoprep.problembank.application.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * The pending bulk upload kept in Redis (BR-14). Only sizes and server-made object keys are stored,
 * never testcase content.
 */
public final class TestcaseUploadSession {
  private TestcaseUploadSession() {}

  /** One side of a pair: the size the client declared and the key the server signed for it. */
  public record Side(long sizeBytes, String objectKey) {}

  public record Slot(
      String clientKey,
      UUID testcaseId,
      String visibility,
      String category,
      Side input,
      Side expectedOutput) {}

  /** What the first successful confirm answered; present once the session is CONFIRMED. */
  public record Confirmation(int testcaseSetVersion, Instant problemUpdatedAt) {}

  public record Session(
      UUID id, UUID problemId, UUID userId, List<Slot> slots, Confirmation confirmation) {
    public Session {
      slots = List.copyOf(slots);
    }

    public boolean confirmed() {
      return confirmation != null;
    }

    public Session confirmedWith(Confirmation result) {
      return new Session(id, problemId, userId, slots, result);
    }

    public Session pending() {
      return new Session(id, problemId, userId, slots, null);
    }
  }
}
