package com.algoprep.bootstrap.judge;

import com.algoprep.judge.application.dto.GradingSpecification;
import com.algoprep.judge.application.dto.GradingTestcase;
import com.algoprep.judge.application.dto.GradingTestcaseSnapshot;
import com.algoprep.judge.application.ports.out.ProblemGradingSpecPort;
import com.algoprep.judge.domain.exception.ProblemGradingReadException;
import com.algoprep.problembank.application.ports.in.ReadProblemGradingSpecUseCase;
import com.algoprep.problembank.application.ports.in.ReadSampleTestcasesUseCase;
import com.algoprep.problembank.application.ports.in.ReadTestcaseSnapshotUseCase;
import com.algoprep.problembank.domain.exception.GradingDataException;
import com.algoprep.problembank.domain.model.TestcaseData;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;

@Component
public final class ProblemGradingReadBridge implements ProblemGradingSpecPort {
  private final ReadProblemGradingSpecUseCase specifications;
  private final ReadSampleTestcasesUseCase samples;
  private final ReadTestcaseSnapshotUseCase snapshots;

  public ProblemGradingReadBridge(
      ReadProblemGradingSpecUseCase specifications,
      ReadSampleTestcasesUseCase samples,
      ReadTestcaseSnapshotUseCase snapshots) {
    this.specifications = specifications;
    this.samples = samples;
    this.snapshots = snapshots;
  }

  @Override
  public Optional<GradingSpecification> loadGradingSpec(UUID problemId) {
    return read(() -> specifications.loadGradingSpec(problemId).map(this::specification));
  }

  private GradingSpecification specification(
      com.algoprep.problembank.domain.model.GradingSpecification result) {
    var data = result.data();
    var limits = data.limits();
    return new GradingSpecification(
        new GradingSpecification.Problem(
            data.problem().problemId(),
            data.problem().published(),
            data.problem().functionWrapperSupported(),
            result.snapshotReady()),
        new GradingSpecification.Limits(
            limits.timeLimitMs(),
            limits.memoryLimitMb(),
            limits.maxOutputSizeKb(),
            limits.maxSubmissionsPerHour()),
        new GradingSpecification.Versions(
            data.versions().specVersion(), data.versions().currentTestcaseSetVersion()),
        new GradingSpecification.Matching(
            data.matching().matchingStrategy(), data.matching().epsilonValue()),
        result.testcases().stream()
            .map(
                row ->
                    new GradingSpecification.Testcase(
                        row.testcaseId(), row.visibility(), row.position()))
            .toList());
  }

  @Override
  public List<GradingTestcase> loadSampleTestcases(UUID problemId) {
    return read(() -> samples.loadSampleTestcases(problemId).stream().map(this::testcase).toList());
  }

  @Override
  public GradingTestcaseSnapshot loadTestcaseSnapshot(UUID problemId, int version) {
    return read(
        () -> {
          var result = snapshots.loadTestcaseSnapshot(problemId, version);
          return new GradingTestcaseSnapshot(
              result.problemId(),
              result.version(),
              result.testcases().stream().map(this::testcase).toList());
        });
  }

  private GradingTestcase testcase(TestcaseData row) {
    return new GradingTestcase(
        row.testcaseId(), row.visibility(), row.position(), row.input(), row.expectedOutput());
  }

  private static <T> T read(Supplier<T> operation) {
    try {
      return operation.get();
    } catch (RuntimeException failure) {
      throw new ProblemGradingReadException(
          failure instanceof GradingDataException data
                  && data.kind() == GradingDataException.Kind.SPEC_UNAVAILABLE
              ? ProblemGradingReadException.Reason.SPEC_UNAVAILABLE
              : ProblemGradingReadException.Reason.SANDBOX_FAULT);
    }
  }
}
