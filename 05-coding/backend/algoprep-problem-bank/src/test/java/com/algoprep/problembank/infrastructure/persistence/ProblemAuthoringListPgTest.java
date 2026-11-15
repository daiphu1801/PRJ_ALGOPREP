package com.algoprep.problembank.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.algoprep.problembank.domain.model.AttentionCount;
import com.algoprep.problembank.domain.model.AuthoringListFilter;
import com.algoprep.problembank.domain.model.AuthoringListPage;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Management list on real PostgreSQL: ORDER BY allowlist, paging, scope, search and statistics. */
class ProblemAuthoringListPgTest extends PostgresTestBase {
  private JdbcProblemAuthoringListRepository repo;
  private UUID a;
  private UUID b;
  private UUID c;

  @BeforeEach
  void seed() {
    repo = new JdbcProblemAuthoringListRepository(provider());
    a = problem(1, "A-ONE", "Zeta", "PUBLISHED", HARD);
    b = problem(2, "B-TWO", "Alpha", "UNPUBLISHED", EASY);
    c = problem(3, "C-THREE", "Mid", "PUBLISHED", MEDIUM);
    stats(a, 5, 1);
    stats(c, 9, 7);
    touched(a, 3);
    touched(b, 1);
    touched(c, 2);
  }

  private void touched(UUID problem, int daysAgo) {
    jdbc.sql("UPDATE problem.problems SET updated_at=:t WHERE id=:id")
        .param("t", utc(Instant.now().minus(daysAgo, ChronoUnit.DAYS)))
        .param("id", problem)
        .update();
  }

  private List<UUID> order(String sort, boolean ascending) {
    var filter = new AuthoringListFilter(null, null, null, sort, ascending, 1, 50, null);
    return repo.list(filter).items().stream().map(AuthoringListPage.Item::id).toList();
  }

  /** Every allowlisted key, plus the default and an unknown key, yields a valid ORDER BY. */
  @ParameterizedTest(name = "{0} ascending={1}")
  @CsvSource({
    "code,true,1 2 3",
    "code,false,3 2 1",
    "title,true,2 3 1",
    "title,false,1 3 2",
    "level,true,2 3 1",
    "level,false,1 3 2",
    "status,true,1 3 2",
    "status,false,2 1 3",
    "submissionCount,true,2 1 3",
    "submissionCount,false,3 1 2",
    "acRate,true,1 3 2",
    "acRate,false,3 1 2",
    "updatedAt,true,1 3 2",
    "updatedAt,false,2 3 1",
    "unknown-key,false,2 3 1"
  })
  void everySortKeyProducesTheExpectedOrder(String sort, boolean ascending, String expected) {
    var wanted =
        java.util.Arrays.stream(expected.split(" ")).map(n -> id(Long.parseLong(n))).toList();

    assertThat(order(sort, ascending)).containsExactlyElementsOf(wanted);
  }

  @Test
  void pagingClampsAnOutOfRangePageAndReportsTotals() {
    for (int i = 10; i < 32; i++) {
      problem(i, "P-" + i, "Bulk " + i, "UNPUBLISHED", EASY);
    }

    var last = repo.list(new AuthoringListFilter(null, null, null, "code", true, 99, 10, null));

    assertThat(last.page()).isEqualTo(3);
    assertThat(last.totalPages()).isEqualTo(3);
    assertThat(last.items()).hasSize(5);
    assertThat(last.totalItems()).isEqualTo(25);
    assertThat(last.published()).isEqualTo(2);
  }

  @Test
  void filtersNarrowTheMatchWhileTotalsKeepTheWholeScope() {
    var page =
        repo.list(new AuthoringListFilter(null, null, "PUBLISHED", "code", true, 1, 10, null));

    assertThat(page.items())
        .extracting(AuthoringListPage.Item::code)
        .containsExactly("A-ONE", "C-THREE");
    assertThat(page.totalItems()).isEqualTo(2);
    assertThat(page.published()).isEqualTo(2);

    var byLevel = repo.list(new AuthoringListFilter(null, EASY, null, "code", true, 1, 10, null));
    assertThat(byLevel.items()).extracting(AuthoringListPage.Item::id).containsExactly(b);
    assertThat(byLevel.totalItems()).isEqualTo(1);
  }

  @Test
  void searchTreatsPercentAndUnderscoreAsLiteralCharacters() {
    problem(20, "PCT-1", "Fifty% done", "UNPUBLISHED", EASY);
    problem(21, "US-1", "snake_case", "UNPUBLISHED", EASY);

    assertThat(searchIds("50%")).isEmpty();
    assertThat(searchIds("y%")).containsExactly(id(20));
    assertThat(searchIds("e_c")).containsExactly(id(21));
    assertThat(searchIds("snakeXcase")).isEmpty();
    assertThat(searchIds("ALPHA")).containsExactly(b);
  }

  private List<UUID> searchIds(String q) {
    var filter = new AuthoringListFilter(q, null, null, "code", true, 1, 10, null);
    return repo.list(filter).items().stream().map(AuthoringListPage.Item::id).toList();
  }

  @Test
  void instructorScopeHidesOthersAndDeletedProblems() {
    UUID other = id(901);
    jdbc.sql("UPDATE problem.problems SET author_id=:o WHERE id=:id")
        .param("o", other)
        .param("id", b)
        .update();
    softDelete(c);

    var mine = repo.list(new AuthoringListFilter(null, null, null, "code", true, 1, 10, AUTHOR));
    var admin = repo.list(new AuthoringListFilter(null, null, null, "code", true, 1, 10, null));

    assertThat(mine.items()).extracting(AuthoringListPage.Item::id).containsExactly(a);
    assertThat(mine.totalItems()).isEqualTo(1);
    assertThat(admin.items()).extracting(AuthoringListPage.Item::id).containsExactly(a, b);
  }

  @Test
  void rowCarriesTopicNamesCountsAndStatisticsFromOneQuery() {
    link(a, CHUOI);
    link(a, MANG);
    testcase(a, 100, "SAMPLE", true, at(1, "x"));
    testcase(a, 101, "HIDDEN", false, at(2, "y"));
    assignment(a, id(500), false);
    assignment(a, id(501), true);

    var item =
        repo.list(new AuthoringListFilter("A-ONE", null, null, "code", true, 1, 10, null))
            .items()
            .get(0);

    assertThat(item.topicNames()).containsExactly("Mảng", "Chuỗi");
    assertThat(item.testcaseCount()).isEqualTo(2);
    assertThat(item.assignedClassCount()).isEqualTo(1);
    assertThat(item.submissionCount()).isEqualTo(5);
    assertThat(item.acRate()).isEqualByComparingTo("20.00");
    assertThat(item.levelCode()).isEqualTo("HARD");
  }

  @Test
  void exportHonoursIdListAndFiltersInUpdatedOrder() {
    var all = repo.export(null, null, null, null, null);
    var some = repo.export(null, List.of(a, c), null, null, "PUBLISHED");
    var none = repo.export(null, List.of(b), null, null, "PUBLISHED");

    assertThat(all).extracting(AuthoringListPage.Item::id).containsExactly(b, c, a);
    assertThat(some).extracting(AuthoringListPage.Item::id).containsExactly(c, a);
    assertThat(none).isEmpty();
  }

  @Test
  void topicDistributionComputesPercentOfProblemsThatHaveATopic() {
    link(a, MANG);
    link(b, MANG);
    link(c, MANG);
    link(c, CHUOI);
    problem(30, "NOTOPIC", "No topic", "UNPUBLISHED", EASY);

    var distribution = repo.topicDistribution(null);

    assertThat(distribution.totalProblemsWithTopic()).isEqualTo(3);
    assertThat(distribution.items()).hasSize(2);
    assertThat(distribution.items().get(0).topicName()).isEqualTo("Mảng");
    assertThat(distribution.items().get(0).count()).isEqualTo(3);
    assertThat(distribution.items().get(0).percent()).isEqualByComparingTo(new BigDecimal("100.0"));
    assertThat(distribution.items().get(1).count()).isEqualTo(1);
    assertThat(distribution.items().get(1).percent()).isEqualByComparingTo(new BigDecimal("33.3"));
  }

  @Test
  void topicDistributionOfEmptyScopeHasZeroPercentAndNoEntries() {
    jdbc.sql("DELETE FROM problem.problem_topics").update();

    var distribution = repo.topicDistribution(id(777));

    assertThat(distribution.totalProblemsWithTopic()).isZero();
    assertThat(distribution.items()).isEmpty();
  }

  @Test
  void attentionCountsFourBucketsWithRealTimeArithmetic() {
    testcase(a, 100, "SAMPLE", true, at(1, "x"));
    testcase(c, 101, "SAMPLE", true, at(1, "x"));
    // b: never published draft, stale (40 days) and no testcase.
    touched(b, 40);
    // d: once published, now hidden from learners.
    UUID d = problem(4, "D-FOUR", "Hidden again", "PUBLISHED", EASY);
    jdbc.sql("UPDATE problem.problems SET status='UNPUBLISHED' WHERE id=:id")
        .param("id", d)
        .update();

    var counts = repo.attention(null, 30, 30);

    assertThat(counts)
        .extracting(AttentionCount::ruleCode, AttentionCount::count)
        .containsExactly(
            org.assertj.core.api.Assertions.tuple("NO_TESTCASE", 2),
            org.assertj.core.api.Assertions.tuple("LOW_AC", 1),
            org.assertj.core.api.Assertions.tuple("HIDDEN_FROM_LEARNERS", 1),
            org.assertj.core.api.Assertions.tuple("STALE_DRAFT", 1));
  }
}
