package com.algoprep.interviewbank.domain.model;

import java.util.List;

/** Newest first; {@code totalAttempts} counts the whole history, not only this page. */
public record AttemptPage(List<AnswerAttempt> items, boolean hasMore, long totalAttempts) {
  public AttemptPage {
    items = List.copyOf(items);
  }
}
