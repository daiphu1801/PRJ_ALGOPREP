package com.algoprep.interviewbank.application.command;

import com.algoprep.common.exception.InvalidRequestException;
import com.algoprep.common.exception.ResourceNotFoundException;
import com.algoprep.common.exception.ServiceUnavailableException;
import com.algoprep.interviewbank.application.dto.BulkRetireCommand;
import com.algoprep.interviewbank.application.dto.BulkRetireResult;
import com.algoprep.interviewbank.application.dto.QuestionCommand;
import com.algoprep.interviewbank.domain.exception.QuestionRuleViolationException;
import com.algoprep.interviewbank.domain.model.AdminQuestion;
import com.algoprep.interviewbank.domain.model.CatalogKind;
import com.algoprep.interviewbank.domain.model.InterviewAuditEntry;
import com.algoprep.interviewbank.domain.model.QuestionDraft;
import com.algoprep.interviewbank.domain.model.QuestionRow;
import com.algoprep.interviewbank.domain.model.RubricCriterion;
import com.algoprep.interviewbank.domain.ports.out.AdminQuestionRepository;
import com.algoprep.interviewbank.domain.ports.out.InterviewBankAuditPort;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

/** BR-IB-03: create, update and soft delete of questions. Every caller is a transaction. */
@Service
class QuestionAuthoringService {
  private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

  private final AdminQuestionRepository repository;
  private final InterviewBankAuditPort audit;
  private final JsonMapper json = JsonMapper.builder().build();

  QuestionAuthoringService(AdminQuestionRepository repository, InterviewBankAuditPort audit) {
    this.repository = repository;
    this.audit = audit;
  }

  AdminQuestion create(QuestionCommand command) {
    checkRubric(command.rubric());
    requireReferences(command.topicId(), command.levelId());
    var created = repository.insert(draft(command), command.actorId());
    record(command.actorId(), "CREATED", created.id(), null, snapshot(created));
    return created;
  }

  AdminQuestion update(QuestionCommand command) {
    checkRubric(command.rubric());
    var row = lockActive(command.id());
    var before = repository.findActive(command.id()).orElseThrow(this::missing);
    requireReferences(command.topicId(), command.levelId());
    repository.update(command.id(), draft(command));
    repository.replaceRubric(command.id(), command.rubric());
    if (!row.topicId().equals(command.topicId())) {
      repository.recalculateProgress(command.id(), List.of(row.topicId(), command.topicId()));
    }
    var after = repository.findActive(command.id()).orElseThrow(this::missing);
    record(command.actorId(), "UPDATED", command.id(), snapshot(before), snapshot(after));
    return after;
  }

  BulkRetireResult retire(BulkRetireCommand command) {
    Map<UUID, QuestionRow> rows =
        repository.lockAll(command.ids()).stream()
            .collect(Collectors.toMap(QuestionRow::id, Function.identity()));
    var retired = new ArrayList<UUID>();
    var failed = new ArrayList<UUID>();
    for (UUID id : command.ids()) {
      var row = rows.get(id);
      if (row == null || !"ACTIVE".equals(row.status())) {
        failed.add(id);
        continue;
      }
      repository.retire(id);
      repository.recalculateProgress(id, List.of(row.topicId()));
      var before = new LinkedHashMap<String, Object>();
      before.put("title", row.title());
      before.put("status", "ACTIVE");
      var after = new LinkedHashMap<String, Object>();
      after.put("title", row.title());
      after.put("status", "RETIRED");
      record(command.actorId(), "RETIRED", id, before, after);
      retired.add(id);
    }
    return new BulkRetireResult(retired, failed);
  }

  /** Rules shared with the CSV import: unique codes (INB-V11), weights that add up (INB-033). */
  static void checkRubric(List<RubricCriterion> rubric) {
    Set<String> seen = new HashSet<>();
    for (RubricCriterion item : rubric) {
      if (!seen.add(item.criterionCode().toLowerCase(Locale.ROOT))) {
        throw new InvalidRequestException("INB-V11", "Rubric criterion names must be unique");
      }
    }
    if (!rubric.isEmpty()) {
      BigDecimal sum =
          rubric.stream()
              .map(RubricCriterion::weightPercent)
              .reduce(BigDecimal.ZERO, BigDecimal::add);
      if (sum.compareTo(HUNDRED) != 0) {
        throw new QuestionRuleViolationException(
            "INB-033", "Rubric weights must add up to 100", Map.of("currentSum", sum));
      }
    }
  }

  /** Locks the topic then the level row; waits out a concurrent catalogue delete. */
  private void requireReferences(UUID topicId, UUID levelId) {
    if (!repository.lockReference(CatalogKind.TOPIC, topicId)) {
      throw missingReference("topicId");
    }
    if (!repository.lockReference(CatalogKind.LEVEL, levelId)) {
      throw missingReference("levelId");
    }
  }

  private QuestionRuleViolationException missingReference(String field) {
    return new QuestionRuleViolationException(
        "INB-035", "Topic or level does not exist", Map.of("field", field));
  }

  private QuestionRow lockActive(UUID id) {
    return repository.lockActive(id).orElseThrow(this::missing);
  }

  private ResourceNotFoundException missing() {
    return new ResourceNotFoundException("INB-010", "Question not available");
  }

  private static QuestionDraft draft(QuestionCommand command) {
    return new QuestionDraft(
        command.topicId(),
        command.levelId(),
        command.title(),
        command.contentMarkdown(),
        command.followUps(),
        command.rubric(),
        null,
        null,
        null);
  }

  private static Map<String, Object> snapshot(AdminQuestion question) {
    var map = new LinkedHashMap<String, Object>();
    map.put("title", question.title());
    map.put("topicId", question.topicId());
    map.put("levelId", question.levelId());
    map.put("rubricCount", question.rubric().size());
    map.put(
        "rubricSum",
        question.rubric().stream()
            .map(RubricCriterion::weightPercent)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .stripTrailingZeros()
            .toPlainString());
    return map;
  }

  void record(UUID actor, String action, UUID target, Object before, Object after) {
    try {
      audit.record(
          new InterviewAuditEntry(
              actor,
              "INTERVIEW_QUESTION_" + action,
              "INTERVIEW_QUESTION",
              target.toString(),
              before == null ? null : json.writeValueAsString(before),
              after == null ? null : json.writeValueAsString(after)));
    } catch (RuntimeException failure) {
      throw new ServiceUnavailableException(
          "INB-503", "Audit write unavailable; change rolled back", failure);
    }
  }
}
