package com.algoprep.identity.application.command.classes;

import com.algoprep.identity.application.dto.classes.UpdateClassCommand;
import com.algoprep.identity.application.ports.in.classes.UpdateClassUseCase;
import com.algoprep.identity.domain.model.classes.InstructorClass;
import com.algoprep.identity.domain.ports.out.InstructorClassRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Merges the sent fields over the stored class, validating only what was sent (#21). */
@Service
class UpdateClassCommandHandler implements UpdateClassUseCase {
  private final InstructorClassRepository classes;

  UpdateClassCommandHandler(InstructorClassRepository classes) {
    this.classes = classes;
  }

  @Override
  @Transactional
  public InstructorClass update(UpdateClassCommand command) {
    InstructorClass current =
        classes
            .findOwned(command.classId(), command.instructorId())
            .orElseThrow(OwnedClasses::notFound);
    classes.update(
        command.classId(),
        command.name() == null ? current.name() : ClassValidation.name(command.name()),
        command.description() == null
            ? current.description()
            : ClassValidation.description(command.description()),
        command.schedule() == null
            ? current.schedule()
            : ClassValidation.schedule(command.schedule()));
    return classes.findOwned(command.classId(), command.instructorId()).orElseThrow();
  }
}
