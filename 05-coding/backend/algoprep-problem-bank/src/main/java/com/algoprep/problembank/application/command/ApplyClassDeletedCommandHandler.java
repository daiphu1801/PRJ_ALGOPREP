package com.algoprep.problembank.application.command;

import com.algoprep.problembank.application.ports.in.ApplyClassDeletedUseCase;
import com.algoprep.problembank.domain.ports.out.ClassDeletedRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class ApplyClassDeletedCommandHandler implements ApplyClassDeletedUseCase {
  private final ClassDeletedRepository repository;

  ApplyClassDeletedCommandHandler(ClassDeletedRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional
  public int apply(UUID classId) {
    return repository.removeAssignments(classId);
  }
}
