package com.algoprep.problembank.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ExportedCsvTest {

  @Test
  void equalityAndHashCodeFollowTheByteContent() {
    var a = new ExportedCsv("p.csv", new byte[] {1, 2});
    assertThat(a).isEqualTo(new ExportedCsv("p.csv", new byte[] {1, 2}));
    assertThat(a).hasSameHashCodeAs(new ExportedCsv("p.csv", new byte[] {1, 2}));
    assertThat(a).isNotEqualTo(new ExportedCsv("p.csv", new byte[] {1, 3}));
    assertThat(a).isNotEqualTo(new ExportedCsv("q.csv", new byte[] {1, 2}));
  }

  @Test
  void toStringShowsTheSizeNotTheBytes() {
    assertThat(new ExportedCsv("p.csv", new byte[] {1, 2}).toString())
        .isEqualTo("ExportedCsv[filename=p.csv, bytes=2]");
  }
}
