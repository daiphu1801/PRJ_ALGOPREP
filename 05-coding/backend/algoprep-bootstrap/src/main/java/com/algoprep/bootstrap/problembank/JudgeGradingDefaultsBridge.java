package com.algoprep.bootstrap.problembank;

import com.algoprep.judge.application.ports.in.ReadGradingDefaultsUseCase;
import com.algoprep.problembank.application.dto.JudgeGradingDefaults;
import com.algoprep.problembank.application.ports.out.JudgeConfigReadPort;
import org.springframework.stereotype.Component;

/** Cross-module translation stays in bootstrap; F2 never queries the judge schema. */
@Component
public final class JudgeGradingDefaultsBridge implements JudgeConfigReadPort {
  private final ReadGradingDefaultsUseCase judge;

  public JudgeGradingDefaultsBridge(ReadGradingDefaultsUseCase judge) {
    this.judge = judge;
  }

  @Override
  public JudgeGradingDefaults getGradingDefaults() {
    var defaults = judge.getGradingDefaults();
    return new JudgeGradingDefaults(
        defaults.timeLimitMs(),
        defaults.memoryLimitMb(),
        defaults.maxOutputSizeKb(),
        defaults.compileTimeoutSeconds());
  }
}
