package com.algoprep.bootstrap.harness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.algoprep.common.exception.AlgoPrepException;
import com.algoprep.common.functionspec.Language;
import com.algoprep.harness.application.dto.GetStarterCodeQuery;
import com.algoprep.harness.application.dto.PrepareSubmissionCommand;
import com.algoprep.harness.application.dto.SubmissionModel;
import com.algoprep.harness.application.ports.in.GetStarterCodeUseCase;
import com.algoprep.harness.application.ports.in.PrepareSubmissionUseCase;
import com.algoprep.harness.application.ports.out.ProblemSpecReaderPort;
import com.algoprep.harness.application.ports.out.StarterCodeAccessPort;
import com.algoprep.identity.application.ports.out.AccessTokenPort;
import com.algoprep.identity.domain.model.BaseCategory;
import com.algoprep.problembank.application.dto.SaveProblemSpecCommand;
import com.algoprep.problembank.application.ports.in.SaveProblemSpecUseCase;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
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

/** Real JWT/current role, PG, existing F2 ports and F3 preparation. No F1/F2 feature changes. */
@SpringBootTest(
    properties =
        "spring.autoconfigure.exclude=org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration")
@ActiveProfiles("test")
@Testcontainers
class StarterCodeFlowTest {
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
  @Autowired AccessTokenPort access;
  @Autowired SaveProblemSpecUseCase save;
  @Autowired PrepareSubmissionUseCase prepare;
  @Autowired GetStarterCodeUseCase starter;
  @Autowired WebApplicationContext context;
  @MockitoSpyBean ProblemSpecReaderPort reader;
  @MockitoSpyBean StarterCodeAccessPort permissions;
  private final JsonMapper json = JsonMapper.builder().findAndAddModules().build();
  private MockMvc mvc;
  private UUID problem;
  private UUID author;
  private UUID student;
  private UUID other;
  private UUID admin;

  @BeforeEach
  void fixture() {
    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    author = user("INSTRUCTOR");
    student = user("STUDENT");
    other = user("INSTRUCTOR");
    admin = user("ADMIN");
    problem = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO problem.problems(id,code,title,author_id,updated_by,level_id,time_limit_ms,memory_limit_mb,max_output_size_kb) VALUES(?,?,'Starter fixture',?,?,'20000000-0000-4000-8000-000000000001',1000,256,64)",
        problem,
        problem.toString(),
        author,
        author);
    saveSpec("echo", "INT", "EXACT");
  }

  private UUID user(String role) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO identity.users(id,email,display_name,role_id,status) SELECT ?,?,'Starter user',id,'ACTIVE' FROM identity.roles WHERE code=?",
        id,
        id + "@example.test",
        role);
    return id;
  }

  private String token(UUID user, BaseCategory category) {
    return access.issue(user, category, category.name(), UUID.randomUUID()).value();
  }

  private String path(UUID id, String language, String model) {
    return "/api/v1/harness/problems/"
        + id
        + "/starter-code?language="
        + language
        + "&model="
        + model;
  }

  private JsonNode fetch(String route, UUID actor, BaseCategory category, int status)
      throws Exception {
    return withToken(route, token(actor, category), status);
  }

  private JsonNode withToken(String path, String bearer, int status) throws Exception {
    var response =
        mvc.perform(get(path).header("Authorization", "Bearer " + bearer))
            .andReturn()
            .getResponse();
    assertThat(response.getStatus()).as(response.getContentAsString()).isEqualTo(status);
    return json.readTree(response.getContentAsString());
  }

  private JsonNode studentGet(int status) throws Exception {
    return fetch(
        path(problem, "PYTHON", "FUNCTION_WRAPPER"), student, BaseCategory.STUDENT, status);
  }

  private void saveSpec(String name, String kind, String matching) {
    var body =
        json.createObjectNode()
            .put("functionName", name)
            .put("matchingStrategy", matching)
            .put("stdinFormatMd", "private-format")
            .put("stdoutFormatMd", "private-format");
    body.set("returnType", json.createObjectNode().put("kind", kind));
    body.set("parameters", json.readTree("[{\"name\":\"value\",\"type\":{\"kind\":\"INT\"}}]"));
    body.set("nameOverrides", json.createObjectNode());
    save.save(
        new SaveProblemSpecCommand(
            problem,
            author,
            false,
            jdbc.queryForObject(
                    "SELECT updated_at FROM problem.problems WHERE id=?",
                    java.sql.Timestamp.class,
                    problem)
                .toInstant(),
            body));
  }

  private void publish() {
    jdbc.update(
        "UPDATE problem.problems SET status='PUBLISHED',published_at=clock_timestamp() WHERE id=?",
        problem);
  }

  private List<Long> counts() {
    return List.of(
            "problem.outbox_events",
            "identity.system_audit_logs",
            "judge.submissions",
            "judge.submission_testcase_results",
            "judge.judge_run_tokens",
            "judge.outbox_events")
        .stream()
        .map(table -> jdbc.queryForObject("SELECT count(*) FROM " + table, Long.class))
        .toList();
  }

  private void hidden(JsonNode error) {
    assertThat(error.path("errorCode").asString()).isEqualTo("HRN-001");
    assertThat(error.toString())
        .doesNotContain("sourceCode", "specVersion", "functionName", "private-format");
  }

  @ParameterizedTest
  @CsvSource({
    "JAVA,FUNCTION_WRAPPER",
    "CPP,FUNCTION_WRAPPER",
    "PYTHON,FUNCTION_WRAPPER",
    "JAVA,STANDARD_IO",
    "CPP,STANDARD_IO",
    "PYTHON,STANDARD_IO"
  })
  void publicStarterHasExactlyThreeFieldsForEveryLanguageAndModel(String language, String model)
      throws Exception {
    publish();
    var before = counts();
    var state = jdbc.queryForMap("SELECT * FROM problem.problems WHERE id=?", problem);
    var first = fetch(path(problem, language, model), student, BaseCategory.STUDENT, 200);
    var second = fetch(path(problem, language, model), student, BaseCategory.STUDENT, 200);
    assertThat(first).isEqualTo(second);
    assertThat(first.size()).isEqualTo(3);
    assertThat(first.path("language").asString()).isEqualTo(language);
    assertThat(first.path("submissionModel").asString()).isEqualTo(model);
    assertThat(first.path("sourceCode").asString()).isNotBlank();
    assertThat(first.toString())
        .doesNotContain("specVersion", "private-format", "parameters", "expectedOutput");
    assertThat(counts()).isEqualTo(before);
    assertThat(jdbc.queryForMap("SELECT * FROM problem.problems WHERE id=?", problem))
        .isEqualTo(state);
  }

  @Test
  void publishedProblemIsReadableForAllAuthenticatedBaseCategories() throws Exception {
    publish();
    studentGet(200);
    fetch(path(problem, "PYTHON", "FUNCTION_WRAPPER"), other, BaseCategory.INSTRUCTOR, 200);
    fetch(path(problem, "PYTHON", "FUNCTION_WRAPPER"), admin, BaseCategory.ADMIN, 200);
    assertThat(
            mvc.perform(get(path(problem, "PYTHON", "FUNCTION_WRAPPER")))
                .andReturn()
                .getResponse()
                .getStatus())
        .isEqualTo(401);
  }

  @Test
  void privateMissingAndDeletedAreIndistinguishableEvenWithPoisonedSpecCache() throws Exception {
    redis.opsForValue().set("problem:spec:" + problem, "poison-private-cached-signature");
    fetch(path(problem, "PYTHON", "FUNCTION_WRAPPER"), author, BaseCategory.INSTRUCTOR, 200);
    fetch(path(problem, "PYTHON", "FUNCTION_WRAPPER"), admin, BaseCategory.ADMIN, 200);
    var denied = studentGet(404);
    var missing =
        fetch(
            path(UUID.randomUUID(), "PYTHON", "FUNCTION_WRAPPER"),
            student,
            BaseCategory.STUDENT,
            404);
    hidden(denied);
    hidden(missing);
    assertThat(denied.path("detail")).isEqualTo(missing.path("detail"));
    hidden(fetch(path(problem, "PYTHON", "FUNCTION_WRAPPER"), other, BaseCategory.INSTRUCTOR, 404));
    jdbc.update("UPDATE problem.problems SET deleted=true WHERE id=?", problem);
    hidden(
        fetch(path(problem, "PYTHON", "FUNCTION_WRAPPER"), author, BaseCategory.INSTRUCTOR, 404));
    hidden(fetch(path(problem, "PYTHON", "FUNCTION_WRAPPER"), admin, BaseCategory.ADMIN, 404));
    hidden(studentGet(404));
  }

  @Test
  void callerAndCategoryParametersCannotImpersonateTheAuthorOrAdmin() throws Exception {
    var response =
        withToken(
            path(problem, "PYTHON", "FUNCTION_WRAPPER")
                + "&callerUserId="
                + author
                + "&baseCategory=ADMIN",
            token(student, BaseCategory.STUDENT),
            404);
    hidden(response);
  }

  @Test
  void demotedAdminCannotKeepPrivateAccessWithTheSameSignedToken() throws Exception {
    String bearer = token(admin, BaseCategory.ADMIN);
    withToken(path(problem, "PYTHON", "FUNCTION_WRAPPER"), bearer, 200);
    jdbc.update(
        "UPDATE identity.users SET role_id=(SELECT id FROM identity.roles WHERE code='STUDENT') WHERE id=?",
        admin);
    hidden(withToken(path(problem, "PYTHON", "FUNCTION_WRAPPER"), bearer, 404));
  }

  @Test
  void languageIsValidatedBeforeCallingTheReaderAndBadModelIs400() throws Exception {
    for (String language : List.of("GO", "java", "")) {
      assertThat(
              fetch(path(problem, language, "STANDARD_IO"), author, BaseCategory.INSTRUCTOR, 400)
                  .path("errorCode")
                  .asString())
          .isEqualTo("HRN-010");
    }
    withToken(
        "/api/v1/harness/problems/" + problem + "/starter-code?model=STANDARD_IO",
        token(author, BaseCategory.INSTRUCTOR),
        400);
    withToken(path(problem, "JAVA", "OTHER"), token(author, BaseCategory.INSTRUCTOR), 400);
    withToken(
        "/api/v1/harness/problems/" + problem + "/starter-code?language=JAVA",
        token(author, BaseCategory.INSTRUCTOR),
        400);
    verify(reader, never()).findSpec(any());
    var malformed =
        withToken(
            "/api/v1/harness/problems/not-uuid/starter-code?language=JAVA&model=STANDARD_IO",
            token(author, BaseCategory.INSTRUCTOR),
            404);
    hidden(malformed);
  }

  @Test
  void wrapperErrorsRespectSignatureFirstAndStandardIoStillWorks() throws Exception {
    jdbc.update("UPDATE problem.problems SET function_wrapper_supported=false WHERE id=?", problem);
    assertThat(
            fetch(path(problem, "JAVA", "FUNCTION_WRAPPER"), author, BaseCategory.INSTRUCTOR, 422)
                .path("errorCode")
                .asString())
        .isEqualTo("HRN-004");
    fetch(path(problem, "JAVA", "STANDARD_IO"), author, BaseCategory.INSTRUCTOR, 200);
    jdbc.update(
        "UPDATE problem.problem_specs SET function_name=null,return_type=null,parameters=null WHERE problem_id=?",
        problem);
    assertThat(
            fetch(path(problem, "JAVA", "FUNCTION_WRAPPER"), author, BaseCategory.INSTRUCTOR, 422)
                .path("errorCode")
                .asString())
        .isEqualTo("HRN-005");
    fetch(path(problem, "PYTHON", "STANDARD_IO"), author, BaseCategory.INSTRUCTOR, 200);
  }

  @Test
  void malformedStoredSignatureIs500WithNoUpstreamData() throws Exception {
    jdbc.update(
        "UPDATE problem.problem_specs SET return_type='{\"kind\":\"PRIVATE_UNKNOWN_KIND\"}'::jsonb WHERE problem_id=?",
        problem);
    var response =
        fetch(path(problem, "JAVA", "FUNCTION_WRAPPER"), author, BaseCategory.INSTRUCTOR, 500);
    assertThat(response.path("errorCode").asString()).isEqualTo("HRN-006");
    assertThat(response.toString())
        .doesNotContain("PRIVATE_UNKNOWN_KIND", "sourceCode", "SQLException", "private-format");
  }

  @Test
  void portFailureIs503AndRecoveryReturnsCurrentCode() throws Exception {
    publish();
    doThrow(new IllegalStateException("private SQL")).when(reader).findSpec(problem);
    var response = studentGet(503);
    assertThat(response.path("errorCode").asString()).isEqualTo("HRN-003");
    assertThat(response.toString()).doesNotContain("private SQL", "sourceCode", "echo");
    doCallRealMethod().when(reader).findSpec(problem);
    assertThat(studentGet(200).path("sourceCode").asString()).contains("echo(");
  }

  @Test
  void permissionPortFailureIs503InsteadOfGrantingOrMisreportingMissing() throws Exception {
    doThrow(new IllegalStateException("private scope SQL"))
        .when(permissions)
        .canRead(any(), any(), anyString());
    var response =
        fetch(path(problem, "JAVA", "STANDARD_IO"), author, BaseCategory.INSTRUCTOR, 503);
    assertThat(response.path("errorCode").asString()).isEqualTo("HRN-003");
    assertThat(response.toString()).doesNotContain("private scope SQL", "sourceCode");
  }

  @Test
  void noLocalCacheReadsFreshVersionAndMatchingWithoutWaitingForEvents() throws Exception {
    publish();
    assertThat(studentGet(200).path("sourceCode").asString()).contains("echo(");
    jdbc.update("UPDATE problem.problems SET status='UNPUBLISHED' WHERE id=?", problem);
    saveSpec("solveNew", "INT", "TRIMMED");
    publish();
    redis.opsForValue().set("problem:spec:" + problem, "poison-v1-echo");
    for (int i = 0; i < 3; i++) {
      var result = studentGet(200);
      assertThat(result.path("sourceCode").asString())
          .contains("solve_new(")
          .doesNotContain("echo(");
    }
    var query =
        new GetStarterCodeQuery(
            problem, Language.JAVA, SubmissionModel.FUNCTION_WRAPPER, student, "STUDENT");
    assertThat(starter.getStarterCode(query).specVersion()).isEqualTo(2);
    var command =
        new PrepareSubmissionCommand(
            problem,
            Language.JAVA,
            SubmissionModel.FUNCTION_WRAPPER,
            "class Solution { public int solveNew(int value) { return value; } }",
            2,
            "fixture");
    var prepared = prepare.prepare(command);
    assertThat(prepared.specVersion()).isEqualTo(2);
    assertThat(prepared.matchSpec().matchingStrategy().name()).isEqualTo("TRIMMED");
    assertThatThrownBy(
            () ->
                prepare.prepare(
                    new PrepareSubmissionCommand(
                        problem,
                        Language.JAVA,
                        SubmissionModel.FUNCTION_WRAPPER,
                        command.sourceCode(),
                        1,
                        "fixture")))
        .isInstanceOfSatisfying(
            AlgoPrepException.class, e -> assertThat(e.errorCode()).isEqualTo("HRN-002"));
    assertThat(redis.opsForValue().get("problem:spec:" + problem)).isEqualTo("poison-v1-echo");
    assertThat(context.getBeansOfType(ProblemSpecReaderPort.class)).hasSize(1);
  }

  @Test
  void nameOverrideOnlyChangesTheSelectedLanguage() throws Exception {
    jdbc.update(
        "UPDATE problem.problem_specs SET name_overrides='{\"JAVA\":\"echoCustom\"}'::jsonb WHERE problem_id=?",
        problem);
    assertThat(
            fetch(path(problem, "JAVA", "FUNCTION_WRAPPER"), author, BaseCategory.INSTRUCTOR, 200)
                .path("sourceCode")
                .asString())
        .contains("echoCustom(");
    assertThat(
            fetch(path(problem, "CPP", "FUNCTION_WRAPPER"), author, BaseCategory.INSTRUCTOR, 200)
                .path("sourceCode")
                .asString())
        .contains("echo(")
        .doesNotContain("echoCustom");
    assertThat(
            fetch(path(problem, "PYTHON", "FUNCTION_WRAPPER"), author, BaseCategory.INSTRUCTOR, 200)
                .path("sourceCode")
                .asString())
        .contains("echo(")
        .doesNotContain("echoCustom");
  }

  @Test
  void savedStarterCanBePreparedAndRunByTheRealF4SamplePipeline() throws Exception {
    publish();
    jdbc.update(
        "INSERT INTO problem.testcases(id,problem_id,visibility,position,approved,input_storage,input_inline,expected_output_storage,expected_output_inline) VALUES(?,?,'SAMPLE',1,true,'INLINE','7','INLINE','7')",
        UUID.randomUUID(),
        problem);
    var before = counts();
    var source = studentGet(200).path("sourceCode").asString();
    assertThat(source).contains("        pass");
    var solution = source.replace("        pass", "        return value");
    var body =
        json.createObjectNode()
            .put("language", "PYTHON")
            .put("submissionModel", "FUNCTION_WRAPPER")
            .put("sourceCode", solution);
    var response =
        mvc.perform(
                post("/api/v1/judge/problems/" + problem + "/run-sample")
                    .header("Authorization", "Bearer " + token(student, BaseCategory.STUDENT))
                    .contentType("application/json")
                    .content(json.writeValueAsString(body)))
            .andReturn()
            .getResponse();
    assertThat(response.getStatus()).as(response.getContentAsString()).isEqualTo(200);
    assertThat(json.readTree(response.getContentAsString()).path("passedCount").asInt())
        .isEqualTo(1);
    assertThat(counts()).isEqualTo(before);
  }
}
