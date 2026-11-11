package com.algoprep.bootstrap.interviewbank;

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
import com.algoprep.interviewbank.application.dto.CatalogCommand;
import com.algoprep.interviewbank.application.ports.in.CreateQuestionTopicUseCase;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
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

/**
 * F6-1 topic and level catalogues on real PostgreSQL with real JWT authorization and the real F1
 * audit use case. Covers AC-24 to AC-29, AC-37 and AC-38 (catalogue part).
 */
@SpringBootTest(
    properties =
        "spring.autoconfigure.exclude=org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration")
@ActiveProfiles("test")
@Testcontainers
class InterviewBankCatalogFlowTest {
  private static final String BASE = "/api/v1/interview-bank/";
  private static final UUID[] TOPIC =
      new UUID[] {
        UUID.fromString("60000000-0000-4000-8000-000000000101"),
        UUID.fromString("60000000-0000-4000-8000-000000000102"),
        UUID.fromString("60000000-0000-4000-8000-000000000103"),
        UUID.fromString("60000000-0000-4000-8000-000000000104"),
        UUID.fromString("60000000-0000-4000-8000-000000000105")
      };
  private static final UUID[] LEVEL =
      new UUID[] {
        UUID.fromString("60000000-0000-4000-8000-000000000001"),
        UUID.fromString("60000000-0000-4000-8000-000000000002"),
        UUID.fromString("60000000-0000-4000-8000-000000000003")
      };

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
  @Autowired PlatformTransactionManager transactions;
  @Autowired CreateQuestionTopicUseCase createTopic;
  private MockMvc mvc;
  private UUID adminId;
  private String admin;
  private String teacher;
  private String student;

  @BeforeEach
  void setup() {
    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    jdbc.execute("TRUNCATE interview_bank.question_topics, interview_bank.question_levels CASCADE");
    jdbc.execute("ALTER SEQUENCE interview_bank.interview_question_display_no_seq RESTART");
    String[][] topics = {
      {"CS_THEORY", "Lý thuyết CS"},
      {"SYSTEM_DESIGN", "System design"},
      {"DATABASE", "Database"},
      {"LANGUAGE", "Ngôn ngữ"},
      {"BEHAVIORAL", "Hành vi"}
    };
    for (int i = 0; i < topics.length; i++) {
      jdbc.update(
          "INSERT INTO interview_bank.question_topics(id,code,display_name,sort_order,uses_star_framework) VALUES (?,?,?,?,?)",
          TOPIC[i],
          topics[i][0],
          topics[i][1],
          i + 1,
          i == 4);
    }
    String[][] levels = {{"EASY", "Dễ"}, {"MEDIUM", "Trung bình"}, {"HARD", "Khó"}};
    for (int i = 0; i < levels.length; i++) {
      jdbc.update(
          "INSERT INTO interview_bank.question_levels(id,code,display_name,sort_order) VALUES (?,?,?,?)",
          LEVEL[i],
          levels[i][0],
          levels[i][1],
          i + 1);
    }
    jdbc.update("DELETE FROM identity.system_audit_logs WHERE action_type LIKE 'INTERVIEW_%'");
    jdbc.update(
        "UPDATE identity.permissions p SET granted = (r.code = 'ADMIN') FROM identity.roles r, identity.functions f WHERE p.role_id = r.id AND p.function_id = f.id AND f.code = 'INTERVIEW_BANK_MANAGEMENT'");
    jdbc.update("UPDATE identity.roles SET permission_version = permission_version + 1");
    adminId = UUID.randomUUID();
    admin = token(adminId, "ADMIN", BaseCategory.ADMIN);
    student = token(UUID.randomUUID(), "STUDENT", BaseCategory.STUDENT);
    teacher = null;
  }

  private String token(UUID id, String role, BaseCategory category) {
    jdbc.update(
        "INSERT INTO identity.users(id,email,display_name,role_id,status) SELECT ?,?, 'Catalogue actor',id,'ACTIVE' FROM identity.roles WHERE code=?",
        id,
        id + "@test.local",
        role);
    return tokens.issue(id, category, role, UUID.randomUUID()).value();
  }

  /** The instructor holds all four management cells, as in AC-38; the role is still not ADMIN. */
  private String instructorWithAllCells() {
    cell("INSTRUCTOR", "CREATE", true);
    cell("INSTRUCTOR", "READ", true);
    cell("INSTRUCTOR", "UPDATE", true);
    cell("INSTRUCTOR", "DELETE", true);
    return token(UUID.randomUUID(), "INSTRUCTOR", BaseCategory.INSTRUCTOR);
  }

  private void cell(String role, String action, boolean granted) {
    jdbc.update(
        "UPDATE identity.permissions p SET granted=? FROM identity.roles r, identity.functions f, identity.actions a WHERE p.role_id=r.id AND p.function_id=f.id AND p.action_id=a.id AND r.code=? AND f.code='INTERVIEW_BANK_MANAGEMENT' AND a.code=?",
        granted,
        role,
        action);
    jdbc.update(
        "UPDATE identity.roles SET permission_version=permission_version+1 WHERE code=?", role);
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

  private static String name(String value) {
    return "{\"displayName\":\"" + value + "\"}";
  }

  private static String order(UUID... ids) {
    var text = new StringBuilder("{\"orderedIds\":[");
    for (int i = 0; i < ids.length; i++) {
      text.append(i == 0 ? "" : ",").append('"').append(ids[i]).append('"');
    }
    return text.append("]}").toString();
  }

  private static String body(MockHttpServletResponse response) throws Exception {
    return response.getContentAsString();
  }

  private void error(MockHttpServletResponse response, int status, String code) throws Exception {
    assertThat(response.getStatus()).isEqualTo(status);
    assertThat(JsonPath.<String>read(body(response), "$.errorCode")).isEqualTo(code);
  }

  private int audits() {
    return jdbc.queryForObject(
        "SELECT count(*) FROM identity.system_audit_logs WHERE action_type LIKE 'INTERVIEW_%'",
        Integer.class);
  }

  private List<String> auditActions() {
    return jdbc.queryForList(
        "SELECT action_type FROM identity.system_audit_logs WHERE action_type LIKE 'INTERVIEW_%' ORDER BY created_at, action_type",
        String.class);
  }

  private void question(UUID topic, UUID level, String status) {
    jdbc.update(
        "INSERT INTO interview_bank.interview_questions(id,topic_id,level_id,title,status,created_by) VALUES (?,?,?,'Q',?,?)",
        UUID.randomUUID(),
        topic,
        level,
        status,
        adminId);
  }

  private String codes(String kind) throws Exception {
    var found = JsonPath.<List<String>>read(body(call(get(BASE + kind), admin)), "$.items[*].code");
    return "[" + String.join(", ", found) + "]";
  }

  // AC-24 -------------------------------------------------------------------------------------

  @Test
  void everyRoleReadsTheOrderedCatalogueButOnlyAdminGetsCountsIncludingRetired() throws Exception {
    question(TOPIC[2], LEVEL[2], "RETIRED");
    var teacherToken = token(UUID.randomUUID(), "INSTRUCTOR", BaseCategory.INSTRUCTOR);
    for (String kind : List.of("topics", "levels")) {
      assertThat(call(get(BASE + kind), null).getStatus()).isEqualTo(401);
    }
    var plain = call(get(BASE + "topics"), student);
    assertThat(plain.getStatus()).isEqualTo(200);
    assertThat(JsonPath.<List<String>>read(body(plain), "$.items[*].code"))
        .containsExactly("CS_THEORY", "SYSTEM_DESIGN", "DATABASE", "LANGUAGE", "BEHAVIORAL");
    assertThat(JsonPath.<List<Boolean>>read(body(plain), "$.items[*].usesStarFramework"))
        .containsExactly(false, false, false, false, true);
    var levels = call(get(BASE + "levels"), student);
    assertThat(JsonPath.<List<String>>read(body(levels), "$.items[*].code"))
        .containsExactly("EASY", "MEDIUM", "HARD");
    assertThat(JsonPath.<Object>read(body(levels), "$.items[0]")).asString().doesNotContain("olor");
    for (String bearer : List.of(student, teacherToken)) {
      for (String kind : List.of("topics", "levels")) {
        var counted = call(get(BASE + kind).param("includeCounts", "true"), bearer);
        assertThat(JsonPath.<Object>read(body(counted), "$.items[0].questionCount")).isNull();
      }
    }
    var topics = call(get(BASE + "topics").param("includeCounts", "true"), admin);
    assertThat(JsonPath.<List<Integer>>read(body(topics), "$.items[*].questionCount"))
        .containsExactly(0, 0, 1, 0, 0);
    var counted = call(get(BASE + "levels").param("includeCounts", "true"), admin);
    assertThat(JsonPath.<List<Integer>>read(body(counted), "$.items[*].questionCount"))
        .containsExactly(0, 0, 1);
    assertThat(audits()).isZero();
  }

  // AC-25 -------------------------------------------------------------------------------------

  @Test
  void createsTopicWithGeneratedCodeAndKeepsItWhenRenamed() throws Exception {
    var created = call(json(post(BASE + "topics"), name(" Mạng máy tính ")), admin);
    assertThat(created.getStatus()).isEqualTo(201);
    assertThat(JsonPath.<String>read(body(created), "$.code")).isEqualTo("MANG_MAY_TINH");
    assertThat(JsonPath.<Integer>read(body(created), "$.sortOrder")).isEqualTo(6);
    assertThat(JsonPath.<Boolean>read(body(created), "$.usesStarFramework")).isFalse();
    assertThat(JsonPath.<Integer>read(body(created), "$.questionCount")).isZero();
    assertThat(JsonPath.<String>read(body(created), "$.displayName")).isEqualTo("Mạng máy tính");
    assertThat(auditActions()).containsExactly("INTERVIEW_TOPIC_CREATED");
    assertThat(
            jdbc.queryForObject(
                "SELECT actor_user_id FROM identity.system_audit_logs WHERE action_type='INTERVIEW_TOPIC_CREATED'",
                UUID.class))
        .isEqualTo(adminId);

    error(call(json(post(BASE + "topics"), name("database")), admin), 409, "INB-020");
    var csdl = call(json(post(BASE + "topics"), name("Cơ sở dữ liệu")), admin);
    String csdlId = JsonPath.read(body(csdl), "$.id");
    assertThat(JsonPath.<String>read(body(csdl), "$.code")).isEqualTo("CO_SO_DU_LIEU");
    var renamed = call(json(patch(BASE + "topics/" + csdlId), name("CSDL")), admin);
    assertThat(JsonPath.<String>read(body(renamed), "$.code")).isEqualTo("CO_SO_DU_LIEU");
    assertThat(JsonPath.<String>read(body(renamed), "$.displayName")).isEqualTo("CSDL");

    var first = call(json(post(BASE + "topics"), name("???")), admin);
    var second = call(json(post(BASE + "topics"), name("!!!")), admin);
    assertThat(JsonPath.<String>read(body(first), "$.code")).isEqualTo("TOPIC");
    assertThat(JsonPath.<String>read(body(second), "$.code")).isEqualTo("TOPIC_2");
    var third = call(json(post(BASE + "topics"), name("Topic")), admin);
    assertThat(JsonPath.<String>read(body(third), "$.code")).isEqualTo("TOPIC_3");
    assertThat(audits()).isEqualTo(6);
  }

  @Test
  void createsLevelAndRejectsBadNamesAndBodiesOnBothCatalogues() throws Exception {
    var level = call(json(post(BASE + "levels"), name("Rất khó")), admin);
    assertThat(level.getStatus()).isEqualTo(201);
    assertThat(JsonPath.<String>read(body(level), "$.code")).isEqualTo("RAT_KHO");
    assertThat(JsonPath.<Integer>read(body(level), "$.sortOrder")).isEqualTo(4);
    error(call(json(post(BASE + "levels"), name("dễ")), admin), 409, "INB-021");
    for (String kind : List.of("topics", "levels")) {
      for (String bad :
          List.of(name(" "), name("x".repeat(61)), "{}", "{\"displayName\":7}", "{", "[]")) {
        error(call(json(post(BASE + kind), bad), admin), 400, "INB-V03");
      }
      assertThat(call(json(post(BASE + kind), name("x".repeat(60))), admin).getStatus())
          .isEqualTo(201);
    }
    assertThat(audits()).isEqualTo(3);
  }

  // AC-26 -------------------------------------------------------------------------------------

  @Test
  void starToggleAndRenameChangeDisplayOnlyAndAreAuditedAndNoOpIsSilent() throws Exception {
    question(TOPIC[4], LEVEL[0], "ACTIVE");
    jdbc.update(
        "UPDATE interview_bank.interview_questions SET sample_answer_framework='S T A R text'");
    String url = BASE + "topics/" + TOPIC[4];
    var off = call(json(patch(url), "{\"usesStarFramework\":false}"), admin);
    assertThat(off.getStatus()).isEqualTo(200);
    assertThat(JsonPath.<Boolean>read(body(off), "$.usesStarFramework")).isFalse();
    assertThat(JsonPath.<String>read(body(off), "$.displayName")).isEqualTo("Hành vi");
    var rename = call(json(patch(url), name("Soft skills")), admin);
    assertThat(JsonPath.<String>read(body(rename), "$.code")).isEqualTo("BEHAVIORAL");
    assertThat(
            jdbc.queryForObject(
                "SELECT sample_answer_framework FROM interview_bank.interview_questions",
                String.class))
        .isEqualTo("S T A R text");
    assertThat(audits()).isEqualTo(2);
    String before =
        jdbc.queryForObject(
            "SELECT before_json::text FROM identity.system_audit_logs WHERE action_type='INTERVIEW_TOPIC_UPDATED' ORDER BY created_at LIMIT 1",
            String.class);
    assertThat(before).contains("\"usesStarFramework\": true").contains("Hành vi");
    // Sending the stored values again changes nothing and writes no audit row.
    var same =
        call(
            json(patch(url), "{\"displayName\":\"Soft skills\",\"usesStarFramework\":false}"),
            admin);
    assertThat(same.getStatus()).isEqualTo(200);
    assertThat(audits()).isEqualTo(2);
  }

  @Test
  void patchRejectsBadBodiesUnknownIdsAndDuplicateNames() throws Exception {
    String topic = BASE + "topics/" + TOPIC[0];
    error(call(json(patch(topic), "{}"), admin), 400, "INB-V13");
    error(call(json(patch(topic), "{\"usesStarFramework\":\"yes\"}"), admin), 400, "INB-V13");
    error(call(json(patch(topic), name(" ")), admin), 400, "INB-V03");
    error(call(json(patch(topic), name("database")), admin), 409, "INB-020");
    error(
        call(json(patch(BASE + "topics/" + UUID.randomUUID()), name("Zed")), admin),
        404,
        "INB-011");
    error(call(json(patch(BASE + "topics/not-a-uuid"), name("Zed")), admin), 404, "INB-011");
    String level = BASE + "levels/" + LEVEL[2];
    error(call(json(patch(level), "{}"), admin), 400, "INB-V03");
    error(call(json(patch(level), name("dễ")), admin), 409, "INB-021");
    error(
        call(json(patch(BASE + "levels/" + UUID.randomUUID()), name("Zed")), admin),
        404,
        "INB-012");
    var renamed = call(json(patch(level), name("Nâng cao")), admin);
    assertThat(JsonPath.<String>read(body(renamed), "$.code")).isEqualTo("HARD");
    assertThat(call(json(patch(level), name("Nâng cao")), admin).getStatus()).isEqualTo(200);
    assertThat(auditActions()).containsExactly("INTERVIEW_LEVEL_RENAMED");
  }

  // AC-27 -------------------------------------------------------------------------------------

  @Test
  void reorderSwapsPositionsEverywhereAndRefusesAnythingButTheFullSet() throws Exception {
    var swapped =
        call(
            json(
                put(BASE + "topics/order"),
                order(TOPIC[1], TOPIC[0], TOPIC[2], TOPIC[3], TOPIC[4])),
            admin);
    assertThat(swapped.getStatus()).isEqualTo(200);
    assertThat(JsonPath.<List<String>>read(body(swapped), "$.items[*].code"))
        .startsWith("SYSTEM_DESIGN", "CS_THEORY");
    assertThat(codes("topics")).startsWith("[SYSTEM_DESIGN, CS_THEORY");
    var levels = call(json(put(BASE + "levels/order"), order(LEVEL[2], LEVEL[1], LEVEL[0])), admin);
    assertThat(JsonPath.<List<Integer>>read(body(levels), "$.items[*].sortOrder"))
        .containsExactly(1, 2, 3);
    assertThat(codes("levels")).isEqualTo("[HARD, MEDIUM, EASY]");
    assertThat(auditActions())
        .containsExactlyInAnyOrder("INTERVIEW_TOPIC_REORDERED", "INTERVIEW_LEVEL_REORDERED");

    String stable = codes("levels");
    for (String bad :
        List.of(
            order(LEVEL[0], LEVEL[1]),
            order(LEVEL[0], LEVEL[1], LEVEL[1]),
            order(LEVEL[0], LEVEL[1], UUID.randomUUID()),
            order(LEVEL[0], LEVEL[1], LEVEL[2], UUID.randomUUID()),
            order())) {
      error(call(json(put(BASE + "levels/order"), bad), admin), 422, "INB-034");
    }
    for (String bad :
        List.of(
            "{}",
            "{\"orderedIds\":\"x\"}",
            "{\"orderedIds\":[null]}",
            "{\"orderedIds\":[\"broken\"]}",
            "{")) {
      error(call(json(put(BASE + "levels/order"), bad), admin), 400, "INB-V14");
    }
    assertThat(codes("levels")).isEqualTo(stable);
    assertThat(audits()).isEqualTo(2);
  }

  @Test
  void aSingleTopicCanBeReorderedAndAnEmptyCatalogueAcceptsAnEmptyList() throws Exception {
    jdbc.update("DELETE FROM interview_bank.question_topics WHERE id <> ?", TOPIC[0]);
    assertThat(call(json(put(BASE + "topics/order"), order(TOPIC[0])), admin).getStatus())
        .isEqualTo(200);
    jdbc.update("DELETE FROM interview_bank.question_topics");
    assertThat(call(json(put(BASE + "topics/order"), order()), admin).getStatus()).isEqualTo(200);
  }

  // AC-28 -------------------------------------------------------------------------------------

  @Test
  void topicDeleteIsRefusedWhileAnyQuestionEvenRetiredReferencesIt() throws Exception {
    question(TOPIC[0], LEVEL[0], "RETIRED");
    var refused = call(delete(BASE + "topics/" + TOPIC[0]), admin);
    error(refused, 422, "INB-030");
    assertThat(JsonPath.<Integer>read(body(refused), "$.details.referencingQuestionCount"))
        .isEqualTo(1);
    assertThat(audits()).isZero();
    assertThat(call(delete(BASE + "topics/" + TOPIC[1]), admin).getStatus()).isEqualTo(204);
    assertThat(codes("topics")).doesNotContain("SYSTEM_DESIGN");
    assertThat(auditActions()).containsExactly("INTERVIEW_TOPIC_DELETED");
    error(call(delete(BASE + "topics/" + TOPIC[1]), admin), 404, "INB-011");
    error(call(delete(BASE + "topics/broken"), admin), 404, "INB-011");
    error(call(delete(BASE + "topics/" + TOPIC[0]), teacherOrInstructor()), 403, "INB-302");
    assertThat(audits()).isEqualTo(1);
  }

  private String teacherOrInstructor() {
    if (teacher == null) {
      teacher = instructorWithAllCells();
    }
    return teacher;
  }

  @Test
  void deletingTopicRemovesItsDerivedProgressRowsButNotLearnerAnswers() throws Exception {
    UUID learner = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO interview_bank.practice_history(id,user_id,topic_id,questions_attempted,updated_at) VALUES (?,?,?,0,now())",
        UUID.randomUUID(),
        learner,
        TOPIC[3]);
    assertThat(call(delete(BASE + "topics/" + TOPIC[3]), admin).getStatus()).isEqualTo(204);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM interview_bank.practice_history", Integer.class))
        .isZero();
  }

  // AC-29 -------------------------------------------------------------------------------------

  @Test
  void levelDeleteRefusesInUseThenLastLevel() throws Exception {
    question(TOPIC[0], LEVEL[2], "RETIRED");
    var inUse = call(delete(BASE + "levels/" + LEVEL[2]), admin);
    error(inUse, 422, "INB-031");
    assertThat(JsonPath.<String>read(body(inUse), "$.details.reason")).isEqualTo("IN_USE");
    assertThat(JsonPath.<Integer>read(body(inUse), "$.details.referencingQuestionCount"))
        .isEqualTo(1);
    var extra = call(json(post(BASE + "levels"), name("Rất khó")), admin);
    String extraId = JsonPath.read(body(extra), "$.id");
    assertThat(call(delete(BASE + "levels/" + extraId), admin).getStatus()).isEqualTo(204);
    assertThat(call(delete(BASE + "levels/" + LEVEL[0]), admin).getStatus()).isEqualTo(204);
    assertThat(call(delete(BASE + "levels/" + LEVEL[1]), admin).getStatus()).isEqualTo(204);
    jdbc.update("DELETE FROM interview_bank.interview_questions");
    var last = call(delete(BASE + "levels/" + LEVEL[2]), admin);
    error(last, 422, "INB-032");
    assertThat(JsonPath.<String>read(body(last), "$.details.reason")).isEqualTo("LAST_LEVEL");
    assertThat(codes("levels")).isEqualTo("[HARD]");
    error(call(delete(BASE + "levels/" + LEVEL[0]), admin), 404, "INB-012");
    assertThat(auditActions()).filteredOn(a -> a.endsWith("_DELETED")).hasSize(3);
  }

  @Test
  void twoAdminsDeletingTwoRemainingLevelsLeaveExactlyOneLevel() throws Exception {
    jdbc.update("DELETE FROM interview_bank.question_levels WHERE id = ?", LEVEL[0]);
    String second = token(UUID.randomUUID(), "ADMIN", BaseCategory.ADMIN);
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      var outcomes =
          executor.invokeAll(
              List.<Callable<MockHttpServletResponse>>of(
                  () -> call(delete(BASE + "levels/" + LEVEL[1]), admin),
                  () -> call(delete(BASE + "levels/" + LEVEL[2]), second)));
      var statuses = List.of(outcomes.get(0).get().getStatus(), outcomes.get(1).get().getStatus());
      assertThat(statuses).containsExactlyInAnyOrder(204, 422);
      var loser =
          outcomes.get(0).get().getStatus() == 422 ? outcomes.get(0).get() : outcomes.get(1).get();
      assertThat(JsonPath.<String>read(body(loser), "$.errorCode")).isEqualTo("INB-032");
    }
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM interview_bank.question_levels", Integer.class))
        .isEqualTo(1);
    assertThat(audits()).isEqualTo(1);
  }

  @Test
  void parallelCreatesWithTheSameNameDifferingInCaseLeaveOneRowAndOneAudit() throws Exception {
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      var made =
          executor.invokeAll(
              List.<Callable<Integer>>of(
                  () -> call(json(post(BASE + "topics"), name("Race")), admin).getStatus(),
                  () -> call(json(post(BASE + "topics"), name("race")), admin).getStatus()));
      assertThat(List.of(made.get(0).get(), made.get(1).get())).containsExactlyInAnyOrder(201, 409);
    }
    assertThat(audits()).isEqualTo(1);
  }

  // AC-38 -------------------------------------------------------------------------------------

  private List<MockHttpServletRequestBuilder> writes(String kind) {
    boolean levels = kind.equals("levels");
    UUID id = levels ? LEVEL[0] : TOPIC[0];
    String swapped =
        levels
            ? order(LEVEL[1], LEVEL[0], LEVEL[2])
            : order(TOPIC[1], TOPIC[0], TOPIC[2], TOPIC[3], TOPIC[4]);
    return List.of(
        json(post(BASE + kind), name("Fresh")),
        json(patch(BASE + kind + "/" + id), name("Changed")),
        json(put(BASE + kind + "/order"), swapped),
        delete(BASE + kind + "/" + id));
  }

  @Test
  void instructorWithEveryCellIsRefusedAsNonAdminAndStillReadsTheCatalogue() throws Exception {
    String instructor = teacherOrInstructor();
    for (String kind : List.of("topics", "levels")) {
      assertThat(call(get(BASE + kind), instructor).getStatus()).isEqualTo(200);
      for (var request : writes(kind)) {
        error(call(request, instructor), 403, "INB-302");
      }
    }
    assertThat(audits()).isZero();
    assertThat(codes("topics")).startsWith("[CS_THEORY, SYSTEM_DESIGN, DATABASE");
    assertThat(codes("levels")).isEqualTo("[EASY, MEDIUM, HARD]");
  }

  @Test
  void missingFunctionCellIsInb301BeforeTheRoleCheckAndAnonymousIs401() throws Exception {
    for (String kind : List.of("topics", "levels")) {
      for (var request : writes(kind)) {
        assertThat(call(request, null).getStatus()).isEqualTo(401);
        error(call(request, student), 403, "INB-301");
      }
      int index = 0;
      for (String action : List.of("CREATE", "UPDATE", "UPDATE", "DELETE")) {
        cell("ADMIN", action, false);
        String revoked = token(UUID.randomUUID(), "ADMIN", BaseCategory.ADMIN);
        try {
          error(call(writes(kind).get(index), revoked), 403, "INB-301");
        } finally {
          cell("ADMIN", action, true);
        }
        index++;
      }
    }
    assertThat(audits()).isZero();
  }

  // AC-37 -------------------------------------------------------------------------------------

  @Test
  void everySuccessfulWriteLeavesExactlyOneAuditRowNamingActorAndTarget() throws Exception {
    var topic = call(json(post(BASE + "topics"), name("Alpha")), admin);
    String topicId = JsonPath.read(body(topic), "$.id");
    call(json(patch(BASE + "topics/" + topicId), "{\"usesStarFramework\":true}"), admin);
    call(
        json(
            put(BASE + "topics/order"),
            order(topicId(topicId), TOPIC[0], TOPIC[1], TOPIC[2], TOPIC[3], TOPIC[4])),
        admin);
    call(delete(BASE + "topics/" + topicId), admin);
    var level = call(json(post(BASE + "levels"), name("Beta")), admin);
    String levelId = JsonPath.read(body(level), "$.id");
    call(json(patch(BASE + "levels/" + levelId), name("Gamma")), admin);
    call(
        json(put(BASE + "levels/order"), order(levelId(levelId), LEVEL[0], LEVEL[1], LEVEL[2])),
        admin);
    call(delete(BASE + "levels/" + levelId), admin);
    assertThat(auditActions())
        .containsExactlyInAnyOrder(
            "INTERVIEW_TOPIC_CREATED",
            "INTERVIEW_TOPIC_UPDATED",
            "INTERVIEW_TOPIC_REORDERED",
            "INTERVIEW_TOPIC_DELETED",
            "INTERVIEW_LEVEL_CREATED",
            "INTERVIEW_LEVEL_RENAMED",
            "INTERVIEW_LEVEL_REORDERED",
            "INTERVIEW_LEVEL_DELETED");
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM identity.system_audit_logs WHERE action_type LIKE 'INTERVIEW_%' AND actor_user_id=?",
                Integer.class, adminId))
        .isEqualTo(8);
    assertThat(
            jdbc.queryForObject(
                "SELECT target_id FROM identity.system_audit_logs WHERE action_type='INTERVIEW_LEVEL_DELETED'",
                String.class))
        .isEqualTo(levelId);
    assertThat(
            jdbc.queryForObject(
                "SELECT target_type FROM identity.system_audit_logs WHERE action_type='INTERVIEW_TOPIC_REORDERED'",
                String.class))
        .isEqualTo("INTERVIEW_TOPIC");
  }

  private static UUID topicId(String text) {
    return UUID.fromString(text);
  }

  private static UUID levelId(String text) {
    return UUID.fromString(text);
  }

  @Test
  void auditFailureReturns503Inb503AndRollsBackEveryWrite() throws Exception {
    jdbc.execute(
        "CREATE FUNCTION identity.reject_interview_audit() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN IF NEW.action_type LIKE 'INTERVIEW_%' THEN RAISE EXCEPTION 'test audit unavailable'; END IF; RETURN NEW; END $$");
    jdbc.execute(
        "CREATE TRIGGER reject_interview_audit BEFORE INSERT ON identity.system_audit_logs FOR EACH ROW EXECUTE FUNCTION identity.reject_interview_audit()");
    try {
      for (String kind : List.of("topics", "levels")) {
        for (var request : writes(kind)) {
          error(call(request, admin), 503, "INB-503");
        }
      }
      assertThat(audits()).isZero();
      assertThat(codes("topics"))
          .isEqualTo("[CS_THEORY, SYSTEM_DESIGN, DATABASE, LANGUAGE, BEHAVIORAL]");
      assertThat(codes("levels")).isEqualTo("[EASY, MEDIUM, HARD]");
      assertThat(
              jdbc.queryForObject(
                  "SELECT display_name FROM interview_bank.question_levels WHERE id=?",
                  String.class,
                  LEVEL[0]))
          .isEqualTo("Dễ");
    } finally {
      jdbc.execute("DROP TRIGGER reject_interview_audit ON identity.system_audit_logs");
      jdbc.execute("DROP FUNCTION identity.reject_interview_audit()");
    }
    assertThat(call(json(post(BASE + "levels"), name("Retry")), admin).getStatus()).isEqualTo(201);
    assertThat(audits()).isEqualTo(1);
  }

  @Test
  void callerTransactionRollbackAlsoRollsBackTheAuditRow() {
    var transaction = new TransactionTemplate(transactions);
    assertThatThrownBy(
            () ->
                transaction.execute(
                    status -> {
                      createTopic.execute(
                          new CatalogCommand(adminId, true, null, "Outer rollback", null, null));
                      assertThat(audits()).isEqualTo(1);
                      throw new IllegalStateException("abort caller");
                    }))
        .isInstanceOf(IllegalStateException.class);
    assertThat(audits()).isZero();
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM interview_bank.question_topics WHERE display_name='Outer rollback'",
                Integer.class))
        .isZero();
  }
}
