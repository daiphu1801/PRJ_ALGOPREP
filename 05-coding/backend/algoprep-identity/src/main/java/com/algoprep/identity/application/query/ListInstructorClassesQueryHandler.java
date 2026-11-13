package com.algoprep.identity.application.query;

import com.algoprep.identity.application.ports.in.ListInstructorClassesUseCase;
import com.algoprep.identity.domain.model.InstructorClass;
import com.algoprep.identity.domain.ports.out.InstructorClassRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class ListInstructorClassesQueryHandler implements ListInstructorClassesUseCase {
  private final InstructorClassRepository classes;

  ListInstructorClassesQueryHandler(InstructorClassRepository classes) {
    this.classes = classes;
  }

  @Override
  @Transactional(readOnly = true)
  public List<InstructorClass> list(UUID instructorId) {
    return classes.listOwned(instructorId);
  }
}
