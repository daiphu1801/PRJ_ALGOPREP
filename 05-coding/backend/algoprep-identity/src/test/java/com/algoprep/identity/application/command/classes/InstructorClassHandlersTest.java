package com.algoprep.identity.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.common.domain.ports.out.ClockPort;
import com.algoprep.common.domain.ports.out.IdGeneratorPort;
import com.algoprep.common.exception.AlgoPrepException;
import com.algoprep.identity.application.dto.CreateClassCommand;
import com.algoprep.identity.application.dto.CreatedClass;
import com.algoprep.identity.application.dto.UpdateClassCommand;
import com.algoprep.identity.domain.model.ClassInviteCode;
import com.algoprep.identity.domain.model.InstructorClass;
import com.algoprep.identity.domain.model.ScheduleSpec;
import com.algoprep.identity.domain.ports.out.ClassEventPort;
import com.algoprep.identity.domain.ports.out.InstructorClassRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** #20 to #23 and #27 on the application side: ownership, merge, retry and event hand-off. */
class InstructorClassHandlersTest {
  private final InstructorClassRepository classes = mock(InstructorClassRepository.class);
  private final ClassEventPort events = mock(ClassEventPort.class);
  private final IdGeneratorPort ids = mock(IdGeneratorPort.class);
  private final ClockPort clock = mock(ClockPort.class);
  private final ClassInviteCodeIssuer issuer = new ClassInviteCodeIssuer(classes, ids);
  private final UUID instructor = UUID.randomUUID();
  private final UUID classId = UUID.randomUUID();
  private final UUID studentId = UUID.randomUUID();
  private final Instant now = Instant.parse("2026-11-01T10:00:00Z");
  private final ClassInviteCode code = new ClassInviteCode(UUID.randomUUID(), "ABCD2345", now);
  private final ScheduleSpec stored =
      new ScheduleSpec(List.of(1), "08:00", "09:00", "A1", null, null, null);
  private final InstructorClass existing =
      new InstructorClass(classId, "Old", "Old description", stored, 3, now);

  private static void assertCode(Throwable error, String code) {
    assertThat(error)
        .isInstanceOfSatisfying(
            AlgoPrepException.class, e -> assertThat(e.errorCode()).isEqualTo(code));
  }

  @Test
  void createWritesTheClassAndItsFirstCodeThenReturnsBoth() {
    when(ids.newId()).thenReturn(classId, code.id());
    when(classes.insertInviteCode(any(), eq(classId), eq(instructor), anyString()))
        .thenReturn(Optional.of(code));
    when(classes.findOwned(classId, instructor)).thenReturn(Optional.of(existing));

    CreatedClass result =
        new CreateClassCommandHandler(classes, issuer, ids)
            .create(new CreateClassCommand(instructor, "  Algo  ", " ", null));

    assertThat(result.created()).isEqualTo(existing);
    assertThat(result.inviteCode()).isEqualTo(code);
    verify(classes).create(classId, instructor, "Algo", null, null);
  }

  @Test
  void createRejectsABadNameBeforeWritingAnything() {
    CreateClassCommandHandler handler = new CreateClassCommandHandler(classes, issuer, ids);

    assertThatThrownBy(() -> handler.create(new CreateClassCommand(instructor, " ", null, null)))
        .satisfies(e -> assertCode(e, "IDT-V14"));
    verify(classes, never()).create(any(), any(), any(), any(), any());
  }

  @Test
  void aTakenCodeIsRedrawnAndGivingUpFailsLoudly() {
    when(ids.newId()).thenReturn(UUID.randomUUID());
    when(classes.insertInviteCode(any(), any(), any(), anyString()))
        .thenReturn(Optional.empty(), Optional.of(code));

    assertThat(issuer.issue(classId, instructor)).isEqualTo(code);
    verify(classes, times(2)).insertInviteCode(any(), any(), any(), anyString());

    when(classes.insertInviteCode(any(), any(), any(), anyString())).thenReturn(Optional.empty());
    assertThatThrownBy(() -> issuer.issue(classId, instructor))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void issuedCodesUseOnlyTheUnambiguousAlphabet() {
    when(ids.newId()).thenReturn(UUID.randomUUID());
    when(classes.insertInviteCode(any(), any(), any(), anyString()))
        .thenAnswer(
            call -> Optional.of(new ClassInviteCode(UUID.randomUUID(), call.getArgument(3), now)));

    for (int i = 0; i < 200; i++) {
      assertThat(issuer.issue(classId, instructor).code())
          .matches("[ABCDEFGHJKLMNPQRSTUVWXYZ2-9]{8}");
    }
  }

  @Test
  void updateKeepsUnsentFieldsAndClearsWhatIsSentEmpty() {
    when(classes.findOwned(classId, instructor)).thenReturn(Optional.of(existing));
    UpdateClassCommandHandler handler = new UpdateClassCommandHandler(classes);

    handler.update(new UpdateClassCommand(instructor, classId, null, null, null));
    verify(classes).update(classId, "Old", "Old description", stored);

    handler.update(
        new UpdateClassCommand(
            instructor,
            classId,
            " New ",
            "",
            new ScheduleSpec(null, null, null, null, null, null, null)));
    verify(classes).update(classId, "New", null, null);
  }

  @Test
  void updateOfAClassThatIsNotTheCallersIs404Idt414() {
    when(classes.findOwned(classId, instructor)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                new UpdateClassCommandHandler(classes)
                    .update(new UpdateClassCommand(instructor, classId, "x", null, null)))
        .satisfies(e -> assertCode(e, "IDT-414"));
    verify(classes, never()).update(any(), any(), any(), any());
  }

  @Test
  void deleteAnnouncesTheEventOnlyWhenTheRowWasRemoved() {
    when(clock.now()).thenReturn(now);
    when(classes.deleteOwned(classId, instructor)).thenReturn(true);
    DeleteClassCommandHandler handler = new DeleteClassCommandHandler(classes, events, clock);

    handler.delete(instructor, classId);

    verify(events).classDeleted(classId, now);
    UUID other = UUID.randomUUID();
    assertThatThrownBy(() -> handler.delete(instructor, other))
        .satisfies(e -> assertCode(e, "IDT-414"));
    verify(events, never()).classDeleted(eq(other), any());
  }

  @Test
  void generatingACodeNeedsOwnership() {
    when(ids.newId()).thenReturn(UUID.randomUUID());
    when(classes.isOwner(classId, instructor)).thenReturn(true);
    when(classes.insertInviteCode(any(), eq(classId), eq(instructor), anyString()))
        .thenReturn(Optional.of(code));
    GenerateClassInviteCodeCommandHandler handler =
        new GenerateClassInviteCodeCommandHandler(classes, issuer);

    assertThat(handler.generate(instructor, classId)).isEqualTo(code);
    assertThatThrownBy(() -> handler.generate(UUID.randomUUID(), classId))
        .satisfies(e -> assertCode(e, "IDT-414"));
  }

  @Test
  void leavePublishesWithTheStoredMomentAndRepeatsWithTheSameOne() {
    Instant leftAt = Instant.parse("2026-10-30T08:00:00.123456Z");
    when(classes.isOwner(classId, instructor)).thenReturn(true);
    when(classes.markLeft(classId, studentId)).thenReturn(Optional.of(leftAt));
    RemoveStudentFromClassCommandHandler handler =
        new RemoveStudentFromClassCommandHandler(classes, events);

    assertThat(handler.remove(instructor, classId, studentId)).isEqualTo(leftAt);
    assertThat(handler.remove(instructor, classId, studentId)).isEqualTo(leftAt);

    verify(events, times(2)).studentRemoved(classId, studentId, leftAt);
  }

  @Test
  void leaveOfAStrangerOrInAForeignClassIs404AndPublishesNothing() {
    when(classes.isOwner(classId, instructor)).thenReturn(true);
    when(classes.markLeft(classId, studentId)).thenReturn(Optional.empty());
    RemoveStudentFromClassCommandHandler handler =
        new RemoveStudentFromClassCommandHandler(classes, events);

    assertThatThrownBy(() -> handler.remove(instructor, classId, studentId))
        .satisfies(e -> assertCode(e, "IDT-414"));
    assertThatThrownBy(() -> handler.remove(UUID.randomUUID(), classId, studentId))
        .satisfies(e -> assertCode(e, "IDT-414"));
    verify(events, never()).studentRemoved(any(), any(), any());
  }
}
