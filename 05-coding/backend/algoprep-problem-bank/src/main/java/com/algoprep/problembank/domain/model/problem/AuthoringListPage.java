package com.algoprep.problembank.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * One page of the management list (DD F2 section 25.3). {@code total} and {@code published} count
 * the actor's whole scope before any filter.
 */
public record AuthoringListPage(
    List<Item> items,
    int page,
    int size,
    long totalItems,
    int totalPages,
    long total,
    long published) {

  public AuthoringListPage {
    items = List.copyOf(items);
  }

  /** One row; {@code acRate} is null when the problem has no statistics yet. */
  public record Item(
      UUID id,
      String code,
      String title,
      List<String> topicNames,
      UUID levelId,
      String levelCode,
      String levelName,
      String status,
      int submissionCount,
      int assignedClassCount,
      java.math.BigDecimal acRate,
      int testcaseCount,
      Instant updatedAt) {

    public Item {
      topicNames = List.copyOf(topicNames);
    }
  }
}
