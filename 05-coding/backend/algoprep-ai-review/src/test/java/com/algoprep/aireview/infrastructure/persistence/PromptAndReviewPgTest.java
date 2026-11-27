package com.algoprep.aireview.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.algoprep.aireview.domain.model.FeatureCode;
import com.algoprep.aireview.domain.model.ResponseGuards;
import com.algoprep.aireview.domain.model.prompt.PromptContent;
import com.algoprep.aireview.domain.model.prompt.PromptSnapshot;
import com.algoprep.aireview.domain.model.prompt.PromptStatus;
import com.algoprep.aireview.domain.model.prompt.PromptTemplate;
import com.algoprep.aireview.domain.model.review.RecentSolutionReview;
import com.algoprep.aireview.domain.model.review.ReviewCacheEntry;
import com.algoprep.aireview.domain.model.review.StoredSolutionReview;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;

/** Prompt templates, the active-prompt snapshot and solution reviews with their cache. */
class PromptAndReviewPgTest extends PostgresTestBase {
  private static final UUID SEEDED_REVIEW_PROMPT =
      UUID.fromString("50000000-0000-4000-8000-000000000001");
  private static final String HASH = "a".repeat(64);

  private final JdbcPromptTemplateRepository prompts = new JdbcPromptTemplateRepository(provider());
  private final JdbcAiPromptSnapshotRepository snapshots =
      new JdbcAiPromptSnapshotRepository(provider());
  private final JdbcSolutionReviewRepository reviews = new JdbcSolutionReviewRepository(provider());

  private static PromptContent content(String text, boolean guards) {
    return new PromptContent(
        text, "model-x", guards ? new ResponseGuards(true, false, true) : null);
  }

  private PromptTemplate active(FeatureCode feature) {
    return prompts.find(feature, PromptStatus.ACTIVE).orElseThrow();
  }

  private void setVersion(FeatureCode feature, String version) {
    jdbc.sql(
            "UPDATE ai.prompt_templates SET version = :v WHERE feature_code = :f AND status = 'ACTIVE'")
        .param("v", version)
        .param("f", feature.name())
        .update();
  }

  private int activeCount(FeatureCode feature) {
    return jdbc.sql(
            "SELECT count(*) FROM ai.prompt_templates WHERE feature_code = :f AND status = 'ACTIVE'")
        .param("f", feature.name())
        .query(Integer.class)
        .single();
  }

  private StoredSolutionReview review(
      UUID submission, UUID user, UUID problem, Instant at, String json) {
    return new StoredSolutionReview(
        UUID.randomUUID(),
        submission,
        user,
        problem,
        "JAVA",
        HASH,
        SEEDED_REVIEW_PROMPT,
        json,
        4,
        false,
        at);
  }

  @Test
  void seededActiveTemplatesAreListedInFeatureOrderWithNullGuardsForTestcaseGeneration() {
    var live = prompts.listLive();

    assertThat(live)
        .extracting(PromptTemplate::featureCode)
        .containsExactly(
            FeatureCode.SOLUTION_REVIEW, FeatureCode.MOCK_INTERVIEW, FeatureCode.GENERATE_TESTCASE);
    assertThat(live).extracting(PromptTemplate::version).containsOnly("v1.0");
    assertThat(live.get(0).content().guards()).isEqualTo(new ResponseGuards(true, true, true));
    assertThat(live.get(2).content().guards()).isNull();
    assertThat(live.get(0).publishedAt()).isNotNull();
  }

  @Test
  void snapshotReadsTheActiveRowAndFallsBackToNoGuardsWhenNull() {
    PromptSnapshot review = snapshots.readActive(FeatureCode.SOLUTION_REVIEW).orElseThrow();
    PromptSnapshot generator = snapshots.readActive(FeatureCode.GENERATE_TESTCASE).orElseThrow();

    assertThat(review.id()).isEqualTo(SEEDED_REVIEW_PROMPT);
    assertThat(review.guards()).isEqualTo(new ResponseGuards(true, true, true));
    assertThat(review.modelId()).isEqualTo("default");
    assertThat(generator.guards()).isEqualTo(ResponseGuards.NONE);
    assertThat(snapshots.readActive(FeatureCode.PRACTICE_FEEDBACK)).isEmpty();

    prompts.archive(SEEDED_REVIEW_PROMPT);

    assertThat(snapshots.readActive(FeatureCode.SOLUTION_REVIEW)).isEmpty();
  }

  @Test
  void createDraftTakesTheNextMinorCopiesSchemaVersionAndHasNoPublishDate() {
    UUID actor = UUID.randomUUID();

    PromptTemplate draft =
        prompts.createDraft(FeatureCode.SOLUTION_REVIEW, content("draft", true), actor);

    assertThat(draft.version()).isEqualTo("v1.1");
    assertThat(draft.status()).isEqualTo(PromptStatus.DRAFT);
    assertThat(draft.publishedAt()).isNull();
    assertThat(draft.responseSchemaVersion()).isEqualTo("v1");
    assertThat(draft.content().guards()).isEqualTo(new ResponseGuards(true, false, true));
    assertThat(
            jdbc.sql("SELECT created_by FROM ai.prompt_templates WHERE id = :id")
                .param("id", draft.id())
                .query(UUID.class)
                .single())
        .isEqualTo(actor);
    assertThat(prompts.find(FeatureCode.SOLUTION_REVIEW, PromptStatus.DRAFT)).contains(draft);
  }

  @Test
  void onlyOneDraftAndOnlyOneActivePerFeatureOnTheRealPartialIndexes() {
    prompts.createDraft(FeatureCode.SOLUTION_REVIEW, content("one", true), null);

    assertThatThrownBy(
            () -> prompts.createDraft(FeatureCode.SOLUTION_REVIEW, content("two", true), null))
        .isInstanceOf(DuplicateKeyException.class);
    assertThatThrownBy(
            () -> prompts.createActive(FeatureCode.SOLUTION_REVIEW, content("three", true), null))
        .isInstanceOf(DuplicateKeyException.class);

    assertThat(activeCount(FeatureCode.SOLUTION_REVIEW)).isEqualTo(1);
    assertThat(prompts.history(FeatureCode.SOLUTION_REVIEW)).hasSize(2);
    // Another feature is not blocked by this one.
    assertThat(prompts.createDraft(FeatureCode.MOCK_INTERVIEW, content("m", true), null).version())
        .isEqualTo("v1.1");
  }

  @Test
  void guardsMustBeNullExactlyForTestcaseGenerationOnTheRealCheck() {
    PromptTemplate draft =
        prompts.createDraft(FeatureCode.GENERATE_TESTCASE, content("g", false), null);
    assertThat(draft.content().guards()).isNull();

    assertThatThrownBy(() -> prompts.updateDraft(draft.id(), content("g2", true)))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(
            () ->
                prompts.createActive(FeatureCode.MOCK_INTERVIEW, content("no guards", false), null))
        .isInstanceOf(DataIntegrityViolationException.class);

    assertThat(
            prompts
                .find(FeatureCode.GENERATE_TESTCASE, PromptStatus.DRAFT)
                .orElseThrow()
                .content()
                .systemInstruction())
        .isEqualTo("g");
  }

  @Test
  void publishArchivesTheOldActiveThenPromotesTheDraftKeepingOneActive() {
    PromptTemplate old = active(FeatureCode.SOLUTION_REVIEW);
    PromptTemplate draft =
        prompts.createDraft(FeatureCode.SOLUTION_REVIEW, content("draft", true), null);

    prompts.archive(old.id());
    PromptTemplate promoted = prompts.promote(draft.id(), content("final", true));

    assertThat(promoted.id()).isEqualTo(draft.id());
    assertThat(promoted.status()).isEqualTo(PromptStatus.ACTIVE);
    assertThat(promoted.version()).isEqualTo("v1.1");
    assertThat(promoted.publishedAt()).isNotNull();
    assertThat(promoted.content().systemInstruction()).isEqualTo("final");
    assertThat(activeCount(FeatureCode.SOLUTION_REVIEW)).isEqualTo(1);
    assertThat(prompts.find(FeatureCode.SOLUTION_REVIEW, PromptStatus.DRAFT)).isEmpty();
    assertThat(prompts.find(FeatureCode.SOLUTION_REVIEW, PromptStatus.ARCHIVED))
        .map(PromptTemplate::id)
        .contains(old.id());
    assertThat(prompts.history(FeatureCode.SOLUTION_REVIEW))
        .extracting(PromptTemplate::id)
        .containsExactlyInAnyOrder(old.id(), draft.id());
  }

  @Test
  void promoteWithoutArchivingFirstIsRejectedAndLeavesTheDraftUntouched() {
    PromptTemplate draft =
        prompts.createDraft(FeatureCode.MOCK_INTERVIEW, content("draft", true), null);

    assertThatThrownBy(() -> prompts.promote(draft.id(), content("final", true)))
        .isInstanceOf(DuplicateKeyException.class);

    PromptTemplate stillDraft =
        prompts.find(FeatureCode.MOCK_INTERVIEW, PromptStatus.DRAFT).orElseThrow();
    assertThat(stillDraft.content().systemInstruction()).isEqualTo("draft");
    assertThat(stillDraft.publishedAt()).isNull();
  }

  @Test
  void updateDraftRewritesContentAndGuardsButNotStatusOrVersion() {
    PromptTemplate draft =
        prompts.createDraft(FeatureCode.SOLUTION_REVIEW, content("v1", true), null);

    PromptTemplate updated =
        prompts.updateDraft(
            draft.id(), new PromptContent("v2", "model-y", new ResponseGuards(false, true, false)));

    assertThat(updated.content())
        .isEqualTo(new PromptContent("v2", "model-y", new ResponseGuards(false, true, false)));
    assertThat(updated.status()).isEqualTo(PromptStatus.DRAFT);
    assertThat(updated.version()).isEqualTo("v1.1");
  }

  @Test
  void createActiveAfterArchiveIsPublishedNowWithTheNextVersion() {
    prompts.archive(SEEDED_REVIEW_PROMPT);

    PromptTemplate created =
        prompts.createActive(FeatureCode.SOLUTION_REVIEW, content("fresh", true), null);

    assertThat(created.version()).isEqualTo("v1.1");
    assertThat(created.status()).isEqualTo(PromptStatus.ACTIVE);
    assertThat(created.publishedAt()).isNotNull();
    assertThat(activeCount(FeatureCode.SOLUTION_REVIEW)).isEqualTo(1);
  }

  @Test
  void nextVersionComparesNumericallyNotAsText() {
    setVersion(FeatureCode.SOLUTION_REVIEW, "v1.9");
    PromptTemplate draft =
        prompts.createDraft(FeatureCode.SOLUTION_REVIEW, content("d", true), null);
    prompts.archive(SEEDED_REVIEW_PROMPT);
    prompts.archive(active(FeatureCode.MOCK_INTERVIEW).id());
    prompts.promote(draft.id(), content("d", true));
    prompts.archive(draft.id());

    PromptTemplate next =
        prompts.createDraft(FeatureCode.SOLUTION_REVIEW, content("n", true), null);

    // Text order would put v1.9 above v1.10 and answer v1.10 again, a unique violation.
    assertThat(draft.version()).isEqualTo("v1.10");
    assertThat(next.version()).isEqualTo("v1.11");
  }

  @Test
  void nextVersionFollowsTheHighestMajorAndStartsAtV10WhenNothingParses() {
    setVersion(FeatureCode.MOCK_INTERVIEW, "v2.3");
    assertThat(prompts.createDraft(FeatureCode.MOCK_INTERVIEW, content("d", true), null).version())
        .isEqualTo("v2.4");

    setVersion(FeatureCode.SOLUTION_REVIEW, "legacy");
    prompts.archive(SEEDED_REVIEW_PROMPT);
    PromptTemplate fallback =
        prompts.createActive(FeatureCode.SOLUTION_REVIEW, content("f", true), null);

    assertThat(fallback.version()).isEqualTo("v1.0");
  }

  @Test
  void historyIsNewestFirst() {
    PromptTemplate draft =
        prompts.createDraft(FeatureCode.SOLUTION_REVIEW, content("d", true), null);

    assertThat(prompts.history(FeatureCode.SOLUTION_REVIEW))
        .extracting(PromptTemplate::id)
        .containsExactly(draft.id(), SEEDED_REVIEW_PROMPT);
    assertThat(prompts.listLive())
        .filteredOn(t -> t.featureCode() == FeatureCode.SOLUTION_REVIEW)
        .extracting(PromptTemplate::status)
        .containsExactly(PromptStatus.ACTIVE, PromptStatus.DRAFT);
  }

  @Test
  void promptAdvisoryLockIsPerFeatureAndHeldUntilCommit() {
    Boolean[] sameFeature = new Boolean[2];
    Boolean[] otherFeature = new Boolean[1];
    String key = "SELECT pg_try_advisory_xact_lock(hashtextextended(:k, 0))";

    tx.executeWithoutResult(
        status -> {
          prompts.lock(FeatureCode.SOLUTION_REVIEW);
          sameFeature[0] =
              other.execute(
                  s ->
                      jdbc.sql(key)
                          .param("k", "ai:prompt:SOLUTION_REVIEW")
                          .query(Boolean.class)
                          .single());
          otherFeature[0] =
              other.execute(
                  s ->
                      jdbc.sql(key)
                          .param("k", "ai:prompt:MOCK_INTERVIEW")
                          .query(Boolean.class)
                          .single());
        });
    sameFeature[1] =
        tx.execute(
            s ->
                jdbc.sql(key)
                    .param("k", "ai:prompt:SOLUTION_REVIEW")
                    .query(Boolean.class)
                    .single());

    assertThat(sameFeature).containsExactly(false, true);
    assertThat(otherFeature).containsExactly(true);
  }

  @Test
  void insertReviewIsFirstWriterWinsOnTheSubmissionUniqueKey() {
    UUID submission = UUID.randomUUID();
    UUID user = UUID.randomUUID();
    UUID problem = UUID.randomUUID();
    Instant at = now();
    StoredSolutionReview first = review(submission, user, problem, at, "{\"a\":1}");
    StoredSolutionReview racer = review(submission, user, problem, at, "{\"a\":2}");

    assertThat(reviews.insertReview(first)).isTrue();
    assertThat(reviews.insertReview(racer)).isFalse();

    assertThat(count("solution_reviews")).isEqualTo(1);
    StoredSolutionReview stored = reviews.findBySubmission(submission).orElseThrow();
    assertThat(stored.id()).isEqualTo(first.id());
    assertThat(stored.resultJson()).isEqualTo("{\"a\": 1}");
    assertThat(stored.createdAt()).isEqualTo(at);
    assertThat(stored.readabilityScore()).isEqualTo(4);
    assertThat(reviews.findBySubmission(UUID.randomUUID())).isEmpty();
  }

  @Test
  void reviewRowsRespectTheHashAndReadabilityChecksAndTheTemplateForeignKey() {
    Instant at = now();
    UUID user = UUID.randomUUID();
    StoredSolutionReview badHash =
        new StoredSolutionReview(
            UUID.randomUUID(),
            UUID.randomUUID(),
            user,
            UUID.randomUUID(),
            "JAVA",
            "XYZ",
            SEEDED_REVIEW_PROMPT,
            "{}",
            3,
            false,
            at);
    StoredSolutionReview badScore =
        new StoredSolutionReview(
            UUID.randomUUID(),
            UUID.randomUUID(),
            user,
            UUID.randomUUID(),
            "JAVA",
            HASH,
            SEEDED_REVIEW_PROMPT,
            "{}",
            6,
            false,
            at);
    StoredSolutionReview noTemplate =
        new StoredSolutionReview(
            UUID.randomUUID(),
            UUID.randomUUID(),
            user,
            UUID.randomUUID(),
            "JAVA",
            HASH,
            UUID.randomUUID(),
            "{}",
            3,
            false,
            at);

    assertThatThrownBy(() -> reviews.insertReview(badHash))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(() -> reviews.insertReview(badScore))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(() -> reviews.insertReview(noTemplate))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThat(count("solution_reviews")).isZero();
  }

  @Test
  void cacheKeyConflictReportsFalseAndKeepsTheOriginalEntry() {
    UUID problem = UUID.randomUUID();
    StoredSolutionReview original =
        review(UUID.randomUUID(), UUID.randomUUID(), problem, now(), "{\"ok\":true}");
    StoredSolutionReview duplicate =
        review(UUID.randomUUID(), UUID.randomUUID(), problem, now(), "{\"ok\":false}");
    reviews.insertReview(original);
    reviews.insertReview(duplicate);
    UUID firstCache = UUID.randomUUID();

    assertThat(reviews.insertCache(firstCache, original)).isTrue();
    assertThat(reviews.insertCache(UUID.randomUUID(), duplicate)).isFalse();

    assertThat(count("ai_cache_entries")).isEqualTo(1);
    ReviewCacheEntry hit =
        reviews.findCache(problem, "JAVA", HASH, SEEDED_REVIEW_PROMPT).orElseThrow();
    assertThat(hit.cacheId()).isEqualTo(firstCache);
    assertThat(hit.originalReviewId()).isEqualTo(original.id());
    assertThat(hit.resultJson()).isEqualTo("{\"ok\": true}");
    assertThat(hit.readabilityScore()).isEqualTo(4);
  }

  @Test
  void findCacheNeedsAllFourKeyPartsToMatch() {
    UUID problem = UUID.randomUUID();
    StoredSolutionReview original =
        review(UUID.randomUUID(), UUID.randomUUID(), problem, now(), "{}");
    reviews.insertReview(original);
    reviews.insertCache(UUID.randomUUID(), original);

    assertThat(reviews.findCache(UUID.randomUUID(), "JAVA", HASH, SEEDED_REVIEW_PROMPT)).isEmpty();
    assertThat(reviews.findCache(problem, "PYTHON", HASH, SEEDED_REVIEW_PROMPT)).isEmpty();
    assertThat(reviews.findCache(problem, "JAVA", "b".repeat(64), SEEDED_REVIEW_PROMPT)).isEmpty();
    assertThat(reviews.findCache(problem, "JAVA", HASH, UUID.randomUUID())).isEmpty();
    assertThat(reviews.findCache(problem, "JAVA", HASH, SEEDED_REVIEW_PROMPT)).isPresent();
  }

  @Test
  void incrementHitAddsOnePerCallToTheRightEntryOnly() {
    UUID problem = UUID.randomUUID();
    StoredSolutionReview a = review(UUID.randomUUID(), UUID.randomUUID(), problem, now(), "{}");
    StoredSolutionReview b =
        review(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), now(), "{}");
    reviews.insertReview(a);
    reviews.insertReview(b);
    UUID cacheA = UUID.randomUUID();
    UUID cacheB = UUID.randomUUID();
    reviews.insertCache(cacheA, a);
    reviews.insertCache(cacheB, b);

    reviews.incrementHit(cacheA);
    reviews.incrementHit(cacheA);

    assertThat(hits(cacheA)).isEqualTo(2);
    assertThat(hits(cacheB)).isZero();
  }

  @Test
  void recentLatestAndDistinctProblemsAreScopedToTheUserAndOrderedNewestFirst() {
    UUID user = UUID.randomUUID();
    UUID p1 = UUID.randomUUID();
    UUID p2 = UUID.randomUUID();
    Instant base = now().minus(1, ChronoUnit.DAYS);
    StoredSolutionReview oldP1 = review(UUID.randomUUID(), user, p1, base, "{\"n\":1}");
    StoredSolutionReview newP1 =
        review(UUID.randomUUID(), user, p1, base.plusSeconds(10), "{\"n\":2}");
    StoredSolutionReview onlyP2 =
        review(UUID.randomUUID(), user, p2, base.plusSeconds(5), "{\"n\":3}");
    StoredSolutionReview stranger =
        review(UUID.randomUUID(), UUID.randomUUID(), p1, base.plusSeconds(99), "{}");
    for (StoredSolutionReview r : java.util.List.of(oldP1, newP1, onlyP2, stranger)) {
      reviews.insertReview(r);
    }

    assertThat(reviews.recent(user, 10))
        .extracting(RecentSolutionReview::reviewId)
        .containsExactly(newP1.id(), onlyP2.id(), oldP1.id());
    assertThat(reviews.recent(user, 2)).hasSize(2);
    assertThat(reviews.latestForProblem(user, p1))
        .map(RecentSolutionReview::reviewId)
        .contains(newP1.id());
    assertThat(reviews.latestForProblem(user, UUID.randomUUID())).isEmpty();
    assertThat(reviews.distinctProblemIds(user)).containsExactlyInAnyOrder(p1, p2);
    assertThat(reviews.distinctProblemIds(UUID.randomUUID())).isEmpty();
  }

  private int hits(UUID cacheId) {
    return jdbc.sql("SELECT hit_count FROM ai.ai_cache_entries WHERE id = :id")
        .param("id", cacheId)
        .query(Integer.class)
        .single();
  }
}
