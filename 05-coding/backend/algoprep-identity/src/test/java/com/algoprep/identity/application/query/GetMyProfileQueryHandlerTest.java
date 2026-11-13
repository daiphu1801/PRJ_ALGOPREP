package com.algoprep.identity.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.algoprep.common.exception.UnauthenticatedException;
import com.algoprep.identity.domain.model.MyProfile;
import com.algoprep.identity.domain.ports.out.MyProfileRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GetMyProfileQueryHandlerTest {
  private final MyProfileRepository profiles = mock(MyProfileRepository.class);
  private final GetMyProfileQueryHandler handler = new GetMyProfileQueryHandler(profiles);
  private final UUID userId = UUID.randomUUID();

  @Test
  void returnsTheCallersOwnProfile() {
    MyProfile profile =
        new MyProfile(
            userId,
            "Name",
            "a@example.test",
            null,
            null,
            null,
            "PYTHON",
            null,
            true,
            null,
            List.of(),
            Instant.parse("2026-10-31T00:00:00Z"),
            "Học viên",
            false);
    when(profiles.findById(userId)).thenReturn(Optional.of(profile));

    assertThat(handler.get(userId)).isSameAs(profile);
  }

  @Test
  void aMissingAccountIs401() {
    when(profiles.findById(userId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> handler.get(userId)).isInstanceOf(UnauthenticatedException.class);
  }
}
