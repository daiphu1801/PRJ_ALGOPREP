package com.algoprep.bootstrap.interviewbank;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.algoprep.identity.application.ports.out.AccessTokenPort;
import com.algoprep.identity.domain.model.BaseCategory;
import com.jayway.jsonpath.JsonPath;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
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
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

/**
 * F6-3 learner study mode on real PostgreSQL (real unaccent) with real JWT authentication. Covers
 * AC-01 to AC-09 for endpoints 2 to 5 of the F6 DD.
 */
@SpringBootTest(
    properties =
        "spring.autoconfigure.exclude=org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration")
@ActiveProfiles("test")
@Testcontainers
class InterviewBankStudyFlowTest {
  private static final String BASE = "/api/v1/interview-bank/questions";
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
        "INSERT INTO identity.users(id,email,display_name,role_id,status) SELECT ?,?, 'Study actor',id,'ACTIVE' FROM identity.roles WHERE code=?",
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

  private MockHttpServletResponse list(String bearer, String query) throws Exception {
    // Parameters are added decoded so MockMvc does not encode a percent sign a second time.
    var request = get(BASE);
    if (!query.isEmpty()) {
      for (String pair : query.split("&")) {
        int eq = pair.indexOf('=');
        request.param(
            URLDecoder.decode(pair.substring(0, eq), StandardCharsets.UTF_8),
            URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8));
      }
    }
    return call(request, bearer);
  }

  private List<String> titles(String bearer, String query) throws Exception {
    var response = list(bearer, query);
    assertThat(response.getStatus()).as(response.getContentAsString()).isEqualTo(200);
    return JsonPath.read(response.getContentAsString(), "$.items[*].title");
  }

  private MockHttpServletResponse putJson(String path, String body, String bearer)
      throws Exception {
    return call(put(path).contentType(MediaType.APPLICATION_JSON).content(body), bearer);
  }

  private MockHttpServletResponse rate(UUID id, String rating, String bearer) throws Exception {
    return putJson(BASE + "/" + id + "/recall", "{\"rating\":\"" + rating + "\"}", bearer);
  }

  private MockHttpServletResponse mark(UUID id, boolean value, String bearer) throws Exception {
    return putJson(BASE + "/" + id + "/bookmark", "{\"bookmarked\":" + value + "}", bearer);
  }

  private void error(MockHttpServletResponse response, int status, String code) throws Exception {
    assertThat(response.getStatus()).as(response.getContentAsString()).isEqualTo(status);
    assertThat(JsonPath.<String>read(response.getContentAsString(), "$.errorCode")).isEqualTo(code);
  }

  private UUID question(int topic, int level, String title, String status) {
    return question(topic, level, title, status, new Body(null, null));
  }

  private record Body(String content, String keywords) {}

  private UUID question(int topic, int level, String title, String status, Body body) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO interview_bank.interview_questions(id,topic_id,level_id,title,content_markdown,core_keywords,suggested_approach,status,created_by)"
            + " VALUES (?,?,?,?,?,CAST(? AS jsonb),?,?,?)",
        id,
        TOPIC[topic],
        LEVEL[level],
        title,
        body.content(),
        body.keywords(),
        "Approach of " + title,
        status,
        UUID.randomUUID());
    return id;
  }

  private void overdue(UUID question, int days) {
    jdbc.update(
        "UPDATE interview_bank.recall_ratings SET next_review_at = now() - make_interval(days => ?) WHERE question_id = ?",
        days,
        question);
  }

  private void rubric(UUID question) {
    jdbc.update(
        "INSERT INTO interview_bank.answer_rubrics(id,question_id,criterion_code,description,weight_percent,sort_order) VALUES (?,?,'Do chinh xac','secret grading note',100,1)",
        UUID.randomUUID(),
        question);
  }

  private int count(String table, UUID user) {
    return jdbc.queryForObject(
        "SELECT count(*) FROM interview_bank." + table + " WHERE user_id = ?", Integer.class, user);
  }

  // AC-01 -------------------------------------------------------------------------------------

  @Test
  void ac01FiltersAreAndedAndCountsUseActiveOnly() throws Exception {
    for (int i = 1; i <= 6; i++) {
      question(1, 2, "SD hard " + i, "ACTIVE");
    }
    question(1, 0, "SD easy", "ACTIVE");
    question(2, 2, "DB hard", "ACTIVE");
    question(1, 2, "SD hard retired", "RETIRED");

    var response = list(a, "topic=SYSTEM_DESIGN&level=HARD");
    assertThat(response.getStatus()).isEqualTo(200);
    String body = response.getContentAsString();
    assertThat(JsonPath.<List<String>>read(body, "$.items[*].title"))
        .containsExactly(
            "SD hard 1", "SD hard 2", "SD hard 3", "SD hard 4", "SD hard 5", "SD hard 6");
    assertThat(JsonPath.<Integer>read(body, "$.meta.shown")).isEqualTo(6);
    assertThat(JsonPath.<Integer>read(body, "$.meta.total")).isEqualTo(8);
    assertThat(JsonPath.<Integer>read(body, "$.page.totalItems")).isEqualTo(6);
    assertThat(JsonPath.<Integer>read(body, "$.page.size")).isEqualTo(20);
    assertThat(JsonPath.<String>read(body, "$.items[0].topic.code")).isEqualTo("SYSTEM_DESIGN");
    assertThat(JsonPath.<String>read(body, "$.items[0].level.displayName")).isEqualTo("Khó");
    assertThat(body).doesNotContain("displayNo").doesNotContain("suggestedApproach");

    // The keyword narrows the same filter; total stays the whole bank.
    var narrowed = list(a, "topic=SYSTEM_DESIGN&level=HARD&q=hard%203").getContentAsString();
    assertThat(JsonPath.<List<String>>read(narrowed, "$.items[*].title"))
        .containsExactly("SD hard 3");
    assertThat(JsonPath.<Integer>read(narrowed, "$.meta.total")).isEqualTo(8);
  }

  @Test
  void ac01UnknownFilterValuesAreIgnoredNotRejected() throws Exception {
    question(0, 0, "Alpha", "ACTIVE");
    question(1, 1, "Beta", "ACTIVE");
    assertThat(
            titles(a, "topic=KHONG_CO&level=XYZ&status=xyz&page=-3&drill=maybe&bookmarkedOnly=x"))
        .containsExactly("Alpha", "Beta");
    assertThat(titles(a, "page=abc")).containsExactly("Alpha", "Beta");
  }

  @Test
  void ac01SearchWorksWithAndWithoutDiacritics() throws Exception {
    question(
        1, 1, "Cân bằng tải (load balancing)", "ACTIVE", new Body("Giải thích Round Robin", null));
    question(0, 0, "Đường đi ngắn nhất", "ACTIVE");
    question(2, 1, "Khoá luồng", "ACTIVE", new Body(null, "[\"Đồng bộ hoá\",\"mutex\"]"));
    question(2, 1, "Chỉ mục", "ACTIVE");

    assertThat(titles(a, "q=can%20bang")).containsExactly("Cân bằng tải (load balancing)");
    assertThat(titles(a, "q=C%C3%82N%20B%E1%BA%B0NG"))
        .containsExactly("Cân bằng tải (load balancing)");
    assertThat(titles(a, "q=b%E1%BA%B1ng%20t")).containsExactly("Cân bằng tải (load balancing)");
    assertThat(titles(a, "q=round")).containsExactly("Cân bằng tải (load balancing)");
    assertThat(titles(a, "q=load%20balancing")).containsExactly("Cân bằng tải (load balancing)");
    assertThat(titles(a, "q=duong%20di")).containsExactly("Đường đi ngắn nhất");
    assertThat(titles(a, "q=%C4%90%C6%AF%E1%BB%9CNG")).containsExactly("Đường đi ngắn nhất");
    assertThat(titles(a, "q=chi%20muc")).containsExactly("Chỉ mục");
    // Core keywords are searched too, with or without marks.
    assertThat(titles(a, "q=dong%20bo")).containsExactly("Khoá luồng");
    assertThat(titles(a, "q=%C4%90%E1%BB%93ng%20b%E1%BB%99")).containsExactly("Khoá luồng");
    assertThat(titles(a, "q=MUTEX")).containsExactly("Khoá luồng");
    assertThat(titles(a, "q=zzzz")).isEmpty();
  }

  @Test
  void ac01SearchTextIsDataNeverSqlOrPatternSyntax() throws Exception {
    question(0, 0, "Mutex lock", "ACTIVE", new Body(null, "[\"100%\"]"));
    question(0, 0, "Deadlock", "ACTIVE", new Body(null, "[\"1000\"]"));
    question(0, 0, "Hash map", "ACTIVE", new Body(null, "[\"a_b\"]"));
    question(0, 0, "Stack frame", "ACTIVE", new Body(null, "[\"axb\"]"));

    // Percent and underscore match literally in the keyword branch.
    assertThat(titles(a, "q=100%25")).containsExactly("Mutex lock");
    assertThat(titles(a, "q=a_b")).containsExactly("Hash map");
    // A query with no letter or digit has no usable token, so the search filter is dropped.
    assertThat(titles(a, "q=%25")).hasSize(4);
    assertThat(titles(a, "q=!!!%20%26%7C")).hasSize(4);
    // Injection-looking text is only data: it matches nothing and breaks nothing.
    assertThat(titles(a, "q=%27%3B%20DROP%20TABLE%20interview_questions%3B--")).isEmpty();
    assertThat(titles(a, "q=a%27%20%7C%20b%20%26%20!c%3A*")).isEmpty();
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM interview_bank.interview_questions", Integer.class))
        .isEqualTo(4);
    // Over-long text is cut to 100 characters instead of rejected.
    assertThat(list(a, "q=" + "x".repeat(150)).getStatus()).isEqualTo(200);
    String hundredOne = "mutex" + "a".repeat(96);
    assertThat(titles(a, "q=" + hundredOne)).isEmpty();
  }

  @Test
  void ac01PaginationIsTwentyPerPageOrderedByDisplayNo() throws Exception {
    for (int i = 1; i <= 25; i++) {
      question(0, 0, String.format("Q%02d", i), "ACTIVE");
    }
    String first = list(a, "").getContentAsString();
    assertThat(JsonPath.<List<String>>read(first, "$.items[*].title"))
        .hasSize(20)
        .startsWith("Q01")
        .endsWith("Q20");
    assertThat(JsonPath.<Integer>read(first, "$.page.totalPages")).isEqualTo(2);
    assertThat(JsonPath.<Integer>read(first, "$.page.number")).isEqualTo(1);
    assertThat(titles(a, "page=2")).containsExactly("Q21", "Q22", "Q23", "Q24", "Q25");
    // A page beyond the end serves the last page; a non-positive page serves the first.
    String beyond = list(a, "page=99").getContentAsString();
    assertThat(JsonPath.<Integer>read(beyond, "$.page.number")).isEqualTo(2);
    assertThat(JsonPath.<List<String>>read(beyond, "$.items[*].title")).hasSize(5);
    assertThat(titles(a, "page=0")).hasSize(20).startsWith("Q01");
    assertThat(titles(a, "page=-3")).hasSize(20).startsWith("Q01");
  }

  // AC-02 -------------------------------------------------------------------------------------

  @Test
  void ac02ReviewingIsNotTheSameAsDue() throws Exception {
    UUID forgotten = question(0, 0, "Forgotten today", "ACTIVE");
    UUID known = question(0, 0, "Known long ago", "ACTIVE");
    question(0, 0, "Never rated", "ACTIVE");
    assertThat(rate(forgotten, "FORGOTTEN", a).getStatus()).isEqualTo(200);
    assertThat(rate(known, "KNOWN", a).getStatus()).isEqualTo(200);
    jdbc.update(
        "UPDATE interview_bank.recall_ratings SET rated_at = now() - interval '10 days',"
            + " next_review_at = now() - interval '3 days' WHERE question_id = ?",
        known);

    assertThat(titles(a, "status=REVIEWING")).containsExactly("Forgotten today");
    assertThat(titles(a, "status=KNOWN")).containsExactly("Known long ago");
    assertThat(titles(a, "status=UNSEEN")).containsExactly("Never rated");
    assertThat(titles(a, "status=ALL")).hasSize(3);
    // Another learner has rated nothing, so everything is unseen for them.
    assertThat(titles(b, "status=UNSEEN")).hasSize(3);
    assertThat(titles(b, "status=REVIEWING")).isEmpty();
    String body = list(a, "status=KNOWN").getContentAsString();
    assertThat(JsonPath.<String>read(body, "$.items[0].recallRating")).isEqualTo("KNOWN");
  }

  // AC-03 -------------------------------------------------------------------------------------

  @Test
  void ac03DrillServesDueFirstThenUnseenWithStudyCards() throws Exception {
    UUID[] due = new UUID[3];
    for (int i = 0; i < 3; i++) {
      due[i] = question(0, 0, "Due " + i, "ACTIVE", new Body(null, "[\"kw" + i + "\"]"));
    }
    UUID notDue = question(0, 0, "Rated not due", "ACTIVE");
    for (int i = 0; i < 9; i++) {
      question(0, 0, "Unseen " + i, "ACTIVE", new Body(null, "[\"k\",\"w\"]"));
    }
    question(0, 0, "Retired unseen", "RETIRED");
    for (UUID id : due) {
      assertThat(rate(id, "VAGUE", a).getStatus()).isEqualTo(200);
    }
    assertThat(rate(notDue, "KNOWN", a).getStatus()).isEqualTo(200);
    // due[1] is the oldest, then due[2], then due[0].
    overdue(due[1], 5);
    overdue(due[2], 3);
    overdue(due[0], 1);

    var response = list(a, "drill=true");
    assertThat(response.getStatus()).isEqualTo(200);
    String body = response.getContentAsString();
    assertThat(JsonPath.<List<String>>read(body, "$.items[*].title"))
        .containsExactly(
            "Due 1",
            "Due 2",
            "Due 0",
            "Unseen 0",
            "Unseen 1",
            "Unseen 2",
            "Unseen 3",
            "Unseen 4",
            "Unseen 5",
            "Unseen 6");
    assertThat(JsonPath.<String>read(body, "$.items[0].suggestedApproach"))
        .isEqualTo("Approach of Due 1");
    assertThat(JsonPath.<List<String>>read(body, "$.items[0].coreKeywords")).containsExactly("kw1");
    assertThat(JsonPath.<Integer>read(body, "$.page.totalPages")).isEqualTo(1);
    assertThat(JsonPath.<Integer>read(body, "$.page.totalItems")).isEqualTo(10);
    assertThat(JsonPath.<Integer>read(body, "$.meta.total")).isEqualTo(13);

    assertThat(titles(a, "drill=true&limit=2")).containsExactly("Due 1", "Due 2");
    assertThat(titles(a, "drill=true&limit=99")).hasSize(13);
    assertThat(titles(a, "drill=true&limit=0")).hasSize(1);
    assertThat(titles(a, "drill=true&limit=abc")).hasSize(10);
    // The learner filter still applies to the drill deck.
    assertThat(titles(a, "drill=true&status=UNSEEN&limit=3"))
        .containsExactly("Unseen 0", "Unseen 1", "Unseen 2");
    // A learner with no ratings gets the unseen questions in display order.
    assertThat(titles(b, "drill=true&limit=4"))
        .containsExactly("Due 0", "Due 1", "Due 2", "Rated not due");
    // Without drill the cards carry no study content.
    assertThat(list(a, "").getContentAsString()).doesNotContain("coreKeywords");
  }

  // AC-04 -------------------------------------------------------------------------------------

  @Test
  void ac04RetiredQuestionVanishesButLearnerDataStays() throws Exception {
    UUID x = question(0, 0, "Soon retired", "ACTIVE");
    UUID y = question(0, 0, "Stays", "ACTIVE");
    assertThat(mark(x, true, a).getStatus()).isEqualTo(200);
    assertThat(rate(x, "VAGUE", a).getStatus()).isEqualTo(200);
    assertThat(titles(a, "")).containsExactly("Soon retired", "Stays");
    jdbc.update("UPDATE interview_bank.interview_questions SET status='RETIRED' WHERE id=?", x);

    assertThat(titles(a, "")).containsExactly("Stays");
    assertThat(titles(a, "bookmarkedOnly=true")).isEmpty();
    assertThat(titles(a, "drill=true")).containsExactly("Stays");
    assertThat(titles(a, "status=REVIEWING")).isEmpty();
    assertThat(JsonPath.<Integer>read(list(a, "").getContentAsString(), "$.meta.total"))
        .isEqualTo(1);
    error(call(get(BASE + "/" + x), a), 404, "INB-010");
    error(rate(x, "KNOWN", a), 404, "INB-010");
    error(mark(x, false, a), 404, "INB-010");
    // The learner's own records are untouched and the rejected calls changed nothing.
    assertThat(count("bookmarks", aId)).isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "SELECT rating FROM interview_bank.recall_ratings WHERE user_id=? AND question_id=?",
                String.class,
                aId,
                x))
        .isEqualTo("VAGUE");
    assertThat(y).isNotNull();
  }

  // AC-05 -------------------------------------------------------------------------------------

  @Test
  void ac05BookmarkSetsStateAndIsIdempotentPerUser() throws Exception {
    UUID x = question(0, 0, "Bookmark me", "ACTIVE");
    question(0, 0, "Other", "ACTIVE");

    var first = mark(x, true, a);
    assertThat(first.getStatus()).isEqualTo(200);
    assertThat(JsonPath.<Boolean>read(first.getContentAsString(), "$.bookmarked")).isTrue();
    assertThat(JsonPath.<String>read(first.getContentAsString(), "$.questionId"))
        .isEqualTo(x.toString());
    assertThat(mark(x, true, a).getStatus()).isEqualTo(200);
    assertThat(count("bookmarks", aId)).isEqualTo(1);
    assertThat(titles(a, "bookmarkedOnly=true")).containsExactly("Bookmark me");
    assertThat(JsonPath.<Boolean>read(list(a, "").getContentAsString(), "$.items[0].bookmarked"))
        .isTrue();

    // Learner B sees nothing of A's bookmark.
    assertThat(titles(b, "bookmarkedOnly=true")).isEmpty();
    assertThat(JsonPath.<Boolean>read(list(b, "").getContentAsString(), "$.items[0].bookmarked"))
        .isFalse();
    assertThat(
            JsonPath.<Boolean>read(
                call(get(BASE + "/" + x), b).getContentAsString(), "$.bookmarked"))
        .isFalse();
    assertThat(mark(x, false, b).getStatus()).isEqualTo(200);
    assertThat(count("bookmarks", aId)).isEqualTo(1);

    assertThat(mark(x, false, a).getStatus()).isEqualTo(200);
    assertThat(mark(x, false, a).getStatus()).isEqualTo(200);
    assertThat(count("bookmarks", aId)).isZero();
    assertThat(titles(a, "bookmarkedOnly=true")).isEmpty();
  }

  @Test
  void ac05BookmarkRejectsBadBodiesAndUnknownQuestions() throws Exception {
    UUID x = question(0, 0, "Target", "ACTIVE");
    String path = BASE + "/" + x + "/bookmark";
    error(putJson(path, "{}", a), 400, "INB-V05");
    error(putJson(path, "{\"bookmarked\":\"yes\"}", a), 400, "INB-V05");
    error(putJson(path, "{\"bookmarked\":null}", a), 400, "INB-V05");
    error(putJson(path, "not json", a), 400, "INB-V05");
    error(mark(UUID.randomUUID(), true, a), 404, "INB-010");
    error(putJson(BASE + "/not-a-uuid/bookmark", "{\"bookmarked\":true}", a), 404, "INB-010");
    assertThat(count("bookmarks", aId)).isZero();
  }

  // AC-06 and AC-07 ---------------------------------------------------------------------------

  @Test
  void ac06StudyDetailHasContentAndNoGradingData() throws Exception {
    UUID star =
        question(
            4,
            1,
            "Tell me about a conflict",
            "ACTIVE",
            new Body("Mo ta tinh huong", "[\"STAR\",\"conflict\"]"));
    jdbc.update(
        "UPDATE interview_bank.interview_questions SET sample_answer_framework='S T A R', follow_up_questions='[\"Why?\",\"What next?\"]'::jsonb WHERE id=?",
        star);
    UUID technical = question(0, 0, "Explain TCP", "ACTIVE");
    rubric(star);

    String body = call(get(BASE + "/" + star), a).getContentAsString();
    assertThat(JsonPath.<String>read(body, "$.topic.code")).isEqualTo("BEHAVIORAL");
    assertThat(JsonPath.<Boolean>read(body, "$.topic.usesStarFramework")).isTrue();
    assertThat(JsonPath.<String>read(body, "$.level.code")).isEqualTo("MEDIUM");
    assertThat(JsonPath.<String>read(body, "$.suggestedApproach"))
        .isEqualTo("Approach of Tell me about a conflict");
    assertThat(JsonPath.<String>read(body, "$.sampleAnswerFramework")).isEqualTo("S T A R");
    assertThat(JsonPath.<String>read(body, "$.contentMarkdown")).isEqualTo("Mo ta tinh huong");
    assertThat(JsonPath.<List<String>>read(body, "$.coreKeywords"))
        .containsExactly("STAR", "conflict");
    assertThat(JsonPath.<List<String>>read(body, "$.followUpQuestions"))
        .containsExactly("Why?", "What next?");
    assertThat(JsonPath.<Boolean>read(body, "$.practiceAvailable")).isTrue();
    assertThat(JsonPath.<Object>read(body, "$.recall")).isNull();
    assertThat(body)
        .doesNotContain("displayNo")
        .doesNotContain("secret grading note")
        .doesNotContain("rubric")
        .doesNotContain("status");

    String plain = call(get(BASE + "/" + technical), a).getContentAsString();
    assertThat(JsonPath.<Boolean>read(plain, "$.topic.usesStarFramework")).isFalse();
    assertThat(JsonPath.<List<String>>read(plain, "$.coreKeywords")).isEmpty();
    assertThat(JsonPath.<List<String>>read(plain, "$.followUpQuestions")).isEmpty();
    assertThat(JsonPath.<Object>read(plain, "$.sampleAnswerFramework")).isNull();
    assertThat(JsonPath.<Object>read(plain, "$.contentMarkdown")).isNull();
  }

  @Test
  void ac07QuestionWithoutRubricIsStillStudyableButNotPracticeable() throws Exception {
    UUID y = question(0, 0, "No rubric", "ACTIVE");
    var response = call(get(BASE + "/" + y), a);
    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(JsonPath.<Boolean>read(response.getContentAsString(), "$.practiceAvailable"))
        .isFalse();
    assertThat(JsonPath.<String>read(response.getContentAsString(), "$.title"))
        .isEqualTo("No rubric");
  }

  @Test
  void ac06DetailRejectsUnknownMalformedAndRetiredIds() throws Exception {
    UUID retired = question(0, 0, "Gone", "RETIRED");
    error(call(get(BASE + "/" + UUID.randomUUID()), a), 404, "INB-010");
    error(call(get(BASE + "/not-a-uuid"), a), 404, "INB-010");
    error(call(get(BASE + "/" + retired), a), 404, "INB-010");
  }

  @Test
  void anyAuthenticatedRoleMayStudy() throws Exception {
    UUID x = question(0, 0, "Open to all roles", "ACTIVE");
    String instructor = token(UUID.randomUUID(), "INSTRUCTOR", BaseCategory.INSTRUCTOR);
    String admin = token(UUID.randomUUID(), "ADMIN", BaseCategory.ADMIN);
    for (String bearer : List.of(instructor, admin)) {
      assertThat(list(bearer, "").getStatus()).isEqualTo(200);
      assertThat(call(get(BASE + "/" + x), bearer).getStatus()).isEqualTo(200);
      assertThat(rate(x, "KNOWN", bearer).getStatus()).isEqualTo(200);
      assertThat(mark(x, true, bearer).getStatus()).isEqualTo(200);
    }
  }

  @Test
  void withoutATokenEveryEndpointIs401() throws Exception {
    UUID x = question(0, 0, "Private", "ACTIVE");
    assertThat(list(null, "").getStatus()).isEqualTo(401);
    assertThat(call(get(BASE + "/" + x), null).getStatus()).isEqualTo(401);
    assertThat(rate(x, "KNOWN", null).getStatus()).isEqualTo(401);
    assertThat(mark(x, true, null).getStatus()).isEqualTo(401);
    assertThat(count("recall_ratings", aId)).isZero();
  }

  // AC-08 -------------------------------------------------------------------------------------

  @Test
  void ac08RatingSetsFixedIntervalFromThatRatingAndOverwrites() throws Exception {
    UUID x = question(0, 0, "Rate me", "ACTIVE");
    String[] ratings = {"FORGOTTEN", "VAGUE", "KNOWN"};
    int[] days = {1, 3, 7};
    for (int i = 0; i < 3; i++) {
      var response = rate(x, ratings[i], a);
      assertThat(response.getStatus()).as(response.getContentAsString()).isEqualTo(200);
      String body = response.getContentAsString();
      assertThat(JsonPath.<String>read(body, "$.rating")).isEqualTo(ratings[i]);
      assertThat(JsonPath.<Integer>read(body, "$.intervalDays")).isEqualTo(days[i]);
      Instant rated = Instant.parse(JsonPath.read(body, "$.ratedAt"));
      Instant next = Instant.parse(JsonPath.read(body, "$.nextReviewAt"));
      assertThat(Duration.between(rated, next)).isEqualTo(Duration.ofDays(days[i]));
      assertThat(Duration.between(rated, Instant.now()).abs()).isLessThan(Duration.ofMinutes(2));
      assertThat(count("recall_ratings", aId)).isEqualTo(1);
    }
    // Detail shows the current rating of the caller only.
    String detail = call(get(BASE + "/" + x), a).getContentAsString();
    assertThat(JsonPath.<String>read(detail, "$.recall.rating")).isEqualTo("KNOWN");
    assertThat(JsonPath.<Integer>read(detail, "$.recall.intervalDays")).isEqualTo(7);
    assertThat(JsonPath.<Object>read(call(get(BASE + "/" + x), b).getContentAsString(), "$.recall"))
        .isNull();
    assertThat(JsonPath.<Object>read(list(b, "").getContentAsString(), "$.items[0].recallRating"))
        .isNull();
  }

  @Test
  void ac08OneLearnerRatingNeverTouchesAnother() throws Exception {
    UUID x = question(0, 0, "Shared question", "ACTIVE");
    assertThat(rate(x, "KNOWN", a).getStatus()).isEqualTo(200);
    assertThat(rate(x, "FORGOTTEN", b).getStatus()).isEqualTo(200);
    assertThat(
            jdbc.queryForObject(
                "SELECT rating FROM interview_bank.recall_ratings WHERE user_id=?",
                String.class,
                aId))
        .isEqualTo("KNOWN");
    assertThat(
            jdbc.queryForObject(
                "SELECT rating FROM interview_bank.recall_ratings WHERE user_id=?",
                String.class,
                bId))
        .isEqualTo("FORGOTTEN");
    assertThat(titles(a, "status=KNOWN")).containsExactly("Shared question");
    assertThat(titles(b, "status=KNOWN")).isEmpty();
    assertThat(titles(b, "status=REVIEWING")).containsExactly("Shared question");
  }

  @Test
  void ac08RejectsBadRatingsAndUnknownQuestions() throws Exception {
    UUID x = question(0, 0, "Strict", "ACTIVE");
    String path = BASE + "/" + x + "/recall";
    error(putJson(path, "{\"rating\":\"GOOD\"}", a), 400, "INB-V04");
    error(putJson(path, "{\"rating\":\"known\"}", a), 400, "INB-V04");
    error(putJson(path, "{\"rating\":7}", a), 400, "INB-V04");
    error(putJson(path, "{}", a), 400, "INB-V04");
    error(putJson(path, "[", a), 400, "INB-V04");
    error(rate(UUID.randomUUID(), "KNOWN", a), 404, "INB-010");
    error(putJson(BASE + "/zzz/recall", "{\"rating\":\"KNOWN\"}", a), 404, "INB-010");
    assertThat(count("recall_ratings", aId)).isZero();
  }

  // AC-09 -------------------------------------------------------------------------------------

  @Test
  void ac09ConcurrentRatingsLeaveOneRowAndNoError() throws Exception {
    UUID x = question(0, 0, "Two tabs", "ACTIVE");
    int threads = 8;
    var pool = Executors.newFixedThreadPool(threads);
    var start = new CountDownLatch(1);
    var calls = new ArrayList<Callable<Integer>>();
    String[] ratings = {
      "KNOWN", "VAGUE", "FORGOTTEN", "KNOWN", "VAGUE", "FORGOTTEN", "KNOWN", "VAGUE"
    };
    for (int i = 0; i < threads; i++) {
      String rating = ratings[i];
      calls.add(
          () -> {
            start.await();
            return rate(x, rating, a).getStatus();
          });
    }
    var futures = new ArrayList<java.util.concurrent.Future<Integer>>();
    for (var task : calls) {
      futures.add(pool.submit(task));
    }
    start.countDown();
    for (var future : futures) {
      assertThat(future.get(30, TimeUnit.SECONDS)).isEqualTo(200);
    }
    pool.shutdown();
    assertThat(count("recall_ratings", aId)).isEqualTo(1);
    // The stored interval matches the stored rating, whichever call won.
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM interview_bank.recall_ratings WHERE user_id=? AND next_review_at - rated_at = make_interval(days => interval_days)",
                Integer.class,
                aId))
        .isEqualTo(1);
  }

  @Test
  void ac09ConcurrentBookmarksLeaveOneRow() throws Exception {
    UUID x = question(0, 0, "Double click", "ACTIVE");
    var pool = Executors.newFixedThreadPool(6);
    var start = new CountDownLatch(1);
    var futures = new ArrayList<java.util.concurrent.Future<Integer>>();
    for (int i = 0; i < 6; i++) {
      futures.add(
          pool.submit(
              () -> {
                start.await();
                return mark(x, true, a).getStatus();
              }));
    }
    start.countDown();
    for (var future : futures) {
      assertThat(future.get(30, TimeUnit.SECONDS)).isEqualTo(200);
    }
    pool.shutdown();
    assertThat(count("bookmarks", aId)).isEqualTo(1);
  }
}
