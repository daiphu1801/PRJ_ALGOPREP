package com.algoprep.aireview.application.dto;

import com.algoprep.aireview.domain.model.InterviewResult;
import com.algoprep.aireview.domain.model.InterviewSession;
import com.algoprep.aireview.domain.model.InterviewTurn;
import com.algoprep.aireview.domain.model.ReplyState;
import java.util.List;

/** Endpoint 9 payload; {@code result} is non-null only for a COMPLETED session with four rows. */
public record InterviewSessionDetail(
    InterviewSession session,
    List<InterviewTurn> turns,
    ReplyState replyState,
    InterviewResult result) {
  public InterviewSessionDetail {
    turns = List.copyOf(turns);
  }
}
