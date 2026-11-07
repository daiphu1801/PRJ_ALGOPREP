package com.algoprep.problembank.application.ports.in;

import com.algoprep.problembank.application.dto.StudentProblemViews;
import com.algoprep.problembank.domain.model.PublishedProblemModels.Overview;
import com.algoprep.problembank.domain.model.PublishedProblemModels.Workspace;
import java.util.Map;
import java.util.UUID;

/**
 * Learner-facing problem reads of DD F2 sections 2, 3, 6, 7 and 8. List and random read their
 * parameters leniently: a value that is not valid is ignored and the default applies.
 */
public interface ReadPublishedProblemsUseCase {
  StudentProblemViews.ProblemList list(UUID userId, Map<String, String> params);

  StudentProblemViews.RandomProblem random(UUID userId, Map<String, String> params);

  Workspace workspace(UUID userId, UUID problemId);

  Overview overview(UUID userId, UUID problemId);

  StudentProblemViews.SampleList samples(UUID problemId);
}
