package com.algoprep.problembank.infrastructure.persistence;

import static com.algoprep.problembank.infrastructure.persistence.FakeJdbc.row;
import static org.assertj.core.api.Assertions.assertThat;

import com.algoprep.problembank.domain.model.PublishedProblemModels.Filter;
import com.algoprep.problembank.domain.model.PublishedProblemModels.Paging;
import com.algoprep.problembank.domain.model.PublishedProblemModels.SolveStates;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Published-problem reads: visibility scope, paging, sort allowlist, LIKE escaping, mapping. */
class JdbcPublishedProblemReadRepositoryTest {
  private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
  private static final Filter NO_FILTER = new Filter(null, null, null, null, null);

  private final FakeJdbc db = new FakeJdbc();
  private final JdbcPublishedProblemReadRepository repo =
      new JdbcPublishedProblemReadRepository(db.provider());

  private static Object listRow() {
    return row(
        "id",
        ID,
        "code",
        "TWO-SUM",
        "title",
        "Two Sum",
        "topic_names",
        new Object[] {"Array", "Hash Table"},
        "level_code",
        "EASY",
        "display_name",
        "Easy",
        "ac_rate",
        new BigDecimal("55.50"),
        "function_wrapper_supported",
        true);
  }

  @Test
  void listMapsRowsAndComputesPageCount() {
    db.onQuery("SELECT count(*)", 25L).onQuery("SELECT p.id, p.code", listRow());

    var page = repo.list(NO_FILTER, null, new Paging("code", true, 2, 10));

    assertThat(page.totalItems()).isEqualTo(25);
    assertThat(page.page()).isEqualTo(2);
    var first = page.items().get(0);
    assertThat(first.code()).isEqualTo("TWO-SUM");
    assertThat(first.topicNames()).containsExactly("Array", "Hash Table");
    assertThat(first.levelDisplayName()).isEqualTo("Easy");
    assertThat(first.acRate()).isEqualByComparingTo("55.5");
    assertThat(first.functionWrapperSupported()).isTrue();
    var call = db.last("SELECT p.id, p.code");
    assertThat(call.param("limit")).isEqualTo(10);
    assertThat(call.param("offset")).isEqualTo(10L);
    assertThat(call.param("solved")).isEqualTo("{}");
    assertThat(call.sql()).contains("p.status = 'PUBLISHED' AND NOT p.deleted");
  }

  @Test
  void listClampsRequestedPageToLastPageAndTreatsEmptyAsOnePage() {
    db.onQuery("SELECT count(*)", 0L);
    var empty = repo.list(NO_FILTER, null, new Paging("code", true, 5, 10));
    assertThat(empty.items()).isEmpty();
    assertThat(empty.page()).isEqualTo(1);
    assertThat(db.last("SELECT p.id, p.code").param("offset")).isEqualTo(0L);

    var db2 = new FakeJdbc().onQuery("SELECT count(*)", 21L);
    var clamped =
        new JdbcPublishedProblemReadRepository(db2.provider())
            .list(NO_FILTER, null, new Paging("code", true, 99, 10));
    assertThat(clamped.page()).isEqualTo(3);
    assertThat(db2.last("SELECT p.id, p.code").param("offset")).isEqualTo(20L);
  }

  @Test
  void sortKeysMapToAllowlistedOrderClausesAndUnknownKeysFallBackToCode() {
    db.onQuery("SELECT count(*)", 1L);
    repo.list(NO_FILTER, null, new Paging("level", true, 1, 10));
    assertThat(db.last("SELECT p.id, p.code").sql()).contains("ORDER BY pl.sort_order ASC");
    repo.list(NO_FILTER, null, new Paging("acRate", false, 1, 10));
    assertThat(db.last("SELECT p.id, p.code").sql()).contains("ORDER BY s.ac_rate DESC NULLS LAST");
    repo.list(NO_FILTER, null, new Paging("title; DROP TABLE problems", false, 1, 10));
    var sql = db.last("SELECT p.id, p.code").sql();
    assertThat(sql).contains("ORDER BY p.code DESC");
    assertThat(sql).doesNotContain("DROP TABLE");
  }

  @Test
  void listBindsEscapedLikePatternAndSolveStateLiterals() {
    db.onQuery("SELECT count(*)", 0L);
    UUID solved = UUID.fromString("00000000-0000-0000-0000-0000000000aa");
    UUID attempted = UUID.fromString("00000000-0000-0000-0000-0000000000bb");
    var filter = new Filter("50%_off\\", "SOLVED", "EASY", ID, "dp");

    repo.list(
        filter, new SolveStates(Set.of(solved), Set.of(attempted)), new Paging("x", true, 1, 5));

    var call = db.last("SELECT count(*)");
    assertThat(call.param("pattern")).isEqualTo("%50\\%\\_off\\\\%");
    assertThat(call.param("q")).isEqualTo("50%_off\\");
    assertThat(call.param("topic")).isEqualTo(ID.toString());
    assertThat(call.param("tag")).isEqualTo("dp");
    assertThat(call.param("status")).isEqualTo("SOLVED");
    assertThat(call.param("solved")).isEqualTo("{" + solved + "}");
    assertThat(call.param("attempted")).isEqualTo("{" + attempted + "}");
  }

  @Test
  void randomReturnsEmptyWhenNothingMatchesAndIdOtherwise() {
    assertThat(repo.random(NO_FILTER, null)).isEmpty();
    db.onQuery("ORDER BY random()", ID);
    assertThat(repo.random(NO_FILTER, new SolveStates(Set.of(), Set.of()))).contains(ID);
  }

  private void scriptWorkspaceParts() {
    db.onQuery(
        "FROM problem.worked_examples",
        row("position", 1, "input", "[1]", "output", "1", "explanation_md", "why"));
  }

  @Test
  void workspaceMapsDetailWithWorkedExamplesAndBookmarkFlag() {
    scriptWorkspaceParts();
    UUID user = UUID.randomUUID();
    db.onQuery(
        "LEFT JOIN problem.problem_specs sp",
        row(
            "id", ID,
            "code", "TWO-SUM",
            "title", "Two Sum",
            "level_code", "EASY",
            "display_name", "Easy",
            "statement_md", "stmt",
            "constraints_md", "n <= 10",
            "function_wrapper_supported", true,
            "stdin_format_md", "in",
            "stdout_format_md", "out",
            "ac_rate", new BigDecimal("10"),
            "bookmarked", true,
            "topic_names", new Object[] {"Array"}));

    var ws = repo.workspace(ID, user).orElseThrow();

    assertThat(ws.bookmarked()).isTrue();
    assertThat(ws.topicNames()).containsExactly("Array");
    assertThat(ws.stdinFormatMd()).isEqualTo("in");
    assertThat(ws.workedExamples()).hasSize(1);
    assertThat(ws.workedExamples().get(0).explanationMd()).isEqualTo("why");
    assertThat(db.last("LEFT JOIN problem.problem_specs sp").param("user")).isEqualTo(user);
  }

  @Test
  void workspaceAndOverviewAreEmptyForHiddenOrMissingProblem() {
    assertThat(repo.workspace(ID, UUID.randomUUID())).isEmpty();
    assertThat(repo.overview(ID, UUID.randomUUID())).isEmpty();
  }

  @Test
  void overviewCarriesHiddenCountButNeverHiddenContent() {
    scriptWorkspaceParts();
    db.onQuery(
        "AS hidden_count",
        row(
            "id",
            ID,
            "code",
            "TWO-SUM",
            "title",
            "Two Sum",
            "level_code",
            "EASY",
            "display_name",
            "Easy",
            "statement_md",
            "stmt",
            "constraints_md",
            null,
            "ac_rate",
            null,
            "hidden_count",
            7,
            "bookmarked",
            false,
            "topic_names",
            new Object[] {}));

    var ov = repo.overview(ID, UUID.randomUUID()).orElseThrow();

    assertThat(ov.hiddenTestcaseCount()).isEqualTo(7);
    assertThat(ov.acRate()).isNull();
    assertThat(ov.constraintsMd()).isNull();
    assertThat(ov.topicNames()).isEmpty();
    assertThat(ov.examples()).hasSize(1);
  }

  @Test
  void samplesAreEmptyForInvisibleProblemAndNeverQueryTestcases() {
    db.onQuery("SELECT EXISTS", false);

    assertThat(repo.samples(ID)).isEmpty();
    assertThat(db.count("FROM problem.testcases")).isZero();
  }

  @Test
  void samplesReturnApprovedSampleContentWithParameterNames() {
    db.onQuery("SELECT EXISTS", true)
        .onQuery(
            "FROM problem.testcases",
            row(
                "id",
                ID,
                "position",
                1,
                "input_storage",
                "INLINE",
                "input_inline",
                "1 2",
                "input_object_key",
                null,
                "expected_output_storage",
                "OBJECT",
                "expected_output_inline",
                null,
                "expected_output_object_key",
                "k/1"))
        .onQuery("CAST(parameters AS text)", "[{\"name\":\"nums\"},{\"name\":\"target\"}]");

    var samples = repo.samples(ID).orElseThrow();

    assertThat(samples.parameterNames()).containsExactly("nums", "target");
    assertThat(samples.items()).hasSize(1);
    assertThat(samples.items().get(0).input().inline()).isEqualTo("1 2");
    assertThat(samples.items().get(0).expectedOutput().objectKey()).isEqualTo("k/1");
    assertThat(db.last("FROM problem.testcases").sql()).contains("visibility = 'SAMPLE'");
  }

  @Test
  void samplesWithoutSpecHaveNoParameterNames() {
    db.onQuery("SELECT EXISTS", true);

    var samples = repo.samples(ID).orElseThrow();

    assertThat(samples.parameterNames()).isEqualTo(List.of());
    assertThat(samples.items()).isEmpty();
  }
}
