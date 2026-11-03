package com.algoprep.problembank.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.algoprep.problembank.domain.model.ClassAssignmentSnapshot;
import com.algoprep.problembank.domain.ports.out.ClassAssignmentReadRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ListClassAssignmentsQueryHandlerTest {
  private final ClassAssignmentReadRepository repository =
      mock(ClassAssignmentReadRepository.class);
  private final ListClassAssignmentsQueryHandler handler =
      new ListClassAssignmentsQueryHandler(repository);
  private final UUID classId = UUID.randomUUID();
  private final UUID problemId = UUID.randomUUID();

  @Test
  void emptyClassesNeverReadAllAssignments() {
    assertThat(handler.listAssignments(List.of())).isEmpty();
    verifyNoInteractions(repository);
  }

  @Test
  void rejectsNullCollectionAndNullIdentifiersBeforeReading() {
    assertThatThrownBy(() -> handler.listAssignments(null))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> handler.listAssignments(Arrays.asList(classId, null)))
        .isInstanceOf(NullPointerException.class);
    verifyNoInteractions(repository);
  }

  @Test
  void deduplicatesInputWithoutCollapsingDistinctAssignmentIntervals() {
    Instant first = Instant.parse("2026-10-01T00:00:00Z");
    Instant removed = first.plusSeconds(3600);
    var history = new ClassAssignmentSnapshot(classId, problemId, "Bài cũ", first, removed);
    var current = new ClassAssignmentSnapshot(classId, problemId, "Bài cũ", removed, null);
    when(repository.findAssignments(Set.of(classId))).thenReturn(List.of(history, current));
    assertThat(handler.listAssignments(List.of(classId, classId)))
        .containsExactly(history, current);
    verify(repository).findAssignments(Set.of(classId));
  }

  @Test
  void outputIsAnImmutableSnapshot() {
    var row = new ClassAssignmentSnapshot(classId, problemId, "Name", Instant.EPOCH, null);
    var stored = new ArrayList<>(List.of(row));
    when(repository.findAssignments(Set.of(classId))).thenReturn(stored);
    var result = handler.listAssignments(List.of(classId));
    stored.clear();
    assertThat(result).containsExactly(row);
    assertThatThrownBy(result::clear).isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void classesWithoutAssignmentsHaveALegitimateEmptyResult() {
    when(repository.findAssignments(Set.of(classId))).thenReturn(List.of());
    assertThat(handler.listAssignments(List.of(classId))).isEmpty();
  }

  @Test
  void databaseFailureIsNotConvertedIntoNoAssignments() {
    var failure = new IllegalStateException("database unavailable");
    when(repository.findAssignments(Set.of(classId))).thenThrow(failure);
    assertThatThrownBy(() -> handler.listAssignments(List.of(classId))).isSameAs(failure);
  }
}
