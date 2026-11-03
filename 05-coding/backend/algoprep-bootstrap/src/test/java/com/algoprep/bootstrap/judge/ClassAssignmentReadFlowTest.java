package com.algoprep.bootstrap.judge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.algoprep.judge.application.ports.out.ClassAssignmentReadPort;
import com.algoprep.problembank.application.ports.in.ListClassAssignmentsUseCase;
import com.algoprep.problembank.domain.model.ClassAssignmentSnapshot;
import com.algoprep.problembank.domain.ports.out.ClassAssignmentReadRepository;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

/**
 * Real F2 assignment history -> bootstrap -> F4; caller authorization remains outside this read.
 */
@SpringBootTest(
    properties =
        "spring.autoconfigure.exclude=org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration")
@ActiveProfiles("test")
@Testcontainers
class ClassAssignmentReadFlowTest {
  private static final Instant ASSIGNED = Instant.parse("2026-10-01T00:00:00.123456Z");
  private static final Instant REMOVED = Instant.parse("2026-10-03T00:00:00.654321Z");
  private static final Instant REASSIGNED = Instant.parse("2026-10-04T00:00:00.123456Z");

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
  @Autowired ClassAssignmentReadPort reader;
  @Autowired ListClassAssignmentsUseCase inbound;
  @Autowired ObjectProvider<JdbcClient> jdbcProvider;
  @TempDir Path temporary;
  private UUID classId;
  private UUID otherClass;
  private UUID actor;
  private UUID published;
  private UUID withdrawn;
  private UUID deleted;

  @BeforeEach
  void fixture() {
    classId = UUID.randomUUID();
    otherClass = UUID.randomUUID();
    actor = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO identity.users(id,email,display_name,role_id,status)"
            + " SELECT ?,?,'Assignment actor',id,'ACTIVE' FROM identity.roles WHERE code='INSTRUCTOR'",
        actor,
        actor + "@example.test");
    published = problem(true, false);
    withdrawn = problem(false, false);
    deleted = problem(false, true);
    assignment(classId, published, ASSIGNED, REMOVED);
    assignment(classId, published, REASSIGNED, null);
    assignment(classId, withdrawn, ASSIGNED, null);
    assignment(classId, deleted, ASSIGNED, REMOVED);
    assignment(otherClass, published, ASSIGNED, null);
  }

  private UUID problem(boolean active, boolean softDeleted) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO problem.problems(id,code,title,author_id,updated_by,level_id,"
            + "time_limit_ms,memory_limit_mb,max_output_size_kb,status,deleted)"
            + " VALUES (?,?,'Đề bài lịch sử',?,?,'20000000-0000-4000-8000-000000000001',"
            + "1000,256,64,?,?)",
        id,
        id.toString(),
        actor,
        actor,
        active ? "PUBLISHED" : "UNPUBLISHED",
        softDeleted);
    return id;
  }

  private void assignment(UUID classroom, UUID problem, Instant start, Instant end) {
    jdbc.update(
        "INSERT INTO problem.class_assignments"
            + "(id,problem_id,class_id,assigned_by,assigned_at,removed_at) VALUES (?,?,?,?,?,?)",
        UUID.randomUUID(),
        problem,
        classroom,
        actor,
        Timestamp.from(start),
        end == null ? null : Timestamp.from(end));
  }

  @Test
  void returnsAllIntervalsForExactlyTheRequestedClasses() {
    var result = reader.listAssignments(List.of(classId, classId, UUID.randomUUID()));
    assertThat(result).hasSize(4).allSatisfy(row -> assertThat(row.classId()).isEqualTo(classId));
    var repeated = result.stream().filter(row -> row.problemId().equals(published)).toList();
    assertThat(repeated).hasSize(2);
    assertThat(repeated.getFirst().assignedAt()).isEqualTo(ASSIGNED);
    assertThat(repeated.getFirst().removedAt()).isEqualTo(REMOVED);
    assertThat(repeated.getLast().assignedAt()).isEqualTo(REASSIGNED);
    assertThat(repeated.getLast().removedAt()).isNull();
    assertThat(reader.listAssignments(List.of(classId, otherClass))).hasSize(5);
  }

  @Test
  void withdrawnAndSoftDeletedProblemsRemainReadableWithoutTheirContents() {
    assertThat(reader.listAssignments(List.of(classId)))
        .extracting(row -> row.problemId())
        .contains(published, withdrawn, deleted);
    assertThat(reader.listAssignments(List.of(classId)))
        .allSatisfy(row -> assertThat(row.problemTitle()).isEqualTo("Đề bài lịch sử"));
  }

  @Test
  void titleChangesAreReadLiveWithoutCachingAssignmentMetadata() {
    reader.listAssignments(List.of(classId));
    jdbc.update("UPDATE problem.problems SET title='Tên mới' WHERE id=?", published);
    assertThat(
            reader.listAssignments(List.of(classId)).stream()
                .filter(row -> row.problemId().equals(published)))
        .allSatisfy(row -> assertThat(row.problemTitle()).isEqualTo("Tên mới"));
  }

  @Test
  void unknownAndEmptyClassBatchesHaveNoAssignments() {
    assertThat(reader.listAssignments(List.of(UUID.randomUUID()))).isEmpty();
    assertThat(reader.listAssignments(List.of())).isEmpty();
    assertThat(inbound.listAssignments(List.of())).isEmpty();
  }

  @Test
  void readsDoNotModifyAssignmentsProblemsOrOutbox() {
    String assignmentsBefore = assignmentRows();
    Long eventsBefore =
        jdbc.queryForObject("SELECT count(*) FROM problem.outbox_events", Long.class);
    String problemsBefore =
        jdbc.queryForObject(
            "SELECT json_agg(p ORDER BY id)::text FROM problem.problems p", String.class);
    reader.listAssignments(List.of(classId));
    assertThat(assignmentRows()).isEqualTo(assignmentsBefore);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM problem.outbox_events", Long.class))
        .isEqualTo(eventsBefore);
    assertThat(
            jdbc.queryForObject(
                "SELECT json_agg(p ORDER BY id)::text FROM problem.problems p", String.class))
        .isEqualTo(problemsBefore);
  }

  private String assignmentRows() {
    return jdbc.queryForObject(
        "SELECT json_agg(a ORDER BY id)::text FROM problem.class_assignments a", String.class);
  }

  @Test
  void repeatedIntervalsWithIdenticalTimestampsAreNotDeduplicated() {
    assignment(classId, published, ASSIGNED, REMOVED);
    assertThat(
            reader.listAssignments(List.of(classId)).stream()
                .filter(
                    row -> row.problemId().equals(published) && row.assignedAt().equals(ASSIGNED)))
        .hasSize(2);
  }

  @Test
  void missingDatabaseTableFailsClosedAndRecoversAfterRepair() {
    jdbc.execute("ALTER TABLE problem.class_assignments RENAME TO unavailable_assignments");
    try {
      assertThatThrownBy(() -> reader.listAssignments(List.of(classId)))
          .isInstanceOf(IllegalStateException.class)
          .hasMessage("Class assignments are unavailable")
          .hasNoCause();
    } finally {
      jdbc.execute("ALTER TABLE problem.unavailable_assignments RENAME TO class_assignments");
    }
    assertThat(reader.listAssignments(List.of(classId))).hasSize(4);
  }

  @ParameterizedTest
  @ValueSource(strings = {"CLASS_SCOPE", "REMOVED_HISTORY", "WITHDRAWN_HISTORY"})
  void copiedSqlMutationsCannotBreakScopeOrDropHistory(String rule) throws Exception {
    Path file =
        Path.of(
            "../algoprep-problem-bank/src/main/java/com/algoprep/problembank/"
                + "infrastructure/persistence/JdbcClassAssignmentReadRepository.java");
    String source = Files.readString(file);
    String predicate = "WHERE a.class_id IN (:classIds)";
    String replacement =
        switch (rule) {
          case "CLASS_SCOPE" -> "WHERE (a.class_id IN (:classIds) OR true)";
          case "REMOVED_HISTORY" -> predicate + " AND a.removed_at IS NULL";
          default -> predicate + " AND p.status='PUBLISHED' AND NOT p.deleted";
        };
    String mutant = source.replace(predicate, replacement);
    assertThat(mutant).isNotEqualTo(source);
    sqlCandidate(source, false);
    sqlCandidate(mutant, true);
    assertThat(Files.readString(file)).isEqualTo(source);
    System.out.println("MUTATION CAUGHT ASSIGNMENT_" + rule + "; original GREEN; mutant RED");
  }

  private void sqlCandidate(String source, boolean mutant) throws Exception {
    String prefix = "com.algoprep.problembank.infrastructure.persistence";
    Path folder = Files.createDirectories(temporary.resolve(mutant ? "mutant" : "original"));
    Path file = folder.resolve("JdbcClassAssignmentReadRepository.java");
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
      var type = loader.loadClass(prefix + ".mutationcopy.JdbcClassAssignmentReadRepository");
      var constructor = type.getDeclaredConstructor(ObjectProvider.class);
      constructor.setAccessible(true);
      var repository = (ClassAssignmentReadRepository) constructor.newInstance(jdbcProvider);
      Runnable oracle =
          () ->
              assertThat(repository.findAssignments(Set.of(classId)))
                  .containsExactlyInAnyOrderElementsOf(expectedAssignments());
      if (mutant) {
        assertThatThrownBy(oracle::run).isInstanceOf(AssertionError.class);
      } else {
        oracle.run();
      }
    }
  }

  private List<ClassAssignmentSnapshot> expectedAssignments() {
    return List.of(
        new ClassAssignmentSnapshot(classId, published, "Đề bài lịch sử", ASSIGNED, REMOVED),
        new ClassAssignmentSnapshot(classId, published, "Đề bài lịch sử", REASSIGNED, null),
        new ClassAssignmentSnapshot(classId, withdrawn, "Đề bài lịch sử", ASSIGNED, null),
        new ClassAssignmentSnapshot(classId, deleted, "Đề bài lịch sử", ASSIGNED, REMOVED));
  }
}
