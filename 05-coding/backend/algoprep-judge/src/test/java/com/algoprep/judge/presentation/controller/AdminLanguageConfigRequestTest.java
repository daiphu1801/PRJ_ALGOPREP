package com.algoprep.judge.presentation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.algoprep.judge.domain.exception.AdminConfigException;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

class AdminLanguageConfigRequestTest {
  private final JsonMapper json = JsonMapper.builder().build();

  private ObjectNode valid() {
    return (ObjectNode)
        json.readTree(
            """
        {"languages":[
          {"language":"JAVA","timeLimitMultiplier":2.00,"memoryLimitMultiplier":2.00,"enabled":true,"updatedAt":"2026-11-05T00:00:00Z"},
          {"language":"CPP","timeLimitMultiplier":1.00,"memoryLimitMultiplier":1.00,"enabled":true,"updatedAt":"2026-11-05T00:00:00Z"},
          {"language":"PYTHON","timeLimitMultiplier":3.00,"memoryLimitMultiplier":1.00,"enabled":true,"updatedAt":"2026-11-05T00:00:00Z"}],
        "gradingDefaults":{"timeLimitMs":1000,"memoryLimitMb":256,"maxOutputSizeKb":64,"compileTimeoutSeconds":10,"updatedAt":"2026-11-05T00:00:00Z"},
        "sandbox":{"networkAccessEnabled":false,"childProcessLimitEnabled":true,"returnStderrToStudent":true,"updatedAt":"2026-11-05T00:00:00Z","maxChildProcesses":999},
        "generatorSandbox":{"maxRuntimeSeconds":30,"maxOutputMb":100,"updatedAt":"2026-11-05T00:00:00Z"}}
        """);
  }

  private static void rejects(JsonNode request, String code) {
    assertThatThrownBy(() -> AdminLanguageConfigRequest.parse(request))
        .isInstanceOfSatisfying(
            AdminConfigException.class, e -> assertThat(e.errorCode()).isEqualTo(code));
  }

  @Test
  void strictTypesLimitsAndUnknownReadOnlyFields() {
    assertThat(AdminLanguageConfigRequest.parse(valid()).languages()).hasSize(3);
    for (String field : new String[] {"timeLimitMultiplier", "memoryLimitMultiplier"}) {
      for (String value : new String[] {"0.09", "10.00", "0.101", "null", "\"2\"", "true"}) {
        var request = valid();
        ((ObjectNode) request.path("languages").get(0)).set(field, json.readTree(value));
        rejects(request, "JDG-V10");
      }
      for (String value : new String[] {"0.10", "9.99"}) {
        var request = valid();
        ((ObjectNode) request.path("languages").get(0)).set(field, json.readTree(value));
        assertThat(AdminLanguageConfigRequest.parse(request).languages()).hasSize(3);
      }
    }
  }

  @Test
  void defaultsMustBePositiveWholeInts() {
    for (String field :
        new String[] {"timeLimitMs", "memoryLimitMb", "maxOutputSizeKb", "compileTimeoutSeconds"}) {
      for (String value : new String[] {"0", "-1", "1.5", "1.0", "2147483648", "\"1\"", "null"}) {
        var request = valid();
        ((ObjectNode) request.path("gradingDefaults")).set(field, json.readTree(value));
        rejects(request, "JDG-V11");
      }
    }
  }

  @Test
  void generatorBoundsAndRequiredBooleansVersionsAndExactLanguageSet() {
    for (String field : new String[] {"maxRuntimeSeconds", "maxOutputMb"}) {
      int max = field.equals("maxRuntimeSeconds") ? 300 : 1024;
      for (int value : new int[] {0, max + 1}) {
        var request = valid();
        ((ObjectNode) request.path("generatorSandbox")).put(field, value);
        rejects(request, "JDG-V12");
      }
      for (int value : new int[] {1, max}) {
        var request = valid();
        ((ObjectNode) request.path("generatorSandbox")).put(field, value);
        assertThat(AdminLanguageConfigRequest.parse(request)).isNotNull();
      }
    }
    var duplicate = valid();
    ((ObjectNode) duplicate.path("languages").get(0)).put("language", "CPP");
    rejects(duplicate, "JDG-V10");
    var missing = valid();
    missing.remove("languages");
    rejects(missing, "JDG-V10");
  }

  @Test
  void nullOrNonBooleanSandboxAndMissingVersionsFailClosed() {
    rejects(null, "JDG-V10");
    rejects(json.readTree("null"), "JDG-V10");
    rejects(json.readTree("[]"), "JDG-V10");
    for (String field :
        new String[] {
          "networkAccessEnabled", "childProcessLimitEnabled", "returnStderrToStudent"
        }) {
      var request = valid();
      ((ObjectNode) request.path("sandbox")).put(field, "true");
      rejects(request, "JDG-V10");
    }
    for (String group : new String[] {"gradingDefaults", "sandbox", "generatorSandbox"}) {
      var request = valid();
      ((ObjectNode) request.path(group)).remove("updatedAt");
      rejects(request, "JDG-V10");
    }
  }
}
