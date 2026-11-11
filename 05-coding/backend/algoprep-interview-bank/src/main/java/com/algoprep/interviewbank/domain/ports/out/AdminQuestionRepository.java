package com.algoprep.interviewbank.domain.ports.out;

import com.algoprep.interviewbank.domain.model.AdminQuestion;
import com.algoprep.interviewbank.domain.model.AdminQuestionPage;
import com.algoprep.interviewbank.domain.model.CatalogKind;
import com.algoprep.interviewbank.domain.model.QuestionDraft;
import com.algoprep.interviewbank.domain.model.QuestionListCriteria;
import com.algoprep.interviewbank.domain.model.QuestionRow;
import com.algoprep.interviewbank.domain.model.RubricCriterion;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Writes join the caller transaction; the lock methods only make sense inside one. */
public interface AdminQuestionRepository {
  AdminQuestionPage page(QuestionListCriteria criteria);

  /** Only ACTIVE questions; a RETIRED one reads as absent. */
  Optional<AdminQuestion> findActive(UUID id);

  /**
   * Key-share lock on a topic or level row. It waits for a concurrent catalogue delete and returns
   * false when the row is gone, so a question can never be created under a deleted reference.
   */
  boolean lockReference(CatalogKind kind, UUID id);

  Optional<QuestionRow> lockActive(UUID id);

  /** Rows of any status, locked in id order so two bulk deletes cannot deadlock. */
  List<QuestionRow> lockAll(Collection<UUID> ids);

  /** Inserts the question and its rubric; the display number comes from the sequence. */
  AdminQuestion insert(QuestionDraft draft, UUID actor);

  void update(UUID id, QuestionDraft draft);

  void replaceRubric(UUID id, List<RubricCriterion> rubric);

  void retire(UUID id);

  /** Recomputes practice_history for everyone who answered the question (BR-IB-09). */
  void recalculateProgress(UUID questionId, Collection<UUID> topicIds);

  /** Transaction-scoped advisory lock that serialises CSV imports. */
  void lockImport();

  /** Normalised titles of every ACTIVE question. */
  Set<String> activeTitleKeys();

  /** Same normalisation the database uses, {@code lower(btrim(title))}. */
  String titleKey(String title);
}
