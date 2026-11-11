package com.algoprep.aireview.presentation.controller;

import com.algoprep.aireview.application.ports.in.CreateModelPriceUseCase;
import com.algoprep.aireview.application.ports.in.DeleteModelPriceUseCase;
import com.algoprep.aireview.application.ports.in.ListModelPricesUseCase;
import com.algoprep.aireview.application.ports.in.UpdateModelPriceUseCase;
import com.algoprep.aireview.domain.model.ModelPrice;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

/** DD api F5 sections 23 to 26 (ADM0303 price table): AI_CONFIG cells plus the ADMIN category. */
@RestController
@RequestMapping("/api/v1/ai/admin/model-prices")
class ModelPriceController {
  private static final String READ =
      "hasAuthority('ROLE_ADMIN') and hasAuthority('AI_CONFIG:READ')";
  private static final String UPDATE =
      "hasAuthority('ROLE_ADMIN') and hasAuthority('AI_CONFIG:UPDATE')";

  private final ListModelPricesUseCase list;
  private final CreateModelPriceUseCase create;
  private final UpdateModelPriceUseCase update;
  private final DeleteModelPriceUseCase delete;

  ModelPriceController(
      ListModelPricesUseCase list,
      CreateModelPriceUseCase create,
      UpdateModelPriceUseCase update,
      DeleteModelPriceUseCase delete) {
    this.list = list;
    this.create = create;
    this.update = update;
    this.delete = delete;
  }

  record Item(UUID id, String modelName, BigDecimal usdPerMillionTokens) {}

  record Items(List<Item> items) {}

  private static Item item(ModelPrice p) {
    return new Item(p.id(), p.modelName(), p.usdPerMillionTokens());
  }

  @GetMapping
  @PreAuthorize(READ)
  Items list() {
    return new Items(list.list().stream().map(ModelPriceController::item).toList());
  }

  @PostMapping
  @PreAuthorize(UPDATE)
  @ResponseStatus(HttpStatus.CREATED)
  Item create(@RequestBody JsonNode body) {
    return item(create.create(BudgetRequests.createName(body)));
  }

  @PatchMapping("/{priceId}")
  @PreAuthorize(UPDATE)
  Item update(@PathVariable String priceId, @RequestBody JsonNode body) {
    UUID id = BudgetRequests.priceId(priceId);
    BudgetRequests.PricePatch patch = BudgetRequests.pricePatch(body);
    return item(update.update(id, patch.modelName(), patch.usdPerMillionTokens()));
  }

  @DeleteMapping("/{priceId}")
  @PreAuthorize(UPDATE)
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void delete(@PathVariable String priceId) {
    delete.delete(BudgetRequests.priceId(priceId));
  }
}
