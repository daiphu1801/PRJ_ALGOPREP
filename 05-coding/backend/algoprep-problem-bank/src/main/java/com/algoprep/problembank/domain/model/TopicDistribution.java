package com.algoprep.problembank.domain.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Topic usage inside the actor's scope (DD F2 section 26.3). */
public record TopicDistribution(
    int totalProblemsWithTopic, int totalTopicsUsed, List<Entry> items) {

  public TopicDistribution {
    items = List.copyOf(items);
  }

  /** {@code percent} is {@code count / totalProblemsWithTopic * 100}, one decimal. */
  public record Entry(UUID topicId, String topicName, int count, BigDecimal percent) {}
}
