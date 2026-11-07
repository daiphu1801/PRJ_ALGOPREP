package com.algoprep.problembank.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.algoprep.common.exception.AlgoPrepException;
import com.algoprep.problembank.application.dto.ProblemAuthoringQuery;
import com.algoprep.problembank.domain.model.AuthoringProblemRecord;
import com.algoprep.problembank.domain.model.AuthoringProblemRecord.AuthoringTestcase;
import com.algoprep.problembank.domain.model.AuthoringProblemRecord.Tag;
import com.algoprep.problembank.domain.model.ProblemContentData;
import com.algoprep.problembank.domain.model.ProblemSpecData;
import com.algoprep.problembank.domain.model.StoredProblemSpec;
import com.algoprep.problembank.domain.ports.out.ProblemAuthoringReadRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GetDuplicateSourceQueryHandlerTest {
  private final ProblemAuthoringReadRepository repository =
      mock(ProblemAuthoringReadRepository.class);
  private final GetDuplicateSourceQueryHandler handler =
      new GetDuplicateSourceQueryHandler(repository);
  private final UUID id = UUID.randomUUID();
  private final UUID author = UUID.randomUUID();

  private AuthoringProblemRecord record(UUID authorId, StoredProblemSpec spec, int cases) {
    var rows =
        java.util.stream.IntStream.rangeClosed(1, cases)
            .mapToObj(
                i ->
                    new AuthoringTestcase(
                        UUID.randomUUID(),
                        i,
                        "HIDDEN",
                        "MANUAL",
                        true,
                        null,
                        "INLINE",
                        "i",
                        null,
                        "INLINE",
                        "o",
                        null))
            .toList();
    return new AuthoringProblemRecord(
        id,
        "two-sum",
        "Two Sum",
        "statement",
        "limits",
        UUID.randomUUID(),
        "PUBLISHED",
        true,
        new ProblemContentData.Limits(1000, 256, 64, 30),
        3,
        0,
        null,
        true,
        true,
        authorId,
        null,
        null,
        Instant.EPOCH,
        null,
        List.of(UUID.randomUUID()),
        List.of(new Tag(UUID.randomUUID(), "array"), new Tag(UUID.randomUUID(), "hash")),
        new ProblemContentData.Solution("PYTHON", "print(1)"),
        List.of(new ProblemContentData.Example(UUID.randomUUID(), 1, "1", "2", "why")),
        rows,
        spec);
  }

  private ProblemAuthoringQuery query(UUID actor, boolean admin) {
    return new ProblemAuthoringQuery(id, actor, admin, false);
  }

  @Test
  void returnsThePrefillWithoutIdsStatusTestcasesOrSpecAndCountsWhatWillBeCopied() {
    var spec =
        new StoredProblemSpec(
            new ProblemSpecData("EXACT", null, "in", "out", null, null, null, null), 2);
    when(repository.find(id, false)).thenReturn(Optional.of(record(author, spec, 3)));

    var view = handler.get(query(author, false));

    assertThat(view.title()).isEqualTo("Two Sum");
    assertThat(view.constraintsMd()).isEqualTo("limits");
    assertThat(view.limits().maxSubmissionsPerHour()).isEqualTo(30);
    assertThat(view.tags()).containsExactly("array", "hash");
    assertThat(view.referenceSolution().sourceCode()).isEqualTo("print(1)");
    assertThat(view.workedExamples()).hasSize(1);
    assertThat(view.workedExamples().get(0).id()).isNull();
    assertThat(view.workedExamples().get(0).explanationMd()).isEqualTo("why");
    assertThat(view.willCopyOnSave().testcaseCount()).isEqualTo(3);
    assertThat(view.willCopyOnSave().hasSpec()).isTrue();
    assertThat(view.duplicatedFromProblemId()).isEqualTo(id);
  }

  @Test
  void aSourceWithoutSpecOrAnswerReportsNoSpecAndNoTestcases() {
    when(repository.find(id, false)).thenReturn(Optional.of(record(author, null, 0)));

    var view = handler.get(query(author, false));

    assertThat(view.willCopyOnSave().hasSpec()).isFalse();
    assertThat(view.willCopyOnSave().testcaseCount()).isZero();
  }

  @Test
  void anAdminMayReadAnotherAuthorsProblem() {
    when(repository.find(id, false)).thenReturn(Optional.of(record(UUID.randomUUID(), null, 1)));

    assertThat(handler.get(query(author, true)).willCopyOnSave().testcaseCount()).isEqualTo(1);
  }

  @Test
  void anInstructorWithAnotherAuthorsProblemIsForbidden() {
    when(repository.find(id, false)).thenReturn(Optional.of(record(UUID.randomUUID(), null, 0)));

    assertThatThrownBy(() -> handler.get(query(author, false)))
        .isInstanceOf(AlgoPrepException.class)
        .extracting(e -> ((AlgoPrepException) e).errorCode())
        .isEqualTo("PRB-003");
  }

  @Test
  void aMissingOrDeletedSourceIsNotFound() {
    when(repository.find(id, false)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> handler.get(query(author, true)))
        .isInstanceOf(AlgoPrepException.class)
        .extracting(e -> ((AlgoPrepException) e).errorCode())
        .isEqualTo("PRB-010");
  }
}
