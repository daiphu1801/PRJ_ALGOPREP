package com.algoprep.judge.application.dto;

import com.algoprep.judge.domain.model.AdminLanguageConfig;
import java.util.LinkedHashMap;
import java.util.Map;

/** Writable fields only, excluding versions, compiler labels and pending job counts. */
public final class AdminConfigFields {
  private AdminConfigFields() {}

  public static Map<String, Object> values(AdminLanguageConfig config) {
    Map<String, Object> result = new LinkedHashMap<>();
    for (var language : config.languages()) {
      String prefix = "languages." + language.language() + ".";
      result.put(
          prefix + "timeLimitMultiplier", language.timeLimitMultiplier().stripTrailingZeros());
      result.put(
          prefix + "memoryLimitMultiplier", language.memoryLimitMultiplier().stripTrailingZeros());
      result.put(prefix + "enabled", language.enabled());
    }
    var defaults = config.gradingDefaults();
    result.put("gradingDefaults.timeLimitMs", defaults.timeLimitMs());
    result.put("gradingDefaults.memoryLimitMb", defaults.memoryLimitMb());
    result.put("gradingDefaults.maxOutputSizeKb", defaults.maxOutputSizeKb());
    result.put("gradingDefaults.compileTimeoutSeconds", defaults.compileTimeoutSeconds());
    var sandbox = config.sandbox();
    result.put("sandbox.networkAccessEnabled", sandbox.networkAccessEnabled());
    result.put("sandbox.childProcessLimitEnabled", sandbox.childProcessLimitEnabled());
    result.put("sandbox.returnStderrToStudent", sandbox.returnStderrToStudent());
    result.put("generatorSandbox.maxRuntimeSeconds", config.generatorSandbox().maxRuntimeSeconds());
    result.put("generatorSandbox.maxOutputMb", config.generatorSandbox().maxOutputMb());
    return result;
  }
}
