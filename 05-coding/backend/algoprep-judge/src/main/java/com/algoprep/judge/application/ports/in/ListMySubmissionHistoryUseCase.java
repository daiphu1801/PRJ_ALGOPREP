package com.algoprep.judge.application.ports.in;

import com.algoprep.judge.application.dto.SubmissionReads.Filters;
import com.algoprep.judge.application.dto.SubmissionReads.History;
import com.algoprep.judge.application.dto.SubmissionReads.Page;
import java.util.UUID;

/** Authorized read contract from F4 API sections 7-10 and 13-15. */
public interface ListMySubmissionHistoryUseCase {
  Page<History> history(UUID owner, Filters filters);
}
