package com.algoprep.identity.application.command.classes;

import com.algoprep.identity.application.ports.in.classes.RevokeClassInviteCodeUseCase;
import com.algoprep.identity.domain.ports.out.InstructorClassRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The other codes of the class stay valid; a repeated call is a no-op (#78, F1-25). */
@Service
class RevokeClassInviteCodeCommandHandler implements RevokeClassInviteCodeUseCase {
  private final InstructorClassRepository classes;

  RevokeClassInviteCodeCommandHandler(InstructorClassRepository classes) {
    this.classes = classes;
  }

  @Override
  @Transactional
  public void revoke(UUID instructorId, UUID classId, UUID codeId) {
    if (!classes.isOwner(classId, instructorId)) {
      throw OwnedClasses.notFound();
    }
    classes.deleteInviteCode(classId, codeId);
  }
}
