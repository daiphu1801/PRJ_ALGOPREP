package com.algoprep.judge.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.judge.domain.exception.AdminConfigException;
import com.algoprep.judge.domain.model.AdminLanguageConfig;
import com.algoprep.judge.domain.model.AdminLanguageConfig.Defaults;
import com.algoprep.judge.domain.model.AdminLanguageConfig.Generator;
import com.algoprep.judge.domain.model.AdminLanguageConfig.Language;
import com.algoprep.judge.domain.model.AdminLanguageConfig.Sandbox;
import com.algoprep.judge.domain.model.JudgeAuditEntry;
import com.algoprep.judge.domain.ports.out.AdminLanguageConfigRepository;
import com.algoprep.judge.domain.ports.out.JudgeAuditPort;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class UpdateAdminLanguageConfigCommandHandlerTest {
  private final AdminLanguageConfigRepository repository =
      mock(AdminLanguageConfigRepository.class);
  private final JudgeAuditPort audit = mock(JudgeAuditPort.class);
  private final UpdateAdminLanguageConfigCommandHandler handler =
      new UpdateAdminLanguageConfigCommandHandler(repository, audit);
  private final UUID actor = UUID.randomUUID();
  private static final Instant NOW = Instant.parse("2026-11-05T00:00:00Z");

  private static AdminLanguageConfig config(
      String multiplier, int time, int memory, boolean enabled) {
    var rows =
        List.of("JAVA", "CPP", "PYTHON").stream()
            .map(
                code ->
                    new Language(
                        code,
                        "Compiler",
                        new BigDecimal(multiplier),
                        BigDecimal.ONE,
                        enabled,
                        0,
                        NOW))
            .toList();
    return new AdminLanguageConfig(
        rows,
        new Defaults(time, memory, 64, 10, NOW),
        new Sandbox(false, true, true, NOW),
        new Generator(30, 100, NOW));
  }

  private void rejects(AdminLanguageConfig value, String code) {
    assertThatThrownBy(() -> handler.update(actor, value))
        .isInstanceOfSatisfying(
            AdminConfigException.class, e -> assertThat(e.errorCode()).isEqualTo(code));
    verify(repository, never()).saveChanges(any(), any(), any());
    verify(audit, never()).record(any());
  }

  @Test
  void changedFieldsOnlyAreAuditedAndTheStoredResultIsReturned() {
    var before = config("1.00", 1000, 256, true);
    var after = config("2.00", 1000, 256, true);
    when(repository.lock()).thenReturn(before);
    when(repository.read()).thenReturn(after);
    assertThat(handler.update(actor, after)).isEqualTo(after);
    verify(repository).saveChanges(before, after, actor);
    var entry = ArgumentCaptor.forClass(JudgeAuditEntry.class);
    verify(audit).record(entry.capture());
    assertThat(entry.getValue().actorUserId()).isEqualTo(actor);
    assertThat(entry.getValue().actionType()).isEqualTo("LANGUAGE_CONFIG_CHANGED");
    assertThat(entry.getValue().beforeJson())
        .contains("languages.JAVA.timeLimitMultiplier")
        .doesNotContain("updatedAt", "compiler", "enabled", "pendingJobCount");
    assertThat(entry.getValue().afterJson()).contains(":2");
  }

  @Test
  void numericallyIdenticalMultipliersDoNotWriteOrAudit() {
    var before = config("1.00", 1000, 256, true);
    when(repository.lock()).thenReturn(before);
    assertThat(handler.update(actor, config("1", 1000, 256, true))).isEqualTo(before);
    verify(repository, never()).saveChanges(any(), any(), any());
    verify(audit, never()).record(any());
  }

  @Test
  void disabledLastLanguageAndEffectiveLimitsAreRejectedBeforeLocks() {
    rejects(config("1", 1000, 256, false), "JDG-203");
    rejects(config("0.10", 994, 256, true), "JDG-204");
    rejects(config("9.99", 4000, 256, true), "JDG-204");
    rejects(config("1", 1000, 63, true), "JDG-205");
    rejects(config("1", 1000, 1025, true), "JDG-205");
    verify(repository, never()).lock();
  }

  @Test
  void roundedEffectiveBoundariesAreAccepted() {
    for (var value : List.of(config("0.10", 995, 64, true), config("9.99", 3003, 1024, true))) {
      when(repository.lock()).thenReturn(value);
      assertThat(handler.update(actor, value)).isEqualTo(value);
    }
  }

  @Test
  void everyRowVersionMustMatchEvenForAnUnchangedRequest() {
    var before = config("1", 1000, 256, true);
    when(repository.lock()).thenReturn(before);
    var staleLanguages =
        before.languages().stream()
            .map(
                row ->
                    new Language(
                        row.language(),
                        "",
                        row.timeLimitMultiplier(),
                        row.memoryLimitMultiplier(),
                        row.enabled(),
                        0,
                        NOW.minusSeconds(1)))
            .toList();
    rejects(
        new AdminLanguageConfig(
            staleLanguages, before.gradingDefaults(), before.sandbox(), before.generatorSandbox()),
        "JDG-202");
    rejects(
        new AdminLanguageConfig(
            before.languages(),
            new Defaults(1000, 256, 64, 10, NOW.minusSeconds(1)),
            before.sandbox(),
            before.generatorSandbox()),
        "JDG-202");
    rejects(
        new AdminLanguageConfig(
            before.languages(),
            before.gradingDefaults(),
            new Sandbox(false, true, true, NOW.minusSeconds(1)),
            before.generatorSandbox()),
        "JDG-202");
    rejects(
        new AdminLanguageConfig(
            before.languages(),
            before.gradingDefaults(),
            before.sandbox(),
            new Generator(30, 100, NOW.minusSeconds(1))),
        "JDG-202");
  }

  @Test
  void auditFailureEscapesSoTheTransactionCannotCommit() {
    var before = config("1", 1000, 256, true);
    var after = config("2", 1000, 256, true);
    when(repository.lock()).thenReturn(before);
    org.mockito.Mockito.doThrow(new IllegalStateException("Audit unavailable"))
        .when(audit)
        .record(any());
    assertThatThrownBy(() -> handler.update(actor, after))
        .isInstanceOf(IllegalStateException.class);
    verify(repository, never()).read();
  }
}
