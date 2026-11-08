package com.algoprep.bootstrap.harness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.algoprep.identity.application.ports.out.AccessTokenPort;
import com.algoprep.identity.domain.model.BaseCategory;
import com.algoprep.problembank.application.dto.SaveProblemSpecCommand;
import com.algoprep.problembank.application.ports.in.SaveProblemSpecUseCase;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
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
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** AC-49 over real HTTP: JWT roles, the 16,384 byte cap, the 10/s throttle, and the save match. */
@SpringBootTest(
    properties =
        "spring.autoconfigure.exclude=org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration")
@ActiveProfiles("test")
@Testcontainers
class PreviewFunctionSignatureFlowTest {
  private static final String PREVIEW = "/api/v1/harness/function-signature/preview";
  private static final int CAP = 16_384;

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
  @Autowired AccessTokenPort access;
  @Autowired SaveProblemSpecUseCase save;
  @Autowired WebApplicationContext context;
  private final JsonMapper json = JsonMapper.builder().findAndAddModules().build();
  private MockMvc mvc;
  private UUID author;

  @BeforeEach
  void fixture() {
    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    author = user("INSTRUCTOR");
  }

  private UUID user(String role) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO identity.users(id,email,display_name,role_id,status) SELECT ?,?,'Preview user',id,'ACTIVE' FROM identity.roles WHERE code=?",
        id,
        id + "@example.test",
        role);
    return id;
  }

  private MockHttpServletResponse call(UUID user, BaseCategory category, byte[] body)
      throws Exception {
    var request = post(PREVIEW).contentType(MediaType.APPLICATION_JSON).content(body);
    if (user != null) {
      String token = access.issue(user, category, category.name(), UUID.randomUUID()).value();
      request = request.header("Authorization", "Bearer " + token);
    }
    return mvc.perform(request).andReturn().getResponse();
  }

  private JsonNode ok(UUID user, String body) throws Exception {
    var response = call(user, BaseCategory.INSTRUCTOR, body.getBytes(StandardCharsets.UTF_8));
    assertThat(response.getStatus()).as(response.getContentAsString()).isEqualTo(200);
    return json.readTree(response.getContentAsString());
  }

  private static String draft(String returnKind) {
    return "{\"functionName\":\"echo\",\"returnType\":{\"kind\":\""
        + returnKind
        + "\"},\"parameters\":[{\"name\":\"value\",\"type\":{\"kind\":\"INT\"}}]}";
  }

  private JsonNode refused(String body, int status, String code) throws Exception {
    var response = call(author, BaseCategory.INSTRUCTOR, body.getBytes(StandardCharsets.UTF_8));
    assertThat(response.getStatus()).as(response.getContentAsString()).isEqualTo(status);
    var error = json.readTree(response.getContentAsString());
    assertThat(error.path("errorCode").asString()).isEqualTo(code);
    return error;
  }

  @Test
  void anonymousIs401AndAStudentIs403() throws Exception {
    assertThat(call(null, null, draft("INT").getBytes(StandardCharsets.UTF_8)).getStatus())
        .isEqualTo(401);
    assertThat(
            call(
                    user("STUDENT"),
                    BaseCategory.STUDENT,
                    draft("INT").getBytes(StandardCharsets.UTF_8))
                .getStatus())
        .isEqualTo(403);
  }

  @Test
  void instructorAndAdminGetThreeDeclarationsThatFollowTheDraft() throws Exception {
    var first = ok(author, draft("INT"));
    assertThat(first.path("wrapperSupported").asBoolean()).isTrue();
    assertThat(first.path("declarations").size()).isEqualTo(3);
    for (String language : List.of("JAVA", "CPP", "PYTHON")) {
      assertThat(first.path("declarations").path(language).asString()).contains("echo");
    }
    var changed = ok(user("ADMIN"), draft("STRING"));
    assertThat(changed.path("declarations").path("JAVA").asString())
        .isNotEqualTo(first.path("declarations").path("JAVA").asString());
    assertThat(changed.path("declarations").path("PYTHON").asString())
        .isNotEqualTo(first.path("declarations").path("PYTHON").asString());
  }

  @Test
  void anEmptyDraftHasNoDeclarationLinesAndAHalfFilledOneIsAHardError() throws Exception {
    var result = ok(author, "{}");
    assertThat(result.path("wrapperSupported").asBoolean()).isFalse();
    assertThat(result.path("unsupportedReasons").get(0).path("code").asString())
        .isEqualTo("HRN-V07");
    for (String language : List.of("JAVA", "CPP", "PYTHON")) {
      assertThat(result.path("declarations").path(language).isNull()).isTrue();
    }
    // DD validation HRN-V17: a name without the rest of the signature is a hard violation.
    var half = refused("{\"returnType\":{\"kind\":\"INT\"}}", 422, "HRN-060");
    assertThat(half.path("details").path("violations").get(0).path("code").asString())
        .isEqualTo("HRN-V17");
  }

  @Test
  void aWrongDraftIsReportedAsHrn060WithPathsAndNothingIsSaved() throws Exception {
    long problems = jdbc.queryForObject("SELECT count(*) FROM problem.problems", Long.class);
    long specs = jdbc.queryForObject("SELECT count(*) FROM problem.problem_specs", Long.class);
    var badShape =
        refused(
            "{\"functionName\":\"echo\",\"returnType\":{\"kind\":\"INT\",\"extra\":1}}",
            422,
            "HRN-060");
    assertThat(badShape.path("details").path("violations").size()).isPositive();
    refused("{\"functionName\":\"echo\",\"unknownField\":true}", 422, "HRN-060");
    refused("{not json", 422, "HRN-060");
    refused("[]", 422, "HRN-060");
    refused(
        "{\"functionName\":\"echo\",\"returnType\":{\"kind\":\"INT\"},\"epsilonValue\":-1}",
        422,
        "HRN-060");
    assertThat(jdbc.queryForObject("SELECT count(*) FROM problem.problems", Long.class))
        .isEqualTo(problems);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM problem.problem_specs", Long.class))
        .isEqualTo(specs);
  }

  @Test
  void theBodyCapIsExactlySixteenKibibytesOfUtf8() throws Exception {
    String base = draft("INT");
    byte[] atCap = padded(base, CAP);
    byte[] overCap = padded(base, CAP + 1);
    assertThat(call(author, BaseCategory.INSTRUCTOR, atCap).getStatus()).isEqualTo(200);
    var response = call(author, BaseCategory.INSTRUCTOR, overCap);
    assertThat(response.getStatus()).isEqualTo(413);
    assertThat(json.readTree(response.getContentAsString()).path("errorCode").asString())
        .isEqualTo("HRN-062");
    // Multi-byte characters count as bytes, not as characters.
    String wide = "{\"functionName\":\"" + "é".repeat(CAP / 2 + 1) + "\"}";
    assertThat(wide.length()).isLessThan(CAP);
    assertThat(
            call(author, BaseCategory.INSTRUCTOR, wide.getBytes(StandardCharsets.UTF_8))
                .getStatus())
        .isEqualTo(413);
  }

  private static byte[] padded(String body, int size) {
    byte[] bytes = new byte[size];
    Arrays.fill(bytes, (byte) ' ');
    System.arraycopy(body.getBytes(StandardCharsets.UTF_8), 0, bytes, 0, body.length());
    return bytes;
  }

  @Test
  void moreThanTenCallsInOneSecondGetHrn061WithRetryAfter() throws Exception {
    UUID heavy = user("INSTRUCTOR");
    ok(heavy, draft("INT"));
    int limited = 0;
    JsonNode error = null;
    for (int i = 0; i < 40 && limited == 0; i++) {
      var response =
          call(heavy, BaseCategory.INSTRUCTOR, draft("INT").getBytes(StandardCharsets.UTF_8));
      if (response.getStatus() == 429) {
        limited++;
        error = json.readTree(response.getContentAsString());
      }
    }
    assertThat(limited).as("a 429 within 40 rapid calls").isEqualTo(1);
    assertThat(error.path("errorCode").asString()).isEqualTo("HRN-061");
    assertThat(error.path("details").path("retryAfterSeconds").asInt()).isGreaterThanOrEqualTo(1);
    // Another author is not throttled by this one.
    assertThat(
            call(author, BaseCategory.INSTRUCTOR, draft("INT").getBytes(StandardCharsets.UTF_8))
                .getStatus())
        .isEqualTo(200);
  }

  @Test
  void thePreviewMatchesTheStarterCodeAfterTheSpecIsSaved() throws Exception {
    UUID problem = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO problem.problems(id,code,title,author_id,updated_by,level_id,time_limit_ms,memory_limit_mb,max_output_size_kb) VALUES(?,?,'Preview match',?,?,'20000000-0000-4000-8000-000000000001',1000,256,64)",
        problem,
        problem.toString(),
        author,
        author);
    var body = json.readTree(draft("INT"));
    var spec =
        json.createObjectNode()
            .put("functionName", "echo")
            .put("matchingStrategy", "EXACT")
            .put("stdinFormatMd", "x")
            .put("stdoutFormatMd", "x");
    spec.set("returnType", body.path("returnType"));
    spec.set("parameters", body.path("parameters"));
    spec.set("nameOverrides", json.createObjectNode());
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
            spec));
    var preview = ok(author, draft("INT"));
    String token =
        access.issue(author, BaseCategory.INSTRUCTOR, "INSTRUCTOR", UUID.randomUUID()).value();
    for (String language : List.of("JAVA", "CPP", "PYTHON")) {
      var starter =
          mvc.perform(
                  get("/api/v1/harness/problems/"
                          + problem
                          + "/starter-code?language="
                          + language
                          + "&model=FUNCTION_WRAPPER")
                      .header("Authorization", "Bearer " + token))
              .andReturn()
              .getResponse();
      assertThat(starter.getStatus()).as(starter.getContentAsString()).isEqualTo(200);
      String source = json.readTree(starter.getContentAsString()).path("sourceCode").asString();
      assertThat(source).contains(preview.path("declarations").path(language).asString());
    }
  }
}
