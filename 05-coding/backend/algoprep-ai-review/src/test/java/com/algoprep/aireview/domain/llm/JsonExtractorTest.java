package com.algoprep.aireview.domain.llm;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class JsonExtractorTest {
  @Test
  void barePlainObjectIsReturnedUnchanged() {
    assertThat(JsonExtractor.firstJsonObject("{\"a\":1}")).contains("{\"a\":1}");
  }

  @Test
  void codeFencesAreStripped() {
    String raw = "```json\n{\"verdict\":\"OPTIMAL\"}\n```";
    assertThat(JsonExtractor.firstJsonObject(raw)).contains("{\"verdict\":\"OPTIMAL\"}");
  }

  @Test
  void proseBeforeAndAfterTheObjectIsDropped() {
    String raw = "Sure! Here is the review: {\"a\":{\"b\":2}} Hope it helps {not json";
    assertThat(JsonExtractor.firstJsonObject(raw)).contains("{\"a\":{\"b\":2}}");
  }

  @Test
  void bracesAndEscapedQuotesInsideStringsDoNotBreakBalancing() {
    String json = "{\"note\":\"use } and { and \\\" quote\",\"n\":1}";
    assertThat(JsonExtractor.firstJsonObject("text " + json + " tail")).contains(json);
  }

  @Test
  void strayOpeningBraceInProseIsSkipped() {
    String raw = "set {x then {\"ok\":true}";
    assertThat(JsonExtractor.firstJsonObject(raw)).contains("{\"ok\":true}");
  }

  @Test
  void truncatedJsonYieldsNothing() {
    assertThat(JsonExtractor.firstJsonObject("{\"a\":{\"b\":2}")).isEmpty();
    assertThat(JsonExtractor.firstJsonObject("{\"a\":\"unterminated")).isEmpty();
  }

  @Test
  void noObjectOrNullYieldsNothing() {
    assertThat(JsonExtractor.firstJsonObject("just text")).isEmpty();
    assertThat(JsonExtractor.firstJsonObject("[1,2]")).isEmpty();
    assertThat(JsonExtractor.firstJsonObject(null)).isEmpty();
  }
}
