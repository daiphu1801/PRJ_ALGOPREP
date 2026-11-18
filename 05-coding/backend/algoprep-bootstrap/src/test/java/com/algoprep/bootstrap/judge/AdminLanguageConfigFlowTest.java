package com.algoprep.bootstrap.judge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.algoprep.identity.application.ports.out.AccessTokenPort;
import com.algoprep.identity.domain.model.BaseCategory;
import com.algoprep.judge.domain.ports.out.AdminLanguageConfigRepository;
import com.algoprep.judge.domain.ports.out.SubmissionAdmissionRepository;
import com.algoprep.judge.domain.ports.out.WorkerJobRepository;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
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
import tools.jackson.databind.node.ObjectNode;

/**
 * Real JWT/matrix, PostgreSQL row locks and REQUIRED audit bridge, without an outer transaction.
 */
@SpringBootTest(
    properties = {
      "spring.autoconfigure.exclude=org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration",
      "algoprep.judge.compiler-display.java=Verified test JDK",
      "spring.datasource.hikari.maximum-pool-size=6"
    })
@ActiveProfiles("test")
@Testcontainers
class AdminLanguageConfigFlowTest {
  private static final String PATH = "/api/v1/judge/admin/language-config";

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
  @Autowired AccessTokenPort tokens;
  @Autowired WebApplicationContext context;
  @MockitoSpyBean AdminLanguageConfigRepository repository;
  @Autowired WorkerJobRepository jobs;
  @Autowired SubmissionAdmissionRepository admission;
  private final JsonMapper json = JsonMapper.builder().build();
  private MockMvc mvc;
  private String admin;
  private UUID adminId;

  @BeforeEach
  void setup() {
    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    adminId = UUID.randomUUID();
    admin = token(adminId, BaseCategory.ADMIN);
    jdbc.update(
        "UPDATE judge.language_configs SET time_limit_multiplier=1,memory_limit_multiplier=1,enabled=true,updated_at=clock_timestamp()");
    jdbc.update(
        "UPDATE judge.grading_defaults SET time_limit_ms=1000,memory_limit_mb=256,output_limit_kb=64,compile_timeout_sec=10,updated_at=clock_timestamp()");
    jdbc.update(
        "UPDATE judge.sandbox_settings SET network_access_enabled=false,child_process_limit_enabled=true,return_stderr_to_student=true,updated_at=clock_timestamp()");
    jdbc.update(
        "UPDATE judge.generator_sandbox_settings SET max_runtime_seconds=30,max_output_mb=100,updated_at=clock_timestamp()");
    setCell("READ", true);
    setCell("UPDATE", true);
  }

  private String token(UUID id, BaseCategory role) {
    jdbc.update(
        "INSERT INTO identity.users(id,email,display_name,role_id,status) SELECT ?,?,'Config user',id,'ACTIVE' FROM identity.roles WHERE code=?",
        id,
        id + "@example.test",
        role.name());
    return tokens.issue(id, role, role.name(), UUID.randomUUID()).value();
  }

  private void setCell(String action, boolean granted) {
    jdbc.update(
        "UPDATE identity.permissions p SET granted=? FROM identity.roles r,identity.functions f,identity.actions a WHERE p.role_id=r.id AND p.function_id=f.id AND p.action_id=a.id AND r.code='ADMIN' AND f.code='LANGUAGE_CONFIG' AND a.code=?",
        granted,
        action);
    jdbc.update(
        "UPDATE identity.roles SET permission_version=permission_version+1 WHERE code='ADMIN'");
  }

  private JsonNode read(String bearer, int expected) throws Exception {
    var request = get(PATH);
    if (bearer != null) {
      request.header("Authorization", "Bearer " + bearer);
    }
    var response = mvc.perform(request).andReturn().getResponse();
    assertThat(response.getStatus()).as(response.getContentAsString()).isEqualTo(expected);
    return json.readTree(response.getContentAsString());
  }

  private JsonNode save(String bearer, JsonNode body, int expected) throws Exception {
    var request = put(PATH).contentType(MediaType.APPLICATION_JSON).content(body.toString());
    if (bearer != null) {
      request.header("Authorization", "Bearer " + bearer);
    }
    var response = mvc.perform(request).andReturn().getResponse();
    assertThat(response.getStatus()).as(response.getContentAsString()).isEqualTo(expected);
    return json.readTree(response.getContentAsString());
  }

  private ObjectNode current() throws Exception {
    return (ObjectNode) read(admin, 200);
  }

  private static ObjectNode group(JsonNode body, String name) {
    return (ObjectNode) body.path(name);
  }

  private static ObjectNode language(JsonNode body, String code) {
    for (var row : body.path("languages")) {
      if (row.path("language").asText().equals(code)) {
        return (ObjectNode) row;
      }
    }
    throw new AssertionError("Missing language");
  }

  private int audits() {
    return jdbc.queryForObject(
        "SELECT count(*) FROM identity.system_audit_logs WHERE actor_user_id=? AND action_type='LANGUAGE_CONFIG_CHANGED'",
        Integer.class,
        adminId);
  }

  @Test
  void allFourGroupsCompilerLabelsAndPendingCountsComeFromRealSources() throws Exception {
    UUID pending = pending("JAVA");
    pending("PYTHON");
    var value = current();
    assertThat(value.properties().stream().map(java.util.Map.Entry::getKey))
        .containsExactlyInAnyOrder("languages", "gradingDefaults", "sandbox", "generatorSandbox");
    assertThat(value.path("languages")).hasSize(3);
    assertThat(language(value, "JAVA").path("compilerDisplayName").asText())
        .isEqualTo("Verified test JDK");
    assertThat(language(value, "JAVA").path("pendingJobCount").asLong())
        .isEqualTo(countPending("JAVA"));
    assertThat(language(value, "PYTHON").path("pendingJobCount").asLong())
        .isEqualTo(countPending("PYTHON"));
    assertThat(value.toString())
        .doesNotContain("maxChildProcesses", "sourceCode", pending.toString());
    assertThat(value.path("gradingDefaults").path("timeLimitMs").asInt()).isEqualTo(1000);
    assertThat(value.path("generatorSandbox").path("maxRuntimeSeconds").asInt()).isEqualTo(30);
    assertThat(current()).isEqualTo(value);
    assertThat(audits()).isZero();
  }

  private long countPending(String code) {
    return jdbc.queryForObject(
        "SELECT count(*) FROM judge.submissions WHERE language=? AND status='PENDING'",
        Long.class,
        code);
  }

  private UUID pending(String code) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO judge.submissions(id,user_id,problem_id,language,submission_model,source_code,source_input_method,status,testcase_set_version_at_grading) VALUES(?,?,?,?,'STANDARD_IO','config-private-source','EDITOR','PENDING',1)",
        id,
        adminId,
        UUID.randomUUID(),
        code);
    return id;
  }

  @Test
  void savesAllGroupsWithOneDiffAuditAndNoOpDoesNotTouchVersions() throws Exception {
    var old = current();
    var body = old.deepCopy();
    language(body, "JAVA").put("timeLimitMultiplier", 2);
    group(body, "gradingDefaults").put("maxOutputSizeKb", 128);
    group(body, "sandbox").put("returnStderrToStudent", false).put("maxChildProcesses", 999);
    group(body, "generatorSandbox").put("maxRuntimeSeconds", 300).put("maxOutputMb", 1024);
    language(body, "JAVA").put("compilerDisplayName", "forged").put("pendingJobCount", 999);
    var saved = save(admin, body, 200);
    assertThat(group(saved, "gradingDefaults").path("maxOutputSizeKb").asInt()).isEqualTo(128);
    assertThat(group(saved, "sandbox").path("returnStderrToStudent").asBoolean()).isFalse();
    assertThat(group(saved, "generatorSandbox").path("maxOutputMb").asInt()).isEqualTo(1024);
    assertThat(language(saved, "CPP").path("updatedAt"))
        .isEqualTo(language(old, "CPP").path("updatedAt"));
    assertThat(language(saved, "JAVA").path("updatedAt"))
        .isNotEqualTo(language(old, "JAVA").path("updatedAt"));
    assertThat(language(saved, "JAVA").path("compilerDisplayName").asText())
        .isEqualTo("Verified test JDK");
    assertThat(audits()).isEqualTo(1);
    String audit =
        jdbc.queryForObject(
            "SELECT after_json::text FROM identity.system_audit_logs WHERE actor_user_id=? AND action_type='LANGUAGE_CONFIG_CHANGED'",
            String.class,
            adminId);
    assertThat(audit)
        .contains("languages.JAVA.timeLimitMultiplier", "gradingDefaults.maxOutputSizeKb")
        .doesNotContain("updatedAt", "compilerDisplayName", "pendingJobCount", "maxChildProcesses");
    assertThat(save(admin, saved, 200)).isEqualTo(saved);
    assertThat(audits()).isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "SELECT updated_by FROM judge.language_configs WHERE language='JAVA'", UUID.class))
        .isEqualTo(adminId);
  }

  @Test
  void staleLanguageAndEachSingletonVersionCannotOverwriteAnything() throws Exception {
    for (String group : List.of("languages", "gradingDefaults", "sandbox", "generatorSandbox")) {
      var before = current();
      var body = before.deepCopy();
      var row = group.equals("languages") ? language(body, "JAVA") : group(body, group);
      row.put("updatedAt", "2000-01-01T00:00:00Z");
      language(body, "JAVA").put("timeLimitMultiplier", 2);
      assertThat(save(admin, body, 409).path("errorCode").asText()).isEqualTo("JDG-202");
      assertThat(current()).isEqualTo(before);
    }
    assertThat(audits()).isZero();
  }

  @Test
  void shapeNumericAndCrossFieldErrorsAreAtomic() throws Exception {
    var before = current();
    var body = before.deepCopy();
    language(body, "JAVA").put("timeLimitMultiplier", 0.09);
    assertThat(save(admin, body, 400).path("errorCode").asText()).isEqualTo("JDG-V10");
    body = before.deepCopy();
    group(body, "gradingDefaults").put("maxOutputSizeKb", 1.5);
    assertThat(save(admin, body, 400).path("errorCode").asText()).isEqualTo("JDG-V11");
    body = before.deepCopy();
    group(body, "generatorSandbox").put("maxRuntimeSeconds", 301);
    assertThat(save(admin, body, 400).path("errorCode").asText()).isEqualTo("JDG-V12");
    body = before.deepCopy();
    for (var row : body.path("languages")) {
      ((ObjectNode) row).put("enabled", false);
    }
    assertThat(save(admin, body, 422).path("errorCode").asText()).isEqualTo("JDG-203");
    body = before.deepCopy();
    group(body, "gradingDefaults").put("timeLimitMs", 30001);
    assertThat(save(admin, body, 422).path("errorCode").asText()).isEqualTo("JDG-204");
    body = before.deepCopy();
    group(body, "gradingDefaults").put("memoryLimitMb", 1025);
    assertThat(save(admin, body, 422).path("errorCode").asText()).isEqualTo("JDG-205");
    assertThat(current()).isEqualTo(before);
    assertThat(audits()).isZero();
  }

  @Test
  void anonymousWrongRolesAndEachRevokedPermissionAreDenied() throws Exception {
    var body = current();
    read(null, 401);
    save(null, body, 401);
    for (var role : List.of(BaseCategory.STUDENT, BaseCategory.INSTRUCTOR)) {
      String bearer = token(UUID.randomUUID(), role);
      assertThat(read(bearer, 403).path("errorCode").asText()).isEqualTo("JDG-301");
      assertThat(save(bearer, body, 403).path("errorCode").asText()).isEqualTo("JDG-301");
    }
    try {
      setCell("READ", false);
      assertThat(read(admin, 403).path("errorCode").asText()).isEqualTo("JDG-301");
      setCell("READ", true);
      setCell("UPDATE", false);
      read(admin, 200);
      assertThat(save(admin, body, 403).path("errorCode").asText()).isEqualTo("JDG-301");
    } finally {
      setCell("READ", true);
      setCell("UPDATE", true);
    }
    assertThat(current()).isEqualTo(body);
    assertThat(audits()).isZero();
  }

  @Test
  void twoConcurrentAdminsCannotBothSaveTheSameVersion() throws Exception {
    jdbc.update("UPDATE judge.language_configs SET enabled=false WHERE language='PYTHON'");
    UUID secondAdminId = UUID.randomUUID();
    String secondAdmin = token(secondAdminId, BaseCategory.ADMIN);
    var first = current();
    language(first, "JAVA").put("enabled", false);
    var second = first.deepCopy();
    language(second, "JAVA").put("enabled", true);
    language(second, "CPP").put("enabled", false);
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      Callable<Integer> a = () -> rawSave(first, admin);
      Callable<Integer> b = () -> rawSave(second, secondAdmin);
      var results = executor.invokeAll(List.of(a, b));
      assertThat(List.of(results.get(0).get(), results.get(1).get()))
          .containsExactlyInAnyOrder(200, 409);
    }
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM judge.language_configs WHERE enabled", Integer.class))
        .isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM identity.system_audit_logs WHERE action_type='LANGUAGE_CONFIG_CHANGED' AND actor_user_id IN (?,?)",
                Integer.class,
                adminId,
                secondAdminId))
        .isEqualTo(1);
  }

  private int rawSave(JsonNode body, String bearer) throws Exception {
    return mvc.perform(
            put(PATH)
                .header("Authorization", "Bearer " + bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andReturn()
        .getResponse()
        .getStatus();
  }

  @Test
  void failedRealAuditInsertRollsBackEveryConfigurationGroup() throws Exception {
    var before = current();
    var body = before.deepCopy();
    language(body, "JAVA").put("timeLimitMultiplier", 2);
    group(body, "gradingDefaults").put("maxOutputSizeKb", 128);
    group(body, "sandbox").put("returnStderrToStudent", false);
    group(body, "generatorSandbox").put("maxRuntimeSeconds", 60);
    jdbc.execute(
        "CREATE FUNCTION identity.fail_judge_config_audit() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN IF NEW.action_type='LANGUAGE_CONFIG_CHANGED' THEN RAISE EXCEPTION 'private-audit-fault'; END IF; RETURN NEW; END $$");
    jdbc.execute(
        "CREATE TRIGGER fail_judge_config_audit BEFORE INSERT ON identity.system_audit_logs FOR EACH ROW EXECUTE FUNCTION identity.fail_judge_config_audit()");
    try {
      var error = save(admin, body, 503);
      assertThat(error.toString())
          .doesNotContain("private-audit-fault", "SQL", "JDG-506", "JDG-507", "errorCode");
      assertThat(current()).isEqualTo(before);
      assertThat(audits()).isZero();
    } finally {
      jdbc.execute("DROP TRIGGER fail_judge_config_audit ON identity.system_audit_logs");
      jdbc.execute("DROP FUNCTION identity.fail_judge_config_audit()");
    }
    save(admin, body, 200);
    assertThat(audits()).isEqualTo(1);
  }

  @Test
  void disablingLanguageLeavesPendingSubmissionsUntouched() throws Exception {
    UUID id = pending("PYTHON");
    var before = jdbc.queryForMap("SELECT * FROM judge.submissions WHERE id=?", id);
    var captured = jobs.settings("PYTHON");
    var body = current();
    language(body, "PYTHON").put("enabled", false);
    language(body, "PYTHON").put("timeLimitMultiplier", 3);
    save(admin, body, 200);
    assertThat(jdbc.queryForMap("SELECT * FROM judge.submissions WHERE id=?", id))
        .isEqualTo(before);
    assertThat(
            jdbc.queryForObject(
                "SELECT enabled FROM judge.language_configs WHERE language='PYTHON'",
                Boolean.class))
        .isFalse();
    assertThat(captured.timeMultiplier()).isEqualByComparingTo("1");
    assertThat(jobs.settings("PYTHON").timeMultiplier()).isEqualByComparingTo("3");
    assertThat(admission.languageEnabled("PYTHON")).isFalse();
  }

  @Test
  void malformedJsonAndExtraDecimalDigitsAreRejectedBeforeWrites() throws Exception {
    var body = current();
    String text = body.toString();
    String invalidScale =
        text.replaceFirst("\"timeLimitMultiplier\":1\\.0+", "\"timeLimitMultiplier\":1.000");
    assertThat(invalidScale).isNotEqualTo(text);
    for (String request : List.of("{", " ", "null", text + " {}", invalidScale)) {
      var response =
          mvc.perform(
                  put(PATH)
                      .header("Authorization", "Bearer " + admin)
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(request))
              .andReturn()
              .getResponse();
      assertThat(response.getStatus()).as(response.getContentAsString()).isEqualTo(400);
      assertThat(json.readTree(response.getContentAsString()).path("errorCode").asText())
          .isEqualTo("JDG-V10");
    }
    assertThat(current()).isEqualTo(body);
    assertThat(audits()).isZero();
  }

  @Test
  void failureAfterAuditAlsoRollsBackAuditAndTheWrite() throws Exception {
    var before = current();
    var body = before.deepCopy();
    language(body, "JAVA").put("timeLimitMultiplier", 2);
    doThrow(new IllegalStateException("private-read-fault")).when(repository).read();
    var error = save(admin, body, 503);
    assertThat(error.toString()).doesNotContain("private-read-fault", "SQL");
    assertThat(audits()).isZero();
    assertThat(
            jdbc.queryForObject(
                "SELECT time_limit_multiplier::int FROM judge.language_configs WHERE language='JAVA'",
                Integer.class))
        .isEqualTo(1);
  }
}
