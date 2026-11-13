package com.algoprep.bootstrap.interviewbank;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.algoprep.identity.application.ports.out.AccessTokenPort;
import com.algoprep.identity.domain.model.BaseCategory;
import com.algoprep.interviewbank.application.dto.CatalogCommand;
import com.algoprep.interviewbank.application.dto.QuestionCommand;
import com.algoprep.interviewbank.application.ports.in.CreateQuestionUseCase;
import com.algoprep.interviewbank.application.ports.in.DeleteQuestionTopicUseCase;
import com.algoprep.interviewbank.domain.model.RubricCriterion;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
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
 * F6-2 question management and CSV import on real PostgreSQL with real JWT authorization and the
 * real F1 audit use case. Covers AC-30 to AC-36 and the question and CSV part of AC-37, plus the
 * IB14-B1 race between creating a question and deleting its topic.
 */
@SpringBootTest(
    properties =
        "spring.autoconfigure.exclude=org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration")
@ActiveProfiles("test")
@Testcontainers
class InterviewBankQuestionFlowTest {
  private static final String BASE = "/api/v1/interview-bank/admin/questions";
  private static final UUID[] TOPIC = {
    UUID.fromString("60000000-0000-4000-8000-000000000101"),
    UUID.fromString("60000000-0000-4000-8000-000000000102"),
    UUID.fromString("60000000-0000-4000-8000-000000000103"),
    UUID.fromString("60000000-0000-4000-8000-000000000104"),
    UUID.fromString("60000000-0000-4000-8000-000000000105")
  };
  private static final UUID[] LEVEL = {
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
  @Autowired CreateQuestionUseCase createQuestion;
  @Autowired DeleteQuestionTopicUseCase deleteTopic;
  private MockMvc mvc;
  private UUID adminId;
  private String admin;
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
  }

  // Helpers -----------------------------------------------------------------------------------

  private String token(UUID id, String role, BaseCategory category) {
    jdbc.update(
        "INSERT INTO identity.users(id,email,display_name,role_id,status) SELECT ?,?, 'Question actor',id,'ACTIVE' FROM identity.roles WHERE code=?",
        id,
        id + "@test.local",
        role);
    return tokens.issue(id, category, role, UUID.randomUUID()).value();
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

  /** An instructor holding exactly the given cells; the role is never ADMIN. */
  private String instructor(String... actions) {
    for (String action : List.of("CREATE", "READ", "UPDATE", "DELETE")) {
      cell("INSTRUCTOR", action, List.of(actions).contains(action));
    }
    return token(UUID.randomUUID(), "INSTRUCTOR", BaseCategory.INSTRUCTOR);
  }

  private String teacher() {
    return instructor("CREATE", "READ", "UPDATE", "DELETE");
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

  private static String body(MockHttpServletResponse response) throws Exception {
    return response.getContentAsString();
  }

  private void error(MockHttpServletResponse response, int status, String code) throws Exception {
    assertThat(response.getStatus()).as(response.getContentAsString()).isEqualTo(status);
    assertThat(JsonPath.<String>read(body(response), "$.errorCode")).isEqualTo(code);
  }

  private static String rubric(String... pairs) {
    var items = new ArrayList<String>();
    for (int i = 0; i < pairs.length; i += 2) {
      items.add("{\"criterionCode\":\"" + pairs[i] + "\",\"weightPercent\":" + pairs[i + 1] + "}");
    }
    return "[" + String.join(",", items) + "]";
  }

  private static String question(UUID topic, UUID level, String title, String rubric) {
    return "{\"topicId\":\""
        + topic
        + "\",\"levelId\":\""
        + level
        + "\",\"title\":\""
        + title
        + "\",\"rubric\":"
        + rubric
        + "}";
  }

  private MockHttpServletResponse create(String bearer, String title, UUID level, String rubric)
      throws Exception {
    return call(json(post(BASE), question(TOPIC[2], level, title, rubric)), bearer);
  }

  private UUID created(MockHttpServletResponse response) throws Exception {
    assertThat(response.getStatus()).as(response.getContentAsString()).isEqualTo(201);
    return UUID.fromString(JsonPath.read(body(response), "$.id"));
  }

  private UUID seed(UUID topic, UUID level, String title, String status) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO interview_bank.interview_questions(id,topic_id,level_id,title,status,created_by) VALUES (?,?,?,?,?,?)",
        id,
        topic,
        level,
        title,
        status,
        adminId);
    return id;
  }

  private void practiceHistory(UUID learner) {
    jdbc.update(
        "INSERT INTO interview_bank.practice_history(id,user_id,topic_id,questions_attempted,last_practiced_at) VALUES (?,?,?,1,now())",
        UUID.randomUUID(),
        learner,
        TOPIC[2]);
  }

  private void bookmark(UUID learner, UUID question) {
    jdbc.update(
        "INSERT INTO interview_bank.bookmarks(id,user_id,question_id) VALUES (?,?,?)",
        UUID.randomUUID(),
        learner,
        question);
  }

  private static String idsBody(UUID... ids) {
    return "{\"ids\":[\""
        + java.util.Arrays.stream(ids)
            .map(UUID::toString)
            .collect(java.util.stream.Collectors.joining("\",\""))
        + "\"]}";
  }

  private void attempt(UUID user, UUID question, int attemptNo, String status, String feedback) {
    jdbc.update(
        "INSERT INTO interview_bank.user_answers(id,user_id,question_id,answer_text,attempt_no,feedback_status,feedback_result_json,rubric_snapshot_json) VALUES (?,?,?,'answer',?,?,CAST(? AS jsonb),'[]'::jsonb)",
        UUID.randomUUID(),
        user,
        question,
        attemptNo,
        status,
        feedback);
  }

  private static String criteria(int... pairs) {
    var items = new ArrayList<String>();
    for (int i = 0; i < pairs.length; i += 2) {
      items.add(
          "{\"criterionCode\":\"c"
              + i
              + "\",\"achievementPercent\":"
              + pairs[i]
              + ",\"weightPercent\":"
              + pairs[i + 1]
              + "}");
    }
    return "{\"achievedPoints\":[],\"missingPoints\":[],\"improvementSuggestions\":[],\"criteria\":["
        + String.join(",", items)
        + "]}";
  }

  private int audits() {
    return jdbc.queryForObject(
        "SELECT count(*) FROM identity.system_audit_logs WHERE action_type LIKE 'INTERVIEW_QUESTION_%'",
        Integer.class);
  }

  private List<String> auditActions() {
    return jdbc.queryForList(
        "SELECT action_type FROM identity.system_audit_logs WHERE action_type LIKE 'INTERVIEW_QUESTION_%' ORDER BY created_at, target_id",
        String.class);
  }

  private int count(String sql, Object... args) {
    return jdbc.queryForObject(sql, Integer.class, args);
  }

  private String list(String bearer, String query) throws Exception {
    var response = call(get(BASE + query), bearer);
    assertThat(response.getStatus()).as(response.getContentAsString()).isEqualTo(200);
    return body(response);
  }

  private List<String> titles(String bearer, String q) throws Exception {
    var response = call(get(BASE).param("q", q).param("pageSize", "20"), bearer);
    assertThat(response.getStatus()).isEqualTo(200);
    return JsonPath.read(body(response), "$.items[*].title");
  }

  // AC-30 -------------------------------------------------------------------------------------

  @Test
  void instructorsAndAdminSeeTheSameBankSortedByCatalogueOrderAndPagedStrictly() throws Exception {
    String g1 = teacher();
    String g2 = teacher();
    created(create(g1, "Câu của G1 một", LEVEL[2], "[]"));
    created(create(g1, "Câu của G1 hai", LEVEL[0], "[]"));
    UUID gone = created(create(g1, "Câu của G1 ba", LEVEL[1], "[]"));
    created(create(admin, "Câu của Q một", LEVEL[1], "[]"));
    created(create(admin, "Câu của Q hai", LEVEL[2], "[]"));
    assertThat(
            call(json(post(BASE + "/bulk/delete"), "{\"ids\":[\"" + gone + "\"]}"), g1).getStatus())
        .isEqualTo(200);
    for (String bearer : List.of(g1, g2, admin)) {
      String page = list(bearer, "?pageSize=20");
      assertThat(JsonPath.<List<String>>read(page, "$.items[*].title"))
          .containsExactly("Câu của G1 một", "Câu của G1 hai", "Câu của Q một", "Câu của Q hai");
      assertThat(JsonPath.<Integer>read(page, "$.meta.shown")).isEqualTo(4);
      assertThat(JsonPath.<Integer>read(page, "$.meta.total")).isEqualTo(4);
      assertThat(JsonPath.<Integer>read(page, "$.items[0].displayNo")).isEqualTo(1);
    }
    // Level descending follows the catalogue order (Khó, Trung bình, Dễ), not the alphabet.
    String byLevel = list(g2, "?sort=level&dir=desc");
    assertThat(JsonPath.<List<String>>read(byLevel, "$.items[*].level.code"))
        .containsExactly("HARD", "HARD", "MEDIUM", "EASY");
    String asc = list(g2, "?sort=level");
    assertThat(JsonPath.<List<String>>read(asc, "$.items[*].level.code"))
        .containsExactly("EASY", "MEDIUM", "HARD", "HARD");
    assertThat(JsonPath.<String>read(asc, "$.items[0].topic.displayName")).isEqualTo("Database");
    error(call(get(BASE), student), 403, "INB-301");
    assertThat(call(get(BASE), null).getStatus()).isEqualTo(401);
  }

  @Test
  void listRejectsBadParametersFiltersAndSearchesWithoutAccents() throws Exception {
    created(create(admin, "Câu hỏi về chỉ mục", LEVEL[0], "[]"));
    seed(TOPIC[0], LEVEL[1], "Another title", "ACTIVE");
    for (String bad :
        List.of(
            "?pageSize=7",
            "?pageSize=abc",
            "?page=0",
            "?page=x",
            "?sort=password",
            "?dir=up",
            "?topic=NOPE",
            "?level=NOPE",
            "?q=" + "x".repeat(101))) {
      error(call(get(BASE + bad), admin), 400, "INB-V01");
    }
    assertThat(titles(admin, "chi muc")).containsExactly("Câu hỏi về chỉ mục");
    assertThat(titles(admin, "CAU ho")).containsExactly("Câu hỏi về chỉ mục");
    assertThat(titles(admin, "%")).hasSize(2);
    assertThat(JsonPath.<List<String>>read(list(admin, "?topic=CS_THEORY"), "$.items[*].title"))
        .containsExactly("Another title");
    assertThat(
            JsonPath.<List<String>>read(
                list(admin, "?level=EASY&topic=DATABASE"), "$.items[*].title"))
        .containsExactly("Câu hỏi về chỉ mục");
    // A page beyond the last returns the last page, never an error.
    String last = list(admin, "?page=99&pageSize=8");
    assertThat(JsonPath.<Integer>read(last, "$.page.number")).isEqualTo(1);
    assertThat(JsonPath.<Integer>read(last, "$.page.totalPages")).isEqualTo(1);
    assertThat(JsonPath.<Integer>read(last, "$.page.totalItems")).isEqualTo(2);
  }

  @Test
  void sortOrderIsAppliedBeforePagingAcrossTheWholeFilteredSet() throws Exception {
    for (int i = 1; i <= 10; i++) {
      seed(TOPIC[2], LEVEL[0], String.format("T%02d", i), "ACTIVE");
    }
    var first = list(admin, "?sort=title&dir=desc&pageSize=8");
    assertThat(JsonPath.<List<String>>read(first, "$.items[*].title"))
        .containsExactly("T10", "T09", "T08", "T07", "T06", "T05", "T04", "T03");
    var second = list(admin, "?sort=title&dir=desc&pageSize=8&page=2");
    assertThat(JsonPath.<List<String>>read(second, "$.items[*].title"))
        .containsExactly("T02", "T01");
  }

  // AC-31 -------------------------------------------------------------------------------------

  @Test
  void averageScoreComesFromCompletedAttemptsAndIsHalfUpAndUsageCountsEveryAttempt()
      throws Exception {
    UUID x = seed(TOPIC[2], LEVEL[0], "Scored", "ACTIVE");
    UUID none = seed(TOPIC[2], LEVEL[0], "Never practiced", "ACTIVE");
    UUID rounding = seed(TOPIC[2], LEVEL[0], "Rounding", "ACTIVE");
    UUID u1 = UUID.randomUUID();
    UUID u2 = UUID.randomUUID();
    attempt(u1, x, 1, "COMPLETED", criteria(80, 30, 60, 40, 100, 30));
    attempt(u2, x, 1, "COMPLETED", criteria(100, 30, 100, 40, 100, 30));
    attempt(u1, x, 2, "FAILED", null);
    String teacher = teacher();
    String page = list(teacher, "?pageSize=20");
    assertThat(
            JsonPath.<List<Integer>>read(page, "$.items[?(@.title=='Scored')].usageCount").get(0))
        .isEqualTo(3);
    assertThat(
            JsonPath.<List<Double>>read(page, "$.items[?(@.title=='Scored')].averageScorePercent")
                .get(0))
        .isEqualTo(89.0);
    assertThat(
            JsonPath.<List<Integer>>read(page, "$.items[?(@.title=='Never practiced')].usageCount")
                .get(0))
        .isZero();
    assertThat(
            JsonPath.<List<Object>>read(
                page, "$.items[?(@.title=='Never practiced')].averageScorePercent"))
        .containsExactly((Object) null);
    // Two attempts scoring exactly 78.25 each round half-up to 78.3.
    jdbc.update("DELETE FROM interview_bank.user_answers WHERE question_id=?", rounding);
    String quarter =
        "{\"criteria\":[{\"criterionCode\":\"a\",\"achievementPercent\":100,\"weightPercent\":78.25},"
            + "{\"criterionCode\":\"b\",\"achievementPercent\":0,\"weightPercent\":21.75}]}";
    attempt(u1, rounding, 1, "COMPLETED", quarter);
    attempt(u2, rounding, 1, "COMPLETED", quarter);
    String detail = body(call(get(BASE + "/" + rounding), teacher));
    assertThat(JsonPath.<Double>read(detail, "$.usage.averageScorePercent")).isEqualTo(78.3);
    // Sorting by average puts the null last in both directions.
    assertThat(
            JsonPath.<List<String>>read(
                list(teacher, "?sort=averageScorePercent&dir=desc&pageSize=20"),
                "$.items[*].title"))
        .containsExactly("Scored", "Rounding", "Never practiced");
    assertThat(
            JsonPath.<List<Integer>>read(
                list(teacher, "?sort=usageCount&dir=desc&pageSize=20"), "$.items[*].usageCount"))
        .containsExactly(3, 2, 0);
    assertThat(none).isNotNull();
  }

  // AC-32 -------------------------------------------------------------------------------------

  @Test
  void detailShowsRubricInOrderAndRetiredOrUnknownIdsAre404() throws Exception {
    String teacher = teacher();
    UUID x =
        created(
            call(
                json(
                    post(BASE),
                    "{\"topicId\":\""
                        + TOPIC[2]
                        + "\",\"levelId\":\""
                        + LEVEL[1]
                        + "\",\"title\":\"X\",\"contentMarkdown\":\"Body\",\"followUpQuestions\":[\" one \",\"\",\"two\"],\"rubric\":"
                        + rubric("Clarity", "30", "Depth", "40", "Example", "30")
                        + "}"),
                teacher));
    UUID y = created(create(teacher, "Y", LEVEL[1], "[]"));
    UUID z = created(create(teacher, "Z", LEVEL[1], "[]"));
    call(json(post(BASE + "/bulk/delete"), "{\"ids\":[\"" + z + "\"]}"), teacher);
    String detail = body(call(get(BASE + "/" + x), teacher));
    assertThat(JsonPath.<List<String>>read(detail, "$.rubric[*].criterionCode"))
        .containsExactly("Clarity", "Depth", "Example");
    assertThat(JsonPath.<List<Double>>read(detail, "$.rubric[*].weightPercent"))
        .containsExactly(30.0, 40.0, 30.0);
    assertThat(JsonPath.<List<String>>read(detail, "$.followUpQuestions"))
        .containsExactly("one", "two");
    assertThat(JsonPath.<Boolean>read(detail, "$.practiceAvailable")).isTrue();
    assertThat(JsonPath.<String>read(detail, "$.status")).isEqualTo("ACTIVE");
    assertThat(JsonPath.<Integer>read(detail, "$.usage.usageCount")).isZero();
    assertThat(JsonPath.<String>read(detail, "$.contentMarkdown")).isEqualTo("Body");
    String empty = body(call(get(BASE + "/" + y), teacher));
    assertThat(JsonPath.<List<Object>>read(empty, "$.rubric")).isEmpty();
    assertThat(JsonPath.<Boolean>read(empty, "$.practiceAvailable")).isFalse();
    error(call(get(BASE + "/" + z), teacher), 404, "INB-010");
    error(call(get(BASE + "/" + UUID.randomUUID()), teacher), 404, "INB-010");
    error(call(get(BASE + "/not-a-uuid"), teacher), 404, "INB-010");
    error(call(get(BASE + "/" + x), student), 403, "INB-301");
    assertThat(call(get(BASE + "/" + x), null).getStatus()).isEqualTo(401);
  }

  // AC-33 -------------------------------------------------------------------------------------

  @Test
  void createValidatesEveryRuleAndStoresCleanData() throws Exception {
    String g = teacher();
    var ok = create(g, "First", LEVEL[1], rubric("A", "30", "B", "40", "C", "30"));
    UUID first = created(ok);
    assertThat(JsonPath.<Integer>read(body(ok), "$.displayNo")).isEqualTo(1);
    assertThat(JsonPath.<Boolean>read(body(ok), "$.practiceAvailable")).isTrue();
    assertThat(JsonPath.<Integer>read(body(ok), "$.usage.usageCount")).isZero();
    var second = create(g, "Second", LEVEL[1], "[]");
    created(second);
    assertThat(JsonPath.<Integer>read(body(second), "$.displayNo")).isEqualTo(2);
    assertThat(JsonPath.<Boolean>read(body(second), "$.practiceAvailable")).isFalse();
    assertThat(auditActions())
        .containsExactly("INTERVIEW_QUESTION_CREATED", "INTERVIEW_QUESTION_CREATED");
    assertThat(
            jdbc.queryForObject(
                "SELECT created_by FROM interview_bank.interview_questions WHERE id=?",
                UUID.class,
                first))
        .isNotEqualTo(adminId);
    assertThat(
            jdbc.queryForObject(
                "SELECT suggested_approach IS NULL AND sample_answer_framework IS NULL AND core_keywords IS NULL FROM interview_bank.interview_questions WHERE id=?",
                Boolean.class,
                first))
        .isTrue();

    var sum = create(g, "Sum 90", LEVEL[1], rubric("A", "30", "B", "40", "C", "20"));
    error(sum, 422, "INB-033");
    assertThat(JsonPath.<Number>read(body(sum), "$.details.currentSum").doubleValue())
        .isEqualTo(90.0);
    error(create(g, "Dup", LEVEL[1], rubric("Rõ ràng", "50", "RÕ RÀNG", "50")), 400, "INB-V11");
    error(create(g, "T".repeat(256), LEVEL[1], "[]"), 400, "INB-V08");
    created(create(g, "T".repeat(255), LEVEL[1], "[]"));
    created(create(g, "Thirds", LEVEL[1], rubric("A", "33.33", "B", "33.33", "C", "33.34")));
    error(create(g, "Over", LEVEL[1], rubric("A", "100.01")), 400, "INB-V10");
    error(create(g, "Scale", LEVEL[1], rubric("A", "12.345")), 400, "INB-V10");
    error(create(g, "Neg", LEVEL[1], rubric("A", "-1")), 400, "INB-V10");
    error(create(g, "Blank", LEVEL[1], rubric(" ", "100")), 400, "INB-V10");
    error(create(g, "Long", LEVEL[1], rubric("c".repeat(101), "100")), 400, "INB-V10");
    created(create(g, "Long ok", LEVEL[1], rubric("c".repeat(100), "100")));
    error(create(g, " ", LEVEL[1], "[]"), 400, "INB-V08");
    createRejectsBadReferencesAndBodies(g);
  }

  private void createRejectsBadReferencesAndBodies(String g) throws Exception {
    var noMatch = create(g, "Gone topic", LEVEL[1], "[]");
    assertThat(noMatch.getStatus()).isEqualTo(201);
    var missingTopic =
        call(json(post(BASE), question(UUID.randomUUID(), LEVEL[1], "No topic", "[]")), g);
    error(missingTopic, 422, "INB-035");
    assertThat(JsonPath.<String>read(body(missingTopic), "$.details.field")).isEqualTo("topicId");
    var missingLevel =
        call(json(post(BASE), question(TOPIC[2], UUID.randomUUID(), "No level", "[]")), g);
    error(missingLevel, 422, "INB-035");
    assertThat(JsonPath.<String>read(body(missingLevel), "$.details.field")).isEqualTo("levelId");
    error(
        call(json(post(BASE), "{\"levelId\":\"" + LEVEL[0] + "\",\"title\":\"x\"}"), g),
        400,
        "INB-V09");
    error(
        call(
            json(
                post(BASE),
                "{\"topicId\":\"nope\",\"levelId\":\"" + LEVEL[0] + "\",\"title\":\"x\"}"),
            g),
        400,
        "INB-V09");
    error(
        call(
            json(
                post(BASE),
                question(TOPIC[2], LEVEL[0], "Fu", "[]")
                    .replace("}", ",\"followUpQuestions\":\"x\"}")),
            g),
        400,
        "INB-V12");
    error(
        call(
            json(
                post(BASE),
                question(TOPIC[2], LEVEL[0], "Fu", "[]")
                    .replace("}", ",\"followUpQuestions\":[1]}")),
            g),
        400,
        "INB-V12");
    error(call(json(post(BASE), "["), g), 400, "INB-V08");
    // Rejected requests leave no rows and no audit beyond the six accepted creates.
    assertThat(count("SELECT count(*) FROM interview_bank.interview_questions")).isEqualTo(6);
    assertThat(audits()).isEqualTo(6);
  }

  @Test
  void createRejectsOversizedBodyAndRequiresTheCreateCell() throws Exception {
    String huge = "{\"title\":\"" + "x".repeat(524_300) + "\"}";
    error(call(json(post(BASE), huge), admin), 413, "INB-V17");
    String exact =
        question(TOPIC[2], LEVEL[0], "Edge", "[]")
            .replace("\"rubric\":[]", "\"contentMarkdown\":\"" + "y".repeat(500_000) + "\"");
    assertThat(call(json(post(BASE), exact), admin).getStatus()).isEqualTo(201);
    error(create(student, "No", LEVEL[0], "[]"), 403, "INB-301");
    error(create(instructor("READ", "UPDATE", "DELETE"), "No", LEVEL[0], "[]"), 403, "INB-301");
    assertThat(call(json(post(BASE), "{}"), null).getStatus()).isEqualTo(401);
    assertThat(count("SELECT count(*) FROM interview_bank.interview_questions")).isEqualTo(1);
  }

  // AC-34 -------------------------------------------------------------------------------------

  private String form(UUID id, UUID topic, UUID level, String title, String rubric) {
    return question(topic, level, title, rubric);
  }

  @Test
  void updateReplacesFormStateKeepsCsvColumnsAndRecalculatesProgress() throws Exception {
    UUID x = created(create(admin, "Original", LEVEL[1], rubric("A", "50", "B", "50")));
    jdbc.update(
        "UPDATE interview_bank.interview_questions SET suggested_approach='- step', sample_answer_framework='Sample', core_keywords='[\"k\"]'::jsonb WHERE id=?",
        x);
    UUID learner = UUID.randomUUID();
    attempt(learner, x, 1, "COMPLETED", criteria(100, 50, 100, 50));
    practiceHistory(learner);
    String snapshotBefore =
        jdbc.queryForObject(
            "SELECT rubric_snapshot_json::text FROM interview_bank.user_answers WHERE question_id=?",
            String.class,
            x);
    String g = teacher();
    var edited =
        call(
            json(putTo(BASE + "/" + x), form(x, TOPIC[2], LEVEL[1], "Renamed", rubric("A", "100"))),
            g);
    assertThat(edited.getStatus()).as(body(edited)).isEqualTo(200);
    assertThat(JsonPath.<String>read(body(edited), "$.title")).isEqualTo("Renamed");
    assertThat(JsonPath.<List<String>>read(body(edited), "$.rubric[*].criterionCode"))
        .containsExactly("A");
    var row =
        jdbc.queryForMap(
            "SELECT suggested_approach, sample_answer_framework, core_keywords::text AS kw, created_by, updated_at > created_at AS touched FROM interview_bank.interview_questions WHERE id=?",
            x);
    assertThat(row.get("suggested_approach")).isEqualTo("- step");
    assertThat(row.get("sample_answer_framework")).isEqualTo("Sample");
    assertThat((String) row.get("kw")).contains("k");
    assertThat(row.get("created_by")).isEqualTo(adminId);
    assertThat(row.get("touched")).isEqualTo(true);
    assertThat(
            jdbc.queryForObject(
                "SELECT rubric_snapshot_json::text FROM interview_bank.user_answers WHERE question_id=?",
                String.class,
                x))
        .isEqualTo(snapshotBefore);

    var cleared =
        call(json(putTo(BASE + "/" + x), form(x, TOPIC[2], LEVEL[1], "Renamed", "[]")), g);
    assertThat(JsonPath.<Boolean>read(body(cleared), "$.practiceAvailable")).isFalse();
    assertThat(count("SELECT count(*) FROM interview_bank.answer_rubrics WHERE question_id=?", x))
        .isZero();

    movingTopicMovesProgressAndAuditsBefore(x, learner, g);
  }

  private void movingTopicMovesProgressAndAuditsBefore(UUID x, UUID learner, String g)
      throws Exception {
    var moved = call(json(putTo(BASE + "/" + x), form(x, TOPIC[3], LEVEL[2], "Renamed", "[]")), g);
    assertThat(moved.getStatus()).isEqualTo(200);
    assertThat(
            jdbc.queryForObject(
                "SELECT questions_attempted FROM interview_bank.practice_history WHERE user_id=? AND topic_id=?",
                Integer.class,
                learner,
                TOPIC[2]))
        .isZero();
    assertThat(
            jdbc.queryForObject(
                "SELECT questions_attempted FROM interview_bank.practice_history WHERE user_id=? AND topic_id=?",
                Integer.class,
                learner,
                TOPIC[3]))
        .isEqualTo(1);
    assertThat(auditActions().stream().filter(a -> a.endsWith("_UPDATED")).count()).isEqualTo(3);
    String before =
        jdbc.queryForObject(
            "SELECT before_json FROM identity.system_audit_logs WHERE action_type='INTERVIEW_QUESTION_UPDATED' ORDER BY created_at LIMIT 1",
            String.class);
    assertThat(JsonPath.<String>read(before, "$.title")).isEqualTo("Original");
    assertThat(JsonPath.<Integer>read(before, "$.rubricCount")).isEqualTo(2);
  }

  @Test
  void updateRefusesBadStateAndLeavesTheQuestionUntouched() throws Exception {
    UUID x = created(create(admin, "Stable", LEVEL[1], rubric("A", "100")));
    UUID retired = seed(TOPIC[2], LEVEL[1], "Retired", "RETIRED");
    String g = teacher();
    error(
        call(
            json(
                putTo(BASE + "/" + x),
                form(x, TOPIC[2], LEVEL[1], "Hacked", rubric("A", "60", "B", "39"))),
            g),
        422,
        "INB-033");
    error(
        call(
            json(
                putTo(BASE + "/" + x),
                form(x, TOPIC[2], LEVEL[1], "Hacked", rubric("A", "50", "a", "50"))),
            g),
        400,
        "INB-V11");
    error(
        call(json(putTo(BASE + "/" + x), form(x, UUID.randomUUID(), LEVEL[1], "Hacked", "[]")), g),
        422,
        "INB-035");
    updateRefusesMissingAndMalformedTargets(x, retired, g);
  }

  private void updateRefusesMissingAndMalformedTargets(UUID x, UUID retired, String g)
      throws Exception {
    error(
        call(json(putTo(BASE + "/" + retired), form(retired, TOPIC[2], LEVEL[1], "Back", "[]")), g),
        404,
        "INB-010");
    error(
        call(
            json(putTo(BASE + "/" + UUID.randomUUID()), form(x, TOPIC[2], LEVEL[1], "Ghost", "[]")),
            g),
        404,
        "INB-010");
    error(
        call(json(putTo(BASE + "/not-a-uuid"), form(x, TOPIC[2], LEVEL[1], "Ghost", "[]")), g),
        404,
        "INB-010");
    error(
        call(json(putTo(BASE + "/" + x), form(x, TOPIC[2], LEVEL[1], " ", "[]")), g),
        400,
        "INB-V08");
    error(
        call(json(putTo(BASE + "/" + x), "{\"title\":\"" + "x".repeat(524_300) + "\"}"), g),
        413,
        "INB-V17");
    assertThat(
            jdbc.queryForObject(
                "SELECT title FROM interview_bank.interview_questions WHERE id=?", String.class, x))
        .isEqualTo("Stable");
    assertThat(count("SELECT count(*) FROM interview_bank.answer_rubrics WHERE question_id=?", x))
        .isEqualTo(1);
    assertThat(audits()).isEqualTo(1);
    error(
        call(json(putTo(BASE + "/" + x), form(x, TOPIC[2], LEVEL[1], "No", "[]")), student),
        403,
        "INB-301");
    error(
        call(
            json(putTo(BASE + "/" + x), form(x, TOPIC[2], LEVEL[1], "No", "[]")),
            instructor("READ", "CREATE", "DELETE")),
        403,
        "INB-301");
    assertThat(call(json(putTo(BASE + "/" + x), "{}"), null).getStatus()).isEqualTo(401);
  }

  private MockHttpServletRequestBuilder putTo(String url) {
    return org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(
        java.net.URI.create(url));
  }

  // AC-35 -------------------------------------------------------------------------------------

  @Test
  void bulkDeleteRetiresActiveOnesReportsFailuresAndKeepsRelatedData() throws Exception {
    String g = teacher();
    UUID a = created(create(g, "A", LEVEL[0], rubric("R", "100")));
    UUID b = created(create(g, "B", LEVEL[0], "[]"));
    UUID c = created(create(g, "C", LEVEL[0], "[]"));
    UUID done = seed(TOPIC[2], LEVEL[0], "Done", "RETIRED");
    UUID unknown = UUID.randomUUID();
    UUID learner = UUID.randomUUID();
    attempt(learner, a, 1, "COMPLETED", criteria(100, 100));
    practiceHistory(learner);
    bookmark(learner, a);
    var response = call(json(post(BASE + "/bulk/delete"), idsBody(a, done, b, unknown, c)), g);
    assertThat(response.getStatus()).as(body(response)).isEqualTo(200);
    assertThat(JsonPath.<List<String>>read(body(response), "$.retiredIds"))
        .containsExactly(a.toString(), b.toString(), c.toString());
    assertThat(JsonPath.<List<String>>read(body(response), "$.failedItems[*].id"))
        .containsExactly(done.toString(), unknown.toString());
    assertThat(JsonPath.<List<String>>read(body(response), "$.failedItems[*].errorCode"))
        .containsOnly("INB-010");
    assertThat(auditActions()).filteredOn(x -> x.endsWith("_RETIRED")).hasSize(3);
    assertThat(
            jdbc.queryForList(
                "SELECT DISTINCT target_id FROM identity.system_audit_logs WHERE action_type='INTERVIEW_QUESTION_RETIRED'",
                String.class))
        .containsExactlyInAnyOrder(a.toString(), b.toString(), c.toString());
    assertThat(JsonPath.<Integer>read(list(g, ""), "$.meta.shown")).isZero();
    assertThat(count("SELECT count(*) FROM interview_bank.answer_rubrics WHERE question_id=?", a))
        .isEqualTo(1);
    assertThat(count("SELECT count(*) FROM interview_bank.user_answers WHERE question_id=?", a))
        .isEqualTo(1);
    assertThat(count("SELECT count(*) FROM interview_bank.bookmarks WHERE question_id=?", a))
        .isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "SELECT questions_attempted FROM interview_bank.practice_history WHERE user_id=?",
                Integer.class,
                learner))
        .isZero();
    error(call(get(BASE + "/" + a), g), 404, "INB-010");
    // Sending the same ids again changes nothing: every id is now a failed item.
    var again = call(json(post(BASE + "/bulk/delete"), "{\"ids\":[\"" + a + "\"]}"), g);
    assertThat(JsonPath.<List<Object>>read(body(again), "$.retiredIds")).isEmpty();
    assertThat(audits()).isEqualTo(6);
  }

  @Test
  void bulkDeleteValidatesTheIdListAndTheDeleteCell() throws Exception {
    String g = teacher();
    List<String> bad =
        new ArrayList<>(
            List.of(
                "{}",
                "{\"ids\":[]}",
                "{\"ids\":\"x\"}",
                "{\"ids\":[1]}",
                "{\"ids\":[\"nope\"]}",
                "["));
    UUID same = UUID.randomUUID();
    bad.add("{\"ids\":[\"" + same + "\",\"" + same + "\"]}");
    var hundredOne = new ArrayList<String>();
    for (int i = 0; i < 101; i++) {
      hundredOne.add("\"" + UUID.randomUUID() + "\"");
    }
    bad.add("{\"ids\":[" + String.join(",", hundredOne) + "]}");
    for (String body : bad) {
      error(call(json(post(BASE + "/bulk/delete"), body), g), 400, "INB-V02");
    }
    var hundred = new ArrayList<String>();
    for (int i = 0; i < 100; i++) {
      hundred.add("\"" + UUID.randomUUID() + "\"");
    }
    var edge =
        call(json(post(BASE + "/bulk/delete"), "{\"ids\":[" + String.join(",", hundred) + "]}"), g);
    assertThat(JsonPath.<List<Object>>read(body(edge), "$.failedItems")).hasSize(100);
    String ok = "{\"ids\":[\"" + UUID.randomUUID() + "\"]}";
    error(call(json(post(BASE + "/bulk/delete"), ok), student), 403, "INB-301");
    error(
        call(json(post(BASE + "/bulk/delete"), ok), instructor("READ", "CREATE", "UPDATE")),
        403,
        "INB-301");
    assertThat(call(json(post(BASE + "/bulk/delete"), ok), null).getStatus()).isEqualTo(401);
    assertThat(audits()).isZero();
  }

  // AC-36 -------------------------------------------------------------------------------------

  private static String row(int line, String topic, String level, String question, String rubric) {
    return "{\"line\":"
        + line
        + ",\"topic\":\""
        + topic
        + "\",\"level\":\""
        + level
        + "\",\"question\":\""
        + question
        + "\",\"rubric\":"
        + rubric
        + "}";
  }

  private MockHttpServletResponse importRows(String bearer, String... rows) throws Exception {
    return call(
        json(post(BASE + "/import"), "{\"rows\":[" + String.join(",", rows) + "]}"), bearer);
  }

  @Test
  void importAcceptsGoodRowsReportsBadOnesAndNeverOverwritesDuplicates() throws Exception {
    String g = teacher();
    UUID existing = seed(TOPIC[2], LEVEL[1], "Existing question", "ACTIVE");
    String rich =
        "{\"line\":6,\"topic\":\"DATABASE\",\"level\":\"trung bình\",\"question\":\"  Rich row \",\"rubric\":[],"
            + "\"followUps\":[\"f1\",\" \",\"f2\"],\"content\":\"Body\",\"suggestedApproach\":[\"step one\",\"step two\"],"
            + "\"coreKeywords\":[\"index\",\"b-tree\"],\"sampleAnswer\":\"Sample\"}";
    var response =
        importRows(
            g,
            row(2, "Database", "Trung bình", "Plain good", rubric("A", "60", "B", "40")),
            row(3, "Unknown topic", "MEDIUM", "Bad topic", "[]"),
            row(4, "DATABASE", "MEDIUM", "Bad sum", rubric("A", "30", "B", "30")),
            row(5, "database", "medium", "EXISTING QUESTION", "[]"),
            rich);
    assertThat(response.getStatus()).as(body(response)).isEqualTo(200);
    assertThat(JsonPath.<Integer>read(body(response), "$.importedCount")).isEqualTo(2);
    assertThat(JsonPath.<List<Integer>>read(body(response), "$.skippedRows[*].line"))
        .containsExactly(3, 4, 5);
    assertThat(JsonPath.<List<String>>read(body(response), "$.skippedRows[*].reasonCode"))
        .containsExactly("unknownTopic", "rubricSum", "duplicate");
    assertThat(
            jdbc.queryForObject(
                "SELECT title FROM interview_bank.interview_questions WHERE id=?",
                String.class,
                existing))
        .isEqualTo("Existing question");
    assertThat(count("SELECT count(*) FROM interview_bank.interview_questions")).isEqualTo(3);
    var imported =
        jdbc.queryForMap(
            "SELECT suggested_approach, sample_answer_framework, core_keywords::text AS kw, follow_up_questions::text AS fu, content_markdown, display_no FROM interview_bank.interview_questions WHERE title='Rich row'");
    assertThat(imported.get("suggested_approach")).isEqualTo("- step one\n- step two");
    assertThat(imported.get("sample_answer_framework")).isEqualTo("Sample");
    assertThat((String) imported.get("kw")).contains("index").contains("b-tree");
    assertThat((String) imported.get("fu")).contains("f1").contains("f2");
    assertThat(imported.get("content_markdown")).isEqualTo("Body");
    assertThat(
            jdbc.queryForList(
                "SELECT display_no FROM interview_bank.interview_questions WHERE status='ACTIVE' AND title<>'Existing question' ORDER BY display_no",
                Integer.class))
        .hasSize(2);
    assertThat(auditActions())
        .containsExactly("INTERVIEW_QUESTION_IMPORTED", "INTERVIEW_QUESTION_IMPORTED");
    assertThat(count("SELECT count(*) FROM interview_bank.answer_rubrics")).isEqualTo(2);

    var again = importRows(g, row(2, "Database", "Trung bình", "Plain good", "[]"), rich);
    assertThat(JsonPath.<Integer>read(body(again), "$.importedCount")).isZero();
    assertThat(JsonPath.<List<String>>read(body(again), "$.skippedRows[*].reasonCode"))
        .containsExactly("duplicate", "duplicate");
    assertThat(audits()).isEqualTo(2);
  }

  @Test
  void importReportsEveryRowReasonAndDuplicatesInsideOneFile() throws Exception {
    var response =
        importRows(
            admin,
            row(2, "", "MEDIUM", "Missing topic", "[]"),
            row(3, "DATABASE", "Nope", "Bad level", "[]"),
            row(4, "DATABASE", "MEDIUM", "Bad rubric", rubric("A", "-5")),
            row(5, "DATABASE", "MEDIUM", "Dup rubric", rubric("A", "50", "a", "50")),
            row(6, "DATABASE", "MEDIUM", "x".repeat(256), "[]"),
            row(7, "DATABASE", "MEDIUM", " ", "[]"),
            row(8, "DATABASE", "MEDIUM", "Twice", "[]"),
            row(9, "DATABASE", "MEDIUM", "TWICE", "[]"),
            "{\"line\":10,\"topic\":\"DATABASE\",\"level\":\"MEDIUM\",\"question\":\"Odd lists\",\"followUps\":\"not-array\"}",
            "{\"line\":11,\"topic\":\"DATABASE\",\"level\":\"MEDIUM\",\"question\":\"Odd rubric\",\"rubric\":\"x\"}");
    assertThat(JsonPath.<Integer>read(body(response), "$.importedCount")).isEqualTo(1);
    assertThat(JsonPath.<List<String>>read(body(response), "$.skippedRows[*].reasonCode"))
        .containsExactly(
            "missingField",
            "unknownLevel",
            "badRubric",
            "badRubric",
            "invalidField",
            "missingField",
            "duplicate",
            "invalidField",
            "badRubric");
    assertThat(JsonPath.<List<Integer>>read(body(response), "$.skippedRows[*].line"))
        .containsExactly(2, 3, 4, 5, 6, 7, 9, 10, 11);
    assertThat(count("SELECT count(*) FROM interview_bank.interview_questions WHERE title='Twice'"))
        .isEqualTo(1);
  }

  @Test
  void importWholeFileLimitsAreEnforcedBeforeAnythingIsWritten() throws Exception {
    var rows = new ArrayList<String>();
    for (int i = 0; i < 501; i++) {
      rows.add(row(i + 2, "DATABASE", "MEDIUM", "Row " + i, "[]"));
    }
    error(importRows(admin, rows.toArray(String[]::new)), 400, "INB-V15");
    assertThat(count("SELECT count(*) FROM interview_bank.interview_questions")).isZero();
    for (String bad :
        List.of(
            "{}",
            "{\"rows\":[]}",
            "{\"rows\":\"x\"}",
            "{\"rows\":[1]}",
            "{\"rows\":[{\"topic\":\"DATABASE\"}]}",
            "{\"rows\":[{\"line\":\"2\"}]}",
            "{\"rows\":[{\"line\":2.5}]}",
            "not json")) {
      error(call(json(post(BASE + "/import"), bad), admin), 400, "INB-V15");
    }
    error(
        call(json(post(BASE + "/import"), "{\"rows\":\"" + "x".repeat(5_300_000) + "\"}"), admin),
        413,
        "INB-V17");
    // Exactly 500 rows is the largest accepted file; 501 was refused above.
    rows.remove(500);
    var exact = importRows(admin, rows.toArray(String[]::new));
    assertThat(JsonPath.<Integer>read(body(exact), "$.importedCount")).isEqualTo(500);
    assertThat(audits()).isEqualTo(500);
  }

  @Test
  void importNeedsTheCreateCellAndTwoParallelImportsOfOneTitleKeepOne() throws Exception {
    String one = row(2, "DATABASE", "MEDIUM", "Race", "[]");
    error(importRows(student, one), 403, "INB-301");
    error(importRows(instructor("READ", "UPDATE", "DELETE"), one), 403, "INB-301");
    assertThat(call(json(post(BASE + "/import"), "{}"), null).getStatus()).isEqualTo(401);
    String second = teacher();
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      var outcomes =
          executor.invokeAll(
              List.<Callable<Integer>>of(
                  () -> JsonPath.<Integer>read(body(importRows(admin, one)), "$.importedCount"),
                  () -> JsonPath.<Integer>read(body(importRows(second, one)), "$.importedCount")));
      assertThat(List.of(outcomes.get(0).get(), outcomes.get(1).get()))
          .containsExactlyInAnyOrder(0, 1);
    }
    assertThat(count("SELECT count(*) FROM interview_bank.interview_questions")).isEqualTo(1);
    assertThat(audits()).isEqualTo(1);
  }

  // AC-37 (question and CSV part) --------------------------------------------------------------

  @Test
  void everyQuestionWriteLeavesOneAuditRowWithActorTypeAndTarget() throws Exception {
    String g = teacher();
    UUID id = created(create(g, "Audited", LEVEL[0], "[]"));
    call(json(putTo(BASE + "/" + id), form(id, TOPIC[2], LEVEL[0], "Audited 2", "[]")), g);
    importRows(g, row(2, "DATABASE", "EASY", "From csv", "[]"));
    call(json(post(BASE + "/bulk/delete"), "{\"ids\":[\"" + id + "\"]}"), g);
    var rows =
        jdbc.queryForList(
            "SELECT action_type, target_type, target_id, actor_user_id::text AS actor FROM identity.system_audit_logs WHERE action_type LIKE 'INTERVIEW_QUESTION_%' ORDER BY created_at");
    assertThat(rows)
        .extracting(r -> r.get("action_type"))
        .containsExactly(
            "INTERVIEW_QUESTION_CREATED",
            "INTERVIEW_QUESTION_UPDATED",
            "INTERVIEW_QUESTION_IMPORTED",
            "INTERVIEW_QUESTION_RETIRED");
    assertThat(rows).extracting(r -> r.get("target_type")).containsOnly("INTERVIEW_QUESTION");
    assertThat(rows.get(0).get("target_id")).isEqualTo(id.toString());
    assertThat(rows.get(3).get("target_id")).isEqualTo(id.toString());
    assertThat(rows).extracting(r -> r.get("actor")).doesNotContain(adminId.toString());
    // Reads leave no audit row.
    list(g, "");
    call(get(BASE + "/" + id), g);
    assertThat(audits()).isEqualTo(4);
  }

  private void withRejectingAudit(Runnable body) {
    jdbc.execute(
        "CREATE FUNCTION identity.reject_interview_audit() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN IF NEW.action_type LIKE 'INTERVIEW_%' THEN RAISE EXCEPTION 'test audit unavailable'; END IF; RETURN NEW; END $$");
    jdbc.execute(
        "CREATE TRIGGER reject_interview_audit BEFORE INSERT ON identity.system_audit_logs FOR EACH ROW EXECUTE FUNCTION identity.reject_interview_audit()");
    try {
      body.run();
    } finally {
      jdbc.execute("DROP TRIGGER reject_interview_audit ON identity.system_audit_logs");
      jdbc.execute("DROP FUNCTION identity.reject_interview_audit()");
    }
  }

  private void everyWriteIsRefusedWith503(UUID existing) throws Exception {
    error(create(admin, "New", LEVEL[0], rubric("A", "100")), 503, "INB-503");
    error(
        call(
            json(putTo(BASE + "/" + existing), form(existing, TOPIC[3], LEVEL[2], "Changed", "[]")),
            admin),
        503,
        "INB-503");
    error(call(json(post(BASE + "/bulk/delete"), idsBody(existing)), admin), 503, "INB-503");
    error(
        importRows(
            admin,
            row(2, "DATABASE", "EASY", "Imp1", "[]"),
            row(3, "DATABASE", "EASY", "Imp2", "[]")),
        503,
        "INB-503");
  }

  @Test
  void auditFailureReturns503AndRollsBackEveryQuestionWrite() throws Exception {
    UUID existing = created(create(admin, "Keep me", LEVEL[1], rubric("A", "100")));
    UUID learner = UUID.randomUUID();
    attempt(learner, existing, 1, "COMPLETED", criteria(100, 100));
    practiceHistory(learner);
    int auditsBefore = audits();
    withRejectingAudit(
        () -> {
          try {
            everyWriteIsRefusedWith503(existing);
          } catch (Exception failure) {
            throw new IllegalStateException(failure);
          }
        });
    assertThat(audits()).isEqualTo(auditsBefore);
    assertThat(count("SELECT count(*) FROM interview_bank.interview_questions")).isEqualTo(1);
    var row =
        jdbc.queryForMap(
            "SELECT title, status, topic_id FROM interview_bank.interview_questions WHERE id=?",
            existing);
    assertThat(row.get("title")).isEqualTo("Keep me");
    assertThat(row.get("status")).isEqualTo("ACTIVE");
    assertThat(row.get("topic_id")).isEqualTo(TOPIC[2]);
    assertThat(
            count(
                "SELECT count(*) FROM interview_bank.answer_rubrics WHERE question_id=?", existing))
        .isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "SELECT questions_attempted FROM interview_bank.practice_history WHERE user_id=?",
                Integer.class,
                learner))
        .isEqualTo(1);
    // Once the audit store is back, the same requests succeed.
    assertThat(create(admin, "New", LEVEL[0], rubric("A", "100")).getStatus()).isEqualTo(201);
  }

  @Test
  void callerTransactionRollbackAlsoRollsBackTheQuestionAndItsAuditRow() {
    var transaction = new TransactionTemplate(transactions);
    assertThatThrownBy(
            () ->
                transaction.execute(
                    status -> {
                      createQuestion.execute(
                          new QuestionCommand(
                              adminId,
                              null,
                              TOPIC[2],
                              LEVEL[0],
                              "Outer rollback",
                              null,
                              List.of(),
                              List.of(new RubricCriterion("A", new BigDecimal("100"), null))));
                      assertThat(audits()).isEqualTo(1);
                      throw new IllegalStateException("abort caller");
                    }))
        .isInstanceOf(IllegalStateException.class);
    assertThat(audits()).isZero();
    assertThat(count("SELECT count(*) FROM interview_bank.interview_questions")).isZero();
    assertThat(count("SELECT count(*) FROM interview_bank.answer_rubrics")).isZero();
  }

  // IB14-B1: a question is created while its topic is deleted ---------------------------------

  private void awaitBlockedSession() throws InterruptedException {
    for (int i = 0; i < 100; i++) {
      if (count("SELECT count(*) FROM pg_stat_activity WHERE wait_event_type='Lock'") > 0) {
        return;
      }
      TimeUnit.MILLISECONDS.sleep(100);
    }
    throw new AssertionError("no session ever waited on a lock");
  }

  @Test
  void topicDeletedFirstMakesTheWaitingCreateFailWithInb035() throws Exception {
    var holding = new CountDownLatch(1);
    var release = new CountDownLatch(1);
    var transaction = new TransactionTemplate(transactions);
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      var deleter =
          executor.submit(
              () ->
                  transaction.execute(
                      status -> {
                        deleteTopic.execute(
                            new CatalogCommand(adminId, true, TOPIC[3], null, null, null));
                        holding.countDown();
                        await(release);
                        return null;
                      }));
      assertThat(holding.await(10, TimeUnit.SECONDS)).isTrue();
      var creator =
          executor.submit(
              () -> call(json(post(BASE), question(TOPIC[3], LEVEL[0], "Raced", "[]")), admin));
      awaitBlockedSession();
      assertThat(creator.isDone()).isFalse();
      release.countDown();
      deleter.get(10, TimeUnit.SECONDS);
      var response = creator.get(10, TimeUnit.SECONDS);
      error(response, 422, "INB-035");
      assertThat(JsonPath.<String>read(body(response), "$.details.field")).isEqualTo("topicId");
    }
    assertThat(count("SELECT count(*) FROM interview_bank.interview_questions")).isZero();
    assertThat(count("SELECT count(*) FROM interview_bank.question_topics WHERE id=?", TOPIC[3]))
        .isZero();
  }

  @Test
  void questionCreatedFirstMakesTheWaitingTopicDeleteFailWithInb030() throws Exception {
    var holding = new CountDownLatch(1);
    var release = new CountDownLatch(1);
    var transaction = new TransactionTemplate(transactions);
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      var creator =
          executor.submit(
              () ->
                  transaction.execute(
                      status -> {
                        createQuestion.execute(
                            new QuestionCommand(
                                adminId, null, TOPIC[3], LEVEL[0], "Raced", null, List.of(),
                                List.of()));
                        holding.countDown();
                        await(release);
                        return null;
                      }));
      assertThat(holding.await(10, TimeUnit.SECONDS)).isTrue();
      var deleter =
          executor.submit(() -> call(delete("/api/v1/interview-bank/topics/" + TOPIC[3]), admin));
      awaitBlockedSession();
      assertThat(deleter.isDone()).isFalse();
      release.countDown();
      creator.get(10, TimeUnit.SECONDS);
      var response = deleter.get(10, TimeUnit.SECONDS);
      error(response, 422, "INB-030");
      assertThat(JsonPath.<Integer>read(body(response), "$.details.referencingQuestionCount"))
          .isEqualTo(1);
    }
    assertThat(count("SELECT count(*) FROM interview_bank.interview_questions")).isEqualTo(1);
    assertThat(count("SELECT count(*) FROM interview_bank.question_topics WHERE id=?", TOPIC[3]))
        .isEqualTo(1);
  }

  private static void await(CountDownLatch latch) {
    try {
      latch.await(30, TimeUnit.SECONDS);
    } catch (InterruptedException interrupted) {
      Thread.currentThread().interrupt();
    }
  }

  // Authority matrix --------------------------------------------------------------------------

  @Test
  void eachEndpointNeedsExactlyItsOwnFunctionCellAndNoAdminRole() throws Exception {
    UUID id = created(create(admin, "Target", LEVEL[0], "[]"));
    String delBody = "{\"ids\":[\"" + UUID.randomUUID() + "\"]}";
    String importBody = "{\"rows\":[" + row(2, "DATABASE", "EASY", "Matrix import", "[]") + "]}";
    var requests =
        List.of(
            new Object[] {"READ", get(BASE)},
            new Object[] {"READ", get(BASE + "/" + id)},
            new Object[] {"CREATE", json(post(BASE), question(TOPIC[2], LEVEL[0], "Matrix", "[]"))},
            new Object[] {
              "UPDATE", json(putTo(BASE + "/" + id), question(TOPIC[2], LEVEL[0], "Matrix", "[]"))
            },
            new Object[] {"DELETE", json(post(BASE + "/bulk/delete"), delBody)},
            new Object[] {"CREATE", json(post(BASE + "/import"), importBody)});
    for (var entry : requests) {
      String needed = (String) entry[0];
      var request = (MockHttpServletRequestBuilder) entry[1];
      for (String other : List.of("CREATE", "READ", "UPDATE", "DELETE")) {
        if (!other.equals(needed)) {
          error(call(request, instructor(other)), 403, "INB-301");
        }
      }
      var allowed = call(request, instructor(needed));
      assertThat(allowed.getStatus()).as(needed + " " + body(allowed)).isIn(200, 201);
    }
  }
}
