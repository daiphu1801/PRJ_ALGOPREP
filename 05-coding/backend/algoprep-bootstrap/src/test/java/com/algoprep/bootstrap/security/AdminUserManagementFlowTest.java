package com.algoprep.bootstrap.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.algoprep.identity.application.ports.out.EmailPort;
import com.jayway.jsonpath.JsonPath;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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
 * Admin user management, #35 to #38, #50, #62, #63, against real PostgreSQL and Redis with real
 * tokens from {@code /auth/login}. The mail port is a mock so a temporary password can be read from
 * the message and proven to work at the next login.
 */
@SpringBootTest(
    properties = {
      "spring.autoconfigure.exclude=org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration",
      "spring.data.redis.timeout=300ms",
      "spring.data.redis.connect-timeout=300ms",
      "spring.datasource.hikari.maximum-pool-size=6"
    })
@ActiveProfiles("test")
@Testcontainers
class AdminUserManagementFlowTest {
  private static final String PASSWORD = "TempPass123";
  private static final String HASH = new BCryptPasswordEncoder().encode(PASSWORD);
  private static final String BASE = "/api/v1/identity/admin/users";

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
  @MockitoBean private EmailPort mail;
  private MockMvc mvc;

  @BeforeEach
  void setup() {
    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  /** Puts the ADMIN role's user-management cells back, whatever a test did. */
  @AfterEach
  void restoreCells() {
    jdbc.update(
        "UPDATE identity.permissions SET granted = true WHERE role_id ="
            + " (SELECT id FROM identity.roles WHERE code = 'ADMIN')");
    jdbc.update("UPDATE identity.roles SET permission_version = permission_version + 1");
  }

  // ---- gates ---------------------------------------------------------------------------------

  @Test
  void everyEndpointRefusesAnonymousStudentAndInstructorCallers() throws Exception {
    String student = login(account("STUDENT", "ACTIVE", HASH));
    String instructor = login(account("INSTRUCTOR", "ACTIVE", HASH));
    String target = userId(account("STUDENT", "ACTIVE", HASH));
    for (String token : List.of(student, instructor)) {
      for (Endpoint endpoint : endpoints(target)) {
        MockHttpServletResponse response = send(endpoint.request(), token, "{}");
        assertThat(response.getStatus()).as(endpoint.label()).isEqualTo(403);
        assertThat(JsonPath.<String>read(response.getContentAsString(), "$.errorCode"))
            .isEqualTo("IDT-445");
      }
    }
    for (Endpoint endpoint : endpoints(target)) {
      assertThat(mvc.perform(endpoint.request()).andReturn().getResponse().getStatus())
          .as(endpoint.label())
          .isEqualTo(401);
    }
  }

  @Test
  void everyEndpointNeedsItsOwnCellAndRefusesWith403Idt445WhenItIsRevoked() throws Exception {
    String admin = login(account("ADMIN", "ACTIVE", HASH));
    String target = userId(account("STUDENT", "ACTIVE", HASH));
    for (Endpoint endpoint : endpoints(target)) {
      setCell(endpoint.action(), false);
      MockHttpServletResponse response = send(endpoint.request(), admin, "{}");
      setCell(endpoint.action(), true);
      assertThat(response.getStatus()).as(endpoint.label()).isEqualTo(403);
      assertThat(JsonPath.<String>read(response.getContentAsString(), "$.errorCode"))
          .isEqualTo("IDT-445");
    }
  }

  // ---- #35, #62, #63 ---------------------------------------------------------------------------

  @Test
  void theListFiltersPagesAndSearchesLiterally() throws Exception {
    String admin = login(account("ADMIN", "ACTIVE", HASH));
    String tag = "zq" + UUID.randomUUID().toString().substring(0, 8);
    String student = insert("STUDENT", "ACTIVE", HASH, tag + "_s@example.test", "Alpha " + tag);
    String locked = insert("STUDENT", "DEACTIVATED", HASH, tag + "_d@example.test", "Beta " + tag);
    insert("INSTRUCTOR", "ACTIVE", null, tag + "_i@example.test", "Gamma " + tag);
    insert("STUDENT", "ACTIVE", HASH, tag + "-x@example.test", "Percent 100% " + tag);

    MockHttpServletResponse all = send(get(BASE + "?q=" + tag), admin, null);
    assertThat(all.getStatus()).isEqualTo(200);
    String body = all.getContentAsString();
    assertThat(JsonPath.<Integer>read(body, "$.total")).isEqualTo(4);
    assertThat(JsonPath.<Integer>read(body, "$.page")).isEqualTo(1);
    assertThat(JsonPath.<Integer>read(body, "$.pageSize")).isEqualTo(20);
    assertThat(JsonPath.<String>read(body, "$.items[0].displayName"))
        .isEqualTo("Percent 100% " + tag);

    String active =
        send(get(BASE + "?q=" + tag + "&status=ACTIVE"), admin, null).getContentAsString();
    assertThat(JsonPath.<Integer>read(active, "$.total")).isEqualTo(3);
    String lockedOnly =
        send(get(BASE + "?q=" + tag + "&status=DEACTIVATED"), admin, null).getContentAsString();
    assertThat(JsonPath.<List<String>>read(lockedOnly, "$.items[*].email"))
        .containsExactly(tag + "_d@example.test");
    assertThat(JsonPath.<Boolean>read(lockedOnly, "$.items[0].anonymized")).isFalse();
    String instructors =
        send(get(BASE + "?q=" + tag + "&role=INSTRUCTOR"), admin, null).getContentAsString();
    assertThat(JsonPath.<Integer>read(instructors, "$.total")).isEqualTo(1);
    assertThat(JsonPath.<Boolean>read(instructors, "$.items[0].hasPassword")).isFalse();
    assertThat(JsonPath.<String>read(instructors, "$.items[0].baseCategory"))
        .isEqualTo("INSTRUCTOR");

    String paged =
        send(get(BASE + "?q=" + tag + "&pageSize=2&page=2"), admin, null).getContentAsString();
    assertThat(JsonPath.<List<Object>>read(paged, "$.items")).hasSize(2);
    assertThat(JsonPath.<Integer>read(paged, "$.total")).isEqualTo(4);

    // "%" is a literal, not a wildcard, and a keyword is matched on name or e-mail.
    String percent = send(get(BASE).param("q", tag + "%"), admin, null).getContentAsString();
    assertThat(JsonPath.<Integer>read(percent, "$.total")).isZero();
    String literal = send(get(BASE).param("q", "100%"), admin, null).getContentAsString();
    assertThat(JsonPath.<List<String>>read(literal, "$.items[*].displayName"))
        .contains("Percent 100% " + tag);
    String underscore = send(get(BASE).param("q", tag + "_"), admin, null).getContentAsString();
    assertThat(JsonPath.<Integer>read(underscore, "$.total")).isEqualTo(3);
    assertThat(userId(student)).isNotBlank();
    assertThat(userId(locked)).isNotBlank();
  }

  @Test
  void theListCarriesSolvedAndSubmissionCountsFromTheReadModelsAndZeroWhenThereIsNoRow()
      throws Exception {
    String admin = login(account("ADMIN", "ACTIVE", HASH));
    String bare = account("STUDENT", "ACTIVE", HASH);
    String busy = account("STUDENT", "ACTIVE", HASH);
    UUID busyId = UUID.fromString(userId(busy));
    // Two accepted problems, one failed, and a counter row; a user with only a counter has 0
    // solved.
    for (String verdict : List.of("ACCEPTED", "ACCEPTED", "WRONG_ANSWER")) {
      jdbc.update(
          "INSERT INTO identity.user_problem_best_score (user_id, problem_id, best_ratio,"
              + " best_verdict) VALUES (?,?,?,?)",
          busyId,
          UUID.randomUUID(),
          "ACCEPTED".equals(verdict) ? 1 : 0.5,
          verdict);
    }
    jdbc.update(
        "INSERT INTO identity.user_submission_stats (user_id, total_submissions,"
            + " ac_eligible_count, accepted_count, first_try_accepted_count) VALUES (?,7,6,2,1)",
        busyId);

    String bareBody = send(get(BASE).param("q", bare), admin, null).getContentAsString();
    String busyBody = send(get(BASE).param("q", busy), admin, null).getContentAsString();

    assertThat(JsonPath.<Integer>read(bareBody, "$.items[0].solvedCount")).isZero();
    assertThat(JsonPath.<Integer>read(bareBody, "$.items[0].totalSubmissions")).isZero();
    assertThat(JsonPath.<Integer>read(busyBody, "$.items[0].solvedCount")).isEqualTo(2);
    assertThat(JsonPath.<Integer>read(busyBody, "$.items[0].totalSubmissions")).isEqualTo(7);
  }

  @Test
  void theListCarriesTheLastActivityOfAnAccountThatLoggedIn() throws Exception {
    String admin = login(account("ADMIN", "ACTIVE", HASH));
    String never = account("STUDENT", "ACTIVE", HASH);
    String active = account("STUDENT", "ACTIVE", HASH);
    login(active);

    String neverBody = send(get(BASE).param("q", never), admin, null).getContentAsString();
    String activeBody = send(get(BASE).param("q", active), admin, null).getContentAsString();

    assertThat(neverBody).contains("\"lastActiveAt\":null");
    String stamp = JsonPath.read(activeBody, "$.items[0].lastActiveAt");
    assertThat(Instant.parse(stamp))
        .isBetween(Instant.now().minusSeconds(120), Instant.now().plusSeconds(5));
  }

  @Test
  void aBadFilterOrPagingValueIsA400Idt1V17() throws Exception {
    String admin = login(account("ADMIN", "ACTIVE", HASH));
    for (String query :
        List.of(
            "?status=NOPE",
            "?role=NOPE",
            "?page=0",
            "?pageSize=0",
            "?pageSize=101",
            "?q=" + "x".repeat(101))) {
      MockHttpServletResponse response = send(get(BASE + query), admin, null);
      assertThat(response.getStatus()).as(query).isEqualTo(400);
      assertThat(JsonPath.<String>read(response.getContentAsString(), "$.errorCode"))
          .isEqualTo("IDT-V17");
    }
    assertThat(send(get(BASE + "?status=ALL&role=ALL&pageSize=100"), admin, null).getStatus())
        .isEqualTo(200);
  }

  @Test
  void theStatsAndTheDistributionCountTheWholeTable() throws Exception {
    String admin = login(account("ADMIN", "ACTIVE", HASH));
    account("STUDENT", "PENDING_VERIFICATION", HASH);
    account("INSTRUCTOR", "DEACTIVATED", HASH);

    String stats = send(get(BASE + "/management-stats"), admin, null).getContentAsString();
    assertThat(JsonPath.<Integer>read(stats, "$.pendingVerificationCount"))
        .isEqualTo(
            jdbc.queryForObject(
                "SELECT count(*) FROM identity.users WHERE status = 'PENDING_VERIFICATION'",
                Integer.class));

    String dist = send(get(BASE + "/role-distribution"), admin, null).getContentAsString();
    assertThat(JsonPath.<List<String>>read(dist, "$.items[*].groupCode"))
        .containsExactly("STUDENT", "INSTRUCTOR", "ADMIN", "DEACTIVATED");
    int total = jdbc.queryForObject("SELECT count(*) FROM identity.users", Integer.class);
    int deactivated =
        jdbc.queryForObject(
            "SELECT count(*) FROM identity.users WHERE status = 'DEACTIVATED'", Integer.class);
    assertThat(JsonPath.<Integer>read(dist, "$.items[3].count")).isEqualTo(deactivated);
    int sum = 0;
    double percentSum = 0;
    for (int i = 0; i < 4; i++) {
      sum += JsonPath.<Integer>read(dist, "$.items[" + i + "].count");
      percentSum += ((Number) JsonPath.read(dist, "$.items[" + i + "].percent")).doubleValue();
    }
    assertThat(sum).isEqualTo(total);
    assertThat(percentSum).isBetween(99.7, 100.3);
    double one = ((Number) JsonPath.read(dist, "$.items[0].percent")).doubleValue();
    assertThat(Math.round(one * 10) / 10.0).isEqualTo(one);
  }

  // ---- #50 -----------------------------------------------------------------------------------

  @Test
  void createdAccountIsPendingGetsATemporaryPasswordAndLogsInOnceWithIt() throws Exception {
    String admin = login(account("ADMIN", "ACTIVE", HASH));
    String address = UUID.randomUUID() + "@Example.Test";

    MockHttpServletResponse created =
        send(
            post(BASE),
            admin,
            "{\"displayName\":\"  New Person \",\"email\":\"  "
                + address
                + " \",\"roleId\":\""
                + roleId("INSTRUCTOR")
                + "\"}");

    assertThat(created.getStatus()).isEqualTo(201);
    String body = created.getContentAsString();
    String lower = address.toLowerCase();
    assertThat(JsonPath.<String>read(body, "$.email")).isEqualTo(lower);
    assertThat(JsonPath.<String>read(body, "$.displayName")).isEqualTo("New Person");
    assertThat(JsonPath.<String>read(body, "$.status")).isEqualTo("PENDING_VERIFICATION");
    assertThat(JsonPath.<String>read(body, "$.baseCategory")).isEqualTo("INSTRUCTOR");
    assertThat(JsonPath.<Boolean>read(body, "$.hasPassword")).isTrue();
    assertThat(body).doesNotContain("password\"");
    assertThat(
            jdbc.queryForObject(
                "SELECT must_change_password FROM identity.users WHERE email = ?",
                Boolean.class,
                lower))
        .isTrue();

    String temporary = mailedPassword(lower);
    assertThat(temporary).hasSize(12).matches(".*[A-Za-z].*").matches(".*[0-9].*");
    String id = JsonPath.read(body, "$.id");
    assertThat(
            jdbc.queryForObject(
                "SELECT password_hash FROM identity.users WHERE id = ?::uuid", String.class, id))
        .doesNotContain(temporary)
        .startsWith("$2");
    assertThat(auditCount("USER_CREATED", id)).isEqualTo(1);
    assertAuditIsClean(id, temporary, lower);

    MockHttpServletResponse login = loginResponse(lower, temporary);
    assertThat(login.getStatus()).isEqualTo(200);
    assertThat(JsonPath.<Boolean>read(login.getContentAsString(), "$.user.mustChangePassword"))
        .isTrue();
    assertThat(status(id)).isEqualTo("ACTIVE");
  }

  @Test
  void createRefusesDuplicatesAdminRolesBadRolesAndBadInput() throws Exception {
    String admin = login(account("ADMIN", "ACTIVE", HASH));
    String taken = account("STUDENT", "ACTIVE", HASH);
    String student = roleId("STUDENT");

    assertError(create(admin, "Dup Person", taken.toUpperCase(), student), 409, "IDT-440");
    assertError(create(admin, "Admin Person", mail(), roleId("ADMIN")), 422, "IDT-441");
    assertError(create(admin, "Odd Person", mail(), UUID.randomUUID().toString()), 422, "IDT-441");
    assertError(create(admin, "Odd Person", mail(), "not-a-uuid"), 422, "IDT-441");
    assertError(create(admin, "Odd Person", mail(), null), 422, "IDT-441");
    assertError(create(admin, "Odd Person", "no-at-sign", student), 400, "IDT-400");
    assertError(create(admin, "A", mail(), student), 422, "IDT-V19");
    assertError(create(admin, "N".repeat(101), mail(), student), 422, "IDT-V20");
    verify(mail, never()).send(any(), any(), any());
  }

  @Test
  void aCustomAdminCategoryRoleCannotBeUsedEither() throws Exception {
    String admin = login(account("ADMIN", "ACTIVE", HASH));
    UUID custom = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO identity.roles (id, code, name, base_category, is_system)"
            + " VALUES (?, ?, ?, 'ADMIN', false)",
        custom,
        "role_" + custom,
        "Custom " + custom);

    assertError(create(admin, "Odd Person", mail(), custom.toString()), 422, "IDT-441");
  }

  @Test
  void aMailFailureUndoesTheCreatedAccount() throws Exception {
    String admin = login(account("ADMIN", "ACTIVE", HASH));
    doThrow(new IllegalStateException("smtp down")).when(mail).send(any(), any(), any());
    String address = mail();

    assertThatThrownBy(() -> create(admin, "Mail Fail", address, roleId("STUDENT")))
        .hasRootCauseMessage("smtp down");
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM identity.users WHERE email = ?", Integer.class, address))
        .isZero();
  }

  // ---- #36 -----------------------------------------------------------------------------------

  @Test
  void aRoleChangeIsLiveAtOnceAndAuditedOnce() throws Exception {
    String admin = login(account("ADMIN", "ACTIVE", HASH));
    String email = account("STUDENT", "ACTIVE", HASH);
    String token = login(email);
    String id = userId(email);
    assertThat(send(get("/api/v1/identity/classes/my"), token, null).getStatus()).isEqualTo(403);

    MockHttpServletResponse changed = changeRole(admin, id, roleId("INSTRUCTOR"), null);

    assertThat(changed.getStatus()).isEqualTo(200);
    assertThat(JsonPath.<String>read(changed.getContentAsString(), "$.baseCategory"))
        .isEqualTo("INSTRUCTOR");
    assertThat(send(get("/api/v1/identity/classes/my"), token, null).getStatus()).isEqualTo(200);
    assertThat(auditCount("USER_ROLE_CHANGED", id)).isEqualTo(1);

    MockHttpServletResponse same = changeRole(admin, id, roleId("INSTRUCTOR"), null);
    assertThat(same.getStatus()).isEqualTo(200);
    assertThat(auditCount("USER_ROLE_CHANGED", id)).isEqualTo(1);
  }

  @Test
  void roleChangeRefusesUnknownUsersRolesAndBadBodies() throws Exception {
    String admin = login(account("ADMIN", "ACTIVE", HASH));
    String id = userId(account("STUDENT", "ACTIVE", HASH));
    String instructor = roleId("INSTRUCTOR");

    assertError(changeRole(admin, UUID.randomUUID().toString(), instructor, null), 404, "IDT-450");
    assertError(changeRole(admin, "not-a-uuid", instructor, null), 404, "IDT-450");
    assertError(changeRole(admin, id, UUID.randomUUID().toString(), null), 404, "IDT-424");
    assertError(changeRole(admin, id, null, null), 422, "IDT-V26");
    assertError(changeRole(admin, id, "not-a-uuid", null), 422, "IDT-V26");
    assertThat(roleCode(id)).isEqualTo("STUDENT");
  }

  @Test
  void theLastActiveAdminCannotBeDemotedWithOrWithoutAcknowledgement() throws Exception {
    String email = account("ADMIN", "ACTIVE", HASH);
    String token = login(email);
    String id = userId(email);
    List<String> others = deactivateOtherAdmins(id);
    try {
      assertError(changeRole(token, id, roleId("STUDENT"), true), 422, "IDT-432");
      assertError(changeRole(token, id, roleId("STUDENT"), null), 422, "IDT-432");
      assertThat(roleCode(id)).isEqualTo("ADMIN");
    } finally {
      reactivate(others);
    }
  }

  @Test
  void theLastAdminCannotMoveToAnAdminRoleThatCannotEditTheMatrix() throws Exception {
    String email = account("ADMIN", "ACTIVE", HASH);
    String token = login(email);
    String id = userId(email);
    UUID empty = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO identity.roles (id, code, name, base_category, is_system)"
            + " VALUES (?, ?, ?, 'ADMIN', false)",
        empty,
        "role_" + empty,
        "Empty " + empty);
    List<String> others = deactivateOtherAdmins(id);
    try {
      // Still an ADMIN-category role, so the old guard stayed silent and nobody could repair
      // access.
      assertError(changeRole(token, id, empty.toString(), true), 422, "IDT-432");
      assertThat(roleCode(id)).isEqualTo("ADMIN");
    } finally {
      reactivate(others);
    }
  }

  @Test
  void demotingYourselfNeedsAnExplicitAcknowledgementButAnotherAdminDoesNot() throws Exception {
    String email = account("ADMIN", "ACTIVE", HASH);
    String token = login(email);
    String id = userId(email);
    String otherEmail = account("ADMIN", "ACTIVE", HASH);
    String otherId = userId(otherEmail);
    account("ADMIN", "ACTIVE", HASH); // a third admin, so the invariant never decides here

    assertError(changeRole(token, otherId, roleId("INSTRUCTOR"), null), 200, null);
    assertThat(roleCode(otherId)).isEqualTo("INSTRUCTOR");

    for (Object flag : new Object[] {null, false, "true", 1}) {
      assertError(changeRole(token, id, roleId("STUDENT"), flag), 400, "IDT-V21");
    }
    assertThat(roleCode(id)).isEqualTo("ADMIN");
    assertThat(auditCount("USER_ROLE_CHANGED", id)).isZero();
    assertThat(changeRole(token, id, roleId("STUDENT"), true).getStatus()).isEqualTo(200);
    assertThat(roleCode(id)).isEqualTo("STUDENT");
  }

  @Test
  void twoAdminsDemotingEachOtherAtOnceLeaveOneAdmin() throws Exception {
    String firstEmail = account("ADMIN", "ACTIVE", HASH);
    String secondEmail = account("ADMIN", "ACTIVE", HASH);
    String first = login(firstEmail);
    String second = login(secondEmail);
    String firstId = userId(firstEmail);
    String secondId = userId(secondEmail);
    List<String> others = deactivateOtherAdmins(firstId, secondId);
    try {
      List<Integer> statuses =
          race(
              () -> changeRole(first, secondId, roleId("STUDENT"), null).getStatus(),
              () -> changeRole(second, firstId, roleId("STUDENT"), null).getStatus());

      assertThat(statuses).containsExactlyInAnyOrder(200, 422);
      assertThat(
              jdbc.queryForObject(
                  "SELECT count(*) FROM identity.users u JOIN identity.roles r ON r.id = u.role_id"
                      + " WHERE r.base_category = 'ADMIN' AND u.status = 'ACTIVE'",
                  Integer.class))
          .isEqualTo(1);
    } finally {
      reactivate(others);
    }
  }

  // ---- #37 -----------------------------------------------------------------------------------

  @Test
  void lockingEndsEverySessionAndMailsTheReasonOnlyWhenAsked() throws Exception {
    String admin = login(account("ADMIN", "ACTIVE", HASH));
    String email = account("STUDENT", "ACTIVE", HASH);
    String token = login(email);
    String id = userId(email);

    MockHttpServletResponse locked = changeStatus(admin, id, "DEACTIVATED", "Spam", true, null);

    assertThat(locked.getStatus()).isEqualTo(200);
    assertThat(JsonPath.<String>read(locked.getContentAsString(), "$.status"))
        .isEqualTo("DEACTIVATED");
    Map<String, Object> row =
        jdbc.queryForMap(
            "SELECT status, deactivation_source, deactivated_at FROM identity.users WHERE id ="
                + " ?::uuid",
            id);
    assertThat(row.get("deactivation_source")).isEqualTo("ADMIN");
    assertThat(row.get("deactivated_at")).isNotNull();
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM identity.refresh_tokens WHERE user_id = ?::uuid AND"
                    + " revoked_at IS NULL",
                Integer.class,
                id))
        .isZero();
    assertThat(send(get("/api/v1/identity/classes/my"), token, null).getStatus()).isEqualTo(401);
    ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
    verify(mail).send(eq(email), any(), body.capture());
    assertThat(body.getValue()).contains("Spam");
    assertThat(auditCount("USER_LOCKED", id)).isEqualTo(1);
    String after =
        jdbc.queryForObject(
            "SELECT after_json::text FROM identity.system_audit_logs WHERE action_type ="
                + " 'USER_LOCKED' AND target_id = ?",
            String.class,
            id);
    assertThat(after).contains("Spam");

    MockHttpServletResponse again = changeStatus(admin, id, "DEACTIVATED", null, null, null);
    assertThat(again.getStatus()).isEqualTo(200);
    assertThat(auditCount("USER_LOCKED", id)).isEqualTo(1);
  }

  @Test
  void lockingQuietlyWritesTheReasonToTheAuditOnly() throws Exception {
    String admin = login(account("ADMIN", "ACTIVE", HASH));
    String id = userId(account("STUDENT", "ACTIVE", HASH));

    assertThat(changeStatus(admin, id, "DEACTIVATED", "Quiet", false, null).getStatus())
        .isEqualTo(200);

    verify(mail, never()).send(any(), any(), any());
    assertThat(auditCount("USER_LOCKED", id)).isEqualTo(1);
  }

  @Test
  void unlockingRestoresAccessWritesOnlyTheAuditAndRefusesAnonymisedAccounts() throws Exception {
    String admin = login(account("ADMIN", "ACTIVE", HASH));
    String email = account("STUDENT", "ACTIVE", HASH);
    String id = userId(email);
    changeStatus(admin, id, "DEACTIVATED", null, false, null);

    MockHttpServletResponse unlocked = changeStatus(admin, id, "ACTIVE", "ignored", true, null);

    assertThat(unlocked.getStatus()).isEqualTo(200);
    assertThat(
            jdbc.queryForMap(
                "SELECT status, deactivation_source, deactivated_at FROM identity.users WHERE id ="
                    + " ?::uuid",
                id))
        .containsEntry("status", "ACTIVE")
        .containsEntry("deactivation_source", null)
        .containsEntry("deactivated_at", null);
    assertThat(auditCount("USER_UNLOCKED", id)).isEqualTo(1);
    verify(mail, never()).send(any(), any(), any());
    assertThat(login(email)).isNotBlank();

    String anonymised = UUID.randomUUID().toString();
    jdbc.update(
        "INSERT INTO identity.users (id,email,display_name,role_id,status,deactivation_source,"
            + "deactivated_at,anonymized_at,must_change_password) SELECT ?::uuid,?,'x',id,"
            + "'DEACTIVATED','SELF',now(),now(),false FROM identity.roles WHERE code='STUDENT'",
        anonymised,
        "deleted-user-" + anonymised + "@anon.local");
    assertError(changeStatus(admin, anonymised, "ACTIVE", null, null, null), 422, "IDT-448");
    assertThat(status(anonymised)).isEqualTo("DEACTIVATED");
    assertThat(auditCount("USER_UNLOCKED", anonymised)).isZero();
  }

  @Test
  void statusRequestsWithBadValuesAreRejectedWithoutChanges() throws Exception {
    String admin = login(account("ADMIN", "ACTIVE", HASH));
    String id = userId(account("STUDENT", "ACTIVE", HASH));

    assertError(
        changeStatus(admin, UUID.randomUUID().toString(), "ACTIVE", null, null, null),
        404,
        "IDT-450");
    assertError(changeStatus(admin, id, "PENDING_VERIFICATION", null, null, null), 422, "IDT-V26");
    assertError(changeStatus(admin, id, null, null, null, null), 422, "IDT-V26");
    assertError(changeStatus(admin, id, "DEACTIVATED", null, true, null), 422, "IDT-V26");
    assertError(changeStatus(admin, id, "DEACTIVATED", "  ", true, null), 422, "IDT-V26");
    assertError(
        changeStatus(admin, id, "DEACTIVATED", "r".repeat(501), false, null), 422, "IDT-V26");
    assertError(changeStatus(admin, id, "DEACTIVATED", 7, false, null), 422, "IDT-V26");
    assertError(changeStatus(admin, id, "DEACTIVATED", "ok", "yes", null), 422, "IDT-V26");
    assertThat(status(id)).isEqualTo("ACTIVE");
    assertThat(auditCount("USER_LOCKED", id)).isZero();
    verify(mail, never()).send(any(), any(), any());
  }

  @Test
  void lockingYourselfNeedsAcknowledgementAndTheLastAdminCannotBeLocked() throws Exception {
    String email = account("ADMIN", "ACTIVE", HASH);
    String token = login(email);
    String id = userId(email);
    String otherId = userId(account("ADMIN", "ACTIVE", HASH));
    account("ADMIN", "ACTIVE", HASH); // a third admin, so the invariant never decides here

    for (Object flag : new Object[] {null, false, "true"}) {
      assertError(changeStatus(token, id, "DEACTIVATED", null, false, flag), 400, "IDT-V21");
    }
    assertThat(status(id)).isEqualTo("ACTIVE");
    assertThat(changeStatus(token, otherId, "DEACTIVATED", null, false, null).getStatus())
        .isEqualTo(200);

    List<String> others = deactivateOtherAdmins(id);
    try {
      assertError(changeStatus(token, id, "DEACTIVATED", null, false, true), 422, "IDT-432");
      assertError(changeStatus(token, id, "DEACTIVATED", null, false, null), 422, "IDT-432");
      assertThat(status(id)).isEqualTo("ACTIVE");
    } finally {
      reactivate(others);
    }
  }

  @Test
  void twoAdminsLockingEachOtherAtOnceLeaveOneAdmin() throws Exception {
    String firstEmail = account("ADMIN", "ACTIVE", HASH);
    String secondEmail = account("ADMIN", "ACTIVE", HASH);
    String first = login(firstEmail);
    String second = login(secondEmail);
    String firstId = userId(firstEmail);
    String secondId = userId(secondEmail);
    List<String> others = deactivateOtherAdmins(firstId, secondId);
    try {
      List<Integer> statuses =
          race(
              () -> changeStatus(first, secondId, "DEACTIVATED", null, false, null).getStatus(),
              () -> changeStatus(second, firstId, "DEACTIVATED", null, false, null).getStatus());

      assertThat(statuses).containsExactlyInAnyOrder(200, 422);
      assertThat(
              jdbc.queryForObject(
                  "SELECT count(*) FROM identity.users u JOIN identity.roles r ON r.id = u.role_id"
                      + " WHERE r.base_category = 'ADMIN' AND u.status = 'ACTIVE'",
                  Integer.class))
          .isEqualTo(1);
    } finally {
      reactivate(others);
    }
  }

  // ---- #38 -----------------------------------------------------------------------------------

  @Test
  void resetMailsATemporaryPasswordEndsSessionsAndKeepsSecretsOutOfTheAudit() throws Exception {
    String admin = login(account("ADMIN", "ACTIVE", HASH));
    String email = account("STUDENT", "ACTIVE", HASH);
    String id = userId(email);
    login(email);
    String oldHash =
        jdbc.queryForObject(
            "SELECT password_hash FROM identity.users WHERE id = ?::uuid", String.class, id);

    MockHttpServletResponse reset = send(post(BASE + "/" + id + "/reset-password"), admin, null);

    assertThat(reset.getStatus()).isEqualTo(204);
    assertThat(reset.getContentAsString()).isEmpty();
    String temporary = mailedPassword(email);
    assertThat(
            jdbc.queryForObject(
                "SELECT password_hash FROM identity.users WHERE id = ?::uuid", String.class, id))
        .isNotEqualTo(oldHash);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM identity.refresh_tokens WHERE user_id = ?::uuid AND"
                    + " revoked_at IS NULL",
                Integer.class,
                id))
        .isZero();
    assertThat(loginResponse(email, PASSWORD).getStatus()).isEqualTo(401);
    MockHttpServletResponse login = loginResponse(email, temporary);
    assertThat(login.getStatus()).isEqualTo(200);
    assertThat(JsonPath.<Boolean>read(login.getContentAsString(), "$.user.mustChangePassword"))
        .isTrue();
    assertThat(auditCount("USER_PASSWORD_RESET", id)).isEqualTo(1);
    assertAuditIsClean(id, temporary, email);
  }

  @Test
  void aPasswordlessAccountGetsGuidanceAndNoPassword() throws Exception {
    String admin = login(account("ADMIN", "ACTIVE", HASH));
    String email = account("STUDENT", "ACTIVE", null);
    String id = userId(email);

    assertThat(send(post(BASE + "/" + id + "/reset-password"), admin, null).getStatus())
        .isEqualTo(204);

    assertThat(
            jdbc.queryForObject(
                "SELECT password_hash FROM identity.users WHERE id = ?::uuid", String.class, id))
        .isNull();
    assertThat(
            jdbc.queryForObject(
                "SELECT must_change_password FROM identity.users WHERE id = ?::uuid",
                Boolean.class,
                id))
        .isFalse();
    verify(mail).send(eq(email), any(), any());
    assertThat(auditCount("USER_PASSWORD_RESET", id)).isEqualTo(1);
  }

  @Test
  void resetRefusesUnknownAndAnonymisedAccountsAndUndoesItselfWhenTheMailFails() throws Exception {
    String admin = login(account("ADMIN", "ACTIVE", HASH));
    assertError(
        send(post(BASE + "/" + UUID.randomUUID() + "/reset-password"), admin, null),
        404,
        "IDT-450");
    assertError(send(post(BASE + "/not-a-uuid/reset-password"), admin, null), 404, "IDT-450");

    String anonymised = UUID.randomUUID().toString();
    jdbc.update(
        "INSERT INTO identity.users (id,email,password_hash,display_name,role_id,status,"
            + "deactivation_source,deactivated_at,anonymized_at,must_change_password) SELECT"
            + " ?::uuid,?,?,'x',id,'DEACTIVATED','SELF',now(),now(),false FROM identity.roles"
            + " WHERE code='STUDENT'",
        anonymised,
        "deleted-user-" + anonymised + "@anon.local",
        HASH);
    assertError(
        send(post(BASE + "/" + anonymised + "/reset-password"), admin, null), 422, "IDT-448");

    String email = account("STUDENT", "ACTIVE", HASH);
    String id = userId(email);
    doThrow(new IllegalStateException("smtp down")).when(mail).send(any(), any(), any());
    assertThatThrownBy(() -> send(post(BASE + "/" + id + "/reset-password"), admin, null))
        .hasRootCauseMessage("smtp down");
    doNothing().when(mail).send(any(), any(), any());
    assertThat(loginResponse(email, PASSWORD).getStatus()).isEqualTo(200);
    assertThat(auditCount("USER_PASSWORD_RESET", id)).isZero();
  }

  // ---- helpers -------------------------------------------------------------------------------

  private record Endpoint(String label, String action, MockHttpServletRequestBuilder request) {}

  /** One request per endpoint, aimed at {@code target}, with the matrix action that gates it. */
  private List<Endpoint> endpoints(String target) {
    return List.of(
        new Endpoint("GET list", "READ", get(BASE)),
        new Endpoint("GET stats", "READ", get(BASE + "/management-stats")),
        new Endpoint("GET distribution", "READ", get(BASE + "/role-distribution")),
        new Endpoint("POST create", "CREATE", post(BASE)),
        new Endpoint("PATCH role", "UPDATE", patch(BASE + "/" + target + "/role")),
        new Endpoint("PATCH status", "UPDATE", patch(BASE + "/" + target + "/status")),
        new Endpoint("POST reset", "UPDATE", post(BASE + "/" + target + "/reset-password")));
  }

  private void setCell(String action, boolean granted) {
    jdbc.update(
        "UPDATE identity.permissions p SET granted = ? FROM identity.functions f,"
            + " identity.actions a, identity.roles r WHERE p.function_id = f.id AND"
            + " p.action_id = a.id AND p.role_id = r.id AND r.code = 'ADMIN' AND f.code ="
            + " 'USER_MANAGEMENT' AND a.code = ?",
        granted,
        action);
    jdbc.update("UPDATE identity.roles SET permission_version = permission_version + 1");
  }

  private MockHttpServletResponse create(String token, String name, String email, String roleId)
      throws Exception {
    return send(
        post(BASE),
        token,
        "{\"displayName\":"
            + json(name)
            + ",\"email\":"
            + json(email)
            + ",\"roleId\":"
            + json(roleId)
            + "}");
  }

  private MockHttpServletResponse changeRole(
      String token, String userId, String roleId, Object acknowledge) throws Exception {
    return send(
        patch(BASE + "/" + userId + "/role"),
        token,
        "{\"targetRoleId\":"
            + json(roleId)
            + ",\"acknowledgeSelfImpact\":"
            + json(acknowledge)
            + "}");
  }

  /** {@code body} is, in order: targetStatus, reason, notifyByEmail, acknowledgeSelfImpact. */
  private MockHttpServletResponse changeStatus(String token, String userId, Object... body)
      throws Exception {
    return send(
        patch(BASE + "/" + userId + "/status"),
        token,
        "{\"targetStatus\":"
            + json(body[0])
            + ",\"reason\":"
            + json(body[1])
            + ",\"notifyByEmail\":"
            + json(body[2])
            + ",\"acknowledgeSelfImpact\":"
            + json(body[3])
            + "}");
  }

  private static String json(Object value) {
    if (value == null) {
      return "null";
    }
    if (value instanceof String text) {
      return "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
    return value.toString();
  }

  /** The temporary password in the last message sent to {@code recipient}. */
  private String mailedPassword(String recipient) {
    ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
    verify(mail).send(eq(recipient), any(), body.capture());
    String text = body.getValue();
    int start = text.indexOf("Mật khẩu tạm thời: ") + "Mật khẩu tạm thời: ".length();
    return text.substring(start, text.indexOf('.', start));
  }

  private void assertAuditIsClean(String targetId, String secret, String email) {
    List<String> rows =
        jdbc.queryForList(
            "SELECT coalesce(before_json::text,'') || coalesce(after_json::text,'') FROM"
                + " identity.system_audit_logs WHERE target_id = ?",
            String.class,
            targetId);
    assertThat(rows).isNotEmpty();
    for (String row : rows) {
      assertThat(row).doesNotContain(secret).doesNotContainIgnoringCase(email);
    }
  }

  private static void assertError(MockHttpServletResponse response, int status, String code)
      throws Exception {
    assertThat(response.getStatus()).as(response.getContentAsString()).isEqualTo(status);
    if (code != null) {
      assertThat(JsonPath.<String>read(response.getContentAsString(), "$.errorCode"))
          .isEqualTo(code);
    }
  }

  /** Runs both calls at the same instant and returns their results. */
  private static List<Integer> race(Callable<Integer> first, Callable<Integer> second)
      throws Exception {
    CountDownLatch go = new CountDownLatch(1);
    var pool = Executors.newFixedThreadPool(2);
    try {
      List<Future<Integer>> futures = new ArrayList<>();
      for (Callable<Integer> call : List.of(first, second)) {
        futures.add(
            pool.submit(
                () -> {
                  go.await();
                  return call.call();
                }));
      }
      go.countDown();
      List<Integer> out = new ArrayList<>();
      for (var future : futures) {
        out.add(future.get());
      }
      return out;
    } finally {
      pool.shutdownNow();
    }
  }

  /** Deactivates every ADMIN-category active account except the given ones; returns their ids. */
  private List<String> deactivateOtherAdmins(String... keep) {
    List<String> others =
        jdbc.queryForList(
            "SELECT u.id::text FROM identity.users u JOIN identity.roles r ON r.id = u.role_id"
                + " WHERE r.base_category = 'ADMIN' AND u.status = 'ACTIVE' AND u.id <> ALL"
                + " (?::uuid[])",
            String.class,
            (Object) keep);
    for (String other : others) {
      jdbc.update(
          "UPDATE identity.users SET status='DEACTIVATED', deactivation_source='ADMIN',"
              + " deactivated_at=now() WHERE id = ?::uuid",
          other);
    }
    return others;
  }

  private void reactivate(List<String> ids) {
    for (String id : ids) {
      jdbc.update(
          "UPDATE identity.users SET status='ACTIVE', deactivation_source=NULL,"
              + " deactivated_at=NULL WHERE id = ?::uuid",
          id);
    }
  }

  private int auditCount(String action, String targetId) {
    return jdbc.queryForObject(
        "SELECT count(*) FROM identity.system_audit_logs WHERE action_type = ? AND target_id = ?",
        Integer.class,
        action,
        targetId);
  }

  private String status(String id) {
    return jdbc.queryForObject(
        "SELECT status FROM identity.users WHERE id = ?::uuid", String.class, id);
  }

  private String roleCode(String userId) {
    return jdbc.queryForObject(
        "SELECT r.code FROM identity.users u JOIN identity.roles r ON r.id = u.role_id WHERE u.id"
            + " = ?::uuid",
        String.class,
        userId);
  }

  private String roleId(String code) {
    return jdbc.queryForObject(
        "SELECT id::text FROM identity.roles WHERE code = ?", String.class, code);
  }

  private String userId(String email) {
    return jdbc.queryForObject(
        "SELECT id::text FROM identity.users WHERE email = ?", String.class, email);
  }

  private static String mail() {
    return UUID.randomUUID() + "@example.test";
  }

  private String account(String roleCode, String status, String hash) {
    return insert(roleCode, status, hash, mail(), "QAUser123");
  }

  private String insert(String roleCode, String status, String hash, String email, String name) {
    jdbc.update(
        "INSERT INTO identity.users (id,email,password_hash,display_name,role_id,status,"
            + "deactivation_source,deactivated_at,must_change_password) SELECT ?,?,?,?,id,?,"
            + "CASE WHEN ? = 'DEACTIVATED' THEN 'ADMIN' END,"
            + "CASE WHEN ? = 'DEACTIVATED' THEN now() END,false FROM identity.roles WHERE code = ?",
        UUID.randomUUID(),
        email,
        hash,
        name,
        status,
        status,
        status,
        roleCode);
    return email;
  }

  private String login(String email) throws Exception {
    MockHttpServletResponse response = loginResponse(email, PASSWORD);
    assertThat(response.getStatus()).isEqualTo(200);
    return JsonPath.read(response.getContentAsString(), "$.accessToken");
  }

  private MockHttpServletResponse loginResponse(String email, String password) throws Exception {
    return mvc.perform(
            post("/api/v1/identity/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
        .andReturn()
        .getResponse();
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
