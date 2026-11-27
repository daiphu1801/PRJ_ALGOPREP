package com.algoprep.judge.application.ports.in.read;

import com.algoprep.judge.application.dto.read.SubmissionReads.Filters;
import com.algoprep.judge.application.dto.read.SubmissionReads.InstructorItem;
import com.algoprep.judge.application.dto.read.SubmissionReads.Page;
import java.util.UUID;

/** Authorized read contract from F4 API sections 7-10 and 13-15. */
public interface ListInstructorSubmissionsUseCase {
  Page<InstructorItem> list(UUID instructor, Filters filters);
}
