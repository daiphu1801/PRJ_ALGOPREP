package com.algoprep.aireview.application.dto.interview;

import com.algoprep.aireview.domain.model.interview.InterviewEntryType;
import com.algoprep.aireview.domain.model.interview.InterviewSessionStage;
import java.util.UUID;

/**
 * Query of endpoint 31A (the caller's own finished sessions). Every filter is optional (null = no
 * filter); {@code page} is 1-based.
 */
public record MySessionFilter(
    UUID userId,
    String search,
    InterviewEntryType entryType,
    InterviewSessionStage stage,
    String level,
    ScoreBand band,
    SortField sortBy,
    boolean descending,
    int page,
    int size) {

  /** Score bands of the screen: weak below 5, fair from 5 below 8, good from 8 (0 to 10 scale). */
  public enum ScoreBand {
    WEAK,
    FAIR,
    GOOD
  }

  public enum SortField {
    STARTED_AT,
    SCORE
  }
}
