package com.algoprep.problembank.infrastructure.persistence;

import static com.algoprep.problembank.infrastructure.persistence.FakeJdbc.row;
import static org.assertj.core.api.Assertions.assertThat;

import com.algoprep.problembank.domain.model.student.PublishedProblemModels.SolveStates;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Student catalogue read: counts with zero rows kept, solved only for authenticated users. */
class JdbcStudentCatalogReadRepositoryTest {
  private static final UUID T = UUID.fromString("00000000-0000-0000-0000-0000000000d1");

  private final FakeJdbc db = new FakeJdbc();
  private final JdbcStudentCatalogReadRepository repo =
      new JdbcStudentCatalogReadRepository(db.provider());

  @Test
  void anonymousTopicProgressHasNullSolvedAndEmptySolvedLiteral() {
    db.onQuery(
        "FROM problem.topics t LEFT JOIN",
        row("id", T, "name", "Array", "sort_order", 1, "total", 0, "solved", 0));

    var topics = repo.topicProgress(null);

    assertThat(topics)
        .singleElement()
        .satisfies(
            t -> {
              assertThat(t.solved()).isNull();
              assertThat(t.total()).isZero();
            });
    assertThat(db.last("FROM problem.topics t").param("solved")).isEqualTo("{}");
  }

  @Test
  void authenticatedTopicProgressCarriesSolvedCountAndLiteral() {
    UUID solved = UUID.fromString("00000000-0000-0000-0000-0000000000e1");
    db.onQuery(
        "FROM problem.topics t LEFT JOIN",
        row("id", T, "name", "Array", "sort_order", 1, "total", 5, "solved", 2));

    var topics = repo.topicProgress(new SolveStates(Set.of(solved), Set.of()));

    assertThat(topics.get(0).solved()).isEqualTo(2);
    assertThat(db.last("FROM problem.topics t").param("solved")).isEqualTo("{" + solved + "}");
  }

  @Test
  void levelProgressMapsCodeNameOrderTotalAndSolved() {
    db.onQuery(
        "FROM problem.problem_levels l LEFT JOIN problem.problems p ON p.level_id = l.id AND p.status = 'PUBLISHED' AND NOT p.deleted GROUP BY l.id, l.code, l.display_name, l.sort_order ORDER BY l.sort_order, l.code",
        row("code", "EASY", "display_name", "Easy", "sort_order", 1, "total", 7, "solved", 3));

    var levels = repo.levelProgress(new SolveStates(Set.of(), Set.of()));

    assertThat(levels)
        .singleElement()
        .satisfies(
            l -> {
              assertThat(l.code()).isEqualTo("EASY");
              assertThat(l.solved()).isEqualTo(3);
              assertThat(l.total()).isEqualTo(7);
            });
  }

  @Test
  void summaryCombinesPublishedTotalWithTopicAndLevelBreakdown() {
    db.onQuery("SELECT count(*) FROM problem.problems", 12)
        .onQuery("JOIN problem.problem_topics pt", row("id", T, "name", "DP", "count", 4))
        .onQuery(
            "FROM problem.problem_levels l LEFT JOIN",
            row("code", "HARD", "display_name", "Hard", "sort_order", 3, "count", 0));

    var summary = repo.summary();

    assertThat(summary.totalPublished()).isEqualTo(12);
    assertThat(summary.byTopic()).singleElement().extracting("count").isEqualTo(4);
    assertThat(summary.byLevel()).singleElement().extracting("count").isEqualTo(0);
  }
}
