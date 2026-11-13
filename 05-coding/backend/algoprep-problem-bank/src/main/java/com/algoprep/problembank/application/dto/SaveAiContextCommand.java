package com.algoprep.problembank.application.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * DD F2 section 39. The two flags are boxed so that a missing one is a 400 instead of a silent
 * {@code false}; {@code briefText} is data and is stored verbatim.
 */
public record SaveAiContextCommand(
    UUID problemId,
    UUID actorId,
    boolean admin,
    String briefText,
    Boolean noFullCode,
    Boolean questionOnly,
    Instant expectedUpdatedAt) {

  /** What was stored and the new {@code updated_at} of the problem. */
  public record Result(
      String briefText, boolean noFullCode, boolean questionOnly, Instant problemUpdatedAt) {}
}
