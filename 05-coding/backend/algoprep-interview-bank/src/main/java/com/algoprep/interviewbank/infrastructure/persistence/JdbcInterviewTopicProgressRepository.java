package com.algoprep.interviewbank.infrastructure.persistence;

import com.algoprep.interviewbank.domain.model.TopicProgress;
import com.algoprep.interviewbank.domain.ports.out.TopicProgressRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class JdbcInterviewTopicProgressRepository implements TopicProgressRepository {
  private final ObjectProvider<JdbcClient> jdbc;

  JdbcInterviewTopicProgressRepository(ObjectProvider<JdbcClient> jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public List<TopicProgress> listFor(UUID userId) {
    return jdbc.getObject()
        .sql(
            "SELECT t.id, t.code, t.display_name, t.sort_order,"
                + " COALESCE(ph.questions_attempted, 0) AS attempted,"
                + " (SELECT count(*) FROM interview_bank.interview_questions q"
                + " WHERE q.topic_id = t.id AND q.status = 'ACTIVE') AS total,"
                + " ph.last_practiced_at"
                + " FROM interview_bank.question_topics t"
                + " LEFT JOIN interview_bank.practice_history ph"
                + " ON ph.topic_id = t.id AND ph.user_id = :user"
                + " ORDER BY t.sort_order, t.code")
        .param("user", userId)
        .query(
            (rs, row) ->
                new TopicProgress(
                    rs.getObject("id", UUID.class),
                    rs.getString("code"),
                    rs.getString("display_name"),
                    rs.getInt("sort_order"),
                    rs.getInt("attempted"),
                    rs.getInt("total"),
                    TopicProgress.percent(rs.getInt("attempted"), rs.getInt("total")),
                    rs.getObject("last_practiced_at", OffsetDateTime.class) == null
                        ? null
                        : rs.getObject("last_practiced_at", OffsetDateTime.class).toInstant()))
        .list();
  }
}
