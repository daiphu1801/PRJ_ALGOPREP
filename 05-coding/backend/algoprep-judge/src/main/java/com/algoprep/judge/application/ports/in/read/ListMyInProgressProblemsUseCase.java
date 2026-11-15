package com.algoprep.judge.application.ports.in.read;

import com.algoprep.judge.application.dto.read.LearnerListLimit;
import com.algoprep.judge.application.dto.read.LearnerSubmissionLists.InProgressItem;
import java.util.List;

public interface ListMyInProgressProblemsUseCase {
  List<InProgressItem> inProgress(LearnerListLimit limit);
}
