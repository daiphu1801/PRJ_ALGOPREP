package com.algoprep.problembank.presentation.controller;

import com.algoprep.problembank.application.dto.CatalogQuery;
import com.algoprep.problembank.application.ports.in.CreateProblemTopicUseCase;
import com.algoprep.problembank.application.ports.in.DeleteProblemTopicUseCase;
import com.algoprep.problembank.application.ports.in.ListProblemTopicsUseCase;
import com.algoprep.problembank.application.ports.in.ReorderProblemTopicsUseCase;
import com.algoprep.problembank.application.ports.in.UpdateProblemTopicUseCase;
import com.algoprep.problembank.domain.model.CatalogItem;
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

/** Ten catalogue endpoints share rules, not another context's implementation. */
@RestController
@RequestMapping("/api/v1/problem-bank/topics")
class ProblemTopicsController {
  private final ListProblemTopicsUseCase list;
  private final CreateProblemTopicUseCase create;
  private final UpdateProblemTopicUseCase update;
  private final ReorderProblemTopicsUseCase reorder;
  private final DeleteProblemTopicUseCase delete;

  ProblemTopicsController(
      ListProblemTopicsUseCase list,
      CreateProblemTopicUseCase create,
      UpdateProblemTopicUseCase update,
      ReorderProblemTopicsUseCase reorder,
      DeleteProblemTopicUseCase delete) {
    this.list = list;
    this.create = create;
    this.update = update;
    this.reorder = reorder;
    this.delete = delete;
  }

  record Item(UUID id, String code, String name, int sortOrder, Long problemCount) {}

  record Items(List<Item> items) {}

  private Item dto(CatalogItem value) {
    return new Item(
        value.id(), value.code(), value.name(), value.sortOrder(), value.problemCount());
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
  @PreAuthorize("hasAuthority('PROBLEM_AUTHORING:CREATE')")
  Item create(JwtAuthenticationToken caller, @RequestBody JsonNode body) {
    return dto(
        create.execute(CatalogRequests.command(caller, null, body, "name", false)).getFirst());
  }

  @PatchMapping("/{id}")
  @PreAuthorize("hasAuthority('PROBLEM_AUTHORING:UPDATE')")
  Item update(JwtAuthenticationToken caller, @PathVariable String id, @RequestBody JsonNode body) {
    return dto(update.execute(CatalogRequests.command(caller, id, body, "name", false)).getFirst());
  }

  @PutMapping("/order")
  @PreAuthorize("hasAuthority('PROBLEM_AUTHORING:UPDATE')")
  Items reorder(JwtAuthenticationToken caller, @RequestBody JsonNode body) {
    return items(reorder.execute(CatalogRequests.command(caller, null, body, "name", true)));
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("hasAuthority('PROBLEM_AUTHORING:DELETE')")
  void delete(JwtAuthenticationToken caller, @PathVariable String id) {
    delete.execute(CatalogRequests.command(caller, id, null, "name", false));
  }
}
