package com.algoprep.problembank.application.snapshot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.algoprep.problembank.domain.exception.GradingDataException;
import com.algoprep.problembank.domain.model.SnapshotMetadata;
import com.algoprep.problembank.domain.model.TestcaseData;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

class TestcaseSnapshotCodecTest {
  private final TestcaseSnapshotCodec codec = new TestcaseSnapshotCodec();
  private final UUID id = UUID.randomUUID();
  private final UUID testcaseId = UUID.randomUUID();

  private TestcaseData testcase() {
    return new TestcaseData(
        testcaseId,
        "HIDDEN",
        3,
        new byte[] {0, -1, 13, 10},
        "bí mật\n".getBytes(StandardCharsets.UTF_8));
  }

  private SnapshotMetadata metadata(byte[] bytes) {
    return new SnapshotMetadata(
        id, 7, "problems/" + id + "/snapshots/7.json", 1, codec.sha256(bytes));
  }

  @Test
  void deterministicVersionedJsonRoundTripsArbitraryBytesWithoutTextNormalization() {
    byte[] bytes = codec.encode(id, 7, List.of(testcase()));
    assertThat(codec.encode(id, 7, List.of(testcase()))).containsExactly(bytes);
    var result = codec.decode(metadata(bytes), bytes);
    assertThat(result.problemId()).isEqualTo(id);
    assertThat(result.version()).isEqualTo(7);
    assertThat(result.testcases().getFirst().input()).containsExactly(0, -1, 13, 10);
    assertThat(result.testcases().getFirst().expectedOutput())
        .containsExactly(testcase().expectedOutput());
    assertThat(new String(bytes, StandardCharsets.UTF_8))
        .contains("\"formatVersion\":1", "inputBase64")
        .doesNotContain("bí mật");
  }

  @Test
  void sortsByPositionThenIdentifierWithoutRejectingEqualPositions() {
    UUID low = UUID.fromString("00000000-0000-4000-8000-000000000001");
    UUID high = UUID.fromString("00000000-0000-4000-8000-000000000002");
    var first = new TestcaseData(low, "SAMPLE", 1, new byte[0], new byte[0]);
    var second = new TestcaseData(high, "HIDDEN", 1, new byte[0], new byte[0]);
    byte[] bytes = codec.encode(id, 7, List.of(second, testcase(), first));
    var meta = new SnapshotMetadata(id, 7, "unused", 3, codec.sha256(bytes));
    assertThat(codec.decode(meta, bytes).testcases())
        .extracting(TestcaseData::testcaseId)
        .containsExactly(low, high, testcaseId);
  }

  @Test
  void verifiesObjectChecksumBeforeTrustingContent() {
    byte[] bytes = codec.encode(id, 7, List.of(testcase()));
    var metadata = metadata(bytes);
    bytes[bytes.length - 1] ^= 1;
    unavailable(() -> codec.decode(metadata, bytes));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "VERSION",
        "PROBLEM",
        "FORMAT",
        "COUNT",
        "UUID",
        "VISIBILITY",
        "POSITION",
        "BASE64",
        "MISSING",
        "EXTRA",
        "TYPE",
        "DUPLICATE_ID",
        "DUPLICATE_FIELD",
        "TRAILING",
        "NOT_JSON"
      })
  void rejectsMalformedButCorrectlyChecksummedBundles(String rule) {
    byte[] original = codec.encode(id, 7, List.of(testcase()));
    var json = JsonMapper.builder().build();
    var root = (ObjectNode) json.readTree(original);
    var row = (ObjectNode) root.get("testcases").get(0);
    switch (rule) {
      case "VERSION" -> root.put("testcaseSetVersion", 8);
      case "PROBLEM" -> root.put("problemId", UUID.randomUUID().toString());
      case "FORMAT" -> root.put("formatVersion", 2);
      case "COUNT" -> root.putArray("testcases");
      case "UUID" -> row.put("testcaseId", "invalid");
      case "VISIBILITY" -> row.put("visibility", "PRIVATE");
      case "POSITION" -> row.put("position", 0);
      case "BASE64" -> row.put("inputBase64", "!secret!");
      case "MISSING" -> row.remove("expectedOutputBase64");
      case "EXTRA" -> row.put("sourceCode", "private answer");
      case "TYPE" -> row.put("position", "3");
      case "DUPLICATE_ID" -> root.withArray("testcases").add(row.deepCopy());
      default -> {}
    }
    String text = json.writeValueAsString(root);
    if (rule.equals("DUPLICATE_FIELD")) {
      text = text.replace("\"formatVersion\":1", "\"formatVersion\":1,\"formatVersion\":1");
    } else if (rule.equals("TRAILING")) {
      text += " {}";
    } else if (rule.equals("NOT_JSON")) {
      text = "secret bytes";
    }
    byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
    var meta =
        new SnapshotMetadata(
            id, 7, "unused", rule.equals("DUPLICATE_ID") ? 2 : 1, codec.sha256(bytes));
    unavailable(() -> codec.decode(meta, bytes));
  }

  @Test
  void rejectsInvalidMetadataAndEmptyOrDuplicateWriterInput() {
    byte[] bytes = codec.encode(id, 7, List.of(testcase()));
    unavailable(() -> codec.decode(new SnapshotMetadata(id, 7, "unused", 1, "invalid"), bytes));
    unavailable(
        () -> codec.decode(new SnapshotMetadata(id, 7, "unused", 0, codec.sha256(bytes)), bytes));
    unavailable(() -> codec.encode(null, 7, List.of(testcase())));
    unavailable(() -> codec.encode(id, 0, List.of(testcase())));
    unavailable(() -> codec.encode(id, 7, List.of()));
    unavailable(() -> codec.encode(id, 7, List.of(testcase(), testcase())));
  }

  @Test
  void byteArraysAndCollectionsCannotBeMutatedThroughTheReadResult() {
    var value = testcase();
    byte[] bytes = value.input();
    bytes[0] = 42;
    assertThat(value.input()[0]).isZero();
    assertThat(value.toString()).doesNotContain("bí mật", "input=", "expectedOutput=");
    var encoded = codec.encode(id, 7, List.of(value));
    var result = codec.decode(metadata(encoded), encoded);
    assertThatThrownBy(() -> result.testcases().clear())
        .isInstanceOf(UnsupportedOperationException.class);
  }

  private static void unavailable(Runnable action) {
    assertThatThrownBy(action::run)
        .isInstanceOfSatisfying(
            GradingDataException.class,
            failure -> {
              assertThat(failure.kind()).isEqualTo(GradingDataException.Kind.SPEC_UNAVAILABLE);
              assertThat(failure.getCause()).isNull();
              assertThat(failure.getMessage()).isEqualTo("Problem grading data is unavailable");
            });
  }
}
