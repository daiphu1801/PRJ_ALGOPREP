package com.algoprep.judge.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.algoprep.judge.application.dto.LearnerListLimit;
import com.algoprep.judge.application.dto.SubmissionReads.Problem;
import com.algoprep.judge.application.ports.out.CurrentMetricsStudentPort;
import com.algoprep.judge.application.ports.out.ProblemSummaryReadPort;
import com.algoprep.judge.domain.exception.JudgeReadUnavailableException;
import com.algoprep.judge.domain.model.LearnerSubmissionCandidate;
import com.algoprep.judge.domain.ports.out.LearnerSubmissionListRepository;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

class GetLearnerSubmissionListsQueryHandlerTest {
  private final CurrentMetricsStudentPort principal = mock(CurrentMetricsStudentPort.class);
  private final LearnerSubmissionListRepository repository =
      mock(LearnerSubmissionListRepository.class);
  private final ProblemSummaryReadPort problems = mock(ProblemSummaryReadPort.class);
  private final GetLearnerSubmissionListsQueryHandler handler =
      new GetLearnerSubmissionListsQueryHandler(principal, repository, problems);
  private final UUID student = UUID.randomUUID();
  private final Instant now = Instant.parse("2026-11-05T00:00:00Z");

  private LearnerSubmissionCandidate row(String status) {
    return new LearnerSubmissionCandidate(
        UUID.randomUUID(), UUID.randomUUID(), "JAVA", status, 1, 2, null, 0, now);
  }

  private Problem metadata(LearnerSubmissionCandidate row, boolean withdrawn) {
    return new Problem(row.problemId(), "CODE", "Title", "slug", "Easy", withdrawn);
  }

  @Test
  void emptyListsRequireThePrincipalButNeverReadMetadata() {
    when(principal.requireStudent()).thenReturn(student);
    when(repository.inProgress(student, null, 32)).thenReturn(List.of());
    when(repository.accepted(student, 10)).thenReturn(List.of());
    assertThat(handler.inProgress(new LearnerListLimit(3))).isEmpty();
    assertThat(handler.accepted(new LearnerListLimit(10))).isEmpty();
    verifyNoInteractions(problems);
  }

  @Test
  void withdrawnCandidatesAreSkippedAcrossKeysetPagesBeforeTheRequestedLimit() {
    when(principal.requireStudent()).thenReturn(student);
    var hidden =
        java.util.stream.IntStream.range(0, 32).mapToObj(i -> row("WRONG_ANSWER")).toList();
    var visible = List.of(row("PENDING"), row("WRONG_ANSWER"), row("SYSTEM_ERROR"));
    when(repository.inProgress(student, null, 32)).thenReturn(hidden);
    when(repository.inProgress(student, hidden.getLast().cursor(), 32)).thenReturn(visible);
    when(problems.summaries(any()))
        .thenAnswer(
            call -> {
              Set<UUID> ids = call.getArgument(0);
              return java.util.stream.Stream.concat(
                      hidden.stream().map(r -> metadata(r, true)),
                      visible.stream().map(r -> metadata(r, false)))
                  .filter(p -> ids.contains(p.problemId()))
                  .toList();
            });
    var result = handler.inProgress(new LearnerListLimit(3));
    assertThat(result).hasSize(3);
    assertThat(result.stream().map(r -> r.latestSubmissionId()))
        .containsExactlyElementsOf(
            visible.stream().map(LearnerSubmissionCandidate::submissionId).toList());
    assertThat(result.getFirst().problem().withdrawn()).isFalse();
    assertThat(result.getLast().passedCount()).isNull();
    verify(repository).inProgress(student, hidden.getLast().cursor(), 32);
    assertThatThrownBy(() -> result.clear()).isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void missingMetadataIsNotPublicButAnUnavailablePortProducesNullSummaries() {
    when(principal.requireStudent()).thenReturn(student);
    var row = row("COMPILE_ERROR");
    when(repository.inProgress(student, null, 32)).thenReturn(List.of(row));
    when(problems.summaries(Set.of(row.problemId()))).thenReturn(List.of());
    assertThat(handler.inProgress(new LearnerListLimit(3))).isEmpty();
    when(problems.summaries(Set.of(row.problemId())))
        .thenThrow(new IllegalStateException("private F2 SQL"));
    var result = handler.inProgress(new LearnerListLimit(3));
    assertThat(result).hasSize(1);
    assertThat(result.getFirst().problem()).isNull();
    assertThat(result.getFirst().passedCount()).isNull();
    assertThat(result.getFirst().totalCount()).isNull();
  }

  @Test
  void acceptedKeepsHistoricalMetadataAndMeasurementsWithoutExtraProblemFields() {
    when(principal.requireStudent()).thenReturn(student);
    var row = row("ACCEPTED");
    when(repository.accepted(student, 20)).thenReturn(List.of(row));
    when(problems.summaries(Set.of(row.problemId()))).thenReturn(List.of(metadata(row, true)));
    var result = handler.accepted(new LearnerListLimit(20));
    assertThat(result).hasSize(1);
    assertThat(result.getFirst().submissionId()).isEqualTo(row.submissionId());
    assertThat(result.getFirst().problem().title()).isEqualTo("Title");
    assertThat(result.getFirst().runtimeMs()).isNull();
    assertThat(result.getFirst().memoryKb()).isZero();
    verify(repository).accepted(student, 20);
  }

  @Test
  void acceptedMetadataErrorsPreserveTheOwnAcceptedRowsWithNullMetadata() {
    when(principal.requireStudent()).thenReturn(student);
    var row = row("ACCEPTED");
    when(repository.accepted(student, 10)).thenReturn(List.of(row));
    when(problems.summaries(Set.of(row.problemId())))
        .thenThrow(new IllegalStateException("private F2"));
    var result = handler.accepted(new LearnerListLimit(10));
    assertThat(result).hasSize(1);
    assertThat(result.getFirst().problem()).isNull();
  }

  @Test
  void principalDenialsStayDenialsAndDatabaseErrorsNeverTurnIntoEmptyLists() {
    when(principal.requireStudent()).thenThrow(new AccessDeniedException("student required"));
    assertThatThrownBy(() -> handler.inProgress(new LearnerListLimit(3)))
        .isInstanceOf(AccessDeniedException.class);
    assertThatThrownBy(() -> handler.accepted(new LearnerListLimit(10)))
        .isInstanceOf(AccessDeniedException.class);
    verifyNoInteractions(repository, problems);
    doReturn(student).when(principal).requireStudent();
    when(repository.inProgress(any(), any(), anyInt()))
        .thenThrow(new IllegalStateException("private SQL password"));
    when(repository.accepted(any(), anyInt()))
        .thenThrow(new IllegalStateException("private SQL password"));
    assertThatThrownBy(() -> handler.inProgress(new LearnerListLimit(3)))
        .isInstanceOf(JudgeReadUnavailableException.class)
        .hasNoCause();
    assertThatThrownBy(() -> handler.accepted(new LearnerListLimit(10)))
        .isInstanceOf(JudgeReadUnavailableException.class)
        .hasMessage("Judge read unavailable")
        .hasNoCause();
  }
}
