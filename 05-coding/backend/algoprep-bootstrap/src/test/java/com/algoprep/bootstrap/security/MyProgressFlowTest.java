package com.algoprep.bootstrap.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.jayway.jsonpath.JsonPath;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
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

/** Personal progress reads #46, #47, #48 and #55 (AC-40, AC-75) against real PostgreSQL. */
@SpringBootTest(
    properties = {
      "spring.autoconfigure.exclude=org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration",
      "spring.data.redis.timeout=300ms",
      "spring.data.redis.connect-timeout=300ms",
      "spring.datasource.hikari.maximum-pool-size=4"
    })
@ActiveProfiles("test")
@Testcontainers
class MyProgressFlowTest {
  private static final String PASSWORD = "TempPass123";
  private static final String HASH = new BCryptPasswordEncoder().encode(PASSWORD);
  private static final String BASE = "/api/v1/identity/me";

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

  @Autowired private WebApplicationContext context;
  @Autowired private JdbcTemplate jdbc;
  private MockMvc mvc;

  @BeforeEach
  void setup() {
    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  @Test
  void summaryCountsSolvedByBestVerdictAndExcludesSystemErrorsFromTheRate() throws Exception {
    UUID me = account();
    UUID other = account();
    best(me, UUID.randomUUID(), "ACCEPTED");
    best(me, UUID.randomUUID(), "ACCEPTED");
    best(me, UUID.randomUUID(), "WRONG_ANSWER");
    best(other, UUID.randomUUID(), "ACCEPTED");
    stats(me, "PYTHON", 10, 8, 5, 2);
    stats(other, "JAVA", 99, 99, 99, 99);

    MockHttpServletResponse r = call(me, BASE + "/progress/summary");

    assertThat(r.getStatus()).isEqualTo(200);
    String body = r.getContentAsString();
    assertThat(JsonPath.<Integer>read(body, "$.solvedProblemCount")).isEqualTo(2);
    assertThat(JsonPath.<Integer>read(body, "$.totalSubmissions")).isEqualTo(10);
    assertThat(JsonPath.<Integer>read(body, "$.acceptedCount")).isEqualTo(5);
    assertThat(JsonPath.<Integer>read(body, "$.acceptanceRatePercent")).isEqualTo(63);
    assertThat(JsonPath.<Integer>read(body, "$.firstTryAcceptedCount")).isEqualTo(2);
    assertThat(JsonPath.<String>read(body, "$.topLanguage")).isEqualTo("PYTHON");
  }

  @Test
  void summaryOfAUserWithNoDataIsZerosWithNullRateAndLanguage() throws Exception {
    MockHttpServletResponse r = call(account(), BASE + "/progress/summary");

    assertThat(r.getStatus()).isEqualTo(200);
    String body = r.getContentAsString();
    assertThat(JsonPath.<Integer>read(body, "$.solvedProblemCount")).isZero();
    assertThat(JsonPath.<Integer>read(body, "$.totalSubmissions")).isZero();
    assertThat(JsonPath.<Object>read(body, "$.acceptanceRatePercent")).isNull();
    assertThat(JsonPath.<Object>read(body, "$.topLanguage")).isNull();
  }

  @Test
  void recentActivityIsTenNewestOfTheLastNinetyDaysAndOnlyTheCallers() throws Exception {
    UUID me = account();
    UUID other = account();
    Instant now = Instant.now();
    for (int i = 1; i <= 12; i++) {
      activity(me, now.minus(i, ChronoUnit.HOURS));
    }
    activity(me, now.minus(91, ChronoUnit.DAYS));
    UUID foreignRef = activity(other, now.minus(1, ChronoUnit.MINUTES));

    MockHttpServletResponse r = call(me, BASE + "/recent-activity");

    assertThat(r.getStatus()).isEqualTo(200);
    String body = r.getContentAsString();
    List<String> times = JsonPath.read(body, "$.items[*].occurredAt");
    assertThat(times).hasSize(10);
    assertThat(JsonPath.<List<String>>read(body, "$.items[*].refId"))
        .doesNotContain(foreignRef.toString());
    assertThat(times.stream().map(Instant::parse).toList())
        .isSortedAccordingTo(java.util.Comparator.reverseOrder());
    assertThat(Instant.parse(times.get(0))).isAfter(now.minus(90, ChronoUnit.MINUTES));
    assertThat(JsonPath.<String>read(body, "$.items[0].activityType"))
        .isEqualTo("SUBMISSION_ACCEPTED");
    assertThat(JsonPath.<String>read(body, "$.items[0].refType")).isEqualTo("SUBMISSION");
  }

  @Test
  void calendarIs364ZeroFilledLocalDaysAndIgnoresOlderAndForeignRows() throws Exception {
    UUID me = account();
    UUID other = account();
    LocalDate today = Instant.now().atOffset(ZoneOffset.ofHours(7)).toLocalDate();
    LocalDate first = today.minusDays(363);
    day(me, today, 4);
    day(me, first, 1);
    day(me, first.minusDays(1), 7);
    day(other, today, 9);

    MockHttpServletResponse r = call(me, BASE + "/activity-calendar");

    assertThat(r.getStatus()).isEqualTo(200);
    String body = r.getContentAsString();
    assertThat(JsonPath.<String>read(body, "$.from")).isEqualTo(first.toString());
    assertThat(JsonPath.<String>read(body, "$.to")).isEqualTo(today.toString());
    assertThat(JsonPath.<List<Object>>read(body, "$.days")).hasSize(364);
    assertThat(JsonPath.<String>read(body, "$.days[0].day")).isEqualTo(first.toString());
    assertThat(JsonPath.<Integer>read(body, "$.days[0].submissionCount")).isEqualTo(1);
    assertThat(JsonPath.<Integer>read(body, "$.days[363].submissionCount")).isEqualTo(4);
    assertThat(JsonPath.<Integer>read(body, "$.days[100].submissionCount")).isZero();
  }

  @Test
  void solveStatesAnswerOnlyForAskedIdsAndOnlyForTheCaller() throws Exception {
    UUID me = account();
    UUID other = account();
    UUID solved = UUID.randomUUID();
    UUID trying = UUID.randomUUID();
    UUID untouched = UUID.randomUUID();
    UUID notAsked = UUID.randomUUID();
    best(me, solved, "ACCEPTED");
    best(me, trying, "TIME_LIMIT_EXCEEDED");
    best(me, notAsked, "ACCEPTED");
    best(other, untouched, "ACCEPTED");

    MockHttpServletResponse r =
        call(
            me,
            BASE + "/problem-solve-states?problemIds=" + solved + "," + trying + "," + untouched);

    assertThat(r.getStatus()).isEqualTo(200);
    String body = r.getContentAsString();
    assertThat(JsonPath.<java.util.Map<String, String>>read(body, "$.states"))
        .containsOnly(
            java.util.Map.entry(solved.toString(), "SOLVED"),
            java.util.Map.entry(trying.toString(), "IN_PROGRESS"));
  }

  @Test
  void solveStatesRejectMissingMalformedAndTooManyIdsWith422() throws Exception {
    UUID me = account();
    List<UUID> many = new ArrayList<>();
    for (int i = 0; i < 101; i++) {
      many.add(UUID.randomUUID());
    }
    String tooMany = String.join(",", many.stream().map(UUID::toString).toList());

    for (String url :
        new String[] {
          BASE + "/problem-solve-states",
          BASE + "/problem-solve-states?problemIds=",
          BASE + "/problem-solve-states?problemIds=abc",
          BASE + "/problem-solve-states?problemIds=" + tooMany
        }) {
      MockHttpServletResponse r = call(me, url);
      assertThat(r.getStatus()).as(url).isEqualTo(422);
      assertThat(JsonPath.<String>read(r.getContentAsString(), "$.errorCode")).isEqualTo("IDT-V27");
    }
  }

  @Test
  void everyEndpointNeedsASession() throws Exception {
    for (String path :
        new String[] {
          "/progress/summary",
          "/recent-activity",
          "/activity-calendar",
          "/problem-solve-states?problemIds=" + UUID.randomUUID()
        }) {
      assertThat(mvc.perform(get(BASE + path)).andReturn().getResponse().getStatus())
          .as(path)
          .isEqualTo(401);
    }
  }

  private MockHttpServletResponse call(UUID user, String url) throws Exception {
    return mvc.perform(get(url).header("Authorization", "Bearer " + access(user)))
        .andReturn()
        .getResponse();
  }

  private UUID account() {
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO identity.users (id,email,password_hash,display_name,role_id,status,"
            + "must_change_password) SELECT ?,?,?,'Progress User',id,'ACTIVE',false FROM"
            + " identity.roles WHERE code='STUDENT'",
        id,
        id + "@example.test",
        HASH);
    return id;
  }

  private String access(UUID userId) throws Exception {
    String email =
        jdbc.queryForObject("SELECT email FROM identity.users WHERE id = ?", String.class, userId);
    MockHttpServletResponse login =
        mvc.perform(
                post("/api/v1/identity/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}"))
            .andReturn()
            .getResponse();
    return JsonPath.read(login.getContentAsString(), "$.accessToken");
  }

  private void best(UUID user, UUID problem, String verdict) {
    jdbc.update(
        "INSERT INTO identity.user_problem_best_score (user_id, problem_id, best_ratio,"
            + " best_verdict) VALUES (?,?,?,?)",
        user,
        problem,
        "ACCEPTED".equals(verdict) ? 1 : 0.5,
        verdict);
  }

  /** Counters in column order: total, eligible, accepted, first try. */
  private void stats(UUID user, String lang, int... counters) {
    jdbc.update(
        "INSERT INTO identity.user_submission_stats (user_id, total_submissions,"
            + " ac_eligible_count, accepted_count, first_try_accepted_count, top_language)"
            + " VALUES (?,?,?,?,?,?)",
        user,
        counters[0],
        counters[1],
        counters[2],
        counters[3],
        lang);
  }

  private UUID activity(UUID user, Instant at) {
    UUID ref = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO identity.identity_recent_activity (id, actor_user_id, activity_type,"
            + " ref_type, ref_id, occurred_at) VALUES (?,?,'SUBMISSION_ACCEPTED','SUBMISSION',?,?)",
        UUID.randomUUID(),
        user,
        ref,
        java.sql.Timestamp.from(at));
    return ref;
  }

  private void day(UUID user, LocalDate day, int count) {
    jdbc.update(
        "INSERT INTO identity.user_daily_submission_stats (user_id, day, submission_count)"
            + " VALUES (?,?,?)",
        user,
        day,
        count);
  }
}
