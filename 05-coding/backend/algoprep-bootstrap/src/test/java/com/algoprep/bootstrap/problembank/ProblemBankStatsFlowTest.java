package com.algoprep.bootstrap.problembank;

import static org.assertj.core.api.Assertions.assertThat;

import com.algoprep.problembank.application.ports.in.ProblemBankStatsUseCase;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

/**
 * The statistics gate for identity (DD F2 58.1) against PostgreSQL: the level catalogue and the
 * per-problem summaries the admin overview groups judge counts by.
 */
@SpringBootTest(
    properties =
        "spring.autoconfigure.exclude=org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration")
@ActiveProfiles("test")
@Testcontainers
class ProblemBankStatsFlowTest {
  private static final String BUCKET = "algoprep-testcases";

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
          .withEnv("MINIO_ROOT_USER", "test-stats-user")
          .withEnv("MINIO_ROOT_PASSWORD", "test-stats-password")
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
    properties.add("algoprep.problem-bank.storage.access-key", () -> "test-stats-user");
    properties.add("algoprep.problem-bank.storage.secret-key", () -> "test-stats-password");
  }

  @Autowired ProblemBankStatsUseCase stats;
  @Autowired JdbcTemplate jdbc;

  @Autowired
  @Qualifier("testcaseServiceClient")
  MinioClient minio;

  private String levelEasy;

  @BeforeEach
  void setup() throws Exception {
    if (!minio.bucketExists(BucketExistsArgs.builder().bucket(BUCKET).build())) {
      minio.makeBucket(MakeBucketArgs.builder().bucket(BUCKET).build());
    }
    jdbc.update("DELETE FROM problem.class_assignments");
    jdbc.update("DELETE FROM problem.outbox_events");
    jdbc.update("DELETE FROM problem.problems");
    levelEasy =
        jdbc.queryForObject(
            "SELECT id::text FROM problem.problem_levels ORDER BY sort_order LIMIT 1",
            String.class);
  }

  @Test
  void theLevelCatalogHasEveryLevelBySortOrderEvenWithoutProblems() {
    var levels = stats.levelCatalog();

    int expected =
        jdbc.queryForObject("SELECT count(*) FROM problem.problem_levels", Integer.class);
    assertThat(levels).hasSize(expected);
    assertThat(levels).extracting("sortOrder").isSorted();
    assertThat(levels.get(0).id().toString()).isEqualTo(levelEasy);
    assertThat(levels).allSatisfy(l -> assertThat(l.displayName()).isNotBlank());
  }

  @Test
  void summariesFlagWithdrawnProblemsAndSkipUnknownIds() {
    var live = problem("LIVE", "PUBLISHED", false);
    var draft = problem("DRAFT", "UNPUBLISHED", false);
    var gone = problem("GONE", "UNPUBLISHED", true);

    var found = stats.problemSummaries(Set.of(live, draft, gone, UUID.randomUUID()));

    assertThat(found).hasSize(3);
    var byId = new java.util.HashMap<UUID, Boolean>();
    found.forEach(p -> byId.put(p.problemId(), p.published()));
    assertThat(byId)
        .containsEntry(live, true)
        .containsEntry(draft, false)
        .containsEntry(gone, false);
    var liveRow = found.stream().filter(p -> p.problemId().equals(live)).findFirst().orElseThrow();
    assertThat(liveRow.title()).isEqualTo("Title LIVE");
    assertThat(liveRow.levelId().toString()).isEqualTo(levelEasy);
    assertThat(liveRow.levelCode()).isNotBlank();
    assertThat(liveRow.levelDisplayName()).isNotBlank();
  }

  @Test
  void noIdsMeansNoRows() {
    assertThat(stats.problemSummaries(Set.of())).isEmpty();
  }

  private UUID problem(String code, String status, boolean deleted) {
    var id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO problem.problems(id,code,title,statement_md,author_id,level_id,status,deleted,time_limit_ms,memory_limit_mb,max_output_size_kb) VALUES (?,?,?,'statement',?,?::uuid,?,?,1000,256,64)",
        id,
        code,
        "Title " + code,
        UUID.randomUUID(),
        levelEasy,
        deleted ? "UNPUBLISHED" : status,
        deleted);
    return id;
  }
}
