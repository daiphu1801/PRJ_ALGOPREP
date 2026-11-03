package com.algoprep.identity.application.command;

import com.algoprep.common.domain.ports.out.IdGeneratorPort;
import com.algoprep.identity.application.dto.CreateClassCommand;
import com.algoprep.identity.application.dto.CreatedClass;
import com.algoprep.identity.application.ports.in.CreateClassUseCase;
import com.algoprep.identity.domain.model.ClassInviteCode;
import com.algoprep.identity.domain.ports.out.InstructorClassRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The class and its first invite code are written in one transaction (#20, F1-23, F1-25). */
@Service
class CreateClassCommandHandler implements CreateClassUseCase {
  private final InstructorClassRepository classes;
  private final ClassInviteCodeIssuer codes;
  private final IdGeneratorPort ids;

  CreateClassCommandHandler(
      InstructorClassRepository classes, ClassInviteCodeIssuer codes, IdGeneratorPort ids) {
    this.classes = classes;
    this.codes = codes;
    this.ids = ids;
  }

  @Override
  @Transactional
  public CreatedClass create(CreateClassCommand command) {
    UUID id = ids.newId();
    classes.create(
        id,
        command.instructorId(),
        ClassValidation.name(command.name()),
        ClassValidation.description(command.description()),
        ClassValidation.schedule(command.schedule()));
    ClassInviteCode code = codes.issue(id, command.instructorId());
    return new CreatedClass(classes.findOwned(id, command.instructorId()).orElseThrow(), code);
  }
}
