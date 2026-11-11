package com.algoprep.bootstrap.aireview;

import com.algoprep.aireview.application.ports.out.SubmissionSnapshotPort;
import com.algoprep.aireview.domain.exception.AiProviderUnavailableException;
import com.algoprep.aireview.domain.exception.AiSubmissionNotAcceptedException;
import com.algoprep.aireview.domain.exception.AiSubmissionNotFoundException;
import com.algoprep.aireview.domain.model.SubmissionSnapshot;
import com.algoprep.judge.application.ports.in.GetAcceptedSubmissionSnapshotUseCase;
import com.algoprep.judge.domain.exception.SubmissionNotAcceptedException;
import com.algoprep.judge.domain.exception.SubmissionNotFoundException;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Type translation only: F5 asks in its own vocabulary, judge-orchestration answers through {@link
 * GetAcceptedSubmissionSnapshotUseCase}. That use case checks no ownership; the F5 use case
 * compares the user id. The F4 snapshot fields that F5 must not see (runtime, memory) are dropped.
 */
@Component
final class SubmissionSnapshotBridge implements SubmissionSnapshotPort {
  private final GetAcceptedSubmissionSnapshotUseCase judge;

  SubmissionSnapshotBridge(GetAcceptedSubmissionSnapshotUseCase judge) {
    this.judge = judge;
  }

  @Override
  public SubmissionSnapshot getAccepted(UUID submissionId) {
    try {
      var snapshot = judge.getAccepted(submissionId);
      return new SubmissionSnapshot(
          snapshot.submissionId(),
          snapshot.userId(),
          snapshot.problemId(),
          snapshot.language(),
          snapshot.sourceCode());
    } catch (SubmissionNotFoundException missing) {
      throw new AiSubmissionNotFoundException();
    } catch (SubmissionNotAcceptedException notAccepted) {
      throw new AiSubmissionNotAcceptedException();
    } catch (RuntimeException failure) {
      throw new AiProviderUnavailableException(
          AiProviderUnavailableException.Reason.UPSTREAM_PORT, failure);
    }
  }
}
