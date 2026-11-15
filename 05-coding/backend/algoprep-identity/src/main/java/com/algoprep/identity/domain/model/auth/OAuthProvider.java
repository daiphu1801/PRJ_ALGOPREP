package com.algoprep.identity.domain.model;

import java.util.Locale;
import java.util.Optional;

/** The two sign-in providers of DD F1 section 0.1 (#5, #6). */
public enum OAuthProvider {
  GITHUB,
  GOOGLE;

  /** Path segment to provider: {@code github} or {@code google}, any case. */
  public static Optional<OAuthProvider> fromPath(String segment) {
    if (segment == null) {
      return Optional.empty();
    }
    return switch (segment.toLowerCase(Locale.ROOT)) {
      case "github" -> Optional.of(GITHUB);
      case "google" -> Optional.of(GOOGLE);
      default -> Optional.empty();
    };
  }

  /** Lower-case name used in URLs and configuration keys. */
  public String path() {
    return name().toLowerCase(Locale.ROOT);
  }
}
