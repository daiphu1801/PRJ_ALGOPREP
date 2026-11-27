package com.algoprep.identity.application.command.avatar;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.common.exception.ServiceUnavailableException;
import com.algoprep.identity.application.ports.out.AvatarImageFetchPort;
import com.algoprep.identity.application.ports.out.AvatarObjectStorePort;
import com.algoprep.identity.domain.model.auth.OAuthProvider;
import com.algoprep.identity.domain.model.avatar.AvatarRecord;
import com.algoprep.identity.domain.ports.out.UserAvatarRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.RejectedExecutionException;
import org.junit.jupiter.api.Test;

class OAuthAvatarImporterTest {
  private static final String URL = "https://lh3.googleusercontent.com/a/abc";
  private final AvatarImageFetchPort fetcher = mock(AvatarImageFetchPort.class);
  private final UserAvatarRepository avatars = mock(UserAvatarRepository.class);
  private final AvatarObjectStorePort store = mock(AvatarObjectStorePort.class);
  private final AvatarObjectCleanup cleanup = new AvatarObjectCleanup(store);
  private final OAuthAvatarImporter importer =
      new OAuthAvatarImporter(
          fetcher, avatars, new OAuthAvatarSaver(store, avatars, cleanup), Runnable::run);
  private final UUID user = UUID.randomUUID();

  private static byte[] png() {
    byte[] d = new byte[40];
    byte[] magic = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    System.arraycopy(magic, 0, d, 0, 8);
    d[12] = 'I';
    d[13] = 'H';
    d[14] = 'D';
    d[15] = 'R';
    d[19] = 8;
    d[23] = 8;
    return d;
  }

  private AvatarRecord existing() {
    return new AvatarRecord(user, "users/x/a.png", "image/png", 5, Instant.now());
  }

  @Test
  void aValidPictureIsFetchedWithTheNormalisedUrlAndStored() {
    when(fetcher.fetch(URL + "=s256-c")).thenReturn(Optional.of(png()));
    when(avatars.findForUpdate(user)).thenReturn(Optional.empty());

    importer.run(user, OAuthProvider.GOOGLE, URL);

    verify(store).put(anyString(), any(), eq("image/png"));
    verify(avatars).upsert(eq(user), anyString(), eq("image/png"), anyInt());
  }

  @Test
  void anExistingAvatarIsNeverFetchedOrOverwritten() {
    when(avatars.findReadable(user)).thenReturn(Optional.of(existing()));

    importer.run(user, OAuthProvider.GOOGLE, URL);

    verify(fetcher, never()).fetch(anyString());
    verify(avatars, never()).upsert(any(), any(), any(), anyInt());
  }

  @Test
  void anAvatarCreatedWhileDownloadingIsNotOverwritten() {
    when(fetcher.fetch(anyString())).thenReturn(Optional.of(png()));
    when(avatars.findForUpdate(user)).thenReturn(Optional.of(existing()));

    importer.run(user, OAuthProvider.GOOGLE, URL);

    verify(store, never()).put(any(), any(), any());
    verify(avatars, never()).upsert(any(), any(), any(), anyInt());
  }

  @Test
  void aRefusedDownloadStoresNothing() {
    when(fetcher.fetch(anyString())).thenReturn(Optional.empty());

    importer.run(user, OAuthProvider.GOOGLE, URL);

    verify(store, never()).put(any(), any(), any());
  }

  @Test
  void aNonImageBodyStoresNothing() {
    when(fetcher.fetch(anyString())).thenReturn(Optional.of("<html>".getBytes()));

    importer.run(user, OAuthProvider.GOOGLE, URL);

    verify(store, never()).put(any(), any(), any());
    verify(avatars, never()).upsert(any(), any(), any(), anyInt());
  }

  @Test
  void aStorageOutageIsSwallowedAndLeavesNoRow() {
    when(fetcher.fetch(anyString())).thenReturn(Optional.of(png()));
    when(avatars.findForUpdate(user)).thenReturn(Optional.empty());
    doThrow(new ServiceUnavailableException("IDT-511", "down", null))
        .when(store)
        .put(anyString(), any(), anyString());

    importer.run(user, OAuthProvider.GOOGLE, URL);

    verify(avatars, never()).upsert(any(), any(), any(), anyInt());
  }

  @Test
  void aBlankPictureAndARejectedExecutorNeverThrow() {
    var rejecting =
        new OAuthAvatarImporter(
            fetcher,
            avatars,
            new OAuthAvatarSaver(store, avatars, cleanup),
            task -> {
              throw new RejectedExecutionException();
            });

    rejecting.scheduleAfterCommit(user, OAuthProvider.GOOGLE, null);
    rejecting.scheduleAfterCommit(user, OAuthProvider.GOOGLE, URL);

    verify(fetcher, never()).fetch(anyString());
  }
}
