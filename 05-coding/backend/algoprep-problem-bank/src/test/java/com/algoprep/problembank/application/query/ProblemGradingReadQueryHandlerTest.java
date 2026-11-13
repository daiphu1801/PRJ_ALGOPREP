package com.algoprep.problembank.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.algoprep.problembank.application.ports.out.TestcaseObjectStorePort;
import com.algoprep.problembank.application.snapshot.TestcaseSnapshotCodec;
import com.algoprep.problembank.domain.exception.GradingDataException;
import com.algoprep.problembank.domain.exception.TestcaseStorageUnavailableException;
import com.algoprep.problembank.domain.model.GradingReadSnapshot;
import com.algoprep.problembank.domain.model.GradingSpecData;
import com.algoprep.problembank.domain.model.SnapshotMetadata;
import com.algoprep.problembank.domain.model.StoredSampleTestcase;
import com.algoprep.problembank.domain.model.TestcaseData;
import com.algoprep.problembank.domain.model.TestcaseDescriptor;
import com.algoprep.problembank.domain.ports.out.GradingReadRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProblemGradingReadQueryHandlerTest {
  private final GradingReadRepository repository = mock(GradingReadRepository.class);
  private final TestcaseObjectStorePort storage = mock(TestcaseObjectStorePort.class);
  private final TestcaseSnapshotCodec codec = new TestcaseSnapshotCodec();
  private final ProblemGradingReadQueryHandler handler =
      new ProblemGradingReadQueryHandler(repository, storage, codec);
  private final UUID id = UUID.randomUUID();
  private final UUID testcaseId = UUID.randomUUID();
  private final String key = "problems/" + id + "/snapshots/7.json";

  private GradingSpecData data(int version, String matching, BigDecimal epsilon) {
    return new GradingSpecData(
        new GradingSpecData.Problem(id, true, true),
        new GradingSpecData.Limits(1200, 256, 64, null),
        new GradingSpecData.Versions(version, 7),
        new GradingSpecData.Matching(matching, epsilon));
  }

  private void specification(SnapshotMetadata metadata) {
    when(repository.findGradingSpec(id))
        .thenReturn(
            Optional.of(
                new GradingReadSnapshot(
                    data(130, "EPSILON", new BigDecimal("0.000000000001")),
                    List.of(new TestcaseDescriptor(testcaseId, "HIDDEN", 1)),
                    metadata)));
  }

  @Test
  void preservesRawUnitsNullQuotaVersionPrecisionAndMetadataOnly() {
    specification(new SnapshotMetadata(id, 7, key, 1, "a".repeat(64)));
    when(storage.head(key)).thenReturn(123L);
    var result = handler.loadGradingSpec(id).orElseThrow();
    assertThat(result.snapshotReady()).isTrue();
    assertThat(result.data().versions().specVersion()).isEqualTo(130);
    assertThat(result.data().versions().currentTestcaseSetVersion()).isEqualTo(7);
    assertThat(result.data().limits().timeLimitMs()).isEqualTo(1200);
    assertThat(result.data().limits().memoryLimitMb()).isEqualTo(256);
    assertThat(result.data().limits().maxOutputSizeKb()).isEqualTo(64);
    assertThat(result.data().limits().maxSubmissionsPerHour()).isNull();
    assertThat(result.data().matching().epsilonValue()).isEqualByComparingTo("0.000000000001");
    assertThat(result.testcases()).containsExactly(new TestcaseDescriptor(testcaseId, "HIDDEN", 1));
  }

  @Test
  void absentProblemAndMissingSnapshotMetadataDoNotReadStorage() {
    when(repository.findGradingSpec(id)).thenReturn(Optional.empty());
    assertThat(handler.loadGradingSpec(id)).isEmpty();
    specification(null);
    assertThat(handler.loadGradingSpec(id).orElseThrow().snapshotReady()).isFalse();
    verifyNoInteractions(storage);
  }

  @Test
  void missingObjectMeansNotReadyButTransportFailureIsNotAFalseReadinessAnswer() {
    specification(new SnapshotMetadata(id, 7, key, 1, "a".repeat(64)));
    when(storage.head(key))
        .thenThrow(new TestcaseStorageUnavailableException(true))
        .thenThrow(new TestcaseStorageUnavailableException());
    assertThat(handler.loadGradingSpec(id).orElseThrow().snapshotReady()).isFalse();
    failure(() -> handler.loadGradingSpec(id), GradingDataException.Kind.INFRASTRUCTURE);
  }

  @Test
  void zeroByteObjectIsNotReady() {
    specification(new SnapshotMetadata(id, 7, key, 1, "a".repeat(64)));
    when(storage.head(key)).thenReturn(0L);
    assertThat(handler.loadGradingSpec(id).orElseThrow().snapshotReady()).isFalse();
  }

  @Test
  void missingOrInvalidSpecificationIsClassifiedAsDataFailure() {
    for (var invalid :
        List.of(
            data(0, "EXACT", null),
            data(1, "OTHER", null),
            data(1, "EPSILON", null),
            data(1, "EPSILON", BigDecimal.ZERO))) {
      when(repository.findGradingSpec(id))
          .thenReturn(Optional.of(new GradingReadSnapshot(invalid, List.of(), null)));
      failure(() -> handler.loadGradingSpec(id), GradingDataException.Kind.SPEC_UNAVAILABLE);
    }
  }

  @Test
  void samplesPreserveInlineEmptyUtf8AndIndependentMinioSides() {
    String inputKey = key + "/input";
    String outputKey = key + "/output";
    when(repository.findSamples(id))
        .thenReturn(
            List.of(
                new StoredSampleTestcase(testcaseId, 1, inline(""), minio(outputKey)),
                new StoredSampleTestcase(
                    UUID.randomUUID(), 2, minio(inputKey), inline("tiếng Việt\r\n"))));
    when(storage.get(inputKey)).thenReturn(new byte[] {0, -1});
    when(storage.get(outputKey)).thenReturn(new byte[] {13, 10});
    var result = handler.loadSampleTestcases(id);
    assertThat(result)
        .hasSize(2)
        .allSatisfy(row -> assertThat(row.visibility()).isEqualTo("SAMPLE"));
    assertThat(result.getFirst().input()).isEmpty();
    assertThat(result.getFirst().expectedOutput()).containsExactly(13, 10);
    assertThat(result.getLast().input()).containsExactly(0, -1);
    assertThat(result.getLast().expectedOutput())
        .containsExactly("tiếng Việt\r\n".getBytes(StandardCharsets.UTF_8));
  }

  @Test
  void invalidStorageBranchAndMissingSampleObjectAreDataFailures() {
    when(repository.findSamples(id))
        .thenReturn(
            List.of(
                new StoredSampleTestcase(
                    testcaseId,
                    1,
                    new StoredSampleTestcase.Content("INLINE", null, null),
                    inline("output"))));
    failure(() -> handler.loadSampleTestcases(id), GradingDataException.Kind.SPEC_UNAVAILABLE);
    when(repository.findSamples(id))
        .thenReturn(List.of(new StoredSampleTestcase(testcaseId, 1, minio(key), inline(""))));
    when(storage.get(key)).thenThrow(new TestcaseStorageUnavailableException(true));
    failure(() -> handler.loadSampleTestcases(id), GradingDataException.Kind.SPEC_UNAVAILABLE);
  }

  @Test
  void pinnedSnapshotUsesOnlyRequestedVersionAndNeverCurrentTestcases() {
    byte[] bytes =
        codec.encode(
            id, 7, List.of(new TestcaseData(testcaseId, "HIDDEN", 1, new byte[0], new byte[] {1})));
    when(repository.findSnapshot(id, 7))
        .thenReturn(Optional.of(new SnapshotMetadata(id, 7, key, 1, codec.sha256(bytes))));
    when(storage.get(key)).thenReturn(bytes);
    var result = handler.loadTestcaseSnapshot(id, 7);
    assertThat(result.version()).isEqualTo(7);
    assertThat(result.testcases().getFirst().expectedOutput()).containsExactly(1);
    verify(repository).findSnapshot(id, 7);
    org.mockito.Mockito.verifyNoMoreInteractions(repository);
  }

  @Test
  void missingPinnedMetadataFailsWithoutTryingCurrentVersion() {
    when(repository.findSnapshot(id, 6)).thenReturn(Optional.empty());
    failure(() -> handler.loadTestcaseSnapshot(id, 6), GradingDataException.Kind.SPEC_UNAVAILABLE);
    verifyNoInteractions(storage);
  }

  @Test
  void snapshotStorageFailureKindsAreRetained() {
    when(repository.findSnapshot(id, 7))
        .thenReturn(Optional.of(new SnapshotMetadata(id, 7, key, 1, "a".repeat(64))));
    when(storage.get(key))
        .thenThrow(new TestcaseStorageUnavailableException(true))
        .thenThrow(new TestcaseStorageUnavailableException());
    failure(() -> handler.loadTestcaseSnapshot(id, 7), GradingDataException.Kind.SPEC_UNAVAILABLE);
    failure(() -> handler.loadTestcaseSnapshot(id, 7), GradingDataException.Kind.INFRASTRUCTURE);
  }

  @Test
  void sqlFailureIsSanitizedForAllThreeReads() {
    when(repository.findGradingSpec(id)).thenThrow(new IllegalStateException("private SQL"));
    when(repository.findSamples(id)).thenThrow(new IllegalStateException("private SQL"));
    when(repository.findSnapshot(id, 7)).thenThrow(new IllegalStateException("private SQL"));
    failure(() -> handler.loadGradingSpec(id), GradingDataException.Kind.INFRASTRUCTURE);
    failure(() -> handler.loadSampleTestcases(id), GradingDataException.Kind.INFRASTRUCTURE);
    failure(() -> handler.loadTestcaseSnapshot(id, 7), GradingDataException.Kind.INFRASTRUCTURE);
  }

  @Test
  void invalidArgumentsDoNotReadAnyRepositoryOrStorage() {
    assertThatThrownBy(() -> handler.loadGradingSpec(null))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> handler.loadSampleTestcases(null))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> handler.loadTestcaseSnapshot(null, 1))
        .isInstanceOf(NullPointerException.class);
    failure(() -> handler.loadTestcaseSnapshot(id, 0), GradingDataException.Kind.SPEC_UNAVAILABLE);
    verifyNoInteractions(repository, storage);
  }

  private static StoredSampleTestcase.Content inline(String value) {
    return new StoredSampleTestcase.Content("INLINE", value, null);
  }

  @Test
  void corruptObjectReferenceIsDataFailureRatherThanRetryableTransportFailure() {
    specification(new SnapshotMetadata(id, 7, key, 1, "a".repeat(64)));
    when(storage.head(key)).thenThrow(new IllegalArgumentException("Invalid testcase object key"));
    failure(() -> handler.loadGradingSpec(id), GradingDataException.Kind.SPEC_UNAVAILABLE);
    when(repository.findSnapshot(id, 7))
        .thenReturn(Optional.of(new SnapshotMetadata(id, 7, key, 1, "a".repeat(64))));
    when(storage.get(key)).thenThrow(new IllegalArgumentException("Invalid testcase object key"));
    failure(() -> handler.loadTestcaseSnapshot(id, 7), GradingDataException.Kind.SPEC_UNAVAILABLE);
  }

  private static StoredSampleTestcase.Content minio(String key) {
    return new StoredSampleTestcase.Content("MINIO", null, key);
  }

  private static void failure(Runnable action, GradingDataException.Kind kind) {
    assertThatThrownBy(action::run)
        .isInstanceOfSatisfying(
            GradingDataException.class,
            failure -> {
              assertThat(failure.kind()).isEqualTo(kind);
              assertThat(failure.getMessage()).isEqualTo("Problem grading data is unavailable");
              assertThat(failure.getCause()).isNull();
              assertThat(failure.getSuppressed()).isEmpty();
            });
  }
}
