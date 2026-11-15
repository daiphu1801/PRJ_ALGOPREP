package com.algoprep.judge.application.ports.in.read;

import com.algoprep.judge.application.dto.read.LearnerListLimit;
import com.algoprep.judge.application.dto.read.LearnerSubmissionLists.AcceptedItem;
import java.util.List;

public interface ListMyAcceptedSubmissionsUseCase {
  List<AcceptedItem> accepted(LearnerListLimit limit);
}
