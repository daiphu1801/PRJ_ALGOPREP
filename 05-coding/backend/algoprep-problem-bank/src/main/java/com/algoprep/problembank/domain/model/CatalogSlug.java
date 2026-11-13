package com.algoprep.problembank.domain.model;

import java.text.Normalizer;
import java.util.Locale;

/** DD BR-03: stable ASCII slug with a bounded collision suffix. */
public final class CatalogSlug {
  private CatalogSlug() {}

  public static String create(String name, CatalogKind kind, int attempt) {
    String value =
        Normalizer.normalize(name, Normalizer.Form.NFD)
            .replaceAll("\\p{M}+", "")
            .replace('đ', 'd')
            .replace('Đ', 'D')
            .toUpperCase(Locale.ROOT)
            .replaceAll("[^A-Z0-9]+", "-")
            .replaceAll("^-|-$", "");
    if (value.isEmpty()) {
      value = kind.name();
    }
    String suffix = attempt == 1 ? "" : "-" + attempt;
    return value.substring(0, Math.min(value.length(), 64 - suffix.length())).replaceAll("-$", "")
        + suffix;
  }
}
