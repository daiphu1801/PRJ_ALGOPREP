package com.algoprep.problembank.presentation.controller;

import com.algoprep.problembank.application.dto.CatalogQuery;
import com.algoprep.problembank.application.ports.in.CreateProblemLevelUseCase;
import com.algoprep.problembank.application.ports.in.DeleteProblemLevelUseCase;
import com.algoprep.problembank.application.ports.in.ListProblemLevelsUseCase;
import com.algoprep.problembank.application.ports.in.ReorderProblemLevelsUseCase;
import com.algoprep.problembank.application.ports.in.UpdateProblemLevelUseCase;
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
@RequestMapping("/api/v1/problem-bank/levels")
class ProblemLevelsController {
  private final ListProblemLevelsUseCase list;
  private final CreateProblemLevelUseCase create;
  private final UpdateProblemLevelUseCase update;
  private final ReorderProblemLevelsUseCase reorder;
  private final DeleteProblemLevelUseCase delete;

  ProblemLevelsController(
      ListProblemLevelsUseCase list,
      CreateProblemLevelUseCase create,
      UpdateProblemLevelUseCase update,
      ReorderProblemLevelsUseCase reorder,
      DeleteProblemLevelUseCase delete) {
    this.list = list;
    this.create = create;
    this.update = update;
    this.reorder = reorder;
    this.delete = delete;
  }

  record Item(UUID id, String code, String displayName, int sortOrder, Long problemCount) {}

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
        create
            .execute(CatalogRequests.command(caller, null, body, "displayName", false))
            .getFirst());
  }

  @PatchMapping("/{id}")
  @PreAuthorize("hasAuthority('PROBLEM_AUTHORING:UPDATE')")
  Item update(JwtAuthenticationToken caller, @PathVariable String id, @RequestBody JsonNode body) {
    return dto(
        update.execute(CatalogRequests.command(caller, id, body, "displayName", false)).getFirst());
  }

  @PutMapping("/order")
  @PreAuthorize("hasAuthority('PROBLEM_AUTHORING:UPDATE')")
  Items reorder(JwtAuthenticationToken caller, @RequestBody JsonNode body) {
    return items(reorder.execute(CatalogRequests.command(caller, null, body, "displayName", true)));
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("hasAuthority('PROBLEM_AUTHORING:DELETE')")
  void delete(JwtAuthenticationToken caller, @PathVariable String id) {
    delete.execute(CatalogRequests.command(caller, id, null, "displayName", false));
  }
}
