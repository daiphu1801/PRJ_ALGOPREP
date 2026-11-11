package com.algoprep.problembank.application.ports.out;

import com.algoprep.problembank.application.dto.ReferenceSolutionRun;
import com.algoprep.problembank.application.dto.TestcaseGeneration;
import java.util.List;

/**
 * The sandbox side of BR-16 (the adapter lives in bootstrap and goes through the harness and the
 * judge). Scripts are untrusted code. A sandbox that cannot answer is an unchecked exception that
 * the caller turns into 503 PRB-050; a script that misbehaves is reported in the result instead.
 */
public interface GeneratorSandboxPort {
  /** Runs the generator once with a fixed seed under the generator limits of the judge. */
  TestcaseGeneration.GeneratorRun runGenerator(String script, long seed);

  /** One flag per input: true when the validator exits 0 for it. */
  List<Boolean> validate(String script, List<String> inputs);

  /** Runs the sample solution on inputs only, in order; the expected output is unused. */
  List<TestcaseGeneration.ReferenceOutput> runReference(ReferenceSolutionRun.Request request);
}
