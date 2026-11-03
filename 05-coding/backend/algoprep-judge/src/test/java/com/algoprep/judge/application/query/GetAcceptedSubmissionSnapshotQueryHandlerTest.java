package com.algoprep.judge.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.algoprep.judge.domain.exception.JudgeReadUnavailableException;
import com.algoprep.judge.domain.exception.SubmissionNotAcceptedException;
import com.algoprep.judge.domain.exception.SubmissionNotFoundException;
import com.algoprep.judge.domain.model.AcceptedSubmissionSnapshot;
import com.algoprep.judge.domain.model.SubmissionSnapshotLookup;
import com.algoprep.judge.domain.ports.out.SubmissionSnapshotRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GetAcceptedSubmissionSnapshotQueryHandlerTest {
  private final SubmissionSnapshotRepository repository = mock(SubmissionSnapshotRepository.class);
  private final GetAcceptedSubmissionSnapshotQueryHandler handler =
      new GetAcceptedSubmissionSnapshotQueryHandler(repository);
  private final UUID id = UUID.randomUUID();
  private final AcceptedSubmissionSnapshot snapshot =
      new AcceptedSubmissionSnapshot(
          id, UUID.randomUUID(), UUID.randomUUID(), "JAVA", "private-source-secret", 12, 1024);

  @Test
  void acceptedReturnsTheSnapshotAndDiagnosticStringsNeverContainSource() {
    var row = new SubmissionSnapshotLookup("ACCEPTED", snapshot);
    when(repository.find(id)).thenReturn(Optional.of(row));
    assertThat(handler.getAccepted(id)).isEqualTo(snapshot);
    assertThat(snapshot.toString()).contains("REDACTED").doesNotContain("private-source-secret");
    assertThat(row.toString()).doesNotContain("private-source-secret");
  }

  @Test
  void missingIsDistinctFromNotAcceptedAndInvalidIdsNeverBecomeAnEmptySnapshot() {
    when(repository.find(id)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> handler.getAccepted(id))
        .isInstanceOf(SubmissionNotFoundException.class)
        .hasNoCause();
    assertThatThrownBy(() -> handler.getAccepted(null)).isInstanceOf(NullPointerException.class);
  }

  @Test
  void everyNonAcceptedStateIsRejectedEvenIfARepositoryAccidentallySuppliesSource() {
    for (String status :
        List.of(
            "PENDING",
            "COMPILING",
            "JUDGING",
            "WRONG_ANSWER",
            "TIME_LIMIT_EXCEEDED",
            "RUNTIME_ERROR",
            "COMPILE_ERROR",
            "SYSTEM_ERROR")) {
      when(repository.find(id))
          .thenReturn(Optional.of(new SubmissionSnapshotLookup(status, snapshot)));
      assertThatThrownBy(() -> handler.getAccepted(id))
          .isInstanceOf(SubmissionNotAcceptedException.class)
          .hasMessage("Submission not accepted")
          .hasNoCause();
    }
  }

  @Test
  void databaseFailureAndMalformedAcceptedRowsAreSanitized() {
    when(repository.find(id))
        .thenReturn(Optional.of(new SubmissionSnapshotLookup("ACCEPTED", null)));
    assertThatThrownBy(() -> handler.getAccepted(id))
        .isInstanceOf(JudgeReadUnavailableException.class)
        .hasNoCause();
    when(repository.find(id)).thenThrow(new IllegalStateException("private SQL source secret"));
    assertThatThrownBy(() -> handler.getAccepted(id))
        .isInstanceOf(JudgeReadUnavailableException.class)
        .hasMessage("Judge read unavailable")
        .hasNoCause();
  }
}
