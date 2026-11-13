package com.algoprep.problembank.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.algoprep.common.exception.AlgoPrepException;
import com.algoprep.common.exception.ServiceUnavailableException;
import com.algoprep.problembank.application.dto.CatalogCommand;
import com.algoprep.problembank.application.ports.out.ProblemListVersionPort;
import com.algoprep.problembank.domain.exception.CatalogInUseException;
import com.algoprep.problembank.domain.exception.LastProblemLevelException;
import com.algoprep.problembank.domain.model.CatalogItem;
import com.algoprep.problembank.domain.model.CatalogKind;
import com.algoprep.problembank.domain.ports.out.CatalogRepository;
import com.algoprep.problembank.domain.ports.out.ProblemAuditPort;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class CatalogMutationServiceTest {
  private final CatalogRepository repository = mock(CatalogRepository.class);
  private final ProblemAuditPort audit = mock(ProblemAuditPort.class);
  private final ProblemListVersionPort cache = mock(ProblemListVersionPort.class);
  private final CatalogMutationService service =
      new CatalogMutationService(repository, audit, cache);
  private final UUID actor = UUID.randomUUID();
  private final CatalogItem one = new CatalogItem(UUID.randomUUID(), "ONE", "One", 1, null);
  private final CatalogItem two = new CatalogItem(UUID.randomUUID(), "TWO", "Two", 2, null);

  private CatalogCommand command(String name) {
    return new CatalogCommand(actor, true, one.id(), name, List.of(two.id(), one.id()));
  }

  private void stored(CatalogKind kind) {
    when(repository.list(kind, false)).thenReturn(List.of(one, two));
  }

  private void code(Runnable call, String code) {
    assertThatThrownBy(call::run)
        .isInstanceOfSatisfying(
            AlgoPrepException.class, error -> assertThat(error.errorCode()).isEqualTo(code));
  }

  @ParameterizedTest
  @EnumSource(CatalogKind.class)
  void createTrimsRecordsAndSchedulesInvalidation(CatalogKind kind) {
    stored(kind);
    when(repository.create(kind, "Fresh"))
        .thenReturn(new CatalogItem(UUID.randomUUID(), "FRESH", "Fresh", 3, null));
    assertThat(service.create(kind, command(" Fresh ")).getFirst().name()).isEqualTo("Fresh");
    var order = inOrder(repository, audit, cache);
    order.verify(repository).lock(kind);
    order.verify(repository).list(kind, false);
    order.verify(repository).create(kind, "Fresh");
    order
        .verify(audit)
        .record(
            argThat(
                entry ->
                    entry.actionType().equals("PROBLEM_" + kind + "_CREATED")
                        && entry.actorUserId().equals(actor)
                        && entry.beforeJson() == null
                        && entry.afterJson().contains("Fresh")));
    order.verify(cache).incrementAfterCommit();
  }

  @ParameterizedTest
  @EnumSource(CatalogKind.class)
  void everyMutationRejectsNonAdminBeforeAnyPort(CatalogKind kind) {
    var denied = new CatalogCommand(actor, false, one.id(), "New", List.of());
    code(() -> service.create(kind, denied), "PRB-004");
    code(() -> service.update(kind, denied), "PRB-004");
    code(() -> service.reorder(kind, denied), "PRB-004");
    code(() -> service.delete(kind, denied), "PRB-004");
    verifyNoInteractions(repository, audit, cache);
  }

  @ParameterizedTest
  @EnumSource(CatalogKind.class)
  void invalidAndDuplicateNamesDoNotAudit(CatalogKind kind) {
    stored(kind);
    for (String name : Arrays.asList(null, "", "  ", "x".repeat(61))) {
      code(() -> service.create(kind, command(name)), "PRB-001");
    }
    code(
        () -> service.create(kind, command("one")),
        kind == CatalogKind.LEVEL ? "PRB-022" : "PRB-021");
    code(
        () -> service.update(kind, command("two")),
        kind == CatalogKind.LEVEL ? "PRB-022" : "PRB-021");
    code(() -> service.update(kind, command(" ")), "PRB-001");
    verifyNoInteractions(audit, cache);
  }

  @ParameterizedTest
  @EnumSource(CatalogKind.class)
  void renameKeepsCodeAndAnUnchangedNameDoesNotAudit(CatalogKind kind) {
    stored(kind);
    assertThat(service.update(kind, command("One"))).containsExactly(one);
    verifyNoInteractions(audit, cache);
    var changed = service.update(kind, command("Renamed")).getFirst();
    assertThat(changed.code()).isEqualTo("ONE");
    verify(audit)
        .record(
            argThat(
                entry ->
                    entry.beforeJson().contains("One") && entry.afterJson().contains("Renamed")));
  }

  @ParameterizedTest
  @EnumSource(CatalogKind.class)
  void absentIdsAndReferencesHaveExactCodesAndMetadata(CatalogKind kind) {
    stored(kind);
    var missing = new CatalogCommand(actor, true, UUID.randomUUID(), "New", null);
    code(() -> service.update(kind, missing), kind == CatalogKind.LEVEL ? "PRB-013" : "PRB-012");
    code(() -> service.delete(kind, missing), kind == CatalogKind.LEVEL ? "PRB-013" : "PRB-012");
    when(repository.references(kind, one.id())).thenReturn(7L);
    assertThatThrownBy(() -> service.delete(kind, command(null)))
        .isInstanceOfSatisfying(
            CatalogInUseException.class,
            error -> {
              assertThat(error.clientErrorDetails()).containsEntry("referencingProblemCount", 7L);
              assertThat(error.errorCode())
                  .isEqualTo(kind == CatalogKind.LEVEL ? "PRB-032" : "PRB-031");
            });
    verifyNoInteractions(audit, cache);
  }

  @ParameterizedTest
  @EnumSource(CatalogKind.class)
  void validReorderAndDeleteAuditOnce(CatalogKind kind) {
    stored(kind);
    service.reorder(kind, command(null));
    verify(repository).reorder(kind, List.of(two.id(), one.id()));
    service.delete(kind, command(null));
    verify(repository).delete(kind, one.id());
    verify(audit, times(2)).record(any());
    verify(cache, times(2)).incrementAfterCommit();
  }

  @ParameterizedTest
  @EnumSource(CatalogKind.class)
  void orderRejectsMissingExtraRepeatedAndNullIds(CatalogKind kind) {
    stored(kind);
    for (List<UUID> order :
        Arrays.asList(
            null,
            List.<UUID>of(),
            List.of(one.id()),
            List.of(one.id(), one.id()),
            List.of(one.id(), UUID.randomUUID()),
            Arrays.asList(one.id(), null))) {
      code(
          () -> service.reorder(kind, new CatalogCommand(actor, true, null, null, order)),
          "PRB-049");
    }
    verifyNoInteractions(audit, cache);
  }

  @Test
  void lastLevelCannotBeDeletedButLastTopicCan() {
    when(repository.list(CatalogKind.LEVEL, false)).thenReturn(List.of(one));
    assertThatThrownBy(() -> service.delete(CatalogKind.LEVEL, command(null)))
        .isInstanceOfSatisfying(
            LastProblemLevelException.class,
            error -> assertThat(error.clientErrorDetails()).containsEntry("reason", "LAST_LEVEL"));
    when(repository.list(CatalogKind.TOPIC, false)).thenReturn(List.of(one));
    service.delete(CatalogKind.TOPIC, command(null));
    verify(repository).delete(CatalogKind.TOPIC, one.id());
  }

  @Test
  void emptyTopicOrderIsValidAndAuditFailurePreventsInvalidation() {
    when(repository.list(CatalogKind.TOPIC, false)).thenReturn(List.of());
    service.reorder(CatalogKind.TOPIC, new CatalogCommand(actor, true, null, null, List.of()));
    clearInvocations(audit, cache);
    when(repository.create(CatalogKind.TOPIC, "Fresh")).thenReturn(one);
    doThrow(new IllegalStateException("database unavailable")).when(audit).record(any());
    assertThatThrownBy(() -> service.create(CatalogKind.TOPIC, command("Fresh")))
        .isInstanceOf(ServiceUnavailableException.class)
        .hasMessage("Audit write unavailable; change rolled back");
    verifyNoInteractions(cache);
  }

  @Test
  void nameBoundaryAcceptsSixtyUnicodeCharacters() {
    when(repository.list(CatalogKind.TOPIC, false)).thenReturn(List.of());
    when(repository.create(CatalogKind.TOPIC, "😀".repeat(60))).thenReturn(one);
    service.create(CatalogKind.TOPIC, command("😀".repeat(60)));
    code(() -> service.create(CatalogKind.TOPIC, command("😀".repeat(61))), "PRB-001");
  }
}
