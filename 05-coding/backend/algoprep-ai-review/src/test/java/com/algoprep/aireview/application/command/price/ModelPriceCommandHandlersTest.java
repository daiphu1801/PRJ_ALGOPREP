package com.algoprep.aireview.application.command.price;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.aireview.domain.exception.price.AiModelPriceNotFoundException;
import com.algoprep.aireview.domain.model.price.ModelPrice;
import com.algoprep.aireview.domain.ports.out.AiModelPriceRepository;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ModelPriceCommandHandlersTest {
  @Test
  void priceHandlersDelegateAndAnUnknownIdIsNotFound() {
    AiModelPriceRepository prices = mock(AiModelPriceRepository.class);
    ModelPriceCommandHandlers handlers = new ModelPriceCommandHandlers(prices);
    UUID id = UUID.randomUUID();
    ModelPrice row = new ModelPrice(id, "gpt-x", BigDecimal.ZERO);
    when(prices.create("gpt-x")).thenReturn(row);
    when(prices.update(id, null, BigDecimal.ONE)).thenReturn(Optional.of(row));
    when(prices.update(eq(id), eq("n"), any())).thenReturn(Optional.empty());

    assertThat(handlers.create("gpt-x")).isSameAs(row);
    assertThat(handlers.update(id, null, BigDecimal.ONE)).isSameAs(row);
    assertThatThrownBy(() -> handlers.update(id, "n", null))
        .isInstanceOf(AiModelPriceNotFoundException.class);
    handlers.delete(id);
    verify(prices).delete(id);
  }
}
