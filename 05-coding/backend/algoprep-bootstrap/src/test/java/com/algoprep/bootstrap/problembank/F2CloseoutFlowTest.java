package com.algoprep.bootstrap.problembank;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.algoprep.identity.application.ports.in.DeleteClassUseCase;
import com.algoprep.identity.application.ports.in.RelayIdentityOutboxUseCase;
import com.algoprep.identity.application.ports.out.AccessTokenPort;
import com.algoprep.identity.domain.model.BaseCategory;
import com.algoprep.judge.application.ports.out.ProblemGradingSpecPort;
import com.algoprep.judge.application.ports.out.ProblemTopicReadPort;
import com.algoprep.problembank.application.ports.in.ApplyClassDeletedUseCase;
import com.algoprep.problembank.application.ports.out.ProblemListVersionPort;
import com.algoprep.problembank.domain.ports.out.StudentCatalogReadRepository;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;
import tools.jackson.databind.json.JsonMapper;

/** New F2 closeout paths against real DB, Redis and Rabbit, isolated from the demo stack. */
@SpringBootTest(
    properties = {
      "spring.autoconfigure.exclude=",
      "identity.outbox.enabled=false",
      "problem.outbox.enabled=false"
    })
@ActiveProfiles("test")
@Testcontainers
@ExtendWith(OutputCaptureExtension.class)
class F2CloseoutFlowTest {
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

  @Container
  static final GenericContainer<?> RABBIT =
      new GenericContainer<>("rabbitmq:4.2.9-management-alpine")
          .withEnv("RABBITMQ_DEFAULT_USER", "test-relay-user")
          .withEnv("RABBITMQ_DEFAULT_PASS", "test-relay-password")
          .withExposedPorts(5672)
          .waitingFor(Wait.forLogMessage(".*Server startup complete.*", 1));

  @DynamicPropertySource
  static void infrastructure(DynamicPropertyRegistry properties) {
    properties.add("spring.data.redis.host", REDIS::getHost);
    properties.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    properties.add("spring.rabbitmq.host", RABBIT::getHost);
    properties.add("spring.rabbitmq.port", () -> RABBIT.getMappedPort(5672));
    properties.add("spring.rabbitmq.username", () -> "test-relay-user");
    properties.add("spring.rabbitmq.password", () -> "test-relay-password");
  }

  @Autowired JdbcTemplate jdbc;
  @Autowired WebApplicationContext context;
  @Autowired AccessTokenPort tokens;
  @Autowired ProblemGradingSpecPort grading;
  @Autowired ProblemTopicReadPort topics;
  @Autowired com.algoprep.problembank.domain.ports.out.ProblemOutboxRepository problemOutbox;
  @Autowired DeleteClassUseCase deleteClass;
  @Autowired RelayIdentityOutboxUseCase relay;
  @Autowired ApplyClassDeletedUseCase cleanup;
  @Autowired PlatformTransactionManager transactions;
  @Autowired ProblemListVersionPort versions;
  @Autowired StudentCatalogReadRepository catalog;
  @Autowired StringRedisTemplate redis;
  private UUID instructor;
  private UUID classId;
  private UUID problem;

  @BeforeEach
  void fixture() {
    jdbc.update("DELETE FROM identity.outbox_events");
    jdbc.update("DELETE FROM problem.outbox_events");
    instructor = UUID.randomUUID();
    classId = UUID.randomUUID();
    problem = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO identity.users(id,email,display_name,role_id,status) SELECT ?,?,'Closeout"
            + " teacher',id,'ACTIVE' FROM identity.roles WHERE code='INSTRUCTOR'",
        instructor,
        instructor + "@example.test");
    jdbc.update(
        "INSERT INTO identity.classes(id,instructor_id,name) VALUES (?,?,'Closeout class')",
        classId,
        instructor);
    jdbc.update(
        "INSERT INTO"
            + " problem.problems(id,code,title,statement_md,author_id,level_id,status,time_limit_ms,memory_limit_mb,max_output_size_kb)"
            + " SELECT ?,?,'Closeout problem','statement',?,id,'PUBLISHED',1000,256,64 FROM"
            + " problem.problem_levels ORDER BY sort_order LIMIT 1",
        problem,
        "CLOSE-" + problem.toString().substring(0, 8),
        instructor);
    jdbc.update(
        "INSERT INTO problem.class_assignments(id,problem_id,class_id,assigned_by) VALUES"
            + " (?,?,?,?)",
        UUID.randomUUID(),
        problem,
        classId,
        instructor);
  }

  @Test
  void deletionRollsBackWithItsOutboxAndCommittedEventSoftRemovesAssignments() {
    new TransactionTemplate(transactions)
        .executeWithoutResult(
            status -> {
              deleteClass.delete(instructor, classId);
              status.setRollbackOnly();
            });
    assertThat(count("identity.classes WHERE id='" + classId + "'")).isEqualTo(1);
    assertThat(count("identity.outbox_events")).isZero();
    deleteClass.delete(instructor, classId);
    assertThat(count("identity.classes WHERE id='" + classId + "'")).isZero();
    assertThat(
            count(
                "identity.outbox_events WHERE event_type='ClassDeleted' AND published_at IS NULL"))
        .isEqualTo(1);
    assertThat(relay.relay()).isEqualTo(1);
    assertThat(count("identity.outbox_events WHERE published_at IS NOT NULL")).isEqualTo(1);
    long deadline = System.nanoTime() + Duration.ofSeconds(15).toNanos();
    while (count(
            "problem.class_assignments WHERE class_id='" + classId + "' AND removed_at IS NULL")
        != 0) {
      if (System.nanoTime() > deadline) {
        throw new AssertionError("ClassDeleted was not consumed");
      }
      try {
        Thread.sleep(100);
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
        throw new AssertionError(ex);
      }
    }
    assertThat(cleanup.apply(classId)).isZero();
    assertThat(count("problem.class_assignments WHERE class_id='" + classId + "'")).isEqualTo(1);
    assertThat(count("problem.problems WHERE id='" + problem + "'")).isEqualTo(1);
  }

  @Test
  void cacheEpochFollowsCommitAndRollbackAndSummaryUsesRealCache() {
    long before = epoch();
    new TransactionTemplate(transactions)
        .executeWithoutResult(
            status -> {
              versions.incrementAfterCommit();
              status.setRollbackOnly();
            });
    assertThat(epoch()).isEqualTo(before);
    new TransactionTemplate(transactions)
        .executeWithoutResult(status -> versions.incrementAfterCommit());
    assertThat(epoch()).isEqualTo(before + 1);
    var summary = catalog.summary();
    String key = "problem:summary:v" + epoch();
    assertThat(redis.hasKey(key)).isTrue();
    assertThat(redis.getExpire(key)).isBetween(1L, 60L);
    assertThat(catalog.summary()).isEqualTo(summary);
    redis.opsForValue().set(key, "bad-json");
    assertThat(catalog.summary()).isEqualTo(summary);
  }

  @Test
  void failedFullBatchDoesNotStarveANewerAssignmentEvent() {
    for (int i = 0; i < 100; i++) {
      jdbc.update(
          "INSERT INTO"
              + " problem.outbox_events(id,aggregate_type,aggregate_id,event_type,routing_key,payload,created_at)"
              + " VALUES (?,'PROBLEM',?,'ProblemPublished','problem.published','{}',now()-interval"
              + " '1 hour')",
          UUID.randomUUID(),
          problem);
    }
    UUID assignment = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO"
            + " problem.outbox_events(id,aggregate_type,aggregate_id,event_type,routing_key,payload)"
            + " VALUES (?,'PROBLEM',?,'ProblemAssignedToClass','problem.assignment.assigned','{}')",
        assignment,
        problem);
    new TransactionTemplate(transactions)
        .executeWithoutResult(
            status -> {
              var batch = problemOutbox.lockPending(100);
              assertThat(batch).hasSize(100);
              assertThat(batch).noneMatch(event -> event.id().equals(assignment));
              batch.forEach(event -> problemOutbox.attempted(event.id(), false));
            });
    new TransactionTemplate(transactions)
        .executeWithoutResult(
            status ->
                assertThat(problemOutbox.lockPending(100).getFirst().id()).isEqualTo(assignment));
  }

  @Test
  void learnerDetailKeepsNullRateAndSampleIdsMatchTheF4Bridge(CapturedOutput output)
      throws Exception {
    UUID sample = seedPrivateAndSampleCases();
    String token = studentToken();
    assertLearnerDetailPrivacy(token);
    var mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    var response =
        mvc.perform(
                get("/api/v1/problem-bank/problems/" + problem + "/sample-testcases")
                    .header("Authorization", "Bearer " + token))
            .andReturn()
            .getResponse();
    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(
            JsonMapper.builder()
                .build()
                .readTree(response.getContentAsString())
                .path("items")
                .get(0)
                .path("testcaseId")
                .asString())
        .isEqualTo(sample.toString());
    assertThat(grading.loadSampleTestcases(problem))
        .extracting(item -> item.testcaseId())
        .containsExactly(sample);
    assertThat(response.getContentAsString()).doesNotContain("secret-hidden");
    assertThat(output.getOut()).doesNotContain("secret-hidden-input", "secret-hidden-output");
  }

  private UUID seedPrivateAndSampleCases() {
    UUID sample = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO"
            + " problem.testcases(id,problem_id,visibility,input_storage,input_inline,expected_output_storage,expected_output_inline,origin,approved,position)"
            + " VALUES (?,?,'SAMPLE','INLINE','safe-input','INLINE','safe-output','MANUAL',true,1)",
        sample,
        problem);
    jdbc.update(
        "INSERT INTO"
            + " problem.testcases(id,problem_id,visibility,input_storage,input_inline,expected_output_storage,expected_output_inline,origin,approved,position)"
            + " VALUES"
            + " (?,?,'HIDDEN','INLINE','secret-hidden-input','INLINE','secret-hidden-output','MANUAL',true,2)",
        UUID.randomUUID(),
        problem);
    return sample;
  }

  private String studentToken() {
    UUID student = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO identity.users(id,email,display_name,role_id,status) SELECT ?,?,'Closeout"
            + " learner',id,'ACTIVE' FROM identity.roles WHERE code='STUDENT'",
        student,
        student + "@example.test");
    return tokens.issue(student, BaseCategory.STUDENT, "STUDENT", UUID.randomUUID()).value();
  }

  private void assertLearnerDetailPrivacy(String token) throws Exception {
    var mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    var json = JsonMapper.builder().build();
    for (String suffix : List.of("workspace", "overview")) {
      var response =
          mvc.perform(
                  get("/api/v1/problem-bank/problems/" + problem + "/" + suffix)
                      .header("Authorization", "Bearer " + token))
              .andReturn()
              .getResponse();
      assertThat(response.getStatus()).isEqualTo(200);
      assertThat(json.readTree(response.getContentAsString()).path("acRate").isNull()).isTrue();
      assertThat(response.getContentAsString())
          .doesNotContain(
              "secret-hidden", "referenceSolution", "inputObjectKey", "expectedOutputObjectKey");
    }
  }

  @Test
  void topicPortKeepsRetainedWithdrawnLinksAndReturnsEmptyForUnknownTopics() {
    UUID topic = UUID.randomUUID();
    UUID unknown = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO problem.topics(id,code,name,sort_order) VALUES (?,? ,?,999)",
        topic,
        "C" + topic.toString().substring(0, 8),
        "Closeout-" + topic);
    jdbc.update(
        "INSERT INTO problem.problem_topics(problem_id,topic_id) VALUES (?,?)", problem, topic);
    jdbc.update("UPDATE problem.problems SET status='UNPUBLISHED' WHERE id=?", problem);
    var result = topics.getProblemIdsByTopics(Set.of(topic, unknown));
    assertThat(result.get(topic)).containsExactly(problem);
    assertThat(result.get(unknown)).isEmpty();
    assertThat(topics.getProblemIdsByTopics(Set.of())).isEmpty();
  }

  @Test
  void cleanupIsSafeAcrossTwoProcessesAndRollsBackAMidBatchFailure() throws Exception {
    for (int i = 0; i < 12; i++) {
      seedPublishedOutbox(UUID.randomUUID());
    }
    try (var pool = Executors.newFixedThreadPool(2)) {
      var a = pool.submit(() -> problemOutbox.deletePublishedOlderThanDays(90, 100));
      var b = pool.submit(() -> problemOutbox.deletePublishedOlderThanDays(90, 100));
      assertThat(a.get(10, TimeUnit.SECONDS) + b.get(10, TimeUnit.SECONDS)).isEqualTo(12);
    }
    UUID poisoned = UUID.randomUUID();
    seedPublishedOutbox(UUID.randomUUID());
    seedPublishedOutbox(poisoned);
    seedPublishedOutbox(UUID.randomUUID());
    jdbc.execute(
        "CREATE FUNCTION public.reject_closeout_delete() RETURNS trigger LANGUAGE plpgsql AS $$"
            + " BEGIN IF OLD.id='"
            + poisoned
            + "'::uuid THEN RAISE EXCEPTION 'simulated cleanup failure'; END IF; RETURN OLD; END"
            + " $$");
    jdbc.execute(
        "CREATE TRIGGER closeout_delete_fault BEFORE DELETE ON problem.outbox_events FOR EACH ROW"
            + " EXECUTE FUNCTION public.reject_closeout_delete()");
    try {
      assertThatThrownBy(() -> problemOutbox.deletePublishedOlderThanDays(90, 100))
          .isInstanceOf(org.springframework.dao.DataAccessException.class);
      assertThat(count("problem.outbox_events WHERE published_at IS NOT NULL")).isEqualTo(3);
    } finally {
      jdbc.execute("DROP TRIGGER closeout_delete_fault ON problem.outbox_events");
      jdbc.execute("DROP FUNCTION public.reject_closeout_delete()");
    }
  }

  @Test
  void authoringDefaultsRequiresAuthorPermissionAndUsesTheLiveSettings() throws Exception {
    var mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    String path = "/api/v1/problem-bank/authoring/defaults";
    assertThat(mvc.perform(get(path)).andReturn().getResponse().getStatus()).isEqualTo(401);
    String teacherToken =
        tokens.issue(instructor, BaseCategory.INSTRUCTOR, "INSTRUCTOR", UUID.randomUUID()).value();
    var response =
        mvc.perform(get(path).header("Authorization", "Bearer " + teacherToken))
            .andReturn()
            .getResponse();
    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(
            JsonMapper.builder()
                .build()
                .readTree(response.getContentAsString())
                .path("timeLimitMs")
                .asInt())
        .isPositive();
    UUID student = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO identity.users(id,email,display_name,role_id,status) SELECT ?,?,'Denied"
            + " reader',id,'ACTIVE' FROM identity.roles WHERE code='STUDENT'",
        student,
        student + "@example.test");
    String token =
        tokens.issue(student, BaseCategory.STUDENT, "STUDENT", UUID.randomUUID()).value();
    response =
        mvc.perform(get(path).header("Authorization", "Bearer " + token)).andReturn().getResponse();
    assertThat(response.getStatus()).isEqualTo(403);
    assertThat(
            JsonMapper.builder()
                .build()
                .readTree(response.getContentAsString())
                .path("errorCode")
                .asString())
        .isEqualTo("PRB-002");
  }

  private void seedPublishedOutbox(UUID id) {
    jdbc.update(
        "INSERT INTO"
            + " problem.outbox_events(id,aggregate_type,aggregate_id,event_type,routing_key,payload,published_at)"
            + " VALUES (?,'PROBLEM',?,'ProblemPublished','problem.published','{}',now()-interval"
            + " '100 days')",
        id,
        problem);
  }

  @Test
  void authoringDefaultsAcceptsCreateAndReadIndependently() throws Exception {
    var mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    for (String action : List.of("CREATE", "READ")) {
      String token = authorWithOnly(action);
      var response =
          mvc.perform(
                  get("/api/v1/problem-bank/authoring/defaults")
                      .header("Authorization", "Bearer " + token))
              .andReturn()
              .getResponse();
      assertThat(response.getStatus()).as("only PROBLEM_AUTHORING:%s", action).isEqualTo(200);
      assertThat(JsonMapper.builder().build().readTree(response.getContentAsString()).properties())
          .extracting(entry -> entry.getKey())
          .containsExactlyInAnyOrder(
              "timeLimitMs", "memoryLimitMb", "maxOutputSizeKb", "maxAiGenerationsPerProblem");
    }
  }

  private String authorWithOnly(String action) {
    UUID role = UUID.randomUUID();
    UUID user = UUID.randomUUID();
    String code = "CLOSE-" + role.toString().substring(0, 8);
    jdbc.update(
        "INSERT INTO identity.roles(id,code,name,base_category) VALUES (?,?,?,'INSTRUCTOR')",
        role,
        code,
        code);
    jdbc.update(
        "INSERT INTO identity.permissions(id,role_id,function_id,action_id,granted) SELECT"
            + " ?,?,f.id,a.id,true FROM identity.functions f CROSS JOIN identity.actions a WHERE"
            + " f.code='PROBLEM_AUTHORING' AND a.code=?",
        UUID.randomUUID(),
        role,
        action);
    jdbc.update(
        "INSERT INTO identity.users(id,email,display_name,role_id,status) VALUES (?,?,'Single"
            + " authority',?,'ACTIVE')",
        user,
        user + "@example.test",
        role);
    return tokens.issue(user, BaseCategory.INSTRUCTOR, code, UUID.randomUUID()).value();
  }

  private long epoch() {
    return jdbc.queryForObject(
        "SELECT version FROM problem.catalog_cache_epoch WHERE id=1", Long.class);
  }

  private int count(String from) {
    return jdbc.queryForObject("SELECT count(*) FROM " + from, Integer.class);
  }
}
