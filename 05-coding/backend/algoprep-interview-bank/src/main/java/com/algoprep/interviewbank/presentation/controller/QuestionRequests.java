package com.algoprep.interviewbank.presentation.controller;

import com.algoprep.common.exception.InvalidRequestException;
import com.algoprep.common.exception.ResourceNotFoundException;
import com.algoprep.interviewbank.application.dto.AdminQuestionQuery;
import com.algoprep.interviewbank.application.dto.BulkRetireCommand;
import com.algoprep.interviewbank.application.dto.ImportCommand;
import com.algoprep.interviewbank.application.dto.ImportRow;
import com.algoprep.interviewbank.application.dto.QuestionCommand;
import com.algoprep.interviewbank.domain.model.QuestionSort;
import com.algoprep.interviewbank.domain.model.RubricCriterion;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Turns the trusted caller, query and raw JSON body into commands; shape errors are INB-V*. */
final class QuestionRequests {
  static final int QUESTION_BODY_LIMIT = 524_288;
  static final int IMPORT_BODY_LIMIT = 5_242_880;
  private static final int MAX_TITLE = 255;
  private static final int MAX_CODE = 100;
  private static final int MAX_QUERY = 100;
  private static final int MAX_BULK = 100;
  private static final int MAX_IMPORT_ROWS = 500;
  private static final Set<Integer> PAGE_SIZES = Set.of(8, 20, 50);
  private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
  private static final Map<String, QuestionSort> SORTS =
      Map.of(
          "displayNo", QuestionSort.DISPLAY_NO,
          "level", QuestionSort.LEVEL,
          "topic", QuestionSort.TOPIC,
          "title", QuestionSort.TITLE,
          "followUpCount", QuestionSort.FOLLOW_UP_COUNT,
          "rubricCount", QuestionSort.RUBRIC_COUNT,
          "usageCount", QuestionSort.USAGE_COUNT,
          "averageScorePercent", QuestionSort.AVERAGE_SCORE_PERCENT);
  private static final JsonMapper JSON =
      JsonMapper.builder().enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS).build();

  private QuestionRequests() {}

  static UUID actor(JwtAuthenticationToken caller) {
    return UUID.fromString(caller.getToken().getSubject());
  }

  /** An id that is not a UUID cannot exist, so it reads as a missing question (404). */
  static UUID questionId(String text) {
    try {
      return UUID.fromString(text);
    } catch (IllegalArgumentException failure) {
      throw new ResourceNotFoundException("INB-010", "Question not available");
    }
  }

  /** Reads at most {@code limit} bytes; a larger body is refused without being buffered. */
  static JsonNode body(HttpServletRequest request, int limit, String unreadableCode)
      throws IOException {
    byte[] bytes = request.getInputStream().readNBytes(limit + 1);
    if (bytes.length > limit) {
      throw new BodyTooLargeException();
    }
    try {
      return JSON.readTree(bytes);
    } catch (RuntimeException failure) {
      throw new InvalidRequestException(unreadableCode, "Request body is not readable JSON");
    }
  }

  static AdminQuestionQuery list(Map<String, String> params) {
    String q = params.get("q");
    String sort = params.get("sort");
    String dir = params.get("dir");
    String text = q == null ? null : q.strip();
    if (text != null && text.codePointCount(0, text.length()) > MAX_QUERY) {
      throw invalidQuery();
    }
    QuestionSort order = sort == null ? QuestionSort.DISPLAY_NO : SORTS.get(sort);
    if (order == null || (dir != null && !dir.equals("asc") && !dir.equals("desc"))) {
      throw invalidQuery();
    }
    int pageNumber = integer(params.get("page"), 1);
    int pageSize = integer(params.get("pageSize"), 8);
    if (pageNumber < 1 || !PAGE_SIZES.contains(pageSize)) {
      throw invalidQuery();
    }
    return new AdminQuestionQuery(
        text == null || text.isEmpty() ? null : text,
        blankToNull(params.get("topic")),
        blankToNull(params.get("level")),
        order,
        "desc".equals(dir),
        pageNumber,
        pageSize);
  }

  static QuestionCommand question(JwtAuthenticationToken caller, UUID id, JsonNode body) {
    if (!body.isObject()) {
      throw new InvalidRequestException("INB-V08", "Body must be a JSON object");
    }
    String title = body.path("title").isString() ? body.path("title").asString().strip() : "";
    if (title.isEmpty() || title.codePointCount(0, title.length()) > MAX_TITLE) {
      throw new InvalidRequestException("INB-V08", "Title must have 1 to 255 characters");
    }
    UUID topic = reference(body.path("topicId"));
    UUID level = reference(body.path("levelId"));
    JsonNode content = body.path("contentMarkdown");
    if (!content.isMissingNode() && !content.isNull() && !content.isString()) {
      throw new InvalidRequestException("INB-V08", "contentMarkdown must be text");
    }
    String markdown =
        content.isString() && !content.asString().isBlank() ? content.asString() : null;
    return new QuestionCommand(
        actor(caller), id, topic, level, title, markdown, followUps(body), rubric(body));
  }

  static BulkRetireCommand bulk(JwtAuthenticationToken caller, JsonNode body) {
    JsonNode ids = body.path("ids");
    if (!ids.isArray() || ids.isEmpty() || ids.size() > MAX_BULK) {
      throw invalidIds();
    }
    var parsed = new ArrayList<UUID>();
    var seen = new HashSet<UUID>();
    for (JsonNode node : ids) {
      UUID id = node.isString() ? uuid(node.asString()) : null;
      if (id == null || !seen.add(id)) {
        throw invalidIds();
      }
      parsed.add(id);
    }
    return new BulkRetireCommand(actor(caller), List.copyOf(parsed));
  }

  static ImportCommand importRows(JwtAuthenticationToken caller, JsonNode body) {
    JsonNode rows = body.path("rows");
    if (!rows.isArray() || rows.isEmpty() || rows.size() > MAX_IMPORT_ROWS) {
      throw invalidRows();
    }
    var parsed = new ArrayList<ImportRow>();
    for (JsonNode row : rows) {
      parsed.add(importRow(row));
    }
    return new ImportCommand(actor(caller), List.copyOf(parsed));
  }

  private static ImportRow importRow(JsonNode row) {
    if (!row.isObject()
        || !row.path("line").isIntegralNumber()
        || !row.path("line").canConvertToInt()) {
      throw invalidRows();
    }
    boolean[] ok = {true};
    boolean rubricOk = true;
    var rubric = new ArrayList<RubricCriterion>();
    JsonNode raw = row.path("rubric");
    if (raw.isArray()) {
      for (JsonNode item : raw) {
        if (!item.isObject()) {
          rubricOk = false;
          break;
        }
        rubric.add(
            new RubricCriterion(
                item.path("criterionCode").isString()
                    ? item.path("criterionCode").asString()
                    : null,
                item.path("weightPercent").isNumber()
                    ? item.path("weightPercent").decimalValue()
                    : null,
                null));
      }
    } else if (!raw.isMissingNode() && !raw.isNull()) {
      rubricOk = false;
    }
    return new ImportRow(
        row.path("line").intValue(),
        text(row.path("topic")),
        text(row.path("level")),
        text(row.path("question")),
        rubricOk ? List.copyOf(rubric) : List.of(),
        rubricOk,
        list(row.path("followUps"), ok),
        optionalText(row.path("content"), ok),
        list(row.path("suggestedApproach"), ok),
        list(row.path("coreKeywords"), ok),
        optionalText(row.path("sampleAnswer"), ok),
        ok[0]);
  }

  private static String text(JsonNode node) {
    return node.isString() ? node.asString() : null;
  }

  private static String optionalText(JsonNode node, boolean[] ok) {
    if (!node.isMissingNode() && !node.isNull() && !node.isString()) {
      ok[0] = false;
    }
    return text(node);
  }

  private static List<String> list(JsonNode node, boolean[] ok) {
    if (node.isMissingNode() || node.isNull()) {
      return List.of();
    }
    var values = new ArrayList<String>();
    if (node.isArray()) {
      for (JsonNode item : node) {
        if (!item.isString()) {
          ok[0] = false;
          return List.of();
        }
        values.add(item.asString());
      }
      return values;
    }
    ok[0] = false;
    return List.of();
  }

  private static UUID reference(JsonNode node) {
    UUID id = node.isString() ? uuid(node.asString()) : null;
    if (id == null) {
      throw new InvalidRequestException("INB-V09", "topicId and levelId must be UUIDs");
    }
    return id;
  }

  private static List<String> followUps(JsonNode body) {
    JsonNode node = body.path("followUpQuestions");
    if (node.isMissingNode() || node.isNull()) {
      return List.of();
    }
    if (!node.isArray()) {
      throw new InvalidRequestException("INB-V12", "followUpQuestions must be an array of text");
    }
    var values = new ArrayList<String>();
    for (JsonNode item : node) {
      if (!item.isString()) {
        throw new InvalidRequestException("INB-V12", "followUpQuestions must be an array of text");
      }
      String value = item.asString().strip();
      if (!value.isEmpty()) {
        values.add(value);
      }
    }
    return List.copyOf(values);
  }

  private static List<RubricCriterion> rubric(JsonNode body) {
    JsonNode node = body.path("rubric");
    if (node.isMissingNode() || node.isNull()) {
      return List.of();
    }
    if (!node.isArray()) {
      throw invalidCriterion();
    }
    var values = new ArrayList<RubricCriterion>();
    for (JsonNode item : node) {
      String code =
          item.path("criterionCode").isString()
              ? item.path("criterionCode").asString().strip()
              : "";
      JsonNode weight = item.path("weightPercent");
      JsonNode description = item.path("description");
      if (!item.isObject()
          || code.isEmpty()
          || code.codePointCount(0, code.length()) > MAX_CODE
          || !weight.isNumber()
          || weight.decimalValue().signum() < 0
          || weight.decimalValue().compareTo(HUNDRED) > 0
          || weight.decimalValue().stripTrailingZeros().scale() > 2
          || (!description.isMissingNode() && !description.isNull() && !description.isString())) {
        throw invalidCriterion();
      }
      values.add(
          new RubricCriterion(
              code, weight.decimalValue(), description.isString() ? description.asString() : null));
    }
    return List.copyOf(values);
  }

  private static UUID uuid(String text) {
    try {
      return UUID.fromString(text);
    } catch (IllegalArgumentException failure) {
      return null;
    }
  }

  private static int integer(String text, int fallback) {
    if (text == null) {
      return fallback;
    }
    try {
      return Integer.parseInt(text.strip());
    } catch (NumberFormatException failure) {
      return -1;
    }
  }

  private static String blankToNull(String text) {
    return text == null || text.isBlank() ? null : text.strip();
  }

  private static InvalidRequestException invalidQuery() {
    return new InvalidRequestException("INB-V01", "Invalid list parameter");
  }

  private static InvalidRequestException invalidIds() {
    return new InvalidRequestException("INB-V02", "ids must be 1 to 100 distinct UUIDs");
  }

  private static InvalidRequestException invalidRows() {
    return new InvalidRequestException("INB-V15", "rows must hold 1 to 500 objects with a line");
  }

  private static InvalidRequestException invalidCriterion() {
    return new InvalidRequestException("INB-V10", "Invalid rubric criterion");
  }
}
