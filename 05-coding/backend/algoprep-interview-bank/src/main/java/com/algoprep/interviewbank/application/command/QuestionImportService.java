package com.algoprep.interviewbank.application.command;

import com.algoprep.interviewbank.application.dto.ImportCommand;
import com.algoprep.interviewbank.application.dto.ImportResult;
import com.algoprep.interviewbank.application.dto.ImportResult.SkippedRow;
import com.algoprep.interviewbank.application.dto.ImportRow;
import com.algoprep.interviewbank.domain.model.CatalogItem;
import com.algoprep.interviewbank.domain.model.CatalogKind;
import com.algoprep.interviewbank.domain.model.QuestionDraft;
import com.algoprep.interviewbank.domain.model.RubricCriterion;
import com.algoprep.interviewbank.domain.ports.out.AdminQuestionRepository;
import com.algoprep.interviewbank.domain.ports.out.CatalogRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * BR-IB-11. Valid rows are inserted, invalid or duplicate rows are skipped with a reason, all in
 * the caller transaction, so an audit failure rolls back every imported question.
 */
@Service
class QuestionImportService {
  private static final int MAX_TITLE = 255;
  private static final int MAX_CODE = 100;
  private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

  private final AdminQuestionRepository repository;
  private final CatalogRepository catalog;
  private final QuestionAuthoringService authoring;

  QuestionImportService(
      AdminQuestionRepository repository,
      CatalogRepository catalog,
      QuestionAuthoringService authoring) {
    this.repository = repository;
    this.catalog = catalog;
    this.authoring = authoring;
  }

  ImportResult run(ImportCommand command) {
    repository.lockImport();
    Set<String> titles = new HashSet<>(repository.activeTitleKeys());
    var topics = index(catalog.list(CatalogKind.TOPIC, false));
    var levels = index(catalog.list(CatalogKind.LEVEL, false));
    var skipped = new ArrayList<SkippedRow>();
    int imported = 0;
    for (ImportRow row : command.rows()) {
      String reason = firstProblem(row, topics, levels);
      if (reason == null) {
        String key = repository.titleKey(row.question().strip());
        reason =
            titles.contains(key) ? "duplicate" : insert(command.actorId(), row, topics, levels);
        if (reason == null) {
          titles.add(key);
        }
      }
      if (reason == null) {
        imported++;
      } else {
        skipped.add(new SkippedRow(row.line(), reason, null));
      }
    }
    return new ImportResult(imported, skipped);
  }

  /** Returns a skip reason, or null after inserting and auditing the question. */
  private String insert(
      UUID actor, ImportRow row, Map<String, UUID> topics, Map<String, UUID> levels) {
    UUID topic = topics.get(lookupKey(row.topic()));
    UUID level = levels.get(lookupKey(row.level()));
    // A topic or level deleted since the catalogue was read must skip the row, not break the file.
    if (!repository.lockReference(CatalogKind.TOPIC, topic)) {
      return "unknownTopic";
    }
    if (!repository.lockReference(CatalogKind.LEVEL, level)) {
      return "unknownLevel";
    }
    var created =
        repository.insert(
            new QuestionDraft(
                topic,
                level,
                row.question().strip(),
                blankToNull(row.content()),
                clean(row.followUps()),
                trimmed(row.rubric()),
                approach(row.suggestedApproach()),
                nullIfEmpty(clean(row.coreKeywords())),
                blankToNull(row.sampleAnswer())),
            actor);
    var after = new LinkedHashMap<String, Object>();
    after.put("title", created.title());
    after.put("topicId", topic);
    after.put("levelId", level);
    after.put("line", row.line());
    authoring.record(actor, "IMPORTED", created.id(), null, after);
    return null;
  }

  private String firstProblem(ImportRow row, Map<String, UUID> topics, Map<String, UUID> levels) {
    if (blank(row.topic()) || blank(row.level()) || blank(row.question())) {
      return "missingField";
    }
    if (!topics.containsKey(lookupKey(row.topic()))) {
      return "unknownTopic";
    }
    if (!levels.containsKey(lookupKey(row.level()))) {
      return "unknownLevel";
    }
    if (!row.rubricShapeOk() || !rubricWellFormed(row.rubric())) {
      return "badRubric";
    }
    if (!row.rubric().isEmpty() && sum(row.rubric()).compareTo(HUNDRED) != 0) {
      return "rubricSum";
    }
    String title = row.question().strip();
    if (!row.fieldsShapeOk() || title.codePointCount(0, title.length()) > MAX_TITLE) {
      return "invalidField";
    }
    return null;
  }

  private static boolean rubricWellFormed(List<RubricCriterion> rubric) {
    Set<String> seen = new HashSet<>();
    for (RubricCriterion item : rubric) {
      String code = item.criterionCode() == null ? "" : item.criterionCode().strip();
      BigDecimal weight = item.weightPercent();
      if (code.isEmpty()
          || code.codePointCount(0, code.length()) > MAX_CODE
          || weight == null
          || weight.signum() < 0
          || weight.compareTo(HUNDRED) > 0
          || weight.stripTrailingZeros().scale() > 2
          || !seen.add(code.toLowerCase(Locale.ROOT))) {
        return false;
      }
    }
    return true;
  }

  private static List<RubricCriterion> trimmed(List<RubricCriterion> rubric) {
    return rubric.stream()
        .map(
            item ->
                new RubricCriterion(
                    item.criterionCode().strip(), item.weightPercent(), item.description()))
        .toList();
  }

  private static BigDecimal sum(List<RubricCriterion> rubric) {
    return rubric.stream()
        .map(RubricCriterion::weightPercent)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  /** Keys by code and by display name, both case-insensitive (code wins on a clash). */
  private static Map<String, UUID> index(List<CatalogItem> items) {
    Map<String, UUID> map = new HashMap<>();
    for (CatalogItem item : items) {
      map.putIfAbsent(lookupKey(item.displayName()), item.id());
    }
    for (CatalogItem item : items) {
      map.put(lookupKey(item.code()), item.id());
    }
    return map;
  }

  private static String lookupKey(String text) {
    return text.strip().toLowerCase(Locale.ROOT);
  }

  private static boolean blank(String text) {
    return text == null || text.isBlank();
  }

  private static String blankToNull(String text) {
    return blank(text) ? null : text;
  }

  private static List<String> clean(List<String> values) {
    return values == null
        ? List.of()
        : values.stream().map(String::strip).filter(v -> !v.isEmpty()).toList();
  }

  private static List<String> nullIfEmpty(List<String> values) {
    return values.isEmpty() ? null : values;
  }

  /** Suggested approach is stored as a Markdown bullet list, one item per line (DD api Q8). */
  private static String approach(List<String> items) {
    var cleaned = clean(items);
    return cleaned.isEmpty() ? null : "- " + String.join("\n- ", cleaned);
  }
}
