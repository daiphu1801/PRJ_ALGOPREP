package com.algoprep.bootstrap.problembank;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.algoprep.identity.application.ports.in.ReadUserDisplayNamesUseCase;
import com.algoprep.identity.domain.ports.out.UserDisplayNamesRepository;
import com.algoprep.problembank.application.ports.out.UserDirectoryPort;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

/** Actual F1 SQL -> inbound -> bootstrap -> F2 port, without changing any existing F1 code. */
@SpringBootTest(
    properties =
        "spring.autoconfigure.exclude=org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration")
@ActiveProfiles("test")
@Testcontainers
class UserDirectoryFlowTest {
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

  @Autowired JdbcTemplate jdbc;
  @Autowired UserDirectoryPort directory;
  @Autowired ReadUserDisplayNamesUseCase identity;
  @Autowired ObjectProvider<JdbcClient> jdbcProvider;
  @Autowired TransactionTemplate transaction;
  @TempDir Path temporary;
  private UUID id;

  @BeforeEach
  void fixture() {
    id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO identity.users(id,email,display_name,role_id,status)"
            + " SELECT ?,?,'Nguyễn Thị Ánh',id,'ACTIVE' FROM identity.roles WHERE code='INSTRUCTOR'",
        id,
        id + "@example.test");
  }

  @Test
  void batchesKnownAndMissingUsersAndObservesCommittedNameChanges() {
    UUID absent = UUID.randomUUID();
    assertThat(directory.getDisplayNames(List.of(id, id, absent)))
        .isEqualTo(Map.of(id, "Nguyễn Thị Ánh"));
    jdbc.update("UPDATE identity.users SET display_name='Tên mới' WHERE id=?", id);
    assertThat(directory.getDisplayNames(List.of(id))).containsEntry(id, "Tên mới");
  }

  @Test
  void deactivatedAccountsRetainAttributionUntilAnonymized() {
    jdbc.update(
        "UPDATE identity.users SET status='DEACTIVATED',deactivation_source='ADMIN',"
            + "deactivated_at=clock_timestamp() WHERE id=?",
        id);
    assertThat(directory.getDisplayNames(List.of(id))).containsEntry(id, "Nguyễn Thị Ánh");
  }

  private void anonymizeWithStaleName() {
    jdbc.update(
        "UPDATE identity.users SET status='DEACTIVATED',deactivation_source='SELF',"
            + "deactivated_at=clock_timestamp()-interval '31 days',"
            + "anonymized_at=clock_timestamp() WHERE id=?",
        id);
  }

  @Test
  void anonymizedRowsNeverRevealEvenAnAccidentallyRetainedOldName() {
    anonymizeWithStaleName();
    assertThat(directory.getDisplayNames(List.of(id))).containsEntry(id, "Người dùng đã xoá");
    assertThat(identity.getDisplayNames(List.of(id))).containsEntry(id, "Người dùng đã xoá");
  }

  @Test
  void lookupDoesNotWriteUserData() {
    String before =
        jdbc.queryForObject(
            "SELECT row_to_json(u)::text FROM identity.users u WHERE id=?", String.class, id);
    directory.getDisplayNames(List.of(id));
    assertThat(
            jdbc.queryForObject(
                "SELECT row_to_json(u)::text FROM identity.users u WHERE id=?", String.class, id))
        .isEqualTo(before);
  }

  @Test
  void sqlFailureDoesNotPoisonTheCallersTransaction() {
    jdbc.execute("ALTER TABLE identity.users RENAME TO directory_unavailable_users");
    try {
      transaction.executeWithoutResult(
          status -> {
            assertThat(directory.getDisplayNames(List.of(id)).get(id)).isNull();
            assertThat(jdbc.queryForObject("SELECT 42", Integer.class)).isEqualTo(42);
            assertThat(status.isRollbackOnly()).isFalse();
          });
    } finally {
      jdbc.execute("ALTER TABLE identity.directory_unavailable_users RENAME TO users");
    }
    assertThat(directory.getDisplayNames(List.of(id))).containsEntry(id, "Nguyễn Thị Ánh");
  }

  @Test
  void realDatabaseLockCannotHoldTheOptionalReadBeyondTwoSeconds() {
    transaction.executeWithoutResult(
        status -> {
          jdbc.execute("LOCK TABLE identity.users IN ACCESS EXCLUSIVE MODE");
          long start = System.nanoTime();
          assertThat(directory.getDisplayNames(List.of(id))).isEmpty();
          Duration elapsed = Duration.ofNanos(System.nanoTime() - start);
          assertThat(elapsed).isBetween(Duration.ofMillis(1500), Duration.ofSeconds(4));
        });
    assertThat(directory.getDisplayNames(List.of(id))).containsEntry(id, "Nguyễn Thị Ánh");
  }

  @Test
  void sqlMutationCannotBypassAnonymization() throws Exception {
    anonymizeWithStaleName();
    Path sourceFile =
        Path.of(
            "../algoprep-identity/src/main/java/com/algoprep/identity/"
                + "infrastructure/persistence/JdbcUserDisplayNamesRepository.java");
    String source = Files.readString(sourceFile);
    String mutant = source.replace("anonymized_at IS NOT NULL", "false");
    assertThat(mutant).isNotEqualTo(source);
    sqlCandidate(source, false);
    sqlCandidate(mutant, true);
    assertThat(Files.readString(sourceFile)).isEqualTo(source);
    System.out.println("MUTATION CAUGHT USER_DIRECTORY_ANONYMIZATION; original GREEN; mutant RED");
  }

  private void sqlCandidate(String source, boolean mutant) throws Exception {
    String prefix = "com.algoprep.identity.infrastructure.persistence";
    Path folder = Files.createDirectories(temporary.resolve(mutant ? "mutant" : "original"));
    Path file = folder.resolve("JdbcUserDisplayNamesRepository.java");
    Files.writeString(
        file, source.replace("package " + prefix + ";", "package " + prefix + ".mutationcopy;"));
    var errors = new ByteArrayOutputStream();
    int code =
        ToolProvider.getSystemJavaCompiler()
            .run(
                null,
                errors,
                errors,
                "-proc:none",
                "-encoding",
                "UTF-8",
                "-classpath",
                System.getProperty("java.class.path"),
                "-d",
                folder.toString(),
                file.toString());
    assertThat(code).withFailMessage(errors.toString(StandardCharsets.UTF_8)).isZero();
    try (var loader =
        new URLClassLoader(new URL[] {folder.toUri().toURL()}, getClass().getClassLoader())) {
      var type = loader.loadClass(prefix + ".mutationcopy.JdbcUserDisplayNamesRepository");
      var constructor = type.getDeclaredConstructor(ObjectProvider.class);
      constructor.setAccessible(true);
      var repository = (UserDisplayNamesRepository) constructor.newInstance(jdbcProvider);
      Runnable oracle =
          () ->
              assertThat(repository.findDisplayNames(Set.of(id)))
                  .containsEntry(id, "Người dùng đã xoá");
      if (mutant) {
        assertThatThrownBy(oracle::run).isInstanceOf(AssertionError.class);
      } else {
        oracle.run();
      }
    }
  }
}
