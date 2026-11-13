package com.algoprep.identity.application.query;

import com.algoprep.identity.application.ports.in.ListMyClassesUseCase;
import com.algoprep.identity.domain.model.MyClass;
import com.algoprep.identity.domain.ports.out.StudentClassRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The caller is the only subject: the id comes from the access token. */
@Service
class ListMyClassesQueryHandler implements ListMyClassesUseCase {
  private final StudentClassRepository classes;

  ListMyClassesQueryHandler(StudentClassRepository classes) {
    this.classes = classes;
  }

  @Override
  @Transactional(readOnly = true)
  public List<MyClass> list(UUID userId) {
    return classes.listActive(userId);
  }
}
