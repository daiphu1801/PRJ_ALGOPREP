package com.algoprep.interviewbank.domain.model;

import java.time.Instant;

/** Current self rating of one learner for one question. */
public record RecallState(
    RecallRating rating, int intervalDays, Instant nextReviewAt, Instant ratedAt) {}
