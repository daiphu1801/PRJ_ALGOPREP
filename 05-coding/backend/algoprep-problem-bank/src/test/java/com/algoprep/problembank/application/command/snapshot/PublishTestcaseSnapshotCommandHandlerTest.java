package com.algoprep.problembank.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.algoprep.common.exception.ResourceNotFoundException;
import com.algoprep.problembank.domain.exception.GradingDataException;
import com.algoprep.problembank.domain.exception.TestcaseStorageUnavailableException;
import com.algoprep.problembank.domain.model.SnapshotMetadata;
import com.algoprep.problembank.domain.model.SnapshotPublicationState;
import com.algoprep.problembank.domain.model.StoredGradingTestcase;
import com.algoprep.problembank.domain.model.TestcaseSnapshot;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.json.JsonMapper;

class PublishTestcaseSnapshotCommandHandlerTest {
  private final SnapshotPublicationFixture f = new SnapshotPublicationFixture();

  @Test
  void firstPublicationConfirmsObjectBeforeMetadataAndUsesTheCurrentVersion() {
    when(f.repository.lockProblem(f.id))
        .thenReturn(Optional.of(new SnapshotPublicationState(7, false)));
    var result = f.handler().publishSnapshot(f.id);
    assertThat(result.problemId()).isEqualTo(f.id);
    assertThat(result.version()).isEqualTo(7);
    assertThat(result.created()).isTrue();
    assertThat(result.versionBumped()).isFalse();
    assertThat(result.testcaseCount()).isEqualTo(1);
    var metadata = ArgumentCaptor.forClass(SnapshotMetadata.class);
    var order = inOrder(f.repository, f.storage);
    order.verify(f.repository).lockProblem(f.id);
    order.verify(f.repository).findApproved(f.id);
    order.verify(f.storage).put(anyString(), any(byte[].class));
    order.verify(f.storage).head(anyString());
    order.verify(f.storage).get(anyString());
    order.verify(f.repository).insertSnapshot(metadata.capture());
    assertThat(metadata.getValue().objectKey())
        .startsWith("problems/" + f.id + "/snapshots/7/")
        .endsWith(".json");
    var bytes = f.objects.get(metadata.getValue().objectKey());
    assertThat(metadata.getValue().contentSha256()).isEqualTo(f.codec.sha256(bytes));
    assertThat(f.codec.decode(metadata.getValue(), bytes).testcases().getFirst().input())
        .containsExactly(SnapshotPublicationFixture.data(f.original).input());
    verify(f.repository, never()).appendBumpEvent(any(), anyString());
    verify(f.repository, never()).bumpVersion(any(), anyInt(), anyInt());
  }

  @Test
  void identicalLogicalContentReusesTheSnapshotWithoutWritingOrBumping() {
    f.existing();
    var result = f.handler().publishSnapshot(f.id);
    assertThat(result.version()).isEqualTo(1);
    assertThat(result.created()).isFalse();
    assertThat(result.versionBumped()).isFalse();
    verifyNoInteractions(f.storage);
    verify(f.repository, never()).insertSnapshot(any());
    verify(f.repository, never()).appendBumpEvent(any(), anyString());
  }

  @ParameterizedTest
  @ValueSource(strings = {"ID", "VISIBILITY", "POSITION", "INPUT", "OUTPUT", "COUNT"})
  void everyGradingRelevantChangeCreatesExactlyOneVersionAndDurableEvent(String change) {
    f.existing();
    var changed =
        SnapshotPublicationFixture.row(
            change.equals("ID") ? UUID.randomUUID() : f.testcaseId,
            change.equals("VISIBILITY") ? "SAMPLE" : "HIDDEN",
            change.equals("POSITION") ? 2 : 1,
            change.equals("INPUT") ? "different input" : "input\r\n",
            change.equals("OUTPUT") ? "different output" : "output\n");
    when(f.repository.findApproved(f.id))
        .thenReturn(
            change.equals("COUNT")
                ? List.of(
                    changed, SnapshotPublicationFixture.row(UUID.randomUUID(), "SAMPLE", 2, "", ""))
                : List.of(changed));
    var result = f.handler().publishSnapshot(f.id);
    assertThat(result.version()).isEqualTo(2);
    assertThat(result.versionBumped()).isTrue();
    var event = ArgumentCaptor.forClass(String.class);
    verify(f.repository).bumpVersion(f.id, 1, 2);
    verify(f.repository).appendBumpEvent(eq(f.id), event.capture());
    var json = JsonMapper.builder().build().readTree(event.getValue());
    assertThat(json.get("eventType").asText()).isEqualTo("TestcaseSetVersionBumped");
    assertThat(json.get("schemaVersion").asInt()).isEqualTo(1);
    assertThat(UUID.fromString(json.get("eventId").asText())).isNotNull();
    assertThat(json.get("occurredAt").asText()).isEqualTo(f.bumpedAt.toString());
    assertThat(json.get("payload").get("testcaseSetVersion").asInt()).isEqualTo(2);
    assertThat(json.get("payload").get("bumpedAt").asText()).isEqualTo(f.bumpedAt.toString());
  }

  @Test
  void minioBytesAndEmptyInlineSidesBecomeIndependentSnapshotBytes() {
    String sourceKey = "problems/" + f.id + "/testcases/input";
    f.objects.put(sourceKey, new byte[] {0, -1, 13, 10});
    when(f.repository.findApproved(f.id))
        .thenReturn(
            List.of(
                new StoredGradingTestcase(
                    f.original.descriptor(),
                    new StoredGradingTestcase.Content("MINIO", null, sourceKey),
                    new StoredGradingTestcase.Content("INLINE", "", null))));
    f.handler().publishSnapshot(f.id);
    var captured = ArgumentCaptor.forClass(SnapshotMetadata.class);
    verify(f.repository).insertSnapshot(captured.capture());
    f.objects.put(sourceKey, new byte[] {99});
    var result =
        f.codec.decode(captured.getValue(), f.objects.get(captured.getValue().objectKey()));
    assertThat(result.testcases().getFirst().input()).containsExactly(0, -1, 13, 10);
    assertThat(result.testcases().getFirst().expectedOutput()).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {"LOCK", "READ", "PUT", "HEAD", "GET", "INSERT", "BUMP", "OUTBOX", "OLD_SNAPSHOT"})
  void failuresAreSanitizedForTheTransactionalBoundary(String phase) {
    if (List.of("BUMP", "OUTBOX", "OLD_SNAPSHOT").contains(phase)) {
      f.existing();
      when(f.repository.findApproved(f.id))
          .thenReturn(
              List.of(
                  SnapshotPublicationFixture.row(
                      f.testcaseId, "HIDDEN", 2, "input\r\n", "output\n")));
    }
    var fault = new IllegalStateException("private SQL, object key, hidden bytes");
    switch (phase) {
      case "LOCK" -> when(f.repository.lockProblem(f.id)).thenThrow(fault);
      case "READ" -> when(f.repository.findApproved(f.id)).thenThrow(fault);
      case "PUT" -> doThrow(fault).when(f.storage).put(anyString(), any(byte[].class));
      case "HEAD" -> doReturn(-1L).when(f.storage).head(anyString());
      case "GET" -> doReturn(new byte[] {42}).when(f.storage).get(anyString());
      case "INSERT" -> doThrow(fault).when(f.repository).insertSnapshot(any());
      case "BUMP" -> when(f.repository.bumpVersion(f.id, 1, 2)).thenThrow(fault);
      case "OUTBOX" -> doThrow(fault).when(f.repository).appendBumpEvent(eq(f.id), anyString());
      default ->
          when(f.snapshots.loadTestcaseSnapshot(f.id, 1))
              .thenThrow(new GradingDataException(GradingDataException.Kind.SPEC_UNAVAILABLE));
    }
    unavailable(() -> f.handler().publishSnapshot(f.id));
  }

  @ParameterizedTest
  @ValueSource(strings = {"UNKNOWN", "BOTH", "NEITHER"})
  void invalidStorageBranchesCannotBePublished(String branch) {
    var input =
        new StoredGradingTestcase.Content(
            branch.equals("UNKNOWN") ? "OTHER" : "MINIO",
            branch.equals("BOTH") ? "unexpected" : null,
            branch.equals("NEITHER") ? null : "problems/key");
    when(f.repository.findApproved(f.id))
        .thenReturn(
            List.of(
                new StoredGradingTestcase(
                    f.original.descriptor(), input, f.original.expectedOutput())));
    unavailable(() -> f.handler().publishSnapshot(f.id));
    verify(f.repository, never()).insertSnapshot(any());
  }

  @Test
  void missingOrDeletedProblemDoesNotReadContentOrStorage() {
    when(f.repository.lockProblem(f.id)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> f.handler().publishSnapshot(f.id))
        .isInstanceOfSatisfying(
            ResourceNotFoundException.class,
            failure -> assertThat(failure.errorCode()).isEqualTo("PRB-010"));
    verify(f.repository, never()).findApproved(any());
    verifyNoInteractions(f.storage, f.snapshots);
  }

  @Test
  void nullArgumentDoesNotReachTheRepository() {
    assertThatThrownBy(() -> f.handler().publishSnapshot(null))
        .isInstanceOf(NullPointerException.class);
    verifyNoInteractions(f.repository, f.storage);
  }

  @Test
  void versionOverflowCannotWriteAnObjectOrWrapTheVersion() {
    when(f.repository.lockProblem(f.id))
        .thenReturn(Optional.of(new SnapshotPublicationState(Integer.MAX_VALUE, true)));
    when(f.snapshots.loadTestcaseSnapshot(f.id, Integer.MAX_VALUE))
        .thenReturn(
            new TestcaseSnapshot(
                f.id, Integer.MAX_VALUE, List.of(SnapshotPublicationFixture.data(f.original))));
    when(f.repository.findApproved(f.id))
        .thenReturn(
            List.of(
                SnapshotPublicationFixture.row(
                    f.testcaseId, "HIDDEN", 1, "different input", "output\n")));
    unavailable(() -> f.handler().publishSnapshot(f.id));
    verifyNoInteractions(f.storage);
    verify(f.repository, never()).bumpVersion(any(), anyInt(), anyInt());
  }

  @Test
  void emptyApprovedSetCannotProduceAnEmptyPublishedBundle() {
    when(f.repository.findApproved(f.id)).thenReturn(List.of());
    unavailable(() -> f.handler().publishSnapshot(f.id));
    verifyNoInteractions(f.storage);
    verify(f.repository, never()).insertSnapshot(any());
  }

  static void unavailable(Runnable action) {
    assertThatThrownBy(action::run)
        .isInstanceOfSatisfying(
            TestcaseStorageUnavailableException.class,
            failure -> {
              assertThat(failure.errorCode()).isEqualTo("PRB-052");
              assertThat(failure.getMessage()).isEqualTo("Testcase storage is unavailable");
              assertThat(failure.getCause()).isNull();
              assertThat(failure.getSuppressed()).isEmpty();
            });
  }
}
