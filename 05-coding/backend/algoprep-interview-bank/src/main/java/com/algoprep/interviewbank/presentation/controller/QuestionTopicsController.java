package com.algoprep.interviewbank.presentation.controller;

import com.algoprep.interviewbank.application.dto.CatalogQuery;
import com.algoprep.interviewbank.application.ports.in.CreateQuestionTopicUseCase;
import com.algoprep.interviewbank.application.ports.in.DeleteQuestionTopicUseCase;
import com.algoprep.interviewbank.application.ports.in.ListQuestionTopicsUseCase;
import com.algoprep.interviewbank.application.ports.in.ReorderQuestionTopicsUseCase;
import com.algoprep.interviewbank.application.ports.in.UpdateQuestionTopicUseCase;
import com.algoprep.interviewbank.domain.model.CatalogItem;
import com.algoprep.interviewbank.domain.model.CatalogKind;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

/**
 * DD api F6 sections 9 and 11-14: read for everyone, write for ADMIN with the matching function
 * cell.
 */
@RestController
@RequestMapping("/api/v1/interview-bank/topics")
class QuestionTopicsController {
  private final ListQuestionTopicsUseCase list;
  private final CreateQuestionTopicUseCase create;
  private final UpdateQuestionTopicUseCase update;
  private final ReorderQuestionTopicsUseCase reorder;
  private final DeleteQuestionTopicUseCase delete;

  QuestionTopicsController(
      ListQuestionTopicsUseCase list,
      CreateQuestionTopicUseCase create,
      UpdateQuestionTopicUseCase update,
      ReorderQuestionTopicsUseCase reorder,
      DeleteQuestionTopicUseCase delete) {
    this.list = list;
    this.create = create;
    this.update = update;
    this.reorder = reorder;
    this.delete = delete;
  }

  record Item(
      UUID id,
      String code,
      String displayName,
      int sortOrder,
      boolean usesStarFramework,
      Long questionCount) {}

  record Items(List<Item> items) {}

  private Item dto(CatalogItem value) {
    return new Item(
        value.id(),
        value.code(),
        value.displayName(),
        value.sortOrder(),
        value.usesStarFramework(),
        value.questionCount());
  }

  private Items items(List<CatalogItem> values) {
    return new Items(values.stream().map(this::dto).toList());
  }

  @GetMapping
  @PreAuthorize("isAuthenticated()")
  Items list(
      JwtAuthenticationToken caller, @RequestParam(defaultValue = "false") boolean includeCounts) {
    return items(list.list(new CatalogQuery(CatalogRequests.admin(caller), includeCounts)));
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAuthority('INTERVIEW_BANK_MANAGEMENT:CREATE')")
  Item create(JwtAuthenticationToken caller, @RequestBody JsonNode body) {
    return dto(create.execute(CatalogRequests.create(caller, body)));
  }

  @PatchMapping("/{id}")
  @PreAuthorize("hasAuthority('INTERVIEW_BANK_MANAGEMENT:UPDATE')")
  Item update(JwtAuthenticationToken caller, @PathVariable String id, @RequestBody JsonNode body) {
    return dto(update.execute(CatalogRequests.update(caller, CatalogKind.TOPIC, id, body)));
  }

  @PutMapping("/order")
  @PreAuthorize("hasAuthority('INTERVIEW_BANK_MANAGEMENT:UPDATE')")
  Items reorder(JwtAuthenticationToken caller, @RequestBody JsonNode body) {
    return items(reorder.execute(CatalogRequests.reorder(caller, body)));
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("hasAuthority('INTERVIEW_BANK_MANAGEMENT:DELETE')")
  void delete(JwtAuthenticationToken caller, @PathVariable String id) {
    delete.execute(CatalogRequests.delete(caller, CatalogKind.TOPIC, id));
  }
}
