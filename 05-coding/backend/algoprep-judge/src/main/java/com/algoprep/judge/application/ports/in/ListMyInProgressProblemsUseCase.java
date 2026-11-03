package com.algoprep.judge.application.ports.in;

import com.algoprep.judge.application.dto.LearnerListLimit;
import com.algoprep.judge.application.dto.LearnerSubmissionLists.InProgressItem;
import java.util.List;

public interface ListMyInProgressProblemsUseCase {
  List<InProgressItem> inProgress(LearnerListLimit limit);
}
