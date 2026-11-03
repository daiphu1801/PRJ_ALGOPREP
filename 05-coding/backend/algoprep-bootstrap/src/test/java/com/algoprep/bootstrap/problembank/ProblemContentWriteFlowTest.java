package com.algoprep.bootstrap.problembank;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.algoprep.common.exception.AlgoPrepException;
import com.algoprep.problembank.application.dto.SaveProblemContentCommand;
import com.algoprep.problembank.application.dto.SaveProblemSpecCommand;
import com.algoprep.problembank.application.ports.in.SaveProblemContentUseCase;
import com.algoprep.problembank.application.ports.in.SaveProblemSpecUseCase;
import com.algoprep.problembank.domain.model.ProblemContentData;
import com.algoprep.problembank.domain.model.ProblemContentSnapshot;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;
import tools.jackson.databind.json.JsonMapper;

/** Internal write core with real PostgreSQL/Redis; no HTTP DTO or external-module stand-ins. */
@SpringBootTest(
    properties =
        "spring.autoconfigure.exclude=org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration")
@ActiveProfiles("test")
@Testcontainers
class ProblemContentWriteFlowTest {
  static final UUID LEVEL = UUID.fromString("20000000-0000-4000-8000-000000000001");
  static final UUID TOPIC = UUID.fromString("20000000-0000-4000-8000-000000000101");

  @Container @ServiceConnection
  static final PostgreSQLContainer POSTGRES =
      new PostgreSQLContainer("postgres:16.15-alpine")
          .withDatabaseName("algoprep")
          .withCopyFileToContainer(
              MountableFile.forHostPath("../../database/init/01-schemas.sql"),
              "/docker-entrypoint-initdb.d/01-schemas.sql");

  @Container
  static final GenericContainer<?> REDIS =
      new GenericContainer<>("redis:7.4-alpine").withExposedPorts(6379);

  @DynamicPropertySource
  static void infrastructure(DynamicPropertyRegistry properties) {
    properties.add("spring.data.redis.host", REDIS::getHost);
    properties.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
  }

  @Autowired SaveProblemContentUseCase content;
  @Autowired SaveProblemSpecUseCase specs;
  @Autowired JdbcTemplate jdbc;
  @Autowired StringRedisTemplate redis;
  @Autowired PlatformTransactionManager transactions;
  private UUID actor;
  private String title;

  @BeforeEach
  void setup() {
    actor = UUID.randomUUID();
    title = "Bài " + UUID.randomUUID();
    redis.delete("problem:list:version");
  }

  private ProblemContentData data(String name) {
    return new ProblemContentData(
        name,
        "# Statement <b>raw</b>",
        null,
        null,
        new ProblemContentData.Limits(1000, 256, 1024, null),
        List.of(TOPIC, TOPIC),
        List.of("Tag " + actor, "tag " + actor),
        List.of(
            new ProblemContentData.Example(null, 50, "1", "2", "raw **explanation**"),
            new ProblemContentData.Example(null, 20, "2", "3", null)),
        new ProblemContentData.Solution("PYTHON", " print(1)\n"));
  }

  private ProblemContentSnapshot create() {
    return content.save(new SaveProblemContentCommand(null, actor, false, true, null, data(title)));
  }

  private SaveProblemContentCommand update(ProblemContentSnapshot old, ProblemContentData next) {
    return new SaveProblemContentCommand(old.id(), actor, false, true, old.updatedAt(), next);
  }

  private ProblemContentData edit(ProblemContentSnapshot old, String name) {
    var data = old.content();
    return new ProblemContentData(
        name,
        "Changed statement",
        "Changed constraints",
        data.levelId(),
        data.limits(),
        List.of(),
        List.of("Edited " + actor),
        data.workedExamples(),
        null);
  }

  private int count(String sql, Object... args) {
    return jdbc.queryForObject(sql, Integer.class, args);
  }

  private void code(Callable<?> action, String expected) {
    assertThatThrownBy(action::call)
        .isInstanceOfSatisfying(
            AlgoPrepException.class, e -> assertThat(e.errorCode()).isEqualTo(expected));
  }

  @Test
  void createsRealDraftWithOrderedExamplesRawSourceNullableQuotaAndNoSyntheticSpec() {
    var created = create();
    assertThat(created.status()).isEqualTo("UNPUBLISHED");
    assertThat(created.authorId()).isEqualTo(actor);
    assertThat(created.content().levelId()).isEqualTo(LEVEL);
    assertThat(created.content().topicIds()).containsExactly(TOPIC);
    assertThat(created.content().tags()).hasSize(1);
    assertThat(created.content().workedExamples())
        .extracting(ProblemContentData.Example::position)
        .containsExactly(1, 2);
    assertThat(created.content().workedExamples()).allMatch(e -> e.id() != null);
    assertThat(created.content().referenceSolution().sourceCode()).isEqualTo(" print(1)\n");
    assertThat(count("SELECT count(*) FROM problem.problem_specs WHERE problem_id=?", created.id()))
        .isZero();
    assertThat(count("SELECT count(*) FROM problem.testcases WHERE problem_id=?", created.id()))
        .isZero();
    assertThat(
            count(
                "SELECT count(*) FROM problem.problems WHERE id=? AND no_full_code AND socratic_only AND max_submissions_per_hour IS NULL AND current_testcase_set_version=1",
                created.id()))
        .isEqualTo(1);
    assertThat(redis.hasKey("problem:list:version")).isFalse();
  }

  @Test
  void duplicateTitlesIncludingDeletedRowsAllocateSuffixAndRenamesKeepCodeAndAuthor() {
    var first = create();
    jdbc.update("UPDATE problem.problems SET deleted=true WHERE id=?", first.id());
    var second = create();
    assertThat(second.code()).isEqualTo(first.code() + "-2");
    var edited = content.save(update(second, edit(second, "Renamed")));
    assertThat(edited.code()).isEqualTo(second.code());
    assertThat(edited.authorId()).isEqualTo(actor);
    assertThat(edited.updatedAt()).isAfter(second.updatedAt());
    assertThat(edited.content().topicIds()).isEmpty();
    assertThat(edited.content().tags()).containsExactly("Edited " + actor);
    assertThat(edited.content().referenceSolution()).isNull();
  }

  @Test
  void contentReplacementPreservesSpecVersionWrapperFlagAndExampleIds() {
    var original = create();
    var savedSpec =
        specs.save(
            new SaveProblemSpecCommand(
                original.id(),
                actor,
                false,
                original.updatedAt(),
                JsonMapper.builder()
                    .build()
                    .readTree(
                        "{\"functionName\":\"solve\",\"returnType\":{\"kind\":\"GRAPH\"},\"parameters\":[]}")));
    var first = original.content().workedExamples().getFirst();
    var examples =
        List.of(
            new ProblemContentData.Example(first.id(), 99, "new input", "new output", null),
            new ProblemContentData.Example(null, 50, "4", "5", null));
    var next =
        new ProblemContentData(
            "New title",
            "New statement",
            "",
            LEVEL,
            original.content().limits(),
            List.of(),
            List.of(),
            examples,
            null);
    var result =
        content.save(
            new SaveProblemContentCommand(
                original.id(), actor, false, true, savedSpec.problemUpdatedAt(), next));
    assertThat(result.content().workedExamples().getFirst().id()).isEqualTo(first.id());
    assertThat(result.content().workedExamples().get(1).id())
        .isNotEqualTo(original.content().workedExamples().get(1).id());
    assertVersionsUnchanged(original.id());
  }

  private void assertVersionsUnchanged(UUID problemId) {
    assertThat(
            count("SELECT spec_version FROM problem.problem_specs WHERE problem_id=?", problemId))
        .isEqualTo(1);
    assertThat(
            count(
                "SELECT current_testcase_set_version FROM problem.problems WHERE id=? AND NOT function_wrapper_supported",
                problemId))
        .isEqualTo(1);
    assertThat(count("SELECT count(*) FROM problem.outbox_events WHERE aggregate_id=?", problemId))
        .isEqualTo(1);
    assertThat(
            count(
                "SELECT count(*) FROM identity.system_audit_logs WHERE target_id=?",
                problemId.toString()))
        .isZero();
  }

  @Test
  void permissionAndOwnershipAreCheckedBeforeDataAndAdminsPreserveOriginalAuthor() {
    code(
        () ->
            content.save(
                new SaveProblemContentCommand(null, actor, true, false, null, data(title))),
        "PRB-002");
    var old = create();
    UUID other = UUID.randomUUID();
    code(
        () ->
            content.save(
                new SaveProblemContentCommand(old.id(), other, false, true, old.updatedAt(), null)),
        "PRB-010");
    jdbc.update("UPDATE problem.problems SET status='PUBLISHED' WHERE id=?", old.id());
    code(
        () ->
            content.save(
                new SaveProblemContentCommand(old.id(), other, false, true, old.updatedAt(), null)),
        "PRB-003");
    var result =
        content.save(
            new SaveProblemContentCommand(
                old.id(), other, true, true, old.updatedAt(), old.content()));
    assertThat(result.authorId()).isEqualTo(actor);
    assertThat(
            jdbc.queryForObject(
                "SELECT updated_by FROM problem.problems WHERE id=?", UUID.class, old.id()))
        .isEqualTo(other);
    jdbc.update(
        "UPDATE problem.problems SET status='UNPUBLISHED',deleted=true WHERE id=?", old.id());
    code(
        () ->
            content.save(
                new SaveProblemContentCommand(
                    old.id(), other, true, true, result.updatedAt(), old.content())),
        "PRB-010");
  }

  @Test
  void publishedExamplesAreProtectedAndSuccessfulWriteInvalidatesCache() {
    var old = create();
    jdbc.update("UPDATE problem.problems SET status='PUBLISHED' WHERE id=?", old.id());
    var data = old.content();
    var one =
        new ProblemContentData(
            title,
            "Statement",
            "",
            LEVEL,
            data.limits(),
            List.of(),
            List.of(),
            List.of(data.workedExamples().getFirst()),
            null);
    code(() -> content.save(update(old, one)), "PRB-053");
    assertThat(count("SELECT count(*) FROM problem.worked_examples WHERE problem_id=?", old.id()))
        .isEqualTo(2);
    assertThat(redis.hasKey("problem:list:version")).isFalse();
    content.save(update(old, edit(old, "Published edit")));
    assertThat(redis.opsForValue().get("problem:list:version")).isEqualTo("1");
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void lateRelationFailureRollsBackParentTagsExamplesAndSolution(boolean existing) {
    var old = existing ? create() : null;
    int parents = count("SELECT count(*) FROM problem.problems");
    String tag = "rollback " + UUID.randomUUID();
    var data = data(title);
    var changed =
        new ProblemContentData(
            "Rollback " + actor,
            data.statementMd(),
            "",
            LEVEL,
            data.limits(),
            List.of(),
            List.of(tag),
            data.workedExamples(),
            data.referenceSolution());
    jdbc.execute(
        "CREATE FUNCTION problem.reject_content() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'fixture relation failure'; END $$");
    jdbc.execute(
        "CREATE TRIGGER reject_content BEFORE INSERT OR UPDATE ON problem.problem_reference_solutions FOR EACH ROW EXECUTE FUNCTION problem.reject_content()");
    try {
      var command =
          existing
              ? update(old, changed)
              : new SaveProblemContentCommand(null, actor, false, true, null, changed);
      assertThatThrownBy(() -> content.save(command))
          .isInstanceOf(org.springframework.dao.DataAccessException.class);
      assertThat(count("SELECT count(*) FROM problem.problems")).isEqualTo(parents);
      assertThat(count("SELECT count(*) FROM problem.tags WHERE name=?", tag)).isZero();
      if (old != null) {
        assertThat(
                jdbc.queryForObject(
                    "SELECT title FROM problem.problems WHERE id=?", String.class, old.id()))
            .isEqualTo(old.content().title());
        assertThat(
                count("SELECT count(*) FROM problem.worked_examples WHERE problem_id=?", old.id()))
            .isEqualTo(2);
      }
    } finally {
      jdbc.execute("DROP TRIGGER reject_content ON problem.problem_reference_solutions");
      jdbc.execute("DROP FUNCTION problem.reject_content()");
    }
    assertThat(redis.hasKey("problem:list:version")).isFalse();
  }

  @Test
  void simultaneousContentWritesHaveOneWinnerAndContentVsSpecShareTheSameLock() throws Exception {
    var old = create();
    var changed = edit(old, "First writer");
    try (var pool = Executors.newFixedThreadPool(2)) {
      var futures =
          pool.invokeAll(
              List.of(
                  (Callable<String>) () -> outcome(() -> content.save(update(old, changed))),
                  () ->
                      outcome(
                          () ->
                              specs.save(
                                  new SaveProblemSpecCommand(
                                      old.id(),
                                      actor,
                                      false,
                                      old.updatedAt(),
                                      JsonMapper.builder().build().readTree("{}"))))));
      assertThat(List.of(futures.get(0).get(), futures.get(1).get()))
          .containsExactlyInAnyOrder("SAVED", "PRB-020");
    }
  }

  @Test
  void competingContentUpdatesKeepExactlyOneFullState() throws Exception {
    var old = create();
    try (var pool = Executors.newFixedThreadPool(2)) {
      var futures =
          pool.invokeAll(
              List.of(
                  (Callable<String>)
                      () -> outcome(() -> content.save(update(old, edit(old, "Writer A")))),
                  () -> outcome(() -> content.save(update(old, edit(old, "Writer B"))))));
      assertThat(List.of(futures.get(0).get(), futures.get(1).get()))
          .containsExactlyInAnyOrder("SAVED", "PRB-020");
    }
    assertThat(
            jdbc.queryForObject(
                "SELECT title FROM problem.problems WHERE id=?", String.class, old.id()))
        .isIn("Writer A", "Writer B");
    assertThat(count("SELECT count(*) FROM problem.problem_topics WHERE problem_id=?", old.id()))
        .isZero();
  }

  @Test
  void absentTopicAndForeignExampleAreRejectedWithoutMovingRows() {
    var old = create();
    var foreign = create();
    var data = old.content();
    var missingTopic =
        new ProblemContentData(
            title,
            data.statementMd(),
            "",
            LEVEL,
            data.limits(),
            List.of(UUID.randomUUID()),
            List.of(),
            data.workedExamples(),
            null);
    code(() -> content.save(update(old, missingTopic)), "PRB-039");
    var wrongExample =
        new ProblemContentData(
            title,
            data.statementMd(),
            "",
            LEVEL,
            data.limits(),
            List.of(),
            List.of(),
            foreign.content().workedExamples(),
            null);
    code(() -> content.save(update(old, wrongExample)), "PRB-001");
    assertThat(count("SELECT count(*) FROM problem.worked_examples WHERE problem_id=?", old.id()))
        .isEqualTo(2);
    assertThat(
            count("SELECT count(*) FROM problem.worked_examples WHERE problem_id=?", foreign.id()))
        .isEqualTo(2);
  }

  @Test
  void simultaneousDraftCreationSharesCaseInsensitiveTagsAndAllocatesUniqueCodes()
      throws Exception {
    try (var pool = Executors.newFixedThreadPool(2)) {
      var futures =
          pool.invokeAll(List.of((Callable<ProblemContentSnapshot>) this::create, this::create));
      assertThat(futures.get(0).get().code()).isNotEqualTo(futures.get(1).get().code());
      assertThat(
              count("SELECT count(*) FROM problem.tags WHERE lower(name)=lower(?)", "tag " + actor))
          .isEqualTo(1);
    }
  }

  @Test
  void callerRollbackAndMissingReferencesDoNotLeavePartialRows() {
    var original = create();
    var invalid =
        new ProblemContentData(
            title,
            "Statement",
            "",
            UUID.randomUUID(),
            original.content().limits(),
            List.of(),
            List.of(),
            List.of(),
            null);
    code(() -> content.save(update(original, invalid)), "PRB-039");
    jdbc.update("UPDATE problem.problems SET status='PUBLISHED' WHERE id=?", original.id());
    var transaction = new TransactionTemplate(transactions);
    assertThatThrownBy(
            () ->
                transaction.executeWithoutResult(
                    status -> {
                      content.save(update(original, edit(original, "Rolled back")));
                      assertThat(redis.hasKey("problem:list:version")).isFalse();
                      throw new IllegalStateException("Caller rollback fixture");
                    }))
        .isInstanceOf(IllegalStateException.class);
    assertThat(redis.hasKey("problem:list:version")).isFalse();
    assertThat(
            jdbc.queryForObject(
                "SELECT title FROM problem.problems WHERE id=?", String.class, original.id()))
        .isEqualTo(original.content().title());
  }

  private String outcome(Callable<?> action) throws Exception {
    try {
      action.call();
      return "SAVED";
    } catch (AlgoPrepException failure) {
      return failure.errorCode();
    }
  }
}
