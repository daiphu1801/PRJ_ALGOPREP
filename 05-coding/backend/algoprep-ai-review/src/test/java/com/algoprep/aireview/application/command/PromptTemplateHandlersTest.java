package com.algoprep.aireview.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.aireview.application.dto.PublishChange;
import com.algoprep.aireview.application.dto.SavePromptDraftCommand;
import com.algoprep.aireview.application.ports.out.AiAuditPort;
import com.algoprep.aireview.application.ports.out.AiModelOptionsPort;
import com.algoprep.aireview.domain.exception.AiPromptConflictException;
import com.algoprep.aireview.domain.exception.AiPromptRuleException;
import com.algoprep.aireview.domain.model.AiAuditEntry;
import com.algoprep.aireview.domain.model.FeatureCode;
import com.algoprep.aireview.domain.model.PromptContent;
import com.algoprep.aireview.domain.model.PromptStatus;
import com.algoprep.aireview.domain.model.PromptTemplate;
import com.algoprep.aireview.domain.model.ResponseGuards;
import com.algoprep.aireview.domain.ports.out.PromptTemplateRepository;
import com.algoprep.common.exception.ServiceUnavailableException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class PromptTemplateHandlersTest {
  private static final FeatureCode F = FeatureCode.SOLUTION_REVIEW;
  private static final ResponseGuards G = new ResponseGuards(true, true, true);
  private final PromptTemplateRepository repo = mock(PromptTemplateRepository.class);
  private final AiModelOptionsPort models = mock(AiModelOptionsPort.class);
  private final AiAuditPort audit = mock(AiAuditPort.class);
  private final SavePromptDraftCommandHandler save =
      new SavePromptDraftCommandHandler(repo, models);
  private final PublishPromptTemplatesCommandHandler publish =
      new PublishPromptTemplatesCommandHandler(repo, models, audit);
  private final UUID actor = UUID.randomUUID();
  private final PromptTemplate active = row(PromptStatus.ACTIVE, "v1.0", "old", "default");

  private static PromptTemplate row(
      PromptStatus status, String version, String text, String model) {
    return new PromptTemplate(
        UUID.randomUUID(),
        F,
        version,
        status,
        new PromptContent(text, model, G),
        "v1",
        status == PromptStatus.DRAFT ? null : Instant.EPOCH,
        Instant.EPOCH);
  }

  private void stubFeature(PromptTemplate draft) {
    when(models.allowed()).thenReturn(List.of("m1", "m2"));
    when(repo.find(F, PromptStatus.ACTIVE)).thenReturn(Optional.of(active));
    when(repo.find(F, PromptStatus.DRAFT)).thenReturn(Optional.ofNullable(draft));
    when(repo.listLive()).thenReturn(List.of(active));
  }

  private PublishChange change(String text, String model, UUID expected) {
    return new PublishChange(F, expected, text, model, null);
  }

  @Test
  void draftWithModelOutsideTheListIsRefusedBeforeAnyWrite() {
    when(models.allowed()).thenReturn(List.of("m1"));
    var command = new SavePromptDraftCommand(actor, F, new PromptContent("x", "evil", G));
    assertThatThrownBy(() -> save.save(command))
        .isInstanceOfSatisfying(
            AiPromptRuleException.class, e -> assertThat(e.errorCode()).isEqualTo("AIR-202"));
    verify(repo, never()).lock(any());
    verify(repo, never()).createDraft(any(), any(), any());
  }

  @Test
  void savingWithoutDraftCreatesOneAndWithDraftUpdatesInPlace() {
    when(models.allowed()).thenReturn(List.of("m1"));
    var content = new PromptContent("x", "m1", G);
    when(repo.find(F, PromptStatus.DRAFT)).thenReturn(Optional.empty());
    save.save(new SavePromptDraftCommand(actor, F, content));
    verify(repo).createDraft(F, content, actor);
    PromptTemplate draft = row(PromptStatus.DRAFT, "v1.1", "d", "m1");
    when(repo.find(F, PromptStatus.DRAFT)).thenReturn(Optional.of(draft));
    save.save(new SavePromptDraftCommand(actor, F, content));
    verify(repo).updateDraft(draft.id(), content);
  }

  @Test
  void publishingADraftArchivesTheActiveRowPromotesTheDraftAndAuditsOnce() {
    PromptTemplate draft = row(PromptStatus.DRAFT, "v1.1", "new", "m1");
    stubFeature(draft);
    when(repo.promote(any(), any())).thenReturn(row(PromptStatus.ACTIVE, "v1.1", "new", "m1"));
    publish.publish(actor, List.of(change(null, null, active.id())));
    verify(repo).archive(active.id());
    verify(repo).promote(draft.id(), draft.content());
    ArgumentCaptor<AiAuditEntry> entry = ArgumentCaptor.forClass(AiAuditEntry.class);
    verify(audit).record(entry.capture());
    assertThat(entry.getValue().actionType()).isEqualTo("AI_PROMPT_PUBLISHED");
    assertThat(entry.getValue().targetId()).isEqualTo("SOLUTION_REVIEW");
    assertThat(entry.getValue().beforeJson()).contains("v1.0").doesNotContain("old");
    assertThat(entry.getValue().afterJson()).contains("v1.1").doesNotContain("new\"");
  }

  @Test
  void publishingWithoutDraftCopiesTheActiveContentWithTheSentModel() {
    stubFeature(null);
    when(repo.createActive(any(), any(), any()))
        .thenReturn(row(PromptStatus.ACTIVE, "v1.1", "old", "m2"));
    publish.publish(actor, List.of(change(null, "m2", active.id())));
    verify(repo).createActive(F, new PromptContent("old", "m2", G), actor);
  }

  @Test
  void aStaleActiveIdIsAConflictAndNothingIsWritten() {
    stubFeature(null);
    assertThatThrownBy(() -> publish.publish(actor, List.of(change("t", "m1", UUID.randomUUID()))))
        .isInstanceOf(AiPromptConflictException.class);
    verify(repo, never()).archive(any());
    verify(audit, never()).record(any());
  }

  @Test
  void aModelOutsideTheListFailsButTheInheritedActiveModelIsTrusted() {
    stubFeature(null);
    assertThatThrownBy(() -> publish.publish(actor, List.of(change(null, "evil", active.id()))))
        .isInstanceOfSatisfying(
            AiPromptRuleException.class, e -> assertThat(e.errorCode()).isEqualTo("AIR-202"));
    when(repo.createActive(any(), any(), any()))
        .thenReturn(row(PromptStatus.ACTIVE, "v1.1", "t", "default"));
    publish.publish(actor, List.of(change("t", null, active.id())));
    verify(repo).createActive(F, new PromptContent("t", "default", G), actor);
  }

  @Test
  void aPublishThatChangesNothingIsAir205() {
    stubFeature(null);
    assertThatThrownBy(() -> publish.publish(actor, List.of(change("old", null, active.id()))))
        .isInstanceOfSatisfying(
            AiPromptRuleException.class, e -> assertThat(e.errorCode()).isEqualTo("AIR-205"));
    verify(repo, never()).archive(any());
  }

  @Test
  void anAuditFailureBecomesAir503() {
    stubFeature(null);
    when(repo.createActive(any(), any(), any()))
        .thenReturn(row(PromptStatus.ACTIVE, "v1.1", "t", "default"));
    doThrow(new IllegalStateException("down")).when(audit).record(any());
    assertThatThrownBy(() -> publish.publish(actor, List.of(change("t", null, active.id()))))
        .isInstanceOfSatisfying(
            ServiceUnavailableException.class, e -> assertThat(e.errorCode()).isEqualTo("AIR-503"));
  }
}
