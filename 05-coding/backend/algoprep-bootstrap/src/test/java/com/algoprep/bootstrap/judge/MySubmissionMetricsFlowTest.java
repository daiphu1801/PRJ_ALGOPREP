package com.algoprep.bootstrap.judge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.algoprep.identity.application.ports.out.AccessTokenPort;
import com.algoprep.identity.domain.model.BaseCategory;
import com.algoprep.judge.application.ports.in.GetMyTopicSubmissionMetricsUseCase;
import com.algoprep.judge.application.ports.out.ProblemTopicReadPort;
import com.algoprep.judge.domain.ports.out.MySubmissionMetricsRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Real JWT/current role, PG, historical F2 links and no-cycle P5. No F1 feature changes. */
@SpringBootTest(
    properties =
        "spring.autoconfigure.exclude=org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration")
@ActiveProfiles("test")
@Testcontainers
class MySubmissionMetricsFlowTest {
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
  static void infrastructure(DynamicPropertyRegistry p) {
    p.add("spring.data.redis.host", REDIS::getHost);
    p.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
  }

  @Autowired JdbcTemplate jdbc;
  @Autowired StringRedisTemplate redis;
  @Autowired AccessTokenPort tokens;
  @Autowired WebApplicationContext context;
  @Autowired GetMyTopicSubmissionMetricsUseCase p5;
  @MockitoSpyBean ProblemTopicReadPort topicReader;
  @MockitoSpyBean MySubmissionMetricsRepository repository;
  private final JsonMapper json = JsonMapper.builder().findAndAddModules().build();
  private MockMvc mvc;
  private UUID student;
  private UUID problem;
  private Instant now;
  private LocalDate today;

  @BeforeEach
  void fixture() {
    SecurityContextHolder.clearContext();
    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    student = user("STUDENT");
    problem = UUID.randomUUID();
    now = jdbc.queryForObject("SELECT clock_timestamp()", OffsetDateTime.class).toInstant();
    today = now.atOffset(ZoneOffset.ofHours(7)).toLocalDate();
  }

  @AfterEach
  void clear() {
    SecurityContextHolder.clearContext();
  }

  private UUID user(String role) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO identity.users(id,email,display_name,role_id,status) SELECT ?,?,'Metric user',id,'ACTIVE' FROM identity.roles WHERE code=?",
        id,
        id + "@example.test",
        role);
    return id;
  }

  private String token(UUID id, BaseCategory role) {
    return tokens.issue(id, role, role.name(), UUID.randomUUID()).value();
  }

  private JsonNode request(String query, UUID actor, BaseCategory role, int status)
      throws Exception {
    var response =
        mvc.perform(
                get("/api/v1/judge/me/metrics" + query)
                    .header("Authorization", "Bearer " + token(actor, role)))
            .andReturn()
            .getResponse();
    assertThat(response.getStatus()).as(response.getContentAsString()).isEqualTo(status);
    return json.readTree(response.getContentAsString());
  }

  private JsonNode own(String query) throws Exception {
    return request(query, student, BaseCategory.STUDENT, 200);
  }

  private Instant day(int ago) {
    return ago == 0
        ? now
        : today
            .minusDays(ago)
            .atStartOfDay()
            .atOffset(ZoneOffset.ofHours(7))
            .toInstant()
            .plusSeconds(3600);
  }

  private void submission(UUID owner, UUID p, Instant at, String status) {
    boolean terminal = !List.of("PENDING", "COMPILING", "JUDGING").contains(status);
    jdbc.update(
        """
        INSERT INTO judge.submissions(id,user_id,problem_id,language,submission_model,source_code,
          source_input_method,status,failure_reason,score_ratio,testcase_set_version_at_grading,submitted_at,finished_at)
        VALUES(?,?,?,'JAVA','STANDARD_IO','private-source-secret','EDITOR',?,?,?,1,CAST(? AS timestamptz),CAST(? AS timestamptz))
        """,
        UUID.randomUUID(),
        owner,
        p,
        status,
        status.equals("SYSTEM_ERROR") ? "SANDBOX_FAULT" : null,
        status.equals("ACCEPTED") ? BigDecimal.ONE : null,
        at.toString(),
        terminal ? at.toString() : null);
  }

  @Test
  void emptyAccountReturnsZeroDaysAndExplicitNullWithoutAnUnrequestedTopicBlock() throws Exception {
    var result = own("?range=7d");
    assertThat(result.path("dailyCounts").size()).isEqualTo(7);
    assertThat(result.path("streakDays").asInt()).isZero();
    assertThat(result.path("lastSubmittedAt").isNull()).isTrue();
    assertThat(result.has("acRateByTopic")).isFalse();
    for (var item : result.path("dailyCounts")) {
      assertThat(item.path("count").asLong()).isZero();
    }
    assertThat(own("?range=all").path("dailyCounts").size()).isZero();
    verify(topicReader, never()).getProblemIdsByTopics(any());
  }

  @Test
  void rangesPreserveOwnCountsAndUserIdCannotSelectAnotherLearner() throws Exception {
    for (int ago : List.of(0, 1, 3, 29, 30, 41)) {
      submission(student, problem, day(ago), "PENDING");
    }
    UUID other = user("STUDENT");
    for (int i = 0; i < 4; i++) {
      submission(other, problem, day(0), "ACCEPTED");
    }
    var seven = own("?range=7d&userId=" + other);
    assertThat(sum(seven)).isEqualTo(3);
    assertThat(seven.path("streakDays").asInt()).isEqualTo(2);
    assertThat(Instant.parse(seven.path("lastSubmittedAt").asString())).isEqualTo(now);
    var thirty = own("?range=30d");
    assertThat(thirty.path("dailyCounts").size()).isEqualTo(14);
    assertThat(sum(thirty)).isEqualTo(4);
    var all = own("?range=all");
    assertThat(sum(all)).isEqualTo(6);
    assertThat(
            all.path("dailyCounts").get(all.path("dailyCounts").size() - 1).path("day").asString())
        .isEqualTo(
            today
                .with(
                    java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
                .toString());
    assertThat(all.toString()).doesNotContain("private-source-secret", "sourceCode", "testcase");
  }

  private long sum(JsonNode result) {
    long total = 0;
    for (var day : result.path("dailyCounts")) {
      total += day.path("count").asLong();
    }
    return total;
  }

  @Test
  void localMidnightAndStreakStopAtTheFirstMissingDay() throws Exception {
    Instant midnight = today.atStartOfDay().atOffset(ZoneOffset.ofHours(7)).toInstant();
    submission(student, problem, midnight.minusSeconds(1), "SYSTEM_ERROR");
    submission(student, problem, midnight, "COMPILING");
    submission(student, problem, day(3), "ACCEPTED");
    submission(student, problem, now.plusSeconds(86400), "ACCEPTED");
    var result = own("?range=7d");
    assertThat(result.path("streakDays").asInt()).isEqualTo(2);
    assertThat(result.path("dailyCounts").get(5).path("count").asLong()).isEqualTo(1);
    assertThat(result.path("dailyCounts").get(6).path("count").asLong()).isEqualTo(1);
    assertThat(sum(result)).isEqualTo(3);
  }

  private List<UUID> topicsWithHistoricalProblem() {
    UUID author = user("INSTRUCTOR");
    jdbc.update(
        "INSERT INTO problem.problems(id,code,title,author_id,updated_by,level_id,status,deleted,time_limit_ms,memory_limit_mb,max_output_size_kb) VALUES(?,?,'Historical metric fixture',?,?,'20000000-0000-4000-8000-000000000001','UNPUBLISHED',true,1000,256,64)",
        problem,
        problem.toString(),
        author,
        author);
    var topics = List.of(UUID.randomUUID(), UUID.randomUUID());
    for (UUID topic : topics) {
      jdbc.update(
          "INSERT INTO problem.topics(id,code,name,sort_order) VALUES(?,?,?,1)",
          topic,
          "t-" + topic,
          "Topic " + topic);
      jdbc.update(
          "INSERT INTO problem.problem_topics(problem_id,topic_id) VALUES(?,?)", problem, topic);
    }
    return topics;
  }

  @Test
  void historicalTopicLinksAreRealAndRatiosExcludeSystemWhileLastAttemptIncludesInflight()
      throws Exception {
    var topics = topicsWithHistoricalProblem();
    submission(student, problem, now.minusSeconds(30), "ACCEPTED");
    submission(student, problem, now.minusSeconds(20), "WRONG_ANSWER");
    submission(student, problem, now.minusSeconds(10), "SYSTEM_ERROR");
    submission(student, problem, now, "JUDGING");
    submission(student, problem, day(31), "ACCEPTED");
    submission(user("STUDENT"), problem, now, "ACCEPTED");
    UUID empty = UUID.randomUUID();
    var result =
        own(
            "?range=7d&topicIds="
                + topics.get(0)
                + "&topicIds="
                + topics.get(1)
                + "&topicIds="
                + empty);
    assertThat(result.path("acRateByTopic").size()).isEqualTo(3);
    for (var item : result.path("acRateByTopic")) {
      if (item.path("topicId").asString().equals(empty.toString())) {
        assertThat(item.path("submissionCount").asLong()).isZero();
        assertThat(item.path("acRate").isNull()).isTrue();
      } else {
        assertThat(item.path("submissionCount").asLong()).isEqualTo(2);
        assertThat(item.path("acceptedCount").asLong()).isEqualTo(1);
        assertThat(item.path("acRate").asDouble()).isEqualTo(.5);
        assertThat(Instant.parse(item.path("lastSubmittedAt").asString())).isEqualTo(now);
      }
    }
  }

  private void currentStudent(UUID id, String role) {
    var token = Jwt.withTokenValue("fixture").header("alg", "RS256").subject(id.toString()).build();
    SecurityContextHolder.getContext()
        .setAuthentication(
            new JwtAuthenticationToken(token, List.of(new SimpleGrantedAuthority("ROLE_" + role))));
  }

  @Test
  void p5CountsOnlyThePrincipalAndDoesNotCallBackToF2() {
    UUID a = UUID.randomUUID();
    UUID empty = UUID.randomUUID();
    submission(student, problem, day(1), "ACCEPTED");
    submission(student, problem, day(2), "COMPILE_ERROR");
    submission(user("STUDENT"), problem, day(1), "ACCEPTED");
    currentStudent(student, "STUDENT");
    var result = p5.readTopics(Map.of(a, Set.of(problem), empty, Set.of()));
    assertThat(result.get(a).submissionCount()).isEqualTo(2);
    assertThat(result.get(a).acRate()).isEqualTo(.5);
    assertThat(result.get(empty).acRate()).isNull();
    verify(topicReader, never()).getProblemIdsByTopics(any());
    currentStudent(student, "ADMIN");
    assertThatThrownBy(() -> p5.readTopics(Map.of(a, Set.of(problem))))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void authenticationAndValidationFailBeforeReadingProblemLinks() throws Exception {
    assertThat(
            mvc.perform(get("/api/v1/judge/me/metrics?range=7d"))
                .andReturn()
                .getResponse()
                .getStatus())
        .isEqualTo(401);
    request("?range=7d", user("ADMIN"), BaseCategory.ADMIN, 403);
    request("?range=7d", user("INSTRUCTOR"), BaseCategory.INSTRUCTOR, 403);
    for (String query : List.of("", "?range=1y", "?range=", "?range=7D")) {
      assertThat(request(query, student, BaseCategory.STUDENT, 400).path("errorCode").asString())
          .isEqualTo("JDG-V07");
    }
    assertThat(
            request("?range=7d&topicIds=private-invalid-topic", student, BaseCategory.STUDENT, 400)
                .path("errorCode")
                .asString())
        .isEqualTo("JDG-V15");
    String query = "?range=7d" + "&topicIds=".concat(UUID.randomUUID().toString()).repeat(51);
    assertThat(request(query, student, BaseCategory.STUDENT, 400).path("errorCode").asString())
        .isEqualTo("JDG-V15");
    verify(topicReader, never()).getProblemIdsByTopics(any());
  }

  @Test
  void readsDoNotWriteBusinessRowsAndFiftyTopicsRemainValid() throws Exception {
    submission(student, problem, day(0), "ACCEPTED");
    long before = jdbc.queryForObject("SELECT count(*) FROM judge.submissions", Long.class);
    var updated =
        jdbc.queryForObject("SELECT max(updated_at) FROM judge.submissions", OffsetDateTime.class);
    String query = "?range=all";
    for (int i = 0; i < 50; i++) {
      query += "&topicIds=" + UUID.randomUUID();
    }
    assertThat(own(query).path("acRateByTopic").size()).isEqualTo(50);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM judge.submissions", Long.class))
        .isEqualTo(before);
    assertThat(
            jdbc.queryForObject(
                "SELECT max(updated_at) FROM judge.submissions", OffsetDateTime.class))
        .isEqualTo(updated);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM judge.outbox_events", Long.class))
        .isZero();
  }

  @Test
  void dependencyFailureCannotReturnFalseZerosOrRawDataAndRecoveryWorks() throws Exception {
    doThrow(new IllegalStateException("private SQL password"))
        .when(repository)
        .days(any(), any(), any());
    var error = request("?range=7d", student, BaseCategory.STUDENT, 503);
    assertThat(error.path("errorCode").asText()).isEqualTo("JDG-507");
    assertThat(error.toString())
        .doesNotContain("private SQL", "sourceCode", "dailyCounts", "password");
    doCallRealMethod().when(repository).days(any(), any(), any());
    assertThat(own("?range=7d").path("dailyCounts").size()).isEqualTo(7);
    doThrow(new IllegalStateException("private F2 connection password"))
        .when(topicReader)
        .getProblemIdsByTopics(any());
    var topicError =
        request("?range=7d&topicIds=" + UUID.randomUUID(), student, BaseCategory.STUDENT, 503);
    assertThat(topicError.path("errorCode").asText()).isEqualTo("JDG-507");
    assertThat(topicError.toString())
        .doesNotContain("private F2", "password", "dailyCounts", "acRateByTopic");
  }
}
