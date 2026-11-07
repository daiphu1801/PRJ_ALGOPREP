package com.algoprep.problembank.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.common.domain.ports.out.IdGeneratorPort;
import com.algoprep.common.exception.DomainRuleViolationException;
import com.algoprep.common.exception.InvalidRequestException;
import com.algoprep.common.exception.PermissionDeniedException;
import com.algoprep.common.exception.ResourceConflictException;
import com.algoprep.common.exception.ResourceNotFoundException;
import com.algoprep.common.exception.ServiceUnavailableException;
import com.algoprep.problembank.application.ports.out.InstructorClassPort;
import com.algoprep.problembank.domain.model.ClassAssignmentModels.ClassRef;
import com.algoprep.problembank.domain.model.ClassAssignmentModels.ProblemState;
import com.algoprep.problembank.domain.ports.out.ClassAssignmentRepository;
import com.algoprep.problembank.domain.ports.out.ProblemPublishRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ClassAssignmentCommandsTest {
  private final ClassAssignmentRepository assignments = mock(ClassAssignmentRepository.class);
  private final ProblemPublishRepository outbox = mock(ProblemPublishRepository.class);
  private final IdGeneratorPort ids = mock(IdGeneratorPort.class);
  private final InstructorClassPort classes = mock(InstructorClassPort.class);
  private final ClassAssignmentExecutor executor =
      new ClassAssignmentExecutor(assignments, outbox, ids);
  private final ClassAssignmentCommandHandler handler =
      new ClassAssignmentCommandHandler(classes, executor);
  private final UUID instructor = UUID.randomUUID();
  private final UUID clazz = UUID.randomUUID();

  private void owns() {
    when(classes.classesOf(instructor)).thenReturn(List.of(new ClassRef(clazz, "A")));
    when(ids.newId()).thenReturn(UUID.randomUUID());
  }

  @Test
  void aBatchNeedsOneToFiftyIdsAndAnOwnedClass() {
    owns();
    assertThatThrownBy(() -> handler.assign(instructor, clazz, List.of()))
        .isInstanceOf(InvalidRequestException.class);
    assertThatThrownBy(() -> handler.assign(instructor, clazz, null))
        .isInstanceOf(InvalidRequestException.class);
    var tooMany = new ArrayList<UUID>();
    for (int i = 0; i < 51; i++) {
      tooMany.add(UUID.randomUUID());
    }
    assertThatThrownBy(() -> handler.assign(instructor, clazz, tooMany))
        .isInstanceOf(InvalidRequestException.class);
    assertThatThrownBy(
            () -> handler.assign(instructor, UUID.randomUUID(), List.of(UUID.randomUUID())))
        .isInstanceOf(PermissionDeniedException.class);
    verify(assignments, never()).insertActive(any(), any(), any());
  }

  @Test
  void eachProblemIsDecidedOnItsOwnAndARepeatedIdRunsOnce() {
    owns();
    var fresh = UUID.randomUUID();
    var known = UUID.randomUUID();
    var draft = UUID.randomUUID();
    var missing = UUID.randomUUID();
    when(assignments.problemState(fresh)).thenReturn(Optional.of(new ProblemState("Fresh", true)));
    when(assignments.problemState(known)).thenReturn(Optional.of(new ProblemState("Known", true)));
    when(assignments.problemState(draft)).thenReturn(Optional.of(new ProblemState("Draft", false)));
    when(assignments.problemState(missing)).thenReturn(Optional.empty());
    when(assignments.insertActive(fresh, clazz, instructor)).thenReturn(true);
    when(assignments.insertActive(known, clazz, instructor)).thenReturn(false);

    var result = handler.assign(instructor, clazz, List.of(fresh, known, draft, missing, fresh));

    assertThat(result.succeededIds()).containsExactly(fresh);
    assertThat(result.skippedIds()).containsExactly(known);
    assertThat(result.failedItems()).hasSize(2);
    assertThat(result.failedItems().get(0).code()).isEqualTo("PRB-037");
    assertThat(result.failedItems().get(1).code()).isEqualTo("PRB-010");
    verify(outbox).appendEvent(any(), any(), any(), any());
  }

  @Test
  void anEventThatCannotBeWrittenFailsThatProblemWith056() {
    owns();
    var problem = UUID.randomUUID();
    when(assignments.problemState(problem)).thenReturn(Optional.of(new ProblemState("P", true)));
    when(assignments.insertActive(problem, clazz, instructor)).thenReturn(true);
    doThrow(new IllegalStateException("outbox down"))
        .when(outbox)
        .appendEvent(any(), any(), any(), any());

    var result = handler.assign(instructor, clazz, List.of(problem));

    assertThat(result.succeededIds()).isEmpty();
    assertThat(result.failedItems().get(0).code()).isEqualTo("PRB-056");
    assertThatThrownBy(() -> executor.assign(instructor, clazz, problem))
        .isInstanceOf(ServiceUnavailableException.class);
  }

  @Test
  void removingEmitsOneEventAndAMissingAssignmentIsAConflict() {
    owns();
    var problem = UUID.randomUUID();
    when(assignments.deactivate(clazz, problem))
        .thenReturn(Optional.of(Instant.parse("2026-01-01T00:00:00Z")))
        .thenReturn(Optional.empty());

    handler.remove(instructor, clazz, problem);

    verify(outbox).appendEvent(any(), any(), any(), any());
    assertThatThrownBy(() -> handler.remove(instructor, clazz, problem))
        .isInstanceOf(ResourceConflictException.class);
    assertThatThrownBy(() -> handler.remove(instructor, UUID.randomUUID(), problem))
        .isInstanceOf(PermissionDeniedException.class);
  }

  @Test
  void theExecutorNamesTheRuleThatRefusedAProblem() {
    var draft = UUID.randomUUID();
    var missing = UUID.randomUUID();
    when(assignments.problemState(draft)).thenReturn(Optional.of(new ProblemState("D", false)));
    when(assignments.problemState(missing)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> executor.assign(instructor, clazz, draft))
        .isInstanceOf(DomainRuleViolationException.class);
    assertThatThrownBy(() -> executor.assign(instructor, clazz, missing))
        .isInstanceOf(ResourceNotFoundException.class);
  }
}
