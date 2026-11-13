package com.algoprep.aireview.application.dto;

import com.algoprep.aireview.domain.model.InterviewSession;
import com.algoprep.aireview.domain.model.InterviewTurn;

/** Endpoint 8 payload: the new session and its opening AI line. */
public record OpenedInterviewSession(InterviewSession session, InterviewTurn opening) {}
