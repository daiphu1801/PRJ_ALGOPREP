package com.algoprep.identity.application.query.avatar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.common.exception.AlgoPrepException;
import com.algoprep.identity.application.dto.avatar.AvatarContent;
import com.algoprep.identity.application.ports.out.AvatarObjectStorePort;
import com.algoprep.identity.domain.model.avatar.AvatarRecord;
import com.algoprep.identity.domain.ports.out.UserAvatarRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** BR-27: one 404 for every reason, conditional GET, bytes read lazily. */
class GetUserAvatarQueryHandlerTest {
  private final UserAvatarRepository avatars = mock(UserAvatarRepository.class);
  private final AvatarObjectStorePort store = mock(AvatarObjectStorePort.class);
  private final GetUserAvatarQueryHandler handler = new GetUserAvatarQueryHandler(avatars, store);
  private final UUID userId = UUID.randomUUID();
  private final AvatarRecord row =
      new AvatarRecord(userId, "users/u/abc-123.png", "image/png", 3, Instant.EPOCH);

  @Test
  void noReadableRowIs404Idt451WithoutTouchingStorage() {
    when(avatars.findReadable(userId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> handler.get(userId, null))
        .isInstanceOfSatisfying(
            AlgoPrepException.class, e -> assertThat(e.errorCode()).isEqualTo("IDT-451"));

    verify(store, never()).get("users/u/abc-123.png");
  }

  @Test
  void theBodyIsReadOnlyWhenTheSupplierRuns() {
    when(avatars.findReadable(userId)).thenReturn(Optional.of(row));
    when(store.get("users/u/abc-123.png")).thenReturn(new byte[] {1, 2, 3});

    AvatarContent content = handler.get(userId, null);

    assertThat(content.notModified()).isFalse();
    assertThat(content.contentType()).isEqualTo("image/png");
    assertThat(content.etag()).isEqualTo("\"abc-123\"");
    verify(store, never()).get("users/u/abc-123.png");
    assertThat(content.body().get()).containsExactly(1, 2, 3);
  }

  @Test
  void aMatchingIfNoneMatchIsNotModifiedInEveryAcceptedForm() {
    when(avatars.findReadable(userId)).thenReturn(Optional.of(row));

    for (String header :
        new String[] {
          "\"abc-123\"", "W/\"abc-123\"", "\"zzz\", \"abc-123\"", " \"zzz\" , W/\"abc-123\" ", "*"
        }) {
      AvatarContent content = handler.get(userId, header);
      assertThat(content.notModified()).as(header).isTrue();
      assertThat(content.body()).isNull();
      assertThat(content.etag()).isEqualTo("\"abc-123\"");
    }
    verify(store, never()).get("users/u/abc-123.png");
  }

  @Test
  void aDifferentOrEmptyIfNoneMatchReturnsTheBody() {
    when(avatars.findReadable(userId)).thenReturn(Optional.of(row));

    for (String header : new String[] {"\"other\"", "abc-123", "", null}) {
      assertThat(handler.get(userId, header).notModified()).as(String.valueOf(header)).isFalse();
    }
  }
}
