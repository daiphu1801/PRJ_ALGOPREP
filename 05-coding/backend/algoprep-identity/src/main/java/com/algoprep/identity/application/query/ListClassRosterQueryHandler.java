package com.algoprep.identity.application.query;

import com.algoprep.common.exception.ResourceNotFoundException;
import com.algoprep.identity.application.ports.in.ListClassRosterUseCase;
import com.algoprep.identity.domain.exception.IdentityErrorCodes;
import com.algoprep.identity.domain.model.ClassRosterEntry;
import com.algoprep.identity.domain.ports.out.InstructorClassRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class ListClassRosterQueryHandler implements ListClassRosterUseCase {
  private final InstructorClassRepository classes;

  ListClassRosterQueryHandler(InstructorClassRepository classes) {
    this.classes = classes;
  }

  @Override
  @Transactional(readOnly = true)
  public List<ClassRosterEntry> list(UUID instructorId, UUID classId) {
    if (!classes.isOwner(classId, instructorId)) {
      throw new ResourceNotFoundException(
          IdentityErrorCodes.CLASS_NOT_FOUND, "Class is not available");
    }
    return classes.listActiveRoster(classId);
  }
}
