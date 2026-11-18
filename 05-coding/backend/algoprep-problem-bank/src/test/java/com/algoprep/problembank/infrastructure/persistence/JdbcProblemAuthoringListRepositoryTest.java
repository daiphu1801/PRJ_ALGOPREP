package com.algoprep.problembank.infrastructure.persistence;

import static com.algoprep.problembank.infrastructure.persistence.FakeJdbc.row;
import static org.assertj.core.api.Assertions.assertThat;

import com.algoprep.problembank.domain.model.problem.AuthoringListFilter;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Management list (DD F2 sections 25 to 27): scope, paging, sort allowlist, statistics. */
class JdbcProblemAuthoringListRepositoryTest {
  private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
  private static final UUID AUTHOR = UUID.fromString("00000000-0000-0000-0000-0000000000a1");

  private final FakeJdbc db = new FakeJdbc();
  private final JdbcProblemAuthoringListRepository repo =
      new JdbcProblemAuthoringListRepository(db.provider());

  private static AuthoringListFilter filter(String sort, boolean asc, int page, int size) {
    return new AuthoringListFilter(null, null, null, sort, asc, page, size, null);
  }

  private static Object item() {
    return row(
        "id",
        ID,
        "code",
        "P1",
        "title",
        "T",
        "topic_names",
        new Object[] {"Array"},
        "level_id",
        ID,
        "level_code",
        "EASY",
        "level_name",
        "Easy",
        "status",
        "PUBLISHED",
        "submission_count",
        4,
        "assigned_class_count",
        2,
        "ac_rate",
        new BigDecimal("50.0"),
        "testcase_count",
        9,
        "updated_at",
        Instant.parse("2026-01-02T03:04:05Z"));
  }

  private void script(long matching) {
    db.onQuery("AS total", row("total", 30L, "published", 12L))
        .onQuery("SELECT count(*) FROM problem.problems p WHERE", matching)
        .onQuery("AS testcase_count", item());
  }

  @Test
  void listMapsRowsAndReportsScopeTotalsIndependentOfFilters() {
    script(21L);

    var page = repo.list(filter("code", true, 2, 10));

    assertThat(page.total()).isEqualTo(30);
    assertThat(page.published()).isEqualTo(12);
    assertThat(page.totalItems()).isEqualTo(21);
    assertThat(page.totalPages()).isEqualTo(3);
    assertThat(page.page()).isEqualTo(2);
    var first = page.items().get(0);
    assertThat(first.topicNames()).containsExactly("Array");
    assertThat(first.acRate()).isEqualByComparingTo("50");
    assertThat(first.updatedAt()).isEqualTo(Instant.parse("2026-01-02T03:04:05Z"));
    assertThat(first.testcaseCount()).isEqualTo(9);
    assertThat(db.last("AS testcase_count").param("offset")).isEqualTo(10L);
  }

  @Test
  void pageIsClampedToLastPageAndEmptyScopeKeepsPageOne() {
    script(0L);
    var empty = repo.list(filter("code", true, 7, 10));
    assertThat(empty.page()).isEqualTo(1);
    assertThat(empty.totalPages()).isZero();
    assertThat(db.last("AS testcase_count").param("offset")).isEqualTo(0L);

    var db2 = new FakeJdbc();
    db2.onQuery("AS total", row("total", 5L, "published", 0L))
        .onQuery("SELECT count(*) FROM problem.problems p WHERE", 25L);
    var clamped =
        new JdbcProblemAuthoringListRepository(db2.provider()).list(filter("code", true, 50, 10));
    assertThat(clamped.page()).isEqualTo(3);
    assertThat(db2.last("AS testcase_count").param("offset")).isEqualTo(20L);
  }

  @Test
  void everyAllowedSortKeyMapsToItsColumnAndUnknownKeysDefaultToUpdatedAt() {
    script(1L);
    String[][] expected = {
      {"code", "p.code ASC"},
      {"title", "p.title ASC"},
      {"level", "l.sort_order ASC"},
      {"status", "p.status ASC"},
      {"submissionCount", "COALESCE(s.submission_count, 0) ASC"},
      {"acRate", "s.ac_rate ASC"},
      {"updatedAt", "p.updated_at ASC"},
    };
    for (String[] e : expected) {
      repo.list(filter(e[0], true, 1, 10));
      assertThat(db.last("AS testcase_count").sql())
          .contains("ORDER BY " + e[1] + " NULLS LAST, p.id");
    }
  }

  @Test
  void unknownSortKeyIsNeverInterpolatedIntoSql() {
    script(1L);

    repo.list(filter("1; DROP TABLE problem.problems --", false, 1, 10));

    var sql = db.last("AS testcase_count").sql();
    assertThat(sql).contains("ORDER BY p.updated_at DESC NULLS LAST, p.id");
    assertThat(sql).doesNotContain("DROP TABLE");
  }

  @Test
  void searchTextIsEscapedAndFiltersAreBound() {
    script(1L);
    UUID level = UUID.randomUUID();

    repo.list(new AuthoringListFilter("a_b%c\\", level, "DRAFT", "code", true, 1, 5, AUTHOR));

    var call = db.last("SELECT count(*) FROM problem.problems p WHERE");
    assertThat(call.param("q")).isEqualTo("%a\\_b\\%c\\\\%");
    assertThat(call.param("level")).isEqualTo(level);
    assertThat(call.param("status")).isEqualTo("DRAFT");
    assertThat(call.param("author")).isEqualTo(AUTHOR);
    assertThat(db.last("AS total").param("author")).isEqualTo(AUTHOR);
  }

  @Test
  void exportWithoutIdsHasNoIdPredicateAndWithIdsBindsArray() {
    db.onQuery("AS testcase_count", item());

    var all = repo.export(null, null, null, null, null);
    assertThat(all).hasSize(1);
    assertThat(db.last("AS testcase_count").sql())
        .doesNotContain("ANY(:ids)")
        .contains("ORDER BY p.updated_at DESC, p.id");

    repo.export(AUTHOR, List.of(ID), "x", null, "PUBLISHED");
    var call = db.last("AS testcase_count");
    assertThat(call.sql()).contains("p.id = ANY(:ids)");
    assertThat((UUID[]) call.param("ids")).containsExactly(ID);
    assertThat(call.param("q")).isEqualTo("%x%");
  }

  @Test
  void topicDistributionComputesPercentOfProblemsWithTopic() {
    UUID t1 = UUID.randomUUID();
    UUID t2 = UUID.randomUUID();
    db.onQuery("count(DISTINCT pt.problem_id)", 3)
        .onQuery(
            "GROUP BY t.id",
            row("id", t1, "name", "Array", "n", 2),
            row("id", t2, "name", "DP", "n", 1));

    var dist = repo.topicDistribution(AUTHOR);

    assertThat(dist.totalProblemsWithTopic()).isEqualTo(3);
    assertThat(dist.totalTopicsUsed()).isEqualTo(2);
    assertThat(dist.items().get(0).percent()).isEqualByComparingTo("66.7");
    assertThat(dist.items().get(1).percent()).isEqualByComparingTo("33.3");
  }

  @Test
  void topicDistributionOfEmptyScopeHasZeroPercentNotDivisionByZero() {
    db.onQuery("count(DISTINCT pt.problem_id)", 0);

    var dist = repo.topicDistribution(null);

    assertThat(dist.items()).isEmpty();
    assertThat(dist.totalTopicsUsed()).isZero();
  }

  @Test
  void attentionReturnsFourRulesInFixedOrderAndBindsThresholds() {
    db.onQuery("AS no_testcase", row("no_testcase", 1, "low_ac", 2, "hidden", 3, "stale", 4));

    var rules = repo.attention(AUTHOR, 30, 14);

    assertThat(rules)
        .extracting(r -> r.ruleCode() + "=" + r.count())
        .containsExactly("NO_TESTCASE=1", "LOW_AC=2", "HIDDEN_FROM_LEARNERS=3", "STALE_DRAFT=4");
    var call = db.last("AS no_testcase");
    assertThat(call.param("low")).isEqualTo(30);
    assertThat(call.param("days")).isEqualTo(14);
  }
}
