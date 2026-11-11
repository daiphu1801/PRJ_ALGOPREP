package com.algoprep.interviewbank.domain.model;

import java.text.Normalizer;
import java.util.Locale;

/** DD logic BR-IB-01: stable upper-case slug with underscores and a bounded numeric suffix. */
public final class CatalogSlug {
  private static final int MAX_LENGTH = 64;
  private static final int MAX_BASE = 60;

  private CatalogSlug() {}

  /**
   * Builds the candidate code for the given collision attempt (1 = no suffix, 2 = {@code _2}...).
   */
  public static String create(String displayName, CatalogKind kind, int attempt) {
    String base =
        Normalizer.normalize(displayName, Normalizer.Form.NFD)
            .replaceAll("\\p{M}+", "")
            .replace('đ', 'd')
            .replace('Đ', 'D')
            .toUpperCase(Locale.ROOT)
            .replaceAll("[^A-Z0-9]+", "_")
            .replaceAll("^_+|_+$", "");
    if (base.isEmpty()) {
      base = kind.name();
    }
    if (base.length() > MAX_BASE) {
      base = base.substring(0, MAX_BASE).replaceAll("_+$", "");
    }
    String suffix = attempt <= 1 ? "" : "_" + attempt;
    return base.substring(0, Math.min(base.length(), MAX_LENGTH - suffix.length())) + suffix;
  }
}
