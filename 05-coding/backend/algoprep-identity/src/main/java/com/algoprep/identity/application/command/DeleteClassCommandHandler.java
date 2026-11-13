package com.algoprep.identity.application.command;

import com.algoprep.common.domain.ports.out.ClockPort;
import com.algoprep.identity.application.ports.in.DeleteClassUseCase;
import com.algoprep.identity.domain.ports.out.ClassEventPort;
import com.algoprep.identity.domain.ports.out.InstructorClassRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Codes and enrolments cascade in the database; the event goes out after commit (BR-22). */
@Service
class DeleteClassCommandHandler implements DeleteClassUseCase {
  private final InstructorClassRepository classes;
  private final ClassEventPort events;
  private final ClockPort clock;

  DeleteClassCommandHandler(
      InstructorClassRepository classes, ClassEventPort events, ClockPort clock) {
    this.classes = classes;
    this.events = events;
    this.clock = clock;
  }

  @Override
  @Transactional
  public void delete(UUID instructorId, UUID classId) {
    if (!classes.deleteOwned(classId, instructorId)) {
      throw OwnedClasses.notFound();
    }
    events.classDeleted(classId, clock.now());
  }
}
