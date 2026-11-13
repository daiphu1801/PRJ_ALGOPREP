package com.algoprep.bootstrap.judge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.algoprep.judge.domain.exception.ProblemGradingReadException;
import com.algoprep.problembank.application.ports.in.ReadProblemGradingSpecUseCase;
import com.algoprep.problembank.application.ports.in.ReadSampleTestcasesUseCase;
import com.algoprep.problembank.application.ports.in.ReadTestcaseSnapshotUseCase;
import com.algoprep.problembank.domain.exception.GradingDataException;
import com.algoprep.problembank.domain.model.GradingSpecData;
import com.algoprep.problembank.domain.model.GradingSpecification;
import com.algoprep.problembank.domain.model.TestcaseData;
import com.algoprep.problembank.domain.model.TestcaseDescriptor;
import com.algoprep.problembank.domain.model.TestcaseSnapshot;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProblemGradingReadBridgeTest {
  private final ReadProblemGradingSpecUseCase specs = mock(ReadProblemGradingSpecUseCase.class);
  private final ReadSampleTestcasesUseCase samples = mock(ReadSampleTestcasesUseCase.class);
  private final ReadTestcaseSnapshotUseCase snapshots = mock(ReadTestcaseSnapshotUseCase.class);
  private final ProblemGradingReadBridge bridge =
      new ProblemGradingReadBridge(specs, samples, snapshots);
  private final UUID id = UUID.randomUUID();
  private final UUID testcaseId = UUID.randomUUID();

  @Test
  void preservesAllMetadataUnitsPrecisionFlagsAndNullableQuota() {
    var data =
        new GradingSpecData(
            new GradingSpecData.Problem(id, false, true),
            new GradingSpecData.Limits(1700, 384, 96, null),
            new GradingSpecData.Versions(130, 7),
            new GradingSpecData.Matching("EPSILON", new BigDecimal("0.000000000001")));
    when(specs.loadGradingSpec(id))
        .thenReturn(
            Optional.of(
                new GradingSpecification(
                    data, List.of(new TestcaseDescriptor(testcaseId, "HIDDEN", 4)), true)));
    var result = bridge.loadGradingSpec(id).orElseThrow();
    assertThat(result.problem().problemId()).isEqualTo(id);
    assertThat(result.problem().published()).isFalse();
    assertThat(result.problem().functionWrapperSupported()).isTrue();
    assertThat(result.problem().snapshotReady()).isTrue();
    assertThat(result.limits().timeLimitMs()).isEqualTo(1700);
    assertThat(result.limits().memoryLimitMb()).isEqualTo(384);
    assertThat(result.limits().maxOutputSizeKb()).isEqualTo(96);
    assertThat(result.limits().maxSubmissionsPerHour()).isNull();
    assertThat(result.versions().specVersion()).isEqualTo(130);
    assertThat(result.versions().currentTestcaseSetVersion()).isEqualTo(7);
    assertThat(result.matching().epsilonValue()).isEqualByComparingTo("0.000000000001");
    assertThat(result.testcases().getFirst().testcaseId()).isEqualTo(testcaseId);
  }

  @Test
  void mapsSamplesAndPinnedSnapshotContentWithoutChangingBytes() {
    var row = new TestcaseData(testcaseId, "SAMPLE", 2, new byte[] {0, -1}, new byte[0]);
    when(samples.loadSampleTestcases(id)).thenReturn(List.of(row));
    when(snapshots.loadTestcaseSnapshot(id, 7))
        .thenReturn(new TestcaseSnapshot(id, 7, List.of(row)));
    assertThat(bridge.loadSampleTestcases(id).getFirst().input()).containsExactly(0, -1);
    var result = bridge.loadTestcaseSnapshot(id, 7);
    assertThat(result.version()).isEqualTo(7);
    assertThat(result.problemId()).isEqualTo(id);
    assertThat(result.testcases().getFirst().expectedOutput()).isEmpty();
  }

  @Test
  void absentProblemRemainsAbsent() {
    when(specs.loadGradingSpec(id)).thenReturn(Optional.empty());
    assertThat(bridge.loadGradingSpec(id)).isEmpty();
  }

  @Test
  void corruptDataUsesSpecUnavailableForEveryReadWithoutLeakingCauses() {
    var failure = new GradingDataException(GradingDataException.Kind.SPEC_UNAVAILABLE);
    when(specs.loadGradingSpec(id)).thenThrow(failure);
    when(samples.loadSampleTestcases(id)).thenThrow(failure);
    when(snapshots.loadTestcaseSnapshot(id, 7)).thenThrow(failure);
    reason(() -> bridge.loadGradingSpec(id), ProblemGradingReadException.Reason.SPEC_UNAVAILABLE);
    reason(
        () -> bridge.loadSampleTestcases(id), ProblemGradingReadException.Reason.SPEC_UNAVAILABLE);
    reason(
        () -> bridge.loadTestcaseSnapshot(id, 7),
        ProblemGradingReadException.Reason.SPEC_UNAVAILABLE);
  }

  @Test
  void unknownAndInfrastructureFailuresUseSandboxFault() {
    when(specs.loadGradingSpec(id)).thenThrow(new IllegalStateException("private SQL"));
    when(snapshots.loadTestcaseSnapshot(id, 7))
        .thenThrow(new GradingDataException(GradingDataException.Kind.INFRASTRUCTURE));
    reason(() -> bridge.loadGradingSpec(id), ProblemGradingReadException.Reason.SANDBOX_FAULT);
    reason(
        () -> bridge.loadTestcaseSnapshot(id, 7), ProblemGradingReadException.Reason.SANDBOX_FAULT);
  }

  private static void reason(Runnable operation, ProblemGradingReadException.Reason reason) {
    assertThatThrownBy(operation::run)
        .isInstanceOfSatisfying(
            ProblemGradingReadException.class,
            failure -> {
              assertThat(failure.reason()).isEqualTo(reason);
              assertThat(failure.getMessage()).isEqualTo("Problem grading data is unavailable");
              assertThat(failure.getCause()).isNull();
              assertThat(failure.getSuppressed()).isEmpty();
            });
  }
}
