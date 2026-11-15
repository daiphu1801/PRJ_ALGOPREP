package com.algoprep.identity.application.query;

import com.algoprep.common.domain.ports.out.ClockPort;
import com.algoprep.common.exception.ResourceNotFoundException;
import com.algoprep.identity.application.ports.in.ListInstructorRecentActivityUseCase;
import com.algoprep.identity.domain.exception.IdentityErrorCodes;
import com.algoprep.identity.domain.model.ClassActivityItem;
import com.algoprep.identity.domain.ports.out.ClassActivityReadRepository;
import com.algoprep.identity.domain.ports.out.InstructorClassRepository;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** A class the caller does not run is 404 IDT-414, the same as a missing one. */
@Service
class ListInstructorRecentActivityQueryHandler implements ListInstructorRecentActivityUseCase {
  static final int LIMIT = 10;
  static final Duration WINDOW = Duration.ofDays(90);

  private final ClassActivityReadRepository feed;
  private final InstructorClassRepository classes;
  private final ClockPort clock;

  ListInstructorRecentActivityQueryHandler(
      ClassActivityReadRepository feed, InstructorClassRepository classes, ClockPort clock) {
    this.feed = feed;
    this.classes = classes;
    this.clock = clock;
  }

  @Override
  @Transactional(readOnly = true)
  public List<ClassActivityItem> list(UUID instructorId, UUID classId) {
    if (classId != null && !classes.isOwner(classId, instructorId)) {
      throw new ResourceNotFoundException(
          IdentityErrorCodes.CLASS_NOT_FOUND, "Class is not available");
    }
    return feed.recent(instructorId, classId, clock.now().minus(WINDOW), LIMIT);
  }
}
