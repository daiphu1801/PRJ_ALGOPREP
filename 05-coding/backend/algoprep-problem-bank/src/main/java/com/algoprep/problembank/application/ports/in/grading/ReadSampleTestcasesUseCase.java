package com.algoprep.problembank.application.ports.in;

import com.algoprep.problembank.domain.model.TestcaseData;
import java.util.List;
import java.util.UUID;

public interface ReadSampleTestcasesUseCase {
  List<TestcaseData> loadSampleTestcases(UUID problemId);
}
