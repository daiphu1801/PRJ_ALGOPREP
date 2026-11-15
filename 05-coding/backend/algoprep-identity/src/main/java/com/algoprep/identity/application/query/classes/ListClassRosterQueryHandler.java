package com.algoprep.identity.application.query;

import com.algoprep.common.exception.ResourceNotFoundException;
import com.algoprep.identity.application.ports.in.ListClassRosterUseCase;
import com.algoprep.identity.domain.exception.IdentityErrorCodes;
import com.algoprep.identity.domain.model.ClassRosterEntry;
import com.algoprep.identity.domain.model.ClassStudentRow;
import com.algoprep.identity.domain.model.InstructorClass;
import com.algoprep.identity.domain.ports.out.InstructorClassRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * #26. The roster is what the instructor needs to manage the class, so when the judge or
 * problem-bank figures cannot be read the students still come back, without figures [SoT: Suy luan,
 * the DD gives #26 no 503].
 */
@Service
class ListClassRosterQueryHandler implements ListClassRosterUseCase {
  private final InstructorClassRepository classes;
  private final ClassMetricsAssembler assembler;

  ListClassRosterQueryHandler(InstructorClassRepository classes, ClassMetricsAssembler assembler) {
    this.classes = classes;
    this.assembler = assembler;
  }

  @Override
  public List<ClassStudentRow> list(UUID instructorId, UUID classId) {
    InstructorClass owned =
        classes
            .findOwned(classId, instructorId)
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        IdentityErrorCodes.CLASS_NOT_FOUND, "Class is not available"));
    List<ClassRosterEntry> roster = classes.listActiveRoster(classId);
    try {
      var windows = assembler.windows(List.of(classId)).getOrDefault(classId, List.of());
      return assembler.rows(classId, owned.name(), roster, windows, assembler.now());
    } catch (RuntimeException unavailable) {
      return roster.stream().map(s -> ClassStudentRow.plain(classId, owned.name(), s)).toList();
    }
  }
}
