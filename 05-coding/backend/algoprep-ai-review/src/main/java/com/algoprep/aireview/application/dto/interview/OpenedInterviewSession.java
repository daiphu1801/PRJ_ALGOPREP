package com.algoprep.aireview.application.dto.interview;

import com.algoprep.aireview.domain.model.interview.InterviewSession;
import com.algoprep.aireview.domain.model.interview.InterviewTurn;

/** Endpoint 8 payload: the new session and its opening AI line. */
public record OpenedInterviewSession(InterviewSession session, InterviewTurn opening) {}
