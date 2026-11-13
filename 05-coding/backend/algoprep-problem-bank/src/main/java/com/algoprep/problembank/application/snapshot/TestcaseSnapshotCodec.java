package com.algoprep.problembank.application.snapshot;

import com.algoprep.problembank.domain.exception.GradingDataException;
import com.algoprep.problembank.domain.model.SnapshotMetadata;
import com.algoprep.problembank.domain.model.TestcaseData;
import com.algoprep.problembank.domain.model.TestcaseSnapshot;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Owner-approved JSON v1. Integrity and identity are checked before exposing any content. */
@Component
public final class TestcaseSnapshotCodec {
  private static final JsonMapper JSON =
      JsonMapper.builder()
          .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
          .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
          .build();
  private static final Comparator<TestcaseData> ORDER =
      Comparator.comparingInt(TestcaseData::position)
          .thenComparing(row -> row.testcaseId().toString());

  public byte[] encode(UUID problemId, int version, List<TestcaseData> testcases) {
    try {
      if (problemId == null || version < 1) {
        throw unavailable();
      }
      var root = JSON.createObjectNode();
      root.put("formatVersion", 1);
      root.put("problemId", problemId.toString());
      root.put("testcaseSetVersion", version);
      var cases = root.putArray("testcases");
      for (TestcaseData row : validated(testcases)) {
        var node = cases.addObject();
        node.put("testcaseId", row.testcaseId().toString());
        node.put("visibility", row.visibility());
        node.put("position", row.position());
        node.put("inputBase64", Base64.getEncoder().encodeToString(row.input()));
        node.put("expectedOutputBase64", Base64.getEncoder().encodeToString(row.expectedOutput()));
      }
      return JSON.writeValueAsBytes(root);
    } catch (RuntimeException failure) {
      throw unavailable();
    }
  }

  public TestcaseSnapshot decode(SnapshotMetadata metadata, byte[] bytes) {
    try {
      if (metadata.problemId() == null
          || metadata.version() < 1
          || metadata.testcaseCount() < 1
          || metadata.contentSha256() == null
          || !metadata.contentSha256().matches("[a-fA-F0-9]{64}")
          || !MessageDigest.isEqual(
              HexFormat.of().parseHex(metadata.contentSha256()), digest(bytes))) {
        throw unavailable();
      }
      JsonNode root = JSON.readTree(bytes);
      fields(root, Set.of("formatVersion", "problemId", "testcaseSetVersion", "testcases"));
      if (integer(root.get("formatVersion")) != 1
          || !UUID.fromString(text(root.get("problemId"))).equals(metadata.problemId())
          || integer(root.get("testcaseSetVersion")) != metadata.version()) {
        throw unavailable();
      }
      JsonNode cases = root.get("testcases");
      if (!cases.isArray() || cases.size() != metadata.testcaseCount()) {
        throw unavailable();
      }
      List<TestcaseData> result = new ArrayList<>();
      for (JsonNode node : cases) {
        result.add(testcase(node));
      }
      return new TestcaseSnapshot(metadata.problemId(), metadata.version(), validated(result));
    } catch (RuntimeException failure) {
      throw unavailable();
    }
  }

  public String sha256(byte[] bytes) {
    return HexFormat.of().formatHex(digest(bytes));
  }

  private static byte[] digest(byte[] bytes) {
    try {
      return MessageDigest.getInstance("SHA-256").digest(bytes);
    } catch (NoSuchAlgorithmException failure) {
      throw new IllegalStateException("SHA-256 is unavailable");
    }
  }

  private static TestcaseData testcase(JsonNode node) {
    fields(
        node,
        Set.of("testcaseId", "visibility", "position", "inputBase64", "expectedOutputBase64"));
    return new TestcaseData(
        UUID.fromString(text(node.get("testcaseId"))),
        text(node.get("visibility")),
        integer(node.get("position")),
        Base64.getDecoder().decode(text(node.get("inputBase64"))),
        Base64.getDecoder().decode(text(node.get("expectedOutputBase64"))));
  }

  private static List<TestcaseData> validated(List<TestcaseData> rows) {
    if (rows == null || rows.isEmpty()) {
      throw unavailable();
    }
    Set<UUID> ids = new HashSet<>();
    for (TestcaseData row : rows) {
      if (row == null
          || row.testcaseId() == null
          || !ids.add(row.testcaseId())
          || row.position() < 1
          || !("SAMPLE".equals(row.visibility()) || "HIDDEN".equals(row.visibility()))) {
        throw unavailable();
      }
    }
    return rows.stream().sorted(ORDER).toList();
  }

  private static void fields(JsonNode node, Set<String> names) {
    if (node == null
        || !node.isObject()
        || node.size() != names.size()
        || !names.stream().allMatch(node::has)) {
      throw unavailable();
    }
  }

  private static String text(JsonNode node) {
    if (node == null || !node.isTextual()) {
      throw unavailable();
    }
    return node.asText();
  }

  private static int integer(JsonNode node) {
    if (node == null || !node.isIntegralNumber() || !node.canConvertToInt()) {
      throw unavailable();
    }
    return node.intValue();
  }

  private static GradingDataException unavailable() {
    return new GradingDataException(GradingDataException.Kind.SPEC_UNAVAILABLE);
  }
}
