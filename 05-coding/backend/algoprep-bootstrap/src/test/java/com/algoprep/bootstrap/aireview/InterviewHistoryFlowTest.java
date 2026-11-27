package com.algoprep.bootstrap.aireview;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.algoprep.identity.application.ports.out.AccessTokenPort;
import com.algoprep.identity.domain.model.BaseCategory;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

/**
 * DD F5 section 31A: the caller's own COMPLETED and ABANDONED sessions as a searchable, filterable,
 * sortable page, on real PostgreSQL with real JWT authentication.
 */
@SpringBootTest(
    properties =
        "spring.autoconfigure.exclude=org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration")
@ActiveProfiles("test")
@Testcontainers
@Import(InterviewScoringFlowTest.Doubles.class)
class InterviewHistoryFlowTest {
  private static final String URL = "/api/v1/ai/me/interview-sessions";

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
    properties.add("spring.data.redis.timeout", () -> "2s");
  }

  @Autowired WebApplicationContext context;
  @Autowired AccessTokenPort tokens;
  @Autowired JdbcTemplate jdbc;
  @Autowired StringRedisTemplate redis;
  private MockMvc mvc;
  private UUID userId;
  private String student;

  @BeforeEach
  void reset() {
    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    jdbc.update(
        "TRUNCATE ai.rubric_scores, ai.interview_turns, ai.interview_sessions, ai.token_usage,"
            + " ai.outbox_events CASCADE");
    userId = UUID.randomUUID();
    student = token(userId);
  }

  // Helpers -----------------------------------------------------------------------------------

  private String token(UUID id) {
    jdbc.update(
        "INSERT INTO identity.users(id,email,display_name,role_id,status) SELECT ?,?,"
            + " 'History actor',id,'ACTIVE' FROM identity.roles WHERE code='STUDENT'",
        id,
        id + "@test.local");
    return tokens.issue(id, BaseCategory.STUDENT, "STUDENT", UUID.randomUUID()).value();
  }

  private MockHttpServletResponse list(String query, String bearer) throws Exception {
    var request = get(URL + query);
    if (bearer != null) {
      request.header("Authorization", "Bearer " + bearer);
    }
    return mvc.perform(request).andReturn().getResponse();
  }

  /** The search text goes through {@code param} so a literal percent sign is not re-encoded. */
  private MockHttpServletResponse search(String text) throws Exception {
    return mvc.perform(get(URL).param("q", text).header("Authorization", "Bearer " + student))
        .andReturn()
        .getResponse();
  }

  /**
   * A TOPIC_SELECTION session ended {@code hoursAgo} hours ago, scored when {@code scores} is
   * given.
   */
  private record Draft(String stage, String topics, String level, int hoursAgo) {}

  private UUID topicSession(UUID owner, Draft draft, String... scores) {
    String stage = draft.stage();
    String topics = draft.topics();
    String level = draft.level();
    int hoursAgo = draft.hoursAgo();
    UUID id = UUID.randomUUID();
    String when = "now() - interval '" + hoursAgo + " hours'";
    boolean closed = stage.equals("COMPLETED") || stage.equals("ABANDONED");
    jdbc.update(
        "INSERT INTO ai.interview_sessions(id,user_id,entry_type,topics,interviewer_level,"
            + "max_turns,hint_allowed,stage,turn_count,started_at,updated_at,ended_at) VALUES"
            + " (?,?,'TOPIC_SELECTION',CAST(? AS jsonb),?,8,true,?,3,"
            + when
            + ","
            + when
            + ","
            + (closed ? when : "NULL")
            + ")",
        id,
        owner,
        topics,
        level,
        stage);
    rubric(id, scores);
    return id;
  }

  private UUID bankSession(UUID owner, String text, String level, int hoursAgo, String... scores) {
    UUID id = UUID.randomUUID();
    String when = "now() - interval '" + hoursAgo + " hours'";
    jdbc.update(
        "INSERT INTO ai.interview_sessions(id,user_id,entry_type,question_ref,question_text,"
            + "interviewer_level,max_turns,hint_allowed,stage,turn_count,started_at,updated_at,"
            + "ended_at) VALUES (?,?,'QUESTION_BANK','q-1',?,?,8,true,'COMPLETED',4,"
            + when
            + ","
            + when
            + ","
            + when
            + ")",
        id,
        owner,
        text,
        level);
    rubric(id, scores);
    return id;
  }

  private void rubric(UUID session, String... scores) {
    String[] codes = {"CLARITY", "TECHNICAL_ACCURACY", "PUSHBACK_HANDLING", "COMPLEXITY_AWARENESS"};
    for (int i = 0; i < scores.length; i++) {
      jdbc.update(
          "INSERT INTO ai.rubric_scores(id,owner_type,owner_id,criterion_code,score,comment)"
              + " VALUES (?,'MOCK_INTERVIEW',?,?,?::numeric,'c')",
          UUID.randomUUID(),
          session,
          codes[i],
          scores[i]);
    }
  }

  private static List<String> ids(MockHttpServletResponse response) throws Exception {
    return JsonPath.read(response.getContentAsString(), "$.items[*].sessionId");
  }

  private static String code(MockHttpServletResponse response) throws Exception {
    return JsonPath.read(response.getContentAsString(), "$.errorCode");
  }

  // Tests -------------------------------------------------------------------------------------

  @Test
  void listsOnlyTheCallersFinishedSessionsNewestFirstAndAbandonedHasNoScore() throws Exception {
    UUID oldest =
        topicSession(
            userId, new Draft("COMPLETED", "[\"graphs\"]", "JUNIOR", 30), "8", "6", "7", "5");
    UUID abandoned = topicSession(userId, new Draft("ABANDONED", "[\"graphs\"]", "JUNIOR", 20));
    UUID newest = bankSession(userId, "What is a hash map?", "MIDDLE", 5, "9", "9", "9", "9");
    topicSession(userId, new Draft("EXPLAIN", "[\"graphs\"]", "JUNIOR", 1));
    topicSession(
        UUID.randomUUID(), new Draft("COMPLETED", "[\"graphs\"]", "JUNIOR", 2), "9", "9", "9", "9");

    MockHttpServletResponse response = list("", student);

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(ids(response))
        .containsExactly(newest.toString(), abandoned.toString(), oldest.toString());
    String body = response.getContentAsString();
    assertThat((Integer) JsonPath.read(body, "$.page.totalItems")).isEqualTo(3);
    assertThat((Integer) JsonPath.read(body, "$.page.totalPages")).isEqualTo(1);
    assertThat(JsonPath.<Number>read(body, "$.items[0].overallScore").doubleValue()).isEqualTo(9.0);
    assertThat(JsonPath.<Object>read(body, "$.items[1].overallScore")).isNull();
    assertThat(JsonPath.<Number>read(body, "$.items[2].overallScore").doubleValue())
        .isEqualTo(6.55);
    assertThat((String) JsonPath.read(body, "$.items[0].questionText"))
        .isEqualTo("What is a hash map?");
    assertThat((List<String>) JsonPath.read(body, "$.items[2].topics")).containsExactly("graphs");
  }

  @Test
  void filtersByStatusScoreBandLevelAndEntryType() throws Exception {
    UUID weak =
        topicSession(
            userId, new Draft("COMPLETED", "[\"graphs\"]", "JUNIOR", 9), "2", "2", "2", "2");
    UUID fair =
        topicSession(userId, new Draft("COMPLETED", "[\"dp\"]", "SENIOR", 8), "5", "5", "5", "5");
    UUID good = bankSession(userId, "Explain CAP", "MIDDLE", 7, "9", "9", "9", "9");
    UUID abandoned = topicSession(userId, new Draft("ABANDONED", "[\"dp\"]", "JUNIOR", 6));

    assertThat(ids(list("?status=abandoned", student))).containsExactly(abandoned.toString());
    assertThat(ids(list("?scoreBand=weak", student))).containsExactly(weak.toString());
    assertThat(ids(list("?scoreBand=fair", student))).containsExactly(fair.toString());
    assertThat(ids(list("?scoreBand=good", student))).containsExactly(good.toString());
    assertThat(ids(list("?level=senior", student))).containsExactly(fair.toString());
    assertThat(ids(list("?entryType=QUESTION_BANK", student))).containsExactly(good.toString());
    assertThat(ids(list("?scoreBand=weak&level=JUNIOR&status=COMPLETED", student)))
        .containsExactly(weak.toString());
  }

  @Test
  void searchesTheQuestionAndTheTopicsAndTreatsPercentAsText() throws Exception {
    UUID byTopic =
        topicSession(
            userId,
            new Draft("COMPLETED", "[\"Dynamic Programming\"]", "JUNIOR", 3),
            "5",
            "5",
            "5",
            "5");
    UUID byQuestion =
        bankSession(userId, "Why use 100% of the cache?", "MIDDLE", 2, "6", "6", "6", "6");
    bankSession(userId, "Unrelated", "MIDDLE", 1, "6", "6", "6", "6");

    assertThat(ids(list("?q=programming", student))).containsExactly(byTopic.toString());
    assertThat(ids(list("?q=HASH", student))).isEmpty();
    assertThat(ids(search("100%"))).containsExactly(byQuestion.toString());
    assertThat(ids(search("%"))).containsExactly(byQuestion.toString());
  }

  @Test
  void sortsByScoreWithUnscoredLastAndPagesWithTotals() throws Exception {
    UUID low =
        topicSession(userId, new Draft("COMPLETED", "[\"a\"]", "JUNIOR", 5), "2", "2", "2", "2");
    UUID high =
        topicSession(userId, new Draft("COMPLETED", "[\"b\"]", "JUNIOR", 4), "9", "9", "9", "9");
    UUID none = topicSession(userId, new Draft("ABANDONED", "[\"c\"]", "JUNIOR", 3));

    assertThat(ids(list("?sort=score&order=desc", student)))
        .containsExactly(high.toString(), low.toString(), none.toString());
    assertThat(ids(list("?sort=score&order=asc", student)))
        .containsExactly(low.toString(), high.toString(), none.toString());
    assertThat(ids(list("?sort=startedAt&order=asc", student)))
        .containsExactly(low.toString(), high.toString(), none.toString());

    MockHttpServletResponse second = list("?size=2&page=2", student);
    assertThat(ids(second)).hasSize(1);
    String body = second.getContentAsString();
    assertThat((Integer) JsonPath.read(body, "$.page.number")).isEqualTo(2);
    assertThat((Integer) JsonPath.read(body, "$.page.size")).isEqualTo(2);
    assertThat((Integer) JsonPath.read(body, "$.page.totalItems")).isEqualTo(3);
    assertThat((Integer) JsonPath.read(body, "$.page.totalPages")).isEqualTo(2);
    assertThat(ids(list("?size=2&page=9", student))).isEmpty();
  }

  @Test
  void aSessionWithThreeRubricRowsHasNoScoreAndStaysOutOfTheBandFilters() throws Exception {
    UUID half = topicSession(userId, new Draft("COMPLETED", "[\"a\"]", "JUNIOR", 2), "9", "9", "9");

    assertThat(ids(list("", student))).containsExactly(half.toString());
    assertThat(ids(list("?scoreBand=good", student))).isEmpty();
  }

  @Test
  void badParametersAreRefusedWithTheirOwnCodesAndAnonymousIsRefused() throws Exception {
    assertThat(list("?size=51", student).getStatus()).isEqualTo(400);
    assertThat(code(list("?size=51", student))).isEqualTo("AIR-V20");
    assertThat(code(list("?page=0", student))).isEqualTo("AIR-V20");
    assertThat(code(list("?page=abc", student))).isEqualTo("AIR-V20");
    assertThat(code(list("?scoreBand=great", student))).isEqualTo("AIR-V21");
    assertThat(code(list("?status=EXPLAIN", student))).isEqualTo("AIR-V21");
    assertThat(code(list("?level=boss", student))).isEqualTo("AIR-V21");
    assertThat(code(list("?entryType=x", student))).isEqualTo("AIR-V21");
    assertThat(code(list("?sort=turns", student))).isEqualTo("AIR-V21");
    assertThat(code(list("?order=up", student))).isEqualTo("AIR-V21");
    assertThat(list("", null).getStatus()).isEqualTo(401);
  }
}
