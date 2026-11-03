package com.algoprep.problembank.application.query;

import com.algoprep.common.functionspec.MatchingStrategy;
import com.algoprep.problembank.application.ports.in.ReadProblemGradingSpecUseCase;
import com.algoprep.problembank.application.ports.in.ReadSampleTestcasesUseCase;
import com.algoprep.problembank.application.ports.in.ReadTestcaseSnapshotUseCase;
import com.algoprep.problembank.application.ports.out.TestcaseObjectStorePort;
import com.algoprep.problembank.application.snapshot.TestcaseSnapshotCodec;
import com.algoprep.problembank.domain.exception.GradingDataException;
import com.algoprep.problembank.domain.exception.TestcaseStorageUnavailableException;
import com.algoprep.problembank.domain.model.GradingReadSnapshot;
import com.algoprep.problembank.domain.model.GradingSpecification;
import com.algoprep.problembank.domain.model.SnapshotMetadata;
import com.algoprep.problembank.domain.model.StoredSampleTestcase;
import com.algoprep.problembank.domain.model.TestcaseData;
import com.algoprep.problembank.domain.model.TestcaseSnapshot;
import com.algoprep.problembank.domain.ports.out.GradingReadRepository;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class ProblemGradingReadQueryHandler
    implements ReadProblemGradingSpecUseCase,
        ReadSampleTestcasesUseCase,
        ReadTestcaseSnapshotUseCase {
  private final GradingReadRepository repository;
  private final TestcaseObjectStorePort storage;
  private final TestcaseSnapshotCodec codec;

  ProblemGradingReadQueryHandler(
      GradingReadRepository repository,
      TestcaseObjectStorePort storage,
      TestcaseSnapshotCodec codec) {
    this.repository = repository;
    this.storage = storage;
    this.codec = codec;
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<GradingSpecification> loadGradingSpec(UUID problemId) {
    Objects.requireNonNull(problemId);
    try {
      return repository.findGradingSpec(problemId).map(this::specification);
    } catch (RuntimeException failure) {
      throw classify(failure);
    }
  }

  private GradingSpecification specification(GradingReadSnapshot row) {
    var data = row.data();
    if (data.versions().specVersion() < 1 || data.versions().currentTestcaseSetVersion() < 1) {
      throw unavailable();
    }
    try {
      MatchingStrategy.valueOf(data.matching().matchingStrategy());
    } catch (RuntimeException failure) {
      throw unavailable();
    }
    if ("EPSILON".equals(data.matching().matchingStrategy())
        && (data.matching().epsilonValue() == null
            || data.matching().epsilonValue().signum() <= 0)) {
      throw unavailable();
    }
    boolean ready = false;
    if (row.snapshot() != null) {
      try {
        ready = storage.head(row.snapshot().objectKey()) > 0;
      } catch (TestcaseStorageUnavailableException failure) {
        if (!failure.isObjectMissing()) {
          throw failure;
        }
      }
    }
    return new GradingSpecification(data, row.testcases(), ready);
  }

  @Override
  @Transactional(readOnly = true)
  public List<TestcaseData> loadSampleTestcases(UUID problemId) {
    Objects.requireNonNull(problemId);
    try {
      return repository.findSamples(problemId).stream()
          .map(
              row ->
                  new TestcaseData(
                      row.testcaseId(),
                      "SAMPLE",
                      row.position(),
                      content(row.input()),
                      content(row.expectedOutput())))
          .toList();
    } catch (RuntimeException failure) {
      throw classify(failure);
    }
  }

  private byte[] content(StoredSampleTestcase.Content value) {
    if ("INLINE".equals(value.storage()) && value.inline() != null && value.objectKey() == null) {
      return value.inline().getBytes(StandardCharsets.UTF_8);
    }
    if ("MINIO".equals(value.storage()) && value.inline() == null && value.objectKey() != null) {
      return storage.get(value.objectKey());
    }
    throw unavailable();
  }

  @Override
  @Transactional(readOnly = true)
  public TestcaseSnapshot loadTestcaseSnapshot(UUID problemId, int version) {
    Objects.requireNonNull(problemId);
    if (version < 1) {
      throw unavailable();
    }
    try {
      SnapshotMetadata metadata =
          repository
              .findSnapshot(problemId, version)
              .orElseThrow(ProblemGradingReadQueryHandler::unavailable);
      return codec.decode(metadata, storage.get(metadata.objectKey()));
    } catch (RuntimeException failure) {
      throw classify(failure);
    }
  }

  private static GradingDataException classify(RuntimeException failure) {
    if (failure instanceof GradingDataException known) {
      return known;
    }
    return new GradingDataException(
        failure instanceof IllegalArgumentException
                || failure instanceof TestcaseStorageUnavailableException storage
                    && storage.isObjectMissing()
            ? GradingDataException.Kind.SPEC_UNAVAILABLE
            : GradingDataException.Kind.INFRASTRUCTURE);
  }

  private static GradingDataException unavailable() {
    return new GradingDataException(GradingDataException.Kind.SPEC_UNAVAILABLE);
  }
}
