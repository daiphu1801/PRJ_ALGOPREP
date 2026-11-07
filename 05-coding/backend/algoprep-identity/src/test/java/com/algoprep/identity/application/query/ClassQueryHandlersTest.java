package com.algoprep.identity.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.algoprep.common.exception.AlgoPrepException;
import com.algoprep.identity.application.ports.out.ClassAssignmentLookupPort;
import com.algoprep.identity.application.ports.out.ClassMetricsPort;
import com.algoprep.identity.domain.model.ClassInviteCode;
import com.algoprep.identity.domain.model.ClassRosterEntry;
import com.algoprep.identity.domain.model.ClassStudentRow;
import com.algoprep.identity.domain.model.InstructorClass;
import com.algoprep.identity.domain.ports.out.InstructorClassRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** #19, #24 and #26: reads, with the same 404 IDT-414 for classes the caller does not own. */
class ClassQueryHandlersTest {
  private final InstructorClassRepository classes = mock(InstructorClassRepository.class);
  private final UUID instructor = UUID.randomUUID();
  private final UUID classId = UUID.randomUUID();
  private final Instant now = Instant.parse("2026-11-01T10:00:00Z");

  private static void assertNotFound(Throwable error) {
    assertThat(error)
        .isInstanceOfSatisfying(
            AlgoPrepException.class, e -> assertThat(e.errorCode()).isEqualTo("IDT-414"));
  }

  @Test
  void listReturnsWhatTheRepositoryHoldsForTheCaller() {
    List<InstructorClass> rows = List.of(new InstructorClass(classId, "A", null, null, 0, now));
    when(classes.listOwned(instructor)).thenReturn(rows);

    assertThat(new ListInstructorClassesQueryHandler(classes).list(instructor)).isEqualTo(rows);
  }

  @Test
  void inviteCodesAreListedOnlyForAnOwnedClass() {
    List<ClassInviteCode> rows = List.of(new ClassInviteCode(UUID.randomUUID(), "ABCD2345", now));
    when(classes.isOwner(classId, instructor)).thenReturn(true);
    when(classes.listActiveInviteCodes(classId)).thenReturn(rows);
    ListClassInviteCodesQueryHandler handler = new ListClassInviteCodesQueryHandler(classes);

    assertThat(handler.list(instructor, classId)).isEqualTo(rows);
    assertThatThrownBy(() -> handler.list(UUID.randomUUID(), classId))
        .satisfies(ClassQueryHandlersTest::assertNotFound);
  }

  @Test
  void rosterIsListedOnlyForAnOwnedClassAndKeepsStudentsWhenFiguresAreDown() {
    List<ClassRosterEntry> rows =
        List.of(new ClassRosterEntry(UUID.randomUUID(), "Student", "s@example.test", null, now));
    InstructorClass owned = new InstructorClass(classId, "A", null, null, 1, now);
    when(classes.findOwned(classId, instructor)).thenReturn(Optional.of(owned));
    when(classes.listActiveRoster(classId)).thenReturn(rows);
    ClassAssignmentLookupPort down = mock(ClassAssignmentLookupPort.class);
    when(down.assignmentsOf(any())).thenThrow(new IllegalStateException("down"));
    ClassMetricsAssembler assembler =
        new ClassMetricsAssembler(classes, down, mock(ClassMetricsPort.class), () -> now);
    ListClassRosterQueryHandler handler = new ListClassRosterQueryHandler(classes, assembler);

    List<ClassStudentRow> listed = handler.list(instructor, classId);

    assertThat(listed).hasSize(1);
    assertThat(listed.get(0).student()).isEqualTo(rows.get(0));
    assertThat(listed.get(0).status()).isNull();
    assertThatThrownBy(() -> handler.list(UUID.randomUUID(), classId))
        .satisfies(ClassQueryHandlersTest::assertNotFound);
  }
}
