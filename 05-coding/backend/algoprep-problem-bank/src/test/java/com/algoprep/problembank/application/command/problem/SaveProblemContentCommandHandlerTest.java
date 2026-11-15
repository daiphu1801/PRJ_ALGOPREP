package com.algoprep.problembank.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.common.exception.AlgoPrepException;
import com.algoprep.problembank.application.dto.SaveProblemContentCommand;
import com.algoprep.problembank.application.ports.out.ProblemListVersionPort;
import com.algoprep.problembank.domain.model.ProblemContentData;
import com.algoprep.problembank.domain.model.ProblemContentSnapshot;
import com.algoprep.problembank.domain.ports.out.ProblemContentRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.env.MockEnvironment;

class SaveProblemContentCommandHandlerTest {
  private final ProblemContentRepository repository = mock(ProblemContentRepository.class);
  private final ProblemListVersionPort cache = mock(ProblemListVersionPort.class);
  private final SaveProblemContentCommandHandler handler =
      new SaveProblemContentCommandHandler(
          repository, new ProblemContentValidation(new MockEnvironment()), cache);
  private final UUID id = UUID.randomUUID();
  private final UUID actor = UUID.randomUUID();
  private final Instant time = Instant.parse("2026-11-02T00:00:00Z");

  @BeforeEach
  void setup() {
    when(repository.lock(id))
        .thenReturn(Optional.of(snapshot(actor, "UNPUBLISHED", ContentTestData.valid())));
    when(repository.cataloguesExist(any(), any())).thenReturn(true);
    when(repository.create(eq(actor), any())).thenReturn(id);
    when(repository.firstLevel()).thenReturn(Optional.of(ContentTestData.LEVEL));
    when(repository.write(eq(id), eq(actor), any()))
        .thenAnswer(call -> snapshot(actor, "UNPUBLISHED", call.getArgument(2)));
  }

  private ProblemContentSnapshot snapshot(UUID author, String status, ProblemContentData data) {
    return new ProblemContentSnapshot(id, "STABLE", author, status, data, time);
  }

  private SaveProblemContentCommand command(UUID problem, ProblemContentData data) {
    return new SaveProblemContentCommand(problem, actor, false, true, time, data);
  }

  @Test
  void createsOrdinaryDraftWithExplicitLimitsFirstLevelAndNullQuota() {
    var result = handler.save(command(null, ContentTestData.replace("levelId", null)));
    assertThat(result.content().levelId()).isEqualTo(ContentTestData.LEVEL);
    assertThat(result.content().limits().maxSubmissionsPerHour()).isNull();
    assertThat(result.content().title()).isEqualTo("Bài cộng");
    verify(cache, never()).incrementAfterCommit();
  }

  @Test
  void missingGrantedCellAndActorCannotWrite() {
    reject(
        new SaveProblemContentCommand(null, actor, false, false, time, ContentTestData.valid()),
        "PRB-002");
    reject(
        new SaveProblemContentCommand(null, null, true, true, time, ContentTestData.valid()),
        "PRB-001");
  }

  @ParameterizedTest
  @CsvSource({"UNPUBLISHED,PRB-010", "PUBLISHED,PRB-003"})
  void nonAuthorsAreDeniedBeforeValidation(String status, String code) {
    when(repository.lock(id))
        .thenReturn(Optional.of(snapshot(UUID.randomUUID(), status, ContentTestData.valid())));
    reject(command(id, null), code);
  }

  @Test
  void adminCanUpdateOthersDraftAndExistingExampleIdsAreKept() {
    var example = new ProblemContentData.Example(UUID.randomUUID(), 1, "1", "2", null);
    var data = ContentTestData.replace("workedExamples", List.of(example));
    when(repository.lock(id))
        .thenReturn(Optional.of(snapshot(UUID.randomUUID(), "UNPUBLISHED", data)));
    var result = handler.save(new SaveProblemContentCommand(id, actor, true, true, time, data));
    assertThat(result.content().workedExamples()).containsExactly(example);
    verify(repository, never()).create(any(), any());
  }

  @Test
  void missingStaleAndMissingTimestampNeverWrite() {
    when(repository.lock(id)).thenReturn(Optional.empty());
    reject(command(id, ContentTestData.valid()), "PRB-010");
    when(repository.lock(id))
        .thenReturn(Optional.of(snapshot(actor, "UNPUBLISHED", ContentTestData.valid())));
    reject(
        new SaveProblemContentCommand(id, actor, false, true, null, ContentTestData.valid()),
        "PRB-001");
    reject(
        new SaveProblemContentCommand(
            id, actor, false, true, time.minusSeconds(1), ContentTestData.valid()),
        "PRB-020");
  }

  @Test
  void missingCataloguesAndMissingRequiredLevelAreRejected() {
    when(repository.cataloguesExist(any(), any())).thenReturn(false);
    reject(command(null, ContentTestData.valid()), "PRB-039");
    reject(command(id, ContentTestData.replace("levelId", null)), "PRB-001");
    when(repository.firstLevel()).thenReturn(Optional.empty());
    reject(command(null, ContentTestData.replace("levelId", null)), "PRB-039");
  }

  @Test
  void publishedRequiresTwoExamplesAndInvalidatesOnlyAfterWrite() {
    when(repository.lock(id))
        .thenReturn(Optional.of(snapshot(actor, "PUBLISHED", ContentTestData.valid())));
    reject(command(id, ContentTestData.valid()), "PRB-053");
    var examples =
        List.of(
            new ProblemContentData.Example(null, 0, "1", "2", null),
            new ProblemContentData.Example(null, 0, "2", "3", null));
    handler.save(command(id, ContentTestData.replace("workedExamples", examples)));
    verify(cache).incrementAfterCommit();
  }

  @Test
  void foreignExampleIdCannotBeInsertedOrUpdated() {
    var foreign = List.of(new ProblemContentData.Example(UUID.randomUUID(), 0, "1", "2", null));
    reject(command(null, ContentTestData.replace("workedExamples", foreign)), "PRB-001");
    reject(command(id, ContentTestData.replace("workedExamples", foreign)), "PRB-001");
  }

  private void reject(SaveProblemContentCommand command, String code) {
    assertThatThrownBy(() -> handler.save(command))
        .isInstanceOfSatisfying(
            AlgoPrepException.class, e -> assertThat(e.errorCode()).isEqualTo(code));
    verify(repository, never()).write(any(), any(), any());
  }
}
