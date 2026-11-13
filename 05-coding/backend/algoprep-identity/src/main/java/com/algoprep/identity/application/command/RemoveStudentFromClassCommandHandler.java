package com.algoprep.identity.application.command;

import com.algoprep.identity.application.ports.in.RemoveStudentFromClassUseCase;
import com.algoprep.identity.domain.ports.out.ClassEventPort;
import com.algoprep.identity.domain.ports.out.InstructorClassRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * A repeat call keeps the first {@code left_at} and announces the event again with that same
 * moment; consumers de-duplicate on (classId, studentId, occurredAt) (BR-11).
 */
@Service
class RemoveStudentFromClassCommandHandler implements RemoveStudentFromClassUseCase {
  private final InstructorClassRepository classes;
  private final ClassEventPort events;

  RemoveStudentFromClassCommandHandler(InstructorClassRepository classes, ClassEventPort events) {
    this.classes = classes;
    this.events = events;
  }

  @Override
  @Transactional
  public Instant remove(UUID instructorId, UUID classId, UUID studentId) {
    if (!classes.isOwner(classId, instructorId)) {
      throw OwnedClasses.notFound();
    }
    Instant leftAt = classes.markLeft(classId, studentId).orElseThrow(OwnedClasses::notFound);
    events.studentRemoved(classId, studentId, leftAt);
    return leftAt;
  }
}
