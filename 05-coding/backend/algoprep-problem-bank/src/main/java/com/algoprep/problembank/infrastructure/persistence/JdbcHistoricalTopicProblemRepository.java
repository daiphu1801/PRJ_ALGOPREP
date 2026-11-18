package com.algoprep.problembank.infrastructure.persistence;

import com.algoprep.problembank.domain.ports.out.HistoricalTopicProblemRepository;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class JdbcHistoricalTopicProblemRepository implements HistoricalTopicProblemRepository {
  private final ObjectProvider<JdbcClient> jdbc;

  JdbcHistoricalTopicProblemRepository(ObjectProvider<JdbcClient> jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public Map<UUID, Set<UUID>> read(Set<UUID> topicIds) {
    Map<UUID, Set<UUID>> result = new LinkedHashMap<>();
    topicIds.forEach(topic -> result.put(topic, new LinkedHashSet<>()));
    if (!topicIds.isEmpty()) {
      jdbc.getObject()
          .sql("SELECT topic_id,problem_id FROM problem.problem_topics WHERE topic_id IN (:ids)")
          .param("ids", topicIds)
          .query(
              (rs, n) ->
                  Map.entry(
                      rs.getObject("topic_id", UUID.class), rs.getObject("problem_id", UUID.class)))
          .list()
          .forEach(entry -> result.get(entry.getKey()).add(entry.getValue()));
    }
    result.replaceAll((topic, problems) -> Set.copyOf(problems));
    return Map.copyOf(result);
  }
}
