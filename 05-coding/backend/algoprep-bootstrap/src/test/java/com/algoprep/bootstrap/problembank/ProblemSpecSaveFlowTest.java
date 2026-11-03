package com.algoprep.bootstrap.problembank;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.algoprep.identity.application.ports.out.AccessTokenPort;
import com.algoprep.identity.domain.model.BaseCategory;
import com.algoprep.problembank.application.dto.SaveProblemSpecCommand;
import com.algoprep.problembank.application.ports.in.SaveProblemSpecUseCase;
import java.time.Instant;
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
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Full HTTP to PostgreSQL transaction with real JWT permissions, Redis and the durable Outbox. */
@SpringBootTest(
    properties =
        "spring.autoconfigure.exclude=org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration")
@ActiveProfiles("test")
@Testcontainers
class ProblemSpecSaveFlowTest {
  private static final JsonMapper JSON = JsonMapper.builder().build();
  private static final String VALID =
      "{\"functionName\":\"solve\",\"returnType\":{\"kind\":\"INT\"},\"parameters\":[]}";

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

  @Autowired WebApplicationContext context;
  @Autowired JdbcTemplate jdbc;
  @Autowired StringRedisTemplate redis;
  @Autowired AccessTokenPort tokens;
  @Autowired SaveProblemSpecUseCase specs;
  @Autowired PlatformTransactionManager transactions;
  private MockMvc mvc;
  private UUID author;
  private UUID problem;
  private String teacher;
  private String admin;

  @BeforeEach
  void setup() {
    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    author = UUID.randomUUID();
    problem = UUID.randomUUID();
    teacher = token(author, "INSTRUCTOR", BaseCategory.INSTRUCTOR);
    admin = token(UUID.randomUUID(), "ADMIN", BaseCategory.ADMIN);
    jdbc.update(
        "INSERT INTO problem.problems(id,code,title,author_id,level_id,time_limit_ms,memory_limit_mb,max_output_size_kb,current_testcase_set_version) VALUES (?,?,'Spec fixture',?,'20000000-0000-4000-8000-000000000001',1000,256,1024,5)",
        problem,
        problem.toString(),
        author);
    redis.opsForValue().set(key(), "cached-before-save");
  }

  private String token(UUID id, String role, BaseCategory category) {
    jdbc.update(
        "INSERT INTO identity.users(id,email,display_name,role_id,status) SELECT ?,?,'Spec actor',id,'ACTIVE' FROM identity.roles WHERE code=?",
        id,
        id + "@test.local",
        role);
    return tokens.issue(id, category, role, UUID.randomUUID()).value();
  }

  private String key() {
    return "problem:spec:" + problem;
  }

  private Instant timestamp() {
    return jdbc.queryForObject(
            "SELECT updated_at FROM problem.problems WHERE id=?", java.sql.Timestamp.class, problem)
        .toInstant();
  }

  private int count(String table) {
    return jdbc.queryForObject(
        "SELECT count(*) FROM problem."
            + table
            + " WHERE "
            + (table.equals("outbox_events") ? "aggregate_id" : "problem_id")
            + "=?",
        Integer.class,
        problem);
  }

  private MockHttpServletResponse save(String bearer, String body, Instant expected)
      throws Exception {
    String input =
        body.substring(0, body.length() - 1) + ",\"expectedUpdatedAt\":\"" + expected + "\"}";
    var request =
        put("/api/v1/problem-bank/authoring/problems/" + problem + "/spec")
            .contentType(MediaType.APPLICATION_JSON)
            .content(input);
    if (bearer != null) {
      request.header("Authorization", "Bearer " + bearer);
    }
    return mvc.perform(request).andReturn().getResponse();
  }

  private JsonNode response(MockHttpServletResponse response) throws Exception {
    return JSON.readTree(response.getContentAsString());
  }

  private void error(MockHttpServletResponse response, int status, String code) throws Exception {
    assertThat(response.getStatus()).isEqualTo(status);
    assertThat(response(response).path("errorCode").asString()).isEqualTo(code);
  }

  @Test
  void savesVersionsAndNoOpWithAtomicEventAndCacheInvalidation() throws Exception {
    Instant before = timestamp();
    var first = save(teacher, VALID, before);
    assertThat(first.getStatus()).isEqualTo(200);
    assertThat(response(first).path("spec").path("specVersion").asInt()).isEqualTo(1);
    assertThat(timestamp()).isAfter(before);
    assertThat(count("outbox_events")).isEqualTo(1);
    assertThat(redis.hasKey(key())).isFalse();
    Instant second = timestamp();
    redis.opsForValue().set(key(), "new-cache");
    assertThat(save(teacher, VALID, second).getStatus()).isEqualTo(200);
    assertThat(timestamp()).isEqualTo(second);
    assertThat(redis.opsForValue().get(key())).isEqualTo("new-cache");
    var changed =
        save(
            teacher,
            VALID.replace("{\"functionName", "{\"stdinFormatMd\":\"Read n\",\"functionName"),
            second);
    assertThat(response(changed).path("spec").path("specVersion").asInt()).isEqualTo(2);
    assertThat(count("outbox_events")).isEqualTo(2);
    assertThat(
            jdbc.queryForObject(
                "SELECT current_testcase_set_version FROM problem.problems WHERE id=?",
                Integer.class,
                problem))
        .isEqualTo(5);
    assertThat(
            jdbc.queryForObject(
                "SELECT updated_by FROM problem.problems WHERE id=?", UUID.class, problem))
        .isEqualTo(author);
  }

  @Test
  void staleTimestampIs409AndConcurrentWritesHaveExactlyOneWinner() throws Exception {
    Instant initial = timestamp();
    try (var pool = Executors.newFixedThreadPool(2)) {
      Callable<Integer> first = () -> save(teacher, VALID, initial).getStatus();
      Callable<Integer> second =
          () -> save(teacher, VALID.replace("solve", "answer"), initial).getStatus();
      var results = pool.invokeAll(List.of(first, second));
      assertThat(List.of(results.get(0).get(), results.get(1).get()))
          .containsExactlyInAnyOrder(200, 409);
    }
    var stale = save(teacher, VALID, initial);
    error(stale, 409, "PRB-020");
    assertThat(Instant.parse(response(stale).path("details").path("currentUpdatedAt").asString()))
        .isEqualTo(timestamp());
    assertThat(count("outbox_events")).isEqualTo(1);
  }

  @Test
  void permissionOwnershipAndDeletedPrivacyHoldWithRealJwtConverter() throws Exception {
    Instant initial = timestamp();
    assertThat(save(null, VALID, initial).getStatus()).isEqualTo(401);
    var student = token(UUID.randomUUID(), "STUDENT", BaseCategory.STUDENT);
    error(save(student, VALID, initial), 403, "PRB-002");
    var other = token(UUID.randomUUID(), "INSTRUCTOR", BaseCategory.INSTRUCTOR);
    error(save(other, VALID, initial), 404, "PRB-010");
    jdbc.update("UPDATE problem.problems SET status='PUBLISHED' WHERE id=?", problem);
    error(save(other, VALID, initial), 403, "PRB-003");
    assertThat(save(admin, VALID, initial).getStatus()).isEqualTo(200);
    jdbc.update(
        "UPDATE problem.problems SET status='UNPUBLISHED',deleted=true WHERE id=?", problem);
    error(save(admin, VALID, timestamp()), 404, "PRB-010");
  }

  @Test
  void revokedCellOnExistingAdminTokenTakesEffectImmediately() throws Exception {
    try {
      jdbc.update(
          "UPDATE identity.permissions p SET granted=false FROM identity.roles r,identity.functions f,identity.actions a WHERE p.role_id=r.id AND p.function_id=f.id AND p.action_id=a.id AND r.code='ADMIN' AND f.code='PROBLEM_AUTHORING' AND a.code='UPDATE'");
      jdbc.update(
          "UPDATE identity.roles SET permission_version=permission_version+1 WHERE code='ADMIN'");
      error(save(admin, VALID, timestamp()), 403, "PRB-002");
      assertThat(count("problem_specs")).isZero();
    } finally {
      jdbc.update(
          "UPDATE identity.permissions p SET granted=true FROM identity.roles r,identity.functions f,identity.actions a WHERE p.role_id=r.id AND p.function_id=f.id AND p.action_id=a.id AND r.code='ADMIN' AND f.code='PROBLEM_AUTHORING' AND a.code='UPDATE'");
      jdbc.update(
          "UPDATE identity.roles SET permission_version=permission_version+1 WHERE code='ADMIN'");
    }
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "{\"matchingStrategy\":\"EPSILON\"}",
        "{\"functionName\":\"solve\"}",
        "{\"epsilonValue\":999999.9999999999991}",
        "{\"epsilonValue\":0.0000000000001}"
      })
  void hardFailuresLeaveNoRowEventTimestampOrCacheChanges(String body) throws Exception {
    Instant initial = timestamp();
    error(save(teacher, body, initial), 422, "PRB-036");
    assertThat(count("problem_specs")).isZero();
    assertThat(count("outbox_events")).isZero();
    assertThat(timestamp()).isEqualTo(initial);
    assertThat(redis.opsForValue().get(key())).isEqualTo("cached-before-save");
  }

  @Test
  void preservesUnknownKindRawAndNormalizesOverrideKeys() throws Exception {
    var saved =
        save(
            teacher,
            VALID
                .replace("INT", "GRAPH")
                .replace(
                    "{\"functionName",
                    "{\"nameOverrides\":{\"python\":\"solve_it\"},\"functionName"),
            timestamp());
    assertThat(saved.getStatus()).isEqualTo(200);
    assertThat(response(saved).path("functionWrapperSupported").asBoolean()).isFalse();
    assertThat(response(saved).path("warnings").get(0).path("reasonCode").asString())
        .isEqualTo("HRN-V01");
    assertThat(
            jdbc.queryForObject(
                "SELECT return_type->>'kind' FROM problem.problem_specs WHERE problem_id=?",
                String.class,
                problem))
        .isEqualTo("GRAPH");
    assertThat(
            jdbc.queryForObject(
                "SELECT name_overrides->>'PYTHON' FROM problem.problem_specs WHERE problem_id=?",
                String.class,
                problem))
        .isEqualTo("solve_it");
    assertThat(
            jdbc.queryForObject(
                "SELECT function_wrapper_supported FROM problem.problems WHERE id=?",
                Boolean.class,
                problem))
        .isFalse();
  }

  @Test
  void storesExactDecimalAndTreatsScaleOnlyChangeAsNoOp() throws Exception {
    String body =
        VALID.replace(
            "{\"functionName",
            "{\"matchingStrategy\":\"EPSILON\",\"epsilonValue\":999999.999999999999,\"functionName");
    assertThat(save(teacher, body, timestamp()).getStatus()).isEqualTo(200);
    assertThat(
            jdbc.queryForObject(
                "SELECT epsilon_value FROM problem.problem_specs WHERE problem_id=?",
                java.math.BigDecimal.class,
                problem))
        .isEqualByComparingTo("999999.999999999999");
    String one = body.replace("999999.999999999999", "1.0");
    assertThat(save(teacher, one, timestamp()).getStatus()).isEqualTo(200);
    int events = count("outbox_events");
    Instant before = timestamp();
    assertThat(save(teacher, one.replace("1.0", "1.0000000000000"), before).getStatus())
        .isEqualTo(200);
    assertThat(count("outbox_events")).isEqualTo(events);
    assertThat(timestamp()).isEqualTo(before);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void outboxFailureRollsBackEverythingAndRetrySucceeds(boolean existing) throws Exception {
    if (existing) {
      assertThat(save(teacher, VALID, timestamp()).getStatus()).isEqualTo(200);
    }
    Instant initial = timestamp();
    int originalCount = existing ? 1 : 0;
    String changed = VALID.replace("solve", "answer");
    redis.opsForValue().set(key(), "cached-before-failure");
    jdbc.execute(
        "CREATE FUNCTION problem.reject_spec_event() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'injected failure'; END $$");
    jdbc.execute(
        "CREATE TRIGGER reject_spec_event BEFORE INSERT ON problem.outbox_events FOR EACH ROW EXECUTE FUNCTION problem.reject_spec_event()");
    try {
      var failed = save(teacher, changed, initial);
      error(failed, 503, "PRB-056");
      assertThat(response(failed).path("detail").asString())
          .isEqualTo("Problem event persistence unavailable");
      assertThat(failed.getContentAsString())
          .doesNotContain("injected failure", "INSERT", "reject_spec_event", "SQL", "stackTrace");
      assertThat(count("problem_specs")).isEqualTo(originalCount);
      assertThat(count("outbox_events")).isEqualTo(originalCount);
      if (existing) {
        assertThat(
                jdbc.queryForObject(
                    "SELECT function_name FROM problem.problem_specs WHERE problem_id=?",
                    String.class,
                    problem))
            .isEqualTo("solve");
        assertThat(
                jdbc.queryForObject(
                    "SELECT spec_version FROM problem.problem_specs WHERE problem_id=?",
                    Integer.class,
                    problem))
            .isEqualTo(1);
      }
      assertThat(timestamp()).isEqualTo(initial);
      assertThat(redis.hasKey(key())).isTrue();
    } finally {
      jdbc.execute("DROP TRIGGER reject_spec_event ON problem.outbox_events");
      jdbc.execute("DROP FUNCTION problem.reject_spec_event()");
    }
    assertThat(save(teacher, changed, initial).getStatus()).isEqualTo(200);
    assertThat(count("outbox_events")).isEqualTo(originalCount + 1);
  }

  @Test
  void malformedHttpShapesAre400AndNeverReachPersistence() throws Exception {
    String path = "/api/v1/problem-bank/authoring/problems/" + problem + "/spec";
    for (String body : List.of("{", "null", "[]", "{}", "{\"expectedUpdatedAt\":\"yesterday\"}")) {
      var request =
          put(path)
              .header("Authorization", "Bearer " + teacher)
              .contentType(MediaType.APPLICATION_JSON)
              .content(body);
      error(mvc.perform(request).andReturn().getResponse(), 400, "PRB-001");
    }
    var invalidId =
        put("/api/v1/problem-bank/authoring/problems/broken/spec")
            .header("Authorization", "Bearer " + teacher)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"expectedUpdatedAt\":\"2026-11-02T00:00:00Z\"}");
    error(mvc.perform(invalidId).andReturn().getResponse(), 400, "PRB-001");
    assertThat(count("problem_specs")).isZero();
    assertThat(count("outbox_events")).isZero();
  }

  @Test
  void overrideCollisionAndRawTypeShapeAreHardFailures() throws Exception {
    for (String body :
        List.of(
            VALID.replace(
                "{\"functionName",
                "{\"nameOverrides\":{\"JAVA\":\"good\",\"java\":\"good\"},\"functionName"),
            VALID.replace("\"kind\":\"INT\"", "\"kind\":\"ARRAY\",\"dimensions\":1.5"))) {
      error(save(teacher, body, timestamp()), 422, "PRB-036");
    }
    assertThat(count("problem_specs")).isZero();
    assertThat(count("outbox_events")).isZero();
  }

  @Test
  void callerRollbackKeepsSpecEventAndCacheUnchanged() {
    Instant initial = timestamp();
    var transaction = new TransactionTemplate(transactions);
    assertThatThrownBy(
            () ->
                transaction.executeWithoutResult(
                    status -> {
                      specs.save(
                          new SaveProblemSpecCommand(
                              problem, author, false, initial, JSON.readTree(VALID)));
                      assertThat(redis.hasKey(key())).isTrue();
                      throw new IllegalStateException("Rollback fixture");
                    }))
        .isInstanceOf(IllegalStateException.class);
    assertThat(count("problem_specs")).isZero();
    assertThat(count("outbox_events")).isZero();
    assertThat(timestamp()).isEqualTo(initial);
    assertThat(redis.hasKey(key())).isTrue();
  }
}
