package com.algoprep.identity.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.algoprep.common.exception.AlgoPrepException;
import com.algoprep.identity.domain.model.MyClass;
import com.algoprep.identity.domain.ports.out.StudentClassRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** #25A and #25B: pass the caller through, and every miss is the same 404 IDT-411. */
class StudentClassQueryHandlersTest {
  private final StudentClassRepository classes = mock(StudentClassRepository.class);
  private final UUID userId = UUID.randomUUID();
  private final MyClass mine =
      new MyClass(UUID.randomUUID(), "Algo 101", "", null, "Teacher", Instant.now());

  @Test
  void listReturnsTheRepositoryRows() {
    when(classes.listActive(userId)).thenReturn(List.of(mine));

    assertThat(new ListMyClassesQueryHandler(classes).list(userId)).containsExactly(mine);
  }

  @Test
  void getReturnsTheClassWhenTheCallerIsAMember() {
    when(classes.findActive(userId, mine.classId())).thenReturn(Optional.of(mine));

    assertThat(new GetMyClassQueryHandler(classes).get(userId, mine.classId().toString()))
        .isEqualTo(mine);
  }

  @Test
  void getIs404Idt411WhenNotAMember() {
    when(classes.findActive(userId, mine.classId())).thenReturn(Optional.empty());

    assertThatThrownBy(
            () -> new GetMyClassQueryHandler(classes).get(userId, mine.classId().toString()))
        .isInstanceOfSatisfying(
            AlgoPrepException.class, e -> assertThat(e.errorCode()).isEqualTo("IDT-411"));
  }

  @Test
  void getIs404Idt411ForAMalformedIdWithoutTouchingTheRepository() {
    GetMyClassQueryHandler handler = new GetMyClassQueryHandler(classes);

    for (String bad : new String[] {"not-a-uuid", "", null}) {
      assertThatThrownBy(() -> handler.get(userId, bad))
          .isInstanceOfSatisfying(
              AlgoPrepException.class, e -> assertThat(e.errorCode()).isEqualTo("IDT-411"));
    }
    verifyNoInteractions(classes);
  }
}
