package com.algoprep.judge.domain.ports.out;

import com.algoprep.judge.domain.model.read.HistoryWindow;
import com.algoprep.judge.domain.model.read.SubmissionReadState;
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

  /** PENDING submissions ordered before the given one by (submitted_at, id); count only. */
  long pendingAhead(UUID id);

  /** Submissions that reached a terminal status within the last 60 seconds (database clock). */
  long finishedLastMinute();

  List<SubmissionReadState> recent(UUID owner, UUID problem, int limit);

  long count(UUID owner, String verdict, String language, Set<UUID> problems);

  List<SubmissionReadState> history(
      UUID owner, String verdict, String language, HistoryWindow window);
}
