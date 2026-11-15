package com.algoprep.problembank.application.command;

import com.algoprep.common.domain.ports.out.IdGeneratorPort;
import com.algoprep.common.exception.ResourceNotFoundException;
import com.algoprep.problembank.application.dto.SnapshotPublicationResult;
import com.algoprep.problembank.application.ports.in.PublishTestcaseSnapshotUseCase;
import com.algoprep.problembank.application.ports.in.ReadTestcaseSnapshotUseCase;
import com.algoprep.problembank.application.ports.out.TestcaseObjectStorePort;
import com.algoprep.problembank.application.snapshot.TestcaseSnapshotCodec;
import com.algoprep.problembank.domain.exception.TestcaseStorageUnavailableException;
import com.algoprep.problembank.domain.model.SnapshotMetadata;
import com.algoprep.problembank.domain.model.StoredGradingTestcase;
import com.algoprep.problembank.domain.model.TestcaseData;
import com.algoprep.problembank.domain.ports.out.SnapshotPublicationRepository;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/** Publication kernel only. The caller owns authoring permissions, validation, status and audit. */
@Service
class PublishTestcaseSnapshotCommandHandler implements PublishTestcaseSnapshotUseCase {
  private final SnapshotPublicationRepository repository;
  private final TestcaseObjectStorePort storage;
  private final TestcaseSnapshotCodec codec;
  private final ReadTestcaseSnapshotUseCase snapshots;
  private final IdGeneratorPort ids;

  PublishTestcaseSnapshotCommandHandler(
      SnapshotPublicationRepository repository,
      TestcaseObjectStorePort storage,
      TestcaseSnapshotCodec codec,
      ReadTestcaseSnapshotUseCase snapshots,
      IdGeneratorPort ids) {
    this.repository = repository;
    this.storage = storage;
    this.codec = codec;
    this.snapshots = snapshots;
    this.ids = ids;
  }

  @Override
  @Transactional
  public SnapshotPublicationResult publishSnapshot(UUID problemId) {
    Objects.requireNonNull(problemId);
    try {
      var state =
          repository
              .lockProblem(problemId)
              .orElseThrow(() -> new ResourceNotFoundException("PRB-010", "Problem not found"));
      var current = repository.findApproved(problemId).stream().map(this::testcase).toList();
      if (state.snapshotExists()
          && sameContent(
              snapshots.loadTestcaseSnapshot(problemId, state.version()).testcases(), current)) {
        return new SnapshotPublicationResult(
            problemId, state.version(), current.size(), false, false);
      }
      int version = state.snapshotExists() ? Math.addExact(state.version(), 1) : state.version();
      writeSnapshot(problemId, version, current);
      if (state.snapshotExists()) {
        Instant bumpedAt = repository.bumpVersion(problemId, state.version(), version);
        repository.appendBumpEvent(problemId, event(problemId, version, bumpedAt));
      }
      return new SnapshotPublicationResult(
          problemId, version, current.size(), true, state.snapshotExists());
    } catch (ResourceNotFoundException missing) {
      throw missing;
    } catch (RuntimeException failure) {
      throw new TestcaseStorageUnavailableException();
    }
  }

  private void writeSnapshot(UUID problemId, int version, List<TestcaseData> current) {
    byte[] bytes = codec.encode(problemId, version, current);
    String key =
        "problems/" + problemId + "/snapshots/" + version + "/" + ids.newId().toString() + ".json";
    var metadata =
        new SnapshotMetadata(problemId, version, key, current.size(), codec.sha256(bytes));
    storage.put(key, bytes);
    verifyObject(metadata, bytes.length);
    repository.insertSnapshot(metadata);
  }

  private void verifyObject(SnapshotMetadata metadata, int length) {
    if (storage.head(metadata.objectKey()) != length) {
      throw new TestcaseStorageUnavailableException();
    }
    codec.decode(metadata, storage.get(metadata.objectKey()));
  }

  private TestcaseData testcase(StoredGradingTestcase row) {
    var descriptor = row.descriptor();
    return new TestcaseData(
        descriptor.testcaseId(),
        descriptor.visibility(),
        descriptor.position(),
        content(row.input()),
        content(row.expectedOutput()));
  }

  private byte[] content(StoredGradingTestcase.Content value) {
    if ("INLINE".equals(value.storage()) && value.inline() != null && value.objectKey() == null) {
      return value.inline().getBytes(StandardCharsets.UTF_8);
    }
    if ("MINIO".equals(value.storage()) && value.inline() == null && value.objectKey() != null) {
      return storage.get(value.objectKey());
    }
    throw new TestcaseStorageUnavailableException();
  }

  private static boolean sameContent(List<TestcaseData> left, List<TestcaseData> right) {
    if (left.size() != right.size()) {
      return false;
    }
    Comparator<TestcaseData> order =
        Comparator.comparingInt(TestcaseData::position)
            .thenComparing(row -> row.testcaseId().toString());
    var before = left.stream().sorted(order).toList();
    var after = right.stream().sorted(order).toList();
    for (int i = 0; i < before.size(); i++) {
      var a = before.get(i);
      var b = after.get(i);
      if (!a.testcaseId().equals(b.testcaseId())
          || !a.visibility().equals(b.visibility())
          || a.position() != b.position()
          || !Arrays.equals(a.input(), b.input())
          || !Arrays.equals(a.expectedOutput(), b.expectedOutput())) {
        return false;
      }
    }
    return true;
  }

  private String event(UUID problemId, int version, Instant bumpedAt) {
    var json = JsonMapper.builder().build();
    var payload =
        json.createObjectNode()
            .put("problemId", problemId.toString())
            .put("testcaseSetVersion", version)
            .put("bumpedAt", bumpedAt.toString());
    return json.createObjectNode()
        .put("eventId", ids.newId().toString())
        .put("eventType", "TestcaseSetVersionBumped")
        .put("occurredAt", bumpedAt.toString())
        .put("schemaVersion", 1)
        .set("payload", payload)
        .toString();
  }
}
