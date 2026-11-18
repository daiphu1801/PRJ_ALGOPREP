package com.algoprep.identity.domain.model.avatar;

import com.algoprep.identity.domain.model.auth.OAuthProvider;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Constants and the URL rewrite of the OAuth avatar import (DD logic F1 BR-28, BD security F1
 * section 6.5). The fetcher enforces the host list; this class only knows it.
 */
public final class AvatarImportRules {
  public static final String GOOGLE_HOST = "lh3.googleusercontent.com";
  public static final String GITHUB_HOST = "avatars.githubusercontent.com";
  public static final Set<String> ALLOWED_HOSTS = Set.of(GOOGLE_HOST, GITHUB_HOST);
  public static final int MAX_BYTES = 524_288;
  public static final int CONNECT_TIMEOUT_MILLIS = 3_000;
  public static final int READ_TIMEOUT_MILLIS = 5_000;

  private static final Pattern GOOGLE_SIZE = Pattern.compile("=s[0-9]+(-c)?$");
  private static final Pattern GITHUB_SIZE = Pattern.compile("([?&])s=[^&]*&?");

  private AvatarImportRules() {}

  /**
   * Asks the provider for the 256 pixel version: Google gets {@code =s256-c} (replacing a size
   * suffix), GitHub gets {@code s=256} in the query. Empty for a blank URL.
   */
  public static Optional<String> normalise(OAuthProvider provider, String url) {
    if (url == null || url.isBlank()) {
      return Optional.empty();
    }
    String trimmed = url.trim();
    if (provider == OAuthProvider.GOOGLE) {
      var matcher = GOOGLE_SIZE.matcher(trimmed);
      return Optional.of(matcher.find() ? matcher.replaceFirst("=s256-c") : trimmed + "=s256-c");
    }
    String stripped = GITHUB_SIZE.matcher(trimmed).replaceAll("$1");
    if (stripped.endsWith("?") || stripped.endsWith("&")) {
      return Optional.of(stripped + "s=256");
    }
    return Optional.of(stripped + (stripped.contains("?") ? "&" : "?") + "s=256");
  }
}
