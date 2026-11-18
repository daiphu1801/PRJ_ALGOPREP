package com.algoprep.identity.application.command.avatar;

import com.algoprep.identity.application.ports.in.avatar.ImportOAuthAvatarUseCase;
import com.algoprep.identity.application.ports.out.AvatarImageFetchPort;
import com.algoprep.identity.domain.model.auth.OAuthProvider;
import com.algoprep.identity.domain.model.avatar.AvatarImage;
import com.algoprep.identity.domain.model.avatar.AvatarImportRules;
import com.algoprep.identity.domain.ports.out.UserAvatarRepository;
import java.net.URI;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * BR-28. The work starts after the login transaction has committed (the new user row must exist for
 * the foreign key) and runs on its own virtual thread, so the login answer never waits for the
 * provider. Every failure ends in one WARN line that names the provider host only.
 */
@Service
class OAuthAvatarImporter implements ImportOAuthAvatarUseCase {
  private static final Logger LOG = LoggerFactory.getLogger(OAuthAvatarImporter.class);

  private final AvatarImageFetchPort fetcher;
  private final UserAvatarRepository avatars;
  private final OAuthAvatarSaver saver;
  private final Executor executor;

  @Autowired
  OAuthAvatarImporter(
      AvatarImageFetchPort fetcher, UserAvatarRepository avatars, OAuthAvatarSaver saver) {
    this(
        fetcher,
        avatars,
        saver,
        task -> Thread.ofVirtual().name("oauth-avatar-import").start(task));
  }

  OAuthAvatarImporter(
      AvatarImageFetchPort fetcher,
      UserAvatarRepository avatars,
      OAuthAvatarSaver saver,
      Executor executor) {
    this.fetcher = fetcher;
    this.avatars = avatars;
    this.saver = saver;
    this.executor = executor;
  }

  @Override
  public void scheduleAfterCommit(UUID userId, OAuthProvider provider, String pictureUrl) {
    if (pictureUrl == null || pictureUrl.isBlank()) {
      return;
    }
    Runnable start = () -> submit(userId, provider, pictureUrl);
    if (!TransactionSynchronizationManager.isSynchronizationActive()) {
      start.run();
      return;
    }
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCommit() {
            start.run();
          }
        });
  }

  private void submit(UUID userId, OAuthProvider provider, String pictureUrl) {
    try {
      executor.execute(() -> run(userId, provider, pictureUrl));
    } catch (RuntimeException rejected) {
      LOG.warn("OAuth avatar import not started for provider {}", provider);
    }
  }

  void run(UUID userId, OAuthProvider provider, String pictureUrl) {
    String host = hostOf(pictureUrl);
    try {
      if (avatars.findReadable(userId).isPresent()) {
        return;
      }
      Optional<String> url = AvatarImportRules.normalise(provider, pictureUrl);
      Optional<byte[]> data = url.flatMap(fetcher::fetch);
      if (data.isEmpty()) {
        LOG.warn("OAuth avatar not imported: download refused or failed, host {}", host);
        return;
      }
      AvatarImage image = AvatarImageInspector.inspect(data.get());
      saver.saveIfAbsent(userId, data.get(), image);
    } catch (RuntimeException failure) {
      LOG.warn("OAuth avatar not imported: {}, host {}", failure.getClass().getSimpleName(), host);
    }
  }

  private static String hostOf(String url) {
    try {
      String host = URI.create(url.trim()).getHost();
      return host == null ? "unknown" : host;
    } catch (IllegalArgumentException malformed) {
      return "unknown";
    }
  }
}
