package com.algoprep.judge.application.ports.in;

import com.algoprep.judge.application.dto.LearnerListLimit;
import com.algoprep.judge.application.dto.LearnerSubmissionLists.AcceptedItem;
import java.util.List;

public interface ListMyAcceptedSubmissionsUseCase {
  List<AcceptedItem> accepted(LearnerListLimit limit);
}
