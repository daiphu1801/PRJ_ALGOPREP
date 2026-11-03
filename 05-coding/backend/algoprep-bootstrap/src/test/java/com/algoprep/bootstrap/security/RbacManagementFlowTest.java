package com.algoprep.bootstrap.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.jayway.jsonpath.JsonPath;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

/**
 * Permission matrix and custom roles, #30 to #34 and BR-13, against real PostgreSQL and Redis with
 * real tokens from {@code /auth/login}. The central proof: a cell toggled through the API changes
 * what an already-issued instructor token may do on the very next request.
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
class RbacManagementFlowTest {
  private static final String PASSWORD = "TempPass123";
  private static final String HASH = new BCryptPasswordEncoder().encode(PASSWORD);
  private static final String BASE = "/api/v1/identity/rbac";

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

  /** Puts the seeded matrix back, whatever a test did, and bumps every version (cache). */
  @AfterEach
  void restoreSeedMatrix() {
    jdbc.update(
        "UPDATE identity.permissions SET granted = true WHERE role_id ="
            + " (SELECT id FROM identity.roles WHERE code = 'ADMIN')");
    jdbc.update(
        "UPDATE identity.permissions SET granted = false WHERE role_id IN (SELECT id FROM"
            + " identity.roles WHERE base_category = 'ADMIN' AND NOT is_system)");
    jdbc.update(
        "UPDATE identity.permissions p SET granted = true FROM identity.roles r,"
            + " identity.functions f, identity.actions a WHERE r.code = 'INSTRUCTOR'"
            + " AND p.role_id = r.id AND p.function_id = f.id AND p.action_id = a.id"
            + " AND f.code IN ('CLASS_MANAGEMENT','PROBLEM_AUTHORING','TESTCASE_MANAGEMENT')");
    jdbc.update("UPDATE identity.roles SET permission_version = permission_version + 1");
  }

  @Test
  void theMatrixListsEveryRoleFunctionAndActionWithTheSeededGrants() throws Exception {
    MockHttpServletResponse response = send(get(BASE + "/matrix"), login(account("ADMIN")), null);

    assertThat(response.getStatus()).isEqualTo(200);
    String body = response.getContentAsString();
    assertThat(JsonPath.<List<String>>read(body, "$.functions[*].code")).hasSize(11);
    assertThat(JsonPath.<List<String>>read(body, "$.actions[*].code"))
        .containsExactly("CREATE", "READ", "UPDATE", "DELETE");
    int roles = JsonPath.<List<Object>>read(body, "$.roles").size();
    assertThat(roles).isGreaterThanOrEqualTo(3);
    assertThat(JsonPath.<List<Object>>read(body, "$.cells")).hasSize(roles * 11 * 4);
    assertThat(JsonPath.<String>read(body, "$.roles[0].code")).isNotBlank();
    assertThat(cellGranted(body, "ADMIN", "PERMISSION_MATRIX", "UPDATE")).isTrue();
    assertThat(cellGranted(body, "INSTRUCTOR", "CLASS_MANAGEMENT", "READ")).isTrue();
    assertThat(cellGranted(body, "INSTRUCTOR", "USER_MANAGEMENT", "READ")).isFalse();
    assertThat(cellGranted(body, "STUDENT", "CLASS_MANAGEMENT", "READ")).isFalse();
  }

  @Test
  void aToggledCellChangesWhatAnIssuedInstructorTokenMayDoOnTheNextRequest() throws Exception {
    String admin = login(account("ADMIN"));
    String instructor = login(account("INSTRUCTOR"));
    String cell = cellPath("INSTRUCTOR", "CLASS_MANAGEMENT", "READ");
    long versionBefore = version("INSTRUCTOR");
    assertThat(send(get("/api/v1/identity/classes/my"), instructor, null).getStatus())
        .isEqualTo(200);

    MockHttpServletResponse off = send(patch(cell), admin, "{\"granted\":false}");

    assertThat(off.getStatus()).isEqualTo(200);
    assertThat(JsonPath.<Boolean>read(off.getContentAsString(), "$.changed")).isTrue();
    assertThat(JsonPath.<Boolean>read(off.getContentAsString(), "$.granted")).isFalse();
    MockHttpServletResponse denied = send(get("/api/v1/identity/classes/my"), instructor, null);
    assertThat(denied.getStatus()).isEqualTo(403);
    assertThat(JsonPath.<String>read(denied.getContentAsString(), "$.errorCode"))
        .isEqualTo("IDT-445");
    assertThat(version("INSTRUCTOR")).isEqualTo(versionBefore + 1);
    assertThat(audits("ROLE_PERMISSION_CHANGED", roleId("INSTRUCTOR"))).isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "SELECT (before_json->>'granted') || '/' || (after_json->>'granted') FROM"
                    + " identity.system_audit_logs WHERE action_type = 'ROLE_PERMISSION_CHANGED'"
                    + " AND target_id = ?",
                String.class,
                roleId("INSTRUCTOR")))
        .isEqualTo("true/false");

    MockHttpServletResponse on = send(patch(cell), admin, "{\"granted\":true}");

    assertThat(on.getStatus()).isEqualTo(200);
    assertThat(send(get("/api/v1/identity/classes/my"), instructor, null).getStatus())
        .isEqualTo(200);
    assertThat(version("INSTRUCTOR")).isEqualTo(versionBefore + 2);
  }

  @Test
  void repeatingTheCurrentValueIsIdempotentWithoutVersionBumpOrAudit() throws Exception {
    String admin = login(account("ADMIN"));
    String cell = cellPath("INSTRUCTOR", "CLASS_MANAGEMENT", "READ");
    long versionBefore = version("INSTRUCTOR");

    MockHttpServletResponse first = send(patch(cell), admin, "{\"granted\":true}");

    assertThat(first.getStatus()).isEqualTo(200);
    assertThat(JsonPath.<Boolean>read(first.getContentAsString(), "$.changed")).isFalse();
    assertThat(version("INSTRUCTOR")).isEqualTo(versionBefore);
    assertThat(audits("ROLE_PERMISSION_CHANGED", roleId("INSTRUCTOR"))).isZero();
  }

  @Test
  void aCellOfACustomRoleWithoutStoredRowsIsCreatedByTheFirstSwitchOn() throws Exception {
    String admin = login(account("ADMIN"));
    String role = createRole(admin, "Reviewer", "INSTRUCTOR");
    String cell =
        BASE + "/matrix/" + role + "/" + functionId("PROBLEM_AUTHORING") + "/" + actionId("READ");

    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM identity.permissions WHERE role_id = ?::uuid",
                Integer.class,
                role))
        .isZero();
    assertThat(send(patch(cell), admin, "{\"granted\":true}").getStatus()).isEqualTo(200);

    assertThat(
            jdbc.queryForObject(
                "SELECT granted FROM identity.permissions WHERE role_id = ?::uuid",
                Boolean.class,
                role))
        .isTrue();
    String matrix = send(get(BASE + "/matrix"), admin, null).getContentAsString();
    assertThat(cellGrantedById(matrix, role, functionId("PROBLEM_AUTHORING"), actionId("READ")))
        .isTrue();
    assertThat(cellGrantedById(matrix, role, functionId("PROBLEM_AUTHORING"), actionId("UPDATE")))
        .isFalse();
  }

  @Test
  void matrixErrorsCarryTheirCodesAndChangeNothing() throws Exception {
    String admin = login(account("ADMIN"));
    String role = roleId("INSTRUCTOR");
    long versionBefore = version("INSTRUCTOR");
    String function = functionId("CLASS_MANAGEMENT");
    String action = actionId("READ");
    String unknown = UUID.randomUUID().toString();

    String off = "{\"granted\":false}";
    matrixError(admin, role + "/" + function + "/" + action, "{}", 422, "IDT-V25");
    matrixError(admin, unknown + "/" + function + "/" + action, off, 404, "IDT-424");
    matrixError(admin, role + "/" + unknown + "/" + action, off, 404, "IDT-424");
    matrixError(admin, role + "/" + function + "/" + unknown, off, 404, "IDT-424");
    matrixError(admin, "not-a-uuid/" + function + "/" + action, off, 404, "IDT-424");
    matrixError(
        admin,
        roleId("STUDENT") + "/" + function + "/" + action,
        "{\"granted\":true}",
        422,
        "IDT-423");

    assertThat(version("INSTRUCTOR")).isEqualTo(versionBefore);
    assertThat(audits("ROLE_PERMISSION_CHANGED", role)).isZero();
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM identity.permissions p JOIN identity.roles r ON r.id ="
                    + " p.role_id WHERE r.code = 'STUDENT' AND p.granted",
                Integer.class))
        .isZero();
  }

  @Test
  void anAdminRoleWithNoActiveAccountDoesNotCountAsAnotherMatrixEditor() throws Exception {
    String admin = login(account("ADMIN"));
    String adminCell = cellPath("ADMIN", "PERMISSION_MATRIX", "UPDATE");
    String deputy = createRole(admin, "Empty deputy", "ADMIN");
    String deputyCell =
        BASE
            + "/matrix/"
            + deputy
            + "/"
            + functionId("PERMISSION_MATRIX")
            + "/"
            + actionId("UPDATE");
    assertThat(send(patch(deputyCell), admin, "{\"granted\":true}").getStatus()).isEqualTo(200);

    // The deputy role holds the cell but nobody is in it: removing the built-in role's cell would
    // leave no one able to repair the matrix.
    assertError(send(patch(adminCell), admin, "{\"granted\":false}"), 422, "IDT-420");
    assertThat(cellStored("ADMIN", "PERMISSION_MATRIX", "UPDATE")).isTrue();
  }

  @Test
  void theLastAdminMatrixUpdateCellCannotBeRemovedButCanOnceAnotherAdminRoleHoldsIt()
      throws Exception {
    String admin = login(account("ADMIN"));
    String adminCell = cellPath("ADMIN", "PERMISSION_MATRIX", "UPDATE");

    assertError(send(patch(adminCell), admin, "{\"granted\":false}"), 422, "IDT-420");
    assertThat(cellStored("ADMIN", "PERMISSION_MATRIX", "UPDATE")).isTrue();

    String deputy = createRole(admin, "Deputy", "ADMIN");
    String deputyCell =
        BASE
            + "/matrix/"
            + deputy
            + "/"
            + functionId("PERMISSION_MATRIX")
            + "/"
            + actionId("UPDATE");
    assertThat(send(patch(deputyCell), admin, "{\"granted\":true}").getStatus()).isEqualTo(200);
    String deputyToken = login(accountWithRoleId(deputy));

    assertThat(send(patch(adminCell), admin, "{\"granted\":false}").getStatus()).isEqualTo(200);
    MockHttpServletResponse lockedOut = send(patch(adminCell), admin, "{\"granted\":true}");
    assertThat(lockedOut.getStatus()).isEqualTo(403);
    assertThat(JsonPath.<String>read(lockedOut.getContentAsString(), "$.errorCode"))
        .isEqualTo("IDT-445");
    assertThat(send(patch(adminCell), deputyToken, "{\"granted\":true}").getStatus())
        .isEqualTo(200);
    assertThat(send(patch(adminCell), admin, "{\"granted\":true}").getStatus()).isEqualTo(200);
  }

  @Test
  void twoAdminRolesRemovingEachOthersLastCellNeverBothSucceed() throws Exception {
    String admin = login(account("ADMIN"));
    String deputy = createRole(admin, "Racer", "ADMIN");
    String function = functionId("PERMISSION_MATRIX");
    String action = actionId("UPDATE");
    String deputyCell = BASE + "/matrix/" + deputy + "/" + function + "/" + action;
    String adminCell = cellPath("ADMIN", "PERMISSION_MATRIX", "UPDATE");
    assertThat(send(patch(deputyCell), admin, "{\"granted\":true}").getStatus()).isEqualTo(200);
    String deputyToken = login(accountWithRoleId(deputy));

    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      Callable<Integer> adminRemovesDeputy =
          () -> send(patch(deputyCell), admin, "{\"granted\":false}").getStatus();
      Callable<Integer> deputyRemovesAdmin =
          () -> send(patch(adminCell), deputyToken, "{\"granted\":false}").getStatus();
      var jobs = executor.invokeAll(List.of(adminRemovesDeputy, deputyRemovesAdmin));
      assertThat(List.of(jobs.get(0).get(), jobs.get(1).get())).doesNotContainSequence(200, 200);
    }

    assertThat(
            jdbc.queryForObject(
                "SELECT count(DISTINCT p.role_id) FROM identity.permissions p JOIN"
                    + " identity.roles r ON r.id = p.role_id WHERE r.base_category = 'ADMIN'"
                    + " AND p.granted AND p.function_id = ?::uuid AND p.action_id = ?::uuid",
                Integer.class,
                function,
                action))
        .isGreaterThanOrEqualTo(1);
  }

  @Test
  void rolesListShowsBuiltInRolesFirstWithAccountCounts() throws Exception {
    String admin = login(account("ADMIN"));
    String role = createRole(admin, "Tutor", "STUDENT");
    accountWithRoleId(role);
    accountWithRoleId(role);

    MockHttpServletResponse response = send(get(BASE + "/roles"), admin, null);

    assertThat(response.getStatus()).isEqualTo(200);
    String body = response.getContentAsString();
    assertThat(JsonPath.<List<Boolean>>read(body, "$.items[0:3].isSystem"))
        .containsExactly(true, true, true);
    assertThat(JsonPath.<List<Integer>>read(body, "$.items[?(@.name=='Tutor')].userCount"))
        .containsExactly(2);
    assertThat(JsonPath.<List<Integer>>read(body, "$.items[?(@.code=='ADMIN')].userCount").get(0))
        .isGreaterThanOrEqualTo(1);
    assertThat(JsonPath.<List<Boolean>>read(body, "$.items[?(@.name=='Tutor')].isSystem"))
        .containsExactly(false);
  }

  @Test
  void createRoleValidatesTheNameAndCategoryAndRefusesDuplicates() throws Exception {
    String admin = login(account("ADMIN"));

    MockHttpServletResponse created =
        send(
            post(BASE + "/roles"),
            admin,
            "{\"name\":\"  Lab  Helper \",\"baseCategory\":\"INSTRUCTOR\"}");

    assertThat(created.getStatus()).isEqualTo(201);
    String body = created.getContentAsString();
    assertThat(JsonPath.<String>read(body, "$.code")).isEqualTo("role_lab_helper");
    assertThat(JsonPath.<String>read(body, "$.name")).isEqualTo("Lab  Helper");
    assertThat(JsonPath.<String>read(body, "$.baseCategory")).isEqualTo("INSTRUCTOR");
    assertThat(JsonPath.<Boolean>read(body, "$.isSystem")).isFalse();
    assertThat(JsonPath.<Integer>read(body, "$.userCount")).isZero();
    String id = JsonPath.read(body, "$.id");
    assertThat(audits("ROLE_CREATED", id)).isEqualTo(1);

    roleError(admin, "LAB HELPER", "STUDENT", 409, "IDT-425");
    roleError(admin, "admin", "STUDENT", 409, "IDT-425");
    roleError(admin, "   ", "STUDENT", 422, "IDT-V22");
    roleError(admin, null, "STUDENT", 422, "IDT-V22");
    roleError(admin, "n".repeat(51), "STUDENT", 422, "IDT-V23");
    roleError(admin, "Fifty", "SYSTEM", 422, "IDT-V24");
    roleError(admin, "Fifty", null, 422, "IDT-V24");
    assertThat(createRole(admin, "n".repeat(50), "STUDENT")).isNotBlank();
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM identity.roles WHERE NOT is_system AND name LIKE 'Lab%'",
                Integer.class))
        .isEqualTo(1);
  }

  @Test
  void deleteRoleRefusesBuiltInAndUsedRolesAndRemovesAFreeOneWithItsCells() throws Exception {
    String admin = login(account("ADMIN"));
    for (String code : List.of("STUDENT", "INSTRUCTOR", "ADMIN")) {
      assertError(send(delete(BASE + "/roles/" + roleId(code)), admin, null), 422, "IDT-421");
      assertThat(roleId(code)).isNotNull();
    }
    String used = createRole(admin, "Busy", "STUDENT");
    accountWithRoleId(used);
    assertError(send(delete(BASE + "/roles/" + used), admin, null), 422, "IDT-422");
    assertThat(audits("ROLE_DELETED", used)).isZero();

    String free = createRole(admin, "Spare", "INSTRUCTOR");
    String cell =
        BASE + "/matrix/" + free + "/" + functionId("PROBLEM_AUTHORING") + "/" + actionId("READ");
    assertThat(send(patch(cell), admin, "{\"granted\":true}").getStatus()).isEqualTo(200);

    MockHttpServletResponse deleted = send(delete(BASE + "/roles/" + free), admin, null);

    assertThat(deleted.getStatus()).isEqualTo(204);
    assertThat(deleted.getContentAsString()).isEmpty();
    assertThat(count("identity.roles", free)).isZero();
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM identity.permissions WHERE role_id = ?::uuid",
                Integer.class,
                free))
        .isZero();
    assertThat(audits("ROLE_DELETED", free)).isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "SELECT (before_json->>'name') || '/' || (after_json IS NULL)::text FROM"
                    + " identity.system_audit_logs WHERE action_type = 'ROLE_DELETED' AND"
                    + " target_id = ?",
                String.class,
                free))
        .isEqualTo("Spare/true");
    assertError(send(delete(BASE + "/roles/" + free), admin, null), 404, "IDT-424");
    assertError(send(delete(BASE + "/roles/not-a-uuid"), admin, null), 404, "IDT-424");
  }

  @Test
  void aFailingAuditWriteRollsTheRoleDeletionBack() throws Exception {
    String admin = login(account("ADMIN"));
    String role = createRole(admin, "Doomed", "INSTRUCTOR");
    String cell =
        BASE + "/matrix/" + role + "/" + functionId("PROBLEM_AUTHORING") + "/" + actionId("READ");
    assertThat(send(patch(cell), admin, "{\"granted\":true}").getStatus()).isEqualTo(200);
    jdbc.execute(
        "CREATE FUNCTION identity.fail_role_deleted_audit() RETURNS trigger LANGUAGE plpgsql AS"
            + " $$ BEGIN IF NEW.action_type = 'ROLE_DELETED' THEN RAISE EXCEPTION 'audit down';"
            + " END IF; RETURN NEW; END $$");
    jdbc.execute(
        "CREATE TRIGGER fail_role_deleted_audit BEFORE INSERT ON identity.system_audit_logs FOR"
            + " EACH ROW EXECUTE FUNCTION identity.fail_role_deleted_audit()");
    try {
      assertError(send(delete(BASE + "/roles/" + role), admin, null), 503, "IDT-446");
    } finally {
      jdbc.execute("DROP TRIGGER fail_role_deleted_audit ON identity.system_audit_logs");
      jdbc.execute("DROP FUNCTION identity.fail_role_deleted_audit()");
    }

    assertThat(count("identity.roles", role)).isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM identity.permissions WHERE role_id = ?::uuid",
                Integer.class,
                role))
        .isEqualTo(1);
    assertThat(audits("ROLE_DELETED", role)).isZero();
  }

  @Test
  void studentsInstructorsAndAdminRolesWithoutTheCellAreRefusedAndAnonymousIs401()
      throws Exception {
    String admin = login(account("ADMIN"));
    String student = login(account("STUDENT"));
    String instructor = login(account("INSTRUCTOR"));
    String limited = createRole(admin, "Limited", "ADMIN");
    String limitedToken = login(accountWithRoleId(limited));
    String target = roleId("INSTRUCTOR");
    String cell = cellPath("INSTRUCTOR", "CLASS_MANAGEMENT", "READ");

    for (String token : List.of(student, instructor)) {
      assertError(send(get(BASE + "/matrix"), token, null), 403, "IDT-445");
      assertError(send(get(BASE + "/roles"), token, null), 403, "IDT-445");
      assertError(send(patch(cell), token, "{\"granted\":false}"), 403, "IDT-445");
      assertError(
          send(post(BASE + "/roles"), token, "{\"name\":\"X\",\"baseCategory\":\"STUDENT\"}"),
          403,
          "IDT-445");
      assertError(send(delete(BASE + "/roles/" + target), token, null), 403, "IDT-445");
    }
    assertThat(send(get(BASE + "/matrix"), limitedToken, null).getStatus()).isEqualTo(200);
    assertThat(send(get(BASE + "/roles"), limitedToken, null).getStatus()).isEqualTo(200);
    assertError(send(patch(cell), limitedToken, "{\"granted\":false}"), 403, "IDT-445");
    assertError(
        send(post(BASE + "/roles"), limitedToken, "{\"name\":\"X\",\"baseCategory\":\"STUDENT\"}"),
        403,
        "IDT-445");
    assertError(send(delete(BASE + "/roles/" + limited), limitedToken, null), 403, "IDT-445");
    for (MockHttpServletRequestBuilder call :
        List.of(get(BASE + "/matrix"), get(BASE + "/roles"), patch(cell), post(BASE + "/roles"))) {
      assertThat(mvc.perform(call).andReturn().getResponse().getStatus()).isEqualTo(401);
    }
    assertThat(cellStored("INSTRUCTOR", "CLASS_MANAGEMENT", "READ")).isTrue();
    assertThat(count("identity.roles", limited)).isEqualTo(1);
  }

  private void matrixError(String token, String cell, String json, int status, String code)
      throws Exception {
    assertError(send(patch(BASE + "/matrix/" + cell), token, json), status, code);
  }

  private void roleError(String token, String name, String category, int status, String code)
      throws Exception {
    String json =
        "{"
            + (name == null ? "" : "\"name\":\"" + name + "\"")
            + (name != null && category != null ? "," : "")
            + (category == null ? "" : "\"baseCategory\":\"" + category + "\"")
            + "}";
    assertError(send(post(BASE + "/roles"), token, json), status, code);
  }

  private String createRole(String token, String name, String category) throws Exception {
    MockHttpServletResponse response =
        send(
            post(BASE + "/roles"),
            token,
            "{\"name\":\"" + name + "\",\"baseCategory\":\"" + category + "\"}");
    assertThat(response.getStatus()).as(response.getContentAsString()).isEqualTo(201);
    return JsonPath.read(response.getContentAsString(), "$.id");
  }

  private static void assertError(MockHttpServletResponse response, int status, String code)
      throws Exception {
    assertThat(response.getStatus()).as(response.getContentAsString()).isEqualTo(status);
    assertThat(JsonPath.<String>read(response.getContentAsString(), "$.errorCode")).isEqualTo(code);
  }

  private static boolean cellGranted(String body, String role, String function, String action) {
    String roleId =
        JsonPath.<List<String>>read(body, "$.roles[?(@.code=='" + role + "')].id").get(0);
    String functionId =
        JsonPath.<List<String>>read(body, "$.functions[?(@.code=='" + function + "')].id").get(0);
    String actionId =
        JsonPath.<List<String>>read(body, "$.actions[?(@.code=='" + action + "')].id").get(0);
    return cellGrantedById(body, roleId, functionId, actionId);
  }

  private static boolean cellGrantedById(String body, String role, String function, String action) {
    List<Boolean> found =
        JsonPath.read(
            body,
            "$.cells[?(@.roleId=='"
                + role
                + "' && @.functionId=='"
                + function
                + "' && @.actionId=='"
                + action
                + "')].granted");
    assertThat(found).hasSize(1);
    return found.get(0);
  }

  private String cellPath(String role, String function, String action) {
    return BASE + "/matrix/" + roleId(role) + "/" + functionId(function) + "/" + actionId(action);
  }

  private boolean cellStored(String role, String function, String action) {
    return Boolean.TRUE.equals(
        jdbc.queryForObject(
            "SELECT granted FROM identity.permissions WHERE role_id = ?::uuid AND function_id ="
                + " ?::uuid AND action_id = ?::uuid",
            Boolean.class,
            roleId(role),
            functionId(function),
            actionId(action)));
  }

  private String roleId(String code) {
    return jdbc.queryForObject(
        "SELECT id::text FROM identity.roles WHERE code = ?", String.class, code);
  }

  private String functionId(String code) {
    return jdbc.queryForObject(
        "SELECT id::text FROM identity.functions WHERE code = ?", String.class, code);
  }

  private String actionId(String code) {
    return jdbc.queryForObject(
        "SELECT id::text FROM identity.actions WHERE code = ?", String.class, code);
  }

  private long version(String roleCode) {
    return jdbc.queryForObject(
        "SELECT permission_version FROM identity.roles WHERE code = ?", Long.class, roleCode);
  }

  private int audits(String action, String targetId) {
    return jdbc.queryForObject(
        "SELECT count(*) FROM identity.system_audit_logs WHERE action_type = ? AND target_id = ?",
        Integer.class,
        action,
        targetId);
  }

  private int count(String table, String id) {
    return jdbc.queryForObject(
        "SELECT count(*) FROM " + table + " WHERE id = ?::uuid", Integer.class, id);
  }

  private String account(String roleCode) {
    return insertUser("SELECT id FROM identity.roles WHERE code = ?", roleCode);
  }

  private String accountWithRoleId(String roleId) {
    return insertUser("SELECT id FROM identity.roles WHERE id = ?::uuid", roleId);
  }

  private String insertUser(String roleSelect, String roleKey) {
    String email = UUID.randomUUID() + "@example.test";
    jdbc.update(
        "INSERT INTO identity.users (id,email,password_hash,display_name,role_id,status,"
            + "must_change_password) SELECT ?,?,?,'QAUser123',id,'ACTIVE',false FROM ("
            + roleSelect
            + ") r",
        UUID.randomUUID(),
        email,
        HASH,
        roleKey);
    return email;
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
