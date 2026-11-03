package com.algoprep.judge.application.dto;

import com.algoprep.judge.domain.model.PendingSubmission.Owner;
import com.algoprep.judge.domain.model.PendingSubmission.Program;

/** Trusted caller supplies the authenticated student, never a user-selected owner. */
public record RecordPendingSubmissionCommand(Owner owner, Program program, String fileName) {
  @Override
  public String toString() {
    return "RecordPendingSubmissionCommand[owner=" + owner + ", program=" + program + "]";
  }
}
