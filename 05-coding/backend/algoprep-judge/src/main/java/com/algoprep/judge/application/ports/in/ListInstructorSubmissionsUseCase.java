package com.algoprep.judge.application.ports.in;

import com.algoprep.judge.application.dto.SubmissionReads.Filters;
import com.algoprep.judge.application.dto.SubmissionReads.InstructorItem;
import com.algoprep.judge.application.dto.SubmissionReads.Page;
import java.util.UUID;

/** Authorized read contract from F4 API sections 7-10 and 13-15. */
public interface ListInstructorSubmissionsUseCase {
  Page<InstructorItem> list(UUID instructor, Filters filters);
}
