package com.algoprep.problembank.application.ports.out;

import com.algoprep.problembank.application.dto.ReferenceSolutionRun;

/**
 * Runs the reference solution of a problem that may still be unpublished against its approved
 * testcases. The adapter lives in bootstrap and goes through the harness (code wrapping) and the
 * judge (sandbox), the same path a learner's Run Code takes. Any failure of that path is an
 * unchecked exception: the caller turns it into 503 PRB-050 and changes nothing.
 */
public interface SampleSolutionRunnerPort {
  ReferenceSolutionRun.Result run(ReferenceSolutionRun.Request request);
}
