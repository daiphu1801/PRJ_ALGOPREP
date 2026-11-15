package com.algoprep.judge.domain.model;

/** Non-Accepted rows carry status only, never a source snapshot. */
public record SubmissionSnapshotLookup(String status, AcceptedSubmissionSnapshot snapshot) {}
