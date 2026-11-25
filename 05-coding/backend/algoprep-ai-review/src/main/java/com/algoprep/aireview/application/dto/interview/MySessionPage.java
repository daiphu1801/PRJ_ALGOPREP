package com.algoprep.aireview.application.dto.interview;

import com.algoprep.aireview.domain.model.interview.InterviewEntryType;
import com.algoprep.aireview.domain.model.interview.InterviewSessionStage;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** One page of endpoint 31A. */
public record MySessionPage(List<Row> items, long totalItems) {
  public MySessionPage {
    items = List.copyOf(items);
  }

  /**
   * {@code overallScore} is null for an ABANDONED session or one with fewer than four rubric rows.
   */
  public record Row(
      UUID sessionId,
      InterviewEntryType entryType,
      InterviewSessionStage stage,
      String questionText,
      List<String> topics,
      String interviewerLevel,
      int turnCount,
      Integer maxTurns,
      Instant startedAt,
      Instant endedAt,
      BigDecimal overallScore) {
    public Row {
      topics = topics == null ? List.of() : List.copyOf(topics);
    }
  }
}
