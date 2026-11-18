package com.algoprep.judge.presentation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.algoprep.judge.domain.exception.AdminConfigException;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

class AdminQueueRequestTest {
  private final JsonMapper json = JsonMapper.builder().build();

  @Test
  void bothFlagFieldsMustBeActualBooleans() {
    var good = AdminQueueRequest.body("{\"paused\":false,\"expectedPaused\":true}", "JDG-V16");
    assertThat(AdminQueueRequest.bool(good, "paused")).isFalse();
    assertThat(AdminQueueRequest.bool(good, "expectedPaused")).isTrue();
    for (String value : List.of("null", "0", "1", "\"true\"", "[]", "{}")) {
      var body = AdminQueueRequest.body("{\"enabled\":" + value + "}", "JDG-V16");
      assertThatThrownBy(() -> AdminQueueRequest.bool(body, "enabled"))
          .isInstanceOfSatisfying(
              AdminConfigException.class, e -> assertThat(e.errorCode()).isEqualTo("JDG-V16"));
    }
    assertThatThrownBy(() -> AdminQueueRequest.bool(good, "enabled"))
        .isInstanceOf(AdminConfigException.class);
  }

  @Test
  void malformedEmptyAndTrailingBodiesUseTheEndpointCode() {
    for (String code : List.of("JDG-V09", "JDG-V16")) {
      assertThatThrownBy(() -> AdminQueueRequest.body(null, code))
          .isInstanceOf(AdminConfigException.class);
      for (String body : List.of("", " ", "null", "[]", "true", "{", "{} {}")) {
        assertThatThrownBy(() -> AdminQueueRequest.body(body, code))
            .isInstanceOfSatisfying(
                AdminConfigException.class, e -> assertThat(e.errorCode()).isEqualTo(code));
      }
    }
  }

  @Test
  void everySettingRejectsNonIntsMissingValuesAndNonpositiveNumbers() {
    for (String field :
        List.of(
            "autoscaleJobThreshold", "jobsPerPage", "errorRetentionHours", "latencyCapSeconds")) {
      for (String value : List.of("0", "-1", "1.5", "1.0", "\"1\"", "null", "2147483648", "true")) {
        var body = valid();
        body.set(field, json.readTree(value));
        assertThatThrownBy(() -> AdminQueueRequest.settings(body.toString()))
            .isInstanceOfSatisfying(
                AdminConfigException.class, e -> assertThat(e.errorCode()).isEqualTo("JDG-V09"));
      }
      var missing = valid();
      missing.remove(field);
      assertThatThrownBy(() -> AdminQueueRequest.settings(missing.toString()))
          .isInstanceOf(AdminConfigException.class);
    }
  }

  @Test
  void oneLargeValuesAndTheFullIntRangeAreAcceptedAndReadonlyExtrasAreIgnored() {
    var body = valid();
    for (int value : new int[] {1, 100000, Integer.MAX_VALUE}) {
      body.put("autoscaleJobThreshold", value)
          .put("jobsPerPage", value)
          .put("errorRetentionHours", value)
          .put("latencyCapSeconds", value)
          .put("updatedAt", "ignored")
          .put("queuePaused", true);
      assertThat(AdminQueueRequest.settings(body.toString()).values().values()).containsOnly(value);
    }
  }

  private ObjectNode valid() {
    return (ObjectNode)
        json.readTree(
            "{\"autoscaleJobThreshold\":20,\"jobsPerPage\":20,\"errorRetentionHours\":24,\"latencyCapSeconds\":10}");
  }
}
