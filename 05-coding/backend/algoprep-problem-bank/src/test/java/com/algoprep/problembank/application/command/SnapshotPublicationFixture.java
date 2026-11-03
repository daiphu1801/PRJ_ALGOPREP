package com.algoprep.problembank.application.command;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.algoprep.common.domain.ports.out.IdGeneratorPort;
import com.algoprep.problembank.application.ports.in.ReadTestcaseSnapshotUseCase;
import com.algoprep.problembank.application.ports.out.TestcaseObjectStorePort;
import com.algoprep.problembank.application.snapshot.TestcaseSnapshotCodec;
import com.algoprep.problembank.domain.model.SnapshotPublicationState;
import com.algoprep.problembank.domain.model.StoredGradingTestcase;
import com.algoprep.problembank.domain.model.TestcaseData;
import com.algoprep.problembank.domain.model.TestcaseDescriptor;
import com.algoprep.problembank.domain.model.TestcaseSnapshot;
import com.algoprep.problembank.domain.ports.out.SnapshotPublicationRepository;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** In-memory storage is a unit fixture only; component tests use PostgreSQL and MinIO. */
final class SnapshotPublicationFixture {
  final UUID id = UUID.randomUUID();
  final UUID testcaseId = UUID.randomUUID();
  final SnapshotPublicationRepository repository = mock(SnapshotPublicationRepository.class);
  final TestcaseObjectStorePort storage = mock(TestcaseObjectStorePort.class);
  final ReadTestcaseSnapshotUseCase snapshots = mock(ReadTestcaseSnapshotUseCase.class);
  final IdGeneratorPort ids = mock(IdGeneratorPort.class);
  final TestcaseSnapshotCodec codec = new TestcaseSnapshotCodec();
  final Map<String, byte[]> objects = new HashMap<>();
  final Instant bumpedAt = Instant.parse("2026-11-03T00:00:00.123456Z");
  final StoredGradingTestcase original = row(testcaseId, "HIDDEN", 1, "input\r\n", "output\n");

  SnapshotPublicationFixture() {
    when(repository.lockProblem(id))
        .thenReturn(Optional.of(new SnapshotPublicationState(1, false)));
    when(repository.findApproved(id)).thenReturn(List.of(original));
    when(repository.bumpVersion(eq(id), anyInt(), anyInt())).thenReturn(bumpedAt);
    when(ids.newId()).thenAnswer(call -> UUID.randomUUID());
    doAnswer(
            call -> {
              objects.put(call.getArgument(0), ((byte[]) call.getArgument(1)).clone());
              return null;
            })
        .when(storage)
        .put(anyString(), any(byte[].class));
    when(storage.head(anyString()))
        .thenAnswer(call -> (long) objects.get(call.getArgument(0)).length);
    when(storage.get(anyString())).thenAnswer(call -> objects.get(call.getArgument(0)).clone());
  }

  PublishTestcaseSnapshotCommandHandler handler() {
    return new PublishTestcaseSnapshotCommandHandler(repository, storage, codec, snapshots, ids);
  }

  void existing() {
    when(repository.lockProblem(id)).thenReturn(Optional.of(new SnapshotPublicationState(1, true)));
    when(snapshots.loadTestcaseSnapshot(id, 1))
        .thenReturn(new TestcaseSnapshot(id, 1, List.of(data(original))));
  }

  static StoredGradingTestcase row(
      UUID id, String visibility, int position, String input, String output) {
    return new StoredGradingTestcase(
        new TestcaseDescriptor(id, visibility, position),
        new StoredGradingTestcase.Content("INLINE", input, null),
        new StoredGradingTestcase.Content("INLINE", output, null));
  }

  static TestcaseData data(StoredGradingTestcase row) {
    var descriptor = row.descriptor();
    return new TestcaseData(
        descriptor.testcaseId(),
        descriptor.visibility(),
        descriptor.position(),
        row.input().inline().getBytes(StandardCharsets.UTF_8),
        row.expectedOutput().inline().getBytes(StandardCharsets.UTF_8));
  }
}
