package com.algoprep.bootstrap.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.algoprep.identity.domain.ports.out.ClassEventPort;
import com.jayway.jsonpath.JsonPath;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
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
 * Instructor class endpoints #19 to #24, #26 and #27 against real PostgreSQL and Redis. No RabbitMQ
 * container: the event port is replaced by a recording bean, so the test checks what would be
 * published (type, ids, occurredAt) without a broker.
 */
@SpringBootTest(
    properties = {
      "spring.autoconfigure.exclude=org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration",
      "spring.data.redis.timeout=300ms",
      "spring.data.redis.connect-timeout=300ms",
      "spring.datasource.hikari.maximum-pool-size=4"
    })
@ActiveProfiles("test")
@Testcontainers
class ClassManagementFlowTest {
  private static final String PASSWORD = "TempPass123";
  private static final String HASH = new BCryptPasswordEncoder().encode(PASSWORD);
  private static final String BASE = "/api/v1/identity/classes";

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

  /** Keeps what the handlers asked to publish; replaces the AMQP adapter in this context. */
  @TestConfiguration
  static class RecordingEvents {
    record Event(String type, UUID classId, UUID studentId, Instant occurredAt) {}

    static final List<Event> EVENTS = new CopyOnWriteArrayList<>();

    @Bean
    @Primary
    ClassEventPort recordingClassEvents() {
      return new ClassEventPort() {
        @Override
        public void classDeleted(UUID classId, Instant occurredAt) {
          EVENTS.add(new Event("ClassDeleted", classId, null, occurredAt));
        }

        @Override
        public void studentRemoved(UUID classId, UUID studentId, Instant occurredAt) {
          EVENTS.add(new Event("StudentRemovedFromClass", classId, studentId, occurredAt));
        }
      };
    }
  }

  @Autowired private WebApplicationContext context;
  @Autowired private JdbcTemplate jdbc;
  private MockMvc mvc;

  @BeforeEach
  void setup() {
    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    RecordingEvents.EVENTS.clear();
  }

  @Test
  void createMakesTheClassAndItsFirstCodeThenListShowsItWithItsStudentCount() throws Exception {
    String token = login(account("INSTRUCTOR"));

    MockHttpServletResponse created =
        send(
            post(BASE),
            token,
            "{\"name\":\"  Algo K21  \",\"description\":\"Intro\",\"schedule\":{\"days\":[3,1],"
                + "\"startTime\":\"09:00\",\"endTime\":\"10:30\",\"room\":\"B2.01\"}}");

    assertThat(created.getStatus()).isEqualTo(201);
    String body = created.getContentAsString();
    String classId = JsonPath.read(body, "$.classId");
    assertThat(JsonPath.<String>read(body, "$.name")).isEqualTo("Algo K21");
    assertThat(JsonPath.<List<Integer>>read(body, "$.schedule.days")).containsExactly(1, 3);
    assertThat(JsonPath.<String>read(body, "$.inviteCode.code"))
        .matches("[ABCDEFGHJKLMNPQRSTUVWXYZ2-9]{8}");
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM identity.class_invite_codes WHERE class_id = ?::uuid",
                Integer.class,
                classId))
        .isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "SELECT expires_at > now() + interval '6 days 23 hours' FROM"
                    + " identity.class_invite_codes WHERE class_id = ?::uuid",
                Boolean.class,
                classId))
        .isTrue();

    UUID student = UUID.fromString(idOf(account("STUDENT")));
    enrol(UUID.fromString(classId), student);
    MockHttpServletResponse list = send(get(BASE + "/my"), token, null);

    assertThat(list.getStatus()).isEqualTo(200);
    String items = list.getContentAsString();
    assertThat(JsonPath.<String>read(items, "$.items[0].id")).isEqualTo(classId);
    assertThat(JsonPath.<Integer>read(items, "$.items[0].studentCount")).isEqualTo(1);
    assertThat(JsonPath.<String>read(items, "$.items[0].createdAt")).endsWith("Z");
    assertThat(items).doesNotContain("completionPct", "avgScore", "absentCount");
  }

  @Test
  void aClassOfSomeoneElseOrWithABadIdIs404Idt414OnEveryEndpoint() throws Exception {
    String owner = login(account("INSTRUCTOR"));
    String stranger = login(account("INSTRUCTOR"));
    String classId = createClass(owner, "Private");
    String student = idOf(account("STUDENT"));

    List<MockHttpServletRequestBuilder> calls =
        List.of(
            patch(BASE + "/" + classId),
            delete(BASE + "/" + classId),
            post(BASE + "/" + classId + "/invite-codes"),
            get(BASE + "/" + classId + "/invite-codes"),
            get(BASE + "/" + classId + "/students"),
            post(BASE + "/" + classId + "/students/" + student + "/leave"),
            patch(BASE + "/not-a-uuid"),
            get(BASE + "/not-a-uuid/students"));
    for (MockHttpServletRequestBuilder call : calls) {
      MockHttpServletResponse response = send(call, stranger, "{}");
      assertThat(response.getStatus()).as(response.getContentAsString()).isEqualTo(404);
      assertThat(JsonPath.<String>read(response.getContentAsString(), "$.errorCode"))
          .isEqualTo("IDT-414");
    }
    assertThat(RecordingEvents.EVENTS).isEmpty();
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM identity.classes WHERE id = ?::uuid", Integer.class, classId))
        .isEqualTo(1);
  }

  @Test
  void studentsAreRefusedOnInstructorEndpointsWith403Idt445() throws Exception {
    String student = login(account("STUDENT"));

    MockHttpServletResponse response = send(get(BASE + "/my"), student, null);

    assertThat(response.getStatus()).isEqualTo(403);
    assertThat(JsonPath.<String>read(response.getContentAsString(), "$.errorCode"))
        .isEqualTo("IDT-445");
    assertThat(send(post(BASE), student, "{\"name\":\"x\"}").getStatus()).isEqualTo(403);
    assertThat(mvc.perform(get(BASE + "/my")).andReturn().getResponse().getStatus()).isEqualTo(401);
  }

  @Test
  void everyInstructorEndpointNeedsItsOwnCellAndRefusesWith403Idt445WhenItIsRevoked()
      throws Exception {
    String token = login(account("INSTRUCTOR"));
    String classId = createClass(token, "Guarded");
    String student = idOf(account("STUDENT"));
    String one = BASE + "/" + classId;
    String[][] needs = {
      {"READ", "GET", BASE + "/my"},
      {"READ", "GET", one + "/invite-codes"},
      {"READ", "GET", one + "/students"},
      {"CREATE", "POST", BASE},
      {"CREATE", "POST", one + "/invite-codes"},
      {"UPDATE", "PATCH", one},
      {"UPDATE", "POST", one + "/students/" + student + "/leave"},
      {"DELETE", "DELETE", one},
    };

    for (String[] need : needs) {
      setInstructorCell(need[0], false);
      try {
        MockHttpServletResponse response =
            send(request(need[1], need[2]), token, "{\"name\":\"x\"}");

        assertThat(response.getStatus()).as(need[1] + " " + need[2]).isEqualTo(403);
        assertThat(JsonPath.<String>read(response.getContentAsString(), "$.errorCode"))
            .as(need[1] + " " + need[2])
            .isEqualTo("IDT-445");
      } finally {
        setInstructorCell(need[0], true);
      }
    }
    assertThat(send(get(BASE + "/my"), token, null).getStatus()).isEqualTo(200);
  }

  private static MockHttpServletRequestBuilder request(String method, String path) {
    return switch (method) {
      case "GET" -> get(path);
      case "POST" -> post(path);
      case "PATCH" -> patch(path);
      default -> delete(path);
    };
  }

  /** Flips one CLASS_MANAGEMENT cell of INSTRUCTOR and bumps the version so the change applies. */
  private void setInstructorCell(String action, boolean granted) {
    jdbc.update(
        "UPDATE identity.permissions p SET granted = ? FROM identity.roles r, identity.functions f,"
            + " identity.actions a WHERE r.id = p.role_id AND f.id = p.function_id AND a.id ="
            + " p.action_id AND r.code = 'INSTRUCTOR' AND f.code = 'CLASS_MANAGEMENT' AND a.code = ?",
        granted,
        action);
    jdbc.update(
        "UPDATE identity.roles SET permission_version = permission_version + 1 WHERE code ="
            + " 'INSTRUCTOR'");
  }

  @Test
  void patchChangesOnlyWhatIsSentAndValidationFailuresChangeNothing() throws Exception {
    String token = login(account("INSTRUCTOR"));
    String classId = createClass(token, "Original");

    MockHttpServletResponse named =
        send(patch(BASE + "/" + classId), token, "{\"name\":\"Renamed\"}");
    assertThat(named.getStatus()).isEqualTo(200);
    assertThat(JsonPath.<String>read(named.getContentAsString(), "$.name")).isEqualTo("Renamed");
    assertThat(JsonPath.<String>read(named.getContentAsString(), "$.description"))
        .isEqualTo("Desc");
    assertThat(JsonPath.<String>read(named.getContentAsString(), "$.schedule.room"))
        .isEqualTo("A1");

    MockHttpServletResponse cleared =
        send(patch(BASE + "/" + classId), token, "{\"description\":\"\",\"schedule\":{}}");
    assertThat(cleared.getStatus()).isEqualTo(200);
    assertThat(JsonPath.<Object>read(cleared.getContentAsString(), "$.description")).isNull();
    assertThat(JsonPath.<Object>read(cleared.getContentAsString(), "$.schedule")).isNull();
    assertThat(
            jdbc.queryForObject(
                "SELECT schedule IS NULL FROM identity.classes WHERE id = ?::uuid",
                Boolean.class,
                classId))
        .isTrue();

    String[][] bad = {
      {"{\"name\":\"  \"}", "IDT-V14"},
      {"{\"description\":\"" + "d".repeat(501) + "\"}", "IDT-V15"},
      {"{\"schedule\":{\"room\":\"" + "r".repeat(101) + "\"}}", "IDT-V10"},
      {"{\"schedule\":{\"days\":[1],\"startTime\":\"10:00\",\"endTime\":\"09:00\"}}", "IDT-V16"},
      {"{\"schedule\":{\"startDate\":\"2026-09-10\",\"endDate\":\"2026-09-01\"}}", "IDT-V16"}
    };
    for (String[] c : bad) {
      MockHttpServletResponse response = send(patch(BASE + "/" + classId), token, c[0]);
      assertThat(response.getStatus()).as(c[0]).isEqualTo(422);
      assertThat(JsonPath.<String>read(response.getContentAsString(), "$.errorCode"))
          .isEqualTo(c[1]);
    }
    assertThat(
            jdbc.queryForObject(
                "SELECT name FROM identity.classes WHERE id = ?::uuid", String.class, classId))
        .isEqualTo("Renamed");
    assertThat(send(post(BASE), token, "{\"name\":\"\"}").getStatus()).isEqualTo(422);
  }

  @Test
  void inviteCodesPileUpInParallelAndOnlyUnexpiredOnesAreListedNewestFirst() throws Exception {
    String token = login(account("INSTRUCTOR"));
    String classId = createClass(token, "Codes");

    MockHttpServletResponse second =
        send(post(BASE + "/" + classId + "/invite-codes"), token, null);
    assertThat(second.getStatus()).isEqualTo(201);
    String secondCode = JsonPath.read(second.getContentAsString(), "$.code");
    jdbc.update(
        "INSERT INTO identity.class_invite_codes (id,class_id,code,expires_at,created_by)"
            + " SELECT gen_random_uuid(), id, 'EXPIRED2', now() - interval '1 minute',"
            + " instructor_id FROM identity.classes WHERE id = ?::uuid",
        classId);

    MockHttpServletResponse list = send(get(BASE + "/" + classId + "/invite-codes"), token, null);

    assertThat(list.getStatus()).isEqualTo(200);
    String body = list.getContentAsString();
    assertThat(JsonPath.<List<String>>read(body, "$.items[*].code")).hasSize(2);
    assertThat(JsonPath.<String>read(body, "$.items[0].code")).isEqualTo(secondCode);
    assertThat(body).doesNotContain("EXPIRED2");
    assertThat(JsonPath.<String>read(body, "$.items[0].expiresAt")).endsWith("Z");
  }

  @Test
  void rosterListsActiveStudentsWithContactAndANullStatus() throws Exception {
    String token = login(account("INSTRUCTOR"));
    String classId = createClass(token, "Roster");
    String active = account("STUDENT");
    String left = account("STUDENT");
    enrol(UUID.fromString(classId), UUID.fromString(idOf(active)));
    enrol(UUID.fromString(classId), UUID.fromString(idOf(left)));
    jdbc.update(
        "UPDATE identity.class_enrollments SET left_at = now() WHERE student_id = ?::uuid",
        idOf(left));
    jdbc.update("UPDATE identity.users SET phone = '0901 234' WHERE email = ?", active);

    MockHttpServletResponse response = send(get(BASE + "/" + classId + "/students"), token, null);

    assertThat(response.getStatus()).isEqualTo(200);
    String body = response.getContentAsString();
    assertThat(JsonPath.<List<String>>read(body, "$.items")).hasSize(1);
    assertThat(JsonPath.<String>read(body, "$.items[0].email")).isEqualTo(active);
    assertThat(JsonPath.<String>read(body, "$.items[0].displayName")).isEqualTo("QAUser123");
    assertThat(JsonPath.<String>read(body, "$.items[0].phone")).isEqualTo("0901 234");
    assertThat(JsonPath.<String>read(body, "$.items[0].joinedAt")).endsWith("Z");
    assertThat(JsonPath.<Object>read(body, "$.items[0].status")).isNull();
  }

  @Test
  void leaveIsIdempotentKeepsTheFirstMomentAndRepublishesWithIt() throws Exception {
    String token = login(account("INSTRUCTOR"));
    UUID classId = UUID.fromString(createClass(token, "Leave"));
    UUID student = UUID.fromString(idOf(account("STUDENT")));
    enrol(classId, student);
    String path = BASE + "/" + classId + "/students/" + student + "/leave";

    MockHttpServletResponse first = send(post(path), token, null);
    Instant stored =
        jdbc.queryForObject(
                "SELECT left_at FROM identity.class_enrollments WHERE student_id = ?",
                OffsetDateTime.class,
                student)
            .toInstant();
    MockHttpServletResponse again = send(post(path), token, null);

    assertThat(first.getStatus()).isEqualTo(200);
    assertThat(again.getStatus()).isEqualTo(200);
    assertThat(Instant.parse(JsonPath.read(first.getContentAsString(), "$.leftAt")))
        .isEqualTo(stored);
    assertThat(Instant.parse(JsonPath.read(again.getContentAsString(), "$.leftAt")))
        .isEqualTo(stored);
    assertThat(
            jdbc.queryForObject(
                    "SELECT left_at FROM identity.class_enrollments WHERE student_id = ?",
                    OffsetDateTime.class,
                    student)
                .toInstant())
        .isEqualTo(stored);
    assertThat(RecordingEvents.EVENTS)
        .containsExactly(
            new RecordingEvents.Event("StudentRemovedFromClass", classId, student, stored),
            new RecordingEvents.Event("StudentRemovedFromClass", classId, student, stored));
    assertThat(
            JsonPath.<List<String>>read(
                send(get(BASE + "/" + classId + "/students"), token, null).getContentAsString(),
                "$.items"))
        .isEmpty();

    UUID never = UUID.fromString(idOf(account("STUDENT")));
    MockHttpServletResponse stranger =
        send(post(BASE + "/" + classId + "/students/" + never + "/leave"), token, null);
    assertThat(stranger.getStatus()).isEqualTo(404);
    assertThat(RecordingEvents.EVENTS).hasSize(2);
  }

  @Test
  void deleteRemovesTheClassWithItsCodesAndEnrolmentsThenAnnouncesIt() throws Exception {
    String token = login(account("INSTRUCTOR"));
    UUID classId = UUID.fromString(createClass(token, "Doomed"));
    UUID student = UUID.fromString(idOf(account("STUDENT")));
    enrol(classId, student);

    MockHttpServletResponse response = send(delete(BASE + "/" + classId), token, null);

    assertThat(response.getStatus()).isEqualTo(204);
    assertThat(count("classes", classId)).isZero();
    assertThat(count("class_invite_codes", classId)).isZero();
    assertThat(count("class_enrollments", classId)).isZero();
    assertThat(RecordingEvents.EVENTS).hasSize(1);
    RecordingEvents.Event event = RecordingEvents.EVENTS.get(0);
    assertThat(event.type()).isEqualTo("ClassDeleted");
    assertThat(event.classId()).isEqualTo(classId);
    assertThat(event.occurredAt()).isBetween(Instant.now().minusSeconds(60), Instant.now());
    assertThat(send(delete(BASE + "/" + classId), token, null).getStatus()).isEqualTo(404);
    assertThat(RecordingEvents.EVENTS).hasSize(1);
  }

  private int count(String table, UUID classId) {
    String column = "classes".equals(table) ? "id" : "class_id";
    return jdbc.queryForObject(
        "SELECT count(*) FROM identity." + table + " WHERE " + column + " = ?",
        Integer.class,
        classId);
  }

  private String createClass(String token, String name) throws Exception {
    MockHttpServletResponse response =
        send(
            post(BASE),
            token,
            "{\"name\":\"" + name + "\",\"description\":\"Desc\",\"schedule\":{\"room\":\"A1\"}}");
    assertThat(response.getStatus()).isEqualTo(201);
    return JsonPath.read(response.getContentAsString(), "$.classId");
  }

  private void enrol(UUID classId, UUID studentId) {
    jdbc.update(
        "INSERT INTO identity.class_enrollments (id, class_id, student_id) VALUES (?, ?, ?)",
        UUID.randomUUID(),
        classId,
        studentId);
  }

  /** An ACTIVE account of the given role code; returns its email. */
  private String account(String role) {
    String email = UUID.randomUUID() + "@example.test";
    jdbc.update(
        "INSERT INTO identity.users (id,email,password_hash,display_name,role_id,status,"
            + "must_change_password) SELECT ?,?,?,'QAUser123',id,'ACTIVE',false"
            + " FROM identity.roles WHERE code=?",
        UUID.randomUUID(),
        email,
        HASH,
        role);
    return email;
  }

  private String idOf(String email) {
    return jdbc.queryForObject(
        "SELECT id FROM identity.users WHERE email = ?", String.class, email);
  }

  private String login(String email) throws Exception {
    MockHttpServletResponse response =
        mvc.perform(
                post("/api/v1/identity/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}"))
            .andReturn()
            .getResponse();
    assertThat(response.getStatus()).isEqualTo(200);
    return JsonPath.read(response.getContentAsString(), "$.accessToken");
  }

  private MockHttpServletResponse send(
      MockHttpServletRequestBuilder request, String token, String json) throws Exception {
    MockHttpServletRequestBuilder builder = request.header("Authorization", "Bearer " + token);
    if (json != null) {
      builder.contentType(MediaType.APPLICATION_JSON).content(json);
    }
    return mvc.perform(builder).andReturn().getResponse();
  }
}
