package com.algoprep.judge.domain.model.read;

import com.algoprep.judge.domain.model.AcceptedSubmissionSnapshot;

/** Non-Accepted rows carry status only, never a source snapshot. */
public record SubmissionSnapshotLookup(String status, AcceptedSubmissionSnapshot snapshot) {}
