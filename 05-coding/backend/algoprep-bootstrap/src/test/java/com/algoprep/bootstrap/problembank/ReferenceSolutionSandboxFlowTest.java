package com.algoprep.bootstrap.problembank;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.algoprep.identity.application.ports.out.AccessTokenPort;
import com.algoprep.identity.domain.model.BaseCategory;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import java.sql.Timestamp;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * #39 against the real chain: the reference solution is wrapped by the harness and run by a real
 * go-judge with toolchains. Start the sandbox from {@code docker-compose.worker-sandbox.yml} and
 * pass {@code -Djudge.go-judge.base-url=http://127.0.0.1:5055}; without it the test is skipped,
 * like {@code ExecutionBridgeFlowTest}.
 */
@EnabledIfSystemProperty(named = "judge.go-judge.base-url", matches = "http://127.0.0.1:[0-9]+")
@SpringBootTest(
    properties =
        "spring.autoconfigure.exclude=org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration")
@ActiveProfiles("test")
@Testcontainers
class ReferenceSolutionSandboxFlowTest {
  private static final JsonMapper JSON = JsonMapper.builder().build();
  private static final String LEVEL = "20000000-0000-4000-8000-000000000001";

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
  static final GenericContainer<?> MINIO =
      new GenericContainer<>("minio/minio:RELEASE.2025-09-07T16-13-09Z")
          .withEnv("MINIO_ROOT_USER", "test-e2e-user")
          .withEnv("MINIO_ROOT_PASSWORD", "test-e2e-password")
          .withCommand("server", "/data")
          .withExposedPorts(9000)
          .waitingFor(Wait.forHttp("/minio/health/ready").forPort(9000));

  @DynamicPropertySource
  static void infrastructure(DynamicPropertyRegistry properties) {
    properties.add("spring.data.redis.host", REDIS::getHost);
    properties.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    properties.add(
        "algoprep.problem-bank.storage.endpoint",
        () -> "http://" + MINIO.getHost() + ":" + MINIO.getMappedPort(9000));
    properties.add("algoprep.problem-bank.storage.access-key", () -> "test-e2e-user");
    properties.add("algoprep.problem-bank.storage.secret-key", () -> "test-e2e-password");
  }

  @Autowired WebApplicationContext context;
  @Autowired JdbcTemplate jdbc;
  @Autowired AccessTokenPort tokens;

  @Autowired
  @Qualifier("testcaseServiceClient")
  MinioClient minio;

  private MockMvc mvc;
  private UUID teacherId;
  private String teacher;

  @BeforeEach
  void setup() throws Exception {
    if (!minio.bucketExists(BucketExistsArgs.builder().bucket("algoprep-testcases").build())) {
      minio.makeBucket(MakeBucketArgs.builder().bucket("algoprep-testcases").build());
    }
    jdbc.update(
        "UPDATE judge.language_configs SET enabled=true,time_limit_multiplier=3,memory_limit_multiplier=1");
    jdbc.update(
        "UPDATE judge.sandbox_settings SET network_access_enabled=false,child_process_limit_enabled=true");
    jdbc.update("UPDATE judge.grading_defaults SET compile_timeout_sec=30");
    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    teacherId = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO identity.users(id,email,display_name,role_id,status) SELECT ?,?,'E2E teacher',id,'ACTIVE' FROM identity.roles WHERE code='INSTRUCTOR'",
        teacherId,
        teacherId + "@test.local");
    teacher =
        tokens.issue(teacherId, BaseCategory.INSTRUCTOR, "INSTRUCTOR", UUID.randomUUID()).value();
  }

  @Test
  void aCorrectReferenceSolutionPublishesAndAWrongOneIsRefusedByTheRealSandbox() throws Exception {
    var good = problem("class Solution:\n def echo(self,value):\n  return value\n");

    var published = publish(good);

    assertThat(published.getStatus()).as(published.getContentAsString()).isEqualTo(200);
    assertThat(JSON.readTree(published.getContentAsString()).path("status").asString())
        .isEqualTo("PUBLISHED");
    assertThat(
            jdbc.queryForObject(
                "SELECT status FROM problem.problems WHERE id=?", String.class, good))
        .isEqualTo("PUBLISHED");

    var bad = problem("class Solution:\n def echo(self,value):\n  return value + 1\n");

    var refused = publish(bad);

    assertThat(refused.getStatus()).as(refused.getContentAsString()).isEqualTo(422);
    JsonNode json = JSON.readTree(refused.getContentAsString());
    assertThat(json.path("errorCode").asString()).isEqualTo("PRB-030");
    assertThat(json.path("details").path("items").get(0).path("code").asString())
        .isEqualTo("REFERENCE_SOLUTION_PASSES_ALL");
    assertThat(
            jdbc.queryForObject(
                "SELECT status FROM problem.problems WHERE id=?", String.class, bad))
        .isEqualTo("UNPUBLISHED");
  }

  private MockHttpServletResponse publish(UUID id) throws Exception {
    var stamp =
        jdbc.queryForObject(
                "SELECT updated_at FROM problem.problems WHERE id=?", Timestamp.class, id)
            .toInstant();
    return mvc.perform(
            put("/api/v1/problem-bank/authoring/problems/" + id + "/status")
                .header("Authorization", "Bearer " + teacher)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"PUBLISHED\",\"expectedUpdatedAt\":\"" + stamp + "\"}"))
        .andReturn()
        .getResponse();
  }

  /** A function-wrapper problem that echoes an INT, with eight approved cases and two examples. */
  private UUID problem(String referenceSource) {
    var id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO problem.problems(id,code,title,statement_md,author_id,level_id,time_limit_ms,memory_limit_mb,max_output_size_kb) VALUES (?,?,'Echo','statement',?,?::uuid,2000,256,64)",
        id,
        id.toString(),
        teacherId,
        LEVEL);
    jdbc.update(
        "INSERT INTO problem.problem_specs(id,problem_id,matching_strategy,spec_version,stdin_format_md,stdout_format_md,function_name,return_type,parameters) VALUES (?,?,'EXACT',1,'one integer','one integer','echo','{\"kind\":\"INT\"}'::jsonb,'[{\"name\":\"value\",\"type\":{\"kind\":\"INT\"}}]'::jsonb)",
        UUID.randomUUID(),
        id);
    jdbc.update(
        "INSERT INTO problem.problem_reference_solutions(id,problem_id,language,source_code) VALUES (?,?,'PYTHON',?)",
        UUID.randomUUID(),
        id,
        referenceSource);
    for (int i = 1; i <= 2; i++) {
      jdbc.update(
          "INSERT INTO problem.worked_examples(id,problem_id,position,input,output) VALUES (?,?,?,?,?)",
          UUID.randomUUID(),
          id,
          i,
          "i" + i,
          "o" + i);
    }
    for (int i = 1; i <= 8; i++) {
      jdbc.update(
          "INSERT INTO problem.testcases(id,problem_id,visibility,position,approved,input_storage,input_inline,expected_output_storage,expected_output_inline) VALUES (?,?,?,?,true,'INLINE',?,'INLINE',?)",
          UUID.randomUUID(),
          id,
          i <= 2 ? "SAMPLE" : "HIDDEN",
          i,
          i + "\n",
          i + "\n");
    }
    return id;
  }
}
