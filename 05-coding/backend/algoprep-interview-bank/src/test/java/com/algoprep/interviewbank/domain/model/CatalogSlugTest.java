package com.algoprep.interviewbank.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CatalogSlugTest {
  @Test
  void stripsAccentsMapsDAndJoinsWithUnderscores() {
    assertThat(CatalogSlug.create("Mạng máy tính", CatalogKind.TOPIC, 1))
        .isEqualTo("MANG_MAY_TINH");
    assertThat(CatalogSlug.create("Đồ thị & cây", CatalogKind.TOPIC, 1)).isEqualTo("DO_THI_CAY");
    assertThat(CatalogSlug.create("Rất khó", CatalogKind.LEVEL, 1)).isEqualTo("RAT_KHO");
  }

  @Test
  void symbolOnlyNamesFallBackToTheKindName() {
    assertThat(CatalogSlug.create("???", CatalogKind.TOPIC, 1)).isEqualTo("TOPIC");
    assertThat(CatalogSlug.create("???", CatalogKind.LEVEL, 1)).isEqualTo("LEVEL");
  }

  @Test
  void collisionAttemptsAppendNumericSuffixesFromTwo() {
    assertThat(CatalogSlug.create("Database", CatalogKind.TOPIC, 1)).isEqualTo("DATABASE");
    assertThat(CatalogSlug.create("Database", CatalogKind.TOPIC, 2)).isEqualTo("DATABASE_2");
    assertThat(CatalogSlug.create("Database", CatalogKind.TOPIC, 20)).isEqualTo("DATABASE_20");
  }

  @Test
  void baseIsCutAtSixtyAndSuffixedCodeNeverExceedsSixtyFour() {
    String longName = "a".repeat(60);
    assertThat(CatalogSlug.create(longName, CatalogKind.TOPIC, 1)).hasSize(60);
    assertThat(CatalogSlug.create(longName, CatalogKind.TOPIC, 12)).hasSize(63).endsWith("_12");
    // A cut that lands on an underscore drops it, so the suffix never doubles it.
    String cutOnSeparator = "a".repeat(59) + " b";
    assertThat(CatalogSlug.create(cutOnSeparator, CatalogKind.TOPIC, 1)).isEqualTo("A".repeat(59));
  }
}
