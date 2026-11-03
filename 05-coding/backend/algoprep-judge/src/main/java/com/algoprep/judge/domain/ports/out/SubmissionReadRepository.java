package com.algoprep.judge.domain.ports.out;

import com.algoprep.judge.domain.model.HistoryWindow;
import com.algoprep.judge.domain.model.SubmissionReadState;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Uses only judge tables. Owner predicate is inside SQL; caller supplies instructor scope. */
public interface SubmissionReadRepository {
  Optional<SubmissionReadState> owned(UUID id, UUID owner);

  Optional<SubmissionReadState> find(UUID id);

  List<SubmissionReadState.Case> cases(UUID id);

  SubmissionReadState.Beats beats(SubmissionReadState state);

  boolean stderrEnabled();

  List<SubmissionReadState> recent(UUID owner, UUID problem, int limit);

  long count(UUID owner, String verdict, String language, Set<UUID> problems);

  List<SubmissionReadState> history(
      UUID owner, String verdict, String language, HistoryWindow window);
}
