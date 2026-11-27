package com.algoprep.judge.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.judge.domain.exception.WorkerPersistenceException;
import com.algoprep.judge.domain.model.language.AdminLanguageConfig;
import com.algoprep.judge.domain.model.language.AdminLanguageConfig.Defaults;
import com.algoprep.judge.domain.model.language.AdminLanguageConfig.Generator;
import com.algoprep.judge.domain.model.language.AdminLanguageConfig.Language;
import com.algoprep.judge.domain.model.language.AdminLanguageConfig.Sandbox;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

/** Admin language configuration persistence (API #21) and worker advisory-lock leases. */
class JdbcAdminLanguageAndLeaseTest {
  private final FakeJdbc db = new FakeJdbc();
  private final Instant at = Instant.parse("2026-11-01T10:00:00Z");
  private final OffsetDateTime ts = OffsetDateTime.ofInstant(at, ZoneOffset.UTC);
  private final UUID actor = UUID.randomUUID();
  private final JdbcAdminLanguageConfigRepository repo =
      new JdbcAdminLanguageConfigRepository(db.provider(), "javac", "g++", "py3");

  private Map<String, Object> lang(String code) {
    return FakeJdbc.row0(
        "language",
        code,
        "time_limit_multiplier",
        new BigDecimal("1.0"),
        "memory_limit_multiplier",
        new BigDecimal("1.0"),
        "enabled",
        true,
        "updated_at",
        ts);
  }

  private void queueFullConfig(List<Map<String, Object>> languages) {
    db.rows(languages)
        .row(
            "time_limit_ms",
            1000,
            "memory_limit_mb",
            256,
            "output_limit_kb",
            64,
            "compile_timeout_sec",
            10,
            "updated_at",
            ts)
        .row(
            "network_access_enabled",
            false,
            "child_process_limit_enabled",
            true,
            "return_stderr_to_student",
            false,
            "updated_at",
            ts)
        .row("max_runtime_seconds", 10, "max_output_mb", 5, "updated_at", ts)
        .rows(List.of(FakeJdbc.row0("language", "CPP", "jobs", 4L)));
  }

  private AdminLanguageConfig config() {
    return new AdminLanguageConfig(
        List.of(
            new Language("CPP", "g++", BigDecimal.ONE, BigDecimal.ONE, true, 0, at),
            new Language("JAVA", "javac", BigDecimal.ONE, BigDecimal.ONE, true, 0, at),
            new Language("PYTHON", "py3", BigDecimal.ONE, BigDecimal.ONE, true, 0, at)),
        new Defaults(1000, 256, 64, 10, at),
        new Sandbox(false, true, false, at),
        new Generator(10, 5, at));
  }

  @Test
  void readMapsAllFourGroupsWithPendingCountsAndDisplayNames() {
    queueFullConfig(List.of(lang("CPP"), lang("JAVA"), lang("PYTHON")));
    var result = repo.read();
    assertThat(result.languages()).hasSize(3);
    assertThat(result.languages().get(0).compilerDisplayName()).isEqualTo("g++");
    assertThat(result.languages().get(0).pendingJobCount()).isEqualTo(4L);
    assertThat(result.languages().get(1).pendingJobCount()).isZero();
    assertThat(result.gradingDefaults().maxOutputSizeKb()).isEqualTo(64);
    assertThat(result.sandbox().childProcessLimitEnabled()).isTrue();
    assertThat(result.generatorSandbox().maxRuntimeSeconds()).isEqualTo(10);
    assertThat(db.calls.get(0).sql()).doesNotContain("FOR UPDATE");
  }

  @Test
  void lockAppendsForUpdateToEveryConfigQuery() {
    queueFullConfig(List.of(lang("CPP"), lang("JAVA"), lang("PYTHON")));
    repo.lock();
    assertThat(db.calls.subList(0, 4)).allMatch(c -> c.sql().endsWith("FOR UPDATE"));
  }

  @Test
  void incompleteLanguageSetIsRejected() {
    db.rows(List.of(lang("CPP"), lang("JAVA")));
    assertThatThrownBy(repo::read)
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Judge configuration unavailable");
  }

  @Test
  void unknownLanguageWithoutDisplayNameIsRejected() {
    db.rows(List.of(lang("CPP"), lang("JAVA"), lang("RUBY")));
    assertThatThrownBy(repo::read).isInstanceOf(RuntimeException.class);
  }

  @Test
  void saveChangesWritesOnlyChangedGroups() {
    var before = config();
    repo.saveChanges(before, before, actor);
    assertThat(db.calls).isEmpty();
    var after =
        new AdminLanguageConfig(
            List.of(
                before.languages().get(0),
                new Language("JAVA", "javac", new BigDecimal("2.5"), BigDecimal.ONE, false, 0, at),
                before.languages().get(2)),
            new Defaults(2000, 256, 64, 10, at),
            new Sandbox(true, true, false, at),
            new Generator(10, 9, at));
    repo.saveChanges(before, after, actor);
    assertThat(db.calls).hasSize(4);
    assertThat(db.calls.get(0).sql()).contains("judge.language_configs");
    assertThat(db.calls.get(0).params().values())
        .contains(new BigDecimal("2.5"), false, actor, "JAVA");
    assertThat(db.calls.get(1).sql()).contains("judge.grading_defaults");
    assertThat(db.calls.get(1).params().values()).contains(2000);
    assertThat(db.calls.get(2).sql()).contains("judge.sandbox_settings");
    assertThat(db.calls.get(2).params().values()).contains(true);
    assertThat(db.calls.get(3).sql()).contains("judge.generator_sandbox_settings");
    assertThat(db.calls.get(3).params().values()).contains(9);
  }

  // ---- lease adapter ----

  private record Lease(
      JdbcWorkerLeaseAdapter adapter,
      Connection connection,
      PreparedStatement statement,
      ResultSet rows) {}

  private Lease leaseFixture(boolean acquired) throws SQLException {
    var rs = mock(ResultSet.class);
    when(rs.next()).thenReturn(true);
    when(rs.getBoolean(1)).thenReturn(acquired);
    var ps = mock(PreparedStatement.class);
    when(ps.executeQuery()).thenReturn(rs);
    var connection = mock(Connection.class);
    when(connection.getAutoCommit()).thenReturn(true);
    when(connection.prepareStatement(anyString())).thenReturn(ps);
    var ds = mock(DataSource.class);
    when(ds.getConnection()).thenReturn(connection);
    ObjectProvider<DataSource> provider =
        new ObjectProvider<>() {
          @Override
          public DataSource getObject() {
            return ds;
          }
        };
    return new Lease(new JdbcWorkerLeaseAdapter(provider), connection, ps, rs);
  }

  @Test
  void acquireReturnsLeaseAndCloseUnlocksThenReleasesConnection() throws SQLException {
    var f = leaseFixture(true);
    var lease = f.adapter().acquire("F4_GRADING_WORKER_LEADER").orElseThrow();
    verify(f.connection()).prepareStatement("SELECT pg_try_advisory_lock(hashtext(?))");
    lease.close();
    verify(f.connection()).prepareStatement("SELECT pg_advisory_unlock(hashtext(?))");
    verify(f.connection()).close();
    lease.close();
    assertThatThrownBy(lease::check).isInstanceOf(WorkerPersistenceException.class);
  }

  @Test
  void otherKeysUseExtendedHash() throws SQLException {
    var f = leaseFixture(true);
    f.adapter().acquire("OTHER_KEY").orElseThrow().close();
    verify(f.connection()).prepareStatement("SELECT pg_try_advisory_lock(hashtextextended(?,0))");
    verify(f.connection()).prepareStatement("SELECT pg_advisory_unlock(hashtextextended(?,0))");
  }

  @Test
  void acquireIsEmptyWhenLockHeldElsewhere() throws SQLException {
    var f = leaseFixture(false);
    assertThat(f.adapter().acquire("F4_GRADING_WORKER_LEADER")).isEmpty();
    verify(f.connection()).close();
  }

  @Test
  void acquireDiscardsConnectionOnFailureOrNonAutoCommit() throws SQLException {
    var f = leaseFixture(true);
    when(f.connection().getAutoCommit()).thenReturn(false);
    assertThatThrownBy(() -> f.adapter().acquire("k"))
        .isInstanceOf(WorkerPersistenceException.class);
    verify(f.connection()).close();
    var g = leaseFixture(true);
    when(g.statement().executeQuery()).thenThrow(new SQLException("down"));
    org.mockito.Mockito.doThrow(new SQLException("gone"))
        .when(g.connection())
        .abort(org.mockito.ArgumentMatchers.any());
    org.mockito.Mockito.doThrow(new SQLException("gone")).when(g.connection()).close();
    assertThatThrownBy(() -> g.adapter().acquire("k"))
        .isInstanceOf(WorkerPersistenceException.class);
  }

  @Test
  void checkRunsHealthQueryAndFailsWhenConnectionBroken() throws SQLException {
    var f = leaseFixture(true);
    var statement = mock(Statement.class);
    when(f.connection().createStatement()).thenReturn(statement);
    var lease = f.adapter().acquire("k").orElseThrow();
    lease.check();
    verify(statement).execute("SELECT 1");
    when(statement.execute(anyString())).thenThrow(new SQLException("dead"));
    assertThatThrownBy(lease::check).isInstanceOf(WorkerPersistenceException.class);
  }

  @Test
  void closeDiscardsConnectionWhenLeaseWasLost() throws SQLException {
    var f = leaseFixture(true);
    var lease = f.adapter().acquire("k").orElseThrow();
    when(f.rows().getBoolean(1)).thenReturn(false);
    assertThatThrownBy(lease::close).isInstanceOf(WorkerPersistenceException.class);
    verify(f.connection()).abort(org.mockito.ArgumentMatchers.any());
  }
}
