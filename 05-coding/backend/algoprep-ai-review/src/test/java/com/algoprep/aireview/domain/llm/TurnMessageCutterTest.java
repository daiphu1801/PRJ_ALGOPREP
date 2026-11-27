package com.algoprep.aireview.domain.llm;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/** The cutter must agree with a full parse of the same reply, however the chunks are split. */
class TurnMessageCutterTest {
  private static final JsonMapper JSON = JsonMapper.builder().build();

  private static final List<String> MESSAGES =
      List.of(
          "Plain sentence.",
          "Quote \" inside and a backslash \\ and a slash / here",
          "Line one\nLine two\ttabbed\r\nend",
          "Braces { } and [ ] inside a string, even \"stageComplete\": true",
          "Tiếng Việt có dấu: Giải trình độ phức tạp",
          "Emoji pair \uD83D\uDE00 in the middle",
          "",
          "x".repeat(20_000));

  /** All chunks fed, the concatenated output, in order. */
  private static String run(TurnMessageCutter cutter, List<String> chunks) {
    StringBuilder out = new StringBuilder();
    chunks.forEach(c -> out.append(cutter.feed(c)));
    return out.toString();
  }

  private static List<String> splitAt(String raw, int position) {
    return List.of(raw.substring(0, position), raw.substring(position));
  }

  private static String oracle(String raw) {
    return JSON.readTree(raw).get("message").asString();
  }

  @Test
  void splitAtEveryPositionEqualsTheFullParse() {
    for (String message : MESSAGES) {
      if (message.length() > 200) {
        continue;
      }
      String raw = "{\"message\":" + JSON.writeValueAsString(message) + ",\"stageComplete\":true}";
      for (int at = 0; at <= raw.length(); at++) {
        TurnMessageCutter cutter = new TurnMessageCutter();
        assertThat(run(cutter, splitAt(raw, at))).as("split %d of %s", at, raw).isEqualTo(message);
        assertThat(cutter.streamedAnything()).isEqualTo(!message.isEmpty());
      }
    }
  }

  @Test
  void oneCharacterPerChunkEqualsTheFullParse() {
    for (String message : MESSAGES) {
      String raw = "{\"message\":" + JSON.writeValueAsString(message) + ",\"stageComplete\":false}";
      List<String> chunks = new ArrayList<>();
      raw.chars().forEach(c -> chunks.add(String.valueOf((char) c)));
      assertThat(run(new TurnMessageCutter(), chunks)).isEqualTo(oracle(raw));
    }
  }

  @Test
  void unicodeEscapesAndSurrogatePairsSplitAcrossChunks() {
    String raw = "{\"message\":\"caf\\u00e9 \\ud83d\\ude00 \\u0041\\n\\\"q\\\" \\/ \\\\\"}";
    String expected = oracle(raw);
    for (int at = 0; at <= raw.length(); at++) {
      assertThat(run(new TurnMessageCutter(), splitAt(raw, at))).isEqualTo(expected);
    }
    List<String> single = new ArrayList<>();
    raw.chars().forEach(c -> single.add(String.valueOf((char) c)));
    assertThat(run(new TurnMessageCutter(), single)).isEqualTo(expected);
  }

  @Test
  void aSplitSurrogatePairNeverLeavesAsALoneHalf() {
    TurnMessageCutter cutter = new TurnMessageCutter();
    String first = cutter.feed("{\"message\":\"a\\ud83d");
    assertThat(first).isEqualTo("a");
    assertThat(cutter.feed("\\ude00b\"}")).isEqualTo("\uD83D\uDE00b");
  }

  @Test
  void whitespaceAndACodeFenceBeforeTheObjectAreTolerated() {
    String raw = "```json\n{ \n \"message\" \n : \n \"Hi there\", \"stageComplete\": false }\n```";
    assertThat(run(new TurnMessageCutter(), List.of(raw))).isEqualTo("Hi there");
  }

  @Test
  void stageCompleteFirstDisablesStreamingSoTheCallerFallsBack() {
    String raw = "{\"stageComplete\":true,\"message\":\"Next stage question\"}";
    for (int at = 0; at <= raw.length(); at++) {
      TurnMessageCutter cutter = new TurnMessageCutter();
      assertThat(run(cutter, splitAt(raw, at))).isEmpty();
      assertThat(cutter.streamedAnything()).isFalse();
    }
  }

  @Test
  void rawStageCompleteTextNeverComesOut() {
    String raw = "{\"message\":\"Fine.\",\"stageComplete\":true}";
    assertThat(run(new TurnMessageCutter(), List.of(raw))).isEqualTo("Fine.");
  }

  @Test
  void missingMessageAndJunkProduceNothing() {
    for (String raw :
        List.of(
            "{\"other\":\"x\"}",
            "{}",
            "not json at all",
            "[\"message\"]",
            "{\"message\" \"no colon\"}",
            "{\"message\": 42}",
            "{\"messages\":\"x\"}",
            "")) {
      TurnMessageCutter cutter = new TurnMessageCutter();
      assertThat(run(cutter, List.of(raw))).as(raw).isEmpty();
      assertThat(cutter.streamedAnything()).isFalse();
    }
  }

  @Test
  void aTruncatedReplyEmitsOnlyWhatArrived() {
    TurnMessageCutter cutter = new TurnMessageCutter();
    assertThat(run(cutter, List.of("{\"message\":\"Half a sent"))).isEqualTo("Half a sent");
    assertThat(cutter.streamedAnything()).isTrue();
  }

  @Test
  void anInvalidUnicodeEscapeStopsStreamingWithoutThrowing() {
    TurnMessageCutter cutter = new TurnMessageCutter();
    assertThat(run(cutter, List.of("{\"message\":\"ok \\u00zz tail\"}"))).isEqualTo("ok ");
    assertThat(cutter.feed("more")).isEmpty();
  }

  @Test
  void charactersAfterTheClosingQuoteAreIgnoredAndNullOrEmptyChunksAreNoOps() {
    TurnMessageCutter cutter = new TurnMessageCutter();
    assertThat(cutter.feed(null)).isEmpty();
    assertThat(cutter.feed("")).isEmpty();
    assertThat(cutter.feed("{\"message\":\"done\"")).isEqualTo("done");
    assertThat(cutter.feed(",\"stageComplete\":true}")).isEmpty();
    assertThat(cutter.feed("\"message\":\"again\"")).isEmpty();
  }

  @Test
  void aVeryLongMessageIsFullyRecoveredInSmallChunks() {
    String message = MESSAGES.get(MESSAGES.size() - 1);
    String raw = "{\"message\":" + JSON.writeValueAsString(message) + ",\"stageComplete\":false}";
    List<String> chunks = new ArrayList<>();
    for (int i = 0; i < raw.length(); i += 7) {
      chunks.add(raw.substring(i, Math.min(raw.length(), i + 7)));
    }
    assertThat(run(new TurnMessageCutter(), chunks)).isEqualTo(message);
  }
}
