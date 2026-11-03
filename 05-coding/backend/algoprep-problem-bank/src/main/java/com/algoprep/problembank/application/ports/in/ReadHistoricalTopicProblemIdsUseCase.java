package com.algoprep.problembank.application.ports.in;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Read-only bridge contract needed by F4 metrics; includes retained withdrawn problem links. */
public interface ReadHistoricalTopicProblemIdsUseCase {
  Map<UUID, Set<UUID>> read(Set<UUID> topicIds);
}
