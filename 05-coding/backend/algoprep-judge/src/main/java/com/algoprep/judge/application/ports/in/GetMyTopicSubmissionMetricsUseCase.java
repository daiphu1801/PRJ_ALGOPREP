package com.algoprep.judge.application.ports.in;

import com.algoprep.judge.application.dto.MySubmissionMetrics.TopicRatio;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** P5 for problem-bank: no userId input and never calls back to problem-bank. */
public interface GetMyTopicSubmissionMetricsUseCase {
  Map<UUID, TopicRatio> readTopics(Map<UUID, Set<UUID>> topicProblemIds);
}
