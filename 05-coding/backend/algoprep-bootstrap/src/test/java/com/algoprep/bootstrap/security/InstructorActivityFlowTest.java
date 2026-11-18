package com.algoprep.bootstrap.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.jayway.jsonpath.JsonPath;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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

/** Instructor class feed #45 (AC-34) against real PostgreSQL. */
@SpringBootTest(
    properties = {
      "spring.autoconfigure.exclude=org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration",
      "spring.data.redis.timeout=300ms",
      "spring.data.redis.connect-timeout=300ms",
      "spring.datasource.hikari.maximum-pool-size=4"
    })
@ActiveProfiles("test")
@Testcontainers
class InstructorActivityFlowTest {
  private static final String PASSWORD = "TempPass123";
  private static final String HASH = new BCryptPasswordEncoder().encode(PASSWORD);
  private static final String URL = "/api/v1/identity/instructor/dashboard/recent-activity";

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
  void feedHoldsOnlyEntriesOfOwnedClassesNewestFirstCappedAtTenWithin90Days() throws Exception {
    UUID teacher = account("INSTRUCTOR", "Teacher One");
    UUID rival = account("INSTRUCTOR", "Teacher Two");
    UUID student = account("STUDENT", "Student A");
    UUID mine = clazz(teacher);
    UUID rivals = clazz(rival);
    Instant now = Instant.now();
    for (int i = 1; i <= 11; i++) {
      entry(student, mine, now.minus(i, ChronoUnit.HOURS));
    }
    entry(student, mine, now.minus(91, ChronoUnit.DAYS));
    UUID foreign = entry(student, rivals, now.minus(1, ChronoUnit.MINUTES));
    entry(student, null, now.minus(2, ChronoUnit.MINUTES));

    MockHttpServletResponse r = call(teacher, URL);

    assertThat(r.getStatus()).isEqualTo(200);
    String body = r.getContentAsString();
    List<String> times = JsonPath.read(body, "$.items[*].occurredAt");
    assertThat(times).hasSize(10);
    assertThat(times.stream().map(Instant::parse).toList())
        .isSortedAccordingTo(java.util.Comparator.reverseOrder());
    assertThat(JsonPath.<List<String>>read(body, "$.items[*].refId"))
        .doesNotContain(foreign.toString());
    assertThat(JsonPath.<List<String>>read(body, "$.items[*].scopeClassId"))
        .containsOnly(mine.toString());
    assertThat(JsonPath.<String>read(body, "$.items[0].actorDisplayName")).isEqualTo("Student A");
    assertThat(JsonPath.<String>read(body, "$.items[0].actorUserId")).isEqualTo(student.toString());
  }

  @Test
  void classIdNarrowsTheFeedToThatClass() throws Exception {
    UUID teacher = account("INSTRUCTOR", "Teacher One");
    UUID student = account("STUDENT", "Student A");
    UUID first = clazz(teacher);
    UUID second = clazz(teacher);
    Instant now = Instant.now();
    UUID inFirst = entry(student, first, now.minus(1, ChronoUnit.HOURS));
    entry(student, second, now.minus(2, ChronoUnit.HOURS));

    MockHttpServletResponse r = call(teacher, URL + "?classId=" + first);

    assertThat(r.getStatus()).isEqualTo(200);
    assertThat(JsonPath.<List<String>>read(r.getContentAsString(), "$.items[*].refId"))
        .containsExactly(inFirst.toString());
  }

  @Test
  void foreignMissingAndMalformedClassIdsAreTheSame404Idt414() throws Exception {
    UUID teacher = account("INSTRUCTOR", "Teacher One");
    UUID rival = account("INSTRUCTOR", "Teacher Two");
    UUID rivals = clazz(rival);

    for (String classId : new String[] {rivals.toString(), UUID.randomUUID().toString(), "abc"}) {
      MockHttpServletResponse r = call(teacher, URL + "?classId=" + classId);
      assertThat(r.getStatus()).as(classId).isEqualTo(404);
      assertThat(JsonPath.<String>read(r.getContentAsString(), "$.errorCode")).isEqualTo("IDT-414");
    }
  }

  @Test
  void aStudentIsForbiddenAndNoSessionIs401() throws Exception {
    UUID student = account("STUDENT", "Student A");

    assertThat(call(student, URL).getStatus()).isEqualTo(403);
    assertThat(mvc.perform(get(URL)).andReturn().getResponse().getStatus()).isEqualTo(401);
  }

  private MockHttpServletResponse call(UUID user, String url) throws Exception {
    return mvc.perform(get(url).header("Authorization", "Bearer " + access(user)))
        .andReturn()
        .getResponse();
  }

  private UUID account(String role, String name) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO identity.users (id,email,password_hash,display_name,role_id,status,"
            + "must_change_password) SELECT ?,?,?,?,id,'ACTIVE',false FROM identity.roles WHERE"
            + " code=?",
        id,
        id + "@example.test",
        HASH,
        name,
        role);
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

  private UUID clazz(UUID instructor) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO identity.classes (id,instructor_id,name) VALUES (?,?,'Algo')", id, instructor);
    return id;
  }

  /** Returns the {@code ref_id} so a test can tell the entry apart. */
  private UUID entry(UUID actor, UUID classId, Instant at) {
    UUID ref = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO identity.identity_recent_activity (id, actor_user_id, scope_class_id,"
            + " activity_type, ref_type, ref_id, occurred_at) VALUES"
            + " (?,?,?,'SUBMISSION_ACCEPTED','SUBMISSION',?,?)",
        UUID.randomUUID(),
        actor,
        classId,
        ref,
        Timestamp.from(at));
    return ref;
  }
}
