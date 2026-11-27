package com.algoprep.interviewbank.application.dto;

import com.algoprep.interviewbank.domain.model.RubricCriterion;
import java.util.List;
import java.util.UUID;

/** Create or update of one question; {@code id} is null on create. Shape already validated. */
public record QuestionCommand(
    UUID actorId,
    UUID id,
    UUID topicId,
    UUID levelId,
    String title,
    String contentMarkdown,
    List<String> followUps,
    List<RubricCriterion> rubric) {}
