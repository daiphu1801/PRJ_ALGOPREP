package com.algoprep.interviewbank.presentation.controller;

import com.algoprep.interviewbank.application.ports.in.CreateQuestionUseCase;
import com.algoprep.interviewbank.application.ports.in.GetAdminQuestionUseCase;
import com.algoprep.interviewbank.application.ports.in.ListAdminQuestionsUseCase;
import com.algoprep.interviewbank.application.ports.in.UpdateQuestionUseCase;
import com.algoprep.interviewbank.domain.model.AdminQuestion;
import com.algoprep.interviewbank.domain.model.AdminQuestionPage;
import com.algoprep.interviewbank.domain.model.AdminQuestionRow;
import com.algoprep.interviewbank.domain.model.RubricCriterion;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * DD api F6 sections 19 to 24. Authority is the Function cell alone: INSTRUCTOR and ADMIN share one
 * scope, so there is no role check and no created_by filter.
 */
@RestController
@RequestMapping("/api/v1/interview-bank/admin/questions")
class AdminQuestionsController {
  private final ListAdminQuestionsUseCase list;
  private final GetAdminQuestionUseCase get;
  private final CreateQuestionUseCase create;
  private final UpdateQuestionUseCase update;

  AdminQuestionsController(
      ListAdminQuestionsUseCase list,
      GetAdminQuestionUseCase get,
      CreateQuestionUseCase create,
      UpdateQuestionUseCase update) {
    this.list = list;
    this.get = get;
    this.create = create;
    this.update = update;
  }

  record Ref(String code, String displayName) {}

  record Row(
      UUID id,
      int displayNo,
      Ref level,
      Ref topic,
      String title,
      int followUpCount,
      int rubricCount,
      long usageCount,
      BigDecimal averageScorePercent) {}

  record Page(int number, int size, long totalItems, int totalPages) {}

  record Meta(long shown, long total) {}

  record Rows(List<Row> items, Page page, Meta meta) {}

  record Criterion(String criterionCode, String description, BigDecimal weightPercent) {}

  record Usage(long usageCount, BigDecimal averageScorePercent) {}

  record Detail(
      UUID id,
      int displayNo,
      UUID topicId,
      UUID levelId,
      String title,
      String contentMarkdown,
      List<String> followUpQuestions,
      List<Criterion> rubric,
      String status,
      boolean practiceAvailable,
      Usage usage,
      Instant createdAt,
      Instant updatedAt) {}

  private static Detail detail(AdminQuestion q) {
    return new Detail(
        q.id(),
        q.displayNo(),
        q.topicId(),
        q.levelId(),
        q.title(),
        q.contentMarkdown(),
        q.followUps(),
        q.rubric().stream().map(AdminQuestionsController::criterion).toList(),
        "ACTIVE",
        !q.rubric().isEmpty(),
        new Usage(q.usageCount(), q.averageScorePercent()),
        q.createdAt(),
        q.updatedAt());
  }

  private static Criterion criterion(RubricCriterion item) {
    return new Criterion(item.criterionCode(), item.description(), item.weightPercent());
  }

  private static Row row(AdminQuestionRow r) {
    return new Row(
        r.id(),
        r.displayNo(),
        new Ref(r.levelCode(), r.levelName()),
        new Ref(r.topicCode(), r.topicName()),
        r.title(),
        r.followUpCount(),
        r.rubricCount(),
        r.usageCount(),
        r.averageScorePercent());
  }

  @GetMapping
  @PreAuthorize("hasAuthority('INTERVIEW_BANK_MANAGEMENT:READ')")
  Rows list(@RequestParam Map<String, String> params) {
    AdminQuestionPage result = list.list(QuestionRequests.list(params));
    int pages = (int) Math.max(1, (result.shown() + result.size() - 1) / result.size());
    return new Rows(
        result.items().stream().map(AdminQuestionsController::row).toList(),
        new Page(result.number(), result.size(), result.shown(), pages),
        new Meta(result.shown(), result.total()));
  }

  @GetMapping("/{questionId}")
  @PreAuthorize("hasAuthority('INTERVIEW_BANK_MANAGEMENT:READ')")
  Detail get(@PathVariable String questionId) {
    return detail(get.get(QuestionRequests.questionId(questionId)));
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAuthority('INTERVIEW_BANK_MANAGEMENT:CREATE')")
  Detail create(JwtAuthenticationToken caller, HttpServletRequest request) throws IOException {
    var body = QuestionRequests.body(request, QuestionRequests.QUESTION_BODY_LIMIT, "INB-V08");
    return detail(create.execute(QuestionRequests.question(caller, null, body)));
  }

  @PutMapping("/{questionId}")
  @PreAuthorize("hasAuthority('INTERVIEW_BANK_MANAGEMENT:UPDATE')")
  Detail update(
      JwtAuthenticationToken caller, @PathVariable String questionId, HttpServletRequest request)
      throws IOException {
    UUID id = QuestionRequests.questionId(questionId);
    var body = QuestionRequests.body(request, QuestionRequests.QUESTION_BODY_LIMIT, "INB-V08");
    return detail(update.execute(QuestionRequests.question(caller, id, body)));
  }
}
