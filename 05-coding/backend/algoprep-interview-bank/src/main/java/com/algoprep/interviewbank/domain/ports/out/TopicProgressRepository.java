package com.algoprep.interviewbank.domain.ports.out;

import com.algoprep.interviewbank.domain.model.TopicProgress;
import java.util.List;
import java.util.UUID;

/** Read side of the derived practice_history model, scoped to one user. */
public interface TopicProgressRepository {
  /** One row per topic ordered by sort order, counting ACTIVE questions only. */
  List<TopicProgress> listFor(UUID userId);
}
