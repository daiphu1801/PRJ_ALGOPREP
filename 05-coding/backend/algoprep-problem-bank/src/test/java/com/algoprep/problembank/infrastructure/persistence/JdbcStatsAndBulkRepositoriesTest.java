package com.algoprep.problembank.infrastructure.persistence;

import static com.algoprep.problembank.infrastructure.persistence.FakeJdbc.row;
import static org.assertj.core.api.Assertions.assertThat;

import com.algoprep.problembank.domain.model.TestcaseGenerationRun;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Bulk actions, AI generation quota, problem statistics and bank statistics persistence. */
class JdbcStatsAndBulkRepositoriesTest {
  private static final UUID P = UUID.fromString("00000000-0000-0000-0000-000000000001");
  private static final UUID ACTOR = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
  private static final UUID L = UUID.fromString("00000000-0000-0000-0000-0000000000c1");

  private final FakeJdbc db = new FakeJdbc();

  // ---- bulk ----

  @Test
  void bulkExistenceChecksUseTheMatchingCatalogueTable() {
    var repo = new JdbcProblemBulkRepository(db.provider());
    db.onQuery("FROM problem.problem_levels", true).onQuery("FROM problem.topics", false);

    assertThat(repo.levelExists(L)).isTrue();
    assertThat(repo.topicExists(L)).isFalse();
  }

  @Test
  void bulkSetLevelBindsLevelActorAndProblem() {
    new JdbcProblemBulkRepository(db.provider()).setLevel(P, ACTOR, L);

    var call = db.last("SET level_id");
    assertThat(call.param("level")).isEqualTo(L);
    assertThat(call.param("actor")).isEqualTo(ACTOR);
    assertThat(call.sql()).contains("NOT deleted");
  }

  @Test
  void addTopicTouchesProblemOnlyWhenLinkWasNew() {
    var repo = new JdbcProblemBulkRepository(db.provider());
    assertThat(repo.addTopic(P, ACTOR, L)).isFalse();
    assertThat(db.count("UPDATE problem.problems")).isZero();

    db.onUpdate("INSERT INTO problem.problem_topics", 1);
    assertThat(repo.addTopic(P, ACTOR, L)).isTrue();
    assertThat(db.count("UPDATE problem.problems")).isEqualTo(1);
  }

  @Test
  void markDeletedReturnsNewVersionTimestamp() {
    Instant stamp = Instant.parse("2026-09-01T00:00:00Z");
    db.onQuery("deleted = true", row("updated_at", stamp));

    assertThat(new JdbcProblemBulkRepository(db.provider()).markDeleted(P, ACTOR)).isEqualTo(stamp);
  }

  // ---- AI generation ----

  @Test
  void consumeQuotaSucceedsOnlyWhenARowWasUpdated() {
    var repo = new JdbcTestcaseGenerationRepository(db.provider());
    assertThat(repo.consumeQuota(P)).isFalse();
    db.onUpdate("ai_testcase_generations_used = ai_testcase_generations_used + 1", 1);
    assertThat(repo.consumeQuota(P)).isTrue();
  }

  @Test
  void insertRunBindsAllRunFieldsAndLockCapUpdateCapWork() {
    var repo = new JdbcTestcaseGenerationRepository(db.provider());
    UUID run = UUID.randomUUID();
    db.onQuery("FOR UPDATE", 20);

    repo.insertRun(
        new TestcaseGenerationRun(run, P, "gen", "val", 42L, "model", "v1", ACTOR, 7, "[]"));
    repo.updateCap(15, ACTOR);

    var insert = db.last("INSERT INTO problem.testcase_generation_runs");
    assertThat(insert.param("id")).isEqualTo(run);
    assertThat(insert.param("seed")).isEqualTo(42L);
    assertThat(insert.param("added")).isEqualTo(7);
    assertThat(insert.param("dropped")).isEqualTo("[]");
    assertThat(repo.lockCap()).isEqualTo(20);
    assertThat(db.last("SET max_ai_generations_per_problem").param("max")).isEqualTo(15);
  }

  // ---- per-problem stats ----

  @Test
  void markConsumedIsIdempotentPerEvent() {
    var repo = new JdbcProblemStatsRepository(db.provider());
    UUID event = UUID.randomUUID();
    db.onUpdate("INSERT INTO problem.consumed_events", 1);
    assertThat(repo.markConsumed(event)).isTrue();

    var duplicate = new FakeJdbc().onUpdate("INSERT INTO problem.consumed_events", 0);
    assertThat(new JdbcProblemStatsRepository(duplicate.provider()).markConsumed(event)).isFalse();
  }

  @Test
  void addSubmissionBindsAcceptedAsOneAndRejectedAsZero() {
    var repo = new JdbcProblemStatsRepository(db.provider());
    db.onUpdate("INSERT INTO problem.problem_stats", 1);

    assertThat(repo.addSubmission(P, true)).isTrue();
    assertThat(db.last("INSERT INTO problem.problem_stats").param("acc")).isEqualTo(1);
    repo.addSubmission(P, false);
    assertThat(db.last("INSERT INTO problem.problem_stats").param("acc")).isEqualTo(0);
  }

  @Test
  void addSubmissionForUnknownProblemReportsNoWrite() {
    assertThat(new JdbcProblemStatsRepository(db.provider()).addSubmission(P, true)).isFalse();
  }

  @Test
  void findStatsMapsCountsAndNullTimestampForProblemWithoutStats() {
    var repo = new JdbcProblemStatsRepository(db.provider());
    assertThat(repo.find(P)).isEmpty();
    db.onQuery(
        "LEFT JOIN problem.problem_stats s",
        row(
            "author_id",
            ACTOR,
            "status",
            "PUBLISHED",
            "submission_count",
            0,
            "ac_rate",
            null,
            "updated_at",
            null));

    var none = repo.find(P).orElseThrow();
    assertThat(none.submissionCount()).isZero();
    assertThat(none.acRate()).isNull();
    assertThat(none.updatedAt()).isNull();

    var db2 = new FakeJdbc();
    db2.onQuery(
        "LEFT JOIN problem.problem_stats s",
        row(
            "author_id",
            ACTOR,
            "status",
            "PUBLISHED",
            "submission_count",
            10,
            "ac_rate",
            new BigDecimal("42.5"),
            "updated_at",
            Instant.EPOCH));
    var some = new JdbcProblemStatsRepository(db2.provider()).find(P).orElseThrow();
    assertThat(some.acRate()).isEqualByComparingTo("42.5");
    assertThat(some.updatedAt()).isEqualTo(Instant.EPOCH);
  }

  // ---- bank-wide stats ----

  @Test
  void bankStatsLevelsAndProblemsAreMapped() {
    var repo = new JdbcProblemBankStatsRepository(db.provider());
    db.onQuery(
            "FROM problem.problem_levels ORDER BY",
            row("id", L, "code", "EASY", "display_name", "Easy", "sort_order", 1))
        .onQuery(
            "WHERE p.id IN (:ids)",
            row(
                "id",
                P,
                "title",
                "T",
                "level_id",
                L,
                "code",
                "EASY",
                "display_name",
                "Easy",
                "published",
                false))
        .onQuery(
            "true AS published",
            row(
                "id",
                P,
                "title",
                "T",
                "level_id",
                L,
                "code",
                "EASY",
                "display_name",
                "Easy",
                "published",
                true));

    assertThat(repo.levels()).singleElement().extracting("code").isEqualTo("EASY");
    assertThat(repo.problems(Set.of(P))).singleElement().extracting("published").isEqualTo(false);
    assertThat(repo.published()).singleElement().extracting("published").isEqualTo(true);
    assertThat(db.last("WHERE p.id IN (:ids)").param("ids")).isEqualTo(Set.of(P));
  }

  // ---- publish / outbox ----

  @Test
  void setStatusMapsPublishedAtWhichStaysNullForNeverPublished() {
    var repo = new JdbcProblemPublishRepository(db.provider());
    db.onQuery(
        "RETURNING updated_at, published_at",
        row("updated_at", Instant.EPOCH, "published_at", null, "current_testcase_set_version", 2));

    var write = repo.setStatus(P, ACTOR, "UNPUBLISHED");

    assertThat(write.publishedAt()).isNull();
    assertThat(write.currentTestcaseSetVersion()).isEqualTo(2);
    assertThat(db.last("RETURNING updated_at, published_at").param("status"))
        .isEqualTo("UNPUBLISHED");

    var db2 = new FakeJdbc();
    db2.onQuery(
        "RETURNING updated_at, published_at",
        row(
            "updated_at",
            Instant.EPOCH,
            "published_at",
            Instant.parse("2026-01-01T00:00:00Z"),
            "current_testcase_set_version",
            1));
    assertThat(
            new JdbcProblemPublishRepository(db2.provider())
                .setStatus(P, ACTOR, "PUBLISHED")
                .publishedAt())
        .isEqualTo(Instant.parse("2026-01-01T00:00:00Z"));
  }

  @Test
  void publishAppendEventBindsTypeAndRoutingKey() {
    new JdbcProblemPublishRepository(db.provider())
        .appendEvent(P, "ProblemPublished", "problem.published", "{}");

    var call = db.last("INSERT INTO problem.outbox_events");
    assertThat(call.param("type")).isEqualTo("ProblemPublished");
    assertThat(call.param("key")).isEqualTo("problem.published");
  }

  @Test
  void outboxLockPendingBindsBackoffConstantsAndMapsRows() {
    var repo = new JdbcProblemOutboxRepository(db.provider());
    UUID id = UUID.randomUUID();
    db.onQuery(
        "FOR UPDATE SKIP LOCKED",
        row(
            "id", id,
            "event_type", "E",
            "routing_key", "k",
            "envelope", "{}",
            "publish_attempts", 3));

    var events = repo.lockPending(25);

    assertThat(events)
        .singleElement()
        .satisfies(
            e -> {
              assertThat(e.id()).isEqualTo(id);
              assertThat(e.attempts()).isEqualTo(3);
            });
    var call = db.last("FOR UPDATE SKIP LOCKED");
    assertThat(call.param("limit")).isEqualTo(25);
    assertThat(call.param("baseSeconds")).isEqualTo(JdbcProblemOutboxRepository.RETRY_BASE_SECONDS);
    assertThat(call.param("capSeconds")).isEqualTo(JdbcProblemOutboxRepository.RETRY_CAP_SECONDS);
  }

  @Test
  void outboxAttemptedAndRetentionDeleteBindArguments() {
    var repo = new JdbcProblemOutboxRepository(db.provider());
    UUID id = UUID.randomUUID();
    db.onUpdate("DELETE FROM problem.outbox_events", 4);

    repo.attempted(id, false);

    assertThat(db.last("SET publish_attempts").param("confirmed")).isEqualTo(false);
    assertThat(repo.deletePublishedOlderThanDays(30, 100)).isEqualTo(4);
    assertThat(db.last("DELETE FROM problem.outbox_events").param("days")).isEqualTo(30);
  }

  // ---- submission metadata ----

  @Test
  void submissionMetadataSummariesSearchAndSamples() {
    var repo = new JdbcSubmissionMetadataRepository(db.provider());
    UUID tc = UUID.randomUUID();
    db.onQuery(
            "AS withdrawn",
            row(
                "id", P,
                "code", "P1",
                "title", "T",
                "display_name", "Easy",
                "withdrawn", true))
        .onQuery("strpos(lower(code)", P)
        .onQuery("visibility='SAMPLE'", tc, tc);

    var summaries = repo.summaries(Set.of(P));

    assertThat(summaries)
        .singleElement()
        .satisfies(
            s -> {
              assertThat(s.withdrawn()).isTrue();
              assertThat(s.slug()).isEqualTo("P1");
            });
    assertThat(repo.search("two", 5)).containsExactly(P);
    assertThat(db.last("strpos(lower(code)").param("limit")).isEqualTo(5);
    assertThat(repo.samples(P, Set.of(tc))).containsExactly(tc);
    assertThat(List.copyOf(repo.samples(P, Set.of()))).hasSize(1);
  }
}
