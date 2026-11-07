package com.algoprep.identity.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.algoprep.common.domain.ports.out.ClockPort;
import com.algoprep.common.exception.ResourceNotFoundException;
import com.algoprep.identity.domain.exception.IdentityErrorCodes;
import com.algoprep.identity.domain.ports.out.ClassActivityReadRepository;
import com.algoprep.identity.domain.ports.out.InstructorClassRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ListInstructorRecentActivityQueryHandlerTest {
  private final ClassActivityReadRepository feed = mock(ClassActivityReadRepository.class);
  private final InstructorClassRepository classes = mock(InstructorClassRepository.class);
  private final ClockPort clock = mock(ClockPort.class);
  private final ListInstructorRecentActivityQueryHandler handler =
      new ListInstructorRecentActivityQueryHandler(feed, classes, clock);
  private final UUID instructor = UUID.randomUUID();
  private final UUID classId = UUID.randomUUID();
  private final Instant now = Instant.parse("2026-11-05T00:00:00Z");
  private final Instant since = Instant.parse("2026-08-07T00:00:00Z");

  @Test
  void aClassTheCallerDoesNotRunIs404Idt414AndTheFeedIsNotRead() {
    when(classes.isOwner(classId, instructor)).thenReturn(false);

    assertThatThrownBy(() -> handler.list(instructor, classId))
        .isInstanceOf(ResourceNotFoundException.class)
        .extracting(e -> ((ResourceNotFoundException) e).errorCode())
        .isEqualTo(IdentityErrorCodes.CLASS_NOT_FOUND);
    verifyNoInteractions(feed);
  }

  @Test
  void anOwnedClassReadsTenEntriesOfTheLastNinetyDaysScopedToIt() {
    when(clock.now()).thenReturn(now);
    when(classes.isOwner(classId, instructor)).thenReturn(true);

    assertThat(handler.list(instructor, classId)).isEmpty();
    verify(feed).recent(instructor, classId, since, 10);
  }

  @Test
  void noClassIdMeansEveryOwnedClassWithoutAnOwnershipCheck() {
    when(clock.now()).thenReturn(now);

    handler.list(instructor, null);

    verify(feed).recent(instructor, null, since, 10);
    verifyNoInteractions(classes);
  }
}
