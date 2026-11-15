package com.algoprep.problembank.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.algoprep.problembank.domain.model.PublishedProblemModels.Filter;
import com.algoprep.problembank.domain.model.PublishedProblemModels.Paging;
import com.algoprep.problembank.domain.model.PublishedProblemModels.Row;
import com.algoprep.problembank.domain.model.PublishedProblemModels.Sample;
import com.algoprep.problembank.domain.model.PublishedProblemModels.SolveStates;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Learner reads on real PostgreSQL: published-only scope, filters, privacy and catalogue maths. */
class PublishedReadPgTest extends PostgresTestBase {
  private static final Paging BY_CODE = new Paging("code", true, 1, 20);
  private static final Filter NONE = new Filter(null, null, null, null, null);

  private JdbcPublishedProblemReadRepository repo;
  private JdbcStudentCatalogReadRepository catalog;
  private UUID p1;
  private UUID p2;
  private UUID draft;
  private UUID deleted;

  @BeforeEach
  void seed() {
    repo = new JdbcPublishedProblemReadRepository(provider());
    catalog = new JdbcStudentCatalogReadRepository(provider());
    p1 = problem(1, "TWO-SUM", "Two Sum", "PUBLISHED", EASY);
    p2 = problem(2, "ZIGZAG", "Zigzag 100% fun", "PUBLISHED", HARD);
    draft = problem(3, "DRAFT", "Draft", "UNPUBLISHED", EASY);
    deleted = problem(4, "GONE", "Gone", "PUBLISHED", EASY);
    softDelete(deleted);
    link(p1, MANG);
    link(p2, CHUOI);
    link(draft, MANG);
    link(deleted, MANG);
    stats(p1, 4, 3);
  }

  private List<UUID> ids(Filter filter, SolveStates states, Paging paging) {
    return repo.list(filter, states, paging).items().stream().map(Row::id).toList();
  }

  @Test
  void listReturnsOnlyPublishedAndNotDeletedProblems() {
    var page = repo.list(NONE, null, BY_CODE);

    assertThat(page.items()).extracting(Row::code).containsExactly("TWO-SUM", "ZIGZAG");
    assertThat(page.totalItems()).isEqualTo(2);
    assertThat(page.items().get(0).topicNames()).containsExactly("Mảng");
    assertThat(page.items().get(0).acRate()).isEqualByComparingTo("75.00");
    assertThat(page.items().get(1).acRate()).isNull();
  }

  @Test
  void sortKeysAndPageClampingWork() {
    assertThat(ids(NONE, null, new Paging("code", false, 1, 20))).containsExactly(p2, p1);
    assertThat(ids(NONE, null, new Paging("level", true, 1, 20))).containsExactly(p1, p2);
    assertThat(ids(NONE, null, new Paging("acRate", false, 1, 20))).containsExactly(p1, p2);
    assertThat(ids(NONE, null, new Paging("acRate", true, 1, 20))).containsExactly(p1, p2);
    var clamped = repo.list(NONE, null, new Paging("code", true, 50, 1));
    assertThat(clamped.page()).isEqualTo(2);
    assertThat(clamped.items()).extracting(Row::id).containsExactly(p2);
  }

  @Test
  void filtersApplyAndUnknownDifficultyCodeIsIgnored() {
    assertThat(ids(new Filter("100%", null, null, null, null), null, BY_CODE)).containsExactly(p2);
    assertThat(ids(new Filter("1_0", null, null, null, null), null, BY_CODE)).isEmpty();
    assertThat(ids(new Filter(null, null, "HARD", null, null), null, BY_CODE)).containsExactly(p2);
    assertThat(ids(new Filter(null, null, "NOPE", null, null), null, BY_CODE))
        .containsExactly(p1, p2);
    assertThat(ids(new Filter(null, null, null, CHUOI, null), null, BY_CODE)).containsExactly(p2);
    jdbc.sql("INSERT INTO problem.tags(id,name) VALUES(:id,'DP')").param("id", id(70)).update();
    jdbc.sql("INSERT INTO problem.problem_tags(problem_id,tag_id) VALUES(:p,:t)")
        .param("p", p1)
        .param("t", id(70))
        .update();
    assertThat(ids(new Filter(null, null, null, null, "dp"), null, BY_CODE)).containsExactly(p1);
  }

  @Test
  void solveStatusFiltersUseTheIdentityStates() {
    var states = new SolveStates(Set.of(p1), Set.of(p2));

    assertThat(ids(new Filter(null, "SOLVED", null, null, null), states, BY_CODE))
        .containsExactly(p1);
    assertThat(ids(new Filter(null, "ATTEMPTED", null, null, null), states, BY_CODE))
        .containsExactly(p2);
    assertThat(ids(new Filter(null, "NOT_SOLVED", null, null, null), states, BY_CODE))
        .containsExactly(p2);
    assertThat(ids(new Filter(null, "UNSOLVED", null, null, null), states, BY_CODE)).isEmpty();
    assertThat(
            ids(
                new Filter(null, "UNSOLVED", null, null, null),
                new SolveStates(Set.of(), Set.of()),
                BY_CODE))
        .containsExactly(p1, p2);
  }

  @Test
  void randomPicksOnlyAVisibleProblem() {
    for (int i = 0; i < 10; i++) {
      assertThat(repo.random(NONE, null)).get().isIn(p1, p2);
    }
    assertThat(repo.random(new Filter(null, null, null, CHUOI, null), null)).contains(p2);
    assertThat(repo.random(new Filter("DRAFT", null, null, null, null), null)).isEmpty();
  }

  @Test
  void workspaceAndOverviewHideUnpublishedAndDeletedAndShowBookmarkFlag() {
    UUID user = id(800);
    jdbc.sql("INSERT INTO problem.bookmarks(user_id,problem_id) VALUES(:u,:p)")
        .param("u", user)
        .param("p", p1)
        .update();
    jdbc.sql(
            "INSERT INTO problem.worked_examples(id,problem_id,position,input,output)"
                + " VALUES(:id,:p,1,'1 2','3')")
        .param("id", id(60))
        .param("p", p1)
        .update();

    assertThat(repo.workspace(draft, user)).isEmpty();
    assertThat(repo.workspace(deleted, user)).isEmpty();
    assertThat(repo.overview(draft, user)).isEmpty();
    var workspace = repo.workspace(p1, user).orElseThrow();
    assertThat(workspace.bookmarked()).isTrue();
    assertThat(workspace.statementMd()).isEmpty();
    assertThat(workspace.workedExamples()).hasSize(1);
    assertThat(repo.workspace(p1, id(801)).orElseThrow().bookmarked()).isFalse();
    assertThat(repo.overview(p1, user).orElseThrow().examples().get(0).output()).isEqualTo("3");
  }

  @Test
  void hiddenTestcaseContentNeverReachesLearnerReads() {
    testcase(p1, 100, "SAMPLE", true, at(1, "visible"));
    testcase(p1, 101, "SAMPLE", false, at(2, "draft-sample"));
    testcase(p1, 102, "HIDDEN", true, at(3, "SECRET-HIDDEN"));
    testcase(p1, 103, "HIDDEN", false, at(4, "SECRET-UNAPPROVED"));
    jdbc.sql(
            "INSERT INTO problem.problem_specs(id,problem_id,function_name,return_type,parameters)"
                + " VALUES(:id,:p,'twoSum',CAST('\"int\"' AS jsonb),"
                + "CAST('[{\"name\":\"nums\"},{\"name\":\"target\"}]' AS jsonb))")
        .param("id", id(61))
        .param("p", p1)
        .update();

    var overview = repo.overview(p1, id(800)).orElseThrow();
    var samples = repo.samples(p1).orElseThrow();

    assertThat(overview.hiddenTestcaseCount()).isEqualTo(1);
    assertThat(samples.parameterNames()).containsExactly("nums", "target");
    assertThat(samples.items()).extracting(Sample::testcaseId).containsExactly(id(100));
    assertThat(samples.items().toString()).doesNotContain("SECRET").doesNotContain("draft-sample");
    assertThat(overview.toString()).doesNotContain("SECRET");
    assertThat(repo.samples(draft)).isEmpty();
    assertThat(repo.samples(deleted)).isEmpty();
  }

  @Test
  void studentCatalogueCountsOnlyVisibleProblemsAndKeepsEmptyTopics() {
    var progress = catalog.topicProgress(new SolveStates(Set.of(p1, draft), Set.of()));

    assertThat(progress).hasSize(6);
    var mang = progress.stream().filter(t -> t.topicId().equals(MANG)).findFirst().orElseThrow();
    assertThat(mang.total()).isEqualTo(1);
    assertThat(mang.solved()).isEqualTo(1);
    assertThat(progress.stream().filter(t -> t.total() == 0)).hasSize(4);
    assertThat(catalog.topicProgress(null).get(0).solved()).isNull();
  }

  @Test
  void levelProgressAndSummaryExcludeDraftsAndDeleted() {
    var levels = catalog.levelProgress(new SolveStates(Set.of(p2), Set.of()));
    var summary = catalog.summary();

    assertThat(levels)
        .extracting(l -> l.code() + ":" + l.total() + ":" + l.solved())
        .containsExactly("EASY:1:0", "MEDIUM:0:0", "HARD:1:1");
    assertThat(summary.totalPublished()).isEqualTo(2);
    assertThat(summary.byTopic())
        .extracting(t -> t.name() + ":" + t.count())
        .containsExactly("Mảng:1", "Chuỗi:1");
    assertThat(summary.byLevel()).extracting(l -> l.count()).containsExactly(1, 0, 1);
  }
}
