package com.algoprep.judge.application.ports.out;

import com.algoprep.judge.application.dto.ExecutionModels.CompileReport;
import com.algoprep.judge.application.dto.ExecutionModels.Match;
import com.algoprep.judge.application.dto.ExecutionModels.Matching;
import com.algoprep.judge.application.dto.ExecutionModels.Program;

/** Caller-owned boundary implemented by bootstrap; no business-module imports. */
public interface HarnessEvaluationPort {
  CompileReport mapCompile(Program program, String compilerOutput);

  Match match(Program program, Matching matching, String stdout, String expected);
}
