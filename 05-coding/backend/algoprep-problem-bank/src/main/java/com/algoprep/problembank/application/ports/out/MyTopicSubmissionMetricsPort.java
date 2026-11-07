package com.algoprep.problembank.application.ports.out;

import com.algoprep.problembank.domain.model.SuggestedProblemModels.TopicMetric;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * The caller's all-time submission counts per topic, from the judge module (F4 P5). The judge never
 * calls back into this module, and the caller is the current student principal.
 */
public interface MyTopicSubmissionMetricsPort {
  /** One batch of at most 50 topics; empty when the judge module cannot answer. */
  Optional<Map<UUID, TopicMetric>> read(Map<UUID, Set<UUID>> topicProblemIds);
}
