package com.algoprep.interviewbank.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.common.exception.AlgoPrepException;
import com.algoprep.common.exception.ClientErrorDetails;
import com.algoprep.interviewbank.application.dto.BulkRetireCommand;
import com.algoprep.interviewbank.application.dto.QuestionCommand;
import com.algoprep.interviewbank.domain.model.AdminQuestion;
import com.algoprep.interviewbank.domain.model.CatalogKind;
import com.algoprep.interviewbank.domain.model.InterviewAuditEntry;
import com.algoprep.interviewbank.domain.model.QuestionDraft;
import com.algoprep.interviewbank.domain.model.QuestionRow;
import com.algoprep.interviewbank.domain.model.RubricCriterion;
import com.algoprep.interviewbank.domain.ports.out.AdminQuestionRepository;
import com.algoprep.interviewbank.domain.ports.out.InterviewBankAuditPort;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class QuestionAuthoringServiceTest {
  private final AdminQuestionRepository repository = mock(AdminQuestionRepository.class);
  private final InterviewBankAuditPort audit = mock(InterviewBankAuditPort.class);
  private final QuestionAuthoringService service = new QuestionAuthoringService(repository, audit);
  private final UUID actor = UUID.randomUUID();
  private final UUID topic = UUID.randomUUID();
  private final UUID level = UUID.randomUUID();
  private final UUID questionId = UUID.randomUUID();

  private static RubricCriterion criterion(String code, String weight) {
    return new RubricCriterion(code, new BigDecimal(weight), null);
  }

  private QuestionCommand command(UUID id, UUID topicId, List<RubricCriterion> rubric) {
    return new QuestionCommand(actor, id, topicId, level, "Title", null, List.of(), rubric);
  }

  private AdminQuestion stored(UUID topicId, List<RubricCriterion> rubric) {
    return new AdminQuestion(
        questionId,
        7,
        topicId,
        level,
        "Title",
        null,
        List.of(),
        rubric,
        Instant.EPOCH,
        Instant.EPOCH,
        0,
        null);
  }

  private static AlgoPrepException failure(Runnable call) {
    var captured = new AlgoPrepException[1];
    assertThatThrownBy(call::run)
        .isInstanceOfSatisfying(AlgoPrepException.class, e -> captured[0] = e);
    return captured[0];
  }

  private void referencesExist() {
    when(repository.lockReference(CatalogKind.TOPIC, topic)).thenReturn(true);
    when(repository.lockReference(CatalogKind.LEVEL, level)).thenReturn(true);
  }

  @Test
  void rubricWeightsMustSumToExactlyOneHundredAndReportTheCurrentSum() {
    var bad =
        command(
            null, topic, List.of(criterion("A", "30"), criterion("B", "40"), criterion("C", "20")));
    var error = failure(() -> service.create(bad));
    assertThat(error.errorCode()).isEqualTo("INB-033");
    assertThat((BigDecimal) ((ClientErrorDetails) error).clientErrorDetails().get("currentSum"))
        .isEqualByComparingTo("90");
    verify(repository, never()).insert(any(), any());
    var close = command(null, topic, List.of(criterion("A", "99.99")));
    assertThat(failure(() -> service.create(close)).errorCode()).isEqualTo("INB-033");
  }

  @Test
  void splitWeightsOfThirdsAreAcceptedBecauseSumIsExactDecimal() {
    referencesExist();
    var rubric = List.of(criterion("A", "33.33"), criterion("B", "33.33"), criterion("C", "33.34"));
    when(repository.insert(any(), any())).thenReturn(stored(topic, rubric));
    service.create(command(null, topic, rubric));
    verify(repository).insert(any(QuestionDraft.class), any());
  }

  @Test
  void emptyRubricIsAllowedAndDuplicateNamesIgnoreCase() {
    referencesExist();
    when(repository.insert(any(), any())).thenReturn(stored(topic, List.of()));
    service.create(command(null, topic, List.of()));
    var twins =
        command(null, topic, List.of(criterion("Rõ ràng", "50"), criterion("RÕ RÀNG", "50")));
    assertThat(failure(() -> service.create(twins)).errorCode()).isEqualTo("INB-V11");
  }

  @Test
  void missingTopicOrLevelNamesTheFieldAndWritesNothing() {
    when(repository.lockReference(CatalogKind.TOPIC, topic)).thenReturn(false);
    var topicError = failure(() -> service.create(command(null, topic, List.of())));
    assertThat(topicError.errorCode()).isEqualTo("INB-035");
    assertThat(((ClientErrorDetails) topicError).clientErrorDetails())
        .containsEntry("field", "topicId");
    when(repository.lockReference(CatalogKind.TOPIC, topic)).thenReturn(true);
    when(repository.lockReference(CatalogKind.LEVEL, level)).thenReturn(false);
    var levelError = failure(() -> service.create(command(null, topic, List.of())));
    assertThat(((ClientErrorDetails) levelError).clientErrorDetails())
        .containsEntry("field", "levelId");
    verify(repository, never()).insert(any(), any());
    verify(audit, never()).record(any());
  }

  @Test
  void createAuditsOneRowWithActorAndSnapshot() {
    referencesExist();
    var rubric = List.of(criterion("A", "100"));
    when(repository.insert(any(), any())).thenReturn(stored(topic, rubric));
    service.create(command(null, topic, rubric));
    var captor = ArgumentCaptor.forClass(InterviewAuditEntry.class);
    verify(audit).record(captor.capture());
    var entry = captor.getValue();
    assertThat(entry.actorUserId()).isEqualTo(actor);
    assertThat(entry.actionType()).isEqualTo("INTERVIEW_QUESTION_CREATED");
    assertThat(entry.targetType()).isEqualTo("INTERVIEW_QUESTION");
    assertThat(entry.targetId()).isEqualTo(questionId.toString());
    assertThat(entry.beforeJson()).isNull();
    assertThat(entry.afterJson()).contains("\"rubricCount\":1", "\"rubricSum\":\"100\"");
  }

  @Test
  void auditFailureBecomesInb503() {
    referencesExist();
    when(repository.insert(any(), any())).thenReturn(stored(topic, List.of()));
    doThrow(new IllegalStateException("down")).when(audit).record(any());
    assertThat(failure(() -> service.create(command(null, topic, List.of()))).errorCode())
        .isEqualTo("INB-503");
  }

  @Test
  void updateOfMissingOrRetiredQuestionIsInb010() {
    when(repository.lockActive(questionId)).thenReturn(Optional.empty());
    assertThat(failure(() -> service.update(command(questionId, topic, List.of()))).errorCode())
        .isEqualTo("INB-010");
    verify(repository, never()).update(any(), any());
  }

  @Test
  void updateChangingTopicRecalculatesBothTopicsOnlyThen() {
    var oldTopic = UUID.randomUUID();
    referencesExist();
    when(repository.lockActive(questionId))
        .thenReturn(Optional.of(new QuestionRow(questionId, oldTopic, "ACTIVE", "Title")));
    when(repository.findActive(questionId)).thenReturn(Optional.of(stored(oldTopic, List.of())));
    service.update(command(questionId, topic, List.of()));
    verify(repository).recalculateProgress(questionId, List.of(oldTopic, topic));
    verify(repository).replaceRubric(questionId, List.of());
    when(repository.lockActive(questionId))
        .thenReturn(Optional.of(new QuestionRow(questionId, topic, "ACTIVE", "Title")));
    service.update(command(questionId, topic, List.of()));
    verify(repository).recalculateProgress(any(), any());
  }

  @Test
  void updateAuditsBeforeAndAfter() {
    referencesExist();
    when(repository.lockActive(questionId))
        .thenReturn(Optional.of(new QuestionRow(questionId, topic, "ACTIVE", "Title")));
    when(repository.findActive(questionId)).thenReturn(Optional.of(stored(topic, List.of())));
    service.update(command(questionId, topic, List.of()));
    var captor = ArgumentCaptor.forClass(InterviewAuditEntry.class);
    verify(audit).record(captor.capture());
    assertThat(captor.getValue().actionType()).isEqualTo("INTERVIEW_QUESTION_UPDATED");
    assertThat(captor.getValue().beforeJson()).contains("\"title\":\"Title\"");
    assertThat(captor.getValue().afterJson()).contains("\"rubricCount\":0");
  }

  @Test
  void bulkRetireSplitsRetiredFromFailedAndAuditsEachRetiredOne() {
    var active = UUID.randomUUID();
    var done = UUID.randomUUID();
    var unknown = UUID.randomUUID();
    when(repository.lockAll(List.of(active, done, unknown)))
        .thenReturn(
            List.of(
                new QuestionRow(active, topic, "ACTIVE", "A"),
                new QuestionRow(done, topic, "RETIRED", "B")));
    var result = service.retire(new BulkRetireCommand(actor, List.of(active, done, unknown)));
    assertThat(result.retiredIds()).containsExactly(active);
    assertThat(result.failedIds()).containsExactly(done, unknown);
    verify(repository).retire(active);
    verify(repository, never()).retire(done);
    verify(repository).recalculateProgress(active, List.of(topic));
    var captor = ArgumentCaptor.forClass(InterviewAuditEntry.class);
    verify(audit).record(captor.capture());
    assertThat(captor.getValue().actionType()).isEqualTo("INTERVIEW_QUESTION_RETIRED");
    assertThat(captor.getValue().afterJson()).contains("RETIRED");
  }

  @Test
  void handlersDelegateToTheService() {
    referencesExist();
    when(repository.insert(any(), any())).thenReturn(stored(topic, List.of()));
    assertThat(new CreateQuestionCommandHandler(service).execute(command(null, topic, List.of())))
        .isNotNull();
    when(repository.lockActive(questionId))
        .thenReturn(Optional.of(new QuestionRow(questionId, topic, "ACTIVE", "Title")));
    when(repository.findActive(questionId)).thenReturn(Optional.of(stored(topic, List.of())));
    assertThat(
            new UpdateQuestionCommandHandler(service)
                .execute(command(questionId, topic, List.of())))
        .isNotNull();
    when(repository.lockAll(List.of(questionId))).thenReturn(List.of());
    assertThat(
            new BulkRetireQuestionsCommandHandler(service)
                .execute(new BulkRetireCommand(actor, List.of(questionId)))
                .failedIds())
        .containsExactly(questionId);
  }
}
