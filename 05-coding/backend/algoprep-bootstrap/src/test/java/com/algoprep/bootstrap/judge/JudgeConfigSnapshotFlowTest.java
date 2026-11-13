package com.algoprep.bootstrap.judge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import com.algoprep.judge.application.ports.in.GetAcceptedSubmissionSnapshotUseCase;
import com.algoprep.judge.application.ports.in.GetEnabledJudgeLanguagesUseCase;
import com.algoprep.judge.application.ports.in.GetGeneratorSandboxLimitsUseCase;
import com.algoprep.judge.application.ports.in.GetLanguageMultipliersUseCase;
import com.algoprep.judge.application.ports.in.ReadGradingDefaultsUseCase;
import com.algoprep.judge.domain.exception.JudgeReadUnavailableException;
import com.algoprep.judge.domain.exception.SubmissionNotAcceptedException;
import com.algoprep.judge.domain.exception.SubmissionNotFoundException;
import com.algoprep.judge.domain.model.GeneratorSandboxLimits;
import com.algoprep.judge.domain.model.LanguageMultipliers;
import com.algoprep.judge.domain.ports.out.JudgeConfigRepository;
import com.algoprep.judge.domain.ports.out.SubmissionSnapshotRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;
import tools.jackson.databind.json.JsonMapper;

/** Trusted internal P3/P4 reads; caller F2/F5 HTTP/authorization is outside this slice. */
@SpringBootTest(
    properties =
        "spring.autoconfigure.exclude=org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration")
@ActiveProfiles("test")
@Testcontainers
@Transactional
class JudgeConfigSnapshotFlowTest {
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
  @Autowired GetGeneratorSandboxLimitsUseCase generator;
  @Autowired GetEnabledJudgeLanguagesUseCase languages;
  @Autowired GetLanguageMultipliersUseCase multipliers;
  @Autowired ReadGradingDefaultsUseCase defaults;
  @Autowired GetAcceptedSubmissionSnapshotUseCase snapshots;
  @MockitoSpyBean JudgeConfigRepository configRepository;
  @MockitoSpyBean SubmissionSnapshotRepository snapshotRepository;

  @BeforeEach
  void setup() {
    SecurityContextHolder.clearContext();
  }

  @AfterEach
  void clear() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void p3ReadsLiveGeneratorSettingsAndRetainsTheExistingFourDefaultsContract() {
    jdbc.update(
        "UPDATE judge.generator_sandbox_settings SET max_runtime_seconds=45,max_output_mb=200 WHERE id=1");
    assertThat(generator.getGeneratorSandboxLimits())
        .isEqualTo(new GeneratorSandboxLimits(45, 200));
    jdbc.update("UPDATE judge.generator_sandbox_settings SET max_runtime_seconds=60 WHERE id=1");
    assertThat(generator.getGeneratorSandboxLimits())
        .isEqualTo(new GeneratorSandboxLimits(60, 200));
    jdbc.update(
        "UPDATE judge.grading_defaults SET time_limit_ms=1200,memory_limit_mb=320,output_limit_kb=96,compile_timeout_sec=12 WHERE id=1");
    var current = defaults.getGradingDefaults();
    assertThat(current.timeLimitMs()).isEqualTo(1200);
    assertThat(current.memoryLimitMb()).isEqualTo(320);
    assertThat(current.maxOutputSizeKb()).isEqualTo(96);
    assertThat(current.compileTimeoutSeconds()).isEqualTo(12);
  }

  @Test
  void p3FiltersEnabledRowsAndReadsExactMultipliersEvenForDisabledLanguages() {
    jdbc.update("UPDATE judge.language_configs SET enabled=false WHERE language='PYTHON'");
    jdbc.update(
        "UPDATE judge.language_configs SET time_limit_multiplier=2.25,memory_limit_multiplier=1.50 WHERE language='JAVA'");
    assertThat(languages.getEnabledLanguages()).containsExactly("CPP", "JAVA");
    assertThat(multipliers.getLanguageMultipliers("JAVA"))
        .isEqualTo(new LanguageMultipliers(new BigDecimal("2.25"), new BigDecimal("1.50")));
    assertThat(multipliers.getLanguageMultipliers("PYTHON").timeMultiplier())
        .isEqualByComparingTo("3.00");
    jdbc.update(
        "UPDATE judge.language_configs SET enabled=true,time_limit_multiplier=4.50 WHERE language='PYTHON'");
    assertThat(languages.getEnabledLanguages()).containsExactly("CPP", "JAVA", "PYTHON");
    assertThat(multipliers.getLanguageMultipliers("PYTHON").timeMultiplier())
        .isEqualByComparingTo("4.50");
    assertThatThrownBy(() -> multipliers.getLanguageMultipliers("RUST"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  private UUID submission(String status, Integer runtime, Integer memory) {
    UUID id = UUID.randomUUID();
    boolean terminal = !List.of("PENDING", "COMPILING", "JUDGING").contains(status);
    jdbc.update(
        """
        INSERT INTO judge.submissions(id,user_id,problem_id,language,submission_model,source_code,
          source_input_method,status,failure_reason,testcase_set_version_at_grading,runtime_ms,memory_kb,finished_at)
        VALUES(?,?,?,'JAVA','STANDARD_IO','private-source-secret','EDITOR',?,?,1,?,?,CAST(? AS timestamptz))
        """,
        id,
        UUID.randomUUID(),
        UUID.randomUUID(),
        status,
        status.equals("SYSTEM_ERROR") ? "SANDBOX_FAULT" : null,
        runtime,
        memory,
        terminal ? "2026-11-01T00:00:00Z" : null);
    return id;
  }

  @Test
  void p4ReturnsExactlySevenContractedFieldsWithoutAnOwnerGateOrTestcaseContent() {
    UUID id = submission("ACCEPTED", 19, 2048);
    var snapshot = snapshots.getAccepted(id);
    assertThat(snapshot.submissionId()).isEqualTo(id);
    assertThat(snapshot.userId())
        .isEqualTo(
            jdbc.queryForObject(
                "SELECT user_id FROM judge.submissions WHERE id=?", UUID.class, id));
    assertThat(snapshot.problemId())
        .isEqualTo(
            jdbc.queryForObject(
                "SELECT problem_id FROM judge.submissions WHERE id=?", UUID.class, id));
    assertThat(snapshot.language()).isEqualTo("JAVA");
    assertThat(snapshot.sourceCode()).isEqualTo("private-source-secret");
    assertThat(snapshot.runtimeMs()).isEqualTo(19);
    assertThat(snapshot.memoryKb()).isEqualTo(2048);
    var json = JsonMapper.builder().findAndAddModules().build().valueToTree(snapshot);
    assertThat(json.size()).isEqualTo(7);
    assertThat(json.toString())
        .doesNotContain("testcase", "expectedOutput", "stderr", "diff", "HIDDEN");
    assertThat(snapshot.toString()).doesNotContain("private-source-secret");
  }

  @Test
  void p4RejectsAllNonAcceptedStatesWithoutReturningSourceAndDistinguishesMissingIds() {
    for (String state :
        List.of(
            "PENDING",
            "COMPILING",
            "JUDGING",
            "WRONG_ANSWER",
            "TIME_LIMIT_EXCEEDED",
            "RUNTIME_ERROR",
            "COMPILE_ERROR",
            "SYSTEM_ERROR")) {
      UUID id = submission(state, null, null);
      assertThatThrownBy(() -> snapshots.getAccepted(id))
          .isInstanceOf(SubmissionNotAcceptedException.class)
          .hasNoCause();
      assertThat(snapshotRepository.find(id).orElseThrow().snapshot()).isNull();
    }
    assertThatThrownBy(() -> snapshots.getAccepted(UUID.randomUUID()))
        .isInstanceOf(SubmissionNotFoundException.class)
        .hasNoCause();
  }

  @Test
  void p4KeepsNullableAndZeroMeasurementsAndDoesNotWriteOnRepeatedReads() {
    UUID unknown = submission("ACCEPTED", null, null);
    UUID zero = submission("ACCEPTED", 0, 0);
    long before = jdbc.queryForObject("SELECT count(*) FROM judge.submissions", Long.class);
    assertThat(snapshots.getAccepted(unknown).runtimeMs()).isNull();
    assertThat(snapshots.getAccepted(unknown).memoryKb()).isNull();
    assertThat(snapshots.getAccepted(zero).runtimeMs()).isZero();
    assertThat(snapshots.getAccepted(zero).memoryKb()).isZero();
    assertThat(jdbc.queryForObject("SELECT count(*) FROM judge.submissions", Long.class))
        .isEqualTo(before);
  }

  @Test
  void missingSingletonAndInfrastructureFailuresDoNotBecomeFallbacksOrMissingSubmissions() {
    jdbc.update("DELETE FROM judge.generator_sandbox_settings WHERE id=1");
    assertThatThrownBy(generator::getGeneratorSandboxLimits)
        .isInstanceOf(JudgeReadUnavailableException.class)
        .hasNoCause();
    doThrow(new IllegalStateException("private SQL password"))
        .when(configRepository)
        .enabledLanguages();
    assertThatThrownBy(languages::getEnabledLanguages)
        .isInstanceOf(JudgeReadUnavailableException.class)
        .hasMessage("Judge read unavailable")
        .hasNoCause();
    doThrow(new IllegalStateException("private source SQL password"))
        .when(snapshotRepository)
        .find(any());
    assertThatThrownBy(() -> snapshots.getAccepted(UUID.randomUUID()))
        .isInstanceOf(JudgeReadUnavailableException.class)
        .hasMessage("Judge read unavailable")
        .hasNoCause();
  }
}
