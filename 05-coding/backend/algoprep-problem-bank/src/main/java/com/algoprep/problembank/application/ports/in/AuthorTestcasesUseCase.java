package com.algoprep.problembank.application.ports.in;

import com.algoprep.problembank.application.dto.TestcaseCommands;
import com.algoprep.problembank.application.dto.TestcaseMutationResults;

/**
 * Inline testcase authoring of DD F2 sections 42 to 45. One interface because the four operations
 * share the problem lock, the author rule and the grading-set versioning.
 */
public interface AuthorTestcasesUseCase {
  TestcaseMutationResults.Saved create(TestcaseCommands.Create command);

  TestcaseMutationResults.Saved update(TestcaseCommands.Update command);

  TestcaseMutationResults.Removed delete(TestcaseCommands.Delete command);

  TestcaseMutationResults.Ordered reorder(TestcaseCommands.Reorder command);
}
