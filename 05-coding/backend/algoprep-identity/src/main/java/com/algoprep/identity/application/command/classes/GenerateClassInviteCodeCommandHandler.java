package com.algoprep.identity.application.command;

import com.algoprep.identity.application.ports.in.GenerateClassInviteCodeUseCase;
import com.algoprep.identity.domain.model.ClassInviteCode;
import com.algoprep.identity.domain.ports.out.InstructorClassRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Several codes may be valid at once, so an earlier code is never revoked here (#23). */
@Service
class GenerateClassInviteCodeCommandHandler implements GenerateClassInviteCodeUseCase {
  private final InstructorClassRepository classes;
  private final ClassInviteCodeIssuer codes;

  GenerateClassInviteCodeCommandHandler(
      InstructorClassRepository classes, ClassInviteCodeIssuer codes) {
    this.classes = classes;
    this.codes = codes;
  }

  @Override
  @Transactional
  public ClassInviteCode generate(UUID instructorId, UUID classId) {
    if (!classes.isOwner(classId, instructorId)) {
      throw OwnedClasses.notFound();
    }
    return codes.issue(classId, instructorId);
  }
}
