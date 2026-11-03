package com.algoprep.bootstrap.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.jayway.jsonpath.JsonPath;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
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

/**
 * #25, #25A and #25B against real PostgreSQL and Redis. Classes, codes and enrolments are seeded
 * with SQL because the instructor endpoints are separate work.
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
class StudentClassFlowTest {
  private static final String PASSWORD = "TempPass123";
  private static final String HASH = new BCryptPasswordEncoder().encode(PASSWORD);

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

  @Container
  static final GenericContainer<?> MAIL =
      new GenericContainer<>("axllent/mailpit:v1.31.5").withExposedPorts(1025, 8025);

  @DynamicPropertySource
  static void infrastructure(DynamicPropertyRegistry properties) {
    properties.add("spring.data.redis.host", REDIS::getHost);
    properties.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    properties.add("spring.mail.host", MAIL::getHost);
    properties.add("spring.mail.port", () -> MAIL.getMappedPort(1025));
  }

  @Autowired private WebApplicationContext context;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private StringRedisTemplate redis;
  private MockMvc mvc;

  @BeforeEach
  void setup() {
    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  @Test
  void joinEnrolsOnceAndIsIdempotentKeepingJoinedAt() throws Exception {
    UUID teacher = account("INSTRUCTOR", "Teacher One");
    UUID classId = clazz(teacher, "Algo 101", "desc", null);
    String code = inviteCode(classId, "1 hour");
    UUID student = account("STUDENT", "Student A");
    String access = access(student);

    MockHttpServletResponse first = join(access, code);

    assertThat(first.getStatus()).isEqualTo(200);
    assertThat(JsonPath.<String>read(first.getContentAsString(), "$.classId"))
        .isEqualTo(classId.toString());
    assertThat(JsonPath.<String>read(first.getContentAsString(), "$.className"))
        .isEqualTo("Algo 101");
    String joinedAt = enrolmentJoinedAt(classId, student);

    MockHttpServletResponse again = join(access, code);

    assertThat(again.getStatus()).isEqualTo(200);
    assertThat(JsonPath.<String>read(again.getContentAsString(), "$.classId"))
        .isEqualTo(classId.toString());
    assertThat(enrolmentRows(classId, student)).isEqualTo(1);
    assertThat(enrolmentJoinedAt(classId, student)).isEqualTo(joinedAt);
  }

  @Test
  void rejoinAfterLeavingReactivatesTheSameRowAndKeepsJoinedAt() throws Exception {
    UUID classId = clazz(account("INSTRUCTOR", "Teacher Two"), "Algo 102", null, null);
    String code = inviteCode(classId, "1 hour");
    UUID student = account("STUDENT", "Student B");
    String access = access(student);
    assertThat(join(access, code).getStatus()).isEqualTo(200);
    String joinedAt = enrolmentJoinedAt(classId, student);
    jdbc.update(
        "UPDATE identity.class_enrollments SET left_at = now() WHERE class_id = ? AND student_id"
            + " = ?",
        classId,
        student);

    MockHttpServletResponse rejoin = join(access, code);

    assertThat(rejoin.getStatus()).isEqualTo(200);
    assertThat(enrolmentRows(classId, student)).isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "SELECT left_at IS NULL FROM identity.class_enrollments WHERE class_id = ? AND"
                    + " student_id = ?",
                Boolean.class,
                classId,
                student))
        .isTrue();
    assertThat(enrolmentJoinedAt(classId, student)).isEqualTo(joinedAt);
  }

  @Test
  void expiredAndMalformedCodesAre422Idt410AndEnrolNobody() throws Exception {
    UUID classId = clazz(account("INSTRUCTOR", "Teacher Three"), "Algo 103", null, null);
    String expired = inviteCode(classId, "-1 minute");
    UUID student = account("STUDENT", "Student C");
    String access = access(student);

    for (String code : List.of(expired, "SHORT", "AB12CD3!", "")) {
      MockHttpServletResponse response = join(access, code);
      assertThat(response.getStatus()).isEqualTo(422);
      assertThat(JsonPath.<String>read(response.getContentAsString(), "$.errorCode"))
          .isEqualTo("IDT-410");
    }

    assertThat(enrolmentRows(classId, student)).isZero();
  }

  @Test
  void lowercaseCodeWithSurroundingSpacesIsNormalised() throws Exception {
    UUID classId = clazz(account("INSTRUCTOR", "Teacher Four"), "Algo 104", null, null);
    String code = inviteCode(classId, "1 hour");
    String access = access(account("STUDENT", "Student D"));

    MockHttpServletResponse response = join(access, "  " + code.toLowerCase() + "  ");

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(JsonPath.<String>read(response.getContentAsString(), "$.classId"))
        .isEqualTo(classId.toString());
  }

  @Test
  void fifthWrongCodeIs422AndTheSixthIs429EvenWithAValidCode() throws Exception {
    UUID classId = clazz(account("INSTRUCTOR", "Teacher Five"), "Algo 105", null, null);
    String valid = inviteCode(classId, "1 hour");
    UUID student = account("STUDENT", "Student E");
    String access = access(student);

    for (int i = 1; i <= 5; i++) {
      MockHttpServletResponse wrong = join(access, "ZZZZZZZ" + i);
      assertThat(wrong.getStatus()).isEqualTo(422);
      assertThat(JsonPath.<String>read(wrong.getContentAsString(), "$.errorCode"))
          .isEqualTo("IDT-410");
    }

    MockHttpServletResponse sixth = join(access, valid);
    assertThat(sixth.getStatus()).isEqualTo(429);
    assertThat(JsonPath.<String>read(sixth.getContentAsString(), "$.errorCode"))
        .isEqualTo("IDT-412");
    assertThat(enrolmentRows(classId, student)).isZero();
    assertThat(redis.getExpire("identity:class-join:failed:" + student)).isPositive();
  }

  @Test
  void successClearsTheFailureCounter() throws Exception {
    UUID classId = clazz(account("INSTRUCTOR", "Teacher Six"), "Algo 106", null, null);
    String valid = inviteCode(classId, "1 hour");
    UUID student = account("STUDENT", "Student F");
    String access = access(student);
    for (int i = 1; i <= 4; i++) {
      assertThat(join(access, "YYYYYYY" + i).getStatus()).isEqualTo(422);
    }

    assertThat(join(access, valid).getStatus()).isEqualTo(200);

    assertThat(redis.hasKey("identity:class-join:failed:" + student)).isFalse();
    for (int i = 1; i <= 4; i++) {
      assertThat(join(access, "XXXXXXX" + i).getStatus()).isEqualTo(422);
    }
  }

  @Test
  void listShowsOnlyActiveClassesNewestFirstWithAllFields() throws Exception {
    UUID teacher = account("INSTRUCTOR", "Teacher Seven");
    UUID older = clazz(teacher, "Older", "first", "{\"days\":[\"MON\"],\"time\":\"18:00\"}");
    UUID newer = clazz(teacher, "Newer", null, null);
    UUID left = clazz(teacher, "Left", null, null);
    UUID foreign = clazz(teacher, "Foreign", null, null);
    UUID student = account("STUDENT", "Student G");
    UUID other = account("STUDENT", "Student H");
    enrol(older, student, "2026-01-01T00:00:00Z", false);
    enrol(newer, student, "2026-02-01T00:00:00Z", false);
    enrol(left, student, "2026-03-01T00:00:00Z", true);
    enrol(foreign, other, "2026-04-01T00:00:00Z", false);

    MockHttpServletResponse response = list(access(student));

    assertThat(response.getStatus()).isEqualTo(200);
    String body = response.getContentAsString();
    assertThat(JsonPath.<List<String>>read(body, "$.items[*].name"))
        .containsExactly("Newer", "Older");
    assertThat(JsonPath.<String>read(body, "$.items[0].description")).isEmpty();
    assertThat(JsonPath.<Object>read(body, "$.items[0].schedule")).isNull();
    assertThat(JsonPath.<String>read(body, "$.items[0].instructorName")).isEqualTo("Teacher Seven");
    assertThat(JsonPath.<String>read(body, "$.items[0].joinedAt"))
        .isEqualTo("2026-02-01T00:00:00Z");
    assertThat(JsonPath.<String>read(body, "$.items[1].classId")).isEqualTo(older.toString());
    assertThat(JsonPath.<String>read(body, "$.items[1].description")).isEqualTo("first");
    assertThat(JsonPath.<String>read(body, "$.items[1].schedule.time")).isEqualTo("18:00");
    assertThat(JsonPath.<List<String>>read(body, "$.items[1].schedule.days"))
        .containsExactly("MON");
  }

  @Test
  void getOneIs200ForAMemberAnd404Idt411ForLeftForeignOrMalformed() throws Exception {
    UUID teacher = account("INSTRUCTOR", "Teacher Eight");
    UUID mine = clazz(teacher, "Mine", "desc", null);
    UUID left = clazz(teacher, "Left", null, null);
    UUID foreign = clazz(teacher, "Foreign", null, null);
    UUID student = account("STUDENT", "Student I");
    enrol(mine, student, "2026-01-01T00:00:00Z", false);
    enrol(left, student, "2026-01-02T00:00:00Z", true);
    enrol(foreign, account("STUDENT", "Student J"), "2026-01-03T00:00:00Z", false);
    String access = access(student);

    MockHttpServletResponse ok = one(access, mine.toString());
    assertThat(ok.getStatus()).isEqualTo(200);
    assertThat(JsonPath.<String>read(ok.getContentAsString(), "$.classId"))
        .isEqualTo(mine.toString());
    assertThat(JsonPath.<String>read(ok.getContentAsString(), "$.instructorName"))
        .isEqualTo("Teacher Eight");

    for (String id :
        List.of(left.toString(), foreign.toString(), UUID.randomUUID().toString(), "not-a-uuid")) {
      MockHttpServletResponse missing = one(access, id);
      assertThat(missing.getStatus()).isEqualTo(404);
      assertThat(JsonPath.<String>read(missing.getContentAsString(), "$.errorCode"))
          .isEqualTo("IDT-411");
    }
  }

  @Test
  void anInstructorTokenIsDeniedOnAllThreeEndpoints() throws Exception {
    UUID classId = clazz(account("INSTRUCTOR", "Teacher Nine"), "Algo 109", null, null);
    String code = inviteCode(classId, "1 hour");
    String access = access(account("INSTRUCTOR", "Teacher Ten"));

    assertThat(join(access, code).getStatus()).isEqualTo(403);
    assertThat(list(access).getStatus()).isEqualTo(403);
    assertThat(one(access, classId.toString()).getStatus()).isEqualTo(403);
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

  private UUID clazz(UUID instructor, String name, String description, String scheduleJson) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO identity.classes (id,instructor_id,name,description,schedule)"
            + " VALUES (?,?,?,?,?::jsonb)",
        id,
        instructor,
        name,
        description,
        scheduleJson);
    return id;
  }

  private String inviteCode(UUID classId, String validFor) {
    String code = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    jdbc.update(
        "INSERT INTO identity.class_invite_codes (id,class_id,code,expires_at,created_by)"
            + " SELECT ?,?,?,now() + ?::interval,instructor_id FROM identity.classes WHERE id=?",
        UUID.randomUUID(),
        classId,
        code,
        validFor,
        classId);
    return code;
  }

  private void enrol(UUID classId, UUID student, String joinedAt, boolean left) {
    jdbc.update(
        "INSERT INTO identity.class_enrollments (id,class_id,student_id,joined_at,left_at)"
            + " VALUES (?,?,?,?::timestamptz,?)",
        UUID.randomUUID(),
        classId,
        student,
        joinedAt,
        left ? java.sql.Timestamp.valueOf("2026-05-01 00:00:00") : null);
  }

  private int enrolmentRows(UUID classId, UUID student) {
    return jdbc.queryForObject(
        "SELECT count(*) FROM identity.class_enrollments WHERE class_id = ? AND student_id = ?",
        Integer.class,
        classId,
        student);
  }

  private String enrolmentJoinedAt(UUID classId, UUID student) {
    return jdbc.queryForObject(
        "SELECT joined_at::text FROM identity.class_enrollments WHERE class_id = ? AND student_id"
            + " = ?",
        String.class,
        classId,
        student);
  }

  private MockHttpServletResponse join(String access, String code) throws Exception {
    return mvc.perform(
            post("/api/v1/identity/classes/join")
                .header("Authorization", "Bearer " + access)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"" + code + "\"}"))
        .andReturn()
        .getResponse();
  }

  private MockHttpServletResponse list(String access) throws Exception {
    return mvc.perform(
            get("/api/v1/identity/me/classes").header("Authorization", "Bearer " + access))
        .andReturn()
        .getResponse();
  }

  private MockHttpServletResponse one(String access, String classId) throws Exception {
    return mvc.perform(
            get("/api/v1/identity/me/classes/" + classId)
                .header("Authorization", "Bearer " + access))
        .andReturn()
        .getResponse();
  }
}
