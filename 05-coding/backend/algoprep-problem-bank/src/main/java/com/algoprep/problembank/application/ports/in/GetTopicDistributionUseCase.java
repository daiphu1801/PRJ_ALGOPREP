package com.algoprep.problembank.application.ports.in;

import com.algoprep.problembank.application.dto.AuthoringScope;
import com.algoprep.problembank.domain.model.TopicDistribution;

/** Topic distribution #25 (DD F2 section 26). */
public interface GetTopicDistributionUseCase {
  TopicDistribution get(AuthoringScope scope);
}
