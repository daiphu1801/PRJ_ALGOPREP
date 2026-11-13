package com.algoprep.interviewbank.application.dto;

import java.util.List;

public record ImportResult(int importedCount, List<SkippedRow> skippedRows) {
  /** {@code line} is the physical start line of the record, echoed from the request. */
  public record SkippedRow(int line, String reasonCode, String detail) {}
}
