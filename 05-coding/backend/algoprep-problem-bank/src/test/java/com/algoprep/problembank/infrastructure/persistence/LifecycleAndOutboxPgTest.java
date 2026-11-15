package com.algoprep.problembank.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.algoprep.problembank.domain.model.OutboxEvent;
import com.algoprep.problembank.domain.model.TestcaseGenerationRun;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;

/** Publish, statistics, bulk edits, AI quota, outbox relay and small read repositories. */
class LifecycleAndOutboxPgTest extends PostgresTestBase {
  private JdbcProblemPublishRepository publish;
  private JdbcProblemStatsRepository stats;
  private JdbcProblemBulkRepository bulk;
  private JdbcTestcaseGenerationRepository generation;
  private JdbcProblemOutboxRepository outbox;
  private JdbcProblemBankStatsRepository bankStats;
  private JdbcSubmissionMetadataRepository metadata;
  private JdbcHistoricalTopicProblemRepository historical;
  private JdbcSuggestedProblemReadRepository suggested;
  private UUID p;

  @BeforeEach
  void seed() {
    var provider = provider();
    publish = new JdbcProblemPublishRepository(provider);
    stats = new JdbcProblemStatsRepository(provider);
    bulk = new JdbcProblemBulkRepository(provider);
    generation = new JdbcTestcaseGenerationRepository(provider);
    outbox = new JdbcProblemOutboxRepository(provider);
    bankStats = new JdbcProblemBankStatsRepository(provider);
    metadata = new JdbcSubmissionMetadataRepository(provider);
    historical = new JdbcHistoricalTopicProblemRepository(provider);
    suggested = new JdbcSuggestedProblemReadRepository(provider);
    p = problem(1, "P-ONE", "One", "UNPUBLISHED", EASY);
  }

  private static String envelope(long n) {
    return "{\"eventId\":\"" + id(n) + "\"}";
  }

  // ---- publish ---------------------------------------------------------------------------------

  @Test
  void publishStampsPublishedAtAndUnpublishKeepsIt() {
    var published = publish.setStatus(p, id(901), "PUBLISHED");
    var hidden = publish.setStatus(p, id(901), "UNPUBLISHED");

    assertThat(published.publishedAt()).isNotNull();
    assertThat(hidden.publishedAt()).isEqualTo(published.publishedAt());
    assertThat(hidden.updatedAt()).isAfterOrEqualTo(published.updatedAt());
    assertThat(published.currentTestcaseSetVersion()).isEqualTo(1);
    assertThat(count("problems", "status='UNPUBLISHED' AND published_at IS NOT NULL")).isEqualTo(1);
    assertThatThrownBy(() -> publish.setStatus(p, id(901), "ARCHIVED"))
        .isInstanceOf(DataIntegrityViolationException.class);
    softDelete(p);
    assertThatThrownBy(() -> publish.setStatus(p, id(901), "PUBLISHED"))
        .isInstanceOf(EmptyResultDataAccessException.class);
  }

  @Test
  void publishEventRowCarriesEnvelopeIdAndRejectsReplays() {
    publish.appendEvent(p, "ProblemPublished", "problem.published", envelope(700));

    var row =
        jdbc.sql(
                "SELECT aggregate_type||'|'||event_type||'|'||routing_key||'|'||published_at IS NULL FROM problem.outbox_events WHERE id=:id")
            .param("id", id(700))
            .query(Boolean.class)
            .single();
    assertThat(row).isNotNull();
    assertThat(count("outbox_events", "payload->>'eventId' = '" + id(700) + "'")).isEqualTo(1);
    assertThatThrownBy(
            () -> publish.appendEvent(p, "ProblemPublished", "problem.published", envelope(700)))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  // ---- statistics ------------------------------------------------------------------------------

  @Test
  void consumedEventsDedupeAndCountersKeepAnExactRate() {
    assertThat(stats.markConsumed(id(700))).isTrue();
    assertThat(stats.markConsumed(id(700))).isFalse();
    assertThat(count("consumed_events")).isEqualTo(1);

    assertThat(stats.addSubmission(p, true)).isTrue();
    assertThat(stats.find(p).orElseThrow().acRate()).isEqualByComparingTo("100.00");
    stats.addSubmission(p, false);
    assertThat(stats.find(p).orElseThrow().acRate()).isEqualByComparingTo("50.00");
    stats.addSubmission(p, false);

    var row = stats.find(p).orElseThrow();
    assertThat(row.submissionCount()).isEqualTo(3);
    assertThat(row.acRate()).isEqualByComparingTo("33.33");
    assertThat(row.updatedAt()).isNotNull();
    assertThat(count("problem_stats", "accepted_count=1 AND submission_count=3")).isEqualTo(1);
  }

  @Test
  void statisticsOfUnknownOrFreshProblemsAreHandled() {
    assertThat(stats.addSubmission(id(999), true)).isFalse();
    assertThat(count("problem_stats")).isZero();

    var fresh = stats.find(p).orElseThrow();
    assertThat(fresh.submissionCount()).isZero();
    assertThat(fresh.acRate()).isNull();
    assertThat(fresh.updatedAt()).isNull();
    assertThat(fresh.authorId()).isEqualTo(AUTHOR);
    softDelete(p);
    assertThat(stats.find(p)).isEmpty();
    assertThat(stats.find(id(999))).isEmpty();
  }

  @Test
  void statsCheckConstraintBlocksMoreAcceptedThanSubmissions() {
    assertThatThrownBy(() -> stats(p, 1, 2)).isInstanceOf(DataIntegrityViolationException.class);
  }

  // ---- bulk edits ------------------------------------------------------------------------------

  @Test
  void bulkLevelTopicAndDeleteWriteRealState() {
    assertThat(bulk.levelExists(HARD)).isTrue();
    assertThat(bulk.levelExists(id(999))).isFalse();
    assertThat(bulk.topicExists(MANG)).isTrue();
    assertThat(bulk.topicExists(id(999))).isFalse();

    bulk.setLevel(p, id(901), HARD);
    assertThat(count("problems", "level_id='" + HARD + "' AND updated_by='" + id(901) + "'"))
        .isEqualTo(1);

    assertThat(bulk.addTopic(p, id(902), MANG)).isTrue();
    assertThat(bulk.addTopic(p, id(903), MANG)).isFalse();
    assertThat(count("problem_topics")).isEqualTo(1);
    assertThat(count("problems", "updated_by='" + id(902) + "'")).isEqualTo(1);

    var at = bulk.markDeleted(p, id(904));
    assertThat(at).isNotNull();
    assertThat(count("problems", "deleted AND status='UNPUBLISHED'")).isEqualTo(1);
    assertThatThrownBy(() -> bulk.markDeleted(p, id(904)))
        .isInstanceOf(EmptyResultDataAccessException.class);
    bulk.setLevel(p, id(905), EASY);
    assertThat(count("problems", "level_id='" + HARD + "'")).isEqualTo(1);
  }

  @Test
  void bulkSetLevelToUnknownLevelViolatesTheForeignKey() {
    assertThatThrownBy(() -> bulk.setLevel(p, id(901), id(999)))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(() -> bulk.addTopic(p, id(901), id(999)))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  // ---- AI generation quota ---------------------------------------------------------------------

  @Test
  void quotaStopsAtTheConfiguredCapAndCapChangesApply() {
    assertThat(generation.lockCap()).isEqualTo(2);
    assertThat(generation.consumeQuota(p)).isTrue();
    assertThat(generation.consumeQuota(p)).isTrue();
    assertThat(generation.consumeQuota(p)).isFalse();
    assertThat(count("problems", "ai_testcase_generations_used=2")).isEqualTo(1);

    generation.updateCap(3, id(901));
    assertThat(generation.consumeQuota(p)).isTrue();
    assertThat(generation.consumeQuota(p)).isFalse();
    assertThat(
            count(
                "problem_bank_settings",
                "max_ai_generations_per_problem=3 AND updated_by='" + id(901) + "'"))
        .isEqualTo(1);
    assertThat(generation.consumeQuota(id(999))).isFalse();
    assertThatThrownBy(() -> generation.updateCap(11, id(901)))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThat(generation.lockCap()).isEqualTo(3);
  }

  @Test
  void generationRunIsStoredWithJsonbDroppedListAndPositiveAddedCount() {
    var run =
        new TestcaseGenerationRun(
            id(50), p, "gen", "val", 42L, "model", "v1", id(901), 3, "[{\"why\":\"dup\"}]");
    generation.insertRun(run);

    assertThat(
            count(
                "testcase_generation_runs",
                "dropped_json->0->>'why'='dup' AND seed=42 AND added_count=3"))
        .isEqualTo(1);
    var empty = new TestcaseGenerationRun(id(51), p, "g", "v", 1L, "m", "v1", id(901), 0, "[]");
    assertThatThrownBy(() -> generation.insertRun(empty))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThat(count("testcase_generation_runs")).isEqualTo(1);
  }

  // ---- outbox relay ----------------------------------------------------------------------------

  private void outboxRow(long n, String createdAgo) {
    jdbc.sql(
            "INSERT INTO problem.outbox_events(id,aggregate_type,aggregate_id,event_type,routing_key,payload,created_at)"
                + " VALUES(:id,'Problem',:p,'E','k',CAST(:payload AS jsonb), now() - CAST(:ago AS interval))")
        .param("id", id(n))
        .param("p", p)
        .param("payload", envelope(n))
        .param("ago", createdAgo)
        .update();
  }

  @Test
  void pendingRowsComeOldestFirstAndBackoffHidesAFreshlyFailedRow() {
    outboxRow(1, "3 minutes");
    outboxRow(2, "2 minutes");
    outboxRow(3, "1 minute");

    assertThat(outbox.lockPending(10))
        .extracting(OutboxEvent::id)
        .containsExactly(id(1), id(2), id(3));
    assertThat(outbox.lockPending(2)).hasSize(2);

    outbox.attempted(id(1), false);

    var after = outbox.lockPending(10);
    assertThat(after).extracting(OutboxEvent::id).containsExactly(id(2), id(3));
    assertThat(
            count(
                "outbox_events",
                "id='"
                    + id(1)
                    + "' AND publish_attempts=1 AND published_at IS NULL AND last_attempted_at IS NOT NULL"))
        .isEqualTo(1);

    jdbc.sql(
            "UPDATE problem.outbox_events SET last_attempted_at = now() - interval '10 seconds' WHERE id=:id")
        .param("id", id(1))
        .update();
    assertThat(outbox.lockPending(10)).extracting(OutboxEvent::id).contains(id(1));
  }

  @Test
  void confirmedRowsLeaveThePendingSetAndStayConfirmed() {
    outboxRow(1, "1 minute");

    outbox.attempted(id(1), true);
    outbox.attempted(id(1), false);

    assertThat(outbox.lockPending(10)).isEmpty();
    assertThat(count("outbox_events", "published_at IS NOT NULL AND publish_attempts=1"))
        .isEqualTo(1);
    assertThat(outbox.lockPending(10)).isEmpty();
  }

  @Test
  void attemptCounterIsCappedAtIntegerMax() {
    outboxRow(1, "1 minute");
    jdbc.sql("UPDATE problem.outbox_events SET publish_attempts=2147483647").update();

    outbox.attempted(id(1), false);

    assertThat(count("outbox_events", "publish_attempts=2147483647")).isEqualTo(1);
  }

  @Test
  void lockedRowsAreSkippedByAConcurrentRelay() {
    outboxRow(1, "1 minute");
    outboxRow(2, "1 minute");
    var other = new JdbcProblemOutboxRepository(provider(JdbcClient.create(dataSource())));

    List<OutboxEvent> second =
        tx.execute(
            status -> {
              var mine = outbox.lockPending(1);
              assertThat(mine).hasSize(1);
              return other.lockPending(10);
            });

    assertThat(second).hasSize(1);
  }

  @Test
  void cleanupDeletesOnlyOldPublishedRowsUpToTheLimit() {
    for (int i = 1; i <= 3; i++) {
      outboxRow(i, "30 days");
      jdbc.sql(
              "UPDATE problem.outbox_events SET published_at = now() - interval '20 days' WHERE id=:id")
          .param("id", id(i))
          .update();
    }
    outboxRow(4, "30 days");
    outboxRow(5, "1 day");
    jdbc.sql("UPDATE problem.outbox_events SET published_at = now() WHERE id=:id")
        .param("id", id(5))
        .update();

    assertThat(outbox.deletePublishedOlderThanDays(7, 2)).isEqualTo(2);
    assertThat(outbox.deletePublishedOlderThanDays(7, 10)).isEqualTo(1);
    assertThat(outbox.deletePublishedOlderThanDays(7, 10)).isZero();
    assertThat(count("outbox_events")).isEqualTo(2);
  }

  // ---- small read repositories
  // -------------------------------------------------------------------

  @Test
  void bankStatsReadLevelsAndPublishedFlag() {
    UUID pub = published(2, "B-PUB");
    softDelete(p);

    assertThat(bankStats.levels())
        .extracting(l -> l.code())
        .containsExactly("EASY", "MEDIUM", "HARD");
    assertThat(bankStats.published()).extracting(s -> s.problemId()).containsExactly(pub);
    var all = bankStats.problems(Set.of(p, pub));
    assertThat(all).extracting(s -> s.problemId()).containsExactly(pub, p).doesNotHaveDuplicates();
    assertThat(all).extracting(s -> s.published()).containsExactly(true, false);
  }

  @Test
  void submissionMetadataSummariesSearchAndSamples() {
    UUID pub = published(2, "SEARCH_ME");
    problem(3, "OTHER", "100% sure", "UNPUBLISHED", EASY);
    testcase(pub, 100, "SAMPLE", true, at(1, "s"));
    testcase(pub, 101, "HIDDEN", true, at(2, "h"));
    softDelete(p);

    var summaries = metadata.summaries(Set.of(p, pub));

    assertThat(summaries).filteredOn(s -> s.withdrawn()).hasSize(1);
    assertThat(metadata.search("search_me", 10)).containsExactly(pub);
    assertThat(metadata.search("SEARCHXME", 10)).isEmpty();
    assertThat(metadata.search("100%", 10)).containsExactly(id(3));
    assertThat(metadata.search("%", 10)).containsExactly(id(3));
    assertThat(metadata.search("e", 1)).hasSize(1);
    assertThat(metadata.samples(pub, Set.of(id(100), id(101)))).containsExactly(id(100));
  }

  @Test
  void historicalTopicLinksIncludeEveryProblemEvenWithdrawnOnes() {
    UUID pub = published(2, "HIST");
    link(p, MANG);
    link(pub, MANG);
    softDelete(p);

    var read = historical.read(Set.of(MANG, CHUOI));

    assertThat(read.get(MANG)).containsExactlyInAnyOrder(p, pub);
    assertThat(read.get(CHUOI)).isEmpty();
    assertThat(historical.read(Set.of())).isEmpty();
  }

  @Test
  void suggestedCandidatesAreVisibleProblemsMatchingDifficultyAndTopic() {
    UUID pub = published(2, "SUG-EASY");
    UUID hard = problem(3, "SUG-HARD", "Hard", "PUBLISHED", HARD);
    link(pub, MANG);
    link(pub, CHUOI);
    link(hard, CHUOI);
    stats(pub, 2, 1);

    assertThat(suggested.candidates(null, null))
        .extracting(c -> c.id())
        .containsExactlyInAnyOrder(pub, hard);
    var easy = suggested.candidates("EASY", MANG);
    assertThat(easy)
        .singleElement()
        .satisfies(
            c -> {
              assertThat(c.id()).isEqualTo(pub);
              assertThat(c.topicNames()).containsExactly("Mảng", "Chuỗi");
              assertThat(c.topicIds()).containsExactlyInAnyOrder(MANG, CHUOI);
              assertThat(c.acRate()).isEqualByComparingTo("50.00");
            });
    assertThat(suggested.candidates("UNKNOWN", null)).hasSize(2);
    assertThat(suggested.candidates("HARD", MANG)).isEmpty();
  }
}
