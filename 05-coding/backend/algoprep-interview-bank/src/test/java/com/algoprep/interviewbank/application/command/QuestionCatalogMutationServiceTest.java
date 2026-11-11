package com.algoprep.interviewbank.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.algoprep.common.exception.AlgoPrepException;
import com.algoprep.common.exception.ClientErrorDetails;
import com.algoprep.interviewbank.application.dto.CatalogCommand;
import com.algoprep.interviewbank.domain.model.CatalogItem;
import com.algoprep.interviewbank.domain.model.CatalogKind;
import com.algoprep.interviewbank.domain.model.InterviewAuditEntry;
import com.algoprep.interviewbank.domain.ports.out.CatalogRepository;
import com.algoprep.interviewbank.domain.ports.out.InterviewBankAuditPort;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;

class QuestionCatalogMutationServiceTest {
  private final CatalogRepository repository = mock(CatalogRepository.class);
  private final InterviewBankAuditPort audit = mock(InterviewBankAuditPort.class);
  private final QuestionCatalogMutationService service =
      new QuestionCatalogMutationService(repository, audit);
  private final UUID actor = UUID.randomUUID();
  private final CatalogItem one = new CatalogItem(UUID.randomUUID(), "ONE", "One", 1, false, null);
  private final CatalogItem two = new CatalogItem(UUID.randomUUID(), "TWO", "Two", 2, true, null);

  private CatalogCommand command(boolean admin, UUID id, String name, Boolean star) {
    return new CatalogCommand(actor, admin, id, name, star, List.of(two.id(), one.id()));
  }

  private static String codeOf(Runnable call) {
    var captured = new AlgoPrepException[1];
    assertThatThrownBy(call::run)
        .isInstanceOfSatisfying(AlgoPrepException.class, e -> captured[0] = e);
    return captured[0].errorCode();
  }

  private InterviewAuditEntry audited() {
    var captor = ArgumentCaptor.forClass(InterviewAuditEntry.class);
    verify(audit).record(captor.capture());
    return captor.getValue();
  }

  private void stored(CatalogKind kind) {
    when(repository.list(kind, false)).thenReturn(List.of(one, two));
    when(repository.find(kind, one.id())).thenReturn(Optional.of(one));
  }

  @ParameterizedTest
  @EnumSource(CatalogKind.class)
  void nonAdminIsRefusedBeforeAnyReadOrWrite(CatalogKind kind) {
    var other = command(false, one.id(), "Name", true);
    assertThat(codeOf(() -> service.create(kind, other))).isEqualTo("INB-302");
    assertThat(codeOf(() -> service.update(kind, other))).isEqualTo("INB-302");
    assertThat(codeOf(() -> service.reorder(kind, other))).isEqualTo("INB-302");
    assertThat(codeOf(() -> service.delete(kind, other))).isEqualTo("INB-302");
    verifyNoInteractions(repository, audit);
  }

  @ParameterizedTest
  @EnumSource(CatalogKind.class)
  void createTrimsLocksChecksNameThenAuditsTheNewRow(CatalogKind kind) {
    var created = new CatalogItem(UUID.randomUUID(), "NEW", "New", 3, false, 0L);
    when(repository.create(kind, "New")).thenReturn(created);
    assertThat(service.create(kind, command(true, null, "  New ", null))).isSameAs(created);
    verify(repository).lock(kind);
    verify(repository).nameTaken(kind, "New", null);
    var entry = audited();
    assertThat(entry.actionType()).isEqualTo("INTERVIEW_" + kind + "_CREATED");
    assertThat(entry.targetType()).isEqualTo("INTERVIEW_" + kind);
    assertThat(entry.targetId()).isEqualTo(created.id().toString());
    assertThat(entry.actorUserId()).isEqualTo(actor);
    assertThat(entry.beforeJson()).isNull();
    assertThat(entry.afterJson()).contains("\"displayName\":\"New\"", "\"code\":\"NEW\"");
    assertThat(entry.afterJson().contains("usesStarFramework"))
        .isEqualTo(kind == CatalogKind.TOPIC);
  }

  @ParameterizedTest
  @EnumSource(CatalogKind.class)
  void createRejectsBlankAndOverlongNamesAndAcceptsSixtyCodePoints(CatalogKind kind) {
    for (String bad : new String[] {null, "", "   ", "x".repeat(61)}) {
      assertThat(codeOf(() -> service.create(kind, command(true, null, bad, null))))
          .isEqualTo("INB-V03");
    }
    verify(repository, never()).create(any(), any());
    when(repository.create(kind, "x".repeat(60))).thenReturn(one);
    service.create(kind, command(true, null, "x".repeat(60), null));
    verify(repository).create(kind, "x".repeat(60));
  }

  @ParameterizedTest
  @EnumSource(CatalogKind.class)
  void createMapsDuplicateNameToTheKindSpecificConflict(CatalogKind kind) {
    when(repository.nameTaken(kind, "Dup", null)).thenReturn(true);
    assertThat(codeOf(() -> service.create(kind, command(true, null, "Dup", null))))
        .isEqualTo(kind == CatalogKind.TOPIC ? "INB-020" : "INB-021");
    verify(repository, never()).create(any(), any());
    verifyNoInteractions(audit);
  }

  @Test
  void topicUpdateRenamesKeepsCodeAndAuditsBeforeAndAfter() {
    stored(CatalogKind.TOPIC);
    var result = service.update(CatalogKind.TOPIC, command(true, one.id(), "Renamed", null));
    assertThat(result.code()).isEqualTo("ONE");
    assertThat(result.displayName()).isEqualTo("Renamed");
    verify(repository).update(CatalogKind.TOPIC, one.id(), "Renamed", false);
    var entry = audited();
    assertThat(entry.actionType()).isEqualTo("INTERVIEW_TOPIC_UPDATED");
    assertThat(entry.beforeJson())
        .contains("\"displayName\":\"One\"", "\"usesStarFramework\":false");
    assertThat(entry.afterJson()).contains("\"displayName\":\"Renamed\"", "\"code\":\"ONE\"");
  }

  @Test
  void topicStarToggleAloneIsAChangeWithTheNameUntouched() {
    stored(CatalogKind.TOPIC);
    var result = service.update(CatalogKind.TOPIC, command(true, one.id(), null, true));
    assertThat(result.usesStarFramework()).isTrue();
    assertThat(result.displayName()).isEqualTo("One");
    verify(repository).update(CatalogKind.TOPIC, one.id(), "One", true);
    verify(repository, never()).nameTaken(any(), any(), any());
    assertThat(audited().afterJson()).contains("\"usesStarFramework\":true");
  }

  @ParameterizedTest
  @EnumSource(CatalogKind.class)
  void updateWithTheCurrentValuesWritesNothingAndAuditsNothing(CatalogKind kind) {
    stored(kind);
    assertThat(service.update(kind, command(true, one.id(), "One", false))).isSameAs(one);
    verify(repository, never()).update(any(), any(), any(), any());
    verifyNoInteractions(audit);
  }

  @Test
  void levelUpdateIsARenameThatNeverTouchesTheStarFlagAndRequiresAName() {
    stored(CatalogKind.LEVEL);
    assertThat(codeOf(() -> service.update(CatalogKind.LEVEL, command(true, one.id(), null, true))))
        .isEqualTo("INB-V03");
    service.update(CatalogKind.LEVEL, command(true, one.id(), "Harder", true));
    verify(repository).update(CatalogKind.LEVEL, one.id(), "Harder", null);
    assertThat(audited().actionType()).isEqualTo("INTERVIEW_LEVEL_RENAMED");
  }

  @ParameterizedTest
  @EnumSource(CatalogKind.class)
  void updateReportsMissingItemAndDuplicateNameWithKindSpecificCodes(CatalogKind kind) {
    stored(kind);
    UUID unknown = UUID.randomUUID();
    when(repository.find(kind, unknown)).thenReturn(Optional.empty());
    assertThat(codeOf(() -> service.update(kind, command(true, unknown, "Name", null))))
        .isEqualTo(kind == CatalogKind.TOPIC ? "INB-011" : "INB-012");
    when(repository.nameTaken(kind, "Two", one.id())).thenReturn(true);
    assertThat(codeOf(() -> service.update(kind, command(true, one.id(), "Two", null))))
        .isEqualTo(kind == CatalogKind.TOPIC ? "INB-020" : "INB-021");
    verify(repository, never()).update(any(), any(), any(), any());
  }

  @ParameterizedTest
  @EnumSource(CatalogKind.class)
  void reorderWritesTheFullListAndAuditsPreviousAndNewIds(CatalogKind kind) {
    var reordered = List.of(two, one);
    when(repository.list(kind, false)).thenReturn(List.of(one, two), reordered);
    assertThat(service.reorder(kind, command(true, null, null, null))).isEqualTo(reordered);
    verify(repository).reorder(kind, List.of(two.id(), one.id()));
    var entry = audited();
    assertThat(entry.actionType()).isEqualTo("INTERVIEW_" + kind + "_REORDERED");
    assertThat(entry.targetId()).isNull();
    assertThat(entry.beforeJson()).isEqualTo("[\"" + one.id() + "\",\"" + two.id() + "\"]");
    assertThat(entry.afterJson()).isEqualTo("[\"" + two.id() + "\",\"" + one.id() + "\"]");
  }

  @ParameterizedTest
  @EnumSource(CatalogKind.class)
  void reorderRejectsEveryMismatchedSetWithInb034AndKeepsTheOldOrder(CatalogKind kind) {
    stored(kind);
    UUID stranger = UUID.randomUUID();
    var bad =
        Arrays.asList(
            null,
            List.<UUID>of(),
            List.of(one.id()),
            List.of(one.id(), one.id()),
            List.of(one.id(), stranger),
            List.of(one.id(), two.id(), stranger),
            Arrays.asList(one.id(), null));
    for (List<UUID> ids : bad) {
      var cmd = new CatalogCommand(actor, true, null, null, null, ids);
      assertThat(codeOf(() -> service.reorder(kind, cmd))).isEqualTo("INB-034");
    }
    verify(repository, never()).reorder(any(), any());
    verifyNoInteractions(audit);
  }

  @Test
  void singleItemCatalogueAcceptsASingleElementOrder() {
    when(repository.list(CatalogKind.TOPIC, false)).thenReturn(List.of(one));
    var cmd = new CatalogCommand(actor, true, null, null, null, List.of(one.id()));
    service.reorder(CatalogKind.TOPIC, cmd);
    verify(repository).reorder(CatalogKind.TOPIC, List.of(one.id()));
  }

  @Test
  void topicInUseIsInb030WithTheCountEvenWhenAllReferencesAreRetired() {
    stored(CatalogKind.TOPIC);
    when(repository.references(CatalogKind.TOPIC, one.id())).thenReturn(1L);
    var cmd = command(true, one.id(), null, null);
    assertThatThrownBy(() -> service.delete(CatalogKind.TOPIC, cmd))
        .isInstanceOfSatisfying(
            AlgoPrepException.class,
            e -> {
              assertThat(e.errorCode()).isEqualTo("INB-030");
              assertThat(((ClientErrorDetails) e).clientErrorDetails())
                  .containsEntry("referencingQuestionCount", 1L)
                  .doesNotContainKey("reason");
            });
    verify(repository, never()).delete(any(), any());
    verifyNoInteractions(audit);
  }

  @Test
  void levelInUseIsInb031ThenLastLevelIsInb032WithItsReason() {
    stored(CatalogKind.LEVEL);
    when(repository.references(CatalogKind.LEVEL, one.id())).thenReturn(3L);
    var cmd = command(true, one.id(), null, null);
    assertThatThrownBy(() -> service.delete(CatalogKind.LEVEL, cmd))
        .isInstanceOfSatisfying(
            AlgoPrepException.class,
            e -> {
              assertThat(e.errorCode()).isEqualTo("INB-031");
              assertThat(((ClientErrorDetails) e).clientErrorDetails())
                  .containsEntry("reason", "IN_USE")
                  .containsEntry("referencingQuestionCount", 3L);
            });
    when(repository.references(CatalogKind.LEVEL, one.id())).thenReturn(0L);
    when(repository.list(CatalogKind.LEVEL, false)).thenReturn(List.of(one));
    assertThatThrownBy(() -> service.delete(CatalogKind.LEVEL, cmd))
        .isInstanceOfSatisfying(
            AlgoPrepException.class,
            e -> {
              assertThat(e.errorCode()).isEqualTo("INB-032");
              assertThat(((ClientErrorDetails) e).clientErrorDetails())
                  .containsEntry("reason", "LAST_LEVEL");
            });
    verify(repository, never()).delete(any(), any());
  }

  @Test
  void lastTopicMayBeDeletedBecauseNoMinimumExistsForTopics() {
    when(repository.find(CatalogKind.TOPIC, one.id())).thenReturn(Optional.of(one));
    when(repository.list(CatalogKind.TOPIC, false)).thenReturn(List.of(one));
    service.delete(CatalogKind.TOPIC, command(true, one.id(), null, null));
    verify(repository).delete(CatalogKind.TOPIC, one.id());
    var entry = audited();
    assertThat(entry.actionType()).isEqualTo("INTERVIEW_TOPIC_DELETED");
    assertThat(entry.afterJson()).isNull();
    assertThat(entry.beforeJson()).contains("\"code\":\"ONE\"");
  }

  @Test
  void deleteMissingItemIsNotFound() {
    when(repository.find(CatalogKind.LEVEL, one.id())).thenReturn(Optional.empty());
    assertThat(codeOf(() -> service.delete(CatalogKind.LEVEL, command(true, one.id(), null, null))))
        .isEqualTo("INB-012");
  }

  @ParameterizedTest
  @EnumSource(CatalogKind.class)
  void auditFailureBecomesInb503SoTheTransactionRollsBack(CatalogKind kind) {
    when(repository.create(kind, "New")).thenReturn(one);
    doThrow(new IllegalStateException("audit down")).when(audit).record(any());
    assertThat(codeOf(() -> service.create(kind, command(true, null, "New", null))))
        .isEqualTo("INB-503");
  }
}
