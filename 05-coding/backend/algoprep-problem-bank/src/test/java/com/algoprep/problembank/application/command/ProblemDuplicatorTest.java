package com.algoprep.problembank.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.common.domain.ports.out.IdGeneratorPort;
import com.algoprep.common.exception.AlgoPrepException;
import com.algoprep.problembank.application.ports.out.TestcaseObjectStorePort;
import com.algoprep.problembank.domain.exception.TestcaseStorageUnavailableException;
import com.algoprep.problembank.domain.model.AuthoringProblemRecord;
import com.algoprep.problembank.domain.model.AuthoringProblemRecord.AuthoringTestcase;
import com.algoprep.problembank.domain.model.ProblemContentData;
import com.algoprep.problembank.domain.model.ProblemSpecData;
import com.algoprep.problembank.domain.model.StoredProblemSpec;
import com.algoprep.problembank.domain.model.StoredTestcase;
import com.algoprep.problembank.domain.ports.out.ProblemAuthoringReadRepository;
import com.algoprep.problembank.domain.ports.out.ProblemContentRepository;
import com.algoprep.problembank.domain.ports.out.ProblemSpecRepository;
import com.algoprep.problembank.domain.ports.out.TestcaseWriteRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class ProblemDuplicatorTest {
  private final ProblemAuthoringReadRepository reads = mock(ProblemAuthoringReadRepository.class);
  private final ProblemContentRepository content = mock(ProblemContentRepository.class);
  private final ProblemSpecRepository specs = mock(ProblemSpecRepository.class);
  private final TestcaseWriteRepository testcases = mock(TestcaseWriteRepository.class);
  private final TestcaseObjectStorePort objects = mock(TestcaseObjectStorePort.class);
  private final IdGeneratorPort ids = mock(IdGeneratorPort.class);
  private final ProblemDuplicator duplicator =
      new ProblemDuplicator(
          reads, content, specs, testcases, new TestcaseContentStore(objects, ids));
  private final UUID sourceId = UUID.randomUUID();
  private final UUID target = UUID.randomUUID();
  private final UUID author = UUID.randomUUID();

  private static AuthoringTestcase row(
      int position, String storage, String inline, String objectKey) {
    return new AuthoringTestcase(
        UUID.randomUUID(),
        position,
        "SAMPLE",
        "AI",
        position % 2 == 0,
        "EXTREME",
        storage,
        inline,
        objectKey,
        "INLINE",
        "out" + position,
        null);
  }

  private AuthoringProblemRecord record(
      UUID authorId, StoredProblemSpec spec, List<AuthoringTestcase> cases) {
    return new AuthoringProblemRecord(
        sourceId,
        "src",
        "Source",
        "statement",
        "",
        UUID.randomUUID(),
        "PUBLISHED",
        false,
        new ProblemContentData.Limits(1000, 256, 64, null),
        4,
        0,
        null,
        true,
        true,
        authorId,
        null,
        null,
        Instant.parse("2026-11-06T00:00:00Z"),
        null,
        List.of(),
        List.of(),
        null,
        List.of(),
        cases,
        spec);
  }

  @Test
  void aMissingSourceIsNotFound() {
    when(reads.find(sourceId, true)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> duplicator.source(sourceId, author, false))
        .isInstanceOf(AlgoPrepException.class)
        .extracting(e -> ((AlgoPrepException) e).errorCode())
        .isEqualTo("PRB-010");
  }

  @Test
  void anInstructorMayNotCopyAnotherAuthorsProblemButAnAdminMay() {
    var foreign = record(UUID.randomUUID(), null, List.of());
    when(reads.find(sourceId, true)).thenReturn(Optional.of(foreign));

    assertThatThrownBy(() -> duplicator.source(sourceId, author, false))
        .isInstanceOf(AlgoPrepException.class)
        .extracting(e -> ((AlgoPrepException) e).errorCode())
        .isEqualTo("PRB-003");
    assertThat(duplicator.source(sourceId, author, true)).isSameAs(foreign);
  }

  @Test
  void theAuthorGetsTheirOwnSource() {
    var own = record(author, null, List.of());
    when(reads.find(sourceId, true)).thenReturn(Optional.of(own));

    assertThat(duplicator.source(sourceId, author, false)).isSameAs(own);
  }

  @BeforeEach
  void openSynchronization() {
    TransactionSynchronizationManager.initSynchronization();
  }

  @AfterEach
  void closeSynchronization() {
    TransactionSynchronizationManager.clearSynchronization();
  }

  @Test
  void copyMarksTheSourceAndCopiesTheSpecAtVersionOneAndEveryTestcase() {
    var spec =
        new StoredProblemSpec(
            new ProblemSpecData("EXACT", null, "in", "out", "solve", "{}", "[]", null), 7);
    var big = "problems/" + sourceId + "/testcases/" + UUID.randomUUID() + "/input-x.txt";
    var cases = List.of(row(1, "INLINE", "in1", null), row(2, "MINIO", null, big));
    var newKeyId = UUID.randomUUID();
    when(ids.newId()).thenReturn(newKeyId, UUID.randomUUID(), UUID.randomUUID());

    duplicator.copy(record(author, spec, cases), target, author);

    verify(content).markDuplicatedFrom(target, sourceId);
    var saved = ArgumentCaptor.forClass(StoredProblemSpec.class);
    verify(specs).save(eq(target), saved.capture());
    assertThat(saved.getValue().specVersion()).isEqualTo(1);
    assertThat(saved.getValue().data()).isEqualTo(spec.data());
    verify(specs).touch(target, author, false);
    var rows = ArgumentCaptor.forClass(StoredTestcase.class);
    verify(testcases, org.mockito.Mockito.times(2)).insert(eq(target), rows.capture());
    var first = rows.getAllValues().get(0);
    var second = rows.getAllValues().get(1);
    assertThat(first.id()).isNotEqualTo(cases.get(0).id());
    assertThat(first.input().inline()).isEqualTo("in1");
    assertThat(first.visibility()).isEqualTo("SAMPLE");
    assertThat(first.origin()).isEqualTo("AI");
    assertThat(first.approved()).isFalse();
    assertThat(first.category()).isEqualTo("EXTREME");
    assertThat(second.position()).isEqualTo(2);
    assertThat(second.input().storage()).isEqualTo("MINIO");
    assertThat(second.input().objectKey())
        .startsWith("problems/" + target + "/testcases/" + second.id() + "/input-")
        .isNotEqualTo(big);
    verify(objects).copy(eq(big), eq(second.input().objectKey()));
    // a rollback removes the object that was copied, a commit keeps it
    TransactionSynchronizationManager.getSynchronizations()
        .forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_COMMITTED));
    verify(objects, never()).delete(any());
    TransactionSynchronizationManager.getSynchronizations()
        .forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
    verify(objects).delete(second.input().objectKey());
  }

  @Test
  void aSourceWithoutSpecOrTestcasesCopiesNothingButTheLink() {
    duplicator.copy(record(author, null, List.of()), target, author);

    verify(content).markDuplicatedFrom(target, sourceId);
    verify(specs, never()).save(any(), any());
    verify(specs, never()).touch(any(), any(), org.mockito.ArgumentMatchers.anyBoolean());
    verify(testcases, never()).insert(any(), any());
  }

  @Test
  void anObjectCopyFailureIsStorageUnavailable() {
    var key = "problems/" + sourceId + "/testcases/" + UUID.randomUUID() + "/input-x.txt";
    when(ids.newId()).thenReturn(UUID.randomUUID());
    doThrow(new IllegalStateException("down")).when(objects).copy(any(), any());
    var source = record(author, null, List.of(row(1, "MINIO", null, key)));

    assertThatThrownBy(() -> duplicator.copy(source, target, author))
        .isInstanceOf(TestcaseStorageUnavailableException.class);
    verify(testcases, never()).insert(any(), any());
  }
}
