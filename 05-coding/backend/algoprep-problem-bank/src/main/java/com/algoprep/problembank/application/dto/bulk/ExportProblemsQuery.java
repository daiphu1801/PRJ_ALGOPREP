package com.algoprep.problembank.application.dto;

import java.util.List;

/**
 * Export request of DD F2 section 28.2: exactly one of {@code problemIds} (the selected rows) and
 * {@code filter} (everything the current filter matches). Both set or both missing is a 400.
 */
public record ExportProblemsQuery(
    AuthoringScope scope, List<java.util.UUID> problemIds, Filter filter) {

  /** The list filter without paging or sorting; raw text, validated by the handler. */
  public record Filter(String q, String level, String status) {}
}
