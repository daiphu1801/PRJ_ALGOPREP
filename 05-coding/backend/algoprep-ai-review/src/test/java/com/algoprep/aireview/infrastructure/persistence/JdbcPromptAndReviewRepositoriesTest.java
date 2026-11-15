package com.algoprep.aireview.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.algoprep.aireview.domain.model.FeatureCode;
import com.algoprep.aireview.domain.model.ResponseGuards;
import com.algoprep.aireview.domain.model.prompt.PromptContent;
import com.algoprep.aireview.domain.model.prompt.PromptStatus;
import com.algoprep.aireview.domain.model.prompt.PromptTemplate;
import com.algoprep.aireview.domain.model.review.ReviewCacheEntry;
import com.algoprep.aireview.domain.model.review.StoredSolutionReview;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Prompt-template and solution-review repositories: versioning, guards JSON, cache and mapping. */
class JdbcPromptAndReviewRepositoriesTest {
  private static final Instant T0 = Instant.parse("2026-11-01T10:00:00Z");
  private static final OffsetDateTime T0_UTC = OffsetDateTime.ofInstant(T0, ZoneOffset.UTC);
  private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
  private static final UUID USER = UUID.fromString("00000000-0000-0000-0000-0000000000b1");
  private static final UUID PROBLEM = UUID.fromString("00000000-0000-0000-0000-0000000000c1");

  private static Map<String, Object> templateRow(String status, Object guards, Object published) {
    return FakeJdbc.row0(
        "id",
        ID,
        "feature_code",
        "SOLUTION_REVIEW",
        "version",
        "v1.2",
        "status",
        status,
        "system_instruction",
        "You are a reviewer",
        "model_id",
        "model-a",
        "guards",
        guards,
        "response_schema_version",
        "s1",
        "published_at",
        published,
        "created_at",
        T0_UTC);
  }

  private static PromptContent content() {
    return new PromptContent("sys", "model-b", new ResponseGuards(true, true, false));
  }

  @Test
  void listLiveAndHistoryAndFindMapTemplates() {
    var jdbc = new FakeJdbc();
    jdbc.rows(List.of(templateRow("ACTIVE", "{\"citeLines\":true}", T0_UTC)));
    jdbc.rows(List.of(templateRow("DRAFT", null, null), templateRow("ARCHIVED", "{}", T0_UTC)));
    jdbc.rows(List.of(templateRow("ACTIVE", "{\"answerInVietnamese\":true}", T0_UTC)));
    jdbc.none();
    var repo = new JdbcPromptTemplateRepository(jdbc.provider());

    List<PromptTemplate> live = repo.listLive();
    assertThat(live)
        .singleElement()
        .satisfies(
            t -> {
              assertThat(t.status()).isEqualTo(PromptStatus.ACTIVE);
              assertThat(t.content().guards().citeLines()).isTrue();
              assertThat(t.publishedAt()).isEqualTo(T0);
              assertThat(t.createdAt()).isEqualTo(T0);
            });
    assertThat(jdbc.lastSql()).contains("status IN ('ACTIVE','DRAFT')");

    List<PromptTemplate> history = repo.history(FeatureCode.SOLUTION_REVIEW);
    assertThat(history).hasSize(2);
    assertThat(history.get(0).content().guards()).isNull();
    assertThat(history.get(0).publishedAt()).isNull();
    assertThat(history.get(1).content().guards()).isEqualTo(ResponseGuards.NONE);
    assertThat(jdbc.lastParams()).containsEntry(1, "SOLUTION_REVIEW");

    assertThat(repo.find(FeatureCode.SOLUTION_REVIEW, PromptStatus.ACTIVE))
        .get()
        .satisfies(t -> assertThat(t.content().guards().answerInVietnamese()).isTrue());
    assertThat(jdbc.lastParams()).containsEntry(2, "ACTIVE");
    assertThat(repo.find(FeatureCode.MOCK_INTERVIEW, PromptStatus.DRAFT)).isEmpty();
  }

  @Test
  void lockUsesPerFeatureAdvisoryKey() {
    var jdbc = new FakeJdbc();
    jdbc.row("pg_advisory_xact_lock", 1);
    var repo = new JdbcPromptTemplateRepository(jdbc.provider());

    repo.lock(FeatureCode.MOCK_INTERVIEW);

    assertThat(jdbc.lastSql()).contains("pg_advisory_xact_lock");
    assertThat(jdbc.lastParams()).containsEntry(1, "ai:prompt:MOCK_INTERVIEW");
  }

  @Test
  void createDraftComputesNextMinorFromLargestParsableVersion() {
    var jdbc = new FakeJdbc();
    jdbc.rows(
        List.of(
            FakeJdbc.row0("version", "v1.0"),
            FakeJdbc.row0("version", "v2.3"),
            FakeJdbc.row0("version", "garbage"),
            FakeJdbc.row0("version", "v2.1")));
    jdbc.rows(List.of(templateRow("DRAFT", "{}", null)));
    var repo = new JdbcPromptTemplateRepository(jdbc.provider());

    PromptTemplate created = repo.createDraft(FeatureCode.SOLUTION_REVIEW, content(), USER);

    assertThat(created.status()).isEqualTo(PromptStatus.DRAFT);
    Map<Integer, Object> insert = jdbc.lastParams();
    assertThat(insert.get(3)).isEqualTo("v2.4");
    assertThat(insert.get(4)).isEqualTo("DRAFT");
    assertThat(insert.get(8).toString())
        .contains("\"citeLines\":true")
        .contains("\"answerInVietnamese\":true");
    assertThat(insert).containsEntry(9, USER);
  }

  @Test
  void createActiveStartsAtV1ZeroWhenNothingParses() {
    var jdbc = new FakeJdbc();
    jdbc.none();
    jdbc.rows(List.of(templateRow("ACTIVE", null, T0_UTC)));
    var repo = new JdbcPromptTemplateRepository(jdbc.provider());

    repo.createActive(FeatureCode.GENERATE_TESTCASE, new PromptContent("sys", "m", null), USER);

    assertThat(jdbc.lastParams()).containsEntry(3, "v1.0").containsEntry(4, "ACTIVE");
    assertThat(jdbc.lastParams()).containsEntry(8, null);
  }

  @Test
  void updateDraftPromoteAndArchiveTouchOneRowById() {
    var jdbc = new FakeJdbc();
    jdbc.rows(List.of(templateRow("DRAFT", "{}", null)));
    jdbc.rows(List.of(templateRow("ACTIVE", "{}", T0_UTC)));
    var repo = new JdbcPromptTemplateRepository(jdbc.provider());

    PromptTemplate draft = repo.updateDraft(ID, content());
    assertThat(draft.status()).isEqualTo(PromptStatus.DRAFT);
    assertThat(jdbc.lastParams()).containsEntry(1, "sys").containsEntry(2, "model-b");
    assertThat(jdbc.lastParams()).containsEntry(4, ID);

    PromptTemplate promoted = repo.promote(ID, content());
    assertThat(promoted.status()).isEqualTo(PromptStatus.ACTIVE);
    assertThat(jdbc.lastSql()).contains("status = 'ACTIVE'").contains("published_at = now()");

    repo.archive(ID);
    assertThat(jdbc.lastSql()).contains("status = 'ARCHIVED'");
    assertThat(jdbc.lastParams()).containsEntry(1, ID);
  }

  private static StoredSolutionReview stored() {
    return new StoredSolutionReview(
        ID, ID, USER, PROBLEM, "JAVA", "hash", ID, "{\"k\":1}", 8, false, T0);
  }

  private static Map<String, Object> reviewRow() {
    return FakeJdbc.row0(
        "id", ID,
        "submission_id", ID,
        "user_id", USER,
        "problem_id", PROBLEM,
        "language", "JAVA",
        "source_hash", "hash",
        "prompt_template_id", ID,
        "result_json", "{\"k\":1}",
        "readability_score", 8,
        "served_from_cache", true,
        "created_at", T0_UTC);
  }

  @Test
  void findBySubmissionMapsStoredReview() {
    var jdbc = new FakeJdbc();
    jdbc.rows(List.of(reviewRow()));
    jdbc.none();
    var repo = new JdbcSolutionReviewRepository(jdbc.provider());

    StoredSolutionReview review = repo.findBySubmission(ID).orElseThrow();

    assertThat(review.servedFromCache()).isTrue();
    assertThat(review.readabilityScore()).isEqualTo(8);
    assertThat(review.createdAt()).isEqualTo(T0);
    assertThat(review.resultJson()).isEqualTo("{\"k\":1}");
    assertThat(repo.findBySubmission(ID)).isEmpty();
  }

  @Test
  void findCacheBindsTheFourKeyPartsAndMapsEntry() {
    var jdbc = new FakeJdbc();
    jdbc.row("cache_id", ID, "review_id", PROBLEM, "result_json", "{}", "readability_score", 6);
    jdbc.none();
    var repo = new JdbcSolutionReviewRepository(jdbc.provider());

    assertThat(repo.findCache(PROBLEM, "JAVA", "h", ID))
        .contains(new ReviewCacheEntry(ID, PROBLEM, "{}", 6));
    assertThat(jdbc.lastParams())
        .containsEntry(1, PROBLEM)
        .containsEntry(2, "JAVA")
        .containsEntry(3, "h")
        .containsEntry(4, ID);
    assertThat(repo.findCache(PROBLEM, "CPP", "h", ID)).isEmpty();
  }

  @Test
  void insertsReportWhetherARowWasWritten() {
    var jdbc = new FakeJdbc();
    jdbc.update(1).update(0).update(1).update(0);
    var repo = new JdbcSolutionReviewRepository(jdbc.provider());

    assertThat(repo.insertReview(stored())).isTrue();
    assertThat(jdbc.lastSql()).contains("ON CONFLICT (submission_id) DO NOTHING");
    assertThat(jdbc.lastParams()).containsEntry(8, "{\"k\":1}").containsEntry(11, T0_UTC);
    assertThat(repo.insertReview(stored())).isFalse();

    assertThat(repo.insertCache(USER, stored())).isTrue();
    assertThat(jdbc.lastParams()).containsEntry(1, USER).containsEntry(6, ID);
    assertThat(repo.insertCache(USER, stored())).isFalse();
  }

  @Test
  void hitCounterAndProblemListings() {
    var jdbc = new FakeJdbc();
    jdbc.rows(List.of(FakeJdbc.row0("problem_id", PROBLEM)));
    var recentRow =
        FakeJdbc.row0(
            "id",
            ID,
            "submission_id",
            ID,
            "problem_id",
            PROBLEM,
            "result_json",
            "{}",
            "created_at",
            T0_UTC);
    jdbc.rows(List.of(recentRow));
    jdbc.rows(List.of(recentRow, recentRow));
    jdbc.none();
    var repo = new JdbcSolutionReviewRepository(jdbc.provider());

    repo.incrementHit(ID);
    assertThat(jdbc.lastSql()).contains("hit_count = hit_count + 1");

    assertThat(repo.distinctProblemIds(USER)).containsExactly(PROBLEM);
    assertThat(repo.latestForProblem(USER, PROBLEM))
        .get()
        .satisfies(r -> assertThat(r.createdAt()).isEqualTo(T0));
    assertThat(repo.recent(USER, 5)).hasSize(2);
    assertThat(jdbc.lastParams()).containsEntry(2, 5);
    assertThat(repo.latestForProblem(USER, PROBLEM)).isEmpty();
  }
}
