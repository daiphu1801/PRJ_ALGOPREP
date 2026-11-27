package com.algoprep.aireview.infrastructure.persistence;

import com.algoprep.aireview.application.dto.interview.MySessionFilter;
import com.algoprep.aireview.application.dto.interview.MySessionPage;
import com.algoprep.aireview.domain.model.interview.InterviewEntryType;
import com.algoprep.aireview.domain.model.interview.InterviewSessionStage;
import com.algoprep.aireview.domain.model.interview.RubricCriterion;
import com.algoprep.aireview.domain.ports.out.InterviewHistoryRepository;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * Endpoint 31A. The overall score is computed in SQL from the four stored rubric rows with the
 * weights of {@link RubricCriterion} (the same formula as {@code InterviewResult.overall}), so the
 * score band filter and the score sort can run in the database. A session with fewer than four
 * rubric rows has no score.
 */
@Repository
class JdbcInterviewHistoryRepository implements InterviewHistoryRepository {
  private static final JsonMapper JSON = JsonMapper.builder().build();

  private static final String WEIGHTS =
      Arrays.stream(RubricCriterion.values())
          .map(c -> "('" + c.name() + "', " + c.weightPercent() + ")")
          .collect(Collectors.joining(", "));

  private static final String SCORED =
      "WITH w(code, weight) AS (VALUES "
          + WEIGHTS
          + "), scored AS (SELECT r.owner_id AS id,"
          + " ROUND(SUM(r.score * w.weight) / 100, 2) AS overall, COUNT(*) AS n"
          + " FROM ai.rubric_scores r JOIN w ON w.code = r.criterion_code"
          + " WHERE r.owner_type = 'MOCK_INTERVIEW' GROUP BY r.owner_id)";

  private static final String SOURCE =
      " FROM ai.interview_sessions s LEFT JOIN scored c ON c.id = s.id AND c.n = 4"
          + " WHERE s.user_id = :user AND s.stage IN ('COMPLETED', 'ABANDONED')";

  private static final String LIKE =
      " AND (s.question_text ILIKE :like ESCAPE '\\' OR s.topics::text ILIKE :like ESCAPE '\\')";

  private final ObjectProvider<JdbcClient> jdbc;

  JdbcInterviewHistoryRepository(ObjectProvider<JdbcClient> jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public MySessionPage search(MySessionFilter f) {
    String where = where(f);
    long total = bind(f, SCORED + " SELECT COUNT(*)" + SOURCE + where).query(Long.class).single();
    String select =
        SCORED
            + " SELECT s.id, s.entry_type, s.stage, s.question_text, s.topics::text AS topics,"
            + " s.interviewer_level, s.turn_count, s.max_turns, s.started_at, s.ended_at,"
            + " c.overall"
            + SOURCE
            + where
            + order(f)
            + " LIMIT :size OFFSET :offset";
    List<MySessionPage.Row> rows =
        bind(f, select)
            .param("size", f.size())
            .param("offset", (long) (f.page() - 1) * f.size())
            .query((rs, n) -> row(rs))
            .list();
    return new MySessionPage(rows, total);
  }

  private JdbcClient.StatementSpec bind(MySessionFilter f, String sql) {
    JdbcClient.StatementSpec spec = jdbc.getObject().sql(sql).param("user", f.userId());
    if (f.search() != null) {
      spec = spec.param("like", "%" + escapeLike(f.search()) + "%");
    }
    if (f.entryType() != null) {
      spec = spec.param("entry", f.entryType().name());
    }
    if (f.stage() != null) {
      spec = spec.param("stage", f.stage().name());
    }
    if (f.level() != null) {
      spec = spec.param("level", f.level());
    }
    return spec;
  }

  private static String where(MySessionFilter f) {
    StringBuilder sql = new StringBuilder();
    if (f.search() != null) {
      sql.append(LIKE);
    }
    if (f.entryType() != null) {
      sql.append(" AND s.entry_type = :entry");
    }
    if (f.stage() != null) {
      sql.append(" AND s.stage = :stage");
    }
    if (f.level() != null) {
      sql.append(" AND s.interviewer_level = :level");
    }
    if (f.band() != null) {
      sql.append(band(f.band()));
    }
    return sql.toString();
  }

  private static String band(MySessionFilter.ScoreBand band) {
    return switch (band) {
      case WEAK -> " AND c.overall < 5";
      case FAIR -> " AND c.overall >= 5 AND c.overall < 8";
      case GOOD -> " AND c.overall >= 8";
    };
  }

  /** The score sort puts unscored sessions last in both directions; the id keeps pages stable. */
  private static String order(MySessionFilter f) {
    String dir = f.descending() ? "DESC" : "ASC";
    return f.sortBy() == MySessionFilter.SortField.SCORE
        ? " ORDER BY c.overall " + dir + " NULLS LAST, s.started_at DESC, s.id"
        : " ORDER BY s.started_at " + dir + ", s.id";
  }

  private static String escapeLike(String raw) {
    return raw.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
  }

  private static MySessionPage.Row row(java.sql.ResultSet rs) throws java.sql.SQLException {
    String topics = rs.getString("topics");
    return new MySessionPage.Row(
        rs.getObject("id", UUID.class),
        InterviewEntryType.valueOf(rs.getString("entry_type")),
        InterviewSessionStage.valueOf(rs.getString("stage")),
        rs.getString("question_text"),
        topics == null ? List.of() : JSON.readValue(topics, new TypeReference<List<String>>() {}),
        rs.getString("interviewer_level"),
        rs.getInt("turn_count"),
        (Integer) rs.getObject("max_turns"),
        rs.getObject("started_at", OffsetDateTime.class).toInstant(),
        rs.getObject("ended_at", OffsetDateTime.class).toInstant(),
        rs.getBigDecimal("overall"));
  }
}
