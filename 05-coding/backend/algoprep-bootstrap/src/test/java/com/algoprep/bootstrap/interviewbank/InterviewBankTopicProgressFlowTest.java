package com.algoprep.bootstrap.interviewbank;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.algoprep.identity.application.ports.out.AccessTokenPort;
import com.algoprep.identity.domain.model.BaseCategory;
import com.jayway.jsonpath.JsonPath;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
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
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

/**
 * F6-4 topic progress on real PostgreSQL with real JWT authentication. Covers AC-22 and AC-23 for
 * endpoint 8 of the F6 DD. practice_history and user_answers are seeded with SQL because the
 * practice endpoints do not exist yet; retire and topic change go through the real admin API.
 */
@SpringBootTest(
    properties =
        "spring.autoconfigure.exclude=org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration")
@ActiveProfiles("test")
@Testcontainers
class InterviewBankTopicProgressFlowTest {
  private static final String PATH = "/api/v1/interview-bank/me/topic-progress";
  private static final String ADMIN_BASE = "/api/v1/interview-bank/admin/questions";
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
  private MockMvc mvc;
  private UUID aId;
  private UUID bId;
  private String a;
  private String b;

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
    aId = UUID.randomUUID();
    bId = UUID.randomUUID();
    a = token(aId, "STUDENT", BaseCategory.STUDENT);
    b = token(bId, "STUDENT", BaseCategory.STUDENT);
  }

  // Helpers -----------------------------------------------------------------------------------

  private String token(UUID id, String role, BaseCategory category) {
    jdbc.update(
        "INSERT INTO identity.users(id,email,display_name,role_id,status) SELECT ?,?, 'Progress actor',id,'ACTIVE' FROM identity.roles WHERE code=?",
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

  private String progress(String bearer) throws Exception {
    var response = call(get(PATH), bearer);
    assertThat(response.getStatus()).as(response.getContentAsString()).isEqualTo(200);
    return response.getContentAsString();
  }

  /** Reads one number of the row at the given topic position. */
  private static int num(String json, int row, String field) {
    return JsonPath.<Integer>read(json, "$.items[" + row + "]." + field);
  }

  private UUID question(int topic, String title, String status) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO interview_bank.interview_questions(id,topic_id,level_id,title,status,created_by) VALUES (?,?,?,?,?,?)",
        id,
        TOPIC[topic],
        LEVEL[0],
        title,
        status,
        UUID.randomUUID());
    return id;
  }

  /** Seeds an attempt row; the aggregate is seeded separately with history. */
  private void attempt(UUID user, UUID question) {
    jdbc.update(
        "INSERT INTO interview_bank.user_answers(id,user_id,question_id,answer_text,attempt_no,feedback_status,feedback_result_json,rubric_snapshot_json) VALUES (?,?,?,'answer',1,'COMPLETED','{}'::jsonb,'[]'::jsonb)",
        UUID.randomUUID(),
        user,
        question);
  }

  private void history(UUID user, int topic, int attempted) {
    jdbc.update(
        "INSERT INTO interview_bank.practice_history(id,user_id,topic_id,questions_attempted,last_practiced_at) VALUES (?,?,?,?,?)",
        UUID.randomUUID(),
        user,
        TOPIC[topic],
        attempted,
        attempted == 0 ? null : java.sql.Timestamp.from(Instant.parse("2026-11-01T10:00:00Z")));
  }

  private int attempted(UUID user, int topic) {
    return jdbc.queryForObject(
        "SELECT questions_attempted FROM interview_bank.practice_history WHERE user_id=? AND topic_id=?",
        Integer.class,
        user,
        TOPIC[topic]);
  }

  private String admin() {
    jdbc.update(
        "UPDATE identity.permissions p SET granted = (r.code = 'ADMIN') FROM identity.roles r, identity.functions f WHERE p.role_id = r.id AND p.function_id = f.id AND f.code = 'INTERVIEW_BANK_MANAGEMENT'");
    jdbc.update("UPDATE identity.roles SET permission_version = permission_version + 1");
    return token(UUID.randomUUID(), "ADMIN", BaseCategory.ADMIN);
  }

  private MockHttpServletResponse retire(UUID question, String bearer) throws Exception {
    return call(
        post(ADMIN_BASE + "/bulk/delete")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"ids\":[\"" + question + "\"]}"),
        bearer);
  }

  // AC-22 -------------------------------------------------------------------------------------

  @Test
  void ac22NewUserSeesEveryTopicInSortOrderWithZeroProgress() throws Exception {
    String json = progress(a);
    assertThat(JsonPath.<List<String>>read(json, "$.items[*].topicCode"))
        .containsExactly("CS_THEORY", "SYSTEM_DESIGN", "DATABASE", "LANGUAGE", "BEHAVIORAL");
    for (int i = 0; i < 5; i++) {
      assertThat(num(json, i, "questionsAttempted")).isZero();
      assertThat(num(json, i, "totalQuestions")).isZero();
      assertThat(num(json, i, "completionPercent")).isZero();
      assertThat(JsonPath.<Object>read(json, "$.items[" + i + "].lastPracticedAt")).isNull();
    }
    assertThat(JsonPath.<String>read(json, "$.items[0].topicName")).isEqualTo("Lý thuyết CS");
    assertThat(num(json, 4, "sortOrder")).isEqualTo(5);
  }

  @Test
  void ac22TopicsWithoutAttemptsShowTotalsAndNullLastPracticed() throws Exception {
    for (int i = 1; i <= 4; i++) {
      question(0, "CS " + i, "ACTIVE");
    }
    question(2, "DB 1", "ACTIVE");
    question(2, "DB retired", "RETIRED");
    String json = progress(a);
    assertThat(num(json, 0, "totalQuestions")).isEqualTo(4);
    assertThat(num(json, 0, "questionsAttempted")).isZero();
    assertThat(num(json, 0, "completionPercent")).isZero();
    assertThat(JsonPath.<Object>read(json, "$.items[0].lastPracticedAt")).isNull();
    assertThat(num(json, 1, "totalQuestions")).isZero();
    assertThat(num(json, 2, "totalQuestions")).isEqualTo(1);
  }

  @Test
  void ac22PercentIsRoundedAndLastPracticedIsReturned() throws Exception {
    for (int i = 1; i <= 10; i++) {
      question(0, "CS " + i, "ACTIVE");
    }
    for (int i = 1; i <= 3; i++) {
      question(1, "SD " + i, "ACTIVE");
    }
    history(aId, 0, 3);
    history(aId, 1, 2);
    String json = progress(a);
    assertThat(num(json, 0, "questionsAttempted")).isEqualTo(3);
    assertThat(num(json, 0, "totalQuestions")).isEqualTo(10);
    assertThat(num(json, 0, "completionPercent")).isEqualTo(30);
    assertThat(JsonPath.<String>read(json, "$.items[0].lastPracticedAt"))
        .isEqualTo("2026-11-01T10:00:00Z");
    assertThat(num(json, 1, "completionPercent")).isEqualTo(67);
  }

  @Test
  void ac22EachUserSeesOnlyTheirOwnProgressAndParametersAreIgnored() throws Exception {
    for (int i = 1; i <= 10; i++) {
      question(0, "CS " + i, "ACTIVE");
    }
    history(aId, 0, 3);
    history(bId, 0, 7);
    assertThat(num(progress(a), 0, "questionsAttempted")).isEqualTo(3);
    assertThat(num(progress(b), 0, "questionsAttempted")).isEqualTo(7);
    var forged = call(get(PATH).param("userId", bId.toString()), a);
    assertThat(num(forged.getContentAsString(), 0, "questionsAttempted")).isEqualTo(3);
    var stranger = token(UUID.randomUUID(), "STUDENT", BaseCategory.STUDENT);
    assertThat(num(progress(stranger), 0, "questionsAttempted")).isZero();
  }

  @Test
  void ac22OrderFollowsSortOrderNotInsertionOrder() throws Exception {
    jdbc.update("UPDATE interview_bank.question_topics SET sort_order = 0 WHERE id = ?", TOPIC[4]);
    jdbc.update("UPDATE interview_bank.question_topics SET sort_order = 9 WHERE id = ?", TOPIC[0]);
    assertThat(JsonPath.<List<String>>read(progress(a), "$.items[*].topicCode"))
        .containsExactly("BEHAVIORAL", "SYSTEM_DESIGN", "DATABASE", "LANGUAGE", "CS_THEORY");
  }

  @Test
  void ac22PercentIsClampedAtOneHundredAndZeroWithoutActiveQuestions() throws Exception {
    for (int i = 1; i <= 10; i++) {
      question(0, "CS " + i, "ACTIVE");
    }
    history(aId, 0, 12);
    history(aId, 3, 4);
    String json = progress(a);
    assertThat(num(json, 0, "questionsAttempted")).isEqualTo(12);
    assertThat(num(json, 0, "completionPercent")).isEqualTo(100);
    assertThat(num(json, 3, "totalQuestions")).isZero();
    assertThat(num(json, 3, "completionPercent")).isZero();
  }

  @Test
  void ac22AnyRoleMayCallAndMissingTokenIs401() throws Exception {
    assertThat(call(get(PATH), null).getStatus()).isEqualTo(401);
    assertThat(call(get(PATH), "not-a-token").getStatus()).isEqualTo(401);
    String instructor = token(UUID.randomUUID(), "INSTRUCTOR", BaseCategory.INSTRUCTOR);
    assertThat(call(get(PATH), instructor).getStatus()).isEqualTo(200);
    assertThat(call(get(PATH), admin()).getStatus()).isEqualTo(200);
  }

  // AC-23 -------------------------------------------------------------------------------------

  @Test
  void ac23RetiringAQuestionLowersNumeratorAndDenominatorTogether() throws Exception {
    UUID answered1 = question(0, "CS answered 1", "ACTIVE");
    UUID answered2 = question(0, "CS answered 2", "ACTIVE");
    UUID untouched = question(0, "CS untouched", "ACTIVE");
    question(0, "CS other", "ACTIVE");
    attempt(aId, answered1);
    attempt(aId, answered2);
    history(aId, 0, 2);
    assertThat(num(progress(a), 0, "completionPercent")).isEqualTo(50);

    String g = admin();
    assertThat(retire(answered1, g).getStatus()).isEqualTo(200);
    String json = progress(a);
    assertThat(num(json, 0, "questionsAttempted")).isEqualTo(1);
    assertThat(num(json, 0, "totalQuestions")).isEqualTo(3);
    assertThat(num(json, 0, "completionPercent")).isEqualTo(33);

    assertThat(retire(untouched, g).getStatus()).isEqualTo(200);
    json = progress(a);
    assertThat(num(json, 0, "questionsAttempted")).isEqualTo(1);
    assertThat(num(json, 0, "totalQuestions")).isEqualTo(2);
    assertThat(num(json, 0, "completionPercent")).isEqualTo(50);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM interview_bank.user_answers WHERE user_id = ?",
                Integer.class,
                aId))
        .isEqualTo(2);
  }

  @Test
  void ac23ChangingTheTopicMovesTheAttemptBetweenTopics() throws Exception {
    UUID moved = question(0, "CS moving", "ACTIVE");
    question(0, "CS stays", "ACTIVE");
    question(1, "SD one", "ACTIVE");
    attempt(aId, moved);
    history(aId, 0, 1);
    var update =
        call(
            put(ADMIN_BASE + "/" + moved)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"topicId\":\""
                        + TOPIC[1]
                        + "\",\"levelId\":\""
                        + LEVEL[0]
                        + "\",\"title\":\"CS moving\",\"rubric\":[]}"),
            admin());
    assertThat(update.getStatus()).as(update.getContentAsString()).isEqualTo(200);
    assertThat(attempted(aId, 0)).isZero();
    assertThat(attempted(aId, 1)).isEqualTo(1);
    String json = progress(a);
    assertThat(num(json, 0, "questionsAttempted")).isZero();
    assertThat(num(json, 0, "totalQuestions")).isEqualTo(1);
    assertThat(JsonPath.<Object>read(json, "$.items[0].lastPracticedAt")).isNull();
    assertThat(num(json, 1, "questionsAttempted")).isEqualTo(1);
    assertThat(num(json, 1, "totalQuestions")).isEqualTo(2);
    assertThat(num(json, 1, "completionPercent")).isEqualTo(50);
  }
}
