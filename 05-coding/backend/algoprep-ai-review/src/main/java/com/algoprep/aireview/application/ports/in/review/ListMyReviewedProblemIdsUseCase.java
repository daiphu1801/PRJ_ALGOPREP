package com.algoprep.aireview.application.ports.in.review;

import java.util.List;
import java.util.UUID;

/** Endpoint 5. */
public interface ListMyReviewedProblemIdsUseCase {
  List<UUID> list(UUID callerId);
}
