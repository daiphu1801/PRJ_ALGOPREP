package com.algoprep.identity.application.query;

import com.algoprep.identity.application.ports.in.GetClassStudentProfileUseCase;
import com.algoprep.identity.domain.model.ClassRosterEntry;
import com.algoprep.identity.domain.model.ClassStudentRow;
import com.algoprep.identity.domain.model.InstructorClass;
import com.algoprep.identity.domain.ports.out.InstructorClassRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** #28. A class or a student outside the caller scope is one 404 IDT-414. */
@Service
class GetClassStudentProfileQueryHandler implements GetClassStudentProfileUseCase {
  private final InstructorClassRepository classes;
  private final ClassMetricsAssembler assembler;

  GetClassStudentProfileQueryHandler(
      InstructorClassRepository classes, ClassMetricsAssembler assembler) {
    this.classes = classes;
    this.assembler = assembler;
  }

  @Override
  public ClassStudentRow get(UUID instructorId, UUID classId, UUID studentId) {
    InstructorClass owned =
        classes.findOwned(classId, instructorId).orElseThrow(ClassMetricsAssembler::classNotFound);
    ClassRosterEntry student =
        classes.listActiveRoster(classId).stream()
            .filter(s -> s.studentId().equals(studentId))
            .findFirst()
            .orElseThrow(ClassMetricsAssembler::classNotFound);
    var windows = assembler.windows(List.of(classId)).getOrDefault(classId, List.of());
    return assembler.rows(classId, owned.name(), List.of(student), windows, assembler.now()).get(0);
  }
}
