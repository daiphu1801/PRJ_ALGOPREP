package com.algoprep.problembank.application.dto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * {@code BulkProblemActionResultDto} (DD F2 section 1.5): every problem is handled on its own, so a
 * failure of one never stops the others and the answer is 200 even when all of them failed.
 */
public record BulkProblemActionResult(List<UUID> succeededIds, List<Failed> failedItems) {

  public BulkProblemActionResult {
    succeededIds = List.copyOf(succeededIds);
    failedItems = List.copyOf(failedItems);
  }

  /** One refused problem; {@code details} is null unless the error carries approved metadata. */
  public record Failed(UUID id, String code, String message, Map<String, Object> details) {}
}
