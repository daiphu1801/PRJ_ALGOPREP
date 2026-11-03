package com.algoprep.bootstrap.problembank;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.algoprep.identity.application.ports.out.AccessTokenPort;
import com.algoprep.identity.domain.model.BaseCategory;
import com.algoprep.problembank.application.dto.CatalogCommand;
import com.algoprep.problembank.application.ports.in.CreateProblemLevelUseCase;
import com.jayway.jsonpath.JsonPath;
import java.util.ArrayList;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

/** Real PostgreSQL/Redis and real JWT authorization; no runtime placeholder or fake audit. */
@SpringBootTest(
    properties =
        "spring.autoconfigure.exclude=org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration")
@ActiveProfiles("test")
@Testcontainers
class CatalogTaxonomyFlowTest {
  private static final String BASE = "/api/v1/problem-bank/";
  private static final UUID LEVEL1 = UUID.fromString("20000000-0000-4000-8000-000000000001");
  private static final UUID LEVEL2 = UUID.fromString("20000000-0000-4000-8000-000000000002");
  private static final UUID TOPIC1 = UUID.fromString("20000000-0000-4000-8000-000000000101");
  private static final UUID TOPIC2 = UUID.fromString("20000000-0000-4000-8000-000000000102");

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
  static void redisProperties(DynamicPropertyRegistry properties) {
    properties.add("spring.data.redis.host", REDIS::getHost);
    properties.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
  }

  @Autowired WebApplicationContext context;
  @Autowired AccessTokenPort tokens;
  @Autowired JdbcTemplate jdbc;
  @Autowired StringRedisTemplate redis;
  @Autowired PlatformTransactionManager transactions;
  @Autowired CreateProblemLevelUseCase createLevel;
  private MockMvc mvc;
  private UUID adminId;
  private String admin;

  @BeforeEach
  void setup() {
    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    adminId = UUID.randomUUID();
    admin = token(adminId, "ADMIN", BaseCategory.ADMIN);
    jdbc.execute("TRUNCATE problem.problem_levels, problem.topics CASCADE");
    jdbc.update(
        "INSERT INTO problem.problem_levels(id,code,display_name,sort_order) VALUES (?, 'EASY','Dễ',1), (?, 'HARD','Khó',2)",
        LEVEL1,
        LEVEL2);
    jdbc.update(
        "INSERT INTO problem.topics(id,code,name,sort_order) VALUES (?, 'MANG','Mảng',1), (?, 'CAY','Cây',2)",
        TOPIC1,
        TOPIC2);
    jdbc.update("DELETE FROM identity.system_audit_logs WHERE action_type LIKE 'PROBLEM_%'");
    redis.delete("problem:list:version");
  }

  private String token(UUID id, String role, BaseCategory category) {
    jdbc.update(
        "INSERT INTO identity.users(id,email,display_name,role_id,status) SELECT ?,?, 'Catalogue actor',id,'ACTIVE' FROM identity.roles WHERE code=?",
        id,
        id + "@test.local",
        role);
    return tokens.issue(id, category, role, UUID.randomUUID()).value();
  }

  private MockHttpServletResponse call(MockHttpServletRequestBuilder request, String bearer)
      throws Exception {
    if (bearer != null) {
      request.header("Authorization", "Bearer " + bearer);
    }
    return mvc.perform(request).andReturn().getResponse();
  }

  private MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, String body) {
    return request.contentType(MediaType.APPLICATION_JSON).content(body);
  }

  private String field(String kind) {
    return kind.equals("levels") ? "displayName" : "name";
  }

  private String nameBody(String kind, String name) {
    return "{\"" + field(kind) + "\":\"" + name + "\"}";
  }

  private String id(String kind) {
    return (kind.equals("levels") ? LEVEL1 : TOPIC1).toString();
  }

  private String order(String kind) {
    return "{\"orderedIds\":[\""
        + (kind.equals("levels") ? LEVEL2 : TOPIC2)
        + "\",\""
        + id(kind)
        + "\"]}";
  }

  private void error(MockHttpServletResponse response, int status, String code) throws Exception {
    assertThat(response.getStatus()).isEqualTo(status);
    assertThat(JsonPath.<String>read(response.getContentAsString(), "$.errorCode")).isEqualTo(code);
  }

  private int audits() {
    return jdbc.queryForObject(
        "SELECT count(*) FROM identity.system_audit_logs WHERE action_type LIKE 'PROBLEM_%'",
        Integer.class);
  }

  @Test
  void allRolesReadOnlyAdminGetsCountsAndAnonymousIs401() throws Exception {
    UUID problem = addReference();
    String student = token(UUID.randomUUID(), "STUDENT", BaseCategory.STUDENT);
    String teacher = token(UUID.randomUUID(), "INSTRUCTOR", BaseCategory.INSTRUCTOR);
    for (String kind : List.of("levels", "topics")) {
      assertThat(call(get(BASE + kind), null).getStatus()).isEqualTo(401);
      for (String bearer : List.of(student, teacher, admin)) {
        var response = call(get(BASE + kind).param("includeCounts", "true"), bearer);
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(JsonPath.<Object>read(response.getContentAsString(), "$.items[0].problemCount"))
            .isEqualTo(bearer.equals(admin) ? 1 : null);
      }
      var noCount = call(get(BASE + kind), admin);
      assertThat(JsonPath.<Object>read(noCount.getContentAsString(), "$.items[0].problemCount"))
          .isNull();
    }
    assertThat(problem).isNotNull();
    assertThat(audits()).isZero();
  }

  @ParameterizedTest
  @ValueSource(strings = {"levels", "topics"})
  void createsRenamesReordersAndDeletesWithStableCodeAndAtomicAudit(String kind) throws Exception {
    var created = call(json(post(BASE + kind), nameBody(kind, " Rất khó ")), admin);
    assertThat(created.getStatus()).isEqualTo(201);
    assertThat(JsonPath.<String>read(created.getContentAsString(), "$.code")).isEqualTo("RAT-KHO");
    String fresh = JsonPath.read(created.getContentAsString(), "$.id");
    var renamed = call(json(patch(BASE + kind + "/" + fresh), nameBody(kind, "Nâng cao")), admin);
    assertThat(renamed.getStatus()).isEqualTo(200);
    assertThat(JsonPath.<String>read(renamed.getContentAsString(), "$.code")).isEqualTo("RAT-KHO");
    call(json(patch(BASE + kind + "/" + fresh), nameBody(kind, "Nâng cao")), admin);
    assertThat(audits()).isEqualTo(2);
    String full =
        "{\"orderedIds\":[\""
            + fresh
            + "\",\""
            + id(kind)
            + "\",\""
            + (kind.equals("levels") ? LEVEL2 : TOPIC2)
            + "\"]}";
    var reordered = call(json(put(BASE + kind + "/order"), full), admin);
    assertThat(reordered.getStatus()).isEqualTo(200);
    assertThat(JsonPath.<String>read(reordered.getContentAsString(), "$.items[0].id"))
        .isEqualTo(fresh);
    assertThat(
            JsonPath.<String>read(
                call(get(BASE + kind), admin).getContentAsString(), "$.items[0].id"))
        .isEqualTo(fresh);
    assertThat(call(delete(BASE + kind + "/" + fresh), admin).getStatus()).isEqualTo(204);
    error(
        call(delete(BASE + kind + "/" + fresh), admin),
        404,
        kind.equals("levels") ? "PRB-013" : "PRB-012");
    assertThat(audits()).isEqualTo(4);
    assertThat(redis.opsForValue().get("problem:list:version")).isEqualTo("4");
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM identity.system_audit_logs WHERE actor_user_id=? AND action_type LIKE 'PROBLEM_%'",
                Integer.class, adminId))
        .isEqualTo(4);
  }

  @ParameterizedTest
  @ValueSource(strings = {"levels", "topics"})
  void rejectsBadBodiesDuplicateNamesMissingIdsAndInvalidOrders(String kind) throws Exception {
    for (String body :
        List.of(
            nameBody(kind, " "),
            nameBody(kind, "x".repeat(61)),
            "{}",
            "{\"" + field(kind) + "\":7}",
            "{")) {
      error(call(json(post(BASE + kind), body), admin), 400, "PRB-001");
    }
    assertThat(call(json(post(BASE + kind), nameBody(kind, "x".repeat(60))), admin).getStatus())
        .isEqualTo(201);
    error(
        call(json(post(BASE + kind), nameBody(kind, kind.equals("levels") ? "dễ" : "mảng")), admin),
        409,
        kind.equals("levels") ? "PRB-022" : "PRB-021");
    error(
        call(json(patch(BASE + kind + "/" + UUID.randomUUID()), nameBody(kind, "Missing")), admin),
        404,
        kind.equals("levels") ? "PRB-013" : "PRB-012");
    error(call(delete(BASE + kind + "/broken"), admin), 400, "PRB-001");
    for (String body :
        List.of(
            "{}",
            "{\"orderedIds\":[]}",
            "{\"orderedIds\":[null]}",
            "{\"orderedIds\":[\"broken\"]}",
            "{\"orderedIds\":[\"" + id(kind) + "\",\"" + id(kind) + "\"]}")) {
      error(call(json(put(BASE + kind + "/order"), body), admin), 422, "PRB-049");
    }
    assertThat(audits()).isEqualTo(1);
  }

  private List<MockHttpServletRequestBuilder> writes(String kind) {
    return List.of(
        json(post(BASE + kind), nameBody(kind, "Fresh")),
        json(patch(BASE + kind + "/" + id(kind)), nameBody(kind, "Changed")),
        json(put(BASE + kind + "/order"), order(kind)),
        delete(BASE + kind + "/" + id(kind)));
  }

  private void cell(String role, String action, boolean granted) {
    jdbc.update(
        "UPDATE identity.permissions p SET granted=? FROM identity.roles r, identity.functions f, identity.actions a WHERE p.role_id=r.id AND p.function_id=f.id AND p.action_id=a.id AND r.code=? AND f.code='PROBLEM_AUTHORING' AND a.code=?",
        granted,
        role,
        action);
    jdbc.update(
        "UPDATE identity.roles SET permission_version=permission_version+1 WHERE code=?", role);
  }

  @Test
  void everyWriteRequiresBothAdminRoleAndItsOwnFunctionCell() throws Exception {
    for (String action : List.of("CREATE", "UPDATE", "DELETE")) {
      cell("INSTRUCTOR", action, true);
    }
    String teacher = token(UUID.randomUUID(), "INSTRUCTOR", BaseCategory.INSTRUCTOR);
    for (String kind : List.of("levels", "topics")) {
      for (var request : writes(kind)) {
        assertThat(call(request, null).getStatus()).isEqualTo(401);
      }
      for (var request : writes(kind)) {
        error(call(request, teacher), 403, "PRB-004");
      }
      for (int index = 0; index < 4; index++) {
        String action = List.of("CREATE", "UPDATE", "UPDATE", "DELETE").get(index);
        cell("ADMIN", action, false);
        try {
          error(call(writes(kind).get(index), admin), 403, "PRB-002");
        } finally {
          cell("ADMIN", action, true);
        }
      }
    }
    assertThat(audits()).isZero();
  }

  private UUID addReference() {
    UUID problem = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO problem.problems(id,code,title,level_id,time_limit_ms,memory_limit_mb,max_output_size_kb,author_id,deleted) VALUES (?,?,'Hidden draft',?,1000,256,64,?,true)",
        problem,
        problem.toString(),
        LEVEL1,
        adminId);
    jdbc.update(
        "INSERT INTO problem.problem_topics(problem_id,topic_id) VALUES (?,?)", problem, TOPIC1);
    return problem;
  }

  @Test
  void referenceChecksIncludeDeletedProblemsAndLevelCannotBecomeEmpty() throws Exception {
    addReference();
    error(call(delete(BASE + "levels/" + LEVEL1), admin), 422, "PRB-032");
    var topic = call(delete(BASE + "topics/" + TOPIC1), admin);
    error(topic, 422, "PRB-031");
    assertThat(
            JsonPath.<Integer>read(topic.getContentAsString(), "$.details.referencingProblemCount"))
        .isEqualTo(1);
    jdbc.update("DELETE FROM problem.problem_topics");
    jdbc.update("DELETE FROM problem.problems");
    assertThat(call(delete(BASE + "levels/" + LEVEL1), admin).getStatus()).isEqualTo(204);
    var last = call(delete(BASE + "levels/" + LEVEL2), admin);
    error(last, 422, "PRB-033");
    assertThat(JsonPath.<String>read(last.getContentAsString(), "$.details.reason"))
        .isEqualTo("LAST_LEVEL");
  }

  @Test
  void parallelDeletesLeaveOneLevelAndParallelDuplicateCreatesLeaveOneAudit() throws Exception {
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      var deletes =
          List.<Callable<Integer>>of(
              () -> call(delete(BASE + "levels/" + LEVEL1), admin).getStatus(),
              () -> call(delete(BASE + "levels/" + LEVEL2), admin).getStatus());
      var outcomes = executor.invokeAll(deletes);
      assertThat(List.of(outcomes.get(0).get(), outcomes.get(1).get()))
          .containsExactlyInAnyOrder(204, 422);
      var creates =
          List.<Callable<Integer>>of(
              () ->
                  call(json(post(BASE + "topics"), nameBody("topics", "Race")), admin).getStatus(),
              () ->
                  call(json(post(BASE + "topics"), nameBody("topics", "race")), admin).getStatus());
      var made = executor.invokeAll(creates);
      assertThat(List.of(made.get(0).get(), made.get(1).get())).containsExactlyInAnyOrder(201, 409);
    }
    assertThat(jdbc.queryForObject("SELECT count(*) FROM problem.problem_levels", Integer.class))
        .isEqualTo(1);
    assertThat(audits()).isEqualTo(2);
  }

  @Test
  void auditFailureRollsBackEveryWriteAndDoesNotInvalidateCache() throws Exception {
    jdbc.execute(
        "CREATE FUNCTION identity.reject_catalog_audit() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN IF NEW.action_type LIKE 'PROBLEM_%' THEN RAISE EXCEPTION 'test audit unavailable'; END IF; RETURN NEW; END $$");
    jdbc.execute(
        "CREATE TRIGGER reject_catalog_audit BEFORE INSERT ON identity.system_audit_logs FOR EACH ROW EXECUTE FUNCTION identity.reject_catalog_audit()");
    try {
      for (String kind : List.of("levels", "topics")) {
        for (var request : writes(kind)) {
          error(call(request, admin), 503, "PRB-050");
        }
      }
      assertThat(audits()).isZero();
      assertThat(jdbc.queryForObject("SELECT count(*) FROM problem.problem_levels", Integer.class))
          .isEqualTo(2);
      assertThat(
              jdbc.queryForObject(
                  "SELECT display_name FROM problem.problem_levels WHERE id=?",
                  String.class,
                  LEVEL1))
          .isEqualTo("Dễ");
      assertThat(redis.opsForValue().get("problem:list:version")).isNull();
    } finally {
      jdbc.execute("DROP TRIGGER reject_catalog_audit ON identity.system_audit_logs");
      jdbc.execute("DROP FUNCTION identity.reject_catalog_audit()");
    }
    assertThat(call(json(post(BASE + "levels"), nameBody("levels", "Retry")), admin).getStatus())
        .isEqualTo(201);
    assertThat(audits()).isEqualTo(1);
  }

  @Test
  void laterCallerFailureRollsBackBothCatalogueAndAuditWithRequiredPropagation() {
    var transaction = new TransactionTemplate(transactions);
    assertThatThrownBy(
            () ->
                transaction.execute(
                    status -> {
                      createLevel.execute(
                          new CatalogCommand(adminId, true, null, "Outer rollback", null));
                      assertThat(audits()).isEqualTo(1);
                      throw new IllegalStateException("abort caller");
                    }))
        .isInstanceOf(IllegalStateException.class);
    assertThat(audits()).isZero();
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM problem.problem_levels WHERE display_name='Outer rollback'",
                Integer.class))
        .isZero();
    assertThat(redis.opsForValue().get("problem:list:version")).isNull();
  }

  @Test
  void symbolOnlyNamesUseFallbackSuffixAndEmptyTopicOrderIsSupported() throws Exception {
    var codes = new ArrayList<String>();
    for (String name : List.of("!!!", "???")) {
      var response = call(json(post(BASE + "topics"), nameBody("topics", name)), admin);
      assertThat(response.getStatus()).isEqualTo(201);
      codes.add(JsonPath.read(response.getContentAsString(), "$.code"));
    }
    assertThat(codes).containsExactly("TOPIC", "TOPIC-2");
    jdbc.update("DELETE FROM problem.topics");
    var response = call(json(put(BASE + "topics/order"), "{\"orderedIds\":[]}"), admin);
    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(JsonPath.<List<Object>>read(response.getContentAsString(), "$.items")).isEmpty();
  }
}
