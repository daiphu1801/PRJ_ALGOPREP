package com.algoprep.judge.application.dto;

import com.algoprep.judge.domain.model.queue.SubmissionLifecycleView;

/** Finalized is true only for the caller that committed the guarded update and its Outbox row. */
public record QueueFailureResolution(SubmissionLifecycleView submission, boolean finalized) {}
